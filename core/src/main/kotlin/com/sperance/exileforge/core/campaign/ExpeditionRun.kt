package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.DesecrationKind
import com.sperance.exileforge.rules.content.DesecrationRule
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetRole
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.content.LoneWolfRule
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunEventKind
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a run stands: walking, fighting, at a kill's loot, at a Vaal portal's gate, at a crystal, at a crack of the Abyss, or over one way or another. */
enum class RunPhase { MAP, FIGHT, LOOT, GATE, CRYSTAL, ABYSS, DEAD, CLEARED, LEFT }

/** A number floating off a fighter, [age] seconds after the blow that made it; [foe] is the foe of the pack it was about. */
data class FloatingHit(val id: Int, val target: Side, val action: Action, val kind: HitKind, val amount: Int, val age: Double, val healed: Int,
    val type: DamageType?, val inflicted: List<Ailment>, val stunned: Boolean, val foe: Int = 0)

/** The blow on screen right now, for the cards to act out. */
data class LungeView(val actor: Side, val action: Action, val kind: HitKind, val landed: Boolean, val progress: Float, val foe: Int = 0)

/** One ailment on a fighter as its tile shows it: the share of time [left], the [stacks], the [seconds] it still holds and its [strength]. */
data class AilmentView(val ailment: Ailment, val left: Float, val stacks: Int, val seconds: Double = 0.0, val strength: Double = 0.0)

/** One foe of the pack as its card prints it. */
data class FoeView(
    val index: Int,
    val monster: RolledMonster,
    val life: Int, val maxLife: Int, val shield: Int, val maxShield: Int,
    val swing: Float,
    val ailments: List<AilmentView>,
    val held: Boolean,
    val alive: Boolean,
    val reachable: Boolean,
    val taunt: Boolean = false,
    val effects: List<EffectView> = emptyList(),
    val mana: Int = 0, val maxMana: Int = 0,
    /** The row it stands in (3.28.0): the back, or the front — by its kind, unless its own row was full. */
    val back: Boolean = monster.ranged,
) {
    val ranged: Boolean get() = monster.ranged
}

/** The fight as the overlay prints it: the pack as cards, the hero's pools and states, what just landed, and the blows so far, newest first. */
/** The pet in a fight as its bar shows it (3.5.0). */
data class AllyView(val species: String, val life: Int, val maxLife: Int, val alive: Boolean)

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
    val loneWolf: LoneWolfRule? = null,
    /** The pet beside the hero (3.5.0). */
    val ally: AllyView? = null,
    val heroTaunt: Boolean = false,
    val heroMana: Int = 0, val heroMaxMana: Int = 0,
    val skills: List<SkillView?> = emptyList(),
    val flasks: List<FlaskView?> = emptyList(),
    val heroEffects: List<EffectView> = emptyList(),
    val heroBarrier: Int = 0,
    /** The level the foes stand at: a depth of the Abyss stands deeper than its zone. */
    val level: Int = 0,
    /** Whether the hero may walk out: the Abyss lets nobody go mid-wave. */
    val escape: Boolean = true,
    /** The stage of a gathered fight (3.28.0), from 1, of [stages]: one pack a stage. */
    val stage: Int = 1, val stages: Int = 1,
    /** Seconds left of the pause before this stage begins by itself; null when there is none. */
    val interlude: Double? = null,
) {
    val scouting: Boolean get() = outcome == null && (!started || paused)
}

/** One member of a pack fought and its own log. */
data class PackHit(val monster: RolledMonster, val events: List<CombatEvent>, val duration: Double)

/** A fight that is over, as the screen after it reads it. */
data class FightReport(val monster: RolledMonster, val outcome: Outcome, val pack: List<PackHit>, val duration: Double) {
    val events: List<CombatEvent> get() = pack.flatMap { it.events }
    val packSize: Int get() = pack.size
    private fun mine() = events.filter { it.actor == Side.HERO }
    private fun theirs() = events.filter { it.actor == Side.MONSTER }
    val dealt: Int get() = mine().sumOf { it.damage }.roundToInt()
    val taken: Int get() = theirs().sumOf { it.damage }.roundToInt()
    val crits: Int get() = mine().count { it.kind == HitKind.CRIT }
    val blocked: Int get() = mine().count { it.kind == HitKind.BLOCKED }
    val inflicted: List<Ailment> get() = mine().flatMap { it.inflicted }.distinct()
}

/** Everything the overlay draws, as one value: it changes only when something on it does. */
data class RunHud(
    val phase: RunPhase,
    val mapCode: String,
    val heroLife: Int, val heroMaxLife: Int, val heroShield: Int, val heroMaxShield: Int,
    val alive: Int, val total: Int,
    /** The exit is sealed while the zone's boss lives. */
    val sealed: Boolean = false,
    val fight: FightHud? = null,
    /** What the fight just won brought, rolled by the run's seed the moment it fell — the server's count follows. */
    val reward: Reward? = null,
    val slain: RolledMonster? = null,
    val report: FightReport? = null,
    /** What the death cost, by the rules' price; the server's answer stands. */
    val fall: Double? = null,
    val gold: Long = 0, val experience: Double = 0.0, val kills: Int = 0,
    val chestsLeft: Int = 0,
    val chest: Reward? = null,
    val fountainsLeft: Int = 0,
    /** The Vaal zone behind the portal the hero stands at. */
    val gate: VaalZone? = null,
    /** This run is a Vaal zone: no way out but its guardian or a death, and a death is not the map's end. */
    val vaal: Boolean = false,
    val heroMana: Int = 0, val heroMaxMana: Int = 0,
    val flasks: List<FlaskView?> = emptyList(),
    val crystal: CrystalView? = null,
    val crystalsLeft: Int = 0,
    val abyss: AbyssView? = null,
    val cracksLeft: Int = 0,
    /** The guardian is slain and not yet back. */
    val bossDown: Boolean = false,
    /** The autorun under way, and what it has gathered (3.2.0). */
    val auto: AutoHud? = null,
    val autoReward: Reward? = null,
    /** Events of the journal the server has not taken yet, the number of the oldest of them, and the ones it refused. */
    val pending: Int = 0,
    val applied: Int = 0,
    val rejected: Int = 0,
    /** The desecration on the hero, underfoot or trailing (3.4.0). */
    val desecration: DesecrationView? = null,
)

