package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.FlaskReadout
import com.sperance.exileforge.rules.sheet.FlaskSheet

/**
 * Мера сводки фляги ([FlaskReadout], 4.2.0), что сравнивает карточка: [read] - её число у сводки, [higherBetter] - больше ли
 * значит лучше (расход заряда - наоборот). Строки, что фляга кладёт на героя, - отдельные строки [FlaskRow.stat].
 */
enum class FlaskMeasure(val higherBetter: Boolean, val read: (FlaskReadout) -> Double) {
    LIFE(true, FlaskReadout::life),
    MANA(true, FlaskReadout::mana),
    INSTANT(true, FlaskReadout::instant),
    DURATION(true, FlaskReadout::duration),
    CHARGES(true, FlaskReadout::maxCharges),
    PER_USE(false, FlaskReadout::perUse),
    USES(true, { it.uses.toDouble() }),
    EFFECT(true, FlaskReadout::effect),
    LINE(true, { 0.0 }),
}

/**
 * Строка «сейчас → станет» фляги: мера [measure] (у строки, что фляга кладёт на героя, - [stat] и [op]), у фляги в гнезде
 * [before] (0 - гнездо пусто) и у этой [after].
 */
data class FlaskRow(val measure: FlaskMeasure, val before: Double, val after: Double, val stat: String = "", val op: Op = Op.ADD) {
    /** Доля изменения в сторону «лучше»: рост меры, где больше - лучше, и падение расхода; новая мера - целиком. */
    val gain: Double get() {
        val change = if (before != 0.0) {
            (after - before) / kotlin.math.abs(before)
        } else if (after != 0.0) {
            1.0
        } else {
            0.0
        }
        return if (measure.higherBetter) change else -change
    }

    val shift: Shift get() = Shift.of(gain)
}

/**
 * Вердикт фляги (4.2.0): строки её листа против фляги в том же гнезде при бонусах героя к флягам. Лист героя фляга не меняет,
 * поэтому урон и защита ей не мера - карточка сравнивает лечение, длительность, заряды, эффект и строки.
 */
data class FlaskVerdict(val rows: List<FlaskRow>) : GearVerdict {
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
            return of(replaced?.let { readout(index, stats, it) }, after)
        }

        /** Строки сводки [after] против [before]: мера, что у обеих ноль, не печатается; сила эффекта - только у фляг со строками. */
        fun of(before: FlaskReadout?, after: FlaskReadout): FlaskVerdict {
            val lined = after.lines.isNotEmpty() || before?.lines.orEmpty().isNotEmpty()
            val measures = FlaskMeasure.entries.filter { it != FlaskMeasure.LINE && (lined || it != FlaskMeasure.EFFECT) }.map { FlaskRow(it, before?.let(it.read) ?: 0.0, it.read(after)) }
            val was = before?.lines.orEmpty().groupBy { it.stat to it.op }.mapValues { (_, ops) -> ops.sumOf { it.value } }
            val will = after.lines.groupBy { it.stat to it.op }.mapValues { (_, ops) -> ops.sumOf { it.value } }
            val lines = (was.keys + will.keys).distinct().map { key -> FlaskRow(FlaskMeasure.LINE, was[key] ?: 0.0, will[key] ?: 0.0, key.first, key.second) }
            return FlaskVerdict((measures + lines).filter { it.before != 0.0 || it.after != 0.0 })
        }
    }
}
