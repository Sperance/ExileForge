package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.FightReading
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.core.sheet.StatDetail
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Effect
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.Source
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.VariantKind
import com.sperance.exileforge.rules.roll.AffixRoller
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.rules.roll.Veils
import com.sperance.exileforge.rules.sheet.Amplify
import com.sperance.exileforge.rules.sheet.FlaskSheet
import com.sperance.exileforge.rules.sheet.LineScope
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatSource
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.text.ModifierText
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Сторож пути «модификатор выпал на вещь → вещь надета → сводка героя». Каждое описание, что любым путём ложится на вещь героя
 * ([ItemLines]), встаёт высшим тиром на подходящую базу, и вещь надевается эталону ([ReferenceHero]) листом правил - со сводом
 * локальных строк в базу вещи, усилителями своей вещи, подъёмами уникалок и силами. Эталон с вещью сравнивается с эталоном с той же
 * вещью без строки:
 * - строка героя без условия и сила с правилами листа двигают видимую строку сводки ([groupedStats]) - свою или ту, что читает бой
 *   ([SummaryProbe.rows]); усилитель своей вещи - строку той строки вещи, что он усиливает;
 * - строка с условием встаёт в раздел «С условием» ([ConditionalStats]) с вещью в источниках;
 * - разбор сдвинутой строки называет вещь (у силы - вещь, что её держит) и сходится с цифрой сводки;
 * - у сдвинутой строки - описание и подробное описание на всех языках клиента.
 * Текст строки на карточке вещи - свой (не общий шаблон), на всех языках сервера и клиента, без подстановок.
 *
 * Сводке по правилам не принадлежат, и сводкой сторож их не проверяет (только текстом карточки): строки инструментов - работа
 * ремесла, и фляг - глоток, лист героя их не берёт ([SheetCalculator.calculate]); силы без правил листа - боя, добычи, ремёсел и
 * слотов - действуют в бою и вне листа, их место - карточка вещи; свойства самой вещи, что не усиливают её строк (места аффиксов
 * верстака), - тоже карточка. Строки редкостей ложатся только на монстров, судьбоносная строка
 * Осквернения - не модификатор: её строки дара сводит разбор Предначертания (`SheetExplainTest`).
 */
class EquippedModifiersTest {
    private val index = TestContent.index
    private val registry = index.stats
    private val items = ItemLines(index)
    private val classes = index.classes.classes.mapNotNull { index.heroClass(it.code) }

    /** Уровень эталона: на нём требования уровня держит любая вещь контента. */
    private val level = index.templates.values.maxOf { it.requiredLevel }

    /** Гнездо дерева для самоцвета. */
    private val socket = index.tree.byCode.values.first { it.type == SkillNodeType.JEWEL_SOCKET }.code

    private val item = StatSource(SourceKind.ITEM, PROBE_ID)

    @Test
    fun every_worn_line_shows_in_the_hero_summary() {
        val lines = items.worn.filter { line -> line.def.effects.any { shown(it) != null } }
        val probes = lines.groupBy { it.template }.flatMap { (_, group) ->
            val heroes = HashMap<ItemInstance, ReferenceHero>()
            group.mapNotNull { probe(it, heroes) }
        }
        assertTrue(lines.isNotEmpty() && probes.size >= lines.size * SummaryProbe.MIN_SHARE, "проверено мало строк вещей: тест ничего не доказал (${probes.size} из ${lines.size})")
        assertEquals(emptyList(), probes.flatten(), "строки надетой вещи, что сводка героя не показывает верно")
    }

    @Test
    fun every_item_line_has_its_own_card_text_in_every_language() {
        val tables = TestContent.serverLocales()
        assertEquals(emptyList(), Lang.entries.map { it.code } - tables.keys, "языки клиента без словаря сервера")
        assertTrue(items.all.isNotEmpty(), "строк вещей нет: тест ничего не доказал")
        val lost = tables.flatMap { (code, table) ->
            val text = ModifierText(index) { table[it] }
            items.all.mapNotNull { (template, def) ->
                val line = text.line(def, top(def)?.values(def) ?: List(def.valueCount) { 1.0 }, template.slot)
                "$code: ${def.code.value} на ${template.slot}".takeIf { line == null || '{' in line || !own(def, template.slot, table) }
            }
        }
        assertEquals(emptyList(), lost.distinct(), "строки вещей без своего текста карточки")
    }

