package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.pick
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.i18n.uiLanguage
import kotlinx.serialization.json.*

fun itemVisualKind(doc: JsonObject): ItemVisualKind = when {
    doc["userId"] != null -> ItemVisualKind.CHARACTER
    doc.text("weaponType") == "BOW" || doc.text("slot") == "QUIVER" -> ItemVisualKind.BOW
    doc.text("weaponType") == "WAND" || doc.text("weaponType") == "SCEPTRE" -> ItemVisualKind.WAND
    doc.text("weaponType") == "STAFF" -> ItemVisualKind.STAFF
    doc.text("weaponType") == "AXE" -> ItemVisualKind.AXE
    doc.text("weaponType") == "MACE" -> ItemVisualKind.MACE
    doc.text("weaponType") == "DAGGER" || doc.text("weaponType") == "CLAW" -> ItemVisualKind.DAGGER
    doc.text("slot").startsWith("WEAPON") || doc.text("type").endsWith("Weapon") -> ItemVisualKind.SWORD
    else -> when(doc.text("slot")) {
        "HELMET" -> ItemVisualKind.HELMET; "BODY" -> ItemVisualKind.ARMOR
        "GLOVES" -> ItemVisualKind.GLOVES; "BOOTS" -> ItemVisualKind.BOOTS
        "RING" -> ItemVisualKind.RING; "AMULET" -> ItemVisualKind.AMULET
        "BELT" -> ItemVisualKind.BELT; "SHIELD" -> ItemVisualKind.SHIELD; "WINGS" -> ItemVisualKind.WINGS
        else -> when {
            doc.text("subCategory") == "Flask" || doc.text("category") == "Flask" -> ItemVisualKind.FLASK
            doc.text("subCategory") == "Gem" || doc.text("category") == "Gem" -> ItemVisualKind.GEM
            doc.text("subCategory") == "Map" || doc.text("category") == "Map" -> ItemVisualKind.MAP
            doc.text("subCategory") == "Scroll" -> ItemVisualKind.SCROLL
            doc.text("category") in setOf("Currency", "POE") -> ItemVisualKind.CURRENCY
            else -> ItemVisualKind.ITEM
        }
    }
}
fun displayName(value: String, lang: Lang = uiLanguage): String = value.substringAfterLast('/').substringAfterLast('.').replace('_', ' ')
    .replace(Regex("([a-z])([A-Z])"), "$1 $2").ifBlank { lang.pick("Без названия", "Unnamed") }
fun slotTitle(slot: String, lang: Lang = uiLanguage) = when(slot) {
    "HELMET" -> lang.pick("Шлем", "Helmet"); "BODY" -> lang.pick("Броня", "Body armour")
    "GLOVES" -> lang.pick("Перчатки", "Gloves"); "BOOTS" -> lang.pick("Сапоги", "Boots")
    "RING" -> lang.pick("Кольцо", "Ring"); "AMULET" -> lang.pick("Амулет", "Amulet")
    "BELT" -> lang.pick("Пояс", "Belt"); "SHIELD" -> lang.pick("Щит", "Shield")
    "WEAPON_1H" -> lang.pick("Одноручное", "One handed"); "WEAPON_2H" -> lang.pick("Двуручное", "Two handed")
    "QUIVER" -> lang.pick("Колчан", "Quiver"); "WINGS" -> lang.pick("Крылья", "Wings")
    else -> displayName(slot, lang)
}
fun rarityTitle(value: String, lang: Lang = uiLanguage) = when(value) {
    "COMMON", "NORMAL" -> lang.pick("Обычный", "Normal"); "UNCOMMON", "MAGIC" -> lang.pick("Магический", "Magic")
    "RARE" -> lang.pick("Редкий", "Rare"); "EPIC" -> lang.pick("Эпический", "Epic")
    "LEGENDARY" -> lang.pick("Легендарный", "Legendary"); "UNIQUE" -> lang.pick("Уникальный", "Unique")
    "MYTHICAL" -> lang.pick("Мифический", "Mythical")
    else -> displayName(value, lang)
}
/** Display projection only. Never send this combined document back to a template endpoint. */
fun inventoryDocument(instance: JsonObject, base: JsonObject?): JsonObject {
    val resolvedBase = instance["baseSnapshot"] as? JsonObject ?: base
    val poe = instance["poe"] as? JsonObject
    return JsonObject(resolvedBase.orEmpty() + mapOf(
        "_id" to (instance["uuid"] ?: JsonPrimitive("")),
        "name" to (resolvedBase?.get("name") ?: JsonPrimitive(displayName(poe?.text("baseId").orEmpty()).takeIf { it != tr("Без названия", "Unnamed") } ?: tr("Предмет экипировки", "Equipment item"))),
        "modifiers" to (instance["params"] ?: JsonArray(emptyList()))
    ) + (poe?.filterKeys { it in setOf("rarity", "itemLevel", "quality", "corrupted", "mirrored", "implicits", "explicits") }.orEmpty()))
}
fun modifierTitle(mod: JsonObject, definitions: List<JsonObject>): String {
    val id = mod.text("definitionId").ifBlank { mod.text("id") }
    val revision = mod.text("definitionRevision").ifBlank { mod.text("revision").ifBlank { "1" } }
    return definitions.firstOrNull { it.text("id") == id && it.text("revision").ifBlank { "1" } == revision }?.text("name")?.takeIf { it.isNotBlank() } ?: displayName(id)
}
fun modifierValues(mod: JsonObject): String = (mod["values"] as? JsonArray).orEmpty().joinToString(" / ") {
    val value = if(it is JsonObject) it["value"] else it
    val n = (value as? JsonPrimitive)?.doubleOrNull
    if(n != null && n.isFinite() && n == n.toLong().toDouble()) n.toLong().toString() else (value as? JsonPrimitive)?.content.orEmpty()
}

fun inventoryDocument(instance: com.sperance.exileforge.core.model.hero.EquipmentInstance, base: JsonObject?): JsonObject = inventoryDocument(instance.document(), base)
