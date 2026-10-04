package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.screens.expedition.LootCard
import com.sperance.exileforge.ui.theme.*

/**
 * What the server rolled — experience, gold, orbs and items — for a kill and a chest alike; each item as its whole card (3.2.0).
 * [awaiting]: some of it is still on its way, and the sheet says so instead of «nothing else».
 * With [onItem] a card opens its comparison with what is worn (3.24.0).
 */
@Composable internal fun RewardLines(game: GameUi, reward: Reward, onItem: ((ItemView) -> Unit)? = null, awaiting: Boolean = false) {
    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (reward.experience > 0) Text(ui("expedition.loot_experience", number(reward.experience)), color = Rune)
            if (reward.gold > 0) Text(ui("expedition.loot_gold", reward.gold), color = GoldBright)
        }
        reward.items.forEach { (code, amount) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BagIcon(code, Modifier.size(20.dp), kind = game.index?.item(ItemCode(code))?.let(::bagVisualKind) ?: ItemVisualKind.ITEM)
                Text(ui("expedition.loot_stack", itemTitle(code), amount), color = Parchment)
            }
        }
        reward.equipment.forEach { instance ->
            // The whole card, not a line (3.2.0): what dropped is read where it dropped
            game.view(instance)?.let { LootCard(game, it, onItem) }
        }
        if (awaiting) {
            Receiving()
        } else if (reward.items.isEmpty() && reward.equipment.isEmpty()) {
            Text(ui("expedition.loot_nothing"), color = Muted)
        }
    }
}

/** The loot on its way (server 1.30.0): the server alone rolls it, and it shows as its answers arrive — offline, once the connection is back. */
@Composable internal fun Receiving(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(Modifier.size(12.dp), color = Rune, strokeWidth = 2.dp)
        Text(ui("expedition.receiving"), color = Rune, style = MaterialTheme.typography.bodySmall)
    }
}
