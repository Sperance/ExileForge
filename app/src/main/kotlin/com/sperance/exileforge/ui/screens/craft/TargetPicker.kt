package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.forge.ForgeTarget
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.ItemFilter
import com.sperance.exileforge.presentation.state.ItemShelf
import com.sperance.exileforge.presentation.state.isWorn
import com.sperance.exileforge.presentation.state.itemShelf
import com.sperance.exileforge.presentation.state.railGroups
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.ItemFilterBar
import com.sperance.exileforge.ui.components.ItemFilterSheet
import com.sperance.exileforge.ui.components.ItemFilterState
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.screens.hero.PetCard
import com.sperance.exileforge.ui.screens.hero.PetCardSheet
import com.sperance.exileforge.ui.theme.*

/** Раздел выбора цели кузницы (4.4.x): вещи тайника и снаряжения или питомцы. */
internal enum class PickerSection(val title: String) { ITEMS("forge.pick_item"), PETS("forge.pick_pet") }

/**
 * Выбор цели кузницы (4.4.x): вещи (надетые тоже - сфере всё равно) или питомцы, раздел - вкладкой, если питомцы есть. С 4.2.0 у
 * вещей общий фильтр предметов ([ItemShelf.FORGE], [filterState] - общий с полкой у наковальни): строка над списком и шторка
 * «Аккордеон», выбор запоминается. Нажатие строки выбирает цель и закрывает выбор, долгое нажатие открывает карточку.
 */
@Composable
internal fun TargetPicker(game: GameUi, filterState: ItemFilterState, opened: PickerSection, current: String?, onDismiss: () -> Unit, onPick: (ForgeTarget) -> Unit) {
    val pets = game.hero?.pets?.pets.orEmpty()
    val sections = listOfNotNull(PickerSection.ITEMS, PickerSection.PETS.takeIf { pets.isNotEmpty() })
    var section by remember(opened) { mutableStateOf(opened.takeIf { it in sections } ?: PickerSection.ITEMS) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f)) {
            if (sections.size > 1) {
                PillTabs(sections.map { ui(it.title) }, sections.indexOf(section), { section = sections[it] }, Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), segmented = true)
            }
            when (section) {
                PickerSection.ITEMS -> ItemTargets(game, filterState, current, onPick)
                PickerSection.PETS -> PetTargets(game, pets, current, onPick)
            }
        }
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
        item { Engraved(ui(PickerSection.ITEMS.title)) }
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

/** Питомцы под кузницу - карточками Зверинца: нажатие выбирает, долгое нажатие открывает карточку в шторке. */
@Composable private fun PetTargets(game: GameUi, pets: List<Pet>, current: String?, onPick: (ForgeTarget) -> Unit) {
    val index = game.index ?: return
    val menagerie = remember(index) { Menagerie(index) }
    var carded by remember { mutableStateOf<Pet?>(null) }
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Engraved(ui(PickerSection.PETS.title)) }
        items(pets, key = { it.id }) { pet ->
            val kind = menagerie.species(pet.species) ?: return@items
            PetCard(
                game,
                index,
                menagerie,
                kind,
                pet,
                game.hero?.pets?.isActive(pet.id) == true,
                selected = pet.id == current,
                onClick = { onPick(ForgeTarget.Beast(pet)) },
                onLongClick = { carded = pet },
            )
        }
    }
    carded?.let { pet -> PetCardSheet(game, pet) { carded = null } }
}
