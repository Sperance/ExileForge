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
import androidx.compose.material.icons.outlined.History
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
import com.sperance.exileforge.presentation.state.ForgeState
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

/** How many items the forge remembers as worked on lately. */
internal const val RECENT_TARGETS = 6

/** A shelf of the forge's item picker (the owner's mockup B): what the item is, or how it stands to the player. */
internal enum class TargetFilter(val title: String, val glyph: ImageVector) {
    ALL("forge.filter_all", Icons.Outlined.Search),
    RECENT("forge.filter_recent", Icons.Outlined.History),
    GEAR("forge.filter_gear", ForgeGlyphs.Swords),
    MAPS("forge.filter_maps", ForgeGlyphs.Atlas),
    TOOLS("forge.filter_tools", Icons.Outlined.Build),
    WORN("forge.filter_worn", Icons.Outlined.CheckCircle),
    ;

    fun admits(piece: ItemView, recent: List<String>): Boolean = when (this) {
        ALL -> true
        RECENT -> piece.id in recent
        GEAR -> piece.slot != Slot.MAP && !piece.slot.isTool
        MAPS -> piece.slot == Slot.MAP
        TOOLS -> piece.slot.isTool
        WORN -> piece.isWorn
    }
}

/**
 * The stash to pick what the forge works on (worn items included, since an orb does not care): a search over names and lines,
 * the shelves that hold anything, and the list — the items worked on lately first, then the rarest and highest.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TargetPicker(s: ForgeState, recent: List<String>, initial: TargetFilter, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val stash = remember(s.hero?.items, s.index) { s.hero?.items.orEmpty().mapNotNull { s.view(it) } }
    val shelves = remember(stash, recent) { TargetFilter.entries.filter { filter -> filter == TargetFilter.ALL || stash.any { filter.admits(it, recent) } } }
    var filter by remember { mutableStateOf(initial.takeIf { it in shelves } ?: TargetFilter.ALL) }
    var query by remember { mutableStateOf("") }
    val shown = remember(stash, recent, filter, query) {
        stash.filter { filter.admits(it, recent) && ItemSearch.matches(it, query) }
            .sortedWith(compareBy<ItemView>({ recent.indexOf(it.id).let { at -> if (at < 0) Int.MAX_VALUE else at } }, { -it.rarity.ordinal }, { -it.level }))
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
                    selected = piece.id == s.play.selectedEquipment,
                    facts = if (piece.isWorn) listOf(ui("hero.equipped")) else emptyList(),
                    price = s.sellPrice(piece.item),
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
