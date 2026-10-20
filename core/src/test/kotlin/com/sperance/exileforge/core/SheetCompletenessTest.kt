package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.campaign.FightReading
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.StatUnit
import com.sperance.exileforge.rules.content.TakenNode
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
     * Строка героя без условия с любого носителя двигает итог своей строки сводки на эталонном герое ([REFERENCE]): цифра её
     * характеристики, строки, частью которой она объявлена, или того, что бой читает вместе с ней ([FightReading]). Так ловится
     * «увеличение без базы» - «увеличение» или «больше» характеристики, которой у героя нет, ничего не меняет и в сводке не видно.
     */
    @Test
    fun every_hero_line_moves_its_summary_row() {
        val heroClass = index.classes.classes.first()
        val hero = SheetCalculator(index).hero(LEVEL, heroClass, listOf(TakenNode(heroClass.startNode)), emptyList())
        val base = hero.operations + REFERENCE.map { (stat, value) -> StatOperation(stat, Op.ADD, value) }
        fun summary(extra: List<StatOperation>): Map<String, Double> {
            val model = SheetModel(hero.base, base + extra, index)
            val kit = Loadout.of(HeroSkills(), index, heroClass.code, emptyList(), model.plain, index.powers, index.rules.charges)
            return CombatSheet(index, model, LEVEL, kit).values
        }
        val before = summary(emptyList())
        val rarityLines = index.campaign.rarities.flatMapTo(HashSet()) { rule -> rule.lines.map { it.code } }
        val lines = heroDefs.filter { it.code !in rarityLines }.flatMap { def ->
            def.effects.filter { it.condition == null && registry[it.stat]?.group == StatGroup.HERO }.map { def to it }
        }
        assertTrue(lines.size >= 300, "строк героя мало: тест ничего не доказал (${lines.size})")
        val still = lines.filter { (_, effect) ->
            val targets = StatSpread.targets(effect.stat, effect.op)
            val rows = targets + targets.map(registry::row) + FightReading.entries.filter { it.from in targets }.map { it.stat }
            probes(effect.stat, effect.op).none { value ->
                val after = summary(listOf(StatOperation(effect.stat, effect.op, value, effect.perStat, effect.perAmount)))
                rows.any { abs((after[it] ?: 0.0) - (before[it] ?: 0.0)) > MOVED }
            }
        }.map { (def, effect) -> "${def.code.value}:${effect.stat}:${effect.op}" }
        assertEquals(emptyList(), still, "строки героя, что не двигают свою строку сводки")
    }

    /** Числа пробы: флаг - единица, замена (`SET`) - заметная величина и запрет, остальное - заметная величина. */
    private fun probes(stat: String, op: Op): List<Double> = when {
        registry[stat]?.unit == StatUnit.FLAG -> listOf(1.0)
        op == Op.SET -> listOf(PROBE, 0.0)
        else -> listOf(PROBE)
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

    private companion object {
        const val LEVEL = 60

        /** Число пробы строки. */
        const val PROBE = 30.0

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
            "STOCK_HEALTH_REGEN" to 10.0, "STOCK_MANA_REGEN_FLAT" to 8.0, "STOCK_STRENGTH" to 150.0, "STOCK_AGILITY" to 150.0, "STOCK_INTELLECT" to 150.0,
            "STOCK_LEECH_PHYSICAL" to 2.0, "STOCK_STUN_THRESHOLD" to 100.0, "STOCK_WORN_UNIQUES" to 1.0, "STOCK_WORN_CORRUPTED" to 1.0, "STOCK_WORN_JEWELS" to 1.0,
        )
    }
}
