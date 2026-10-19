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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Blood
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnDanger
import com.sperance.exileforge.ui.theme.OnPrimary
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import kotlin.math.sqrt

/**
 * Окраска поверхности кнопки, иконки-кнопки и чипа: окантовка [rim] в 1 dp, заливка [fill] внутри неё и цвет слов [ink];
 * нажатая - [pressedRim] и [pressedFill]. [ledge] - кромка тени снизу и блик сверху (только у главной), [hatched] - погашенная:
 * косая штриховка вместо заливки.
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
)

/** Цвета кнопок, которых нет в теме: оттенки окантовок, нажатых заливок и шкал удержания. */
internal object ButtonInk {
    val PrimaryLedge = Color(0xFF23653F)
    val SecondaryRim = Color(0xFF5E6E79)
    val SecondaryPressed = Color(0xFF1B2A33)
    val DangerRim = Color(0xFF9A3533)
    val OffRim = Color(0xFF2B3944)
    val HatchLine = Color(0xFF18222A)
    val TextPressed = Gold.copy(alpha = .16f)
    val GhostPressed = Parchment.copy(alpha = .12f)
    val Badge = Color(0xFFB23A35)

    val HoldGreenRim = Color(0xFF2C7553)
    val HoldGreenFill = Color(0xFF182F2A)
    val HoldGreenBar = Color(0xFF2B7352)
    val HoldRedRim = Color(0xFF8A3A36)
    val HoldRedFill = Color(0xFF2E1E21)
    val HoldRedBar = Color(0xFF7E3431)
    val FateRim = Color(0xFFF0C76A)
    val FateInk = Color(0xFF1B1306)
    val FateFill = Brush.horizontalGradient(0f to Color(0xFFA88236), .35f to Color(0xFFC99A3C), .65f to Color(0xFFF0C76A), 1f to Color(0xFFC99A3C))
    val FateBar = Color.White.copy(alpha = .35f)
    val Ticks = GoldBright.copy(alpha = .4f)
    val FateTicks = FateInk.copy(alpha = .45f)

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

/** Скругление по высоте: 44 - 10 dp, 36 - 9, чип 32 - 8, малая 30 - 7; выросшая по содержимому кнопка остаётся при 10. */
internal fun cornerFor(height: Dp): Dp = when {
    height >= 43.5.dp -> 10.dp
    height >= 35.5.dp -> 9.dp
    height >= 31.5.dp -> 8.dp
    else -> 7.dp
}

/** Нажатое опускается на 1 dp; читается в фазе слоя, без перекомпоновки. */
internal fun Modifier.pressShift(pressed: State<Boolean>): Modifier = graphicsLayer { translationY = if (pressed.value) 1.dp.toPx() else 0f }

/**
 * Поверхность [look] скруглённым прямоугольником по высоте ([cornerFor]); [pressed] читается только в рисовании. [extra] рисуется
 * поверх заливки внутри окантовки (шкала удержания).
 */
internal fun Modifier.forgeSurface(look: SurfaceLook, pressed: State<Boolean>, extra: (DrawScope.(inner: Size) -> Unit)? = null): Modifier = drawWithCache {
    val border = 1.dp.toPx()
    val radius = cornerFor(size.height.toDp()).toPx()
    val inner = CornerRadius((radius - border).coerceAtLeast(0f))
    val innerSize = Size(size.width - 2 * border, size.height - 2 * border)
    val clip = Path().apply { addRoundRect(RoundRect(border, border, size.width - border, size.height - border, inner)) }
    val hatch = if (look.hatched) hatchLines(size, 5.dp.toPx()) else emptyList()
    val stripe = 5.dp.toPx()
    val ledge = 2.dp.toPx()
    onDrawBehind {
        val down = pressed.value
        if (look.ledge != null && !down) drawRoundRect(look.ledge, Offset(0f, ledge), size, CornerRadius(radius))
        val rim = if (down) look.pressedRim else look.rim
        if (rim.alpha > 0f) drawRoundRect(rim, cornerRadius = CornerRadius(radius))
        val fill = if (down) look.pressedFill else look.fill
        if (look.hatched) {
            clipPath(clip) {
                drawRect(Panel)
                hatch.forEach { (from, to) -> drawLine(ButtonInk.HatchLine, from, to, stripe) }
            }
        } else if (fill != null) {
            drawRoundRect(fill, Offset(border, border), innerSize, inner)
        }
        extra?.let { clipPath(clip) { it(innerSize) } }
        if (look.ledge != null && !down) drawLine(Color.White.copy(alpha = .35f), Offset(radius, border * 1.5f), Offset(size.width - radius, border * 1.5f), border)
    }
}

/** Косые полосы штриховки «/» шагом 10 dp поперёк: концы отрезков, что перекрывают всю поверхность [size]. */
private fun hatchLines(size: Size, stripe: Float): List<Pair<Offset, Offset>> {
    val step = 2 * stripe * sqrt(2f)
    val lines = ArrayList<Pair<Offset, Offset>>()
    var x = -size.height
    while (x < size.width + step) {
        lines += Offset(x, size.height) to Offset(x + size.height, 0f)
        x += step
    }
    return lines
}
