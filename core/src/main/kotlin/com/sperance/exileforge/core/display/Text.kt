package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locFirst
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.rules.content.AdditiveEffect
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Effect
import com.sperance.exileforge.rules.content.HitRange
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.content.StatUnit
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.sheet.DamageRange
import com.sperance.exileforge.rules.sheet.HitRanges
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.text.ModifierText

/** A code made readable when no dictionary names it: `IRON_HAT` → «IRON HAT». */
fun displayName(value: String, lang: Lang = uiLanguage): String = value.substringAfterLast('/').substringAfterLast('.').replace('_', ' ')
    .replace(CAMEL_GAP, "$1 $2").ifBlank { ui(lang, "item.unnamed") }

/** Имена из кодов - только из словаря, без кода на месте имени: полноту держит `UiStringsTest`. */
fun slotTitle(slot: Slot, lang: Lang = uiLanguage): String = ui(lang, "enum.slot.${slot.name}")
fun slotTitle(slot: String, lang: Lang = uiLanguage): String = ui(lang, "enum.slot.$slot")
fun rarityTitle(rarity: Rarity, lang: Lang = uiLanguage): String = ui(lang, "enum.rarity.${rarity.name}")
fun rarityTitle(value: String, lang: Lang = uiLanguage): String = ui(lang, "enum.rarity.$value")
fun nodeTypeTitle(type: SkillNodeType, lang: Lang = uiLanguage): String = ui(lang, "enum.node.${type.name}")

/**
 * Имена из словаря сервера: шаблон снаряжения, вещь, монстр, свойство, фаза, тотем, карта, регион, класс, узел Атласа,
 * профессия, работа - нет строки в словаре на устройстве - заглушка «…» и дыра словаря ([loc], `LocaleHoles`), не код.
 */
fun equipmentTitle(code: String): String = if (code.isBlank()) ui("item.equipment") else loc(LocaleKey.equipmentName(code))

/** Вид оружия - подпись сервера `enum.EnumEquipmentWeapon.<код>`: своего словаря видов у клиента нет. */
fun weaponTitle(value: WeaponType): String = loc(LocaleKey.enumLabel("EnumEquipmentWeapon", value.name))

/** The name of a stacking item — an orb, a material, a book, an essence. */
fun itemTitle(code: String): String = if (code.isBlank()) ui("common.item") else locOr(LocaleKey.itemName(code), uiOr(uiLanguage, "enum.orb.$code") { loc(LocaleKey.itemName(code)) })

/**
 * Описание стопки [code] (сфера, материал, оттиск): числа правил `{r:…}` подставляет [index] ([com.sperance.exileforge.rules.text.numbered]);
 * без контента или без числа - пусто, сырой плейсхолдер не печатается.
 */
fun itemDescription(code: String, index: ContentIndex?): String = index.numberedText(locOr(LocaleKey.itemDescription(code), uiOr(uiLanguage, "enum.orb.$code.rule", ""))).orEmpty()

/** Действие сферы или оттиска [code] одной фразой (`enum.orb.<код>.rule`) - итог рецепта кузницы; у прочих стопок пусто. */
fun itemRule(code: String): String = uiOr(uiLanguage, "enum.orb.$code.rule", "")

/**
 * Что делает дар Предначертания [card], словами сервера: числа записи дара `{r:fates.<код>.…}` подставляет [index]
 * ([com.sperance.exileforge.rules.text.numbered]); без контента или без числа - пусто.
 */
fun fateDescription(card: FateCard, index: ContentIndex?): String = index.numberedText(locOr(card.description, "")).orEmpty()

/** The English trade name, one in every language, for a full card only; null when the dictionary has none or it is the shown name already. */
fun tradeName(code: String, equipment: Boolean): String? = locOr(if (equipment) LocaleKey.equipmentTrade(code) else LocaleKey.itemTrade(code), "").takeIf { it.isNotBlank() && it != (if (equipment) equipmentTitle(code) else itemTitle(code)) }

