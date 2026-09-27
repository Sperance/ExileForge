package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierDef
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
fun tradeName(code: String, equipment: Boolean): String? =
    locOr(if (equipment) LocaleKey.equipmentTrade(code) else LocaleKey.itemTrade(code), "").takeIf { it.isNotBlank() && it != (if (equipment) equipmentTitle(code) else itemTitle(code)) }

fun monsterTitle(code: String): String = locOr(LocaleKey.monsterName(code), displayName(code))
fun mapTitle(code: String): String = locOr(LocaleKey.mapName(code), displayName(code))
fun mapDescription(code: String): String = locOr(LocaleKey.mapDescription(code), "")
fun regionTitle(code: String): String = locOr(LocaleKey.regionName(code), displayName(code))
fun classTitle(code: String): String = locOr(LocaleKey.className(code), displayName(code))
fun classDescription(code: String): String = locOr(LocaleKey.classDescription(code), "")
fun nodeTitle(code: String): String = locOr(LocaleKey.skillNodeName(code), displayName(code))
fun nodeDescription(code: String): String = locOr(LocaleKey.skillNodeDescription(code), "")
fun atlasNodeTitle(code: String): String = locOr(LocaleKey.atlasNodeName(code), displayName(code))
fun professionTitle(code: String): String = locOr(LocaleKey.professionName(code), displayName(code))
fun professionDescription(code: String): String = locOr(LocaleKey.professionDescription(code), "")
fun jobTitle(code: String): String = locOr(LocaleKey.jobName(code), displayName(code))

/**
 * Title of a stat: the client's own table first (it takes an explicit language), then the server's label
 * for the stat's group, then the humanised code.
 */
fun statTitle(stat: String, lang: Lang = uiLanguage): String = rawStatTitle(stat, lang).replace(PERCENT_MARK, "")

private fun rawStatTitle(stat: String, lang: Lang): String =
    uiOr(lang, "enum.stat.$stat", locOr("enum.EnumStatStock.$stat", locOr("enum.EnumStatBattle.$stat", locOr("enum.EnumStatProfession.$stat", displayName(stat.substringAfter('_'), lang)))))

/**
 * What a stat is and what it moves: the client's table, then the server's; a unique's power stat not
 * described on its own reads the powers' common line.
 */
fun statDescription(stat: String, lang: Lang = uiLanguage, power: Boolean = false): String =
    uiOr(lang, "enum.stat_desc.$stat", locOr("enum.EnumStatStockDesc.$stat", if (power) uiOr(lang, "enum.stat_desc.__POWER__", "") else ""))

/** «Шанс крита, %»: the dictionaries mark a stat counted in percent at the end of its name; the mark moves to the figure (3.2.0). */
private val PERCENT_MARK = Regex("""[,\s]*%\s*$""")

/** Whether a stat reads in percent: its name carries the mark, or the registry counts it so. */
fun statPercent(stat: String, index: ContentIndex? = null): Boolean =
    PERCENT_MARK.containsMatchIn(rawStatTitle(stat, uiLanguage)) || index?.stats?.isPercent(stat) == true

/** A stat's figure as a sheet shows it, the percent sign at the number rather than in the name: «Шанс крита 5%». */
fun statValue(stat: String, value: Double, index: ContentIndex? = null): String = statNumber(stat, value) + if (statPercent(stat, index)) "%" else ""

/** The rules' modifier text over the server's dictionary as it stands now. */
fun modifierText(index: ContentIndex): ModifierText = ModifierText(index.stats, serverLocale::string)

/** A modifier's sentence with its values in; a definition the dictionary cannot word prints its numbers and stats. */
fun modifierLine(index: ContentIndex, def: ModifierDef, values: List<Double>): String =
    modifierText(index).template(def)?.let { fillTemplate(it, values.mapIndexed { i, v -> statNumber(def.effects.getOrNull(i)?.stat.orEmpty(), v) }) }
        ?: values.mapIndexed { i, v -> def.effects.getOrNull(i)?.let { "${statNumber(it.stat, v)} ${statTitle(it.stat)}" } ?: number(v) }.joinToString(" · ").ifBlank { displayName(def.code) }

/** A fixed line — a base, a class's or a tree node's — as one sentence. */
fun lineText(index: ContentIndex, line: Line): String = index.modifier(line.code)?.let { modifierLine(index, it, line.values) } ?: displayName(line.code)

/** A template with its numbers in: `{0}` takes the value as printed, `{|0|}` its size without the sign. */
fun fillTemplate(template: String, values: List<String>): String =
    values.foldIndexed(template) { index, text, value -> text.replace("{|$index|}", value.removePrefix("-").removePrefix("−")).replace("{$index}", value) }

/** Characteristics whose meaning lives in the fraction: rounding them destroys them. */
val preciseStats = setOf(
    "STOCK_ATTACK_SPEED", "STOCK_CAST_SPEED", "STOCK_CRITICAL_CHANCE", "STOCK_CRITICAL_MULTIPLIER", "STOCK_MOVEMENT_SPEED",
    "STOCK_LEECH_PHYSICAL", "STOCK_LEECH_ALL", "STOCK_CRITICAL_VAMPIRE",
)

/** A number as every screen prints it: whole, unless the stat lives in its fraction. The value itself is never rounded. */
fun statNumber(stat: String, value: Double): String =
    if (stat in preciseStats) String.format(java.util.Locale.ROOT, "%.2f", value) else Math.round(value).toString()

fun number(value: Double): String = statNumber("", value)

/** A small figure with its tenth: a bleed of 0.4 a second is «0.4», not «0»; from ten up the whole number stays. */
fun fineNumber(value: Double): String =
    if (kotlin.math.abs(value) >= 10) Math.round(value).toString() else String.format(java.util.Locale.ROOT, "%.1f", value).removeSuffix(".0")

/** Why an equipped item does not count, from the rules' own words — "strength: need 30, have 14". */
fun requirementReason(reason: String, lang: Lang = uiLanguage): String {
    val name = reason.substringBefore(':').trim()
    val rest = reason.substringAfter(':', "").trim()
    val title = uiOr(lang, "req.$name", displayName(name, lang))
    if (rest.isBlank()) return title
    val need = NEED.find(rest)?.groupValues?.get(1)
    val have = HAVE.find(rest)?.groupValues?.get(1)
    return if (need == null || have == null) "$title: $rest" else "$title: " + ui(lang, "req.reason", need, have)
}

private val CAMEL_GAP = Regex("([a-z])([A-Z])")
private val NEED = Regex("need\\s+(-?\\d+)")
private val HAVE = Regex("have\\s+(-?\\d+)")
