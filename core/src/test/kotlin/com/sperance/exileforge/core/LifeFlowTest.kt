package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Итог здоровья героя: одна строка в секунду - регенерация минус деген, у героя без них - ни одной. */
class LifeFlowTest {
    private val index get() = TestContent.index
    private val rules get() = index.campaign.combat

    private fun battle(extra: Map<String, Double>): Battle {
        val heroClass = index.classes.classes.first()
        val sheet = Sheets.calculate(index, 40, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val hero = Combatant(sheet.stats + mapOf(CoreStat.HEALTH.code to 100_000.0) + extra, 40, rules)
        val zone = index.campaign.zones.first()
        val run = Run(index, zone, 7L, RunContext(heroClass.code, 40))
        val rolled = checkNotNull(Spawns(index, run).boss(zone, MapEffects.buffs(emptyMap()), emptyList()))
        val foe = PhaseFoes(index, rules).foe(rolled, zone.level)
        val boss: Foe = foe.copy(body = Combatant(foe.body.stats + (CoreStat.HEALTH.code to 1e9), foe.body.level, rules))
        return Battle(hero, listOf(boss), rules, index.rules.fight, hero.maxLife, Random(3)).also { b ->
            repeat(35) { if (b.outcome == null) b.advance(0.1) }
        }
    }

    private fun Battle.flows() = events.mapNotNull { (it.trace as? NoteTrace)?.takeIf { t -> t.kind == NoteKind.LIFE_FLOW } }

    @Test
    fun degeneration_and_regeneration_net_into_one_line_a_second() {
        val battle = battle(mapOf(CoreStat.LIFE_DEGEN_PERCENT.code to 2.0, CoreStat.HEALTH_REGEN.code to 300.0))
        val flows = battle.flows()
        assertEquals(3, flows.size, "one line a second for 3.5 s")
        flows.forEach { note ->
            val flow = checkNotNull(note.flow)
            assertEquals(2.0, flow.degenShare, 1e-9)
            assertTrue(abs(flow.degen - 2_000) < 50, "2% of 100k a second: ${flow.degen}")
            assertTrue(flow.regen > 0, "regeneration fills what the degeneration took")
            assertEquals(flow.net, note.value, 1e-9)
            assertTrue(note.value < 0)
        }
        assertTrue(battle.lifeFlow.net < 0, "the screen reads the last second")
    }

    @Test
    fun nothing_flowing_writes_no_line() {
        val battle = battle(mapOf(CoreStat.HEALTH_REGEN.code to 0.0, CoreStat.LIFE_REGEN_PERCENT.code to 0.0))
        assertTrue(battle.flows().isEmpty(), "a hero without regeneration and degeneration writes no line")
    }
}
