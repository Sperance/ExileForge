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

    @Before fun before(): Unit = runBlocking {
        server = MockWebServer(); server.start(); api = GameApi(server.url("/game/").toString(), journal)
        // The app always has the server's dictionary by the time it reads anything: documents carry
        // codes since 0.14.0, so without it every assertion below would be about a code.
        serverLocale = LocaleBundle.parse("ru", "sha", """{
            "equipment.IRON_SKULLCAP.name": "Iron Skullcap", "item.CHAOS_ORB.name": "Chaos Orb",
            "class.MARAUDER.name": "Marauder", "class.MARAUDER.description": "Сила",
            "currency.chaos": "{0}: перекатаны аффиксы, всего {1}"}""")
        ok("""{"id":"$id","version":1,"name":"Admin","login":"admin","role":"ADMIN","isActive":true}""")
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
        ok("""{"id":"$other","version":3,"name":"","login":"","role":"USER","isActive":true,"countCharacters":2}""")
        val profile = api.loginByDevice("device-uuid")
        assertEquals("/game/api/v1/user/login/byDeviceId?deviceId=device-uuid", server.takeRequest().path)
        assertEquals(other, profile.id)
        assertEquals(2, profile.countCharacters)
        // The account document is the whole session, exactly as a password login leaves it.
        assertEquals(other, assertNotNull(api.currentUser()).id)
        // One request: an account that exists is never re-registered.
        assertEquals(sent + 1, server.requestCount)
    }

    @Test fun `an unknown device is registered on the spot`(): Unit = runBlocking {
        api.logout()
        // US_015 is not an error to report — it is the server saying "this one is new".
        refusal(404, "US_015", "User with deviceId device-uuid not found")
        ok("""{"id":"$other","version":0,"name":"","login":"","role":"USER","isActive":true}""")
        assertEquals(other, api.loginByDevice("device-uuid").id)
        assertEquals("/game/api/v1/user/login/byDeviceId?deviceId=device-uuid", server.takeRequest().path)
        val registration = server.takeRequest()
        assertEquals("POST", registration.method)
        assertEquals("/game/api/v1/user/byDeviceId?deviceId=device-uuid", registration.path)
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
        val manifest = api.iconManifest()
        assertEquals("/game/icons/index.json", server.takeRequest().path)
        assertEquals("cc20a339", manifest.hash)
        assertEquals(3, manifest.icons)

        server.enqueue(MockResponse().setBody("""{"sprites":{"orb":{"viewBox":24,"paths":[{"d":"M1 1h2v2h-2z","alpha":1.0}]}},
            "icons":{"item.CHAOS_ORB":"orb"}}"""))
        val bundle = IconBundle.parse(manifest.hash, api.iconDocument(manifest.file))
        assertEquals("/game/icons/icons.json", server.takeRequest().path)
        assertEquals("cc20a339", bundle.hash)
        assertNotNull(bundle[IconKey.item("CHAOS_ORB")])
        // The manifest names the file, so a renamed set is still fetched; a blank name is refused.
        assertFailsWith<IllegalArgumentException> { api.iconDocument("") }
    }

    @Test fun `the character menu reads one account's characters and no one else's`(): Unit = runBlocking {
        val sent = server.requestCount
        ok("""[{"_id":"$id","userId":"$other","name":"Изгнанник","level":7,"classId":"$id"},
               {"_id":"$other","userId":"$other","name":"Ведьма","level":1,"classId":"$other"}]""")
        val mine = api.charactersOf(other)
        assertEquals("/game/api/v1/character/byUser?userId=$other", server.takeRequest().path)
        assertEquals(listOf("Изгнанник", "Ведьма"), mine.map { it.name })
        assertEquals(7, mine.first().level)
        // The id is checked before the request is built, as everywhere else.
        assertFailsWith<IllegalArgumentException> { api.charactersOf("nope") }
        assertEquals(sent + 1, server.requestCount)
    }

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
        assertEquals(other, api.create(Catalog.CHARACTERS, document).entityId)
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
        val refused = assertFailsWith<IllegalArgumentException> { api.create(Catalog.CHARACTERS, draft) }
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
        assertEquals(7L, api.update(Catalog.ITEMS, id, changes).entityVersion)
        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/game/api/v1/items?id=$id", update.path)
        assertEquals(changes, WireJson.parseToJsonElement(update.body.readUtf8()).jsonObject)
        ok("\"system.deleted\""); api.delete(Catalog.ITEMS, id)
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

    @Test fun `orbs are the currency category of the shared items collection`(): Unit = runBlocking {
        val catalogue = JsonArray(listOf(
            buildJsonObject { put("_id", id); put("code", "CHAOS_ORB"); put("category", "CURRENCY"); put("subCategory", "CHAOS_ORB"); put("price", 300) },
            buildJsonObject { put("_id", other); put("code", "ORB_OF_TRANSMUTATION"); put("category", "CURRENCY"); put("subCategory", "ORB_OF_TRANSMUTATION"); put("price", 10) },
            buildJsonObject { put("_id", id); put("code", "IRON_SHARD"); put("category", "STONE_STOCK"); put("subCategory", "STONE"); put("price", 1) }))
        ok(catalogue.toString())
        val orbs = api.currencyOrbs()
        assertEquals("/game/api/v1/items", server.takeRequest().path)
        // Only the currency category, cheapest first; a document carries a code and no text at all.
        assertEquals(listOf("ORB_OF_TRANSMUTATION", "CHAOS_ORB"), orbs.map { it.code })
        assertEquals(CurrencyOrb.CHAOS_ORB, orbs.last().orb)
        // The dictionary wins over the client's own table: the server owns what a thing is called.
        assertEquals("Chaos Orb", orbs.last().title())
    }

    @Test fun `applying an orb names the pair and prints what the server did`(): Unit = runBlocking {
        val rerolled = """{"_id":"$id","characterId":"$other","equipmentId":"$other","rarity":"RARE","corrupted":false,
            "params":[{"modifierId":"$id","tierId":"$other","tier":2,"values":[7.0]}]}"""
        ok("""{"messageKey":"currency.chaos","messageArgs":["equipment.IRON_SKULLCAP.name","4"],"item":$rerolled}""")
        val outcome = api.applyOrb(other, id, other)
        assertEquals("/game/api/v1/characterequipment/applyOrb?characterId=$other&inventoryId=$id&orbItemId=$other", server.takeRequest().path)
        // The server sends a key and arguments; the arguments are keys too, so the item is named.
        assertEquals("Iron Skullcap: перекатаны аффиксы, всего 4", outcome.message)
        assertEquals("RARE", outcome.item.rarity)
        assertFalse(outcome.item.corrupted)
        // A mirror is the one orb that answers with a second document, and the copy is locked.
        ok("""{"message":"Helm was mirrored","item":$rerolled,"created":{"_id":"$other","characterId":"$other","equipmentId":"$other","rarity":"RARE","corrupted":true,"params":[]}}""")
        val copy = assertNotNull(api.applyOrb(other, id, other).created)
        assertEquals(other, copy.id)
        assertTrue(copy.corrupted)
        server.takeRequest()
    }

    @Test fun `stats and the bag come from the server as they are`(): Unit = runBlocking {
        // The sheet reports the numbers and the server's verdict on every worn item alongside them.
        ok("""{"characterId":"$id","level":12,"stats":{"STOCK_HEALTH":188.4,"STOCK_ARMOR":40.0},"active":["$other"],
            "inactive":[{"inventoryId":"$id","code":"IRON_SKULLCAP","reasons":["strength: need 30, have 14"]}]}""")
        val sheet = api.stats(id)
        assertEquals(188.4, sheet.stats.getValue("STOCK_HEALTH"))
        assertEquals(12, sheet.level)
        assertEquals(listOf(other), sheet.active)
        assertEquals("strength: need 30, have 14", sheet.inactive.single().reasons.single())
        assertEquals("/game/api/v1/character/inventory/stats?characterId=$id", server.takeRequest().path)
        ok("""[{"itemId":"chaos_orb","amount":50}]""")
        assertEquals(50L, api.bag(id).single().amount)
        assertEquals("/game/api/v1/character/inventory/items?characterId=$id", server.takeRequest().path)
        ok("\"system.success\"")
        // The server answers a locale key now, not a word; the client passes it on untouched.
        assertEquals("system.success", api.adjustItems(id, listOf(ItemStack("chaos_orb", -2))))
        val adjust = server.takeRequest()
        assertEquals("/game/api/v1/character/inventory/addItem?characterId=$id", adjust.path)
        assertEquals(-2, WireJson.parseToJsonElement(adjust.body.readUtf8()).jsonArray.single().jsonObject.getValue("amount").jsonPrimitive.int)
        assertFailsWith<IllegalArgumentException> { ItemStack("chaos_orb", 0) }
    }

    @Test fun `the world's reference tables are read whole and once`(): Unit = runBlocking {
        ok("""[{"_id":"$id","code":"MARAUDER","startNodeCode":"STR_START",
            "baseStats":[{"stat":"STOCK_STRENGTH","value":32.0}],"perLevelStats":[{"stat":"STOCK_HEALTH","value":12.0}],
            "params":[{"modifierId":"$other","values":[1.0]}]}]""")
        val marauder = api.characterClasses().single()
        assertEquals("/game/api/v1/characterclass", server.takeRequest().path)
        // The class document has no text at all: its name is the dictionary's, under its code.
        assertEquals("Marauder", marauder.title)
        assertEquals("Сила", marauder.details)
        assertEquals(32.0, marauder.baseStats.single().value)
        // A class conversion is fixed by the reference table, so it carries no tier at all.
        assertFalse(marauder.params.single().rolled)

        ok("""[{"_id":"$other","level":3,"experience":300.0,"skillPoints":2},{"_id":"$id","level":1,"experience":0.0}]""")
        val levels = api.experienceLevels()
        assertEquals("/game/api/v1/experiencelevel", server.takeRequest().path)
        assertEquals(listOf(1, 3), levels.map { it.level })
        assertEquals(2, levels.last().skillPoints)
    }

    @Test fun `the skill tree is one graph and every command answers with the whole state`(): Unit = runBlocking {
        ok("""[{"_id":"$id","code":"STR_START","type":"START","cost":0,"positionX":-40,"positionY":0,
            "connections":["STR_LIFE_1"],"params":[{"modifierId":"$other","values":[10.0]}]}]""")
        val node = api.skillTree().single()
        assertEquals("/game/api/v1/skilltreenode", server.takeRequest().path)
        assertEquals(SkillNodeType.START, node.type)
        assertEquals(listOf("STR_LIFE_1"), node.connections)
        assertFalse(node.params.single().rolled)

        val state = """{"characterId":"$id","total":5,"spent":1,"available":4,
            "nodes":[{"code":"STR_START","type":"START","cost":0,"params":[]}]}"""
        ok(state)
        assertEquals(setOf("STR_START"), api.characterTree(id).takenCodes)
        assertEquals("/game/api/v1/character/skilltree/state?characterId=$id", server.takeRequest().path)
        ok(state)
        assertEquals(4, api.allocateNode(id, "STR_LIFE_1").available)
        assertEquals("/game/api/v1/character/skilltree/allocate?characterId=$id&nodeCode=STR_LIFE_1", server.takeRequest().path)
        ok(state); api.refundNode(id, "STR_LIFE_1")
        assertEquals("/game/api/v1/character/skilltree/refund?characterId=$id&nodeCode=STR_LIFE_1", server.takeRequest().path)
        ok(state); api.resetTree(id)
        assertEquals("/game/api/v1/character/skilltree/reset?characterId=$id", server.takeRequest().path)
        // A node is named by its code, never by an id, and a blank one never reaches the network.
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.allocateNode(id, " ") }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `experience is granted and the level comes back from the server`(): Unit = runBlocking {
        ok("""{"_id":"$id","userId":"$other","name":"Изгнанник","classId":"$id","level":7,"experience":1200.0}""")
        assertEquals(7, api.addExperience(id, 1200.0).level)
        assertEquals("/game/api/v1/character/inventory/experience?characterId=$id&amount=1200.0", server.takeRequest().path)
        val sent = server.requestCount
        // Taking experience away is not a route this server has; the level only ever rises.
        assertFailsWith<IllegalArgumentException> { api.addExperience(id, -5.0) }
        assertFailsWith<IllegalArgumentException> { api.addExperience(id, 0.0) }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `the showcase is narrowed by the server and only the set fields travel`(): Unit = runBlocking {
        ok("""{"items":[{"_id":"$id","sellerId":"$other","sellerName":"Изгнанник","kind":"EQUIPMENT","itemCode":"IRON_SKULLCAP",
            "slot":"HELMET","rarity":"RARE","itemLevel":30,"priceOrbId":"$other","price":40,"status":"ACTIVE",
            "equipment":{"_id":"$other","equipmentId":"$id","rarity":"RARE","params":[]}}],
            "page":1,"pageSize":20,"totalItems":25,"totalPages":2}""")
        val filter = AuctionFilter(title = "skull", kind = "EQUIPMENT", maxPrice = "50", excludeSellerId = other)
        val page = api.auctionSearch(id, filter, 1)
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
        assertEquals("Iron Skullcap", api.sellEquipment(other, id, other, 40).title)
        assertEquals("/game/api/v1/auctionlot/sell/equipment?inventoryId=$id&characterId=$other&priceOrbId=$other&price=40", server.takeRequest().path)
        ok("""{"_id":"$id","sellerId":"$other","kind":"ITEM","itemId":"$id","amount":5,"itemCode":"CHAOS_ORB","priceOrbId":"$other","price":2,"status":"ACTIVE"}""")
        assertEquals(5L, api.sellItem(other, id, 5, other, 2).amount)
        assertEquals("/game/api/v1/auctionlot/sell/item?itemId=$id&amount=5&characterId=$other&priceOrbId=$other&price=2", server.takeRequest().path)
        ok("""{"_id":"$id","sellerId":"$other","kind":"EQUIPMENT","itemCode":"IRON_SKULLCAP","status":"SOLD","buyerId":"$id"}""")
        assertEquals(AuctionLotStatus.SOLD, api.buyLot(id, id).status)
        assertEquals("/game/api/v1/auctionlot/buy?characterId=$id&lotId=$id", server.takeRequest().path)
        ok("""{"_id":"$id","sellerId":"$other","kind":"EQUIPMENT","itemCode":"IRON_SKULLCAP","status":"CANCELLED"}""")
        assertFalse(api.cancelLot(other, id).onSale)
        assertEquals("/game/api/v1/auctionlot/cancel?characterId=$other&lotId=$id", server.takeRequest().path)
        ok("""[$lot]""")
        assertEquals(1, api.myLots(other).size)
        assertEquals("/game/api/v1/auctionlot/my?characterId=$other", server.takeRequest().path)
        // A price that buys nothing, or a price not set in orbs, never reaches the network.
        val sent = server.requestCount
        assertFailsWith<IllegalArgumentException> { api.sellEquipment(other, id, other, 0) }
        assertFailsWith<IllegalArgumentException> { api.sellItem(other, id, 0, other, 5) }
        assertFailsWith<IllegalArgumentException> { api.sellEquipment(other, id, "not-an-orb", 5) }
        assertFailsWith<IllegalArgumentException> { api.buyLot(id, "wrong") }
        assertEquals(sent, server.requestCount)
    }

    @Test fun `capabilities are read from the server's own route table`(): Unit = runBlocking {
        val routes = listOf("GET" to "/api/v1/user/login", "GET" to "/api/v1/user/login/byDeviceId",
            "POST" to "/api/v1/user/byDeviceId", "GET" to "/api/v1/character/byUser",
            "GET" to "/api/v1/equipment/paged", "GET" to "/api/v1/character/inventory/equipments",
            "GET" to "/api/v1/character/inventory/stats", "POST" to "/api/v1/character/inventory/itemToInventory",
            "POST" to "/api/v1/characterequipment/equip", "POST" to "/api/v1/characterequipment/applyOrb",
            "GET" to "/api/v1/modifierdefinition", "GET" to "/api/v1/characterclass", "GET" to "/api/v1/experiencelevel",
            "GET" to "/api/v1/skilltreenode", "GET" to "/api/v1/character/skilltree/state",
            "POST" to "/api/v1/character/skilltree/allocate", "GET" to "/api/v1/auctionlot/search",
            "POST" to "/api/v1/auctionlot/sell/equipment", "POST" to "/api/v1/auctionlot/buy",
            "GET" to "/api/v1/equipment", "POST" to "/api/v1/characterequipment/socket",
            "POST" to "/api/v1/characterequipment/unsocket")
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
        // A dead handshake and a silent server share an exception but not a remedy.
        val handshake = java.net.SocketTimeoutException("failed to connect to /10.0.2.2 (port 8080) from /10.0.2.16 (port 36470) after 10000ms")
        assertTrue(transportDetail(handshake).contains("файрвол"), transportDetail(handshake))
        assertTrue(transportDetail(java.net.SocketTimeoutException("timeout")).contains("не ответил вовремя"))
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
        ok("\"system.success\""); api.changePassword("private-old", "private-New1")
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
        assertFailsWith<IllegalArgumentException> { api.applyOrb(id, id, "wrong") }
        assertFailsWith<IllegalArgumentException> { api.characterTree("wrong") }
        assertFailsWith<IllegalArgumentException> { api.allocateNode("wrong", "STR_START") }
        assertFailsWith<IllegalArgumentException> { api.addExperience("wrong", 10.0) }
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
