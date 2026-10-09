package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Effect
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.text.ModifierText

/** A code made readable when no dictionary names it: `IRON_HAT` → «IRON HAT». */
fun displayName(value: String, lang: Lang = uiLanguage): String = value.substringAfterLast('/').substringAfterLast('.').replace('_', ' ')
    .replace(CAMEL_GAP, "$1 $2").ifBlank { ui(lang, "item.unnamed") }

fun slotTitle(slot: Slot, lang: Lang = uiLanguage): String = uiOr(lang, "enum.slot.${slot.name}", displayName(slot.name, lang))
fun slotTitle(slot: String, lang: Lang = uiLanguage): String = uiOr(lang, "enum.slot.$slot", displayName(slot, lang))
fun rarityTitle(rarity: Rarity, lang: Lang = uiLanguage): String = uiOr(lang, "enum.rarity.${rarity.name}", displayName(rarity.name, lang))
fun rarityTitle(value: String, lang: Lang = uiLanguage): String = uiOr(lang, "enum.rarity.$value", displayName(value, lang))
fun weaponTitle(value: WeaponType, lang: Lang = uiLanguage): String = uiOr(lang, "enum.weapon.${value.name}", displayName(value.name, lang))
fun nodeTypeTitle(type: SkillNodeType, lang: Lang = uiLanguage): String = uiOr(lang, "enum.node.${type.name}", displayName(type.name, lang))

/** The name of an equipment template, out of the dictionary; a code it does not know reads as the code. */
fun equipmentTitle(code: String): String = if (code.isBlank()) ui("item.equipment") else locOr(LocaleKey.equipmentName(code), displayName(code))
fun equipmentDescription(code: String): String = locOr(LocaleKey.equipmentDescription(code), "")

/** The name of a stacking item — an orb, a material, a book, an essence. */
fun itemTitle(code: String): String = if (code.isBlank()) ui("common.item") else locOr(LocaleKey.itemName(code), uiOr(uiLanguage, "enum.orb.$code", displayName(code)))
fun itemDescription(code: String): String = locOr(LocaleKey.itemDescription(code), uiOr(uiLanguage, "enum.orb.$code.rule", ""))

/** The English trade name, one in every language, for a full card only; null when the dictionary has none or it is the shown name already. */
fun tradeName(code: String, equipment: Boolean): String? = locOr(if (equipment) LocaleKey.equipmentTrade(code) else LocaleKey.itemTrade(code), "").takeIf { it.isNotBlank() && it != (if (equipment) equipmentTitle(code) else itemTitle(code)) }

fun monsterTitle(code: String): String = locOr(LocaleKey.monsterName(code), displayName(code))
fun monsterTitle(code: MonsterCode): String = monsterTitle(code.value)

/** A monster's trait (3.73.0): its name, and what it does with `{0}` its strength, `{1}` the life threshold, `{2}` the seconds. */
fun traitTitle(code: String): String = locOr(LocaleKey.traitName(code), displayName(code))
fun traitText(code: String, value: Double = 0.0, threshold: Double = 0.0, seconds: Double = 0.0): String = loc(LocaleKey.traitDescription(code), listOf(fineNumber(value), fineNumber(threshold), fineNumber(seconds)))

/** Фаза босса (3.92.0): название шаблона и что он делает. */
fun phaseTitle(code: String): String = locOr("phase.$code.name", displayName(code))
fun phaseText(code: String): String = locOr("phase.$code.description", "")

/** Тотем босса (3.93.0): название и что делает, пока стоит. */
fun totemTitle(code: String): String = locOr("totem.$code.name", displayName(code))
fun totemText(code: String): String = locOr("totem.$code.description", "")

fun mapTitle(code: String): String = locOr(LocaleKey.mapName(code), displayName(code))
fun mapTitle(code: MapCode): String = mapTitle(code.value)

