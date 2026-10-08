package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.screens.expedition.scene.BlightTint
import kotlin.math.PI
import kotlin.math.sin

// ============================================================ Матерь Скверны (4.0.0, макет B)

/**
 * «Сердце Скверны» под полосой жизни Матери: пуповины ромбами - живые дышат жаром, порванные гаснут; разрыв - кольцо-вспышка
 * над ромбом и строка «Пуповина рвётся!». Всех пуповин [BossHud.cords] - по одной на порог фазы и последняя на смерть.
 */
@Composable internal fun CordStrip(boss: BossHud, alive: Boolean, time: Float) {
    val motion = LocalMotion.current
    val whole = if (alive) boss.cords - boss.torn else 0
    var seen by remember { mutableIntStateOf(whole) }
    val burst = remember { Animatable(1f) }
    var torn by remember { mutableIntStateOf(-1) }
    LaunchedEffect(whole) {
        if (whole < seen) {
            torn = whole
            burst.snapTo(0f)
            burst.animateTo(1f, tween(BURST_MS, easing = LinearEasing))
        }
        seen = whole
    }
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(BlightTint.flesh.copy(alpha = .35f), BlightTint.void))).border(1.dp, BlightTint.flesh, shape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            ui("boss.cords", whole, boss.cords).uppercase(),
            color = BlightTint.hot,
            fontFamily = FontFamily.Serif,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            maxLines = 1,
        )
        if (burst.value < 1f) {
            Text(
                ui("feature.blight.cord"),
                color = BlightTint.bile.copy(alpha = 1f - burst.value),
                fontFamily = FontFamily.Serif,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(boss.cords) { i ->
                val pulse = if (motion) .7f + .3f * sin(time * 2 * PI.toFloat() / HEART_SECONDS + i) else 1f
                Box { CordPip(i < whole, pulse, if (i == torn) burst.value else 1f) }
            }
        }
    }
}

/** Ромб пуповины: [lit] - цела, [pulse] - дыхание жара; [burst] < 1 - только что порвалась: кольцо расходится и гаснет. */
@Composable private fun CordPip(lit: Boolean, pulse: Float, burst: Float) {
    val glow by animateFloatAsState(if (lit) 1f else 0f, tween(if (lit) 0 else BURST_MS, easing = FastOutSlowInEasing), label = "cord")
    Canvas(Modifier.size(13.dp)) {
        val c = center
        val r = size.minDimension / 2
        val gem = Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + r * .72f, c.y)
            lineTo(c.x, c.y + r)
            lineTo(c.x - r * .72f, c.y)
            close()
        }
        if (glow > 0f) drawCircle(BlightTint.vein.copy(alpha = .45f * glow * pulse), r * 1.15f, c)
        drawPath(gem, Brush.linearGradient(listOf(BlightTint.hot.copy(alpha = glow), BlightTint.vein.copy(alpha = .25f + .75f * glow), BlightTint.void), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)))
        drawPath(gem, if (glow > .5f) BlightTint.bile else BlightTint.flesh, style = Stroke(1.dp.toPx()))
        if (burst < 1f) drawCircle(BlightTint.hot.copy(alpha = 1f - burst), r * (1f + 1.6f * burst), c, style = Stroke(1.5.dp.toPx()))
    }
}

/** Двойной удар сердца Матери, секунды периода (макет: 1,4 с). */
private const val HEART_SECONDS = 1.4f

/** Вспышка разрыва пуповины, мс. */
private const val BURST_MS = 900
