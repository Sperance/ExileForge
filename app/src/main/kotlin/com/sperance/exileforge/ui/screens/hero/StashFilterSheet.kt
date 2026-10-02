package com.sperance.exileforge.ui.screens.hero

import com.sperance.exileforge.ui.components.ForgeSheet
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.SlotGroup
import com.sperance.exileforge.presentation.state.StashFilter
import com.sperance.exileforge.presentation.state.StashSort
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** How many of the shelf's narrowings are on: each filter chip, the query, «hide equipped» and an order other than the newest first. */
internal fun stashTweaks(filter: StashFilter, sort: StashSort, hideWorn: Boolean = false): Int =
    filter.groups.size + filter.rarities.size + (if (filter.wearable) 1 else 0) + (if (filter.query.isNotBlank()) 1 else 0) +
        (if (hideWorn) 1 else 0) + (if (sort != StashSort.NEWEST) 1 else 0)

/** «Hide equipped» (3.69.0): the gear shelf without what the hero wears or has socketed; the same chip sits in the sheet. */
@Composable internal fun HideWornChip(on: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(selected = on, onClick = { onToggle(!on) }, label = { Text(ui("stash.hide_worn"), maxLines = 1) })
}

/** The stash's one filter glyph: lit and badged with the count of what narrows the shelf; its size is required, so no row squeezes it. */
@Composable internal fun StashFilterButton(tweaks: Int, onClick: () -> Unit) {
    val tint = if (tweaks > 0) GoldBright else Muted
    BadgedBox(badge = { if (tweaks > 0) Badge(containerColor = Vital, contentColor = Ink) { Text(tweaks.toString(), fontSize = 9.sp, maxLines = 1) } }) {
        IconButton(onClick = onClick, modifier = Modifier.requiredSize(36.dp).border(1.dp, tint.copy(alpha = .6f), CircleShape)) {
            Icon(Icons.Outlined.FilterList, ui("stash.filters"), tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * What narrows the shelf, in one sheet: the search, the order, the groups, «can wear», «hide equipped» and the rarities, and a
 * reset of them all. [hideWorn] is null on a shelf it does not apply to (the tools), and its chip is not drawn there.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable internal fun StashFilterSheet(filter: StashFilter, sort: StashSort, lang: Lang, shelfSize: Int, groupCounts: Map<SlotGroup, Int>,
    rarities: List<Rarity>, onFilter: (StashFilter) -> Unit, onSort: (StashSort) -> Unit, hideWorn: Boolean?, onHideWorn: (Boolean) -> Unit,
    onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("stash.filters"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(filter.query, { onFilter(filter.copy(query = it.take(DefaultInputs.search))) }, placeholder = { Text(ui("hero.search_hint")) },
                label = { Text(ui("hero.find_item")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (filter.query.isNotEmpty()) IconButton(onClick = { onFilter(filter.copy(query = "")) }) { Icon(Icons.Outlined.Close, null) } },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onDismiss() }),
                modifier = Modifier.fillMaxWidth())
            Engraved(ui("stash.sort"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StashSort.entries.forEach { entry ->
                    FilterChip(selected = entry == sort, onClick = { onSort(entry) }, label = { Text(ui("stash.sort.${entry.name.lowercase()}")) })
                }
            }
            Engraved(ui("stash.filter_groups"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter.groups.isEmpty(), onClick = { onFilter(filter.copy(groups = emptySet())) },
                    label = { Text(ui("hero.slot_count", ui("common.all"), shelfSize)) })
                groupCounts.forEach { (group, count) ->
                    FilterChip(selected = group in filter.groups, onClick = { onFilter(filter.toggle(group)) },
                        label = { Text(ui("hero.slot_count", group.title(lang), count)) })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter.wearable, onClick = { onFilter(filter.copy(wearable = !filter.wearable)) }, label = { Text(ui("stash.can_wear")) })
                hideWorn?.let { HideWornChip(it, onHideWorn) }
            }
            Engraved(ui("stash.filter_rarity"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rarities.forEach { rarity ->
                    FilterChip(selected = rarity in filter.rarities, onClick = { onFilter(filter.toggle(rarity)) },
                        label = { Text(rarityTitle(rarity, lang), color = rarityColor(rarity.name)) })
                }
            }
            ForgeOutlinedButton(onClick = { onFilter(StashFilter()); onSort(StashSort.NEWEST); if (hideWorn == true) onHideWorn(false) },
                enabled = stashTweaks(filter, sort, hideWorn == true) > 0,
                modifier = Modifier.fillMaxWidth()) { Text(ui("stash.reset")) }
        }
    }
}
