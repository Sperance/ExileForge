package com.sperance.exileforge.core

import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.ConnectionEvents
import com.sperance.exileforge.core.session.RUNNER_LIMIT_MS
import com.sperance.exileforge.core.session.Stall
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommandRunnerWatchdogTest {
    private val stalls = mutableListOf<Stall>()

    private fun TestScope.runner() = CommandRunner(backgroundScope, NoConnection, { stalls += it })

    @Test
    fun a_task_that_hangs_is_cancelled_released_and_reported() = runTest {
        val runner = runner()
        runner.ready()
        var cancelled = false
        val started = runner.task {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        assertTrue(started)
        runCurrent()
        assertTrue(runner.state.value.busy)
        assertFalse(runner.task { }, "a second task is refused while the first holds")

        advanceTimeBy(RUNNER_LIMIT_MS + 1)
        runCurrent()

        assertTrue(cancelled)
        assertFalse(runner.state.value.busy)
        assertTrue(runner.state.value.error)
        assertNotNull(runner.state.value.message)
        assertEquals(listOf("task"), stalls.map { it.key })
        assertTrue("busy=true" in stalls.single().details, "the report says who held the runner")
        assertTrue(runner.task { }, "the runner takes the next task")
    }

    @Test
    fun a_visible_read_that_hangs_is_cancelled_and_a_silent_one_is_left_alone() = runTest {
        val runner = runner()
        runner.ready()
        runner.read("seen") { awaitCancellation() }
        runner.read("quiet", silent = true) { delay(2 * RUNNER_LIMIT_MS) }
        runCurrent()
        assertEquals(setOf("seen"), runner.state.value.loading)

        advanceTimeBy(RUNNER_LIMIT_MS + 1)
        runCurrent()

        assertTrue(runner.state.value.loading.isEmpty())
        assertEquals(listOf("read:seen"), stalls.map { it.key })
    }

    @Test
    fun work_within_the_limit_is_not_touched() = runTest {
        val runner = runner()
        runner.ready()
        runner.task { delay(RUNNER_LIMIT_MS - 1) }
        advanceTimeBy(RUNNER_LIMIT_MS * 2)
        runCurrent()
        assertFalse(runner.state.value.busy)
        assertTrue(stalls.isEmpty())
        assertFalse(runner.state.value.error)
    }

    @Test
    fun a_start_that_never_becomes_ready_is_released_and_reported() = runTest {
        val runner = runner()
        assertTrue(runner.state.value.starting)
        assertFalse(runner.task { }, "no task before the start is over")

        advanceTimeBy(RUNNER_LIMIT_MS + 1)
        runCurrent()

        assertFalse(runner.state.value.starting)
        assertEquals(listOf("starting"), stalls.map { it.key })
    }

    private object NoConnection : ConnectionEvents {
        override fun lost(error: Throwable?) = Unit

        override fun queued() = Unit
    }
}
