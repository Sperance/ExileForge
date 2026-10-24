package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.display.preciseStats
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.EquipSlots
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.DamageGrowth
import com.sperance.exileforge.rules.sheet.MightReading
import com.sperance.exileforge.rules.sheet.Requirements
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SheetExplainer
import com.sperance.exileforge.rules.sheet.SheetFold
import com.sperance.exileforge.rules.sheet.SheetResult
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.WornCount
import kotlin.math.abs

/** Строка на листе на время - благо, проклятие, фляга, пассивное умение (одна с правилами, [com.sperance.exileforge.rules.sheet.StatLine]). */
typealias StatLine = com.sperance.exileforge.rules.sheet.StatLine

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
    /** Предначертание листа: его преобразования готового листа (рычаги и, правила строк) - и после строк боя. */
    val fate: FateEffects = FateEffects.NONE,
) {
    private val calculator = SheetCalculator(index)

    /** Свод листа правил: лист со строками боя складывает он - тот же, что у Мощи сервера ([com.sperance.exileforge.rules.sheet.HeroBody]). */
    val fold: SheetFold = SheetFold(base, ops, index, fate)

    val plain: Map<String, Double> get() = fold.plain

    /** Операции листа с их источниками: чьи строки легли на какую характеристику. */
    val operations: List<StatOperation> get() = ops

    /** The sheet taken apart by source, for a figure's own window; преобразования дара - его последним шагом. */
    val explainer: SheetExplainer by lazy { explainer(emptyList()) }

    /**
     * Разбивка листа с операциями [extra] среди его собственных: строки боя, что [with] складывает сводом ([folds]), со
     * своими источниками - итог разбивки равен [with] от них.
     */
    fun explainer(extra: List<StatOperation>): SheetExplainer = SheetExplainer(index, base, ops + extra, fate)

    /** Что преобразования дара сдвинули на листе со строками боя [lines], по характеристике: те же шаги, что в [explainer]. */
    fun reshapes(lines: List<StatLine>): Map<String, List<Shift>> = fate.shifts(calculator.compute(base, ops + lines.filter(::folds).map { StatOperation(it.stat, it.op, it.value) }), index)

    /** Строка боя ложится сводом листа; «больше» процент-стата [with] берёт на весь его множитель, поверх свода. */
    fun folds(line: StatLine): Boolean = fold.folds(line)

    fun with(lines: List<StatLine>): Map<String, Double> = fold.with(lines)

    /** The sheet's conditional lines: kept out of the sheet, laid on by a fight while they hold. */
    private val conditional: List<StatOperation> = ops.filter { it.condition != null }

    /** The hero's own conditional lines that hold under [active], as lines a fight lays on. */
    fun conditional(active: Set<Condition>): List<StatLine> = conditionalSourced(active).map { it.second }

    /** The same lines with the condition each waits for, for the log's card. */
    fun conditionalSourced(active: Set<Condition>): List<Pair<Condition, StatLine>> = conditional.filter { it.condition in active && it.condition?.target == false }.map { it.condition!! to StatLine(it.stat, it.op, it.value) }

    /** How much more damage the lines waiting for a target's state give against a target in [states], in percent increased. */
    fun against(states: Set<Condition>): Double = conditional.filter { it.condition in states && it.condition?.target == true }.sumOf { it.value }

    /**
     * Безусловные операции листа, разложенные по характеристике, как их складывает свод ([SheetFold.increased], [SheetFold.more]):
     * сумма увеличений и произведение «больше» каждой - один раз на лист, строки боя ложатся на них в своём порядке.
     */
    private val baseIncreased: Map<String, Double> by lazy { unconditional(Op.INCREASED).mapValues { (_, found) -> found.sumOf { op -> op.resolve(op.perStat?.let { plain[it] } ?: 0.0) } } }
    private val baseMore: Map<String, Double> by lazy { unconditional(Op.MORE).mapValues { (_, found) -> found.fold(1.0) { product, op -> product * (1 + op.resolve(op.perStat?.let { plain[it] } ?: 0.0) / 100) } } }

    private fun unconditional(op: Op): Map<String, List<StatOperation>> = ops.filter { it.condition == null }.flatMap(calculator::spread).filter { it.op == op }.groupBy { it.stat }

    /** Безусловные операции [op] листа, каждая - своими разложенными долями: рост хоть одного из видов берёт операцию один раз. */
    private val increasedParts: List<List<StatOperation>> by lazy { spreadParts(Op.INCREASED) }
    private val moreParts: List<List<StatOperation>> by lazy { spreadParts(Op.MORE) }

    private fun spreadParts(op: Op): List<List<StatOperation>> = ops.filter { it.condition == null && it.op == op }.map(calculator::spread)

    /** Значения операций [parts] листа и строк [lines] вида [op], что растят хоть один из видов [types]: по операции - одно, наибольшей доли. */
    private fun touching(parts: List<List<StatOperation>>, op: Op, lines: List<StatLine>, types: Set<String>): List<Double> = parts.mapNotNull { spread -> spread.filter { it.stat in types }.maxOfOrNull { it.resolve(it.perStat?.let { stat -> plain[stat] } ?: 0.0) } } +
        lines.filter { line -> line.op == op && types.any { StatSpread.touches(line.stat, line.op, it) } }.map { it.value }

    /**
     * Рост урона умений по виду со строками [lines]: увеличения (урон вообще - увеличение каждого вида) и «больше» вида - на них растут чары и урон во времени.
     * Тот же итог, что у свода правил ([SheetFold.growth]): операции листа сложены заранее, строки боя - поверх, в порядке свода.
     */
    fun growth(lines: List<StatLine> = emptyList()): DamageGrowth = object : DamageGrowth {
        override fun increased(type: String): Double {
            var sum = baseIncreased[type] ?: 0.0
            for (line in lines) if (line.op == Op.INCREASED && StatSpread.touches(line.stat, line.op, type)) sum += line.value
            return sum
        }

        override fun more(type: String): Double {
            var product = baseMore[type] ?: 1.0
            for (line in lines) if (line.op == Op.MORE && StatSpread.touches(line.stat, line.op, type)) product *= 1 + line.value / 100
            return product
        }

        override fun increasedAny(types: Set<String>): Double = touching(increasedParts, Op.INCREASED, lines, types).sum()

        override fun moreAny(types: Set<String>): Double = touching(moreParts, Op.MORE, lines, types).fold(1.0) { product, value -> product * (1 + value / 100) }
    }

    override fun equals(other: Any?): Boolean = other is SheetModel && other.plain == plain
    override fun hashCode(): Int = plain.hashCode()
}

