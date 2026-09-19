package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.Test

class ItemPresentationTest {
    private val id = "0123456789abcdef01234567"

    @Test fun instanceProjectionShowsRolledValuesAndLeavesTheTemplateUntouched() {
        val base = buildJsonObject { put("_id", "template"); put("name", "Iron Ring"); put("slot", "RING"); put("rarity", "COMMON") }
        val instance = buildJsonObject {
            put("_id", "instance"); put("equipmentId", "template"); put("equippedSlot", "RING")
            put("params", buildJsonArray { add(buildJsonObject { put("modifierId", id); put("values", buildJsonArray { add(42.0) }) }) })
        }
        val display = inventoryDocument(instance, base)
        assertEquals("instance", display.text("_id"))
        assertEquals("Iron Ring", display.text("name"))
        assertEquals("RING", display.text("slot"))
        assertEquals(1, display.getValue("params").jsonArray.size)
        // The shared template is a different document and keeps its own identity.
        assertEquals("template", base.text("_id"))
        assertFalse("params" in base)
    }

    @Test fun theCopysOwnRarityWinsOverTheTemplateItDroppedFrom() {
        // A template says what the item drops as; the orbs then move this copy up or down on its own.
        val base = buildJsonObject { put("_id", "template"); put("name", "Iron Ring"); put("slot", "RING"); put("rarity", "COMMON") }
        val upgraded = buildJsonObject { put("_id", "instance"); put("equipmentId", "template"); put("rarity", "RARE"); put("corrupted", true) }
        val display = inventoryDocument(upgraded, base)
        assertEquals("RARE", display.text("rarity"))
        assertEquals(true, display.getValue("corrupted").jsonPrimitive.boolean)
        assertEquals("COMMON", base.text("rarity"))
    }

    @Test fun missingTemplateStillProducesANamedCard() {
        val display = inventoryDocument(buildJsonObject { put("_id", "instance"); put("equipmentId", "unknown") }, null)
        assertEquals("Предмет экипировки", display.text("name"))
        assertEquals(ItemVisualKind.ITEM, itemVisualKind(display))
    }

    @Test fun rolledValuesAreNamedByTheirDefinitionsEffects() {
        val definitions = listOf(WireJson.decodeFromString(ModifierDefinition.serializer(),
            """{"_id":"$id","code":"life_and_mana","name":"Life and Mana","source":"PREFIX",
                "effects":[{"stat":"STOCK_HEALTH","operation":"ADD"},{"stat":"STOCK_MANA","operation":"ADD"}]}"""))
        val modifier = buildJsonObject { put("modifierId", id); put("values", buildJsonArray { add(46.0); add(11.5) }) }
        assertEquals("Life and Mana", modifierTitle(modifier, definitions))
        assertEquals("46 Здоровье · 11.5 Мана", modifierValues(modifier, definitions))
        // An unknown description still prints the numbers rather than dropping them.
        assertEquals("46 · 11.5", modifierValues(modifier, emptyList()))
        assertEquals("0123456789abcdef01234567", modifierTitle(modifier, emptyList()))
    }

    @Test fun slotsWeaponsAndRaritiesHaveTitlesInBothTongues() {
        assertEquals(ItemVisualKind.BOOTS, itemVisualKind(buildJsonObject { put("slot", "BOOTS") }))
        assertEquals(ItemVisualKind.BOW, itemVisualKind(buildJsonObject { put("weaponType", "BOW"); put("slot", "WEAPON_2H") }))
        assertEquals(ItemVisualKind.CHARACTER, itemVisualKind(buildJsonObject { put("userId", "user") }))
        assertEquals("Уникальный", rarityTitle("UNIQUE", Lang.RU))
        assertEquals("Mythical", rarityTitle("MYTHICAL", Lang.EN))
        assertEquals("Двуручное", slotTitle("WEAPON_2H", Lang.RU))
        assertEquals("Double axe", weaponTitle("DOUBLEAXE", Lang.EN))
        assertEquals("Броня", statTitle("STOCK_ARMOR", Lang.RU))
        assertEquals("Armour", statTitle("STOCK_ARMOR", Lang.EN))
        // A stat the client does not know keeps its humanised server identifier.
        assertEquals("NEW STAT", statTitle("STOCK_NEW_STAT", Lang.RU))
    }
}
