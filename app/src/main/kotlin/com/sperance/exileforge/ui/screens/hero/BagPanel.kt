package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.display.icon
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.spriteVector
import com.sperance.exileforge.ui.theme.*

private data class BagLine(val item: CharacterItem, val orb: CurrencyItem?, val title: String)

/**
 * The bag: everything the character owns that is not an equipment instance, as a ledger.
 *
 * It heads the stash because orbs are what a player spends on the items below it. A stack has no
 * card of its own — it is an id and a count — so a line opens nothing.
 */
@Composable fun BagPanel(s: ForgeState) {
    val hero = s.play.hero ?: return
    var byAmount by rememberSaveable { mutableStateOf(true) }
    // Currency is named from the catalogue the hero screen already read; anything else is an id.
    val lines = hero.bag.map { item -> s.world.orbs.firstOrNull { it.id == item.itemId }.let { orb -> BagLine(item, orb, orb?.title(s.lang) ?: (ui("common.item") + " …${item.itemId.takeLast(6)}")) } }
        .let { rows -> if (byAmount) rows.sortedWith(compareByDescending<BagLine> { it.item.amount }.thenBy { it.title }) else rows.sortedBy { it.title.lowercase() } }
    ForgePanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Engraved(ui("hero.bag"))
                if (hero.bag.isNotEmpty()) Text(ui("hero.bag_summary", hero.bag.size, hero.bag.sumOf { it.amount }), color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            if (hero.bag.size > 1) OutlinedButton(onClick = { byAmount = !byAmount }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                Text(ui(if (byAmount) "hero.bag_sort_amount" else "hero.bag_sort_name"), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (hero.bag.isEmpty()) { Text(ui("hero.bag_empty"), color = Muted, style = MaterialTheme.typography.bodySmall); return@ForgePanel }
        Column {
            lines.forEachIndexed { index, (item, orb, title) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).background(Rune.copy(alpha = .10f), CutCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                        val art = orb?.let { icon(IconKey.item(it.code))?.let(::spriteVector) }
                        Icon(art ?: if (orb != null) ForgeGlyphs.Orb else ForgeGlyphs.Gem, null, tint = if (orb != null) Gold else Muted, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = if (orb != null) Parchment else Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(ui(if (orb != null) "hero.bag_kind_orb" else "hero.bag_kind_item"), color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(item.amount.toString(), color = GoldBright, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 44.dp))
                }
                if (index < lines.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
