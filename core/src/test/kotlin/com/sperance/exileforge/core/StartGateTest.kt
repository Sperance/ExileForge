package com.sperance.exileforge.core

import com.sperance.exileforge.core.session.StartGate
import com.sperance.exileforge.core.session.StartSignals
import com.sperance.exileforge.core.session.StartVerdict
import kotlin.test.Test
import kotlin.test.assertEquals

class StartGateTest {
    private val starting = StartSignals(
        startupDone = false,
        sessionBusy = true,
        contentLoading = true,
        contentReady = false,
        serverAnswered = false,
        serverFailed = false,
        running = 3,
        elapsedMs = 500,
    )

    @Test
    fun holds_while_the_start_is_under_way() {
        assertEquals(StartVerdict.HOLD, StartGate.verdict(starting))
    }

    @Test
    fun releases_once_the_start_is_settled() {
        val settled = starting.copy(startupDone = true, sessionBusy = false, contentLoading = false, contentReady = true, serverAnswered = true, running = 0)
        assertEquals(StartVerdict.RELEASE, StartGate.verdict(settled))
    }

    @Test
    fun releases_when_the_server_answered_and_nothing_runs_even_with_a_stuck_flag() {
        // Повторный запуск 3.84.2: все шаги DONE, контент есть, а флаг старта так и не поднялся - окно не ждёт вечно.
        val answered = starting.copy(serverAnswered = true, contentReady = true, contentLoading = false, sessionBusy = false, running = 0, elapsedMs = 2_000)
        assertEquals(StartVerdict.RELEASE, StartGate.verdict(answered))
    }

    @Test
    fun holds_while_a_step_still_runs_after_the_answer() {
        val reading = starting.copy(serverAnswered = true, contentReady = true, running = 1, elapsedMs = 20_000)
        assertEquals(StartVerdict.HOLD, StartGate.verdict(reading))
    }

    @Test
    fun says_unreachable_at_once_when_a_network_step_failed_before_any_answer() {
        assertEquals(StartVerdict.UNREACHABLE, StartGate.verdict(starting.copy(serverFailed = true)))
    }

    @Test
    fun says_unreachable_when_the_server_is_silent_too_long() {
        assertEquals(StartVerdict.HOLD, StartGate.verdict(starting.copy(elapsedMs = StartGate.UNREACHABLE_AFTER_MS - 1)))
        assertEquals(StartVerdict.UNREACHABLE, StartGate.verdict(starting.copy(elapsedMs = StartGate.UNREACHABLE_AFTER_MS)))
    }

    @Test
    fun a_late_failure_after_an_answer_is_not_unreachable() {
        assertEquals(StartVerdict.HOLD, StartGate.verdict(starting.copy(serverAnswered = true, serverFailed = true, elapsedMs = 30_000)))
    }
}
