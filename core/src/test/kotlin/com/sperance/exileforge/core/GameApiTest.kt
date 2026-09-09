package com.sperance.exileforge.core

import kotlin.test.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class GameApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GameApi
    private val id = "0123456789abcdef01234567"
    @Before fun before() { server = MockWebServer(); server.start(); api = GameApi(server.url("/").toString()) }
    @After fun after() { server.shutdown() }
    private fun ok(data: String) { server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"success":true,"data":$data}""")) }
    @Test fun `creation is an array with exact weapon discriminator`(): Unit = runBlocking {
        val document = template(Catalog.EQUIPMENT)
        ok("[${JsonObject(document + ("_id" to JsonPrimitive(id)))}]")
        assertEquals(id, api.create(Catalog.EQUIPMENT, document).entityId)
        val request = server.takeRequest()
        assertEquals("POST", request.method); assertEquals("/api/v1/equipment", request.path)
        val sent = WireJson.parseToJsonElement(request.body.readUtf8()).jsonArray.single().jsonObject
        assertEquals("features.data.equipment.equipment_data.Weapon", sent.text("type"))
        assertEquals(10.0, sent.getValue("damage_min").jsonPrimitive.double)
    }
    @Test fun `put preserves nested modifier values tags and definitions`(): Unit = runBlocking {
        val modifier = JsonObject(starterModifier() + mapOf(
            "values" to WireJson.parseToJsonElement("""[{"value":10.0},{"value":20.0}]"""),
            "tags" to buildJsonArray { add("fire"); add("custom") }
        ))
        val changes = buildJsonObject {
            put("modifiers", JsonArray(listOf(modifier)))
            put("modifierDefinitions", JsonArray(listOf(starterDefinition())))
        }
        val response = JsonObject(changes + ("_id" to JsonPrimitive(id)))
        ok(response.toString())
        assertEquals(response, api.update(Catalog.EQUIPMENT, id, changes))
        assertEquals(changes, WireJson.parseToJsonElement(server.takeRequest().body.readUtf8()))
    }
    @Test fun `get uses query id and handles null data`(): Unit = runBlocking {
        ok("null")
        assertNull(api.get(Catalog.ITEMS, id))
        assertEquals("/api/v1/items?id=$id", server.takeRequest().path)
    }
    @Test fun `put sends partial object and delete uses same query route`(): Unit = runBlocking {
        ok("""{"_id":"$id","name":"Changed"}""")
        api.update(Catalog.ITEMS, id, buildJsonObject { put("name", "Changed") })
        val put = server.takeRequest()
        assertEquals("PUT", put.method); assertEquals("/api/v1/items?id=$id", put.path)
        assertEquals("""{"name":"Changed"}""", put.body.readUtf8())
        ok("\"Deleted\""); api.delete(Catalog.ITEMS, id)
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method); assertEquals("/api/v1/items?id=$id", delete.path)
        assertEquals(0L, delete.bodySize)
    }
    @Test fun `business error with HTTP 200 must fail`(): Unit = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":false,"error":{"message":"Conflict","errorCode":"BRY_002"}}"""))
        val failure = assertFailsWith<ApiFailure> { api.get(Catalog.ITEMS, id) }
        assertEquals("BRY_002", failure.code); assertEquals(200, failure.status)
    }
    @Test fun `rate limit error preserved`(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"success":false,"error":{"message":"Wait","errorCode":"SP_004"}}"""))
        val failure = assertFailsWith<ApiFailure> { api.get(Catalog.ITEMS, id) }
        assertEquals(429, failure.status); assertEquals("SP_004", failure.code)
    }
    @Test fun `html response produces useful protocol error`(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad gateway</html>"))
        val failure = assertFailsWith<ApiFailure> { api.get(Catalog.ITEMS, id) }
        assertEquals(502, failure.status); assertTrue(failure.message!!.contains("JSON"))
    }
    @Test fun `page index is zero based`(): Unit = runBlocking {
        ok("""{"items":[],"page":0,"pageSize":20,"totalItems":0,"totalPages":0}""")
        assertEquals(0, api.page(Catalog.ITEMS, 0).page)
        assertEquals("/api/v1/items/paged?page=0&size=20", server.takeRequest().path)
    }
    @Test fun `invalid ids never reach server`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { api.delete(Catalog.ITEMS, "z".repeat(24)) }
        assertEquals(0, server.requestCount)
    }
    @Test fun `read modify diff omits system fields and type`() {
        val original = buildJsonObject { put("_id", id); put("type", "Weapon"); put("name", "old"); put("version", 2) }
        val edited = buildJsonObject { put("_id", "other"); put("type", "Armor"); put("name", "new"); put("version", 4) }
        assertEquals(setOf("name"), diff(original, edited).keys)
    }
    @Test fun `bad modifier tier is rejected`() {
        val doc = JsonObject(template(Catalog.EQUIPMENT) + ("modifiers" to buildJsonArray {
            add(buildJsonObject { starterModifier().forEach { (key, value) -> put(key, value) }; put("tier", 0) })
        }))
        assertFailsWith<IllegalArgumentException> { validate(doc, Catalog.EQUIPMENT) }
    }
    @Test fun `every starter template validates`() {
        validate(template(Catalog.ITEMS), Catalog.ITEMS)
        EquipmentKind.entries.forEach { validate(template(Catalog.EQUIPMENT, it), Catalog.EQUIPMENT) }
    }
    @Test fun `URL preserves reverse proxy prefix and rejects credentials`(): Unit = runBlocking {
        api = GameApi(server.url("/game/").toString())
        ok("null"); api.get(Catalog.ITEMS, id)
        assertEquals("/game/api/v1/items?id=$id", server.takeRequest().path)
        assertFailsWith<IllegalArgumentException> { normalizeServer("https://user:pass@example.com") }
    }
    @Test fun `protected fields cannot be updated`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { api.update(Catalog.ITEMS, id, buildJsonObject { put("type", "Armor") }) }
        assertEquals(0, server.requestCount)
    }
    @Test fun `cancellation during delayed body finishes promptly`(): Unit = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}""").setBodyDelay(3, TimeUnit.SECONDS))
        val operation = async(start = CoroutineStart.UNDISPATCHED) { api.get(Catalog.ITEMS, id) }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        withTimeout(1_000) { operation.cancelAndJoin() }
        assertTrue(operation.isCancelled)
    }

}
