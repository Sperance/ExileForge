package com.sperance.exileforge.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Blood
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnDanger
import com.sperance.exileforge.ui.theme.OnPrimary
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import kotlin.math.sqrt

/**
 * Окраска поверхности кнопки, иконки-кнопки и чипа: окантовка [rim] толщиной [rimWidth] (обычно 1 dp; рисуется внутри поверхности
 * и её размер не меняет), заливка [fill] внутри неё и цвет слов [ink]; нажатая - [pressedRim] и [pressedFill]. [ledge] - кромка тени
 * по нижнему краю и блик сверху (только у главной), [hatched] - погашенная: косая штриховка вместо заливки.
 */
@Immutable
internal class SurfaceLook(
    val rim: Color,
    val fill: Brush?,
    val ink: Color,
    val pressedRim: Color = rim,
    val pressedFill: Brush? = fill,
    val ledge: Color? = null,
    val hatched: Boolean = false,
    val rimWidth: Dp = 1.dp,
)

/** Цвета кнопок, которых нет в теме: оттенки окантовок, нажатых заливок и шкал удержания. */
internal object ButtonInk {
    val PrimaryLedge = Color(0xFF23653F)
    val SecondaryRim = Color(0xFF5E6E79)
    val SecondaryPressed = Color(0xFF1B2A33)
    val DangerRim = Color(0xFF9A3533)
    val OffRim = Color(0xFF2B3944)
    val HatchLine = Color(0xFF18222A)
    val GhostPressed = Parchment.copy(alpha = .12f)
    val Badge = Color(0xFFB23A35)

    val HoldGreenRim = Color(0xFF2C7553)
    val HoldGreenFill = Color(0xFF182F2A)
    val HoldGreenBar = Color(0xFF2B7352)
    val HoldRedRim = Color(0xFF8A3A36)
    val HoldRedFill = Color(0xFF2E1E21)
    val HoldRedBar = Color(0xFF7E3431)
    val Ticks = GoldBright.copy(alpha = .4f)

    val ChipOnFill = Color(0xFF173029)
    val CountFill = Color(0xFF24323C)
    val CountOffFill = Color(0xFF1C262D)
}

/** Нажатая заливка: та же краска на 18% темнее. */
private fun Color.pressed(): Color = copy(red = red * .82f, green = green * .82f, blue = blue * .82f)

/** Сплошная заливка с темнее нажатой - главная, опасная. */
internal fun solidLook(fill: Color, rim: Color, ink: Color, ledge: Color? = null) = SurfaceLook(rim, SolidColor(fill), ink, rim.pressed(), SolidColor(fill.pressed()), ledge)

/** Погашенное: штриховка и приглушённые слова - одинаково во всех видах с фоном. */
internal val OffLook = SurfaceLook(ButtonInk.OffRim, null, Muted, hatched = true)

/** Без фона: на нажатие - лёгкая подложка [pressed]. */
internal fun bareLook(ink: Color, pressed: Color) = SurfaceLook(Color.Transparent, null, ink, pressedFill = SolidColor(pressed))

internal val PrimaryLook = solidLook(Gold, Gold, OnPrimary, ButtonInk.PrimaryLedge)
internal val SecondaryLook = SurfaceLook(ButtonInk.SecondaryRim, SolidColor(PanelRaised), GoldBright, Gold, SolidColor(ButtonInk.SecondaryPressed))
internal val DangerLook = solidLook(Blood, ButtonInk.DangerRim, OnDanger)

/** Слова на заливке [fill]: на светлой - тёмные [Ink], на тёмной - светлые [GoldBright]. */
internal fun inkOn(fill: Color): Color = if (fill.luminance() > .3f) Ink else GoldBright

/** Тон акцента на тёмной панели: [share] - доля акцента в смеси с [Panel]. */
internal fun Color.onPanel(share: Float): Color = lerp(Panel, this, share)

/**
 * Окраска вида [kind] с цветом места [accent] (живая; погашенную даёт [OffLook]). Главная - заливка и окантовка, вторичная -
 * окантовка и слова [secondaryInk] без акцента, текстовая - слова и подложка нажатия; опасную акцент не красит.
 */
internal fun liveLook(kind: ButtonKind, accent: Color?, secondaryInk: Color = GoldBright): SurfaceLook = when (kind) {
    ButtonKind.Primary -> accent?.let { solidLook(it, it, inkOn(it), it.onPanel(.55f)) } ?: PrimaryLook

    ButtonKind.Secondary -> accent?.let { SurfaceLook(it.copy(alpha = .55f), SolidColor(PanelRaised), it, it, SolidColor(ButtonInk.SecondaryPressed)) }
        ?: SurfaceLook(SecondaryLook.rim, SecondaryLook.fill, secondaryInk, SecondaryLook.pressedRim, SecondaryLook.pressedFill)

    ButtonKind.Text -> (accent ?: Gold).let { bareLook(it, it.copy(alpha = .16f)) }

    ButtonKind.Danger -> DangerLook
}

