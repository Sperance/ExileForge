package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.BodyModel
import com.sperance.exileforge.core.campaign.FoeWindow
import com.sperance.exileforge.core.campaign.HeroCharges
import com.sperance.exileforge.core.campaign.HeroModel
import com.sperance.exileforge.core.campaign.HitTrace
import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.PowerRunner
import com.sperance.exileforge.core.campaign.RollTrace
import com.sperance.exileforge.core.campaign.StageCarry
import com.sperance.exileforge.core.campaign.TraceOrigin
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.rules.content.AilmentRule
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.Condition
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * The fight, alive: stepped in fixed slices of time so that the same seed is the same fight on any
 * screen, and open to the player while it runs — a retreat can be begun, a foe can be singled out.
 *
 * Since 2.70.0 it is the hero against the whole pack at once: every foe swings at the hero at its own
 * speed from the first second, and the hero at any one foe on screen, whatever the weapon. Whom is the
 * player's [focus] when given, the class's [TargetRule] otherwise, chosen afresh at every swing; a taunter
 * comes first. Only [FoeWindow.SIZE] of the pack fight at once: the rest wait in
 * line and step in, each into the place of one that fell ([window]); a foe waiting is not [Fighter.alive].
 *
 * Each swing can be evaded (evasion against the attacker's level), blocked, or land; a landing hit
 * rolls the rule's variance per damage type, may be a critical strike, and is reduced by armour
 * (physical, `armour / (armour + factor × damage)`) and by resistances (elements and chaos,
 * capped). Energy shield takes a hit before life, except chaos, which goes around it; a shield left
 * alone for the rule's delay recharges. Leech gives back a share of what was dealt; a hit big enough
 * against the target's life stuns it, and a frozen or stunned fighter does nothing until it passes.
 * Every landing hit may inflict the ailments its damage types carry: burning, poison and bleeding
 * deal a share of the hit over time, chill slows the target's actions, shock makes it take more, a
 * freeze stops it. Life and shield regenerate as they go.
 *
 * Since 2.78.0 (server 0.69.0) mana is back, and the hero brings a [Loadout]: the active skills are
 * tried in their slots' order whenever one is ready and its condition holds — a ready one short of
 * mana holds back the ones after it — or at a tap; the passives lie on the sheet or answer the fight's
 * events; the flasks are drunk by their conditions or a tap. Bosses and casters cast for their own
 * mana. A buff, a curse or a draught lies on a fighter as a [TimedEffect], and its body is made again
 * from its sheet with their lines whenever one comes or goes.
 *
 * The client fights by the owner's decision (rule 23); every constant here is the server's [rules].
 */
class Battle(
    val hero: Combatant,
    val foes: List<Foe>,
    val rules: CombatRules,
    heroLife: Double,
    internal val random: Random,
    val stance: HeroStance = HeroStance(),
    /** How many fight on the hero's side (2.71.0); alone, the hero is a lone wolf. */
    val party: Int = 1,
    /** What the hero brings beyond the sheet (2.78.0), and how their sheet takes the lines of the moment. */
    val kit: Loadout = Loadout(),
    internal val model: HeroModel = HeroModel.of(hero),
    /** What the hero walks in with (2.78.0): mana, the flasks' charges and draughts still running; null brings them full. */
    pools: HeroPools? = null,
    /** The stats that are a percent already: the lines laid on a monster fold by them. */
    private val percent: Set<String> = emptySet(),
    /** The combat pet at the hero's side (3.5.0); it stands up whole after the fight. */
    val ally: Ally? = null,
    /** A later stage of a staged fight (3.32.0): what the stage won before it hands on; null for a fight of its own or a first stage. */
    val stage: StageCarry? = null,
) {
    /** One side in motion: its pools, its clocks and what is on it; [index] is its place in the pack, -1 for the hero. */
    inner class Fighter(val side: Side, body: Combatant, life: Double, val index: Int = -1) {
        /** The sheet as it stands: the hero's changes under the auras of the foes still standing (2.75.0) and with what lies on them (2.78.0). */
        var body: Combatant = body
            private set

        /** A monster's sheet taking the lines laid on it (2.78.0). */
        val model: BodyModel = BodyModel.of(body, percent)
        var life = life.coerceIn(0.0, body.maxLife)
        var shield = body.maxShield

        /** Mana (2.78.0): the hero's comes in from the fight before, a monster's is full. */
        var mana = body.maxMana
        var nextAttack = if (side == Side.HERO) 0.35 else rules.entry + index * rules.stagger
        var attackInterval = 1 / body.attackSpeed

        /** A new sheet mid-fight: the pools keep their share, the swing keeps its pace from the next one. */
        fun rebody(next: Combatant) {
            if (next === body) return
            life = if (body.maxLife > 0) life / body.maxLife * next.maxLife else next.maxLife
            shield = if (body.maxShield > 0) shield / body.maxShield * next.maxShield else min(shield, next.maxShield)
            mana = min(mana, next.maxMana)
            body = next
            attackInterval = 1 / next.attackSpeed
        }

        /** Stunned until then: nothing is swung before it. A freeze holds by its own ailment (3.71.0), so lifting it frees at once. */
        var heldUntil = 0.0
        var lastHit = -1e9

        /** Vampirism within the current second (server 1.76.0): [leechStart] opens it, [leeched] is what it has given back so far. */
        var leechStart = -1e9
        var leeched = 0.0

        /** How much of [leech] the per-second cap still lets through at [time], counting it in. */
        fun leechRoom(leech: Double, time: Double, cap: Double): Double {
            if (time - leechStart >= 1.0) {
                leechStart = time
                leeched = 0.0
            }
            val allowed = min(leech, max(0.0, body.maxLife * cap / 100 - leeched))
            leeched += allowed
            return allowed
        }
        val ailments = mutableListOf<ActiveAilment>()

        /** Damage over time gathered per ailment since its last tick was logged, and when that was. */
        val ticking = mutableMapOf<Ailment, Double>()
        val tickedAt = mutableMapOf<Ailment, Double>()

        /** Buffs, curses and draughts on it (2.78.0). */
        val effects = mutableListOf<TimedEffect>()

        /** What a barrier still soaks, and until when (2.78.0). */
        var barrier = 0.0
        var barrierUntil = 0.0
        var invulnerableUntil = -1.0

        /** When each of its skills is ready again, by code for a monster and by slot for the hero (2.78.0). */
        val readyAt = mutableMapOf<String, Double>()

        /** Its low-life lines are on (2.78.0). */
        var low = false

        /** The buildups (3.78.0): each bar's fill from 0 to 1, when a blow last filled one, and how long each is shut after going off. */
        val buildup = DoubleArray(Buildup.entries.size)
        var builtAt = -1e9
        val buildupShutUntil = DoubleArray(Buildup.entries.size)

        /** A full stun bar: every damage heavier until then; a full freeze bar: the blow that breaks the ice is heavier; electrocuted: lightning heavier. */
        var stunnedUntil = 0.0
        var shatter = false
        var electrocutedUntil = 0.0

        /** The fullest bar, for the card: which and how full. */
        fun leading(): Pair<Buildup, Double>? = Buildup.entries.map { it to buildup[it.ordinal] }.filter { it.second > 0.005 }.maxByOrNull { it.second }

        /** On the field: the hero's side always, a foe once the [window] lets it in — one waiting its turn is not in the fight yet. */
        var engaged: Boolean = side == Side.HERO
            private set

        /** A foe steps onto the field at [place]: a moment to close in before its first swing, a little longer the further its place. */
        fun enter(place: Int, at: Double) {
            engaged = true
            nextAttack = at + rules.entry + place * rules.stagger
        }
        val alive: Boolean get() = engaged && life > 0
        val held: Boolean get() = heldUntil > time || ailments.any { it.ailment == Ailment.FROZEN && it.until > time }
        val cursed: Boolean get() = effects.any { it.kind == EffectKind.CURSE }
        val invulnerable: Boolean get() = invulnerableUntil > time
        fun frozen() = ailments.any { it.ailment == Ailment.FROZEN }

        /** Chill slows every action; the strongest chill counts. */
        fun slow() = 1 + (ailments.filter { it.ailment == Ailment.CHILLED }.maxOfOrNull { it.magnitude } ?: 0.0) / 100

        /** Shock makes every hit heavier; a curse can make it heavier still (2.78.0). */
        fun weakness() = 1 + (ailments.filter { it.ailment == Ailment.SHOCKED }.maxOfOrNull { it.magnitude } ?: 0.0) * body.ailmentTaken(Ailment.SHOCKED) / 100
        fun stacks(ailment: Ailment) = ailments.count { it.ailment == ailment }
    }

    /** Who of the pack is on the field: at most [FoeWindow.SIZE] at once, the strongest first, the rest stepping in as they fall. */
    val window = FoeWindow(FoeWindow.order(foes.map { it.rarity }))
    val foeFighters: List<Fighter> = foes.mapIndexed { i, foe -> Fighter(Side.MONSTER, foe.body, foe.body.maxLife, i) }
        .also { all -> window.field.forEachIndexed { place, i -> all[i].enter(place, 0.0) } }
    val heroFighter = Fighter(Side.HERO, hero.under(auras()), heroLife)

    /** The pet fighting beside the hero (3.5.0): it strikes the hero's target and draws blows meant for the hero. */
    val allyFighter: Fighter? = ally?.let { Fighter(Side.HERO, it.body, it.body.maxLife, ALLY) }

    /** Whom a monster's blow is for: a standing tank pet takes them all, any other pet its share, the hero the rest. */
    internal fun foeTarget(): Fighter {
        val pet = allyFighter?.takeIf { it.alive } ?: return heroFighter
        return if (ally!!.tank || random.nextDouble() < ally.drawFire) pet else heroFighter
    }

    /** The auras of the foes still standing, summed per stat (server 0.66.0). */
    internal fun auras(): Map<String, Double> {
        val sum = mutableMapOf<String, Double>()
        foeFighters.filter { it.alive }.forEach { foe -> foe.body.auras.forEach { (stat, value) -> sum.merge(stat, value, Double::plus) } }
        return sum
    }
    internal val ailmentRules: Map<AilmentRule, Pair<Ailment, DamageType>> = rules.ailments
        .mapNotNull { rule -> Ailment.of(rule.ailment)?.let { a -> DamageType.of(rule.type)?.let { t -> rule to (a to t) } } }.toMap()
    internal val ruleOf: Map<Ailment, Pair<AilmentRule, DamageType>> = ailmentRules.entries.associate { (rule, what) -> what.first to (rule to what.second) }

    var time = 0.0
        internal set

    /** When the fight ended; equals [time] then, and stays. */
    var duration = 0.0
        internal set
    var outcome: Outcome? = null
        internal set

    /** The foe the player singled out, until it falls or is tapped again. */
    var focus: Int? = null
        internal set

    /** The foe that struck the hero last — the Templar's answer. */
    internal var lastStriker: Int? = null
    internal var retreatAt = Double.NaN
    private var carry = 0.0
    internal val log = mutableListOf<CombatEvent>()
    val events: List<CombatEvent> get() = log
    internal val fallenOrder = mutableListOf<Int>()

    /** The foes that fell, in the order they fell: each is a kill to report the moment it happens. */
    val fallen: List<Int> get() = fallenOrder

    // ==================== The hero's skills and flasks (2.78.0) ====================

    /** A slot whose opening condition has fired this fight, and the slots tapped since the last slice. */
    internal val opened = BooleanArray(kit.actives.size)
    internal val taps = mutableSetOf<Int>()
    internal val charges = DoubleArray(kit.flasks.size) { i -> kit.flasks[i]?.let { pools?.charges?.getOrNull(i)?.coerceIn(0.0, it.maxCharges) ?: it.maxCharges } ?: 0.0 }
    internal val flaskOpened = BooleanArray(kit.flasks.size)
    internal val drinks = mutableSetOf<Int>()
    internal val triggerReady = mutableMapOf<String, Double>()
    internal val recoveries = mutableListOf<Recovery>()

    /** The hero's regeneration since the last recovery line: summed into one line a second (3.79.0). */
    internal var regenLogged = 0.0
    internal var regenLoggedAt = 0.0

    /** The next blow of the hero is a critical strike (a cloak of shadows). */
    internal var nextCrit = false

    /** How deep in answers the fight is: an answer may set off one more, never a chain. */
    internal var depth = 0
    internal var belowLow = false
    internal var shieldUp = true

    /** The hero's powers (2.79.0): the unique items' answers to what happens here. */
    internal val powers = PowerRunner(this, kit.powers, stage)

    /** The hero's frenzy, power and endurance charges (3.33.0, server 1.32.0): none as a fight opens, a stage's from the one before. */
    internal val heroCharges = HeroCharges(kit.charges, stage?.charges.orEmpty())

    /** The delayed life damage still to come (3.33.0). */
    internal val delayed = mutableListOf<Delayed>()

    /** The flasks a hero drinks all at once as the fight opens (3.33.0) are behind them. */
    internal var autoDrunk = false

    /** The pet stood at the last look (3.33.0): its fall is a power's event once. */
    internal var petStood = true

    /** When the hero last killed, blocked and struck critically (3.35.0): what «recently» of a conditional line reads. */
    internal var killedAt = NEVER
    internal var blockedAt = NEVER
    internal var critAt = NEVER

    /** Whether the sheet has conditional lines at all, and the hero's conditions at the last look (3.35.0). */
    internal val conditioned = model.conditional(Condition.entries.toSet()).isNotEmpty()
    internal var conditions: Set<Condition> = emptySet()

    /** What this fight, won, hands the next stage of a staged fight (3.32.0): the momentum and, since 3.33.0, the charges. */
    fun carry(): StageCarry = StageCarry(powers.momentum, heroCharges.snapshot())

    init {
        // A draught still running from the map comes into the fight, and the belt's opening ones count as drunk.
        pools?.flaskLeft?.forEachIndexed { i, left ->
            val flask = kit.flasks.getOrNull(i) ?: return@forEachIndexed
            if (left <= 0) return@forEachIndexed
            val draught = flask.draught(heroFighter.body, heroFighter.life, 0.0)
            heroFighter.effects += TimedEffect(EffectKind.FLASK, flask.code, draught.lines, left, draught.duration, slot = i)
            // Its recovery comes along with it (2.81.0): before, the draught's buff ran on but its healing stopped.
            pools?.rates?.getOrNull(i)?.takeIf { it.flows }?.let { recoveries += Recovery(it.life, it.mana, left, i, draught.lifeOnly) }
            flaskOpened[i] = true
        }
        if (heroCharges.any) heroCharges.start(time, heroFighter.body.stats)
        // Buffs worn from the start (3.35.0): a monster's modifier or the map's.
        (listOf(heroFighter) + listOfNotNull(allyFighter) + foeFighters).forEach(::wear)
        if (conditioned) conditions = heroConditions()
        if (heroFighter.effects.isNotEmpty() || heroCharges.any || conditions.isNotEmpty()) remake(heroFighter)
        heroFighter.mana = (pools?.mana ?: Double.MAX_VALUE).coerceIn(0.0, manaCap())
        shieldUp = heroFighter.shield > 0
    }

    fun foe(index: Int) = foeFighters[index]
    val heroLife: Double get() = heroFighter.life
    val heroMana: Double get() = heroFighter.mana
    val retreating: Boolean get() = !retreatAt.isNaN()

    /** The hero's mana the auras leave free (2.78.0). */
    fun manaCap(): Double = heroFighter.body.maxMana * (1 - kit.reserved(heroFighter.body) / 100)

    /** The hero's mana the auras hold: the rest of the pool past [manaCap]. */
    fun manaReserved(): Double = heroFighter.body.maxMana - manaCap()
    internal fun manaCap(fighter: Fighter): Double = if (fighter === heroFighter) manaCap() else fighter.body.maxMana

    /** Uses the skill of active slot [slot] at the next slice, if it is ready and the mana is there — its condition aside. */
    fun useSkill(slot: Int) {
        if (kit.actives.getOrNull(slot) != null) taps += slot
    }

    /** Drinks the flask of belt place [slot] at the next slice, if it has the charges and is not running already. */
    fun useFlask(slot: Int) {
        if (kit.flasks.getOrNull(slot) != null) drinks += slot
    }

    /** The drawings of every skill in this fight, by code: the hero's and the foes'. */
    internal val icons: Map<String, String> = (kit.actives.filterNotNull() + kit.passives + kit.curses).associate { it.skill.code to it.skill.icon } +
        foes.flatMap { it.skills }.associate { it.code to it.icon }

    internal fun draughtOf(slot: Int): TimedEffect? = heroFighter.effects.firstOrNull { it.kind == EffectKind.FLASK && it.slot == slot && it.until > time }
    internal fun skillsFree(): Boolean = kit.flasks.indices.any { i -> kit.flasks[i]?.skillsFree == true && draughtOf(i) != null }
    internal fun slotKey(slot: Int) = "#$slot"
    internal fun cost(kitSkill: KitSkill, level: Int): Double = (kitSkill.skill.mana?.at(level) ?: 0.0) * heroFighter.body.skillCost

    /** Moves the fight on by [dt] seconds in fixed slices, so a frame's length never changes what happens. */
    fun advance(dt: Double) {
        // Once it is over the clock still runs, so the last blow's lunge and fade can play out.
        if (outcome != null) {
            time += dt
            return
        }
        carry += dt
        while (carry >= STEP - 1e-12 && outcome == null) {
            carry -= STEP
            step(STEP)
        }
    }

    /** Turns to leave: the hero stops swinging, the pack gets the rule's delay of free swings, then the fight is over. */
    fun retreat(): Boolean {
        if (outcome != null || retreating) return false
        retreatAt = time + rules.retreat.delay
        record(Side.HERO, Action.RETREAT, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, target()?.index ?: 0)
        return true
    }

    /** Singles out foe [index]; the same foe again, or a fallen one, gives the choice back to the class. */
    fun focus(index: Int?) {
        focus = index?.takeIf { it != focus && foeFighters.getOrNull(it)?.alive == true }
    }

    /** Foes taunting right now: while any stands, only they can be struck. */
    private fun taunters() = foeFighters.filter { it.alive && it.body.taunt }

    /**
     * Whether the hero can strike foe [index] right now: any foe on screen, by any weapon; a taunter (2.71.0)
     * always, and while one stands nobody else.
     */
    fun reachable(index: Int): Boolean {
        val foe = foeFighters.getOrNull(index)?.takeIf { it.alive } ?: return false
        return taunters().isEmpty() || foe.body.taunt
    }

    /**
     * The foe the hero's next swing goes to: the focus while it can be struck, else the class's
     * pick among those that can. A focus behind a taunter waits its turn.
     */
    fun target(): Fighter? {
        val reach = foeFighters.filter { reachable(it.index) }
        if (reach.isEmpty()) return null
        focus?.let { f -> reach.firstOrNull { it.index == f }?.let { return it } }
        fun weakest() = reach.minBy { it.life + it.shield }
        fun threat() = reach.maxBy { it.body.threat }
        fun avenge() = lastStriker?.let { s -> reach.firstOrNull { it.index == s } } ?: threat()
        return when (stance.rule) {
            TargetRule.THREAT -> threat()
            TargetRule.WEAKEST -> weakest()
            TargetRule.EXPOSED -> hero.leading.let { type -> reach.minWith(compareBy<Fighter> { it.body.resist(type) }.thenBy { it.life + it.shield }) }
            TargetRule.AVENGE -> avenge()
            TargetRule.ADAPTIVE -> if (heroFighter.life >= heroFighter.body.maxLife / 2) weakest() else avenge()
        }
    }

    /** Whom a skill of [count] targets strikes: the hero's target first, then the others on screen; zero is all of them. */
    internal fun targets(count: Int): List<Fighter> {
        val reach = foeFighters.filter { reachable(it.index) }
        if (reach.isEmpty()) return emptyList()
        val first = target()?.takeIf { it in reach } ?: reach.first()
        val order = listOf(first) + (reach - first)
        return if (count <= 0) order else order.take(count)
    }

    /** The blow whose lunge is on screen right now, and how far into it the scene is (0..1). */
    fun lunge(): Pair<CombatEvent, Double>? = log.lastOrNull {
        it.action != Action.TICK && it.action != Action.REFLECT && it.action != Action.FLASK && !it.onSelf &&
            time - it.time in -rules.lunge..rules.lunge
    }?.let { it to ((time - it.time + rules.lunge) / (2 * rules.lunge)).coerceIn(0.0, 1.0) }

    /** How far [fighter] is into its next swing, 0 just after one and 1 as the next lands. */
    fun swing(fighter: Fighter): Float = when {
        outcome != null || !fighter.alive || (fighter.side == Side.HERO && retreating) -> 0f
        else -> (1 - (fighter.nextAttack - time) / fighter.attackInterval).toFloat().coerceIn(0f, 1f)
    }

    /** The log as a test or a report reads it, once the fight is over. */
    fun log(): CombatLog = CombatLog(log.toList(), outcome ?: Outcome.RETREAT, heroLife, duration)

    /** The draws of the action being worked out; null outside one, so a draw elsewhere is not written anywhere. */
    internal var tape: MutableList<RollTrace>? = null

    /** A hit worked out up to its landing: [land] finishes it with the rolls and where the damage went. */
    internal var pendingHit: HitTrace? = null

    /** What the card needs beyond the numbers: the hero's sheet and the foes' rolls. */
    internal val origin = TraceOrigin(model, foes)

    /** The foes that have swung once already: an ambusher's first blow is the heavier one. */
    internal val swung = mutableSetOf<Int>()

    /** Foes whose rage has been lit, and by which trait: it lights once and holds to the end of the fight. */
    internal val enraged = mutableSetOf<Pair<Int, String>>()

    /** When each fallen foe of the field was first seen down (3.73.0): its place waits [CombatRules.reinforceDelay] from then. */
    internal val downSince = mutableMapOf<Int, Double>()

    /**
     * What a support pet mended since its last line (3.70.0): its healing runs every slice, so the log gathers it into a
     * line a [CombatRules.petMendEvery]. The line is the hero's own, on themselves — the figures leave it out, as they always did.
     */
    internal var petMend = 0.0

    internal var petMendFrom = 0.0

    /** Seconds until a foe steps into [place], or null when nobody waits for it or its foe still stands (3.73.0). */
    fun reinforceIn(place: Int): Double? {
        if (window.waiting == 0 || outcome != null) return null
        val foe = window.field.getOrNull(place)?.takeIf { !foeFighters[it].alive } ?: return null
        return (rules.reinforceDelay - (time - (downSince[foe] ?: time))).coerceAtLeast(0.0)
    }

    companion object {
        /** The slice the fight is stepped in: fine enough for a 5-per-second swing, fixed so a fight is a function of its seed. */
        const val STEP = 1.0 / 60

        /** How often an ailment's damage is written into the log. */
        const val TICK = 1.0

        /** The least a tick of damage over time deals — a bleeding, a poison, a burning, the ground's degeneration — once it deals any. */

        /** The pet's place in the fight: neither the hero's -1 nor a monster's. */
        const val ALLY = -2

        /** How often, in seconds, a support pet's healing is written as a line of the log (3.70.0). */

        /** How long a foe that stepped onto the field closes in before its first swing, and how much later each further place. */

        /** A time long gone (3.35.0): nothing happened «recently» at the fight's start. */
        const val NEVER = -1e9

        /** A buff worn the whole fight (3.35.0). */
        const val FOREVER = 1e9
    }
}
