package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.RolledMonster
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.Reads
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
class ExpeditionViewModel(private val runtime: ForgeRuntime) {
    private val mutableRun = MutableStateFlow<ExpeditionRun?>(null)
    val run: StateFlow<ExpeditionRun?> = mutableRun.asStateFlow()
    private val reports = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init { runtime.scope.launch { for (report in reports) report() } }

    /** The chapters once per session, and this character's progress every time the tab opens. */
    fun loadCampaign() { with(runtime) { read(Reads.CAMPAIGN) {
        val id = state.value.play.characterId
        if (id.isBlank()) return@read
        // The loot panel names orbs and draws items as the stash does, so it needs the same tables.
        ensureOrbs(); ensureEquipment(); ensureDefinitions()
        if (state.value.world.campaign == null) {
            val view = api.campaign.chapters()
            mutable.update { it.copy(world = it.world.copy(campaign = view)) }
        }
        val progress = api.campaign.progress(id)
        mutable.update { if (it.play.characterId == id) it.copy(play = it.play.copy(campaign = progress)) else it }
    } } }

    /** The map «Кампания» leads to: the first open one not yet cleared, or the deepest open one. */
    fun nextMap(): CampaignMap? {
        val s = runtime.state.value
        val maps = s.world.campaign?.chapters?.flatMap { it.maps }.orEmpty()
        val progress = s.play.campaign ?: return null
        return maps.firstOrNull { it.code in progress.unlocked && it.code !in progress.cleared }
            ?: maps.lastOrNull { it.code in progress.unlocked }
    }

    /**
     * A new run of [mapCode] with the hero as the sheet has them now: their life full, their gear
     * as worn. The map, its monsters and their rolls come from one fresh seed.
     */
    fun start(mapCode: String) { with(runtime) {
        if (mutableRun.value != null || state.value.busy) return
        val s = state.value
        val view = s.world.campaign ?: return
        val map = view.chapters.flatMap { it.maps }.firstOrNull { it.code == mapCode } ?: return
        if (s.play.campaign?.unlocked?.contains(mapCode) != true) return
        val hero = s.play.hero ?: return
        val characterId = s.play.characterId
        lateinit var run: ExpeditionRun
        run = ExpeditionRun.start(map, view.rarities, hero.sheet.stats, hero.sheet.level, System.nanoTime(),
            onKill = { monster -> reports.trySend { kill(run, characterId, map.code, monster) } },
            onCleared = { reports.trySend { complete(characterId, map.code) } },
            rules = view.combat,
            onFallen = { reports.trySend { fall(run, characterId, map.code) } },
            onChest = { reports.trySend { openChest(run, characterId, map.code) } })
        mutableRun.value = run
        // How many chests stand on the map is the server's (0.31.0); they appear once it says.
        reports.trySend { chests(run, characterId, map.code) }
        // The boss stands until the server says it was slain within the hour (0.32.0).
        if (map.boss != null) reports.trySend { boss(run, characterId, map.code) }
    } }

    fun send(command: RunCommand) { mutableRun.value?.send(command) }

    /** The run is over or abandoned; the hero changed on the way, so the next glance re-reads them. */
    fun close() {
        mutableRun.value = null
        runtime.mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
        loadCampaign()
    }

    /** Dropped without a word: the character or the session it belonged to is gone. */
    fun drop() { mutableRun.value = null }

    private suspend fun kill(run: ExpeditionRun, characterId: String, mapCode: String, monster: RolledMonster) { with(runtime) {
        try {
            // A boss is reported by its own route (0.32.0): the server opens the exit and rolls its uniques.
            val reward = if (monster.rarity == MonsterRarity.UNIQUE) api.campaign.slayBoss(characterId, mapCode)
                else api.campaign.kill(characterId, mapCode, monster.code, monster.rarity)
            run.send(RunCommand.Reward(reward))
            // The purse, the level and the experience are what the header prints; the bag and the
            // stash are re-read when the hero is next opened.
            mutable.update { s -> if (s.play.characterId != characterId) s else s.copy(play = s.play.copy(heroReadAt = 0,
                hero = s.play.hero?.let { it.copy(character = it.character.copy(level = reward.level, experience = reward.totalExperience, money = reward.money)) })) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { run.send(RunCommand.RewardFailed); report(e, writing = true) }
    } }

    /** The hero fell: the server prices it, and the header's experience is what it says now. */
    private suspend fun fall(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try {
            val fall = api.campaign.fall(characterId, mapCode)
            run.send(RunCommand.Fallen(fall))
            mutable.update { s -> if (s.play.characterId != characterId) s else s.copy(play = s.play.copy(heroReadAt = 0,
                hero = s.play.hero?.let { it.copy(character = it.character.copy(level = fall.level, experience = fall.totalExperience)) })) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { run.send(RunCommand.FallFailed); report(e, writing = true) }
    } }

    /** Whether the map's boss is there: a failure leaves it standing, which is what the exit expects. */
    private suspend fun boss(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try { if (!api.campaign.boss(characterId, mapCode).alive) run.send(RunCommand.BossAbsent) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
    } }

    /** The map's chests: a failure only means none this run, never a banner. */
    private suspend fun chests(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try { run.send(RunCommand.Chests(api.campaign.chests(characterId, mapCode).left)) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
    } }

    /** A chest the hero opened: the server rolls it, the purse in the header follows. Never retried. */
    private suspend fun openChest(run: ExpeditionRun, characterId: String, mapCode: String) { with(runtime) {
        try {
            val reward = api.campaign.openChest(characterId, mapCode)
            run.send(RunCommand.ChestReward(reward))
            mutable.update { s -> if (s.play.characterId != characterId) s else s.copy(play = s.play.copy(heroReadAt = 0,
                hero = s.play.hero?.let { it.copy(character = it.character.copy(level = reward.level, experience = reward.totalExperience, money = reward.money)) })) }
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
