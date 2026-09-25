package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.HeroStance
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.campaign.RolledMonster
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.VaalZones
import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.model.atlas.AtlasState
import com.sperance.exileforge.core.model.atlas.AtlasTree
import com.sperance.exileforge.presentation.state.AtlasScreenState
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.MapLaunchState
import com.sperance.exileforge.presentation.state.LootEntry
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.core.model.campaign.MapServiceOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The campaign: its chapters and progress, and the run on the map a player is walking.
 *
 * The run is not a slice of the state: it is a world the scene steps every frame, and copying it
 * into an immutable state sixty times a second would buy nothing. It is
 * held here and the overlay reads its [ExpeditionRun.hud].
 *
 * What a run reports goes through a lane of its own, one report after another, rather than
 * [ForgeRuntime.task]: a command refuses to start while another runs, and a kill that lands while
 * the previous one is still on its way must wait its turn, not be dropped — it is loot. A death is
 * reported the same way (0.28.0), and none of it is ever retried: a repeated kill would be paid
 * twice, a repeated death charged twice.
 */
class ExpeditionViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {
    private val mutableRun = MutableStateFlow<ExpeditionRun?>(null)
    val run: StateFlow<ExpeditionRun?> = mutableRun.asStateFlow()
    private val reports = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    /** The map run a Vaal zone was entered from (2.65.0): it stands still while the zone is played. */
    private var parent: ExpeditionRun? = null

    init { runtime.scope.launch { for (report in reports) report() } }

    /** The chapters once per session, and this character's progress every time the tab opens. */
    fun loadCampaign() { with(runtime) { read(Reads.CAMPAIGN) {
        val id = state.value.play.characterId
        if (id.isBlank()) return@read
        // The loot panel names orbs and draws items as the stash does, so it needs the same tables.
        ensureWorld()
        if (state.value.world.campaign == null) {
            val view = api.campaign.chapters()
            mutable.update { it.copy(world = it.world.copy(campaign = view)) }
        }
        val progress = api.campaign.progress(id)
        mutable.update { if (it.play.characterId == id) it.copy(play = it.play.copy(campaign = progress)) else it }
    } } }

    /**
     * A location's launch window (since 2.37.0) opens before every run: it shows at once, and its
     * chests and its boss arrive when the server has said. A stash map of the location's own level is
     * picked in it by the player; nothing is picked for them.
     */
    fun openLaunch(mapCode: String) { with(runtime) {
        mutable.update { it.copy(play = it.play.copy(launch = MapLaunchState(mapCode))) }
        read(Reads.MAP_SERVICES, restart = true) {
            val id = state.value.play.characterId
            val chests = api.campaign.chests(id, mapCode)
            val boss = api.campaign.boss(id, mapCode)
            mutable.update { s ->
                val open = s.play.launch?.takeIf { s.play.characterId == id && it.mapCode == mapCode }
                if (open == null) s else s.copy(play = s.play.copy(launch = open.copy(chests = chests, boss = boss)))
            }
        }
    } }

    fun closeLaunch() { runtime.mutable.update { it.copy(play = it.play.copy(launch = null)) } }

    /** The stash map to enter with, or null to enter without one. */
    fun pickMap(instanceId: String?) {
        runtime.mutable.update { s -> s.copy(play = s.play.copy(launch = s.play.launch?.copy(picked = instanceId))) }
    }

    /** One more chest on the map this window, for gold. */
    fun buyTreasure(mapCode: String) = service(mapCode) { id -> runtime.api.campaign.treasure(id, mapCode) }

    /** A slain guardian back at the exit, for gold. */
    fun summonGuardian(mapCode: String) = service(mapCode) { id -> runtime.api.campaign.summon(id, mapCode) }

    private fun service(mapCode: String, call: suspend (String) -> MapServiceOutcome) { with(runtime) {
        task(writing = true, touches = setOf(Reads.MAP_SERVICES)) {
            val id = state.value.play.characterId
            val outcome = call(id)
            mutable.update { s -> s.copy(play = s.play.copy(
                launch = s.play.launch?.takeIf { it.mapCode == mapCode }?.copy(chests = outcome.chests, boss = outcome.boss),
                hero = s.play.hero?.let { it.copy(character = it.character.copy(money = outcome.money)) })) }
        }
    } }