fun monsterTitle(code: String): String = loc(LocaleKey.monsterName(code))
fun monsterTitle(code: MonsterCode): String = monsterTitle(code.value)

/** A monster's trait: its name, and what it does with `{0}` its strength, `{1}` the life threshold, `{2}` the seconds, `{3}` - потолок складываний. */
fun traitTitle(code: String): String = loc(LocaleKey.traitName(code))
fun traitText(code: String, value: Double = 0.0, threshold: Double = 0.0, seconds: Double = 0.0, stacks: Int = 0): String = loc(LocaleKey.traitDescription(code), listOf(fineNumber(value), fineNumber(threshold), fineNumber(seconds), stacks.toString()))

/** Фаза босса: название шаблона и что он делает. */
fun phaseTitle(code: String): String = loc("phase.$code.name")
fun phaseText(code: String): String = locOr("phase.$code.description", "")

/** Тотем босса: название и что делает, пока стоит. */
fun totemTitle(code: String): String = loc("totem.$code.name")
fun totemText(code: String): String = locOr("totem.$code.description", "")

fun mapTitle(code: String): String = loc(LocaleKey.mapName(code))
fun mapTitle(code: MapCode): String = mapTitle(code.value)

/** A map item by its zone: «Tidal Shore Map». */
fun mapItemTitle(zone: MapCode): String = loc(LocaleKey.mapItemName(), listOf(mapTitle(zone)))
fun mapDescription(code: String): String = locOr(LocaleKey.mapDescription(code), "")
fun regionTitle(code: String): String = loc(LocaleKey.regionName(code))
fun classTitle(code: String): String = loc(LocaleKey.className(code))

/** Имя узла дерева - только словарём: нет строки - заглушка и дыра словаря ([loc]), не код. */
fun nodeTitle(code: String): String = loc(LocaleKey.skillNodeName(code))

fun atlasNodeTitle(code: String): String = loc(LocaleKey.atlasNodeName(code))

/** Имя ветви Атласа - словарём клиента. */
fun atlasBranchTitle(branch: AtlasBranch): String = ui("atlas.branch.${branch.name}")
fun professionTitle(code: String): String = loc(LocaleKey.professionName(code))
fun professionDescription(code: String): String = locOr(LocaleKey.professionDescription(code), "")
fun jobTitle(code: String): String = loc(LocaleKey.jobName(code))

/**
 * Что даёт добавка работы: строка кузнеца - характеристики её модификатора из [index] («строка: Здоровье»), лишние единицы - «+1 за цикл».
 */
fun additiveTitle(effect: AdditiveEffect, index: ContentIndex?): String = when (effect) {
    is AdditiveEffect.Modifier -> ui("crafts.additive.modifier", index?.modifier(effect.code)?.effects.orEmpty().map { statTitle(it.stat) }.distinct().joinToString(", "))
    is AdditiveEffect.Yield -> ui("crafts.additive.yield", effect.units)
}

/** What a choosing work was told to make: the item, the smith's group and attribute, the tool base or the skill of a tier ritual. */
fun choiceTitle(choice: String): String = locFirst(LocaleKey.itemName(choice), LocaleKey.choiceName(choice), LocaleKey.equipmentName(choice), LocaleKey.skillName(choice))

/** A work with its choice: «Condense Essence · Weeping Essence of Greed». */
fun workTitle(code: String, choice: String): String = if (choice.isEmpty()) jobTitle(code) else "${jobTitle(code)} · ${choiceTitle(choice)}"

/**
 * Title of a stat: the client's own table first (it takes an explicit language), then the server's label
 * for the stat's group. Нет и его - заглушка и дыра словаря ([loc]), кода игрок не видит.
 */
fun statTitle(stat: String, lang: Lang = uiLanguage): String = rawStatTitle(stat, lang).replace(PERCENT_MARK, "")

private fun rawStatTitle(stat: String, lang: Lang): String = uiOr(lang, "enum.stat.$stat") { loc("enum.EnumStatStock.$stat") }

