package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.campaign.FightReading
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.SheetReading
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.itemSourceIndex
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.core.sheet.StatDetail
import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Effect
import com.sperance.exileforge.rules.content.FlaskStat
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.HitRange
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.Source
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.StatUnit
import com.sperance.exileforge.rules.content.VariantKind
import com.sperance.exileforge.rules.roll.AffixRoller
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.rules.roll.Veils
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.sheet.Amplify
import com.sperance.exileforge.rules.sheet.FlaskSheet
import com.sperance.exileforge.rules.sheet.LineScope
import com.sperance.exileforge.rules.sheet.MightStats
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatSource
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.text.ModifierText
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Сторожа сводки героя по контенту закреплённого сервера, на общих фикстурах (эталоны [ReferenceHero] по классам строятся один
 * раз на все проверки):
 * - полнота сводки: ни одно свойство модификатора, что ложится на героя (вещь, уникалка, дерево, закон, дар), не выпадает из
 *   сводки - строка без условия встаёт в свой раздел ([ConditionalStats.placed]) и на эталоне двигает итог своей строки, строка с
 *   условием (`when`) - в раздел «С условием» ([ConditionalStats]) со своим условием и вкладом; у каждого условия правил - текст
 *   условия на сервере и пояснение разбора с числами правил на всех языках;
 * - путь «модификатор выпал на вещь → вещь надета → сводка героя»: каждое описание, что любым путём ложится на вещь героя
 *   ([ItemLines]), встаёт высшим тиром на подходящую базу, и вещь надевается эталону листом правил - со сводом локальных строк в
 *   базу вещи, усилителями своей вещи, подъёмами уникалок и силами. Эталон с вещью сравнивается с эталоном с той же вещью без строки:
 *   строка героя без условия и сила с правилами листа двигают видимую строку сводки ([groupedStats]) - свою или ту, что читает бой
 *   ([SummaryProbe.rows]); усилитель своей вещи - строку той строки вещи, что он усиливает; строка с условием встаёт в раздел
 *   «С условием» с вещью в источниках; разбор сдвинутой строки называет вещь (у силы - вещь, что её держит) и сходится с цифрой
 *   сводки; у сдвинутой строки - описание и подробное описание на всех языках клиента. Текст строки на карточке вещи - свой (не
 *   общий шаблон), на всех языках сервера и клиента, без подстановок. Сводке по правилам не принадлежат, и сводкой сторож их не
 *   проверяет (только текстом карточки): строки инструментов - работа ремесла, и фляг - глоток, лист героя их не берёт
 *   ([SheetCalculator.calculate]); силы без правил листа - боя, добычи, ремёсел и слотов - действуют в бою и вне листа, их место -
 *   карточка вещи; свойства самой вещи, что не усиливают её строк (места аффиксов верстака), - тоже карточка. Строки редкостей
 *   ложатся только на монстров, судьбоносная строка Осквернения - не модификатор: её строки дара сводит разбор Предначертания;
 * - разбор цифры сходится с цифрой: формула долей плюс сдвиги над ней - и у листа, и у листа боя с пассивными умениями класса и
 *   каждым Предначертанием, у потока здоровья - ещё и с тиком боя;
 * - каждая строка, что выпадает модификатору или уникалке, учтена: её характеристика зарегистрирована и её читает движок - по
 *   имени в реестрах правил или как силу (без префиксов имён: новая мёртвая характеристика со знакомым именем тоже ловится). Текст
 *   строки на каждом языке проверяет один тест - `LocalizationTest` сервера;
 * - «Где добывается»: у каждой стопки `items.json` есть хотя бы один источник в указателе карточки.
 */
class HeroSheetTest {
    private val item = StatSource(SourceKind.ITEM, PROBE_ID)

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
     * меняет и в сводке не видно. Путь строки через надетую вещь проверяет [every_worn_line_shows_in_the_hero_summary].
     */
    @Test
    fun every_hero_line_moves_its_summary_row() {
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
        val rows = ConditionalStats.of(ops.shuffled(Random(7)), registry)
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
        fun hero(on: List<Roll>): ReferenceHero = wear(template, def, on).let { copy -> heroes.getOrPut(copy) { ReferenceHero(index, heroClass, listOf(copy), wornLevel) } }
        val wearing = hero(rolls)
        if (PROBE_ID !in wearing.active) return listOf("$label: эталон её не носит")
        val extra = sources(def) + gates(def, wearing.plain)
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
        val from = if (kind == Shown.HERO) StatSpread.parts(effect.stat, effect.op).map { it.stat } else emptyList()
        return listOfNotNull("разбор не называет вещь".takeIf { rows.none { row -> (readFrom(row) + from).any { after.names(it) } } }) +
            rows.filter { abs(after.combat.explain(it).total - (after.values[it] ?: 0.0)) > TOTAL }.map { "разбор $it не сходится с цифрой" } +
            rows.flatMap { row -> Lang.entries.filter { lang -> UiStrings.table(lang).let { it["stat.desc.$row"].isNullOrBlank() || it[StatDetail.key(row)].isNullOrBlank() } }.map { "${it.code}: нет описания $row" } }
    }

