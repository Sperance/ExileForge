package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnDanger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Вид кнопки - что действие значит для игрока. */
enum class ButtonKind {
    /** Главное действие места: плотная зелёная заливка. Одно на экран, лист или строку. */
    Primary,

    /** Второй выбор: тёмная заливка в светлой окантовке («Отмена», «Ответить»). */
    Secondary,

    /** Слово-действие без фона, зелёным; погашенное - зачёркнуто. */
    Text,

    /** Опасное или необратимое: тёмно-красная заливка и всегда значок (корзина, крестик). */
    Danger,
}

/** Размер кнопки: высота [height], поля по бокам [padding], значок [icon] и его отступ [gap]; скругление - по высоте. */
enum class ButtonSize(val height: Dp, internal val padding: Dp, internal val icon: Dp, internal val gap: Dp) {
    /** Широкая 44 dp: подвал листа, кнопка во всю ширину. */
    Wide(44.dp, 20.dp, 18.dp, 8.dp),

    /** Компактная 36 dp: в строке, рядом с текстом. */
    Compact(36.dp, 16.dp, 18.dp, 8.dp),

    /** Малая 30 dp: плотные строки (карточка игрока, модерация); касание добирает до 44 dp сама разметка Compose. */
    Small(30.dp, 10.dp, 15.dp, 6.dp),
}

/**
 * Режим удержания: действие срабатывает, только когда палец продержал кнопку [millis]; шкала с засечками по четвертям
 * наполняется слева и пустеет, если отпустить раньше, конец - отклик вибрацией.
 *
 * [rearm] - кнопка остаётся (кузница: та же сфера снова и снова) и после срабатывания пустеет; без него срабатывает один раз
 * (лист уходит вместе с ней). [fate] - золото Предначертания, только для выбора дара. [done] - подпись результата на 100%
 * («Удалено», «Дар принят»). [onRefused] - касание погашенной кнопки (например, «сначала выбери карту»).
 */
@Immutable
data class Hold(
    val millis: Int = HOLD_TO_CONFIRM_MS,
    val rearm: Boolean = false,
    val fate: Boolean = false,
    val done: String? = null,
    val onRefused: (() -> Unit)? = null,
)

/** Поля по бокам кнопки без заданного размера - как у компактной. */
private val AutoPadding = PaddingValues(horizontal = ButtonSize.Compact.padding)

/** Наименьшая ширина кнопки: короткая подпись («ОК») не сжимает её в квадрат. */
private val MinWidth = 58.dp

/** Сколько видна подпись результата у кнопки удержания, что остаётся ([Hold.rearm]). */
private const val DONE_SHOW_MS = 600L

/**
 * Кнопка игры - единственная: экраны зовут её, а не кнопки Material (сторож сборки `:app:forgeButtonsOnly`).
 *
 * [kind] - вид ([ButtonKind]), [size] - размер ([ButtonSize]); без размера - по месту: ширину задал родитель - широкая 44 dp, иначе
 * компактная 36 dp. Форма - скруглённый прямоугольник (10/9/7 dp по высоте). [icon] - значок слева от подписи; [busy] - «занято»:
 * спиннер на месте значка, касание не срабатывает, вид не гаснет. Погашенная ([enabled] false) - штриховка и приглушённые слова.
 * [hold] - режим удержания ([Hold]): [onClick] срабатывает по полному удержанию; цвета шкалы - зелёный ([ButtonKind.Primary]),
 * опасный ([ButtonKind.Danger]) или золото Предначертания ([Hold.fate]).
 *
 * Подпись - [content]: цвет и стиль слов задаёт кнопка ([LocalContentColor], стиль подписи), свой цвет у `Text` их перебивает.
 */
@Composable fun ForgeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    size: ButtonSize? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    hold: Hold? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (hold != null) {
        HoldFace(onClick, modifier, kind, size, icon, enabled, hold, contentPadding, content)
        return
    }
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val look = remember(kind, enabled) { kind.look(enabled) }
    ButtonRow(
        modifier.forgeHeight(size).pressShift(pressed).forgeSurface(look, pressed)
            .clickable(source, indication = null, enabled = enabled && !busy, role = Role.Button, onClick = onClick),
        size,
        contentPadding,
        look.ink,
        labelStyle(size, strike = kind == ButtonKind.Text && !enabled),
    ) {
        Lead(if (busy) null else icon, busy, size)
        content()
    }
}

/**
 * Сумма на кнопке: число в золотой плашке справа от подписи («+1 250» продажи, цена действия карточки).
 */