/**
 * Что даст надеть вещь: сдвинутые цифры сводки [delta], Мощь героя с вещью [might] (null - листа боя нет) и итоги листа героя
 * с ней [stats] - по ним «Теряешь» ([com.sperance.exileforge.core.display.LostLines]) видит, что ушло в ноль.
 */
data class WearOutcome(val delta: List<StatDelta>, val might: MightReading?, val stats: Map<String, Double> = emptyMap()) {
    companion object {
        val NONE = WearOutcome(emptyList(), null)
    }
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
    /**
     * Наименьший сдвиг, что сравнение «было → стало» показывает (лист точный, округляет показ): у характеристик с сотыми
     * ([preciseStats]) - сотая, у прочих - половина десятой. Константы показа, не баланс.
     */
    private const val PRECISE_STEP = 0.005
    private const val SHOWN_STEP = 0.05

    /** Предначертание аккаунта и законы тронов кладут строки и преобразования листа тем же правилом, что у сервера. */
    fun calculate(
        index: ContentIndex,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        laws: List<String> = emptyList(),
        fate: String? = null,
    ): HeroSheet {
        val result = SheetCalculator(index).hero(level, index.heroClass(heroClass), tree, items.filter { it.equipped }, laws, fate)
        return HeroSheet(result.stats, result.active, result.inactive.associate { it.id to it.reasons }, SheetModel(result.base, result.operations, index, index.fates.effects(fate, result.stats[CoreStat.FATE_EFFECT.code] ?: 0.0, index.fates.worn(index, fate, items.filter { it.equipped }))))
    }

