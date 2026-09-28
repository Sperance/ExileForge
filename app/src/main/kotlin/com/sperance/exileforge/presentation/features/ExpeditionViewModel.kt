package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.campaign.AutoPlan
import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.HeroStance
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.VaalZones
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.party.MateView
import com.sperance.exileforge.core.party.PartyFollow
import com.sperance.exileforge.core.party.PartyLead
import com.sperance.exileforge.core.party.PartyMessage
import com.sperance.exileforge.core.party.RunParty
import com.sperance.exileforge.rules.party.PartyLaunch
import com.sperance.exileforge.rules.party.PartyWorld
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AtlasScreenState
import com.sperance.exileforge.presentation.state.LootEntry
import com.sperance.exileforge.presentation.state.MapLaunchState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunEventKind
import com.sperance.exileforge.rules.run.RunStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The campaign: the world map and the run on the map a player is walking.
 *
 * The run is a world the scene steps every frame; it is held here and the overlay reads its [ExpeditionRun.hud].
 * Every kill, chest and descent is an event of the run's journal, its reward rolled by the run's seed the moment
 * it happens; the journal is kept on disk and sent to the server in batches — at checkpoints, at the exit, at a
 * death, in the background — and the server's replay is the truth: what it refuses is counted, what it grants
 * comes back as the hero snapshot with its answer. A journal not sent is sent on the next launch.
 */
class ExpeditionViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {
    private val mutableRun = MutableStateFlow<ExpeditionRun?>(null)
    val run: StateFlow<ExpeditionRun?> = mutableRun.asStateFlow()
    /** The map run a Vaal zone was entered from: it stands still while the zone is played. */
    private var parent: ExpeditionRun? = null
    /** The journal of the run under way; named apart from [ForgeRuntime.journal], which `with(runtime)` would pick first. */
    private var runJournal: RunJournal? = null
    private val flushes = Channel<Unit>(Channel.CONFLATED)
    private var saveJob: Job? = null
    /** The next send of its own: a quiet stretch after the last event, or the retry after a failed send. */
    private var sendJob: Job? = null
    /** Sends failed in a row: each waits twice as long as the last before trying again. */
    private var failures = 0
    /** The autorun the run under way was started with; its Vaal zone runs by itself too. */
    private var autoPlan: AutoPlan? = null
    /** The Vaal zone's tokens the server already counted when the run was entered again. */
    private var vaalKilled: List<Int> = emptyList()
    /** The party the run under way is played in (3.25.0), shared by the map's run and its Vaal zone. */
    private var party: RunParty? = null
    /** A guest's campaign as the host's world set it: the windows the guest's map is built from. */
    private var hostWorld: CampaignState? = null
    /** Events of the host's Vaal zone that came before the guest's run entered it: they wait for it. */
    private val early = mutableListOf<RunEvent>()

    init {
        runtime.scope.launch { for (signal in flushes) flush() }
        // The clock's flush: what a quiet stretch of the map leaves pending goes out on its own.
        runtime.scope.launch { while (true) { delay(FLUSH_EVERY); if (runJournal?.pending?.isNotEmpty() == true) flushes.trySend(Unit) } }
    }

    /** The world map reads nothing of its own: progress and the atlas are the hero's. */
    fun loadCampaign() { runtime.heroViewModel.ensureHero() }

    fun selectZone(mapCode: String) { runtime.mutable.update { it.copy(play = it.play.copy(launch = MapLaunchState(mapCode))) } }
    fun closeZone() { runtime.mutable.update { it.copy(play = it.play.copy(launch = null)) } }
    /** The stash map to enter with, or null to enter without one. */
    fun pickMap(itemId: String?) { runtime.mutable.update { s -> s.copy(play = s.play.copy(launch = s.play.launch?.copy(picked = itemId))) } }

