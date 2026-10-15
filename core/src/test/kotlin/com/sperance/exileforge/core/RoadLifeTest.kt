package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.AilmentLaw
import com.sperance.exileforge.core.campaign.combat.CarriedAilment
import com.sperance.exileforge.core.campaign.combat.CarriedCharge
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.HeroCarry
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.RoadLife
import com.sperance.exileforge.core.quests.live
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Counter
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.Rarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Карта между боями (4.10.0): здоровье по чистой скорости (ранящий недуг глушит восстановление, 0.14.5), несомое из боя тает, шаг после босса, счёт заданий от снимка доски. */
class RoadLifeTest {
    @Test
    fun regeneration_fills_up_to_the_maximum() {
        assertEquals(60.0, RoadLife(10.0).after(50.0, 100.0, 1.0))
        assertEquals(100.0, RoadLife(10.0).after(95.0, 100.0, 1.0))
    }

    @Test
    fun degeneration_off_the_fight_stops_at_one() {
        assertEquals(40.0, RoadLife(-10.0).after(50.0, 100.0, 1.0))
        assertEquals(1.0, RoadLife(-10.0).after(5.0, 100.0, 1.0))
        assertEquals(1.0, RoadLife(-10.0).after(1.0, 100.0, 1.0))
    }

    @Test
    fun what_the_hero_carries_melts_on_the_road_and_burnings_do_not_stack() {
        val law = AilmentLaw(TestContent.index.campaign.combat)
        val weak = CarriedAilment(Ailment.BURNING, left = 2.0, magnitude = 5.0, duration = 4.0)
        val strong = CarriedAilment(Ailment.BURNING, left = 1.0, magnitude = 9.0, duration = 4.0)
        assertEquals(listOf(strong), law.laid(listOf(weak), strong))
        val carry = HeroCarry(ailments = listOf(weak), charges = listOf(CarriedCharge(ChargeKind.FRENZY, 2, 0.5)))
        val later = carry.after(1.0)
        assertEquals(1.0, later.ailments.single().left, 1e-9)
        assertEquals(emptyList(), later.charges)
        assertEquals(true, later.ailments.any { it.ailment.hurts })
    }

    @Test
    fun a_hurting_ailment_stalls_regeneration_and_rest_off_the_fight() {
        val index = TestContent.index
        val hero = Combatant(mapOf(CoreStat.HEALTH.code to 1_000.0, CoreStat.HEALTH_REGEN.code to 50.0), 40, index.campaign.combat)
        val pace = index.campaign.expedition
        val free = RoadLife.flow(hero, pace, index.campaign.combat.mana)
        assertTrue(free.regen > free.rest && free.rest > 0, "regeneration and rest flow without an ailment")
        Ailment.entries.filterNot { it.hurts }.forEach { assertEquals(free, RoadLife.flow(hero, pace, index.campaign.combat.mana, stalledBy = it)) }
        val bled = RoadLife.flow(hero, pace, index.campaign.combat.mana, stalledBy = Ailment.BLEEDING)
        assertEquals(0.0, bled.regen)
        assertEquals(0.0, bled.rest)
        assertEquals(free.regen, bled.stalled, 1e-9)
        assertEquals(free.degen, bled.degen, 1e-9)
        assertTrue(RoadLife.of(hero, pace, index.campaign.combat.mana, stalledBy = Ailment.POISONED).net <= 0)
    }

    @Test
    fun the_life_label_shows_only_a_moving_life() {
        assertFalse(LifeFlow(regen = 10.0).moves(100.0, 100.0), "full life gains nothing")
        assertTrue(LifeFlow(regen = 10.0).moves(50.0, 100.0))
        assertFalse(LifeFlow(degen = 10.0).moves(1.0, 100.0), "the degeneration floor holds")
        assertTrue(LifeFlow(stalledBy = Ailment.BURNING).moves(100.0, 100.0), "an ailment that stalls recovery is always named")
        assertEquals(-4.0, LifeFlow(ailments = 10.0, shielded = 6.0).net, 1e-9, "what the shield takes leaves life alone")
    }

    @Test
    fun boss_slain_speed_lies_over_the_sheet_cap() {
        val rules = TestContent.index.campaign.expedition.copy(heroSpeed = 3.0, bossSlainSpeed = 2.0)
        val fast = mapOf(CoreStat.MOVEMENT_SPEED.code to 400.0)
        assertEquals(3.0 * 2.5, ExpeditionWorld.heroSpeed(rules, fast), 1e-9)
        assertEquals(3.0 * 2.5 * 2.0, ExpeditionWorld.heroSpeed(rules, fast, bossSlain = true), 1e-9)
    }

    @Test
    fun the_run_adds_its_tally_to_the_board_taken_at_entry_once() {
        val atEntry = Quest("q", QuestKind.DAILY, "kill", Counter.KILLS, Rarity.COMMON, target = 10, progress = 3)
        assertEquals(7, atEntry.live(mapOf(Counter.KILLS to 4L)).progress)
        assertEquals(false, atEntry.live(mapOf(Counter.KILLS to 4L)).done)
        assertEquals(10, atEntry.live(mapOf(Counter.KILLS to 40L)).progress)
    }
}
