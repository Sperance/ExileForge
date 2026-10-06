package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.statDescription
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*
import kotlin.math.roundToInt

/**
 * What a modifier line is, behind a tap on it: its name and sentence, where it sits (prefix, suffix,
 * implicit…), its tier out of how many, the tier's range and where the roll landed inside it, and
 * what each characteristic it moves does. The range lives here, not on the line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifierInfoSheet(line: ItemLine, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AffixBadge(line.marks)
                Text(
                    line.title(),
                    color = GoldBright,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(line.text, color = ModBlue, style = MaterialTheme.typography.bodyMedium)
            Column(Modifier.fillMaxWidth().background(Abyss, RoundedCornerShape(6.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Fact(ui("modinfo.kind"), line.kindText())
                if (line.roll.tier > 0 && line.tierCount > 0) TierScale(line.roll.tier, line.tierOpen)
                line.range?.let { Fact(ui("modinfo.range"), it) }
                line.quality?.takeIf { line.range != null }?.let { share ->
                    if (line.fixed) {
                        Fact(ui("modinfo.position"), ui("modinfo.fixed"))
                    } else {
                        Position((share * 100).roundToInt().coerceIn(0, 100))
                    }
                }
            }
            line.stats.forEach { stat ->
                val about = statDescription(stat)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(statTitle(stat), color = Parchment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    MutedText(about.ifBlank { ui("modinfo.no_description") })
                }
            }
        }
    }
}

@Composable private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        MutedText(label, Modifier.weight(1f), MaterialTheme.typography.labelMedium)
        Text(value, color = Parchment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * The tier among its modifier's tiers: «T3 из 3», then a segment per tier, T1 first, the
 * line's own lit and those the item's level does not reach dimmed.
 */
@Composable private fun TierScale(tier: Int, open: List<Boolean>) {
    val count = open.size
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Fact(ui("modinfo.tier"), ui("modinfo.tier_of", tier, count))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            open.forEachIndexed { at, reached ->
                val color = when {
                    at + 1 == tier -> Gold
                    reached -> PanelRaised
                    else -> PanelRaised.copy(alpha = .35f)
                }
                Box(Modifier.weight(1f).height(6.dp).background(color, RoundedCornerShape(3.dp)))
            }
        }
    }
}

/** Where the roll landed inside its tier: the share as a figure and as a bar from the tier's bottom to its top. */
@Composable private fun Position(percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Fact(ui("modinfo.position"), ui("modinfo.position_value", percent))
        Box(Modifier.fillMaxWidth().height(6.dp).background(PanelRaised, RoundedCornerShape(3.dp))) {
            Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().background(Gold, RoundedCornerShape(3.dp)))
        }
    }
}

/** The line's name: the characteristics it moves, or its code when the content no longer knows it. */
private fun ItemLine.title(): String = stats.joinToString(" / ") { statTitle(it) }.ifBlank { displayName(code.value) }

/** Where the line sits, then what set it apart: «Префикс · Расколот». */
private fun ItemLine.kindText(): String = listOfNotNull(
    placement?.let { ui("mod.kind.${it.name}") } ?: ui("modinfo.kind_other"),
    ui("mod.kind.CRAFTED").takeIf { marks.crafted },
    ui("mod.kind.FRACTURED").takeIf { marks.fractured },
).joinToString(" · ")
