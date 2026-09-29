package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.Source
import com.sperance.exileforge.rules.content.Tier
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SellPrice
import kotlin.math.round

/**
 * What put a line on an item, lettered as Path of Exile's trade site letters it: P prefix, S suffix,
 * I implicit, E enchantment, C the bench, F fractured, U unique — and this game's own H for the smith's
 * handcraft, V for a corruption, A for a map's alchemy, X for a special essence's line.
 */
enum class AffixKind(val letter: Char) {
    PREFIX('P'), SUFFIX('S'), IMPLICIT('I'), ENCHANTMENT('E'), CRAFTED('C'), HANDCRAFTED('H'),
    FRACTURED('F'), CORRUPTION('V'), ALCHEMY('A'), UNIQUE('U'), ESSENCE('X');

    companion object {
        /** A fracture or the bench outranks the place a line holds: that is what decides what an orb may do to it. */
        fun of(source: Source?, crafted: Boolean, fractured: Boolean): AffixKind? = when {
            fractured -> FRACTURED
            crafted -> CRAFTED
            else -> when (source) {
                Source.PREFIX -> PREFIX
                Source.SUFFIX -> SUFFIX
                Source.IMPLICIT -> IMPLICIT
                Source.ENCHANTMENT -> ENCHANTMENT
                Source.HANDCRAFTED -> HANDCRAFTED
                Source.CORRUPTION -> CORRUPTION
                Source.ALCHEMY -> ALCHEMY
                Source.UNIQUE -> UNIQUE
                Source.ESSENCE -> ESSENCE
                Source.PASSIVE, Source.MONSTER, Source.ATLAS, Source.RULE, null -> null
            }
        }
    }
}

/** What a rolled line is besides its sentence: its tier, and what placed it. */
data class AffixMarks(val tier: Int, val crafted: Boolean, val fractured: Boolean, val handcrafted: Boolean = false, val alchemy: Boolean = false,
                      val kind: AffixKind? = null) {
    /** The badge as it is printed: the letter and, for a rolled line, its tier — "P1", "S3", "I". */
    val badge: String? get() = kind?.let { if (tier > 0) "${it.letter}$tier" else "${it.letter}" }

    companion object {
        fun of(def: ModifierDef?, roll: Roll) = AffixMarks(
            tier = roll.tier, crafted = def?.crafted == true, fractured = roll.fractured,
            handcrafted = def?.source == Source.HANDCRAFTED, alchemy = def?.source == Source.ALCHEMY,
            kind = AffixKind.of(def?.source, def?.crafted == true, roll.fractured),
        )
    }
}

/** One line of an item as a card draws it: the roll, its words, its badge, and how well it rolled inside its tier. */
data class ItemLine(val roll: Roll, val definition: ModifierDef?, val values: List<Double>, val text: String, val marks: AffixMarks) {
    val code: String get() = roll.code
    val affix: Boolean get() = definition?.affix == true
    /** 0 the bottom of the tier, 1 its top; a fixed range is perfect; null for a line with no tier. */
    val quality: Double? get() = definition?.tier(roll.tier)?.let { tier -> if (tier.values.all { it[1] <= it[0] }) 1.0 else roll.share }
    /** The tier's ranges as the card prints them: "70–79", a fixed one as its single number, effects split by " / ". */
    val range: String? get() = definition?.let { def -> def.tier(roll.tier)?.let { rangeText(def, it) } }
}

/** What the head of a card reads before any line: how well the item rolled on average, how many affix places are still open, and its best tier. */
data class RollSummary(val quality: Int?, val openSlots: Int?, val bestTier: Int?)

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
 */
class ItemView(val item: ItemInstance, val template: ItemTemplate, val index: ContentIndex) {
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
    /** The kind of quality (3.36.0): a catalyst's, or the base's when null. */
    val catalyst: com.sperance.exileforge.rules.content.Catalyst? get() = item.catalyst
    val weaponType: WeaponType? get() = template.weaponType
    val level: Int get() = item.level(template)
    val title: String get() = equipmentTitle(code)
    val description: String get() = equipmentDescription(code)
    val trade: String? get() = tradeName(code, equipment = true)
    val requirements: List<String> get() = itemRequirements(template)
    val visualKind: ItemVisualKind get() = itemVisualKind(template)

    private val text by lazy { modifierText(index) }

    /** Every roll on the copy as a line, the retired ones left out, in the order they lie on the copy. */
    val lines: List<ItemLine> by lazy {
        item.rolls.mapNotNull { roll ->
            val def = index.modifier(roll.code)
            if (def?.retired() == true) return@mapNotNull null
            val values = def?.let(roll::values).orEmpty()
            val words = def?.let { modifierLine(index, it, values) } ?: displayName(roll.code)
            ItemLine(roll, def, values, words, AffixMarks.of(def, roll))
        }
    }

