package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.pick
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import kotlinx.serialization.json.*

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
    .replace(Regex("([a-z])([A-Z])"), "$1 $2").ifBlank { lang.pick("Без названия", "Unnamed") }

fun slotTitle(slot: String, lang: Lang = uiLanguage) = when (slot) {
    "HELMET" -> lang.pick("Шлем", "Helmet"); "BODY" -> lang.pick("Броня", "Body armour")
    "GLOVES" -> lang.pick("Перчатки", "Gloves"); "BOOTS" -> lang.pick("Сапоги", "Boots")
    "RING" -> lang.pick("Кольцо", "Ring"); "AMULET" -> lang.pick("Амулет", "Amulet")
    "BELT" -> lang.pick("Пояс", "Belt"); "SHIELD" -> lang.pick("Щит", "Shield")
    "WEAPON_1H" -> lang.pick("Одноручное", "One handed"); "WEAPON_2H" -> lang.pick("Двуручное", "Two handed")
    "QUIVER" -> lang.pick("Колчан", "Quiver"); "WINGS" -> lang.pick("Крылья", "Wings")
    // A jewel is not worn on the body: its "slot" is a socket on the passive tree.
    "JEWEL" -> lang.pick("Самоцвет", "Jewel")
    else -> displayName(slot, lang)
}

fun rarityTitle(value: String, lang: Lang = uiLanguage) = when (value) {
    "COMMON" -> lang.pick("Обычный", "Common"); "UNCOMMON" -> lang.pick("Необычный", "Uncommon")
    "RARE" -> lang.pick("Редкий", "Rare"); "EPIC" -> lang.pick("Эпический", "Epic")
    "UNIQUE" -> lang.pick("Уникальный", "Unique"); "MYTHICAL" -> lang.pick("Мифический", "Mythical")
    else -> displayName(value, lang)
}

fun weaponTitle(value: String, lang: Lang = uiLanguage) = when (value) {
    "SWORD" -> lang.pick("Меч", "Sword"); "LONGSWORD" -> lang.pick("Длинный меч", "Longsword")
    "BOW" -> lang.pick("Лук", "Bow"); "WAND" -> lang.pick("Жезл", "Wand")
    "AXE" -> lang.pick("Топор", "Axe"); "DOUBLEAXE" -> lang.pick("Двойной топор", "Double axe")
    "DOUBLESWORD" -> lang.pick("Двойной меч", "Double sword"); "BLADE" -> lang.pick("Клинок", "Blade")
    else -> displayName(value, lang)
}

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
        + instance.filterKeys { it in setOf("_id", "equipmentId", "params", "equippedSlot", "socketCode", "rarity", "corrupted") }
)

/**
 * The name of an equipment template, out of the locale bundle.
 *
 * Templates carry only a code since 0.14.0, so a card that has not had its template read yet, or a
 * code the dictionary does not know, falls back to something readable rather than to a raw key.
 */
fun equipmentTitle(template: JsonObject?): String {
    val code = template?.text("code").orEmpty()
    if (code.isBlank()) return tr("Предмет экипировки", "Equipment item")
    return locOr(LocaleKey.equipmentName(code), displayName(code))
}

