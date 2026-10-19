package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/** Ворота эффектов арены (аура портрета, частицы неба, порталы ниш): [effects] - их сила 0..1, читается в рисовании. */
@Stable internal interface ArenaFxGate {
    val effects: Float

    /** Ждёт, пока эффекты начнут разгораться. */
    suspend fun awaitEffects()

    companion object {
        /** Эффекты горят всегда: бой без входа стража. */
        val OPEN: ArenaFxGate = object : ArenaFxGate {
            override val effects = 1f

            override suspend fun awaitEffects() = Unit
        }
    }
}

/** Ворота эффектов для глубоких слоёв арены; вне входа стража - открыты. */
internal val LocalArenaFx = staticCompositionLocalOf { ArenaFxGate.OPEN }

/**
 * Вход стража на арену: часы входа от появления босса в бою. Арена проступает поверх карты (только для босса с первого кадра), две
 * полосы въезжают навстречу друг другу, плашка с именем раскрывается от центра, всё уходит, затем разгораются эффекты арены. Без
 * анимаций или при упрощённых эффектах полос и движения нет: арена сразу, плашка стоит и пропадает, эффекты - сразу. Тайминг - чистые
 * функции [IntroTiming]; сцену рисует [Stage].
 *
 * Доли читаются только в рисовании и слое; после входа [entry] и [effects] часов не читают.
 */
@Stable internal class BossIntro private constructor(private val reduced: Boolean, private val fadeIn: Boolean) : ArenaFxGate {
    private val clock = Animatable(0f)
    private val ms get() = clock.value
    private val end = IntroTiming.end(reduced)
    private val entering by derivedStateOf { fadeIn && !reduced && ms < IntroTiming.ENTRY_MS }
    private val lit by derivedStateOf { reduced || ms >= end }

    /** Сцена (полосы и плашка) ещё на экране; читается композицией - меняется раз за вход. */
    val staged by derivedStateOf { IntroTiming.staged(ms, reduced) }

    /** Непрозрачность арены поверх карты. */
    val entry: Float get() = if (entering) IntroTiming.entry(ms) else 1f

    override val effects: Float get() = if (lit) 1f else IntroTiming.effects(ms)

    override suspend fun awaitEffects() {
        snapshotFlow { effects > 0f }.first { it }
    }

    /** Проигрывает вход; упрощённый - выдержкой без анимации, чтобы плашка стояла и при выключенных системных анимациях. */
    suspend fun play() {
        if (ms >= end) return
        if (reduced) {
            delay(end.toLong())
            clock.snapTo(end)
        } else {
            clock.animateTo(end, tween((end - ms).roundToInt(), easing = LinearEasing))
        }
    }

    /**
     * Сцена входа внутри поля боя: полосы по 8% высоты - верхняя под рамкой стража, нижняя над панелью умений ([bounds]), рамку и
     * кнопку не закрывают; посередине - плашка «Страж зоны» с именем [name] (длинное - до трёх строк, кегль до 16sp, лишнее срезает
     * плашка).
     */
    @Composable fun Stage(name: String, bounds: IntroBounds) {
        if (staged) IntroStage(reduced, { ms }, name, bounds)
    }

    companion object {
        /** Новый вход; [reduced] - без движения, [fadeIn] - арена проступает поверх карты. */
        fun fresh(reduced: Boolean, fadeIn: Boolean) = BossIntro(reduced, fadeIn)
    }
}

/**
 * Вход стража [bossIndex] - новый на каждого босса; без босса - null. Арена проступает поверх карты, только если босс в бою с первого
 * кадра. Настройки движения берутся на момент входа.
 */
@Composable internal fun rememberBossIntro(bossIndex: Int?): BossIntro? {
    val reduced = !LocalMotion.current || LocalSettings.current.simpleEffects
    val opening = remember { bossIndex }
    val intro = remember(bossIndex) { bossIndex?.let { BossIntro.fresh(reduced, fadeIn = it == opening) } }
    LaunchedEffect(intro) { intro?.play() }
    return intro
}

/**
 * Границы поля для сцены входа: низ рамки стража и низ поля над панелью умений, в координатах корня; сцена переводит их в свои при
 * размещении. Не измеренная граница - край сцены.
 */
@Stable internal class IntroBounds {
    private var frame by mutableStateOf<Float?>(null)
    private var field by mutableStateOf<Float?>(null)
    private var origin by mutableFloatStateOf(0f)

    fun frame(coordinates: LayoutCoordinates) {
        frame = coordinates.boundsInRoot().bottom
    }

    fun field(coordinates: LayoutCoordinates) {
        field = coordinates.boundsInRoot().bottom
    }

    fun stage(coordinates: LayoutCoordinates) {
        origin = coordinates.positionInRoot().y
    }

    /** Верх поля в координатах сцены (с зазором под рамкой). */
    fun Density.top(): Float = frame?.let { it - origin + FIELD_GAP.toPx() } ?: 0f