@Composable fun ButtonFigure(text: String, enabled: Boolean = true) {
    Text(
        text,
        color = if (enabled) Ink else Muted,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        modifier = Modifier.padding(start = 8.dp)
            .background(Brush.verticalGradient(listOf(GoldBright, Gold)), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Окраска вида: погашенная - общая штриховка, у текстовой - без фона. */
private fun ButtonKind.look(enabled: Boolean): SurfaceLook = when (this) {
    ButtonKind.Primary -> if (enabled) PrimaryLook else OffLook
    ButtonKind.Secondary -> if (enabled) SecondaryLook else OffLook
    ButtonKind.Text -> if (enabled) bareLook(Gold, ButtonInk.TextPressed) else bareLook(Muted, Color.Transparent)
    ButtonKind.Danger -> if (enabled) DangerLook else OffLook
}

/** Стиль подписи: малая - мельче; погашенная текстовая - зачёркнута. */
@Composable private fun labelStyle(size: ButtonSize?, strike: Boolean = false, fate: Boolean = false): TextStyle {
    val base = if (size == ButtonSize.Small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    return when {
        fate -> base.copy(fontWeight = FontWeight.SemiBold, letterSpacing = .6.sp)
        strike -> base.copy(textDecoration = TextDecoration.LineThrough)
        else -> base
    }
}

/** Высота по размеру; без размера - по месту: задана ширина - 44 dp, иначе 36 dp. Содержимое выше - кнопка растёт. */
private fun Modifier.forgeHeight(size: ButtonSize?): Modifier = layout { measurable, constraints ->
    val wanted = (size?.height ?: if (constraints.hasFixedWidth) ButtonSize.Wide.height else ButtonSize.Compact.height).roundToPx()
    val minHeight = wanted.coerceIn(constraints.minHeight, constraints.maxHeight)
    val minWidth = MinWidth.roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
    val placeable = measurable.measure(constraints.copy(minHeight = minHeight, minWidth = minWidth))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Строка кнопки: подпись по центру, цвет и стиль слов - кнопки. */
@Composable private fun ButtonRow(
    modifier: Modifier,
    size: ButtonSize?,
    contentPadding: PaddingValues?,
    ink: Color,
    style: TextStyle,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.padding(contentPadding ?: size?.let { PaddingValues(horizontal = it.padding) } ?: AutoPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides ink) {
            ProvideTextStyle(style) { content() }
        }
    }
}

/** Значок слева от подписи или спиннер «занято» на его месте. */
@Composable private fun Lead(icon: ImageVector?, busy: Boolean, size: ButtonSize?) {
    val side = (size ?: ButtonSize.Compact).icon
    when {
        busy -> CircularProgressIndicator(Modifier.size(side - 2.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        icon != null -> Icon(icon, null, Modifier.size(side))
        else -> return
    }
    Spacer(Modifier.width((size ?: ButtonSize.Compact).gap))
}

/** Окраска удержания: поверхность, шкала и засечки; золото Предначертания - по [fate]. */
private class HoldLook(val surface: SurfaceLook, val bar: Color, val ticks: Color)

private val GreenHold = HoldLook(SurfaceLook(ButtonInk.HoldGreenRim, SolidColor(ButtonInk.HoldGreenFill), GoldBright), ButtonInk.HoldGreenBar, ButtonInk.Ticks)
private val RedHold = HoldLook(SurfaceLook(ButtonInk.HoldRedRim, SolidColor(ButtonInk.HoldRedFill), OnDanger), ButtonInk.HoldRedBar, ButtonInk.Ticks)
private val FateHold = HoldLook(SurfaceLook(ButtonInk.FateRim, ButtonInk.FateFill, ButtonInk.FateInk), ButtonInk.FateBar, ButtonInk.FateTicks)
private val OffHold = HoldLook(OffLook, Color.Transparent, Color.Transparent)

/** Шкала удержания: полоса [bar] на долю [progress] ширины и засечки по четвертям у нижнего края; читается в рисовании. */
private fun holdBar(progress: Animatable<Float, AnimationVector1D>, bar: Color, ticks: Color): DrawScope.(Size) -> Unit = { inner ->
    val border = 1.dp.toPx()
    val share = progress.value
    if (share > 0f) drawRect(bar, Offset(border, border), Size(inner.width * share, inner.height))
    if (ticks.alpha > 0f) {
        val inset = 10.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        val top = bottom - 4.dp.toPx()
        val span = size.width - 2 * inset
        for (quarter in 1..3) {
            val x = inset + span * quarter / 4f
            drawLine(ticks, Offset(x, top), Offset(x, bottom), border)
        }
    }
}

/** Кнопка в режиме удержания ([Hold]). */
@Composable private fun HoldFace(
    onHeld: () -> Unit,
    modifier: Modifier,
    kind: ButtonKind,
    size: ButtonSize?,
    icon: ImageVector?,
    enabled: Boolean,
    hold: Hold,
    contentPadding: PaddingValues?,
    content: @Composable RowScope.() -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val held by rememberUpdatedState(onHeld)
    val spec by rememberUpdatedState(hold)
    var fired by remember { mutableStateOf(false) }
    val live by rememberUpdatedState(enabled && !fired)
    val fire = {
        if (live) {
            if (!spec.rearm) fired = true
            held()
            if (spec.rearm) {
                scope.launch {
                    if (spec.done != null) delay(DONE_SHOW_MS)
                    progress.animateTo(0f, tween(180))
                }
            }
        }
    }
    val full by remember { derivedStateOf { progress.value >= 1f } }
    val look = when {
        !enabled && !fired -> OffHold
        hold.fate -> FateHold
        kind == ButtonKind.Danger -> RedHold
        else -> GreenHold
    }
    ButtonRow(
        modifier.forgeHeight(size)
            .forgeSurface(look.surface, Unpressed, holdBar(progress, look.bar, look.ticks))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!live) disabled()
                onClick {
                    fire()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (!live) {
                        spec.onRefused?.invoke()
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    val filling = scope.launch {
                        val left = ((1f - progress.value) * spec.millis).toInt().coerceAtLeast(1)
                        progress.animateTo(1f, tween(left, easing = LinearEasing))
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        fire()
                    }
                    waitForUpOrCancellation()
                    if (progress.value < 1f) {
                        filling.cancel()
                        scope.launch { progress.animateTo(0f, tween(180)) }
                    }
                }
            },
        size,
        contentPadding,
        look.surface.ink,
        labelStyle(size, fate = hold.fate),
    ) {
        val done = hold.done?.takeIf { full }
        val lead = when {
            full -> Icons.Outlined.Check
            hold.fate -> icon
            else -> icon ?: Icons.Outlined.TouchApp
        }
        Lead(lead, busy = false, size)
        if (done != null) Text(done, maxLines = 1) else content()
    }
}

/** Пустое «нажато» для поверхностей, что не опускаются (удержание). */
internal val Unpressed: State<Boolean> = mutableStateOf(false)
