package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.contract.validateIcon
import com.sperance.exileforge.core.display.icons.IconSet
import com.sperance.exileforge.core.display.icons.iconSuggestions
import com.sperance.exileforge.core.display.svg.SvgPaint
import com.sperance.exileforge.core.display.svg.parseSvgIcon
import com.sperance.exileforge.core.display.svg.parseSvgSprite
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.combat.Monster
import com.sperance.exileforge.core.model.command.ApiCapabilities
import com.sperance.exileforge.core.model.command.EquipmentSlot
import com.sperance.exileforge.core.model.icons.IconBindingTables
import com.sperance.exileforge.core.model.icons.IconDescriptor
import com.sperance.exileforge.core.model.icons.IconManifest
import com.sperance.exileforge.core.model.passives.PassiveEffect
import com.sperance.exileforge.core.model.passives.PassiveNode
import com.sperance.exileforge.core.model.passives.PassiveNodeKind
import com.sperance.exileforge.core.model.passives.PassiveOperation
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

/** The icon set is served by the server; this client only picks, caches and draws what it sends. */
class IconContractTest {
    private val plate = """<path d="M6 13 L13 6 H51 L58 13 V51 L51 58 H13 L6 51 Z" fill="url(#plate-weapon-sword)" stroke="#5d6f8a" stroke-width="1.6"/>""" +
        """<circle cx="32" cy="32" r="22" fill="url(#halo-weapon-sword)"/>""" +
        """<path d="M10 14.5 L14.5 10 H49.5 L54 14.5 V49.5 L49.5 54 H14.5 L10 49.5 Z" fill="none" stroke="#dbe4f0" stroke-opacity="0.25" stroke-width="1"/>""" +
        """<circle cx="13" cy="13" r="1.3" fill="#dbe4f0" fill-opacity="0.45"/>"""
    private val defs = """<defs><linearGradient id="edge-weapon-sword" gradientUnits="userSpaceOnUse" x1="32" y1="6" x2="32" y2="58">""" +
        """<stop offset="0" stop-color="#dbe4f0"/><stop offset="1" stop-color="#5d6f8a"/></linearGradient>""" +
        """<radialGradient id="core-weapon-sword" gradientUnits="userSpaceOnUse" cx="32" cy="28" r="28">""" +
        """<stop offset="0" stop-color="#dbe4f0" stop-opacity="0.55"/><stop offset="1" stop-color="#5d6f8a" stop-opacity="0.12"/></radialGradient></defs>"""
    private val art = """<g fill="none" stroke="url(#edge-weapon-sword)" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">""" +
        """<path d="M50 11 L31 36 L26 31 Z" fill="url(#core-weapon-sword)"/><path d="M47 15 L29 33"/><circle cx="19" cy="45" r="2.4"/></g>"""
    private val sword = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64" role="img" aria-label="Меч">""" +
        """<title>Меч</title>$defs$plate$art</svg>"""
    private val sprite = """<svg xmlns="http://www.w3.org/2000/svg" width="0" height="0" style="display:none"><title>ExileForge icon sprite</title>""" +
        """<symbol id="icon-weapon-sword" viewBox="0 0 64 64"><title>Меч</title>$defs$plate$art</symbol>""" +
        """<symbol id="icon-ui-unknown" viewBox="0 0 64 64"><title>?</title>$defs$plate$art</symbol></svg>"""

    private fun MockWebServer.ok(data: String) = enqueue(MockResponse().setBody("""{"success":true,"data":$data}"""))

    @Test fun `plate and drawing are separated so one download serves both variants`() {
        val icon = assertNotNull(parseSvgIcon(sword, "weapon-sword"))
        assertEquals(64f, icon.viewBox)
        assertEquals(4, icon.frame.size)
        assertEquals(3, icon.art.size)
        assertEquals(7, icon.shapes(framed = true).size)
        assertEquals(icon.art, icon.shapes(framed = false))
        // The drawing inherits stroke, width and joins from the group it sits in.
        assertEquals(SvgPaint.Ref("edge-weapon-sword"), icon.art[1].stroke)
        assertEquals(2.4f, icon.art[1].strokeWidth)
        assertNull(icon.art[1].fill)
        assertEquals(SvgPaint.Ref("core-weapon-sword"), icon.art[0].fill)
        // A circle becomes path data: a renderer then only has to understand paths.
        assertEquals("M16.6 45.0 a 2.4 2.4 0 1 1 4.8 0 a 2.4 2.4 0 1 1 -4.8 0 Z", icon.art[2].data)
    }

