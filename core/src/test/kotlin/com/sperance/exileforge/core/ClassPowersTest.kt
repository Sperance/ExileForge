package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.PowerMoment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.EffectKind
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.TimedEffect
import com.sperance.exileforge.core.campaign.combat.lay
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.PowerBook
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.sheet.KitSkill
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Механики классовых уникалок на живом бою клиента: клич и лечение умением будят свои силы, стихийное состояние силы, проклятие-умение
 * цели, эхо оглушения и строка «пока стоит барьер».
 */
class ClassPowersTest {
    private val index get() = TestContent.index
    private val rules get() = index.campaign.combat

    private fun body(extra: Map<String, Double> = emptyMap(), life: Double = 1000.0) = Combatant(
        mapOf(
            CoreStat.HEALTH.code to life,
            CoreStat.MANA.code to 10_000.0,
            CoreStat.ATTACK_PHYSICAL.code to 1.0,
            CoreStat.ATTACK_SPEED.code to 1.0,
            CoreStat.CRITICAL_CHANCE.code to 0.0,
            CoreStat.ACCURACY.code to 100_000.0,
        ) + extra,
        40,
        rules,
    )

    private fun foes(count: Int) = List(count) { Foe(body(life = 1e9)) }

    private fun battle(power: String, value: Double, foes: Int = 1, life: Double = 1000.0, active: KitSkill? = null): Battle {
        val kit = Loadout(actives = listOfNotNull(active), powers = PowerBook(index.powers.powers.filter { it.stat == power }))
        val hero = body(mapOf(power to value, CoreStat.HEALTH.code to life))
        return Battle(hero, foes(foes), rules, index.rules.fight, hero.maxLife, Random(5), kit = kit)
    }

    private fun slot(type: SkillType, pick: (KitSkill) -> Boolean = { true }): KitSkill = index.skills.skills.filter { it.type == type }.map { KitSkill(it, 1, 20, SlotCondition.READY) }.first(pick)

    private fun Battle.run(seconds: Double, done: () -> Boolean = { false }) {
        var left = seconds
        while (left > 0 && outcome == null && !done()) {
            advance(0.1)
            left -= 0.1
        }
    }

    /** Клич героя будит силу клича: «Ревущая глотка» оглушает всех врагов. */
    @Test
    fun a_warcry_stuns_every_foe() {
        val b = battle("POWER_ROARING_THROAT", 1.0, foes = 3, active = slot(SkillType.WARCRY))
        b.run(10.0) { (0 until 3).all { b.foe(it).heldUntil > b.time } }
        assertTrue((0 until 3).all { b.foe(it).heldUntil > b.time }, "a warcry stunned not every foe")
    }

    /** Умение с лечением будит силу лечения умением: «Благодать паломника» ставит барьер. */
    @Test
    fun a_healing_skill_lays_a_barrier() {
        val b = battle("POWER_PILGRIMS_GRACE", 10.0, life = 1000.0, active = slot(SkillType.HEAL) { it.skill.heal != null && it.skill.barrier == null })
        b.run(10.0) { b.heroFighter.barrier > 0 }
        assertEquals(b.heroFighter.body.maxLife * 0.1, b.heroFighter.barrier, 1e-6)
    }

    /** Барьер держит строку силы «пока стоит барьер», без барьера её нет. */
    @Test
    fun a_barrier_holds_a_standing_line() {
        val b = battle("POWER_JUDGEMENT_THRONE", 30.0)
        b.advance(0.1)
        b.heroFighter.barrier = 0.0
        b.powers.restand()
        assertTrue(b.powers.standing.isEmpty(), "a line without a barrier")
        b.heroFighter.barrier = 100.0
        b.powers.restand()
        assertTrue(b.powers.standing.any { it.stat == CoreStat.ATTACK_LIGHTNING.code && it.value == 30.0 }, "no line under a barrier")
    }

    /** Удар по проклятому врагу кладёт стихийное состояние правил; по чистому - ничего. */
    @Test
    fun a_cursed_foe_takes_an_elemental_ailment() {
        val b = battle("POWER_BONE_WITCHERY", 100.0)
        b.advance(0.1)
        val foe = b.foe(0)
        val hit = PowerMoment(foe, mapOf(DamageType.FIRE to 100.0, DamageType.COLD to 100.0, DamageType.LIGHTNING to 100.0))
        b.powers.dealt(hit, crit = false, stunned = false, inflicted = emptyList())
        assertTrue(foe.ailments.isEmpty(), "an ailment on a clean foe")
        b.lay(foe, TimedEffect(EffectKind.CURSE, "ANY_CURSE", emptyList(), 1e9, 1e9))
        b.powers.dealt(hit, crit = false, stunned = false, inflicted = emptyList())
        val laid = foe.ailments.map { it.ailment }
        assertTrue(laid.size == 1 && laid.single() in b.elementalAilments, "laid $laid")
    }

    /** Проверка проклятия-умения читает код проклятия: «Путы времени» задерживают удар, чужое проклятие - нет. */
    @Test
    fun a_named_curse_gates_a_power() {
        val b = battle("POWER_THREE_BLOODS", 100.0)
        b.advance(0.1)
        val foe = b.foe(0)
        val hit = PowerMoment(foe, mapOf(DamageType.PHYSICAL to 10.0))
        b.lay(foe, TimedEffect(EffectKind.CURSE, "ENFEEBLE", emptyList(), 1e9, 1e9))
        val was = foe.nextAttack
        b.powers.dealt(hit, crit = false, stunned = false, inflicted = emptyList())
        assertEquals(was, foe.nextAttack, 1e-9, "another curse delayed the foe")
        b.lay(foe, TimedEffect(EffectKind.CURSE, "TEMPORAL_CHAINS", emptyList(), 1e9, 1e9))
        b.powers.dealt(hit, crit = false, stunned = false, inflicted = emptyList())
        assertEquals(was + 0.9, foe.nextAttack, 1e-9, "Temporal Chains did not delay the foe")
    }

    /** Оглушение героя повторяет свой удар эхом через срок силы. */
    @Test
    fun a_stun_echoes_its_blow() {
        val b = battle("POWER_EARTHSPLITTER", 50.0)
        b.advance(0.1)
        val foe = b.foe(0)
        b.powers.dealt(PowerMoment(foe, mapOf(DamageType.PHYSICAL to 1000.0)), crit = false, stunned = true, inflicted = emptyList())
        b.run(3.0) { b.events.any { it.skill == "POWER_EARTHSPLITTER" } }
        assertTrue(b.events.any { it.skill == "POWER_EARTHSPLITTER" }, "the stun did not echo")
    }
}
