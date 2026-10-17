package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.sync.HeroPart
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Чтение героя по версии: часть, что сервер считает по часам (витрина торговца), со сроком в прошлом шлёт чтение без `If-None-Match`. */
class HeroPartsTest {
    private fun held(refreshAt: Long): HeroParts {
        val empty = HeroPart("1", JsonObject(emptyMap()))
        val parts = HeroParts.NAMES.associateWith { empty } + (HeroParts.MERCHANT to HeroPart("2", JsonObject(mapOf("refreshAt" to JsonPrimitive(refreshAt)))))
        return HeroParts("hero").merge(HeroSnapshot("7", parts), fresh = false)
    }

    @Test
    fun a_shelf_past_its_refresh_is_read_without_the_version() {
        assertTrue(held(refreshAt = NOW + 1).conditional(NOW))
        assertFalse(held(refreshAt = NOW).conditional(NOW))
        assertFalse(held(refreshAt = NOW - 1).conditional(NOW))
    }

    private companion object {
        const val NOW = 1_000_000L
    }
}
