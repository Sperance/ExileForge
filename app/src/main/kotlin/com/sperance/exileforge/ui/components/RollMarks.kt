package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.RollSummary
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/**
 * The roll quality in steps of twenty (3.2.0): each step louder than the one below — from a quiet grey
 * with no frame to a gold that glows — and a perfect hundred shimmers through every colour.
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

/** The colours a perfect roll runs through. */
private val Prism = listOf(Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF7BE0A6), Color(0xFF8FD3FF), Color(0xFFB48CFF), Color(0xFFFF6B6B))

/** A perfect roll's light: a band of every colour sliding along, for its frame and its figure. */
@Composable private fun prismBrush(): Brush {
    val shift by rememberInfiniteTransition(label = "prism").animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "prism",
    )
    val span = 240f
    return Brush.linearGradient(Prism, start = Offset(shift * span * 2 - span, 0f), end = Offset(shift * span * 2, span / 3), tileMode = TileMode.Mirror)
}

/** The frame, fill and glow a roll quality wears on [shape]; a perfect one is [solid]ly prismatic on a pill, framed on a card. */
@Composable private fun Modifier.rollShine(tier: RollTier, shape: Shape, solid: Boolean = false): Modifier {
    val base = glow(tier.tint, on = tier.glow > 0.dp, radius = tier.glow, shape = shape)
    return if (tier == RollTier.PERFECT) {
        base.background(if (solid) prismBrush() else Brush.horizontalGradient(listOf(PanelRaised, Abyss)), shape).border(1.5.dp, prismBrush(), shape)
    } else {
        base.background(Brush.horizontalGradient(listOf(tier.tint.copy(alpha = tier.fill), tier.tint.copy(alpha = tier.fill / 3))), shape)
            .border(1.dp, tier.tint.copy(alpha = tier.edge), shape)
    }
}

/**
 * How well the item rolled (2.72.0; a ring in the card's head since this version): a gauge filling its
 * ring to the share with the figure inside, in its step's colour; a tap names the step and what it measures.
 */
@Composable fun RollRing(quality: Int, modifier: Modifier = Modifier) {
    val tier = RollTier.of(quality)
    val tint = tier.tint
    Tipped({ Tip("${ui(tier.key)} · $quality%", ui("card.roll_quality_hint"), tint = tint) }, modifier) {
        Box(Modifier.size(34.dp).glow(tint, on = tier.glow > 0.dp, radius = tier.glow / 2, shape = CircleShape), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val width = 3.5.dp.toPx()
                val arc = Size(size.width - width, size.height - width)
                val corner = Offset(width / 2, width / 2)
                drawArc(PanelRaised, -90f, 360f, false, corner, arc, style = Stroke(width))
                drawArc(tint, -90f, 360f * quality / 100f, false, corner, arc, style = Stroke(width, cap = StrokeCap.Round))
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
