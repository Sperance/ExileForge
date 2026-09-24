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
        val base = buildJsonObject { put("_id", "template"); put("code", "IRON_RING"); put("slot", "RING"); put("rarity", "COMMON") }
        val instance = buildJsonObject {
            put("_id", "instance"); put("equipmentId", "template"); put("equippedSlot", "RING")
            put("params", buildJsonArray { add(buildJsonObject { put("modifierId", id); put("values", buildJsonArray { add(42.0) }) }) })
        }
        val display = inventoryDocument(instance, base)
        assertEquals("instance", display.text("_id"))
        // A template carries a code; without the dictionary the card names it readably.
        assertEquals("IRON RING", display.text("name"))
        assertEquals("RING", display.text("slot"))
        assertEquals(1, display.getValue("params").jsonArray.size)
        // The shared template is a different document and keeps its own identity.
        assertEquals("template", base.text("_id"))
        assertFalse("params" in base)
    }

    @Test fun theCopysOwnRarityWinsOverTheTemplateItDroppedFrom() {
        // A template says what the item drops as; the orbs then move this copy up or down on its own.
        val base = buildJsonObject { put("_id", "template"); put("code", "IRON_RING"); put("slot", "RING"); put("rarity", "COMMON") }
        val upgraded = buildJsonObject {
            put("_id", "instance"); put("equipmentId", "template"); put("rarity", "RARE")
            put("corrupted", true); put("mirrored", true)
        }
        val display = inventoryDocument(upgraded, base)
        assertEquals("RARE", display.text("rarity"))
        assertEquals(true, display.getValue("corrupted").jsonPrimitive.boolean)
        // Every state of the copy has to survive the projection, or the card shows the template's.
        assertEquals(true, display.getValue("mirrored").jsonPrimitive.boolean)
        assertEquals(listOf("corrupted", "mirrored"), itemStates(display))
        assertEquals("COMMON", base.text("rarity"))
    }

    @Test fun missingTemplateStillProducesANamedCard() {
        val display = inventoryDocument(buildJsonObject { put("_id", "instance"); put("equipmentId", "unknown") }, null)
        assertEquals("Предмет экипировки", display.text("name"))
        assertEquals(ItemVisualKind.ITEM, itemVisualKind(display))
    }

    @Test fun rolledValuesSurviveAMissingTranslation() {
        // The sentence itself comes from the server's dictionary (see ServerLocaleTest); what is
        // pinned here is what a screen shows without one — the numbers, never nothing.
        val definitions = listOf(WireJson.decodeFromString(ModifierDefinition.serializer(),
            """{"_id":"$id","code":"LIFE_AND_MANA","source":"PREFIX",
                "effects":[{"stat":"STOCK_HEALTH","operation":"ADD"},{"stat":"STOCK_MANA","operation":"ADD"}]}"""))
        val modifier = buildJsonObject { put("modifierId", id); put("values", buildJsonArray { add(46.0); add(11.5) }) }
        assertEquals("46 Здоровье · 12 Мана", modifierText(modifier, definitions))
        // A definition the client has not read still prints what was rolled.
        assertEquals("46 · 12", modifierText(modifier, emptyList()))
        // Nothing rolled at all: the modifier is named rather than shown as an empty line.
        assertEquals("LIFE_AND_MANA", modifierText(buildJsonObject { put("modifierId", id) }, definitions))
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

    /**
     * What an item asks of a character, shortened for a line.
     *
     * Level 1 is not a requirement and an unset attribute is not a zero — both are left off rather
     * than printed as noise. Nothing here is ever enforced: the server checks requirements twice,
     * by two different rules, and a control is never disabled on a reading done here.
     */
    @Test fun `requirements are listed short, and only where the template set them`() {
        val helmet = buildJsonObject {
            put("requiredLevel", 25); put("requiredStrength", 40); put("requiredDexterity", 0)
        }
        assertEquals(listOf("25 ур.", "40 сил"), itemRequirements(helmet, Lang.RU))
        assertEquals(listOf("25 lvl", "40 str"), itemRequirements(helmet, Lang.EN))
        // A level-1 item asks for nothing, and neither does one that set no requirement at all.
        assertEquals(emptyList(), itemRequirements(buildJsonObject { put("requiredLevel", 1) }, Lang.RU))
        assertEquals(emptyList(), itemRequirements(buildJsonObject { put("code", "PLAIN") }, Lang.RU))
    }

    /**
     * Numbers are whole everywhere, as Path of Exile prints them — except where the fraction is
     * the whole point.
     *
     * Full precision is never lost: the value travels and is stored as the Double the server sent,
     * and this is only the last step before a string. But armour with a dot in it is noise, while
     * an attack speed rounded to a whole number stops saying anything at all.
     */
    @Test fun `a bench line reads as the sentence it adds, with the tier's range`() {
        val life = ModifierDefinition(id = "life", code = "CRAFTED_ADD_MAXIMUM_LIFE",
            effects = listOf(com.sperance.exileforge.core.model.modifier.ModifierEffect("STOCK_HEALTH", com.sperance.exileforge.core.model.modifier.ModifierOperation.ADD)), crafted = true)
        val recipe = com.sperance.exileforge.core.model.modifier.BenchRecipe(code = "CRAFTED_ADD_MAXIMUM_LIFE_T3", modifierId = "life",
            modifierCode = "CRAFTED_ADD_MAXIMUM_LIFE", tier = 3,
            values = listOf(com.sperance.exileforge.core.model.modifier.ModifierTierValue(25.0, 34.0)), slots = listOf("HELMET"))
        // The range stands where the roll will land, printed by its stat's rule, dictionary or not.
        assertTrue("(25–34)" in recipeText(recipe, listOf(life)), recipeText(recipe, listOf(life)))
        assertTrue(recipe.fits("HELMET")); assertFalse(recipe.fits("JEWEL"))
    }

    @Test fun `the sheet is read in groups, and a stat nobody named still lands somewhere`() {
        val grouped = groupedStats(mapOf("STOCK_RARITY" to 38.0, "STOCK_RESIST_COLD" to 68.0, "STOCK_HEALTH" to 1184.0,
            "STOCK_ATTACK_SPEED" to 1.55, "STOCK_STRENGTH" to 142.0, "STOCK_ARMOR" to 1242.0, "STOCK_MANA" to 412.0, "STOCK_IGNITE_CHANCE" to 25.0, "BATTLE_ARCHERY" to 3.0))
        assertEquals(StatGroup.entries.toList(), grouped.map { it.first })
        // Inside a group the server's own enum order holds: life before mana.
        assertEquals(listOf("STOCK_HEALTH", "STOCK_MANA"), grouped.first().second.map { it.first })
        assertEquals(StatGroup.OTHER, StatGroup.of("STOCK_SOMETHING_NEW"))
        // Ailments sit together, but avoiding a stun is a defence and a higher ceiling is a resistance.
        assertEquals(StatGroup.AILMENT, StatGroup.of("STOCK_AVOID_FREEZE"))
        assertEquals(StatGroup.AILMENT, StatGroup.of("STOCK_BLEED_DURATION_ON_SELF"))
        assertEquals(StatGroup.DEFENCE, StatGroup.of("STOCK_AVOID_STUN"))
        assertEquals(StatGroup.RESISTANCE, StatGroup.of("STOCK_RESIST_MAX_FIRE"))
        assertEquals(listOf("STOCK_RARITY", "BATTLE_ARCHERY"), grouped.last().second.map { it.first })
        // An empty group is left out rather than drawn as an empty card.
        assertEquals(listOf(StatGroup.RESERVE), groupedStats(mapOf("STOCK_HEALTH" to 1.0)).map { it.first })
    }

    @Test fun `a number is whole unless its fraction is the point`() {
        assertEquals("48", statNumber("STOCK_ARMOR", 47.6))
        assertEquals("12", statNumber("STOCK_MANA", 11.5))
        assertEquals("188", number(188.4))
        assertEquals("0", number(0.4))

        // Rounding these would destroy them: 1.25 attacks a second is not 1.
        assertEquals("1.25", statNumber("STOCK_ATTACK_SPEED", 1.25))
        assertEquals("5.50", statNumber("STOCK_CRITICAL_CHANCE", 5.5))
        assertEquals("1.50", statNumber("STOCK_CRITICAL_MULTIPLIER", 1.5))
        // Leech lives below one percent; printed whole, 0.4% would read as nothing.
        assertEquals("0.40", statNumber("STOCK_LEECH_ALL", 0.4))
        preciseStats.forEach { assertTrue(it.startsWith("STOCK_"), "$it is not a server stat") }
    }
}

