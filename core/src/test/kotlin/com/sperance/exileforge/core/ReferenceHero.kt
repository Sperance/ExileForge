package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.campaign.FightReading
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Effect
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.StatRegistry
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.SourcedLine
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatSource
import com.sperance.exileforge.rules.sheet.sourcedLines

/** Общее сторожей сводки героя ([SheetCompletenessTest], [EquippedModifiersTest]): эталон, пробы и строки сводки, что двигает строка. */
internal object SummaryProbe {
    const val LEVEL = 60

    /** Число пробы строки. */
    const val PROBE = 30.0

    /** Какая доля строк должна попасть в пробу, чтобы тест что-то доказывал. */
    const val MIN_SHARE = 0.9

    /** Сдвиг меньше этого - не сдвиг. */
    const val MOVED = 1e-9

    /**
     * Эталонный герой поверх класса: что даёт обычное снаряжение - урон, защиты, щит, запасы, регенерация, крит, недуги,
     * вампиризм, порог оглушения и счёт надетого. Строка «увеличения» характеристики, которой здесь нет, требует базы у эталона
     * или своей плоской строки.
     */
    val REFERENCE = mapOf(
        "STOCK_ATTACK_PHYSICAL" to 120.0, "STOCK_ATTACK_FIRE" to 30.0, "STOCK_ATTACK_COLD" to 30.0, "STOCK_ATTACK_LIGHTNING" to 30.0, "STOCK_ATTACK_CHAOS" to 20.0,
        "STOCK_ATTACK_SPEED" to 1.4, "STOCK_HEALTH" to 3000.0, "STOCK_MANA" to 300.0, "STOCK_ENERGY_SHIELD" to 300.0, "STOCK_ARMOR" to 800.0,
        "STOCK_EVASION" to 600.0, "STOCK_BLOCK_CHANCE" to 15.0, "STOCK_ALL_ELEMENTAL_RESISTANCES" to 30.0, "STOCK_CRITICAL_CHANCE" to 20.0, "STOCK_SPELL_CRITICAL_CHANCE" to 20.0,
        "STOCK_IGNITE_CHANCE" to 20.0, "STOCK_SHOCK_CHANCE" to 20.0, "STOCK_POISON_CHANCE" to 20.0, "STOCK_BLEED_CHANCE" to 20.0, "STOCK_FREEZE_CHANCE" to 10.0,
        "STOCK_HEALTH_REGEN" to 10.0, "STOCK_MANA_REGEN_FLAT" to 8.0, "STOCK_LEECH_PHYSICAL" to 2.0, "STOCK_LEECH_MANA" to 2.0, "STOCK_STUN_THRESHOLD" to 100.0,
        "STOCK_WORN_UNIQUES" to 1.0, "STOCK_WORN_CORRUPTED" to 1.0, "STOCK_WORN_JEWELS" to 1.0,
    )

    /**
     * Атрибуты эталона - строками листа, как у героя с деревом: их видят требования вещей, так эталон носит любую вещь контента
     * (требования атрибутов не выше этого).
     */
    val ATTRIBUTES = mapOf(CoreStat.STRENGTH.code to 300.0, CoreStat.AGILITY.code to 300.0, CoreStat.INTELLECT.code to 300.0)

    /**
     * Строки сводки, что двигает операция [op] над [stat]: цифры членов, на которые она раскладывается ([StatSpread]), строки, частью
     * которых они объявлены, и то, что бой читает вместе с ними ([FightReading]).
     */
    fun rows(stat: String, op: Op, registry: StatRegistry): Set<String> {
        val targets = StatSpread.targets(stat, op)
        return (targets + targets.map(registry::row) + FightReading.entries.filter { it.from in targets }.map { it.stat }).toSet()
    }
}

/**
 * Эталонный герой класса [heroClass] уровня [level] со стартовым узлом, атрибутами [SummaryProbe.ATTRIBUTES] и надетыми [equipped]
 * (самоцвет - во взятом гнезде): лист правил ([SheetCalculator.calculate], тот же, что у листа игры) и [SummaryProbe.REFERENCE] поверх.
 * [plain] - итог характеристик, [values] - сводка без проб.
 */
internal class ReferenceHero(
    private val index: ContentIndex,
    private val heroClass: HeroClass,
    equipped: Collection<ItemInstance> = emptyList(),
    private val level: Int = SummaryProbe.LEVEL,
) {
    private val taken = listOf(TakenNode(heroClass.startNode)) + equipped.mapNotNull { it.socket }.map { TakenNode(it) }
    private val result = SheetCalculator(index).calculate(level, heroClass, index.tree.sourcedLines(taken) + attributes(), equipped, taken.mapTo(HashSet()) { it.code })
    private val operations = result.operations + SummaryProbe.REFERENCE.map { (stat, value) -> StatOperation(stat, Op.ADD, value) }

    /** Ид работающих вещей эталона. */
    val active: List<String> get() = result.active

    val plain: Map<String, Double> = SheetModel(result.base, operations, index).plain
    val values: Map<String, Double> by lazy { summary(emptyList()) }

    /** Лист эталона со строками [extra] сверх него. */
    fun sheet(extra: List<StatOperation> = emptyList()): ReferenceSheet {
        val model = SheetModel(result.base, operations + extra, index)
        val kit = Loadout.of(HeroSkills(), index, heroClass.code, emptyList(), model.plain, index.powers, index.rules.charges)
        return ReferenceSheet(model, CombatSheet(index, model, level, kit))
    }

    /** Сводка эталона со строками [extra] сверх него ([ReferenceSheet.values]). */
    fun summary(extra: List<StatOperation>): Map<String, Double> = sheet(extra).values

    /** Атрибуты эталона строкой «+N к атрибуту» контента. */
    private fun attributes(): List<SourcedLine> = SummaryProbe.ATTRIBUTES.map { (stat, value) ->
        val def = checkNotNull(index.definitions.firstOrNull { !it.local && it.effects == listOf(Effect(stat)) }) { "нет строки «+N» к $stat" }
        SourcedLine(Line(def.code, listOf(value)), StatSource(SourceKind.CLASS, heroClass.code))
    }
}

/** Лист эталона: модель листа [model] и лист боя [combat]; [values] - итог боя поверх итога характеристик (строки вне боя - радиус света, добыча - берут его). */
internal class ReferenceSheet(val model: SheetModel, val combat: CombatSheet) {
    val values: Map<String, Double> by lazy { model.plain + combat.values }
}
