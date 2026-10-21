package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.campaign.RunState
import com.sperance.exileforge.core.model.sync.HeroPart
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.rules.run.RunContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Чтение героя по версии: часть, что сервер считает по часам (витрина торговца), со сроком в прошлом шлёт чтение без `If-None-Match`.
 * Заход едет своей частью `run` и ложится в `campaign.run`; без неё герой не полон.
 */
class HeroPartsTest {
    private fun held(refreshAt: Long = NOW + 1, run: JsonElement = JsonNull): HeroParts {
        val empty = HeroPart("1", JsonObject(emptyMap()))
        val parts = HeroParts.NAMES.associateWith { empty } +
            (HeroParts.MERCHANT to HeroPart("2", JsonObject(mapOf("refreshAt" to JsonPrimitive(refreshAt))))) +
            (HeroParts.RUN to HeroPart("3", run))
        return HeroParts("hero").merge(HeroSnapshot("7", parts), fresh = false)
    }

    @Test
    fun a_shelf_past_its_refresh_is_read_without_the_version() {
        assertTrue(held(refreshAt = NOW + 1).conditional(NOW))
        assertFalse(held(refreshAt = NOW).conditional(NOW))
        assertFalse(held(refreshAt = NOW - 1).conditional(NOW))
    }

    @Test
    fun the_run_part_lands_in_the_campaign() {
        assertNull(held().campaign.run)
        val run = RunState("r1", 5L, "Z1", RunContext("warrior", 3))
        assertEquals(run, held(run = WireJson.encodeToJsonElement(RunState.serializer(), run)).campaign.run)
    }

    @Test
    fun a_hero_without_the_run_part_is_not_complete() {
        val parts = HeroParts.NAMES.filter { it != HeroParts.RUN }.associateWith { HeroPart("1", JsonObject(emptyMap())) }
        assertFalse(HeroParts("hero").merge(HeroSnapshot("7", parts), fresh = false).complete)
        assertTrue(held().complete)
    }

    private companion object {
        const val NOW = 1_000_000L
    }
}
