package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.ActiveAilment
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.BattleMark
import com.sperance.exileforge.core.campaign.combat.Blow
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.castTick
import com.sperance.exileforge.core.campaign.combat.intimidate
import com.sperance.exileforge.core.campaign.combat.intimidatedSkills
import com.sperance.exileforge.core.campaign.combat.intimidatedTaken
import com.sperance.exileforge.core.campaign.combat.monsterCast
import com.sperance.exileforge.core.campaign.combat.place
import com.sperance.exileforge.core.campaign.combat.strike
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.PowerBook
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.sheet.CastTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Правила боя пакета 4 на живом бою клиента: единая скорость действий, конечный урон, лечение сил
 * под `Recovery`, потолки, лёд шкалы с длительностями, одинаковые ауры,
 * отрицательная регенерация; пакет 7 - одна сила возрождения на смерть, провокация героя.
 */
class ClientCombatTest {
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

    /**
     * Холод, замедление аур и темп боя правил - один множитель замаха и перезарядки умений.
     *
     * Холод растягивает и применение; перезарядка идёт с его конца.
     */
    @Test
    fun chill_and_slow_stretch_skill_cooldowns() {
        val skill = index.skills.monsterSkills.first { it.hit != null }
        val b = battle(body(), Foe(body(mapOf("AURA_SLOW" to 50.0), life = 1e9), skills = listOf(skill)))
        val foe = b.foe(0)
        b.place(foe, ActiveAilment(Ailment.CHILLED, 1e9, 30.0, 1e9, Side.HERO), null)
        val tempo = rules.tempo
        assertEquals(1.3 / tempo, foe.slow(), 1e-9)
        assertEquals(1 / 0.5 / tempo, b.heroFighter.slow(), 1e-9)
        foe.readyAt[skill.code] = 0.0
        b.monsterCast(foe)
        val cast = assertNotNull(foe.casting)
        assertEquals(CastTime.of(skill, foe.body, rules) * 1.3 / tempo, cast.until - cast.from, 1e-9)
        cast.finish()
        assertEquals(skill.cooldown / foe.body.recovery * 1.3 / tempo, foe.readyAt.getValue(skill.code) - b.time, 1e-9)
    }

    /**
     * Оглушение прерывает применение - умение не срабатывает, маны не тратит, перезарядка не начинается; заново - не раньше
     * паузы правил, прерывание в журнале.
     */
    @Test
    fun a_stun_breaks_a_cast() {
        val skill = index.skills.monsterSkills.first { it.hit != null }
        val b = battle(body(), Foe(body(mapOf(CoreStat.MANA.code to 100.0), life = 1e9), skills = listOf(skill)))
        val foe = b.foe(0)
        foe.readyAt[skill.code] = 0.0
        val mana = foe.mana
        b.monsterCast(foe)
        assertNotNull(foe.casting)
        foe.heldUntil = b.time + 1
        b.castTick(foe)
        assertNull(foe.casting)
        assertEquals(mana, foe.mana, 1e-9)
        assertEquals(b.time + rules.cast.interruptPause, foe.readyAt.getValue(skill.code), 1e-9)
        assertTrue(b.events.any { (it.trace as? NoteTrace)?.kind == NoteKind.CAST_BROKEN }, "the break is not in the log")
    }

    /**
     * Умения монстров маны не тратят - босс без маны с «Ударом по земле» и «Ослаблением» за 30 с при темпе 1 (дольше на темп боя)
     * бьёт «Ударом» не раз и не немеет; держит его только перезарядка.
     */
    @Test
    fun a_boss_casts_without_mana() {
        val skills = listOf("MOB_SLAM", "MOB_ENFEEBLE").map { index.skills.monsterByCode.getValue(it) }
        val b = battle(body(life = 1e9), Foe(body(mapOf(CoreStat.MANA.code to 0.0), life = 1e9), MonsterRarity.UNIQUE, skills = skills))
        val seconds = 30 / rules.tempo
        repeat((seconds * 10).toInt()) { b.advance(0.1) }
        val slams = b.events.count { it.actor == Side.MONSTER && it.skill == "MOB_SLAM" && it.trace !is NoteTrace }
        assertTrue(slams >= 3, "the boss slammed $slams times in $seconds s")
    }

    /** Возврат здоровья работает и у монстра - урон по нему возвращается за срок правила. */
    @Test
    fun a_monster_recoups_too() {
        fun life(recoup: Double): Double {
            val b = battle(body(), Foe(body(mapOf(CoreStat.LIFE_RECOUP.code to recoup), life = 1e6)))
            repeat(60) { b.advance(0.1) }
            return b.foe(0).life
        }
        assertTrue(life(50.0) > life(0.0), "a monster's recoup gave nothing back")
    }

