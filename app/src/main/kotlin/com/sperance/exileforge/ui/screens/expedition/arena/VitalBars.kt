package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.sin

/** A foe's or a pet's life bar: the shield laid over its top edge and the life written across it. */
@Composable internal fun LifeBar(life: Int, maxLife: Int, shield: Int, maxShield: Int, modifier: Modifier) {
    Box(modifier) {
        VitalBar(life, maxLife, LifeRed, Modifier.fillMaxSize(), text = "$life", size = 8.sp)
        if (maxShield > 0) {
            Box(
                Modifier.padding(horizontal = 3.dp).fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(3.dp)
                    .background(ShieldCyan.copy(alpha = .9f), RoundedCornerShape(50)),
            )
        }
    }
}

/**
 * One pool as a bar (3.24.0): a rounded dark track with a soft border of the pool's colour, the fill lit from above,
 * and the figures — now / most · share — across it in a shadowed type that reads over the fill and the track alike.
 * [ring] outlines it brighter, as a barrier (2.78.0) does the hero's life while it soaks. [reserved] past [max] is
 * the mana the auras hold: the bar's hatched tail, the fill running up to it.
 */
@Composable internal fun VitalBar(
    value: Int,
    max: Int,
    tint: Color,
    modifier: Modifier,
    ring: Color? = null,
    text: String = vitalFigures(value, max),
    size: TextUnit = 10.sp,
    reserved: Int = 0,
) {
    val shape = RoundedCornerShape(50)
    val whole = max + reserved.coerceAtLeast(0)
    val share by animateFloatAsState(if (whole > 0) (value / whole.toFloat()).coerceIn(0f, 1f) else 0f, label = "vital")
    Box(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(0xE6050709), Color(0xCC161B23))), shape)
            .reservedTail(reservedShare(max, reserved), tint).border(if (ring != null) 2.dp else 1.dp, ring ?: tint.copy(alpha = .45f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth(share).fillMaxHeight().clip(shape)
                .background(Brush.verticalGradient(listOf(lerp(tint, Color.White, .35f), tint, tint.copy(alpha = .7f)))),
        )
        Text(
            text,
            color = Color.White,
            fontSize = size,
            lineHeight = size,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            style = LocalTextStyle.current.copy(shadow = Shadow(Color.Black, Offset(0f, 1f), 3f)),
        )
    }
}

/** A pool's figures on its bar: now and most, and the share as a whole percent. */
private fun vitalFigures(value: Int, max: Int): String = "$value / $max · ${if (max > 0) (value * 100f / max).roundToInt() else 0}%"

/** The swing: full the instant the next blow lands; dimmed while nothing can land. */
@Composable internal fun SwingBar(swing: Float, held: Boolean, modifier: Modifier, tint: Color = Gold) {
    Box(modifier.height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
        Box(Modifier.fillMaxWidth(swing.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else tint, RoundedCornerShape(2.dp)))
    }
}

/** The colour and glyph of a buildup (3.78.0, variant A «Тонкая полоса со значком»): stun gold, freeze ice, electrocute violet. */
internal fun buildupTint(kind: Buildup): Color = when (kind) {
    Buildup.STUN -> Color(0xFFF2D23A)
    Buildup.FREEZE -> Color(0xFF8FD3FF)
    Buildup.ELECTROCUTE -> Color(0xFFB07CFF)
}
internal fun buildupGlyph(kind: Buildup): String = when (kind) {
    Buildup.STUN -> "✦"
    Buildup.FREEZE -> "❄"
    Buildup.ELECTROCUTE -> "ϟ"
}

/** The fullest buildup under the life: a 3-dp thread in its colour with its glyph in front; nothing while every bar is empty. */
@Composable internal fun BuildupBar(view: BuildupView, modifier: Modifier) {
    val kind = view.leading ?: return
    val tint = buildupTint(kind)
    Row(modifier.height(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(buildupGlyph(kind), color = tint, fontSize = 8.sp, lineHeight = 8.sp)
        Box(Modifier.weight(1f).height(3.dp).background(Color(0x1FFFFFFF), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(view.share).fillMaxHeight().background(tint, RoundedCornerShape(2.dp)))
        }
    }
}

/** A buildup gone off, on the portrait: stars over a stunned one, a crust of ice over a frozen one, sparks over an electrocuted one, and the word. */
@Composable internal fun BoxScope.BuildupMark(view: BuildupView, time: Float) {
    val (kind, word) = when {
        view.frozen -> Buildup.FREEZE to "fight.buildup.frozen"
        view.electrocuted -> Buildup.ELECTROCUTE to "fight.buildup.electrocuted"
        view.stunned -> Buildup.STUN to "fight.buildup.stunned"
        else -> return
    }
    val tint = buildupTint(kind)
    when (kind) {
        Buildup.FREEZE -> Box(
            Modifier.matchParentSize().background(Brush.linearGradient(listOf(tint.copy(alpha = .35f), tint.copy(alpha = .08f))))
                .border(2.dp, tint.copy(alpha = .8f), RoundedCornerShape(4.dp)),
        )

        Buildup.ELECTROCUTE -> Canvas(Modifier.matchParentSize().graphicsLayer { alpha = if (sin(time * 40f) > 0) 1f else .25f }) {
            val w = size.width
            val h = size.height
            val bolt = Path().apply {
                moveTo(w * .2f, h * .1f)
                lineTo(w * .42f, h * .45f)
                lineTo(w * .3f, h * .48f)
                lineTo(w * .62f, h * .9f)
            }
            drawPath(bolt, tint, style = Stroke(2.dp.toPx()))
        }

        Buildup.STUN -> Text(
            "✦ ✦ ✦",
            color = tint,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp)
                .graphicsLayer { translationY = sin(time * 6f) * 2.dp.toPx() },
        )
    }
    Text(
        ui(word),
        color = tint,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier.align(Alignment.BottomCenter).background(Ink.copy(alpha = .75f)).padding(horizontal = 3.dp),
    )
}