    @Test fun `colours keep their opacity and gradients keep the coordinates they were drawn in`() {
        val icon = assertNotNull(parseSvgIcon(sword, "weapon-sword"))
        assertEquals(SvgPaint.Solid(0xFF5d6f8aL), icon.frame[0].stroke)
        assertEquals(SvgPaint.Solid(0x72dbe4f0L), icon.frame[3].fill) // fill-opacity 0.45
        assertNull(icon.frame[2].fill)                                 // fill="none" paints nothing
        val edge = assertNotNull(icon.gradients["edge-weapon-sword"])
        assertFalse(edge.radial)
        assertEquals(listOf(32f, 6f, 32f, 58f), listOf(edge.x1, edge.y1, edge.x2, edge.y2))
        val core = assertNotNull(icon.gradients["core-weapon-sword"])
        assertTrue(core.radial)
        assertEquals(28f, core.radius)
        assertEquals(0x8Cdbe4f0L, core.stops.first().argb)             // stop-opacity 0.55
    }

    @Test fun `the sprite carries the whole set under its symbol ids`() {
        val icons = parseSvgSprite(sprite)
        assertEquals(setOf("weapon-sword", "ui-unknown"), icons.keys)
        assertEquals(3, icons.getValue("weapon-sword").art.size)
        assertEquals(emptyMap(), parseSvgSprite("not an svg at all"))
    }

