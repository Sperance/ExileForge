package com.sperance.exileforge.core

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.LostLines
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Roll
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * «Теряешь»: надевание снимает всё, что вытесняет (двуручное - оружие и щит, лук - щит), теряется то, чей итог у героя после замены -
 * ноль: ни новая вещь, ни прочее надетое, ни лист героя этого не дают.
 */
class WearLossTest {
    private val index = TestContent.index

    /** Две строки с одной характеристикой каждая, характеристики разные. */
    private val affixes: List<ModifierDef> = index.definitions.filter { it.affix && it.effects.size == 1 && it.tiers.isNotEmpty() }
        .distinctBy { it.effects.single().stat }.take(2)

    private fun roll(def: ModifierDef) = Roll(def.code, tier = 1, share = 0.5)

    private val axe = ItemInstance("axe", "RUSTED_HATCHET", slot = Slot.WEAPON_1H, rolls = listOf(roll(affixes[0])))
    private val shield = ItemInstance("shield", "SPLINTERED_TOWER_SHIELD", slot = Slot.SHIELD, rolls = listOf(roll(affixes[1])))
    private val worn = listOf(axe, shield)

    @Test
    fun two_handed_takes_off_weapon_and_shield() {
        val blade = ItemInstance("blade", "CORRODED_BLADE")
        assertEquals(listOf(axe, shield), Sheets.takenOff(index, blade, worn, null))
    }

    @Test
    fun bow_takes_off_weapon_and_shield() {
        val bow = ItemInstance("bow", "CRUDE_BOW")
        assertEquals(listOf(axe, shield), Sheets.takenOff(index, bow, worn, null))
    }

    private val blade = ItemInstance("blade", "CORRODED_BLADE", rolls = listOf(roll(affixes[0])))

    @Test
    fun lost_is_what_goes_to_zero() {
        val removed = Sheets.takenOff(index, blade, worn, null)
        val shieldLines = ItemView.of(shield, index)!!.orderedLines.map { it.text }
        assertEquals(shieldLines, LostLines.of(index, blade, removed, worn))
    }

    @Test
    fun what_another_worn_item_gives_is_not_lost() {
        val template = index.templates.values.first { it.slot == Slot.RING && !it.unique }.code
        val ring = ItemInstance("ring", template, slot = Slot.RING, rolls = listOf(roll(affixes[1])))
        val gear = worn + ring
        assertEquals(emptyList(), LostLines.of(index, blade, Sheets.takenOff(index, blade, gear, null), gear))
    }

    @Test
    fun what_the_sheet_keeps_is_not_lost() {
        val after = mapOf(affixes[1].effects.single().stat to 5.0)
        assertEquals(emptyList(), LostLines.of(index, blade, Sheets.takenOff(index, blade, worn, null), worn, after))
    }
}