    /** Пустой удар по цели без брони - ноль, не NaN; оба живы. */
    @Test
    fun a_zero_blow_deals_nothing() {
        assertEquals(0.0, Combatant(emptyMap(), 1, rules).physicalMitigation(0.0, rules.armour.factor))
        val b = battle(body(mapOf(CoreStat.DAMAGE.code to -100.0)), Foe(body(life = 1e6)))
        repeat(30) { if (b.outcome == null) b.advance(0.1) }
        assertTrue(b.foe(0).life.isFinite() && b.foe(0).alive && b.heroFighter.alive, "foe ${b.foe(0).life}, hero ${b.heroFighter.life}")
    }

    /** Лечение силы от нанесённого урона режет запрет восстановления. */
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

    /** Добивание (босса - под своим потолком), цена умений, двойной урон и возврат - под потолками правил. */
    @Test
    fun combat_caps_hold() {
        val caps = rules.caps
        val capped = body(mapOf(CoreStat.CULLING.code to 60.0, CoreStat.SKILL_COST.code to 145.0, CoreStat.DOUBLE_DAMAGE.code to 90.0, CoreStat.LIFE_RECOUP.code to 90.0))
        assertEquals(caps.culling / 100, capped.culling(boss = false), 1e-9)
        assertEquals(caps.bossCulling / 100, capped.culling(boss = true), 1e-9)
        assertEquals(1 - caps.skillCost / 100, capped.skillCost, 1e-9)
        assertEquals(caps.doubleDamage / 100, capped.doubleDamage, 1e-9)
        assertEquals(caps.recoup / 100, capped.recoup, 1e-9)
    }

    /** Лёд шкалы слушает длительность заморозки нападающего и сокращение на себе цели. */
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

    /** Одинаковые ауры не складываются - действует сильнейшая. */
    @Test
    fun the_same_aura_does_not_stack() {
        val b = battle(body(), Foe(body(mapOf("AURA_WEAKEN" to 25.0), life = 1e6)), Foe(body(mapOf("AURA_WEAKEN" to 20.0, "AURA_RESIST" to 10.0), life = 1e6)))
        assertEquals(mapOf("AURA_WEAKEN" to 25.0, "AURA_RESIST" to 10.0), b.auras())
    }

    /** Отрицательная регенерация отнимает здоровье - против того же боя без неё. */
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

    /** Возрождение одно на бой на все силы смерти - поднимает сильнейшая по здоровью, вторая смерть - конец. */
    @Test
    fun one_revival_a_fight_by_the_strongest() {
        val kit = Loadout(powers = PowerBook(index.powers.powers.filter { it.stat == "POWER_PHOENIX_REBIRTH" || it.stat == "POWER_UNWILLING_DEATH" }))
        val b = battle(body(mapOf("POWER_PHOENIX_REBIRTH" to 30.0, "POWER_UNWILLING_DEATH" to 40.0)), Foe(body(life = 1e9)), kit = kit)
        val hero = b.heroFighter
        hero.life = 0.0
        b.powers.fire(PowerEvent.DEATH)
        assertTrue(hero.alive, "the first death found no revival")
        assertEquals(hero.body.maxLife * 0.4, hero.life, 1e-6, "the strongest revival raises the hero")
        hero.life = 0.0
        b.powers.fire(PowerEvent.DEATH)
        assertTrue(!hero.alive, "a second revival in one fight")
    }

    /** Устрашённый бьёт героя слабее (до потолка правила) и не применяет умений - его перезарядки стоят. */
    @Test
    fun an_intimidated_foe_hits_softer_and_waits() {
        val chance = rules.caps.intimidate
        val b = battle(body(mapOf(CoreStat.INTIMIDATE.code to chance * 2)), Foe(body(life = 1e9)))
        val foe = b.foe(0)
        repeat(1000) { if (!foe.intimidated) b.intimidate(foe) }
        assertTrue(foe.intimidated)
        assertEquals(1 - rules.intimidate.cut(chance, false) / 100, b.intimidatedTaken(foe, b.heroFighter), 1e-9)
        foe.readyAt["X"] = 5.0
        assertTrue(b.intimidatedSkills(foe, 1.0))
        assertEquals(6.0, foe.readyAt.getValue("X"), 1e-9)
    }

    /**
     * Метка копит долю прошедшего урона героя по цели и, истекши, взрывается им мимо защит по цели и соседу; подрыв - в журнале.
     */
    @Test
    fun a_mark_stores_damage_and_bursts_on_neighbours() {
        val b = battle(body(life = 1e9), Foe(body(life = 1e6)), Foe(body(life = 1e6)))
        val target = assertNotNull(b.target())
        val other = b.foe(1 - target.index)
        val mark = BattleMark("MARK_OF_RUIN", 0.5, 3.0, 1, DamageType.CHAOS)
        b.marks[target.index] = mark
        repeat(25) { b.advance(0.1) }
        val stored = mark.pool
        assertTrue(stored > 0, "the mark stored nothing")
        val before = other.life
        repeat(6) { b.advance(0.1) }
        assertTrue(target.index !in b.marks, "the mark did not burst")
        assertEquals(before - stored, other.life, 1e-6)
        assertTrue(b.events.any { (it.trace as? NoteTrace)?.kind == NoteKind.MARK_BURST }, "the burst is not in the log")
    }
}