/** Погашенная окраска вида: у текстовой - без фона, у прочих - штриховка. */
internal fun offLook(kind: ButtonKind): SurfaceLook = if (kind == ButtonKind.Text) bareLook(Muted, Color.Transparent) else OffLook

/** Скругление по высоте: широкая 40 - 10 dp, компактная и чип 32 - 8, малая 30 - 7; выросшая по содержимому кнопка остаётся при 10. */
internal fun cornerFor(height: Dp): Dp = when {
    height >= 39.5.dp -> 10.dp
    height >= 31.5.dp -> 8.dp
    else -> 7.dp
}

/** Нажатое опускается на 1 dp; читается в фазе слоя, без перекомпоновки. */
internal fun Modifier.pressShift(pressed: State<Boolean>): Modifier = graphicsLayer { translationY = if (pressed.value) 1.dp.toPx() else 0f }

/**
 * Поверхность [look] скруглённым прямоугольником по высоте ([cornerFor]) или кругом ([circle]); [pressed] читается только в
 * рисовании (null - поверхность не нажимается). [extra] рисуется поверх заливки внутри окантовки (шкала удержания). Всё рисуется в своих границах.
 */
internal fun Modifier.forgeSurface(
    look: SurfaceLook,
    pressed: State<Boolean>?,
    circle: Boolean = false,
    extra: (DrawScope.(inner: Size) -> Unit)? = null,
): Modifier = drawWithCache {
    val border = look.rimWidth.toPx()
    val radius = if (circle) size.minDimension / 2 else cornerFor(size.height.toDp()).toPx()
    val inner = CornerRadius((radius - border).coerceAtLeast(0f))
    val innerSize = Size(size.width - 2 * border, size.height - 2 * border)
    val clip = Path().apply { addRoundRect(RoundRect(border, border, size.width - border, size.height - border, inner)) }
    val stripe = 5.dp.toPx()
    val hatch = if (look.hatched) hatchLines(size, stripe) else FloatArray(0)
    val ledge = 2.dp.toPx()
    onDrawBehind {
        val down = pressed?.value == true
        val rim = if (down) look.pressedRim else look.rim
        if (rim.alpha > 0f) drawRoundRect(rim, cornerRadius = CornerRadius(radius))
        val fill = if (down) look.pressedFill else look.fill
        clipPath(clip) {
            if (look.hatched) {
                drawRect(Panel)
                var i = 0
                while (i < hatch.size) {
                    drawLine(ButtonInk.HatchLine, Offset(hatch[i], hatch[i + 1]), Offset(hatch[i + 2], hatch[i + 3]), stripe)
                    i += 4
                }
            } else if (fill != null) {
                drawRect(fill)
            }
            if (look.ledge != null && !down) drawRect(look.ledge, Offset(0f, size.height - border - ledge), Size(size.width, ledge))
            extra?.invoke(this, innerSize)
        }
        if (look.ledge != null && !down) drawLine(Color.White.copy(alpha = .35f), Offset(radius, border * 1.5f), Offset(size.width - radius, border * 1.5f), border)
    }
}

/** Наименьшая зона касания кнопки, значка и чипа. */
private val TouchZone = 44.dp

/**
 * Явная зона касания [zone] (обычно 44 dp): меньшее содержимое встаёт по центру, разметка отводит ему всю зону. Стоит в цепочке после
 * обработчика касания и до поверхности - касается вся зона, рисуется только содержимое.
 */
internal fun Modifier.touchZone(zone: Dp = TouchZone): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val side = zone.roundToPx()
    val width = maxOf(placeable.width, side).coerceAtMost(constraints.maxWidth)
    val height = maxOf(placeable.height, side).coerceAtMost(constraints.maxHeight)
    layout(width, height) { placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2) }
}

/** Косые полосы штриховки «/» шагом 10 dp поперёк: концы отрезков подряд (x0, y0, x1, y1), что перекрывают всю поверхность [size]. */
private fun hatchLines(size: Size, stripe: Float): FloatArray {
    val step = 2 * stripe * sqrt(2f)
    val count = ((size.width + size.height) / step).toInt() + 2
    return FloatArray(count * 4).also { lines ->
        for (i in 0 until count) {
            val x = -size.height + i * step
            lines[i * 4] = x
            lines[i * 4 + 1] = size.height
            lines[i * 4 + 2] = x + size.height
            lines[i * 4 + 3] = 0f
        }
    }
}
