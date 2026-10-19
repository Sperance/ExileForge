package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.BuffStack
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.MonsterTrait
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.sheet.FlaskKind

/**
 * An ailment on a fighter: what, until when, how hard, and who put it there — the side and, for a
 * monster's, which foe of the pack ([foe]).
 */
data class ActiveAilment(
    val ailment: Ailment,
    val until: Double,
    val magnitude: Double,
    val duration: Double,
    val source: Side,
    val foe: Int = 0,
    /** Put there by a spell: a kill by it is a spell's kill. */
    val spell: Boolean = false,
    /** Burns as chaos: an ignite of a striker whose ignites deal chaos goes around the shield. */
    val chaos: Boolean = false,
    /**
     * Сколько сильнейших стаков этого недуга действуют (предел правила и «+N к пределу» наложившего); null - все.
     * Висят все стаки - слабые вступают, когда сильные истекут.
     */
    val limit: Int? = null,
    /** Умение, чей урон во времени это: тик идёт в разбор боя этим умением; null - недуг удара. */
    val skill: String? = null,
)

/** Строка тика: недуг и умение, чей это урон во времени (null - недуг удара), - отдельной строкой журнала и разбора. */
data class Ticking(val ailment: Ailment, val skill: String?)

/**
 * The traits of this roll, as the content names them.
 *
 * Враг стаи, как его берёт бой: его лист; каждый враг на экране бьёт с первой секунды, и его можно ударить любым оружием - рядов нет. Его
 * редкость нужна умению, что ждёт редкого или босса, умения - что он применяет.
 */
fun com.sperance.exileforge.rules.roll.RolledMonster.traitsIn(index: com.sperance.exileforge.rules.content.ContentIndex): List<MonsterTrait> = traits.mapNotNull(index.campaign.traits.byCode::get)

data class Foe(
    val body: Combatant,
    val rarity: MonsterRarity = MonsterRarity.NORMAL,
    val skills: List<MonsterSkill> = emptyList(),
    /** Its roll at the fight's [level]: what the log's card lays its stats out with. */
    val origin: com.sperance.exileforge.rules.roll.RolledMonster? = null,
    val level: Int = 0,
    /** Its traits and their strength for its rarity: their lines are on [body] already, their answers the fight plays. */
    val traits: List<MonsterTrait> = emptyList(),
    val traitPower: Double = 1.0,
    /** Фазы босса: шаблон [phase] и его шаги по убыванию порога; пусто - фаз нет. */
    val phase: String? = null,
    val phases: List<FoePhase> = emptyList(),
    /** Свита: номер босса, чья фаза её зовёт; в бой она встаёт только по зову, добычи не даёт и падает с ним. */
    val summonOf: Int? = null,
    /**
     * Тотемы: свои тотемы босса [totems] - ставит случайный раз в [totemEvery] секунд, первый через [totemFirst];
     * [slots] - слоты вокруг него для свиты и тотемов, 0 - у врага их нет.
     */
    val totems: List<FoeTotem> = emptyList(),
    val totemEvery: Double = 0.0,
    val totemFirst: Double = 0.0,
    val slots: Int = 0,
    /** Раунд боя, в котором враг выходит на поле: стаи, собранные боем, бьются по очереди; 0 - с начала. */
    val round: Int = 0,
    /** Осквернён: несёт печать Скверны - цель условия `VS_BLIGHTED`, а герой со снижением от Скверны получает от него меньше. */
    val tainted: Boolean = false,
) {
    val summoned: Boolean get() = summonOf != null
}

/**
 * What lies on a fighter for a while; [harmful] - во вред тому, на ком лежит. [HINDER] - помеха свойства
 * монстра на герое: вредит, но не проклятие (его не снимает и не считает ничто, что ищет проклятия). [MARK] - метка
 * героя с подрывом на враге: не проклятие, `counter` - доля урона, что она копит, в процентах.
 */
