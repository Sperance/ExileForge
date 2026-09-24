package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.currency.*
import com.sperance.exileforge.core.model.skilltree.*
import com.sperance.exileforge.core.model.modifier.*
import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.Test

class ContractTest {
    private val id = "0123456789abcdef01234567"

    @Test fun `a diff carries changed editable fields only`() {
        val original = buildJsonObject { put("_id", id); put("version", 4); put("type", "X"); put("name", "Old"); put("itemLevel", 30) }
        val edited = JsonObject(original + mapOf("name" to JsonPrimitive("New"), "version" to JsonPrimitive(9), "type" to JsonPrimitive("Y")))
        assertEquals(buildJsonObject { put("name", "New") }, diff(original, edited))
        assertEquals(JsonObject(emptyMap()), diff(original, original))
    }

    @Test fun `every editable field exists in its form and no service field does`() {
        Catalog.entries.forEach { catalog ->
            val document = if (catalog == Catalog.EQUIPMENT) template(catalog, EquipmentKind.Weapon) else template(catalog)
            val fields = com.sperance.exileforge.core.editor.schemaFields(com.sperance.exileforge.core.editor.formSchema(catalog), document).map { it.key }.toSet()
            assertTrue(editableFields(catalog).none { it in protectedFields }, "$catalog exposes a service field")
            // Weapon-only fields appear on their own kind; the rest must be in the form.
            val always = editableFields(catalog) - setOf("weaponType", "durability")
            assertTrue(fields.containsAll(always), "$catalog is missing ${always - fields}")
        }
    }

