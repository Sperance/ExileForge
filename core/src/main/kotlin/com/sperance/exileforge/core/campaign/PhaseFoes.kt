package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.FoePhase
import com.sperance.exileforge.core.campaign.combat.FoeTotem
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.Monster
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster

/**
 * Враги боя с фазами боссов (3.92.0): кто как встаёт в бой - на карте, в Испытании, в прогоне экрана-вызова. Босс несёт шаги
 * своего шаблона с найденными умениями; к стае добавляется его свита - столько обычных монстров, сколько зовут все шаги, - она
 * ждёт зова вне поля и добычи не даёт. Кого звать - решает шаблон босса (`Monster.summons`, 4.0.0): своя свита с её печатями,
 * без неё - монстры его зоны.
 */
class PhaseFoes(private val index: ContentIndex, private val combat: CombatRules) {
    private val roller by lazy { MonsterRoller(index) }

    /** Враг [monster] на [level]: его лист, умения, свойства и - у босса - фазы. */
    fun foe(monster: RolledMonster, level: Int): Foe {
        val own = monster.skills.mapNotNull(index.skills.monsterByCode::get)
        val template = index.monster(monster.code)
        val steps = template?.let(index.campaign::phasesOf).orEmpty()
        return Foe(
            Combatant(monster.stats, level, combat),
            monster.rarity,
            own,
            monster,
            level,
            monster.traitsIn(index),
            index.campaign.traits.power(monster.rarity),
            phase = template?.phases?.takeIf { steps.isNotEmpty() },
            phases = steps.map { step ->
                val code = step.skill ?: step.skills.firstOrNull { it !in monster.skills }
                FoePhase(step, code?.let(index.skills.monsterByCode::get), totems(step.totems))
            },
            totems = totems(template?.totems.orEmpty()),
            totemEvery = rules.every,
            totemFirst = rules.first,
            slots = if (steps.isNotEmpty() || template?.totems.orEmpty().isNotEmpty()) rules.slots else 0,
            tainted = index.campaign.features?.blight?.tainted(monster) == true,
        )
    }

    private val rules get() = index.campaign.totems

    /** Тотемы правил по кодам [codes] (3.93.0), с проклятием-умением монстров. */
    private fun totems(codes: List<String>): List<FoeTotem> = codes.mapNotNull(rules.byCode::get).map { totem -> FoeTotem(totem, totem.curse?.let(index.skills.monsterByCode::get)) }

    /** Стая [foes] со свитой каждого босса с фазами в конце - на костях [dice], чтобы бой без фаз не менял своих. */
    fun withRetinue(foes: List<Foe>, dice: Dice): List<Foe> {
        val retinue = foes.flatMapIndexed { boss, foe ->
            val count = foe.phases.sumOf { it.step.summon }
            val template = foe.origin?.code?.let(index::monster)
            val pool = template?.let(::pool).orEmpty()
            if (count <= 0 || pool.isEmpty()) return@flatMapIndexed emptyList()
            val seals = template?.retinue?.seals.orEmpty()
            List(count) { pool[dice.nextInt(pool.size)] }.mapNotNull { code -> normal(code, foe.level, seals, dice)?.let { foe(it, foe.level).copy(summonOf = boss) } }
        }
        return foes + retinue
    }

    /**
     * Кого зовёт босс [boss] (4.0.0): `Monster.summons` его зоны - своя свита шаблона, без неё монстры зоны, где он страж или
     * порченый страж; босс без зоны и без свиты (пусто) не зовёт никого.
     */
    private fun pool(boss: Monster): List<MonsterCode> {
        val home = index.zones.values.firstOrNull { it.boss == boss.code || it.corrupted == boss.code }
        return home?.let(boss::summons) ?: boss.retinue?.monsters.orEmpty()
    }

    /** Обычный монстр свиты [code] на [level] с печатями свиты [seals] (4.0.0) - на тех же костях. */
    private fun normal(code: MonsterCode, level: Int, seals: List<ModifierCode>, dice: Dice): RolledMonster? {
        val monster = index.monster(code) ?: return null
        return roller.sealed(roller.build(monster, level, index.campaign.rarity(MonsterRarity.NORMAL), emptyList(), dice), seals, level, dice)
    }
}
