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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnDanger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Вид кнопки, значка-кнопки - что действие значит для игрока. */
enum class ButtonKind {
    /** Главное действие места: плотная заливка (зелёная или цвета места). Одно на экран, лист или строку. */
    Primary,

    /** Второй выбор: тёмная заливка в светлой окантовке («Отмена», «Ответить»); у значка - тональный квадрат. */
    Secondary,

    /** Слово-действие без фона; погашенное - зачёркнуто. У значка - значок без фона. */
    Text,

    /** Опасное или необратимое: тёмно-красная заливка и всегда значок (без своего - корзина). */
    Danger,
}

/**
 * Размер кнопки: высота [height], поля по бокам [padding], значок [icon] и его отступ [gap]; скругление - по высоте. Зона касания
 * любого размера - не меньше 44 dp.
 */
enum class ButtonSize(val height: Dp, internal val padding: Dp, internal val icon: Dp, internal val gap: Dp) {
    /**
     * По месту: родитель задал ширину (`fillMaxWidth`, `weight`) - как [Wide], иначе - как [Compact]. У значка-кнопки - как [Wide].
     */
    Auto(Dp.Unspecified, 16.dp, 18.dp, 8.dp),

    /** Широкая 44 dp: подвал листа, кнопка во всю ширину. */
    Wide(44.dp, 20.dp, 18.dp, 8.dp),

    /** Компактная 36 dp: в строке, рядом с текстом. */
    Compact(36.dp, 16.dp, 18.dp, 8.dp),

    /** Малая 30 dp: плотные строки (карточка игрока, модерация). */
    Small(30.dp, 10.dp, 15.dp, 6.dp),
}

/** Состояние кнопки, значка-кнопки и чипа. */
@Immutable
sealed interface ButtonState {
    /** Касание действует. */
    data object Enabled : ButtonState

    /** Погашена: штриховка и приглушённые слова; касание зовёт лишь `onDisabledClick`, если он есть. */
    data object Disabled : ButtonState

    /** Занято: действие уже идёт - спиннер на месте значка, ширина та же, касание не срабатывает, вид не гаснет. */
    data object Busy : ButtonState

    /** Выполнено: галочка и подпись результата [label] («Удалено», «Дар принят»), касания нет. */
    data class Done(val label: String) : ButtonState

    companion object {
        /** Состояние из флагов места: [busy] - занято, иначе живая при [enabled] или погашенная. */
        fun of(enabled: Boolean, busy: Boolean = false): ButtonState = when {
            busy -> Busy
            enabled -> Enabled
            else -> Disabled
        }
    }
}

/**
 * Тон шкалы удержания, если он не следует из вида и акцента кнопки: окантовка, фон, слова, шкала и засечки; [ribbon] - лента:
 * плотная подпись без значка руки. Новый тон - новое значение рядом с [Fate], кнопка о нём не знает.
 */
@Immutable
class HoldTone internal constructor(internal val surface: SurfaceLook, internal val bar: Color, internal val ticks: Color, internal val ribbon: Boolean = false) {
    companion object {
        /** Золото Предначертания - только выбор дара. */
        val Fate = HoldTone(
            SurfaceLook(FateInk.Gold, Brush.horizontalGradient(0f to FateInk.GoldDeep, .35f to FateInk.Brass, .65f to FateInk.Gold, 1f to FateInk.Brass), FateInk.OnGold),
            Color.White.copy(alpha = .35f),
            FateInk.OnGold.copy(alpha = .45f),
            ribbon = true,
        )
    }
}

/**
 * Режим удержания: действие срабатывает, только когда палец продержал кнопку [millis]; шкала с засечками по четвертям наполняется
 * слева и пустеет, если отпустить раньше, конец - отклик вибрацией, после срабатывания кнопка снова пуста. Итог показывает
 * место: [ButtonState.Busy] на время запроса, [ButtonState.Done] с подписью результата. [tone] - свой тон шкалы ([HoldTone.Fate]);
 * без него - зелёный у главной, красный у опасной, тона акцента у кнопки с `accent`.
 */
@Immutable
data class Hold(val millis: Int = CONFIRM_MS, val tone: HoldTone? = null) {
    companion object {
        /** Подтверждение листом и действия кузницы. */
        const val CONFIRM_MS = 700

        /** Возврат узла древа. */
        const val REFUND_MS = 1_000

        /** Сброс всего древа - героя или гильдии. */
        const val RESET_MS = 1_200

        /** Выбор дара Предначертания. */
        const val FATE_MS = 1_200
    }
}

/** Наименьшая ширина кнопки: короткая подпись («ОК») не сжимает её в квадрат. */
private val MinWidth = 58.dp

/** Пауза полной шкалы после срабатывания удержания, до того как она опустеет. */
private const val FULL_PAUSE_MS = 300L

/** Живая ли кнопка, в которой рисуется содержимое: сумма [ButtonFigure] гаснет вместе с ней. */
private val LocalButtonLive = staticCompositionLocalOf { true }