    /** Как строка [effect] видна герою; null - сводке по правилам не принадлежит. */
    private fun shown(effect: Effect): Shown? = when {
        effect.condition != null -> Shown.CONDITIONAL
        registry[effect.stat]?.group == StatGroup.HERO -> Shown.HERO
        Amplify.scope(effect.stat) != null -> Shown.AMPLIFIER
        index.powers.byStat[effect.stat]?.sheet?.isNotEmpty() == true -> Shown.POWER
        else -> null
    }

    /**
     * Проба строки [line]: что с ней не так в сводке надетой вещи (пусто - всё верно); null - пробы нет (у строки нет тира).
     * Источники пересчётов строки и правил её сил эталон получает пробой поровну с ней и без неё: строка «за каждые N» и сила
     * «от характеристики» иначе молчали бы у героя без источника. Усилителю своей вещи без врождённой строки его вида база получает
     * плоскую строку этого вида. [heroes] - эталоны с вещами этой базы.
     */
    private fun probe(line: ItemLine, heroes: MutableMap<ItemInstance, ReferenceHero>): List<String>? {
        val (template, def) = line
        val label = "${def.code.value} на ${template.code}"
        val fixed = template.fixedCodes.mapNotNull(index::modifier).filter { it.source.permanent }
        val partner = Amplify.target(def)?.takeIf { scope -> fixed.none { amplified(scope, it) } }?.let { scope ->
            items.on(template).firstOrNull { amplified(scope, it) && it.rolls && it.effects.all { e -> e.op == Op.ADD && e.perStat == null } } ?: return listOf("$label: усилителю нечего усиливать")
        }
        val lines = (fixed + listOfNotNull(partner) + def).distinctBy { it.code }
        val rolls = lines.mapNotNull(::top).takeIf { worn -> worn.any { it.code == def.code } } ?: return null
        val heroClass = template.heroClass?.let(index::heroClass) ?: classes.first()
        fun hero(on: List<Roll>): ReferenceHero = wear(template, def, on).let { copy -> heroes.getOrPut(copy) { ReferenceHero(index, heroClass, listOf(copy), level) } }
        val wearing = hero(rolls)
        if (PROBE_ID !in wearing.active) return listOf("$label: эталон её не носит")
        val extra = sources(def)
        val before = hero(rolls.filter { it.code != def.code }).sheet(extra)
        val after = wearing.sheet(extra)
        val moved = (visible(before) + visible(after)).filterTo(HashSet()) { abs((after.values[it] ?: 0.0) - (before.values[it] ?: 0.0)) > SummaryProbe.MOVED }
        return def.effects.flatMap { effect -> check(effect, shown(effect) ?: return@flatMap emptyList(), moved, after).map { "$label: ${effect.stat} ${effect.op} - $it" } }
    }

