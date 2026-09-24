package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.CampaignFall
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.CampaignReward
import com.sperance.exileforge.core.model.campaign.CombatRules
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a run stands: walking, fighting, waiting on a kill's loot, or over one way or another. */
enum class RunPhase { MAP, FIGHT, LOOT, DEAD, CLEARED, LEFT }

/** A number floating off a fighter, [age] seconds after the blow that made it. */
data class FloatingHit(val id: Int, val target: Side, val action: Action, val kind: HitKind, val amount: Int, val age: Double, val healed: Int,
    val type: DamageType?, val inflicted: List<Ailment>, val stunned: Boolean)

/** The blow on screen right now, for the frames to act out: who, what, whether it landed, and how far along (0..1). */
data class LungeView(val actor: Side, val action: Action, val kind: HitKind, val landed: Boolean, val progress: Float)

/** An ailment on a fighter as the overlay prints it: what, how much of it is left (1 fresh, 0 gone), and how many stacks. */
data class AilmentView(val ailment: Ailment, val left: Float, val stacks: Int)

/**
 * The fight as the overlay prints it: who, how much life, shield and mana each side has, what just
 * landed, how far each side is into its next swing and its next cast (0..1), what is on each of
 * them, the hero's flask, and the blows so far, newest first, for the log under the fighters.
 */
data class FightHud(
    val monster: RolledMonster,
    val heroLife: Int, val heroShield: Int, val heroMana: Int, val heroMaxMana: Int,
    val monsterLife: Int, val monsterShield: Int, val monsterMaxLife: Int, val monsterMaxShield: Int,
    val hits: List<FloatingHit>,
    val speed: Int,
    val outcome: Outcome?,
    val heroSwing: Float = 0f, val monsterSwing: Float = 0f,
    val heroCast: Float = 0f, val monsterCast: Float = 0f,
    val heroCasts: Boolean = false, val monsterCasts: Boolean = false,
    val heroAilments: List<AilmentView> = emptyList(), val monsterAilments: List<AilmentView> = emptyList(),
    val heroHeld: Boolean = false, val monsterHeld: Boolean = false,
    val flasks: Int = 0, val flaskActive: Boolean = false, val retreating: Boolean = false,
    val lunge: LungeView? = null,
    val events: List<CombatEvent> = emptyList(),
)

/**
 * A fight that is over, as the screen after it reads it: the whole log to scroll back through and
 * what it came to — dealt and taken by blows and by ailments, how long, criticals, spells cast,
 * blocks, evasions, what was inflicted, flasks drunk.
 */
data class FightReport(
    val monster: RolledMonster,
    val outcome: Outcome,
    val events: List<CombatEvent>,
    val duration: Double,
) {
    private fun mine(action: Action? = null) = events.filter { it.actor == Side.HERO && (action == null || it.action == action) }
    private fun theirs(action: Action? = null) = events.filter { it.actor == Side.MONSTER && (action == null || it.action == action) }
    val dealt: Int get() = mine().sumOf { it.damage }.roundToInt()
    val taken: Int get() = theirs().sumOf { it.damage }.roundToInt()
    val dotDealt: Int get() = mine(Action.TICK).sumOf { it.damage }.roundToInt()
    val dotTaken: Int get() = theirs(Action.TICK).sumOf { it.damage }.roundToInt()
    val crits: Int get() = mine().count { it.kind == HitKind.CRIT }
    val spells: Int get() = mine(Action.SPELL).count { it.landed }
    val blocked: Int get() = mine().count { it.kind == HitKind.BLOCKED }
    val evaded: Int get() = theirs().count { it.kind == HitKind.EVADED }
    val flasks: Int get() = mine(Action.FLASK).size
    val inflicted: List<Ailment> get() = mine().flatMap { it.inflicted }.distinct()
    val suffered: List<Ailment> get() = theirs().flatMap { it.inflicted }.distinct()
    val retreated: Boolean get() = mine(Action.RETREAT).isNotEmpty()
}