/**
 * Кнопка игры - единственная: экраны зовут её, а не кнопки Material (сторож сборки `:app:forgeButtonsOnly`).
 *
 * [kind] - вид ([ButtonKind]), [size] - размер ([ButtonSize]), [state] - состояние ([ButtonState]). Форма - скруглённый прямоугольник
 * (10/9/7 dp по высоте), зона касания - не меньше 44 dp. [icon] - значок слева от подписи. [accent] - цвет места из темы (Бездна,
 * алтарь Ваал, атлас, сущности кузницы): у главной - заливка и окантовка, у вторичной и текстовой - окантовка и слова, у удержания -
 * окантовка, фон и шкала; слова на заливке система подбирает по яркости, опасную акцент не красит. [hold] - режим удержания ([Hold]):
 * [onClick] срабатывает по полному удержанию. [onDisabledClick] - касание погашенной кнопки (например, «сначала выбери карту»).
 *
 * Подпись - [content]: цвет и стиль слов задаёт кнопка ([LocalContentColor] и стиль подписи); свой цвет у `Text` не ставится -
 * погашенная кнопка его не приглушила бы, цвет места - [accent].
 */
@Composable fun ForgeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    size: ButtonSize = ButtonSize.Auto,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageVector? = null,
    accent: Color? = null,
    hold: Hold? = null,
    onDisabledClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val lead = icon ?: Icons.Outlined.DeleteOutline.takeIf { kind == ButtonKind.Danger }
    if (hold != null) {
        HoldFace(onClick, modifier, kind, size, state, lead, accent, hold, onDisabledClick, contentPadding, content)
        return
    }
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val look = remember(kind, state is ButtonState.Disabled, accent) { if (state is ButtonState.Disabled) offLook(kind) else liveLook(kind, accent) }
    val busyText = ui("button.busy")
    ButtonRow(
        modifier
            .clickable(
                source,
                indication = null,
                enabled = state is ButtonState.Enabled || state is ButtonState.Busy || (state is ButtonState.Disabled && onDisabledClick != null),
                role = Role.Button,
            ) {
                when (state) {
                    ButtonState.Enabled -> onClick()
                    ButtonState.Disabled -> onDisabledClick?.invoke()
                    else -> Unit
                }
            }
            .semantics {
                when (state) {
                    ButtonState.Disabled -> disabled()
                    ButtonState.Busy -> stateDescription = busyText
                    is ButtonState.Done -> stateDescription = state.label
                    ButtonState.Enabled -> Unit
                }
            }
            .touchZone()
            .forgeHeight(size)
            .pressShift(pressed)
            .forgeSurface(look, pressed),
        size,
        contentPadding,
        look.ink,
        labelStyle(size, strike = kind == ButtonKind.Text && state is ButtonState.Disabled),
        live = state !is ButtonState.Disabled,
    ) {
        Lead(state, lead, size)
        if (state is ButtonState.Done) Text(state.label, maxLines = 1) else content()
    }
}

/** Плашка суммы на кнопке: золотая; на погашенной кнопке - приглушена. */
private val FigureBrush = Brush.verticalGradient(listOf(GoldBright, Gold))

