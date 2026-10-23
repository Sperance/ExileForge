package com.sperance.exileforge.ui.screens.hero

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.sheet.SheetShift
import com.sperance.exileforge.core.sheet.Trend
import com.sperance.exileforge.ui.components.SumShare
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Vital

/**
 * Сравнение листа с прежним листом [baseLabel] («На карте» или «Герой»): сдвинутая цифра ([shift]) - стрелкой ▲ зелёным или ▼ красным
 * и зачёркнутым прежним числом, её строка раскрывается - из чего сложилась: доли источников по цифре ([parts], [SumShare]).
 */
@Immutable
class StatTrend(val shift: SheetShift, val baseLabel: String, val parts: Map<String, List<SumShare>>)

/** Цвет сдвига: рост - зелёным, падение - красным. */
internal val Trend.tint: Color get() = if (this == Trend.UP) Vital else LifeRed

/** Знак сдвига у числа. */
internal val Trend.mark: String get() = if (this == Trend.UP) "▲" else "▼"
