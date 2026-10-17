package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.BodyModel
import com.sperance.exileforge.core.campaign.HeroCharges
import com.sperance.exileforge.core.campaign.HeroModel
import com.sperance.exileforge.core.campaign.HitTrace
import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.PowerRunner
import com.sperance.exileforge.core.campaign.TraceOrigin
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.campaign.skillsFree
import com.sperance.exileforge.rules.content.AilmentRule
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.EnrageRule
import com.sperance.exileforge.rules.content.FightKind
import com.sperance.exileforge.rules.content.FightRules
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.RecoveryPool
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import com.sperance.exileforge.rules.sheet.Recovery as RecoveryShare

/**
 * The fight, alive: stepped in fixed slices of time so that the same seed is the same fight on any
 * screen, and open to the player while it runs — a retreat can be begun, a foe can be singled out.
 *
 * It is the hero against the whole pack at once: every foe swings at the hero at its own
 * speed from the first second, and the hero at any one foe on screen, whatever the weapon. Whom is the
 * player's [focus] when given, the class's [TargetRule] otherwise, chosen afresh at every swing; a taunter
 * comes first. Вся стая на поле сразу: бой не больше `fight.maxFoes` врагов, свита босса встаёт по зову фаз ([field]).
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
 * Mana is back, and the hero brings a [Loadout]: the active skills are
 * tried in their slots' order whenever one is ready and its condition holds — a ready one short of
 * mana holds back the ones after it — or at a tap; the passives lie on the sheet or answer the fight's
 * events; the flasks are drunk by their conditions or a tap. Bosses and casters cast for their own
 * mana. A buff, a curse or a draught lies on a fighter as a [TimedEffect], and its body is made again
 * from its sheet with their lines whenever one comes or goes.
 *
 * The client fights by the owner's decision (rule 23); every constant here is the server's [rules].
 *
 * Глоток, что ещё идёт с карты, входит в бой вместе со своим восстановлением, начальные глотки пояса считаются выпитыми. Принесённое с
 * карты (недуги, баффы, проклятия, заряды) - остатком времени ([HeroCarry]); баффы с начала боя - от модификаторов монстра и карты. Щит к
 * бою не полнится: каким его оставили бой и карта, таким он и входит.
 *
 * Предначертание: что несёт заход, - до засады Разлома, она сдвигает их ходы.
 */
