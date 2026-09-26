package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.AbyssDepth
import com.sperance.exileforge.core.model.campaign.AbyssLaunch
import com.sperance.exileforge.core.model.campaign.AbyssOpened
import com.sperance.exileforge.core.model.campaign.LoneWolfRule
import com.sperance.exileforge.core.model.campaign.CampaignFall
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.CampaignReward
import com.sperance.exileforge.core.model.campaign.CombatRules
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.core.model.campaign.VaalZone
import com.sperance.exileforge.core.model.essences.CrystalState
import com.sperance.exileforge.core.model.essences.EssenceBook
import com.sperance.exileforge.core.model.skills.SkillBook
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where a run stands: walking, fighting, waiting on a kill's loot, at a Vaal portal's gate (2.65.0), at a
 * crystal of essences (2.78.0), at a crack of the Abyss or between its depths (2.82.0), or over one way or another.
 */
enum class RunPhase { MAP, FIGHT, LOOT, GATE, CRYSTAL, ABYSS, DEAD, CLEARED, LEFT }

/** A number floating off a fighter, [age] seconds after the blow that made it; [foe] is the foe of the pack it was about. */
data class FloatingHit(val id: Int, val target: Side, val action: Action, val kind: HitKind, val amount: Int, val age: Double, val healed: Int,
    val type: DamageType?, val inflicted: List<Ailment>, val stunned: Boolean, val foe: Int = 0)

/** The blow on screen right now, for the cards to act out: who, at or by which foe, what, whether it landed, and how far along (0..1). */
data class LungeView(val actor: Side, val action: Action, val kind: HitKind, val landed: Boolean, val progress: Float, val foe: Int = 0)

/** An ailment on a fighter as the overlay prints it: what, how much of it is left (1 fresh, 0 gone), and how many stacks. */
/**
 * One ailment on a fighter as its tile shows it: the share of time [left], how many [stacks], and
 * since 2.73.0 the [seconds] it still holds and its [strength] — damage a second for one that
 * hurts (every stack summed), the percent of slow or weakness for a chill or a shock.
 */
data class AilmentView(val ailment: Ailment, val left: Float, val stacks: Int, val seconds: Double = 0.0, val strength: Double = 0.0)

/**
 * One foe of the pack as its card prints it (2.70.0): who, its row, its pools, its swing, what is
 * on it, and whether the hero's weapon reaches it right now.
 */
data class FoeView(
    val index: Int,
    val monster: RolledMonster,
    val life: Int, val maxLife: Int, val shield: Int, val maxShield: Int,
    val swing: Float,
    val ailments: List<AilmentView>,
    val held: Boolean,
    val alive: Boolean,
    val reachable: Boolean,
    /** Taunts (2.71.0): while it stands the hero must strike it, past any row. */
    val taunt: Boolean = false,
    /** The buffs and the hero's curses on it (2.78.0). */
    val effects: List<EffectView> = emptyList(),
    /** Its mana (2.78.0): a boss's or a caster's, what its spells are paid with. */
    val mana: Int = 0, val maxMana: Int = 0,
) {
    val ranged: Boolean get() = monster.ranged
}

/**
 * The fight as the overlay prints it (2.70.0, the owner's mockup B «карточки против карточек»):
 * the pack as cards — [foes] in the order they were rolled, the overlay sorts them into rows — the
 * hero's pools, swing and states, what just landed, whom the hero strikes next ([target]) and
 * whether the player singled that one out ([focus]), and the blows so far, newest first.
 *
 * Until [started] nothing moves: it is the scouting pause, the pack laid open. [paused] is the same
 * pause asked for mid-fight.
 */
data class FightHud(
    val leader: RolledMonster,
    val foes: List<FoeView>,
    val heroLife: Int, val heroShield: Int,
    val hits: List<FloatingHit>,
    val speed: Int,
    val outcome: Outcome?,
    val heroSwing: Float = 0f,
    val heroAilments: List<AilmentView> = emptyList(),
    val heroHeld: Boolean = false,
    val retreating: Boolean = false,
    val lunge: LungeView? = null,
    val events: List<CombatEvent> = emptyList(),
    val started: Boolean = true,
    val paused: Boolean = false,
    val target: Int? = null,
    val focus: Int? = null,
    /** The hero fights alone and has the «Волк-одиночка» bonus (2.71.0), and how much of it. */
    val loneWolf: LoneWolfRule? = null,
    /** The hero taunts (2.71.0): it will matter once they have a party to cover. */
    val heroTaunt: Boolean = false,
    /** Since 2.78.0: the hero's mana and what the auras leave of it, the active slots, the belt, and what lies on the hero. */
    val heroMana: Int = 0, val heroMaxMana: Int = 0,
    val skills: List<SkillView?> = emptyList(),
    val flasks: List<FlaskView?> = emptyList(),
    val heroEffects: List<EffectView> = emptyList(),
    /** A barrier's soak left (2.78.0). */
    val heroBarrier: Int = 0,
    /** The level the foes stand at (2.82.0): a depth of the Abyss stands deeper than its zone; 0 is the zone's. */
    val level: Int = 0,
    /** Whether the hero may walk out (2.82.0): the Abyss lets nobody go mid-wave. */
    val escape: Boolean = true,
) {
    /** Nothing is moving and the pack is laid open: before «В бой», or paused. */
    val scouting: Boolean get() = outcome == null && (!started || paused)
}

/** One member of a pack fought and its own log, kept apart so a mixed pack's log names each one right. */
data class PackHit(val monster: RolledMonster, val events: List<CombatEvent>, val duration: Double)

/**
 * A fight that is over, as the screen after it reads it: the whole log to scroll back through and
 * what it came to — dealt and taken by blows and by ailments, how long, criticals,
 * blocks, evasions, what was inflicted.
 *
 * Since 2.54.0 a jetton can be a pack of up to three, fought all at once since 2.70.0: [pack]
 * holds one [PackHit] per foe in the fight, each with its own blows, and [monster] — for the header
 * and the portrait — is the strongest of them, the one the token showed on the map. Every figure
 * below sums over the whole pack.
 */
