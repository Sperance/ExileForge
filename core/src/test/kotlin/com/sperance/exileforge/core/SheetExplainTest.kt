package com.sperance.exileforge.core

import com.sperance.exileforge.core.character.Sheets
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** A figure's window must add up to the figure: the formula of its shares plus the shifts over it. */
class SheetExplainTest {
    @Test fun breakdownAddsUpToTheSheet() {
        val index = TestContent.index
        index.classes.classes.forEach { heroClass ->
            val sheet = Sheets.calculate(index, 20, heroClass.code, emptyList(), emptyList())
            val explainer = sheet.model!!.explainer
            sheet.stats.forEach { (stat, value) ->
                val b = explainer.explain(stat)
                assertTrue(abs(b.total - value) < 0.05, "${heroClass.code} $stat total ${b.total} != $value")
                assertTrue(abs(b.formed + b.shifts.sumOf { it.delta } - value) < 0.15, "${heroClass.code} $stat formed ${b.formed} + shifts != $value")
            }
        }
    }
}
