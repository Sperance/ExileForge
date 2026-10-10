package com.sperance.exileforge.core.character

import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EquipSlots
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.Requirements
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SheetExplainer
import com.sperance.exileforge.rules.sheet.SheetResult
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.WornCount
import kotlin.math.abs

/** A line laid on a sheet for a while — a buff, a curse, a flask, a passive skill — shaped as a modifier's effect. */
data class StatLine(val stat: String, val op: Op, val value: Double)

/** What wearing an item changes: the characteristic, what it is now and what it would be. */
data class StatDelta(val stat: String, val before: Double, val after: Double) {
    val change: Double get() = after - before
}

/**
 * What a sheet was added up from: the class's base and every operation in the registry's order, so a
 * fight can lay [StatLine]s among them and add the sheet up again as the rules would — an increase joins
 * the increases of its stat instead of multiplying the total. A percent stat takes MORE on its whole multiplier.
 */
class SheetModel(
    private val base: Map<String, Double>,
    private val ops: List<StatOperation>,
    private val index: ContentIndex,
    /** Предначертание листа (4.6.1): его преобразования готового листа (рычаги и, 4.6.2, правила строк) - и после строк боя. */
    private val fate: FateEffects = FateEffects.NONE,
) {
    private val calculator = SheetCalculator(index)
    val plain: Map<String, Double> by lazy { shaped(calculator.compute(base, ops)) }

    /** Лист после преобразований дара (4.6.1, «Равновесие»; 4.6.2 - и судьбоносных строк) одним резолвером правил: без них - тот же. */
    private fun shaped(stats: Map<String, Double>): Map<String, Double> = fate.reshape(SheetResult(stats, emptyList(), emptyList(), base, ops), index).stats

    /** The sheet taken apart by source, for a figure's own window. */
    val explainer: SheetExplainer by lazy { SheetExplainer(index, base, ops) }

    fun with(lines: List<StatLine>): Map<String, Double> {
        if (lines.isEmpty()) return plain
        val (scaling, folding) = lines.partition { it.op == Op.MORE && index.stats.isPercent(it.stat) }
        val stats = calculator.compute(base, ops + folding.map { StatOperation(it.stat, it.op, it.value) }).toMutableMap()
        scaling.forEach { line -> stats[line.stat] = (100 + (stats[line.stat] ?: 0.0)) * (1 + line.value / 100) - 100 }
        return shaped(stats)
    }

    /** The sheet's conditional lines (3.35.0, server 1.34.0): kept out of the sheet, laid on by a fight while they hold. */
    private val conditional: List<StatOperation> = ops.filter { it.condition != null }

    /** The hero's own conditional lines that hold under [active], as lines a fight lays on. */
    fun conditional(active: Set<Condition>): List<StatLine> = conditionalSourced(active).map { it.second }

    /** The same lines with the condition each waits for (3.37.0), for the log's card. */
    fun conditionalSourced(active: Set<Condition>): List<Pair<Condition, StatLine>> = conditional.filter { it.condition in active && it.condition?.target == false }.map { it.condition!! to StatLine(it.stat, it.op, it.value) }

    /** How much more damage the lines waiting for a target's state give against a target in [states], in percent increased. */
    fun against(states: Set<Condition>): Double = conditional.filter { it.condition in states && it.condition?.target == true }.sumOf { it.value }

    /**
     * The increases of [stat] summed, [lines] among them: what a spell of that element is multiplied by. The damage in
     * general (server 1.57.0) is an increase of every type of damage, as the sheet adds it up.
     */
    fun increased(stat: String, lines: List<StatLine> = emptyList()): Double = (ops.filter { it.condition == null } + lines.map { StatOperation(it.stat, it.op, it.value) }).flatMap(calculator::spread)
        .filter { it.stat == stat && it.op == Op.INCREASED }.sumOf { op -> op.resolve(op.perStat?.let { plain[it] } ?: 0.0) }

    override fun equals(other: Any?): Boolean = other is SheetModel && other.plain == plain
    override fun hashCode(): Int = plain.hashCode()
}

/** The hero sheet as the client added it up: the numbers, which worn items count and why the rest do not, and what it was made of. */
class HeroSheet(val stats: Map<String, Double>, val active: List<String>, val inactive: Map<String, List<String>>, val model: SheetModel?) {
    companion object {
        val EMPTY = HeroSheet(emptyMap(), emptyList(), emptyMap(), null)
    }
}

/**
 * The hero sheet on the client — the rules' own calculator, so it cannot disagree with the server:
 * the class at the level, the tree, then the worn items in slot order, each checked against what came before.
 */
