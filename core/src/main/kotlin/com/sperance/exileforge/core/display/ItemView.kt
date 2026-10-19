package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.BaseVariance
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Range
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.Source
import com.sperance.exileforge.rules.content.Tier
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.roll.AffixRoller
import com.sperance.exileforge.rules.roll.AffixSides
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.rules.sheet.AmplifiedLine
import com.sperance.exileforge.rules.sheet.Amplify
import com.sperance.exileforge.rules.sheet.FlaskSheet
import com.sperance.exileforge.rules.sheet.SellPrice
import com.sperance.exileforge.rules.sheet.SheetCalculator
import kotlin.math.round

/**
 * Что поставило строку на вещь - буквой, как её пишет торговая площадка Path of Exile: P префикс, S суффикс, I врождённая,
 * C верстак, F расколотая, U уникальная, и свои буквы игры: H ручная работа кузнеца, V порча, A алхимия карты, X строка особой
 * эссенции, N строка пула влияния.
 *
 * Порядок записей - порядок строк на карточке, в плитке и строках списков ([ItemView.lineGroups]). [special] - особый источник:
 * полная карточка кладёт его строки плашками особого блока над основными.
 */
enum class AffixKind(val letter: Char, val special: Boolean) {
    IMPLICIT('I', false),
    CORRUPTION('V', true),
    FRACTURED('F', true),
    ESSENCE('X', true),
    HANDCRAFTED('H', true),
    ALCHEMY('A', true),
    CRAFTED('C', true),
    INFLUENCE('N', true),
    UNIQUE('U', false),
    PREFIX('P', false),
    SUFFIX('S', false),
    ;

    companion object {
        /**
         * Расколотость и верстак сильнее места строки: они решают, что сфера может с ней сделать; строка пула влияния [influenced] -
         * своего вида, место аффикса она всё равно занимает.
         */
        fun of(source: Source?, crafted: Boolean, fractured: Boolean, influenced: Boolean = false): AffixKind? = when {
            fractured -> FRACTURED

            crafted -> CRAFTED

            influenced -> INFLUENCE

            else -> when (source) {
                Source.PREFIX -> PREFIX
                Source.SUFFIX -> SUFFIX
                Source.IMPLICIT -> IMPLICIT
                Source.HANDCRAFTED -> HANDCRAFTED
                Source.CORRUPTION -> CORRUPTION
                Source.ALCHEMY -> ALCHEMY
                Source.UNIQUE -> UNIQUE
                Source.ESSENCE -> ESSENCE
                Source.PASSIVE, Source.MONSTER, Source.ATLAS, Source.RULE, Source.ALTAR, Source.LAW, null -> null
            }
        }
    }
}

/** Имя влияния по словарю сервера: «Зодчий», «Пустота», «Бездна». */
fun influenceTitle(influence: Influence): String = loc("enum.EnumInfluence.${influence.name}")

/**
 * Тир модификатора словами: «T3 из 8 (T1 - сильнейший)» - одна подпись везде, где печатается тир строки. T1 - лучший тир
 * (высший уровень открытия), как цвет значка тира на карточке.
 */
fun tierOf(tier: Int, count: Int): String = ui("modinfo.tier_of", tier, count)

/** What a rolled line is besides its sentence: its tier, and what placed it. */
data class AffixMarks(
    val tier: Int,
    val crafted: Boolean,
    val fractured: Boolean,
    val handcrafted: Boolean = false,
    val alchemy: Boolean = false,
    val kind: AffixKind? = null,
    /** Влияние строки вида [AffixKind.INFLUENCE]: его цвет и буква; у прочих - null. */
    val influence: Influence? = null,
) {
    /** Буква значка: у строки влияния - первая буква имени влияния, у прочих - буква вида; строка без вида - пусто. */
    val letter: String get() = influence?.let { influenceTitle(it).take(1).uppercase() } ?: kind?.letter?.toString().orEmpty()

    /** The badge as it is printed: the letter and, for a rolled line, its tier — "P1", "S3", "I". */
    val badge: String? get() = kind?.let { if (tier > 0) "$letter$tier" else letter }

    /** Имя источника на плашке особой строки: у строки влияния - имя влияния, у прочих - слово вида (`mod.seal.<вид>`). */
    val sealName: String get() = influence?.let(::influenceTitle) ?: kind?.let { ui("mod.seal.${it.name}") }.orEmpty()

    companion object {
        fun of(def: ModifierDef?, roll: Roll): AffixMarks {
            val kind = AffixKind.of(def?.source, def?.crafted == true, roll.fractured, def?.influence != null)
            return AffixMarks(
                tier = roll.tier,
                crafted = def?.crafted == true,
                fractured = roll.fractured,
                handcrafted = def?.source == Source.HANDCRAFTED,
                alchemy = def?.source == Source.ALCHEMY,
                kind = kind,
                influence = def?.influence?.takeIf { kind == AffixKind.INFLUENCE },
            )
        }
    }
}

