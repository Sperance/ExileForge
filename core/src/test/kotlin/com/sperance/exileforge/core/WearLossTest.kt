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

/** «Теряешь»: надевание снимает всё, что вытесняет (двуручное - оружие и щит, лук - щит), теряется то, чего новая вещь не даёт совсем. */
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

    @Test
    fun lost_is_only_what_the_new_item_does_not_give() {
        val blade = ItemInstance("blade", "CORRODED_BLADE", rolls = listOf(roll(affixes[0])))
        val removed = Sheets.takenOff(index, blade, worn, null)
        val shieldLines = ItemView.of(shield, index)!!.orderedLines.map { it.text }
        assertEquals(shieldLines, LostLines.of(index, blade, removed))
    }
}