    /** The lines that hold affix places (prefixes and suffixes, the bench's among them). */
    val affixes: List<ItemLine> get() = lines.filter { it.affix }

    /** The base of the item with every local line already folded into it: a helmet of 100 armour with «20% increased armour» wears 120. */
    val base: List<BaseProperty> by lazy {
        if (template.base.isEmpty()) emptyList() else {
            val calc = SheetCalculator(index)
            val baseOps = calc.expand(template.base)
            val baseTotals = calc.compute(emptyMap(), baseOps)
            val totals = calc.compute(emptyMap(), baseOps + calc.expandRolls(item.rolls.filter { index.modifier(it.code)?.local == true }))
            template.base.mapNotNull { line ->
                val def = index.modifier(line.code) ?: return@mapNotNull null
                if (def.retired()) return@mapNotNull null
                val values = line.values.mapIndexed { i, own -> val stat = def.effects.getOrNull(i)?.stat.orEmpty(); PropertyValue(stat, baseTotals[stat] ?: own, totals[stat] ?: own) }
                BaseProperty(line.code, text.template(def).orEmpty(), values)
            }.filter { it.values.isNotEmpty() }
        }
    }

    val summary: RollSummary by lazy {
        val qualities = lines.mapNotNull { it.quality }
        val limits = index.limits(rarity, slot)
        RollSummary(
            quality = qualities.takeIf { it.isNotEmpty() }?.let { Math.round(it.average() * 100).toInt() },
            openSlots = if (limits.ceiling > 0) (limits.prefixes + limits.suffixes - affixes.size).coerceAtLeast(0) else null,
            bestTier = lines.filter { it.marks.kind in RANKED }.mapNotNull { it.roll.tier.takeIf { t -> t > 0 } }.minOrNull(),
        )
    }

    /** The true/false states, in a fixed order: corrupted, mirrored, an influence, fractured, crafted, worn, socketed. */
    val states: List<String> by lazy {
        listOfNotNull(
            "corrupted".takeIf { corrupted }, "mirrored".takeIf { mirrored }, influence?.name?.lowercase(),
            "fractured".takeIf { lines.any { it.marks.fractured } }, "crafted".takeIf { lines.any { it.marks.crafted } },
            "equipped".takeIf { equipped && !socketed }, "socketed".takeIf { socketed },
        )
    }

    /** What the merchant pays now, by the rules' price: base, rarity, a share per line, and `STOCK_GOLD`. */
    fun sellPrice(stats: Map<String, Double>): Long = SellPrice.of(index, template, item, stats)

    /** The item's lines summed by stat — a map's effects — as the server sums them on entry. */
    fun effects(): Map<String, Double> {
        val effects = mutableMapOf<String, Double>()
        item.rolls.forEach { roll ->
            val def = index.modifier(roll.code) ?: return@forEach
            val values = roll.values(def)
            def.effects.forEachIndexed { i, effect -> effects.merge(effect.stat, values.getOrElse(i) { 0.0 }, Double::plus) }
        }
        return effects
    }

    override fun equals(other: Any?): Boolean = other is ItemView && other.item == item && other.template.code == template.code
    override fun hashCode(): Int = item.hashCode()

    companion object {
        private val RANKED = setOf(AffixKind.PREFIX, AffixKind.SUFFIX, AffixKind.FRACTURED, AffixKind.CRAFTED)

        /** The view of [item], or null for a copy whose template the content does not hold. */
        fun of(item: ItemInstance, index: ContentIndex): ItemView? = index.template(item.template)?.let { ItemView(item, it, index) }
    }
}

/** What a hero must reach before an item counts, shortened for a line; level 1 is not a requirement at all. */
fun itemRequirements(template: ItemTemplate, lang: Lang = uiLanguage): List<String> = listOfNotNull(
    template.requiredLevel.takeIf { it > 1 }?.let { "$it ${ui(lang, "req.short.requiredLevel")}" },
    template.requiredStrength.takeIf { it > 0 }?.let { "$it ${ui(lang, "req.short.requiredStrength")}" },
    template.requiredDexterity.takeIf { it > 0 }?.let { "$it ${ui(lang, "req.short.requiredDexterity")}" },
    template.requiredIntelligence.takeIf { it > 0 }?.let { "$it ${ui(lang, "req.short.requiredIntelligence")}" },
)