    @Test fun `icon routes are public, conditional and reported by capabilities`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val journal = RequestJournal(); val api = GameApi(server.url("/").toString(), journal)
            server.ok("""{"apiRevision":4,"versionedCrud":true,"characterCommands":true,"equipmentComparison":true,"catalogSearch":true,"craftOptions":true,"icons":true,"iconSet":"forge-vector","iconSetRevision":1,"iconSetVersion":"abc123","iconCount":120,"iconsEndpoint":"/api/v1/icons"}""")
            val capabilities = api.capabilities()
            assertTrue(capabilities.hasIcons())
            assertEquals("forge-vector", capabilities.iconSet)
            server.takeRequest()
            server.ok("""{"set":"forge-vector","revision":1,"version":"abc123","total":1,"icons":[{"id":"weapon-sword","title":"Меч","category":"WEAPON","tint":"#dbe4f0","deep":"#5d6f8a","url":"/api/v1/icons/weapon-sword.svg"}]}""")
            val manifest = api.icons(category = "WEAPON", query = "sword")
            assertEquals("weapon-sword", manifest.icons.single().id)
            val query = server.takeRequest()
            assertEquals("/api/v1/icons", query.requestUrl!!.encodedPath)
            assertEquals("WEAPON", query.requestUrl!!.queryParameter("category"))
            assertEquals("sword", query.requestUrl!!.queryParameter("q"))
            assertNull(query.getHeader("Authorization"))
            server.enqueue(MockResponse().setBody(sprite).setHeader("ETag", "\"deadbeef\"").setHeader("Content-Type", "image/svg+xml"))
            val downloaded = api.iconSprite()
            assertEquals("\"deadbeef\"", downloaded.etag)
            assertEquals(2, parseSvgSprite(downloaded.svg).size)
            assertEquals("/api/v1/icons/sprite.svg", server.takeRequest().requestUrl!!.encodedPath)
            // An unchanged set costs one conditional request and no picture.
            server.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "\"deadbeef\""))
            val again = api.iconSprite(downloaded.etag)
            assertTrue(again.unchanged)
            assertEquals("", again.svg)
            assertEquals("\"deadbeef\"", server.takeRequest().getHeader("If-None-Match"))
            // Pictures never reach the journal as text, and an unknown id never reaches the network.
            assertFalse(journal.entries.value.toString().contains("<symbol"))
            assertFailsWith<IllegalArgumentException> { api.iconSvg("../secret") }
            server.enqueue(MockResponse().setBody(sword).setHeader("ETag", "\"one\""))
            assertEquals(3, assertNotNull(parseSvgIcon(api.iconSvg("weapon-sword", plain = true).svg, "weapon-sword")).art.size)
            assertEquals("plain", server.takeRequest().requestUrl!!.queryParameter("variant"))
        }
    }

    @Test fun `an older server leaves the client on its bundled emblems`() {
        assertFalse(ApiCapabilities(apiRevision = 3, icons = false).hasIcons())
        assertFalse(ApiCapabilities(apiRevision = 4, icons = true).hasIcons())
        val empty = IconSet()
        assertFalse(empty.ready)
        assertNull(empty.drawing("weapon-sword"))
        assertNull(empty.forDocument(buildJsonObject { put("slot", "HELMET") }))
    }

    @Test fun `the server value wins and the binding tables answer for documents written before the set`() {
        val set = IconSet(IconManifest(fallback = "ui-unknown", icons = listOf(IconDescriptor("weapon-bow"))), bindings)
        assertEquals("weapon-bow", set.forDocument(buildJsonObject { put("icon", "weapon-bow"); put("slot", "HELMET") }))
        assertEquals("jewellery-ring", set.forDocument(buildJsonObject { put("poeBaseId", "Metadata/Items/Rings/Ring1") }))
        assertEquals("weapon-sword", set.forDocument(buildJsonObject { put("weaponType", "SWORD"); put("slot", "WEAPON_1H") }))
        assertEquals("armour-helmet", set.forDocument(buildJsonObject { put("slot", "HELMET") }))
        assertEquals("item-flask", set.forDocument(buildJsonObject { put("category", "Life Flask") }))
        assertEquals("ui-character", set.forDocument(buildJsonObject { put("userId", "u") }))
        assertNull(set.forDocument(buildJsonObject { put("name", "Nothing known") }))
        assertEquals("stat-life", set.forStat("MAXIMUM_LIFE"))
        assertEquals("stat-life", set.forProperty("maximum_life"))
        assertEquals("affix-prefix", set.forModifier(buildJsonObject { put("definitionId", "unknown"); put("source", "PREFIX") }))
        assertEquals("stat-life", set.forModifier(buildJsonObject { put("definitionId", "life_bonus") }))
        assertEquals("armour-shield", set.forSlot(EquipmentSlot.OFF_HAND))
        assertEquals("jewellery-ring", set.forSlot(EquipmentSlot.RING_RIGHT))
        assertEquals("rarity-rare", set.forRarity("RARE"))
        assertEquals("currency-chaos", set.forCurrency("CHAOS"))
        assertEquals("combat-attack", set.forBattleAction("ATTACK"))
    }

    @Test fun `monsters and passive nodes fall back the way the server resolves them`() {
        val set = IconSet(IconManifest(), bindings)
        val monster = Monster("m", "Beast", 10.0, 1.0, element = "fire", lootTableId = "t", experience = 1, gold = 1)
        assertEquals("combat-monster", set.forMonster(monster.copy(icon = "combat-monster")))
        assertEquals("stat-fire-damage", set.forMonster(monster))
        assertEquals("combat-boss", set.forMonster(monster.copy(boss = true)))
        val life = PassiveEffect("maximum_life", PassiveOperation.FLAT, 5.0)
        assertEquals("stat-life", set.forNode(node(PassiveNodeKind.SMALL, life)))
        assertEquals("passive-keystone", set.forNode(node(PassiveNodeKind.KEYSTONE, life)))
        assertEquals("passive-notable", set.forNode(node(PassiveNodeKind.SMALL, life).copy(icon = "passive-notable")))
    }

    @Test fun `an icon outside the loaded set is rejected before the request is built`() {
        val document = buildJsonObject {
            put("name", "Ring"); put("category", "Currency"); put("subCategory", "Shard"); put("price", 1L); put("icon", "weapon-bow")
        }
        iconSuggestions = emptyList()
        validate(document, Catalog.ITEMS)
        iconSuggestions = listOf("weapon-sword")
        assertFailsWith<IllegalArgumentException> { validate(document, Catalog.ITEMS) }
        iconSuggestions = listOf("weapon-bow")
        validate(document, Catalog.ITEMS)
        assertFailsWith<IllegalArgumentException> { validateIcon(buildJsonObject { put("icon", "https://example.com/a.svg") }) }
        validateIcon(buildJsonObject { put("icon", JsonNull) })
        iconSuggestions = emptyList()
    }

    private fun node(kind: PassiveNodeKind, vararg effects: PassiveEffect) =
        PassiveNode("n", "Node", "", kind, 0.0, 0.0, effects.toList())

    private val bindings = IconBindingTables(
        stats = mapOf("maximum_life" to "stat-life"),
        tags = mapOf("life" to "stat-life"),
        modifierSources = mapOf("PREFIX" to "affix-prefix"),
        itemClasses = mapOf("Life Flask" to "item-flask", "Ring" to "jewellery-ring"),
        weapons = mapOf("SWORD" to "weapon-sword"),
        slots = mapOf("HELMET" to "armour-helmet", "SHIELD" to "armour-shield", "RING" to "jewellery-ring"),
        rarities = mapOf("RARE" to "rarity-rare"),
        currencies = mapOf("CHAOS" to "currency-chaos"),
        passiveKinds = mapOf("SMALL" to "passive-small", "KEYSTONE" to "passive-keystone"),
        battleActions = mapOf("ATTACK" to "combat-attack"),
        combatElements = mapOf("fire" to "stat-fire-damage"),
        modifiers = mapOf("life_bonus" to "stat-life"),
        bases = mapOf("Metadata/Items/Rings/Ring1" to "jewellery-ring"))
}