    /**
     * «В путь»: the zone is entered on the server — with the picked map, spent there, or without one — and the
     * seed and the frozen context come back with the hero; the run is then built here and walked.
     */
    fun start(mapCode: String, auto: AutoPlan? = null) { with(runtime) {
        if (mutableRun.value != null || state.value.busy) return
        // In a lobby the host sets out with everyone (3.25.0); a guest waits for the host
        state.value.play.party.view?.let { lobby -> if (auto == null && lobby.zone == mapCode) { if (lobby.isHost(heroId)) startParty(); return } }
        val s = state.value
        val index = s.index ?: return
        if (index.zone(mapCode) == null || s.progress?.unlocked?.contains(mapCode) != true) return
        val picked = s.play.launch?.takeIf { it.mapCode == mapCode }?.picked
        // An autorun (3.2.0) spends a map of a zone whose guardian has fallen once
        if (auto != null && (picked == null || s.progress?.cleared?.contains(mapCode) != true)) return
        autoPlan = auto
        task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroId
            // A journal the server has not taken yet is not dropped for a new run: its kills are the hero's.
            runJournal?.let { j ->
                flush()
                check(j.settled || runJournal == null) { ui("expedition.unsent") }
                store.clearJournal(id); runJournal = null
            }
            val started = api.campaign.start(id, mapCode, picked)
            if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
            begin(id, started)
        }
    } }

    /** The host sets out with the lobby (3.25.0): the server enters everyone, and the host's run leads the party. */
    private fun startParty() { with(runtime) {
        task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroId
            runJournal?.let { j ->
                flush()
                check(j.settled || runJournal == null) { ui("expedition.unsent") }
                store.clearJournal(id); runJournal = null
            }
            val launch = partyViewModel.start(id)
            if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
            val hero = state.value.hero ?: return@task
            val index = state.value.index ?: return@task
            autoPlan = null
            party = PartyLead(launch.size, index.campaign.party, MateView(hero.id, hero.info.name, hero.heroClass, 0, 0)) { to, message -> partyViewModel.send(message, to) }
            begin(id, launch.start)
        }
    } }

    /** A guest's way in (3.25.0): the host set out, and the guest's own run in the host's zone is built from the host's world. */
    fun follow(id: String, launch: PartyLaunch) { with(runtime) {
        val current = mutableRun.value
        if (current != null && runJournal?.runId == launch.start.id) return
        val index = state.value.index ?: return
        if (current != null) drop()
        party = PartyFollow(launch.size, index.campaign.party) { act -> partyViewModel.send(PartyMessage.Act(act)) }
        hostWorld = campaignOf(launch.start.zone, launch.world)
        early.clear()
        autoPlan = null
        begin(id, launch.start)
        partyViewModel.sendCard()
    } }

    private fun campaignOf(zone: String, world: PartyWorld) = CampaignState(
        chests = world.chests?.let { mapOf(zone to it) }.orEmpty(), bosses = mapOf(zone to world.bossAt),
        crystals = world.crystals?.let { mapOf(zone to it) }.orEmpty(), abyss = world.abyss?.let { mapOf(zone to it) }.orEmpty(),
        vaalZone = world.vaalZone, corruptionOpened = world.corruptionOpened,
    )

    /** What a guest sent the host: their card, or what they do. */
    fun fromGuest(heroId: String, message: PartyMessage) {
        val lead = party as? PartyLead ?: return
        when (message) {
            is PartyMessage.Card -> if (message.card.heroId == heroId) lead.card(message.card)
            is PartyMessage.Act -> listOfNotNull(mutableRun.value).forEach { it.send(RunCommand.Mate(heroId, message.act)) }
            else -> Unit
        }
    }

    /** A guest left the lobby: the host's fights go on without them. */
    fun guestGone(heroId: String) { (party as? PartyLead)?.gone(heroId) }

    /** What the host sent a guest: the world, a fight's pack, an event of the journal — the Vaal zone entered or left on the way. */
    fun fromHost(message: PartyMessage) {
        val guest = party as? PartyFollow ?: return
        when (message) {
            is PartyMessage.Mirror -> { guest.mirror = message; zoneAs(message.world.vaal) }
            is PartyMessage.Setup -> guest.setup = message
            is PartyMessage.Event -> {
                if (!zoneAs(message.event.vaal)) { early += message.event; return }
                mutableRun.value?.send(RunCommand.Follow(message.event))
            }
            else -> Unit
        }
    }

    /**
     * The guest's run follows the host into the Vaal zone and back. In, only once the map's run has taken the portal's
     * event and knows the zone; true when the guest stands where [vaal] says.
     */
    private fun zoneAs(vaal: Boolean): Boolean { with(runtime) {
        val current = mutableRun.value ?: return false
        if (current.vaal == vaal) return true
        if (!vaal) { close(); return true }
        if (current.run.context.vaal == null) return false
        val hero = state.value.hero ?: return false
        val index = state.value.index ?: return false
        val gear = gear() ?: return false
        val journal = runJournal ?: return false
        val inner = ExpeditionRun.start(index, current.run.zone, current.run, journal, gear, hostWorld ?: hero.campaign, System.currentTimeMillis(), hero.info.experience, hero.level,
            vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded, vaal = true, startPools = current.pools,
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, onLoot = { loot(hero.id, it) }, party = party)
        parent = current
        mutableRun.value = inner
        early.forEach { inner.send(RunCommand.Follow(it)) }
        early.clear()
        return true
    } }

    /** The lobby is gone under a guest (3.25.0): what the journal holds goes out, and the run is over. */
    fun partyGone() { with(runtime) {
        if (party !is PartyFollow) { party = null; return }
        scope.launch { flush() }
        mutableRun.value = null
        parent = null
        party = null
        hostWorld = null
        mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
        heroViewModel.ensureHero()
    } }

    private fun begin(id: String, started: RunStart) { with(runtime) {
        val s = state.value
        val index = s.index ?: return
        val hero = s.hero?.takeIf { it.id == id } ?: return
        val zone = index.zone(started.zone) ?: return
        val gear = gear() ?: return
        // Entered again, the run goes on (server 1.1.0): its numbers, its dead and its rewards counted so far.
        val run = Run(index, zone, started.seed, started.context, started.tally.copy())
        val journal = RunJournal(started.id, id, zone.code, applied = started.applied, base = started.applied).also { runJournal = it }
        mutable.update { it.copy(play = it.play.copy(runLoot = emptyList(), launch = null, runPending = 0, runRejected = 0)) }
        mutableRun.value = ExpeditionRun.start(index, zone, run, journal, gear, hostWorld.takeIf { party is PartyFollow } ?: hero.campaign, System.currentTimeMillis(),
            hero.info.experience, hero.level, vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded,
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, onLoot = { loot(id, it) }, killed = started.killed, auto = autoPlan,
            pet = hero.pets.pet(hero.pets.combat), party = party)
        vaalKilled = started.vaalKilled
        persist()
    } }

    /** Vaal orbs at hand, the ones the journal has spent but the server not yet counted taken out. */
    private fun vaalOrbsFree(): Long = (state.value.hero?.bag?.get(Orb.VAAL_ORB.name) ?: 0L) - (runJournal?.pending?.count { it.kind == RunEventKind.CRYSTAL_VAAL } ?: 0)

    /** One event more: the journal is written, and a checkpoint or a full batch sends it. */
    private fun recorded(event: RunEvent) {
        val j = runJournal ?: return
        runtime.mutable.update { it.copy(play = it.play.copy(runPending = j.pending.size)) }
        persist()
        if (event.kind in CHECKPOINTS || j.pending.size >= BATCH) flushes.trySend(Unit) else if (failures == 0) sendIn(QUIET_AFTER)
    }

    /**
     * A send [after] a pause, replacing the one waiting. A plain kill used to wait for a full batch or the
     * clock's 20 seconds, and a failed send for the clock again: the journal sat unsent for half a minute and more.
     */
    private fun sendIn(after: Long) { with(runtime) {
        sendJob?.cancel()
        sendJob = scope.launch { delay(after); flushes.trySend(Unit) }
    } }

    /** A send that did not reach the server: the next one after a doubling pause, the clock's at the longest. */
    private fun retryLater() {
        failures++
        sendIn((RETRY_FIRST shl (failures - 1).coerceAtMost(RETRY_DOUBLINGS)).coerceAtMost(FLUSH_EVERY))
    }

    /** The journal on disk, a moment after the last event so a burst of kills is one write. */
    private fun persist() { with(runtime) {
        val j = runJournal ?: return
        saveJob?.cancel()
        saveJob = scope.launch { delay(SAVE_AFTER); store.saveJournal(j.heroId, j.encode()) }
    } }

    /** Sends what the journal holds; the answer is the server's count, and the hero snapshot rides with it. */
    private suspend fun flush() { with(runtime) {
        val j = runJournal ?: return
        val pending = j.pending
        if (pending.isEmpty()) { if (j.closed) done(j); return }
        try {
            val report = api.campaign.events(j.heroId, pending)
            failures = 0
            j.confirm(report.applied, report.rejected)
            mutable.update { it.copy(play = it.play.copy(runPending = j.pending.size, runRejected = j.rejected.size)) }
            if (report.rejected.isNotEmpty()) toast(ui("expedition.rejected", report.rejected.size))
            report.received.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { toast(ui("stash.received", it.overflowed, it.sold, it.gold)) }
            if (!report.open && j.settled) done(j) else store.saveJournal(j.heroId, j.encode())
            if (state.value.play.heroReadAt == 0L && onScreen(j.heroId)) heroViewModel.readHero()
        } catch (e: CancellationException) { throw e }
        catch (e: ApiFailure) {
            // CP_018: the server holds no run for this hero - the journal's run is over, its last batch already settled;
            // CP_020: the world changed under the run, and the server closed it.
            if (e.code in RUN_CLOSED) done(j) else report(e, writing = true)
        }
        // Offline or timed out: tried again soon, not at the clock's next round
        catch (_: Exception) { retryLater() }
    } }

    private suspend fun done(j: RunJournal) { with(runtime) {
        if (runJournal === j) runJournal = null
        store.clearJournal(j.heroId)
        // The run is counted whole (3.24.0): whatever it finished is handed in at once.
        questViewModel.claimAll(j.heroId)
    } }

    /** Sends the journal now: the app goes to the background, or the run is over. */
    fun flushRun() { failures = 0; flushes.trySend(Unit) }

    /**
     * On entering a hero: a journal kept from an earlier launch is sent if the hero's run is still the one it
     * names — a run since started, or none, means the server closed it, and the journal is dropped.
     */
    suspend fun resume(id: String) { with(runtime) {
        val kept = store.journal(id)?.let(RunJournal::decode) ?: return
        val open = state.value.hero?.takeIf { it.id == id }?.campaign?.run
        if (open?.id != kept.runId || kept.settled) { store.clearJournal(id); return }
        runJournal = kept
        mutable.update { it.copy(play = it.play.copy(runPending = kept.pending.size, runRejected = kept.rejected.size)) }
        flush()
    } }

    private fun stance(): HeroStance { with(runtime) {
        val s = state.value
        val hero = s.hero ?: return HeroStance()
        val weapon = hero.equipped[Slot.WEAPON_1H] ?: hero.equipped[Slot.WEAPON_2H]
        return HeroStance.of(hero.heroClass, weapon?.let { s.index?.template(it.template)?.weaponType })
    } }

    /** The hero as a run takes them: the sheet and what it was added up from, their stance, and what they bring beyond the sheet. */
    private fun gear(): HeroGear? { with(runtime) {
        val s = state.value
        val index = s.index ?: return null
        val hero = s.hero ?: return null
        val conditions = hero.skills.flasks
        val flasks = Slot.FLASKS.mapIndexed { i, slot -> hero.equipped[slot]?.let { item -> index.template(item.template)?.let { Flask.of(item, it, index, conditions.getOrNull(i)) } } }
        return HeroGear(hero.stats, hero.level, hero.sheet.model, stance(), Loadout.of(hero.skills, index.skills, hero.heroClass, flasks, index.powers), index.stats.percent)
    } }

    fun send(command: RunCommand) { mutableRun.value?.send(command) }

    /** «Войти» at the Vaal gate: the portal closes behind the hero and the zone is a run of its own on the same seed and journal. */
    fun enterVaal() { with(runtime) {
        val outer = mutableRun.value ?: return
        val s = state.value
        val index = s.index ?: return
        val hero = s.hero ?: return
        val gear = gear()?.copy(stance = outer.stance) ?: return
        val journal = runJournal ?: return
        if (parent != null || outer.hud.value.gate == null) return
        outer.send(RunCommand.ShutGate(entered = true))
        val inner = ExpeditionRun.start(index, outer.run.zone, outer.run, journal, gear, hero.campaign, System.currentTimeMillis(), hero.info.experience, hero.level,
            vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded, vaal = true, startPools = outer.pools,
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, onLoot = { loot(hero.id, it) }, killed = vaalKilled, auto = autoPlan.takeIf { outer.hud.value.auto != null },
            pet = hero.pets.pet(hero.pets.combat), party = party)
        parent = outer
        mutableRun.value = inner
    } }

    /** «Отказаться» at the Vaal gate: the portal is gone and the zone closed for good. */
    fun refuseVaal() { mutableRun.value?.send(RunCommand.ShutGate(entered = false)) }

    // ==================== The atlas ====================

    fun openAtlas() { with(runtime) {
        mutable.update { s -> s.copy(play = s.play.copy(atlas = s.play.atlas ?: AtlasScreenState(selected = s.atlasState?.allocated?.lastOrNull().orEmpty()))) }
        heroViewModel.ensureHero()
    } }
    fun closeAtlas() { runtime.mutable.update { it.copy(play = it.play.copy(atlas = null)) } }
    fun selectAtlasNode(code: String) { runtime.mutable.update { s -> s.copy(play = s.play.copy(atlas = s.play.atlas?.copy(selected = code))) } }

    /** A command of the atlas: the hero snapshot with its answer carries the new nodes and points. */
    fun allocateAtlas(code: String) = atlasCommand { id -> runtime.api.atlas.allocate(id, code) }
    fun refundAtlas(code: String) = atlasCommand { id -> runtime.api.atlas.refund(id, code) }
    fun resetAtlas() = atlasCommand { id -> runtime.api.atlas.reset(id) }

    private fun atlasCommand(call: suspend (String) -> Unit) { with(runtime) {
        task(writing = true, touches = setOf(Reads.HERO)) {
            call(heroId)
            if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
        }
    } }

    /** The hero was re-read after a change of gear: a run under way takes the new sheet between fights. */
    fun regear() {
        val gear = gear() ?: return
        listOfNotNull(mutableRun.value, parent).forEach { it.send(RunCommand.Regear(gear)) }
        // A guest's gear is fought by the host: the new card goes to the host's next fight
        if (party is PartyFollow) runtime.partyViewModel.sendCard()
    }

    /** A new reading of the hero: nothing to do — the run reads the bag through the state. */
    fun heroChanged(view: HeroView) { }

    /**
     * The run is over or abandoned: the journal goes out, and the hero is read once more if the answers left
     * it cold. A Vaal zone over is not the end: the hero is back on the map by its portal with the life the zone
     * left, or — fallen in it — with [VaalZones.WAKE_LIFE] of it.
     */
    fun close() {
        val outer = parent
        val zone = mutableRun.value
        if (outer != null && zone != null) {
            val share = if (zone.heroLife <= 0) VaalZones.WAKE_LIFE else zone.heroLife / zone.hero.maxLife
            outer.send(RunCommand.Returned(outer.hero.maxLife * share, zone.pools))
            parent = null
            mutableRun.value = outer
            flushes.trySend(Unit)
            return
        }
        mutableRun.value = null
        flushes.trySend(Unit)
        // The run is over: so is its party — the journal goes out first, a guest's run closes with the lobby
        if (party != null) {
            party = null
            hostWorld = null
            early.clear()
            with(runtime) { scope.launch { flush(); partyViewModel.quit(heroId) } }
        }
        runtime.mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
        runtime.heroViewModel.ensureHero()
    }

    private fun loot(heroId: String, equipment: List<ItemInstance>) {
        if (equipment.isEmpty()) return
        val now = System.currentTimeMillis()
        runtime.mutable.update { s -> if (s.play.heroId != heroId) s else s.copy(play = s.play.copy(runLoot = s.play.runLoot + equipment.map { LootEntry(it, now) })) }
    }

    /** Dropped without a word: the hero or the session it belonged to is gone. The journal stays on disk for the next entry. */
    fun drop() { mutableRun.value = null; parent = null; runJournal = null; party = null; hostWorld = null; early.clear() }

    private companion object {
        /** The server's "no run is open" and "the world changed, the run is closed" refusals: the journal has nowhere to go. */
        val RUN_CLOSED = setOf("CP_018", "CP_020")
        /** Events that send the journal at once: everything that is not a plain kill or a chest. */
        val CHECKPOINTS = setOf(RunEventKind.BOSS, RunEventKind.CORRUPT, RunEventKind.CRYSTAL, RunEventKind.CRYSTAL_VAAL, RunEventKind.VAAL_OPEN, RunEventKind.VAAL_LEAVE,
            RunEventKind.ABYSS_OPEN, RunEventKind.ABYSS_CLAIM, RunEventKind.SUMMON, RunEventKind.FALL, RunEventKind.LEAVE)
        const val BATCH = 6
        const val FLUSH_EVERY = 20_000L
        /** A kill's batch goes out this long after the last event, if nothing sends it sooner. */
        const val QUIET_AFTER = 3_000L
        /** The first retry after a failed send, and how many times the pause doubles before the clock's. */
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 3
        const val SAVE_AFTER = 800L
    }
}
