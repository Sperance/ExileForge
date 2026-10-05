package com.sperance.exileforge.core

import com.sperance.exileforge.core.session.Reach
import com.sperance.exileforge.core.session.StartGate
import com.sperance.exileforge.core.session.StartVerdict
import kotlin.test.Test
import kotlin.test.assertEquals

class StartGateTest {
    @Test
    fun holds_until_the_server_answers() {
        assertEquals(StartVerdict.HOLD, StartGate.verdict(Reach.CONNECTING, mandatoryUpdate = false))
    }

    @Test
    fun releases_as_soon_as_the_server_answered() {
        assertEquals(StartVerdict.RELEASE, StartGate.verdict(Reach.ANSWERED, mandatoryUpdate = false))
    }

    @Test
    fun says_when_the_server_is_unreachable() {
        assertEquals(StartVerdict.UNREACHABLE, StartGate.verdict(Reach.UNREACHABLE, mandatoryUpdate = false))
    }

    @Test
    fun a_mandatory_update_wins_over_everything() {
        Reach.entries.forEach { assertEquals(StartVerdict.UPDATE, StartGate.verdict(it, mandatoryUpdate = true)) }
    }

    @Test
    fun never_holds_longer_than_the_limit_whatever_the_state() {
        // 3.86.0: сервер ответил за 0,5 с, а окно так и стояло - предел снимает окно при любом состоянии.
        assertEquals(StartVerdict.HOLD, StartGate.verdict(Reach.CONNECTING, mandatoryUpdate = false, elapsedMs = StartGate.LIMIT_MS - 1))
        assertEquals(StartVerdict.RELEASE, StartGate.verdict(Reach.CONNECTING, mandatoryUpdate = false, elapsedMs = StartGate.LIMIT_MS))
        assertEquals(StartVerdict.RELEASE, StartGate.verdict(Reach.UNREACHABLE, mandatoryUpdate = false, elapsedMs = StartGate.LIMIT_MS))
    }

    @Test
    fun a_mandatory_update_is_not_cut_by_the_limit() {
        assertEquals(StartVerdict.UPDATE, StartGate.verdict(Reach.CONNECTING, mandatoryUpdate = true, elapsedMs = 10 * StartGate.LIMIT_MS))
    }
}
