package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.AilmentRule
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.ChargeRules
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.RecoveryPool
import com.sperance.exileforge.rules.sheet.Recovery
import kotlin.math.max

/**
 * Что герой несёт между боем и картой, в обе стороны: недуги (урон во времени, Изранение и Иссушение, холод, шок и заморозка -
 * каждый своей строкой), временные баффы и проклятия, энергощит и заряды. Время - «сколько осталось», как у глотков флаконов
 * ([HeroPools.flaskLeft]): в бою - до его конца, на карте тает по часам дороги ([after]), в следующий бой входит остатком.
 * Не несёт: оглушение и удержание, перезарядки, барьер, неуязвимость, отложенный урон, полосы накопления и носимое весь бой.
 * Горение и яд ловушек карты - те же недуги ([AilmentLaw.laid]): одна система, и они тоже входят в следующий бой.
 */
data class HeroCarry(
    val ailments: List<CarriedAilment> = emptyList(),
    val effects: List<CarriedEffect> = emptyList(),
    /** Энергощит на конце боя; null - полный (заход только начат). */
    val shield: Double? = null,
    val charges: List<CarriedCharge> = emptyList(),
) {
    /** Нечего нести, кроме щита. */
    val bare: Boolean get() = ailments.isEmpty() && effects.isEmpty() && charges.isEmpty()

    /** На герое недуг, что ранит во времени: отдых вне боя под ним не идёт. */
    val hurting: Boolean get() = ailments.any { it.ailment.hurts }

    /** Прошло [dt] секунд вне боя: всё тает, истёкшее уходит. */
    fun after(dt: Double): HeroCarry = copy(
        ailments = ailments.mapNotNull { it.copy(left = it.left - dt).takeIf { a -> a.left > 0 } },
        effects = effects.mapNotNull { it.copy(left = it.left - dt).takeIf { e -> e.left > 0 } },
        charges = charges.mapNotNull { it.copy(left = it.left - dt).takeIf { c -> c.left > 0 } },
    )

    /** Строки, что несомое кладёт на героя вне боя: эффекты (условные - без условий боя) и заряды по правилам [rules]. */
    fun lines(rules: ChargeRules): List<StatLine> = effects.flatMap { it.lines } +
        charges.flatMap { charge -> rules.kinds[charge.kind]?.lines.orEmpty().map { StatLine(it.stat, it.op, (it.value ?: 0.0) * charge.count) } }

    companion object {
        val NONE = HeroCarry()
    }
}

/** Недуг на герое вне боя: [left] секунд из [duration], сила [magnitude] - как у [ActiveAilment]; [chaos] - горит хаосом. */
data class CarriedAilment(val ailment: Ailment, val left: Double, val magnitude: Double, val duration: Double, val chaos: Boolean = false)

/** Временный бафф или проклятие вне боя: [left] секунд из [duration]; остальное - как у [TimedEffect]. */
data class CarriedEffect(
    val kind: EffectKind,
    val source: String,
    val lines: List<StatLine>,
    val left: Double,
    val duration: Double,
    val icon: String = "",
    val counter: Double = 0.0,
    val conditional: List<ConditionalLine> = emptyList(),
)

/** Заряды одного вида вне боя: [count] штук ещё [left] секунд. */
data class CarriedCharge(val kind: ChargeKind, val count: Int, val left: Double)

/**
 * Сила недуга [ailment] (4.11.0): сильнейший экземпляр [strongest], усиленный приёмом недуга на бойце [body]
 * (`STOCK_<слово>_TAKEN`), под потолком правила [rule]. Одна формула боя ([Battle.Fighter.held]) и карты ([AilmentLaw]).
 */
internal fun heldStrength(ailment: Ailment, strongest: Double, body: Combatant, rule: AilmentRule?): Double {
    val taken = strongest * body.ailmentTaken(ailment)
    return rule?.capped(taken) ?: taken
}

/**
 * Урон в секунду недуга силой [magnitude] на бойце [body] под шоком [weakness]: приём урона во времени и самого недуга; что
 * ранит вообще - не меньше [minDot] за тик. Одна формула боя и карты.
 */
internal fun dotRate(ailment: Ailment, magnitude: Double, body: Combatant, weakness: Double, minDot: Double): Double {
    val rate = magnitude * weakness * body.dotTaken * body.ailmentTaken(ailment)
    return if (magnitude > 0) max(rate, minDot / Battle.TICK) else rate
}

/**
 * Недуги героя вне арены - по тем же законам, что в бою, из правил [rules]: сила ([heldStrength]), урон в секунду ([dotRate]) под
 * шоком, срез восстановления ([Recovery.curb]), стаки по правилу недуга (как `Battle.place`) и вид урона, что несёт недуг.
 */
class AilmentLaw(private val rules: CombatRules) {
    private val ruleOf: Map<Ailment, AilmentRule> = rules.ailments.mapNotNull { rule -> Ailment.of(rule.ailment)?.let { it to rule } }.toMap()

    /** Сила недуга [ailment] среди [ailments] на [body]; нет - 0. */
    fun held(body: Combatant, ailments: List<CarriedAilment>, ailment: Ailment): Double =
        ailments.filter { it.ailment == ailment }.maxOfOrNull { it.magnitude }?.let { heldStrength(ailment, it, body, ruleOf[ailment]) } ?: 0.0

    /** Урон в секунду недуга [active] на [body] под шоком из [ailments]. */
    fun perSecond(body: Combatant, ailments: List<CarriedAilment>, active: CarriedAilment): Double =
        dotRate(active.ailment, active.magnitude, body, 1 + held(body, ailments, Ailment.SHOCKED) / 100, rules.minDot)

    /** Сколько процентов запаса [pool] режут [ailments] на [body]. */
    fun curb(body: Combatant, ailments: List<CarriedAilment>, pool: RecoveryPool): Double {
        if (ailments.isEmpty()) return 0.0
        val active = ailments.mapTo(HashSet()) { it.ailment }.associate { it.name to held(body, ailments, it) }
        return Recovery.curb(rules.ailments, pool, active)
    }

    /** Новый недуг [fresh] к [ailments]: копится, если правило стакает, иначе остаётся сильнейший с дальним концом. */
    fun laid(ailments: List<CarriedAilment>, fresh: CarriedAilment): List<CarriedAilment> {
        val existing = ailments.filter { it.ailment == fresh.ailment }
        return when {
            ruleOf[fresh.ailment]?.stacks == true || existing.isEmpty() -> ailments + fresh
            existing.maxOf { it.magnitude } <= fresh.magnitude -> ailments - existing.toSet() + fresh
            else -> existing.maxBy { it.magnitude }.let { kept -> ailments - existing.toSet() + kept.copy(left = max(kept.left, fresh.left)) }
        }
    }

    /** Вид урона недуга [active]: хаос, если он горит хаосом, иначе - вид его правила. */
    fun type(active: CarriedAilment): DamageType? = if (active.chaos) DamageType.CHAOS else ruleOf[active.ailment]?.type?.let(DamageType::of)

    companion object {
        /** Недуг [active] идёт мимо щита: яд и всё, что горит хаосом. */
        fun piercing(active: CarriedAilment): Boolean = active.ailment == Ailment.POISONED || active.chaos
    }
}
