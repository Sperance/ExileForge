package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.StackLook
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.StackValue
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.theme.Boon
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune

/**
 * Ячейка стопки - одна на все места, где лежат стопки (сумка, кузница, ремёсла, рынок, хранилище гильдии, добыча): значок «Витраж»
 * 36dp без рамки, под ним круглый ореол цвета ценности ([StackValue]), число справа снизу с тенью («1» не пишется), тир сундука и
 * эссенции - арабской цифрой на малой табличке в левом верхнем углу ([StackLook.tier]); [selected] - золотая обводка. [size] - сторона
 * ячейки, значок - её 0.6; ячейка мельче [ROOMY] (значок строки списка) - без числа и таблички, их пишет строка; [badge] - знак места поверх (знамение, что нужно сфере); касание - [onClick] (null - ячейка не нажимается).
 */
@Composable fun StackCell(
    index: ContentIndex?,
    code: String,
    amount: Long,
    modifier: Modifier = Modifier,
    size: Dp = StackCellSize,
    selected: Boolean = false,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val look = remember(index, code) { StackLook.of(index, code) }
    val kind = remember(index, code) { index?.item(ItemCode(code))?.let(::bagVisualKind) }
    val title = itemTitle(code)
    val halo = valueColor(look.value)
    val shape = RoundedCornerShape(10.dp)
    val tap = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
    Box(
        modifier.size(size).background(CellGround, shape)
            .then(if (selected) Modifier.border(2.dp, GoldBright, shape) else Modifier)
            .drawBehind {
                val inset = HALO_INSET.toPx()
                val radius = (this.size.minDimension / 2 - inset).coerceAtLeast(0f)
                drawCircle(Brush.radialGradient(listOf(halo.copy(alpha = HALO_ALPHA), Color.Transparent), center, radius * HALO_SPREAD), radius, center)
            }
            .then(tap)
            .semantics { contentDescription = if (amount > 1) ui("bag.cell_label", title, amount) else title },
        contentAlignment = Alignment.Center,
    ) {
        BagIcon(code, Modifier.size(size * ICON_SHARE), tint = if (kind != null) Gold else Muted, kind = kind ?: ItemVisualKind.ITEM)
        val roomy = size >= ROOMY
        if (roomy) look.tier?.let { TierPlate(it, Modifier.align(Alignment.TopStart).padding(4.dp)) }
        if (roomy && amount > 1) {
            Text(
                stackCount(amount),
                color = GoldBright,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall.copy(shadow = CountShadow),
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 4.dp),
            )
        }
        badge?.invoke(this)
    }
}

/** Тир стопки арабской цифрой на малой табличке: тёмная цифра на латуни. */
@Composable private fun TierPlate(tier: Int, modifier: Modifier) {
    Text(
        tier.toString(),
        color = PlateInk,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier.background(Boon, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp),
    )
}

/** Число стопки, что влезает в угол ячейки: 12 345 - «12k», миллион и больше - «1.2M». */
fun stackCount(amount: Long): String = when {
    amount >= 1_000_000 -> String.format(java.util.Locale.ROOT, "%.1fM", amount / 1_000_000.0).replace(".0M", "M")
    amount >= 10_000 -> "${amount / 1_000}k"
    else -> amount.toString()
}

/** Цвет ореола ценности стопки. */
private fun valueColor(value: StackValue): Color = when (value) {
    StackValue.COMMON -> Muted
    StackValue.POPULAR -> Rune
    StackValue.VALUABLE -> Boon
    StackValue.RAREST -> Rarest
}

/** Сторона ячейки стопки в сетке сумки. */
val StackCellSize = 60.dp

/** Сторона ячейки стопки в добыче - во всех местах выдачи ([LootPanel]). */
val LootCellSize = 52.dp

/** С какой стороны ячейка пишет число и табличку тира: мельче - значок строки списка. */
private val ROOMY = 40.dp

/** Доля значка в ячейке: 36dp у ячейки 60dp. */
private const val ICON_SHARE = .6f

/** Отступ круга ореола от края ячейки. */
private val HALO_INSET = 4.dp

/** Яркость центра ореола. */
private const val HALO_ALPHA = .55f

/** Ореол гаснет к 68% радиуса круга. */
private const val HALO_SPREAD = .68f

/** Подложка ячейки: едва заметная, без рамки. */
private val CellGround = Color.White.copy(alpha = .03f)

/** Ореол редчайшей стопки - жар уникальной. */
private val Rarest = Color(0xFFF0965A)

/** Цифра на табличке тира. */
private val PlateInk = Color(0xFF15171A)

/** Тень числа стопки: читается поверх любого значка. */
private val CountShadow = Shadow(Color.Black, Offset(0f, 1f), 3f)
