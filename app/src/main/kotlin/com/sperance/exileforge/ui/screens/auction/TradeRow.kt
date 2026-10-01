package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ItemLines
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/**
 * One piece of goods as a compact line (the auction's and the merchant's variant A): the icon in its rarity's frame, the
 * name in its colour on one line, the facts under it — or, in red, why the hero could not wear it — then every line the
 * item carries, and the price opposite. [mark] closes the facts in rune blue: «your lot», a lot's time left.
 */
@Composable internal fun TradeRow(title: String, color: Color, facts: List<String>, lines: List<ItemLine>, enabled: Boolean,
    unmet: List<String> = emptyList(), mark: String? = null, onClick: () -> Unit,
    icon: @Composable BoxScope.() -> Unit, price: @Composable ColumnScope.() -> Unit) {
    val card = RoundedCornerShape(12.dp)
    val frame = RoundedCornerShape(10.dp)
    Row(Modifier.fillMaxWidth().background(Panel, card).border(1.dp, Bronze, card).clickable(enabled = enabled, onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(44.dp).background(Abyss, frame).border(1.dp, color.copy(alpha = .6f), frame), contentAlignment = Alignment.Center, content = icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = buildAnnotatedString {
                if (unmet.isNotEmpty()) withStyle(SpanStyle(color = LifeRed)) { append(unmet.joinToString(", ") { requirementReason(it) }) }
                else append(facts.joinToString(" · "))
                mark?.let {
                    if (length > 0) append(" · ")
                    withStyle(SpanStyle(color = Rune)) { append(it) }
                }
            }
            Text(line, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ItemLines(lines)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp), content = price)
    }
}

/** A copy of an item as a [TradeRow]: its slot, item level and weapon kind for facts, its own drawing for the icon. */
@Composable internal fun ItemTradeRow(view: ItemView, enabled: Boolean, unmet: List<String>, extra: List<String> = emptyList(),
    mark: String? = null, onClick: () -> Unit, price: @Composable ColumnScope.() -> Unit) {
    val color = rarityColor(view.rarity.name)
    TradeRow(view.title, color, listOfNotNull(slotTitle(view.slot), ui("row.level", view.level), view.weaponType?.let { weaponTitle(it) }) + extra,
        view.lines, enabled, unmet, mark, onClick, icon = { ItemIcon(view, color, Modifier.size(28.dp)) }, price = price)
}
