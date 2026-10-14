package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.ActiveAilment
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Blow
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.monsterCast
import com.sperance.exileforge.core.campaign.combat.place
import com.sperance.exileforge.core.campaign.combat.strike
import com.sperance.exileforge.rules.content.CoreStat
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Правила боя пакета 4 (4.12.x) на живом бою клиента: единая скорость действий (CAST-02), конечный урон (COMBAT-02), лечение сил
 * под `Recovery` (COMBAT-01), потолки (STAT-ATK-08), лёд шкалы с длительностями (STAT-DEF-01), одинаковые ауры (STAT-MISC-07),
 * отрицательная регенерация (STAT-ATK-04).
 */
class CombatRulesTest {
    private val index get() = TestContent.index
    private val rules get() = index.campaign.combat

    private fun body(extra: Map<String, Double> = emptyMap(), life: Double = 1000.0) = Combatant(
        mapOf(
            CoreStat.HEALTH.code to life,
            CoreStat.ATTACK_PHYSICAL.code to 100.0,
            CoreStat.ATTACK_SPEED.code to 1.0,
            CoreStat.CRITICAL_CHANCE.code to 0.0,
            CoreStat.ACCURACY.code to 100_000.0,
        ) + extra,
        40,
        rules,
    )

    private fun battle(hero: Combatant, vararg foes: Foe, life: Double = hero.maxLife, kit: Loadout = Loadout()) = Battle(hero, foes.toList(), rules, index.rules.fight, life, Random(11), kit = kit)

    /** CAST-02: холод и замедление аур - один множитель замаха и перезарядки умений. */
    @Test
    fun chill_and_slow_stretch_skill_cooldowns() {
        val skill = index.skills.monsterSkills.first { it.hit != null }.copy(mana = 0.0)
        val b = battle(body(), Foe(body(mapOf("AURA_SLOW" to 50.0), life = 1e9), skills = listOf(skill)))
        val foe = b.foe(0)
        b.place(foe, ActiveAilment(Ailment.CHILLED, 1e9, 30.0, 1e9, Side.HERO), false)
        assertEquals(1.3, foe.slow(), 1e-9)
        assertEquals(1 / 0.5, b.heroFighter.slow(), 1e-9)
        foe.readyAt[skill.code] = 0.0
        b.monsterCast(foe)
        assertEquals(skill.cooldown / foe.body.recovery(skill.spell) * 1.3, foe.readyAt.getValue(skill.code) - b.time, 1e-9)
    }

    /** COMBAT-02: пустой удар по цели без брони - ноль, не NaN; оба живы. */
    @Test
    fun a_zero_blow_deals_nothing() {
        assertEquals(0.0, Combatant(emptyMap(), 1, rules).physicalMitigation(0.0, rules.armour.factor))
        val b = battle(body(mapOf(CoreStat.DAMAGE.code to -100.0)), Foe(body(life = 1e6)))
        repeat(30) { if (b.outcome == null) b.advance(0.1) }
        assertTrue(b.foe(0).life.isFinite() && b.foe(0).alive && b.heroFighter.alive, "foe ${b.foe(0).life}, hero ${b.heroFighter.life}")
    }

    /** COMBAT-01: лечение силы от нанесённого урона режет запрет восстановления. */
    @Test
    fun power_healing_reads_recovery() {
        fun healed(extra: Map<String, Double>): Double {
            val b = battle(body(mapOf("POWER_LADLE_SHARE" to 2.0, CoreStat.ATTACK_PHYSICAL.code to 100_000.0) + extra), Foe(body(life = 1e15)), life = 100.0, kit = Loadout(powers = index.powers))
            repeat(30) { if (b.outcome == null) b.advance(0.1) }
            return b.heroLife
        }
        assertTrue(healed(emptyMap()) > 500, "the power heals at all")
        assertTrue(healed(mapOf(CoreStat.NO_LIFE_REGEN.code to 1.0)) <= 100.0, "a ban on recovery stops the power's healing")
    }

    /** STAT-ATK-08: добивание, цена умений, двойной урон и возврат - под потолками правил. */
    @Test
    fun combat_caps_hold() {
        val caps = rules.caps
        val capped = body(mapOf(CoreStat.CULLING.code to 60.0, CoreStat.SKILL_COST.code to 145.0, CoreStat.DOUBLE_DAMAGE.code to 90.0, CoreStat.LIFE_RECOUP.code to 90.0))
        assertEquals(caps.culling / 100, capped.culling, 1e-9)
        assertEquals(1 - caps.skillCost / 100, capped.skillCost, 1e-9)
        assertEquals(caps.doubleDamage / 100, capped.doubleDamage, 1e-9)
        assertEquals(caps.recoup / 100, capped.recoup, 1e-9)
    }

    /** STAT-DEF-01: лёд шкалы слушает длительность заморозки нападающего и сокращение на себе цели. */
    @Test
    fun freeze_bar_ice_follows_durations() {
        val rule = requireNotNull(rules.buildup).freeze
        fun ice(attacker: Map<String, Double>, defender: Map<String, Double>): Double {
            val b = battle(body(attacker), Foe(body(defender, life = 1e6)))
            val target = b.foe(0)
            target.buildup[Buildup.FREEZE.ordinal] = 0.999
            b.strike(b.heroFighter, target, Blow(mapOf(DamageType.COLD to 1000.0), Action.SKILL, spell = true, spread = false))
            return target.ailments.first { it.ailment == Ailment.FROZEN }.duration
        }
        assertEquals(rule.duration, ice(emptyMap(), emptyMap()), 1e-9)
        assertEquals(rule.duration * 2, ice(mapOf("STOCK_FREEZE_DURATION" to 100.0), emptyMap()), 1e-9)
        assertEquals(rule.duration * 0.4, ice(emptyMap(), mapOf("STOCK_FREEZE_DURATION_ON_SELF" to 60.0)), 1e-9)
    }

    /** STAT-MISC-07: одинаковые ауры не складываются - действует сильнейшая. */
    @Test
    fun the_same_aura_does_not_stack() {
        val b = battle(body(), Foe(body(mapOf("AURA_WEAKEN" to 25.0), life = 1e6)), Foe(body(mapOf("AURA_WEAKEN" to 20.0, "AURA_RESIST" to 10.0), life = 1e6)))
        assertEquals(mapOf("AURA_WEAKEN" to 25.0, "AURA_RESIST" to 10.0), b.auras())
    }

    /** STAT-ATK-04: отрицательная регенерация отнимает здоровье - против того же боя без неё. */
    @Test
    fun negative_regeneration_drains_life() {
        fun life(regen: Double): Pair<Double, Double> {
            val b = battle(body(mapOf(CoreStat.HEALTH_REGEN.code to regen)), Foe(body(life = 1e9)))
            repeat(50) { if (b.outcome == null) b.advance(0.1) }
            return b.heroLife to b.time
        }
        val (plain, time) = life(0.0)
        assertEquals(plain - 6.0 * time, life(-6.0).first, 0.5)
    }
}
