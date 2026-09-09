package com.sperance.exileforge.core

import kotlinx.serialization.json.*
import kotlin.random.Random
import kotlin.test.*
import org.junit.Test

class EditorAndGenerationTest {
    @Test fun `forms construct every effect condition and expression variant`() {
        effectVariants.keys.forEach { type ->
            val value = defaultObject("effect", type)
            validateForm("effect", value)
            WireJson.decodeFromJsonElement(ModifierEffect.serializer(), value)
        }
        conditionVariants.keys.forEach { type ->
            val value = defaultObject("condition", type)
            validateForm("condition", value)
            WireJson.decodeFromJsonElement(ModifierCondition.serializer(), value)
        }
        expressionVariants.keys.forEach { type ->
            val value = defaultObject("expression", type)
            validateForm("expression", value)
            WireJson.decodeFromJsonElement(ValueExpression.serializer(), value)
        }
    }
    @Test fun `character has all editable collections and enforces server number widths`() {
        val character = JsonObject(template(Catalog.CHARACTERS) + mapOf("name" to JsonPrimitive("Test"), "userId" to JsonPrimitive("0123456789abcdef01234567")))
        validate(character, Catalog.CHARACTERS)
        assertTrue(character.keys.containsAll(listOf("params", "equipments", "items", "professionSkills", "stockSkills", "battleSkills", "boolSkills", "recipeAccess", "gainedRedemptionCodes")))
        assertFails { validate(JsonObject(character + ("level" to JsonPrimitive(32768))), Catalog.CHARACTERS) }
        assertFails { validate(JsonObject(character + ("money" to JsonPrimitive("12.5"))), Catalog.CHARACTERS) }
        val edited = JsonObject(character + mapOf("level" to JsonPrimitive(2), "version" to JsonPrimitive(99)))
        assertEquals(setOf("level"), diff(character, edited).keys)
    }
    @Test fun `generation respects eligibility nonstacking and fixed value ranges`() {
        val definition = ModifierDefinition("test", "Test", ModifierSource.PREFIX, affixType = AffixType.PREFIX,
            tiers = listOf(ModifierTier(1, 1, 100, listOf(ValueRange(42.0, 42.0))), ModifierTier(2, 50, 100, listOf(ValueRange(999.0, 999.0)))))
        val result = ItemGenerator(Random(1)).roll(listOf(definition), 10, 3)
        assertEquals(1, result.size)
        assertEquals(42.0, result.single().values.single().value)
        assertEquals(1, result.single().tier)
        assertTrue(ItemGenerator(Random(1)).roll(listOf(definition.copy(rollable = false)), 10, 3).isEmpty())
    }
    @Test fun `generation uses rarity counts preserves definition data and removes identity`() {
        val defs = (1..8).map { i -> JsonObject(starterDefinition() + mapOf("id" to JsonPrimitive("stat_$i"), "affixType" to JsonPrimitive(if(i <= 4) "PREFIX" else "SUFFIX"))) }
        val base = JsonObject(template(Catalog.EQUIPMENT) + mapOf("_id" to JsonPrimitive("0123456789abcdef01234567"), "rarity" to JsonPrimitive("MYTHICAL"), "modifierDefinitions" to JsonArray(defs)))
        val generated = ItemGenerator(Random(42)).generate(base)
        assertEquals(6, generated.getValue("modifiers").jsonArray.size)
        assertFalse("_id" in generated)
        assertEquals(base["modifierDefinitions"], generated["modifierDefinitions"])
        validate(generated, Catalog.EQUIPMENT)
        assertEquals(generated, ItemGenerator(Random(42)).generate(base))
    }
    @Test fun `selecting preset attaches missing definition only once`() {
        val document = JsonObject(template(Catalog.EQUIPMENT) + ("modifierDefinitions" to JsonArray(emptyList())))
        val selected = JsonArray(listOf(starterModifier()))
        val changed = attachSelectedDefinitions(document, selected, listOf(starterDefinition()))
        assertEquals(1, changed.getValue("modifierDefinitions").jsonArray.size)
        assertEquals(changed, attachSelectedDefinitions(changed, selected, listOf(starterDefinition())))
    }
    @Test fun `modifier spinner fills multi-value tier source and tags`() {
        val definition = WireJson.encodeToJsonElement(ModifierDefinition.serializer(), ModifierDefinition("fire", "Fire", ModifierSource.SUFFIX, tiers = listOf(ModifierTier(2, values = listOf(ValueRange(1.0, 10.0), ValueRange(20.0, 30.0)))), tags = setOf(ModifierTag("fire")))).jsonObject
        val modifier = modifierFromDefinition(definition, 2)
        validateModifier(modifier)
        assertEquals("SUFFIX", modifier.text("source"))
        assertEquals(2, modifier.getValue("values").jsonArray.size)
        assertEquals("2", modifier.text("tier"))
        assertEquals("fire", modifier.getValue("tags").jsonArray.single().jsonPrimitive.content)
    }
}
