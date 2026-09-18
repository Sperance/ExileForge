package com.sperance.exileforge.core.contract

import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.core.editor.formSchema
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.model.modifier.Modifier
import kotlinx.serialization.json.*

val WireJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
val JsonObject.entityId: String get() = text("_id").ifBlank { text("id") }
/** Fields MongoDB owns. `type` is the polymorphic discriminator: allowed on create, never on update. */
val protectedFields = setOf("_id", "id", "version", "deleted", "createdAt", "updatedAt", "type")

val rarities = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "UNIQUE", "MYTHICAL")
val slots = listOf("HELMET", "BODY", "GLOVES", "RING", "BOOTS", "WINGS", "BELT", "WEAPON_1H", "WEAPON_2H", "QUIVER", "SHIELD", "AMULET")
val weapons = listOf("SWORD", "LONGSWORD", "BOW", "WAND", "AXE", "DOUBLEAXE", "DOUBLESWORD", "BLADE")
val modifierSources = listOf("IMPLICIT", "PREFIX", "SUFFIX", "UNIQUE", "ENCHANTMENT", "CORRUPTION")
val modifierOperations = listOf("ADD", "INCREASED", "MORE", "SET")
const val SERVER_COMMIT = "e8e9ae824dda7484622462da892c636c6369be6b"
const val SERVER_BRANCH = "claude/tender-pasteur-a36kj2"
const val SERVER_VERSION = "0.9.0"

fun template(catalog: Catalog, kind: EquipmentKind = EquipmentKind.Weapon): JsonObject = when (catalog) {
    Catalog.CHARACTERS -> defaultObject("character")
    Catalog.ITEMS -> buildJsonObject {
        put("name", tr("Осколок древних", "Shard of the Ancients"))
        put("description", tr("Тестовый предмет Exile Forge", "Exile Forge test item"))
        put("image", JsonNull); put("category", "STONE_STOCK"); put("subCategory", "STONE"); put("price", 1L)
    }
    Catalog.EQUIPMENT -> buildJsonObject {
        put("type", kind.type)
        put("name", tr("Наследие изгнанника", "Exile's Legacy"))
        put("description", tr("Тестовый предмет Exile Forge", "Exile Forge test item"))
        put("image", JsonNull)
        put("slot", when (kind) { EquipmentKind.Weapon -> "WEAPON_1H"; EquipmentKind.Armor -> "BODY"; EquipmentKind.Accessory -> "RING" })
        put("rarity", "RARE"); put("itemLevel", 30)
        put("modifierIds", JsonArray(emptyList()))
        when (kind) {
            EquipmentKind.Weapon -> { put("weaponType", "SWORD"); put("damage_min", 10.0); put("damage_max", 20.0); put("attackSpeed", 1.2); put("durability", 100) }
            EquipmentKind.Armor -> put("defense", 50)
            EquipmentKind.Accessory -> Unit
        }
    }
}

fun requireId(id: String) { require(Regex("[0-9a-fA-F]{24}").matches(id)) { tr("ID должен содержать 24 шестнадцатеричных символа", "The id must be 24 hexadecimal characters") } }

/** Only changed mutable fields: never sends the polymorphic discriminator to MongoDB. */
fun diff(original: JsonObject, edited: JsonObject): JsonObject = JsonObject(
    edited.filter { (key, value) -> key !in protectedFields && original[key] != value }
)

fun validate(document: JsonObject, catalog: Catalog) {
    require(document.text("name").isNotBlank()) { tr("Введите название", "Enter a name") }
    validateForm(formSchema(catalog), document)
    when (catalog) {
        Catalog.CHARACTERS -> validateCharacter(document)
        Catalog.ITEMS -> {
            require(document.text("category").isNotBlank()) { tr("Введите категорию", "Enter a category") }
            require(document.text("subCategory").isNotBlank()) { tr("Введите подкатегорию", "Enter a sub-category") }
            require((document["price"] as? JsonPrimitive)?.longOrNull?.let { it >= 0 } == true) { tr("Цена должна быть целым неотрицательным числом", "The price must be a non-negative whole number") }
        }
        Catalog.EQUIPMENT -> validateEquipment(document)
    }
}