    /** Что не так с одной строкой [effect] вида [kind] на эталоне с вещью [after]; [moved] - видимые строки сводки, что вещь сдвинула. */
    private fun check(effect: Effect, kind: Shown, moved: Set<String>, after: ReferenceSheet): List<String> {
        if (kind == Shown.CONDITIONAL) {
            val placed = ConditionalStats.of(after.model.operations, registry).any { row ->
                row.condition == effect.condition && row.stat == effect.stat && row.op == effect.op && row.value != 0.0 && row.sources.any { it.source == item }
            }
            return if (placed) emptyList() else listOf("нет в разделе «С условием»")
        }
        val rows = when (kind) {
            Shown.HERO -> moved.filter { it in SummaryProbe.rows(effect.stat, effect.op, registry) }
            Shown.POWER -> moved.filter { row -> index.powers.byStat.getValue(effect.stat).sheet.any { row in SummaryProbe.rows(it.to, it.line ?: Op.ADD, registry) } }
            else -> moved.toList()
        }
        if (rows.isEmpty()) return listOf("не двигает строку сводки")
        val from = if (kind == Shown.HERO) StatSpread.targets(effect.stat, effect.op) else emptyList()
        return listOfNotNull("разбор не называет вещь".takeIf { rows.none { row -> (readFrom(row) + from).any { after.names(it) } } }) +
            rows.filter { abs(after.combat.explain(it).total - (after.values[it] ?: 0.0)) > TOTAL }.map { "разбор $it не сходится с цифрой" } +
            rows.flatMap { row -> Lang.entries.filter { lang -> UiStrings.table(lang).let { it["stat.desc.$row"].isNullOrBlank() || it[StatDetail.key(row)].isNullOrBlank() } }.map { "${it.code}: нет описания $row" } }
    }

    /** Строка сводки [row] и то, из чего её читает бой ([FightReading]): в разборе любой из них видна вещь. */
    private fun readFrom(row: String): List<String> = listOf(row) + FightReading.entries.filter { it.stat == row }.map { it.from }

    /** Разбор характеристики [stat] называет вещь пробы: её вклад или сила, что она держит. */
    private fun ReferenceSheet.names(stat: String): Boolean {
        val breakdown = combat.explain(stat)
        return (breakdown.shares.mapNotNull { it.source } + breakdown.shifts.map { it.source }).any { it == item || it.kind == SourceKind.POWER && item in combat.holders(it.ref) }
    }

    /** Видимые строки сводки эталона: что игрок читает в разделах ([groupedStats]). */
    private fun visible(sheet: ReferenceSheet): Set<String> = groupedStats(sheet.values, registry).flatMapTo(HashSet()) { (_, rows) -> rows.map { it.first } }

    /** Источники пересчётов строки [def] и правил её сил (кроме правил «пока источник ниже порога»): проба [SummaryProbe.PROBE] каждому. */
    private fun sources(def: ModifierDef): List<StatOperation> = def.effects.flatMap { effect ->
        listOfNotNull(effect.perStat) + index.powers.byStat[effect.stat]?.sheet.orEmpty().filter { it.under == null }.flatMap { listOfNotNull(it.from) + it.also }
    }.distinct().map { StatOperation(it, Op.ADD, SummaryProbe.PROBE) }

    /** Строку [def] вида [scope] усиливает усилитель своей вещи: строка героя без условия, что усиление берёт ([Amplify.amplifiable]). */
    private fun amplified(scope: LineScope, def: ModifierDef): Boolean = scope.covers(def) && def.effects.any { effect ->
        effect.condition == null && registry[effect.stat]?.group == StatGroup.HERO && Amplify.amplifiable(index, def, effect.stat, effect.op)
    }

    /** Копия базы [template] со строками [rolls], надетая на своё место (самоцвет - в гнездо): порча - у самой базы и у строки порчи [def]. */
    private fun wear(template: ItemTemplate, def: ModifierDef, rolls: List<Roll>) = ItemInstance(
        id = PROBE_ID,
        template = template.code,
        rarity = if (template.rarity.fixed) template.rarity else Rarity.RARE,
        rolls = rolls,
        corrupted = template.corrupted || def.source == Source.CORRUPTION,
        slot = template.slot,
        socket = socket.takeIf { template.slot == Slot.JEWEL },
    )

    /** Ролл строки [def] высшим тиром и верхом его разброса; тиров нет - null. */
    private fun top(def: ModifierDef): Roll? = def.bestTierAt(Int.MAX_VALUE)?.let { (tier) -> Roll(def.code, tier, 1.0) }