/** Строки вещи одного вида [kind] (null - строки без вида): группа карточки в порядке записей [AffixKind]. */
data class LineGroup(val kind: AffixKind?, val lines: List<ItemLine>) {
    /** Группа особого источника: полная карточка кладёт её строки плашками. */
    val special: Boolean get() = kind?.special == true
}

/**
 * One line of an item as a card draws it: the roll, its words, its badge, and how well it rolled inside its tier;
 * [itemLevel] is the copy's level the tiers are gated by, 0 when unknown.
 */
data class ItemLine(
    val roll: Roll,
    val definition: ModifierDef?,
    val values: List<Double>,
    val text: String,
    val marks: AffixMarks,
    val itemLevel: Int = 0,
    /** Строка, усиленная усилителями своей вещи: [values] и [text] - уже итог, ролл и процент - здесь; null - как есть. */
    val amplified: AmplifiedLine? = null,
) {
    val code: ModifierCode get() = roll.code

    /** Разбор усиленной строки: «ролл 134 · ▲ +15% = 154», значения через « / »; null - строка не усилена. */
    val amplifiedText: String? get() = amplified?.let { amp ->
        val stats = amp.rolled.indices.map { definition?.effectOf(it)?.stat.orEmpty() }
        val rolled = amp.rolled.mapIndexed { i, v -> modNumber(stats[i], v) }.joinToString(" / ")
        val shown = amp.shown.mapIndexed { i, v -> modNumber(stats[i], v) }.joinToString(" / ")
        ui("modinfo.amplified_value", rolled, modNumber("", amp.percent), shown)
    }
    val affix: Boolean get() = definition?.affix == true

    /** 0 the bottom of the tier, 1 its top; a fixed range is perfect; null for a line with no tier. */
    val quality: Double? get() = definition?.tier(roll.tier)?.let { if (fixed) 1.0 else roll.share }

    /** The tier's every range is a single number: there is nowhere inside it to land. */
    val fixed: Boolean get() = definition?.tier(roll.tier)?.values?.all { it[1] <= it[0] } == true

    /** The tier's ranges as the card prints them: "70–79", a fixed one as its single number, effects split by " / ". */
    val range: String? get() = definition?.let { def -> def.tier(roll.tier)?.let { rangeText(def, it) } }

    /** How many tiers the modifier has; 0 for a line with none. */
    val tierCount: Int get() = definition?.tiers?.size ?: 0

    /** Per tier, best (T1) first, whether the copy's level reaches it; every tier when the level is unknown. */
    val tierOpen: List<Boolean> get() = definition?.tiers?.map { itemLevel <= 0 || it.level <= itemLevel }.orEmpty()

    /** Where the definition puts the line — prefix, suffix, implicit…; a fracture and the bench are [marks], not a place. */
    val placement: AffixKind? get() = AffixKind.of(definition?.source, crafted = false, fractured = false)

    /** The characteristics the line moves, each once, in its effects' order. */
    val stats: List<String> get() = definition?.effects?.map { it.stat }?.distinct().orEmpty()
}

/** What the head of a card reads before any line: how well the item rolled on average and its best tier. */
data class RollSummary(val quality: Int?, val bestTier: Int?)

/** One value inside a base line: what the base said, and what the item carries with its local lines folded in. */
data class PropertyValue(val stat: String, val base: Double, val total: Double) {
    val augmented: Boolean get() = round(total * 10) != round(base * 10)
    val text: String get() = statNumber(stat, total)
    val baseText: String get() = statNumber(stat, base)
}

/** One line of an item's base, ready to be drawn; [template] is the dictionary's sentence with a `{0}` per value, or empty. */
data class BaseProperty(val code: String, val template: String, val values: List<PropertyValue>) {
    val augmented: Boolean get() = values.any { it.augmented }
    fun line(): String {
        if (template.isNotBlank()) return fillTemplate(template, values.map { it.text })
        return values.joinToString(" · ") { if (it.stat.isBlank()) it.text else "${it.text} ${statTitle(it.stat)}" }
    }
}

