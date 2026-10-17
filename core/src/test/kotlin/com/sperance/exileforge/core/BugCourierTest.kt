package com.sperance.exileforge.core

import com.sperance.exileforge.core.feedback.BugCourier
import com.sperance.exileforge.core.feedback.BugNews
import com.sperance.exileforge.core.feedback.BugOutbox
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.network.ApiFailure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Курьер отчётов: запрос в полёте не рвётся, очередь чистится до вести, за проход - одна весть. */
@OptIn(ExperimentalCoroutinesApi::class)
class BugCourierTest {
    private fun report(text: String) = BugReportRequest(text, "screen", emptyMap())

    /** Новый отчёт будит курьера, но запрос в полёте не рвётся. */
    @Test
    fun a_new_report_wakes_the_courier_without_cutting_the_request_in_flight() = runTest {
        val outbox = BugOutbox(Files.createTempDirectory("outbox").toFile())
        val news = mutableListOf<BugNews>()
        val sent = mutableListOf<String>()
        var down = true
        val inFlight = CompletableDeferred<Unit>()
        val courier = BugCourier(outbox, { r ->
            if (down) throw ApiFailure(503, null, "gateway")
            if (r.text == "a" && !inFlight.isCompleted) inFlight.await()
            assertTrue(outbox.pending().any { it.key == r.key }, "a report leaves the queue only after its answer")
            sent += r.text
        }, { news += it }, StandardTestDispatcher(testScheduler))
        backgroundScope.launch { courier.run() }
        courier.put(report("a"))
        runCurrent()
        assertEquals(listOf<BugNews>(BugNews.Queued), news, "the gateway is down: one word, the report waits")

        down = false
        advanceTimeBy(2_001)
        runCurrent()
        courier.put(report("b"))
        runCurrent()
        assertEquals(emptyList(), sent)
        inFlight.complete(Unit)
        runCurrent()
        assertEquals(listOf("a", "b"), sent, "each report once, the oldest first")
        assertEquals(listOf(BugNews.Queued, BugNews.Sent(2)), news, "one word for the pass")
        assertEquals(emptyList(), outbox.pending())
    }

    @Test
    fun a_final_refusal_leaves_the_queue_with_its_reason() = runTest {
        val outbox = BugOutbox(Files.createTempDirectory("outbox").toFile())
        val news = mutableListOf<BugNews>()
        val courier = BugCourier(outbox, { throw ApiFailure(400, "BUG_001", "too long") }, { news += it }, StandardTestDispatcher(testScheduler))
        backgroundScope.launch { courier.run() }
        courier.put(report("a"))
        runCurrent()
        assertEquals(listOf<BugNews>(BugNews.Refused("too long")), news)
        assertEquals(emptyList(), outbox.pending())
    }
}
