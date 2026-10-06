package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.gearVerdict
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Vital
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Вердикт вещи для героя (3.89.0), сложенный вне главного потока и запомненный на вещь и лист героя: строки длинного
 * списка не держат прокрутку. Null - пока считается или когда вещь не надеть.
 */
@Composable fun rememberGearVerdict(game: GameUi, item: ItemInstance): GearVerdict? {
    val verdict by produceState<GearVerdict?>(null, item, game.hero, game.index) {
        value = withContext(Dispatchers.Default) { game.gearVerdict(item) }
    }
    return verdict
}

/** Одна из двух осей вердикта: урон или защита, с глифом, словом и долей изменения. */
private enum class VerdictAxis(val glyph: ImageVector, val key: String, val change: (GearVerdict) -> Double) {
    OFFENCE(ForgeGlyphs.Swords, "verdict.offence", GearVerdict::offence),
    DEFENCE(ForgeGlyphs.Kite, "verdict.defence", GearVerdict::defence),
}

private fun Shift.arrow(): String = when (this) {
    Shift.UP -> "▲"
    Shift.DOWN -> "▼"
    Shift.EVEN -> "="
}

private fun Shift.tint(): Color = when (this) {
    Shift.UP -> Vital
    Shift.DOWN -> LifeRed
    Shift.EVEN -> Muted
}

/** «+12%» / «−4%»: доля изменения целыми процентами со знаком. */
private fun percent(change: Double): String {
    val value = (change * 100).roundToInt()
    return if (value < 0) "−${abs(value)}%" else "+$value%"
}

/** «Урон ▲ +12% · Защита ▼ −4%» (3.89.0): итог вердикта над «Если надеть» в карточке вещи. */
@Composable fun GearVerdictSummary(verdict: GearVerdict, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        VerdictAxis.entries.forEachIndexed { at, axis ->
            if (at > 0) Text("·", color = Muted, style = MaterialTheme.typography.labelLarge)
            val change = axis.change(verdict)
            val shift = Shift.of(change)
            Text(ui(axis.key), color = Muted, style = MaterialTheme.typography.labelLarge)
            Text(
                if (shift == Shift.EVEN) shift.arrow() else "${shift.arrow()} ${percent(change)}",
                color = shift.tint(),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Значок строки (3.89.0): глиф урона и защиты, у каждого стрелка вверх, вниз или «=». */
@Composable fun GearVerdictBadge(verdict: GearVerdict, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        VerdictAxis.entries.forEach { axis ->
            val shift = Shift.of(axis.change(verdict))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Icon(axis.glyph, ui(axis.key), tint = shift.tint(), modifier = Modifier.size(12.dp))
                Text(shift.arrow(), color = shift.tint(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