    /** Строка сводки [row] и то, из чего её читает бой ([SheetReading.RULELESS]): в разборе любой из них видна вещь. */
    private fun readFrom(row: String): List<String> = listOf(row) + SheetReading.RULELESS.filter { it.stat == row }.map { it.from }

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

    /**
     * Условия правил «пока источник ниже порога» у сил строки [def]: каждый источник правила сводится к нулю, а главный - к
     * порогу без единицы, считая от итога [plain] эталона с вещью; так правило действует у эталона, что держит источник выше.
     */
    private fun gates(def: ModifierDef, plain: Map<String, Double>): List<StatOperation> {
        val targets = def.effects.flatMap { effect -> index.powers.byStat[effect.stat]?.sheet.orEmpty() }.flatMap { rule ->
            val from = rule.from ?: return@flatMap emptyList()
            val under = rule.under ?: return@flatMap emptyList()
            rule.also.map { it to 0.0 } + (from to under - 1)
        }.toMap()
        return targets.map { (stat, target) -> StatOperation(stat, Op.ADD, target - (plain[stat] ?: 0.0)) }
    }

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
    private fun own(def: ModifierDef, slot: Slot, table: Map<String, String>): Boolean = LocaleKey.modifierName(def.code) in table || def.variant != VariantKind.NATURAL && LocaleKey.modifierName(def.family) in table ||
        def.effects.indices.all { i ->
            val effect = def.effects[i]
            val weapon = def.ownWeapon(effect, slot)
            (HitRange.ofEnd(effect.stat)?.let { ModifierText.rangeKey(it, weapon) } ?: ModifierText.templateKey(effect, ModifierText.negative(def, def.slot(i)), slot.isFlask && FlaskSheet.local(def, effect), weapon)) in table
        }

    /** Как строка вещи видна герою: строка героя, строка с условием, сила с правилами листа, усилитель своей вещи. */
    private enum class Shown { HERO, CONDITIONAL, POWER, AMPLIFIER }

    /** Окно цифры складывается в цифру: формула её долей плюс сдвиги над ней. */
    @Test fun breakdownAddsUpToTheSheet() {
        index.classes.classes.forEach { heroClass ->
            val sheet = Sheets.calculate(index, 20, heroClass.code, emptyList(), emptyList())
            val explainer = sheet.model!!.explainer
            sheet.stats.forEach { (stat, value) ->
                val b = explainer.explain(stat)
                assertTrue(abs(b.total - value) < 0.05, "${heroClass.code} $stat total ${b.total} != $value")
                assertTrue(abs(b.formed + b.shifts.sumOf { it.delta } - value) < 0.15, "${heroClass.code} $stat formed ${b.formed} + shifts != $value")
            }
        }
    }

    /**
     * Сводка героя - лист боя: с пассивными умениями класса и каждым Предначертанием разбор цифры сводки складывается в
     * неё же - доли умений, шаги перестройки дара и что бой читает вместе с цифрой; максимум зарядов - тот, что держит бой.
     */
    @Test fun combatBreakdownAddsUpToTheSummary() {
        index.classes.classes.forEach { heroClass ->
            val passives = index.skills.ofClass(heroClass.code).filter { it.type == SkillType.AURA || it.type == SkillType.BONUS }.map { it.code }
            val skills = HeroSkills(learned = passives.associateWith { 10 }, passive = passives)
            (index.fates.all.map { it.code } + null).forEach { fate ->
                val sheet = Sheets.calculate(index, 60, heroClass.code, emptyList(), emptyList(), fate = fate)
                val combat = CombatSheet.of(index, sheet, 60, heroClass.code, skills, fate, emptyList())!!
                combat.figures.values.forEach { (stat, value) ->
                    val b = combat.explain(stat)
                    assertTrue(abs(b.total - value) < 0.15, "${heroClass.code} $fate $stat total ${b.total} != $value")
                    assertTrue(abs(b.formed + b.shifts.sumOf { it.delta } - value) < 0.25, "${heroClass.code} $fate $stat formed ${b.formed} + shifts != $value")
                }
                index.rules.charges.kinds.forEach { (kind, rule) ->
                    val held = index.rules.charges.max(kind, combat.stats).toDouble()
                    assertTrue(combat.figures.values[rule.max] == held, "${heroClass.code} $fate $kind: сводка ${combat.figures.values[rule.max]}, бой держит $held зарядов")
                }
                if (fate == LIVING_FORTRESS) {
                    val regen = CoreStat.HEALTH_REGEN.code
                    assertTrue(regen in combat.figures.fated && (combat.figures.values[regen] ?: 0.0) > 0, "${heroClass.code}: регенерация Живой крепости не в сводке")
                }
            }
        }
    }

