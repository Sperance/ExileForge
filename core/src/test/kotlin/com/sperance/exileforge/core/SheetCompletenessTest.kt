package com.sperance.exileforge.core

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatSource
import com.sperance.exileforge.rules.text.LocaleKey
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
