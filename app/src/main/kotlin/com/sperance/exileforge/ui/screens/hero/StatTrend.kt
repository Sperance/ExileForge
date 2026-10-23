package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.SheetShift
import com.sperance.exileforge.core.sheet.Trend
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital

/**
 * Источник сдвига цифры в бою: что сдвинуло ([title]) и чем оно ([kind]: бафф, недуг, глоток), сколько ему осталось ([left] - уже
 * словами: секунды или «весь бой»), что даёт его строка этой цифры ([text]) цветом [tint].
 */
@Immutable
data class StatPart(val title: String, val kind: String, val left: String, val text: String, val tint: Color)

/**
 * Сравнение листа с прежним листом [baseLabel] («На карте» или «Герой»): сдвинутая цифра ([shift]) - стрелкой ▲ зелёным или ▼ красным
 * и зачёркнутым прежним числом, её строка раскрывается - из чего сложилась ([parts] по цифре), как [TrendFold].
 */
@Immutable
class StatTrend(val shift: SheetShift, val baseLabel: String, val parts: Map<String, List<StatPart>>)

/** Цвет сдвига: рост - зелёным, падение - красным. */
internal val Trend.tint: Color get() = if (this == Trend.UP) Vital else LifeRed

/** Знак сдвига у числа. */
internal val Trend.mark: String get() = if (this == Trend.UP) "▲" else "▼"

/**
 * Раскрытая строка сдвинутой цифры: прежнее число под именем прежнего листа ([baseLabel], зачёркнуто), строки источников с их сроком и
 * вкладом ([parts]), «Сейчас» с новым числом; внизу - «Полный разбор числа» ([onExplain]). Порядок строк - источники как есть, без
 * перестановки при живых числах.
 */
@Composable internal fun TrendFold(baseLabel: String, was: String, now: String, trend: Trend, parts: List<StatPart>, onExplain: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 38.dp, end = 12.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        FoldLine(baseLabel, Muted) { Text(was, color = Muted, style = MaterialTheme.typography.labelMedium, textDecoration = TextDecoration.LineThrough) }
        parts.forEach { part ->
            FoldLine(listOf(part.title, part.kind).filter { it.isNotBlank() }.joinToString(" · "), Parchment) {
                Text(part.left, color = Muted, style = MaterialTheme.typography.labelSmall)
                Text(part.text, color = part.tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        FoldLine(ui("stat.trend.now"), Parchment) { Text(now, color = trend.tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
        Text(ui("stat.trend.explain"), color = Rune, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(onClick = onExplain).padding(vertical = 6.dp))
    }
}

@Composable private fun FoldLine(label: String, tint: Color, values: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = tint, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        values()
    }
}
