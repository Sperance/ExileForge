package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sperance.exileforge.ui.components.LocalMotion
import kotlin.math.PI
import kotlin.math.sin

/**
 * Живой портрет (4.0.0): слои анимации поверх статичного портрета сервера для монстров с утверждённым анимированным обликом.
 * Каждая механика (Разлом, Скверна, ...) - свой набор; [LivingPortraits] выбирает набор по коду монстра, так что новая механика
 * добавляется одной строкой в его список, а не ветвлением у вызывающего.
 */
internal interface PortraitMotion {
    /** Есть ли у монстра [code] живые слои в этом наборе. */
    fun animates(code: String): Boolean

    /**
     * Слои монстра [code] над его портретом, в том же поле и с тем же сдвигом [lift], что у портрета. [time] - секунды, при
     * выключенных анимациях неподвижны. [phase] - стадия боя, которую облик показывает (у Матери Скверны - порванные пуповины);
     * вне боя и у монстров без стадий - 0.
     */
    fun draw(scope: DrawScope, code: String, time: Float, lift: Float, phase: Int = 0)

    /** Глубина вдоха фигуры 0..1 в момент [time]: портрет растягивается от низа рамки; 0 - фигура не дышит. */
    fun breath(code: String, time: Float): Float = 0f
}

/**
 * Набор, нарисованный в поле портрета сервера 300 на 400: слои кладутся в рамку тем же переносом, что и сам портрет
 * (вписать с запасом и выровнять по центру), так что координаты слоёв - координаты генератора портретов сервера.
 */
internal abstract class FieldPortraitMotion : PortraitMotion {
    /** Слои по коду монстра: рисуют в поле 300 на 400 по секундам и стадии боя. */
    protected abstract val layers: Map<String, DrawScope.(time: Float, phase: Int) -> Unit>

    override fun animates(code: String): Boolean = code in layers

    override fun draw(scope: DrawScope, code: String, time: Float, lift: Float, phase: Int) = with(scope) {
        val layer = layers[code] ?: return@with
        val scale = maxOf(size.width / FIELD_W, size.height / FIELD_H)
        val dx = (size.width - FIELD_W * scale) / 2
        val dy = (size.height - FIELD_H * scale) / 2 + lift
        withTransform({
            translate(dx, dy)
            scale(scale, scale, Offset.Zero)
        }) { layer(time, phase) }
    }

    companion object {
        const val FIELD_W = 300f
        const val FIELD_H = 400f
    }
}

/** Все наборы живых портретов: единственная точка, через которую портрет монстра узнаёт о своих слоях. */
internal object LivingPortraits {
    private val motions: List<PortraitMotion> = listOf(RiftPortraits, BlightPortraits)

    private fun of(code: String): PortraitMotion? = motions.firstOrNull { it.animates(code) }

    fun animated(code: String): Boolean = of(code) != null

    fun draw(scope: DrawScope, code: String, time: Float, lift: Float, phase: Int = 0) {
        of(code)?.draw(scope, code, time, lift, phase)
    }

    /** Рисует [block] с дыханием фигуры [code]: на вдохе на 2% шире и на 3% выше, опора - низ рамки. */
    fun breathing(scope: DrawScope, code: String, time: Float, block: DrawScope.() -> Unit) = with(scope) {
        val depth = of(code)?.breath(code, time) ?: 0f
        if (depth == 0f) return@with block()
        withTransform({ scale(1 + BREATH_X * depth, 1 + BREATH_Y * depth, Offset(size.width / 2, size.height)) }, block)
    }

    private const val BREATH_X = .02f
    private const val BREATH_Y = .03f
}

/** Синусоида 0..1 с периодом [period] секунд и сдвигом [phase] в долях периода. */
internal fun portraitWave(t: Float, period: Float, phase: Float = 0f) = .5f + .5f * sin((t / period + phase) * 2 * PI.toFloat())

/** Мягкое радиальное свечение: цвет [color] с прозрачностью [alpha] в центре, к краю [radius] - ничего. */
internal fun DrawScope.portraitGlow(center: Offset, radius: Float, color: Color, alpha: Float) = drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius), radius, center)

/** Искры, что всплывают снизу поля вверх и гаснут. */
internal fun DrawScope.portraitSparks(t: Float, count: Int, color: Color) {
    repeat(count) { i ->
        val seed = i * 37.17f
        val u = ((t * (.08f + (i % 5) * .015f) + seed % 1f) % 1f)
        val x = (seed * 7.3f) % FieldPortraitMotion.FIELD_W + sin(t * .9f + i) * 6f
        val y = FieldPortraitMotion.FIELD_H + 10 - u * (FieldPortraitMotion.FIELD_H + 30)
        val a = (sin(u * PI.toFloat())) * .8f
        portraitGlow(Offset(x, y), 5f, color, a * .5f)
        drawCircle(color.copy(alpha = a), 1.1f, Offset(x, y))
    }
}

/** Часы живого портрета (3.96.1): секунды для [LivingPortraits] у живого монстра при включённых анимациях, иначе - покой. */
@Composable
internal fun portraitClock(code: String): Float {
    val moving = LocalMotion.current && LivingPortraits.animated(code)
    val time = remember(code) { mutableFloatStateOf(0f) }
    LaunchedEffect(code, moving) {
        if (!moving) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { now -> time.floatValue = (now - start) / 1e9f }
    }
    return time.floatValue
}
