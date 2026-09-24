package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.modifier.BenchRecipe
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import kotlinx.serialization.json.*

/**
 * What a character must reach before an item counts, shortened for a line.
 *
 * A requirement the template did not set is left out rather than printed as zero, and level 1 is
 * not a requirement at all. The client only prints these — the server checks them, twice and by
 * two different rules, and a control is never disabled on a reading done here.
 */
fun itemRequirements(doc: JsonObject, lang: Lang = uiLanguage): List<String> = listOf(
    "requiredLevel", "requiredStrength", "requiredDexterity", "requiredIntelligence",
).mapNotNull { key ->
    val short = ui(lang, "req.short.$key")
    val value = (doc[key] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
    if (value <= if (key == "requiredLevel") 1 else 0) null else "$value $short"
}

fun itemVisualKind(doc: JsonObject): ItemVisualKind = when {
    doc["userId"] != null -> ItemVisualKind.CHARACTER
    doc.text("weaponType") == "BOW" || doc.text("slot") == "QUIVER" -> ItemVisualKind.BOW
    doc.text("weaponType") == "WAND" -> ItemVisualKind.WAND
    doc.text("weaponType") in setOf("AXE", "DOUBLEAXE") -> ItemVisualKind.AXE
    doc.text("weaponType") == "BLADE" -> ItemVisualKind.DAGGER
    doc.text("weaponType") in setOf("LONGSWORD", "DOUBLESWORD") -> ItemVisualKind.STAFF
    doc.text("slot").startsWith("WEAPON") || doc.text("type").endsWith("Weapon") -> ItemVisualKind.SWORD
    else -> when (doc.text("slot")) {
        "HELMET" -> ItemVisualKind.HELMET; "BODY" -> ItemVisualKind.ARMOR
        "GLOVES" -> ItemVisualKind.GLOVES; "BOOTS" -> ItemVisualKind.BOOTS
        "RING" -> ItemVisualKind.RING; "AMULET" -> ItemVisualKind.AMULET
        "BELT" -> ItemVisualKind.BELT; "SHIELD" -> ItemVisualKind.SHIELD; "WINGS" -> ItemVisualKind.WINGS
        else -> when {
            doc.text("subCategory") == "STONE" -> ItemVisualKind.GEM
            doc.text("category") == "CONSUMABLE" -> ItemVisualKind.FLASK
            doc.text("category") == CURRENCY_CATEGORY || doc.text("category").endsWith("_STOCK") -> ItemVisualKind.CURRENCY
            else -> ItemVisualKind.ITEM
        }
    }
}

fun displayName(value: String, lang: Lang = uiLanguage): String = value.substringAfterLast('/').substringAfterLast('.').replace('_', ' ')
    .replace(Regex("([a-z])([A-Z])"), "$1 $2").ifBlank { ui(lang, "item.unnamed") }

// A jewel is not worn on the body: its "slot" is a socket on the passive tree, and the table
// names it all the same, because a stash line still has to say what the thing is.
fun slotTitle(slot: String, lang: Lang = uiLanguage) = uiOr(lang, "enum.slot.$slot", displayName(slot, lang))

fun rarityTitle(value: String, lang: Lang = uiLanguage) = uiOr(lang, "enum.rarity.$value", displayName(value, lang))

fun weaponTitle(value: String, lang: Lang = uiLanguage) = uiOr(lang, "enum.weapon.$value", displayName(value, lang))

/**
 * Display projection of one inventory instance over its template.
 *
 * The instance is laid over the template, so its own `rarity` wins: the template only says what the
 * item dropped as, and the orbs move the copy up and down that ladder afterwards.
 *
 * Never post this combined document back: the template belongs to the `equipment` collection and
 * `params` belongs to the instance.
 */
fun inventoryDocument(instance: JsonObject, base: JsonObject?): JsonObject = JsonObject(
    base.orEmpty()
        + mapOf("name" to JsonPrimitive(equipmentTitle(base)))
        + instance.filterKeys { it in setOf("_id", "equipmentId", "params", "equippedSlot", "socketCode", "rarity", "corrupted", "mirrored", "influence") }
)

/**
 * The name of an equipment template, out of the locale bundle.
 *
 * Templates carry only a code since 0.14.0, so a card that has not had its template read yet, or a
 * code the dictionary does not know, falls back to something readable rather than to a raw key.
 */
fun equipmentTitle(template: JsonObject?): String {
    val code = template?.text("code").orEmpty()
    if (code.isBlank()) return ui("item.equipment")
    return locOr(LocaleKey.equipmentName(code), displayName(code))
}

/** The name of an `items` document, out of the locale bundle. */
fun itemTitle(document: JsonObject?): String {
    val code = document?.text("code").orEmpty()
    if (code.isBlank()) return ui("common.item")
    return locOr(LocaleKey.itemName(code), displayName(code))
}

/**
 * The name of any catalogue document.
 *
 * A document that still carries a `name` wrote it itself — a character named by its player, a
 * recipe named by the server, an inventory projection that already looked its template up. Content
 * carries a code instead, and which section of the dictionary that code belongs to is decided by
 * the shape of the document: a slot or a `type` means equipment, anything else is an `items` row.
 */
fun documentTitle(document: JsonObject): String = when {
    document.text("name").isNotBlank() -> document.text("name")
    document["slot"] != null || document.text("type").isNotBlank() -> equipmentTitle(document)
    else -> itemTitle(document)
}

/** The description of any catalogue document, from the same place its name comes from. */
fun documentDescription(document: JsonObject): String {
    val code = document.text("code")
    if (document["userId"] != null || code.isBlank()) return document.text("description")
    val key = if (document["slot"] != null || document.text("type").isNotBlank())
        LocaleKey.equipmentDescription(code) else LocaleKey.itemDescription(code)
    return locOr(key, "")
}

fun inventoryDocument(instance: com.sperance.exileforge.core.model.hero.EquipmentInstance, base: JsonObject?): JsonObject =
    inventoryDocument(instance.document(), base)

/**
 * A rolled modifier as one sentence.
 *
 * Since 0.14.0 a modifier's text is a template in the locale bundle — "+{0} to armour" — with one
 * placeholder per effect, because the words of a composite modifier cannot be reordered in every
 * language if the numbers are bolted on afterwards. So this is the whole line, not a label.
 *
 * Without a dictionary, or for a definition the client has not read, the stats and the numbers are
 * still printed: a value the server rolled should never vanish because a translation is missing.
 */
fun modifierText(modifier: JsonObject, definitions: List<ModifierDefinition> = emptyList()): String {
    val definition = definitions.firstOrNull { it.id == modifier.text("modifierId") }
    val values = rolledValues(modifier, definition)
    val template = definition?.template
    if (template != null && template != definition.code && values.isNotEmpty())
        return values.foldIndexed(template) { index, text, value -> text.replace("{$index}", value) }
    val effects = definition?.effects.orEmpty()
    return values.mapIndexed { index, value ->
        effects.getOrNull(index)?.let { "$value ${statTitle(it.stat)}" } ?: value
    }.joinToString(" · ").ifBlank { definition?.code ?: displayName(modifier.text("modifierId")) }
}

/**
 * The rolled numbers as text, each one printed by the rule of the characteristic it rolled on.
 *
 * The effect at the same index names that characteristic, which is why the definition is passed
 * in: a value is not a number in the abstract, it is armour or an attack speed, and the two are
 * printed differently.
 */
private fun rolledValues(modifier: JsonObject, definition: ModifierDefinition? = null): List<String> =
    (modifier["values"] as? JsonArray).orEmpty().mapIndexed { index, value ->
        val number = (value as? JsonPrimitive)?.doubleOrNull
        if (number == null) (value as? JsonPrimitive)?.content.orEmpty()
        else statNumber(definition?.effects?.getOrNull(index)?.stat.orEmpty(), number)
    }

/**
 * What a rolled affix is besides its sentence: its tier, and whether the bench placed it or a
 * Fracturing Orb fixed it. A fixed modifier (a base, a tree node) has no tier and answers zero.
 */
data class AffixMarks(val tier: Int, val crafted: Boolean, val fractured: Boolean, val handcrafted: Boolean = false, val alchemy: Boolean = false)

fun affixMarks(modifier: JsonObject, definitions: List<ModifierDefinition>): AffixMarks {
    val definition = definitions.firstOrNull { it.id == modifier.text("modifierId") }
    return AffixMarks(
        tier = modifier.text("tier").toIntOrNull() ?: 0,
        crafted = definition?.crafted == true,
        fractured = (modifier["fractured"] as? JsonPrimitive)?.booleanOrNull == true,
        // Since server 0.38.0: the smith's and cartographer's lines, and a map's alchemy — no orb touches either.
        handcrafted = definition?.source == com.sperance.exileforge.core.model.modifier.ModifierSource.HANDCRAFTED,
        alchemy = definition?.source == com.sperance.exileforge.core.model.modifier.ModifierSource.ALCHEMY,
    )
}

/**
 * A bench line as the sentence it would add, with the tier's range where the roll will land:
 * "+(70–79) to maximum Life". The same template a rolled modifier fills, filled with a range.
 */
fun recipeText(recipe: BenchRecipe, definitions: List<ModifierDefinition>): String {
    val definition = definitions.firstOrNull { it.id == recipe.modifierId }
    val ranges = recipe.values.mapIndexed { index, range ->
        val stat = definition?.effects?.getOrNull(index)?.stat.orEmpty()
        val low = statNumber(stat, range.valueMin); val high = statNumber(stat, range.valueMax)
        if (low == high) low else "($low–$high)"
    }
    val template = definition?.template?.takeIf { it != definition.code }
        ?: return ranges.joinToString(" · ").ifBlank { displayName(recipe.modifierCode) }
    return ranges.foldIndexed(template) { index, text, value -> text.replace("{$index}", value) }
}

/**
 * Characteristics whose meaning lives in the fraction.
 *
 * Everything else is printed whole, as Path of Exile prints it: a dot in front of a player is
 * noise when the number is armour or life. These are the exception because rounding them
 * destroys them — 1.25 attacks per second becomes 1, a 1.5 critical multiplier becomes 2, and
 * 0.4% leech becomes nothing at all.
 */
val preciseStats = setOf(
    "STOCK_ATTACK_SPEED", "STOCK_CAST_SPEED", "STOCK_CRITICAL_CHANCE",
    "STOCK_CRITICAL_MULTIPLIER", "STOCK_MOVEMENT_SPEED",
    // Leech lives below one percent: 0.4% printed whole is 0%, a modifier that seems to do nothing.
    "STOCK_LEECH_PHYSICAL", "STOCK_LEECH_MAGICAL", "STOCK_LEECH_ALL", "STOCK_CRITICAL_VAMPIRE",
)

/**
 * A server number as every screen prints it.
 *
 * The value itself is never rounded — it travels and is stored as the Double the server sent,
 * and the server counts with all of it. This is the last step before a string, and the only
 * place in the client that decides how many digits a player sees.
 */
fun statNumber(stat: String, value: Double): String =
    if (stat in preciseStats) String.format(java.util.Locale.ROOT, "%.2f", value)
    else Math.round(value).toString()

/** The same rule for a number that belongs to no particular characteristic. */
fun number(value: Double): String = statNumber("", value)

/** Title of a skill-tree node's grade, as the server sorts them. */
fun nodeTypeTitle(type: String, lang: Lang = uiLanguage) = uiOr(lang, "enum.node.$type", displayName(type, lang))

/**
 * Why the server refused to count an equipped item.
 *
 * The reasons arrive as the server writes them — "strength: need 30, have 14" — so the requirement
 * name is translated and the two numbers are printed untouched.
 */
fun requirementReason(reason: String, lang: Lang = uiLanguage): String {
    val name = reason.substringBefore(':').trim()
    val rest = reason.substringAfter(':', "").trim()
    val title = uiOr(lang, "req.$name", displayName(name, lang))
    if (rest.isBlank()) return title
    val need = Regex("need\\s+(-?\\d+)").find(rest)?.groupValues?.get(1)
    val have = Regex("have\\s+(-?\\d+)").find(rest)?.groupValues?.get(1)
    return if (need == null || have == null) "$title: $rest"
        else "$title: " + ui(lang, "req.reason", need, have)
}

/**
 * Title of a server stat enum. Names outside this table keep their humanised identifier, which is
 * language-neutral and still readable — the server owns the list and it grows without the client.
 */
fun statTitle(stat: String, lang: Lang = uiLanguage): String =
    uiOr(lang, "enum.stat.$stat", displayName(stat.substringAfter('_'), lang))