    /**
     * Чего копии [item] не хватает у героя уровня [level] с листом [stats], словами правил: уровень копии ([Requirements.level])
     * и атрибуты шаблона; пусто - можно надеть.
     */
    fun unmet(index: ContentIndex, item: ItemInstance, level: Int, stats: Map<String, Double>): List<String> = index.template(item.template)?.let { Requirements.unmet(item, it, level, stats) }.orEmpty()

    /**
     * What wearing [item] would change, by the rules' own placement: a ring takes a free one of two, a
     * two-handed weapon frees both hands, a bow pairs with a quiver. Only the characteristics that move are returned.
     * [place] - место из нескольких (второе кольцо, третья фляга), куда вещь надевают; null - решают правила.
     * Цифры сводки - лист боя ([figures]) с умениями героя [skills]; [before] - цифры сводки героя сейчас.
     * Вместе со сдвигами - Мощь героя с вещью ([WearOutcome.might]) из того же листа боя, без второго подсчёта.
     */
    fun wearing(
        index: ContentIndex,
        item: ItemInstance,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        before: Map<String, Double>,
        place: Slot? = null,
        laws: List<String> = emptyList(),
        fate: String? = null,
        skills: HeroSkills = HeroSkills(),
    ): WearOutcome {
        val after = worn(index, item, items, place) ?: return WearOutcome.NONE
        return outcome(index, level, heroClass, tree, after, before, laws, fate, skills)
    }

    /**
     * Что даст самоцвет [jewel] в гнезде [socket] дерева: прежний самоцвет гнезда уходит в тайник, как у сервера; лист с ним -
     * тем же расчётом правил, что лист героя (гнездо не взято или вторая копия уникального - самоцвет не работает и там).
     * Сдвиги сводки против [before] и Мощь героя с заменой ([WearOutcome.might]).
     */
    fun socketing(
        index: ContentIndex,
        jewel: ItemInstance,
        socket: String,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        before: Map<String, Double>,
        laws: List<String> = emptyList(),
        fate: String? = null,
        skills: HeroSkills = HeroSkills(),
    ): WearOutcome {
        val after = items.map {
            when {
                it.id == jewel.id -> it.copy(slot = Slot.JEWEL, socket = socket)
                it.socket == socket -> it.copy(slot = null, socket = null)
                else -> it
            }
        }
        return outcome(index, level, heroClass, tree, after, before, laws, fate, skills)
    }

    /** Итог листа героя с вещами [after]: сдвиги цифр сводки против [before], Мощь и итоги листа - общий шаг [wearing] и [socketing]. */
    private fun outcome(
        index: ContentIndex,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        after: List<ItemInstance>,
        before: Map<String, Double>,
        laws: List<String>,
        fate: String?,
        skills: HeroSkills,
    ): WearOutcome {
        val sheet = calculate(index, level, heroClass, tree, after, laws, fate)
        val combat = CombatSheet.of(index, sheet, level, heroClass, skills, fate, after.filter { it.equipped })
        return WearOutcome(changes(index, before, combat?.values ?: sheet.stats), combat?.might, sheet.stats)
    }

    /**
     * Что изменит в листе взятие узла дерева [node] (с выбранным вариантом у узла с выбором) - тем же расчётом, что сервер,
     * без запроса: лист с деревом героя и этим узлом против [before]. Только сдвинутые характеристики, как у [wearing].
     */
    fun allocating(
        index: ContentIndex,
        node: TakenNode,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        before: Map<String, Double>,
        laws: List<String> = emptyList(),
        fate: String? = null,
        skills: HeroSkills = HeroSkills(),
    ): List<StatDelta> = changes(index, before, figures(index, calculate(index, level, heroClass, tree + node, items, laws, fate), level, heroClass, skills, fate, items))

    /**
     * Цифры сводки героя с листом [sheet]: лист боя ([CombatSheet]) с умениями [skills] и Предначертанием [fate] надетого
     * из [items] - одно правило сводки, сравнения вещи и узла дерева; без модели листа - сам лист.
     */
    fun figures(index: ContentIndex, sheet: HeroSheet, level: Int, heroClass: String, skills: HeroSkills, fate: String?, items: List<ItemInstance>): Map<String, Double> = CombatSheet.of(index, sheet, level, heroClass, skills, fate, items.filter { it.equipped })?.values ?: sheet.stats

