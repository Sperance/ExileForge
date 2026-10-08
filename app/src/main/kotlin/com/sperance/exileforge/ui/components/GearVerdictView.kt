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
import com.sperance.exileforge.core.character.FlaskVerdict
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.core.character.SheetVerdict
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.gearVerdict
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Vital
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Вердикт вещи для героя (3.89.0), сложенный вне главного потока и запомненный на вещь и лист героя: строки длинного
 * списка не держат прокрутку. Null - пока считается или когда вещь не надеть. [place] (3.90.3) - место пары, на которое
 * вещь мерится; без него - лучшее.
 */
@Composable fun rememberGearVerdict(game: GameUi, item: ItemInstance, place: Slot? = null): GearVerdict? {
    val verdict by produceState<GearVerdict?>(null, item, game.hero, game.index, place) {
        value = withContext(Dispatchers.Default) { game.gearVerdict(item, place) }
    }
    return verdict
}

/** Одна из двух осей вердикта листа: урон или защита, с глифом, словом и долей изменения. */
private enum class VerdictAxis(val glyph: ImageVector, val key: String, val change: (SheetVerdict) -> Double) {
    OFFENCE(ForgeGlyphs.Swords, "verdict.offence", SheetVerdict::offence),
    DEFENCE(ForgeGlyphs.Kite, "verdict.defence", SheetVerdict::defence),
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
    return signedNumber(value.toDouble()) { "${it.toInt()}" } + "%"
}

/**
 * Итог вердикта над «Если надеть» в карточке вещи: «Урон ▲ +12% · Защита ▼ −4%» (3.89.0) у снаряжения, «Фляга ▲ лучше 3 · хуже 0»
 * у фляги (4.2.0) - по строкам её листа.
 */
@Composable fun GearVerdictSummary(verdict: GearVerdict, modifier: Modifier = Modifier) = when (verdict) {
    is SheetVerdict -> SheetSummary(verdict, modifier)
    is FlaskVerdict -> FlaskSummary(verdict, modifier)
}

@Composable private fun FlaskSummary(verdict: FlaskVerdict, modifier: Modifier) {
    val shift = verdict.shift
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui("verdict.flask"), color = Muted, style = MaterialTheme.typography.labelLarge)
        Text(shift.arrow(), color = shift.tint(), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
        Text(ui("verdict.flask_rows", verdict.better, verdict.worse), color = shift.tint(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable private fun SheetSummary(verdict: SheetVerdict, modifier: Modifier) {
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

/** Значок строки (3.89.0): глиф урона и защиты, у каждого стрелка вверх, вниз или «=»; у фляги (4.2.0) - глиф фляги и итог её строк. */
@Composable fun GearVerdictBadge(verdict: GearVerdict, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        when (verdict) {
            is SheetVerdict -> VerdictAxis.entries.forEach { axis -> BadgeMark(axis.glyph, ui(axis.key), Shift.of(axis.change(verdict))) }
            is FlaskVerdict -> BadgeMark(ForgeGlyphs.Flask, ui("verdict.flask"), verdict.shift)
        }
    }
}

@Composable private fun BadgeMark(glyph: ImageVector, label: String, shift: Shift) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        Icon(glyph, label, tint = shift.tint(), modifier = Modifier.size(12.dp))
        Text(shift.arrow(), color = shift.tint(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}
