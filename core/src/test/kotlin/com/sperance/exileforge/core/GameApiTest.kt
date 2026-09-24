package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.campaign.MapRule
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
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
    private val token = "private-token-0123456789abcdef"

    @Before fun before(): Unit = runBlocking {
        server = MockWebServer(); server.start(); api = GameApi(server.url("/game/").toString(), journal)
        // The app always has the server's dictionary by the time it reads anything: documents carry
        // codes since 0.14.0, so without it every assertion below would be about a code.
        serverLocale = LocaleBundle.parse("ru", "sha", """{
            "equipment.IRON_SKULLCAP.name": "Iron Skullcap", "item.CHAOS_ORB.name": "Chaos Orb",
            "class.MARAUDER.name": "Marauder", "class.MARAUDER.description": "Сила",
            "currency.chaos": "{0}: перекатаны аффиксы, всего {1}"}""")
        ok("""{"user":{"id":"$id","version":1,"name":"Admin","login":"admin","role":"ADMIN","isActive":true},"token":"$token"}""")
        api.login("admin", "private-password"); server.takeRequest(); Unit
    }
    @After fun after() { server.shutdown(); serverLocale = LocaleBundle() }
    private fun ok(data: String) { server.enqueue(MockResponse().setBody("""{"success":true,"data":$data}""")) }
    private fun failure(status: Int) { server.enqueue(MockResponse().setResponseCode(status).setBody("""{"success":false,"error":{"errorCode":"REJECTED","message":"Rejected"}}""")) }
    private fun refusal(status: Int, code: String, message: String) {
        server.enqueue(MockResponse().setResponseCode(status).setBody("""{"success":false,"error":{"errorCode":"$code","message":"$message"}}"""))
    }

    @Test fun `a known device signs straight in and nothing is registered`(): Unit = runBlocking {
        api.logout()
        val sent = server.requestCount
        ok("""{"user":{"id":"$other","version":3,"name":"","login":"","role":"USER","isActive":true,"countCharacters":2},"token":"device-token-0123456789"}""")
        val profile = api.loginByDevice("device-uuid")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/game/api/v1/user/login/byDeviceId", request.path)
        assertEquals("""{"deviceId":"device-uuid"}""", request.body.readUtf8())
        assertEquals(other, profile.id)
        assertEquals(2, profile.countCharacters)
        // The session is the token, exactly as a password login leaves it.
        assertEquals(other, assertNotNull(api.currentUser()).id)
        assertEquals("device-token-0123456789", api.sessionToken())
        // One request: an account that exists is never re-registered.
        assertEquals(sent + 1, server.requestCount)
    }

    @Test fun `an unknown device is registered on the spot`(): Unit = runBlocking {
        api.logout()
        // US_015 is not an error to report — it is the server saying "this one is new".
        refusal(404, "US_015", "User with deviceId device-uuid not found")
        ok("""{"user":{"id":"$other","version":0,"name":"","login":"","role":"USER","isActive":true},"token":"device-token-0123456789"}""")
        assertEquals(other, api.loginByDevice("device-uuid").id)
        assertEquals("/game/api/v1/user/login/byDeviceId", server.takeRequest().path)
        val registration = server.takeRequest()
        assertEquals("POST", registration.method)
        assertEquals("/game/api/v1/user/byDeviceId", registration.path)
        assertEquals("""{"deviceId":"device-uuid"}""", registration.body.readUtf8())
    }

    @Test fun `any other device refusal is reported, not registered around`(): Unit = runBlocking {
        api.logout()
        val sent = server.requestCount
        // A disabled account must not be quietly replaced by a fresh one under the same device.
        refusal(400, "US_011", "Account is inactive")
        assertFailsWith<ApiFailure> { api.loginByDevice("device-uuid") }
        assertEquals(sent + 1, server.requestCount)
        assertNull(api.currentUser())
        // A device the client could not identify never reaches the network.
        assertFailsWith<IllegalArgumentException> { api.loginByDevice("  ") }
        assertEquals(sent + 1, server.requestCount)
    }

    @Test fun `the icon set is fetched by its fingerprint, outside the envelope`(): Unit = runBlocking {
        // Static content like the dictionaries: plain JSON, no {success,data}, no account needed.
        server.enqueue(MockResponse().setBody("""{"hash":"cc20a339","file":"icons.json","sprites":2,"icons":3}"""))
        val manifest = api.files.iconManifest()
        assertEquals("/game/icons/index.json", server.takeRequest().path)
        assertEquals("cc20a339", manifest.hash)
        assertEquals(3, manifest.icons)

        server.enqueue(MockResponse().setBody("""{"sprites":{"orb":{"viewBox":24,"paths":[{"d":"M1 1h2v2h-2z","alpha":1.0}]}},
            "icons":{"item.CHAOS_ORB":"orb"}}"""))
        val bundle = IconBundle.parse(manifest.hash, api.files.iconDocument(manifest.file))
        assertEquals("/game/icons/icons.json", server.takeRequest().path)
        assertEquals("cc20a339", bundle.hash)
        assertNotNull(bundle[IconKey.item("CHAOS_ORB")])
        // The manifest names the file, so a renamed set is still fetched; a blank name is refused.
        assertFailsWith<IllegalArgumentException> { api.files.iconDocument("") }
    }

    @Test fun `a portrait is fetched by its key, each file on its own`(): Unit = runBlocking {
        server.enqueue(MockResponse().setBody("""{"hash":"ab12","width":300,"height":400,"portraits":{"class.WITCH":"f00d","form.BAT":"beef"}}"""))
        val manifest = api.files.portraitManifest()
        assertEquals("/game/portraits/index.json", server.takeRequest().path)
        assertEquals(setOf("class.WITCH", "form.BAT"), manifest.portraits.keys)

        server.enqueue(MockResponse().setBody("""<svg viewBox="0 0 300 400"/>"""))
        assertEquals("""<svg viewBox="0 0 300 400"/>""", api.files.portraitDocument("class.WITCH"))
        assertEquals("/game/portraits/class/WITCH.svg", server.takeRequest().path)
        assertFailsWith<IllegalArgumentException> { api.files.portraitDocument("WITCH") }
    }

    @Test fun `the character menu reads one account's characters and no one else's`(): Unit = runBlocking {
        val sent = server.requestCount
        ok("""[{"_id":"$id","userId":"$other","name":"Изгнанник","level":7,"classId":"$id"},
               {"_id":"$other","userId":"$other","name":"Ведьма","level":1,"classId":"$other"}]""")
        val mine = api.hero.charactersOf(other)
        assertEquals("/game/api/v1/character/byUser?userId=$other", server.takeRequest().path)
        assertEquals(listOf("Изгнанник", "Ведьма"), mine.map { it.name })
        assertEquals(7, mine.first().level)
        // The id is checked before the request is built, as everywhere else.
        assertFailsWith<IllegalArgumentException> { api.hero.charactersOf("nope") }
        assertEquals(sent + 1, server.requestCount)
    }

    @Test fun `login posts the credentials and never records them or the token`(): Unit = runBlocking {
        assertEquals("ADMIN", assertNotNull(api.currentUser()).role)
        assertEquals(token, api.sessionToken())
        // Neither the password nor the token that came back may reach the journal.
        assertFalse(journal.entries.value.toString().contains("private-"))
        // Signing out drops the whole session, token included.
        api.logout()
        assertNull(api.currentUser())
        assertNull(api.sessionToken())
        assertFailsWith<IllegalArgumentException> { api.catalog.page(Catalog.ITEMS, 0) }
        assertEquals(1, server.requestCount)
    }

    @Test fun `the password travels in a POST body, never in the query string`(): Unit = runBlocking {
        ok("""{"user":{"id":"$id","version":1,"login":"admin","role":"ADMIN","isActive":true},"token":"$token"}""")
        api.login("admin", "private-password")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/game/api/v1/user/login", request.path)
        assertEquals("""{"login":"admin","password":"private-password"}""", request.body.readUtf8())
        assertNull(request.getHeader("Authorization"))
    }

    @Test fun `every signed-in request carries the token as a bearer`(): Unit = runBlocking {
        ok("[]"); api.catalog.page(Catalog.ITEMS, 0)
        assertEquals("Bearer $token", server.takeRequest().getHeader("Authorization"))
        // What anyone may read carries no token at all: it is not the server's business who asked.
        server.enqueue(MockResponse().setBody("""{"default":"ru","languages":[]}"""))
        api.files.localeManifest()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun `a kept token comes back through me`(): Unit = runBlocking {
        api.logout()
        ok("""{"id":"$other","version":2,"login":"test1","role":"USER","isActive":true}""")
        assertEquals(other, api.resume("kept-token-0123456789").id)
        val request = server.takeRequest()
        assertEquals("/game/api/v1/user/me", request.path)
        assertEquals("Bearer kept-token-0123456789", request.getHeader("Authorization"))
        assertEquals("kept-token-0123456789", api.sessionToken())
    }

    @Test fun `a refused kept token leaves nobody signed in`(): Unit = runBlocking {
        var unauthorized = 0
        val fresh = GameApi(server.url("/game/").toString(), journal, onUnauthorized = { unauthorized++ })
        refusal(401, "AUTH_002", "Session expired")
        assertEquals(401, assertFailsWith<ApiFailure> { fresh.resume("stale-token-0123456789") }.status)
        assertNull(fresh.sessionToken())
        assertNull(fresh.currentUser())
        assertEquals(1, unauthorized)
    }

    @Test fun `signing out revokes the token it was handed`(): Unit = runBlocking {
        api.logout()
        ok("\"system.success\""); api.revoke(token)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/game/api/v1/user/logout", request.path)
        assertEquals("Bearer $token", request.getHeader("Authorization"))
        // The echo is best effort: a server that cannot be reached changes nothing on screen.
        failure(500); api.revoke(token)
        assertNull(api.currentUser())
    }

    @Test fun `create posts an array without identity and keeps the discriminator`(): Unit = runBlocking {
        val document = template(Catalog.EQUIPMENT)
        ok("[${JsonObject(document + mapOf("_id" to JsonPrimitive(id), "version" to JsonPrimitive(0)))}]")
        assertEquals(id, api.catalog.create(Catalog.EQUIPMENT, document).entityId)
        val request = server.takeRequest()
        assertEquals("/game/api/v1/equipment", request.path)
        val sent = WireJson.parseToJsonElement(request.body.readUtf8()).jsonArray.single().jsonObject
        assertEquals("features.data.equipment.equipment_data.Weapon", sent.text("type"))
        assertFalse("_id" in sent); assertFalse("version" in sent); assertFalse("price" in sent)
    }

    /**
     * The character menu's own create, end to end.
     *
     * It is tested through the document the menu actually builds rather than a literal one written
     * here: the menu used to post the editor's form seed, whose skill lists are the server's to
     * fill, and a hand-written fixture is exactly what let that through CI and out to a player.
     */
    @Test fun `the menu creates a character with only the fields a creation may carry`(): Unit = runBlocking {
        ok("""[{"_id":"$other","version":0,"userId":"$id","name":"Изгнанник","classId":"$id","level":1}]""")
        val document = characterDocument(id, "  Изгнанник  ", id)
        assertEquals(other, api.catalog.create(Catalog.CHARACTERS, document).entityId)
        val request = server.takeRequest()
        assertEquals("/game/api/v1/character", request.path)
        val sent = WireJson.parseToJsonElement(request.body.readUtf8()).jsonArray.single().jsonObject
        // The owner, the name trimmed and the class — and nothing else. The level, the bag and
        // every skill list are the server's from the first moment.
        assertEquals(setOf("userId", "name", "classId"), sent.keys)
        assertEquals("Изгнанник", sent.text("name"))
        assertTrue(document.keys.all { it in creationFields(Catalog.CHARACTERS) + editableFields(Catalog.CHARACTERS) })
    }

    @Test fun `a character form's draft is refused on create, by the name of every field`(): Unit = runBlocking {
        // `template` seeds the editor's form, so it carries every field the form draws. The editor
        // filters before posting; anything else that hands it to `create` is told which fields.
        val draft = JsonObject(template(Catalog.CHARACTERS) + mapOf(
            "userId" to JsonPrimitive(id), "name" to JsonPrimitive("Изгнанник"), "classId" to JsonPrimitive(id)))
        val refused = assertFailsWith<IllegalArgumentException> { api.catalog.create(Catalog.CHARACTERS, draft) }
        listOf("professionSkills", "battleSkills", "boolSkills").forEach {
            assertTrue(it in draft, "the form still draws $it")
            assertTrue(it in refused.message.orEmpty(), "the refusal names $it")
        }
        // A refusal at the boundary costs no request: nothing half-made reaches the collection.
        assertEquals(1, server.requestCount)
    }

    @Test fun `update sends only the changed fields and delete carries no body`(): Unit = runBlocking {
        // An `items` document has no text to change since 0.14.0; its price is still its own.
        val changes = buildJsonObject { put("price", 42) }
        ok("""{"_id":"$id","version":7,"price":42}""")
        assertEquals(7L, api.catalog.update(Catalog.ITEMS, id, changes).entityVersion)
        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/game/api/v1/items?id=$id", update.path)
        assertEquals(changes, WireJson.parseToJsonElement(update.body.readUtf8()).jsonObject)
        ok("\"system.deleted\""); api.catalog.delete(Catalog.ITEMS, id)
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/game/api/v1/items?id=$id", delete.path)
        assertEquals(0, delete.body.size)
    }

    @Test fun `server owned fields never reach a write`(): Unit = runBlocking {
        for (key in listOf("_id", "version", "type", "deleted", "userId", "money", "level", "params")) {
            assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.CHARACTERS, id, buildJsonObject { put(key, "bad") }) }
        }
        assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.ITEMS, id, JsonObject(emptyMap())) }
        assertEquals(1, server.requestCount)
    }

    @Test fun `an equipment template carries references, never rolled values`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.EQUIPMENT, id, buildJsonObject { put("fixedModifierIds", buildJsonArray { add("not-an-id") }) }) }
        assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.EQUIPMENT, id, buildJsonObject { put("modifierPools", buildJsonArray { add(" ") }) }) }
        assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.EQUIPMENT, id, buildJsonObject { put("pools", buildJsonObject { put("drop", -1) }) }) }
        assertFailsWith<IllegalArgumentException> { api.catalog.create(Catalog.EQUIPMENT, JsonObject(template(Catalog.EQUIPMENT) + ("params" to JsonArray(emptyList())))) }
        ok("""{"_id":"$id","version":2}""")
        api.catalog.update(Catalog.EQUIPMENT, id, buildJsonObject {
            put("fixedModifierIds", buildJsonArray { add(other) }); put("modifierPools", buildJsonArray { add("helmet"); add("local:armor") })
            put("pools", buildJsonObject { put("drop", 100); put("smith", 0) })
        })
        assertEquals(2, server.requestCount)
    }

    @Test fun `a list is paged on the client, because the server's page route answers with nothing`(): Unit = runBlocking {
        // /paged passes `page` as the limit and `size` as the offset, so page 0 returns an empty list.
        val records = (1..25).map { buildJsonObject { put("_id", id); put("name", "Item $it") } }
        ok(JsonArray(records).toString())
        val second = api.catalog.page(Catalog.ITEMS, 1)
        assertEquals("/game/api/v1/items", server.takeRequest().path)
        assertEquals(1, second.page)
        assertEquals(2, second.totalPages)
        assertEquals(25L, second.totalItems)
        assertEquals(5, second.items.size)
        assertEquals("Item 21", second.items.first().text("name"))
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.catalog.page(Catalog.ITEMS, -1) }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `a filtered search narrows the collection the server cannot filter`(): Unit = runBlocking {
        val bows = (1..3).map { buildJsonObject { put("_id", id); put("name", "Bow $it"); put("slot", "WEAPON_2H"); put("rarity", "RARE"); put("itemLevel", 40) } }
        val ring = buildJsonObject { put("_id", other); put("name", "Ring"); put("slot", "RING"); put("rarity", "RARE"); put("itemLevel", 40) }
        ok(JsonArray(bows + ring).toString())
        val result = api.catalog.search(Catalog.EQUIPMENT, 0, CatalogFilter(slot = "WEAPON_2H"))
        assertEquals("/game/api/v1/equipment", server.takeRequest().path)
        assertEquals(3, result.items.size)
        assertEquals(3L, result.totalItems)
        assertTrue(result.items.all { it.text("slot") == "WEAPON_2H" })
        // A level bound outside the data leaves nothing, and still reports an honest page count.
        ok(JsonArray(bows).toString())
        assertEquals(0, api.catalog.search(Catalog.EQUIPMENT, 0, CatalogFilter(minLevel = "80")).totalPages)
    }

    @Test fun `a random grant picks a matching template and lets the server roll it`(): Unit = runBlocking {
        val templates = JsonArray(listOf(
            buildJsonObject { put("_id", id); put("name", "Epic helm"); put("slot", "HELMET"); put("rarity", "EPIC") },
            buildJsonObject { put("_id", other); put("name", "Rare helm"); put("slot", "HELMET"); put("rarity", "RARE") },
            buildJsonObject { put("_id", other); put("name", "Epic ring"); put("slot", "RING"); put("rarity", "EPIC") }))
        ok(templates.toString())
        val chosen = api.catalog.randomTemplate("EPIC", "HELMET", Random(1))
        assertEquals("Epic helm", chosen.text("name"))
        assertEquals("/game/api/v1/equipment", server.takeRequest().path)
        // The rolls are the server's: the client only names the base it wants an instance of.
        ok("""{"_id":"$id","characterId":"$other","equipmentId":"$id","params":[{"modifierId":"$other","tierId":"$id","tier":3,"values":[42.0]}]}""")
        val instance = api.hero.grant(other, chosen.entityId)
        assertEquals(3, instance.params.single().tier)
        assertEquals(listOf(42.0), instance.params.single().values)
        assertEquals("/game/api/v1/character/inventory/itemToInventory?characterId=$other&equipmentId=$id", server.takeRequest().path)
        // Nothing matching means no request at all, and an unknown choice is refused up front.
        ok(templates.toString())
        assertFailsWith<IllegalArgumentException> { api.catalog.randomTemplate("MYTHICAL", "HELMET") }
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.catalog.randomTemplate("SHINY", "") }
        assertFailsWith<IllegalArgumentException> { api.catalog.randomTemplate("", "POCKET") }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `equipment is worn by instance id and the slot stays the template's`(): Unit = runBlocking {
        val worn = """{"_id":"$id","characterId":"$other","equipmentId":"$other","equippedSlot":"RING","params":[]}"""
        ok(worn)
        assertEquals("RING", api.hero.equip(other, id).equippedSlot)
        assertEquals("/game/api/v1/characterequipment/equip?characterId=$other&inventoryId=$id", server.takeRequest().path)
        ok("""{"_id":"$id","characterId":"$other","equipmentId":"$other","params":[]}""")
        assertFalse(api.hero.unequip(other, id).equipped)
        assertEquals("/game/api/v1/characterequipment/unequip?characterId=$other&inventoryId=$id", server.takeRequest().path)
        // A ring can be sent to the second place by name; the slot rides in the query, not a body.
        ok("""{"_id":"$id","characterId":"$other","equipmentId":"$other","equippedSlot":"RING_2","params":[]}""")
        assertEquals("RING_2", api.hero.equip(other, id, "RING_2").equippedSlot)
        assertEquals("/game/api/v1/characterequipment/equip?characterId=$other&inventoryId=$id&slot=RING_2", server.takeRequest().path)
    }

    @Test fun `the ledger has two hands and two rings, and every template slot has a place`() {
        val places = com.sperance.exileforge.core.contract.bodyPlaces
        assertEquals(11, places.size)
        assertEquals((com.sperance.exileforge.core.contract.slots.filterNot { it.startsWith("TOOL_") } - "JEWEL" - "MAP").toSet(), places.flatMap { it.fits }.toSet())
        val main = places.first { it.code == "MAIN_HAND" }
        val off = places.first { it.code == "OFF_HAND" }
        // A two-handed weapon fills the main hand and takes the other one with it.
        val worn = mapOf("WEAPON_2H" to "greatsword", "RING_2" to "second")
        assertEquals("greatsword", main.wornIn(worn))
        assertNull(off.wornIn(worn))
        assertTrue(off.blockedBy(worn))
        assertFalse(main.blockedBy(worn))
        // The two rings are filled from one slot, and each place asks for its own.
        assertEquals("second", places.first { it.code == "RING_2" }.wornIn(worn))
        assertNull(places.first { it.code == "RING" }.wornIn(worn))
        assertEquals(listOf("RING", "RING_2"), places.mapNotNull { it.ring })
    }

    /**
     * A jewel is worn in a socket on the tree, so the command names the node rather than a slot.
     *
     * The slot still says what the item is — the server sets `equippedSlot` to JEWEL — and
     * `socketCode` says where it sits, because the tree has many sockets and one slot could not
     * tell them apart.
     */
    @Test fun `a jewel is worn in a named socket and taken back out of it`(): Unit = runBlocking {
        ok("""{"_id":"$id","characterId":"$other","equipmentId":"$other","equippedSlot":"JEWEL","socketCode":"STR_SOCKET_1","params":[]}""")
        val socketed = api.hero.socket(other, id, "STR_SOCKET_1")
        assertEquals("STR_SOCKET_1", socketed.socketCode)
        assertTrue(socketed.socketed)
        assertEquals("/game/api/v1/characterequipment/socket?characterId=$other&inventoryId=$id&nodeCode=STR_SOCKET_1",
            server.takeRequest().path)

        ok("""{"_id":"$id","characterId":"$other","equipmentId":"$other","params":[]}""")
        val free = api.hero.unsocket(other, id)
        assertFalse(free.socketed)
        assertFalse(free.equipped)
        assertEquals("/game/api/v1/characterequipment/unsocket?characterId=$other&inventoryId=$id", server.takeRequest().path)

        // A socket has to be named: without one the command would be meaningless, so it never flies.
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.hero.socket(other, id, "  ") }
        assertFailsWith<IllegalArgumentException> { api.hero.socket(other, "not-an-id", "STR_SOCKET_1") }
        assertEquals(sent, server.requestCount)
    }

    /**
     * The catalogue is read whole, because since 0.16.0 it is where an item's base lives.
     *
     * An instance carries only what it rolled, so armour, damage and every requirement have to
     * come from the template — one read of the lot rather than a request per item on screen.
     */
    @Test fun `the equipment catalogue is read whole in one request`(): Unit = runBlocking {
        ok(JsonArray(listOf(
            buildJsonObject { put("_id", id); put("code", "IRON_HELMET"); put("slot", "HELMET") },
            buildJsonObject { put("_id", other); put("code", "CRIMSON_JEWEL"); put("slot", "JEWEL") },
        )).toString())
        val catalogue = api.catalog.equipment()
        assertEquals(listOf("IRON_HELMET", "CRIMSON_JEWEL"), catalogue.map { it.text("code") })
        assertEquals("/game/api/v1/equipment", server.takeRequest().path)
    }

    @Test fun `orbs are the currency category of the shared items collection`(): Unit = runBlocking {
        val catalogue = JsonArray(listOf(
            buildJsonObject { put("_id", id); put("code", "CHAOS_ORB"); put("category", "CURRENCY"); put("subCategory", "CHAOS_ORB"); put("price", 300) },
            buildJsonObject { put("_id", other); put("code", "ORB_OF_TRANSMUTATION"); put("category", "CURRENCY"); put("subCategory", "ORB_OF_TRANSMUTATION"); put("price", 10) },
            buildJsonObject { put("_id", id); put("code", "IRON_SHARD"); put("category", "STONE_STOCK"); put("subCategory", "STONE"); put("price", 1) }))
        ok(catalogue.toString())
        val orbs = api.world.orbs()
        assertEquals("/game/api/v1/items", server.takeRequest().path)
        // Only the currency category, cheapest first; a document carries a code and no text at all.
        assertEquals(listOf("ORB_OF_TRANSMUTATION", "CHAOS_ORB"), orbs.map { it.code })
        assertEquals(CurrencyOrb.CHAOS_ORB, orbs.last().orb)
        // The dictionary wins over the client's own table: the server owns what a thing is called.
        assertEquals("Chaos Orb", orbs.last().title())
    }

    @Test fun `promo codes are read whole and posted as one document`(): Unit = runBlocking {
        ok("""[{"_id":"$id","code":"WELCOME","description":"Стартовый набор","used":3,
            "treasure":[{"kind":"EXPERIENCE","itemId":"","amount":500.0},
                        {"kind":"ITEM","itemId":"$other","amount":3.0}]}]""")
        val codes = api.promo.codes()
        assertEquals("/game/api/v1/redemptioncodes", server.takeRequest().path)
        assertEquals("WELCOME", codes.single().code)
        assertEquals(3L, codes.single().used)
        // One list, two kinds: an administrator writes the reward as one thought.
        assertEquals(listOf(RedemptionKind.EXPERIENCE, RedemptionKind.ITEM), codes.single().treasure.map { it.kind })
    }

    @Test fun `creating a promo code sends neither an id nor a use count`(): Unit = runBlocking {
        ok("""[{"_id":"$id","code":"WELCOME","used":0,"treasure":[{"kind":"GOLD","itemId":"","amount":100.0}]}]""")
        api.promo.create(RedemptionCode(id = id, code = " WELCOME ", used = 9,
            treasure = listOf(RedemptionReward(RedemptionKind.GOLD, amount = 100.0))))
        val sent = server.takeRequest()
        assertEquals("/game/api/v1/redemptioncodes", sent.path)
        val body = WireJson.parseToJsonElement(sent.body.readUtf8()).jsonArray.single().jsonObject
        // Identity and the counter belong to the server; posting either would be the client
        // deciding something it has no business deciding.
        assertFalse("_id" in body, "the id must not be sent on create")
        assertFalse("used" in body, "the use count must not be sent on create")
        assertEquals("WELCOME", body.text("code"))
    }

    @Test fun `a promo code with no reward never reaches the server`(): Unit = runBlocking {
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.promo.create(RedemptionCode(code = "EMPTY")) }
        assertFailsWith<IllegalArgumentException> {
            api.promo.create(RedemptionCode(code = "FREE", treasure = listOf(RedemptionReward(RedemptionKind.GOLD, amount = 0.0))))
        }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `deleting a promo code names it in the query and carries no body`(): Unit = runBlocking {
        ok("{}")
        api.promo.delete(id)
        val sent = server.takeRequest()
        assertEquals("DELETE", sent.method)
        assertEquals("/game/api/v1/redemptioncodes?id=$id", sent.path)
        assertEquals("", sent.body.readUtf8())
    }

    @Test fun `applying an orb names the pair and prints what the server did`(): Unit = runBlocking {
        val rerolled = """{"_id":"$id","characterId":"$other","equipmentId":"$other","rarity":"RARE","corrupted":false,
            "params":[{"modifierId":"$id","tierId":"$other","tier":2,"values":[7.0]}]}"""
        ok("""{"messageKey":"currency.chaos","messageArgs":["equipment.IRON_SKULLCAP.name","4"],"item":$rerolled}""")
        val outcome = api.hero.applyOrb(other, id, other)
        assertEquals("/game/api/v1/characterequipment/applyOrb?characterId=$other&inventoryId=$id&orbItemId=$other", server.takeRequest().path)
        // The server sends a key and arguments; the arguments are keys too, so the item is named.
        assertEquals("Iron Skullcap: перекатаны аффиксы, всего 4", outcome.message)
        assertEquals("RARE", outcome.item.rarity)
        assertFalse(outcome.item.corrupted)
        // A mirror is the one orb that answers with a second document, and the copy is locked.
        ok("""{"message":"Helm was mirrored","item":$rerolled,"created":{"_id":"$other","characterId":"$other","equipmentId":"$other","rarity":"RARE","corrupted":true,"params":[]}}""")
        val copy = assertNotNull(api.hero.applyOrb(other, id, other).created)
        assertEquals(other, copy.id)
        assertTrue(copy.corrupted)
        server.takeRequest()
    }

    @Test fun `the bench lists its lines and crafts and uncrafts by name`(): Unit = runBlocking {
        ok("""[{"code":"CRAFTED_ADD_MAXIMUM_LIFE_T3","modifierId":"$id","modifierCode":"CRAFTED_ADD_MAXIMUM_LIFE","tierId":"$other","tier":3,
            "source":"PREFIX","group":"ADD_MAXIMUM_LIFE","values":[{"valueMin":25.0,"valueMax":34.0}],"orb":"ORB_OF_TRANSMUTATION",
            "orbItemId":"$other","amount":3,"slots":[]}]""")
        val line = api.hero.bench(other).single()
        assertEquals("/game/api/v1/characterequipment/bench?characterId=$other", server.takeRequest().path)
        assertEquals(3L, line.amount)
        assertTrue(line.fits("JEWEL"))

        val item = """{"_id":"$id","characterId":"$other","equipmentId":"$other","rarity":"UNCOMMON","influence":"SHAPER",
            "params":[{"modifierId":"$id","tierId":"$other","tier":3,"values":[30.0]},{"modifierId":"$other","tierId":"$id","tier":1,"values":[9.0],"fractured":true}]}"""
        ok("""{"messageKey":"currency.crafted","messageArgs":["equipment.IRON_SKULLCAP.name"],"item":$item}""")
        val crafted = api.hero.craft(other, id, line.code)
        val sent = server.takeRequest()
        assertEquals("POST", sent.method)
        assertEquals("/game/api/v1/characterequipment/craft?characterId=$other&inventoryId=$id&recipe=CRAFTED_ADD_MAXIMUM_LIFE_T3", sent.path)
        assertEquals("SHAPER", crafted.item.influence)
        assertTrue(crafted.item.params.last().fractured)

        ok("""{"messageKey":"currency.uncrafted","messageArgs":[],"item":$item}""")
        api.hero.uncraft(other, id)
        assertEquals("/game/api/v1/characterequipment/uncraft?characterId=$other&inventoryId=$id", server.takeRequest().path)

        // A blank line is refused before anything is sent.
        val count = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.hero.craft(other, id, " ") }
        assertEquals(count, server.requestCount)
    }

    @Test fun `stats and the bag come from the server as they are`(): Unit = runBlocking {
        // The sheet reports the numbers and the server's verdict on every worn item alongside them.
        ok("""{"characterId":"$id","level":12,"stats":{"STOCK_HEALTH":188.4,"STOCK_ARMOR":40.0},"active":["$other"],
            "inactive":[{"inventoryId":"$id","code":"IRON_SKULLCAP","reasons":["strength: need 30, have 14"]}]}""")
        val sheet = api.hero.stats(id)
        assertEquals(188.4, sheet.stats.getValue("STOCK_HEALTH"))
        assertEquals(12, sheet.level)
        assertEquals(listOf(other), sheet.active)
        assertEquals("strength: need 30, have 14", sheet.inactive.single().reasons.single())
        assertEquals("/game/api/v1/character/inventory/stats?characterId=$id", server.takeRequest().path)
        ok("""[{"itemId":"chaos_orb","amount":50}]""")
        assertEquals(50L, api.hero.bag(id).single().amount)
        assertEquals("/game/api/v1/character/inventory/items?characterId=$id", server.takeRequest().path)
        // Since 0.41.0 the sheet is added up here, in the order the server counts and checks in.
        ok("""{"stats":[{"stat":"STOCK_STRENGTH","order":10},{"stat":"STOCK_HEALTH","order":100}],"slots":["HELMET","BODY"],
            "sell":{"affixShare":0.15,"rarity":{"COMMON":1.0,"RARE":2.5}}}""")
        val tables = api.world.statTables()
        assertEquals(100, tables.order.getValue("STOCK_HEALTH"))
        assertEquals(listOf("HELMET", "BODY"), tables.slots)
        assertEquals(2.5, tables.sell.rarity.getValue("RARE"))
        assertEquals("/game/system/stats", server.takeRequest().path)
        ok("\"system.success\"")
        // The server answers a locale key now, not a word; the client passes it on untouched.
        assertEquals("system.success", api.hero.adjustItems(id, listOf(ItemStack("chaos_orb", -2))))
        val adjust = server.takeRequest()
        assertEquals("/game/api/v1/character/inventory/addItem?characterId=$id", adjust.path)
        assertEquals(-2, WireJson.parseToJsonElement(adjust.body.readUtf8()).jsonArray.single().jsonObject.getValue("amount").jsonPrimitive.int)
        assertFailsWith<IllegalArgumentException> { ItemStack("chaos_orb", 0) }
    }

    @Test fun `the world's reference tables are read whole and once`(): Unit = runBlocking {
        ok("""[{"_id":"$id","code":"MARAUDER","startNodeCode":"STR_START",
            "baseStats":[{"stat":"STOCK_STRENGTH","value":32.0}],"perLevelStats":[{"stat":"STOCK_HEALTH","value":12.0}],
            "params":[{"modifierId":"$other","values":[1.0]}]}]""")
        val marauder = api.world.classes().single()
        assertEquals("/game/api/v1/characterclass", server.takeRequest().path)
        // The class document has no text at all: its name is the dictionary's, under its code.
        assertEquals("Marauder", marauder.title)
        assertEquals("Сила", marauder.details)
        assertEquals(32.0, marauder.baseStats.single().value)
        // A class conversion is fixed by the reference table, so it carries no tier at all.
        assertFalse(marauder.params.single().rolled)

        ok("""[{"_id":"$other","level":3,"experience":300.0,"skillPoints":2},{"_id":"$id","level":1,"experience":0.0}]""")
        val levels = api.world.levels()
        assertEquals("/game/api/v1/experiencelevel", server.takeRequest().path)
        assertEquals(listOf(1, 3), levels.map { it.level })
        assertEquals(2, levels.last().skillPoints)
    }

    @Test fun `the skill tree is one graph and every command answers with the whole state`(): Unit = runBlocking {
        ok("""[{"_id":"$id","code":"STR_START","type":"START","cost":0,"positionX":-40,"positionY":0,
            "connections":["STR_LIFE_1"],"params":[{"modifierId":"$other","values":[10.0]}]}]""")
        val node = api.world.tree().single()
        assertEquals("/game/api/v1/skilltreenode", server.takeRequest().path)
        assertEquals(SkillNodeType.START, node.type)
        assertEquals(listOf("STR_LIFE_1"), node.connections)
        assertFalse(node.params.single().rolled)

        val state = """{"characterId":"$id","total":5,"spent":1,"available":4,
            "nodes":[{"code":"STR_START","type":"START","cost":0,"params":[]}]}"""
        ok(state)
        assertEquals(setOf("STR_START"), api.tree.state(id).takenCodes)
        assertEquals("/game/api/v1/character/skilltree/state?characterId=$id", server.takeRequest().path)
        ok(state)
        assertEquals(4, api.tree.allocate(id, "STR_LIFE_1").available)
        assertEquals("/game/api/v1/character/skilltree/allocate?characterId=$id&nodeCode=STR_LIFE_1", server.takeRequest().path)
        ok(state); api.tree.refund(id, "STR_LIFE_1")
        assertEquals("/game/api/v1/character/skilltree/refund?characterId=$id&nodeCode=STR_LIFE_1", server.takeRequest().path)
        ok(state); api.tree.reset(id)
        assertEquals("/game/api/v1/character/skilltree/reset?characterId=$id", server.takeRequest().path)
        // A node is named by its code, never by an id, and a blank one never reaches the network.
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.tree.allocate(id, " ") }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `the merchant's shelf is read and bought from by offer`(): Unit = runBlocking {
        ok("""{"refreshAt":1700000000000,"offers":[{"id":"o1","item":{"_id":"$other","characterId":"$id","equipmentId":"$id","params":[],"rarity":"RARE"},"price":480}]}""")
        val stock = api.merchant.stock(id)
        assertEquals("/game/api/v1/character/merchant?characterId=$id", server.takeRequest().path)
        assertEquals(480L, stock.offers.single().price)
        ok("""{"item":{"_id":"$other","characterId":"$id","equipmentId":"$id","params":[],"rarity":"RARE"},"money":20}""")
        assertEquals(20L, api.merchant.buy(id, "o1").money)
        val buy = server.takeRequest()
        assertEquals("POST", buy.method)
        assertEquals("/game/api/v1/character/merchant/buy?characterId=$id&offerId=o1", buy.path)
    }

    @Test fun `lot places are read and bought, and map services are named by the map`(): Unit = runBlocking {
        ok("""{"used":5,"limit":5,"max":20,"price":500}""")
        assertTrue(api.auction.slots(id).full)
        assertEquals("/game/api/v1/auctionlot/slots?characterId=$id", server.takeRequest().path)
        ok("""{"used":5,"limit":6,"max":20,"price":750,"money":100}""")
        assertEquals(6, api.auction.buySlot(id).limit)
        assertEquals("POST", server.takeRequest().method)
        ok("""{"money":40,"chests":{"left":2,"refreshAt":1,"bought":true},"boss":{"alive":true,"respawnAt":0}}""")
        assertTrue(api.campaign.treasure(id, "C1_TIDAL_SHORE").chests.bought)
        assertEquals("/game/api/v1/character/campaign/treasure?characterId=$id&mapCode=C1_TIDAL_SHORE", server.takeRequest().path)
        ok("""{"money":10,"chests":{"left":0,"refreshAt":1},"boss":{"alive":true,"respawnAt":0}}""")
        assertTrue(api.campaign.summon(id, "C1_TIDAL_SHORE").boss.alive)
        assertEquals("/game/api/v1/character/campaign/summon?characterId=$id&mapCode=C1_TIDAL_SHORE", server.takeRequest().path)
    }

    @Test fun `the crafts are read, started and stopped by the hero and the work's code`(): Unit = runBlocking {
        ok("""{"now":1000,"rules":{"offlineHours":8.0,"maxLevel":50},"professions":[{"code":"MINING","tool":"TOOL_MINING","level":3,"experience":12.0,"next":80.0,
            "bonus":{"speed":10.0},"jobs":[{"code":"COPPER_VEIN","level":1,"seconds":5.0,"cycleMillis":4500,"nothing":30.0,"output":"COPPER_ORE","experience":4.0}]}],
            "work":{"profession":"MINING","job":"COPPER_VEIN","startedAt":1,"settledAt":900,"cycleMillis":4500,"nextAt":5400},
            "gains":{"cycles":3,"nothing":1,"items":{"COPPER_ORE":2},"experience":8.0}}""")
        val state = api.crafts.state(id)
        assertEquals(4500L, state.professions.single().jobs.single().cycleMillis)
        assertEquals(2L, state.gains.items["COPPER_ORE"])
        assertEquals("/game/api/v1/character/crafts?characterId=$id", server.takeRequest().path)
        ok("""{"now":1,"professions":[]}""")
        api.crafts.start(id, "IRON_VEIN")
        val started = server.takeRequest()
        assertEquals("POST", started.method)
        assertEquals("/game/api/v1/character/crafts/start?characterId=$id&job=IRON_VEIN", started.path)
        ok("""{"now":1,"professions":[]}""")
        assertNull(api.crafts.stop(id).work)
        assertEquals("/game/api/v1/character/crafts/stop?characterId=$id", server.takeRequest().path)
        ok("""{"now":1,"professions":[],"gains":{"made":1,"starved":true,"spent":{"COPPER_ORE":5}},"additives":{"FIRE_FLUX":"HC_FIRE"},"maxAdditives":2}""")
        val forged = api.crafts.start(id, "COPPER_FORGING", listOf("FIRE_FLUX", "STONE_FLUX"))
        assertTrue(forged.gains.starved)
        assertEquals(2, forged.maxAdditives)
        assertEquals("/game/api/v1/character/crafts/start?characterId=$id&job=COPPER_FORGING&additives=FIRE_FLUX%2CSTONE_FLUX", server.takeRequest().path)
    }

    @Test fun `a location is entered with a map from the stash or without one`(): Unit = runBlocking {
        val mapItem = "b".repeat(24)
        ok("""{"map":{"mapCode":"C1_TIDAL_SHORE","effects":{"MAP_MONSTER_LIFE":30.0,"MAP_CHESTS":1.0},"quantity":12.0,"rarity":12.0,"experience":12.0},"chests":{"left":3,"refreshAt":1}}""")
        val launch = api.campaign.start(id, "C1_TIDAL_SHORE", mapItem)
        assertEquals(30.0, launch.map!!.effects["MAP_MONSTER_LIFE"])
        assertEquals(3, launch.chests.left)
        val entered = server.takeRequest()
        assertEquals("POST", entered.method)
        assertEquals("/game/api/v1/character/campaign/start?characterId=$id&mapCode=C1_TIDAL_SHORE&itemId=$mapItem", entered.path)
        ok("""{"map":null,"chests":{"left":1,"refreshAt":1}}""")
        assertEquals(null, api.campaign.start(id, "C1_TIDAL_SHORE").map)
        assertEquals("/game/api/v1/character/campaign/start?characterId=$id&mapCode=C1_TIDAL_SHORE", server.takeRequest().path)
        assertFailsWith<IllegalArgumentException> { api.campaign.start(id, "C1_TIDAL_SHORE", "not-an-id") }
    }

    @Test fun `a map's risk pays by the server's weights`() {
        val rule = MapRule(risk = mapOf("MAP_MONSTER_LIFE" to .4, "MAP_HERO_FLASK" to 6.0))
        val bonus = rule.bonus(mapOf("MAP_MONSTER_LIFE" to 30.0, "MAP_HERO_FLASK" to 2.0, "MAP_QUANTITY" to 10.0, "MAP_PACK_SIZE" to 25.0))
        assertEquals(34.0, bonus.quantity)
        assertEquals(24.0, bonus.rarity)
        assertEquals(24.0, bonus.experience)
        assertEquals(com.sperance.exileforge.core.model.campaign.MapLineKind.HARM, rule.kindOf("MAP_MONSTER_LIFE"))
        assertEquals(com.sperance.exileforge.core.model.campaign.MapLineKind.CONTENT, rule.kindOf("MAP_PACK_SIZE"))
        assertEquals(com.sperance.exileforge.core.model.campaign.MapLineKind.REWARD, rule.kindOf("MAP_QUANTITY"))
        assertEquals(12.0, rule.riskOf("MAP_HERO_FLASK", 2.0))
        assertEquals(0.0, rule.riskOf("MAP_PACK_SIZE", 25.0))
    }

    @Test fun `a map's boss is asked after and reported by its own route`(): Unit = runBlocking {
        ok("""{"alive":false,"respawnAt":1700000000000}""")
        assertFalse(api.campaign.boss(id, "C1_TIDAL_SHORE").alive)
        assertEquals("/game/api/v1/character/campaign/boss?characterId=$id&mapCode=C1_TIDAL_SHORE", server.takeRequest().path)
        ok("""{"experience":900.0,"gold":80,"items":[],"equipment":[],"level":3,"totalExperience":1020.0,"money":620}""")
        assertEquals(80L, api.campaign.slayBoss(id, "C1_TIDAL_SHORE").gold)
        val slain = server.takeRequest()
        assertEquals("POST", slain.method)
        assertEquals("/game/api/v1/character/campaign/boss?characterId=$id&mapCode=C1_TIDAL_SHORE", slain.path)
    }

    @Test fun `a map's chests are asked for and opened by the map's code`(): Unit = runBlocking {
        ok("""{"left":2,"refreshAt":1700000000000}""")
        val chests = api.campaign.chests(id, "C1_TIDAL_SHORE")
        assertEquals(2, chests.left)
        assertEquals("/game/api/v1/character/campaign/chests?characterId=$id&mapCode=C1_TIDAL_SHORE", server.takeRequest().path)
        ok("""{"experience":0.0,"gold":40,"items":[],"equipment":[],"level":3,"totalExperience":120.0,"money":540}""")
        assertEquals(40L, api.campaign.openChest(id, "C1_TIDAL_SHORE").gold)
        val open = server.takeRequest()
        assertEquals("POST", open.method)
        assertEquals("/game/api/v1/character/campaign/chest?characterId=$id&mapCode=C1_TIDAL_SHORE", open.path)
    }

    @Test fun `the campaign is read whole and a kill names the map, the monster and the rarity`(): Unit = runBlocking {
        ok("""{"chapters":[{"code":"CHAPTER_1","maps":[{"code":"C1_TIDAL_SHORE","chapter":"CHAPTER_1","order":1,"biome":"SHORE","level":1,
            "monsterCount":[10,14],"monsters":[{"code":"DROWNED","form":"HUMANOID","stats":{"STOCK_HEALTH":18.0}}],
            "modifiers":[{"code":"MOB_TOUGH","weight":100,"minLevel":1,"effects":[{"stat":"STOCK_HEALTH","operation":"INCREASED","value":60.0}]}]}]}],
            "rarities":[{"rarity":"NORMAL","weight":85,"modifiers":[0,0]}],
            "combat":{"timeLimit":45.0,"variance":20.0,"resistCap":75.0,"blockCap":75.0,"spellBlockShare":50.0,"unarmed":{"damage":4.0,"speed":1.2},
            "critical":{"chance":5.0,"multiplier":150.0},"armour":{"factor":5.0,"cap":90.0},"evasion":{"base":150.0,"perLevel":40.0,"cap":75.0},
            "stun":{"share":15.0,"duration":0.4},"shield":{"rechargeDelay":2.0,"rechargePerSecond":20.0},
            "spell":{"innateDamage":2.0,"innatePerLevel":0.5,"castSpeed":0.8,"manaCost":12.0,"manaRegenShare":1.75},
            "flask":{"charges":2,"perKill":1,"heal":40.0,"duration":3.0},"retreat":{"delay":1.5},"death":{"fromLevel":10,"experienceShare":5.0},
            "ailments":[{"ailment":"BURNING","type":"STOCK_ATTACK_FIRE","chance":30.0,"magnitude":60.0,"duration":4.0}]}}""")
        val view = api.campaign.chapters()
        assertEquals(18.0, view.chapters.single().maps.single().monsters.single().stats["STOCK_HEALTH"])
        // The rules of the fight are the server's and arrive with the chapters (0.28.0).
        assertEquals(45.0, view.combat.timeLimit)
        assertEquals(2, view.combat.flask.charges)
        assertEquals("BURNING", view.combat.ailments.single().ailment)
        assertEquals("/game/api/v1/character/campaign/chapters", server.takeRequest().path)
        ok("""{"cleared":[],"unlocked":["C1_TIDAL_SHORE"]}""")
        assertEquals(listOf("C1_TIDAL_SHORE"), api.campaign.progress(id).unlocked)
        assertEquals("/game/api/v1/character/campaign/progress?characterId=$id", server.takeRequest().path)
        ok("""{"experience":20.0,"gold":5,"items":[{"itemId":"$other","amount":1}],"equipment":[],"level":1,"totalExperience":20.0,"money":5}""")
        assertEquals(5L, api.campaign.kill(id, "C1_TIDAL_SHORE", "DROWNED", com.sperance.exileforge.core.model.campaign.MonsterRarity.MAGIC).gold)
        val kill = server.takeRequest()
        assertEquals("POST", kill.method)
        assertEquals("/game/api/v1/character/campaign/kill?characterId=$id&mapCode=C1_TIDAL_SHORE&monsterCode=DROWNED&rarity=MAGIC", kill.path)
        ok("""{"cleared":["C1_TIDAL_SHORE"],"unlocked":["C1_TIDAL_SHORE","C1_BRINE_CAVES"]}""")
        assertEquals(2, api.campaign.complete(id, "C1_TIDAL_SHORE").unlocked.size)
        assertEquals("/game/api/v1/character/campaign/complete?characterId=$id&mapCode=C1_TIDAL_SHORE", server.takeRequest().path)
        ok("""{"lost":50.0,"level":12,"totalExperience":1450.0}""")
        assertEquals(50.0, api.campaign.fall(id, "C1_TIDAL_SHORE").lost)
        val fall = server.takeRequest()
        assertEquals("POST", fall.method)
        assertEquals("/game/api/v1/character/campaign/fall?characterId=$id&mapCode=C1_TIDAL_SHORE", fall.path)
    }

    @Test fun `experience is granted and the level comes back from the server`(): Unit = runBlocking {
        ok("""{"_id":"$id","userId":"$other","name":"Изгнанник","classId":"$id","level":7,"experience":1200.0}""")
        assertEquals(7, api.hero.addExperience(id, 1200.0).level)
        assertEquals("/game/api/v1/character/inventory/experience?characterId=$id&amount=1200.0", server.takeRequest().path)
        val sent = server.requestCount
        // Taking experience away is not a route this server has; the level only ever rises.
        assertFailsWith<IllegalArgumentException> { api.hero.addExperience(id, -5.0) }
        assertFailsWith<IllegalArgumentException> { api.hero.addExperience(id, 0.0) }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `the showcase is narrowed by the server and only the set fields travel`(): Unit = runBlocking {
        ok("""{"items":[{"_id":"$id","sellerId":"$other","sellerName":"Изгнанник","kind":"EQUIPMENT","itemCode":"IRON_SKULLCAP",
            "slot":"HELMET","rarity":"RARE","itemLevel":30,"priceOrbId":"$other","price":40,"status":"ACTIVE",
            "equipment":{"_id":"$other","equipmentId":"$id","rarity":"RARE","params":[]}}],
            "page":1,"pageSize":20,"totalItems":25,"totalPages":2}""")
        val filter = AuctionFilter(title = "skull", kind = "EQUIPMENT", maxPrice = "50", excludeSellerId = other)
        val page = api.auction.search(id, filter, 1)
        val request = server.takeRequest()
        assertEquals("/game/api/v1/auctionlot/search", request.path!!.substringBefore('?'))
        // A blank field is "do not filter": an empty enum would be rejected by the server outright.
        val query = request.requestUrl!!
        assertEquals(listOf("characterId", "page", "size", "kind", "title", "maxPrice", "excludeSellerId").sorted(), query.queryParameterNames.sorted())
        assertEquals("1", query.queryParameter("page"))
        assertEquals(other, query.queryParameter("excludeSellerId"))
        assertEquals(2, page.totalPages)
        val lot = page.items.single()
        assertEquals("Iron Skullcap", lot.title)
        assertTrue(lot.onSale)
        assertTrue(lot.belongsTo(other))
        // The showcase card describes the instance: its rarity may have been changed by an orb.
        assertEquals("RARE", assertNotNull(lot.equipment).rarity)
        assertTrue(AuctionFilter().isEmpty)
    }

    @Test fun `listing, buying and withdrawing name the lot and nothing else`(): Unit = runBlocking {
        val lot = """{"_id":"$id","sellerId":"$other","kind":"EQUIPMENT","itemCode":"IRON_SKULLCAP","priceOrbId":"$other","price":40,"status":"ACTIVE"}"""
        ok(lot)
        assertEquals("Iron Skullcap", api.auction.sellEquipment(other, id, other, 40).title)
        assertEquals("/game/api/v1/auctionlot/sell/equipment?inventoryId=$id&characterId=$other&priceOrbId=$other&price=40", server.takeRequest().path)
        ok("""{"_id":"$id","sellerId":"$other","kind":"ITEM","itemId":"$id","amount":5,"itemCode":"CHAOS_ORB","priceOrbId":"$other","price":2,"status":"ACTIVE"}""")
        assertEquals(5L, api.auction.sellItem(other, id, 5, other, 2).amount)
        assertEquals("/game/api/v1/auctionlot/sell/item?itemId=$id&amount=5&characterId=$other&priceOrbId=$other&price=2", server.takeRequest().path)
        ok("""{"_id":"$id","sellerId":"$other","kind":"EQUIPMENT","itemCode":"IRON_SKULLCAP","status":"SOLD","buyerId":"$id"}""")
        assertEquals(AuctionLotStatus.SOLD, api.auction.buy(id, id).status)
        assertEquals("/game/api/v1/auctionlot/buy?characterId=$id&lotId=$id", server.takeRequest().path)
        ok("""{"_id":"$id","sellerId":"$other","kind":"EQUIPMENT","itemCode":"IRON_SKULLCAP","status":"CANCELLED"}""")
        assertFalse(api.auction.cancel(other, id).onSale)
        assertEquals("/game/api/v1/auctionlot/cancel?characterId=$other&lotId=$id", server.takeRequest().path)
        ok("""[$lot]""")
        assertEquals(1, api.auction.myLots(other).size)
        assertEquals("/game/api/v1/auctionlot/my?characterId=$other", server.takeRequest().path)
        // A price that buys nothing, or a price not set in orbs, never reaches the network.
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.auction.sellEquipment(other, id, other, 0) }
        assertFailsWith<IllegalArgumentException> { api.auction.sellItem(other, id, 0, other, 5) }
        assertFailsWith<IllegalArgumentException> { api.auction.sellEquipment(other, id, "not-an-orb", 5) }
        assertFailsWith<IllegalArgumentException> { api.auction.buy(id, "wrong") }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `capabilities are read from the server's own route table`(): Unit = runBlocking {
        val routes = listOf("POST" to "/api/v1/user/login", "POST" to "/api/v1/user/login/byDeviceId",
            "POST" to "/api/v1/user/byDeviceId", "GET" to "/api/v1/user/me", "POST" to "/api/v1/user/logout",
            "GET" to "/api/v1/character/byUser",
            "GET" to "/api/v1/equipment/paged", "GET" to "/api/v1/character/inventory/equipments",
            "GET" to "/system/stats", "POST" to "/api/v1/character/inventory/itemToInventory",
            "POST" to "/api/v1/characterequipment/equip", "POST" to "/api/v1/characterequipment/applyOrb",
            "GET" to "/api/v1/modifierdefinition", "GET" to "/api/v1/characterclass", "GET" to "/api/v1/experiencelevel",
            "GET" to "/api/v1/skilltreenode", "GET" to "/api/v1/character/skilltree/state",
            "POST" to "/api/v1/character/skilltree/allocate", "GET" to "/api/v1/auctionlot/search",
            "POST" to "/api/v1/auctionlot/sell/equipment", "POST" to "/api/v1/auctionlot/buy",
            "GET" to "/api/v1/equipment", "POST" to "/api/v1/characterequipment/socket",
            "POST" to "/api/v1/characterequipment/unsocket", "POST" to "/api/v1/characterequipment/sell",
            "GET" to "/api/v1/characterequipment/bench", "POST" to "/api/v1/characterequipment/craft",
            "POST" to "/api/v1/characterequipment/uncraft",
            "GET" to "/api/v1/character/campaign/chapters", "GET" to "/api/v1/character/campaign/progress",
            "POST" to "/api/v1/character/campaign/kill", "POST" to "/api/v1/character/campaign/complete",
            "POST" to "/api/v1/character/campaign/fall", "POST" to "/api/v1/character/campaign/start",
            "GET" to "/api/v1/character/crafts", "POST" to "/api/v1/character/crafts/start")
        // The server prints the Ktor selector, so a method arrives as "(GET)".
        ok(JsonArray(routes.map { buildJsonObject { put("path", it.second); put("method", "(${it.first})") } }).toString())
        val capabilities = api.capabilities()
        capabilities.requireWorkbench()
        assertTrue(capabilities.has("POST", "/api/v1/user/login"))
        assertEquals("/game/system/routes", server.takeRequest().path)
        // A server from before 0.21.0 signs in by GET and issues no token: it is named, not guessed at.
        val stale = ApiCapabilities.of(routes.map { (method, path) -> RouteInfo(path, "(${if (path == "/api/v1/user/login") "GET" else method})") })
        assertFailsWith<IllegalArgumentException> { stale.requireWorkbench() }
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

    @Test fun `a refusal reads as a sentence to a player and with its code to an administrator`() {
        val refused = ApiFailure(403, "AUTH_004", "Character belongs to another account")
        assertEquals("Это не ваш персонаж", refusalLine(refused, "Это не ваш персонаж", detailed = false))
        assertEquals("HTTP 403 AUTH_004: Это не ваш персонаж", refusalLine(refused, "Это не ваш персонаж", detailed = true))
        // Only a server's answer has a status and a code to show.
        assertEquals("нет связи", refusalLine(java.io.IOException("reset"), "нет связи", detailed = true))
    }

    @Test fun `a transport failure names itself instead of blaming the network`() {
        // Android's cleartext block is the one that looks exactly like "no connection" but is config.
        val blocked = java.net.UnknownServiceException("CLEARTEXT communication to 10.0.2.2 not permitted by network security policy")
        assertTrue(transportDetail(blocked).contains("открытый HTTP"), transportDetail(blocked))
        assertTrue(transportDetail(java.net.ConnectException("Failed to connect to /10.0.2.2:8080")).contains("10.0.2.2:8080"))
        assertTrue(transportDetail(java.net.UnknownHostException("example.invalid")).contains("example.invalid"))
        // A dead handshake and a silent server share an exception but not a remedy.
        val handshake = java.net.SocketTimeoutException("failed to connect to /10.0.2.2 (port 8080) from /10.0.2.16 (port 36470) after 10000ms")
        assertTrue(transportDetail(handshake).contains("файрвол"), transportDetail(handshake))
        assertTrue(transportDetail(java.net.SocketTimeoutException("timeout")).contains("не ответил вовремя"))
        // A failure with no message still says which one it was.
        assertEquals("EOFException", transportDetail(java.io.EOFException()))
    }

    @Test fun `404 is absent, other errors are preserved`(): Unit = runBlocking {
        failure(404); assertNull(api.catalog.get(Catalog.ITEMS, id)); server.takeRequest()
        for (status in listOf(400, 403, 429, 500)) {
            failure(status); assertEquals(status, assertFailsWith<ApiFailure> { api.catalog.get(Catalog.ITEMS, id) }.status); server.takeRequest()
        }
    }

    @Test fun `unauthorized response clears the session and is never retried`(): Unit = runBlocking {
        failure(401)
        assertEquals(401, assertFailsWith<ApiFailure> { api.catalog.page(Catalog.ITEMS, 0) }.status)
        assertFailsWith<IllegalArgumentException> { api.catalog.page(Catalog.ITEMS, 0) }
        assertEquals(2, server.requestCount)
    }

    @Test fun `a rejected write is never automatically resubmitted`(): Unit = runBlocking {
        failure(400)
        assertFailsWith<ApiFailure> { api.catalog.update(Catalog.CHARACTERS, id, buildJsonObject { put("name", "Hero") }) }
        assertEquals(2, server.requestCount)
    }

    @Test fun `password change posts both secrets, hides them and keeps this session`(): Unit = runBlocking {
        ok("\"system.success\""); api.changePassword("private-old", "private-New1")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/game/api/v1/user/changePassword", request.path)
        assertEquals("""{"password":"private-old","newPassword":"private-New1"}""", request.body.readUtf8())
        assertFalse(journal.entries.value.toString().contains("private-"))
        // The server ends every other session and keeps this one.
        assertEquals(token, api.sessionToken())
    }

    @Test fun `malformed server response remains a useful protocol error`(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Error</html>"))
        val e = assertFailsWith<ApiFailure> { api.catalog.get(Catalog.ITEMS, id) }
        assertEquals(502, e.status); assertTrue(e.message!!.contains("JSON"))
    }

    @Test fun `a cancelled request stops reading the body`(): Unit = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}""").setBodyDelay(3, TimeUnit.SECONDS))
        val job = async(start = CoroutineStart.UNDISPATCHED) { api.catalog.get(Catalog.ITEMS, id) }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        withTimeout(1_000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
    }

    @Test fun `invalid identity never reaches the network`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { api.catalog.delete(Catalog.ITEMS, "wrong") }
        assertFailsWith<IllegalArgumentException> { api.hero.grant("wrong", id) }
        assertFailsWith<IllegalArgumentException> { api.hero.equip(id, "wrong") }
        assertFailsWith<IllegalArgumentException> { api.hero.applyOrb(id, id, "wrong") }
        assertFailsWith<IllegalArgumentException> { api.tree.state("wrong") }
        assertFailsWith<IllegalArgumentException> { api.tree.allocate("wrong", "STR_START") }
        assertFailsWith<IllegalArgumentException> { api.hero.addExperience("wrong", 10.0) }
        assertFailsWith<IllegalArgumentException> { api.hero.stats("wrong") }
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
