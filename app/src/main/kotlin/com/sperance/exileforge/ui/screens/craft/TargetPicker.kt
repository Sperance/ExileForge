package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.character.sellPrice
import com.sperance.exileforge.core.character.view
import com.sperance.exileforge.core.display.ItemFilter
import com.sperance.exileforge.core.display.ItemShelf
import com.sperance.exileforge.core.display.isWorn
import com.sperance.exileforge.core.display.itemShelf
import com.sperance.exileforge.core.display.railGroups
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.forge.ForgeTarget
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.HoldHint
import com.sperance.exileforge.ui.components.HoldSubject
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.ItemFilterBar
import com.sperance.exileforge.ui.components.ItemFilterSheet
import com.sperance.exileforge.ui.components.ItemFilterState
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.theme.*

/**
 * Выбор цели кузницы: вещи тайника и снаряжения (надетые тоже - сфере всё равно). Общий фильтр предметов
 * ([ItemShelf.FORGE], [filterState] - общий с полкой у наковальни): строка над списком и шторка «Аккордеон», выбор запоминается.
 * Нажатие строки выбирает цель и закрывает выбор, долгое нажатие открывает карточку.
 */
@Composable
internal fun TargetPicker(game: GameUi, filterState: ItemFilterState, current: String?, onDismiss: () -> Unit, onPick: (ForgeTarget) -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f)) { ItemTargets(game, filterState, current, onPick) }
    }
}

/** Вещи под кузницу: общий фильтр, строка - выбрать, долгое нажатие - карточка вещи. */
@Composable private fun ItemTargets(game: GameUi, filterState: ItemFilterState, current: String?, onPick: (ForgeTarget) -> Unit) {
    val stash = remember(game.hero?.items, game.index) { game.hero?.items.orEmpty().mapNotNull { game.view(it) } }
    var filtering by remember { mutableStateOf(false) }
    val inspect = rememberInspect()
    val shelf = { draft: ItemFilter -> game.itemShelf(stash, ItemShelf.FORGE, draft) { game.sellPrice(it.item) } }
    val shown = remember(stash, filterState.filter, game.hero) { shelf(filterState.filter) }
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Engraved(ui("forge.pick_item")) }
        if (stash.isNotEmpty()) item { HoldHint(HoldSubject.ITEM) }
        item { ItemFilterBar(filterState, shown.size, onOpen = { filtering = true }) }
        if (shown.isEmpty()) item { Text(ui(if (stash.isEmpty()) "forge.stash_empty" else "filter.empty"), color = Muted) }
        items(shown, key = { it.id }) { piece ->
            ItemRow(
                piece,
                selected = piece.id == current,
                facts = if (piece.isWorn) listOf(ui("hero.equipped")) else emptyList(),
                price = game.sellPrice(piece.item),
                onLongClick = { inspect(Inspect.Copy(piece.item)) },
            ) { onPick(ForgeTarget.Gear(piece)) }
        }
    }
    if (filtering) {
        val groups = remember(stash) { railGroups(stash).map { it.first } }
        val rarities = remember(stash) { stash.map { it.rarity }.distinct().sortedByDescending { it.ordinal } }
        ItemFilterSheet(filterState, groups, rarities, game.lang, count = { shelf(it).size }) { filtering = false }
    }
}
