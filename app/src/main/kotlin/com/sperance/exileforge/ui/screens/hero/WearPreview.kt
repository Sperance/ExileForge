package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.wearDelta
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.HeroTotals
import com.sperance.exileforge.ui.components.RelicLook
import com.sperance.exileforge.ui.theme.*

/** Whether an item goes on the body at all: a map, a tool and a jewel are placed elsewhere. */
fun wearable(game: GameUi, item: ItemInstance): Boolean = game.index?.template(item.template)?.slot?.let { !it.isJewelLike && !it.isTool } == true

/**
 * «Если надеть» (2.46.0) в итоге героя карточки (3.89.0): что станет с листом с этой вещью, сложенное здесь формулой правил, -
 * строка на каждую сдвинутую характеристику. Недоступная вещь говорит вместо этого красным, чего ей не хватает.
 */
@Composable private fun WearTotals(game: GameUi, item: ItemInstance, look: RelicLook) {
    val unmet = game.unmetFor(item.template)
    val delta = remember(item, game.hero, game.index) { game.wearDelta(item) }
    if (unmet.isEmpty()) {
        HeroTotals(delta, look)
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("wear.blocked"), color = LifeRed, style = MaterialTheme.typography.labelLarge)
        unmet.forEach { Text(requirementReason(it, game.lang), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Итог героя для карточки [item] или null, когда вещь не надевается: карта, самоцвет, уже надетая. */
fun wearTotals(game: GameUi, item: ItemInstance): (@Composable (RelicLook) -> Unit)? = if (!wearable(game, item) || item.equipped || item.socketed) null else ({ look -> WearTotals(game, item, look) })
