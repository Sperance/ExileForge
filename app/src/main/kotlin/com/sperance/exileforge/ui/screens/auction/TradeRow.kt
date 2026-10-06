package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/**
 * Товар «Ценником» (3.89.0, выбор владельца): плита стопки сумки - иконка, имя, факты и ярлык цены сверху; [mark] руническим
 * синим после фактов: «ваш лот», сколько ему стоять.
 */
@Composable internal fun TradeRow(
    title: String,
    color: Color,
    facts: List<String>,
    enabled: Boolean,
    mark: String? = null,
    onClick: () -> Unit,
    icon: @Composable BoxScope.() -> Unit,
    price: @Composable RowScope.() -> Unit,
) {
    val card = RoundedCornerShape(16.dp)
    val frame = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(card).background(Brush.verticalGradient(listOf(Color(0xFF121A20), Panel))).border(1.dp, color.copy(alpha = .2f), card)
                .clickable(enabled = enabled, onClick = onClick).padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).background(Abyss, frame).border(1.dp, color.copy(alpha = .55f), frame), contentAlignment = Alignment.Center, content = icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = color, style = relicName(15), maxLines = 2, overflow = TextOverflow.Ellipsis)
                TradeFacts(facts, mark)
            }
        }
        val shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
        Row(
            Modifier.align(Alignment.TopEnd).padding(end = 12.dp).height(26.dp).background(Color(0xFF1A242C), shape).border(1.dp, Color(0x40E2C15A), shape)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            content = price,
        )
    }
}

/** Факты товара одной строкой и [mark] руническим синим в конце. */
@Composable private fun TradeFacts(facts: List<String>, mark: String?) {
    val line = buildAnnotatedString {
        append(facts.joinToString(" · "))
        mark?.let {
            if (length > 0) append(" · ")
            withStyle(SpanStyle(color = Rune)) { append(it) }
        }
    }
    if (line.isNotEmpty()) Text(line, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/**
 * Копия вещи «Ценником»: плита тайника со всеми строками и значками тиров, ярлык цены сверху, а внизу через нить - продавец
 * и [mark]. Вещь, которую герой надеть не может, говорит почему красным, как в тайнике.
 */
@Composable internal fun ItemTradeRow(
    view: ItemView,
    enabled: Boolean,
    unmet: List<String>,
    extra: List<String> = emptyList(),
    mark: String? = null,
    onClick: () -> Unit,
    price: @Composable RowScope.() -> Unit,
) {
    ItemRow(
        view,
        enabled = enabled,
        unwearable = unmet,
        tag = price,
        footer = if (extra.isEmpty() && mark == null) {
            null
        } else {
            {
                HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = .05f))
                TradeFacts(extra, mark)
            }
        },
        onClick = onClick,
    )
}