    /** The map «Кампания» leads to: the first open one not yet cleared, or the deepest open one. */
    fun nextMap(): CampaignMap? {
        val s = runtime.state.value
        val maps = s.world.campaign?.chapters?.flatMap { it.maps }.orEmpty()
        val progress = s.play.campaign ?: return null
        return maps.firstOrNull { it.code in progress.unlocked && it.code !in progress.cleared }
            ?: maps.lastOrNull { it.code in progress.unlocked }
    }

    /**
     * «В путь»: the location is entered on the server first (since 0.35.0) — with the picked map,
     * which is spent there, or without one — and then a new run starts with the hero as the sheet has
     * them now and the map's effects on the monsters and on the hero. The map, its monsters and their
     * rolls come from one fresh seed; how many chests stand on it is the entry's answer.
     */
    fun start(mapCode: String) { with(runtime) {
        if (mutableRun.value != null || state.value.busy) return
        val s = state.value
        val view = s.world.campaign ?: return
        val map = view.chapters.flatMap { it.maps }.firstOrNull { it.code == mapCode } ?: return
        if (s.play.campaign?.unlocked?.contains(mapCode) != true) return
        val picked = s.play.launch?.takeIf { it.mapCode == mapCode }?.picked
        task(writing = true, touches = setOf(Reads.MAP_SERVICES)) {
            val characterId = state.value.play.characterId
            val launch = api.campaign.start(characterId, mapCode, picked)
            val hero = state.value.play.hero ?: return@task
            mutable.update { it.copy(play = it.play.copy(launch = null, heroReadAt = if (picked != null) 0 else it.play.heroReadAt,
                hero = if (picked == null) it.play.hero else it.play.hero?.let { h -> h.copy(inventory = h.inventory.filterNot { item -> item.id == picked }) })) }
            begin(map, view, hero, characterId, launch.map?.effects.orEmpty(), launch.chests.left, launch.atlas)
        }
    } }

    private fun begin(map: CampaignMap, view: com.sperance.exileforge.core.model.campaign.CampaignView, hero: com.sperance.exileforge.core.model.hero.HeroView,
                      characterId: String, effects: Map<String, Double>, chests: Int, atlas: Map<String, Double> = emptyMap()) {
        runtime.mutable.update { it.copy(play = it.play.copy(runLoot = emptyList())) }
        lateinit var run: ExpeditionRun
        run = ExpeditionRun.start(map, view.rarities, hero.sheet.stats, hero.sheet.level, System.nanoTime(),
            onKill = { monster -> reports.trySend { kill(run, characterId, map.code, monster) } },
            onCleared = { reports.trySend { complete(characterId, map.code) } },
            rules = view.combat,
            onFallen = { reports.trySend { fall(run, characterId, map.code) } },
            onChest = { reports.trySend { openChest(run, characterId, map.code) } },
            // The atlas's own bonuses (2.68.0) ride the map item's: more and rarer monsters, fountains, the portal.
            mapEffects = AtlasEffects.map(effects, atlas), fountains = AtlasEffects.fountains(view.fountains, atlas),
            portalChance = AtlasEffects.portalChance(view, atlas), onPortal = { reports.trySend { gate(run, characterId, map.code) } },
            stance = stance())
        // How many chests stand on the map is the server's (0.31.0), answered by the entry itself.
        run.send(RunCommand.Chests(chests))
        mutableRun.value = run
        // The boss stands until the server says it was slain within the hour (0.32.0).
        if (map.boss != null) reports.trySend { boss(run, characterId, map.code) }
    }

    /**
     * Whom the hero strikes and how far they reach (2.70.0): the class's rule, and a bow or a wand
     * in the main hand to reach the back row while the front stands.
     */
    private fun stance(): HeroStance = with(runtime) {
        val s = state.value
        val weapon = s.play.hero?.equipped?.let { it["WEAPON_1H"] ?: it["WEAPON_2H"] }
        HeroStance.of(s.heroClass?.code, weapon?.let { s.world.inventoryBases[it.equipmentId]?.text("weaponType") })
    }

    fun send(command: RunCommand) { mutableRun.value?.send(command) }

