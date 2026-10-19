package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnPrimary
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment

/** Вид иконки-кнопки. */
enum class IconButtonStyle {
    /** Тёмная заливка в окантовке - как вторичная кнопка: «−»/«+», поиск у поля. */
    Tonal,

    /** Без фона: закрыть, «⋮», назад, шестерёнка шапки; на нажатие - лёгкая подложка. */
    Ghost,

    /** Главное действие значком: зелёная заливка. */
    Primary,

    /** Опасное значком: тёмно-красная заливка. */
    Danger,
}

/** Сторона зоны касания иконки-кнопки: и обычной, и компактной. */
private val TouchSide = 44.dp

/** Сторона компактной иконки-кнопки внутри зоны 44 dp. */
private val CompactSide = 36.dp

/**
 * Иконка-кнопка игры: значок [icon] (подпись для чтения с экрана - [contentDescription]) в скруглённом квадрате 44 dp, [compact] -
 * 36 dp в той же зоне касания 44. [style] - вид ([IconButtonStyle]); [tint] - свой цвет значка поверх цвета вида (только там, где
 * цвет несёт смысл: открытое, мир экрана). [badge] - счётчик в углу, ноль не показывается. Погашенная - штриховка (у [IconButtonStyle.Ghost] -
 * приглушённый значок).
 */
@Composable fun ForgeIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Ghost,
    compact: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    badge: Int? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val look = remember(style, enabled) { style.look(enabled) }
    Box(
        modifier.size(TouchSide).clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(if (compact) CompactSide else TouchSide).pressShift(pressed).forgeSurface(look, pressed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, Modifier.size(20.dp), tint = if (enabled && tint != Color.Unspecified) tint else look.ink)
        }
        badge?.takeIf { it > 0 }?.let { CornerCount(it, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-3).dp)) }
    }
}

/** Окраска вида: погашенная - штриховка, у кнопки без фона - лишь приглушённый значок. */
private fun IconButtonStyle.look(enabled: Boolean): SurfaceLook = when {
    !enabled -> if (this == IconButtonStyle.Ghost) bareLook(Muted, Color.Transparent) else OffLook
    this == IconButtonStyle.Tonal -> SurfaceLook(SecondaryLook.rim, SecondaryLook.fill, Parchment, SecondaryLook.pressedRim, SecondaryLook.pressedFill)
    this == IconButtonStyle.Ghost -> bareLook(Parchment, ButtonInk.GhostPressed)
    this == IconButtonStyle.Primary -> solidLook(PrimaryLook.rim, PrimaryLook.rim, OnPrimary)
    else -> DangerLook
}

/** Счётчик в углу иконки-кнопки: красная таблетка с числом, «99+» сверх. */
@Composable private fun CornerCount(count: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(9.dp)
    Text(
        if (count > 99) "99+" else count.toString(),
        color = Color.White,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier.defaultMinSize(minWidth = 18.dp).height(18.dp)
            .border(2.dp, Panel, shape).background(ButtonInk.Badge, shape).padding(horizontal = 5.dp),
    )
}
