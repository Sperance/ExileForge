package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.*
import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test

/**
 * Icons as the server serves them since 0.15.0.
 *
 * A drawing is path data, not a picture: the client paints it and tints it, so what is pinned here
 * is the shape of the file and, above all, what happens when part of it is wrong. Every hole has to
 * fall back to a bundled emblem, because an icon is decoration and must never be able to blank a
 * screen — which is exactly what makes a hole easy to leave in by accident.
 */
class ServerIconsTest {
    private val document = """{
        "sprites": {
            "helmet": {"viewBox": 24, "paths": [{"d": "M4 11a8 8 0 0 1 16 0v6h-4v-5h-8v5H4z", "alpha": 1.0},
                                                {"d": "M10 17h4v4h-4z", "alpha": 0.45}]},
            "orb":    {"viewBox": 24, "paths": [{"d": "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18", "alpha": 1.0}]},
            "armour": {"viewBox": 32, "paths": [{"d": "M16 2l12 4v10z", "alpha": 1.0}]}
        },
        "icons": {
            "equipment.IRON_SKULLCAP": "helmet",
            "equipment.SALLET": "helmet",
            "item.CHAOS_ORB": "orb",
            "stat.STOCK_ARMOR": "armour",
            "stat.STOCK_MANA": "nobody_drew_this"
        }
    }"""

    private fun load() { serverIcons = IconBundle.parse("sha-1", document) }
    @After fun forget() { serverIcons = IconBundle() }

    @Test fun `a key is the section and the code, exactly as the server writes it`() {
        assertEquals("equipment.IRON_SKULLCAP", IconKey.equipment("IRON_SKULLCAP"))
        assertEquals("item.CHAOS_ORB", IconKey.item("CHAOS_ORB"))
        assertEquals("stat.STOCK_ARMOR", IconKey.stat("STOCK_ARMOR"))
    }

    @Test fun `without a set nothing resolves, and that is the signal to draw your own`() {
        assertTrue(IconBundle().isEmpty)
        assertNull(icon(IconKey.equipment("IRON_SKULLCAP")))
        load()
        assertEquals(4, serverIcons.size)
        assertEquals(3, serverIcons.spriteCount)
        assertNotNull(icon(IconKey.equipment("IRON_SKULLCAP")))
        assertNull(icon(IconKey.equipment("NOTHING")))
    }

    @Test fun `a drawing is outlines and alpha, and never a colour`() {
        load()
        val helmet = assertNotNull(icon(IconKey.equipment("IRON_SKULLCAP")))
        assertEquals(24f, helmet.viewBox)
        assertEquals(2, helmet.paths.size)
        assertEquals(1f, helmet.paths.first().alpha)
        assertEquals(0.45f, helmet.paths.last().alpha)
        // The grid is the sprite's own: an icon drawn at 32 is not silently squashed into 24.
        assertEquals(32f, assertNotNull(icon(IconKey.stat("STOCK_ARMOR"))).viewBox)
    }

    @Test fun `one drawing serves many codes, which is the point of the table`() {
        load()
        assertEquals("helmet", serverIcons.spriteOf(IconKey.equipment("SALLET")))
        assertEquals(icon(IconKey.equipment("IRON_SKULLCAP")), icon(IconKey.equipment("SALLET")))
    }

    @Test fun `a code pointing at a drawing nobody made is dropped, not served empty`() {
        load()
        // The table named a sprite the file never defined. Serving it as an icon with no outlines
        // would paint nothing at all; dropping it sends the caller to the bundled emblem instead.
        assertFalse(serverIcons.contains(IconKey.stat("STOCK_MANA")))
        assertNull(icon(IconKey.stat("STOCK_MANA")))
        assertEquals(4, serverIcons.size)
    }

    @Test fun `a document finds its drawing the way it finds its name`() {
        load()
        val helmet = buildJsonObject { put("_id", "x"); put("code", "IRON_SKULLCAP"); put("slot", "HELMET") }
        assertEquals(icon(IconKey.equipment("IRON_SKULLCAP")), documentIcon(helmet))
        val orb = buildJsonObject { put("_id", "x"); put("code", "CHAOS_ORB"); put("category", "CURRENCY") }
        assertEquals(icon(IconKey.item("CHAOS_ORB")), documentIcon(orb))
        // A character is a player's, not content: it keeps the client's own emblem.
        assertNull(documentIcon(buildJsonObject { put("userId", "u"); put("code", "IRON_SKULLCAP"); put("name", "Изгнанник") }))
        // So does a document from before the set, which carries no code at all.
        assertNull(documentIcon(buildJsonObject { put("slot", "HELMET") }))
    }

    @Test fun `the manifest carries the fingerprint the server computed`() {
        val manifest = WireJson.decodeFromString(IconManifest.serializer(),
            """{"hash":"cc20a339a7b0a5a0","file":"icons.json","sprites":89,"icons":174}""")
        assertEquals("cc20a339a7b0a5a0", manifest.hash)
        assertEquals("icons.json", manifest.file)
        assertEquals(174, manifest.icons)
    }

    @Test fun `an unknown field in the file does not cost the whole set`() {
        // The set will grow — a sprite may gain a stroke width, the file a version. A client that
        // rejected the lot over one new key would lose every icon the moment the server moved on.
        val bundle = IconBundle.parse("sha-2", """{
            "version": 2, "sprites": {"orb": {"viewBox": 24, "stroke": 2, "paths": [{"d": "M1 1h2v2h-2z", "alpha": 1.0}]}},
            "icons": {"item.CHAOS_ORB": "orb"}
        }""")
        assertEquals(1, bundle.size)
        assertNotNull(bundle[IconKey.item("CHAOS_ORB")])
    }
}
