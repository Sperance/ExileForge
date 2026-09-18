package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EquipmentKind
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
            // Weapon-only and armour-only fields appear on their own kind; the rest must be in the form.
            val always = editableFields(catalog) - setOf("weaponType", "damage_min", "damage_max", "attackSpeed", "durability", "defense")
            assertTrue(fields.containsAll(always), "$catalog is missing ${always - fields}")
        }
    }

    @Test fun `equipment validation follows the server's own rules`() {
        val weapon = template(Catalog.EQUIPMENT, EquipmentKind.Weapon)
        validate(weapon, Catalog.EQUIPMENT)
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("rarity" to JsonPrimitive("LEGENDARY"))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("slot" to JsonPrimitive("POCKET"))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("damage_max" to JsonPrimitive(1.0))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon + ("attackSpeed" to JsonPrimitive(0.0))), Catalog.EQUIPMENT) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(weapon - "type"), Catalog.EQUIPMENT) }
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
        val twice = buildJsonArray { add(buildJsonObject { put("stat", "STOCK_HEALTH"); put("value", 1) }); add(buildJsonObject { put("stat", "STOCK_HEALTH"); put("value", 2) }) }
        assertFailsWith<IllegalArgumentException> { validate(JsonObject(character + ("stockSkills" to twice)), Catalog.CHARACTERS) }
    }

    @Test fun `a rolled modifier points at a definition and a tier`() {
        val modifier = buildJsonObject { put("modifierId", id); put("tierId", id); put("tier", 2); put("values", buildJsonArray { add(12.5); add(3.0) }) }
        validateModifier(modifier)
        assertFailsWith<IllegalArgumentException> { validateModifier(JsonObject(modifier + ("modifierId" to JsonPrimitive("life")))) }
        assertFailsWith<IllegalArgumentException> { validateModifier(JsonObject(modifier + ("tier" to JsonPrimitive(0)))) }
    }

    @Test fun `a composite modifier decodes one value per effect`() {
        val definition = WireJson.decodeFromString(ModifierDefinition.serializer(),
            """{"_id":"$id","code":"life_and_mana","name":"Life and Mana","source":"PREFIX","tags":["life"],
                "effects":[{"stat":"STOCK_HEALTH","operation":"ADD"},{"stat":"STOCK_MANA","operation":"ADD"}]}""")
        assertTrue(definition.composite)
        assertEquals("Life and Mana", definition.title)
        assertEquals(listOf(ModifierOperation.ADD, ModifierOperation.ADD), definition.effects.map { it.operation })
        val tier = WireJson.decodeFromString(ModifierTier.serializer(),
            """{"_id":"$id","modifierId":"$id","tier":1,"minItemLevel":84,"weight":100,"values":[{"valueMin":46.0,"valueMax":48.0},{"valueMin":10.0,"valueMax":12.0}]}""")
        assertEquals(2, tier.values.size)
        assertEquals(84, tier.minItemLevel)
    }

    @Test fun `filters compare stored fields and nothing else`() {
        val bow = buildJsonObject { put("name", "Short Bow"); put("slot", "WEAPON_2H"); put("weaponType", "BOW"); put("rarity", "RARE"); put("itemLevel", 40); put("modifierIds", buildJsonArray { add(id) }) }
        assertTrue(CatalogFilter().isEmpty)
        assertTrue(CatalogFilter(query = "short", slot = "WEAPON_2H", rarity = "RARE", minLevel = "10", maxLevel = "40", weaponType = "BOW", modifierId = id).matches(bow))
        assertFalse(CatalogFilter(maxLevel = "39").matches(bow))
        assertFalse(CatalogFilter(modifierId = "other").matches(bow))
        assertFalse(CatalogFilter(query = "sword").matches(bow))
    }

    @Test fun `identity is 24 hexadecimal characters`() {
        requireId(id)
        listOf("", "0123", "0123456789abcdef0123456", "0123456789abcdef012345678", "0123456789abcdef0123456g").forEach {
            assertFailsWith<IllegalArgumentException> { requireId(it) }
        }
    }
}
