package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Action
import com.sperance.exileforge.core.campaign.Battle
import com.sperance.exileforge.core.campaign.Combatant
import com.sperance.exileforge.core.campaign.Foe
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.model.campaign.CombatRules
import com.sperance.exileforge.core.model.powers.Power
import com.sperance.exileforge.core.model.powers.PowerAct
import com.sperance.exileforge.core.model.powers.PowerBase
import com.sperance.exileforge.core.model.powers.PowerBook
import com.sperance.exileforge.core.model.powers.PowerEffect
import com.sperance.exileforge.core.model.powers.PowerEvent
import com.sperance.exileforge.core.model.powers.PowerTarget
import com.sperance.exileforge.core.model.powers.SheetOp
import com.sperance.exileforge.core.model.powers.SheetRule
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The unique items' powers (2.79.0): a sheet rule as the server applies it, and an answer to a fight's event. */
class PowerTest {
    private val convert = Power("POWER_VEIL", sheet = listOf(SheetRule(SheetOp.CONVERT, "STOCK_HEALTH", "STOCK_ENERGY_SHIELD")))
    private val nova = Power("POWER_NOVA", on = PowerEvent.KILL, effects = listOf(PowerEffect(PowerAct.DAMAGE, of = PowerBase.LIFE, type = "FIRE", to = PowerTarget.ALL)))
    private val book = PowerBook(listOf(convert, nova))

    @Test fun aSheetRuleMovesItsShare() {
        val sheet = book.applySheet(mutableMapOf("POWER_VEIL" to 25.0, "STOCK_HEALTH" to 200.0, "STOCK_ENERGY_SHIELD" to 10.0))
        assertEquals(150.0, sheet["STOCK_HEALTH"])
        assertEquals(60.0, sheet["STOCK_ENERGY_SHIELD"])
    }

    @Test fun aKillSetsOffThePowerOnlyForItsBearer() {
        fun fight(power: Double): Battle {
            val rules = CombatRules()
            val hero = Combatant(mapOf("STOCK_HEALTH" to 500.0, "STOCK_ATTACK_PHYSICAL" to 60.0, "STOCK_ATTACK_SPEED" to 1.5, "POWER_NOVA" to power), 10, rules)
            val weak = Combatant(mapOf("STOCK_HEALTH" to 30.0, "STOCK_ATTACK_PHYSICAL" to 1.0, "STOCK_ATTACK_SPEED" to 0.5), 10, rules)
            val tough = Combatant(mapOf("STOCK_HEALTH" to 2000.0, "STOCK_ATTACK_PHYSICAL" to 1.0, "STOCK_ATTACK_SPEED" to 0.5), 10, rules)
            return Battle(hero, listOf(Foe(weak), Foe(tough)), rules, hero.maxLife, Random(3), kit = Loadout(powers = book))
                .also { battle -> repeat(60 * 5) { battle.advance(1.0 / 60) } }
        }
        assertTrue(fight(10.0).events.any { it.action == Action.SKILL && it.skill == "POWER_NOVA" && it.damage > 0 }, "the kill set off no nova")
        assertTrue(fight(0.0).events.none { it.skill == "POWER_NOVA" }, "a power worked without its item")
    }
}
