package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.StatGain
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.FlaskReadout
import com.sperance.exileforge.rules.sheet.FlaskSheet
import kotlin.math.abs
import kotlin.math.sign

/**
 * Мера сводки фляги ([FlaskReadout]), что сравнивает карточка: [read] - её число у сводки, [gain] - полярность меры (расход
 * заряда - «меньше лучше»). Строки, что фляга кладёт на героя, и механики фляги - отдельные строки [FlaskRow.stat] с полярностью
 * своей характеристики ([StatPolarity]).
 */
enum class FlaskMeasure(val gain: StatGain, val read: (FlaskReadout) -> Double) {
    LIFE(StatGain.HIGHER, FlaskReadout::life),
    MANA(StatGain.HIGHER, FlaskReadout::mana),
    INSTANT(StatGain.HIGHER, FlaskReadout::instant),
    DURATION(StatGain.HIGHER, FlaskReadout::duration),
    CHARGES(StatGain.HIGHER, FlaskReadout::maxCharges),
    PER_USE(StatGain.LOWER, FlaskReadout::perUse),
    USES(StatGain.HIGHER, { it.uses.toDouble() }),
    EFFECT(StatGain.HIGHER, FlaskReadout::effect),
    LINE(StatGain.HIGHER, { 0.0 }),

    /** Своя механика фляги без меры: заряды за крит, перелив, шанс не тратить заряд… - [FlaskRow.stat]. */
    MECHANIC(StatGain.HIGHER, { 0.0 }),
}

/**
 * Строка «сейчас → станет» фляги: мера [measure] (у строки, что фляга кладёт на героя, - [stat] и [op]), у фляги в гнезде
 * [before] (0 - гнездо пусто) и у этой [after]; [polarity] - что у строки лучше (у меры - своя, у строки - её характеристики).
 */
data class FlaskRow(
    val measure: FlaskMeasure,
    val before: Double,
    val after: Double,
    val stat: String = "",
    val op: Op = Op.ADD,
    val polarity: StatGain = measure.gain,
) {
    /**
     * Доля изменения в сторону «лучше» по полярности: рост меры, где больше - лучше, падение получаемого и расхода; новая строка -
     * целиком, со знаком своего числа; нейтральная - ноль.
     */
    val gain: Double get() {
        val change = if (before != 0.0) (after - before) / abs(before) else after.sign
        return when (polarity.better(change)) {
            true -> abs(change)
            false -> -abs(change)
            null -> 0.0
        }
    }

    val shift: Shift get() = Shift.of(gain)
}

/**
 * Вердикт фляги: строки её листа против фляги в том же гнезде при бонусах героя к флягам. Лист героя фляга не меняет,
 * поэтому урон и защита ей не мера - карточка сравнивает лечение, длительность, заряды, эффект и строки.
 */
data class FlaskVerdict(val rows: List<FlaskRow>, override val might: MightShift? = null) : GearVerdict {
    /** Строк, что станут лучше и хуже. */
    val better: Int get() = rows.count { it.shift == Shift.UP }
    val worse: Int get() = rows.count { it.shift == Shift.DOWN }

    /** Итог одной стрелкой: только лучше, только хуже или поровну. */
    val shift: Shift get() = when {
        better > 0 && worse == 0 -> Shift.UP
        worse > 0 && better == 0 -> Shift.DOWN
        else -> Shift.EVEN
    }

    override val score: Double get() = rows.sumOf { it.gain.coerceIn(-1.0, 1.0) }

    companion object {
        /** Сводка фляги [item] при листе героя [stats]; null - шаблона нет в контенте. */
        fun readout(index: ContentIndex, stats: Map<String, Double>, item: ItemInstance): FlaskReadout? = index.template(item.template)?.let { FlaskSheet.of(item, it, index).readout { stat -> stats[stat] ?: 0.0 } }

        /** Фляга [item] против [replaced] в гнезде (null - гнездо пусто) при листе героя [stats]; null - шаблона нет. */
        fun of(index: ContentIndex, stats: Map<String, Double>, item: ItemInstance, replaced: ItemInstance?): FlaskVerdict? {
            val after = readout(index, stats, item) ?: return null
            return of(replaced?.let { readout(index, stats, it) }, after, index)
        }

        /**
         * Строки сводки [after] против [before]: мера, что у обеих ноль, не печатается; сила эффекта - только у фляг со строками.
         * Строки и механики оценивает полярность их характеристик в контенте [index] ([StatPolarity]).
         *
         * Механики фляги - и те, что пропадут с заменой.
         */
        fun of(before: FlaskReadout?, after: FlaskReadout, index: ContentIndex?): FlaskVerdict {
            val lined = after.lines.isNotEmpty() || before?.lines.orEmpty().isNotEmpty()
            val measures = FlaskMeasure.entries.filter { it != FlaskMeasure.LINE && it != FlaskMeasure.MECHANIC && (lined || it != FlaskMeasure.EFFECT) }.map { FlaskRow(it, before?.let(it.read) ?: 0.0, it.read(after)) }
            val was = before?.lines.orEmpty().groupBy { it.stat to it.op }.mapValues { (_, ops) -> ops.sumOf { it.value } }
            val will = after.lines.groupBy { it.stat to it.op }.mapValues { (_, ops) -> ops.sumOf { it.value } }
            val lines = (was.keys + will.keys).distinct().map { key -> FlaskRow(FlaskMeasure.LINE, was[key] ?: 0.0, will[key] ?: 0.0, key.first, key.second, StatPolarity.of(index, key.first)) }
            val held = before?.mechanics.orEmpty()
            val mechanics = (held.keys + after.mechanics.keys).distinct().map { stat -> FlaskRow(FlaskMeasure.MECHANIC, held[stat] ?: 0.0, after.mechanics[stat] ?: 0.0, stat, polarity = StatPolarity.of(index, stat)) }
            return FlaskVerdict((measures + lines + mechanics).filter { it.before != 0.0 || it.after != 0.0 })
        }
    }
}