    /** Свой текст строки [def] на носителе [slot] в словаре [table]: имя описания или семейства, иначе шаблон каждого эффекта. */
    private fun own(def: ModifierDef, slot: Slot, table: Map<String, String>): Boolean =
        LocaleKey.modifierName(def.code) in table || def.variant != VariantKind.NATURAL && LocaleKey.modifierName(def.family) in table ||
            def.effects.indices.all { i ->
                val effect = def.effects[i]
                ModifierText.templateKey(effect, ModifierText.negative(def, def.slot(i)), slot.isFlask && FlaskSheet.local(def, effect)) in table
            }

    /** Как строка вещи видна герою: строка героя, строка с условием, сила с правилами листа, усилитель своей вещи. */
    private enum class Shown { HERO, CONDITIONAL, POWER, AMPLIFIER }

    private companion object {
        /** Ид копии пробы. */
        const val PROBE_ID = "PROBE"

        /** Расхождение разбора с цифрой сводки - как у `SheetExplainTest`. */
        const val TOTAL = 0.15
    }
}

/** Строка вещи: описание [def] на базе [template]. */
private data class ItemLine(val template: ItemTemplate, val def: ModifierDef)

/**
 * Все строки, что ложатся на вещи контента любым путём: таблицы шаблона, порча Ваал, влияние, раскрытие скрытого аффикса
 * (таблица `veiled:<сторона>`, у инструмента - своя), верстак, гарантия эссенции, ручная работа кузнеца, врождённые и строки
 * уникалок; локальная строка - только там, где у базы есть что увеличивать ([AffixRoller.bears]). Без монстров и меток скрытого.
 */
private class ItemLines(private val index: ContentIndex) {
    private val affixes = AffixRoller(index)
    private val smithed: Set<String> = index.templatePool(index.professions.crafting.tables).mapTo(HashSet()) { it.value.code }
    private val handcrafted: List<ModifierDef> = index.modifierPool(index.professions.crafting.modifiers).map { it.value }

    /** Строки по базам: каждое описание на каждом носителе - для текста карточки (без карт: у них свой текст). */
    val all: List<ItemLine> by lazy {
        index.templates.values.filter { it.slot != Slot.MAP }.flatMap { template -> on(template).map { ItemLine(template, it) } }.distinctBy { it.def.code to it.template.slot }
    }

    /** Строки вещей, что носит герой (без инструментов, фляг и карт): каждое описание - на первой базе, обычной раньше уникальной. */
    val worn: List<ItemLine> by lazy {
        index.templates.values.filter { it.slot.let { slot -> slot != Slot.MAP && !slot.isTool && !slot.isFlask } }.sortedBy { it.unique }
            .flatMap { template -> on(template).map { ItemLine(template, it) } }.distinctBy { it.def.code }
    }

    /** Описания, что могут лечь на базу [template]. */
    fun on(template: ItemTemplate): List<ModifierDef> {
        val slot = template.slot
        val influences = if (slot.influenceable) Influence.entries.flatMap { AffixRoller.influenceTags(it, slot) } else emptyList()
        val pooled = index.modifierPool(template.tables + affixes.corruptionTags(template) + influences + veils(slot)).map { it.value }
        val bench = index.bench.filter { it.fits(slot) }.mapNotNull { index.modifier(it.modifier) }
        val essences = index.essences.essences.values.flatMap { it.guarantees(template) }.mapNotNull(index::modifier)
        val fixed = template.fixedCodes.mapNotNull(index::modifier)
        return (pooled + bench + essences + fixed + (if (template.code in smithed) handcrafted else emptyList()))
            .filter { !it.monster && it.code !in Veils.CODES && affixes.bears(template, it) }.distinctBy { it.code }
    }

    /** Таблицы гибридов раскрытия на месте [slot]: как их берёт [Veils]; самоцвет, карта и фляга скрытого не несут. */
    private fun veils(slot: Slot): List<String> = if (slot.isJewelLike || slot.isFlask) {
        emptyList()
    } else {
        listOf(Source.PREFIX, Source.SUFFIX).map { side -> Veils.table(slot, side) }
    }
}