/** A desecration on the hero as the screen shows it: its kind, the lines it lays at this zone for this hero, and the trail left. */
data class DesecrationView(val kind: DesecrationKind, val lines: Map<String, Double>, val underfoot: Boolean, val trail: Double)

/** The Abyss as its sheet shows it: how many depths the crack leads down, how many are cleared, every depth's wave and hoard, and the share a fall keeps. */
data class AbyssView(val depth: Int, val cleared: Int, val open: Boolean, val depths: List<AbyssDepth>,
                     val hoard: Reward? = null, val fallen: Boolean = false) {
    val current: AbyssDepth? get() = depths.getOrNull(cleared - 1)
    val next: AbyssDepth? get() = if (cleared < depth) depths.getOrNull(cleared) else null
}

/** A crystal of essences as its sheet shows it, and what a Vaal orb on it did. */
data class CrystalView(val id: Int, val essences: List<String>, val guardian: String, val stronger: Boolean, val vaal: Boolean, val outcome: String? = null)

/** What the overlay asks of the run; applied at the start of the next step. */
sealed interface RunCommand {
    data object Continue : RunCommand
    data object Speed : RunCommand
    data object Leave : RunCommand
    /** Walk out of the fight: the monster gets its free swings first; before it began, simply walk away. */
    data object Retreat : RunCommand
    /** The fight begins: until then the pack is laid open to be studied. Mid-fight it ends a [Pause]. */
    data object Begin : RunCommand
    data object Pause : RunCommand
    /** A window over the map stops the world while it is open: `true` takes a hold, `false` gives one back. */
    data class Hold(val on: Boolean) : RunCommand
    data class Focus(val index: Int) : RunCommand
    data object DismissChest : RunCommand
    /** Stepping back from the gate undecided: the portal stays, and opens again when walked onto. */
    data object StepBack : RunCommand
    /** The gate is decided: [entered] the zone, or refused it — a refused zone closes for good. */
    data class ShutGate(val entered: Boolean) : RunCommand
    /** Back from the Vaal zone with [life] left, and the mana and flasks it left. */
    data class Returned(val life: Double, val pools: HeroPools? = null) : RunCommand
    /** The gear changed on the map: it lands between fights, and life keeps its share. */
    data class Regear(val gear: HeroGear) : RunCommand
    data class Cast(val slot: Int) : RunCommand
    data class Drink(val slot: Int) : RunCommand
    /** Takes on the guardian of the crystal the hero stands at. */
    data object Release : RunCommand
    /** A Vaal orb on the crystal the hero stands at: spent from the bag, the outcome rolled by the seed. */
    data object VaalCrystal : RunCommand
    /** Steps away from a crystal or a crack undecided. */
    data object StepOff : RunCommand
    /** At a crack of the Abyss: opens it, or goes a depth deeper from the sheet between depths. */
    data object Descend : RunCommand
    /** Takes the hoard of the depths cleared and leaves the Abyss. */
    data object TakeHoard : RunCommand
    /** Stops the autorun: the run goes on by hand from where it stands. */
    data object StopAuto : RunCommand
}

/**
 * One run of a zone: the world, the hero's life across it and the fights on the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the overlay reads [hud] and sends
 * [RunCommand]s. Nothing here talks to the server: every kill, chest, boss and descent is an event of the
 * [journal], its reward rolled at once by the run's seed — the same roll the server makes when the journal
 * reaches it. The hero's life carries from fight to fight and does not return while walking; mana comes back
 * on the road, flasks fill with kills.
 */
