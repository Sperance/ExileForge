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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import kotlinx.coroutines.delay

/** Форма значка-кнопки. */
enum class IconButtonShape {
    /** Скруглённый квадрат - в панелях, шапках и строках. */
    Rounded,

    /** Круг - плавающие поверх карты, древа и боя (меню забега, масштаб, «к герою»). */
    Circle,
}

/** Сторона зоны касания значка-кнопки любого размера. */
private val TouchSide = 44.dp

/** Автоповтор удержания: первый повтор через [REPEAT_FIRST_MS], шаг с [REPEAT_START_MS] сжимается в [REPEAT_PACE] раза до [REPEAT_FAST_MS]. */
private const val REPEAT_FIRST_MS = 400L
private const val REPEAT_START_MS = 180L
private const val REPEAT_FAST_MS = 40L
private const val REPEAT_PACE = .8

/** Сторона видимой кнопки и значка по размеру: [ButtonSize.Auto] - как широкая. */
private val ButtonSize.side: Dp get() = if (this == ButtonSize.Auto) ButtonSize.Wide.height else height
private val ButtonSize.glyph: Dp get() = when (this) {
    ButtonSize.Auto, ButtonSize.Wide -> 20.dp
    ButtonSize.Compact -> 18.dp
    ButtonSize.Small -> 16.dp
}

/**
 * Значок-кнопка игры: значок [icon] в скруглённом квадрате или круге ([shape]) размера [size] (40/32/30 dp) в зоне касания 44.
 * [contentDescription] - обязательное имя для чтения с экрана. Оси - как у [ForgeButton]: [kind] ([ButtonKind.Secondary] -
 * тональная, [ButtonKind.Text] - без фона), [state], [accent] (цвет значка и окантовки - открытое, мир экрана). [badge] - счётчик
 * в углу цветом [badgeColor] (ноль не показывается), для чтения с экрана - в описании состояния. [repeatOnHold] - автоповтор:
 * удержание повторяет [onClick] всё чаще, пока кнопка доступна; отпускание после повторов лишнего шага не делает.
 */
@Composable fun ForgeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Text,
    size: ButtonSize = ButtonSize.Auto,
    state: ButtonState = ButtonState.Enabled,
    accent: Color? = null,
    shape: IconButtonShape = IconButtonShape.Rounded,
    badge: Int? = null,
    badgeColor: Color = ButtonInk.Badge,
    repeatOnHold: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val action by rememberUpdatedState(onClick)
    var repeated by remember { mutableStateOf(false) }
    if (repeatOnHold && state == ButtonState.Enabled && pressed.value) {
        LaunchedEffect(Unit) {
            repeated = false
            delay(REPEAT_FIRST_MS)
            var pause = REPEAT_START_MS
            while (true) {
                repeated = true
                action()
                delay(pause)
                pause = (pause * REPEAT_PACE).toLong().coerceAtLeast(REPEAT_FAST_MS)
            }
        }
    }
    val disabled = state is ButtonState.Disabled
    val look = remember(kind, disabled, accent) {
        if (disabled) offLook(kind) else liveLook(kind, accent ?: Parchment.takeIf { kind == ButtonKind.Text }, secondaryInk = Parchment)
    }
    val shown = badge?.takeIf { it > 0 }
    val count = shown?.let { ui("button.count", it) }
    val busyText = ui("button.busy")
    Box(
        modifier.size(TouchSide)
            .clickable(source, indication = null, enabled = state == ButtonState.Enabled || state == ButtonState.Busy, role = Role.Button) {
                if (state != ButtonState.Enabled) return@clickable
                if (repeated) repeated = false else onClick()
            }
            .semantics {
                when {
                    state == ButtonState.Busy -> stateDescription = busyText
                    state is ButtonState.Done -> stateDescription = state.label
                    count != null -> stateDescription = count
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size.side).pressShift(pressed).forgeSurface(look, pressed, circle = shape == IconButtonShape.Circle),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                ButtonState.Busy -> CircularProgressIndicator(Modifier.size(size.glyph - 2.dp), color = look.ink, strokeWidth = 2.dp)
                is ButtonState.Done -> Icon(Icons.Outlined.Check, contentDescription, Modifier.size(size.glyph), tint = look.ink)
                else -> Icon(icon, contentDescription, Modifier.size(size.glyph), tint = look.ink)
            }
        }
        shown?.let { CornerCount(it, badgeColor, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-3).dp)) }
    }
}

/** Счётчик в углу значка-кнопки: таблетка цвета [fill] с числом, «99+» сверх; слова - по яркости заливки; читает его состояние кнопки. */
@Composable private fun CornerCount(count: Int, fill: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(9.dp)
    Text(
        if (count > 99) "99+" else count.toString(),
        color = inkOn(fill),
        fontSize = 11.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier.clearAndSetSemantics {}.defaultMinSize(minWidth = 18.dp).height(18.dp)
            .border(2.dp, Panel, shape).background(fill, shape).padding(horizontal = 5.dp),
    )
}
