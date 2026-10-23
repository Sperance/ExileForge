package com.sperance.exileforge.ui.screens.expedition.scene

import com.sperance.exileforge.rules.run.TrapLayout
import java.util.WeakHashMap

/**
 * Соседство клеток ловушки: у каждой клетки формы (по порядку [TrapLayout.cells]) - какие из восьми соседей тоже клетки этой ловушки,
 * битами [NORTH]..[NORTH_WEST]. Художник по ним сводит плиты многоклеточной ловушки в одну фигуру.
 */
internal class TrapJoints private constructor(private val joints: IntArray) {
    /** Соседи клетки номер [cell] формы. */
    operator fun get(cell: Int): Int = joints[cell]

    companion object {
        const val NORTH = 1
        const val EAST = 2
        const val SOUTH = 4
        const val WEST = 8
        const val NORTH_EAST = 16
        const val SOUTH_EAST = 32
        const val SOUTH_WEST = 64
        const val NORTH_WEST = 128

        /** Сколько соседей у клетки: бит номер `i` - сосед со сдвигом ([dx], [dy]). */
        const val SIDES = 8

        private val DX = intArrayOf(0, 1, 0, -1, 1, 1, -1, -1)
        private val DY = intArrayOf(-1, 0, 1, 0, -1, 1, 1, -1)

        /** Сдвиг по x соседа за битом номер [bit]. */
        fun dx(bit: Int): Int = DX[bit]

        /** Сдвиг по y соседа за битом номер [bit]. */
        fun dy(bit: Int): Int = DY[bit]

        /** Соседство клеток ловушки [layout]. */
        fun of(layout: TrapLayout): TrapJoints {
            val cells = layout.cells
            return TrapJoints(
                IntArray(cells.size) { i ->
                    val cell = cells[i]
                    (0 until SIDES).fold(0) { joints, bit ->
                        val x = cell.x + DX[bit]
                        val y = cell.y + DY[bit]
                        if (cells.any { it.x == x && it.y == y }) joints or (1 shl bit) else joints
                    }
                },
            )
        }
    }
}

/** Соседство клеток ловушек сцены: считается раз на ловушку и уходит вместе с её картой. */
internal class TrapJointsCache {
    private val joints = WeakHashMap<TrapLayout, TrapJoints>()

    /** Соседство клеток ловушки [layout], посчитанное при первом обращении. */
    fun of(layout: TrapLayout): TrapJoints = joints.getOrPut(layout) { TrapJoints.of(layout) }
}
