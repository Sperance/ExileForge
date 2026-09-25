package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.PropertyValue
import com.sperance.exileforge.core.display.baseProperties
import com.sperance.exileforge.core.display.documentTitle
import com.sperance.exileforge.core.display.itemStates
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.rollSummary
import com.sperance.exileforge.core.display.shownLines
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.stateTitle
import com.sperance.exileforge.core.display.statTitle
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

/**
 * One item of a stash, as a line rather than a card.
 *
 * A card is a page about one item; a line is a stash you can read down. Everything that decides
 * whether to stop and open it rides here — what it is, where it goes, what it rolled — and the
 * card behind the tap keeps the rest.
 *
 * Since 2.21.0 the icon leads, in a square framed in the rarity colour with the item level and the
 * item's states under it, and the name takes that colour too: the frame is enough to find a unique
 * in a list without a band down every line. The base is read as figures — a chip per number, the
 * number set bold — and since 2.60.0 the rolls are summed up in one line: the rarity, how well they
 * landed inside their tiers and how many affix places are open. The card behind the tap lists them. [trailing] sits opposite
 * the name — a lot's price — or else [price], what the merchant pays, and [footer] under everything, for what a list adds about the item.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun ItemRow(document: JsonObject, definitions: List<ModifierDefinition> = emptyList(),
    note: String? = null, noteColor: Color = Gold, selected: Boolean = false, enabled: Boolean = true,
    /** Worn or socketed (2.51.0): the line is framed and washed in gold and the icon carries a badge. */
    worn: Boolean = false,
    /** The server's reasons this cannot be worn right now; empty means it can. */
    unwearable: List<String> = emptyList(),
    /** Extra facts for the line under the name, after the slot. */
    facts: List<String> = emptyList(),
    /** Opposite the name: the price of a lot. */
    trailing: (@Composable () -> Unit)? = null,
    /** What the merchant pays for it (2.46.0), drawn opposite the name when nothing else is there. */
    price: Long? = null,
    /** A line below the properties — the seller of a lot, and what else belongs at the bottom. */
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: () -> Unit) {
    val color = rarityColor(document.text("rarity"))
    // The base carries the number this copy really has — its own local modifiers are already in
    // it. The base the item started from stays on the card: a line has no room for a sum and its
    // history both.
    val base = baseProperties(document, definitions).flatMap { it.values }
    val rolled = shownLines((document["params"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }, definitions)
    val states = itemStates(document, definitions)
    val level = document.text("itemLevel")
    val slot = document.text("slot").takeIf { it.isNotBlank() }?.let(::slotTitle)
    val frame = RoundedCornerShape(6.dp)
    Row(Modifier.fillMaxWidth().background(if (worn) Gold.copy(alpha = .12f).compositeOver(Panel) else Panel, RoundedCornerShape(8.dp))
        .border(if (selected || worn) 2.dp else 1.dp, if (selected) GoldBright else if (worn) Gold else PanelRaised, RoundedCornerShape(8.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // The marker rides on the icon rather than in the text: the icon is where the eye starts.
            Box(Modifier.size(54.dp).background(color.copy(alpha = .08f), frame).border(1.dp, color, frame), contentAlignment = Alignment.Center) {
                ItemIcon(document, color, Modifier.size(34.dp))
                if (unwearable.isNotEmpty()) Icon(Icons.Outlined.Block, null, tint = LifeRed,
                    modifier = Modifier.align(Alignment.TopStart).padding(2.dp).size(14.dp))
                if (worn) Icon(Icons.Outlined.CheckCircle, ui("row.worn"), tint = Ink,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).background(Gold, CircleShape).padding(1.dp).size(15.dp))
            }
            if (level.isNotBlank()) MutedText(ui("row.level", level), style = MaterialTheme.typography.labelSmall)
            // States as symbols, three to a row under the icon: words about corruption and sockets
            // would push the properties off the line.
            states.chunked(3).forEach { three ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    three.forEach { state ->
                        Tipped({ Tip(stateTitle(state), tint = stateColor(state)) }) { Icon(stateGlyph(state), stateTitle(state), tint = stateColor(state), modifier = Modifier.size(13.dp)) }
                    }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(document.text("name").ifBlank { documentTitle(document) }, color = color,
                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                note?.let { Text(it, color = noteColor, style = MaterialTheme.typography.labelSmall) }
                trailing?.invoke() ?: price?.let { GoldPrice(it) }
            }
            (listOfNotNull(slot) + facts).takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString(" · "), color = Muted, style = MaterialTheme.typography.labelSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (base.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                base.forEach { value -> BaseChip(value) }
            }
            // Every line it rolled, as sentences (2.72.0): a stash is read down without opening each card.
            RollTops(rollSummary(document, rolled, definitions), rolled, definitions)
            // The server's verdict, in its own words — never a requirement worked out here.
            unwearable.forEach {
                Text(requirementReason(it), color = LifeRed, style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            footer?.invoke(this)
        }
    }
}

/** One base figure: the number bold, coloured when a local modifier moved it, and what it counts. */
@Composable internal fun BaseChip(value: PropertyValue) {
    Row(Modifier.background(Abyss, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(value.text, color = if (value.augmented) Rune else GoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        if (value.stat.isNotBlank()) Text(statTitle(value.stat), color = Muted, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    "shaper" -> ForgeGlyphs.Constellation
    "elder" -> ForgeGlyphs.Portal
    "fractured" -> ForgeGlyphs.Shard
    "crafted" -> ForgeGlyphs.Anvil
    "equipped" -> ForgeGlyphs.Helm
    "socketed" -> ForgeGlyphs.Gem
    else -> ForgeGlyphs.Sigil
}

internal fun stateColor(state: String) = when (state) {
    "corrupted" -> LifeRed
    "equipped" -> Gold
    "mirrored", "socketed" -> Rune
    "shaper" -> Shaper
    "elder" -> Elder
    "fractured" -> Fractured
    "crafted" -> Crafted
    else -> Muted
}