/** «Шанс крита, %»: the dictionaries mark a stat counted in percent at the end of its name; the mark moves to the figure. */
private val PERCENT_MARK = Regex("""[,\s]*%\s*$""")

/** Единица отображения характеристики (`StatDef.unit`); без контента или вне реестра - null. */
fun statUnit(stat: String, index: ContentIndex?): StatUnit? = index?.stats?.get(stat)?.unit

/**
 * Читается ли характеристика в процентах: решает её единица ([StatUnit]). Метка «%» в имени словаря - только для
 * строк вне реестра, у которых единицы нет.
 */
fun statPercent(stat: String, index: ContentIndex? = null): Boolean = when (statUnit(stat, index)) {
    StatUnit.PERCENT -> true
    StatUnit.FLAG, StatUnit.NUMBER -> false
    null -> PERCENT_MARK.containsMatchIn(rawStatTitle(stat, uiLanguage))
}

/**
 * Часть [part] строки сводки [row] (`partOf`) словами, её число - [figure]: конец диапазона урона ([HitRange]) - «мин …» и «макс …»,
 * «увеличение» (вид `PERCENT` реестра, как `STOCK_MANA_REGEN` у плоской регенерации маны) - «увеличение …», процентная доля числовой
 * строки (`STOCK_LIFE_REGEN_PERCENT`) - «… от максимума»; своя характеристика строки и прочее - число как есть.
 */
fun partFigure(row: String, part: String, figure: String, index: ContentIndex?): String {
    HitRange.ofEnd(part)?.let { return ui(if (part == it.min) "hero.part_min" else "hero.part_max", figure) }
    return when {
        part == row || statPercent(row, index) -> figure
        index?.stats?.isPercent(part) == true -> ui("hero.part_increased", figure)
        statPercent(part, index) -> ui("hero.part_share", figure)
        else -> figure
    }
}

/** Характеристика-флаг ([StatUnit.FLAG]): игрок видит отметку, а не число. */
fun statFlag(stat: String, index: ContentIndex?): Boolean = statUnit(stat, index) == StatUnit.FLAG

/** Отметка флага: действует - ✓, нет - тире. */
fun flagMark(value: Double): String = if (value > 0) FLAG_ON else FLAG_OFF

private const val FLAG_ON = "✓"
private const val FLAG_OFF = "—"

/**
 * A stat's figure as a sheet shows it, the percent sign at the number rather than in the name: «Шанс крита 5%». Множитель
 * (`StatDef.multiplier`) читается долей «60%» - «урон 60% от полного», а не «−40%». Флаг - отметкой без числа.
 */
fun statValue(stat: String, value: Double, index: ContentIndex? = null): String = when {
    index?.stats?.get(stat)?.multiplier == true -> multiplierText(value)
    statFlag(stat, index) -> flagMark(value)
    else -> statNumber(stat, value) + if (statPercent(stat, index)) "%" else ""
}

/** Диапазон урона «мин–макс» вида [stat] концами [ends] (целыми); сошедшиеся концы - одно число. */
fun rangeNumber(stat: String, ends: DamageRange): String {
    val (low, high) = ends
    return if (statNumber(stat, low) == statNumber(stat, high)) statNumber(stat, high) else "${statNumber(stat, low)}–${statNumber(stat, high)}"
}

/**
 * Диапазон урона вида удара [stat] листа [sheet] со средним [mean] (по умолчанию - своё среднее листа) - «мин–макс» сводки
 * ([HitRanges.range]); не вид удара - null.
 */
fun hitRangeText(stat: String, sheet: Map<String, Double>, index: ContentIndex, mean: Double = sheet[stat] ?: 0.0): String? {
    val range = HitRange.of(stat) ?: return null
    return rangeNumber(stat, HitRanges.range(sheet, range, index.stats, mean))
}

/** Процент «больше/меньше» [percent] долей от полного: −40 → «60%». */
fun multiplierText(percent: Double): String = shareText(100 + percent) + "%"

