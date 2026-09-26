package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.RunPhase
import com.sperance.exileforge.core.model.campaign.AbyssDepth
import com.sperance.exileforge.core.model.campaign.AbyssLaunch
import com.sperance.exileforge.core.model.campaign.AbyssOpened
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignMonster
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/** The Abyss (2.82.0): a crack opens a descent, its wave rises at the depth's level in fights the arena holds, and none walks away. */
class AbyssRunTest {

    @Test fun aCrackOpensADescentWhoseWaveNobodyWalksAwayFrom() {
        val foe = CampaignMonster("CHASM_SPAWN", stats = mapOf("STOCK_HEALTH" to 50.0, "STOCK_ATTACK_PHYSICAL" to 1.0, "STOCK_ATTACK_SPEED" to 1.0))
        val zone = CampaignMap("ZONE", level = 12, monsters = listOf(foe.copy(code = "ZONE_BEAST")), size = 32)
        val depths = (1..3).map { AbyssDepth(level = 12 + it, count = listOf(5, 5), monsters = listOf(foe)) }
        val rarities = MonsterRarity.entries.map { CampaignRarity(it.name, if (it == MonsterRarity.NORMAL) 100 else 0) }
        var opened = -1
        val run = ExpeditionRun.start(zone, rarities, HeroGear(mapOf("STOCK_HEALTH" to 500.0, "STOCK_ATTACK_PHYSICAL" to 5.0, "STOCK_ATTACK_SPEED" to 1.0), 12),
            seed = 5, onKill = {}, onCleared = {}, abyss = AbyssLaunch(cracks = listOf(3), depths = depths, keep = 25.0), onAbyssOpen = { opened = it })
        val crack = run.world.cracks.single()
        run.world.heroX = crack.cell.x + 0.5
        run.world.heroY = crack.cell.y + 0.5
        run.update(0.016)
        assertEquals(RunPhase.ABYSS, run.hud.value.phase)
        assertEquals(3, run.hud.value.abyss?.depth)
        run.send(RunCommand.Descend); run.update(0.016)
        assertEquals(0, opened, "the crack is named by its place among those unopened")
        run.send(RunCommand.AbyssOpened(AbyssOpened(depth = 3))); run.update(0.016)
        val fight = assertNotNull(run.hud.value.fight)
        assertEquals(RunPhase.FIGHT, run.hud.value.phase)
        assertEquals(13, fight.level, "the first depth stands at its own level")
        assertEquals(4, fight.foes.size, "five foes come as a fight of four and one of one")
        assertFalse(fight.escape)
        run.send(RunCommand.Retreat); run.update(0.016)
        assertEquals(RunPhase.FIGHT, run.hud.value.phase, "there is no walking away from a wave")
        assertEquals(0, run.hud.value.cracksLeft)
    }
}