    /**
     * «Войти» at the Vaal gate (2.65.0): the portal closes behind the hero and the zone is a run of
     * its own — the zone's map, its modifiers laid on it as a map item's are, the hero walking in with
     * the life the map left. The map run waits for them underneath.
     */
    fun enterVaal() { with(runtime) {
        val run = mutableRun.value ?: return
        val zone = run.hud.value.gate ?: return
        val view = state.value.world.campaign ?: return
        val hero = state.value.play.hero ?: return
        val map = VaalZones.map(run.map) ?: return
        if (parent != null) return
        val characterId = state.value.play.characterId
        run.send(RunCommand.ShutGate)
        lateinit var inner: ExpeditionRun
        inner = ExpeditionRun.start(map, view.rarities, hero.sheet.stats, hero.sheet.level, System.nanoTime(),
            onKill = { monster -> reports.trySend { kill(inner, characterId, map.code, monster, vaal = true) } },
            onCleared = {},
            rules = view.combat,
            onFallen = { reports.trySend { vaalLeave(inner, characterId, map.code) } },
            mapEffects = stack(run.mapEffects, zone.effects), startLife = run.heroLife, stance = run.stance)
        parent = run
        mutableRun.value = inner
    } }

    /**
     * The zone's modifiers on top of the map item's (2.65.1): the map still holds inside its Vaal
     * zone, each stat summed, as the server sums both bonuses on the zone's loot.
     */
    private fun stack(map: Map<String, Double>, zone: Map<String, Double>): Map<String, Double> =
        (map.keys + zone.keys).associateWith { (map[it] ?: 0.0) + (zone[it] ?: 0.0) }

    // ==================== The atlas (2.68.0) ====================

    /** Opens the atlas window: the tree once a session, this character's state every time. */
    fun openAtlas() { with(runtime) {
        mutable.update { it.copy(play = it.play.copy(atlas = it.play.atlas ?: AtlasScreenState(tree = atlasTree))) }
        read(Reads.ATLAS, restart = true) {
            val id = state.value.play.characterId
            val tree = atlasTree ?: api.atlas.tree().also { atlasTree = it }
            val atlas = api.atlas.state(id)
            mutable.update { s -> s.play.atlas?.takeIf { s.play.characterId == id }
                ?.let { s.copy(play = s.play.copy(atlas = it.copy(tree = tree, state = atlas, selected = it.selected.ifBlank { atlas.allocated.lastOrNull().orEmpty() }))) } ?: s }
        }
    } }

    fun closeAtlas() { runtime.mutable.update { it.copy(play = it.play.copy(atlas = null)) } }

    fun selectAtlasNode(code: String) { runtime.mutable.update { s -> s.copy(play = s.play.copy(atlas = s.play.atlas?.copy(selected = code))) } }

    fun allocateAtlas(code: String) = atlasCommand { id -> runtime.api.atlas.allocate(id, code) }
    fun refundAtlas(code: String) = atlasCommand { id -> runtime.api.atlas.refund(id, code) }
    fun resetAtlas() = atlasCommand { id -> runtime.api.atlas.reset(id) }

    /** A command of the atlas: its answer is the whole state; the purse follows the hero it brought back. */
    private fun atlasCommand(call: suspend (String) -> AtlasState) { with(runtime) {
        task(writing = true, touches = setOf(Reads.ATLAS)) {
            val id = state.value.play.characterId
            val atlas = call(id)
            mutable.update { s -> s.copy(play = s.play.copy(atlas = s.play.atlas?.copy(state = atlas))) }
        }
    } }

    /** The tree is the server's content: read once a session, not per character. */
    private var atlasTree: AtlasTree? = null

    /** «Отказаться» at the Vaal gate: the portal is gone and the zone closed on the server. */
    fun refuseVaal() {
        val run = mutableRun.value ?: return
        val characterId = runtime.state.value.play.characterId
        run.send(RunCommand.ShutGate)
        reports.trySend { vaalLeave(null, characterId, run.map.code) }
    }

    /** The hero was re-read after a change of gear (2.40.0): a run under way takes the new sheet between fights. */
    fun regear() {
        val hero = runtime.state.value.play.hero ?: return
        val stance = stance()
        listOfNotNull(mutableRun.value, parent).forEach { it.send(RunCommand.Regear(hero.sheet.stats, hero.sheet.level, stance)) }
    }

