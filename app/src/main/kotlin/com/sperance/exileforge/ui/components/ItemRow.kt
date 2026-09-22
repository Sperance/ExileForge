package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.baseProperties
import com.sperance.exileforge.core.display.documentTitle
import com.sperance.exileforge.core.display.itemStates
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.stateTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.ui.icons.ForgeGlyphs
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

/** How many properties a line carries before it stops being a line. */
const val ROW_PROPERTIES = 5

/**
 * One item of a stash, as a line rather than a card.
 *
 * A card is a page about one item; a line is a stash you can read down. Everything that decides
 * whether to stop and open it rides here — what it is, where it goes, what it rolled — and the
 * card behind the tap keeps the rest.
 *
 * Rarity is the band down the left edge rather than a frame around the whole line: a stash is a
 * column of these, and a hundred coloured boxes read as a fence. The band is enough to find a
 * unique in a list, and it leaves the name in the colour of every other name.
 *
 * The properties are the deciding half, so they are printed one per line rather than crushed into
 * one: five of them clipped at a screen edge is a count, not a reading. What does not fit is
 * counted instead of dropped.
 */
@Composable fun ItemRow(document: JsonObject, definitions: List<ModifierDefinition> = emptyList(),
    note: String? = null, noteColor: Color = Gold, selected: Boolean = false, enabled: Boolean = true,
    /** The server's reasons this cannot be worn right now; empty means it can. */
    unwearable: List<String> = emptyList(),
    /** Extra facts for the second line, after the slot and the level. */
    facts: List<String> = emptyList(),
    /** A line below the properties — the price of a lot, and what else belongs at the bottom. */
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: () -> Unit) {
    val color = rarityColor(document.text("rarity"))
    // The base first, carrying the number this copy really has — its own local modifiers are
    // already in it — and the rolls after, which is the order a card reads in too. The base the
    // item started from stays on the card: a line has no room for a sum and its history both.
    val base = baseProperties(document, definitions)
    val rolled = (document["params"] as? JsonArray).orEmpty()
        .mapNotNull { (it as? JsonObject)?.let { one -> AnnotatedString(modifierText(one, definitions)) } }
    val properties = base.map { basePropertyText(it, withBase = false) } + rolled
    val states = itemStates(document)
    val level = document.text("itemLevel")
    val slot = document.text("slot").takeIf { it.isNotBlank() }?.let(::slotTitle)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Panel)
        .border(if (selected) 2.dp else 1.dp, if (selected) GoldBright else Bronze.copy(alpha = .30f))
        .clickable(enabled = enabled, onClick = onClick)) {
        RaritySpine(color, 4.dp)
        Column(Modifier.weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                // The marker rides on the icon rather than in the text: the icon is where the eye
                // starts, and a line of its own would push the properties further down every row.
                Box {
                    ItemIcon(document, color, Modifier.size(34.dp))
                    if (unwearable.isNotEmpty()) Icon(Icons.Outlined.Block, null, tint = LifeRed,
                        modifier = Modifier.size(16.dp).align(Alignment.TopStart))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(document.text("name").ifBlank { documentTitle(document) }, color = Parchment,
                            style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        note?.let { Text(it, color = noteColor, style = MaterialTheme.typography.labelSmall) }
                    }
                    (listOfNotNull(slot, level.takeIf { it.isNotBlank() }?.let { ui("row.level", it) }) + facts)
                        .takeIf { it.isNotEmpty() }?.let {
                            Text(it.joinToString(" · "), color = Muted, style = MaterialTheme.typography.labelSmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    properties.take(ROW_PROPERTIES).forEachIndexed { index, property ->
                        Text(property, color = if (index < base.size) Parchment else Rune,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Counted rather than dropped: "ещё 3" is the difference between a short item
                    // and one whose best roll is just off the edge.
                    (properties.size - ROW_PROPERTIES).takeIf { it > 0 }?.let {
                        Text(ui("row.more", it), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    // The server's verdict, in its own words — never a requirement worked out here.
                    unwearable.forEach {
                        Text(requirementReason(it), color = LifeRed, style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // States get a line of their own at the bottom, as symbols: a line is read down,
                    // and four words about corruption and sockets would push the properties off it.
                    if (states.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        states.forEach { state ->
                            Icon(stateGlyph(state), stateTitle(state), tint = stateColor(state), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            footer?.invoke(this)
        }
    }
}

/**
 * The drawing of a state, and its colour.
 *
 * A flag the server grows tomorrow gets the neutral sigil rather than nothing, so a row never
 * silently drops a state it has no picture for.
 */
internal fun stateGlyph(state: String) = when (state) {
    "corrupted" -> ForgeGlyphs.Skull
    "mirrored" -> ForgeGlyphs.Chain
    "equipped" -> ForgeGlyphs.Helm
    "socketed" -> ForgeGlyphs.Gem
    else -> ForgeGlyphs.Sigil
}

internal fun stateColor(state: String) = when (state) {
    "corrupted" -> LifeRed
    "equipped" -> Gold
    "mirrored", "socketed" -> Rune
    else -> Muted
}
