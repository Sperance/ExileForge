package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.campaign.FightReading
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.StatRegistry
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatPart

/**
 * Строка раздела «С условием» сводки героя: вклад [value] операций [op] над характеристикой [stat], что лист правил держит вне
 * свода, а бой кладёт, пока держится [condition] ([com.sperance.exileforge.core.character.SheetModel.conditional]). [sources] -
 * операции листа, из которых она сложена: разбор по источникам.
 */
data class ConditionalStat(val condition: Condition, val stat: String, val op: Op, val value: Double, val sources: List<StatOperation>) {
    /** Ключ строки в сводке и в разборе. */
    val key: String get() = "${condition.name}|$stat|${op.name}"

    /** Часть строки по ключу правил: так [StatPart.of] складывает значения одной группы. */
    val part: StatPart.Key get() = StatPart.Key(stat, op, condition)
}

/**
 * Условные строки листа героя - одна точка сводки и теста полноты: ни одна строка с условием (`when`) не выпадает из сводки,
 * хотя в итог своей характеристики она не входит.
 */
object ConditionalStats {
    /**
     * Условные строки операций [operations]: условие, характеристика и операция - одна строка; нулевой вклад строки не даёт.
     * Порядок - условия правил ([Condition]: сначала героя, потом цели), внутри - раздел сводки и порядок реестра [registry].
     * Конверсия «за каждые N» читает лист [sheet].
     */
    fun of(operations: Collection<StatOperation>, registry: StatRegistry, sheet: (String) -> Double = { 0.0 }): List<ConditionalStat> = operations
        .filter { it.condition != null }
        .groupBy { StatPart.Key(it.stat, it.op, it.condition) }
        .mapNotNull { (key, ops) ->
            StatPart.of(key, ops.map { it.resolve(it.perStat?.let(sheet) ?: 0.0) })?.let { ConditionalStat(key.condition!!, key.stat, key.op, it.value, ops) }
        }
        .sortedWith(compareBy({ it.condition.ordinal }, { section(it, registry)?.ordinal ?: Int.MAX_VALUE }, { registry.order(it.stat) }, { it.op.ordinal }))

    /** Условные строки листа героя [hero]; листа ещё нет - пусто. */
    fun of(hero: HeroView, index: ContentIndex): List<ConditionalStat> = hero.sheet.model?.operations?.let { ops -> of(ops, index.stats) { hero.stats[it] ?: 0.0 } }.orEmpty()

    /** Раздел сводки строки [row] ([placed]). */
    fun section(row: ConditionalStat, registry: StatRegistry): StatSection? = placed(row.stat, row.op, registry)

    /**
     * Где сводка показывает операцию [op] над [stat]: свой раздел (строка или та, частью которой она читается), раздел первого члена,
     * на который она раскладывается ([StatSpread]), или строки, к которой её прибавляет бой ([FightReading]: «все сопротивления
     * стихиям» - в сопротивлениях); null - места в сводке нет.
     */
    fun placed(stat: String, op: Op, registry: StatRegistry): StatSection? = registry.section(stat)
        ?: StatSpread.stats(stat, op).firstNotNullOfOrNull { registry.section(it) }
        ?: FightReading.entries.firstOrNull { it.from == stat }?.let { registry.section(it.stat) }
}