    /**
     * The run is over or abandoned. Every report on the way brought the hero back with it; the
     * next glance still asks once, which is a 304 unless a report was lost on the way.
     *
     * A Vaal zone (2.65.0) over is not the end: the hero is back on the map by its portal with the
     * life the zone left, or — fallen in it — with [VaalZones.WAKE_LIFE] of it.
     */
    fun close() {
        val outer = parent
        val zone = mutableRun.value
        if (outer != null && zone != null) {
            val share = if (zone.heroLife <= 0) VaalZones.WAKE_LIFE else zone.heroLife / zone.hero.maxLife
            outer.send(RunCommand.Returned(outer.hero.maxLife * share))
            parent = null
            mutableRun.value = outer
            return
        }
        mutableRun.value = null
        runtime.mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
        loadCampaign()
    }

    /** A reward's gear joins the run's «Новый лут» (2.45.0). */
    private fun loot(characterId: String, equipment: List<com.sperance.exileforge.core.model.hero.EquipmentInstance>) {
        if (equipment.isEmpty()) return
        val now = System.currentTimeMillis()
        runtime.mutable.update { s -> if (s.play.characterId != characterId) s
            else s.copy(play = s.play.copy(runLoot = s.play.runLoot + equipment.map { LootEntry(it, now) })) }
    }

    /** Dropped without a word: the character or the session it belonged to is gone. */
    fun drop() { mutableRun.value = null; parent = null }

    private suspend fun kill(run: ExpeditionRun, characterId: String, mapCode: String, monster: RolledMonster, vaal: Boolean = false) { with(runtime) {
        try {
            // The guardians are each reported by their own route (0.57.0, 0.32.0): the Vaal zone's
            // closes the zone and pays its bonus, the map's boss opens the exit.
            val reward = when {
                monster.rarity == MonsterRarity.UNIQUE && vaal -> api.campaign.corrupt(characterId, mapCode, monster.code)
                monster.rarity == MonsterRarity.UNIQUE -> api.campaign.slayBoss(characterId, mapCode)
                else -> api.campaign.kill(characterId, mapCode, monster.code, monster.rarity, vaal)
            }
            run.send(RunCommand.Reward(reward))
            loot(characterId, reward.equipment)
            // The hero, bag, stash and bench came back with the answer (server 0.48.0); an answer
            // without them has already set the reading cold.
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { run.send(RunCommand.RewardFailed); report(e, writing = true) }
    } }

    /** The hero fell: the server prices it, and the header's experience is what it says now. */
    private suspend fun fall(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try {
            val fall = api.campaign.fall(characterId, mapCode)
            run.send(RunCommand.Fallen(fall))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { run.send(RunCommand.FallFailed); report(e, writing = true) }
    } }

    /** The zone behind the portal the hero reached: the gate waits for it, or says it could not be had. */
    private suspend fun gate(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try { run.send(RunCommand.Gate(api.campaign.vaal(characterId, mapCode))) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { run.send(RunCommand.GateFailed); report(e, writing = true) }
    } }

    /** The Vaal zone closed without its guardian: refused ([run] null) or fallen in, which costs no experience. */
    private suspend fun vaalLeave(run: ExpeditionRun?, characterId: String, mapCode: String) { with(runtime) {
        try {
            val fall = api.campaign.vaalLeave(characterId, mapCode)
            run?.send(RunCommand.Fallen(fall))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { run?.send(RunCommand.FallFailed); report(e, writing = true) }
    } }

    /** Whether the map's boss is there: a failure leaves it standing, which is what the exit expects. */
    private suspend fun boss(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try { if (!api.campaign.boss(characterId, mapCode).alive) run.send(RunCommand.BossAbsent) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
    } }

    /** A chest the hero opened: the server rolls it, the purse in the header follows. Never retried. */
    private suspend fun openChest(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try {
            val reward = api.campaign.openChest(characterId, mapCode)
            run.send(RunCommand.ChestReward(reward))
            loot(characterId, reward.equipment)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { run.send(RunCommand.ChestFailed); report(e, writing = true) }
    } }

    private suspend fun complete(characterId: String, mapCode: String) { with(runtime) {
        try {
            val progress = api.campaign.complete(characterId, mapCode)
            mutable.update { if (it.play.characterId == characterId) it.copy(play = it.play.copy(campaign = progress)) else it }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { report(e, writing = true) }
    } }
}

/** How a monster's rarity is written in the client's own dictionary. */
fun MonsterRarity.key() = "enum.monster_rarity.$name"
