package com.sperance.exileforge.core

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.content.StatUnit
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatSource
import com.sperance.exileforge.rules.text.LocaleKey
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Полнота сводки героя: ни одно свойство модификатора, что ложится на героя (вещь, уникалка, дерево, закон, дар), не выпадает из
 * сводки - строка без условия встаёт в свой раздел ([ConditionalStats.placed]), строка с условием (`when`) - в
 * раздел «С условием» ([ConditionalStats]) со своим условием и вкладом. У каждого условия правил - текст условия на сервере и пояснение
 * разбора с числами правил на всех языках.
 */
class SheetCompletenessTest {
    private val index = TestContent.index
    private val registry = index.stats

    /** Описания модификаторов героя: без строк монстров и скрытой метки (её раскрывает сфера, в сводке её нет). */
    private val heroDefs: List<ModifierDef> by lazy {
        index.families.keys.mapNotNull { index.modifier(ModifierCode(it)) }.filter { !it.monster && !it.veiled }
    }

    @Test
    fun every_hero_line_has_a_place_in_the_summary() {
        val lost = heroDefs.flatMap { def ->
            def.effects.filter { it.condition == null && registry[it.stat]?.group == StatGroup.HERO }
                .filter { effect -> ConditionalStats.placed(effect.stat, effect.op, registry) == null }
                .map { "${def.code.value}:${it.stat}" }
        }
        assertEquals(emptyList(), lost, "строки героя без места в сводке")
    }

    /**
     * Строка героя без условия с любого носителя двигает итог своей строки сводки на эталонном герое ([ReferenceHero]) - строка класса
     * на эталоне своего класса: цифра её характеристики, строки, частью которой она объявлена, или того, что бой читает вместе с ней
     * ([SummaryProbe.rows]). Так ловится «увеличение без базы» - «увеличение» или «больше» характеристики, которой у героя нет, ничего не
     * меняет и в сводке не видно. Путь строки через надетую вещь проверяет [EquippedModifiersTest].
     */
    @Test
    fun every_hero_line_moves_its_summary_row() {
        val classes = index.classes.classes.mapNotNull { index.heroClass(it.code) }
        val references = classes.associate { it.code to ReferenceHero(index, it) }
        val owners = classes.flatMap { heroClass -> heroClass.lines.map { it.code to heroClass.code } }.toMap()
        val rarityLines = index.campaign.rarities.flatMapTo(HashSet()) { rule -> rule.lines.map { it.code } }
        val heroLines = heroDefs.flatMap { def -> def.effects.filter { it.condition == null && registry[it.stat]?.group == StatGroup.HERO }.map { def to it } }
        val lines = heroLines.filter { (def) -> def.code !in rarityLines }
        assertTrue(heroLines.isNotEmpty() && lines.size >= heroLines.size * SummaryProbe.MIN_SHARE, "проверено мало строк героя: тест ничего не доказал (${lines.size} из ${heroLines.size})")
        val still = lines.filter { (def, effect) ->
            val reference = references.getValue(owners[def.code] ?: classes.first().code)
            val rows = SummaryProbe.rows(effect.stat, effect.op, registry)
            probes(effect.stat, effect.op, reference.plain[effect.stat] ?: 0.0).none { value ->
                val after = reference.summary(listOf(StatOperation(effect.stat, effect.op, value, effect.perStat, effect.perAmount)))
                rows.any { abs((after[it] ?: 0.0) - (reference.values[it] ?: 0.0)) > SummaryProbe.MOVED }
            }
        }.map { (def, effect) -> "${def.code.value}:${effect.stat}:${effect.op}" }
        assertEquals(emptyList(), still, "строки героя, что не двигают свою строку сводки")
    }

    /** Числа пробы в обе стороны: флаг - единица, замена (`SET`) - база эталона [base] ± заметная величина, остальное - ± заметная величина. */
    private fun probes(stat: String, op: Op, base: Double): List<Double> = when {
        registry[stat]?.unit == StatUnit.FLAG -> listOf(1.0)
        op == Op.SET -> listOf(base + SummaryProbe.PROBE, base - SummaryProbe.PROBE)
        else -> listOf(SummaryProbe.PROBE, -SummaryProbe.PROBE)
    }

    @Test
    fun every_conditional_line_shows_in_its_section() {
        val calc = SheetCalculator(index)
        val conditional = heroDefs.filter { def -> def.effects.any { it.condition != null } }
        assertTrue(conditional.size >= Condition.entries.size, "условных строк в контенте меньше, чем условий: тест ничего не доказал")
        val lost = conditional.flatMap { def ->
            val source = StatSource(SourceKind.ITEM, def.code.value)
            val ops = calc.expand(listOf(Line(def.code, List(def.valueCount) { 10.0 })), source)
            val rows = ConditionalStats.of(ops, registry)
            def.effects.filter { it.condition != null }.filter { effect ->
                rows.none { it.condition == effect.condition && it.stat == effect.stat && it.op == effect.op && it.value != 0.0 && it.sources.all { op -> op.source == source } }
            }.map { "${def.code.value}:${it.stat}:${it.condition}" }
        }
        assertEquals(emptyList(), lost, "условные строки, что сводка не показывает")
        val covered = conditional.flatMap { def -> def.effects.mapNotNull { it.condition } }.toSet()
        assertEquals(Condition.entries.toSet(), covered, "условия правил без строки в контенте - сводку их строк тест не проверил")
    }

    @Test
    fun conditional_rows_follow_the_rules_order() {
        val ops = heroDefs.filter { def -> def.effects.any { it.condition != null } }.flatMap { def ->
            SheetCalculator(index).expand(listOf(Line(def.code, List(def.valueCount) { 10.0 })))
        }
        val rows = ConditionalStats.of(ops.shuffled(kotlin.random.Random(7)), registry)
        assertEquals(rows.sortedBy { it.condition.ordinal }.map { it.condition }, rows.map { it.condition })
        assertTrue(rows.all { ConditionalStats.section(it, registry) != null }, "условная строка без раздела для порядка")
    }

    @Test
    fun every_condition_is_named_and_explained_with_numbers() {
        Condition.entries.forEach { condition ->
            TestContent.serverLocales().forEach { (code, table) -> assertTrue(!table[LocaleKey.condition(condition)].isNullOrBlank(), "$code: нет ${LocaleKey.condition(condition)}") }
            Lang.entries.forEach { lang ->
                val text = UiStrings.table(lang)["stat.condition.${condition.name}"]
                assertTrue(!text.isNullOrBlank(), "${lang.code}: нет пояснения условия $condition")
                assertTrue(index.textNumbers.text(text) != null, "${lang.code}: подстановка без числа в пояснении $condition")
            }
        }
    }
}
