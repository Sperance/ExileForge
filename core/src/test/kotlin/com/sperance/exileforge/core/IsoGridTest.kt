package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.run.IsoGrid
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

/** Изометрия похода: обратная проекция точна, стик держит направление, вертикаль - 0.75 скорости вбок. */
class IsoGridTest {
    @Test
    fun the_inverse_undoes_the_projection() {
        listOf(0.0 to 0.0, 3.5 to -1.25, -7.0 to 12.0).forEach { (x, y) ->
            val (bx, by) = IsoGrid.inverse(IsoGrid.across(x, y), IsoGrid.down(x, y))
            assertEquals(x, bx, 1e-9)
            assertEquals(y, by, 1e-9)
        }
    }

    @Test
    fun the_stick_keeps_its_direction_and_the_vertical_runs_at_three_quarters() {
        fun screen(sx: Double, sy: Double): Pair<Double, Double> = IsoGrid.pace(sx, sy).let { (x, y) -> IsoGrid.across(x, y) to IsoGrid.down(x, y) }
        val (sideways, _) = screen(1.0, 0.0)
        val (_, upright) = screen(0.0, 1.0)
        assertEquals(IsoGrid.VERTICAL_PACE, upright / sideways, 1e-9)
        assertEquals(1.0, IsoGrid.pace(1.0, 0.0).let { hypot(it.first, it.second) }, 1e-9, "sideways - one hero speed, as before")
        assertEquals(1.5, IsoGrid.pace(0.0, -1.0).let { hypot(it.first, it.second) }, 1e-9, "upright - faster than the hero speed in cells")
        (0 until 16).map { it * Math.PI / 8 }.forEach { angle ->
            val (ax, ay) = screen(cos(angle) * .6, sin(angle) * .6)
            assertEquals(0.0, Math.IEEEremainder(atan2(ay, ax) - angle, 2 * Math.PI), 1e-9, "direction at $angle")
        }
        assertEquals(0.0 to 0.0, IsoGrid.pace(0.0, 0.0))
    }
}
