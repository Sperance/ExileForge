package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.LoneWolfRule
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.content.EssenceBook
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
    /** Its place on the field, of [FoeWindow.SIZE]; -1 while it waits its turn or once the next took its place. */
    val place: Int = index,
    /** Still in line: not in the fight yet, but still to be beaten. */
    val waiting: Boolean = false,
) {
    /** Its card is on the field: it fights there, or fell there and nobody stepped in yet. */
    val onField: Boolean get() = place >= 0
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
    /** [heroMaxMana] is the mana the auras leave free; [heroReserved] what they hold past it. */
    val heroMana: Int = 0, val heroMaxMana: Int = 0, val heroReserved: Int = 0,
    val skills: List<SkillView?> = emptyList(),
    val flasks: List<FlaskView?> = emptyList(),
    val heroEffects: List<EffectView> = emptyList(),
    /** The hero's frenzy, power and endurance charges (3.33.0), a counter per kind held. */
    val heroCharges: List<ChargeView> = emptyList(),
    val heroBarrier: Int = 0,
    /** The level the foes stand at: a depth of the Abyss stands deeper than its zone. */
    val level: Int = 0,
    /** Whether the hero may walk out: the Abyss lets nobody go mid-wave. */
    val escape: Boolean = true,
    /** The stage of a gathered fight (3.28.0), from 1, of [stages]: small packs in a row share one (3.70.0). */
    val stage: Int = 1, val stages: Int = 1,
    /** Seconds left of the pause before this stage begins by itself; null when there is none. */
    val interlude: Double? = null,
) {
    val scouting: Boolean get() = outcome == null && (!started || paused)
    /** The foes on the field, by their places: at most [FoeWindow.SIZE] cards whatever the pack. */
    val field: List<FoeView> get() = foes.filter { it.onField }.sortedBy { it.place }
    /** How many of the whole pack are still to be beaten: those standing and those waiting their turn. */
    val standing: Int get() = foes.count { it.alive || it.waiting }
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
    /**
     * What the fight just won brought, as the server's answers bring it (server 1.30.0): nothing is rolled here,
     * and [rewardAwaiting] of its events are still to be answered — the report opens at once and fills in.
     */
    val reward: Reward? = null,
    val rewardAwaiting: Int = 0,
    val slain: RolledMonster? = null,
    val report: FightReport? = null,
    /** What the death cost, by the rules' price until the server's answer replaces it. */
    val fall: Double? = null,
    /** The run's gold and experience as the server's answers granted them; [awaiting] of its rewarding events are not answered yet. */
    val gold: Long = 0, val experience: Double = 0.0, val kills: Int = 0,
    /** The run's figures (3.47.0) and the blows that ended it. */
    val summary: RunSummary = RunSummary(), val recap: List<DeathHit> = emptyList(),
    val awaiting: Int = 0,
    val chestsLeft: Int = 0,
    val chest: Reward? = null,
    val chestAwaiting: Boolean = false,
    val fountainsLeft: Int = 0,
    /** The Vaal zone behind the portal the hero stands at. */
    val gate: VaalZone? = null,
    /** This run is a Vaal zone: no way out but its guardian or a death, and a death is not the map's end. */
    val vaal: Boolean = false,
    /** [heroMaxMana] is the mana the auras leave free; [heroReserved] what they hold past it. */
    val heroMana: Int = 0, val heroMaxMana: Int = 0, val heroReserved: Int = 0,
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
    val autoAwaiting: Int = 0,
    /** Events of the journal the server has not taken yet, the number of the oldest of them, and the ones it refused. */
    val pending: Int = 0,
    val applied: Int = 0,
    val rejected: Int = 0,
    /** What the map came to so far: its summary before the camp. */
    val tally: MapTally = MapTally(),
)

/** The Abyss as its sheet shows it: how many depths the crack leads down, how many are cleared, every depth's wave and hoard, and the share a fall keeps. */
data class AbyssView(val depth: Int, val cleared: Int, val open: Boolean, val depths: List<AbyssDepth>,
                     val hoard: Reward? = null, val fallen: Boolean = false, val hoardAwaiting: Boolean = false) {
    val current: AbyssDepth? get() = depths.getOrNull(cleared - 1)
    val next: AbyssDepth? get() = if (cleared < depth) depths.getOrNull(cleared) else null
}

