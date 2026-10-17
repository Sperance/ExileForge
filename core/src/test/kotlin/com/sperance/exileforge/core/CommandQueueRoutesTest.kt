package com.sperance.exileforge.core

import com.sperance.exileforge.core.network.CommandQueue
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Журналы с собственным повтором не ждут общей очереди: очередь положила бы их копию под своим ключом, а журнал
 * повторил бы себя под своим - на сервер уходили бы две разные команды. Обычная команда героя - в очереди.
 */
class CommandQueueRoutesTest {
    @Test fun journals_that_retry_themselves_never_wait_in_the_queue() {
        listOf("api/v1/hero/campaign/events", "api/v1/hero/trials/events", "api/v1/hero/trials/rift/act").forEach {
            assertFalse(CommandQueue.queues(it), "$it waits in the queue under a second key")
        }
        assertTrue(CommandQueue.queues("api/v1/hero/equip"))
    }
}
