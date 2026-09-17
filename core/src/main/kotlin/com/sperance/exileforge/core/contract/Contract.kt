package com.sperance.exileforge.core.contract

import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.core.editor.formSchema
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierSource
import kotlinx.serialization.json.*
import com.sperance.exileforge.core.i18n.tr

val WireJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
val JsonObject.entityId: String get() = text("_id").ifBlank { text("id") }
val protectedFields = setOf("_id", "id", "version", "deleted", "createdAt", "updatedAt", "type")


val rarities = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHICAL")
val slots = listOf("HELMET", "BODY", "GLOVES", "RING", "BOOTS", "WINGS", "BELT", "WEAPON_1H", "WEAPON_2H", "QUIVER", "SHIELD", "AMULET")
val weapons = listOf("SWORD", "LONGSWORD", "BOW", "WAND", "AXE", "DOUBLEAXE", "DOUBLESWORD", "BLADE")
val modifierSources = ModifierSource.entries.map { it.name }
const val SERVER_COMMIT = "5d4015ad138088142902d822a8d67580424be404"
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
    put("name", if (catalog == Catalog.ITEMS) tr("Осколок древних", "Shard of the Ancients") else tr("Наследие изгнанника", "Exile's Legacy"))
    put("description", tr("Тестовый предмет Exile Forge", "Exile Forge test item"))
    put("image", JsonNull)
    if (catalog == Catalog.ITEMS) {
        put("category", "Currency"); put("subCategory", "Shard"); put("price", 1L)
    } else {
        put("type", "features.data.equipment.equipment_data.${kind.name}")
        put("slot", when (kind) { EquipmentKind.Weapon -> "WEAPON_1H"; EquipmentKind.Armor -> "BODY"; EquipmentKind.Accessory -> "RING" })
        put("rarity", "RARE"); put("itemLevel", 30)
        put("modifierDefinitionRefs", JsonArray(emptyList()))
        put("stockModifierDefinitionRefs", JsonArray(emptyList()))
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
    if (catalog == Catalog.CHARACTERS) return
    validateIcon(document)
    if (catalog == Catalog.ITEMS) {
        require(document.text("category").isNotBlank()) { tr("Введите категорию", "Enter a category") }
        require(document.text("subCategory").isNotBlank()) { tr("Введите подкатегорию", "Enter a sub-category") }
        require((document["price"] as? JsonPrimitive)?.longOrNull?.let { it >= 0 } == true) { tr("Цена должна быть целым неотрицательным числом", "The price must be a non-negative whole number") }
    } else {
        require(document.text("rarity") in rarities) { tr("Неизвестная редкость", "Unknown rarity") }
        require(document.text("slot") in slots) { tr("Неизвестный слот", "Unknown slot") }
        require((document["itemLevel"] as? JsonPrimitive)?.intOrNull?.let { it >= 1 } == true) { tr("Уровень должен быть целым числом от 1", "The level must be a whole number of 1 or more") }
        val kind = document.text("type").substringAfterLast('.')
        require(kind in EquipmentKind.entries.map { it.name }) { tr("Неизвестный тип экипировки", "Unknown equipment type") }
        if (kind == "Weapon") {
            require(document.text("weaponType") in weapons) { tr("Неизвестный тип оружия", "Unknown weapon type") }
            val min = document.text("damage_min").toDoubleOrNull()
            val max = document.text("damage_max").toDoubleOrNull()
            require(min != null && max != null && min.isFinite() && max.isFinite() && min >= 0 && max >= min) { tr("Проверьте диапазон урона", "Check the damage range") }
            require(document.text("attackSpeed").toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true) { tr("Скорость атаки должна быть больше 0", "Attack speed must be greater than 0") }
            require(document.text("durability").toIntOrNull()?.let { it >= 0 } == true) { tr("Прочность должна быть целой и неотрицательной", "Durability must be a non-negative whole number") }
        }
        if (kind == "Armor") require(document.text("defense").toIntOrNull()?.let { it >= 0 } == true) { tr("Защита должна быть целой и неотрицательной", "Defence must be a non-negative whole number") }
        val mods = document["modifiers"]
        require(mods == null || mods == JsonNull || mods is JsonArray) { tr("modifiers должен быть массивом", "modifiers must be an array") }
        (mods as? JsonArray)?.forEach { element ->
            val mod = element as? JsonObject ?: error(tr("Модификатор должен быть объектом", "A modifier must be an object"))
            validateModifier(mod)
        }
        listOf("modifierDefinitions", "modifierDefinitionsStock").forEach { key ->
            val values = document[key]
            require(values == null || values == JsonNull || values is JsonArray) { tr("$key должен быть массивом объектов", "$key must be an array of objects") }
            (values as? JsonArray)?.forEach { element ->
                val definition = element as? JsonObject ?: throw IllegalArgumentException(tr("$key: ожидается объект определения", "$key: a definition object is expected"))
                val parsed = WireJson.decodeFromJsonElement(ModifierDefinition.serializer(), definition)
                require(parsed.id.isNotBlank() && parsed.name.isNotBlank()) { tr("Укажите id и name определения", "Set the id and name of the definition") }
            }
        }
    }
}

