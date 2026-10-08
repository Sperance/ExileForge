package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.EnergyShield
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.AutoPlan
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.RunPhase
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ChestWindow
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Страж, что отдыхает и возвращается посреди захода, «Завершить» автопробега и общее правило энергощита (3.95.2). */
class GuardianAndAutoTest {
    private val index get() = TestContent.index
    private val zone get() = index.campaign.zones.first()

    private fun expedition(campaign: CampaignState, auto: Boolean = false): ExpeditionRun {
        val heroClass = index.classes.classes.first()
        val level = 12
        val sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val gear = HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass.code), Loadout.of(HeroSkills(), index.skills, heroClass.code, emptyList(), index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, zone, 7L, RunContext(heroClass.code, level))
        val journal = RunJournal("run-7", "hero", zone.code.value)
        return ExpeditionRun.start(index, zone, run, journal, gear, campaign, NOW, 0.0, level, { 0L }, auto = AutoPlan.takeIf { auto })
    }

    private val chests get() = mapOf(zone.code.value to ChestWindow(Long.MAX_VALUE, 0))

    @Test
    fun a_resting_guardian_returns_to_its_post_when_its_rest_ends() {
        val back = NOW + 60_000
        val run = expedition(CampaignState(chests = chests, bosses = mapOf(zone.code.value to back)))
        var clock = NOW
        run.clock = { clock }
        run.update(0.0)
        assertFalse(run.hud.value.sealed, "a resting guardian does not seal the exit")
        assertNotNull(run.hud.value.guardianRest)
        clock = back + ExpeditionRun.GUARDIAN_GRACE_MS
        run.update(0.0)
        assertTrue(run.hud.value.sealed, "the guardian is back on its post")
        assertNull(run.hud.value.guardianRest)
    }

    @Test
    fun finishing_an_autorun_leaves_the_map_with_what_it_gathered() {
        val run = expedition(CampaignState(chests = chests), auto = true)
        run.send(RunCommand.FinishAuto)
        var guard = 0
        while (run.hud.value.phase != RunPhase.LEFT && guard++ < 50_000) run.update(0.1)
        assertEquals(RunPhase.LEFT, run.hud.value.phase)
        assertEquals(MapEnd.LEFT, run.hud.value.tally.end)
    }

    @Test
    fun energy_shield_takes_all_but_chaos_and_recharges_after_its_delay() {
        assertEquals(30.0, EnergyShield.absorbed(50.0, 40.0, chaos = 10.0))
        assertEquals(0.0, EnergyShield.absorbed(50.0, 40.0, chaos = 40.0))
        val rules = index.campaign.combat
        val body = Combatant(mapOf("STOCK_ENERGY_SHIELD" to 100.0), 10, rules)
        if (body.maxShield <= 0) return
        assertEquals(0.0, EnergyShield.recovered(body, rules, 0.0, sinceHit = 0.0, dt = 1.0))
        assertTrue(EnergyShield.recovered(body, rules, 0.0, sinceHit = 60.0, dt = 1.0) > 0.0)
    }

    private companion object {
        const val NOW = 1_000_000_000_000L
    }
}
