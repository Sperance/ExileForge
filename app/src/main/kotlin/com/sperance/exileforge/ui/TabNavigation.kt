package com.sperance.exileforge.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sperance.exileforge.ui.components.LocalScreenAway
import com.sperance.exileforge.ui.nav.Route
import com.sperance.exileforge.ui.nav.barSlot
import kotlin.math.roundToInt

/** Длительность смены вкладки нижней панели: и экран, и шапка, и капсула кнопки. */
const val TAB_SHIFT_MS = 220

/** Кривая смены вкладки: быстрый старт и мягкая посадка. */
val TabShiftEasing: Easing = CubicBezierEasing(.2f, 0f, 0f, 1f)

/** Доля ширины, на которую уезжает экран при смене вкладки. */
private const val SHIFT_SHARE = .3f

private fun <T> shiftSpec(): FiniteAnimationSpec<T> = tween(TAB_SHIFT_MS, easing = TabShiftEasing)

/**
 * Переход смены вкладки: при [step] ≠ 0 новое входит со стороны порядка вкладок (вкладка правее - справа), прежнее уходит
 * в другую сторону, оба на ~30% ширины, с затуханием, без масштаба; при 0 (экран поверх, «назад») - только затухание.
 * Переходы экранов анимируются всегда, без `LocalMotion`.
 */
fun tabShift(step: Int): ContentTransform {
    if (step == 0) return fadeIn(shiftSpec()) togetherWith fadeOut(shiftSpec())
    val slide = shiftSpec<IntOffset>()
    val enter = slideInHorizontally(slide) { (it * SHIFT_SHARE * step).roundToInt() } + fadeIn(shiftSpec())
    val exit = slideOutHorizontally(slide) { (-it * SHIFT_SHARE * step).roundToInt() } + fadeOut(shiftSpec())
    return enter togetherWith exit
}

/**
 * Часть оболочки над экраном (шапка, полосы разделов), что сдвигается вместе с экраном при смене вкладки: [look] - её облик для
 * маршрута; новый облик въезжает той же кривой, что и экран, со стороны порядка вкладок, тот же облик не двигается. Высота
 * шапки меняется сразу, без анимации: экран под ней не перемеряется посреди смены.
 *
 * Размер шапки встаёт сразу: анимированная высота перемеряла бы экран под ней каждый кадр смены вкладки.
 */
@Composable fun <K> TabShifted(route: Route, look: (Route) -> K, modifier: Modifier = Modifier, content: @Composable (Route) -> Unit) {
    AnimatedContent(
        targetState = route,
        modifier = modifier,
        contentKey = look,
        transitionSpec = {
            val from = initialState.barSlot
            val to = targetState.barSlot
            val step = if (from == null || to == null) 0 else to.compareTo(from)
            tabShift(step) using SizeTransform(clip = true) { _, _ -> snap() }
        },
        label = "tabChrome",
    ) { content(it) }
}

/**
 * Украшение записей `NavDisplay`: запись, чей выход доводит переход, стоит ([LocalScreenAway]) - уходящий экран не дышит декором,
 * пока доезжает, и не делит кадры с входящим.
 */
val ScreenAwayDecorator: NavEntryDecorator<Route> = NavEntryDecorator { entry ->
    val transition = LocalNavAnimatedContentScope.current.transition
    val away = remember(transition) { derivedStateOf { transition.targetState == EnterExitState.PostExit } }
    CompositionLocalProvider(LocalScreenAway provides away) { entry.Content() }
}

/**
 * Память вкладок: сохранённое состояние экранов стека (прокрутка, раскрытое, введённое) по ключам содержимого. Экран,
 * снятый со стека, своё состояние теряет; корень вкладки нижней панели (запись с [ROOT] в метаданных) - бережёт до возвращения,
 * так что вкладка открывается там, где её оставили. Сменился герой - память корней забывается ([forget]).
 */
class TabMemory(private val holder: SaveableStateHolder) {
    /** Ключи содержимого корней вкладок, встреченных на экране. */
    private val roots = HashSet<Any>()

    /** Корни, снятые со стека, чьё состояние ждёт возвращения. */
    private val parked = HashSet<Any>()

    /** Украшение записей `NavDisplay`: каждая запись - в своём сохраняемом состоянии. */
    val decorator: NavEntryDecorator<Route> = NavEntryDecorator(
        onPop = { key -> if (key in roots) parked += key else holder.removeState(key) },
    ) { entry ->
        DisposableEffect(entry.contentKey) {
            if (entry.metadata[ROOT_KEY] == true) roots += entry.contentKey
            parked -= entry.contentKey
            onDispose { }
        }
        holder.SaveableStateProvider(entry.contentKey) { entry.Content() }
    }

    /** Забыть отложенные корни: их прокрутка принадлежала прежнему герою. */
    fun forget() {
        parked.forEach(holder::removeState)
        parked.clear()
    }

    companion object {
        private const val ROOT_KEY = "tabRoot"

        /** Метаданные записи корня вкладки: её состояние переживает уход с вкладки. */
        val ROOT: Map<String, Any> = mapOf(ROOT_KEY to true)
    }
}

/** Память вкладок на композицию оболочки; [owner] - чья это память (герой): сменился - отложенное забывается. */
@Composable fun rememberTabMemory(owner: Any?): TabMemory {
    val holder = rememberSaveableStateHolder()
    val memory = remember(holder) { TabMemory(holder) }
    LaunchedEffect(memory, owner) { memory.forget() }
    return memory
}