class Battle(
    val hero: Combatant,
    val foes: List<Foe>,
    val rules: CombatRules,
    /** Правила боя движка (`rules.json` → `fight`): потолок восстановления монстра и подавление восстановления. */
    val fight: FightRules,
    heroLife: Double,
    internal val random: Random,
    val stance: HeroStance = HeroStance(),
    /** How many fight on the hero's side; alone, the hero is a lone wolf. */
    val party: Int = 1,
    /** What the hero brings beyond the sheet, and how their sheet takes the lines of the moment. */
    val kit: Loadout = Loadout(),
    internal val model: HeroModel = HeroModel.of(hero),
    /** What the hero walks in with: mana, the flasks' charges and draughts still running; null brings them full. */
    pools: HeroPools? = null,
    /** The stats that are a percent already: the lines laid on a monster fold by them. */
    private val percent: Set<String> = emptySet(),
    /** Бой Разлома недели: механики стража и правила забега; null - обычный бой, ни одного лишнего броска. */
    val rift: RiftCombat? = null,
    /** Прошлый бой героя выигран: силы [com.sperance.exileforge.rules.content.PowerEvent.FIGHT_CLEAR] ложатся в начале этого. */
    val cleared: Boolean = false,
    /** Вид боя: по нему правила ярости `combat.enrage`; без него - бой похода, со стражем или со стаей. */
    val kind: FightKind = FightKinds.expedition(foes),
    /** Ярость этого боя: по умолчанию - правило его вида, у ступени раша и вождя башни - раньше. */
    rage: EnrageRule = rules.enrage(kind),
    /** Счёт Предначертания на заход: бои захода делят его; без захода - свой. */
    val fateRun: FateRun = FateRun(),
    /** Как враги выходят на поле: раундами собранных стай или подкреплением по очереди. */
    muster: Muster = Muster.Rounds,
) {
    /** One side in motion: its pools, its clocks and what is on it; [index] is its place in the pack, -1 for the hero. */
    inner class Fighter(val side: Side, body: Combatant, life: Double, val index: Int = -1) {
        /** The sheet as it stands: the hero's changes under the auras of the foes still standing and with what lies on them. */
        var body: Combatant = body
            private set

        /** A monster's sheet taking the lines laid on it. */
        val model: BodyModel = BodyModel.of(body, percent)
        var life = life.coerceIn(0.0, body.maxLife)
        var shield = body.maxShield

        /** Mana: the hero's comes in from the fight before, a monster's is full. */
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

        /** Босс боя: уникальный враг и выше - стражи, боссы карт, башни и Разлома; на нём свои потолки (добивание, провокация, силы). */
        val chief: Boolean get() = side == Side.MONSTER && index >= 0 && foes[index].rarity >= MonsterRarity.UNIQUE

        /** Stunned until then: nothing is swung before it. A freeze holds by its own ailment, so lifting it frees at once. */
        var heldUntil = 0.0
        var lastHit = -1e9

        /** Vampirism within the current second: [leechStart] opens it, [leeched] is what it has given back so far. */
        var leechStart = -1e9
        var leeched = 0.0

        /** Восстановление монстра в текущей секунде: [recoveryStart] открывает её, [recovered] - сколько вернули реген и вампиризм. */
        var recoveryStart = -1e9
        var recovered = 0.0

        /** Сколько из [amount] здоровья монстр ещё вернёт в секунду [time] под потолком [FightRules.recoveryRoom], учтя его. */
        fun recoveryRoom(amount: Double, time: Double): Double {
            if (time - recoveryStart >= 1.0) {
                recoveryStart = time
                recovered = 0.0
            }
            val allowed = min(amount, fight.recoveryRoom(body.maxLife, recovered))
            recovered += allowed
            return allowed
        }

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

        /** Damage over time gathered per ailment (и умению) since its last tick was logged, and when that was. */
        val ticking = mutableMapOf<Ticking, Double>()
        val tickedAt = mutableMapOf<Ailment, Double>()

        /** Buffs, curses and draughts on it. */
        val effects = mutableListOf<TimedEffect>()

        /** What a barrier still soaks, and until when. */
        var barrier = 0.0
        var barrierUntil = 0.0
        var invulnerableUntil = -1.0

        /** When each of its skills is ready again, by code for a monster and by slot for the hero. */
        val readyAt = mutableMapOf<String, Double>()

        /** Дно готовности умения героя по ячейке: раньше него силы перезарядок умение не готовят (`CombatCaps.readyFloor`). */
        val readyFloor = mutableMapOf<String, Double>()

        /** Умение, что боец применяет сейчас: удар и прочие умения ждут его конца; null - не применяет. */
        internal var casting: Casting? = null

        /** Рывок силы в этом замахе: следующий удар - сразу, а не через интервал. */
        internal var rushed = false

        /** Выжигание маны на монстре: до [burnUntil] его перезарядки идут в [burnFactor] раз медленнее. */
        internal var burnUntil = -1.0
        internal var burnFactor = 1.0

        /** Возврат здоровья бойца-монстра: у героя - общий [recoveries] с журналом. */
        internal val recouping = mutableListOf<Recovery>()

        /**
         * Провокация героя на этом враге: до [provokedUntil] спровоцирован силой [provoke], повторно - не
         * раньше [provokeFree].
         */
        internal var provokedUntil = -1.0
        internal var provokeFree = -1.0
        internal var provoke = 0.0
        val provoked: Boolean get() = provokedUntil > time

        /** Its low-life lines are on. */
        var low = false

        /** The buildups: each bar's fill from 0 to 1, when a blow last filled one, and how long each is shut after going off. */
        val buildup = DoubleArray(Buildup.entries.size)
        var builtAt = -1e9
        val buildupShutUntil = DoubleArray(Buildup.entries.size)

        /** A full stun bar: every damage heavier until then; a full freeze bar: the blow that breaks the ice is heavier; electrocuted: lightning heavier. */
        var stunnedUntil = 0.0
        var shatter = false
        var electrocutedUntil = 0.0

        /** The fullest bar, for the card: which and how full. */
        fun leading(): Pair<Buildup, Double>? = Buildup.entries.map { it to buildup[it.ordinal] }.filter { it.second > 0.005 }.maxByOrNull { it.second }

        /** On the field: the hero's side always, a foe from the start or, свита босса, once its phase calls it ([field]). */
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

        /**
         * Chill slows every action; the strongest chill counts. Единая скорость действий: холод и замедление аур
         * врагов ([Combatant.pace]) одним множителем растягивают и замах, и перезарядку всех умений.
         */
        fun slow() = (1 + (ailments.filter { it.ailment == Ailment.CHILLED }.maxOfOrNull { it.magnitude } ?: 0.0) / 100) / body.pace

        /** Shock makes every hit heavier; a curse can make it heavier still. */
        fun weakness() = 1 + held(Ailment.SHOCKED) / 100

        /**
         * Сила недуга [ailment] на бойце: сильнейший экземпляр, усиленный приёмом недуга на нём (`STOCK_<слово>_TAKEN`),
         * под потолком правила (`ceiling`); нет недуга - 0. Её читают шок, восстановление и плитка недуга.
         */
        fun held(ailment: Ailment): Double {
            val strongest = ailments.filter { it.ailment == ailment }.maxOfOrNull { it.magnitude } ?: return 0.0
            return heldStrength(ailment, strongest, body, ruleOf[ailment]?.first)
        }

        /**
         * Доля восстановления запаса [pool] бойца в этот миг - одна функция правил [RecoveryShare.share]: лист («меньше
         * регенерации», запрет, дно) и недуги, что режут этот запас по `curbs` своего правила.
         */
        fun share(pool: RecoveryPool): Double = RecoveryShare.share(body, pool, curb(pool))

        /** Сколько процентов запаса [pool] режут недуги на бойце ([RecoveryShare.curb]) - по их силе [held]. */
        fun curb(pool: RecoveryPool): Double {
            if (ailments.isEmpty()) return 0.0
            val active = ailments.mapTo(HashSet()) { it.ailment }.associate { it.name to held(it) }
            return RecoveryShare.curb(ailmentRules.keys, pool, active)
        }
        fun stacks(ailment: Ailment) = ailments.count { it.ailment == ailment }

        /** Под недугом: висит хоть одно состояние-недуг ([Ailment.ailing]) - Разложение и урон умения не в счёт. */
        val ailed: Boolean get() = ailments.any { it.ailment.ailing }

        /** Стаки [ailment], что действуют: у каждого источника - свои сильнейшие под его пределом ([actingStacks]). */
        fun acting(ailment: Ailment): List<ActiveAilment> = actingStacks(ailments.filter { it.ailment == ailment }, { it.source to it.foe }, { it.limit }, { it.magnitude })

        /**
         * Уязвимость к урону [type] (Разложение): 1 + сила действующих стаков недугов-уязвимостей этого вида
         * (правило `exposes`), %, усиленная приёмом недуга на бойце. Удар и тик урона во времени этого вида растут на неё.
         */
        fun exposure(type: DamageType): Double {
            if (ailments.isEmpty()) return 1.0
            var more = 0.0
            exposing.forEach { (ailment, kind) -> if (kind == type) more += acting(ailment).sumOf { it.magnitude } * body.ailmentTaken(ailment) }
            return 1 + more / 100
        }
    }

    /** Выход врагов на поле (раунды, подкрепления, [Muster]). */
    private val mustered: Muster.Field = muster.field(foes, fight)

    /** Смены поля боя: раунды собранных стай или смены подкреплений; [round] - идущая, с 0, из [rounds]. */
    val rounds: Int get() = mustered.rounds
    val round: Int get() = mustered.round

    /** Когда началась идущая смена поля: ярость каждой смены считается от её начала. */
    private var roundAt = 0.0

    /** Секунды идущей смены поля - часы ярости. */
    internal val rageClock: Double get() = time - roundAt

    /**
     * Кто на поле, по местам: первые сразу, сильнейший первым; свита босса встаёт по зову фаз на следующее место
     * ([enterField]). Павший держит своё место до конца раунда или до подкрепления, что встаёт на него.
     */
    private val places: MutableList<Int> = mustered.opening().toMutableList()

    /** Враг [index] ещё ждёт своего выхода: не на поле, цел и не свита. */
    fun waits(index: Int): Boolean = mustered.waits(index)

    /**
     * Враги выходят на поле, если пора (следующий раунд, подкрепление): новые подходят к герою, новый раунд
     * уводит павших с поля, новая смена начинает ярость заново. Герой остаётся каким был. False - никто не вышел.
     *
     * Ауры вставших врагов ложатся на героя.
     */
    internal fun muster(): Boolean {
        val join = mustered.join(foeFighters.count { it.alive }) ?: return false
        if (join.rage) {
            roundAt = time
            enrage = 0
            calm()
        }
        if (join.fresh) places.clear()
        join.entering.forEach { index ->
            val fallen = places.indexOfFirst { !foeFighters[it].alive && !foes[it].summoned }
            val place = if (fallen >= 0) fallen.also { places[it] = index } else places.size.also { places += index }
            foeFighters[index].enter(place, time)
        }
        remake(heroFighter)
        return join.entering.isNotEmpty()
    }

    /** Место врага [index] на поле; -1 - свита, что ещё не звана. */
    fun place(index: Int): Int = places.indexOf(index)

    /** Званая свита [index] встаёт на следующее место и подходит к герою. */
    internal fun enterField(index: Int) {
        if (index in places) return
        places += index
        foeFighters[index].enter(places.lastIndex, time)
    }

    /**
     * Слоты вокруг босса: их делят свита и тотемы; пустые - null. Свита при полных слотах не встаёт, тотем вытесняет
     * самый старый тотем. У боя без босса с фазами или тотемами слотов нет.
     */
    val slotHolders: Array<SlotHolder?> = arrayOfNulls(max(foes.maxOfOrNull { it.slots } ?: 0, rift?.slots ?: 0))

    /** Тотемы, что стоят сейчас, по порядку, в котором встали. */
    val totems: List<StandingTotem> get() = standingTotems
    internal val standingTotems = mutableListOf<StandingTotem>()
    internal var totemSerial = 0

    /** Когда босс со своими тотемами ставит следующий, по номеру врага. */
    internal val totemAt = mutableMapOf<Int, Double>()
    val foeFighters: List<Fighter> = foes.mapIndexed { i, foe -> Fighter(Side.MONSTER, foe.body, foe.body.maxLife, i) }
        .also { all -> places.forEachIndexed { place, i -> all[i].enter(place, 0.0) } }
    val heroFighter = Fighter(Side.HERO, hero.under(auras()), heroLife)

    /** Все бойцы боя: герой, затем враги; состав за бой не меняется - список один. */
    internal val everyone: List<Fighter> = listOf(heroFighter) + foeFighters

    /** Очередь хода шага ([everyone] по `nextAttack`): один буфер на бой, без новой копии и сортировки на каждый шаг. */
    internal val turnOrder = ArrayList<Fighter>(everyone.size)

    /** Отклики Предначертания героя в бою (и судьбоносных строк) и их состояние в этом бою. */
    internal val fated: List<FateReaction> = FateReaction.of(kit)
    internal val fateFight = FateFight()

    /**
     * The auras of the foes still standing: одинаковые ауры не складываются - действует сильнейшая, разные - вместе.
     */
    internal fun auras(): Map<String, Double> {
        val strongest = mutableMapOf<String, Double>()
        foeFighters.filter { it.alive }.forEach { foe -> foe.body.auras.forEach { (stat, value) -> strongest.merge(stat, value, ::max) } }
        return strongest
    }

    /**
     * Правила недугов, что знает клиент, с видом урона, что их несёт; без вида (изранение и иссушение) - null: недуг несёт
     * любой урон удара. Вид, которого клиент не знает, выкидывает правило.
     */
    internal val ailmentRules: Map<AilmentRule, Pair<Ailment, DamageType?>> = rules.ailments.mapNotNull { rule ->
        val ailment = Ailment.of(rule.ailment) ?: return@mapNotNull null
        val type = rule.type?.let { DamageType.of(it) ?: return@mapNotNull null }
        rule to (ailment to type)
    }.toMap()

    /** Недуги, что подавляют восстановление цели: поджог, яд, кровотечение по правилу. */
    private val suppressing: Set<Ailment> = fight.suppression.ailments.mapNotNull { code: String -> Ailment.of(code) }.toSet()

    /**
     * Восстановление врага героя [amount] здоровья за этот миг: урезано подавлением, пока на нём недуг героя
     * из [suppressing], затем - потолком в секунду. Герою - как есть.
     */
    internal fun recovery(me: Fighter, amount: Double): Double {
        if (me.side != Side.MONSTER || amount <= 0) return amount
        val ailed = me.ailments.any { it.source == Side.HERO && it.ailment in suppressing }
        val suppressed = amount * (1 - fight.suppression.share(heroFighter.body.recoverySuppression, me.body.suppressionAvoid, ailed))
        return me.recoveryRoom(suppressed, time)
    }

    internal val ruleOf: Map<Ailment, Pair<AilmentRule, DamageType?>> = ailmentRules.entries.associate { (rule, what) -> what.first to (rule to what.second) }

    /** Недуги-уязвимости (правило `exposes`: Разложение) и вид урона, что они растят. */
    private val exposing: List<Pair<Ailment, DamageType>> = ruleOf.mapNotNull { (ailment, ruled) -> ruled.second?.takeIf { ruled.first.exposes }?.let { ailment to it } }

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
    private var carry = 0.0
    internal val log = mutableListOf<CombatEvent>()
    val events: List<CombatEvent> get() = log

    /** Последнее окно журнала новыми сверху ([newestFirst]): то же, пока журнал не вырос. */
    private var newest: NewestFirst? = null

    /** Журнал новыми сверху: окно без копии, одно и то же, пока не пришла новая строка. */
    fun newestFirst(): List<CombatEvent> = newest?.takeIf { it.size == log.size } ?: NewestFirst(log, log.size).also { newest = it }
    internal val fallenOrder = mutableListOf<Int>()

    /** The foes that fell, in the order they fell: each is a kill to report the moment it happens. */
    val fallen: List<Int> get() = fallenOrder

    /** A slot whose opening condition has fired this fight, and the slots tapped since the last slice. */
    internal val opened = BooleanArray(kit.actives.size)
    internal val taps = mutableSetOf<Int>()
    internal val charges = DoubleArray(kit.flasks.size) { i -> kit.flasks[i]?.sheet?.let { pools?.charges?.getOrNull(i)?.coerceIn(0.0, it.maxCharges) ?: it.maxCharges } ?: 0.0 }
    internal val flaskOpened = BooleanArray(kit.flasks.size)
    internal val drinks = mutableSetOf<Int>()
    internal val triggerReady = mutableMapOf<String, Double>()
    internal val recoveries = mutableListOf<Recovery>()

    /** Регенерация, деген и отложенный урон героя с прошлой строки итога: одна строка журнала в секунду. */
    internal val lifeMeter = LifeMeter()

    /** Итог здоровья героя за последнюю секунду: плитка «−12/с» у портрета и её разбор. */
    var lifeFlow: LifeFlow = LifeFlow.NONE
        internal set

    /** Метки героя на врагах: по месту врага - одна на цель. */
    internal val marks = mutableMapOf<Int, BattleMark>()

    /** The next blow of the hero is a critical strike (a cloak of shadows). */
    internal var nextCrit = false

    /** Сколько ещё применений умений героя в этом бою мгновенны («Вечерня», `PowerAct.QUICKEN`). */
    internal var quickCasts = 0

    /** How deep in answers the fight is: an answer may set off one more, never a chain. */
    internal var depth = 0

    /** Идёт эхо применения (руна эха или сила `RECAST`): строки лога, что оно оставит, помечены [CombatEvent.echo]. */
    internal var echoing = false
    internal var belowLow = false
    internal var shieldUp = true

    /** The hero's powers: the unique items' answers to what happens here. */
    internal val powers = PowerRunner(this, kit.powers, cleared, pools?.carry?.counts.orEmpty())

    /** The hero's frenzy, power and endurance charges: none as a fight opens. */
    internal val heroCharges = HeroCharges(kit.charges)

    /** The delayed life damage still to come. */
    internal val delayed = mutableListOf<Delayed>()

    /** The flasks a hero drinks all at once as the fight opens are behind them. */
    internal var autoDrunk = false

    /** When the hero last killed, blocked and struck critically: what «recently» of a conditional line reads. */
    internal var killedAt = NEVER
    internal var blockedAt = NEVER
    internal var critAt = NEVER

    /** Отражённый урон по герою (В-5): секунда боя [reflectSecond] и сколько уже снято в ней - под `caps.reflected`. */
    internal var reflectSecond = -1L
    internal var reflectTaken = 0.0

    /** Whether the sheet or a flask of the belt has conditional lines at all, and the hero's conditions at the last look. */
    internal val conditioned = model.conditional(Condition.entries.toSet()).isNotEmpty() || kit.flasks.any { flask -> flask?.sheet?.lines?.any { it.condition != null } == true }
    internal var conditions: Set<Condition> = emptySet()

    init {
        pools?.flaskLeft?.forEachIndexed { i, left ->
            val flask = kit.flasks.getOrNull(i) ?: return@forEachIndexed
            if (left <= 0) return@forEachIndexed
            val draught = flask.sheet.draught(heroFighter.body, heroFighter.life, 0.0)
            heroFighter.effects += TimedEffect(EffectKind.FLASK, flask.sheet.code, draught.lines, left, draught.duration, slot = i, conditional = draught.conditional)
            pools?.rates?.getOrNull(i)?.takeIf { it.flows }?.let { recoveries += Recovery(it.life, it.mana, left, i, draught.lifeOnly, it.toMana, it.toShield) }
            flaskOpened[i] = true
        }
        val carried = pools?.carry?.takeIf { !it.bare }?.also(::carryIn) != null
        everyone.forEach(::wear)
        if (conditioned) conditions = heroConditions()
        if (heroFighter.effects.isNotEmpty() || conditions.isNotEmpty() || carried) remake(heroFighter)
        heroFighter.mana = (pools?.mana ?: Double.MAX_VALUE).coerceIn(0.0, manaCap())
        pools?.carry?.shield?.let { heroFighter.shield = it.coerceIn(0.0, heroFighter.body.maxShield) }
        shieldUp = heroFighter.shield > 0
    }

    /**
     * Несомое [carry] ложится на героя к началу боя: недуги - от врагов, с первым тиком через секунду; эффекты и заряды -
     * остатком своего времени.
     */
    private fun carryIn(carry: HeroCarry) {
        carry.ailments.forEach { a ->
            heroFighter.ailments += ActiveAilment(a.ailment, a.left, a.magnitude, a.duration, Side.MONSTER, CarriedAilment.foe(a.origin), chaos = a.chaos, limit = a.limit)
            if (a.ailment.hurts) heroFighter.tickedAt.putIfAbsent(a.ailment, 0.0)
        }
        carry.effects.forEach { e -> heroFighter.effects += TimedEffect(e.kind, e.source, e.lines, e.left, e.duration, e.counter, conditional = e.conditional, stacks = e.stacks) }
        carry.charges.forEach { heroCharges.carryIn(it) }
    }

    fun foe(index: Int) = foeFighters[index]
    val heroLife: Double get() = heroFighter.life
    val heroMana: Double get() = heroFighter.mana

    /** The hero's mana the auras leave free. */
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
        foes.flatMap { it.skills + it.phases.mapNotNull(FoePhase::skill) }.associate { it.code to it.icon }

    /**
     * Пассивные умения героя - ауры, бонусы - плитками полки «на весь бой»: что кладут по листу начала боя. Только
     * показ: лист боя их строки уже несёт.
     */
    internal val passiveViews: List<EffectView> by lazy {
        kit.passiveSources(heroFighter.body).map { (skill, lines) ->
            EffectView(skill.skill.code, EffectKind.BUFF, 1f, FOREVER, skill.skill.icon, lines = lines, passive = true)
        }
    }

    internal fun draughtOf(slot: Int): TimedEffect? = heroFighter.effects.firstOrNull { it.kind == EffectKind.FLASK && it.slot == slot && it.until > time }
    internal fun skillsFree(): Boolean = kit.flasks.indices.any { i -> kit.flasks[i]?.sheet?.skillsFree == true && draughtOf(i) != null }
    internal fun slotKey(slot: Int) = "#$slot"
    internal fun cost(kitSkill: KitSkill, level: Int): Double = kitSkill.mana(level) * heroFighter.body.skillCost

    /**
     * Moves the fight on by [dt] seconds in fixed slices, so a frame's length never changes what happens.
     *
     * Когда бой кончен, часы ещё идут: выпад и затухание последнего удара доигрывают.
     */
    fun advance(dt: Double) {
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

    /** Singles out foe [index]; the same foe again, or a fallen one, gives the choice back to the class. */
    fun focus(index: Int?) {
        val was = focus
        focus = index?.takeIf { it != focus && foeFighters.getOrNull(it)?.alive == true }
        if (was != null && focus != null && foeFighters[was].alive) trial.focusChanges++
    }

    /** Foes taunting right now: while any stands, only they can be struck. */
    private fun taunters() = foeFighters.filter { it.alive && it.body.taunt }

    /**
     * Whether the hero can strike foe [index] right now: any foe on screen, by any weapon; a taunter
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

    /**
     * The blow whose lunge is on screen right now, and how far into it the scene is (0..1).
     *
     * Журнал идёт по времени: с конца читаются только строки последнего выпада.
     */
    fun lunge(): Pair<CombatEvent, Double>? {
        var i = log.size - 1
        while (i >= 0) {
            val event = log[i--]
            if (time - event.time > rules.lunge) return null
            if (event.action != Action.TICK && event.action != Action.REFLECT && event.action != Action.FLASK && !event.onSelf && time - event.time >= -rules.lunge) {
                return event to ((time - event.time + rules.lunge) / (2 * rules.lunge)).coerceIn(0.0, 1.0)
            }
        }
        return null
    }

    /** How far [fighter] is into its next swing, 0 just after one and 1 as the next lands. */
    fun swing(fighter: Fighter): Float = when {
        outcome != null || !fighter.alive -> 0f
        else -> (1 - (fighter.nextAttack - time) / fighter.attackInterval).toFloat().coerceIn(0f, 1f)
    }

    /** The log as a test or a report reads it, once the fight is over. */
    fun log(): CombatLog = CombatLog(log.toList(), checkNotNull(outcome) { "the fight is not over" }, heroLife, duration)

    /** A hit worked out up to its landing: [land] finishes it with the rolls and where the damage went. */
    internal var pendingHit: HitTrace? = null

    /** What the card needs beyond the numbers: the hero's sheet and the foes' rolls. */
    internal val origin = TraceOrigin(model, foes)

    /** The foes that have swung once already: an ambusher's first blow is the heavier one. */
    internal val swung = mutableSetOf<Int>()

    /** Сработавшие шаги фаз: номер врага и шага - каждый срабатывает раз за бой. */
    internal val phased = mutableSetOf<Pair<Int, Int>>()

    /** Умения, что фазы дали врагам, по номеру врага: он применяет их вслед за своими. */
    internal val learned = mutableMapOf<Int, MutableList<com.sperance.exileforge.rules.content.MonsterSkill>>()

    /** Свита, что уже звана, и павшая: её смерть не убийство, о ней не сообщают. */
    internal val called = mutableSetOf<Int>()
    internal val retinueDown = mutableSetOf<Int>()

    /** Foes whose rage has been lit, and by which trait: it lights once and holds to the end of the fight. */
    internal val enraged = mutableSetOf<Pair<Int, String>>()

    /** Отклики свойств каждого врага, по его номеру: свойства с откликом на силе его редкости. */
    internal val traitsAt: List<List<TraitAt>> = foes.map { foe -> foe.traits.mapNotNull { trait -> trait.trigger?.let { TraitAt(trait, it, foe.traitPower) } } }

    /** Свойства, что ответят следующим замахом ([com.sperance.exileforge.rules.content.TraitAct.RIPOSTE]): номер врага и код. */
    internal val riposting = mutableSetOf<Pair<Int, String>>()

    /** Сколько раз сложилось свойство врага ([com.sperance.exileforge.rules.content.TraitAct.STACK]): номер врага и код - число. */
    internal val stacked = mutableMapOf<Pair<Int, String>, Int>()

    /** Правило ярости этого боя. */
    internal val rageRule: EnrageRule = rage

    /** Ступень ярости (любого боя): сколько ступеней [rageRule] уже прошло. */
    internal var enrage = 0

    /** Ступень ярости, что уже лежит на каждом враге: вставший позже догоняет её. */
    internal val raged = mutableMapOf<Int, Int>()

    /** Враги уже в ярости: хоть одна ступень прошла. */
    val furious: Boolean get() = enrage > 0

    /** Страж боя: первый враг редкости босса, с фазами или тотемами; null - бой без стража. */
    internal val guardian: Int? by lazy {
        foes.indices.firstOrNull { foes[it].guards }
    }

    /** След боя для Испытания чемпиона: флаконы, низшая доля здоровья, смены цели. */
    val trial = RiftTrace()

    /** Механики Разлома; null вне Разлома. */
    internal val riftFight: RiftFight? = rift?.let { RiftFight(it, kit.actives.size, kit.flasks.size) }

    init {
        fateOpens()
        riftFight?.let { riftOpen(it) }
    }

    companion object {
        /** The slice the fight is stepped in: fine enough for a 5-per-second swing, fixed so a fight is a function of its seed. */
        const val STEP = 1.0 / 60

        /** How often an ailment's damage is written into the log. */
        const val TICK = 1.0

        /** A time long gone: nothing happened «recently» at the fight's start. */
        const val NEVER = -1e9

        /** A buff worn the whole fight. */
        const val FOREVER = 1e9
    }
}