/**
 * An item as the screen reads it: the copy, its template and the index that explains both. Everything a
 * card, a row or a tooltip prints is derived here once, so no screen reads a roll on its own.
 * [ranged] - витрина шаблона ([showcase]): строка печатает вилку своего тира, а не выпавшее число.
 */
class ItemView(val item: ItemInstance, val template: ItemTemplate, val index: ContentIndex, private val ranged: Boolean = false) {
    val id: String get() = item.id
    val code: String get() = template.code
    val rarity: Rarity get() = item.rarity

    /** The template's slot: what the thing is. Where it is worn is [wornSlot]. */
    val slot: Slot get() = template.slot
    val socket: String? get() = item.socket
    val equipped: Boolean get() = item.equipped
    val socketed: Boolean get() = item.socketed
    val corrupted: Boolean get() = item.corrupted
    val mirrored: Boolean get() = item.mirrored
    val influence: Influence? get() = item.influence
    val quality: Int get() = item.quality

    /** The kind of quality: a catalyst's, or the base's when null. */
    val catalyst: com.sperance.exileforge.rules.content.Catalyst? get() = item.catalyst
    val weaponType: WeaponType? get() = template.weaponType
    val level: Int get() = item.level(template)

    /**
     * A map names its zone (one template for every zone), and its tier rides the name with the area level it lifts the
     * zone to: «Rotten Heart Map · tier 8 · lv 85».
     */
    val title: String get() = (if (item.mapZone.isNotEmpty()) mapItemTitle(item.mapZone) else equipmentTitle(code))
        .let { name -> if (item.mapTier > 0) ui("card.map_tier_title", name, item.mapTier, areaLevel ?: level) else name }

    /** Уровень района карты: уровень её зоны, поднятый тиром (`MapRule.areaLevel` правил); у не-карты - null. */
    val areaLevel: Int? get() = index.zone(item.mapZone)?.let { index.campaign.maps.areaLevel(it.level, item.mapTier) }

    val trade: String? get() = tradeName(code, equipment = true)
    val requirements: List<ItemNeed> get() = itemRequirements(template)
    val visualKind: ItemVisualKind get() = itemVisualKind(template)

    private val text by lazy { modifierText(index) }

    /**
     * Every roll on the copy as a line, in the order they lie on the copy.
     *
     * Усилители своей вещи - правилом `Amplify`: строка печатает итог, ролл и процент - в разборе.
     */
    val lines: List<ItemLine> by lazy {
        item.rolls.map { roll ->
            val def = index.modifier(roll.code)
            val tier = def?.tier(roll.tier)?.takeIf { ranged }
            val amplified = if (ranged) null else Amplify.line(index, roll, item.rolls)
            val values = amplified?.shown ?: def?.let(roll::values).orEmpty()
            val words = def?.let { tier?.let { rangedLine(index, roll.code, it.values, template.slot) } ?: modifierLine(index, it, values, template.slot) } ?: displayName(roll.code.value)
            ItemLine(roll, def, values, words, AffixMarks.of(def, roll), level, amplified)
        }
    }

    /**
     * Строки для показа, сложенные по видам в порядке записей [AffixKind] (врождённые, особые источники, уникальные, префиксы,
     * суффиксы), внутри вида - как лежат на копии; строки без вида - последними. Один порядок для полной и короткой карточки,
     * плитки и строк списков; [lines] держит порядок копии - по нему адресуют строку команды.
     */
    val lineGroups: List<LineGroup> by lazy {
        lines.groupBy { it.marks.kind }.entries.sortedBy { it.key?.ordinal ?: Int.MAX_VALUE }.map { LineGroup(it.key, it.value) }
    }

    /** Строки в порядке показа ([lineGroups]) одним списком. */
    val orderedLines: List<ItemLine> by lazy { lineGroups.flatMap { it.lines } }

    /** The lines that hold affix places (prefixes and suffixes, the bench's among them). */
    val affixes: List<ItemLine> get() = lines.filter { it.affix }

