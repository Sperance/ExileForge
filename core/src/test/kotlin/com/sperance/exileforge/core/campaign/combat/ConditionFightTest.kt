package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.BuffLayer
import com.sperance.exileforge.rules.content.BuffStack
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.StatOperation
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Условные строки листа работают в бою (#315) - по одной на каждое условие правил ([Condition]), строкой контента с этим условием:
 * условие героя бой видит в своём состоянии ([heroConditions]) и кладёт строку на тело героя, пока оно держится (тело без условий
 * считается уже в этом состоянии - заряды и баффы кладут и свои строки); условие цели читается на ударе ([states]) и даёт
 * увеличение урона против такой цели.
 */
class ConditionFightTest {
    private val index = TestContent.index
    private val rules = index.campaign.combat

    /** Строка контента с условием [condition]: первая по коду, значение 40 на каждом месте. */
    private fun line(condition: Condition): List<StatOperation> {
        val def = index.families.keys.sorted().mapNotNull { index.modifier(ModifierCode(it)) }
            .first { def -> !def.monster && def.effects.any { it.condition == condition } }
        return SheetCalculator(index).expand(listOf(Line(def.code, List(def.valueCount) { 40.0 })))
    }

    private val base = mapOf(
        CoreStat.HEALTH.code to 1000.0,
        CoreStat.ENERGY_SHIELD.code to 200.0,
        CoreStat.ATTACK_PHYSICAL.code to 100.0,
        CoreStat.ATTACK_SPEED.code to 1.0,
        CoreStat.ARMOR.code to 100.0,
        CoreStat.CRITICAL_CHANCE.code to 5.0,
        CoreStat.ACCURACY.code to 100_000.0,
    )

    /** Бой героя со строками [extra] против одного врага [foe]. */
    private fun battle(extra: List<StatOperation>, foe: Foe = Foe(Combatant(mapOf(CoreStat.HEALTH.code to 1e6), 40, rules))): Battle {
        val model = SheetModel(base, extra, index)
        val kit = Loadout(charges = index.rules.charges)
        val gear = HeroGear(model.plain, 40, model, kit = kit, percent = index.stats.percent)
        val build = HeroBuild(gear, emptyMap(), rules)
        return Battle(build.body, listOf(foe), rules, index.rules.fight, build.body.maxLife, Random(5), kit = kit, model = build, percent = index.stats.percent)
    }

    /** Что делает условие героя [condition] истинным в бою [b]. */
    private fun hold(condition: Condition, b: Battle) {
        val hero = b.heroFighter
        when (condition) {
            Condition.LOW_LIFE -> hero.life = 1.0
            Condition.FULL_LIFE, Condition.FULL_SHIELD -> Unit
            Condition.RECENT_KILL -> b.killedAt = b.time
            Condition.RECENT_HIT_TAKEN -> hero.lastHit = b.time
            Condition.RECENT_BLOCK -> b.blockedAt = b.time
            Condition.RECENT_CRIT -> b.critAt = b.time
            Condition.ONSLAUGHT -> hero.effects += buff(BuffKind.ONSLAUGHT)
            Condition.FORTIFIED -> hero.effects += buff(BuffKind.FORTIFY)
            Condition.FRENZY_CHARGE -> charge(b, ChargeKind.FRENZY)
            Condition.POWER_CHARGE -> charge(b, ChargeKind.POWER)
            Condition.ENDURANCE_CHARGE -> charge(b, ChargeKind.ENDURANCE)
            Condition.FLASK_ACTIVE -> hero.effects += TimedEffect(EffectKind.FLASK, "FLASK", emptyList(), b.time + 5, 5.0)
            Condition.VS_RARE, Condition.VS_UNIQUE, Condition.VS_FULL_LIFE, Condition.VS_BLIGHTED -> error("условие цели: $condition")
        }
    }

    private fun buff(kind: BuffKind) = TimedEffect(EffectKind.BUFF, kind.source, emptyList(), 100.0, 100.0, buff = BuffStack(kind, BuffLayer.TIMED, 100.0))

    private fun charge(b: Battle, kind: ChargeKind) {
        assertTrue(b.heroCharges.gain(kind, 1, b.time, b.heroFighter.body.stats, b.random), "заряд $kind не набран")
    }

    @Test
    fun every_hero_condition_lays_its_line_in_the_fight() {
        Condition.entries.filterNot { it.target }.forEach { condition ->
            val b = battle(line(condition))
            assertTrue(b.conditioned, "$condition: бой не видит условных строк листа")
            hold(condition, b)
            b.conditions = emptySet()
            val without = b.heroBody(emptyList()).stats
            b.conditions = b.heroConditions()
            assertTrue(condition in b.conditions, "$condition: бой не узнал условие")
            assertNotEquals(without, b.heroBody(emptyList()).stats, "$condition: строка не легла на героя")
        }
    }

    @Test
    fun a_hero_condition_that_does_not_hold_lays_nothing() {
        val b = battle(line(Condition.LOW_LIFE))
        b.conditions = b.heroConditions()
        assertTrue(Condition.LOW_LIFE !in b.conditions)
        assertEquals(b.heroBody(emptyList()).stats, battle(emptyList()).heroBody(emptyList()).stats)
    }

    @Test
    fun every_target_condition_grows_the_blow_against_its_target() {
        val plain = Combatant(mapOf(CoreStat.HEALTH.code to 1e6), 40, rules)
        val cases = mapOf(
            Condition.VS_RARE to Foe(plain, MonsterRarity.RARE),
            Condition.VS_UNIQUE to Foe(plain, MonsterRarity.UNIQUE),
            Condition.VS_FULL_LIFE to Foe(plain),
            Condition.VS_BLIGHTED to Foe(plain, tainted = true),
        )
        assertEquals(Condition.entries.filter { it.target }.toSet(), cases.keys, "условие цели без проверки")
        cases.forEach { (condition, foe) ->
            val b = battle(line(condition), foe)
            val states = b.states(b.foe(0))
            assertTrue(condition in states, "$condition: бой не узнал цель")
            assertTrue(b.model.against(states) > 0, "$condition: нет прибавки урона против цели")
            assertEquals(0.0, b.model.against(states - condition), "$condition: прибавка без своего условия")
        }
    }
}