enum class EffectKind(val harmful: Boolean = false) { BUFF, CURSE(harmful = true), FLASK, HINDER(harmful = true), MARK(harmful = true) }

/**
 * A buff, a curse or a flask's draught on a fighter: its [lines] until [until], named by its
 * [source] — a skill's code or a flask's. [counter] strikes back at a blocked blow with that percent of
 * the weapon; [slot] is a flask's place on the belt.
 */
data class TimedEffect(
    val kind: EffectKind,
    val source: String,
    val lines: List<StatLine>,
    val until: Double,
    val duration: Double,
    val counter: Double = 0.0,
    val slot: Int = -1,
    /** Условные строки: держатся, пока держится их условие, - как условные строки листа. */
    val conditional: List<ConditionalLine> = emptyList(),
    /** Сколько стопок силы в его строках: следующая стопка растёт от них, и после переноса между боями. */
    val stacks: Int = 1,
    /** Слой баффа, если эффект - бафф вида ([BuffStack]): вечный или временный, с силой; иначе null. */
    val buff: BuffStack? = null,
) {
    /** Строки на бойце при условиях героя [active]: постоянные и условные, чьё условие держится. */
    fun holding(active: Set<Condition>): List<StatLine> = if (conditional.isEmpty()) lines else lines + conditional.filter { !it.condition.target && it.condition in active }.map { it.line }

    /** Увеличение урона против цели в состояниях [states] - условные строки цели ([Condition.target]), как `against` листа. */
    fun against(states: Set<Condition>): Double = conditional.filter { it.condition.target && it.condition in states }.sumOf { it.line.value }
}

/** Строка [line], что действует, пока держится условие [condition]: условная строка фляги во время глотка. */
data class ConditionalLine(val condition: Condition, val line: StatLine)

/**
 * What the hero carries from fight to fight: life, mana, each flask's charges and how long
 * its draught still runs, in seconds. [carry] - недуги, баффы и проклятия, щит и заряды между боем и картой ([HeroCarry]);
 * бой отдаёт его отдельно ([Battle.carry]): испытания и Разлом его не несут.
 */
data class HeroPools(
    val life: Double,
    val mana: Double,
    val charges: List<Double> = emptyList(),
    val flaskLeft: List<Double> = emptyList(),
    val rates: List<DraughtRate> = emptyList(),
    val carry: HeroCarry = HeroCarry.NONE,
)

/**
 * How much life and mana a running draught still gives each second. It rides with the draught's
 * time left, so a flask drunk at the end of a fight keeps healing on the map and in the next fight.
 */
data class DraughtRate(
    val life: Double = 0.0,
    val mana: Double = 0.0,
    /** Доли вылеченного глотком здоровья в ману и энергощит. */
    val toMana: Double = 0.0,
    val toShield: Double = 0.0,
    /** Глоток только лечит: гаснет на полном здоровье - в бою и на карте. */
    val stopsAtFull: Boolean = false,
) {
    val flows: Boolean get() = life > 0 || mana > 0
}

/** An active slot as the fight's buttons draw it: how far it has recovered (1 ready), and whether the mana is there. */
data class SkillView(
    val slot: Int,
    val code: String,
    val icon: String,
    val level: Int,
    val cost: Int,
    val ready: Float,
    val affordable: Boolean,
    val condition: SlotCondition,
    val seconds: Double = 0.0,
    /** Сколько ещё секунд слот заперт «Запечатыванием» Стража Врат; 0 - свободен. */
    val locked: Double = 0.0,
    /** Доля пройденного применения слота; 0 - слот не применяется. */
    val casting: Float = 0f,
)

/** A flask of the belt as its button draws it: charges, the price of a draught, and how much of one is left (0 none). */
data class FlaskView(
    val slot: Int,
    val code: String,
    val kind: FlaskKind,
    val charges: Int,
    val maxCharges: Int,
    val perUse: Int,
    val active: Float,
    val condition: SlotCondition,
    /** Сколько ещё секунд флакон заперт «Запечатыванием» Стража Врат; 0 - свободен. */
    val locked: Double = 0.0,
) {
    val usable: Boolean get() = charges >= perUse && active <= 0f && locked <= 0.0
}