    /**
     * The base of the item with every local line and its quality folded into it, by the rules' own fold — the
     * one the sheet and the fight wear: a helmet of 100 armour with «+20 armour» and «20% increased armour» wears 144.
     *
     * Своя база копии: её качество базы и уровень вещи растят строки шаблона прежде всего. Фляга печатает лечение, длительность, заряды и
     * эффект по своему листу - со своими строками и качеством, без бонусов героя.
     */
    val base: List<BaseProperty> by lazy {
        if (template.base.isEmpty()) {
            emptyList()
        } else {
            val calc = SheetCalculator(index)
            val baseTotals = calc.compute(emptyMap(), calc.expand(index.rules.loot.baseVariance.base(template, calc.baseScale(template, item))))
            val totals = calc.itemBase(template, item)
            val flask = if (template.slot.isFlask) FlaskSheet.of(item, template, index) else null
            template.base.mapNotNull { line ->
                val def = index.modifier(line.code) ?: return@mapNotNull null
                val values = line.values.mapIndexed { i, own ->
                    val stat = def.effectOf(i)?.stat.orEmpty()
                    val base = baseTotals[stat] ?: own
                    PropertyValue(stat, base, flask?.shown(stat, base) ?: totals[stat] ?: own)
                }
                BaseProperty(line.code.value, text.template(def).orEmpty(), values)
            }.filter { it.values.isNotEmpty() }
        }
    }

    /** Качество роллов: только строки с вилкой - фиксированная строка не «идеальна», ей некуда было выпасть. */
    val summary: RollSummary by lazy {
        val qualities = lines.filterNot { it.fixed }.mapNotNull { it.quality }
        RollSummary(
            quality = qualities.takeIf { it.isNotEmpty() }?.let { Math.round(it.average() * 100).toInt() },
            bestTier = lines.filter { it.marks.kind in RANKED }.mapNotNull { it.roll.tier.takeIf { t -> t > 0 } }.minOrNull(),
        )
    }

    /**
     * Стороны аффиксов по правилу [AffixRoller.sides] - то же, что держат сферы и верстак; null - у редкости в этом
     * слоте мест нет (обычная, уникальная).
     */
    val sides: AffixSides? by lazy { AffixRoller(index).sides(item, template).takeIf { it.prefixLimit + it.suffixLimit > 0 } }

    /** The true/false states, in a fixed order: corrupted, mirrored, an influence, fractured, crafted, worn, socketed. */
    val states: List<String> by lazy {
        listOfNotNull(
            "corrupted".takeIf { corrupted },
            "mirrored".takeIf { mirrored },
            influence?.name?.lowercase(),
            "fractured".takeIf { lines.any { it.marks.fractured } },
            "crafted".takeIf { lines.any { it.marks.crafted } },
            "equipped".takeIf { equipped && !socketed },
            "socketed".takeIf { socketed },
        )
    }

    /**
     * Что значит состояние [state] на этой вещи: подсказка её слота (`state.<состояние>_hint.<слот>` - у карты влияние значит
     * захват), а без неё - общая ([stateHint]).
     */
    fun stateHint(state: String): String = uiOr(uiLanguage, "state.${state}_hint.${slot.name.lowercase()}", com.sperance.exileforge.core.display.stateHint(state))

    /** What the merchant pays, by the rules' price: base, rarity and a share per line; the hero's sheet does not change it. */
    fun sellPrice(): Long = SellPrice.of(index, template, item)

    /** The item's lines summed by stat — a map's effects — as the server sums them on entry. */
    fun effects(): Map<String, Double> {
        val effects = mutableMapOf<String, Double>()
        item.rolls.forEach { roll ->
            val def = index.modifier(roll.code) ?: return@forEach
            def.bind(roll.values(def)).forEach { (effect, value) -> effects.merge(effect.stat, value, Double::plus) }
        }
        return effects
    }

    override fun equals(other: Any?): Boolean = other is ItemView && other.item == item && other.template.code == template.code
    override fun hashCode(): Int = item.hashCode()

    companion object {
        private val RANKED = setOf(AffixKind.PREFIX, AffixKind.SUFFIX, AffixKind.FRACTURED, AffixKind.CRAFTED, AffixKind.INFLUENCE)

        /** The view of [item], or null for a copy whose template the content does not hold. */
        fun of(item: ItemInstance, index: ContentIndex): ItemView? = index.template(item.template)?.let { ItemView(item, it, index) }

        private const val SHOWCASE = "showcase"
        private const val SHOWCASE_SEED = 1L

        /**
         * Шаблон как витрина («История»): копия правил с закреплёнными строками шаблона на нейтральной базе, каждая
         * строка - вилкой тира. Своего ролла, номера и владельца у неё нет; кости с постоянным семенем - вид не мигает.
         */
        fun showcase(template: ItemTemplate, index: ContentIndex): ItemView {
            val copy = ItemFactory(index).create(SHOWCASE, template, template.rarity, Dice(SHOWCASE_SEED)).copy(baseQuality = BaseVariance.NEUTRAL)
            return ItemView(copy, template, index, ranged = true)
        }
    }
}

