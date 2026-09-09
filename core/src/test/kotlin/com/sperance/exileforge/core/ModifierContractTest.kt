package com.sperance.exileforge.core

import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.Test

class ModifierContractTest {
    @Test fun `multiple rolled values and arbitrary tags survive validation`() {
        val document = WireJson.parseToJsonElement("""{"definitionId":"custom.fire","values":[{"value":10.0},{"value":20.0}],"tier":12,"source":"CORRUPTION","tags":["fire","custom.tag"],"futureField":{"enabled":true}}""").jsonObject
        validateModifier(document)
        val parsed = WireJson.decodeFromJsonElement(Modifier.serializer(), document)
        assertEquals(listOf(10.0, 20.0), parsed.values.map { it.value })
        assertEquals(12, parsed.tier)
        assertTrue("futureField" in document)
    }
    @Test fun `legacy modifier and string definitions are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            validateModifier(buildJsonObject { put("type", "PREFIX_ADD_HEALTH"); put("value", 42); put("tier", 1) })
        }
        assertFailsWith<IllegalArgumentException> {
            validate(JsonObject(template(Catalog.EQUIPMENT) + ("modifierDefinitions" to buildJsonArray { add("PREFIX_ADD_HEALTH") })), Catalog.EQUIPMENT)
        }
    }
    @Test fun `definition decodes nested effects conditions and expressions`() {
        val definition = JsonObject(starterDefinition() + mapOf(
            "conditions" to WireJson.parseToJsonElement("""[{"type":"and","conditions":[{"type":"has_tag","tag":"attack"},{"type":"not","condition":{"type":"full_life"}}]}]"""),
            "effects" to WireJson.parseToJsonElement("""[{"type":"damage_conversion","from":"physical","to":"fire","percentage":{"type":"multiply","left":{"type":"modifier_value","index":0},"right":{"type":"constant","value":2.0}}}]""")
        ))
        validate(JsonObject(template(Catalog.EQUIPMENT) + ("modifierDefinitions" to JsonArray(listOf(definition)))), Catalog.EQUIPMENT)
        val parsed = WireJson.decodeFromJsonElement(ModifierDefinition.serializer(), definition)
        assertIs<ModifierEffect.DamageConversion>(parsed.effects.single())
        assertIs<ModifierCondition.And>(parsed.conditions.single())
    }
    @Test fun `invalid source tier and rolled number are rejected`() {
        listOf(
            starterModifier() + ("source" to JsonPrimitive("INVALID")),
            starterModifier() + ("tier" to JsonPrimitive(0)),
            starterModifier() + ("values" to JsonArray(listOf(buildJsonObject { put("value", "wrong") })))
        ).forEach { assertFails { validateModifier(JsonObject(it)) } }
    }
    @Test fun `invalid definition range is rejected`() {
        val definition = JsonObject(starterDefinition() + ("tiers" to WireJson.parseToJsonElement("""[{"tier":1,"values":[{"min":20.0,"max":10.0}]}]""")))
        assertFails { validate(JsonObject(template(Catalog.EQUIPMENT) + ("modifierDefinitions" to JsonArray(listOf(definition)))), Catalog.EQUIPMENT) }
    }
}
