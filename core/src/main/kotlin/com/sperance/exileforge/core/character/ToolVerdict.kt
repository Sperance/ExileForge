package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.WorkBonus
import kotlin.math.abs

/**
 * Мера итога ремесла: поле бонуса профессии [read] и характеристика [stat], что его задаёт, - по ней сравнение
 * называет строку и печатает число.
 */
enum class WorkMeasure(val stat: CoreStat, val read: (WorkBonus) -> Double) {
    SPEED(CoreStat.WORK_SPEED, WorkBonus::speed),
    YIELD(CoreStat.WORK_YIELD, WorkBonus::yield),
    LUCK(CoreStat.WORK_LUCK, WorkBonus::luck),
    EXPERIENCE(CoreStat.WORK_EXPERIENCE, WorkBonus::experience),
    FIND(CoreStat.WORK_FIND, WorkBonus::find),
    DOUBLE(CoreStat.WORK_DOUBLE, WorkBonus::double),
    SAVE(CoreStat.WORK_SAVE, WorkBonus::save),
    RARITY(CoreStat.WORK_RARITY, WorkBonus::rarity),
    ITEM_LEVEL(CoreStat.WORK_ITEM_LEVEL, WorkBonus::itemLevel),
    UNIQUE(CoreStat.WORK_UNIQUE, WorkBonus::unique),
}

/**
 * Вердикт инструмента профессии: итог ремесла «было → стало» - бонус профессии одной функцией правил с сервером
 * ([WorkBonus.of]: лист героя, строки инструмента, дар) с инструментом, что надет в гнезде этой профессии, против этого. Лист героя
 * инструмент не меняет, Мощи у него нет; прибавка гильдии одна с обеих сторон, поэтому сравнение её не берёт. [delta] - сдвинутые
 * меры (характеристики `STOCK_WORK_*`), [score] - сумма их долей изменения со знаком полярности ([StatPolarity]).
 */
data class ToolVerdict(val delta: List<StatDelta>, override val score: Double) : GearVerdict {
    override val might: MightShift? get() = null

    /** Итог одной стрелкой: лучше, хуже или без разницы (шум - [Shift.EVEN]). */
    val shift: Shift get() = Shift.of(score)

    companion object {
        /** Инструмент [tool] против [worn] в гнезде его профессии (null - пусто) при листе героя [sheet] уровня [level] и даре [fate]. */
        fun of(index: ContentIndex, sheet: Map<String, Double>, level: Int, tool: ItemInstance, worn: ItemInstance?, fate: FateEffects?): ToolVerdict {
            val before = WorkBonus.of(index, sheet, level, worn, fate = fate)
            val after = WorkBonus.of(index, sheet, level, tool, fate = fate)
            val delta = WorkMeasure.entries.map { StatDelta(it.stat.code, it.read(before), it.read(after)) }.filter { it.change != 0.0 }
            return ToolVerdict(delta, delta.sumOf { gain(index, it) })
        }

        /** Доля изменения меры к лучшему (плюс) или к худшему (минус), не больше единицы: новая мера - целиком. */
        private fun gain(index: ContentIndex, change: StatDelta): Double {
            val size = (if (change.before != 0.0) abs(change.change / change.before) else 1.0).coerceAtMost(1.0)
            return when (StatPolarity.better(index, change.stat, change.change)) {
                true -> size
                false -> -size
                null -> 0.0
            }
        }
    }
}