    /**
     * Поток здоровья - одна функция правил: итог разбора «Регенерации здоровья», её цифра сводки и что тик боя даёт за
     * секунду - одно число, и под картой со всеми частями (доля максимума, «меньше регенерации», из маны, скорость восстановления,
     * деген).
     */
    @Test fun lifeFlowBreakdownIsTheSummaryAndTheFight() {
        val rules = index.campaign.combat
        val heroClass = index.classes.classes.first()
        val sheet = Sheets.calculate(index, 40, heroClass.code, emptyList(), emptyList())
        val combat = CombatSheet.of(index, sheet, 40, heroClass.code, HeroSkills(), null, emptyList())!!
        val regen = CoreStat.HEALTH_REGEN.code
        val moves = mapOf(
            CoreStat.HEALTH.code to 100_000.0 - (combat.stats[CoreStat.HEALTH.code] ?: 0.0),
            regen to 300.0,
            CoreStat.LIFE_REGEN_PERCENT.code to 1.0,
            CoreStat.LIFE_DEGEN_PERCENT.code to 2.0,
            CoreStat.RECOVERY_RATE.code to 20.0,
            CoreStat.MANA_REGEN_TO_LIFE.code to 20.0,
            CoreStat.LIFE_REGEN_LESS.code to 30.0,
        )
        val layer = moves.mapValues { (_, delta) -> listOf(Shift(StatSource(SourceKind.MAP, MAP), delta)) }
        val stats = combat.stats + moves.mapValues { (stat, delta) -> (combat.stats[stat] ?: 0.0) + delta }
        val summary = combat.figures(stats).values.getValue(regen)
        val breakdown = combat.explain(regen, layer).total
        assertTrue(abs(summary - breakdown) < 0.15, "разбор $breakdown != сводка $summary")
        val hero = Combatant(stats, 40, rules)
        val zone = index.campaign.zones.first()
        val rolled = checkNotNull(Spawns(index, Run(index, zone, 7L, RunContext(heroClass.code, 40))).boss(zone, MapEffects.buffs(emptyMap()), emptyList()))
        val foe = PhaseFoes(index, rules).foe(rolled, zone.level)
        val boss = foe.copy(body = Combatant(foe.body.stats + (CoreStat.HEALTH.code to 1e9), foe.body.level, rules))
        val battle = Battle(hero, listOf(boss), rules, index.rules.fight, hero.maxLife, Random(3))
        repeat(25) { if (battle.outcome == null) battle.advance(0.1) }
        val fight = battle.lifeFlow.net
        assertTrue(summary < 0 && abs(fight - summary) < abs(summary) * 0.01, "бой $fight/с != сводка $summary/с")
    }

    /**
     * Регенерация маны не ниже нуля: отрицательная база или «увеличение» останавливают её - сводка ноль, разбор доходит до нуля,
     * и после базы ни один шаг не уводит итог ниже нуля.
     */
    @Test fun manaRegenStopsAtZero() {
        val heroClass = index.classes.classes.first()
        val sheet = Sheets.calculate(index, 40, heroClass.code, emptyList(), emptyList())
        val combat = CombatSheet.of(index, sheet, 40, heroClass.code, HeroSkills(), null, emptyList())!!
        val reading = FightReading.MANA_REGEN
        listOf(CoreStat.MANA_REGEN_FLAT.code to -1e4, CoreStat.MANA_REGEN.code to -1e4).forEach { (stat, delta) ->
            val stats = combat.stats + (stat to (combat.stats[stat] ?: 0.0) + delta)
            val summary = combat.figures(stats).values[reading.stat] ?: 0.0
            val body = Combatant(stats, 40, index.campaign.combat)
            val totals = reading.steps(body)!!.runningFold(body[reading.stat]) { at, step -> at + step.delta }
            assertTrue(abs(summary) < 1e-9 && abs(totals.last()) < 1e-9, "$stat: сводка $summary, разбор ${totals.last()}")
            assertTrue(totals.drop(2).all { it >= -1e-9 }, "$stat: отрицательный шаг разбора $totals")
        }
    }

