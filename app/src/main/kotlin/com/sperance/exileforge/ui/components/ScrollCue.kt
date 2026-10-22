package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Quiet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.roundToInt

/** Край ряда с прокруткой вбок: начало или конец. */
enum class CueEdge { START, END }

/**
 * Подсказка прокрутки горизонтального ряда - одна на все ряды, что листаются вбок ([scrollCue]): край, за которым есть ещё элементы,
 * затухает в цвет дорожки, на нём некликабельный шеврон; выбранный элемент за краем - шеврон акцентом с точкой, и ряд сам доводится
 * до выбранного ([rememberRowScrollCue], [rememberLazyRowScrollCue]), когда выбор сменился или ряд впервые разложен. Ряд обычной
 * прокрутки - [RowScrollCue], ленивый - [LazyRowScrollCue].
 */
@Stable
sealed class ScrollCue {
    /** Прокрутка ряда: по ней видно, есть ли что за каждым краем. */
    internal abstract val scroll: ScrollableState

    /** Край, за которым прячется выбранный элемент; null - он виден или выбора нет. */
    internal abstract val hidden: CueEdge?

    /** Выбор, к которому ряд доводится; null - выбора нет или ряд ещё не разложен. */
    internal abstract val target: Any?

    /** Довести выбранный элемент в видимую часть ряда; виден - ничего. */
    internal abstract suspend fun reveal()
}

/** Подсказка ряда обычной прокрутки [state]; выбранный элемент отмечает себя [cueSelected]. */
class RowScrollCue internal constructor(val state: ScrollState) : ScrollCue() {
    /** Где выбранный элемент в содержимом ряда, px; null - не отмечен. */
    internal var selected by mutableStateOf<IntRange?>(null)

    override val scroll: ScrollableState get() = state

    override val hidden: CueEdge?
        get() {
            val span = selected ?: return null
            return when {
                span.first < state.value -> CueEdge.START
                span.last > state.value + state.viewportSize -> CueEdge.END
                else -> null
            }
        }

    override val target: Any? get() = selected?.takeIf { state.viewportSize > 0 }

    override suspend fun reveal() {
        val span = selected ?: return
        if (hidden == null) return
        state.animateScrollTo(((span.first + span.last - state.viewportSize) / 2).coerceIn(0, state.maxValue))
    }
}

/** Подсказка ленивого ряда [state]; выбранный элемент - номер [selected] ([rememberLazyRowScrollCue]). */
class LazyRowScrollCue internal constructor(val state: LazyListState) : ScrollCue() {
    /** Номер выбранного элемента ряда; null - выбора нет. */
    internal var selected by mutableStateOf<Int?>(null)

    override val scroll: ScrollableState get() = state

    override val hidden: CueEdge?
        get() {
            val index = selected ?: return null
            val layout = state.layoutInfo
            val shown = layout.visibleItemsInfo
            val item = shown.firstOrNull { it.index == index }
            return when {
                shown.isEmpty() -> null
                item == null -> if (index < shown.first().index) CueEdge.START else CueEdge.END
                item.offset < layout.viewportStartOffset -> CueEdge.START
                item.offset + item.size > layout.viewportEndOffset -> CueEdge.END
                else -> null
            }
        }

    override val target: Any? get() = selected?.takeIf { state.layoutInfo.visibleItemsInfo.isNotEmpty() }

    override suspend fun reveal() {
        val index = selected ?: return
        if (hidden != null) state.animateScrollToItem(index)
    }
}

/** Подсказка ряда обычной прокрутки [state] (его же отдают `horizontalScroll`); ряд доводится до отмеченного [cueSelected]. */
@Composable fun rememberRowScrollCue(state: ScrollState = rememberScrollState()): RowScrollCue {
    val cue = remember(state) { RowScrollCue(state) }
    RevealSelected(cue)
    return cue
}