data class FightReport(
    val monster: RolledMonster,
    val outcome: Outcome,
    val pack: List<PackHit>,
    val duration: Double,
) {
    val events: List<CombatEvent> get() = pack.flatMap { it.events }
    val packSize: Int get() = pack.size
    private fun mine() = events.filter { it.actor == Side.HERO }
    private fun theirs() = events.filter { it.actor == Side.MONSTER }
    val dealt: Int get() = mine().sumOf { it.damage }.roundToInt()
    val taken: Int get() = theirs().sumOf { it.damage }.roundToInt()
    val crits: Int get() = mine().count { it.kind == HitKind.CRIT }
    val blocked: Int get() = mine().count { it.kind == HitKind.BLOCKED }
    val evaded: Int get() = theirs().count { it.kind == HitKind.EVADED }
    val inflicted: List<Ailment> get() = mine().flatMap { it.inflicted }.distinct()
}

/** Everything the overlay draws, as one value: it changes only when something on it does. */
data class RunHud(
    val phase: RunPhase,
    val mapCode: String,
    val heroLife: Int, val heroMaxLife: Int, val heroShield: Int, val heroMaxShield: Int,
    val alive: Int, val total: Int,
    /** The exit is sealed while the map's boss lives (since 2.34.0). */
    val sealed: Boolean = false,
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
    /** Fountains on the map not yet drunk (since 2.48.0). */
    val fountainsLeft: Int = 0,
    /** The Vaal zone behind the portal the hero stands at (2.65.0), once the server has rolled it. */
    val gate: VaalZone? = null,
    val gatePending: Boolean = false,
    val gateFailed: Boolean = false,
    /** This run is a Vaal zone (2.65.0): no way out but its guardian or a death, and a death is not the map's end. */
    val vaal: Boolean = false,
    /** Since 2.78.0: the hero's mana between fights, and the belt as the map's buttons draw it. */
    val heroMana: Int = 0, val heroMaxMana: Int = 0,
    val flasks: List<FlaskView?> = emptyList(),
    /** The crystal the hero stands at, its sheet open (2.78.0); the ones still standing on the map. */
    val crystal: CrystalView? = null,
    val crystalsLeft: Int = 0,
    /** The crack of the Abyss the hero stands at or descends (2.82.0), its sheet open; the cracks not yet opened. */
    val abyss: AbyssView? = null,
    val cracksLeft: Int = 0,
)

/**
 * The Abyss as its sheet shows it (2.82.0, server 0.72.0): how many depths the crack leads down, how many
 * are [cleared] — none before it is [open] — every depth's wave and hoard, and the share of the hoard in
 * percent a fall keeps. [pending] and [failed] are the server's answer to an opening or a claim; [hoard] is
 * what the claim brought, [fallen] once the hero fell in the depths and the hoard burned.
 */
data class AbyssView(val depth: Int, val cleared: Int, val open: Boolean, val depths: List<AbyssDepth>, val keep: Double,
                     val pending: Boolean = false, val failed: Boolean = false, val hoard: CampaignReward? = null, val fallen: Boolean = false) {
    /** The hoard as it stands now, the depths cleared counted; none before the first. */
    val current: com.sperance.exileforge.core.model.campaign.AbyssHoard? get() = depths.getOrNull(cleared - 1)?.hoard
    /** The depth below, if the crack leads there. */
    val next: AbyssDepth? get() = if (cleared < depth) depths.getOrNull(cleared) else null
}

/**
 * A crystal of essences as its sheet shows it (2.78.0): what it holds, who guards it — the zone's monster
 * standing up rare with its essences' modifiers — and whether a Vaal orb passed over it and made the
 * guardian stronger; a Vaal orb asked for and not yet answered, or refused.
 */
data class CrystalView(val id: Int, val essences: List<String>, val guardian: String, val stronger: Boolean, val vaal: Boolean,
                       val pending: Boolean = false, val failed: Boolean = false, val outcome: String? = null)

/** What the overlay asks of the run; applied at the start of the next step. */
sealed interface RunCommand {
    data object Continue : RunCommand
    data object Speed : RunCommand
    data object Leave : RunCommand
    /** Walk out of the fight: the monster gets its free swings first; before it began, simply walk away. */
    data object Retreat : RunCommand
    /** The fight begins (since 2.48.0): until then the pack is laid open to be studied. Mid-fight it ends a [Pause]. */
    data object Begin : RunCommand
    /** Stops the fight where it stands and lays the pack open again (2.70.0); [Begin] goes on. */
    data object Pause : RunCommand
    /**
     * A window over the map — the whole map, the gear — stops the world while it is open (2.73.0):
     * `true` takes a hold, `false` gives one back, and the run goes on once none is left.
     */
    data class Hold(val on: Boolean) : RunCommand
    /** Singles out foe [index] of the pack as the hero's target (2.70.0); the same one again lets the class choose. */
    data class Focus(val index: Int) : RunCommand
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
    /** The server says the map's boss was slain within the hour: it is not there, the exit is open. */
    data object BossAbsent : RunCommand
    /** The server rolled the Vaal zone behind the portal (2.65.0), for the gate to show. */
    data class Gate(val zone: VaalZone) : RunCommand
    data object GateFailed : RunCommand
    /** Stepping back from the gate undecided: the portal stays, and opens again when walked onto. */
    data object StepBack : RunCommand
    /** The zone was entered or refused: the portal is gone and the map goes on. */
    data object ShutGate : RunCommand
    /** Back from the Vaal zone with [life] left (2.65.0), and (2.78.0) the mana and flasks it left. */
    data class Returned(val life: Double, val pools: HeroPools? = null) : RunCommand
    /**
     * The gear changed on the map (since 2.40.0): the server's new sheet, and (2.78.0) the skills and the
     * belt. It lands between fights — one under way keeps the fighter it began with — and life keeps its share.
     */
    data class Regear(val gear: HeroGear) : RunCommand
    /** Uses the skill of active slot [slot] now (2.78.0), whatever its condition. */
    data class Cast(val slot: Int) : RunCommand
    /** Drinks the flask of belt place [slot] (2.78.0): in a fight at the next slice, on the map at once. */
    data class Drink(val slot: Int) : RunCommand
    /** Takes on the guardian of the crystal the hero stands at (2.78.0). */
    data object Release : RunCommand
    /** Asks for a Vaal orb on the crystal the hero stands at (2.78.0): whoever listens asks the server. */
    data object VaalCrystal : RunCommand
    /** The server's answer to a Vaal orb on a crystal: what it did, and the zone's crystals as they stand. */
    data class CrystalChanged(val outcome: String, val state: CrystalState) : RunCommand
    data object CrystalFailed : RunCommand
    /** Steps away from the crystal undecided: it stays, and opens again when walked up to. Leaves a crack of the Abyss as well. */
    data object StepOff : RunCommand
    /** At a crack of the Abyss (2.82.0): opens it, or goes a depth deeper from the sheet between depths. */
    data object Descend : RunCommand
    /** The server opened the crack: how deep the descent goes. The first wave rises at once. */
    data class AbyssOpened(val opened: com.sperance.exileforge.core.model.campaign.AbyssOpened) : RunCommand
    data object AbyssFailed : RunCommand
    /** Takes the hoard of the depths cleared and leaves the Abyss. */
    data object TakeHoard : RunCommand
    /** What the hoard brought — all of it, or what a fall left of it. */
    data class Hoard(val reward: CampaignReward) : RunCommand
    data object HoardFailed : RunCommand
}

