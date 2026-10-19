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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Вход стража на арену: часы входа от появления босса в бою. Арена с боссом с первого кадра ([fade]) проступает поверх карты за
 * [ENTRY_MS] мс; две полосы въезжают навстречу друг другу за [BAR_IN_MS] мс, плашка с именем раскрывается от центра; к [HOLD_END_MS]
 * всё уходит за [OUT_MS] мс, затем за [FX_MS] мс разгораются эффекты арены (аура портрета, частицы, порталы ниш). Без анимаций или
 * при упрощённых эффектах ([reduced]) полос и движения нет: арена сразу, плашка стоит [STILL_MS] мс и пропадает, эффекты - сразу.
 *
 * Доли читаются только в рисовании и слое: вход не переразмечает и не перекомпоновывает экран по кадрам.
 */
@Stable internal class BossIntro(val reduced: Boolean, played: Boolean, private val fade: Boolean = false) {
    private val end = if (reduced) STILL_MS else SHOW_MS + FX_MS
    private val clock = Animatable(if (played) end else 0f)
    private val ms get() = clock.value

    /** Плашка и полосы ещё на экране; читается композицией - меняется раз за вход. */
    val staged by derivedStateOf { ms < if (reduced) STILL_MS else SHOW_MS }

    /** Непрозрачность арены поверх карты. */
    val entry: Float get() = if (reduced || !fade) 1f else span(0f, ENTRY_MS, EaseOut)

    /** Насколько полосы въехали: 0 - за краем, 1 - на месте. */
    val bars: Float get() = when {
        reduced -> 0f
        ms < HOLD_END_MS -> span(0f, BAR_IN_MS, BarEase)
        else -> 1 - span(HOLD_END_MS, SHOW_MS, BarEase)
    }

    /** Раскрытая от центра доля ширины плашки. */
    val reveal: Float get() = if (reduced) 1f else span(BAR_IN_MS, REVEAL_END_MS, EaseOut)

    /** Непрозрачность плашки. */
    val plaque: Float get() = when {
        reduced -> 1f
        ms < HOLD_END_MS -> span(BAR_IN_MS, REVEAL_END_MS, EaseOut)
        else -> 1 - span(HOLD_END_MS, SHOW_MS, EaseOut)
    }

    /** Подъём уходящей плашки: 0..1 от [LIFT]. */
    val lift: Float get() = if (reduced) 0f else span(HOLD_END_MS, SHOW_MS, EaseOut)

    /** Сила эффектов арены: 0 на входе, 1 после него. */
    val effects: Float get() = if (reduced) 1f else span(SHOW_MS, SHOW_MS + FX_MS, EaseOut)

    /** Проигрывает вход с места, где остановился. */
    suspend fun play() {
        if (ms < end) clock.animateTo(end, tween((end - ms).roundToInt(), easing = LinearEasing))
    }

    /** Ждёт, пока эффекты арены начнут разгораться. */
    suspend fun awaitEffects() {
        snapshotFlow { effects > 0f }.first { it }
    }

    private fun span(from: Float, to: Float, easing: Easing) = easing.transform(((ms - from) / (to - from)).coerceIn(0f, 1f))

    companion object {
        const val ENTRY_MS = 200f
        const val BAR_IN_MS = 250f
        const val REVEAL_END_MS = 440f
        const val HOLD_END_MS = 1400f
        const val SHOW_MS = 2000f
        const val OUT_MS = SHOW_MS - HOLD_END_MS
        const val FX_MS = 500f
        const val STILL_MS = 1200f
        val LIFT = 8.dp

        /** Вход уже сыгран: эффекты горят, плашки нет. */
        val PLAYED = BossIntro(reduced = false, played = true)
    }
}

private val BarEase = CubicBezierEasing(.3f, .7f, .2f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, .58f, 1f)

/** Вход стража для слоёв арены, что ждут его конца (аура, частицы, порталы); вне боя с боссом - уже сыгран. */
internal val LocalBossIntro = staticCompositionLocalOf { BossIntro.PLAYED }

/**
 * Вход стража [boss] - новый на каждого босса; без босса - уже сыгран. Арена проступает поверх карты, только если босс в ней с первого
 * кадра. Настройки движения берутся на момент входа.
 */
@Composable internal fun rememberBossIntro(boss: Int?): BossIntro {
    val reduced = !LocalMotion.current || LocalSettings.current.simpleEffects
    val opening = remember { boss }
    val intro = remember(boss) { if (boss == null) BossIntro.PLAYED else BossIntro(reduced, played = false, fade = boss == opening) }
    LaunchedEffect(intro) { intro.play() }
    return intro
}

/**
 * Сцена входа стража внутри поля боя: полосы по 8% высоты - верхняя под рамкой стража ([top]), нижняя над панелью умений
 * ([bottom]), рамку и кнопку не закрывают; посередине - плашка «Страж зоны» с именем [name] (длинное - до трёх строк, кегль до 16sp,
 * лишнее срезает плашка). Границы - в координатах слоя, читаются при размещении: сдвиг раскладки не перекомпоновывает сцену.
 */
@Composable internal fun BossIntroStage(intro: BossIntro, name: String, top: () -> Float, bottom: () -> Float) {
    if (!intro.staged) return
    Box(Modifier.fillMaxSize()) {
        if (!intro.reduced) {
            IntroBar(intro, fromLeft = true) { _, _ -> top() }
            IntroBar(intro, fromLeft = false) { h, parent -> (bottom().takeIf { !it.isNaN() } ?: parent.toFloat()) - h }
        }
        val shape = RoundedCornerShape(PLAQUE_RADIUS)
        val clip = remember { Path() }
        Column(
            Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                .pinY { h, parent -> centreOf(top(), bottom(), parent) - h / 2f }
                .graphicsLayer {
                    alpha = intro.plaque
                    translationY = -BossIntro.LIFT.toPx() * intro.lift
                }
                .drawWithContent {
                    val half = size.width * (1 - intro.reveal) / 2
                    clip.reset()
                    clip.addRoundRect(RoundRect(half, 0f, size.width - half, size.height, CornerRadius(PLAQUE_RADIUS.toPx())))
                    clipPath(clip) { this@drawWithContent.drawContent() }
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

/** Полоса входа: въезжает слева или справа сдвигом слоя; [y] - её верх по её высоте и высоте слоя. */
@Composable private fun IntroBar(intro: BossIntro, fromLeft: Boolean, y: (height: Int, parent: Int) -> Float) {
    Spacer(
        Modifier.pinY(y).fillMaxWidth().fillMaxHeight(BAR_SHARE).graphicsLayer {
            val away = (1 - intro.bars) * size.width * 1.01f
            translationX = if (fromLeft) -away else away
        }.background(BarInk),
    )
}

/** Ставит элемент по вертикали в точку [y] (от его высоты и высоты родителя), вычисленную при размещении. */
private fun Modifier.pinY(y: (height: Int, parent: Int) -> Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    layout(placeable.width, placeable.height) {
        placeable.place(0, y(placeable.height, constraints.maxHeight).takeIf { !it.isNaN() }?.roundToInt() ?: 0)
    }
}

/** Середина поля между [top] и [bottom]; неизвестная граница - край слоя. */
private fun centreOf(top: Float, bottom: Float, parent: Int): Float {
    val from = top.takeIf { !it.isNaN() } ?: 0f
    val to = bottom.takeIf { !it.isNaN() } ?: parent.toFloat()
    return (from + to) / 2
}

private const val BAR_SHARE = .08f
private val PLAQUE_RADIUS = 14.dp
private val BarInk = Color(0xEB030506)
private val PlaqueInk = Color(0xF00A0E12)
