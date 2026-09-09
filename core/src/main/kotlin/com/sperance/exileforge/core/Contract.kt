package com.sperance.exileforge.core

import kotlinx.serialization.json.*

val WireJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
val JsonObject.entityId: String get() = text("_id")
val protectedFields = setOf("_id", "id", "version", "deleted", "createdAt", "updatedAt", "type")
enum class Catalog(val path: String, val title: String) {
    ITEMS("items", "Предметы"), EQUIPMENT("equipment", "Экипировка"), CHARACTERS("character", "Персонажи")
}
enum class EquipmentKind { Weapon, Armor, Accessory }
val rarities = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHICAL")
val slots = listOf("HELMET", "BODY", "GLOVES", "RING", "BOOTS", "WINGS", "BELT", "WEAPON_1H", "WEAPON_2H", "QUIVER", "SHIELD", "AMULET")
val weapons = listOf("SWORD", "LONGSWORD", "BOW", "WAND", "AXE", "DOUBLEAXE", "DOUBLESWORD", "BLADE")
val modifierSources = ModifierSource.entries.map { it.name }
const val SERVER_COMMIT = "5fb037f3ba6a60f5e45da9da35832e2165339432"
fun starterDefinition(): JsonObject = WireJson.parseToJsonElement("""{
    "id":"life", "name":"Maximum life", "source":"PREFIX", "scope":"ITEM", "affixType":"PREFIX",
    "tiers":[{"tier":1,"minItemLevel":1,"weight":100,"values":[{"min":1.0,"max":100.0}]}],
    "effects":[{"type":"stat","stat":"life","operation":"FLAT","value":{"type":"modifier_value","index":0}}]
}""").jsonObject
fun starterModifier(value: Double = 42.0): JsonObject = buildJsonObject {
    put("definitionId", "life"); put("tier", 1); put("source", "PREFIX")
    put("values", buildJsonArray { add(buildJsonObject { put("value", value) }) })
    put("tags", JsonArray(emptyList()))
}
fun template(catalog: Catalog, kind: EquipmentKind = EquipmentKind.Weapon): JsonObject = if (catalog == Catalog.CHARACTERS) defaultObject("character") else buildJsonObject {
    put("name", if (catalog == Catalog.ITEMS) "Осколок древних" else "Наследие изгнанника")
    put("description", "Тестовый предмет Exile Forge")
    put("image", JsonNull)
    if (catalog == Catalog.ITEMS) {
        put("category", "Currency"); put("subCategory", "Shard"); put("price", 1L)
    } else {
        put("type", "features.data.equipment.equipment_data.${kind.name}")
        put("slot", when (kind) { EquipmentKind.Weapon -> "WEAPON_1H"; EquipmentKind.Armor -> "BODY"; EquipmentKind.Accessory -> "RING" })
        put("rarity", "RARE"); put("itemLevel", 30)
        put("modifiers", JsonArray(emptyList()))
        put("modifierDefinitions", JsonArray(listOf(starterDefinition())))
        put("modifierDefinitionsStock", JsonArray(emptyList()))
        when (kind) {
            EquipmentKind.Weapon -> { put("weaponType", "SWORD"); put("damage_min", 10.0); put("damage_max", 20.0); put("attackSpeed", 1.2); put("durability", 100) }
            EquipmentKind.Armor -> put("defense", 50)
            EquipmentKind.Accessory -> Unit
        }
    }
}
fun requireId(id: String) { require(Regex("[0-9a-fA-F]{24}").matches(id)) { "ID должен содержать 24 шестнадцатеричных символа" } }
/** Only changed mutable fields: never sends the polymorphic discriminator to MongoDB. */
fun diff(original: JsonObject, edited: JsonObject): JsonObject = JsonObject(
    edited.filter { (key, value) -> key !in protectedFields && original[key] != value }
)
fun validate(document: JsonObject, catalog: Catalog) {
    require(document.text("name").isNotBlank()) { "Введите название" }
    validateForm(formSchema(catalog), document)
    if (catalog == Catalog.CHARACTERS) { validateCharacter(document); return }
    if (catalog == Catalog.ITEMS) {
        require(document.text("category").isNotBlank()) { "Введите категорию" }
        require(document.text("subCategory").isNotBlank()) { "Введите подкатегорию" }
        require((document["price"] as? JsonPrimitive)?.longOrNull?.let { it >= 0 } == true) { "Цена должна быть целым неотрицательным числом" }
    } else {
        require(document.text("rarity") in rarities) { "Неизвестная редкость" }
        require(document.text("slot") in slots) { "Неизвестный слот" }
        require((document["itemLevel"] as? JsonPrimitive)?.intOrNull?.let { it >= 1 } == true) { "Уровень должен быть целым числом от 1" }
        val kind = document.text("type").substringAfterLast('.')
        require(kind in EquipmentKind.entries.map { it.name }) { "Неизвестный тип экипировки" }
        if (kind == "Weapon") {
            require(document.text("weaponType") in weapons) { "Неизвестный тип оружия" }
            val min = document.text("damage_min").toDoubleOrNull()
            val max = document.text("damage_max").toDoubleOrNull()
            require(min != null && max != null && min.isFinite() && max.isFinite() && min >= 0 && max >= min) { "Проверьте диапазон урона" }
            require(document.text("attackSpeed").toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true) { "Скорость атаки должна быть больше 0" }
            require(document.text("durability").toIntOrNull()?.let { it >= 0 } == true) { "Прочность должна быть целой и неотрицательной" }
        }
        if (kind == "Armor") require(document.text("defense").toIntOrNull()?.let { it >= 0 } == true) { "Защита должна быть целой и неотрицательной" }
        val mods = document["modifiers"]
        require(mods == null || mods == JsonNull || mods is JsonArray) { "modifiers должен быть массивом" }
        (mods as? JsonArray)?.forEach { element ->
            val mod = element as? JsonObject ?: error("Модификатор должен быть объектом")
            validateModifier(mod)
        }
        listOf("modifierDefinitions", "modifierDefinitionsStock").forEach { key ->
            val values = document[key]
            require(values == null || values == JsonNull || values is JsonArray) { "$key должен быть массивом объектов" }
            (values as? JsonArray)?.forEach { element ->
                val definition = element as? JsonObject ?: throw IllegalArgumentException("$key: ожидается объект определения")
                val parsed = WireJson.decodeFromJsonElement(ModifierDefinition.serializer(), definition)
                require(parsed.id.isNotBlank() && parsed.name.isNotBlank()) { "Укажите id и name определения" }
            }
        }
    }
}

fun validateModifier(document: JsonObject) {
    require("type" !in document && "value" !in document) { "Старый формат модификатора: нужны definitionId и values" }
    val modifier = WireJson.decodeFromJsonElement(Modifier.serializer(), document)
    require(modifier.definitionId.isNotBlank()) { "Введите definitionId" }
    require(modifier.tier > 0) { "Tier должен быть больше 0" }
    require(modifier.values.all { it.value.isFinite() }) { "Значения должны быть конечными числами" }
}