private fun validateEquipment(document: JsonObject) {
    require(document.text("rarity") in rarities) { tr("Неизвестная редкость", "Unknown rarity") }
    require(document.text("slot") in slots) { tr("Неизвестный слот", "Unknown slot") }
    require((document["itemLevel"] as? JsonPrimitive)?.intOrNull?.let { it >= 1 } == true) { tr("Уровень должен быть целым числом от 1", "The level must be a whole number of 1 or more") }
    val kind = requireNotNull(EquipmentKind.of(document.text("type"))) { tr("Неизвестный тип экипировки", "Unknown equipment type") }
    when (kind) {
        EquipmentKind.Weapon -> {
            require(document.text("weaponType") in weapons) { tr("Неизвестный тип оружия", "Unknown weapon type") }
            val min = document.text("damage_min").toDoubleOrNull()
            val max = document.text("damage_max").toDoubleOrNull()
            require(min != null && max != null && min.isFinite() && max.isFinite() && min >= 0 && max >= min) { tr("Проверьте диапазон урона", "Check the damage range") }
            require(document.text("attackSpeed").toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true) { tr("Скорость атаки должна быть больше 0", "Attack speed must be greater than 0") }
            require(document.text("durability").toIntOrNull()?.let { it >= 0 } == true) { tr("Прочность должна быть целой и неотрицательной", "Durability must be a non-negative whole number") }
        }
        EquipmentKind.Armor -> require(document.text("defense").toIntOrNull()?.let { it >= 0 } == true) { tr("Защита должна быть целой и неотрицательной", "Defence must be a non-negative whole number") }
        EquipmentKind.Accessory -> Unit
    }
    validateModifierPool(document)
}

/**
 * A template keeps a pool of references, never inline definitions and never rolled values.
 *
 * Which of them land on an instance is the server's decision: prefixes and suffixes are rolled in
 * the count the rarity allows, the other sources sit on every copy.
 */
fun validateModifierPool(document: JsonObject) {
    val pool = document["modifierIds"] ?: return
    require(pool is JsonArray) { tr("modifierIds: требуется список ссылок", "modifierIds: a list of references is required") }
    pool.forEach { requireId((it as? JsonPrimitive)?.contentOrNull.orEmpty()) }
    require("params" !in document) { tr("Зароленные модификаторы принадлежат экземпляру, а не шаблону", "Rolled modifiers belong to an instance, not to a template") }
}

fun validateModifier(document: JsonObject) {
    val modifier = WireJson.decodeFromJsonElement(Modifier.serializer(), document)
    requireId(modifier.modifierId)
    require(modifier.tier > 0) { tr("Tier должен быть больше 0", "Tier must be greater than 0") }
    require(modifier.values.all { it.isFinite() }) { tr("Значения должны быть конечными числами", "Values must be finite numbers") }
}

/**
 * The server derives the version itself on PUT and DELETE, but the client still refuses to write a
 * document it never read a version from — that is the signal the record was fetched, not invented.
 */
val JsonObject.entityVersion: Long get() = get("version")?.jsonPrimitive?.longOrNull?.takeIf { it >= 0 }
    ?: error(tr("Сервер не вернул версию записи. Обновите данные.", "The server returned no record version. Refresh the data."))

fun editableFields(catalog: Catalog): Set<String> = when (catalog) {
    Catalog.CHARACTERS -> setOf("name", "description")
    Catalog.ITEMS -> setOf("name", "description", "image", "category", "subCategory", "price")
    Catalog.EQUIPMENT -> setOf("name", "description", "image", "slot", "rarity", "itemLevel", "weaponType", "damage_min", "damage_max", "attackSpeed", "durability", "defense", "modifierIds")
}

/** Fields only accepted when the record is created; afterwards they are the server's. */
fun creationFields(catalog: Catalog): Set<String> = when (catalog) {
    Catalog.CHARACTERS -> setOf("userId")
    Catalog.EQUIPMENT -> setOf("type")
    Catalog.ITEMS -> emptySet()
}
