package com.sperance.exileforge.core

import com.sperance.exileforge.core.crafts.CraftCycle
import com.sperance.exileforge.core.crafts.minus
import com.sperance.exileforge.core.crafts.plus
import com.sperance.exileforge.core.model.crafts.JobExtra
import com.sperance.exileforge.core.model.crafts.JobInput
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.WorkBonus
import com.sperance.exileforge.core.model.crafts.WorkGains
import org.junit.Test
import kotlin.test.assertEquals

/** A cycle thrown here (2.47.0) is the server's cycle: the same seed and number, the same generator, the same order. */
class CraftCycleTest {

    private val ore = JobView("ORE", nothing = 0.0, output = "IRON_ORE", experience = 10.0, extra = listOf(JobExtra("GEM", 100.0)))

    @Test fun theGeneratorIsTheServersOwn() {
        // The same literal stands in the server's CraftsTest: both sides draw this number first for cycle 3 of seed 42.
        assertEquals(PROBE, CraftCycle.random(42, 3).nextDouble())
    }

    @Test fun aCycleIsItsSeedAndNumber() {
        assertEquals(CraftCycle.roll(7, 11, ore, WorkBonus(yield = 50.0)), CraftCycle.roll(7, 11, ore, WorkBonus(yield = 50.0)))
        assertEquals(1, CraftCycle.roll(1, 0, ore.copy(nothing = 100.0), WorkBonus()).nothing)
        val sure = CraftCycle.roll(1, 0, ore, WorkBonus(yield = 100.0, experience = 50.0))
        assertEquals(mapOf("IRON_ORE" to 2L, "GEM" to 1L), sure.items)
        assertEquals(15.0, sure.experience)
        val craft = CraftCycle.roll(1, 0, ore.copy(inputs = listOf(JobInput("IRON_ORE", 3))), WorkBonus(), listOf("FLUX"))
        assertEquals(mapOf("IRON_ORE" to 3L, "FLUX" to 1L), craft.spent)
    }

    @Test fun talliesAddAndSubtractStackByStack() {
        val a = WorkGains(cycles = 2, items = mapOf("A" to 3L))
        val b = WorkGains(cycles = 1, items = mapOf("A" to 1L, "B" to 2L))
        assertEquals(mapOf("A" to 4L, "B" to 2L), (a + b).items)
        assertEquals(mapOf("A" to 2L, "B" to -2L), (a - b).items)
        assertEquals(WorkGains().items, (a - a).items)
    }

    private companion object { const val PROBE = 0.9186378747095982 }
}
