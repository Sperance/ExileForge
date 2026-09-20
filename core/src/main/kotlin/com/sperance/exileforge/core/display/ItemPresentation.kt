package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.pick
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
        + mapOf("name" to (base?.get("name") ?: JsonPrimitive(tr("Предмет экипировки", "Equipment item"))))
        + instance.filterKeys { it in setOf("_id", "equipmentId", "params", "equippedSlot", "rarity", "corrupted") }
)

fun inventoryDocument(instance: com.sperance.exileforge.core.model.hero.EquipmentInstance, base: JsonObject?): JsonObject =
    inventoryDocument(instance.document(), base)

/** The title of a rolled modifier: the description's name, else its stable code. */
fun modifierTitle(modifier: JsonObject, definitions: List<ModifierDefinition>): String {
    val id = modifier.text("modifierId")
    val definition = definitions.firstOrNull { it.id == id }
    return definition?.title?.takeIf { it.isNotBlank() } ?: displayName(id)
}

/** What a rolled modifier changed: one value per effect of its description, in the same order. */
fun modifierValues(modifier: JsonObject, definitions: List<ModifierDefinition> = emptyList()): String {
    val effects = definitions.firstOrNull { it.id == modifier.text("modifierId") }?.effects.orEmpty()
    return (modifier["values"] as? JsonArray).orEmpty().mapIndexed { index, value ->
        val number = (value as? JsonPrimitive)?.doubleOrNull
        val printed = if (number == null) (value as? JsonPrimitive)?.content.orEmpty()
            else if (number == number.toLong().toDouble()) number.toLong().toString() else number.toString()
        effects.getOrNull(index)?.let { "$printed ${statTitle(it.stat)}" } ?: printed
    }.joinToString(" · ")
}

/**
 * Title of a server stat enum. Names outside this table keep their humanised identifier, which is
 * language-neutral and still readable — the server owns the list and it grows without the client.
 */
/** Title of a skill-tree node's grade, as the server sorts them. */
fun nodeTypeTitle(type: String, lang: Lang = uiLanguage) = when (type) {
    "START" -> lang.pick("Старт", "Start"); "SMALL" -> lang.pick("Малый", "Small")
    "NOTABLE" -> lang.pick("Нотабль", "Notable"); "KEYSTONE" -> lang.pick("Кейстоун", "Keystone")
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