/** Сдвиг множителя [factor] процентом со знаком: 0.60 → «−40%», 1.10 → «+10%». */
fun factorChange(factor: Double): String {
    val change = (factor - 1) * 100
    val mark = if (change < 0) ModifierText.MINUS else "+"
    return mark + shareText(kotlin.math.abs(change)) + "%"
}

/** Доля в процентах: целое без дробной части, иначе одна цифра после точки. */
fun shareText(value: Double): String {
    val rounded = Math.round(value * 10) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else String.format(java.util.Locale.ROOT, "%.1f", rounded)
}

/** The rules' modifier text over the server's dictionary as it stands now. */
fun modifierText(index: ContentIndex): ModifierText = ModifierText(index, serverLocale::string)

/**
 * A modifier's sentence with its values in; a definition the dictionary cannot word prints its numbers and stats. [carrier]
 * - слот вещи со строкой: «этой фляги» и «во время действия» - только на самой фляге.
 */
fun modifierLine(index: ContentIndex, def: ModifierDef, values: List<Double>, carrier: Slot? = null): String = modifierText(index).template(def, values, carrier)?.let { fillTemplate(it, values.mapIndexed { i, v -> effectNumber(def.effectOf(i), v) }) }
    ?: values.mapIndexed { i, v -> def.effectOf(i)?.let { "${modNumber(it.stat, v)} ${statTitle(it.stat)}" } ?: number(v) }.joinToString(" · ").ifBlank { displayName(def.code.value) }

/** A fixed line — a base, a class's or a tree node's — as one sentence. */
fun lineText(index: ContentIndex, line: Line): String = index.modifier(line.code)?.let { modifierLine(index, it, line.values) } ?: displayName(line.code.value)

/** Характеристики, что двигает модификатор, - каждая один раз, по порядку его эффектов. */
fun ModifierDef.stats(): List<String> = effects.map { it.stat }.distinct()

/** Характеристики модификатора [code] ([ModifierDef.stats]); модификатора нет в контенте - пусто. */
fun ContentIndex.statsOf(code: ModifierCode): List<String> = modifier(code)?.stats().orEmpty()

/**
 * Пояснение строки модификатора [def]: что делает её характеристика, словами сервера (`stat.desc.<код>`) с числами правил `{r:…}`
 * ([com.sperance.exileforge.rules.text.numbered]). Пояснение есть у групп [StatGroup.described]: строка карты показывает его
 * подсказкой по касанию. У строки из нескольких эффектов учитывается только первый: его характеристика решает, есть ли пояснение, и
 * даёт его текст. Характеристика без пояснения или подстановка без числа - null.
 */
fun lineNote(index: ContentIndex, def: ModifierDef?): String? = def?.effectOf(0)?.stat
    ?.takeIf { index.stats[it]?.group?.described == true }
    ?.let { index.numberedText(loc(LocaleKey.statDescription(it))) }

/**
 * A template with its numbers in: `{0}` takes the value as printed, `{|0|}` its size without the sign. Плюс шаблона перед
 * плейсхолдером (`+{0}`) - лишь место знака: знак даёт само число, отрицательное печатается «−51», а не «+-51».
 */
fun fillTemplate(template: String, values: List<String>): String = values.foldIndexed(template) { index, text, value ->
    val size = value.removePrefix("-").removePrefix(ModifierText.MINUS)
    val negative = size.length < value.length
    val shown = if (negative) ModifierText.MINUS + size else value
    text.replace("{|$index|}", size).replace("+{$index}", if (negative) shown else "+$value").replace("{$index}", shown)
}

/**
 * Число со знаком - единственный путь знака в строки эффектов, монстров, карт и атласа: «+12», «−51» (типографский
 * минус). Знак берётся из числа, шаблоны словаря его не пишут; [format] печатает модуль. Ноль - с плюсом.
 */
