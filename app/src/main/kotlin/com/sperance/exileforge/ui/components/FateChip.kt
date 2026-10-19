package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.FateCounter
import com.sperance.exileforge.core.display.FateTally
import com.sperance.exileforge.core.display.fateDescription
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Чип счёта дара [card] в шапке героя рядом с золотом: печать ✦ (у порогового счёта - в кольце пути до порога), знак счёта и
 * число [FateTally.short]. Касание открывает подсказку ([TipCallout], она компонуется только открытой): имя дара, описание,
 * полная подпись, строки счёта и кнопка к зданию, где счёт тратят ([onVisit]; null - кнопки нет). Чип высотой 24dp - строка
 * золота не растёт; цель касания до 48dp добирает минимальная цель касания Compose, поэтому чип не обрезается (`clip`).
 */
@Composable fun FateChip(tally: FateTally, card: FateCard, onVisit: (() -> Unit)?, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(modifier) {
        Row(
            Modifier
                .clickable(remember { MutableInteractionSource() }, null, onClickLabel = ui("fate.counter.more"), role = Role.Button) { open = true }
                .semantics(mergeDescendants = true) { contentDescription = "${card.title}: ${tally.caption}" }
                .height(24.dp)
                .background(Brush.horizontalGradient(CHIP), shape)
                .border(1.dp, if (open) FateInk.Gold else FateInk.Rim, shape)
                .padding(start = 2.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CounterSeal(tally.progress)
            Icon(tally.counter.glyph, null, tint = FateInk.Gold, modifier = Modifier.size(13.dp))
            Text(tally.short, color = FateInk.GoldLight, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        if (open) FateCounterTip(tally, card, onVisit) { open = false }
    }
}

/** Печать чипа: ✦, у порогового счёта [progress] - в кольце, жар заполняет пройденную долю от верха по часовой. */
@Composable private fun CounterSeal(progress: Float?) {
    Box(
        Modifier.size(20.dp).then(
            if (progress == null) {
                Modifier
            } else {
                Modifier.drawBehind {
                    val stroke = 2.dp.toPx()
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    val at = Offset(inset, inset)
                    drawArc(RING_TRACK, 0f, 360f, false, at, arc, style = Stroke(stroke))
                    drawArc(FateInk.Cinder, -90f, 360f * progress.coerceIn(0f, 1f), false, at, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            },
        ),
        contentAlignment = Alignment.Center,
    ) { Text(SIGIL, color = FateInk.Gold, fontSize = 11.sp) }
}

/** Подсказка чипа: имя дара, описание, полная подпись счёта, строки «подпись - число» и кнопка к зданию счёта. */
@Composable private fun FateCounterTip(tally: FateTally, card: FateCard, onVisit: (() -> Unit)?, onClose: () -> Unit) {
    val index = LocalContentIndex.current
    val description = remember(card.code, index) { fateDescription(card, index) }
    TipCallout(FateInk.Gold, onClose) {
        Text("$SIGIL ${card.title.uppercase()}", style = relicName(13), color = FateInk.GoldLight, letterSpacing = 1.2.sp)
        if (description.isNotBlank()) Text(description, color = Parchment, style = MaterialTheme.typography.bodySmall)
        Text(tally.caption, color = FateInk.Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        tally.facts.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text(value, color = FateInk.GoldLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
        val visit = tally.counter.visit
        if (visit != null && onVisit != null) {
            ForgeButton(
                onClick = {
                    onClose()
                    onVisit()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui("fate.visit.${visit.name}")) }
        }
    }
}

/** Знак счёта в чипе: кубок трофеев, сфера Щедрости, клинки Разгона. */
private val FateCounter.glyph: ImageVector
    get() = when (this) {
        FateCounter.TROPHIES -> ForgeGlyphs.Trophy
        FateCounter.BOUNTY -> ForgeGlyphs.Orb
        FateCounter.STREAK -> ForgeGlyphs.Swords
    }

private val CHIP = listOf(FateInk.Face.first(), FateInk.Back.first())
private val RING_TRACK = Color(0xFF3D2614)
