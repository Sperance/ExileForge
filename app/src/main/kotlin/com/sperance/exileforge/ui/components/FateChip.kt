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
import com.sperance.exileforge.core.app.Building
import com.sperance.exileforge.core.display.FateCounter
import com.sperance.exileforge.core.display.FateTally
import com.sperance.exileforge.core.display.fateDescription
import com.sperance.exileforge.core.hero.FateVisit
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.ui.icons.ForgeGlyphs

/**
 * Чип счёта дара [card] в шапке героя рядом с золотом: печать ✦ (у порогового счёта - в кольце пути до порога), знак счёта и
 * число [FateTally.short]; чтение с экрана слышит имя дара и полную подпись. Касание открывает подсказку ([TipCallout], она
 * компонуется только открытой): имя дара, описание, строки счёта и кнопка к зданию, где счёт тратят ([visit]; null - кнопки нет,
 * [onVisit] ведёт туда). Чип высотой 24dp - строка золота не растёт; цель касания до 48dp добирает минимальная цель касания
 * Compose, поэтому чип не обрезается (`clip`).
 */
@Composable fun FateChip(tally: FateTally, card: FateCard, visit: FateVisit?, onVisit: (Building) -> Unit, modifier: Modifier = Modifier) {
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
        if (open) FateCounterTip(tally, card, visit, onVisit) { open = false }
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

/** Подсказка чипа - [Tip]: имя дара, описание и строки счёта (первая - сам счёт); под ними кнопка в здание счёта [visit]. */
@Composable private fun FateCounterTip(tally: FateTally, card: FateCard, visit: FateVisit?, onVisit: (Building) -> Unit, onClose: () -> Unit) {
    val index = LocalContentIndex.current
    val tip = remember(tally, card, index) {
        Tip("$SIGIL ${card.title.uppercase()}", fateDescription(card, index), FateInk.Gold, tally.facts, FateInk.GoldLight)
    }
    TipCallout(
        tip,
        onClose = onClose,
        footer = if (visit == null) {
            null
        } else {
            {
                ForgeButton(
                    onClick = {
                        onClose()
                        onVisit(visit.building)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(visit.label) }
            }
        },
    )
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
