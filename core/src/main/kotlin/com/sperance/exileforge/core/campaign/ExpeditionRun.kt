package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.CampaignReward
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a run stands: walking, fighting, waiting on a kill's loot, or over one way or another. */
enum class RunPhase { MAP, FIGHT, LOOT, DEAD, CLEARED, LEFT }

/** A number floating off a fighter, [age] seconds after the swing that made it. */
data class FloatingHit(val id: Int, val target: Side, val kind: HitKind, val amount: Int, val age: Double, val healed: Int)

/** The fight as the overlay prints it: who, how much life each side has, and what just landed. */
data class FightHud(
    val monster: RolledMonster,
    val heroLife: Int, val heroShield: Int,
    val monsterLife: Int, val monsterShield: Int, val monsterMaxLife: Int, val monsterMaxShield: Int,
    val hits: List<FloatingHit>,
    val speed: Int,
    val outcome: Outcome?,
)

/** Everything the overlay draws, as one value: it changes only when something on it does. */
data class RunHud(
    val phase: RunPhase,
    val mapCode: String,
    val heroLife: Int, val heroMaxLife: Int, val heroShield: Int, val heroMaxShield: Int,
    val alive: Int, val total: Int,
    val fight: FightHud? = null,
    val reward: CampaignReward? = null,
    val rewardPending: Boolean = false,
    val rewardFailed: Boolean = false,
    val slain: RolledMonster? = null,
    val gold: Long = 0, val experience: Double = 0.0, val kills: Int = 0,
)

/** What the overlay asks of the run; applied at the start of the next step. */
sealed interface RunCommand {
    data object Continue : RunCommand
    data object Speed : RunCommand
    data object Leave : RunCommand
    data class Reward(val reward: CampaignReward) : RunCommand
    data object RewardFailed : RunCommand
}

/**
 * One run of a campaign map: the world, the hero's life across it and the fights on the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the
 * overlay reads [hud] and sends [RunCommand]s, and the stick writes [stickX]/[stickY] in screen
 * axes. Nothing here talks to the server: a won fight calls [onKill] and the map's exit calls
 * [onCleared], and whoever listens reports them and hands the reward back as a command.
 *
 * The hero's life carries from fight to fight and slowly returns while walking; the shield is
 * whole again after every fight. A lost fight ends the run and keeps everything already looted.
 */
