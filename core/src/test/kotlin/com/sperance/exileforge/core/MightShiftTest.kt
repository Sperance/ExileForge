package com.sperance.exileforge.core

import com.sperance.exileforge.core.character.MightShift
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Сдвиг Мощи (0.14.5, #44): одно правило показа - шум и непосчитанная Мощь не пишутся нигде. */
class MightShiftTest {
    @Test
    fun only_a_counted_and_real_shift_is_shown() {
        assertTrue(MightShift(10_000.0, 10_100.0).shown)
        assertTrue(MightShift(10_000.0, 9_900.0).shown)
        assertFalse(MightShift(10_000.0, 10_040.0).shown, "under half a percent is rounding noise")
        assertFalse(MightShift(0.0, 5_000.0).shown, "no base - nothing to compare with")
    }
}
