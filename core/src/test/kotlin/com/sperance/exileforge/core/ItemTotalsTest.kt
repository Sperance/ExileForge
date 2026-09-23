package com.sperance.exileforge.core

import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierEffect
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.Test

/**
 * The base of an item with its own local modifiers folded in, and the states it carries.
 *
 * The arithmetic mirrors the server's ModifierMath, so the numbers here are the ones the character
 * sheet is built from; what is checked is that the client folds the same way and folds only what
 * belongs to the item.
 */
class ItemTotalsTest {

    private fun definition(id: String, stat: String, operation: ModifierOperation, local: Boolean) =
        ModifierDefinition(id = id, code = id, isLocal = local,
            effects = listOf(ModifierEffect(stat = stat, operation = operation)))

    private val flatArmour = definition("base", "STOCK_ARMOR", ModifierOperation.ADD, local = true)
    private val moreArmour = definition("increased", "STOCK_ARMOR", ModifierOperation.INCREASED, local = true)
    private val strength = definition("strength", "STOCK_STRENGTH", ModifierOperation.ADD, local = false)

    private fun modifier(id: String, value: Double) = buildJsonObject {
        put("modifierId", id); put("values", buildJsonArray { add(value) })
    }

    private fun item(vararg rolled: JsonObject, base: Double = 100.0) = buildJsonObject {
        put("baseParams", buildJsonArray { add(modifier("base", base)) })
        put("params", buildJsonArray { rolled.forEach { add(it) } })
    }

    private val definitions = listOf(flatArmour, moreArmour, strength)

    @Test fun aLocalPercentageRaisesTheItemsOwnBase() {
        val property = baseProperties(item(modifier("increased", 20.0)), definitions).single()
        val value = property.values.single()
        assertEquals(100.0, value.base)
        assertEquals(120.0, value.total)
        assertTrue(value.augmented)
        assertEquals("120", value.text)
        assertEquals("100", value.baseText)
    }

    @Test fun twoPercentagesAddUpBeforeTheyMultiply() {
        // As in Path of Exile: 20% and 30% increased are 50%, not 1.2 * 1.3.
        val property = baseProperties(item(modifier("increased", 20.0), modifier("increased", 30.0)), definitions).single()
        assertEquals(150.0, property.values.single().total)
    }

    @Test fun aGlobalModifierLeavesTheItemsBaseAlone() {
        // "+10 to strength" is the character's, not the helmet's, however it is worn.
        val property = baseProperties(item(modifier("strength", 10.0)), definitions).single()
        assertFalse(property.augmented)
        assertEquals(100.0, property.values.single().total)
    }

    @Test fun anUntouchedBaseIsNotMarkedAsRaised() {
        val property = baseProperties(item(), definitions).single()
        assertFalse(property.augmented)
        assertEquals(property.values.single().base, property.values.single().total)
    }

    @Test fun aBaseWhoseDefinitionIsUnknownStillPrintsItsNumber() {
        // The reference tables are read once per session; a line drawn before they arrive shows
        // the number the server wrote rather than nothing at all.
        val property = baseProperties(item(), definitions = emptyList()).single()
        assertEquals("100", property.line())
        assertFalse(property.augmented)
    }

    @Test fun aDocumentWithoutABaseHasNoPropertyLines() {
        assertEquals(emptyList(), baseProperties(buildJsonObject { put("code", "CHAOS_ORB") }, definitions))
    }

    @Test fun withoutADictionaryTheLineStillNamesItsCharacteristic() {
        // The value a server rolled never vanishes because a translation is missing.
        val line = baseProperties(item(modifier("increased", 20.0)), definitions).single().line()
        assertTrue("120" in line, line)
    }

    @Test fun everyTrueFlagIsAState() {
        val document = buildJsonObject {
            put("corrupted", true); put("mirrored", true); put("deleted", true)
            put("equippedSlot", "HELMET"); put("socketCode", "SCI_J1")
        }
        // deleted is bookkeeping, not a state of the thing; the order is fixed so two items
        // never list the same pair differently.
        assertEquals(listOf("corrupted", "mirrored", "equipped", "socketed"), itemStates(document))
    }

    @Test fun aFlagTheClientDoesNotKnowIsStillShown() {
        assertEquals(listOf("shiny"), itemStates(buildJsonObject { put("shiny", true); put("corrupted", false) }))
        assertEquals("Shiny", stateTitle("shiny", com.sperance.exileforge.core.i18n.Lang.EN))
    }

    @Test fun anItemInTheBagCarriesNoState() {
        assertEquals(emptyList(), itemStates(buildJsonObject { put("corrupted", false); put("equippedSlot", JsonNull) }))
    }

    @Test fun influenceFracturedAndCraftedAreReadOffTheItem() {
        val bench = ModifierDefinition(id = "bench", code = "CRAFTED_ADD_MAXIMUM_LIFE", crafted = true)
        val document = buildJsonObject {
            put("influence", "ELDER"); put("equippedSlot", "HELMET")
            putJsonArray("params") {
                addJsonObject { put("modifierId", "bench"); put("tier", 2); putJsonArray("values") { add(30.0) } }
                addJsonObject { put("modifierId", "rolled"); put("tier", 1); put("fractured", true); putJsonArray("values") { add(9.0) } }
            }
        }
        assertEquals(listOf("elder", "fractured", "crafted", "equipped"), itemStates(document, listOf(bench)))
        // Without the definitions the bench cannot be told apart from a roll, and is not guessed at.
        assertEquals(listOf("elder", "fractured", "equipped"), itemStates(document))
        assertEquals("Elder influence", stateTitle("elder", com.sperance.exileforge.core.i18n.Lang.EN))

        val params = (document["params"] as JsonArray).map { it.jsonObject }
        assertEquals(AffixMarks(tier = 2, crafted = true, fractured = false), affixMarks(params[0], listOf(bench)))
        assertEquals(AffixMarks(tier = 1, crafted = false, fractured = true), affixMarks(params[1], listOf(bench)))
    }
}