/** Подсказка ленивого ряда [state] с выбранным элементом номер [selected] (null - выбора нет). */
@Composable fun rememberLazyRowScrollCue(state: LazyListState, selected: Int? = null): LazyRowScrollCue {
    val cue = remember(state) { LazyRowScrollCue(state) }
    SideEffect { cue.selected = selected }
    RevealSelected(cue)
    return cue
}

/** Ряд доводится до выбранного [cue], когда выбор сменился или ряд впервые разложен. */
@Composable private fun RevealSelected(cue: ScrollCue) {
    LaunchedEffect(cue) {
        snapshotFlow { cue.target }.filterNotNull().distinctUntilChanged().collect { cue.reveal() }
    }
}

/** Выбранный элемент ряда [cue]: отмечает своё место в содержимом ряда, пока [selected]. */
fun Modifier.cueSelected(cue: RowScrollCue, selected: Boolean): Modifier = if (selected) {
    onPlaced { coordinates ->
        val start = coordinates.positionInParent().x.roundToInt()
        cue.selected = start until start + coordinates.size.width
    }
} else {
    this
}

/**
 * Подсказка прокрутки [cue] поверх ряда - ставится перед его прокруткой (`horizontalScroll` или ленивым рядом): у края, за которым есть
 * ещё элементы, - затухание в цвет дорожки [track] и шеврон, акцентный с точкой, если за краем выбранный. Читает прокрутку только при
 * отрисовке: листание ряда перерисовывает подсказку, не перекомпонуя экран.
 */
fun Modifier.scrollCue(cue: ScrollCue, track: Color = Ink): Modifier = drawWithContent {
    drawContent()
    val hidden = cue.hidden
    if (cue.scroll.canScrollBackward) cueEdge(CueEdge.START, track, lit = hidden == CueEdge.START)
    if (cue.scroll.canScrollForward) cueEdge(CueEdge.END, track, lit = hidden == CueEdge.END)
}

/** Край [edge]: затухание в [track] шириной [FADE] и шеврон; [lit] - за краем выбранный: шеврон [Gold] с точкой. */
private fun DrawScope.cueEdge(edge: CueEdge, track: Color, lit: Boolean) {
    val start = edge == CueEdge.START
    val width = FADE.toPx().coerceAtMost(size.width / 2)
    val left = if (start) 0f else size.width - width
    val clear = track.copy(alpha = 0f)
    val stops = if (start) arrayOf(0f to track, SOLID to track, 1f to clear) else arrayOf(0f to clear, 1f - SOLID to track, 1f to track)
    drawRect(Brush.horizontalGradient(*stops, startX = left, endX = left + width), Offset(left, 0f), Size(width, size.height))
    val tint = if (lit) Gold else Quiet
    val arm = CHEVRON.toPx() / 2
    val pad = CHEVRON_PAD.toPx()
    val tipX = if (start) pad else size.width - pad
    val backX = if (start) tipX + arm else tipX - arm
    val midY = size.height / 2
    val stroke = CHEVRON_STROKE.toPx()
    drawLine(tint, Offset(backX, midY - arm), Offset(tipX, midY), stroke, StrokeCap.Round)
    drawLine(tint, Offset(tipX, midY), Offset(backX, midY + arm), stroke, StrokeCap.Round)
    if (lit) {
        val inset = DOT_INSET.toPx()
        val dot = Offset(if (start) inset else size.width - inset, DOT_TOP.toPx())
        drawCircle(Gold.copy(alpha = DOT_GLOW), DOT.toPx(), dot)
        drawCircle(Gold, DOT.toPx() / 2, dot)
    }
}

/** Затухание края: ширина и доля, что залита цветом дорожки целиком. */
private val FADE = 40.dp
private const val SOLID = .3f

/** Шеврон края: высота, отступ острия от края и толщина. */
private val CHEVRON = 12.dp
private val CHEVRON_PAD = 6.dp
private val CHEVRON_STROKE = 1.75.dp

/** Точка выбранного за краем: поперечник, отступ от края и от верха ряда, яркость ореола. */
private val DOT = 6.dp
private val DOT_INSET = 9.dp
private val DOT_TOP = 8.dp
private const val DOT_GLOW = .35f