/**
 * Вид требования вещи: [word] - как его называют правила в причине отказа (`Requirements.unmet`: «strength: need 130…»);
 * у класса слова нет - правила его не называют, класс сверяет [unmetNeeds].
 */
enum class NeedKind(val word: String?) {
    LEVEL("level"),
    STRENGTH("strength"),
    DEXTERITY("dexterity"),
    INTELLIGENCE("intelligence"),
    CLASS(null),
    ;

    companion object {
        /** Вид причины отказа правил [reason]; не требование вещи - null. */
        fun of(reason: String): NeedKind? = entries.firstOrNull { it.word != null && reason.substringBefore(':').trim() == it.word }
    }
}

/** Требование вещи: вид и короткая подпись «130 сил». */
data class ItemNeed(val kind: NeedKind, val text: String)

/**
 * What a hero must reach before an item counts, shortened for a line; level 1 is not a requirement at all.
 *
 * Классовую уникалку носит только свой класс.
 */
fun itemRequirements(template: ItemTemplate, lang: Lang = uiLanguage): List<ItemNeed> = listOfNotNull(
    template.requiredLevel.takeIf { it > 1 }?.let { ItemNeed(NeedKind.LEVEL, "$it ${ui(lang, "req.short.requiredLevel")}") },
    template.requiredStrength.takeIf { it > 0 }?.let { ItemNeed(NeedKind.STRENGTH, "$it ${ui(lang, "req.short.requiredStrength")}") },
    template.requiredDexterity.takeIf { it > 0 }?.let { ItemNeed(NeedKind.DEXTERITY, "$it ${ui(lang, "req.short.requiredDexterity")}") },
    template.requiredIntelligence.takeIf { it > 0 }?.let { ItemNeed(NeedKind.INTELLIGENCE, "$it ${ui(lang, "req.short.requiredIntelligence")}") },
    template.heroClass?.let { ItemNeed(NeedKind.CLASS, ui(lang, "req.short.heroClass", classTitle(it))) },
)

/** Невыполненные требования вещи [template] у героя класса [heroClass] по причинам правил [reasons] (`Requirements.unmet`). */
fun unmetNeeds(template: ItemTemplate, heroClass: String, reasons: List<String>): Set<NeedKind> = reasons.mapNotNullTo(HashSet()) { NeedKind.of(it) }.apply { if (template.heroClass?.let { it != heroClass } == true) add(NeedKind.CLASS) }

fun itemVisualKind(template: ItemTemplate): ItemVisualKind = when {
    template.weaponType == WeaponType.BOW || template.slot == Slot.QUIVER -> ItemVisualKind.BOW

    template.weaponType == WeaponType.WAND -> ItemVisualKind.WAND

    template.weaponType == WeaponType.STAFF -> ItemVisualKind.STAFF

    template.weaponType == WeaponType.SCEPTRE -> ItemVisualKind.MACE

    template.weaponType == WeaponType.AXE || template.weaponType == WeaponType.DOUBLEAXE -> ItemVisualKind.AXE

    template.weaponType == WeaponType.BLADE -> ItemVisualKind.DAGGER

    template.weaponType == WeaponType.LONGSWORD || template.weaponType == WeaponType.DOUBLESWORD -> ItemVisualKind.STAFF

    template.slot.isWeapon -> ItemVisualKind.SWORD

    else -> when (template.slot) {
        Slot.HELMET -> ItemVisualKind.HELMET
        Slot.BODY -> ItemVisualKind.ARMOR
        Slot.GLOVES -> ItemVisualKind.GLOVES
        Slot.BOOTS -> ItemVisualKind.BOOTS
        Slot.RING, Slot.RING_2 -> ItemVisualKind.RING
        Slot.AMULET -> ItemVisualKind.AMULET
        Slot.BELT -> ItemVisualKind.BELT
        Slot.SHIELD -> ItemVisualKind.SHIELD
        Slot.WINGS -> ItemVisualKind.WINGS
        Slot.JEWEL -> ItemVisualKind.GEM
        Slot.MAP -> ItemVisualKind.MAP
        Slot.FLASK, Slot.FLASK_2 -> ItemVisualKind.SCROLL
        else -> ItemVisualKind.ITEM
    }
}

