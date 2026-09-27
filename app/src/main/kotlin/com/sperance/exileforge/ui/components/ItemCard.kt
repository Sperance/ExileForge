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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.BaseProperty
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.PropertyValue
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.stateTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.*

/**
 * One fixed line — a tree node's, a class's — as a whole sentence.
 *
 * The server's dictionary holds the phrasing — "+{0} to armour" — and the line's values fill it,
 * so there is no label to put on the left of a number any more. The glyph is the characteristic
 * the modifier's first effect changes.
 */
@Composable fun ModifierLine(index: ContentIndex, line: Line) = ModifierLine(lineText(index, line), Glyph.ofModifier(line.code, index))

/** A modifier's sentence already worded, under the glyph of what it changes. */
@Composable fun ModifierLine(text: String, glyph: Glyph = Glyph.INFO) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(glyph.vector, null, tint = Rune, modifier = Modifier.size(16.dp))
        Text(text, color = ModBlue, style = MaterialTheme.typography.bodyMedium)
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
 * rolls as a trade table (2.60.0): the score of the roll over a row per line. The icon sits beside the name: it is how the item is
 * recognised before any of it is read. Everything printed is the [item]'s view (3.0.0): the copy over its template and the content.
 */
@Composable fun ItemCard(item: ItemView, enabled: Boolean = true, selected: Boolean = false,
    detailed: Boolean = false, actionLabel: String = ui("common.open"),
    /** What the merchant pays for this copy (2.46.0); it replaces the template's bare base price. */
    price: Long? = null,
    /** Whether the card shows its [actionLabel]: a short card always, a full one only when it leads somewhere (3.2.0). */
    action: Boolean = !detailed, onClick: () -> Unit = {}) {
    val color = rarityColor(item.rarity.name)
    val base = item.base
    val states = item.states
    val rolled = item.lines
    val kind = slotTitle(item.slot)

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
                MutedText(kind, style = MaterialTheme.typography.labelSmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ItemIcon(item, color, Modifier.size(56.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, color = Parchment,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = if (detailed) 5 else 2, overflow = TextOverflow.Ellipsis)
                    // The English trade name, on a full card only (2.51.0): what it is searched by.
                    if (detailed) item.trade?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic) }
                    cardFacts(item, withPrice = price == null).forEach {
                        MutedText(it, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (selected) Icon(Icons.Outlined.CheckCircle, ui("card.selected"), tint = GoldBright, modifier = Modifier.size(22.dp))
            }

            // The base first, as figures: the biggest is what the item is bought for.
            base.forEachIndexed { index, property -> BannerStat(property, big = index == 0) }
            // Then what this copy rolled, as a trade table (2.60.0): the figures a trader weighs it by,
            // then a row per line with how high it landed inside its tier.
            if (rolled.isNotEmpty()) {
                if (detailed) RollScore(item.summary)
                TradeTable(rolled.take(if (detailed) rolled.size else 3))
            }
            if (!detailed && rolled.size > 3) MutedText(ui("card.more_properties", rolled.size - 3), style = MaterialTheme.typography.labelMedium)
            if (detailed) item.description.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                price?.let {
                    MutedText(ui("price.sell"), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(6.dp)); GoldPrice(it); Spacer(Modifier.weight(1f))
                }
                // A full card is a page, not a way in (2.51.0) — unless it is asked to lead on (3.2.0).
                if (action) {
                    Text(actionLabel.uppercase(), color = Gold, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
                }
            }
        }
    }
}

/**
 * The short facts under a name: everything that is a field of the template rather than a modifier.
 *
 * Two lines at most — what the item is worth knowing about before its properties, then the odds
 * and ends a particular kind of item carries. A fact the template does not have is left out
 * rather than printed as nothing.
 */
private fun cardFacts(item: ItemView, withPrice: Boolean = true): List<String> = listOfNotNull(
    listOfNotNull(
        ui("row.level", item.level),
        item.requirements.takeIf { it.isNotEmpty() }?.let { ui("auction.needs", it.joinToString(", ")) },
    ).joinToString(" · ").takeIf { it.isNotBlank() },
    listOfNotNull(
        item.weaponType?.let { weaponTitle(it) },
        // A flask's quality (2.78.0): each percent a percent more effect or recovery.
        item.quality.takeIf { it > 0 }?.let { ui("card.quality", it) },
        "${ui("card.durability")} ${item.template.durability}",
        item.template.price?.takeIf { withPrice }?.let { "${ui("card.price")} $it" },
    ).joinToString(" · ").takeIf { it.isNotBlank() },
)