fun itemVisualKind(template: ItemTemplate): ItemVisualKind = when {
    template.weaponType == WeaponType.BOW || template.slot == Slot.QUIVER -> ItemVisualKind.BOW
    template.weaponType == WeaponType.WAND -> ItemVisualKind.WAND
    template.weaponType == WeaponType.AXE || template.weaponType == WeaponType.DOUBLEAXE -> ItemVisualKind.AXE
    template.weaponType == WeaponType.BLADE -> ItemVisualKind.DAGGER
    template.weaponType == WeaponType.LONGSWORD || template.weaponType == WeaponType.DOUBLESWORD -> ItemVisualKind.STAFF
    template.slot.isWeapon -> ItemVisualKind.SWORD
    else -> when (template.slot) {
        Slot.HELMET -> ItemVisualKind.HELMET; Slot.BODY -> ItemVisualKind.ARMOR
        Slot.GLOVES -> ItemVisualKind.GLOVES; Slot.BOOTS -> ItemVisualKind.BOOTS
        Slot.RING, Slot.RING_2 -> ItemVisualKind.RING; Slot.AMULET, Slot.COLLAR -> ItemVisualKind.AMULET
        Slot.BELT -> ItemVisualKind.BELT; Slot.SHIELD -> ItemVisualKind.SHIELD; Slot.WINGS -> ItemVisualKind.WINGS
        Slot.JEWEL -> ItemVisualKind.GEM; Slot.MAP -> ItemVisualKind.MAP
        Slot.FLASK, Slot.FLASK_2, Slot.FLASK_3 -> ItemVisualKind.SCROLL
        else -> ItemVisualKind.ITEM
    }
}

/** What a state is called; a flag the dictionary has no name for yet reads as its own word, capitalised. */
fun stateTitle(state: String, lang: Lang = uiLanguage): String = uiOr(lang, "state.$state", displayName(state, lang).replaceFirstChar { it.uppercase() })

/** The tier's ranges as text: "70–79" per effect, a point as its number, effects split by " / ". */
fun rangeText(def: ModifierDef, tier: Tier): String? = tier.values.mapIndexedNotNull { index, range ->
    val (low, high) = range.takeIf { it.size == 2 } ?: return@mapIndexedNotNull null
    val stat = def.effects.getOrNull(index)?.stat.orEmpty()
    val from = modNumber(stat, low); val to = modNumber(stat, high)
    if (from == to) from else "$from–$to"
}.joinToString(" / ").ifBlank { null }

/** A bench line as the sentence it would add, with the tier's range where the roll will land: "+(70–79) to maximum Life". */
fun recipeText(index: ContentIndex, recipe: BenchRecipe): String {
    val def = index.modifier(recipe.modifier)
    val ranges = recipe.values.filter { it.size == 2 }.mapIndexed { i, (min, max) ->
        val stat = def?.effects?.getOrNull(i)?.stat.orEmpty()
        val low = modNumber(stat, min); val high = modNumber(stat, max)
        if (low == high) low else "($low–$high)"
    }
    val template = def?.let { modifierText(index).template(it) } ?: return ranges.joinToString(" · ").ifBlank { displayName(recipe.modifier) }
    return fillTemplate(template, ranges)
}

/** A monster's or a map's summed effect as a short line: «+40% life», by operation. */
fun effectText(stat: String, op: Op, value: Double, index: ContentIndex? = null): String {
    val title = statTitle(stat)
    val size = modNumber(stat, value)
    return when (op) {
        Op.ADD -> ui("fight.line_add", size + effectUnit(stat, op, index), title)
        Op.INCREASED -> ui("fight.line_increased", size, title)
        Op.MORE -> ui("fight.line_more", size, title)
        Op.SET -> ui("fight.line_set", size, title)
    }
}

/** The sign an effect's figure carries: a percent for a relative operation, and for a flat one on a stat counted in percent. */
fun effectUnit(stat: String, op: Op, index: ContentIndex? = null): String =
    if (op == Op.INCREASED || op == Op.MORE || (op != Op.SET && statPercent(stat, index))) "%" else ""

/** How a stacking item of the bag is drawn when the server has no icon for it: by its category. */
fun bagVisualKind(item: com.sperance.exileforge.rules.content.Item): ItemVisualKind = when (item.category) {
    com.sperance.exileforge.rules.content.Item.CURRENCY -> ItemVisualKind.CURRENCY
    com.sperance.exileforge.rules.content.Item.BOOK -> ItemVisualKind.SCROLL
    com.sperance.exileforge.rules.content.Item.ESSENCE -> ItemVisualKind.GEM
    else -> ItemVisualKind.ITEM
}
