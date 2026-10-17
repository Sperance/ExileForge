package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.campaign.FightReading
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Power
import com.sperance.exileforge.rules.content.PowerAct
import com.sperance.exileforge.rules.content.PowerEffect
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.SkillStat
import com.sperance.exileforge.rules.content.StatSpread

/** Носитель, что даёт или тратит характеристику героя: умение, уникальный предмет (его шаблон) или узел древа. */
sealed interface StatHolder {
    /** Умение [code]: строки ауры или пассивного умения, усиление, накопитель или трата зарядов. */
    data class Skill(val code: String) : StatHolder

    /** Уникальный предмет шаблона [template]: своя строка или сила. */
    data class Unique(val template: String) : StatHolder

    /** Узел древа [code] с именем (знаковый, ключевой, мастерство): своя строка или сила. */
    data class Node(val code: String) : StatHolder
}

/** Кто даёт характеристику ([gives]) и кто её тратит ([spends]); у большинства характеристик трат нет. */
data class StatLinks(val gives: List<StatHolder>, val spends: List<StatHolder>) {
    val isEmpty: Boolean get() = gives.isEmpty() && spends.isEmpty()
}

/**
 * Связи характеристик героя, собранные по контенту: какие умения, уникальные предметы и узлы древа с именем её дают - своей
 * строкой (с её частями и тем, что бой читает вместе с ней, общее сопротивление стихиям у каждой стихии) или силой, что кладёт её
 * на лист или усилением. У максимума зарядов вида - ещё накопители этих зарядов (умения и силы `CHARGE`) и их траты: умения,
 * что расходуют заряды, и силы, что снимают их. Строки проклятий и удара умения - на враге и в одном ударе, героя не дают.
 * Мелкие узлы древа без имени не называются - их слишком много, и связь читается из строки узла.
 */
class StatRelations(private val index: ContentIndex) {
    private val memo = HashMap<String, StatLinks>()

    /** Связи характеристики [stat]. */
    fun of(stat: String): StatLinks = synchronized(memo) { memo.getOrPut(stat) { build(stat) } }

    private fun build(stat: String): StatLinks {
        val inputs = inputs(stat)
        val charge = index.rules.charges.kinds.entries.firstOrNull { it.value.max == stat }?.key
        val givingPowers = index.powers.powers.filter { power -> gives(power, inputs) || charge != null && charges(power.effects, charge, consume = false) }.mapTo(HashSet()) { it.stat }
        val spendingPowers = if (charge == null) emptySet() else index.powers.powers.filter { charges(it.effects, charge, consume = true) }.mapTo(HashSet()) { it.stat }
        val skills = index.skills.skills
        val gives = skills.filter { skill -> lines(skill).any { it.gives(inputs) } || charge != null && gains(skill, charge) }.map { StatHolder.Skill(it.code) } +
            holders(inputs + givingPowers)
        val spends = (if (charge == null) emptyList() else skills.filter { spends(it, charge) }.map { StatHolder.Skill(it.code) }) + holders(spendingPowers)
        return StatLinks(gives.distinct(), spends.distinct())
    }

    /** Что читается как сама характеристика [stat]: она, её части (`partOf`) и то, что бой прибавляет к ней. */
    private fun inputs(stat: String): Set<String> = setOf(stat) + index.stats.parts(stat) + FightReading.entries.filter { it.stat == stat }.map { it.from }

    /** Уникальные предметы и узлы древа с именем, чья строка - одна из [stats] (или раскрывается в неё). */
    private fun holders(stats: Set<String>): List<StatHolder> {
        if (stats.isEmpty()) return emptyList()
        val uniques = index.templates.values.filter { template ->
            template.rarity == Rarity.UNIQUE && (template.lines.flatten().any { gives(it.stat, it.op, stats) } || template.fixed.any { code -> index.modifier(code)?.effects?.any { gives(it.stat, it.op, stats) } == true })
        }.map { StatHolder.Unique(it.code) }
        val nodes = index.content.tree.nodes.filter { node ->
            node.type in NAMED && (node.lines + node.options.flatten()).any { line -> index.modifier(line.code)?.effects?.any { gives(it.stat, it.op, stats) } == true }
        }.map { StatHolder.Node(it.code) }
        return uniques + nodes
    }

    /** Строки умения на герое: пассивное умение и аура, его усиление и усиление срабатывания. */
    private fun lines(skill: SkillDefinition): List<SkillStat> = skill.stats + skill.buff?.stats.orEmpty() + skill.trigger?.buff?.stats.orEmpty()

    private fun SkillStat.gives(stats: Set<String>): Boolean = gives(stat, op, stats)

    /** Сила кладёт на лист или усилением одну из [stats]. */
    private fun gives(power: Power, stats: Set<String>): Boolean = power.sheet.any { rule -> rule.to in stats || rule.also.any { it in stats } } || effects(power.effects).any { effect -> effect.act == PowerAct.BUFF && effect.lines.any { gives(it.stat, it.op, stats) } }

    /** Действия силы с вложенными вариантами «одно из». */
    private fun effects(effects: List<PowerEffect>): List<PowerEffect> = effects.flatMap { listOf(it) + effects(it.options) }

    /** Действие силы даёт ([consume] false) или снимает заряды вида [kind]: случайный вид даёт любой, «все» снимают любой. */
    private fun charges(effects: List<PowerEffect>, kind: ChargeKind, consume: Boolean): Boolean = effects(effects).any { effect ->
        effect.act == PowerAct.CHARGE && effect.consume == consume && effect.charge?.let { it == kind || it == if (consume) ChargeKind.ALL else ChargeKind.RANDOM } == true
    }

    private fun gains(skill: SkillDefinition, kind: ChargeKind): Boolean = skill.charges?.let { it.gain != null && (it.kind == kind || it.kind == ChargeKind.RANDOM) } == true

    private fun spends(skill: SkillDefinition, kind: ChargeKind): Boolean = skill.charges?.let { it.consume && (it.kind == kind || it.kind == ChargeKind.ALL) } == true

    private companion object {
        /** Узлы древа с именем: их связь называется. */
        val NAMED = setOf(SkillNodeType.NOTABLE, SkillNodeType.KEYSTONE, SkillNodeType.MASTERY)

        /** Строка [stat] с действием [op] - одна из [stats] сама или раскрывается в неё (все сопротивления, все атрибуты). */
        fun gives(stat: String, op: Op, stats: Set<String>): Boolean = stat in stats || StatSpread.targets(stat, op).any { it in stats }
    }
}