/** What a state is called; a flag the dictionary has no name for yet reads as its own word, capitalised. */
fun stateTitle(state: String, lang: Lang = uiLanguage): String = ui(lang, "state.$state")

/** Что значит состояние для игрока - текст подсказки значка; состояние без записи в словаре - пустая строка. */
fun stateHint(state: String, lang: Lang = uiLanguage): String = uiOr(lang, "state.${state}_hint", "")

/** The tier's ranges as text: "70–79" per value, a point as its number, values split by " / ". */
fun rangeText(def: ModifierDef, tier: Tier): String? = tier.values.mapIndexedNotNull { index, range ->
    val (low, high) = range.takeIf { it.size == 2 } ?: return@mapIndexedNotNull null
    val stat = def.effectOf(index)?.stat.orEmpty()
    val from = modNumber(stat, low)
    val to = modNumber(stat, high)
    if (from == to) from else "$from–$to"
}.joinToString(" / ").ifBlank { null }

/** Стороны аффиксов одной строкой: «Преф. 2/3 · Суф. 1/3». */
fun sidesText(sides: AffixSides): String = ui("card.sides", sides.prefixes, sides.prefixLimit, sides.suffixes, sides.suffixLimit)

/** Строка верстака как фраза, что она добавит, с вилкой тира, куда ляжет ролл: «+(70–79) к максимуму здоровья». */
fun recipeText(index: ContentIndex, recipe: BenchRecipe): String = rangedLine(index, recipe.modifier, recipe.values)

/** A modifier's sentence with each value as its range: "+(70–79) to maximum Life" — a bench line, an essence's guarantee. */
fun rangedLine(index: ContentIndex, modifier: ModifierCode, values: List<Range>, carrier: Slot? = null): String {
    val def = index.modifier(modifier)
    val ranges = values.filter { it.size == 2 }.mapIndexed { i, (min, max) ->
        val stat = def?.effectOf(i)?.stat.orEmpty()
        val low = modNumber(stat, min)
        val high = modNumber(stat, max)
        if (low == high) low else "($low–$high)"
    }
    val template = def?.let { modifierText(index).template(it, carrier) } ?: return ranges.joinToString(" · ").ifBlank { displayName(modifier.value) }
    return fillTemplate(template, ranges)
}

/**
 * A monster's or a map's summed effect as a short line: «Здоровье: +40%», by operation; знак - из числа, «−51%», не «+-51%».
 *
 * SET заменяет значение, а не прибавляет: знака у него нет.
 */
fun effectText(stat: String, op: Op, value: Double, index: ContentIndex? = null): String {
    val title = statTitle(stat)
    val size = signedNumber(value) { modNumber(stat, it) }
    return when (op) {
        Op.ADD -> ui("fight.line_add", size + effectUnit(stat, op, index), title)
        Op.INCREASED -> ui("fight.line_increased", size, title)
        Op.MORE -> ui("fight.line_more", size, title)
        Op.SET -> ui("fight.line_set", modNumber(stat, value), title)
    }
}

/** The sign an effect's figure carries: a percent for a relative operation, and for a flat one on a stat counted in percent. */
fun effectUnit(stat: String, op: Op, index: ContentIndex? = null): String = if (op == Op.INCREASED || op == Op.MORE || (op != Op.SET && statPercent(stat, index))) "%" else ""

/** How a stacking item of the bag is drawn when the server has no icon for it: by its category. */
fun bagVisualKind(item: com.sperance.exileforge.rules.content.Item): ItemVisualKind = when (item.category) {
    com.sperance.exileforge.rules.content.Item.CURRENCY, com.sperance.exileforge.rules.content.Item.ATLAS_SEAL -> ItemVisualKind.CURRENCY
    com.sperance.exileforge.rules.content.Item.BOOK -> ItemVisualKind.SCROLL
    com.sperance.exileforge.rules.content.Item.ESSENCE, com.sperance.exileforge.rules.content.Item.RUNE -> ItemVisualKind.GEM
    else -> ItemVisualKind.ITEM
}