/**
 * One run of a campaign map: the world, the hero's life across it and the fights on
 * the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the overlay reads [hud] and
 * sends [RunCommand]s, and the stick writes [stickX]/[stickY] in screen axes. Nothing here talks to
 * the server: a won fight calls [onKill], a lost one [onFallen] and the map's exit [onCleared], and
 * whoever listens reports them and hands the answer back as a command.
 *
 * The hero's life carries from fight to fight and **does not return while walking** (since
 * 2.29.0): life comes back only in a fight — by regeneration or leech — or once from each
 * fountain the map holds (since 2.48.0). The shield is whole
 * again after every fight. A lost fight ends the run and keeps everything already looted; a fight the hero walked out
 * of, or that ran out of time, leaves the monster standing and calm for a while.
 *
 * Since 2.78.0 mana carries too and comes back as the hero walks; the flasks come in full and fill with
 * kills, a draught runs on between fights, and a fountain fills every flask. A crystal of essences opens a
 * sheet: its guardian is fought like any pack and reported by [onCrystal], a Vaal orb on it by [onCrystalVaal].
 */
class ExpeditionRun(
    val map: CampaignMap,
    val world: ExpeditionWorld,
    build: HeroBuild,
    val rules: CombatRules,
    private val seed: Long,
    private val onKill: (RolledMonster) -> Unit,
    private val onCleared: () -> Unit,
    private val onFallen: () -> Unit = {},
    private val onChest: () -> Unit = {},
    /** The entered map's effects, laid again over a sheet that changes on the way (since 2.40.0); a Vaal zone inherits them (2.65.1). */
    val mapEffects: Map<String, Double> = emptyMap(),
    /** The hero reached the Vaal portal (2.65.0): whoever listens asks the server for its zone. */
    private val onPortal: () -> Unit = {},
    /** What the hero walks in with; a Vaal zone (2.65.0) is entered with what the map left, a map at full. */
    startPools: HeroPools? = null,
    /** The monsters' skills (2.78.0): what a crystal's guardian casts. */
    private val skills: SkillBook = SkillBook(),
    /** The essences (2.78.0): which modifier a crystal's guardian takes from each. */
    private val essences: EssenceBook = EssenceBook(),
    /** The rarities of the map, as it changes them: a guardian stands up rare by them. */
    private val rarities: List<CampaignRarity> = emptyList(),
    /** A crystal's guardian slain (2.78.0): its place among the crystals still standing, as the server counts them. */
    private val onCrystal: (Int) -> Unit = {},
    /** A Vaal orb asked for on a crystal (2.78.0), by the same place. */
    private val onCrystalVaal: (Int) -> Unit = {},
    /** The zone's Abyss (2.82.0): its depths and hoards; none without a crack. */
    private val abyss: AbyssLaunch? = null,
    /** A crack opened, by its place among those still unopened, as the server counts them. */
    private val onAbyssOpen: (Int) -> Unit = {},
    /** The descent over at so many depths cleared: taken, or fallen. */
    private val onAbyssClaim: (Int, Boolean) -> Unit = { _, _ -> },
    /** Modifiers a rare monster carries beyond its rule (the atlas): the Abyss's rares too. */
    private val extraRareMods: Int = 0,
) {
    /** How the hero's body is made: the sheet, the passives and the map; a change of gear replaces it between fights. */
    var build: HeroBuild = build
        private set
    /** Whom the hero picks and how far their weapon reaches; a change of weapon on the map changes it between fights. */
    val stance: HeroStance get() = build.gear.stance
    /** The hero between fights: the sheet with the passives and a draught still running. */
    var hero: Combatant = build.body
        private set
    @Volatile var stickX = 0.0
    @Volatile var stickY = 0.0
    private val commands = ConcurrentLinkedQueue<RunCommand>()
    private var phase = RunPhase.MAP
    private var life = startPools?.life?.coerceIn(0.0, hero.maxLife) ?: hero.maxLife
    /** The hero's life right now, between fights: what a Vaal zone is entered with. */
    val heroLife: Double get() = life
    private val kit: Loadout get() = build.gear.kit
    /** Mana, the flasks' charges and how long each draught still runs (2.78.0), between fights. */
    private var mana = startPools?.mana?.coerceIn(0.0, manaCap()) ?: manaCap()
    private var charges: List<Double> = kit.flasks.mapIndexed { i, flask -> flask?.let { startPools?.charges?.getOrNull(i)?.coerceIn(0.0, it.maxCharges) ?: it.maxCharges } ?: 0.0 }
    private var flaskLeft: List<Double> = kit.flasks.indices.map { startPools?.flaskLeft?.getOrNull(it) ?: 0.0 }
    /** What each running draught still gives per second (2.81.0): it heals on the road too, not only in a fight. */
    private var rates: List<DraughtRate> = kit.flasks.indices.map { startPools?.rates?.getOrNull(it) ?: DraughtRate() }
    /** What the hero carries into a Vaal zone and back out of it. */
    val pools: HeroPools get() = HeroPools(life, mana, charges, flaskLeft, rates)
    private var gate: VaalZone? = null
    private var gatePending = false
    private var gateFailed = false
    private var crystal: CrystalSpot? = null
    private var crystalPending = false
    private var crystalFailed = false
    private var crystalOutcome: String? = null
    /** The crack the hero stands at (2.82.0), the descent under way, and the server's answer awaited or refused. */
    private var rift: AbyssSpot? = null
    private var descent: Descent? = null
    private var abyssPending = false
    private var abyssFailed = false
    /** The fight is a wave of the Abyss: no walking out, no kill reported — the hoard pays. */
    private var abyssFight = false
    /** The level the fight's foes stand at. */
    private var fightLevel = 0
    private var started = false
    private var paused = false
    /** How many windows hold the run (2.73.0); the world stands still while any does. */
    private var holds = 0
    private var fights = 0
    private var speed = 1
    private var reward: CampaignReward? = null
    /** In-flight `kill` calls (since 2.54.0, one per pack member): the loot screen waits for all of them. */
    private var pendingRewards = 0
    private var rewardFailed = false
    private var slain: RolledMonster? = null
    private var report: FightReport? = null
    /** The pack members in the fight, by their place in the pack; the battle numbers them in this order. */
    private var members: List<Int> = emptyList()
    /** How many of the battle's fallen were reported already. */
    private var reported = 0
    private var fall: CampaignFall? = null
    private var fallPending = false
    private var gold = 0L
    private var experience = 0.0
    private var kills = 0
    private var chest: CampaignReward? = null
    private var chestPending = false
    private var chestFailed = false
    private var fightAgent: MonsterAgent? = null

    private var pendingGear: RunCommand.Regear? = null

    private fun manaCap(): Double = hero.maxMana * (1 - kit.reserved(hero) / 100)

    private fun regear(gear: HeroGear) {
        val next = HeroBuild(gear, mapEffects, rules)
        val before = hero
        build = next
        rebody()
        life = if (before.maxLife > 0) life / before.maxLife * hero.maxLife else hero.maxLife
        // A flask put on the belt on the way comes with the charges its place had, up to its own ceiling.
        charges = next.gear.kit.flasks.mapIndexed { i, flask -> flask?.let { (charges.getOrNull(i) ?: 0.0).coerceIn(0.0, it.maxCharges) } ?: 0.0 }
        flaskLeft = next.gear.kit.flasks.indices.map { i -> if (next.gear.kit.flasks[i] != null) flaskLeft.getOrNull(i) ?: 0.0 else 0.0 }
        rates = next.gear.kit.flasks.indices.map { i -> if (next.gear.kit.flasks[i] != null) rates.getOrNull(i) ?: DraughtRate() else DraughtRate() }
        rebody()
    }

    /** The hero between fights made again: the sheet with the draughts still running, and the pace and sight they give. */
    private fun rebody() {
        val lines = kit.flasks.withIndex().filter { (i, flask) -> flask != null && (flaskLeft.getOrNull(i) ?: 0.0) > 0 }
            .flatMap { (_, flask) -> flask!!.draught(build.body, life, 0.0).lines }
        hero = if (lines.isEmpty()) build.body else build.body(lines)
        mana = mana.coerceIn(0.0, manaCap())
        world.regear(ExpeditionWorld.heroSpeed(hero.stats), ExpeditionWorld.lightRadius(hero.stats, map.light))
    }

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
        if (holds == 0) when (phase) {
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
            RunCommand.Retreat -> if (abyssFight) Unit else if (fight != null && !started) walkAway() else { paused = false; fight?.retreat() }
            RunCommand.Begin -> if (fight != null) { started = true; paused = false }
            RunCommand.Pause -> if (fight != null && started && fight?.outcome == null) paused = !paused
            is RunCommand.Focus -> fight?.focus(command.index)
            is RunCommand.Hold -> holds = (holds + if (command.on) 1 else -1).coerceAtLeast(0)
            RunCommand.Continue -> when (phase) {
                RunPhase.LOOT -> if (pendingRewards == 0) { phase = RunPhase.MAP; reward = null; slain = null; report = null; rewardFailed = false }
                RunPhase.DEAD -> if (!fallPending) phase = RunPhase.LEFT
                RunPhase.CLEARED -> phase = RunPhase.LEFT
                else -> Unit
            }
            // A pack (2.54.0) reports one kill per foe, so this can land more than once per
            // encounter — merged rather than replaced, gold and experience summed as always.
            is RunCommand.Reward -> {
                val incoming = command.reward
                reward = reward?.copy(
                    experience = reward!!.experience + incoming.experience, gold = reward!!.gold + incoming.gold,
                    items = mergeStacks(reward!!.items, incoming.items), equipment = reward!!.equipment + incoming.equipment,
                    level = incoming.level, totalExperience = incoming.totalExperience, money = incoming.money,
                    recipeFound = reward!!.recipeFound ?: incoming.recipeFound,
                ) ?: incoming
                pendingRewards--
                gold += incoming.gold
                experience += incoming.experience
            }
            RunCommand.RewardFailed -> { pendingRewards--; rewardFailed = true }
            is RunCommand.Fallen -> { fall = command.fall; fallPending = false }
            RunCommand.FallFailed -> fallPending = false
            is RunCommand.Chests -> world.placeChests(command.count)
            is RunCommand.Regear -> if (phase == RunPhase.FIGHT) pendingGear = command else regear(command.gear)
            is RunCommand.ChestReward -> {
                chest = command.reward
                chestPending = false
                gold += command.reward.gold
            }
            RunCommand.ChestFailed -> { chestPending = false; chestFailed = true }
            RunCommand.DismissChest -> if (!chestPending) { chest = null; chestFailed = false }
            RunCommand.BossAbsent -> if (fightAgent !== world.boss) world.bossAbsent()
            is RunCommand.Gate -> if (phase == RunPhase.GATE) { gate = command.zone; gatePending = false; gateFailed = false }
            RunCommand.GateFailed -> { gatePending = false; gateFailed = true }
            RunCommand.StepBack -> if (phase == RunPhase.GATE) closeGate()
            RunCommand.ShutGate -> { world.closePortal(); closeGate() }
            is RunCommand.Returned -> {
                life = command.life.coerceIn(0.0, hero.maxLife)
                command.pools?.let { mana = it.mana.coerceIn(0.0, manaCap()); charges = it.charges.ifEmpty { charges }; flaskLeft = it.flaskLeft.ifEmpty { flaskLeft }; rates = it.rates.ifEmpty { rates }; rebody() }
                phase = RunPhase.MAP
            }
            is RunCommand.Cast -> fight?.useSkill(command.slot)
            is RunCommand.Drink -> if (phase == RunPhase.FIGHT) fight?.useFlask(command.slot)
                else if (phase == RunPhase.MAP || phase == RunPhase.CRYSTAL || phase == RunPhase.ABYSS) drinkOnMap(command.slot)
            RunCommand.Release -> if (phase == RunPhase.CRYSTAL && !crystalPending) release()
            RunCommand.VaalCrystal -> crystal?.takeIf { phase == RunPhase.CRYSTAL && !crystalPending && !it.crystal.vaal }?.let { spot ->
                crystalPending = true; crystalFailed = false; crystalOutcome = null
                onCrystalVaal(world.standingCrystals.indexOf(spot))
            }
            is RunCommand.CrystalChanged -> {
                crystalPending = false
                crystalOutcome = command.outcome
                // The server's crystals in its order are the ones still standing here, in theirs.
                world.standingCrystals.zip(command.state.crystals).forEach { (spot, held) -> spot.crystal = held }
            }
            RunCommand.CrystalFailed -> { crystalPending = false; crystalFailed = true }
            RunCommand.StepOff -> when {
                phase == RunPhase.CRYSTAL && !crystalPending -> closeCrystal()
                // A crack is left unopened, or once its hoard is in — never mid-descent with the hoard at stake.
                phase == RunPhase.ABYSS && !abyssPending && (descent == null || descent?.hoard != null || abyssFailed) -> closeRift()
            }
            RunCommand.Descend -> if (phase == RunPhase.ABYSS && !abyssPending) descend()
            is RunCommand.AbyssOpened -> if (phase == RunPhase.ABYSS) opened(command.opened)
            RunCommand.AbyssFailed -> { abyssPending = false; abyssFailed = true }
            RunCommand.TakeHoard -> descent?.takeIf { phase == RunPhase.ABYSS && !abyssPending && it.cleared > 0 && it.hoard == null }?.let { take(it, fallen = false) }
            is RunCommand.Hoard -> {
                abyssPending = false
                descent?.hoard = command.reward
                gold += command.reward.gold
                experience += command.reward.experience
            }
            RunCommand.HoardFailed -> { abyssPending = false; abyssFailed = true }
        }
    }

    // ==================== The Abyss (2.82.0) ====================

    /** A descent under way: its crack, how many depths it leads down, how many are cleared, and the fights of the wave left. */
    private class Descent(val spot: AbyssSpot, val depth: Int) {
        var cleared = 0
        var level = 0
        var fights: List<List<RolledMonster>> = emptyList()
        var hoard: CampaignReward? = null
        var fallen = false
    }

    /** «Спуститься»: the crack is opened on the server first; between depths, the next wave rises. */
    private fun descend() {
        val spot = rift ?: return
        val current = descent
        if (current == null) {
            abyssPending = true; abyssFailed = false
            onAbyssOpen(world.standingCracks.indexOf(spot))
        } else if (current.hoard == null && current.cleared < current.depth) wave(current, current.cleared + 1)
    }

    private fun opened(answer: AbyssOpened) {
        abyssPending = false
        val spot = rift ?: return
        spot.opened = true
        Descent(spot, answer.depth.coerceAtLeast(1)).also { descent = it; wave(it, 1) }
    }

    /** Depth [depth]'s wave rises from the crack: its fights, fought one after another. */
    private fun wave(current: Descent, depth: Int) {
        val launch = abyss ?: return
        current.level = launch.depths.getOrNull(depth - 1)?.level ?: map.level
        current.fights = AbyssWaves.wave(launch, depth, map, rarities, mapEffects, skills, extraRareMods, Random(seed * 211 + current.spot.id * 31L + depth))
        nextFight(current)
    }

    private fun nextFight(current: Descent) {
        val group = current.fights.firstOrNull() ?: return
        current.fights = current.fights.drop(1)
        engage(MonsterAgent(ABYSS_AGENT - current.spot.id, group, current.spot.cell.x + 0.5, current.spot.cell.y + 0.5), current.level, abyssal = true)
    }

    /** The descent is over: the hoard of the depths cleared is asked for — whole, or what a fall leaves of it. */
    private fun take(current: Descent, fallen: Boolean) {
        current.fallen = fallen
        abyssPending = true; abyssFailed = false
        onAbyssClaim(current.cleared, fallen)
    }

    private fun closeRift() {
        rift = null; descent = null; abyssFailed = false
        if (phase == RunPhase.ABYSS) phase = RunPhase.MAP
    }

    private fun closeGate() {
        gate = null; gatePending = false; gateFailed = false
        if (phase == RunPhase.GATE) phase = RunPhase.MAP
    }

    private fun closeCrystal() {
        crystal = null; crystalFailed = false; crystalOutcome = null
        if (phase == RunPhase.CRYSTAL) phase = RunPhase.MAP
    }

    /** Two orb stacks merged by item (2.54.0): a pack's rewards are summed, not replaced. */
    private fun mergeStacks(a: List<com.sperance.exileforge.core.model.hero.CharacterItem>, b: List<com.sperance.exileforge.core.model.hero.CharacterItem>) =
        (a + b).groupingBy { it.itemId }.fold(0L) { total, item -> total + item.amount }
            .map { (itemId, amount) -> com.sperance.exileforge.core.model.hero.CharacterItem(itemId, amount) }

    /** Turning away before the fight began: nothing was struck, and the monster stays calm a while. */
    private fun walkAway() {
        fightAgent?.let { agent -> if (agent.pack.none { it.crystal != null }) world.retreatFrom(agent) }
        fight = null
        fightAgent = null
        phase = RunPhase.MAP
    }

    /**
     * A draught on the map (2.78.0): what it gives back comes at once, and what it lays on runs as the hero
     * walks — a quicksilver flask is for the road as much as for the fight.
     */
    private fun drinkOnMap(slot: Int) {
        val flask = kit.flasks.getOrNull(slot) ?: return
        if ((flaskLeft.getOrNull(slot) ?: 0.0) > 0 || (charges.getOrNull(slot) ?: 0.0) + 1e-9 < flask.perUse(hero)) return
        val draught = flask.draught(hero, life, manaCap())
        charges = charges.toMutableList().also { it[slot] = if (flask.usesAll) 0.0 else (it[slot] - flask.perUse(hero)).coerceAtLeast(0.0) }
        flaskLeft = flaskLeft.toMutableList().also { it[slot] = draught.duration }
        rates = rates.toMutableList().also { it[slot] = DraughtRate(draught.lifeRate, draught.manaRate) }
        rebody()
        // The instant share now, the rest over the draught's time as the hero walks — as in a fight.
        life = (life + draught.life).coerceAtMost(hero.maxLife)
        mana = (mana + draught.mana).coerceAtMost(manaCap())
    }

    private fun walk(dt: Double) {
        // Mana comes back as the hero walks (2.78.0), and a draught runs out on the road as in a fight.
        mana = (mana + hero.manaRegen(rules.mana) * dt).coerceAtMost(manaCap())
        if (flaskLeft.any { it > 0 }) {
            val before = flaskLeft.map { it > 0 }
            flaskLeft.forEachIndexed { i, left ->
                val rate = rates.getOrNull(i) ?: return@forEachIndexed
                val slice = min(dt, left)
                if (slice <= 0 || !rate.flows) return@forEachIndexed
                life = (life + rate.life * slice).coerceAtMost(hero.maxLife)
                mana = (mana + rate.mana * slice).coerceAtMost(manaCap())
            }
            flaskLeft = flaskLeft.map { (it - dt).coerceAtLeast(0.0) }
            if (flaskLeft.map { it > 0 } != before) rebody()
        }
        val (x, y) = ExpeditionWorld.screenToWorld(stickX, stickY)
        when (val event = world.step(dt, x, y)) {
            is WorldEvent.Encounter -> engage(event.agent)
            WorldEvent.Exit -> { phase = RunPhase.CLEARED; onCleared() }
            is WorldEvent.Opened -> { chest = null; chestFailed = false; chestPending = true; onChest() }
            is WorldEvent.Drank -> {
                life = (life + hero.maxLife * event.fountain.heal / 100).coerceAtMost(hero.maxLife)
                // A fountain fills the flasks, and gives mana back as it gives life (2.78.0).
                mana = (mana + manaCap() * event.fountain.heal / 100).coerceAtMost(manaCap())
                charges = kit.flasks.map { it?.maxCharges ?: 0.0 }
            }
            WorldEvent.Portal -> { phase = RunPhase.GATE; gate = null; gateFailed = false; gatePending = true; onPortal() }
            is WorldEvent.Crystal -> { phase = RunPhase.CRYSTAL; crystal = event.spot; crystalFailed = false; crystalOutcome = null }
            is WorldEvent.Abyss -> { phase = RunPhase.ABYSS; rift = event.spot; descent = null; abyssFailed = false }
            null -> Unit
        }
    }

    /**
     * A fight with [agent]'s pack still standing, all at once (2.70.0), melee in front and ranged behind, at
     * [level] — the zone's, or a depth's of the Abyss ([abyssal], 2.82.0).
     */
    private fun engage(agent: MonsterAgent, level: Int = map.level, abyssal: Boolean = false) {
        members = agent.standing
        reported = 0
        fightAgent = agent
        abyssFight = abyssal
        fightLevel = level
        fight = Battle(hero, members.map { index ->
            val monster = agent.pack[index]
            Foe(Combatant(monster.stats, level, rules), monster.ranged, monster.rarity, monster.skills.mapNotNull(skills.monsters::get))
        }, rules, life, Random(seed * 31 + fights++), stance, kit = kit, model = build, pools = HeroPools(life, mana, charges, flaskLeft),
            percent = build.gear.percent)
        started = false
        paused = false
        phase = RunPhase.FIGHT
    }

    /**
     * «Освободить»: the crystal's guardian stands up (2.78.0) — the zone's monster, rare, with the modifiers
     * of the essences it guards, stronger if a Vaal orb or the atlas made it so — and the fight begins.
     */
    private fun release() {
        val spot = crystal ?: return
        val modifiers = spot.crystal.essences.mapNotNull(essences::monster).distinct().mapNotNull { code -> map.essences.firstOrNull { it.code == code } }
        val extra = MapEffects.guardianBuffs(spot.crystal.stronger, essences.crystals.stronger, mapEffects)
        val guardian = MonsterRoller.guardian(map, rarities, spot.crystal.guardian, modifiers, extra, spot.id, Random(seed * 131 + spot.id))
            ?.let { MonsterRoller.skilled(it, skills, Random(seed * 137 + spot.id)) } ?: return
        crystal = null
        crystalOutcome = null
        engage(MonsterAgent(-1 - spot.id, listOf(guardian), spot.cell.x + 0.5, spot.cell.y + 0.5))
    }

    private fun play(dt: Double) {
        val battle = fight ?: return
        val agent = fightAgent ?: return
        if (!started || paused) return
        battle.advance(dt * speed)
        // Every foe is a kill of its own, reported the moment it falls (2.54.0), the fight still going.
        while (reported < battle.fallen.size) {
            val member = members[battle.fallen[reported++]]
            agent.fallen += member
            kills++
            // A wave of the Abyss pays with its hoard (2.82.0), not kill by kill.
            if (abyssFight) continue
            pendingRewards++
            val monster = agent.pack[member]
            // A crystal's guardian is reported by its crystal's place among those still standing, then the crystal is gone.
            val spot = monster.crystal?.let { id -> world.crystals.firstOrNull { it.id == id } }
            if (spot != null) { onCrystal(world.standingCrystals.indexOf(spot)); spot.freed = true } else onKill(monster)
        }
        val outcome = battle.outcome ?: return
        if (battle.time < battle.duration + AFTERMATH) return
        val out = battle.pools()
        life = out.life
        mana = out.mana
        charges = out.charges
        flaskLeft = out.flaskLeft
        rates = out.rates
        rebody()
        val pack = members.mapIndexed { index, member -> PackHit(agent.pack[member], battle.events.filter { it.foe == index }, battle.duration) }
        val down = descent?.takeIf { abyssFight }
        when (outcome) {
            Outcome.WIN -> {
                agent.alive = false
                // A wave's fight won goes on to the next, or clears the depth: no loot screen in the Abyss (2.82.0).
                if (down != null) { report = null; phase = RunPhase.ABYSS }
                else {
                    slain = agent.monster
                    report = FightReport(agent.monster, Outcome.WIN, pack, battle.duration)
                    phase = RunPhase.LOOT
                }
            }
            Outcome.LOSS -> {
                report = FightReport(agent.monster, Outcome.LOSS, pack, battle.duration)
                life = 0.0; fallPending = true; phase = RunPhase.DEAD; onFallen()
                // A fall in the Abyss burns its hoard, but for the atlas's share.
                down?.let { take(it, fallen = true) }
            }
            // Nothing already looted is lost — every foe that fell was reported as it fell — but
            // there is no report for a fight cut short, and the rest of the pack stays standing.
            // A wave that ran out of time throws the hero out of the Abyss, the hoard as if fallen.
            Outcome.RETREAT -> {
                if (agent.id >= 0) world.retreatFrom(agent)
                report = null
                if (down != null) { phase = RunPhase.ABYSS; take(down, fallen = true) } else phase = RunPhase.MAP
            }
        }
        fight = null
        fightAgent = null
        abyssFight = false
        // Gear changed while the fight went on lands now, on the life the fight left.
        if (phase != RunPhase.DEAD) pendingGear?.let { regear(it.gear) }
        pendingGear = null
        // The wave goes on (2.82.0): its next fight at once, or the depth is cleared and its sheet opens.
        if (outcome == Outcome.WIN && down != null) { if (down.fights.isNotEmpty()) nextFight(down) else down.cleared++ }
    }

    private fun snapshot(): RunHud {
        val battle = fight
        return RunHud(
            phase = phase, mapCode = map.code,
            heroLife = (battle?.heroLife ?: life).roundToInt(), heroMaxLife = hero.maxLife.roundToInt(),
            heroShield = (battle?.heroFighter?.shield ?: hero.maxShield).roundToInt(), heroMaxShield = hero.maxShield.roundToInt(),
            alive = world.alive, total = world.total, sealed = world.sealed,
            fight = battle?.let { b -> fightAgent?.let { fightHud(b, it) } },
            reward = reward, rewardPending = pendingRewards > 0, rewardFailed = rewardFailed, slain = slain, report = report,
            fall = fall, fallPending = fallPending,
            gold = gold, experience = experience, kills = kills,
            chestsLeft = world.chests.count { !it.opened }, chest = chest, chestPending = chestPending, chestFailed = chestFailed,
            fountainsLeft = world.fountains.count { !it.used },
            gate = gate, gatePending = gatePending, gateFailed = gateFailed, vaal = VaalZones.isZone(map),
            heroMana = (battle?.heroMana ?: mana).roundToInt(), heroMaxMana = (battle?.manaCap() ?: manaCap()).roundToInt(),
            flasks = battle?.flaskViews() ?: mapFlasks(),
            crystal = crystal?.let { CrystalView(it.id, it.crystal.essences, it.crystal.guardian, it.crystal.stronger, it.crystal.vaal, crystalPending, crystalFailed, crystalOutcome) },
            crystalsLeft = world.standingCrystals.size,
            abyss = abyssView(), cracksLeft = world.standingCracks.size,
        )
    }

    private fun abyssView(): AbyssView? {
        val spot = rift ?: return null
        val launch = abyss ?: return null
        val down = descent
        return AbyssView(down?.depth ?: spot.depth, down?.cleared ?: 0, down != null, launch.depths, launch.keep, abyssPending, abyssFailed, down?.hoard,
            down?.fallen == true)
    }

    /** The belt between fights, as the map's buttons draw it. */
    private fun mapFlasks(): List<FlaskView?> = kit.flasks.mapIndexed { i, flask ->
        flask?.let {
            val left = flaskLeft.getOrNull(i) ?: 0.0
            FlaskView(i, it.code, it.kind, (charges.getOrNull(i) ?: 0.0).toInt(), it.maxCharges.toInt(), kotlin.math.ceil(it.perUse(hero) - 1e-9).toInt(),
                if (left > 0) (left / it.duration(hero)).toFloat().coerceIn(0f, 1f) else 0f, it.condition)
        }
    }

    private fun fightHud(battle: Battle, agent: MonsterAgent): FightHud {
        val h = battle.heroFighter
        val hits = battle.events.withIndex()
            .filter { (_, event) -> event.time <= battle.time && battle.time - event.time < HIT_LIFETIME && event.action != Action.RETREAT &&
                (event.damage > 0 || event.healed > 0 || event.kind == HitKind.EVADED || event.kind == HitKind.BLOCKED || event.action == Action.ATTACK) }
            .map { (index, event) ->
                FloatingHit(index, event.target, event.action, event.kind, event.damage.roundToInt(), battle.time - event.time, event.healed.roundToInt(),
                    event.type, event.inflicted, event.stunned, event.foe)
            }
        fun ailments(f: Battle.Fighter) = f.ailments.groupBy { it.ailment }.map { (ailment, active) ->
            val until = active.maxOf { it.until }
            AilmentView(ailment, ((until - battle.time) / active.first().duration).toFloat().coerceIn(0f, 1f), active.size,
                (until - battle.time).coerceAtLeast(0.0), if (ailment.hurts) active.sumOf { it.magnitude } else active.maxOf { it.magnitude })
        }
        val foes = battle.foeFighters.map { f ->
            FoeView(f.index, agent.pack[members[f.index]], f.life.roundToInt(), f.body.maxLife.roundToInt(), f.shield.roundToInt(), f.body.maxShield.roundToInt(),
                battle.swing(f), ailments(f), f.held, f.alive, battle.reachable(f.index), f.body.taunt, battle.effects(f),
                f.mana.roundToInt(), f.body.maxMana.roundToInt())
        }
        return FightHud(
            leader = agent.monster, foes = foes,
            heroLife = h.life.roundToInt(), heroShield = h.shield.roundToInt(),
            hits = hits, speed = speed,
            outcome = battle.outcome,
            heroSwing = battle.swing(h), heroAilments = ailments(h), heroHeld = h.held,
            retreating = battle.retreating,
            lunge = battle.lunge()?.let { (event, progress) -> LungeView(event.actor, event.action, event.kind, event.landed, progress.toFloat(), event.foe) },
            events = battle.events.toList().asReversed(),
            started = started, paused = paused,
            target = battle.target()?.index, focus = battle.focus,
            loneWolf = rules.loneWolf.takeIf { battle.loneWolf }, heroTaunt = hero.taunt,
            heroMana = h.mana.roundToInt(), heroMaxMana = battle.manaCap().roundToInt(),
            skills = battle.skillViews(), flasks = battle.flaskViews(), heroEffects = battle.effects(h),
            heroBarrier = h.barrier.roundToInt(),
            level = fightLevel, escape = !abyssFight,
        )
    }

    companion object {
        /** How long the fight's last blow hangs before the scene moves on. */
        const val AFTERMATH = 0.8
        const val HIT_LIFETIME = 1.0
        /** The agents of the Abyss's waves are numbered down from here, out of the way of the map's and the crystals'. */
        private const val ABYSS_AGENT = -10_000

        /**
         * A run of [map] by the hero as [gear] has them; [mapEffects] are the summed effects of the map item it
         * was entered with (since 2.37.0), see [MapEffects]. [crystals] are the zone's crystals of essences (2.78.0).
         */
        fun start(map: CampaignMap, rarities: List<CampaignRarity>, gear: HeroGear, seed: Long,
                  onKill: (RolledMonster) -> Unit, onCleared: () -> Unit, rules: CombatRules = CombatRules(), onFallen: () -> Unit = {},
                  onChest: () -> Unit = {}, mapEffects: Map<String, Double> = emptyMap(),
                  fountains: com.sperance.exileforge.core.model.campaign.FountainRule = com.sperance.exileforge.core.model.campaign.FountainRule(),
                  portalChance: Double = 0.0, onPortal: () -> Unit = {}, startPools: HeroPools? = null,
                  /** Modifiers a rare monster carries beyond its rule (the atlas, server 0.66.0). */
                  extraRareMods: Int = 0,
                  skills: SkillBook = SkillBook(), essences: EssenceBook = EssenceBook(), crystals: CrystalState? = null,
                  onCrystal: (Int) -> Unit = {}, onCrystalVaal: (Int) -> Unit = {},
                  abyss: AbyssLaunch? = null, onAbyssOpen: (Int) -> Unit = {}, onAbyssClaim: (Int, Boolean) -> Unit = { _, _ -> }): ExpeditionRun {
            val build = HeroBuild(gear, mapEffects, rules)
            val stats = build.body.stats
            val changed = MapEffects.rarities(rarities, mapEffects)
            val world = ExpeditionWorld.create(MapEffects.map(map, mapEffects), changed, stats, seed, MapEffects.buffs(mapEffects), portalChance,
                MapEffects.bossBuffs(mapEffects), extraRareMods, skills)
            // The map's own fountains (server 0.66.0) join the rule's, low and high alike.
            val extraFountains = MapEffects.fountains(mapEffects)
            world.placeFountains(fountains.count.getOrElse(0) { 0 } + extraFountains, fountains.count.getOrElse(1) { 0 } + extraFountains, fountains.heal)
            crystals?.let { world.placeCrystals(it.crystals) }
            abyss?.let { world.placeCracks(it.cracks) }
            return ExpeditionRun(map, world, build, rules, seed, onKill, onCleared, onFallen, onChest, mapEffects, onPortal, startPools, skills, essences, changed,
                onCrystal, onCrystalVaal, abyss, onAbyssOpen, onAbyssClaim, extraRareMods)
        }
    }
}
