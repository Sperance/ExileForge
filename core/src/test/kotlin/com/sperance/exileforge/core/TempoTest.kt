package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.HeroCharges
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.heroConditions
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.FightKind
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Темп боя (#318): `combat.tempo` замедляет замах обеих сторон, а временные свойства - заряды, ярость, «недавно», баффы и
 * недуги - растянуты под него: в замахах эталонной скорости они длятся не меньше прежнего и работают в бою.
 */
class TempoTest {
    private val index get() = TestContent.index
    private val rules get() = index.campaign.combat

    /** Секунды [seconds] - в замахах эталонной скорости (`combat.unarmed.speed`) под темпом боя. */
    private fun swings(seconds: Double): Double = seconds * rules.unarmed.speed * rules.tempo

    private fun body(life: Double, speed: Double = 1.0, damage: Double = 0.01) =
        Combatant(mapOf("STOCK_HEALTH" to life, "STOCK_ATTACK_SPEED" to speed, "STOCK_ATTACK_PHYSICAL" to damage), LEVEL, rules)

    private fun battle(foes: List<Foe>, kind: FightKind = FightKind.PACK, damage: Double = 0.01): Battle {
        val hero = body(HUGE, damage = damage)
        return Battle(hero, foes, rules, index.rules.fight, hero.maxLife, Random(1), kind = kind)
    }

    private fun Battle.runTo(at: Double) {
        while (time < at - 1e-9 && outcome == null) advance(minOf(STEP, at - time))
    }

    /** Замах героя и монстра идёт в темпе боя: ударов за отрезок - скорость атаки листа × темп. */
    @Test
    fun the_tempo_slows_every_swing_of_both_sides() {
        assertTrue(rules.tempo < 1.0, "the content slows the fight")
        val battle = battle(listOf(Foe(body(HUGE))))
        battle.runTo(SPAN)
        val expected = SPAN * rules.tempo
        listOf(Side.HERO, Side.MONSTER).forEach { side ->
            val swings = battle.events.count { it.actor == side && it.action == Action.ATTACK }
            assertTrue(abs(swings - expected) <= 2.0, "$side swings $swings times in $SPAN s, expected about $expected")
        }
    }

    /** Заряд живёт `charges.duration` секунд и переживает не меньше [CHARGE_SWINGS] замахов под темпом. */
    @Test
    fun charges_last_their_duration_and_several_swings() {
        val charges = HeroCharges(index.rules.charges)
        val life = index.rules.charges.lifetime(emptyMap())
        assertTrue(charges.gain(ChargeKind.FRENZY, 1, 0.0, emptyMap(), Random(1)))
        assertFalse(charges.expire(life - 0.1), "the charge still holds just before its end")
        assertEquals(1, charges.count(ChargeKind.FRENZY))
        assertTrue(charges.expire(life + 0.1), "the charge is gone after its end")
        assertTrue(swings(life) >= CHARGE_SWINGS, "a charge outlasts $CHARGE_SWINGS swings: ${swings(life)}")
    }

    /** Ярость стража встаёт на `enrage.first` секунде боя - не раньше; до неё герой успевает не меньше [RAGE_SWINGS] замахов. */
    @Test
    fun the_guardian_rage_lights_at_its_first_second() {
        val rule = rules.enrage(FightKind.GUARDIAN)
        val battle = battle(listOf(Foe(body(HUGE))), FightKind.GUARDIAN)
        battle.runTo(rule.first - 0.5)
        assertFalse(battle.furious, "no rage before its first second")
        battle.runTo(rule.first + 0.5)
        assertTrue(battle.furious, "the rage lights at its first second")
        assertTrue(swings(rule.first) >= RAGE_SWINGS, "the rage waits $RAGE_SWINGS swings: ${swings(rule.first)}")
    }

    /** «Недавно убил» держится `recentSeconds` после убийства и гаснет после; окно - не меньше [RECENT_SWINGS] замахов. */
    @Test
    fun a_recent_kill_holds_for_the_recent_window() {
        val battle = battle(listOf(Foe(body(1.0)), Foe(body(HUGE))), damage = 50.0)
        var guard = 0
        while (battle.foe(0).alive && guard++ < 10_000) battle.advance(STEP)
        val killed = battle.time
        assertTrue(Condition.RECENT_KILL in battle.heroConditions(), "a kill is recent at once")
        battle.runTo(killed + rules.recentSeconds - 0.2)
        assertTrue(Condition.RECENT_KILL in battle.heroConditions(), "the kill is still recent")
        battle.runTo(killed + rules.recentSeconds + 0.2)
        assertFalse(Condition.RECENT_KILL in battle.heroConditions(), "the kill is no longer recent")
        assertTrue(swings(rules.recentSeconds) >= RECENT_SWINGS, "the recent window spans $RECENT_SWINGS swings: ${swings(rules.recentSeconds)}")
    }

    /** Баффы правил и недуги растянуты под темп: в замахах они не короче прежних длительностей при темпе 1. */
    @Test
    fun buffs_and_ailments_last_under_the_tempo() {
        listOf(rules.buffs.onslaught, rules.buffs.unholyMight, rules.buffs.arcaneSurge, rules.buffs.fortify).forEach { buff ->
            assertTrue(swings(buff.duration) >= BUFF_SWINGS, "a buff spans $BUFF_SWINGS swings: ${swings(buff.duration)}")
        }
        rules.ailments.filter { it.ailment != Ailment.FROZEN.name }.forEach { rule ->
            assertTrue(swings(rule.duration) >= AILMENT_SWINGS, "${rule.ailment} spans $AILMENT_SWINGS swings: ${swings(rule.duration)}")
        }
    }

    private companion object {
        const val LEVEL = 10
        const val HUGE = 1e9
        const val STEP = 0.05
        const val SPAN = 20.0

        /** Прежние длительности при темпе 1 и скорости 1,2: заряд 10 с, ярость стража с 30-й, «недавно» 4 с, баффы 4 с, недуги от 2 с. */
        const val CHARGE_SWINGS = 11.0
        const val RAGE_SWINGS = 34.0
        const val RECENT_SWINGS = 4.5
        const val BUFF_SWINGS = 4.5
        const val AILMENT_SWINGS = 2.2
    }
}
