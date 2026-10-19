package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.engage
import com.sperance.exileforge.core.campaign.run.spring
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TrapKind
import com.sperance.exileforge.rules.roll.ChestWindow
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Недуг ловушки на карте входит в следующий бой и тикает в нём (#320). */
class TrapCarryTest {
    private val index get() = TestContent.index
    private val zone get() = index.campaign.zones.first()

    private fun expedition(): ExpeditionRun {
        val heroClass = index.classes.classes.first()
        val level = 12
        val sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val gear = HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass.code), Loadout.of(HeroSkills(), index, heroClass.code, emptyList(), sheet.stats, index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, zone, 7L, RunContext(heroClass.code, level))
        val journal = RunJournal("run-7", "hero", zone.code.value)
        val campaign = CampaignState(chests = mapOf(zone.code.value to ChestWindow(Long.MAX_VALUE, 0)))
        return ExpeditionRun.start(index, zone, run, journal, gear, campaign, NOW, 0.0, level, { 0L })
    }

    /** Шипы с кровотечением: недуг ложится на героя на карте, бой принимает его с тем же остатком и силой и тикает им. */
    @Test
    fun a_trap_ailment_rides_into_the_next_fight_and_ticks_there() {
        val run = expedition()
        run.spring(MapFeature.Trap(1, TrapKind("SPIKES", "PHYSICAL", hit = 5.0, dot = 2.0, seconds = 10.0)))
        val carried = run.carry.ailments.single { it.ailment == Ailment.BLEEDING }
        assertTrue(carried.magnitude > 0, "the trap leaves a bleeding that hurts")
        run.engage(run.world.agents.first())
        val battle = assertNotNull(run.fight, "the fight starts")
        val laid = battle.heroFighter.ailments.single { it.ailment == Ailment.BLEEDING }
        assertEquals(carried.magnitude, laid.magnitude, 1e-9, "the fight takes the trap's strength")
        assertEquals(carried.left, laid.until - battle.time, 1e-9, "and what is left of its time")
        battle.advance(1.5)
        assertTrue((battle.heroFighter.tickedAt[Ailment.BLEEDING] ?: 0.0) > 0.0, "the carried bleeding ticks in the fight")
    }

    /** Каждая ловушка контента оставляет длящийся недуг: его несёт и следующий бой. */
    @Test
    fun every_trap_of_the_content_leaves_a_lasting_ailment() {
        val traps = index.campaign.features?.traps ?: return
        traps.kinds.forEach { kind -> assertTrue(kind.dot > 0 && kind.seconds > 0, "${kind.code} leaves an ailment") }
    }

    private companion object {
        const val NOW = 1_000_000_000_000L
    }
}
