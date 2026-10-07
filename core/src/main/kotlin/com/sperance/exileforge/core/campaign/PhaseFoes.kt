package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.FoePhase
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster

/**
 * Враги боя с фазами боссов (3.92.0): кто как встаёт в бой - на карте, в Испытании, в прогоне экрана-вызова. Босс несёт шаги
 * своего шаблона с найденными умениями; к стае добавляется его свита - столько обычных монстров его зоны, сколько зовут
 * все шаги, - она ждёт зова вне поля и добычи не даёт.
 */
class PhaseFoes(private val index: ContentIndex, private val rules: CombatRules) {
    private val roller by lazy { MonsterRoller(index) }

    /** Враг [monster] на [level]: его лист, умения, свойства и - у босса - фазы. */
    fun foe(monster: RolledMonster, level: Int): Foe {
        val own = monster.skills.mapNotNull(index.skills.monsterByCode::get)
        val template = index.monster(monster.code)
        val steps = template?.let(index.campaign::phasesOf).orEmpty()
        return Foe(
            Combatant(monster.stats, level, rules),
            monster.rarity,
            own,
            monster,
            level,
            monster.traitsIn(index),
            index.campaign.traits.power(monster.rarity),
            phase = template?.phases?.takeIf { steps.isNotEmpty() },
            phases = steps.map { step ->
                val code = step.skill ?: step.skills.firstOrNull { it !in monster.skills }
                FoePhase(step, code?.let(index.skills.monsterByCode::get))
            },
        )
    }

    /** Стая [foes] со свитой каждого босса с фазами в конце - на костях [dice], чтобы бой без фаз не менял своих. */
    fun withRetinue(foes: List<Foe>, dice: Dice): List<Foe> {
        val retinue = foes.flatMapIndexed { boss, foe ->
            val count = foe.phases.sumOf { it.step.summon }
            val pool = foe.origin?.code?.let(::pool).orEmpty()
            if (count <= 0 || pool.isEmpty()) return@flatMapIndexed emptyList()
            List(count) { pool[dice.nextInt(pool.size)] }.mapNotNull { code -> normal(code, foe.level, dice)?.let { foe(it, foe.level).copy(summonOf = boss) } }
        }
        return foes + retinue
    }

    /** Обычные монстры зоны, чей страж или порченый страж - [boss]; пусто - звать некого. */
    private fun pool(boss: MonsterCode): List<MonsterCode> = index.zones.values.firstOrNull { it.boss == boss || it.corrupted == boss }?.monsters.orEmpty()

    private fun normal(code: MonsterCode, level: Int, dice: Dice): RolledMonster? {
        val monster = index.monster(code) ?: return null
        return roller.build(monster, level, index.campaign.rarity(MonsterRarity.NORMAL), emptyList(), dice)
    }
}