    /** Часть строки сводки из контента (`partOf`) читается боем вместе со строкой: иначе её цифра пропала бы из сводки. */
    @Test fun everyRowPartIsReadByTheFight() {
        val read = SheetReading.RULELESS.map { it.stat to it.from }.toSet()
        registry.stats.filter { it.partOf != null }.forEach { part -> assertTrue(part.partOf to part.code in read, "${part.code}: часть ${part.partOf} без чтения боя") }
    }

    @Test
    fun everyRolledStatIsRegistered() {
        val missing = effects.map { it.stat }.distinct().filter { it !in index.stats }
        assertTrue(missing.isEmpty(), "not in stats.json: $missing")
    }

    @Test
    fun everyRolledStatIsReadByTheEngine() {
        val dead = effects.map { it.stat }.distinct().filterNot { it in read }
        assertTrue(dead.isEmpty(), "stats nothing reads: $dead")
    }

    @Test
    fun every_stack_has_a_source() {
        val sources = itemSourceIndex(index)
        val orphans = index.content.items.items.map { it.code.value }.filter { it !in EXEMPT && sources[it].isNullOrEmpty() }
        assertTrue(orphans.isEmpty(), "stacks with no source on their card: $orphans")
    }

    private companion object {
        val index: ContentIndex get() = TestContent.index
        val registry get() = index.stats
        val classes by lazy { index.classes.classes.mapNotNull { index.heroClass(it.code) } }
        val items by lazy { ItemLines(index) }

        /** Эталоны [ReferenceHero] по кодам классов: строятся один раз на все проверки. */
        val references by lazy { classes.associate { it.code to ReferenceHero(index, it) } }

        /** Описания модификаторов героя: без строк монстров и скрытой метки (её раскрывает сфера, в сводке её нет). */
        val heroDefs: List<ModifierDef> by lazy {
            index.families.keys.mapNotNull { index.modifier(ModifierCode(it)) }.filter { !it.monster && !it.veiled }
        }

        /** Уровень эталона с вещью: на нём требования уровня держит любая вещь контента. */
        val wornLevel by lazy { index.templates.values.maxOf { it.requiredLevel } }

        /** Гнездо дерева для самоцвета. */
        val socket by lazy { index.tree.byCode.values.first { it.type == SkillNodeType.JEWEL_SOCKET }.code }

        /** Строки, что выпадают модификаторам и уникалкам. */
        val effects: List<Effect> by lazy { index.families.values.flatMap { it.effects } }

        /**
         * Что читает движок: реестры правил, а не префиксы имён - характеристики ядра, карт, Атласа и фляг, силы,
         * повышаемые пределы, перечень боевых характеристик героя Мощи ([MightStats]: каждую он относит к учтённым, сведённым или
         * читаемым боем вне Мощи), ауры врагов (группа `AURA` - их все читает бой), механики фляг - реестр читателей `FlaskStat`
         * метка скрытой строки (её ищет сфера раскрытия) и строки места (`room`: их читают места аффиксов вещи - `ContentIndex.limits`).
         */
        val read: Set<String> by lazy {
            buildSet {
                CoreStat.entries.mapTo(this) { it.code }
                MapStat.entries.mapTo(this) { it.code }
                AtlasStat.entries.mapTo(this) { it.code }
                FlaskStat.entries.mapTo(this) { it.code }
                addAll(index.powers.byStat.keys)
                index.campaign.combat.ceilings.all.mapTo(this) { it.raise }
                addAll(MightStats.COUNTED + MightStats.FOLDED + MightStats.OUTSIDE)
                index.stats.ofGroup(StatGroup.AURA).mapTo(this) { it.code }
                index.families.values.filter { ModifierDef.VEILED in it.tags }.flatMapTo(this) { def -> def.effects.map { it.stat } }
                index.families.values.filter { it.room }.flatMapTo(this) { def -> def.effects.map { it.stat } }
            }
        }

        /** Ид копии пробы. */
        const val PROBE_ID = "PROBE"

        /** Расхождение разбора с цифрой сводки - как у [combatBreakdownAddsUpToTheSummary]. */
        const val TOTAL = 0.15

        const val LIVING_FORTRESS = "LIVING_FORTRESS"

        /** Источник сдвигов листа «под картой» в тесте потока. */
        const val MAP = "MAP_TEST"

        /** Стопки без источника по замыслу контента - код и причина; сейчас таких нет. */
        val EXEMPT: Map<String, String> = emptyMap()
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
