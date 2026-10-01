package com.sperance.exileforge.ui.screens.hero

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

/** How many of the shelf's narrowings are on: each filter chip, the query and an order other than the newest first. */
internal fun stashTweaks(filter: StashFilter, sort: StashSort): Int =
    filter.groups.size + filter.rarities.size + (if (filter.wearable) 1 else 0) + (if (filter.query.isNotBlank()) 1 else 0) +
        (if (sort != StashSort.NEWEST) 1 else 0)

/** The stash's one filter glyph: lit and badged with the count of what narrows the shelf. */
@Composable internal fun StashFilterButton(tweaks: Int, onClick: () -> Unit) {
    val tint = if (tweaks > 0) GoldBright else Muted
    BadgedBox(badge = { if (tweaks > 0) Badge(containerColor = Vital, contentColor = Ink) { Text(tweaks.toString(), fontSize = 9.sp) } }) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp).border(1.dp, tint.copy(alpha = .6f), CircleShape)) {
            Icon(Icons.Outlined.FilterList, ui("stash.filters"), tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

/** What narrows the shelf, in one sheet: the search, the order, the groups, «can wear» and the rarities, and a reset of them all. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable internal fun StashFilterSheet(filter: StashFilter, sort: StashSort, lang: Lang, shelfSize: Int, groupCounts: Map<SlotGroup, Int>,
    rarities: List<Rarity>, onFilter: (StashFilter) -> Unit, onSort: (StashSort) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("stash.filters"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(filter.query, { onFilter(filter.copy(query = it)) }, placeholder = { Text(ui("hero.search_hint")) },
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
            FilterChip(selected = filter.wearable, onClick = { onFilter(filter.copy(wearable = !filter.wearable)) }, label = { Text(ui("stash.can_wear")) })
            Engraved(ui("stash.filter_rarity"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rarities.forEach { rarity ->
                    FilterChip(selected = rarity in filter.rarities, onClick = { onFilter(filter.toggle(rarity)) },
                        label = { Text(rarityTitle(rarity, lang), color = rarityColor(rarity.name)) })
                }
            }
            ForgeOutlinedButton(onClick = { onFilter(StashFilter()); onSort(StashSort.NEWEST) }, enabled = stashTweaks(filter, sort) > 0,
                modifier = Modifier.fillMaxWidth()) { Text(ui("stash.reset")) }
        }
    }
}
