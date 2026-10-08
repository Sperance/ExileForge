package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.ItemFilter
import com.sperance.exileforge.presentation.state.ItemShelf
import com.sperance.exileforge.presentation.state.isWorn
import com.sperance.exileforge.presentation.state.itemShelf
import com.sperance.exileforge.presentation.state.railGroups
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ItemFilterBar
import com.sperance.exileforge.ui.components.ItemFilterSheet
import com.sperance.exileforge.ui.components.ItemFilterState
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.theme.*

/**
 * The stash to pick what the forge works on (worn items included, since an orb does not care). С 4.2.0 - общий фильтр предметов
 * ([ItemShelf.FORGE], [filterState] - общий с полкой у наковальни): строка над списком и шторка «Аккордеон», выбор запоминается.
 */
@Composable
internal fun TargetPicker(game: GameUi, filterState: ItemFilterState, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val stash = remember(game.hero?.items, game.index) { game.hero?.items.orEmpty().mapNotNull { game.view(it) } }
    var filtering by remember { mutableStateOf(false) }
    val shelf = { draft: ItemFilter -> game.itemShelf(stash, ItemShelf.FORGE, draft) { game.sellPrice(it.item) } }
    val shown = remember(stash, filterState.filter, game.hero) { shelf(filterState.filter) }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("forge.pick_item")) }
            item { ItemFilterBar(filterState, shown.size, onOpen = { filtering = true }) }
            if (shown.isEmpty()) item { Text(ui(if (stash.isEmpty()) "forge.stash_empty" else "filter.empty"), color = Muted) }
            items(shown, key = { it.id }) { piece ->
                ItemRow(
                    piece,
                    selected = piece.id == game.holding.selectedEquipment,
                    facts = if (piece.isWorn) listOf(ui("hero.equipped")) else emptyList(),
                    price = game.sellPrice(piece.item),
                ) { onPick(piece.id) }
            }
        }
    }
    if (filtering) {
        val groups = remember(stash) { railGroups(stash).map { it.first } }
        val rarities = remember(stash) { stash.map { it.rarity }.distinct().sortedByDescending { it.ordinal } }
        ItemFilterSheet(filterState, groups, rarities, game.lang, count = { shelf(it).size }) { filtering = false }
    }
}
