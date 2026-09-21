package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.documentTitle
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * A stash of anything, folded away until it is wanted.
 *
 * The count belongs in the header rather than inside: a player deciding whether to open a bag is
 * asking how much is in it, and answering that costs no space at all when the answer is nothing.
 */
@Composable fun ExpandableSection(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title, count, expanded, onToggle)
        if (expanded) content()
    }
}

/**
 * The header alone, for a section whose rows are items of a `LazyColumn`.
 *
 * A stash of a hundred instances has to stay lazy, and lazy rows cannot live inside a composable
 * that takes its content as a block — so the fold is offered as a header the list can carry.
 */
@Composable fun SectionHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Engraved(title, modifier = Modifier.weight(1f))
        Text(count.toString(), color = GoldBright, style = MaterialTheme.typography.labelLarge)
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Gold, modifier = Modifier.size(20.dp))
    }
}

/**
 * One item of a stash, as a line rather than a card.
 *
 * A card is a page about one item; a line is a stash you can read down. Everything that decides
 * whether to stop and open it rides here — what it is, where it goes, what it rolled — and the
 * card behind the tap keeps the rest.
 *
 * The rolled modifiers are the deciding half, so they are printed even though they will not fit:
 * one clipped line of real properties tells a player more than a count of them would.
 */
@Composable fun ItemRow(document: JsonObject, definitions: List<ModifierDefinition> = emptyList(),
    note: String? = null, noteColor: Color = Gold, selected: Boolean = false, enabled: Boolean = true,
    onClick: () -> Unit) {
    val color = rarityColor(document.text("rarity"))
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    val rolled = (document["params"] as? JsonArray).orEmpty()
    val level = document.text("itemLevel")
    val slot = document.text("slot").takeIf { it.isNotBlank() }?.let(::slotTitle)
    Row(Modifier.fillMaxWidth().background(Panel, shape)
        .border(if (selected) 2.dp else 1.dp, if (selected) GoldBright else color.copy(alpha = .40f), shape)
        .clickable(enabled = enabled, onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ItemIcon(document, color, Modifier.size(36.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(document.text("name").ifBlank { documentTitle(document) }, color = color,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                note?.let { Text(it, color = noteColor, style = MaterialTheme.typography.labelSmall) }
            }
            listOfNotNull(slot, level.takeIf { it.isNotBlank() }?.let { tr("ур. $it", "lvl $it") })
                .takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(" · "), color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            if (rolled.isNotEmpty()) Text(
                rolled.mapNotNull { (it as? JsonObject)?.let { one -> modifierText(one, definitions) } }.joinToString(" · "),
                color = Rune, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
