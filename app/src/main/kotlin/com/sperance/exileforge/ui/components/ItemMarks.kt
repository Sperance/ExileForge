package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import kotlin.math.sin

/** The mythical's sky (3.81.0, mockup B «Небесный свод»): violet night, starlight and the pale gold of its rim. */
internal val MythicNight = Color(0xFF170D1D)
internal val MythicDusk = Color(0xFF3A1A2E)
internal val MythicStar = Color(0xFFFFE9A8)
internal val MythicViolet = Color(0xFFC8A0FF)
internal val MythicName = Color(0xFFFFE2B0)

/** The stars of the sky, as shares of the card: fixed, so a card does not reshuffle its night on every draw. */
private val STARS = listOf(.2f to .3f, .7f to .2f, .4f to .7f, .85f to .6f, .1f to .85f, .6f to .9f, .3f to .1f, .92f to .15f, .52f to .45f, .15f to .55f)

/** The clock of the sky's twinkle and its sigil's turn; still when the animations are off in the settings. */
@Composable private fun skyClock(): Float {
    if (!LocalMotion.current) return remember { mutableFloatStateOf(0f) }.floatValue
    val time by rememberInfiniteTransition(label = "mythic").animateFloat(0f, 1f, infiniteRepeatable(tween(SKY_MS, easing = LinearEasing), RepeatMode.Restart), label = "sky")
    return time
}

/**
 * A mythical card's ground: a night sky deepening from a dusky top, its stars breathing, under a thin rim of starlight —
 * the card stays as readable as any other, only its night is its own.
 */
@Composable internal fun Modifier.celestial(): Modifier {
    val time = skyClock()
    return this.clipToBounds().drawBehind {
        drawRect(Brush.radialGradient(listOf(MythicDusk, MythicNight, Color(0xFF08070D)), Offset(size.width / 2, 0f), size.maxDimension))
        STARS.forEachIndexed { i, (x, y) ->
            val glow = .35f + .65f * ((sin((time * 2 * Math.PI + i * 1.3).toFloat()) + 1) / 2)
            drawCircle((if (i % 3 == 0) MythicStar else Color.White).copy(alpha = glow), if (i % 4 == 0) 1.6.dp.toPx() else 1.dp.toPx(), Offset(size.width * x, size.height * y))
        }
    }.border(1.dp, MythicStar.copy(alpha = .55f))
}

/** The turning sigil round a mythical's icon: a dashed star-ring and a crimson star turning the other way. */
@Composable internal fun Modifier.sigil(): Modifier {
    val time = skyClock()
    return this.drawBehind {
        val r = size.minDimension / 2 + 6.dp.toPx()
        val c = Offset(size.width / 2, size.height / 2)
        rotate(time * 360f, c) {
            drawCircle(MythicStar.copy(alpha = .6f), r, c, style = Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 7f))))
        }
        rotate(-time * 480f, c) {
            val star = androidx.compose.ui.graphics.Path().apply {
                (0 until 8).forEach { k ->
                    val a = Math.PI * k / 4
                    val rr = if (k % 2 == 0) r - 2.dp.toPx() else r * .62f
                    val p = Offset(c.x + (rr * kotlin.math.cos(a)).toFloat(), c.y + (rr * kotlin.math.sin(a)).toFloat())
                    if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(star, Color(0xFFE05A4E).copy(alpha = .55f), style = Stroke(1.dp.toPx()))
        }
    }
}

/** A mythical's name: warm starlight with a crimson glow. */
internal val mythicTitle: TextStyle
    @Composable get() = MaterialTheme.typography.titleLarge.copy(color = MythicName, shadow = Shadow(Color(0xFFE05A4E), blurRadius = 14f))

/**
 * The copy's place among every copy of its unique in the game (3.81.0, mockup C «Лента и клеймо»): a diagonal ribbon over
 * the card's corner — gold for the very first — whose figure the stamp under the description repeats in words.
 */
@Composable internal fun BoxScope.SerialRibbon(serial: Long) {
    val first = serial == 1L
    Box(
        Modifier.align(Alignment.TopStart).padding(top = 14.dp).requiredWidth(120.dp).rotate(-45f).drawBehind {
            drawRect(Brush.verticalGradient(if (first) listOf(Color(0xFFFFE9A8), Color(0xFFE0A93A)) else listOf(Color(0xFFD4B06A), Color(0xFF9C7A3C))))
        },
        contentAlignment = Alignment.Center,
    ) {
        Text("№ $serial", color = Color(0xFF2B1F0E), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 2.dp))
    }
}

/** «Экземпляр № 7»: the stamp under a unique's description. */
@Composable internal fun SerialStamp(serial: Long) {
    Text(
        ui("item.serial", serial),
        color = Color(0xFFC8AA6E),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.border(1.dp, Color(0xFFC8AA6E).copy(alpha = .6f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** One turn of the sky's clock, ms. */
private const val SKY_MS = 24_000