/** A map item by its zone: «Tidal Shore Map». */
fun mapItemTitle(zone: MapCode): String = loc(LocaleKey.mapItemName(), listOf(mapTitle(zone)))
fun mapDescription(code: String): String = locOr(LocaleKey.mapDescription(code), "")
fun regionTitle(code: String): String = locOr(LocaleKey.regionName(code), displayName(code))
fun classTitle(code: String): String = locOr(LocaleKey.className(code), displayName(code))
fun nodeTitle(code: String): String = locOr(LocaleKey.skillNodeName(code), displayName(code))
fun atlasNodeTitle(code: String): String = locOr(LocaleKey.atlasNodeName(code), displayName(code))
fun professionTitle(code: String): String = locOr(LocaleKey.professionName(code), displayName(code))
fun professionDescription(code: String): String = locOr(LocaleKey.professionDescription(code), "")
fun jobTitle(code: String): String = locOr(LocaleKey.jobName(code), displayName(code))

/** What a choosing work was told to make: the item, the smith's group and attribute (3.46.0) or the tool base (server 4.2.1). */
fun choiceTitle(choice: String): String = locOr(LocaleKey.itemName(choice), locOr(LocaleKey.choiceName(choice), locOr(LocaleKey.equipmentName(choice), displayName(choice))))

/** A work with its choice (3.45.0): «Condense Essence · Weeping Essence of Greed». */
fun workTitle(code: String, choice: String): String = if (choice.isEmpty()) jobTitle(code) else "${jobTitle(code)} · ${choiceTitle(choice)}"

/**
 * Title of a stat: the client's own table first (it takes an explicit language), then the server's label
 * for the stat's group, then the humanised code.
 */
fun statTitle(stat: String, lang: Lang = uiLanguage): String = rawStatTitle(stat, lang).replace(PERCENT_MARK, "")

private fun rawStatTitle(stat: String, lang: Lang): String = uiOr(lang, "enum.stat.$stat", locOr("enum.EnumStatStock.$stat", locOr("enum.EnumStatBattle.$stat", locOr("enum.EnumStatProfession.$stat", displayName(stat.substringAfter('_'), lang)))))

/** «Шанс крита, %»: the dictionaries mark a stat counted in percent at the end of its name; the mark moves to the figure (3.2.0). */
private val PERCENT_MARK = Regex("""[,\s]*%\s*$""")

/** Whether a stat reads in percent: its name carries the mark, or the registry counts it so. */
fun statPercent(stat: String, index: ContentIndex? = null): Boolean = PERCENT_MARK.containsMatchIn(rawStatTitle(stat, uiLanguage)) || index?.stats?.isPercent(stat) == true

/** A stat's figure as a sheet shows it, the percent sign at the number rather than in the name: «Шанс крита 5%». */
fun statValue(stat: String, value: Double, index: ContentIndex? = null): String = statNumber(stat, value) + if (statPercent(stat, index)) "%" else ""

/** The rules' modifier text over the server's dictionary as it stands now. */
fun modifierText(index: ContentIndex): ModifierText = ModifierText(index.stats, serverLocale::string)

/** A modifier's sentence with its values in; a definition the dictionary cannot word prints its numbers and stats. */
fun modifierLine(index: ContentIndex, def: ModifierDef, values: List<Double>): String = modifierText(index).template(def)?.let { fillTemplate(it, values.mapIndexed { i, v -> effectNumber(def.effectOf(i), v) }) }
    ?: values.mapIndexed { i, v -> def.effectOf(i)?.let { "${modNumber(it.stat, v)} ${statTitle(it.stat)}" } ?: number(v) }.joinToString(" · ").ifBlank { displayName(def.code.value) }

/** A fixed line — a base, a class's or a tree node's — as one sentence. */
fun lineText(index: ContentIndex, line: Line): String = index.modifier(line.code)?.let { modifierLine(index, it, line.values) } ?: displayName(line.code.value)

/**
 * A template with its numbers in: `{0}` takes the value as printed, `{|0|}` its size without the sign. Плюс шаблона перед
 * плейсхолдером (`+{0}`) - лишь место знака (3.90.0): знак даёт само число, отрицательное печатается «−51», а не «+-51».
 */
fun fillTemplate(template: String, values: List<String>): String = values.foldIndexed(template) { index, text, value ->
    val size = value.removePrefix("-").removePrefix(ModifierText.MINUS)
    val negative = size.length < value.length
    val shown = if (negative) ModifierText.MINUS + size else value
    text.replace("{|$index|}", size).replace("+{$index}", if (negative) shown else "+$value").replace("{$index}", shown)
}