    /**
     * Сдвинутые характеристики листа [next] против [before] в порядке подсчёта. Счётчики надетого (пустые места,
     * уникалки…) - чтение листа силами, а не число для сравнения.
     */
    private fun changes(index: ContentIndex, before: Map<String, Double>, next: Map<String, Double>): List<StatDelta> = (before.keys + next.keys).filterNot { it in WornCount.STATS }.sortedBy { index.stats.order(it) }
        .map { StatDelta(it, before[it] ?: 0.0, next[it] ?: 0.0) }
        .filter { abs(it.change) >= if (it.stat in preciseStats) PRECISE_STEP else SHOWN_STEP }

    /**
     * Лучше или хуже: урон и защита героя с [item] против листа [before] - на место [place] или, без него,
     * на то, что выберут правила. Урон - натиск Мощи: нынешняя Мощь [might] против листа боя с вещью и умениями
     * [skills]. Null, когда шаблона вещи нет в контенте или листа боя нет.
     */
    fun verdict(
        index: ContentIndex,
        item: ItemInstance,
        level: Int,
        heroClass: String,
        tree: List<TakenNode>,
        items: List<ItemInstance>,
        before: Map<String, Double>,
        might: MightReading,
        place: Slot? = null,
        laws: List<String> = emptyList(),
        fate: String? = null,
        skills: HeroSkills = HeroSkills(),
    ): SheetVerdict? {
        val after = worn(index, item, items, place) ?: return null
        val sheet = calculate(index, level, heroClass, tree, after, laws, fate)
        val combat = CombatSheet.of(index, sheet, level, heroClass, skills, fate, after.filter { it.equipped }) ?: return null
        return SheetVerdict.of(index, level, before, sheet.stats, might, combat.might)
    }

    /**
     * The hero's items with [item] put on as the server would place it — a ring on a free one of two (или на место [place]),
     * a two-handed weapon freeing both hands, a bow pairing with a quiver; null when the content does not know the item.
     *
     * Вещь, что ещё не у героя (товар, лот), взвешивается как лежащая в тайнике.
     */
    fun worn(index: ContentIndex, item: ItemInstance, items: List<ItemInstance>, place: Slot?): List<ItemInstance>? {
        val fit = fit(index, item, items, place) ?: return null
        return (if (items.none { it.id == item.id }) items + item else items).map {
            when {
                it.id == item.id -> it.copy(slot = fit.target, socket = null)
                !it.socketed && it.slot in fit.freed -> it.copy(slot = null)
                else -> it
            }
        }
    }

    /**
     * Что снимет надевание [item] (на место [place] у кольца и фляги) из надетого [items]: вещь на её месте и всё, что
     * освобождает [EquipSlots.displaced], - двуручное снимает оружие и щит, лук снимает щит; пусто - места свободны или
     * контент вещи не знает. Одна мера для листа «если надеть», места сравнения и «Теряешь».
     */
    fun takenOff(index: ContentIndex, item: ItemInstance, items: List<ItemInstance>, place: Slot?): List<ItemInstance> {
        val fit = fit(index, item, items, place) ?: return emptyList()
        return items.filter { it.equipped && !it.socketed && it.id != item.id && it.slot in fit.freed }
    }

    /** Место вещи [item] и слоты, что она освобождает (вместе с её собственным), - как надел бы сервер. */
    private fun fit(index: ContentIndex, item: ItemInstance, items: List<ItemInstance>, place: Slot?): Fit? {
        val template = index.template(item.template) ?: return null
        val worn = items.filter { it.equipped && !it.socketed && it.id != item.id }
        val target = EquipSlots.target(template.slot, place, worn.mapNotNull { it.slot })
        val wornWeapon = worn.firstOrNull { it.slot == Slot.WEAPON_1H }?.let { index.template(it.template)?.weaponType }
        return Fit(target, EquipSlots.displaced(template.slot, template.weaponType, wornWeapon) + target)
    }

    private class Fit(val target: Slot, val freed: Set<Slot>)
}
