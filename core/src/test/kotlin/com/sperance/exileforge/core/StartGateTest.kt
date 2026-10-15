package com.sperance.exileforge.core

import com.sperance.exileforge.core.session.Reach
import com.sperance.exileforge.core.session.StartGate
import com.sperance.exileforge.core.session.StartInput
import com.sperance.exileforge.core.session.StartVerdict
import com.sperance.exileforge.core.session.WireFit
import com.sperance.exileforge.core.update.Wire
import kotlin.test.Test
import kotlin.test.assertEquals

class StartGateTest {
    private fun verdict(reach: Reach, vararg tweaks: (StartInput) -> StartInput) = StartGate.verdict(tweaks.fold(StartInput(reach)) { input, tweak -> tweak(input) })

    @Test
    fun holds_until_the_server_answers_the_build_is_checked_and_the_device_is_read() {
        assertEquals(StartVerdict.HOLD, verdict(Reach.CONNECTING))
        assertEquals(StartVerdict.HOLD, verdict(Reach.ANSWERED, { it.copy(checking = true) }))
        assertEquals(StartVerdict.HOLD, verdict(Reach.ANSWERED, { it.copy(deviceReady = false) }))
        assertEquals(StartVerdict.RELEASE, verdict(Reach.ANSWERED, { it.copy(fit = WireFit.MATCH) }))
    }

    @Test
    fun says_when_the_server_is_unreachable() {
        assertEquals(StartVerdict.UNREACHABLE, verdict(Reach.UNREACHABLE))
    }

    @Test
    fun a_mandatory_update_wins_over_everything_but_a_fight() {
        Reach.entries.forEach { assertEquals(StartVerdict.UPDATE, verdict(it, { input -> input.copy(mandatoryUpdate = true, fit = WireFit.GAME_BEHIND, warming = true) })) }
        assertEquals(StartVerdict.RELEASE, verdict(Reach.ANSWERED, { it.copy(mandatoryUpdate = true, busy = true) }))
    }

    @Test
    fun never_holds_the_start_longer_than_the_limit() {
        // 3.86.0: сервер ответил за 0,5 с, а окно так и стояло - предел снимает окно старта при любом состоянии.
        assertEquals(StartVerdict.HOLD, verdict(Reach.CONNECTING, { it.copy(elapsedMs = StartGate.LIMIT_MS - 1) }))
        assertEquals(StartVerdict.RELEASE, verdict(Reach.CONNECTING, { it.copy(elapsedMs = StartGate.LIMIT_MS) }))
        assertEquals(StartVerdict.RELEASE, verdict(Reach.UNREACHABLE, { it.copy(elapsedMs = StartGate.LIMIT_MS) }))
        assertEquals(StartVerdict.RELEASE, verdict(Reach.ANSWERED, { it.copy(checking = true, released = true) }))
    }

    @Test
    fun an_update_or_a_wire_that_does_not_fit_is_not_cut_by_the_limit() {
        val late: (StartInput) -> StartInput = { it.copy(elapsedMs = 10 * StartGate.LIMIT_MS, released = true) }
        assertEquals(StartVerdict.UPDATE, verdict(Reach.ANSWERED, late, { it.copy(mandatoryUpdate = true) }))
        assertEquals(StartVerdict.AWAIT_GAME, verdict(Reach.ANSWERED, late, { it.copy(fit = WireFit.GAME_BEHIND) }))
        assertEquals(StartVerdict.AWAIT_SERVER, verdict(Reach.ANSWERED, late, { it.copy(fit = WireFit.SERVER_BEHIND) }))
    }

    @Test
    fun the_hero_warm_up_is_the_same_window() {
        assertEquals(StartVerdict.WARMING, verdict(Reach.ANSWERED, { it.copy(released = true, warming = true) }))
    }

    @Test
    fun the_newer_side_is_the_one_the_other_waits_for() {
        val own = Wire(10, 5)
        assertEquals(WireFit.MATCH, WireFit.of(Wire(10, 5), own))
        assertEquals(WireFit.GAME_BEHIND, WireFit.of(Wire(11, 1), own))
        assertEquals(WireFit.GAME_BEHIND, WireFit.of(Wire(10, 6), own))
        assertEquals(WireFit.SERVER_BEHIND, WireFit.of(Wire(9, 9), own))
        assertEquals(WireFit.SERVER_BEHIND, WireFit.of(Wire(10, 4), own))
    }
}
