package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Action
import com.sperance.exileforge.core.campaign.Battle
import com.sperance.exileforge.core.campaign.Combatant
import com.sperance.exileforge.core.campaign.DraughtRate
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.FlaskKind
import com.sperance.exileforge.core.campaign.Foe
import com.sperance.exileforge.core.campaign.HeroPools
import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.model.campaign.CombatRules
import com.sperance.exileforge.core.model.skills.Scale
import com.sperance.exileforge.core.model.skills.SkillDefinition
import com.sperance.exileforge.core.model.skills.SkillHit
import com.sperance.exileforge.core.model.skills.SkillType
import com.sperance.exileforge.core.model.skills.SlotCondition
import com.sperance.exileforge.core.model.skills.SpellDamage
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The fight with the class skills and the belt (2.78.0): what the kit brings is paid for, and drunk by its condition. */
class SkillCombatTest {
    private val rules = CombatRules()
    private val fireball = SkillDefinition("FIREBALL", "WITCH", SkillType.SPELL, 1, mana = Scale(10.0), cooldown = 2.0,
        hit = SkillHit(spell = SpellDamage("FIRE", Scale(20.0), Scale(30.0))))
    private val flask = Flask("FLASK_SMALL_LIFE", FlaskKind.LIFE, SlotCondition.LIFE_50,
        added = mapOf("FLASK_LIFE" to 60.0, "FLASK_CHARGES" to 30.0, "FLASK_CHARGES_PER_USE" to 10.0, "FLASK_DURATION" to 3.0))

    private fun fight(mana: Double): Battle {
        val hero = Combatant(mapOf("STOCK_HEALTH" to 200.0, "STOCK_MANA" to mana, "STOCK_ATTACK_PHYSICAL" to 5.0, "STOCK_ATTACK_SPEED" to 1.0), 10, rules)
        val foe = Combatant(mapOf("STOCK_HEALTH" to 600.0, "STOCK_ATTACK_PHYSICAL" to 25.0, "STOCK_ATTACK_SPEED" to 1.5), 10, rules)
        return Battle(hero, listOf(Foe(foe)), rules, hero.maxLife, Random(7), kit = Loadout(actives = listOf(KitSkill(fireball, 1)), flasks = listOf(flask)))
            .also { battle -> repeat(60 * 20) { battle.advance(1.0 / 60) } }
    }

    @Test fun aReadySkillIsCastForItsManaAndALifeFlaskIsDrunkAtHalfLife() {
        val battle = fight(mana = 50.0)
        val casts = battle.events.filter { it.action == Action.SKILL && it.skill == "FIREBALL" }
        assertTrue(casts.isNotEmpty(), "the fireball was never cast")
        // The first cast took its ten mana off a full pool.
        assertEquals(40.0, casts.first().heroMana, 1.0)
        val drunk = battle.events.firstOrNull { it.action == Action.FLASK }
        assertTrue(drunk == null || drunk.heroLife > 0, "a draught of the dead")
        if (battle.events.any { it.heroLife < 100.0 }) assertTrue(drunk != null, "half life went by and the flask stayed full")
    }

    @Test fun noManaNoSpell() {
        val battle = fight(mana = 0.0)
        assertTrue(battle.events.none { it.action == Action.SKILL }, "a spell cast for nothing")
    }

    /** A draught drunk as the last fight ended heals on in the next (2.81.0): its recovery rides with its time left. */
    @Test fun aRunningDraughtKeepsHealingInTheNextFight() {
        val hero = Combatant(mapOf("STOCK_HEALTH" to 200.0, "STOCK_ATTACK_PHYSICAL" to 1.0, "STOCK_ATTACK_SPEED" to 1.0), 10, rules)
        val foe = Combatant(mapOf("STOCK_HEALTH" to 600.0, "STOCK_ATTACK_SPEED" to 1.0), 10, rules)
        val carried = HeroPools(100.0, 0.0, listOf(30.0), listOf(2.0), listOf(DraughtRate(life = 20.0)))
        val battle = Battle(hero, listOf(Foe(foe)), rules, 100.0, Random(7), kit = Loadout(flasks = listOf(flask)), pools = carried)
        repeat(60) { battle.advance(1.0 / 60) }
        assertTrue(battle.pools().life > 110.0, "the carried draught stopped healing: ${battle.pools().life}")
    }
}
