package com.sperance.exileforge.core

import com.sperance.exileforge.core.display.itemSourceIndex
import kotlin.test.Test
import kotlin.test.assertTrue

/** «Где добывается» по контенту закреплённого сервера: у каждой стопки `items.json` есть хотя бы один источник в указателе карточки. */
class StackSourcesTest {
    @Test
    fun every_stack_has_a_source() {
        val sources = itemSourceIndex(TestContent.index)
        val orphans = TestContent.index.content.items.items.map { it.code.value }.filter { it !in EXEMPT && sources[it].isNullOrEmpty() }
        assertTrue(orphans.isEmpty(), "stacks with no source on their card: $orphans")
    }

    private companion object {
        /** Стопки без источника по замыслу контента - код и причина; сейчас таких нет. */
        val EXEMPT: Map<String, String> = emptyMap()
    }
}
