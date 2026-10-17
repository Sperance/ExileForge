package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.CardSeal
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.influenceTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital

/** Цвет влияния - тот же, что у значка его состояния на карточке. */
fun influenceColor(influence: Influence): Color = stateColor(influence.name.lowercase())

/** Буква плашки качества карты. */
private const val QUALITY_LETTER = "Q"

/** Строка особого источника плашкой: буква источника на его цвете, текст строки, справа имя источника; касание - [onClick]. */
@Composable fun LinePlaque(line: ItemLine, onClick: (() -> Unit)?) {
    SealPlaque(
        letter = line.marks.letter,
        tint = tierColor(line.marks),
        text = line.text,
        ink = modColor(line.marks),
        source = line.marks.sealName,
        amplified = line.amplified != null,
        onClick = onClick,
    )
}

/** Плашка, что не строка вещи: захват карты влиянием (имя и коротко числа) или качество карты (что даёт и сколько). */
@Composable fun CardSealPlaque(seal: CardSeal) {
    when (seal) {
        is CardSeal.Capture -> {
            val capture = seal.capture
            val tint = influenceColor(capture.influence)
            val name = influenceTitle(capture.influence)
            SealPlaque(
                letter = name.take(1).uppercase(),
                tint = tint,
                text = ui("expedition.capture.title", name),
                ink = tint,
                note = ui("card.seal.capture", number(capture.power), number(capture.items)),
                bold = true,
            )
        }

        is CardSeal.MapQuality -> SealPlaque(
            letter = QUALITY_LETTER,
            tint = Parchment,
            text = ui("card.seal.quality", number(seal.quantity)),
            ink = Parchment,
            source = ui("card.seal.quality_source", seal.quality),
        )
    }
}

/**
 * Плашка особого блока карточки: буква-герб [letter] на заливке [tint], [text] цветом [ink] (ниже - мелкая [note]), справа [source]
 * одной строкой. [amplified] - ▲ перед текстом (строка, усиленная своей вещью).
 */
@Composable private fun SealPlaque(
    letter: String,
    tint: Color,
    text: String,
    ink: Color,
    source: String? = null,
    note: String? = null,
    bold: Boolean = false,
    amplified: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(9.dp)
    val tap = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = text, onClick = onClick)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(tint.copy(alpha = .13f), shape).then(tap).padding(start = 5.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(20.dp).background(tint, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Text(letter, color = SealInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (amplified) Text(ui("relic.amplified"), color = Vital, style = MaterialTheme.typography.labelMedium)
        Column(Modifier.weight(1f)) {
            Text(text, color = ink, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.SemiBold else null)
            note?.let { Text(it, color = ink.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall) }
        }
        source?.let { Text(it, color = ink.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
    }
}

/** Чернила буквы на заливке плашки. */
private val SealInk = Color(0xFF0C1115)
