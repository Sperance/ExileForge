package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.RollSummary
import com.sperance.exileforge.core.display.affixMarks
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.rollQuality
import com.sperance.exileforge.core.display.rollRange
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.JsonObject

/**
 * The trade table (2.60.0): an item's lines as rows of a ledger — the badge, the sentence in its
 * kind's colour, a bar of how high the value landed inside its tier, and the tier's range. One row
 * per line and nothing between them but a hairline, so seven affixes read as one block.
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
        Text(modifierText(modifier, definitions), color = affixTint(marks.kind), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f))
        rollQuality(modifier, definitions)?.let { QualityBar(it) }
        rollRange(modifier, definitions)?.let {
            Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, maxLines = 1,
                modifier = Modifier.widthIn(min = 34.dp))
        }
    }
}

/** How far up its range a value landed; a perfect roll is drawn in bright gold. */
@Composable fun QualityBar(quality: Double, width: Int = 40) {
    val shape = RoundedCornerShape(3.dp)
    Box(Modifier.size(width.dp, 5.dp).background(PanelRaised, shape)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(quality.toFloat().coerceIn(0f, 1f))
            .background(if (quality >= .999) GoldBright else Gold, shape))
    }
}

/** The three figures over the table: how well the item rolled, what room it has left, its best tier. */
@Composable fun RollScore(summary: RollSummary) {
    val tiles = listOfNotNull(
        summary.quality?.let { "$it%" to ui("card.roll_quality") },
        summary.openSlots?.let { "$it" to ui("card.open_slots") },
        summary.bestTier?.let { "T$it" to ui("card.best_tier") },
    )
    if (tiles.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { (value, title) ->
            Column(Modifier.weight(1f).background(Abyss, RoundedCornerShape(6.dp)).padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = GoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(title, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

/** A list line's summary under the name: its rarity in its colour, then how it rolled and what room is left. */
@Composable fun RollTops(rarity: String, color: Color, summary: RollSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (rarity.isNotBlank()) Text(rarityTitle(rarity), color = color, style = MaterialTheme.typography.labelSmall)
        summary.quality?.let { Text(ui("row.rolls", it), color = Parchment, style = MaterialTheme.typography.labelSmall) }
        summary.openSlots?.takeIf { it > 0 }?.let { MutedText(ui("row.open", it), style = MaterialTheme.typography.labelSmall) }
    }
}