/** Сумма на кнопке: число в золотой плашке справа от подписи («+1 250» продажи, цена действия карточки). */
@Composable fun ButtonFigure(text: String) {
    val live = LocalButtonLive.current
    Text(
        text,
        color = if (live) Ink else Muted,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        modifier = Modifier.padding(start = 8.dp)
            .then(if (live) Modifier.background(FigureBrush, RoundedCornerShape(7.dp)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Стиль подписи: малая - мельче; погашенная текстовая - зачёркнута; [strong] - плотнее (золото Предначертания). */
@Composable private fun labelStyle(size: ButtonSize, strike: Boolean = false, strong: Boolean = false): TextStyle {
    val base = if (size == ButtonSize.Small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    return when {
        strong -> base.copy(fontWeight = FontWeight.SemiBold, letterSpacing = .6.sp)
        strike -> base.copy(textDecoration = TextDecoration.LineThrough)
        else -> base
    }
}

/** Высота по размеру; [ButtonSize.Auto] - задана ширина - 44 dp, иначе 36 dp. Содержимое выше - кнопка растёт. */
private fun Modifier.forgeHeight(size: ButtonSize): Modifier = layout { measurable, constraints ->
    val height = when {
        size != ButtonSize.Auto -> size.height
        constraints.hasFixedWidth -> ButtonSize.Wide.height
        else -> ButtonSize.Compact.height
    }
    val minHeight = height.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    val minWidth = MinWidth.roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
    val placeable = measurable.measure(constraints.copy(minHeight = minHeight, minWidth = minWidth))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Строка кнопки: подпись по центру, цвет и стиль слов - кнопки. */
@Composable private fun ButtonRow(
    modifier: Modifier,
    size: ButtonSize,
    contentPadding: PaddingValues?,
    ink: Color,
    style: TextStyle,
    live: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.padding(contentPadding ?: PaddingValues(horizontal = size.padding)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides ink, LocalButtonLive provides live) {
            ProvideTextStyle(style) { content() }
        }
    }
}

/** Значок слева от подписи: спиннер у занятой, галочка у выполненной, иначе [icon]. */
@Composable private fun Lead(state: ButtonState, icon: ImageVector?, size: ButtonSize) {
    when {
        state is ButtonState.Busy -> CircularProgressIndicator(Modifier.size(size.icon - 2.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        state is ButtonState.Done -> Icon(Icons.Outlined.Check, null, Modifier.size(size.icon))
        icon != null -> Icon(icon, null, Modifier.size(size.icon))
        else -> return
    }
    Spacer(Modifier.width(size.gap))
}

private val GreenHold = HoldTone(SurfaceLook(ButtonInk.HoldGreenRim, SolidColor(ButtonInk.HoldGreenFill), GoldBright), ButtonInk.HoldGreenBar, ButtonInk.Ticks)
private val RedHold = HoldTone(SurfaceLook(ButtonInk.HoldRedRim, SolidColor(ButtonInk.HoldRedFill), OnDanger), ButtonInk.HoldRedBar, ButtonInk.Ticks)
private val OffHold = HoldTone(OffLook, Color.Transparent, Color.Transparent)

/** Тон удержания цветом места [accent]: тёмный фон и шкала его тонами, окантовка ярче. */
private fun accentHold(accent: Color) = HoldTone(SurfaceLook(accent.onPanel(.6f), SolidColor(accent.onPanel(.15f)), GoldBright), accent.onPanel(.45f), ButtonInk.Ticks)

/** Тон удержания кнопки: погашенная - штриховка, иначе свой тон [Hold.tone], опасный, акцента или зелёный. */
private fun holdTone(kind: ButtonKind, accent: Color?, hold: Hold, disabled: Boolean): HoldTone = when {
    disabled -> OffHold
    hold.tone != null -> hold.tone
    kind == ButtonKind.Danger -> RedHold
    accent != null -> accentHold(accent)
    else -> GreenHold
}

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
    size: ButtonSize,
    state: ButtonState,
    icon: ImageVector?,
    accent: Color?,
    hold: Hold,
    onDisabledClick: (() -> Unit)?,
    contentPadding: PaddingValues?,
    content: @Composable RowScope.() -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val holding = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val held by rememberUpdatedState(onHeld)
    val refused by rememberUpdatedState(onDisabledClick)
    val millis by rememberUpdatedState(hold.millis)
    val current by rememberUpdatedState(state)
    val fire = {
        if (current == ButtonState.Enabled) {
            held()
            scope.launch {
                delay(FULL_PAUSE_MS)
                progress.animateTo(0f, tween(180))
            }
        }
    }
    LaunchedEffect(state) { if (state !is ButtonState.Done && progress.value >= 1f) progress.snapTo(0f) }
    val quarter by remember { derivedStateOf { (progress.value * 4).toInt() } }
    val tone = remember(kind, accent, hold, state is ButtonState.Disabled) { holdTone(kind, accent, hold, state is ButtonState.Disabled) }
    val bar = remember(tone, progress) { holdBar(progress, tone.bar, tone.ticks) }
    val done = state as? ButtonState.Done
    val busyText = ui("button.busy")
    val holdText = ui("button.hold")
    val progressText = ui("button.hold_progress", quarter * 25)
    ButtonRow(
        modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                when {
                    state == ButtonState.Disabled -> disabled()
                    state == ButtonState.Busy -> stateDescription = busyText
                    done != null -> stateDescription = done.label
                    quarter in 1..3 -> stateDescription = progressText
                }
                if (state == ButtonState.Enabled) {
                    onClick(holdText) {
                        fire()
                        true
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    if (current != ButtonState.Enabled) {
                        if (current == ButtonState.Disabled) refused?.invoke()
                        waitForUpOrCancellation()?.consume()
                        return@awaitEachGesture
                    }
                    holding.value = true
                    val filling = scope.launch {
                        val left = ((1f - progress.value) * millis).toInt().coerceAtLeast(1)
                        progress.animateTo(1f, tween(left, easing = LinearEasing))
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        fire()
                    }
                    waitForUpOrCancellation()?.consume()
                    holding.value = false
                    if (progress.value < 1f) {
                        filling.cancel()
                        scope.launch { progress.animateTo(0f, tween(180)) }
                    }
                }
            }
            .touchZone()
            .forgeHeight(size)
            .pressShift(holding)
            .forgeSurface(tone.surface, holding, extra = if (done != null) fullBar(tone.bar) else bar),
        size,
        contentPadding,
        tone.surface.ink,
        labelStyle(size, strong = tone.ribbon),
        live = state != ButtonState.Disabled,
    ) {
        Lead(state, if (tone.ribbon) icon else icon ?: Icons.Outlined.TouchApp, size)
        if (done != null) Text(done.label, maxLines = 1) else content()
    }
}

/** Полная шкала выполненного удержания. */
private fun fullBar(bar: Color): DrawScope.(Size) -> Unit = { inner -> drawRect(bar, Offset(1.dp.toPx(), 1.dp.toPx()), inner) }
