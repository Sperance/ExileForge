package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*

/**
 * One modifier, as a whole sentence.
 *
 * The server's dictionary holds the phrasing — "+{0} to armour" — and the rolled values fill it,
 * so there is no label to put on the left of a number any more.
 */
@Composable fun ModifierLine(modifier: JsonObject, definitions: List<ModifierDefinition>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Glyph.ofModifier(modifier.text("modifierId"), definitions).vector, null, tint = Rune, modifier = Modifier.size(16.dp))
        Text(modifierText(modifier, definitions), color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * A rolled affix on a card, under its rhombus: its sentence, then its tier and what placed it.
 *
 * The line takes the colour Path of Exile gives it — a crafted modifier reads in the bench's blue,
 * a fractured one in its dull gold — and says so in a word as well, because a colour alone is not
 * read by everyone. The tier is small and last: it is what a trader checks, not what a player reads.
 */
@Composable fun AffixLine(modifier: JsonObject, definitions: List<ModifierDefinition>) {
    val marks = affixMarks(modifier, definitions)
    val tone = when { marks.fractured -> Fractured; marks.crafted -> Crafted; marks.handcrafted -> Handcrafted; marks.alchemy -> Vital; else -> Rune }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.padding(top = 6.dp)) { Rhombus() }
        Text(modifierText(modifier, definitions), color = tone, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        listOfNotNull(
            (ui("mod.fractured") to tone).takeIf { marks.fractured },
            (ui("mod.crafted") to tone).takeIf { marks.crafted },
            (ui("mod.handcrafted") to tone).takeIf { marks.handcrafted },
            (ui("mod.alchemy") to tone).takeIf { marks.alchemy },
            (ui("mod.tier", marks.tier) to Muted).takeIf { marks.tier > 0 },
        ).forEach { (word, color) ->
            Text(word, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * A base property as one sentence, with the number the item really carries.
 *
 * The dictionary's template is filled with the folded value, and that value is coloured when a
 * modifier moved it — the base it started from follows in brackets, so the line answers both
 * "how much" and "why is it not the number on the shelf".
 */
fun basePropertyText(property: BaseProperty, withBase: Boolean): AnnotatedString = buildAnnotatedString {
    fun value(one: PropertyValue) {
        if (!one.augmented) { append(one.text); return }
        withStyle(SpanStyle(color = Rune, fontWeight = FontWeight.SemiBold)) { append(one.text) }
        if (withBase) withStyle(SpanStyle(color = Muted)) { append(" (${one.baseText})") }
    }
    if (property.template.isBlank()) {
        property.values.forEachIndexed { index, one ->
            if (index > 0) append(" · ")
            value(one)
            if (one.stat.isNotBlank()) { append(" "); append(statTitle(one.stat)) }
        }
        return@buildAnnotatedString
    }
    var rest = property.template
    while (true) {
        val next = property.values.indices
            .mapNotNull { index -> rest.indexOf("{$index}").takeIf { it >= 0 }?.let { it to index } }
            .minByOrNull { it.first } ?: break
        val (at, index) = next
        append(rest.substring(0, at))
        value(property.values[index])
        rest = rest.substring(at + "{$index}".length)
    }
    append(rest)
}

/**
 * One line of an item's base, drawn like a modifier but carrying the folded number.
 */
@Composable fun BasePropertyLine(property: BaseProperty, withBase: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Glyph.ofStat(property.values.firstOrNull()?.stat.orEmpty()).vector, null, tint = Rune, modifier = Modifier.size(16.dp))
        Text(basePropertyText(property, withBase), color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * The number an item *is*, set large, with the characteristic under it.
 *
 * The base of an item is the one thing a player weighs it by, so it is read as a figure rather
 * than as a sentence: 120 and "броня", not "+120 к броне". What the base was before the item's own
 * modifiers raised it follows quietly, because a raised number without its origin is a claim.
 *
 * A property the dictionary cannot name a characteristic for falls back to the sentence — a line
 * that reads oddly is better than a number with nothing beside it.
 */
@Composable private fun BannerStat(property: BaseProperty, big: Boolean) {
    val stat = property.values.firstOrNull()?.stat.orEmpty()
    if (stat.isBlank()) { BasePropertyLine(property); return }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(property.values.joinToString(" · ") { it.text },
            color = if (property.augmented) Rune else Parchment,
            style = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium)
        Text(statTitle(stat).uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = if (big) 4.dp else 1.dp))
        property.values.firstOrNull()?.takeIf { it.augmented }?.let {
            Text(ui("card.was", it.baseText), color = Muted, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = if (big) 4.dp else 1.dp))
        }
    }
}

/**
 * An item as a banner: a band of its rarity down the edge, and the rest read top to bottom.
 *
 * There is no frame — rarity is the spine, which leaves the name in the colour of every other name
 * and the card quiet enough to read. Above the name are the states it is in, drawn rather than
 * spelled out because they are glanced at, and what it is; below it the base as figures, and the
 * rolls as a list under their rhombus. The icon sits beside the name: it is how the item is
 * recognised before any of it is read.
 */
@Composable fun ItemCard(doc: JsonObject, enabled: Boolean = true, selected: Boolean = false,
    detailed: Boolean = false, definitions: List<ModifierDefinition> = emptyList(),
    actionLabel: String = ui("common.open"),
    /** What the merchant pays for this copy (2.46.0); it replaces the template's bare base price. */
    price: Long? = null, onClick: () -> Unit = {}) {
    val color = rarityColor(doc.text("rarity"))
    val base = baseProperties(doc, definitions)
    val states = itemStates(doc, definitions)
    val rolled = shownLines((doc["params"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }, definitions)
    val kind = doc.text("slot").takeIf { it.isNotBlank() }?.let(::slotTitle)
        ?: doc.text("category").takeIf { it.isNotBlank() }
        ?: if (doc["userId"] != null) ui("card.character") else ui("card.item")

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Panel)
        .border(if (selected) 2.dp else 1.dp, if (selected) GoldBright else Bronze.copy(alpha = .40f))
        .clickable(enabled = enabled, onClick = onClick)) {
        RaritySpine(color, 6.dp)
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // The ribbon: what it is, and what state it is in. Both are glanced at, never read.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                states.forEach {
                    Icon(stateGlyph(it), stateTitle(it), tint = stateColor(it), modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(kind, color = Muted, style = MaterialTheme.typography.labelSmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ItemIcon(doc, color, Modifier.size(56.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(doc.text("name").ifBlank { documentTitle(doc) }, color = Parchment,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = if (detailed) 5 else 2, overflow = TextOverflow.Ellipsis)
                    cardFacts(doc, withPrice = price == null).forEach {
                        Text(it, color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (selected) Icon(Icons.Outlined.CheckCircle, ui("card.selected"), tint = GoldBright, modifier = Modifier.size(22.dp))
            }

            // The base first, as figures: the biggest is what the item is bought for.
            base.forEachIndexed { index, property -> BannerStat(property, big = index == 0) }
            // Then what this copy rolled, which is what makes it this one rather than another.
            rolled.take(if (detailed) rolled.size else 3).forEach { AffixLine(it, definitions) }
            if (!detailed && rolled.size > 3) Text(ui("card.more_properties", rolled.size - 3), color = Muted, style = MaterialTheme.typography.labelMedium)
            if (detailed) documentDescription(doc).takeIf { it.isNotBlank() }?.let {
                Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                price?.let {
                    Text(ui("price.sell"), color = Muted, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(6.dp)); GoldPrice(it); Spacer(Modifier.weight(1f))
                }
                Text(actionLabel.uppercase(), color = Gold, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
            }
        }
    }
}

/**
 * The short facts under a name: everything that is a field of the document rather than a modifier.
 *
 * Two lines at most — what the item is worth knowing about before its properties, then the odds
 * and ends a particular kind of document carries. A field the document does not have is left out
 * rather than printed as nothing.
 */
private fun cardFacts(doc: JsonObject, withPrice: Boolean = true): List<String> = listOfNotNull(
    listOfNotNull(
        doc.text("itemLevel").takeIf { it.isNotBlank() }?.let { ui("row.level", it) },
        doc.text("level").takeIf { it.isNotBlank() }?.let { ui("row.level", it) },
        itemRequirements(doc).takeIf { it.isNotEmpty() }?.let { ui("auction.needs", it.joinToString(", ")) },
    ).joinToString(" · ").takeIf { it.isNotBlank() },
    listOfNotNull(
        doc.text("weaponType").takeIf { it.isNotBlank() }?.let(::weaponTitle),
        doc.text("durability").takeIf { it.isNotBlank() }?.let { "${ui("card.durability")} $it" },
        doc.text("price").takeIf { withPrice && it.isNotBlank() }?.let { "${ui("card.price")} $it" },
        doc.text("money").takeIf { it.isNotBlank() }?.let { "${ui("card.gold")} $it" },
    ).joinToString(" · ").takeIf { it.isNotBlank() },
)