class ExpeditionRun(
    val index: ContentIndex,
    /** The zone walked: the location, or its Vaal zone. */
    val zone: Zone,
    run: Run,
    val journal: RunJournal,
    val world: ExpeditionWorld,
    build: HeroBuild,
    val rules: CombatRules,
    private val seed: Long,
    /** The map's summed effects, the atlas's share in them; a Vaal zone adds its own lines. */
    val mapEffects: Map<String, Double>,
    /** This run is the Vaal zone behind the portal. */
    val vaal: Boolean,
    startPools: HeroPools?,
    private val heroExperience: Double,
    private val heroLevel: Int,
    /** Vaal orbs at hand, the ones already spent on this run's journal counted out. */
    private val vaalOrbs: () -> Long,
    private var corruptionOpened: Boolean,
    private var vaalZone: VaalZone?,
    private var bossDown: Boolean,
    private val onRecorded: (RunEvent) -> Unit,
    private val onCleared: () -> Unit,
    private val onFallen: () -> Unit,
    /** Gear a reward brought, the moment it was rolled: for the gear sheet's «Новый лут». */
    private val onLoot: (List<com.sperance.exileforge.rules.roll.ItemInstance>) -> Unit = {},
    /** The autorun that drives this run instead of the stick (3.2.0); null walks by hand. */
    private var autopilot: AutoPilot? = null,
    /** The combat pet at work (3.5.0). */
    private val pet: Pet? = null,
) {
    /** The run as the rules roll it; it takes the Vaal zone's context once the portal opens. */
    var run: Run = run
        private set
    private val spawns get() = Spawns(index, run)
    var build: HeroBuild = build
        private set
    val stance: HeroStance get() = build.gear.stance
    var hero: Combatant = build.body
        private set
    @Volatile var stickX = 0.0
    @Volatile var stickY = 0.0
    private val commands = ConcurrentLinkedQueue<RunCommand>()
    private var phase = RunPhase.MAP
    private var life = startPools?.life?.coerceIn(0.0, hero.maxLife) ?: hero.maxLife
    val heroLife: Double get() = life
    private val kit: Loadout get() = build.gear.kit
    private var mana = startPools?.mana?.coerceIn(0.0, manaCap()) ?: manaCap()
    private var charges: List<Double> = kit.flasks.mapIndexed { i, flask -> flask?.let { startPools?.charges?.getOrNull(i)?.coerceIn(0.0, it.maxCharges) ?: it.maxCharges } ?: 0.0 }
    private var flaskLeft: List<Double> = kit.flasks.indices.map { startPools?.flaskLeft?.getOrNull(it) ?: 0.0 }
    private var rates: List<DraughtRate> = kit.flasks.indices.map { startPools?.rates?.getOrNull(it) ?: DraughtRate() }
    val pools: HeroPools get() = HeroPools(life, mana, charges, flaskLeft, rates)
    private var gate: VaalZone? = null
    private var crystal: CrystalSpot? = null
    private var crystalOutcome: String? = null
    private var rift: AbyssSpot? = null
    private var descent: Descent? = null
    private var abyssFight = false
    private var fightLevel = 0
    private var started = false
    private var paused = false
    private var holds = 0
    private var fights = 0
    private var speed = 1
    private var reward: Reward? = null
    /** What an autorun has gathered so far, fight by fight: its report at the end. */
    private var autoReward: Reward? = null
    private val desecration = index.campaign.desecration
    /** The pet as a fighter: its sheet at its level, what its role does; a new one each fight stands up whole. */
    private val ally: Ally? by lazy {
        pet?.let { p ->
            val pets = Menagerie(index)
            val kind = pets.species(p.species) ?: return@let null
            Ally(p.species, Combatant(pets.sheet(p), p.level, rules), kind.role == PetRole.TANK,
                if (kind.role == PetRole.SUPPORT) pets.supportHeal(p) else 0.0, index.pets.drawFire)
        }
    }
    /** The patch whose lines are on the hero: the one underfoot, or the last one stepped off within its trail. */
    private var desecratedBy: Desecrated? = null
    private var trailLeft = 0.0

    /** What a patch of [kind] lays on this hero at this zone: its lines, cut by the hero's own guard against desecration. */
    fun desecrationLines(kind: DesecrationKind): Map<String, Double> =
        desecration?.lines(kind, zone.level, hero.stats[DesecrationRule.GUARD] ?: 0.0).orEmpty()
    private fun desecrationLines(spot: Desecrated): Map<String, Double> = desecrationLines(spot.kind)

    /** The map's lines with the desecration on the hero over them. */
    private fun effects(): Map<String, Double> = desecratedBy?.let { MapEffects.sum(mapEffects, desecrationLines(it)) } ?: mapEffects

    /** A step on desecrated ground or off it: the patch's lines come on at once and stay for the trail after it. */
    private fun desecrate(dt: Double) {
        val rule = desecration ?: return
        val under = world.underfoot
        trailLeft = if (under != null) rule.trail else (trailLeft - dt).coerceAtLeast(0.0)
        val next = under ?: desecratedBy?.takeIf { trailLeft > 0 }
        if (next !== desecratedBy) { desecratedBy = next; regear(build.gear) }
        wound(dt)
    }

    /**
     * The ground's degeneration on the road (3.4.0): the fight burns it in its own beat, the walk did not, so a
     * patch laid its curses but never ate life. Off a fight it wounds to the last point: only a fight ends a run.
     */
    private fun wound(dt: Double) {
        val share = hero.lifeDegenShare
        if (share > 0 && life > 1) life = (life - hero.maxLife * share * dt).coerceAtLeast(1.0)
    }
    private var slain: RolledMonster? = null
    private var report: FightReport? = null
    /** The foes of the stage in the battle's order, each with the pack it walked with. */
    private var members: List<FightMember> = emptyList()
    /** Every pack the fight drew in, one a stage: the engaged one first, then the rest by their distance to it. */
    private var fightAgents: List<MonsterAgent> = emptyList()
    /** The stage under way, from 1 (3.28.0): the pack of [fightAgents] it fights. */
    private var stage = 0
    /** Seconds of the pause before a later stage begins on its own; null outside that pause. */
    private var interlude: Double? = null
    /** What the stages already won leave to the one report: every foe's log, and the seconds they took. */
    private var stageHits: List<PackHit> = emptyList()
    private var stageTime = 0.0
    /** The strongest of every stage: it stands for the whole fight in the report. */
    private var fightStrongest: RolledMonster? = null
    /** The dice stream of the stage's battle: a draught in the pause builds the battle again on the same dice. */
    private var fightStream = 0L
    private var reported = 0
    private var fall: Double? = null
    private var gold = 0L
    private var experience = 0.0
    private var kills = 0
    private var chest: Reward? = null
    private var fightAgent: MonsterAgent? = null
    private var pendingGear: RunCommand.Regear? = null
    private val waves get() = AbyssWaves(index, run)
    private val abyssRule get() = index.campaign.abyss

    private fun manaCap(): Double = hero.maxMana * (1 - kit.reserved(hero) / 100)

    private fun regear(gear: HeroGear) {
        val next = HeroBuild(gear, effects(), rules)
        val before = hero
        build = next
        rebody()
        life = if (before.maxLife > 0) life / before.maxLife * hero.maxLife else hero.maxLife
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
        world.regear(ExpeditionWorld.heroSpeed(hero.stats), ExpeditionWorld.lightRadius(hero.stats, zone.light))
    }

    var fight: Battle? = null
        private set

    private val state = MutableStateFlow(snapshot())
    val hud: StateFlow<RunHud> = state.asStateFlow()

    fun send(command: RunCommand) { commands.add(command) }

    fun update(dt: Double) {
        while (true) apply(commands.poll() ?: break)
        if (holds == 0) when (phase) {
            RunPhase.MAP -> autopilot?.let { drive(it, dt) } ?: walk(dt)
            RunPhase.FIGHT -> play(dt)
            else -> Unit
        }
        state.value = snapshot()
    }

    /** One event of the journal, and the listener told: the reward it earns is the caller's to roll. */
    private fun record(kind: RunEventKind, i: Int = 0, m: Int = 0, index: Int = 0, depth: Int = 0, fallen: Boolean = false): RunEvent? =
        journal.record(kind, i, m, index, depth, fallen, vaal)?.also(onRecorded)

    private fun earn(reward: Reward?) {
        val gained = reward ?: return
        if (autopilot != null) autoReward = autoReward?.plus(gained) ?: gained
        gold += gained.gold
        experience += gained.experience
        if (gained.equipment.isNotEmpty()) onLoot(gained.equipment)
    }

    private fun apply(command: RunCommand) {
        when (command) {
            RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2
            RunCommand.StopAuto -> autopilot = null
            RunCommand.Leave -> if (phase == RunPhase.MAP || phase == RunPhase.DEAD || phase == RunPhase.CLEARED) phase = RunPhase.LEFT
            RunCommand.Retreat -> if (abyssFight) Unit else if (fight != null && !started) walkAway() else { paused = false; fight?.retreat() }
            RunCommand.Begin -> if (fight != null) { started = true; paused = false; interlude = null }
            RunCommand.Pause -> if (fight != null && started && fight?.outcome == null) paused = !paused
            is RunCommand.Focus -> fight?.focus(command.index)
            is RunCommand.Hold -> holds = (holds + if (command.on) 1 else -1).coerceAtLeast(0)
            RunCommand.Continue -> when (phase) {
                RunPhase.LOOT -> { phase = RunPhase.MAP; reward = null; slain = null; report = null }
                RunPhase.DEAD, RunPhase.CLEARED -> phase = RunPhase.LEFT
                else -> Unit
            }
            // In the pause between stages the stage has not begun: the kit changes now, and its battle is drawn again.
            is RunCommand.Regear -> if (phase == RunPhase.FIGHT && interlude != null && !started) { pendingGear = null; regear(command.gear); fight = battle() }
                else if (phase == RunPhase.FIGHT) pendingGear = command else regear(command.gear)
            RunCommand.DismissChest -> chest = null
            RunCommand.StepBack -> if (phase == RunPhase.GATE) closeGate()
            is RunCommand.ShutGate -> {
                if (!command.entered && vaalZone != null) { record(RunEventKind.VAAL_LEAVE); vaalZone = null; corruptionOpened = true }
                world.closePortal(); closeGate()
            }
            is RunCommand.Returned -> {
                life = command.life.coerceIn(0.0, hero.maxLife)
                command.pools?.let { mana = it.mana.coerceIn(0.0, manaCap()); charges = it.charges.ifEmpty { charges }; flaskLeft = it.flaskLeft.ifEmpty { flaskLeft }; rates = it.rates.ifEmpty { rates }; rebody() }
                // The zone is closed either way: its guardian fell, or the hero did.
                vaalZone = null; corruptionOpened = true
                phase = RunPhase.MAP
            }
            is RunCommand.Cast -> fight?.useSkill(command.slot)
            // In the pause between stages a draught is drunk as on the road, and the stage's battle takes the pools it leaves.
            is RunCommand.Drink -> if (phase == RunPhase.FIGHT && interlude != null && !started) { drinkOnMap(command.slot); fight = battle() }
                else if (phase == RunPhase.FIGHT) fight?.useFlask(command.slot)
                else if (phase == RunPhase.MAP || phase == RunPhase.CRYSTAL || phase == RunPhase.ABYSS) drinkOnMap(command.slot)
            RunCommand.Release -> if (phase == RunPhase.CRYSTAL) release()
            RunCommand.VaalCrystal -> crystal?.takeIf { phase == RunPhase.CRYSTAL && !it.crystal.vaal && vaalOrbs() >= 1 }?.let { spot ->
                val place = world.standingCrystals.indexOf(spot)
                if (record(RunEventKind.CRYSTAL_VAAL, index = place) != null) {
                    val (outcome, changed) = run.crystalVaal(place, spot.crystal)
                    spot.crystal = changed
                    crystalOutcome = outcome
                }
            }
            RunCommand.StepOff -> when {
                phase == RunPhase.CRYSTAL -> closeCrystal()
                // A crack is left unopened, or once its hoard is in — never mid-descent with the hoard at stake.
                phase == RunPhase.ABYSS && (descent == null || descent?.hoard != null) -> closeRift()
            }
            RunCommand.Descend -> if (phase == RunPhase.ABYSS) descend()
            RunCommand.TakeHoard -> descent?.takeIf { phase == RunPhase.ABYSS && it.cleared > 0 && it.hoard == null }?.let { take(it, fallen = false) }
        }
    }

    // ==================== The Abyss ====================

    /** A descent under way: its crack, how many depths it leads down, how many are cleared, and the fights of the wave left. */
    private class Descent(val spot: AbyssSpot, val depth: Int) {
        var cleared = 0
        var level = 0
        var fights: List<List<RolledMonster>> = emptyList()
        var hoard: Reward? = null
        var fallen = false
    }

    /** «Спуститься»: the crack is opened — its event recorded — and the first wave rises; between depths, the next. */
    private fun descend() {
        val spot = rift ?: return
        val rule = abyssRule ?: return
        val current = descent
        if (current == null) {
            val place = world.standingCracks.indexOf(spot)
            record(RunEventKind.ABYSS_OPEN, index = place) ?: return
            spot.opened = true
            val depth = AbyssRifts(index).depth(rule, spot.depth, mapEffects[MapStats.ABYSS_DEPTH] ?: 0.0)
            Descent(spot, depth).also { descent = it; wave(it, 1) }
        } else if (current.hoard == null && current.cleared < current.depth) wave(current, current.cleared + 1)
    }

    private fun wave(current: Descent, depth: Int) {
        val rule = abyssRule ?: return
        current.level = zone.level + (rule.waves.getOrNull(depth - 1)?.level ?: 0)
        current.fights = waves.wave(rule, depth, current.spot.id, zone, mapEffects, run.context.extraRareMods)
        nextFight(current)
    }

    private fun nextFight(current: Descent) {
        val group = current.fights.firstOrNull() ?: return
        current.fights = current.fights.drop(1)
        engage(MonsterAgent(ABYSS_AGENT - current.spot.id, group, current.spot.cell.x + 0.5, current.spot.cell.y + 0.5), current.level, abyssal = true)
    }

    /** The descent is over: the hoard of the depths cleared — whole, or what a fall leaves of it. */
    private fun take(current: Descent, fallen: Boolean) {
        current.fallen = fallen
        record(RunEventKind.ABYSS_CLAIM, depth = current.cleared, fallen = fallen) ?: return
        // A fall in the Abyss burns the whole hoard (server 1.2.0); the hoard is still counted, as the server counts it
        current.hoard = run.hoard(current.cleared, if (fallen) 0.0 else 1.0).also(::earn)
    }

    private fun closeRift() {
        rift = null; descent = null
        if (phase == RunPhase.ABYSS) phase = RunPhase.MAP
    }

    private fun closeGate() {
        gate = null
        if (phase == RunPhase.GATE) phase = RunPhase.MAP
    }

    private fun closeCrystal() {
        crystal = null; crystalOutcome = null
        if (phase == RunPhase.CRYSTAL) phase = RunPhase.MAP
    }

    /**
     * Turning away before the stage began: nothing of it was struck, and the packs still standing stay calm a while;
     * the packs of the stages already won stay dead.
     */
    private fun walkAway() {
        fightAgents.filter { it.crystal == null && it.alive }.forEach(world::retreatFrom)
        endFight()
        // No report follows: the stages already won must not bring their loot to the next fight's screen.
        reward = null; slain = null
        phase = RunPhase.MAP
    }

    /** A draught on the map: what it gives back comes at once, and what it lays on runs as the hero walks. */
    private fun drinkOnMap(slot: Int) {
        val flask = kit.flasks.getOrNull(slot) ?: return
        if ((flaskLeft.getOrNull(slot) ?: 0.0) > 0 || (charges.getOrNull(slot) ?: 0.0) + 1e-9 < flask.perUse(hero)) return
        val draught = flask.draught(hero, life, manaCap())
        charges = charges.toMutableList().also { it[slot] = if (flask.usesAll) 0.0 else (it[slot] - flask.perUse(hero)).coerceAtLeast(0.0) }
        flaskLeft = flaskLeft.toMutableList().also { it[slot] = draught.duration }
        rates = rates.toMutableList().also { it[slot] = DraughtRate(draught.lifeRate, draught.manaRate) }
        rebody()
        life = (life + draught.life).coerceAtMost(hero.maxLife)
        mana = (mana + draught.mana).coerceAtMost(manaCap())
    }

    private fun walk(dt: Double) {
        recover(dt)
        desecrate(dt)
        val (x, y) = ExpeditionWorld.screenToWorld(stickX, stickY)
        when (val event = world.step(dt, x, y)) {
            is WorldEvent.Encounter -> engage(event.agent)
            WorldEvent.Exit -> exit()
            is WorldEvent.Opened -> {
                chest = if (record(RunEventKind.CHEST, index = event.chest.id) != null) run.chest().also(::earn) else null
            }
            is WorldEvent.Drank -> {
                life = (life + hero.maxLife * event.fountain.heal / 100).coerceAtMost(hero.maxLife)
                mana = (mana + manaCap() * event.fountain.heal / 100).coerceAtMost(manaCap())
                charges = kit.flasks.map { it?.maxCharges ?: 0.0 }
            }
            WorldEvent.Portal -> openGate()
            is WorldEvent.Crystal -> { phase = RunPhase.CRYSTAL; crystal = event.spot; crystalOutcome = null }
            is WorldEvent.Abyss -> { phase = RunPhase.ABYSS; rift = event.spot; descent = null }
            null -> Unit
        }
    }

    /** The way out: the Vaal zone's exit leads back to the map; the zone's own records the leaving, the boss passed. */
    private fun exit() {
        if (!vaal) record(RunEventKind.LEAVE)
        phase = RunPhase.CLEARED; onCleared()
    }

    /**
     * The autorun's beat (3.2.0): mana and draughts run as on the road, and after a short rest the next step
     * is taken — a fight begins at once, a chest opens, a guardian stands up; a crack or the portal stops the
     * run for the player's word, and the way out ends it.
     */
    private fun drive(pilot: AutoPilot, dt: Double) {
        recover(dt)
        pilot.rest -= dt * speed
        if (pilot.rest > 0) return
        pilot.rest = AutoPilot.BEAT
        while (phase == RunPhase.MAP) when (val step = pilot.next()) {
            null -> { autopilot = null; return }
            is AutoStep.Wave -> Unit
            is AutoStep.Fight -> if (step.agent.alive && step.agent.standing.isNotEmpty()) { engage(step.agent); started = true; return }
            is AutoStep.OpenChest -> if (!step.chest.opened) {
                step.chest.opened = true
                if (record(RunEventKind.CHEST, index = step.chest.id) != null) earn(run.chest())
                return
            }
            is AutoStep.Guardian -> if (!step.spot.freed) { crystal = step.spot; release(); started = true; return }
            is AutoStep.Rift -> if (!step.spot.opened) { phase = RunPhase.ABYSS; rift = step.spot; descent = null }
            AutoStep.Portal -> if (world.portal != null) openGate()
            AutoStep.Boss -> world.boss?.takeIf { it.alive }?.let { engage(it); started = true; return }
            AutoStep.Exit -> exit()
        }
    }

    /** Mana back on the road and the draughts still running, over [dt] seconds off the fight. */
    private fun recover(dt: Double) {
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
    }

    /** The portal opens its gate: the Vaal zone behind it is rolled by the seed once, and the run takes its context. */
    private fun openGate() {
        val zone = vaalZone ?: run {
            if (corruptionOpened || record(RunEventKind.VAAL_OPEN) == null) { world.closePortal(); return }
            run.vaalZone().also { rolled -> vaalZone = rolled; run = Run(index, run.zone, run.seed, run.context.copy(vaal = rolled)) }
        }
        gate = zone
        phase = RunPhase.GATE
    }

    /**
     * A fight with [agent]'s pack still standing, all at once, at [level] — the zone's, or a depth's of the Abyss.
     * On the map the packs standing close by are drawn in (3.26.0) and, since 3.28.0, fought one pack a stage — the
     * engaged one first, then the rest by their distance to it; the boss and a guardian always fight alone.
     */
    private fun engage(agent: MonsterAgent, level: Int = zone.level, abyssal: Boolean = false) {
        fightAgents = if (abyssal) listOf(agent) else world.gathered(agent)
        fightStrongest = fightAgents.flatMap { pack -> pack.standing.map { pack.pack[it] } }.maxByOrNull { it.rarity.ordinal }
        stageHits = emptyList()
        stageTime = 0.0
        fightAgent = agent
        abyssFight = abyssal
        fightLevel = level
        begin(1)
    }

    /**
     * Stage [number] of the fight stands up, the hero as the last one left them — life, mana, charges and the draughts
     * running, no ailment. A later stage waits [STAGE_PAUSE] seconds, or the player's word, and an autorun not at all.
     */
    private fun begin(number: Int) {
        stage = number
        val pack = fightAgents[number - 1]
        members = pack.standing.map { FightMember(pack, it) }
        reported = 0
        fightStream = (fights++).toLong()
        fight = battle()
        // An autorun goes straight on; the player gets the pause.
        started = number > 1 && autopilot != null
        interlude = STAGE_PAUSE.takeIf { number > 1 && !started }
        paused = false
        phase = RunPhase.FIGHT
    }

    /** The stage's battle on the hero's pools now, at the fight's level, on its own dice. */
    private fun battle(): Battle = Battle(hero, members.map { member ->
        val monster = member.monster
        Foe(Combatant(monster.stats, fightLevel, rules), monster.ranged, monster.rarity, monster.skills.mapNotNull(this.index.skills.monsterByCode::get))
    }, rules, life, Random(Streams.mix(seed, FIGHT_STREAM, fightStream)), stance, kit = kit, model = build, pools = pools,
        percent = build.gear.percent, ally = ally)

    /** The fight is over, whichever way: nothing of it is held any longer. */
    private fun endFight() {
        fight = null
        fightAgent = null
        fightAgents = emptyList()
        members = emptyList()
        stage = 0
        interlude = null
        stageHits = emptyList()
        stageTime = 0.0
        fightStrongest = null
    }

    /** «Освободить»: the crystal's guardian stands up — the zone's monster, rare, with the lines of the essences it guards — and the fight begins. */
    private fun release() {
        val spot = crystal ?: return
        val extra = MapEffects.guardianBuffs(spot.crystal.stronger, index.essences.crystals.stronger, mapEffects)
        val guardian = spawns.crystalGuardian(zone, spot.crystal, spot.id, MapEffects.buffs(mapEffects), extra) ?: return
        crystal = null
        crystalOutcome = null
        engage(MonsterAgent(-1 - spot.id, listOf(guardian), spot.cell.x + 0.5, spot.cell.y + 0.5, crystal = spot.id))
    }

    /** A foe of the fight fell: the event and its reward, by what it was. */
    private fun fell(agent: MonsterAgent, member: Int) {
        kills++
        if (abyssFight) return
        val spot = agent.crystal?.let { id -> world.crystals.firstOrNull { it.id == id } }
        val gained = when {
            spot != null -> {
                val place = world.standingCrystals.indexOf(spot)
                record(RunEventKind.CRYSTAL, index = place)?.let { run.crystal(spot.crystal) }.also { spot.freed = true }
            }
            agent === world.boss -> if (vaal) record(RunEventKind.CORRUPT)?.let { run.corrupt() }
                else record(RunEventKind.BOSS)?.let { bossDown = true; run.boss() }
            else -> record(RunEventKind.KILL, i = agent.id, m = member)?.let { run.kill(agent.id, member, vaal) }
        } ?: return
        earn(gained)
        reward = reward?.plus(gained) ?: gained
    }

    private fun play(dt: Double) {
        val battle = fight ?: return
        fightAgent ?: return
        // The pause between stages runs out by itself, at the wall clock's pace.
        if (!started) interlude?.let { left -> if (left > dt) interlude = left - dt else { interlude = null; started = true } }
        if (!started || paused) return
        battle.advance(dt * speed)
        // Every foe is a kill of its own, recorded the moment it falls, the fight still going.
        while (reported < battle.fallen.size) {
            val member = members[battle.fallen[reported++]]
            member.agent.fallen += member.index
            fell(member.agent, member.index)
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
        val pack = stageHits + members.mapIndexed { index, member -> PackHit(member.monster, battle.events.filter { it.foe == index }, battle.duration) }
        val duration = stageTime + battle.duration
        // A stage won with packs still waiting: the next one stands up, and the report waits for the last.
        if (outcome == Outcome.WIN && stage < fightAgents.size) {
            fightAgents[stage - 1].alive = false
            stageHits = pack
            stageTime = duration
            pendingGear?.let { regear(it.gear) }
            pendingGear = null
            begin(stage + 1)
            return
        }
        val leader = fightStrongest ?: fightLeader()
        val down = descent?.takeIf { abyssFight }
        when (outcome) {
            Outcome.WIN -> {
                fightAgents.forEach { it.alive = false }
                if (down != null) { report = null; phase = RunPhase.ABYSS }
                // An autorun goes on without the report: what the fight brought is in its tally already
                else if (autopilot != null) { reward = null; slain = null; report = null; phase = RunPhase.MAP }
                else {
                    slain = leader
                    report = FightReport(leader, Outcome.WIN, pack, duration)
                    phase = RunPhase.LOOT
                }
            }
            Outcome.LOSS -> {
                report = FightReport(leader, Outcome.LOSS, pack, duration)
                life = 0.0
                autopilot = null
                phase = RunPhase.DEAD
                // A fall in the Abyss burns its hoard, but for the atlas's share; then the zone's own price.
                down?.let { take(it, fallen = true) }
                if (vaal) record(RunEventKind.VAAL_LEAVE) else record(RunEventKind.FALL)?.let { fall = deathLoss() }
                onFallen()
            }
            // Nothing already looted is lost, but there is no report for a fight cut short: the packs of the stages won stay dead,
            // the current one and those still waiting go back to their places.
            Outcome.RETREAT -> {
                // Walking out of a fight takes the run back into the player's hands
                autopilot = null
                fightAgents.filter { it.id >= 0 && it.alive }.forEach(world::retreatFrom)
                report = null; reward = null; slain = null
                if (down != null) { phase = RunPhase.ABYSS; take(down, fallen = true) } else phase = RunPhase.MAP
            }
        }
        endFight()
        abyssFight = false
        if (phase != RunPhase.DEAD) pendingGear?.let { regear(it.gear) }
        pendingGear = null
        if (outcome == Outcome.WIN && down != null) { if (down.fights.isNotEmpty()) nextFight(down) else down.cleared++ }
    }

    /** What the death costs by the rules: a share of the level's experience, never the level. */
    private fun deathLoss(): Double {
        val classes = index.classes
        val total = heroExperience + experience
        val level = classes.levelOf(total).coerceAtLeast(heroLevel)
        return LootRoller(index).deathLoss(rules.death, zone.level, total, classes.threshold(level) ?: 0.0, classes.nextThreshold(level))
    }

    private fun snapshot(): RunHud {
        val battle = fight
        return RunHud(
            phase = phase, mapCode = zone.code,
            heroLife = (battle?.heroLife ?: life).roundToInt(), heroMaxLife = hero.maxLife.roundToInt(),
            heroShield = (battle?.heroFighter?.shield ?: hero.maxShield).roundToInt(), heroMaxShield = hero.maxShield.roundToInt(),
            alive = world.alive, total = world.total, sealed = world.sealed,
            fight = battle?.takeIf { fightAgent != null }?.let(::fightHud),
            reward = reward, slain = slain, report = report,
            fall = fall,
            gold = gold, experience = experience, kills = kills,
            chestsLeft = world.chests.count { !it.opened }, chest = chest,
            fountainsLeft = world.fountains.count { !it.used },
            gate = gate, vaal = vaal,
            heroMana = (battle?.heroMana ?: mana).roundToInt(), heroMaxMana = (battle?.manaCap() ?: manaCap()).roundToInt(),
            flasks = battle?.flaskViews() ?: mapFlasks(),
            crystal = crystal?.let { CrystalView(it.id, it.crystal.essences, it.crystal.guardian, it.crystal.stronger, it.crystal.vaal, crystalOutcome) },
            crystalsLeft = world.standingCrystals.size,
            abyss = abyssView(), cracksLeft = world.standingCracks.size,
            bossDown = bossDown,
            auto = autopilot?.let { AutoHud(it.wave, it.waves) }, autoReward = autoReward,
            pending = journal.pending.size, applied = journal.applied, rejected = journal.rejected.size,
            desecration = desecratedBy?.let { DesecrationView(it.kind, desecrationLines(it), world.underfoot === it, trailLeft) },
        )
    }

    private fun abyssView(): AbyssView? {
        val spot = rift ?: return null
        val rule = abyssRule ?: return null
        val down = descent
        return AbyssView(down?.depth ?: spot.depth, down?.cleared ?: 0, down != null, waves.depths(rule, zone),
            down?.hoard, down?.fallen == true)
    }

    private fun mapFlasks(): List<FlaskView?> = kit.flasks.mapIndexed { i, flask ->
        flask?.let {
            val left = flaskLeft.getOrNull(i) ?: 0.0
            FlaskView(i, it.code, it.kind, (charges.getOrNull(i) ?: 0.0).toInt(), it.maxCharges.toInt(), kotlin.math.ceil(it.perUse(hero) - 1e-9).toInt(),
                if (left > 0) (left / it.duration(hero)).toFloat().coerceIn(0f, 1f) else 0f, it.condition)
        }
    }

    /** The strongest of the stage: its portrait and its name head the pack fought now. */
    private fun fightLeader(): RolledMonster = members.maxBy { it.monster.rarity.ordinal }.monster

    private fun fightHud(battle: Battle): FightHud {
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
            FoeView(f.index, members[f.index].monster, f.life.roundToInt(), f.body.maxLife.roundToInt(), f.shield.roundToInt(), f.body.maxShield.roundToInt(),
                battle.swing(f), ailments(f), f.held, f.alive, battle.reachable(f.index), f.body.taunt, battle.effects(f),
                f.mana.roundToInt(), f.body.maxMana.roundToInt(), back = f.ranged)
        }
        return FightHud(
            ally = battle.allyFighter?.let { f -> AllyView(battle.ally!!.code, f.life.roundToInt(), f.body.maxLife.roundToInt(), f.alive) },
            leader = fightLeader(), foes = foes,
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
            stage = stage, stages = fightAgents.size, interlude = interlude,
        )
    }

    companion object {
        /** How long the fight's last blow hangs before the scene moves on. */
        const val AFTERMATH = 0.8
        const val HIT_LIFETIME = 1.0
        /** Seconds between the stages of a fight before the next begins on its own. */
        const val STAGE_PAUSE = 3.0
        /** The agents of the Abyss's waves are numbered down from here, out of the way of the map's and the crystals'. */
        private const val ABYSS_AGENT = -10_000
        private val FIGHT_STREAM = "fight".hashCode().toLong()

        /**
         * A run of [location] as the seed rolls it — or, [vaal], of the Vaal zone behind its portal, entered with
         * the pools the map left. [campaign] is the hero's campaign as the server holds it after the entry: the
         * windows of chests, crystals and cracks, the boss's return, the map and the Vaal zone rolled; [killed],
         * what of it the server already counted when the run is entered again.
         */
        fun start(
            index: ContentIndex, location: Zone, run: Run, journal: RunJournal, gear: HeroGear, campaign: CampaignState, now: Long,
            heroExperience: Double, heroLevel: Int, vaalOrbs: () -> Long, onRecorded: (RunEvent) -> Unit = {},
            vaal: Boolean = false, startPools: HeroPools? = null, onCleared: () -> Unit = {}, onFallen: () -> Unit = {},
            onLoot: (List<com.sperance.exileforge.rules.roll.ItemInstance>) -> Unit = {},
            /** Tokens `i*[Run.PACK_SLOTS]+m` the server already counts as killed: a run entered again keeps its dead dead. */
            killed: Collection<Int> = emptyList(),
            /** An autorun instead of the stick (3.2.0). */
            auto: AutoPlan? = null,
            /** The combat pet at work (3.5.0): it fights every fight at the hero's side. */
            pet: Pet? = null,
        ): ExpeditionRun {
            val zone = if (vaal) VaalZones.zone(location) ?: location else location
            val context = run.context
            val base = AtlasEffects.map(context.active?.effects.orEmpty(), context.atlas)
            // The act's resistance penalty (3.18.0, server 1.16.0) rides with the map's own "less resistances".
            val penalty = index.campaign.resistPenalty(location.code, onMap = context.active != null)
            val acted = if (penalty > 0) MapEffects.sum(base, mapOf(MapStats.HERO_RESIST to penalty)) else base
            val effects = if (vaal) MapEffects.sum(acted, context.vaal?.effects.orEmpty()) else acted
            val rules = index.campaign.combat
            val build = HeroBuild(gear, effects, rules)
            val stats = build.body.stats
            val spawns = Spawns(index, run)
            val buffs = MapEffects.buffs(effects)
            val packs = spawns.packs(vaal, buffs)
            val boss = spawns.boss(zone, buffs, MapEffects.bossBuffs(effects))
            val bossDown = !vaal && campaign.bossDown(location.code, now)
            val vaalZone = campaign.vaalZone?.takeIf { it.mapCode == location.code }
            val portal = !vaal && !campaign.corruptionOpened && (vaalZone != null || spawns.portal(location, AtlasEffects.portalChance(index, context.atlas)))
            val world = ExpeditionWorld.create(zone, packs, stats, if (vaal) run.seed xor VAAL_SALT else run.seed, boss, portal)
            if (bossDown) world.bossAbsent()
            world.restore(killed, Run.PACK_SLOTS)
            val fountains = AtlasEffects.fountains(index.campaign.fountains, context.atlas)
            val extraFountains = MapEffects.fountains(effects)
            world.placeFountains(fountains.count.getOrElse(0) { 0 } + extraFountains, fountains.count.getOrElse(1) { fountains.count.getOrElse(0) { 0 } } + extraFountains, fountains.heal)
            index.campaign.desecration?.let { rule -> world.placeDesecration(rule.roll(zone.level, if (vaal) run.seed xor VAAL_SALT else run.seed), rule.radius) }
            if (!vaal) {
                world.placeChests(campaign.chests[location.code]?.left ?: 0)
                world.placeCrystals(campaign.crystals[location.code]?.crystals.orEmpty())
                world.placeCracks(campaign.abyss[location.code]?.cracks.orEmpty())
            }
            val pilot = auto?.let { AutoPilot.of(world, it, if (vaal) run.seed xor VAAL_SALT else run.seed, bossStands = world.boss?.alive == true) }
            return ExpeditionRun(index, zone, run, journal, world, build, rules, run.seed, effects, vaal, startPools, heroExperience, heroLevel, vaalOrbs,
                campaign.corruptionOpened, vaalZone, bossDown, onRecorded, onCleared, onFallen, onLoot, pilot, pet)
        }

        private const val VAAL_SALT = 0x5661616C5A6F6E65L
    }
}

/** One foe of a fight: the pack it walked with and its place there. */
internal class FightMember(val agent: MonsterAgent, val index: Int) {
    val monster: RolledMonster get() = agent.pack[index]
}
