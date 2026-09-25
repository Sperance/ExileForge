package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.RollSummary
import com.sperance.exileforge.core.display.affixMarks
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.rollRange
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.JsonObject

/**
 * The trade table (2.60.0): an item's lines as rows of a ledger — the badge, the sentence in its
 * kind's colour and the tier's range. One row per line and nothing between them but a hairline, so
 * seven affixes read as one block. The bar of where a value landed left in 2.72.0: the figures say it.
 */
@Composable fun TradeTable(lines: List<JsonObject>, definitions: List<ModifierDefinition>) {
    Column(Modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            if (index > 0) HorizontalDivider(thickness = .5.dp, color = PanelRaised)
            TradeLine(line, definitions)
        }
    }
}

@Composable private fun TradeLine(modifier: JsonObject, definitions: List<ModifierDefinition>) {
    val marks = affixMarks(modifier, definitions)
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AffixBadge(marks)
        Text(modifierText(modifier, definitions), color = ModBlue, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f))
        rollRange(modifier, definitions)?.let {
            Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, maxLines = 1,
                modifier = Modifier.widthIn(min = 34.dp))
        }
    }
}

/** How a roll quality reads in words, and the colour it is drawn in. */
private fun qualityVerdict(quality: Int): Pair<String, Color> = when {
    quality >= 90 -> ui("card.roll_superb") to GoldBright
    quality >= 70 -> ui("card.roll_good") to Vital
    quality >= 40 -> ui("card.roll_fair") to Parchment
    else -> ui("card.roll_poor") to LifeRed
}

/**
 * How well the item rolled (2.72.0): a gauge filling its ring to the share, the figure in the
 * middle, and a word for it beside — superb, good, fair, poor — in that word's colour.
 */
@Composable fun RollScore(summary: RollSummary) {
    val quality = summary.quality ?: return
    val (verdict, tint) = qualityVerdict(quality)
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(tint.copy(alpha = .12f), Abyss)), shape)
        .border(1.dp, tint.copy(alpha = .35f), shape).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val width = 4.dp.toPx()
                val arc = Size(size.width - width, size.height - width)
                val corner = Offset(width / 2, width / 2)
                drawArc(PanelRaised, 135f, 270f, false, corner, arc, style = Stroke(width, cap = StrokeCap.Round))
                drawArc(tint, 135f, 270f * quality / 100f, false, corner, arc, style = Stroke(width, cap = StrokeCap.Round))
            }
            Text("$quality%", color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(ui("card.roll_quality"), color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(verdict, color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(ui("card.roll_quality_hint"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * A list line's rolls (2.72.0): how well it rolled, then every line it carries as a sentence — no
 * tier letters and no bars, the card behind the tap has those. No rarity since 2.73.0: the frame says it.
 */
@Composable fun RollTops(summary: RollSummary, lines: List<JsonObject> = emptyList(), definitions: List<ModifierDefinition> = emptyList()) {
    summary.quality?.let { RollPill(it) }
    lines.forEach { line ->
        Text(modifierText(line, definitions), color = ModBlue, style = MaterialTheme.typography.labelSmall)
    }
}

/** «роллы 87%» as a pill in the verdict's colour (2.73.0): the one figure a list line is judged by. */
@Composable fun RollPill(quality: Int) {
    val tint = qualityVerdict(quality).second
    val shape = RoundedCornerShape(50)
    Text(ui("row.rolls", quality), color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.background(Brush.horizontalGradient(listOf(tint.copy(alpha = .22f), tint.copy(alpha = .08f))), shape)
            .border(1.dp, tint.copy(alpha = .55f), shape).padding(horizontal = 8.dp, vertical = 1.dp))
}
