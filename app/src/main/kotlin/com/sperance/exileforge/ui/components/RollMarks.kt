package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*
import kotlin.math.ceil

/**
 * The roll quality in steps of twenty (3.2.0): each step louder than the one below — from a quiet grey
 * with no frame to a gold that glows — and a perfect hundred is molten gold (4.7.0, макет «Идеальный ролл», вариант A:
 * радуга выбивалась из тёмного фэнтези).
 */
private enum class RollTier(val key: String, val tint: Color, val fill: Float, val edge: Float, val glow: Dp) {
    POOR("card.roll_poor", Muted, .06f, .25f, 0.dp),
    MODEST("card.roll_modest", Parchment, .12f, .4f, 0.dp),
    FAIR("card.roll_fair", Rune, .18f, .55f, 2.dp),
    GOOD("card.roll_good", Vital, .24f, .75f, 5.dp),
    SUPERB("card.roll_superb", GoldBright, .3f, .95f, 9.dp),
    PERFECT("card.roll_perfect", GoldBright, .32f, 1f, 12.dp),
    ;

    companion object {
        fun of(quality: Int): RollTier = if (quality >= 100) PERFECT else entries[(quality / 20).coerceIn(0, SUPERB.ordinal)]
    }
}

/** Расплавленное золото идеального ролла (4.7.0): тёмная бронза, золото и белый жар посередине. */
private val Molten = listOf(Color(0xFF8A5A12), Color(0xFFF2D27A), Color(0xFFFFF1C2), Color(0xFFF2D27A), Color(0xFF8A5A12))

/**
 * Течение жара идеального ролла: доля круга [MOLTEN_MS] состоянием - читается в рисовании (4.9.0), так что ролл
 * перерисовывается шагом часов декора без перекомпоновки; без анимаций (`LocalMotion`) - стоит на середине.
 */
@Composable private fun moltenPhase(): State<Float> = motionPhase(MOLTEN_MS, still = MOLTEN_STILL)

/** Свет идеального ролла на доле течения [shift]: жар медленно течёт по золоту. */
private fun moltenBrush(shift: Float): Brush {
    val span = MOLTEN_SPAN
    return Brush.linearGradient(Molten, start = Offset(shift * span * 2 - span, 0f), end = Offset(shift * span * 2, span / 3), tileMode = TileMode.Mirror)
}

/**
 * Заливка [fill] (null - тем же жаром) и рамка [width] жаром идеального ролла по [shape] - как `background` + `border`,
 * но кисть жара собирается в рисовании по фазе [phase].
 */
private fun Modifier.moltenFill(phase: State<Float>, shape: Shape, width: Dp, fill: Brush?): Modifier = drawBehind {
    val brush = moltenBrush(phase.value)
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline, fill ?: brush)
    val stroke = ceil(width.toPx())
    if (outline is Outline.Rounded && outline.roundRect.isSimple && stroke * 2 <= size.minDimension) {
        // Рамка как у `border`: штрих внутри формы, углы уже на полштриха
        val half = stroke / 2
        val corner = outline.roundRect.topLeftCornerRadius
        drawRoundRect(
            brush,
            topLeft = Offset(half, half),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius((corner.x - half).coerceAtLeast(0f), (corner.y - half).coerceAtLeast(0f)),
            style = Stroke(stroke),
        )
    } else {
        drawOutline(outline, brush, style = Stroke(stroke))
    }
}

/** Сколько течёт жар по идеальному роллу, мс. */
private const val MOLTEN_MS = 3600

/** Доля течения, на которой жар стоит без анимаций. */
private const val MOLTEN_STILL = .5f

/** Длина полосы градиента жара, px. */
private const val MOLTEN_SPAN = 240f

/** Рамка идеального ролла. */
private val MOLTEN_EDGE = 1.5.dp

/** The frame, fill and glow a roll quality wears on [shape]; a perfect one is [solid]ly molten on a pill, framed on a card. */
@Composable private fun Modifier.rollShine(tier: RollTier, shape: Shape, solid: Boolean = false): Modifier {
    val base = glow(tier.tint, on = tier.glow > 0.dp, radius = tier.glow, shape = shape)
    return if (tier == RollTier.PERFECT) {
        base.moltenFill(moltenPhase(), shape, MOLTEN_EDGE, fill = if (solid) null else Brush.horizontalGradient(listOf(PanelRaised, Abyss)))
    } else {
        base.background(Brush.horizontalGradient(listOf(tier.tint.copy(alpha = tier.fill), tier.tint.copy(alpha = tier.fill / 3))), shape)
            .border(1.dp, tier.tint.copy(alpha = tier.edge), shape)
    }
}

/**
 * How well the item rolled (2.72.0; a ring in the card's head since this version): a gauge filling its
 * ring to the share with the figure inside, in its step's colour; a tap says «Модификаторы предмета · Хорошие · 72%».
 */
@Composable fun RollRing(quality: Int, modifier: Modifier = Modifier) {
    val tier = RollTier.of(quality)
    val tint = tier.tint
    Tipped({ Tip(ui("card.roll_quality", ui(tier.key), quality), tint = tint) }, modifier) {
        Box(Modifier.size(34.dp).glow(tint, on = tier.glow > 0.dp, radius = tier.glow / 2, shape = CircleShape), contentAlignment = Alignment.Center) {
            val molten = if (tier == RollTier.PERFECT) moltenPhase() else null
            Canvas(Modifier.fillMaxSize()) {
                val width = 3.5.dp.toPx()
                val arc = Size(size.width - width, size.height - width)
                val corner = Offset(width / 2, width / 2)
                drawArc(PanelRaised, -90f, 360f, false, corner, arc, style = Stroke(width))
                if (molten != null) {
                    drawArc(moltenBrush(molten.value), -90f, 360f, false, corner, arc, style = Stroke(width, cap = StrokeCap.Round))
                } else {
                    drawArc(tint, -90f, 360f * quality / 100f, false, corner, arc, style = Stroke(width, cap = StrokeCap.Round))
                }
            }
            Text("$quality", color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** «роллы 87%» as a pill in its step's dress (2.73.0, steps since 3.2.0): the one figure a list line is judged by. */
@Composable fun RollPill(quality: Int) {
    val tier = RollTier.of(quality)
    val shape = RoundedCornerShape(50)
    Text(
        ui("row.rolls", quality),
        color = if (tier == RollTier.PERFECT) Ink else tier.tint,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.rollShine(tier, shape, solid = true).padding(horizontal = 8.dp, vertical = 1.dp),
    )
}