/** The name of an `items` document, out of the locale bundle. */
fun itemTitle(document: JsonObject?): String {
    val code = document?.text("code").orEmpty()
    if (code.isBlank()) return tr("Предмет", "Item")
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
 * Characteristics whose meaning lives in the fraction.
 *
 * Everything else is printed whole, as Path of Exile prints it: a dot in front of a player is
 * noise when the number is armour or life. These five are the exception because rounding them
 * destroys them — 1.25 attacks per second becomes 1, and a 1.5 critical multiplier becomes 2.
 */
val preciseStats = setOf(
    "STOCK_ATTACK_SPEED", "STOCK_CAST_SPEED", "STOCK_CRITICAL_CHANCE",
    "STOCK_CRITICAL_MULTIPLIER", "STOCK_MOVEMENT_SPEED",
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

/**
 * Title of a server stat enum. Names outside this table keep their humanised identifier, which is
 * language-neutral and still readable — the server owns the list and it grows without the client.
 */
/** Title of a skill-tree node's grade, as the server sorts them. */
fun nodeTypeTitle(type: String, lang: Lang = uiLanguage) = when (type) {
    "START" -> lang.pick("Старт", "Start"); "SMALL" -> lang.pick("Малый", "Small")
    "NOTABLE" -> lang.pick("Нотабль", "Notable"); "KEYSTONE" -> lang.pick("Кейстоун", "Keystone")
    "JEWEL_SOCKET" -> lang.pick("Гнездо", "Socket")
    else -> displayName(type, lang)
}

/**
 * Why the server refused to count an equipped item.
 *
 * The reasons arrive as the server writes them — "strength: need 30, have 14" — so the requirement
 * name is translated and the two numbers are printed untouched.
 */
fun requirementReason(reason: String, lang: Lang = uiLanguage): String {
    val name = reason.substringBefore(':').trim()
    val rest = reason.substringAfter(':', "").trim()
    val title = when (name) {
        "level" -> lang.pick("Уровень", "Level"); "strength" -> lang.pick("Сила", "Strength")
        "dexterity" -> lang.pick("Ловкость", "Dexterity"); "intelligence" -> lang.pick("Интеллект", "Intelligence")
        else -> displayName(name, lang)
    }
    if (rest.isBlank()) return title
    val need = Regex("need\\s+(-?\\d+)").find(rest)?.groupValues?.get(1)
    val have = Regex("have\\s+(-?\\d+)").find(rest)?.groupValues?.get(1)
    return if (need == null || have == null) "$title: $rest"
        else "$title: " + lang.pick("нужно $need, есть $have", "need $need, have $have")
}

fun statTitle(stat: String, lang: Lang = uiLanguage): String = when (stat) {
    "STOCK_HEALTH" -> lang.pick("Здоровье", "Life"); "STOCK_MANA" -> lang.pick("Мана", "Mana")
    "STOCK_ENERGY" -> lang.pick("Энергия", "Energy"); "STOCK_ENERGY_SHIELD" -> lang.pick("Энергощит", "Energy shield")
    "STOCK_STRENGTH" -> lang.pick("Сила", "Strength"); "STOCK_AGILITY" -> lang.pick("Ловкость", "Agility")
    "STOCK_INTELLECT" -> lang.pick("Интеллект", "Intellect"); "STOCK_CONSTITUTION" -> lang.pick("Телосложение", "Constitution")
    "STOCK_ARMOR" -> lang.pick("Броня", "Armour"); "STOCK_EVASION" -> lang.pick("Уклонение", "Evasion")
    "STOCK_BLOCK_CHANCE" -> lang.pick("Шанс блока, %", "Block chance, %")
    "STOCK_ATTACK_PHYSICAL" -> lang.pick("Физический урон", "Physical damage"); "STOCK_ATTACK_MAGICAL" -> lang.pick("Магический урон", "Magical damage")
    "STOCK_ATTACK_FIRE" -> lang.pick("Урон огнём", "Fire damage"); "STOCK_ATTACK_COLD" -> lang.pick("Урон холодом", "Cold damage")
    "STOCK_ATTACK_LIGHTNING" -> lang.pick("Урон молнией", "Lightning damage"); "STOCK_ATTACK_CHAOS" -> lang.pick("Урон хаосом", "Chaos damage")
    "STOCK_CRITICAL_CHANCE" -> lang.pick("Шанс крита, %", "Critical chance, %"); "STOCK_CRITICAL_DAMAGE" -> lang.pick("Урон крита", "Critical damage")
    "STOCK_CRITICAL_MULTIPLIER" -> lang.pick("Множитель крита", "Critical multiplier")
    "STOCK_ATTACK_SPEED" -> lang.pick("Скорость атаки", "Attack speed"); "STOCK_CAST_SPEED" -> lang.pick("Скорость каста", "Cast speed")
    "STOCK_MOVEMENT_SPEED" -> lang.pick("Скорость движения", "Movement speed")
    "STOCK_RESIST_FIRE" -> lang.pick("Сопротивление огню, %", "Fire resistance, %"); "STOCK_RESIST_COLD" -> lang.pick("Сопротивление холоду, %", "Cold resistance, %")
    "STOCK_RESIST_LIGHTNING" -> lang.pick("Сопротивление молнии, %", "Lightning resistance, %"); "STOCK_RESIST_CHAOS" -> lang.pick("Сопротивление хаосу, %", "Chaos resistance, %")
    "STOCK_RESIST_ALL" -> lang.pick("Все сопротивления, %", "All resistances, %")
    "STOCK_HEALTH_REGEN" -> lang.pick("Реген здоровья", "Life regeneration"); "STOCK_MANA_REGEN" -> lang.pick("Реген маны", "Mana regeneration")
    "STOCK_RARITY" -> lang.pick("Редкость добычи, %", "Item rarity, %"); "STOCK_QUANTITY" -> lang.pick("Количество добычи, %", "Item quantity, %")
    "STOCK_STUN_THRESHOLD" -> lang.pick("Порог оглушения", "Stun threshold"); "STOCK_ENERGY_REGEN" -> lang.pick("Реген энергии", "Energy regeneration")
    "STOCK_CRITICAL_VAMPIRE" -> lang.pick("Вампиризм крита", "Critical leech")
    "STOCK_LEECH_PHYSICAL" -> lang.pick("Вампиризм физический", "Physical leech"); "STOCK_LEECH_MAGICAL" -> lang.pick("Вампиризм магический", "Magical leech")
    "STOCK_LEECH_ALL" -> lang.pick("Вампиризм общий", "Total leech"); "STOCK_INVENTORY_SIZE" -> lang.pick("Размер инвентаря", "Inventory size")
    "STOCK_AURA_EFFECT" -> lang.pick("Эффект аур, %", "Aura effect, %"); "STOCK_CURSE_EFFECT" -> lang.pick("Эффект проклятий, %", "Curse effect, %")
    "STOCK_CAST_STRENGTH" -> lang.pick("Сила заклинаний", "Cast strength")
    "STOCK_GOLD" -> lang.pick("Золото", "Gold"); "STOCK_EXPERIENCE" -> lang.pick("Опыт", "Experience")
    else -> displayName(stat.substringAfter('_'), lang)
}
