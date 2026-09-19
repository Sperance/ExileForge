package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.network.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class GameApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GameApi
    private val id = "0123456789abcdef01234567"
    private val other = "89abcdef0123456701234567"
    private val journal = RequestJournal()

    @Before fun before(): Unit = runBlocking {
        server = MockWebServer(); server.start(); api = GameApi(server.url("/game/").toString(), journal)
        ok("""{"id":"$id","version":1,"name":"Admin","login":"admin","role":"ADMIN","isActive":true}""")
        api.login("admin", "private-password"); server.takeRequest(); Unit
    }
    @After fun after() { server.shutdown() }
    private fun ok(data: String) { server.enqueue(MockResponse().setBody("""{"success":true,"data":$data}""")) }
    private fun failure(status: Int) { server.enqueue(MockResponse().setResponseCode(status).setBody("""{"success":false,"error":{"errorCode":"REJECTED","message":"Rejected"}}""")) }

    @Test fun `login answers with the account itself and never records the password`(): Unit = runBlocking {
        assertEquals("ADMIN", assertNotNull(api.currentUser()).role)
        assertFalse(journal.entries.value.toString().contains("private-password"))
        // Signing out drops the whole session: there is no token to fall back on.
        api.logout()
        assertNull(api.currentUser())
        assertFailsWith<IllegalArgumentException> { api.page(Catalog.ITEMS, 0) }
        assertEquals(1, server.requestCount)
    }

    @Test fun `create posts an array without identity and keeps the discriminator`(): Unit = runBlocking {
        val document = template(Catalog.EQUIPMENT)
        ok("[${JsonObject(document + mapOf("_id" to JsonPrimitive(id), "version" to JsonPrimitive(0)))}]")
        assertEquals(id, api.create(Catalog.EQUIPMENT, document).entityId)
        val request = server.takeRequest()
        assertEquals("/game/api/v1/equipment", request.path)
        val sent = WireJson.parseToJsonElement(request.body.readUtf8()).jsonArray.single().jsonObject
        assertEquals("features.data.equipment.equipment_data.Weapon", sent.text("type"))
        assertFalse("_id" in sent); assertFalse("version" in sent); assertFalse("price" in sent)
    }

    @Test fun `update sends only the changed fields and delete carries no body`(): Unit = runBlocking {
        val changes = buildJsonObject { put("name", "Changed") }
        ok("""{"_id":"$id","version":7,"name":"Changed"}""")
        assertEquals(7L, api.update(Catalog.ITEMS, id, changes).entityVersion)
        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/game/api/v1/items?id=$id", update.path)
        assertEquals(changes, WireJson.parseToJsonElement(update.body.readUtf8()).jsonObject)
        ok("\"Deleted\""); api.delete(Catalog.ITEMS, id)
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/game/api/v1/items?id=$id", delete.path)
        assertEquals(0, delete.body.size)
    }

    @Test fun `server owned fields never reach a write`(): Unit = runBlocking {
        for (key in listOf("_id", "version", "type", "deleted", "userId", "money", "level", "params")) {
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.CHARACTERS, id, buildJsonObject { put(key, "bad") }) }
        }
        assertFailsWith<IllegalArgumentException> { api.update(Catalog.ITEMS, id, JsonObject(emptyMap())) }
        assertEquals(1, server.requestCount)
    }

    @Test fun `an equipment template carries references, never rolled values`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, id, buildJsonObject { put("modifierIds", buildJsonArray { add("not-an-id") }) }) }
        assertFailsWith<IllegalArgumentException> { api.create(Catalog.EQUIPMENT, JsonObject(template(Catalog.EQUIPMENT) + ("params" to JsonArray(emptyList())))) }
        ok("""{"_id":"$id","version":2}""")
        api.update(Catalog.EQUIPMENT, id, buildJsonObject { put("modifierIds", buildJsonArray { add(other) }) })
        assertEquals(2, server.requestCount)
    }

    @Test fun `a list is paged on the client, because the server's page route answers with nothing`(): Unit = runBlocking {
        // /paged passes `page` as the limit and `size` as the offset, so page 0 returns an empty list.
        val records = (1..25).map { buildJsonObject { put("_id", id); put("name", "Item $it") } }
        ok(JsonArray(records).toString())
        val second = api.page(Catalog.ITEMS, 1)
        assertEquals("/game/api/v1/items", server.takeRequest().path)
        assertEquals(1, second.page)
        assertEquals(2, second.totalPages)
        assertEquals(25L, second.totalItems)
        assertEquals(5, second.items.size)
        assertEquals("Item 21", second.items.first().text("name"))
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.page(Catalog.ITEMS, -1) }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `a filtered search narrows the collection the server cannot filter`(): Unit = runBlocking {
        val bows = (1..3).map { buildJsonObject { put("_id", id); put("name", "Bow $it"); put("slot", "WEAPON_2H"); put("rarity", "RARE"); put("itemLevel", 40) } }
        val ring = buildJsonObject { put("_id", other); put("name", "Ring"); put("slot", "RING"); put("rarity", "RARE"); put("itemLevel", 40) }
        ok(JsonArray(bows + ring).toString())
        val result = api.search(Catalog.EQUIPMENT, 0, CatalogFilter(slot = "WEAPON_2H"))
        assertEquals("/game/api/v1/equipment", server.takeRequest().path)
        assertEquals(3, result.items.size)
        assertEquals(3L, result.totalItems)
        assertTrue(result.items.all { it.text("slot") == "WEAPON_2H" })
        // A level bound outside the data leaves nothing, and still reports an honest page count.
        ok(JsonArray(bows).toString())
        assertEquals(0, api.search(Catalog.EQUIPMENT, 0, CatalogFilter(minLevel = "80")).totalPages)
    }

    @Test fun `a random grant picks a matching template and lets the server roll it`(): Unit = runBlocking {
        val templates = JsonArray(listOf(
            buildJsonObject { put("_id", id); put("name", "Epic helm"); put("slot", "HELMET"); put("rarity", "EPIC") },
            buildJsonObject { put("_id", other); put("name", "Rare helm"); put("slot", "HELMET"); put("rarity", "RARE") },
            buildJsonObject { put("_id", other); put("name", "Epic ring"); put("slot", "RING"); put("rarity", "EPIC") }))
        ok(templates.toString())
        val chosen = api.randomTemplate("EPIC", "HELMET", Random(1))
        assertEquals("Epic helm", chosen.text("name"))
        assertEquals("/game/api/v1/equipment", server.takeRequest().path)
        // The rolls are the server's: the client only names the base it wants an instance of.
        ok("""{"_id":"$id","characterId":"$other","equipmentId":"$id","params":[{"modifierId":"$other","tierId":"$id","tier":3,"values":[42.0]}]}""")
        val instance = api.grant(other, chosen.entityId)
        assertEquals(3, instance.params.single().tier)
        assertEquals(listOf(42.0), instance.params.single().values)
        assertEquals("/game/api/v1/character/inventory/itemToInventory?characterId=$other&equipmentId=$id", server.takeRequest().path)
        // Nothing matching means no request at all, and an unknown choice is refused up front.
        ok(templates.toString())
        assertFailsWith<IllegalArgumentException> { api.randomTemplate("MYTHICAL", "HELMET") }
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.randomTemplate("SHINY", "") }
        assertFailsWith<IllegalArgumentException> { api.randomTemplate("", "POCKET") }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `equipment is worn by instance id and the slot stays the template's`(): Unit = runBlocking {
        val worn = """{"_id":"$id","characterId":"$other","equipmentId":"$other","equippedSlot":"RING","params":[]}"""
        ok(worn)
        assertEquals("RING", api.equip(other, id).equippedSlot)
        assertEquals("/game/api/v1/characterequipment/equip?characterId=$other&inventoryId=$id", server.takeRequest().path)
        ok("""{"_id":"$id","characterId":"$other","equipmentId":"$other","params":[]}""")
        assertFalse(api.unequip(other, id).equipped)
        assertEquals("/game/api/v1/characterequipment/unequip?characterId=$other&inventoryId=$id", server.takeRequest().path)
    }

    @Test fun `stats and the bag come from the server as they are`(): Unit = runBlocking {
        ok("""{"STOCK_HEALTH":188.4,"STOCK_ARMOR":40.0}""")
        assertEquals(188.4, api.stats(id).getValue("STOCK_HEALTH"))
        assertEquals("/game/api/v1/character/inventory/stats?characterId=$id", server.takeRequest().path)
        ok("""[{"itemId":"chaos_orb","amount":50}]""")
        assertEquals(50L, api.bag(id).single().amount)
        assertEquals("/game/api/v1/character/inventory/items?characterId=$id", server.takeRequest().path)
        ok("\"Success\"")
        assertEquals("Success", api.adjustItems(id, listOf(ItemStack("chaos_orb", -2))))
        val adjust = server.takeRequest()
        assertEquals("/game/api/v1/character/inventory/addItem?characterId=$id", adjust.path)
        assertEquals(-2, WireJson.parseToJsonElement(adjust.body.readUtf8()).jsonArray.single().jsonObject.getValue("amount").jsonPrimitive.int)
        assertFailsWith<IllegalArgumentException> { ItemStack("chaos_orb", 0) }
    }

    @Test fun `capabilities are read from the server's own route table`(): Unit = runBlocking {
        val routes = listOf("GET" to "/api/v1/user/login", "GET" to "/api/v1/equipment/paged", "GET" to "/api/v1/character/inventory/equipments",
            "GET" to "/api/v1/character/inventory/stats", "POST" to "/api/v1/character/inventory/itemToInventory",
            "POST" to "/api/v1/characterequipment/equip", "GET" to "/api/v1/modifierdefinition")
        // The server prints the Ktor selector, so a method arrives as "(GET)".
        ok(JsonArray(routes.map { buildJsonObject { put("path", it.second); put("method", "(${it.first})") } }).toString())
        val capabilities = api.capabilities()
        capabilities.requireWorkbench()
        assertTrue(capabilities.has("GET", "/api/v1/user/login"))
        assertEquals("/game/system/routes", server.takeRequest().path)
        assertFailsWith<IllegalArgumentException> { ApiCapabilities.of(listOf(RouteInfo("/api/v1/user/login", "(GET)"))).requireWorkbench() }
    }

    @Test fun `a server answer is never reported as a lost connection`() {
        // ApiFailure extends IOException; classifying it as Offline would hide what the server said.
        val rejected = FailureState.from(ApiFailure(404, "SP_001", "Not find endpoint /system/health"), writing = false)
        assertEquals(FailureState.Rejected("Not find endpoint /system/health"), rejected)
        assertEquals(FailureState.SessionExpired, FailureState.from(ApiFailure(401, null, "no"), writing = false))
        assertEquals(FailureState.Forbidden, FailureState.from(ApiFailure(403, null, "no"), writing = false))
        assertEquals(FailureState.Conflict, FailureState.from(ApiFailure(409, null, "no"), writing = false))
        assertEquals(FailureState.Rejected("no"), FailureState.from(ApiFailure(500, null, "no"), writing = false))
        assertEquals(FailureState.UncertainWrite, FailureState.from(ApiFailure(500, null, "no"), writing = true))
        assertEquals(FailureState.Rejected("no"), FailureState.from(ApiFailure(400, null, "no"), writing = true))
        // Only a transport failure is an absent connection.
        assertEquals(FailureState.Offline, FailureState.from(java.net.ConnectException("refused"), writing = false))
        assertEquals(FailureState.UncertainWrite, FailureState.from(java.net.ConnectException("refused"), writing = true))
    }

    @Test fun `a transport failure names itself instead of blaming the network`() {
        // Android's cleartext block is the one that looks exactly like "no connection" but is config.
        val blocked = java.net.UnknownServiceException("CLEARTEXT communication to 10.0.2.2 not permitted by network security policy")
        assertTrue(transportDetail(blocked).contains("открытый HTTP"), transportDetail(blocked))
        assertTrue(transportDetail(java.net.ConnectException("Failed to connect to /10.0.2.2:8080")).contains("10.0.2.2:8080"))
        assertTrue(transportDetail(java.net.UnknownHostException("example.invalid")).contains("example.invalid"))
        assertTrue(transportDetail(java.net.SocketTimeoutException("timeout")).contains("timeout"))
        // A failure with no message still says which one it was.
        assertEquals("EOFException", transportDetail(java.io.EOFException()))
    }

    @Test fun `404 is absent, other errors are preserved`(): Unit = runBlocking {
        failure(404); assertNull(api.get(Catalog.ITEMS, id)); server.takeRequest()
        for (status in listOf(400, 403, 429, 500)) {
            failure(status); assertEquals(status, assertFailsWith<ApiFailure> { api.get(Catalog.ITEMS, id) }.status); server.takeRequest()
        }
    }

    @Test fun `unauthorized response clears the session and is never retried`(): Unit = runBlocking {
        failure(401)
        assertEquals(401, assertFailsWith<ApiFailure> { api.page(Catalog.ITEMS, 0) }.status)
        assertFailsWith<IllegalArgumentException> { api.page(Catalog.ITEMS, 0) }
        assertEquals(2, server.requestCount)
    }

    @Test fun `a rejected write is never automatically resubmitted`(): Unit = runBlocking {
        failure(400)
        assertFailsWith<ApiFailure> { api.update(Catalog.CHARACTERS, id, buildJsonObject { put("name", "Hero") }) }
        assertEquals(2, server.requestCount)
    }

    @Test fun `password change hides both secrets and ends the session`(): Unit = runBlocking {
        ok("\"Success\""); api.changePassword("private-old", "private-New1")
        assertEquals("/game/api/v1/user/changePassword", server.takeRequest().path!!.substringBefore('?'))
        assertFalse(journal.entries.value.toString().contains("private-"))
        assertNull(api.currentUser())
    }

    @Test fun `malformed server response remains a useful protocol error`(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Error</html>"))
        val e = assertFailsWith<ApiFailure> { api.get(Catalog.ITEMS, id) }
        assertEquals(502, e.status); assertTrue(e.message!!.contains("JSON"))
    }

    @Test fun `a cancelled request stops reading the body`(): Unit = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}""").setBodyDelay(3, TimeUnit.SECONDS))
        val job = async(start = CoroutineStart.UNDISPATCHED) { api.get(Catalog.ITEMS, id) }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        withTimeout(1_000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
    }

    @Test fun `invalid identity never reaches the network`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { api.delete(Catalog.ITEMS, "wrong") }
        assertFailsWith<IllegalArgumentException> { api.grant("wrong", id) }
        assertFailsWith<IllegalArgumentException> { api.equip(id, "wrong") }
        assertFailsWith<IllegalArgumentException> { api.stats("wrong") }
        assertEquals(1, server.requestCount)
    }

    @Test fun `templates match the contract the server validates`() {
        validate(template(Catalog.ITEMS), Catalog.ITEMS)
        EquipmentKind.entries.forEach { kind ->
            val document = template(Catalog.EQUIPMENT, kind)
            validate(document, Catalog.EQUIPMENT)
            assertEquals(kind, EquipmentKind.of(document.text("type")))
        }
        assertFailsWith<IllegalArgumentException> { normalizeServer("https://user:secret@example.com") }
    }
}