object Sheets {
    fun calculate(
        index: ContentIndex,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        pets: List<com.sperance.exileforge.rules.content.Pet> = emptyList(),
        laws: List<String> = emptyList(),
        /** Предначертание аккаунта (4.6.0): его строки листа и преобразования листа (4.6.1), как у сервера. */
        fate: String? = null,
    ): HeroSheet {
        // A helper pet's lines lie on the hero beside the tree's (3.5.0), as the server adds them.
        // Законы тронов героя - строки листа тем же правилом, что у сервера.
        val helpers = com.sperance.exileforge.rules.roll.Menagerie(index).helperSourced(pets)
        val result = SheetCalculator(index).hero(level, index.heroClass(heroClass), tree, helpers, items.filter { it.equipped }, laws, fate)
        return HeroSheet(result.stats, result.active, result.inactive.associate { it.id to it.reasons }, SheetModel(result.base, result.operations, index, index.fates.effects(fate, boons = index.fates.worn(index, fate, items.filter { it.equipped }))))
    }

    /** What an item's requirements miss for a hero with [stats], in the rules' words; empty means it can be worn. */
    fun unmet(index: ContentIndex, templateCode: String, level: Int, stats: Map<String, Double>): List<String> = index.template(templateCode)?.let { Requirements.unmet(it, level, stats) }.orEmpty()

    /**
     * What wearing [item] would change, by the rules' own placement: a ring takes a free one of two, a
     * two-handed weapon frees both hands, a bow pairs with a quiver. Only the characteristics that move are returned.
     * [place] (3.90.3) - место из нескольких (второе кольцо, третья фляга), куда вещь надевают; null - решают правила.
     */
    fun wearing(
        index: ContentIndex,
        item: ItemInstance,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        before: Map<String, Double>,
        pets: List<com.sperance.exileforge.rules.content.Pet> = emptyList(),
        place: Slot? = null,
        laws: List<String> = emptyList(),
        fate: String? = null,
    ): List<StatDelta> {
        val next = worn(index, item, level, heroClass, tree, items, pets, place, laws, fate) ?: return emptyList()
        // The counts of what is worn (empty slots, uniques…) are the powers' reading of the sheet, not a figure to compare.
        return (before.keys + next.keys).filterNot { it in WornCount.STATS }.sortedBy { index.stats.order(it) }
            .map { StatDelta(it, before[it] ?: 0.0, next[it] ?: 0.0) }
            .filter { abs(it.change) >= 0.05 }
    }

    /**
     * Лучше или хуже (3.89.0): урон и защита героя с [item] против листа [before] - на место [place] (3.90.3) или, без него,
     * на то, что выберут правила. Null, когда шаблона вещи нет в контенте.
     */
    fun verdict(
        index: ContentIndex,
        item: ItemInstance,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        before: Map<String, Double>,
        pets: List<com.sperance.exileforge.rules.content.Pet> = emptyList(),
        place: Slot? = null,
        laws: List<String> = emptyList(),
        fate: String? = null,
    ): SheetVerdict? = worn(index, item, level, heroClass, tree, items, pets, place, laws, fate)?.let { SheetVerdict.of(index, level, before, it) }

    /**
     * The whole sheet with [item] put on as the server would place it — a ring on a free one of two (или на место [place]),
     * a two-handed weapon freeing both hands, a bow pairing with a quiver; null when the content does not know the item.
     */
    private fun worn(
        index: ContentIndex,
        item: ItemInstance,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        pets: List<com.sperance.exileforge.rules.content.Pet>,
        place: Slot?,
        laws: List<String>,
        fate: String?,
    ): Map<String, Double>? {
        val template = index.template(item.template) ?: return null
        val worn = items.filter { it.equipped && !it.socketed && it.id != item.id }
        val target = EquipSlots.target(template.slot, place, worn.mapNotNull { it.slot })
        val wornWeapon = worn.firstOrNull { it.slot == Slot.WEAPON_1H }?.let { index.template(it.template)?.weaponType }
        val freed = EquipSlots.displaced(template.slot, template.weaponType, wornWeapon) + target
        // An item not yet the hero's — a merchant's offer, a lot — is weighed as if it were in the stash.
        val after = (if (items.none { it.id == item.id }) items + item else items).map {
            when {
                it.id == item.id -> it.copy(slot = target, socket = null)
                !it.socketed && it.slot in freed -> it.copy(slot = null)
                else -> it
            }
        }
        return calculate(index, level, heroClass, tree, after, pets, laws, fate).stats
    }
}