fun signedNumber(value: Double, format: (Double) -> String = ::number): String = (if (value < 0) ModifierText.MINUS else "+") + format(kotlin.math.abs(value))

/** Characteristics whose meaning lives in the fraction: rounding them destroys them. Скорость передвижения - целые проценты. */
val preciseStats = setOf(
    CoreStat.ATTACK_SPEED.code, CoreStat.CAST_SPEED.code, CoreStat.CRITICAL_CHANCE.code, CoreStat.CRITICAL_MULTIPLIER.code,
    CoreStat.LEECH_PHYSICAL.code, CoreStat.LEECH_ALL.code, CoreStat.CRITICAL_VAMPIRE.code, CoreStat.SPELL_CRITICAL_CHANCE.code, CoreStat.SPELL_CRITICAL_MULTIPLIER.code,
)

/**
 * A number as every screen prints it: a stat that lives in its fraction keeps two places; any other keeps its
 * tenth below ten — 7.4, 1.8, 0.4, but «5», not «5.0» — and is whole from ten up. The value itself is never rounded.
 */
fun statNumber(stat: String, value: Double): String = if (stat in preciseStats) preciseNumber(value) else fineNumber(value)

/** Число с сотыми: характеристика, что живёт в дроби, и темп «атак в секунду» - «1.25». */
fun preciseNumber(value: Double): String = String.format(java.util.Locale.ROOT, "%.2f", value)

fun number(value: Double): String = statNumber("", value)

/** Целое с разрядами через неразрывный пробел: «1 240», «2 950» - крупные счётчики вроде опыта. */
fun groupedNumber(value: Long): String = String.format(java.util.Locale.ROOT, "%,d", value).replace(',', '\u00A0')

/** Число строки модификатора: увеличение - доля без хвостовых нулей, прочее - как бросок. */
private fun effectNumber(effect: Effect?, value: Double): String = if (effect?.op == Op.INCREASED) shareNumber(effect.stat, value) else modNumber(effect?.stat.orEmpty(), value)

/** Доля в процентах (увеличение, «больше») точной характеристики - без хвостовых нулей: «0.5%», а не «0.50%». */
fun shareNumber(stat: String, value: Double): String = statNumber(stat, value).let { if ('.' in it) it.trimEnd('0').removeSuffix(".") else it }

/** A modifier's figure, whatever its size, with the tenth it was rolled to: «+12.3», «+1.8», «+5» — a precise stat keeps its hundredths. */
fun modNumber(stat: String, value: Double): String = if (stat in preciseStats) statNumber(stat, value) else String.format(java.util.Locale.ROOT, "%.1f", value).removeSuffix(".0")

/** A small figure with its tenth: a bleed of 0.4 a second is «0.4», not «0»; from ten up the whole number stays. */
fun fineNumber(value: Double): String = if (kotlin.math.abs(value) >= 10) Math.round(value).toString() else String.format(java.util.Locale.ROOT, "%.1f", value).removeSuffix(".0")

/** Время часами: «3:41», «1:05:09». */
fun clock(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Долгий срок до конца: «3 д 14 ч», меньше суток - «14 ч 05 мин». */
fun timeLeft(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    val hours = minutes / 60
    return if (hours >= 24) ui("time.days_hours", hours / 24, hours % 24) else ui("time.hours_minutes", hours, "%02d".format(minutes % 60))
}

/**
 * Why an equipped item does not count, from the rules' own words — "strength: need 30, have 14" reads «Требуется 30 силы»:
 * the hero's own figure is on the hero screen already, so only the need is told.
 *
 * Вторая копия уникального самоцвета: лист называет самоцвет кодом.
 */
fun requirementReason(reason: String, lang: Lang = uiLanguage): String {
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

/** Доля древа гильдии: «Древо гильдии: Обереги» - узлы [nodes] (коды через запятую, `EffectOrigins`) своими именами. */
fun guildLineTitle(nodes: String): String = ui("stat.src.guild", nodes.split(",").joinToString(", ") { loc("guild.node.$it.name") })