    @Test fun `equipment validation follows the server's own rules`() {
        val weapon = template(Catalog.EQUIPMENT, EquipmentKind.Weapon)
        validate(weapon, Catalog.EQUIPMENT)
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("rarity" to JsonPrimitive("LEGENDARY"))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("slot" to JsonPrimitive("POCKET"))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("requiredLevel" to JsonPrimitive(0))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("requiredStrength" to JsonPrimitive(-1))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon - "type"), Catalog.EQUIPMENT) }
        // Damage, attack speed and defence are implicit modifiers since 0.10.0, not item fields.
        assertFalse(listOf("damage_min", "damage_max", "attackSpeed", "defense").any { it in weapon })
        assertFalse(listOf("damage_min", "damage_max", "attackSpeed", "defense").any { it in editableFields(Catalog.EQUIPMENT) })
        // UNIQUE is a rarity of this server; LEGENDARY is not.
        validate(JsonObject(weapon + ("rarity" to JsonPrimitive("UNIQUE"))), Catalog.EQUIPMENT)
    }

    @Test fun `a character document never carries equipment or a malformed bag`() {
        val character = JsonObject(template(Catalog.CHARACTERS) + mapOf("userId" to JsonPrimitive(id), "name" to JsonPrimitive("Изгнанник")))
        validate(character, Catalog.CHARACTERS)
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("equipped" to JsonObject(emptyMap()))), Catalog.CHARACTERS) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("userId" to JsonPrimitive("nope"))), Catalog.CHARACTERS) }
        validate(JsonObject(character + ("items" to buildJsonArray { add("chaos_orb:50") })), Catalog.CHARACTERS)
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("items" to buildJsonArray { add("chaos_orb") })), Catalog.CHARACTERS) }
        // The same stat may not be listed twice: the server keeps one entry per stat.
        val twice = buildJsonArray { add(buildJsonObject { put("stat", "PROFESSION_MINER"); put("level", 1) }); add(buildJsonObject { put("stat", "PROFESSION_MINER"); put("level", 2) }) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("professionSkills" to twice)), Catalog.CHARACTERS) }
    }

    @Test fun `a rolled modifier points at a definition and a tier`() {
        val modifier = buildJsonObject { put("modifierId", id); put("tierId", id); put("tier", 2); put("values", buildJsonArray { add(12.5); add(3.0) }) }
        validateModifier(modifier)
        assertFailsWith<IllegalArgumentException> { validateModifier(JsonObject(modifier + ("modifierId" to JsonPrimitive("life")))) }
        assertFailsWith<IllegalArgumentException> { validateModifier(JsonObject(modifier + ("tier" to JsonPrimitive(0)))) }
    }

    @Test fun `a composite modifier decodes one value per effect`() {
        val definition = WireJson.decodeFromString(ModifierDefinition.serializer(),
            """{"_id":"$id","code":"LIFE_AND_MANA","source":"PREFIX","tags":["life"],
                "effects":[{"stat":"STOCK_HEALTH","operation":"ADD"},{"stat":"STOCK_MANA","operation":"ADD"}]}""")
        assertTrue(definition.composite)
        // A definition carries no text since 0.14.0: without the dictionary its code stands in.
        assertEquals("LIFE_AND_MANA", definition.template)
        assertEquals(listOf(ModifierOperation.ADD, ModifierOperation.ADD), definition.effects.map { it.operation })
        val tier = WireJson.decodeFromString(ModifierTier.serializer(),
            """{"_id":"$id","modifierId":"$id","tier":1,"minItemLevel":84,"weight":100,"values":[{"valueMin":46.0,"valueMax":48.0},{"valueMin":10.0,"valueMax":12.0}]}""")
        assertEquals(2, tier.values.size)
        assertEquals(84, tier.minItemLevel)
    }

    @Test fun `filters compare stored fields and nothing else`() {
        val bow = buildJsonObject { put("name", "Short Bow"); put("slot", "WEAPON_2H"); put("weaponType", "BOW"); put("rarity", "RARE"); put("itemLevel", 40); put("modifierPools", buildJsonArray { add("bow") }); put("pools", buildJsonObject { put("drop", 100) }) }
        assertTrue(CatalogFilter().isEmpty)
        assertTrue(CatalogFilter(query = "short", slot = "WEAPON_2H", rarity = "RARE", minLevel = "10", maxLevel = "40", weaponType = "BOW", pool = "bow").matches(bow))
        assertTrue(CatalogFilter(pool = "drop").matches(bow))
        assertFalse(CatalogFilter(maxLevel = "39").matches(bow))
        assertFalse(CatalogFilter(pool = "other").matches(bow))
        assertFalse(CatalogFilter(query = "sword").matches(bow))
    }

    @Test fun `an item base is fixed modifiers and never a rolled one`() {
        val weapon = template(Catalog.EQUIPMENT, EquipmentKind.Weapon)
        val base = buildJsonArray { add(buildJsonObject { put("modifierId", id); put("values", buildJsonArray { add(12.0) }) }) }
        validate(JsonObject(weapon + ("baseParams" to base)), Catalog.EQUIPMENT)
        // A base is not rolled: a tier on it would mean the server had rolled the item's own armour.
        val rolled = buildJsonArray { add(buildJsonObject { put("modifierId", id); put("tierId", id); put("values", buildJsonArray { add(12.0) }) }) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("baseParams" to rolled)), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("baseParams" to buildJsonArray { add(buildJsonObject { put("modifierId", "life") }) })), Catalog.EQUIPMENT) }
        // The same split applies to an applied modifier: rolled names a tier, fixed does not.
        validateModifier(buildJsonObject { put("modifierId", id); put("values", buildJsonArray { add(4.0) }) })
        validateModifier(buildJsonObject { put("modifierId", id); put("tierId", id); put("tier", 2); put("values", buildJsonArray { add(4.0) }) })
        assertFailsWith<IllegalArgumentException> { validateModifier(buildJsonObject { put("modifierId", id); put("tier", 3); put("values", buildJsonArray { add(4.0) }) }) }
    }

    @Test fun `a character carries a class and no base stats of its own`() {
        val character = JsonObject(template(Catalog.CHARACTERS) + mapOf(
            "userId" to JsonPrimitive(id), "name" to JsonPrimitive("Изгнанник"), "classId" to JsonPrimitive(id)))
        validate(character, Catalog.CHARACTERS)
        assertTrue("classId" in creationFields(Catalog.CHARACTERS))
        // The class is chosen once: the server has no route that moves a character to another one.
        assertFalse("classId" in editableFields(Catalog.CHARACTERS))
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("classId" to JsonPrimitive("marauder"))), Catalog.CHARACTERS) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("stockSkills" to JsonArray(emptyList()))), Catalog.CHARACTERS) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("params" to JsonArray(emptyList()))), Catalog.CHARACTERS) }
    }

    @Test fun `the orb table matches the currency the server seeds`() {
        // EnumCurrencyOrb on the server; a sub-category outside it is served under its document name.
        assertEquals(setOf("ORB_OF_TRANSMUTATION", "ORB_OF_AUGMENTATION", "ORB_OF_ALTERATION", "ORB_OF_ALCHEMY", "REGAL_ORB",
            "CHAOS_ORB", "EXALTED_ORB", "DIVINE_ORB", "ORB_OF_ANNULMENT", "ORB_OF_SCOURING", "BLESSED_ORB", "VAAL_ORB",
            "ORB_OF_CHANCE", "MIRROR_OF_KALANDRA", "FRACTURING_ORB", "SHAPERS_ORB", "ELDER_ORB", "ORB_OF_REGRET",
            "EMPOWERING_ORB", "MERCY_ORB", "PERIL_ORB", "HORDE_ORB", "MAGUS_ORB", "ELITE_ORB", "BOUNTY_ORB"), CurrencyOrb.entries.map { it.name }.toSet())
        assertEquals(CurrencyOrb.VAAL_ORB, CurrencyOrb.of("VAAL_ORB"))
        assertNull(CurrencyOrb.of("ORB_OF_FUSING"))
        // An orb is called by its English name in every language, as in PoE; what it does is translated.
        assertTrue(CurrencyOrb.entries.all { it.title(Lang.RU) == it.title(Lang.EN) })
        assertEquals("Divine Orb", CurrencyOrb.DIVINE_ORB.title(Lang.RU))
        assertNotEquals(CurrencyOrb.DIVINE_ORB.rule(Lang.RU), CurrencyOrb.DIVINE_ORB.rule(Lang.EN))
        assertTrue(CurrencyOrb.entries.all { it.rule(Lang.RU).isNotBlank() && it.rule(Lang.EN).isNotBlank() })
        // An orb is a document with a code and no text; without the dictionary the client's own
        // table names the ones it knows, and an unknown one is shown by its code.
        val chaos = CurrencyItem(id, "CHAOS_ORB", "CHAOS_ORB", 1)
        assertEquals("Chaos Orb", chaos.title(Lang.RU))
        val unknown = CurrencyItem(id, "ORB_OF_FUSING", "ORB_OF_FUSING", 5)
        assertNull(unknown.orb)
        assertEquals("ORB_OF_FUSING", unknown.title(Lang.EN))
    }

    @Test fun `the reachable nodes are the neighbours of what is taken`() {
        fun node(code: String, type: SkillNodeType, vararg links: String) =
            SkillTreeNode(code = code, type = type, connections = links.toList())
        val tree = listOf(
            node("STR_START", SkillNodeType.START, "STR_LIFE_1"),
            node("INT_START", SkillNodeType.START, "INT_MANA_1"),
            node("STR_LIFE_1", SkillNodeType.SMALL, "STR_START", "STR_LIFE_2"),
            // Declared on one end only: the edge still has to be visible from the other.
            node("STR_LIFE_2", SkillNodeType.NOTABLE),
            node("INT_MANA_1", SkillNodeType.SMALL, "INT_START"))
        // Nothing taken: the only way in is a start node, exactly as the server has it.
        assertEquals(setOf("STR_START", "INT_START"), reachableFrom(tree, emptySet()))
        assertEquals(setOf("STR_LIFE_1"), reachableFrom(tree, setOf("STR_START")))
        assertEquals(setOf("STR_LIFE_2"), reachableFrom(tree, setOf("STR_START", "STR_LIFE_1")))
        // A taken node is never offered again, and an unknown code contributes nothing.
        assertTrue(reachableFrom(tree, setOf("STR_START", "STR_LIFE_1", "STR_LIFE_2")).none { it in setOf("STR_START", "STR_LIFE_1") })
        assertEquals(emptySet(), reachableFrom(tree, setOf("NOWHERE")))
    }

    @Test fun `an auction filter sends only the fields that are set`() {
        assertTrue(AuctionFilter().isEmpty)
        assertEquals(emptyMap(), AuctionFilter().query())
        // A blank enum would be rejected by the server outright, so it never leaves the client.
        val filter = AuctionFilter(title = "  skull  ", kind = "EQUIPMENT", slot = "", maxPrice = " 40 ")
        assertEquals(mapOf("title" to "skull", "kind" to "EQUIPMENT", "maxPrice" to "40"), filter.query())
        assertFalse(filter.isEmpty)
        assertEquals("Экипировка", lotKindTitle(AuctionLotKind.EQUIPMENT, Lang.RU))
        assertEquals("Sold", lotStatusTitle(AuctionLotStatus.SOLD, Lang.EN))
        // A lot knows whose it is; buying one's own is refused by the server, not hidden here.
        val lot = AuctionLot(id = id, sellerId = id, status = AuctionLotStatus.ACTIVE)
        assertTrue(lot.onSale && lot.belongsTo(id))
        assertFalse(AuctionLot(status = AuctionLotStatus.CANCELLED).onSale)
    }

    @Test fun `the showcase filter names what is set and drops one thing at a time`() {
        val filter = AuctionFilter(title = "visor", kind = "EQUIPMENT", slot = "HELMET", minItemLevel = " ", maxPrice = " 5 ", priceOrbId = "chaos")
        // The name is the search field, not a chip; a blank level is not a filter.
        assertEquals(listOf(FilterField.KIND, FilterField.SLOT, FilterField.ORB, FilterField.MAX_PRICE), filter.active())
        assertEquals("5", filter.value(FilterField.MAX_PRICE))
        val narrower = filter.without(FilterField.SLOT)
        assertEquals(listOf(FilterField.KIND, FilterField.ORB, FilterField.MAX_PRICE), narrower.active())
        assertEquals("visor", narrower.title)
        // Reset keeps the typed name and nothing else.
        assertEquals(AuctionFilter(title = "visor"), filter.cleared())
        FilterField.entries.forEach { assertFalse(it in filter.without(it).active()) }
    }

    @Test fun `identity is 24 hexadecimal characters`() {
        requireId(id)
        listOf("", "0123", "0123456789abcdef0123456", "0123456789abcdef012345678", "0123456789abcdef0123456g").forEach {
            assertFailsWith<IllegalArgumentException> { requireId(it) }
        }
    }
}