/** A crystal of essences as its sheet shows it, what a Vaal orb on it did, and whether that orb's outcome is still the server's to tell. */
data class CrystalView(val id: Int, val essences: List<String>, val guardian: String, val stronger: Boolean, val vaal: Boolean, val outcome: String? = null,
                       val awaiting: Boolean = false)

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
    data class Returned(val life: Double, val pools: HeroPools? = null, val zone: ZoneShare? = null) : RunCommand
    /** The gear changed on the map: it lands between fights, and life keeps its share. */
    data class Regear(val gear: HeroGear) : RunCommand
    data class Cast(val slot: Int) : RunCommand
    data class Drink(val slot: Int) : RunCommand
    /** Takes on the guardian of the crystal the hero stands at. */
    data object Release : RunCommand
    /** A Vaal orb on the crystal the hero stands at: spent from the bag, the outcome told by the server's answer. */
    data object VaalCrystal : RunCommand
    /** Steps away from a crystal or a crack undecided. */
    data object StepOff : RunCommand
    /** At a crack of the Abyss: opens it, or goes a depth deeper from the sheet between depths. */
    data object Descend : RunCommand
    /** Takes the hoard of the depths cleared and leaves the Abyss. */
    data object TakeHoard : RunCommand
    /** Stops the autorun: the run goes on by hand from where it stands. */
    data object StopAuto : RunCommand
    /**
     * The server answered the journal up to [applied] (server 1.30.0): what each accepted event brought, by its
     * number, the numbers it [rejected], the experience a fall in the batch [lost], and the crystals Vaal orbs
     * changed, by event number (server 1.30.2).
     */
    data class Settled(val applied: Int, val rewards: Map<Int, Reward> = emptyMap(), val rejected: List<Int> = emptyList(), val lost: Double? = null,
                       val crystals: Map<Int, Crystal> = emptyMap()) : RunCommand
    /** The hero's campaign as the server holds it now: the Vaal zone a portal opened and a crystal a Vaal orb changed are read from it. */
    data class Campaign(val state: CampaignState) : RunCommand
}