/**
 * Сила уникального предмета героя плиткой боя: [stat] - код силы, [value] - её ролл на листе. [standing] - постоянная (строки листа,
 * силы слотов, строки «пока держится условие»): плитка на рельсе «на весь бой». Сила-событие - на временной полке: [flash] - доля
 * вспышки срабатывания (1 - только что, 0 - нет), [fired] - сколько раз сработала за бой, [every] - «каждое N-е» (0 - без счёта) и
 * [count] - счёт к нему, [cooldown] - перезарядка в секундах (0 - нет), [ready] - сколько секунд до готовности, [spent] - раз на бой и
 * уже сработала. [holders] - источник силы: id надетых вещей, что её несут (лист эффектов называет вещь по имени); пусто - носитель
 * не найден.
 */
data class PowerView(
    val stat: String,
    val value: Double,
    val standing: Boolean,
    val flash: Float = 0f,
    val fired: Int = 0,
    val every: Int = 0,
    val count: Int = 0,
    val cooldown: Double = 0.0,
    val ready: Double = 0.0,
    val spent: Boolean = false,
    val holders: List<String> = emptyList(),
) {
    /** Счёт «каждое N-е» парой (сколько набрано до следующего, N); у силы без такого счёта - null. */
    val nth: Pair<Int, Int>? get() = if (every > 1) count % every to every else null

    /** Доля перезарядки, что ещё осталась: 1 - только что сработала или потрачена на бой, 0 - готова. */
    val cooling: Float get() = when {
        spent -> 1f
        cooldown > 0 -> (ready / cooldown).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }
}

/**
 * A buff or a curse on a fighter as its tile shows it: what, of which kind, and how much of it is left;
 * строки, что эффект кладёт ([lines], для листа эффектов), и [passive] - пассивное умение героя (аура, бонус) на весь бой.
 */
data class EffectView(
    val source: String,
    val kind: EffectKind,
    val left: Float,
    val seconds: Double,
    val icon: String = "",
    val buff: BuffKind? = null,
    val lines: List<StatLine> = emptyList(),
    val passive: Boolean = false,
    /** Глоток флакона: вид флакона - плитка рисует его бутыль; null - не флакон. */
    val flask: FlaskKind? = null,
    /** Глоток погас на полном здоровье: плитка «прервано: здоровье полное», срока у неё нет. */
    val halted: Boolean = false,
) {
    /** Лежит весь бой: носимый с начала бафф ([Battle.wear]) или пассивное умение - полка «на весь бой», не таймер. */
    val lasting: Boolean get() = passive || seconds >= Battle.FOREVER / 2
}

/**
 * Whom the hero strikes when the player has not said, by class — the owner's table: a
 * Marauder, a Duelist and a Ranger go for the one that presses hardest, a Shadow finishes the
 * weakest, a Witch the one least resistant to her leading element, a Templar answers whoever
 * struck last, a Scion finishes the weakest while healthy and answers when hurt.
 */
enum class TargetRule {
    THREAT,
    WEAKEST,
    EXPOSED,
    AVENGE,
    ADAPTIVE,
    ;

    companion object {
        fun of(classCode: String?): TargetRule = when (classCode) {
            "SHADOW" -> WEAKEST
            "WITCH" -> EXPOSED
            "TEMPLAR" -> AVENGE
            "SCION" -> ADAPTIVE
            else -> THREAT
        }
    }
}

/**
 * The hero's side of a fight beyond the sheet: whom they pick by [rule]. Any weapon reaches every
 * foe on screen — the fight has no rows.
 */
data class HeroStance(val rule: TargetRule = TargetRule.THREAT) {
    companion object {
        fun of(classCode: String?) = HeroStance(TargetRule.of(classCode))
    }
}
