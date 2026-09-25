package com.sperance.exileforge.core

import com.sperance.exileforge.core.character.SellRule
import com.sperance.exileforge.core.character.Sheet
import com.sperance.exileforge.core.character.StatOrder
import com.sperance.exileforge.core.character.StatTables
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierEffect
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.StatValue
import com.sperance.exileforge.core.model.skilltree.CharacterSkillNode
import org.junit.Test
import kotlinx.serialization.json.*
import kotlin.test.*

/** The sheet the client adds up (2.46.0) follows the server's CharacterStatsCalculator step by step. */
class SheetTest {

    private val tables = StatTables(
        stats = listOf(StatOrder("STOCK_STRENGTH", 10), StatOrder("STOCK_HEALTH", 100), StatOrder("STOCK_ARMOR", 120)),
        slots = listOf("HELMET", "BODY", "GLOVES", "RING", "BOOTS", "WINGS", "BELT", "WEAPON_1H", "WEAPON_2H", "QUIVER", "SHIELD", "AMULET", "RING_2", "JEWEL"),
        sell = SellRule(0.15, mapOf("COMMON" to 1.0, "RARE" to 2.5)),
    )

    private fun def(id: String, stat: String, op: ModifierOperation, local: Boolean = false, perStat: String? = null, perAmount: Double = 1.0) =
        ModifierDefinition(id = id, code = id, isLocal = local, effects = listOf(ModifierEffect(stat, op, perStat, perAmount)))

    private val definitions = listOf(
        def("str", "STOCK_STRENGTH", ModifierOperation.ADD),
        def("life", "STOCK_HEALTH", ModifierOperation.ADD),
        def("lifeInc", "STOCK_HEALTH", ModifierOperation.INCREASED),
        def("lifePerStr", "STOCK_HEALTH", ModifierOperation.ADD, perStat = "STOCK_STRENGTH", perAmount = 2.0),
        def("armour", "STOCK_ARMOR", ModifierOperation.ADD, local = true),
        def("armourInc", "STOCK_ARMOR", ModifierOperation.INCREASED, local = true),
    )

    private val warrior = CharacterClass(id = "c", code = "WARRIOR",
        baseStats = listOf(StatValue("STOCK_STRENGTH", 20.0), StatValue("STOCK_HEALTH", 50.0)),
        perLevelStats = listOf(StatValue("STOCK_HEALTH", 10.0)),
        params = listOf(Modifier("lifePerStr", listOf(1.0))))

    private val hero = CharacterSummary(id = "h", userId = "u", name = "Hero", level = 3, classId = "c")

    private fun mod(id: String, value: Double) = Modifier(id, listOf(value))

    private fun template(slot: String, vararg base: Modifier, strength: Int = 0, price: Long = 100) = buildJsonObject {
        put("slot", slot); put("code", slot); put("price", price); put("requiredStrength", strength)
        put("baseParams", buildJsonArray { base.forEach { add(buildJsonObject { put("modifierCode", it.modifierCode); put("values", buildJsonArray { it.values.forEach { v -> add(v) } }) }) } })
    }

    @Test fun theBaseTheTreeAndTheConversionsComeFirst() {
        val sheet = Sheet.calculate(hero, warrior, listOf(CharacterSkillNode("n", listOf(mod("str", 10.0), mod("lifeInc", 10.0)))),
            emptyList(), emptyMap(), definitions, tables)
        // Strength 30 is counted before life: 50 + 2 * 10 per level + 30 / 2 = 85, then 10% more.
        assertEquals(30.0, sheet.stats["STOCK_STRENGTH"])
        assertEquals(93.5, sheet.stats["STOCK_HEALTH"])
    }

    @Test fun anItemFoldsItsLocalModifiersAndNeedsItsRequirements() {
        val templates = mapOf("helm" to template("HELMET", mod("armour", 100.0)), "body" to template("BODY", mod("armour", 50.0), strength = 40))
        val inventory = listOf(
            EquipmentInstance("i1", equipmentId = "helm", params = listOf(mod("armourInc", 20.0), mod("str", 20.0)), equippedSlot = "HELMET"),
            EquipmentInstance("i2", equipmentId = "body", equippedSlot = "BODY"),
        )
        val sheet = Sheet.calculate(hero, warrior, emptyList(), inventory, templates, definitions, tables)
        // The helmet is checked first and gives the 20 strength the body armour needs.
        assertEquals(listOf("i1", "i2"), sheet.active)
        assertEquals(170.0, sheet.stats["STOCK_ARMOR"])
        val weak = Sheet.calculate(hero, warrior, emptyList(), inventory.take(1).map { it.copy(params = listOf(mod("armourInc", 20.0))) } + inventory[1],
            templates, definitions, tables)
        assertEquals("strength: need 40, have 20", weak.inactive.single().reasons.single())
        assertEquals(listOf("strength: need 40, have 20"), weak.unwearableBy["body"])
    }

    @Test fun wearingNamesWhatMovesAndTheRingTakesTheFreeHand() {
        val templates = mapOf("ring" to template("RING", mod("life", 10.0)))
        val worn = EquipmentInstance("r1", equipmentId = "ring", equippedSlot = "RING")
        val loose = EquipmentInstance("r2", equipmentId = "ring")
        val before = Sheet.calculate(hero, warrior, emptyList(), listOf(worn, loose), templates, definitions, tables).stats
        val delta = Sheet.wearing(loose, hero, warrior, emptyList(), listOf(worn, loose), templates, definitions, tables, before)
        assertEquals(10.0, delta.single { it.stat == "STOCK_HEALTH" }.change)
    }

    @Test fun thePriceIsTheMerchantsRule() {
        val item = EquipmentInstance("i", equipmentId = "t", rarity = "RARE", params = listOf(mod("life", 1.0), mod("str", 1.0)))
        assertEquals(325L, Sheet.sellPrice(item, template("HELMET", price = 100), tables.sell, emptyMap()))
        assertEquals(487L, Sheet.sellPrice(item, template("HELMET", price = 100), tables.sell, mapOf("STOCK_GOLD" to 50.0)))
        assertEquals(1L, Sheet.sellPrice(item.copy(params = emptyList(), rarity = "COMMON"), template("HELMET", price = 0), tables.sell, emptyMap()))
    }
}