/** The icon is a reference into the server set, never a free string or a foreign URL. */
fun validateIcon(document: JsonObject) {
    val icon = document["icon"]
    if (icon == null || icon == JsonNull) return
    val id = (icon as? JsonPrimitive)?.takeIf { it.isString }?.content ?: error(tr("Иконка: требуется идентификатор набора", "Icon: a set identifier is required"))
    require(Regex("[a-z0-9-]{3,48}").matches(id)) { tr("Иконка: идентификатор из строчных букв, цифр и дефисов", "Icon: lower-case letters, digits and hyphens only") }
    val known = com.sperance.exileforge.core.display.icons.iconSuggestions
    require(known.isEmpty() || id in known) { tr("Иконки «$id» нет в наборе сервера", "The server set has no icon \"$id\"") }
}

fun validateModifier(document: JsonObject) {
    require("type" !in document && "value" !in document) { tr("Старый формат модификатора: нужны definitionId и values", "Legacy modifier format: definitionId and values are required") }
    val modifier = WireJson.decodeFromJsonElement(Modifier.serializer(), document)
    require(modifier.definitionId.isNotBlank()) { tr("Введите definitionId", "Enter a definitionId") }
    require(modifier.tier > 0) { tr("Tier должен быть больше 0", "Tier must be greater than 0") }
    require(modifier.values.all { it.value.isFinite() }) { tr("Значения должны быть конечными числами", "Values must be finite numbers") }
}

fun validateReferenceWrite(document: JsonObject) {
    listOf("modifierDefinitions", "modifierDefinitionsStock").forEach { key ->
        require(document[key] == null || document[key] == JsonNull || document[key] == JsonArray(emptyList())) { tr("Определения хранятся отдельно. Обновите сервер и загрузите предмет повторно.", "Definitions are stored separately. Update the server and reload the item.") }
    }
    listOf("modifierDefinitionRefs", "stockModifierDefinitionRefs").forEach { key ->
        document[key]?.let { value ->
            require(value is JsonArray) { tr("$key: требуется список ссылок", "$key: a list of references is required") }
            value.forEach { raw ->
                val ref = raw.jsonObject
                require(ref.text("definitionId").isNotBlank() && (ref["revision"] as? JsonPrimitive)?.intOrNull?.let { it > 0 } == true) { tr("Выберите определение и положительную версию", "Choose a definition and a positive revision") }
            }
        }
    }
}
fun definitionKey(definition: JsonObject) = definition.text("id") + "@" + definition.text("revision").ifBlank { "1" }
fun referenceKey(reference: JsonObject, revisionField: String = "revision") = reference.text("definitionId") + "@" + reference.text(revisionField).ifBlank { "1" }

val JsonObject.entityVersion: Long get() = get("version")?.jsonPrimitive?.longOrNull?.takeIf { it >= 0 } ?: error(tr("Сервер не вернул версию записи. Обновите данные.", "The server returned no record version. Refresh the data."))

fun editableFields(catalog: Catalog): Set<String> = when(catalog) {
    Catalog.CHARACTERS -> setOf("name", "description")
    Catalog.ITEMS -> setOf("name", "description", "image", "icon", "category", "subCategory", "price")
    Catalog.EQUIPMENT -> setOf("name", "description", "image", "icon", "slot", "rarity", "itemLevel", "weaponType", "damage_min", "damage_max", "attackSpeed", "durability", "defense", "price", "modifierDefinitionRefs", "stockModifierDefinitionRefs")
}

fun com.sperance.exileforge.core.model.hero.EquipmentInstance.text(key: String): String = document().text(key)