/**
 * Число со знаком (3.90.0) - единственный путь знака в строки эффектов, монстров, карт и атласа: «+12», «−51» (типографский
 * минус). Знак берётся из числа, шаблоны словаря его не пишут; [format] печатает модуль. Ноль - с плюсом.
 */
fun signedNumber(value: Double, format: (Double) -> String = ::number): String = (if (value < 0) ModifierText.MINUS else "+") + format(kotlin.math.abs(value))

/** Characteristics whose meaning lives in the fraction: rounding them destroys them. */
val preciseStats = setOf(
    CoreStat.ATTACK_SPEED.code, CoreStat.CAST_SPEED.code, CoreStat.CRITICAL_CHANCE.code, CoreStat.CRITICAL_MULTIPLIER.code, CoreStat.MOVEMENT_SPEED.code,
    CoreStat.LEECH_PHYSICAL.code, CoreStat.LEECH_ALL.code, CoreStat.CRITICAL_VAMPIRE.code, CoreStat.SPELL_CRITICAL_CHANCE.code, CoreStat.SPELL_CRITICAL_MULTIPLIER.code,
)

/**
 * A number as every screen prints it (3.28.0): a stat that lives in its fraction keeps two places; any other keeps its
 * tenth below ten — 7.4, 1.8, 0.4, but «5», not «5.0» — and is whole from ten up. The value itself is never rounded.
 */
fun statNumber(stat: String, value: Double): String = if (stat in preciseStats) String.format(java.util.Locale.ROOT, "%.2f", value) else fineNumber(value)

fun number(value: Double): String = statNumber("", value)

/** Целое с разрядами через неразрывный пробел (3.90.0): «1 240», «2 950» - крупные счётчики вроде опыта. */
fun groupedNumber(value: Long): String = String.format(java.util.Locale.ROOT, "%,d", value).replace(',', '\u00A0')

/** Число строки модификатора: увеличение - доля без хвостовых нулей, прочее - как бросок. */
private fun effectNumber(effect: Effect?, value: Double): String = if (effect?.op == Op.INCREASED) shareNumber(effect.stat, value) else modNumber(effect?.stat.orEmpty(), value)

/** Доля в процентах (увеличение, «больше») точной характеристики - без хвостовых нулей: «0.5%», а не «0.50%» (3.88.9). */
fun shareNumber(stat: String, value: Double): String = statNumber(stat, value).let { if ('.' in it) it.trimEnd('0').removeSuffix(".") else it }

/** A modifier's figure, whatever its size, with the tenth it was rolled to: «+12.3», «+1.8», «+5» — a precise stat keeps its hundredths. */
fun modNumber(stat: String, value: Double): String = if (stat in preciseStats) statNumber(stat, value) else String.format(java.util.Locale.ROOT, "%.1f", value).removeSuffix(".0")

/** A small figure with its tenth: a bleed of 0.4 a second is «0.4», not «0»; from ten up the whole number stays. */
fun fineNumber(value: Double): String = if (kotlin.math.abs(value) >= 10) Math.round(value).toString() else String.format(java.util.Locale.ROOT, "%.1f", value).removeSuffix(".0")

/**
 * Why an equipped item does not count, from the rules' own words — "strength: need 30, have 14" reads «Требуется 30 силы»:
 * the hero's own figure is on the hero screen already (4.0.0), so only the need is told.
 */
fun requirementReason(reason: String, lang: Lang = uiLanguage): String {
    // A second copy of a unique jewel (server 1.31.0): the sheet names the jewel by its code.
    UNIQUE_JEWEL.find(reason)?.let { return ui(lang, "req.unique_jewel", equipmentTitle(it.groupValues[1])) }
    val name = reason.substringBefore(':').trim()
    val rest = reason.substringAfter(':', "").trim()
    val title = uiOr(lang, "req.$name", displayName(name, lang))
    if (rest.isBlank()) return title
    val need = NEED.find(rest)?.groupValues?.get(1) ?: return "$title: $rest"
    return UiStrings.table(lang)["req.need.$name"]?.let { ui(lang, "req.need.$name", need) } ?: ui(lang, "req.need", title, need)
}

private val CAMEL_GAP = Regex("([a-z])([A-Z])")
private val UNIQUE_JEWEL = Regex("^unique jewel: one (\\S+) per hero")
private val NEED = Regex("need\\s+(-?\\d+)")