class GlyphTest {
    @Test fun `a stat is drawn by its element first, then by its kind`() {
        assertEquals(Glyph.FIRE, Glyph.ofStat("STOCK_RESIST_FIRE"))
        assertEquals(Glyph.FIRE, Glyph.ofStat("STOCK_ATTACK_FIRE"))
        assertEquals(Glyph.SHIELD, Glyph.ofStat("STOCK_ENERGY_SHIELD"))
        assertEquals(Glyph.MANA, Glyph.ofStat("STOCK_ENERGY"))
        assertEquals(Glyph.LIFE, Glyph.ofStat("STOCK_HEALTH_REGEN"))
        assertEquals(Glyph.DEFENCE, Glyph.ofStat("STOCK_ARMOR"))
        assertEquals(Glyph.CRITICAL, Glyph.ofStat("STOCK_CRITICAL_MULTIPLIER"))
        assertEquals(Glyph.ATTACK, Glyph.ofStat("STOCK_ATTACK_PHYSICAL"))
        // A code nobody mapped keeps the neutral glyph rather than a guess.
        assertEquals(Glyph.INFO, Glyph.ofStat("STOCK_SOMETHING_NEW"))
    }

    @Test fun `every stat the client names has a glyph of its own`() {
        val unnamed = com.sperance.exileforge.core.model.character.stockStats.filter { Glyph.ofStat(it) == Glyph.INFO }
        assertEquals(emptySet(), unnamed.toSet())
    }

    @Test fun `a field, a catalogue and a modifier name their glyph exactly`() {
        assertEquals(Glyph.LEVEL, Glyph.ofField("requiredLevel"))
        assertEquals(Glyph.FIRE, Glyph.ofField("STOCK_RESIST_FIRE"))
        assertEquals(Glyph.INFO, Glyph.ofField("somethingElse"))
        assertEquals(Glyph.CHARACTER, Glyph.of(com.sperance.exileforge.core.model.Catalog.CHARACTERS))
        val armour = com.sperance.exileforge.core.model.modifier.ModifierDefinition(id = "m1",
            effects = listOf(com.sperance.exileforge.core.model.modifier.ModifierEffect("STOCK_ARMOR", com.sperance.exileforge.core.model.modifier.ModifierOperation.ADD)))
        assertEquals(Glyph.DEFENCE, Glyph.ofModifier("m1", listOf(armour)))
        assertEquals(Glyph.INFO, Glyph.ofModifier("unknown", listOf(armour)))
    }
}
