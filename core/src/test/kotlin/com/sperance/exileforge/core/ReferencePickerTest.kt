package com.sperance.exileforge.core

import com.sperance.exileforge.core.editor.InputSpec
import com.sperance.exileforge.core.editor.schemaFields
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.GameApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import kotlin.test.*

class ReferencePickerTest {
    @Test fun `every Mongo relationship has a typed picker`() {
        val fields = mapOf(
            "character" to mapOf("userId" to EntitySource.USER),
            "characterEquipment" to mapOf("equipmentId" to EntitySource.EQUIPMENT),
            "characterItem" to mapOf("itemId" to EntitySource.ITEM),
            "redemption" to mapOf("redemptionCodeId" to EntitySource.REDEMPTION)
        )
        fields.forEach { (schema, references) -> references.forEach { (key, source) ->
            assertEquals(InputSpec.Reference(source), schemaFields(schema).single { it.key == key }.spec)
        } }
        assertEquals(InputSpec.ListOf(InputSpec.Reference(EntitySource.RECIPE)), schemaFields("character").single { it.key == "recipeAccess" }.spec)
    }
    @Test fun `reference values retain IDs and reject invalid selections`() {
        validateForm("characterItem", buildJsonObject { put("itemId", "0123456789abcdef01234567"); put("amount", 1) })
        assertFailsWith<IllegalArgumentException> { validateForm("characterItem", buildJsonObject { put("itemId", "Item name"); put("amount", 1) }) }
    }
    @Test fun `each picker uses correct collection and server pagination`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = GameApi(server.url("/game/").toString())
            EntitySource.entries.forEach { source ->
                server.enqueue(MockResponse().setBody("""{"success":true,"data":{"items":[{"_id":"0123456789abcdef01234567","name":"Visible name"}],"page":2,"totalPages":4,"totalItems":180}}"""))
                val result = api.referencePage(source, 2)
                assertEquals(2, result.page); assertEquals(4, result.totalPages); assertEquals(180L, result.totalItems)
                assertEquals("/game/api/v1/${source.path}/paged?page=2&size=50", server.takeRequest().path)
            }
        }
    }
    @Test fun `invalid page never sends a request`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = GameApi(server.url("/").toString())
            assertFailsWith<IllegalArgumentException> { api.referencePage(EntitySource.CHARACTER, -1) }
            assertEquals(0, server.requestCount)
        }
    }
}