/**
 * One run of a zone: the world, the hero's life across it and the fights on the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the overlay reads [hud] and sends
 * [RunCommand]s. Nothing here talks to the server: every kill, chest, boss and descent is an event of the
 * [journal]. Its reward is the server's alone (1.30.0): it comes back with the answer as [RunCommand.Settled],
 * and until then the screens say it is on its way — as do the Vaal zone behind a portal and a crystal's Vaal
 * orb, read from the campaign the answer brings ([RunCommand.Campaign]). The hero's life carries from fight to
 * fight and does not return while walking; mana comes back on the road, flasks fill with kills.
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
    /** How far the server has answered the journal, what it granted by event number, and what it refused (server 1.30.0). */
    private var answered = 0
    private val earned = HashMap<Int, Reward>()
    private val refused = HashSet<Int>()
    /** The rewarding events this run recorded, and what the answers brought for them all told. */
    private val mine = HashSet<Int>()
    private var granted = Reward.NONE
    /** The events of the fight on the report, and what they brought so far; null before the fight's first kill. */
    private val fightEvents = mutableListOf<Int>()
    private var reward: Reward? = null
    /** What an autorun has gathered, fight by fight: its report at the end. */
    private val autoEvents = HashSet<Int>()
    private var autoReward: Reward? = null
    /** The chest on screen, by its event. */
    private var chestEvent: Int? = null
    /** The hero's campaign as the last answer brought it. */
    private var campaign: CampaignState? = null
    /** The portal's opening, until the server tells the Vaal zone behind it. */
    private var gateEvent: Int? = null
    /** Vaal orbs on crystals, by event, until the server tells what they did. */
    private val vaalings = HashMap<Int, Vaaling>()
    private class Vaaling(val spot: CrystalSpot, val place: Int)
    /** The crystals the server's answers said Vaal orbs made, by event, until their orbs are resolved. */
    private val vaaled = HashMap<Int, Crystal>()
    private var fallEvent: Int? = null
    /** The pet as a fighter (3.5.0), made again only when the hero's sheet that reaches it changes. */
    private val allies = PetAllies(index, pet, rules)
    private fun ally(): Ally? = allies.of(hero.stats)
    /**
     * The hero's degeneration on the road (3.4.0): the fight burns it in its own beat, the walk did not.
     * Off a fight it wounds to the last point: only a fight ends a run.
     */
    private fun wound(dt: Double) {
        val share = hero.lifeDegenShare
        if (share > 0 && life > 1) life = (life - hero.maxLife * share * dt).coerceAtLeast(1.0)
    }
    private var slain: RolledMonster? = null
    private var report: FightReport? = null
    /** The foes of the stage in the battle's order, each with the pack it walked with. */
    private var members: List<FightMember> = emptyList()
    /** Every pack the fight drew in: the engaged one first, then the rest by their distance to it. */
    private var fightAgents: List<MonsterAgent> = emptyList()
    /** [fightAgents] as the stages they are fought in: small packs in a row merge into one (3.70.0). */
    private var fightStages: List<List<MonsterAgent>> = emptyList()
    /** The stage under way, from 1 (3.28.0): the packs of [fightStages] it fights. */
    private var stage = 0
    /** Seconds of the pause before a later stage begins on its own; null outside that pause. */
    private var interlude: Double? = null
    /** What the stages already won leave to the one report: every foe's log, and the seconds they took. */
    private var stageHits: List<PackHit> = emptyList()
    private var stageTime = 0.0
    /**
     * What the stage won last hands the one under way (3.32.0): its STAGE_CLEAR powers and the momentum; null for a first stage.
     * Held by the journal (3.32.1), so a run entered again after a restart mid-fight hands it to its next fight.
     */
    private var stageCarry: StageCarry?
        get() = journal.carry
        set(value) { journal.carry = value }
    /** The strongest of every stage: it stands for the whole fight in the report. */
    private var fightStrongest: RolledMonster? = null
    /** The dice stream of the stage's battle: a draught in the pause builds the battle again on the same dice. */
    private var fightStream = 0L
    private var reported = 0
    private var fall: Double? = null
    private var kills = 0
    /** The map's summary (see [MapTally]): how it ended, the seconds on it, the guardians slain and the deaths. */
    private var end: MapEnd? = null
    private var seconds = 0.0
    private var bosses = 0
    private var deaths = 0
    /** The run's figures (3.47.0) and, after a fall, its last blows. */
    private val stats = RunStats()
    private var recap: List<DeathHit> = emptyList()
    private var fightAgent: MonsterAgent? = null
    private var pendingGear: RunCommand.Regear? = null
    private val waves get() = AbyssWaves(index, run)
    private val abyssRule get() = index.campaign.abyss

    private fun manaCap(): Double = hero.maxMana * (1 - kit.reserved(hero) / 100)

    private fun regear(gear: HeroGear) {
        val next = HeroBuild(gear, mapEffects, rules)
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
        if (phase != RunPhase.DEAD && phase != RunPhase.CLEARED && phase != RunPhase.LEFT) seconds += dt
        if (holds == 0) when (phase) {
            RunPhase.MAP -> autopilot?.let { drive(it, dt) } ?: walk(dt)
            RunPhase.FIGHT -> play(dt)
            else -> Unit
        }
        state.value = snapshot()
    }

    /** One event of the journal, and the listener told. */
    private fun record(kind: RunEventKind, i: Int = 0, m: Int = 0, index: Int = 0, depth: Int = 0, fallen: Boolean = false,
                       fight: com.sperance.exileforge.rules.content.FightTally? = null): RunEvent? =
        journal.record(kind, i, m, index, depth, fallen, vaal, fight)?.also(onRecorded)

    /** A rewarding event recorded: what it brings comes with the server's answer, into the run's count, the autorun's and, [fought], the fight's report. */
    private fun rewarding(event: RunEvent?, fought: Boolean = false): RunEvent? = event?.also {
        mine += it.n
        if (autopilot != null) { autoEvents += it.n; autoReward = autoReward ?: Reward.NONE }
        if (fought) { fightEvents += it.n; reward = reward ?: Reward.NONE }
    }

    private fun clearSpoils() { reward = null; fightEvents.clear() }

    /** The server's answer: every reward of this run's events lands where it was waited for. */
    private fun settle(answer: RunCommand.Settled) {
        answered = maxOf(answered, answer.applied)
        refused += answer.rejected
        answer.rewards.forEach { (n, gained) ->
            if (n !in mine || n in earned) return@forEach
            earned[n] = gained
            granted += gained
            if (n in fightEvents) reward = (reward ?: Reward.NONE) + gained
            if (n in autoEvents) autoReward = (autoReward ?: Reward.NONE) + gained
        }
        if (fallEvent != null) answer.lost?.let { fall = it }
        answer.crystals.forEach { (n, changed) -> if (n in vaalings) vaaled[n] = changed }
        resolve()
    }

    /**
     * What only the server's answer tells: the Vaal zone behind the opened portal, and what a Vaal orb did to a crystal —
     * told by the event's answer, or, from an older server, matched on the campaign it brought.
     */
    private fun resolve() {
        val state = campaign
        gateEvent?.takeIf { vaalZone == null }?.let { n ->
            val rolled = state?.vaalZone?.takeIf { it.mapCode == zone.code }
            if (rolled != null) {
                vaalZone = rolled
                run = Run(index, run.zone, run.seed, run.context.copy(vaal = rolled))
                if (phase == RunPhase.GATE) gate = rolled
            } else if (n in refused) {
                gateEvent = null; corruptionOpened = true
                world.closePortal(); closeGate()
            }
        }
        val standing = state?.crystals?.get(zone.code)?.crystals.orEmpty()
        val waiting = vaalings.entries.iterator()
        while (waiting.hasNext()) {
            val (n, orb) = waiting.next()
            if (n >= answered) continue
            val before = orb.spot.crystal
            fun changed(c: Crystal) = c.vaal && c.guardian == before.guardian && c.essences.size == before.essences.size
            val after = if (n in refused) before
                else vaaled[n] ?: standing.getOrNull(orb.place)?.takeIf(::changed) ?: standing.firstOrNull(::changed) ?: continue
            waiting.remove()
            vaaled.remove(n)
            if (after === before) continue
            orb.spot.crystal = after
            if (crystal === orb.spot) crystalOutcome = vaalOutcome(before, after)
        }
    }

    /** What the orb did, read off the crystal before and after it. */
    private fun vaalOutcome(before: Crystal, after: Crystal): String = when {
        after.essences.count { index.essence(it)?.special == true } > before.essences.count { index.essence(it)?.special == true } -> EssenceBook.VAAL_SPECIAL
        after.essences != before.essences -> EssenceBook.VAAL_UPGRADE
        else -> EssenceBook.VAAL_STRONGER
    }

    private fun apply(command: RunCommand) {
        when (command) {
            RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2
            RunCommand.StopAuto -> autopilot = null
            RunCommand.Leave -> if (phase == RunPhase.MAP || phase == RunPhase.DEAD || phase == RunPhase.CLEARED) { if (phase == RunPhase.MAP) end = MapEnd.LEFT; phase = RunPhase.LEFT }
            RunCommand.Retreat -> if (abyssFight) Unit else if (fight != null && !started) walkAway() else { paused = false; fight?.retreat() }
            RunCommand.Begin -> if (fight != null) { started = true; paused = false; interlude = null }
            RunCommand.Pause -> if (fight != null && started && fight?.outcome == null) paused = !paused
            is RunCommand.Focus -> fight?.focus(command.index)
            is RunCommand.Hold -> holds = (holds + if (command.on) 1 else -1).coerceAtLeast(0)
            RunCommand.Continue -> when (phase) {
                RunPhase.LOOT -> { phase = RunPhase.MAP; clearSpoils(); slain = null; report = null }
                RunPhase.DEAD, RunPhase.CLEARED -> phase = RunPhase.LEFT
                else -> Unit
            }
            // In the pause between stages the stage has not begun: the kit changes now, and its battle is drawn again.
            is RunCommand.Regear -> if (phase == RunPhase.FIGHT && interlude != null && !started) { pendingGear = null; regear(command.gear); fight = battle() }
                else if (phase == RunPhase.FIGHT) pendingGear = command else regear(command.gear)
            RunCommand.DismissChest -> chestEvent = null
            RunCommand.StepBack -> if (phase == RunPhase.GATE) closeGate()
            is RunCommand.ShutGate -> {
                // Refused while its zone is still on the way, the portal is as good as opened: the server rolled the zone, and it is left.
                if (!command.entered && (vaalZone != null || gateEvent != null)) { record(RunEventKind.VAAL_LEAVE); vaalZone = null; gateEvent = null; corruptionOpened = true }
                world.closePortal(); closeGate()
            }
            is RunCommand.Returned -> {
                command.zone?.let(::adopt)
                life = command.life.coerceIn(0.0, hero.maxLife)
                command.pools?.let { mana = it.mana.coerceIn(0.0, manaCap()); charges = it.charges.ifEmpty { charges }; flaskLeft = it.flaskLeft.ifEmpty { flaskLeft }; rates = it.rates.ifEmpty { rates }; rebody() }
                // The zone is closed either way: its guardian fell, or the hero did.
                vaalZone = null; gateEvent = null; corruptionOpened = true
                phase = RunPhase.MAP
            }
            is RunCommand.Cast -> fight?.useSkill(command.slot)
            // In the pause between stages a draught is drunk as on the road, and the stage's battle takes the pools it leaves.
            is RunCommand.Drink -> if (phase == RunPhase.FIGHT && interlude != null && !started) { drinkOnMap(command.slot); fight = battle() }
                else if (phase == RunPhase.FIGHT) fight?.useFlask(command.slot)
                else if (phase == RunPhase.MAP || phase == RunPhase.CRYSTAL || phase == RunPhase.ABYSS) drinkOnMap(command.slot)
            // The guardian waits for the orb's outcome: it stands up as the crystal the server holds.
            RunCommand.Release -> if (phase == RunPhase.CRYSTAL && crystal?.let(::vaaling) != true) release()
            RunCommand.VaalCrystal -> crystal?.takeIf { phase == RunPhase.CRYSTAL && !it.crystal.vaal && !vaaling(it) && vaalOrbs() >= 1 }?.let { spot ->
                val place = world.standingCrystals.indexOf(spot)
                record(RunEventKind.CRYSTAL_VAAL, index = place)?.let { vaalings[it.n] = Vaaling(spot, place); crystalOutcome = null }
            }
            RunCommand.StepOff -> when {
                phase == RunPhase.CRYSTAL -> closeCrystal()
                // A crack is left unopened, or once its hoard is in — never mid-descent with the hoard at stake.
                phase == RunPhase.ABYSS && (descent == null || descent?.claim != null) -> closeRift()
            }
            RunCommand.Descend -> if (phase == RunPhase.ABYSS) descend()
            RunCommand.TakeHoard -> descent?.takeIf { phase == RunPhase.ABYSS && it.cleared > 0 && it.claim == null }?.let { take(it, fallen = false) }
            is RunCommand.Settled -> settle(command)
            is RunCommand.Campaign -> { campaign = command.state; resolve() }
        }
    }

    /** A Vaal orb on [spot] whose outcome the server has not told yet. */
    private fun vaaling(spot: CrystalSpot): Boolean = vaalings.values.any { it.spot === spot }

    // ==================== The Abyss ====================

    /** A descent under way: its crack, how many depths it leads down, how many are cleared, and the fights of the wave left. */
    private class Descent(val spot: AbyssSpot, val depth: Int) {
        var cleared = 0
        var level = 0
        var fights: List<List<RolledMonster>> = emptyList()
        /** The claim of the hoard, by its event: what it holds is the server's answer. */
        var claim: Int? = null
        var fallen = false
        /** The last fight of the descent won (3.32.0): the next wave or depth is its next stage. */
        var carry: StageCarry? = null
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
        } else if (current.claim == null && current.cleared < current.depth) wave(current, current.cleared + 1)
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
        engage(MonsterAgent(ABYSS_AGENT - current.spot.id, group, current.spot.cell.x + 0.5, current.spot.cell.y + 0.5), current.level, abyssal = true, carry = current.carry)
    }

    /** The descent is over: the hoard of the depths cleared — whole, or what a fall leaves of it. */
    private fun take(current: Descent, fallen: Boolean) {
        current.fallen = fallen
        // A fall in the Abyss burns the whole hoard (server 1.2.0); the hoard is still counted, as the server counts it
        current.claim = rewarding(record(RunEventKind.ABYSS_CLAIM, depth = current.cleared, fallen = fallen))?.n
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
        clearSpoils(); slain = null
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
        wound(dt)
        val (x, y) = ExpeditionWorld.screenToWorld(stickX, stickY)
        when (val event = world.step(dt, x, y)) {
            is WorldEvent.Encounter -> engage(event.agent)
            WorldEvent.Exit -> exit()
            is WorldEvent.Opened -> chestEvent = rewarding(record(RunEventKind.CHEST, index = event.chest.id))?.n
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
        end = MapEnd.CLEARED
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
                rewarding(record(RunEventKind.CHEST, index = step.chest.id))
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

    /**
     * The portal opens its gate: the first time, the opening is recorded and the server rolls the Vaal zone behind
     * it (1.30.0) — the gate waits for it, and the run takes its context when the answer brings it.
     */
    private fun openGate() {
        if (vaalZone == null) {
            if (corruptionOpened) { world.closePortal(); return }
            if (gateEvent == null) gateEvent = record(RunEventKind.VAAL_OPEN)?.n
            if (gateEvent == null) { world.closePortal(); return }
        }
        gate = vaalZone
        phase = RunPhase.GATE
    }

    /**
     * A fight with [agent]'s pack still standing, all at once, at [level] — the zone's, or a depth's of the Abyss.
     * On the map the packs standing close by are drawn in (3.26.0) and, since 3.28.0, fought a stage at a time — small packs in a row merged into one (3.70.0):
     * the engaged one first, then the rest by their distance to it; the boss and a guardian always fight alone.
     */
    private fun engage(agent: MonsterAgent, level: Int = zone.level, abyssal: Boolean = false, carry: StageCarry? = null) {
        fightAgents = if (abyssal) listOf(agent) else world.gathered(agent)
        fightStages = if (abyssal) listOf(fightAgents) else world.stages(fightAgents)
        fightStrongest = fightAgents.flatMap { pack -> pack.standing.map { pack.pack[it] } }.maxByOrNull { it.rarity.ordinal }
        stageHits = emptyList()
        stageTime = 0.0
        // Every fight's end drops the carry: one still held here is the journal's, kept over a restart mid-fight.
        stageCarry = carry ?: stageCarry
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
        members = fightStages[number - 1].flatMap { pack -> pack.standing.map { FightMember(pack, it) } }
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
        Foe(Combatant(monster.stats, fightLevel, rules), monster.rarity, monster.skills.mapNotNull(this.index.skills.monsterByCode::get), monster, fightLevel)
    }, rules, life, Random(Streams.mix(seed, FIGHT_STREAM, fightStream)), stance, kit = kit, model = build, pools = pools,
        percent = build.gear.percent, ally = ally(), stage = stageCarry)

    /** The fight is over, whichever way: nothing of it is held any longer. */
    private fun endFight() {
        fight = null
        fightAgent = null
        fightAgents = emptyList()
        fightStages = emptyList()
        members = emptyList()
        stage = 0
        interlude = null
        stageHits = emptyList()
        stageTime = 0.0
        stageCarry = null
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

    /** A foe of the fight fell: the event by what it was; its reward comes with the server's answer. */
    private fun fell(agent: MonsterAgent, member: Int) {
        kills++
        if (agent === world.boss) bosses++
        if (abyssFight) return
        val spot = agent.crystal?.let { id -> world.crystals.firstOrNull { it.id == id } }
        val event = when {
            spot != null -> record(RunEventKind.CRYSTAL, index = world.standingCrystals.indexOf(spot)).also { spot.freed = true }
            agent === world.boss -> if (vaal) record(RunEventKind.CORRUPT) else record(RunEventKind.BOSS)?.also { bossDown = true }
            else -> record(RunEventKind.KILL, i = agent.id, m = member)
        }
        rewarding(event, fought = true)
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
        if (outcome == Outcome.WIN && stage < fightStages.size) {
            fightStages[stage - 1].forEach { it.alive = false }
            stageHits = pack
            stageTime = duration
            stageCarry = battle.carry()
            pendingGear?.let { regear(it.gear) }
            pendingGear = null
            begin(stage + 1)
            return
        }
        stats.add(pack, duration)
        // The fight's figures (3.51.0): only the hero's statistics, before a fall closes the journal.
        record(RunEventKind.FIGHT, fight = FightFigures.of(pack, duration, boss = fightAgents.any { it === world.boss }, won = outcome == Outcome.WIN))
        val leader = fightStrongest ?: fightLeader()
        val down = descent?.takeIf { abyssFight }
        when (outcome) {
            Outcome.WIN -> {
                fightAgents.forEach { it.alive = false }
                if (down != null) { report = null; phase = RunPhase.ABYSS }
                // An autorun goes on without the report: what the fight brought is in its tally already
                else if (autopilot != null) { clearSpoils(); slain = null; report = null; phase = RunPhase.MAP }
                else {
                    slain = leader
                    report = FightReport(leader, Outcome.WIN, pack, duration)
                    phase = RunPhase.LOOT
                }
            }
            Outcome.LOSS -> {
                report = FightReport(leader, Outcome.LOSS, pack, duration)
                recap = RunStats.recap(pack)
                life = 0.0
                autopilot = null
                phase = RunPhase.DEAD
                deaths++; end = MapEnd.FELL
                // A fall in the Abyss burns its hoard, but for the atlas's share; then the zone's own price.
                down?.let { take(it, fallen = true) }
                if (vaal) record(RunEventKind.VAAL_LEAVE) else record(RunEventKind.FALL)?.let { fallEvent = it.n; fall = deathLoss() }
                onFallen()
            }
            // Nothing already looted is lost, but there is no report for a fight cut short: the packs of the stages won stay dead,
            // the current one and those still waiting go back to their places.
            Outcome.RETREAT -> {
                // Walking out of a fight takes the run back into the player's hands
                autopilot = null
                fightAgents.filter { it.id >= 0 && it.alive }.forEach(world::retreatFrom)
                report = null; clearSpoils(); slain = null
                if (down != null) { phase = RunPhase.ABYSS; take(down, fallen = true) } else phase = RunPhase.MAP
            }
        }
        endFight()
        abyssFight = false
        if (phase != RunPhase.DEAD) pendingGear?.let { regear(it.gear) }
        pendingGear = null
        // The Abyss chains its fights (3.32.0): the next wave, or the next depth once the player descends, is the next stage.
        if (outcome == Outcome.WIN && down != null) { down.carry = battle.carry(); if (down.fights.isNotEmpty()) nextFight(down) else down.cleared++ }
    }

    /** What the death costs by the rules: a share of the level's experience, never the level — on what the answers granted so far. */
    private fun deathLoss(): Double {
        val classes = index.classes
        val gained = heroExperience + granted.experience
        val level = classes.levelOf(gained).coerceAtLeast(heroLevel)
        return LootRoller(index).deathLoss(rules.death, zone.level, gained, classes.threshold(level) ?: 0.0, classes.nextThreshold(level))
    }

    private fun snapshot(): RunHud {
        val battle = fight
        val figures = stats.summary(kills)
        val awaiting = mine.count(::awaits)
        return RunHud(
            phase = phase, mapCode = zone.code,
            heroLife = (battle?.heroLife ?: life).roundToInt(), heroMaxLife = hero.maxLife.roundToInt(),
            heroShield = (battle?.heroFighter?.shield ?: hero.maxShield).roundToInt(), heroMaxShield = hero.maxShield.roundToInt(),
            alive = world.alive, total = world.total, sealed = world.sealed,
            fight = battle?.takeIf { fightAgent != null }?.let(::fightHud),
            reward = reward, rewardAwaiting = fightEvents.count(::awaits), slain = slain, report = report,
            fall = fall,
            gold = granted.gold, experience = granted.experience, kills = kills, awaiting = awaiting,
            chestsLeft = world.chests.count { !it.opened },
            chest = chestEvent?.let { earned[it] ?: Reward.NONE }, chestAwaiting = chestEvent?.let(::awaits) == true,
            fountainsLeft = world.fountains.count { !it.used },
            gate = gate, vaal = vaal,
            heroMana = (battle?.heroMana ?: mana).roundToInt(), heroMaxMana = (battle?.manaCap() ?: manaCap()).roundToInt(),
            heroReserved = (battle?.manaReserved() ?: (hero.maxMana - manaCap())).roundToInt(),
            flasks = battle?.flaskViews() ?: mapFlasks(),
            crystal = crystal?.let { CrystalView(it.id, it.crystal.essences, it.crystal.guardian, it.crystal.stronger, it.crystal.vaal, crystalOutcome, vaaling(it)) },
            crystalsLeft = world.standingCrystals.size,
            abyss = abyssView(), cracksLeft = world.standingCracks.size,
            bossDown = bossDown,
            auto = autopilot?.let { AutoHud(it.wave, it.waves) }, autoReward = autoReward, autoAwaiting = autoEvents.count(::awaits),
            pending = journal.pending.size, applied = journal.applied, rejected = journal.rejected.size,
            summary = figures, recap = recap,
            tally = MapTally(end, seconds, kills, bosses, deaths, granted, figures, awaiting),
        )
    }

    private fun abyssView(): AbyssView? {
        val spot = rift ?: return null
        val rule = abyssRule ?: return null
        val down = descent
        val claim = down?.claim
        return AbyssView(down?.depth ?: spot.depth, down?.cleared ?: 0, down != null, waves.depths(rule, zone),
            claim?.let { earned[it] ?: Reward.NONE }, down?.fallen == true, claim?.let(::awaits) == true)
    }

    /** What this run hands the map it was entered from, a Vaal zone over: its rewarding events and its own counts. */
    fun share(): ZoneShare = ZoneShare(mine.associateWith { earned[it] }, seconds, kills, bosses, deaths, stats.summary(kills))

    /**
     * A Vaal zone's share taken into the map's: what the server granted for its events is the map's loot now, and
     * the answers still to come for the rest land here, as this run's own do.
     */
    private fun adopt(zone: ZoneShare) {
        zone.events.forEach { (n, gained) ->
            if (!mine.add(n) || n in earned) return@forEach
            gained?.let { earned[n] = it; granted += it }
        }
        seconds += zone.seconds
        kills += zone.kills
        bosses += zone.bosses
        deaths += zone.deaths
        stats.add(zone.figures)
    }

    /** Event [n] is not answered yet. */
    private fun awaits(n: Int): Boolean = n >= answered

    private fun mapFlasks(): List<FlaskView?> = kit.flasks.mapIndexed { i, flask ->
        flask?.let {
            val left = flaskLeft.getOrNull(i) ?: 0.0
            FlaskView(i, it.code, it.kind, (charges.getOrNull(i) ?: 0.0).toInt(), it.maxCharges.toInt(), kotlin.math.ceil(it.perUse(hero) - 1e-9).toInt(),
                if (left > 0) (left / it.duration(hero)).toFloat().coerceIn(0f, 1f) else 0f, it.condition)
        }
    }

    /** The strongest of the stage: its portrait and its name head the pack fought now. */
    private fun fightLeader(): RolledMonster = members.maxBy { it.monster.rarity.ordinal }.monster

    private fun fightHud(battle: Battle): FightHud = battle.hud(members.map { it.monster }, fightLeader(), speed, started, paused, hero.taunt,
        level = fightLevel, escape = !abyssFight, stage = stage, stages = fightStages.size, interlude = interlude)

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
            if (!vaal) {
                world.placeChests(campaign.chests[location.code]?.left ?: 0)
                world.placeCrystals(campaign.crystals[location.code]?.crystals.orEmpty())
                world.placeCracks(campaign.abyss[location.code]?.cracks.orEmpty())
            }
            val pilot = auto?.let { AutoPilot.of(world, it, if (vaal) run.seed xor VAAL_SALT else run.seed, bossStands = world.boss?.alive == true) }
            return ExpeditionRun(index, zone, run, journal, world, build, rules, run.seed, effects, vaal, startPools, heroExperience, heroLevel, vaalOrbs,
                campaign.corruptionOpened, vaalZone, bossDown, onRecorded, onCleared, onFallen, pilot, pet)
        }

        private const val VAAL_SALT = 0x5661616C5A6F6E65L
    }
}

/** One foe of a fight: the pack it walked with and its place there. */
internal class FightMember(val agent: MonsterAgent, val index: Int) {
    val monster: RolledMonster get() = agent.pack[index]
}
