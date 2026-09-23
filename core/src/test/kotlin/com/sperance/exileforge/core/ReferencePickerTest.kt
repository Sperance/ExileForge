package com.sperance.exileforge.core

import com.sperance.exileforge.core.editor.InputSpec
import com.sperance.exileforge.core.editor.schemaFields
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.GameApi
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class ReferencePickerTest {
    private val id = "0123456789abcdef01234567"

    private suspend fun signedIn(server: MockWebServer): GameApi {
        val api = GameApi(server.url("/game/").toString())
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"user":{"id":"$id","name":"Admin","login":"admin","role":"ADMIN","isActive":true},"token":"token-0123456789abcdef"}}"""))
        api.login("admin", "password"); server.takeRequest()
        return api
    }

    @Test fun `the modifier pool is a typed picker into its own collection`() {
        val pool = schemaFields("equipment").single { it.key == "modifierIds" }.spec
        assertEquals(InputSpec.ListOf(InputSpec.Reference(EntitySource.MODIFIER)), pool)
        // The base stats left the character document in 0.10.0: the class carries them now.
        assertEquals(setOf("name", "description", "professionSkills", "battleSkills", "boolSkills"), schemaFields("character").map { it.key }.toSet())
        // An item's base is fixed modifiers, so it picks from the same collection as the pool.
        val base = schemaFields("fixedModifier").single { it.key == "modifierId" }.spec
        assertEquals(InputSpec.Reference(EntitySource.MODIFIER), base)
    }

    @Test fun `reference values retain identifiers and reject names`() {
        validateForm("equipment", buildJsonObject { put("modifierIds", buildJsonArray { add(id) }) })
        assertFailsWith<IllegalArgumentException> { validateForm("equipment", buildJsonObject { put("modifierIds", buildJsonArray { add("Maximum life") }) }) }
    }

    @Test fun `each picker reads its own collection and pages it here`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = signedIn(server)
            val records = JsonArray((1..120).map { buildJsonObject { put("_id", id); put("name", "Record $it") } })
            EntitySource.entries.forEach { source ->
                server.enqueue(MockResponse().setBody("""{"success":true,"data":$records}"""))
                val result = api.referencePage(source, 2)
                assertEquals(2, result.page); assertEquals(3, result.totalPages); assertEquals(120L, result.totalItems)
                assertEquals(20, result.items.size)
                assertEquals("/game/api/v1/${source.path}", server.takeRequest().path)
            }
        }
    }

    @Test fun `a search reads the collection the server cannot narrow`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = signedIn(server)
            val records = JsonArray(listOf("Iron Ring", "Coral Ring", "Iron Helm").map { name -> buildJsonObject { put("_id", id); put("name", name) } })
            server.enqueue(MockResponse().setBody("""{"success":true,"data":$records}"""))
            val result = api.referencePage(EntitySource.EQUIPMENT, 0, "iron")
            assertEquals("/game/api/v1/equipment", server.takeRequest().path)
            assertEquals(2L, result.totalItems)
        }
    }

    @Test fun `an invalid page never sends a request`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = signedIn(server)
            assertFailsWith<IllegalArgumentException> { api.referencePage(EntitySource.CHARACTER, -1) }
            assertEquals(1, server.requestCount)
        }
    }
}