/** Everything the overlay draws, as one value: it changes only when something on it does. */
data class RunHud(
    val phase: RunPhase,
    val mapCode: String,
    val heroLife: Int, val heroMaxLife: Int, val heroShield: Int, val heroMaxShield: Int,
    val heroMana: Int = 0, val heroMaxMana: Int = 0,
    val flasks: Int = 0, val maxFlasks: Int = 0,
    /** A flask being drunk on the map. */
    val flaskActive: Boolean = false,
    val alive: Int, val total: Int,
    val fight: FightHud? = null,
    val reward: CampaignReward? = null,
    val rewardPending: Boolean = false,
    val rewardFailed: Boolean = false,
    val slain: RolledMonster? = null,
    /** The fight just over, kept for the screen after it — a victory's loot or a defeat. */
    val report: FightReport? = null,
    /** What the death cost, once the server has said; pending until it has. */
    val fall: CampaignFall? = null,
    val fallPending: Boolean = false,
    val gold: Long = 0, val experience: Double = 0.0, val kills: Int = 0,
    /** Chests on the map not yet opened (since 2.33.0), and what the last one opened brought. */
    val chestsLeft: Int = 0,
    val chest: CampaignReward? = null,
    val chestPending: Boolean = false,
    val chestFailed: Boolean = false,
)

/** What the overlay asks of the run; applied at the start of the next step. */
sealed interface RunCommand {
    data object Continue : RunCommand
    data object Speed : RunCommand
    data object Leave : RunCommand
    /** Drink a life flask, mid-fight. */
    data object Flask : RunCommand
    /** Walk out of the fight: the monster gets its free swings first. */
    data object Retreat : RunCommand
    data class Reward(val reward: CampaignReward) : RunCommand
    data object RewardFailed : RunCommand
    data class Fallen(val fall: CampaignFall) : RunCommand
    data object FallFailed : RunCommand
    /** The server's count of chests on this map: they are placed once it arrives. */
    data class Chests(val count: Int) : RunCommand
    data class ChestReward(val reward: CampaignReward) : RunCommand
    data object ChestFailed : RunCommand
    /** The chest's loot panel is put away. */
    data object DismissChest : RunCommand
}

/**
 * One run of a campaign map: the world, the hero's life, mana and flask across it and the fights on
 * the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the overlay reads [hud] and
 * sends [RunCommand]s, and the stick writes [stickX]/[stickY] in screen axes. Nothing here talks to
 * the server: a won fight calls [onKill], a lost one [onFallen] and the map's exit [onCleared], and
 * whoever listens reports them and hands the answer back as a command.
 *
 * The hero's life and mana carry from fight to fight and **do not return while walking** (since
 * 2.29.0): life comes back only in a fight — by regeneration, leech or the flask — or from a flask
 * drunk on the map, which heals over the same seconds it does in a fight and is cut short by an
 * encounter. The shield is whole again after every fight. The flask starts the run with the rule's
 * charges and earns one per kill. A lost fight ends the run and keeps everything already looted; a fight the hero walked out
 * of, or that ran out of time, leaves the monster standing and calm for a while.
 */
