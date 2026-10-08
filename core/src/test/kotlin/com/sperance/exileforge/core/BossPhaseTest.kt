package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.FoePhase
import com.sperance.exileforge.core.campaign.combat.FoeTotem
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Фазы босса (3.92.0): каждый шаг срабатывает раз, свита встаёт по зову, падает с боссом и убийством не считается. */
class BossPhaseTest {
    private val index get() = TestContent.index

    @Test
    fun the_retinue_answers_the_phase_and_falls_with_the_boss() {
        val heroClass = index.classes.classes.first()
        val zone = index.campaign.zones.first()
        val rules = index.campaign.combat
        val sheet = Sheets.calculate(index, 40, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        // Герой заведомо сильнее: проверяется не баланс, а ход фаз
        val hero = Combatant(sheet.stats + mapOf("STOCK_HEALTH" to 1_000_000.0, "STOCK_ATTACK_PHYSICAL" to 40.0), 40, rules)
        val run = Run(index, zone, 7L, RunContext(heroClass.code, 40))
        val boss = checkNotNull(Spawns(index, run).boss(zone, MapEffects.buffs(emptyMap()), emptyList()))
        val phases = PhaseFoes(index, rules)
        val steps = index.campaign.phasesByCode.getValue("SUMMON").steps.sortedByDescending { it.at }
        val foe = phases.foe(boss, zone.level).copy(phase = "SUMMON", phases = steps.map { FoePhase(it) }, slots = index.campaign.totems.slots)
        val foes = phases.withRetinue(listOf(foe), Dice(3L))
        assertEquals(1 + steps.sumOf { it.summon }, foes.size)
        val battle = Battle(hero, foes, rules, index.rules.fight, hero.maxLife, Random(11L))
        var guard = 0
        while (battle.outcome == null && guard++ < 100_000) battle.advance(0.1)
        assertEquals(Outcome.WIN, battle.outcome)
        assertEquals(listOf(0), battle.fallen, "only the boss is a kill")
        val notes = battle.events.mapNotNull { (it.trace as? NoteTrace)?.takeIf { t -> t.kind == NoteKind.PHASE } }
        assertEquals(steps.size, notes.size, "each step once")
        assertTrue(foes.indices.drop(1).all { !battle.foe(it).alive })
    }

    /** Тотемы (3.93.0): свой тотем босса встаёт в слот, бить его нельзя, и все рассыпаются со смертью босса. */
    @Test
    fun totems_stand_in_the_slots_and_crumble_with_the_boss() {
        val heroClass = index.classes.classes.first()
        val zone = index.campaign.zones.first()
        val rules = index.campaign.combat
        val sheet = Sheets.calculate(index, 40, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val hero = Combatant(sheet.stats + mapOf("STOCK_HEALTH" to 1_000_000.0, "STOCK_ATTACK_PHYSICAL" to 8.0), 40, rules)
        val run = Run(index, zone, 7L, RunContext(heroClass.code, 40))
        val boss = checkNotNull(Spawns(index, run).boss(zone, MapEffects.buffs(emptyMap()), emptyList()))
        val totems = index.campaign.totems
        val own = listOf("TOTEM_RAGE", "TOTEM_FLAME").map { FoeTotem(totems.byCode.getValue(it)) }
        val foe = PhaseFoes(index, rules).foe(boss, zone.level).copy(totems = own, totemEvery = 3.0, totemFirst = 1.0, slots = totems.slots)
        val battle = Battle(hero, listOf(foe), rules, index.rules.fight, hero.maxLife, Random(5L))
        var most = 0
        var guard = 0
        while (battle.outcome == null && guard++ < 100_000) {
            battle.advance(0.1)
            most = maxOf(most, battle.totems.size)
            assertTrue(battle.totems.size <= totems.slots)
        }
        assertEquals(Outcome.WIN, battle.outcome)
        assertTrue(most > 0, "a totem stood")
        assertTrue(battle.totems.isEmpty(), "the totems crumbled with the boss")
        assertEquals(listOf(0), battle.fallen, "a totem is never a kill")
    }
}
