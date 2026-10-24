package com.sperance.exileforge.ui.screens.hero

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.character.StatPolarity
import com.sperance.exileforge.core.sheet.SheetShift
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.components.SumShare
import com.sperance.exileforge.ui.theme.Ember
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Vital

/**
 * Сравнение листа похода с листом героя без похода [base]: сдвинутые цифры ([shift]) и строки раскрытия каждой ([shares]: «Герой без
 * похода» и вклады карты и боя в единице строки - сумма равна числу строки). Строки считаются по запросу раскрытой строки. Лучше или
 * хуже - по смыслу характеристики ([StatPolarity]: у «меньше - лучше» рост - хуже), а не по знаку сдвига; сдвиг без оценки - нейтральным
 * цветом и без стрелки.
 */
@Immutable
class StatTrend(val base: Map<String, Double>, val shift: SheetShift, private val parts: (String) -> List<SumShare>) {
    /** Строки раскрытия сдвинутой цифры [key]: «Герой без похода» первой, затем вклады по порядку. */
    fun shares(key: String): List<SumShare> = parts(key)

    /** Вердикт сдвинутой цифры [key] со значением [value]: не сдвинута - null. */
    fun of(key: String, value: Double, index: ContentIndex?): TrendMark? {
        if (shift.of(key) == null) return null
        return when (StatPolarity.better(index, key, value - (base[key] ?: 0.0))) {
            true -> TrendMark.BETTER
            false -> TrendMark.WORSE
            null -> TrendMark.NEUTRAL
        }
    }

    /** Сколько сдвинутых цифр листа [now] к лучшему и к худшему. */
    fun count(now: Map<String, Double>, index: ContentIndex?): Pair<Int, Int> {
        val marks = (shift.up + shift.down).mapNotNull { key -> of(key, now[key] ?: 0.0, index) }
        return marks.count { it == TrendMark.BETTER } to marks.count { it == TrendMark.WORSE }
    }
}

/** Отметка сдвинутой цифры: цвет полоски и числа и знак у числа ([mark], у нейтральной - нет). */
enum class TrendMark(val tint: Color, val mark: String?) {
    BETTER(Vital, "▲"),
    WORSE(LifeRed, "▼"),
    NEUTRAL(Ember, null),
}