class ExpeditionRun(
    val map: CampaignMap,
    val world: ExpeditionWorld,
    val hero: Combatant,
    private val seed: Long,
    private val onKill: (RolledMonster) -> Unit,
    private val onCleared: () -> Unit,
) {
    @Volatile var stickX = 0.0
    @Volatile var stickY = 0.0
    private val commands = ConcurrentLinkedQueue<RunCommand>()
    private var phase = RunPhase.MAP
    private var life = hero.maxLife
    private var fights = 0
    private var speed = 1
    private var reward: CampaignReward? = null
    private var rewardPending = false
    private var rewardFailed = false
    private var slain: RolledMonster? = null
    private var gold = 0L
    private var experience = 0.0
    private var kills = 0

    /** The fight being played, for the scene: the agent, its log and how far the playback is. */
    var fight: FightPlayback? = null
        private set

    private val state = MutableStateFlow(snapshot())
    val hud: StateFlow<RunHud> = state.asStateFlow()

    fun send(command: RunCommand) { commands.add(command) }

    fun update(dt: Double) {
        while (true) apply(commands.poll() ?: break)
        when (phase) {
            RunPhase.MAP -> walk(dt)
            RunPhase.FIGHT -> play(dt)
            else -> Unit
        }
        state.value = snapshot()
    }

    private fun apply(command: RunCommand) {
        when (command) {
            RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2
            RunCommand.Leave -> if (phase == RunPhase.MAP || phase == RunPhase.DEAD || phase == RunPhase.CLEARED) phase = RunPhase.LEFT
            RunCommand.Continue -> when (phase) {
                RunPhase.LOOT -> if (!rewardPending) { phase = RunPhase.MAP; reward = null; slain = null; rewardFailed = false }
                RunPhase.DEAD, RunPhase.CLEARED -> phase = RunPhase.LEFT
                else -> Unit
            }
            is RunCommand.Reward -> {
                reward = command.reward
                rewardPending = false
                gold += command.reward.gold
                experience += command.reward.experience
            }
            RunCommand.RewardFailed -> { rewardPending = false; rewardFailed = true }
        }
    }

    private fun walk(dt: Double) {
        val (x, y) = ExpeditionWorld.screenToWorld(stickX, stickY)
        life = (life + Combat.walkingRegen(hero) * dt).coerceAtMost(hero.maxLife)
        when (val event = world.step(dt, x, y)) {
            is WorldEvent.Encounter -> {
                val monster = Combatant(event.agent.monster.stats, map.level)
                val log = Combat.fight(hero, monster, life, Random(seed * 31 + fights++))
                fight = FightPlayback(event.agent, monster, log)
                phase = RunPhase.FIGHT
            }
            WorldEvent.Exit -> { phase = RunPhase.CLEARED; onCleared() }
            null -> Unit
        }
    }

    private fun play(dt: Double) {
        val playback = fight ?: return
        playback.clock += dt * speed
        if (playback.clock < playback.log.duration + AFTERMATH) return
        when (playback.log.outcome) {
            Outcome.WIN -> {
                life = playback.log.heroLife
                playback.agent.alive = false
                slain = playback.agent.monster
                kills++
                rewardPending = true
                phase = RunPhase.LOOT
                onKill(playback.agent.monster)
            }
            Outcome.LOSS -> { life = 0.0; phase = RunPhase.DEAD }
            Outcome.RETREAT -> { life = playback.log.heroLife; world.retreatFrom(playback.agent); phase = RunPhase.MAP }
        }
        fight = null
    }

    private fun snapshot(): RunHud {
        val playback = fight
        return RunHud(
            phase = phase, mapCode = map.code,
            heroLife = (playback?.current()?.heroLife ?: life).roundToInt(), heroMaxLife = hero.maxLife.roundToInt(),
            heroShield = (playback?.current()?.heroShield ?: hero.maxShield).roundToInt(), heroMaxShield = hero.maxShield.roundToInt(),
            alive = world.alive, total = world.agents.size,
            fight = playback?.let { fightHud(it) },
            reward = reward, rewardPending = rewardPending, rewardFailed = rewardFailed, slain = slain,
            gold = gold, experience = experience, kills = kills,
        )
    }

    private fun fightHud(playback: FightPlayback): FightHud {
        val current = playback.current()
        val hits = playback.log.events.withIndex()
            .filter { (_, event) -> event.time <= playback.clock && playback.clock - event.time < HIT_LIFETIME }
            .map { (index, event) ->
                FloatingHit(index, if (event.attacker == Side.HERO) Side.MONSTER else Side.HERO, event.kind,
                    event.damage.roundToInt(), playback.clock - event.time, event.healed.roundToInt())
            }
        return FightHud(
            monster = playback.agent.monster,
            heroLife = (current?.heroLife ?: life).roundToInt(), heroShield = (current?.heroShield ?: hero.maxShield).roundToInt(),
            monsterLife = (current?.monsterLife ?: playback.monster.maxLife).roundToInt(),
            monsterShield = (current?.monsterShield ?: playback.monster.maxShield).roundToInt(),
            monsterMaxLife = playback.monster.maxLife.roundToInt(), monsterMaxShield = playback.monster.maxShield.roundToInt(),
            hits = hits, speed = speed,
            outcome = playback.log.outcome.takeIf { playback.clock >= playback.log.duration },
        )
    }

    companion object {
        /** How long the fight's last blow hangs before the scene moves on. */
        const val AFTERMATH = 0.8
        const val HIT_LIFETIME = 1.0

        fun start(map: CampaignMap, rarities: List<CampaignRarity>, heroStats: Map<String, Double>, heroLevel: Int, seed: Long,
                  onKill: (RolledMonster) -> Unit, onCleared: () -> Unit): ExpeditionRun =
            ExpeditionRun(map, ExpeditionWorld.create(map, rarities, heroStats, seed), Combatant(heroStats, heroLevel), seed, onKill, onCleared)
    }
}

/** A fight being played back: [clock] runs from zero to the log's duration and a little past it. */
class FightPlayback(val agent: MonsterAgent, val monster: Combatant, val log: CombatLog) {
    var clock = 0.0

    /** The last swing that has already happened. */
    fun current(): CombatEvent? = log.events.lastOrNull { it.time <= clock }

    /** The swing whose lunge is on screen right now, and how far into it the scene is (0..1). */
    fun lunge(): Pair<CombatEvent, Double>? = log.events.firstOrNull { clock - it.time in -LUNGE..LUNGE }
        ?.let { it to ((clock - it.time + LUNGE) / (2 * LUNGE)).coerceIn(0.0, 1.0) }

    companion object { const val LUNGE = 0.16 }
}
