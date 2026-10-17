package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.campaign.FightReading
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatSource
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/** A figure's window must add up to the figure: the formula of its shares plus the shifts over it. */
class SheetExplainTest {
    @Test fun breakdownAddsUpToTheSheet() {
        val index = TestContent.index
        index.classes.classes.forEach { heroClass ->
            val sheet = Sheets.calculate(index, 20, heroClass.code, emptyList(), emptyList())
            val explainer = sheet.model!!.explainer
            sheet.stats.forEach { (stat, value) ->
                val b = explainer.explain(stat)
                assertTrue(abs(b.total - value) < 0.05, "${heroClass.code} $stat total ${b.total} != $value")
                assertTrue(abs(b.formed + b.shifts.sumOf { it.delta } - value) < 0.15, "${heroClass.code} $stat formed ${b.formed} + shifts != $value")
            }
        }
    }

    /**
     * Сводка героя - лист боя: с пассивными умениями класса и каждым Предначертанием разбор цифры сводки складывается в
     * неё же - доли умений, шаги перестройки дара и что бой читает вместе с цифрой.
     */
    @Test fun combatBreakdownAddsUpToTheSummary() {
        val index = TestContent.index
        index.classes.classes.forEach { heroClass ->
            val passives = index.skills.ofClass(heroClass.code).filter { it.type == SkillType.AURA || it.type == SkillType.BONUS }.map { it.code }
            val skills = HeroSkills(learned = passives.associateWith { 10 }, passive = passives)
            (index.fates.all.map { it.code } + null).forEach { fate ->
                val sheet = Sheets.calculate(index, 60, heroClass.code, emptyList(), emptyList(), fate = fate)
                val combat = CombatSheet.of(index, sheet, 60, heroClass.code, skills, fate, emptyList())!!
                combat.figures.values.forEach { (stat, value) ->
                    val b = combat.explain(stat)
                    assertTrue(abs(b.total - value) < 0.15, "${heroClass.code} $fate $stat total ${b.total} != $value")
                    assertTrue(abs(b.formed + b.shifts.sumOf { it.delta } - value) < 0.25, "${heroClass.code} $fate $stat formed ${b.formed} + shifts != $value")
                }
                if (fate == LIVING_FORTRESS) {
                    val regen = CoreStat.HEALTH_REGEN.code
                    assertTrue(regen in combat.figures.fated && (combat.figures.values[regen] ?: 0.0) > 0, "${heroClass.code}: регенерация Живой крепости не в сводке")
                }
            }
        }
    }

    /**
     * Поток здоровья - одна функция правил: итог разбора «Регенерации здоровья», её цифра сводки и что тик боя даёт за
     * секунду - одно число, и под картой со всеми частями (доля максимума, «меньше регенерации», из маны, скорость восстановления,
     * деген).
     */
    @Test fun lifeFlowBreakdownIsTheSummaryAndTheFight() {
        val index = TestContent.index
        val rules = index.campaign.combat
        val heroClass = index.classes.classes.first()
        val sheet = Sheets.calculate(index, 40, heroClass.code, emptyList(), emptyList())
        val combat = CombatSheet.of(index, sheet, 40, heroClass.code, HeroSkills(), null, emptyList())!!
        val regen = CoreStat.HEALTH_REGEN.code
        val moves = mapOf(
            CoreStat.HEALTH.code to 100_000.0 - (combat.stats[CoreStat.HEALTH.code] ?: 0.0),
            regen to 300.0,
            CoreStat.LIFE_REGEN_PERCENT.code to 1.0,
            CoreStat.LIFE_DEGEN_PERCENT.code to 2.0,
            CoreStat.RECOVERY_RATE.code to 20.0,
            CoreStat.MANA_REGEN_TO_LIFE.code to 20.0,
            CoreStat.LIFE_REGEN_LESS.code to 30.0,
        )
        val layer = moves.mapValues { (_, delta) -> listOf(Shift(StatSource(SourceKind.MAP, MAP), delta)) }
        val stats = combat.stats + moves.mapValues { (stat, delta) -> (combat.stats[stat] ?: 0.0) + delta }
        val summary = combat.figures(stats).values.getValue(regen)
        val breakdown = combat.explain(regen, layer).total
        assertTrue(abs(summary - breakdown) < 0.15, "разбор $breakdown != сводка $summary")
        val hero = Combatant(stats, 40, rules)
        val zone = index.campaign.zones.first()
        val rolled = checkNotNull(Spawns(index, Run(index, zone, 7L, RunContext(heroClass.code, 40))).boss(zone, MapEffects.buffs(emptyMap()), emptyList()))
        val foe = PhaseFoes(index, rules).foe(rolled, zone.level)
        val boss = foe.copy(body = Combatant(foe.body.stats + (CoreStat.HEALTH.code to 1e9), foe.body.level, rules))
        val battle = Battle(hero, listOf(boss), rules, index.rules.fight, hero.maxLife, Random(3))
        repeat(25) { if (battle.outcome == null) battle.advance(0.1) }
        val fight = battle.lifeFlow.net
        assertTrue(summary < 0 && abs(fight - summary) < abs(summary) * 0.01, "бой $fight/с != сводка $summary/с")
    }

    /** Часть строки сводки из контента (`partOf`) читается боем вместе со строкой: иначе её цифра пропала бы из сводки. */
    @Test fun everyRowPartIsReadByTheFight() {
        val registry = TestContent.index.stats
        val read = FightReading.entries.map { it.stat to it.from }.toSet()
        registry.stats.filter { it.partOf != null }.forEach { part -> assertTrue(part.partOf to part.code in read, "${part.code}: часть ${part.partOf} без FightReading") }
    }

    private companion object {
        const val LIVING_FORTRESS = "LIVING_FORTRESS"

        /** Источник сдвигов листа «под картой» в тесте потока. */
        const val MAP = "MAP_TEST"
    }
}
