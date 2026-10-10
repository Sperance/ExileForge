package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.SkillType
import kotlin.math.abs
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
     * Сводка героя - лист боя (4.6.4): с пассивными умениями класса и каждым Предначертанием разбор цифры сводки складывается в
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
                    val regen = CoreStat.LIFE_REGEN_PERCENT.code
                    assertTrue(regen in combat.figures.fated && (combat.figures.rates[regen] ?: 0.0) > 0, "${heroClass.code}: регенерация Живой крепости не в сводке")
                }
            }
        }
    }

    private companion object {
        const val LIVING_FORTRESS = "LIVING_FORTRESS"
    }
}
