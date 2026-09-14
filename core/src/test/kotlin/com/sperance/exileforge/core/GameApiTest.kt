package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.network.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.*
import java.util.concurrent.TimeUnit

class GameApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GameApi
    private val id = "0123456789abcdef01234567"
    private val journal = RequestJournal()
    @Before fun before() = runBlocking {
        server = MockWebServer(); server.start(); api = GameApi(server.url("/game/").toString(), journal)
        ok("""{"token":"private-token"}"""); api.login("name", "private-password"); server.takeRequest(); Unit
    }
    @After fun after() { server.shutdown() }
    private fun ok(data: String) { server.enqueue(MockResponse().setBody("""{"success":true,"data":$data}""")) }
    private fun failure(status: Int) { server.enqueue(MockResponse().setResponseCode(status).setBody("""{"success":false,"error":{"errorCode":"REJECTED","message":"Rejected"}}""")) }
    @Test fun `create uses server identity and auth`() = runBlocking {
        val doc = template(Catalog.EQUIPMENT)
        ok("[${JsonObject(doc + mapOf("_id" to JsonPrimitive(id), "version" to JsonPrimitive(0)))}]")
        assertEquals(id, api.create(Catalog.EQUIPMENT, doc).entityId)
        val request = server.takeRequest()
        assertEquals("Bearer private-token", request.getHeader("Authorization"))
        val sent = WireJson.parseToJsonElement(request.body.readUtf8()).jsonArray.single().jsonObject
        assertEquals("features.data.equipment.equipment_data.Weapon", sent.text("type"))
        assertFalse("_id" in sent); assertFalse("modifiers" in sent)
    }
    @Test fun `update and delete preserve exact client version`() = runBlocking {
        val version = 9007199254740993L
        val changes = buildJsonObject { put("name", "Changed") }
        ok("""{"_id":"$id","version":9007199254740994}""")
        api.update(Catalog.ITEMS, id, changes, version)
        val update = server.takeRequest()
        assertEquals("/game/api/v1/items?id=$id", update.path)
        val body = WireJson.parseToJsonElement(update.body.readUtf8()).jsonObject
        assertEquals(version, body.getValue("expectedVersion").jsonPrimitive.long)
        assertEquals(changes, body["changes"])
        ok("null"); api.delete(Catalog.ITEMS, id, version + 1)
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals(version + 1, WireJson.parseToJsonElement(delete.body.readUtf8()).jsonObject.getValue("expectedVersion").jsonPrimitive.long)
    }
    @Test fun `404 is absent but denied and other errors are preserved`() = runBlocking {
        failure(404); assertNull(api.get(Catalog.ITEMS, id)); server.takeRequest()
        for(status in listOf(403, 409, 429, 500)) {
            failure(status); assertEquals(status, assertFailsWith<ApiFailure> { api.get(Catalog.ITEMS, id) }.status); server.takeRequest()
        }
    }
    @Test fun `unauthorized response revokes token and does not retry`() = runBlocking {
        failure(401); assertEquals(401, assertFailsWith<ApiFailure> { api.page(Catalog.ITEMS, 0) }.status)
        assertFailsWith<IllegalArgumentException> { api.page(Catalog.ITEMS, 0) }
        assertEquals(2, server.requestCount)
    }
    @Test fun `stale mutation is never automatically resubmitted`() = runBlocking {
        failure(409)
        assertFailsWith<ApiFailure> { api.update(Catalog.CHARACTERS, id, buildJsonObject { put("name", "Hero") }, 4) }
        assertEquals(2, server.requestCount)
    }
    @Test fun `character cannot assign owner inventory money or stats`() = runBlocking {
        for(key in listOf("userId", "money", "level", "items", "params", "role", "version")) {
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.CHARACTERS, id, buildJsonObject { put(key, "bad") }, 0) }
        }
        assertEquals(1, server.requestCount)
        val doc = JsonObject(template(Catalog.CHARACTERS) + ("name" to JsonPrimitive("Hero")))
        ok("[${JsonObject(doc + ("_id" to JsonPrimitive(id)))}]"); api.create(Catalog.CHARACTERS, doc)
        assertEquals(setOf("name", "description"), WireJson.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonArray.single().jsonObject.keys)
    }
    @Test fun `equipment commands use instance UUID and return server stats`() = runBlocking {
        val view = """{"characterVersion":5,"equipped":{"RING_LEFT":"$id"},"inventory":[],"items":[{"itemId":"$id","amount":8}],"stats":{"version":5,"values":{"maximum_life":88.0},"weapons":{},"unsupported":["custom"]}}"""
        ok(view); val result = api.equip(id, EquipCommand(4, id, EquipmentSlot.RING_LEFT))
        assertEquals(88.0, result.stats.values["maximum_life"])
        assertEquals(listOf("custom"), result.stats.unsupported)
        val equip = server.takeRequest(); assertEquals("/game/api/v1/character/$id/equip", equip.path)
        assertEquals("RING_LEFT", WireJson.parseToJsonElement(equip.body.readUtf8()).jsonObject.text("slot"))
        ok(view); api.unequip(id, UnequipCommand(5, EquipmentSlot.RING_LEFT))
        assertEquals("/game/api/v1/character/$id/unequip", server.takeRequest().path)
    }
    @Test fun `capabilities refuse legacy server`() {
        assertFailsWith<IllegalArgumentException> { ApiCapabilities().requireCompatible() }
        ApiCapabilities(2, true, true).requireCompatible()
    }
    @Test fun `password change is redacted and clears session`() = runBlocking {
        ok("{}"); api.changePassword(ChangePasswordCommand(8, "private-old-password", "private-new-password"))
        assertEquals("/game/api/v1/user/changePassword", server.takeRequest().path)
        assertFalse(journal.entries.value.toString().contains("private-"))
        assertFailsWith<IllegalArgumentException> { api.page(Catalog.ITEMS, 0) }
    }
    @Test fun `catalog pagination uses bearer and zero based page`() = runBlocking {
        ok("""{"items":[],"page":0,"totalPages":0,"totalItems":0}""")
        assertEquals(0, api.page(Catalog.ITEMS, 0).page)
        val request = server.takeRequest(); assertEquals("/game/api/v1/items/paged?page=0&size=20", request.path)
        assertEquals("Bearer private-token", request.getHeader("Authorization"))
    }
    @Test fun `malformed server response remains a useful protocol error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Error</html>"))
        val e = assertFailsWith<ApiFailure> { api.get(Catalog.ITEMS, id) }
        assertEquals(502, e.status); assertTrue(e.message!!.contains("JSON"))
    }
    @Test fun `cancel response body promptly`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}""").setBodyDelay(3, TimeUnit.SECONDS))
        val job = async(start = CoroutineStart.UNDISPATCHED) { api.get(Catalog.ITEMS, id) }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        withTimeout(1_000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
    }
    @Test fun `invalid identity and versions never reach network`() = runBlocking {
        assertFailsWith<IllegalArgumentException> { api.delete(Catalog.ITEMS, "wrong", 0) }
        assertFailsWith<IllegalArgumentException> { api.delete(Catalog.ITEMS, id, -1) }
        assertEquals(1, server.requestCount)
    }
    @Test fun `profile is fetched with JWT subject and server role`() = runBlocking {
        val payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$id","role":"ADMIN"}""".toByteArray())
        ok("""{"token":"header.$payload.signature"}"""); api.login("user", "password"); server.takeRequest()
        ok("""{"id":"$id","version":3,"name":"Hero","login":"user","role":"USER"}""")
        val profile = api.currentUser()
        assertEquals("USER", profile.role)
        assertEquals("/game/api/v1/user?id=$id", server.takeRequest().path)
    }
    @Test fun `instance snapshot wins over edited catalog template`() {
        val instance = buildJsonObject { put("uuid", id); put("baseSnapshot", buildJsonObject { put("name", "Original"); put("slot", "RING") }) }
        val catalog = buildJsonObject { put("name", "Changed"); put("slot", "HELMET") }
        val doc = com.sperance.exileforge.core.display.inventoryDocument(instance, catalog)
        assertEquals("Original", doc.text("name")); assertEquals("RING", doc.text("slot"))
    }
    @Test fun `recipe and grants retain command versions and identities`() = runBlocking {
        val view = """{"characterVersion":8,"equipped":{},"inventory":[],"items":[],"stats":{"version":8,"values":{}}}"""
        ok(view); api.useRecipe(id, UseRecipeCommand(7, id, 12, listOf(id), 2))
        val recipe = server.takeRequest(); val sent = WireJson.parseToJsonElement(recipe.body.readUtf8()).jsonObject
        assertEquals("/game/api/v1/character/$id/useRecipe", recipe.path)
        assertEquals(12, sent.getValue("recipeVersion").jsonPrimitive.int); assertEquals(2, sent.getValue("amount").jsonPrimitive.int)
        ok(view); api.grant(id, GrantEquipmentCommand(8, id))
        assertEquals("/game/api/v1/character/inventory/itemToInventory?characterId=$id", server.takeRequest().path)
        ok(view); api.adjustItems(id, AdjustItemsCommand(9, listOf(ItemStack(id, -2))))
        assertEquals("/game/api/v1/character/inventory/addItem?characterId=$id", server.takeRequest().path)
        ok(view); api.redeem(id, RedeemCommand(10, "reward"))
        assertEquals("/game/api/v1/character/$id/redeem", server.takeRequest().path)
    }
    @Test fun `templates and slots match contract`() {
        validate(template(Catalog.ITEMS), Catalog.ITEMS)
        EquipmentKind.entries.forEach { validate(template(Catalog.EQUIPMENT, it), Catalog.EQUIPMENT) }
        assertEquals(listOf(EquipmentSlot.RING_LEFT, EquipmentSlot.RING_RIGHT), EquipmentSlot.forItem("RING"))
        assertEquals(listOf(EquipmentSlot.MAIN_HAND), EquipmentSlot.forItem("WEAPON_2H"))
        assertFailsWith<IllegalArgumentException> { normalizeServer("https://user:secret@example.com") }
    }
}
