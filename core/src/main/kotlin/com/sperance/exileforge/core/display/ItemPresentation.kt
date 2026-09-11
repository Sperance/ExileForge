package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import kotlinx.serialization.json.*

fun itemVisualKind(doc: JsonObject): ItemVisualKind = when {
    doc["userId"] != null -> ItemVisualKind.CHARACTER
    doc.text("weaponType") == "BOW" || doc.text("slot") == "QUIVER" -> ItemVisualKind.BOW
    doc.text("weaponType") == "WAND" -> ItemVisualKind.STAFF
    doc.text("slot").startsWith("WEAPON") || doc.text("type").endsWith("Weapon") -> ItemVisualKind.SWORD
    else -> when(doc.text("slot")) {
        "HELMET" -> ItemVisualKind.HELMET; "BODY" -> ItemVisualKind.ARMOR
        "GLOVES" -> ItemVisualKind.GLOVES; "BOOTS" -> ItemVisualKind.BOOTS
        "RING" -> ItemVisualKind.RING; "AMULET" -> ItemVisualKind.AMULET
        "BELT" -> ItemVisualKind.BELT; "SHIELD" -> ItemVisualKind.SHIELD; "WINGS" -> ItemVisualKind.WINGS
        else -> if(doc.text("category") in setOf("Currency", "POE")) ItemVisualKind.CURRENCY else ItemVisualKind.ITEM
    }
}
fun displayName(value: String): String = value.substringAfterLast('/').substringAfterLast('.').replace('_', ' ')
    .replace(Regex("([a-z])([A-Z])"), "$1 $2").ifBlank { "Без названия" }
fun slotTitle(slot: String) = when(slot) {
    "HELMET" -> "Шлем"; "BODY" -> "Броня"; "GLOVES" -> "Перчатки"; "BOOTS" -> "Сапоги"
    "RING" -> "Кольцо"; "AMULET" -> "Амулет"; "BELT" -> "Пояс"; "SHIELD" -> "Щит"
    "WEAPON_1H" -> "Одноручное"; "WEAPON_2H" -> "Двуручное"; "QUIVER" -> "Колчан"; "WINGS" -> "Крылья"
    else -> displayName(slot)
}
fun rarityTitle(value: String) = when(value) {
    "COMMON", "NORMAL" -> "Обычный"; "UNCOMMON", "MAGIC" -> "Магический"; "RARE" -> "Редкий"
    "EPIC" -> "Эпический"; "LEGENDARY" -> "Легендарный"; "UNIQUE" -> "Уникальный"; "MYTHICAL" -> "Мифический"
    else -> displayName(value)
}
/** Display projection only. Never send this combined document back to a template endpoint. */
fun inventoryDocument(instance: JsonObject, base: JsonObject?): JsonObject {
    val poe = instance["poe"] as? JsonObject
    return JsonObject(base.orEmpty() + mapOf(
        "_id" to (instance["uuid"] ?: JsonPrimitive("")),
        "name" to (base?.get("name") ?: JsonPrimitive(displayName(poe?.text("baseId").orEmpty()).takeIf { it != "Без названия" } ?: "Предмет экипировки")),
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