class ExpeditionRun(
    val map: CampaignMap,
    val world: ExpeditionWorld,
    val hero: Combatant,
    val rules: CombatRules,
    private val seed: Long,
    private val onKill: (RolledMonster) -> Unit,
    private val onCleared: () -> Unit,
    private val onFallen: () -> Unit = {},
    private val onChest: () -> Unit = {},
) {
    @Volatile var stickX = 0.0
    @Volatile var stickY = 0.0
    private val commands = ConcurrentLinkedQueue<RunCommand>()
    private var phase = RunPhase.MAP
    private var life = hero.maxLife
    private var mana = hero.maxMana
    private var flasks = rules.flask.charges
    private var fights = 0
    private var speed = 1
    private var reward: CampaignReward? = null
    private var rewardPending = false
    private var rewardFailed = false
    private var slain: RolledMonster? = null
    private var report: FightReport? = null
    private var fall: CampaignFall? = null
    private var fallPending = false
    private var gold = 0L
    private var experience = 0.0
    private var kills = 0
    private var chest: CampaignReward? = null
    private var chestPending = false
    private var chestFailed = false
    private var fightAgent: MonsterAgent? = null
    /** The map's own clock, and the flask drunk on it: until when it heals, and how fast. */
    private var clock = 0.0
    private var flaskUntil = 0.0
    private var flaskRate = 0.0

    /** The fight being played, for the scene: the battle itself, alive, with its clock and its log. */
    var fight: Battle? = null
        private set
    /** Whose fight it is on the map. */
    val fightAgentOnMap: MonsterAgent? get() = fightAgent

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
            RunCommand.Flask -> fight?.useFlask() ?: drinkOnMap()
            RunCommand.Retreat -> fight?.retreat()
            RunCommand.Continue -> when (phase) {
                RunPhase.LOOT -> if (!rewardPending) { phase = RunPhase.MAP; reward = null; slain = null; report = null; rewardFailed = false }
                RunPhase.DEAD -> if (!fallPending) phase = RunPhase.LEFT
                RunPhase.CLEARED -> phase = RunPhase.LEFT
                else -> Unit
            }
            is RunCommand.Reward -> {
                reward = command.reward
                rewardPending = false
                gold += command.reward.gold
                experience += command.reward.experience
            }
            RunCommand.RewardFailed -> { rewardPending = false; rewardFailed = true }
            is RunCommand.Fallen -> { fall = command.fall; fallPending = false }
            RunCommand.FallFailed -> fallPending = false
            is RunCommand.Chests -> world.placeChests(command.count)
            is RunCommand.ChestReward -> {
                chest = command.reward
                chestPending = false
                gold += command.reward.gold
            }
            RunCommand.ChestFailed -> { chestPending = false; chestFailed = true }
            RunCommand.DismissChest -> if (!chestPending) { chest = null; chestFailed = false }
        }
    }

    /** A flask on the map: the same charge, the same heal over the same seconds, one at a time. */
    private fun drinkOnMap(): Boolean {
        if (phase != RunPhase.MAP || flasks <= 0 || flaskUntil > clock) return false
        flasks--
        flaskUntil = clock + rules.flask.duration
        flaskRate = hero.maxLife * rules.flask.heal / 100 / rules.flask.duration
        return true
    }

    private fun walk(dt: Double) {
        val (x, y) = ExpeditionWorld.screenToWorld(stickX, stickY)
        clock += dt
        // Nothing returns on its own between fights; only a flask heals here.
        if (flaskUntil > clock) life = (life + flaskRate * dt).coerceAtMost(hero.maxLife)
        when (val event = world.step(dt, x, y)) {
            is WorldEvent.Encounter -> {
                flaskUntil = 0.0
                val monster = Combatant(event.agent.monster.stats, map.level, rules)
                fightAgent = event.agent
                fight = Battle(hero, monster, rules, life, mana, flasks, Random(seed * 31 + fights++))
                phase = RunPhase.FIGHT
            }
            WorldEvent.Exit -> { phase = RunPhase.CLEARED; onCleared() }
            is WorldEvent.Opened -> { chest = null; chestFailed = false; chestPending = true; onChest() }
            null -> Unit
        }
    }

    private fun play(dt: Double) {
        val battle = fight ?: return
        val agent = fightAgent ?: return
        battle.advance(dt * speed)
        val outcome = battle.outcome ?: return
        if (battle.time < battle.duration + AFTERMATH) return
        report = FightReport(agent.monster, outcome, battle.events, battle.duration)
        life = battle.heroLife
        mana = battle.heroMana
        flasks = battle.flasks
        when (outcome) {
            Outcome.WIN -> {
                agent.alive = false
                slain = agent.monster
                kills++
                flasks = min(rules.flask.charges, flasks + rules.flask.perKill)
                rewardPending = true
                phase = RunPhase.LOOT
                onKill(agent.monster)
            }
            Outcome.LOSS -> { life = 0.0; fallPending = true; phase = RunPhase.DEAD; onFallen() }
            Outcome.RETREAT -> { world.retreatFrom(agent); report = null; phase = RunPhase.MAP }
        }
        fight = null
        fightAgent = null
    }

    private fun snapshot(): RunHud {
        val battle = fight
        return RunHud(
            phase = phase, mapCode = map.code,
            heroLife = (battle?.heroLife ?: life).roundToInt(), heroMaxLife = hero.maxLife.roundToInt(),
            heroShield = (battle?.fighter(Side.HERO)?.shield ?: hero.maxShield).roundToInt(), heroMaxShield = hero.maxShield.roundToInt(),
            heroMana = (battle?.heroMana ?: mana).roundToInt(), heroMaxMana = hero.maxMana.roundToInt(),
            flasks = battle?.flasks ?: flasks, maxFlasks = rules.flask.charges, flaskActive = battle?.flaskActive ?: (flaskUntil > clock),
            alive = world.alive, total = world.agents.size,
            fight = battle?.let { b -> fightAgent?.let { fightHud(b, it.monster) } },
            reward = reward, rewardPending = rewardPending, rewardFailed = rewardFailed, slain = slain, report = report,
            fall = fall, fallPending = fallPending,
            gold = gold, experience = experience, kills = kills,
            chestsLeft = world.chests.count { !it.opened }, chest = chest, chestPending = chestPending, chestFailed = chestFailed,
        )
    }

    private fun fightHud(battle: Battle, monster: RolledMonster): FightHud {
        val h = battle.fighter(Side.HERO)
        val m = battle.fighter(Side.MONSTER)
        val hits = battle.events.withIndex()
            .filter { (_, event) -> event.time <= battle.time && battle.time - event.time < HIT_LIFETIME && event.action != Action.RETREAT }
            .map { (index, event) ->
                FloatingHit(index, event.target, event.action, event.kind, event.damage.roundToInt(), battle.time - event.time, event.healed.roundToInt(),
                    event.type, event.inflicted, event.stunned)
            }
        fun ailments(f: Battle.Fighter) = f.ailments.groupBy { it.ailment }.map { (ailment, active) ->
            AilmentView(ailment, ((active.maxOf { it.until } - battle.time) / active.first().duration).toFloat().coerceIn(0f, 1f), active.size)
        }
        return FightHud(
            monster = monster,
            heroLife = h.life.roundToInt(), heroShield = h.shield.roundToInt(), heroMana = h.mana.roundToInt(), heroMaxMana = hero.maxMana.roundToInt(),
            monsterLife = m.life.roundToInt(), monsterShield = m.shield.roundToInt(),
            monsterMaxLife = battle.monster.maxLife.roundToInt(), monsterMaxShield = battle.monster.maxShield.roundToInt(),
            hits = hits, speed = speed,
            outcome = battle.outcome,
            heroSwing = battle.swing(Side.HERO), monsterSwing = battle.swing(Side.MONSTER),
            heroCast = battle.cast(Side.HERO), monsterCast = battle.cast(Side.MONSTER),
            heroCasts = hero.casts, monsterCasts = battle.monster.casts,
            heroAilments = ailments(h), monsterAilments = ailments(m),
            heroHeld = h.held, monsterHeld = m.held,
            flasks = battle.flasks, flaskActive = battle.flaskActive, retreating = battle.retreating,
            lunge = battle.lunge()?.let { (event, progress) -> LungeView(event.actor, event.action, event.kind, event.landed, progress.toFloat()) },
            events = battle.events.toList().asReversed(),
        )
    }

    companion object {
        /** How long the fight's last blow hangs before the scene moves on. */
        const val AFTERMATH = 0.8
        const val HIT_LIFETIME = 1.0

        fun start(map: CampaignMap, rarities: List<CampaignRarity>, heroStats: Map<String, Double>, heroLevel: Int, seed: Long,
                  onKill: (RolledMonster) -> Unit, onCleared: () -> Unit, rules: CombatRules = CombatRules(), onFallen: () -> Unit = {},
                  onChest: () -> Unit = {}): ExpeditionRun =
            ExpeditionRun(map, ExpeditionWorld.create(map, rarities, heroStats, seed), Combatant(heroStats, heroLevel, rules, innateSpell = true), rules, seed,
                onKill, onCleared, onFallen, onChest)
    }
}
