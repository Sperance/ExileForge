package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ItemInstance

/** Что билд даёт герою к бою: надетые вещи, взятые узлы дерева, умения и пояс. */
data class SimBuild(val items: List<ItemInstance>, val tree: List<TakenNode>, val skills: HeroSkills, val flasks: List<BeltFlask?>)

/** Общие постоянные эталонов MOB-BAL: кости, доля ролла фляг, поиск вещей и места брони. */
object SimKit {
    const val SEED = 20_261_009L
    const val AVERAGE = 0.5

    /** Старших шаблонов места среди находок и проходов выбора вещей. */
    const val BASES = 3
    const val PASSES = 2

    val ARMOUR = listOf(Slot.HELMET, Slot.BODY, Slot.GLOVES, Slot.BOOTS, Slot.WINGS, Slot.BELT, Slot.AMULET, Slot.RING)
}