    /** Низ поля в координатах сцены; [stage] - высота сцены. */
    fun bottom(stage: Int): Float = field?.let { it - origin } ?: stage.toFloat()
}

/** Тайминг входа по варианту «Кино, но аккуратно»: доли сцены от миллисекунд входа. */
private object IntroTiming {
    const val ENTRY_MS = 200f
    private const val BAR_IN_MS = 250f
    private const val REVEAL_END_MS = 440f
    private const val HOLD_END_MS = 1400f
    private const val SHOW_MS = 2000f
    private const val FX_MS = 500f
    private const val STILL_MS = 1200f
    private val BarEase = CubicBezierEasing(.3f, .7f, .2f, 1f)
    private val EaseOut = CubicBezierEasing(0f, 0f, .58f, 1f)

    fun end(reduced: Boolean) = if (reduced) STILL_MS else SHOW_MS + FX_MS

    fun staged(ms: Float, reduced: Boolean) = ms < if (reduced) STILL_MS else SHOW_MS

    fun entry(ms: Float) = span(ms, 0f, ENTRY_MS, EaseOut)

    fun effects(ms: Float) = span(ms, SHOW_MS, SHOW_MS + FX_MS, EaseOut)

    /** Насколько полосы въехали: 0 - за краем, 1 - на месте. */
    fun bars(ms: Float) = if (ms < HOLD_END_MS) span(ms, 0f, BAR_IN_MS, BarEase) else 1 - span(ms, HOLD_END_MS, SHOW_MS, BarEase)

    /** Раскрытая от центра доля ширины плашки. */
    fun reveal(ms: Float) = span(ms, BAR_IN_MS, REVEAL_END_MS, EaseOut)

    fun plaque(ms: Float) = if (ms < HOLD_END_MS) reveal(ms) else 1 - lift(ms)

    /** Подъём уходящей плашки: 0..1. */
    fun lift(ms: Float) = span(ms, HOLD_END_MS, SHOW_MS, EaseOut)

    private fun span(ms: Float, from: Float, to: Float, easing: Easing) = easing.transform(((ms - from) / (to - from)).coerceIn(0f, 1f))
}

@Composable private fun IntroStage(reduced: Boolean, ms: () -> Float, name: String, bounds: IntroBounds) {
    Box(Modifier.fillMaxSize().onGloballyPositioned(bounds::stage)) {
        if (!reduced) {
            IntroBar(ms, fromLeft = true) { _, _ -> with(bounds) { top() } }
            IntroBar(ms, fromLeft = false) { h, stage -> bounds.bottom(stage) - h }
        }
        val shape = RoundedCornerShape(14.dp)
        Column(
            Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                .pinY { h, stage -> (with(bounds) { top() } + bounds.bottom(stage)) / 2 - h / 2f }
                .graphicsLayer {
                    if (!reduced) {
                        alpha = IntroTiming.plaque(ms())
                        translationY = -PLAQUE_LIFT.toPx() * IntroTiming.lift(ms())
                    }
                }
                .drawWithContent {
                    val reveal = if (reduced) 1f else IntroTiming.reveal(ms())
                    if (reveal >= 1f) {
                        drawContent()
                    } else {
                        val half = size.width * (1 - reveal) / 2
                        clipRect(left = half, right = size.width - half) { this@drawWithContent.drawContent() }
                    }
                }
                .heightIn(max = 156.dp).clip(shape).background(PlaqueInk, shape).border(1.dp, FrameGold.copy(alpha = .6f), shape)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(ui("boss.intro"), color = Color(0xFF9A8B7A), fontSize = 11.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            FittedName(name, FrameGold, 20.sp, 16.sp, Modifier.fillMaxWidth(), TextAlign.Center, maxLines = 3)
        }
    }
}

/** Полоса входа: въезжает слева или справа сдвигом слоя; [y] - её верх по её высоте и высоте сцены. */
@Composable private fun IntroBar(ms: () -> Float, fromLeft: Boolean, y: Density.(height: Int, stage: Int) -> Float) {
    Spacer(
        Modifier.pinY(y).fillMaxWidth().fillMaxHeight(BAR_SHARE).graphicsLayer {
            val away = (1 - IntroTiming.bars(ms())) * size.width * 1.01f
            translationX = if (fromLeft) -away else away
        }.background(BarInk),
    )
}

/** Ставит элемент по вертикали в точку [y] (от его высоты и высоты сцены), вычисленную при размещении. */
private fun Modifier.pinY(y: Density.(height: Int, stage: Int) -> Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    layout(placeable.width, placeable.height) {
        placeable.place(0, y(placeable.height, constraints.maxHeight).roundToInt())
    }
}

private const val BAR_SHARE = .08f
private val FIELD_GAP = 8.dp
private val PLAQUE_LIFT = 8.dp
private val BarInk = Color(0xEB030506)
private val PlaqueInk = Color(0xF00A0E12)
