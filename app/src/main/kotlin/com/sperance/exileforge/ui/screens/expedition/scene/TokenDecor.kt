package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sperance.exileforge.core.campaign.run.BlightSpot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Декор жетона на карте (4.3.0): слой поверх жетона и его кольца редкости - кольцо остаётся, декор лишь говорит, чей монстр.
 * Новая метка жетона - новая реализация, а не ветка в [ScenePainter]. [time] - часы декора ([ScenePainter.decor]): при
 * выключенном `LocalMotion` стоят на нуле, и декор стоит в покое.
 */
internal interface TokenDecor {
    /** Под жетоном: ореол, что выглядывает из-за кольца. */
    fun DrawScope.under(centre: Offset, radius: Float, time: Float) {}

    /** Поверх жетона и кольца. */
    fun DrawScope.over(centre: Offset, radius: Float, time: Float) {}
}

/** Декор жетона [token] на карте: метка очага Скверны или ничего. */
internal fun BlightSpot?.decorOf(token: Int): TokenDecor? = this?.age(token)?.let { (_, n) -> BlightVeins(leader = n == 0) }

/**
 * «Пульсирующие жилы» монстра Скверны: рваный багровый ореол вокруг кольца, жилы-трещины наружу и капля ихора снизу - всё бьётся
 * в ритм сердца Матери ([motherBeat]). У вожака точки ([leader]) - желчная обводка. Палитра - только [BlightTint].
 */
private class BlightVeins(private val leader: Boolean) : TokenDecor {
    override fun DrawScope.under(centre: Offset, radius: Float, time: Float) {
        val surge = surge(time)
        glow(centre, radius, surge)
        veins(centre, radius, surge)
    }

    override fun DrawScope.over(centre: Offset, radius: Float, time: Float) {
        rag(centre, radius, time, surge(time))
        ichor(centre, radius, time)
        if (leader) drawCircle(BlightTint.bile, radius * 1.16f, centre, style = Stroke(radius * .07f))
    }

    /** Сила удара сердца 0..1: покой - 0, пик первого удара - 1. */
    private fun surge(time: Float) = ((motherBeat(time) - 1f) / (BEAT_PEAK - 1f)).coerceIn(0f, 1f)

    /** Багровое свечение из-за кольца: густеет и ширится на ударе. */
    private fun DrawScope.glow(centre: Offset, radius: Float, surge: Float) {
        val reach = radius * (HALO_REACH + HALO_SURGE * surge)
        drawCircle(
            Brush.radialGradient(
                0f to Color.Transparent,
                radius / reach to BlightTint.vein.copy(alpha = .4f + .3f * surge),
                1f to Color.Transparent,
                center = centre,
                radius = reach,
            ),
            reach,
            centre,
        )
    }

    /** Рваный ореол по кольцу: край пляшет волнами и вспыхивает на ударе. */
    private fun DrawScope.rag(centre: Offset, radius: Float, time: Float, surge: Float) {
        val edge = Path()
        for (i in 0..HALO_POINTS) {
            val a = i * 2 * PI.toFloat() / HALO_POINTS
            val rag = 1f + .05f * sin(5 * a + time * 1.7f) + .035f * sin(11 * a - time * 2.3f)
            val r = radius * (1.06f + .04f * surge) * rag
            val p = centre + Offset(cos(a) * r, sin(a) * r)
            if (i == 0) edge.moveTo(p.x, p.y) else edge.lineTo(p.x, p.y)
        }
        edge.close()
        drawPath(edge, BlightTint.vein.copy(alpha = .7f + .3f * surge), style = Stroke(radius * .07f))
    }

    /** Жилы-трещины: из-под кольца наружу, ломаной в два колена; на ударе их кончики загораются жаром. */
    private fun DrawScope.veins(centre: Offset, radius: Float, surge: Float) {
        VEIN_ANGLES.forEach { (angle, kink) ->
            fun at(r: Float, a: Float) = centre + Offset(cos(a) * radius * r, sin(a) * radius * r)
            val reach = VEIN_REACH + .12f * surge
            val path = Path().apply {
                val start = at(1.02f, angle)
                moveTo(start.x, start.y)
                val mid = at((1.02f + reach) / 2, angle + kink)
                lineTo(mid.x, mid.y)
                val tip = at(reach, angle - kink * .5f)
                lineTo(tip.x, tip.y)
            }
            val colour = Brush.linearGradient(listOf(BlightTint.vein, BlightTint.hot.copy(alpha = .5f + .5f * surge)), at(1f, angle), at(reach, angle))
            drawPath(path, colour, style = Stroke(radius * .07f, cap = StrokeCap.Round))
        }
    }

    /** Капля ихора: набухает под жетоном и срывается вниз, по кругу ([ICHOR_SECONDS]). */
    private fun DrawScope.ichor(centre: Offset, radius: Float, time: Float) {
        val u = (time / ICHOR_SECONDS) % 1f
        val fall = u * u * radius * .35f
        val drop = radius * (.07f + .04f * (1 - u))
        drawCircle(BlightTint.vein.copy(alpha = 1f - u * .8f), drop, centre + Offset(0f, radius * 1.1f + fall))
        drawCircle(BlightTint.hot.copy(alpha = (1f - u) * .6f), drop * .45f, centre + Offset(-drop * .3f, radius * 1.1f + fall - drop * .3f))
    }
}

/** Пик первого удара сердца Матери ([motherBeat]). */
private const val BEAT_PEAK = 1.07f

/** Ореол Скверны: радиус свечения в радиусах жетона и прибавка на ударе. */
private const val HALO_REACH = 1.45f
private const val HALO_SURGE = .2f

/** Точек на рваном крае ореола. */
private const val HALO_POINTS = 40

/** Длина жилы в радиусах жетона. */
private const val VEIN_REACH = 1.42f

/** Жилы-трещины: угол от центра (0 - вправо, по часовой) и излом колена, радианы; четыре, вразброс. */
private val VEIN_ANGLES = listOf(-2.4f to .22f, -.75f to -.18f, .5f to .2f, 2.6f to -.2f)

/** Капля ихора: секунд на набухание и падение. */
private const val ICHOR_SECONDS = 2.2f
