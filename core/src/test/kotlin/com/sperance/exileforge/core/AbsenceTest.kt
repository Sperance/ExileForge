package com.sperance.exileforge.core

import com.sperance.exileforge.core.crafts.Absence
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.rules.roll.ActiveWork
import com.sperance.exileforge.rules.roll.WorkGains
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Отлучка ремёсел: досчёт с циклами, покрывший не меньше `awayMinutes` правил, - лист «Пока вас не было»; короче или пустой - нет. */
class AbsenceTest {
    private val rules = TestContent.index.professions.rules
    private val away = (rules.awayMinutes * MINUTE).toLong()
    private val before = ActiveWork("SMITHING", "IRON_FORGING", startedAt = 0, settledAt = 0)

    private fun answer(span: Long, cycles: Int) = CraftsState(now = NOW, rules = rules, gains = WorkGains(cycles = cycles), from = NOW - span)

    @Test
    fun aLongCatchUpWithCyclesIsAnAbsence() {
        val absence = assertNotNull(Absence.of(answer(away, cycles = 3), before))
        assertEquals(away, absence.millis)
        assertEquals("IRON_FORGING", absence.job)
        assertFalse(absence.running)
    }

    @Test
    fun aShortOrEmptyCatchUpIsNot() {
        assertNull(Absence.of(answer(away - 1, cycles = 3), before))
        assertNull(Absence.of(answer(away, cycles = 0), before))
    }

    private companion object {
        const val MINUTE = 60_000.0
        const val NOW = 10_000_000_000L
    }
}
