package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.*

class ItemPresentationTest {
    @Test fun instanceProjectionUsesRolledValuesAndKeepsTemplateUnchanged() {
        val base = buildJsonObject { put("_id", "template"); put("name", "Iron Ring"); put("slot", "RING"); put("rarity", "COMMON"); put("modifiers", JsonArray(emptyList())) }
        val instance = buildJsonObject { put("uuid", "instance"); put("params", buildJsonArray { add(buildJsonObject { put("definitionId", "life") }) }); put("poe", buildJsonObject { put("rarity", "RARE"); put("quality", 20); put("itemLevel", 85) }) }
        val display = inventoryDocument(instance, base)
        assertEquals("instance", display.text("_id")); assertEquals("RARE", display.text("rarity")); assertEquals("85", display.text("itemLevel"))
        assertEquals("COMMON", base.text("rarity")); assertEquals("template", base.text("_id"))
        assertEquals(1, display.getValue("modifiers").jsonArray.size)
    }
    @Test fun missingMetadataStillProducesNamedCardAndIcon() {
        val display = inventoryDocument(buildJsonObject { put("uuid", "instance"); put("poe", buildJsonObject { put("baseId", "Metadata/Items/Rings/IronRing") }) }, null)
        assertEquals("Iron Ring", display.text("name")); assertEquals(ItemVisualKind.ITEM, itemVisualKind(display))
    }
    @Test fun modifierNamesArePinnedToExactRevisionAndValuesSupportBothFormats() {
        val definitions = listOf(1, 2).map { n -> buildJsonObject { put("id", "life"); put("revision", n); put("name", "Life $n") } }
        val mod = buildJsonObject { put("definitionId", "life"); put("definitionRevision", 1); put("values", buildJsonArray { add(buildJsonObject { put("value", 42.0) }); add(5) }) }
        assertEquals("Life 1", modifierTitle(mod, definitions)); assertEquals("42 / 5", modifierValues(mod))
    }
    @Test fun equipmentSlotsAndWeaponTypesHaveSpecificEmblems() {
        assertEquals(ItemVisualKind.BOOTS, itemVisualKind(buildJsonObject { put("slot", "BOOTS") }))
        assertEquals(ItemVisualKind.BOW, itemVisualKind(buildJsonObject { put("weaponType", "BOW"); put("slot", "WEAPON_2H") }))
        assertEquals(ItemVisualKind.CHARACTER, itemVisualKind(buildJsonObject { put("userId", "user") }))
        assertEquals("Магический", rarityTitle("MAGIC")); assertEquals("Уникальный", rarityTitle("UNIQUE"))
    }
}
