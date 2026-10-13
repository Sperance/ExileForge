package com.sperance.exileforge.ui.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Нити рисования без новой [Stroke] на кадр: последние [SIZE] толщин по кругу. Рисунок, что каждый кадр берёт нить той же
 * толщины (жетоны карты, кольца), получает прежнюю. Только главный поток.
 */
object Strokes {
    private const val SIZE = 24
    private val widths = FloatArray(SIZE) { -1f }
    private val caps = arrayOfNulls<StrokeCap>(SIZE)
    private val strokes = arrayOfNulls<Stroke>(SIZE)
    private var next = 0

    fun of(width: Float, cap: StrokeCap = StrokeCap.Butt): Stroke {
        for (i in 0 until SIZE) if (widths[i] == width && caps[i] == cap) return strokes[i]!!
        val stroke = Stroke(width, cap = cap)
        widths[next] = width
        caps[next] = cap
        strokes[next] = stroke
        next = (next + 1) % SIZE
        return stroke
    }
}
