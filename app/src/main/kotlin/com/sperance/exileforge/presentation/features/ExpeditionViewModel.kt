package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.campaign.AutoPlan
import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.StageCarry
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.RunReport
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AtlasScreenState
import com.sperance.exileforge.presentation.state.LootEntry
import com.sperance.exileforge.presentation.state.MapLaunchState
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Pet
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
import java.util.UUID

/**
 * The campaign: the world map and the run on the map a player is walking.
 *
 * The run is a world the scene steps every frame; it is held here and the overlay reads its [ExpeditionRun.hud].
 * Every kill, chest and descent is an event of the run's journal; the journal is kept on disk and sent to the
 * server in batches — at checkpoints, at the exit, at a death, in the background — and the server's replay is
 * the truth. Rewards are the server's alone (1.30.0): each answer names what every event brought, and the run
 * shows it as it arrives — offline, it waits for the connection. A batch keeps its idempotency key until it is
 * answered, so a lost answer is asked for again and repeated, rewards and all. A journal not sent is sent on
 * the next launch.
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

    /** A loot-bearing event's send, [PROMPT_AFTER] after the first of a burst; later events join it, nothing postpones it. */
    private var promptJob: Job? = null

    /** Sends failed in a row: each waits twice as long as the last before trying again. */
    private var failures = 0

    /** The autorun the run under way was started with; its Vaal zone runs by itself too. */
    private var autoPlan: AutoPlan? = null

    /** The Vaal zone's tokens the server already counted when the run was entered again. */
    private var vaalKilled: List<Int> = emptyList()

    init {
        runtime.scope.launch { for (signal in flushes) flush() }
        // The clock's flush: what a quiet stretch of the map leaves pending goes out on its own.
        runtime.scope.launch {
            while (true) {
                delay(FLUSH_EVERY)
                if (runJournal?.pending?.isNotEmpty() == true) flushes.trySend(Unit)
            }
        }
    }

    /** The world map reads nothing of its own: progress and the atlas are the hero's. */
    fun loadCampaign() {
        runtime.heroViewModel.ensureHero()
    }

    fun selectZone(mapCode: String) {
        runtime.mutable.update { it.copy(play = it.play.copy(launch = MapLaunchState(mapCode))) }
    }
    fun closeZone() {
        runtime.mutable.update { it.copy(play = it.play.copy(launch = null)) }
    }

    /** The stash map to enter with, or null to enter without one. */
    fun pickMap(itemId: String?) {
        runtime.mutable.update { s ->
            s.copy(
                play = s.play.copy(
                    launch = s.play.launch?.copy(
                        picked = itemId,
                        scarabs = if (itemId == null) emptyList() else s.play.launch.scarabs,
                    ),
                ),
            )
        }
    }

    /** The potion for the run (3.79.0): one, tapped again to take it back. */
    fun pickPotion(code: String?) {
        runtime.mutable.update { s -> s.copy(play = s.play.copy(launch = s.play.launch?.let { it.copy(potion = code.takeIf { c -> c != it.potion }) })) }
    }

    /** A scarab set with the map or taken off (3.79.0): up to the rules' count, no more of a kind than the bag holds. */
    fun toggleScarab(code: String, add: Boolean) {
        runtime.mutable.update { s ->
            val launch = s.play.launch ?: return@update s
            val max = s.index?.rules?.brews?.scarabsPerMap ?: 0
            val scarabs = when {
                !add -> launch.scarabs - code
                launch.picked == null || launch.scarabs.size >= max || launch.scarabs.count { it == code } >= (s.bagAmount(code) ?: 0L) -> return@update s
                else -> launch.scarabs + code
            }
            s.copy(play = s.play.copy(launch = launch.copy(scarabs = scarabs)))
        }
    }

    /**
     * «В путь»: the zone is entered on the server — with the picked map, spent there, or without one — and the
     * seed and the frozen context come back with the hero; the run is then built here and walked.
     */
    fun start(mapCode: String, auto: AutoPlan? = null) {
        with(runtime) {
            if (mutableRun.value != null || state.value.busy) return
            val s = state.value
            val index = s.index ?: return
            if (index.zone(mapCode) == null || s.progress?.unlocked?.contains(mapCode) != true) return
            val launch = s.play.launch?.takeIf { it.mapCode == mapCode }
            val picked = launch?.picked
            // An autorun (3.2.0) spends a map of a zone whose guardian has fallen once
            if (auto != null && (picked == null || s.progress?.cleared?.contains(mapCode) != true)) return
            autoPlan = auto
            task(writing = true, touches = setOf(Reads.HERO)) {
                val id = heroId
                // The stage carry of a fight cut short by a restart goes on in the same run entered again.
                val kept = runJournal?.let { it.runId to it.carry }
                // A journal the server has not taken yet is not dropped for a new run: its kills are the hero's.
                runJournal?.let { j ->
                    flush()
                    check(j.settled || runJournal == null) { ui("expedition.unsent") }
                    store.clearJournal(id)
                    runJournal = null
                }
                val started = try {
                    api.campaign.start(id, mapCode, picked, launch?.potion, launch?.scarabs.orEmpty().takeIf { picked != null }.orEmpty())
                } catch (e: ApiFailure) {
                    // A new seed comes no sooner than the rules allow, whatever closed the last run (server 1.30.0): a wait, not a failure.
                    if (e.code != SEED_TOO_SOON) throw e
                    toast(ui("expedition.seed_wait", e.args.firstOrNull().orEmpty()))
                    return@task
                }
                if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
                heroViewModel.drawn()
                begin(id, started, kept?.takeIf { it.first == started.id }?.second)
            }
        }
    }

    private fun begin(id: String, started: RunStart, carry: StageCarry? = null) {
        with(runtime) {
            val s = state.value
            val index = s.index ?: return
            val hero = s.hero?.takeIf { it.id == id } ?: return
            val zone = index.zone(started.zone) ?: return
            val gear = gear() ?: return
            // Entered again, the run goes on (server 1.1.0): its numbers and its dead so far.
            val run = Run(index, zone, started.seed, started.context)
            val journal = RunJournal(started.id, id, zone.code, applied = started.applied, base = started.applied, carry = carry).also { runJournal = it }
            journal.onCarry = ::persist
            mutable.update { it.copy(play = it.play.copy(runLoot = emptyList(), launch = null, runPending = 0, runRejected = 0)) }
            mutableRun.value = ExpeditionRun.start(
                index, zone, run, journal, gear, hero.campaign, System.currentTimeMillis(), hero.info.experience, hero.level,
                vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded,
                onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, killed = started.killed, auto = autoPlan,
                pet = ::combatPet,
            ).also { r -> repeat(speedSteps) { r.send(RunCommand.Speed) } }
            vaalKilled = started.vaalKilled
            persist()
        }
    }

    /** The hero's combat pet at work now (3.70.0): a run reads it as each fight begins, so «В дело» mid-run counts from the next fight. */
    private fun combatPet(): Pet? = runtime.state.value.hero?.let { it.pets.pet(it.pets.combat) }

    /** Vaal orbs at hand, the ones the journal has spent but the server not yet counted taken out. */
    private fun vaalOrbsFree(): Long = (state.value.hero?.bag?.get(Orb.VAAL_ORB.name) ?: 0L) - (runJournal?.pending?.count { it.kind == RunEventKind.CRYSTAL_VAAL } ?: 0)

    /**
     * One event more: the journal is written, and a checkpoint or a full batch sends it. A kill or a chest of a
     * fight played by hand goes out at once: its loot is on the way while the fight still plays, and the
     * report no longer opens on a batch that only then leaves. An autorun's waits for the quiet stretch — nobody
     * waits on its report, and the server is not asked once a kill. A failing link keeps to the retry's pause.
     */
    private fun recorded(event: RunEvent) {
        val j = runJournal ?: return
        runtime.mutable.update { it.copy(play = it.play.copy(runPending = j.pending.size)) }
        persist()
        when {
            event.kind in CHECKPOINTS || j.pending.size >= BATCH -> flushes.trySend(Unit)
            failures > 0 -> Unit
            event.kind in LOOT && mutableRun.value?.hud?.value?.auto == null -> sendSoon()
            else -> sendIn(QUIET_AFTER)
        }
    }

    /** A send [PROMPT_AFTER] from now, unless one is already due sooner: the foes a single blow fells leave as one batch. */
    private fun sendSoon() {
        with(runtime) {
            if (promptJob?.isActive == true) return
            promptJob = scope.launch {
                delay(PROMPT_AFTER)
                flushes.trySend(Unit)
            }
        }
    }

    /**
     * A send [after] a pause, replacing the one waiting. A plain kill used to wait for a full batch or the
     * clock's 20 seconds, and a failed send for the clock again: the journal sat unsent for half a minute and more.
     */
    private fun sendIn(after: Long) {
        with(runtime) {
            sendJob?.cancel()
            sendJob = scope.launch {
                delay(after)
                flushes.trySend(Unit)
            }
        }
    }

    /** A send that did not reach the server: the next one after a doubling pause, the clock's at the longest. */
    private fun retryLater() {
        failures++
        sendIn((RETRY_FIRST shl (failures - 1).coerceAtMost(RETRY_DOUBLINGS)).coerceAtMost(FLUSH_EVERY))
    }

    /** The journal on disk, a moment after the last event so a burst of kills is one write. */
    private fun persist() {
        with(runtime) {
            val j = runJournal ?: return
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(SAVE_AFTER)
                store.saveJournal(j.heroId, j.encode())
            }
        }
    }

    /**
     * Sends what the journal holds — the batch in flight again under its own key, or the pending events under a
     * new one; the answer is the server's count and every event's reward, and the hero snapshot rides with it.
     */
    private suspend fun flush() {
        with(runtime) {
            val j = runJournal ?: return
            val outgoing = j.outgoing { UUID.randomUUID().toString() }
            if (outgoing == null) {
                if (j.closed) done(j)
                return
            }
            val (batch, pending) = outgoing
            try {
                // The batch's key is on disk before it leaves: an answer lost with the process is asked for again after it.
                store.saveJournal(j.heroId, j.encode())
                val report = api.campaign.events(j.heroId, j.runId, pending, batch.key)
                failures = 0
                if (report == null) {
                    landed(j, batch.end)
                    return
                }
                settle(j, pending, report)
                j.confirm(report.applied, report.rejected)
                mutable.update { it.copy(play = it.play.copy(runPending = j.pending.size, runRejected = j.rejected.size)) }
                if (report.rejected.isNotEmpty()) toast(ui("expedition.rejected", report.rejected.size))
                report.received.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { toast(ui("stash.received", it.overflowed, it.sold, it.gold)) }
                if (!report.open && j.settled) done(j) else store.saveJournal(j.heroId, j.encode())
                if (state.value.play.heroReadAt == 0L && onScreen(j.heroId)) heroViewModel.readHero()
                // A journal longer than one batch goes on at once, part by part
                if (report.open && !j.settled) flushes.trySend(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiFailure) {
                // CP_018: the server holds no run for this hero - the journal's run is over, its last batch already settled;
                // CP_020: the world changed under the run, and the server closed it; CP_026: the journal is of an older run.
                if (e.code in RUN_CLOSED) done(j) else report(e, writing = true)
                // A refusal is the server's final word, kept under the batch's key: the next send is a new request, not its repeat.
                if (!CommandQueue.transient(e.status)) j.release()
            }
            // Offline or timed out: tried again soon, not at the clock's next round
            catch (_: Exception) {
                retryLater()
            }
        }
    }

    /**
     * A batch answered as landed without its report (a repeat whose report the server did not keep): it is taken up to
     * [end], its loot comes with the hero read again, and the runs under way stop waiting for it.
     */
    private suspend fun landed(j: RunJournal, end: Int) {
        with(runtime) {
            runs().forEach { it.send(RunCommand.Settled(end)) }
            j.confirm(end)
            mutable.update { it.copy(play = it.play.copy(runPending = j.pending.size, runRejected = j.rejected.size)) }
            if (j.closed && j.settled) done(j) else store.saveJournal(j.heroId, j.encode())
            heroViewModel.readHero()
            campaign()
            if (!j.settled) flushes.trySend(Unit)
        }
    }

    /**
     * The answer lands on the runs under way — the rewards by event, then the campaign it brought, where the Vaal
     * zone and a crystal's Vaal orb are read — and its gear goes to the gear sheet's «Новый лут».
     */
    private fun settle(j: RunJournal, batch: List<RunEvent>, report: RunReport) {
        val fell = batch.any { it.kind == RunEventKind.FALL && it.n < report.applied && it.n !in report.rejected }
        val crystals = report.rewards.mapNotNull { event -> event.crystal?.let { event.n to it.crystal } }.toMap()
        val answer = RunCommand.Settled(report.applied, report.rewards.associate { it.n to it.reward.toReward() }, report.rejected, report.lost.takeIf { fell }, crystals)
        runs().forEach { it.send(answer) }
        campaign()
        loot(j.heroId, report.rewards.flatMap { it.reward.equipment })
    }

    /** The runs a journal answer concerns: the one on screen and the map a Vaal zone was entered from. */
    private fun runs(): List<ExpeditionRun> = listOfNotNull(mutableRun.value, parent)

    /** The hero's campaign as the client holds it now, to the runs under way. */
    private fun campaign() {
        val state = runtime.state.value.hero?.takeIf { it.id == runJournal?.heroId }?.campaign ?: return
        runs().forEach { it.send(RunCommand.Campaign(state)) }
    }

    private suspend fun done(j: RunJournal) {
        with(runtime) {
            // Whatever the server never answered will not be answered now: nothing waits for it any longer.
            if (runJournal === j) runs().forEach { it.send(RunCommand.Settled(Int.MAX_VALUE)) }
            if (runJournal === j) runJournal = null
            store.clearJournal(j.heroId)
            // The run is counted whole (3.24.0): whatever it finished is handed in at once.
            quests.claimAll(j.heroId)
        }
    }

    /** Sends the journal now: the app goes to the background, or the run is over. */
    fun flushRun() {
        failures = 0
        flushes.trySend(Unit)
    }

    /**
     * On entering a hero: a journal kept from an earlier launch is sent if the hero's run is still the one it
     * names — a run since started, or none, means the server closed it, and the journal is dropped.
     */
    suspend fun resume(id: String) {
        with(runtime) {
            val kept = store.journal(id)?.let(RunJournal::decode) ?: return
            val open = state.value.hero?.takeIf { it.id == id }?.campaign?.run
            // A settled journal still holding a stage carry stays: the run entered again takes it up.
            if (open?.id != kept.runId || kept.settled && kept.carry == null) {
                store.clearJournal(id)
                return
            }
            runJournal = kept
            kept.onCarry = ::persist
            mutable.update { it.copy(play = it.play.copy(runPending = kept.pending.size, runRejected = kept.rejected.size)) }
            flush()
        }
    }

    private fun stance(): HeroStance {
        with(runtime) {
            val s = state.value
            val hero = s.hero ?: return HeroStance()
            return HeroStance.of(hero.heroClass)
        }
    }

    /** The hero as a run takes them: the sheet and what it was added up from, their stance, and what they bring beyond the sheet. */
    internal fun gear(): HeroGear? {
        with(runtime) {
            val s = state.value
            val index = s.index ?: return null
            val hero = s.hero ?: return null
            val conditions = hero.skills.flasks
            val flasks = Slot.FLASKS.mapIndexed { i, slot -> hero.equipped[slot]?.let { item -> index.template(item.template)?.let { Flask.of(item, it, index, conditions.getOrNull(i)) } } }
            return HeroGear(hero.stats, hero.level, hero.sheet.model, stance(), Loadout.of(hero.skills, index.skills, hero.heroClass, flasks, index.powers, index.rules.charges), index.stats.percent)
        }
    }

    fun send(command: RunCommand) {
        mutableRun.value?.send(command)
    }

    /** «Войти» at the Vaal gate: the portal closes behind the hero and the zone is a run of its own on the same seed and journal. */
    fun enterVaal() {
        with(runtime) {
            val outer = mutableRun.value ?: return
            val s = state.value
            val index = s.index ?: return
            val hero = s.hero ?: return
            val gear = gear()?.copy(stance = outer.stance) ?: return
            val journal = runJournal ?: return
            if (parent != null || outer.hud.value.gate == null) return
            outer.send(RunCommand.ShutGate(entered = true))
            val inner = ExpeditionRun.start(
                index, outer.run.zone, outer.run, journal, gear, hero.campaign, System.currentTimeMillis(), hero.info.experience, hero.level,
                vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded, vaal = true, startPools = outer.pools,
                onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, killed = vaalKilled, auto = autoPlan.takeIf { outer.hud.value.auto != null },
                pet = ::combatPet,
            ).also { r -> repeat(speedSteps) { r.send(RunCommand.Speed) } }
            parent = outer
            mutableRun.value = inner
        }
    }

    /** «Отказаться» at the Vaal gate: the portal is gone and the zone closed for good. */
    fun refuseVaal() {
        mutableRun.value?.send(RunCommand.ShutGate(entered = false))
    }

    // ==================== The atlas ====================

    fun openAtlas() {
        with(runtime) {
            mutable.update { s -> s.copy(play = s.play.copy(atlas = s.play.atlas ?: AtlasScreenState(selected = s.atlasState?.allocated?.lastOrNull().orEmpty()))) }
            heroViewModel.ensureHero()
        }
    }
    fun closeAtlas() {
        runtime.mutable.update { it.copy(play = it.play.copy(atlas = null)) }
    }
    fun selectAtlasNode(code: String) {
        runtime.mutable.update { s -> s.copy(play = s.play.copy(atlas = s.play.atlas?.copy(selected = code))) }
    }

    /** A command of the atlas: the hero snapshot with its answer carries the new nodes and points. */
    fun allocateAtlas(code: String) = atlasCommand { id -> runtime.api.atlas.allocate(id, code) }

    /** Gold or, with [regret] (server 1.65.0), an Orb of Regret per node. */
    fun refundAtlas(code: String, regret: Boolean = false) = atlasCommand { id -> runtime.api.atlas.refund(id, code, regret) }
    fun resetAtlas(regret: Boolean = false) = atlasCommand { id -> runtime.api.atlas.reset(id, regret) }

    private fun atlasCommand(call: suspend (String) -> Unit) {
        with(runtime) {
            task(writing = true, touches = setOf(Reads.HERO)) {
                call(heroId)
                if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
            }
        }
    }

    /** The hero was re-read after a change of gear: a run under way takes the new sheet between fights. */
    fun regear() {
        val gear = gear() ?: return
        listOfNotNull(mutableRun.value, parent).forEach { it.send(RunCommand.Regear(gear)) }
    }

    /** A new reading of the hero: the run reads the bag through the state, and takes the campaign — a Vaal zone or a crystal the server settled. */
    fun heroChanged(view: HeroView) {
        if (runJournal?.heroId == view.id) runs().forEach { it.send(RunCommand.Campaign(view.campaign)) }
    }

    /**
     * The run is over or abandoned: the journal goes out, and the hero is read once more if the answers left
     * it cold. A Vaal zone won is not the end: the hero is back on the map by its portal with the life the zone
     * left. Fallen in it (3.71.0), the hero dies as anywhere: the whole run is over.
     */
    fun close() {
        val outer = parent
        val zone = mutableRun.value
        if (outer != null && zone != null && zone.heroLife > 0) {
            outer.send(RunCommand.Returned(outer.hero.maxLife * zone.heroLife / zone.hero.maxLife, zone.pools, zone.share()))
            parent = null
            mutableRun.value = outer
            flushes.trySend(Unit)
            return
        }
        parent = null
        mutableRun.value = null
        flushes.trySend(Unit)
        runtime.mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
        runtime.heroViewModel.ensureHero()
    }

    private fun loot(heroId: String, equipment: List<ItemInstance>) {
        if (equipment.isEmpty()) return
        val now = System.currentTimeMillis()
        runtime.mutable.update { s -> if (s.play.heroId != heroId) s else s.copy(play = s.play.copy(runLoot = s.play.runLoot + equipment.map { LootEntry(it, now) })) }
    }

    /** Dropped without a word: the hero or the session it belonged to is gone. The journal stays on disk for the next entry. */
    fun drop() {
        mutableRun.value = null
        parent = null
        runJournal = null
    }

    private companion object {
        /** The server's "no run is open" and "the world changed, the run is closed" refusals: the journal has nowhere to go. */
        val RUN_CLOSED = setOf("CP_018", "CP_020", "CP_026")

        /** The server's "a new seed only after a while" refusal (CP_021). */
        const val SEED_TOO_SOON = "CP_021"

        /** Events that send the journal at once: everything that is not a plain kill or a chest. */
        val CHECKPOINTS = setOf(
            RunEventKind.BOSS, RunEventKind.CORRUPT, RunEventKind.CRYSTAL, RunEventKind.CRYSTAL_VAAL, RunEventKind.VAAL_OPEN, RunEventKind.VAAL_LEAVE,
            RunEventKind.ABYSS_OPEN, RunEventKind.ABYSS_CLAIM, RunEventKind.SUMMON, RunEventKind.FALL, RunEventKind.LEAVE,
        )
        const val BATCH = 6
        const val FLUSH_EVERY = 20_000L

        /** A kill's batch goes out this long after the last event, if nothing sends it sooner. */
        const val QUIET_AFTER = 3_000L

        /** Events that bring loot and are sent the moment they happen in a fight played by hand. */
        val LOOT = setOf(RunEventKind.KILL, RunEventKind.CHEST)

        /** How long a loot event waits for the rest of its burst: one frame's kills, one request. */
        const val PROMPT_AFTER = 120L

        /** The first retry after a failed send, and how many times the pause doubles before the clock's. */
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 3
        const val SAVE_AFTER = 800L
    }
}
