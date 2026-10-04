package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.wearDelta
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

/** Whether an item goes on the body at all: a map, a tool and a jewel are placed elsewhere. */
fun wearable(game: GameUi, item: ItemInstance): Boolean = game.index?.template(item.template)?.slot?.let { !it.isJewelLike && !it.isTool } == true

/**
 * «Если надеть» (2.46.0): what the sheet would become with this item on, added up here by the
 * rules' own formula, one line per characteristic that moves. An item out of reach says instead, in
 * red, what it needs — and its button stays off.
 */
@Composable fun WearPreview(game: GameUi, item: ItemInstance) {
    if (!wearable(game, item) || item.equipped || item.socketed) return
    val unmet = game.unmetFor(item.template)
    val delta = remember(item, game.hero, game.index) { game.wearDelta(item) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (unmet.isNotEmpty()) {
            Text(ui("wear.blocked"), color = LifeRed, style = MaterialTheme.typography.labelLarge)
            unmet.forEach { Text(requirementReason(it, game.lang), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
            return@Column
        }
        Text(ui("wear.title"), color = Gold, style = MaterialTheme.typography.labelLarge)
        if (delta.isEmpty()) MutedText(ui("wear.nothing"))
        delta.forEach { line ->
            val tone = if (line.change > 0) Vital else LifeRed
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Tipped({
                    Tip(
                        statTitle(line.stat),
                        tint = tone,
                        facts = listOf(
                            ui("tip.before") to statValue(line.stat, line.before),
                            ui("tip.after") to statValue(line.stat, line.after),
                        ),
                    )
                }) { StatIcon(line.stat, Muted, Modifier.size(14.dp), muted = true) }
                Text(statTitle(line.stat), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                MutedText(ui("wear.from_to", statValue(line.stat, line.before), statValue(line.stat, line.after)), style = MaterialTheme.typography.labelSmall)
                Text(
                    (if (line.change > 0) "+" else "−") + statValue(line.stat, kotlin.math.abs(line.change)),
                    color = tone,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
