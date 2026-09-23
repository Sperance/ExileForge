package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The bag: everything the character owns that is not an equipment instance, as one strip.
 *
 * It heads the stash because orbs are what a player spends on the items below it. A stack has no
 * card of its own — it is an id and a count — so a chip opens nothing.
 */
@Composable fun BagPanel(s: ForgeState) {
    val hero = s.play.hero ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Engraved(ui("hero.bag"))
        if (hero.bag.isEmpty()) { Text(ui("hero.bag_empty"), color = Muted, style = MaterialTheme.typography.bodySmall); return@Column }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Currency is named from the catalogue the hero screen already read; anything else is an id.
            items(hero.bag, key = { it.itemId }) { item ->
                val orb = s.world.orbs.firstOrNull { it.id == item.itemId }
                val shape = CutCornerShape(5.dp)
                Row(Modifier.background(Color.Black.copy(alpha = .22f), shape).border(1.dp, Gold.copy(alpha = .35f), shape)
                    .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(if (orb != null) ForgeGlyphs.Orb else ForgeGlyphs.Gem, null, tint = Gold, modifier = Modifier.size(14.dp))
                    Text(orb?.title(s.lang) ?: (ui("common.item") + " …${item.itemId.takeLast(6)}"), color = Parchment, style = MaterialTheme.typography.labelMedium)
                    Text(item.amount.toString(), color = GoldBright, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
