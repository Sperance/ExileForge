package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.isWorn
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.DefaultInputs
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** A shelf of the forge's item picker (the owner's mockup B): what the item is, or how it stands to the player. */
internal enum class TargetFilter(val title: String, val glyph: ImageVector) {
    ALL("forge.filter_all", Icons.Outlined.Search),
    GEAR("forge.filter_gear", ForgeGlyphs.Swords),
    MAPS("forge.filter_maps", ForgeGlyphs.Atlas),
    TOOLS("forge.filter_tools", Icons.Outlined.Build),
    WORN("forge.filter_worn", Icons.Outlined.CheckCircle),
    ;

    fun admits(piece: ItemView): Boolean = when (this) {
        ALL -> true
        GEAR -> piece.slot != Slot.MAP && !piece.slot.isTool
        MAPS -> piece.slot == Slot.MAP
        TOOLS -> piece.slot.isTool
        WORN -> piece.isWorn
    }

    companion object {
        /** Разделы на полке у наковальни. */
        val RAIL = listOf(ALL, GEAR, MAPS, TOOLS)
    }
}

/**
 * The stash to pick what the forge works on (worn items included, since an orb does not care): a search over names and lines,
 * the shelves that hold anything, and the list — the rarest and highest first.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TargetPicker(game: GameUi, initial: TargetFilter, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val stash = remember(game.hero?.items, game.index) { game.hero?.items.orEmpty().mapNotNull { game.view(it) } }
    val shelves = remember(stash) { TargetFilter.entries.filter { filter -> filter == TargetFilter.ALL || stash.any { filter.admits(it) } } }
    var filter by remember { mutableStateOf(initial.takeIf { it in shelves } ?: TargetFilter.ALL) }
    var query by remember { mutableStateOf("") }
    val shown = remember(stash, filter, query) {
        stash.filter { filter.admits(it) && ItemSearch.matches(it, query) }
            .sortedWith(compareBy<ItemView>({ -it.rarity.ordinal }, { -it.level }))
    }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("forge.pick_item")) }
            item {
                OutlinedTextField(
                    query,
                    { query = it.take(DefaultInputs.search) },
                    placeholder = { Text(ui("hero.search_hint")) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, null) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (shelves.size > 1) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shelves.forEach { shelf -> ShelfPill(shelf, on = shelf == filter) { filter = shelf } }
                    }
                }
            }
            if (shown.isEmpty()) item { Text(ui(if (stash.isEmpty()) "forge.stash_empty" else "forge.nothing_found"), color = Muted) }
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
}

/** One shelf of the picker: its drawing and name, filled in gold when open. */
@Composable private fun ShelfPill(shelf: TargetFilter, on: Boolean, onClick: () -> Unit) {
    val pill = RoundedCornerShape(16.dp)
    Row(
        Modifier.clip(pill).background(if (on) Gold else PanelRaised, pill).clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(shelf.glyph, null, tint = if (on) Ink else Muted, modifier = Modifier.size(14.dp))
        Text(ui(shelf.title), color = if (on) Ink else Parchment, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
