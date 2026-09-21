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
val modifierSources = listOf("IMPLICIT", "PREFIX", "SUFFIX", "UNIQUE", "ENCHANTMENT", "CORRUPTION", "PASSIVE")
val skillNodeTypes = listOf("START", "SMALL", "NOTABLE", "KEYSTONE")
val lotKinds = listOf("EQUIPMENT", "ITEM")
val modifierOperations = listOf("ADD", "INCREASED", "MORE", "SET")
const val SERVER_COMMIT = "ea6ad038bd5c9628559c0c4647fd9b08c95baf24"
const val SERVER_BRANCH = "claude/tender-pasteur-a36kj2"
const val SERVER_VERSION = "0.15.0"

fun template(catalog: Catalog, kind: EquipmentKind = EquipmentKind.Weapon): JsonObject = when (catalog) {
    Catalog.CHARACTERS -> defaultObject("character")
    // No text since 0.14.0: a document carries a code and its words live in the locale bundle.
    Catalog.ITEMS -> buildJsonObject {
        put("code", "EF_TEST_SHARD")
        put("image", JsonNull); put("category", "STONE_STOCK"); put("subCategory", "STONE"); put("price", 1L)
    }
    Catalog.EQUIPMENT -> buildJsonObject {
        put("type", kind.type)
        put("code", "EF_TEST_LEGACY")
        put("image", JsonNull)
        put("slot", when (kind) { EquipmentKind.Weapon -> "WEAPON_1H"; EquipmentKind.Armor -> "BODY"; EquipmentKind.Accessory -> "RING" })
        put("rarity", "RARE"); put("itemLevel", 30)
        put("modifierIds", JsonArray(emptyList()))
        // Armour, damage and attack speed are implicit modifiers since 0.10.0: the item has no
        // stat fields of its own, so its base is a list of fixed modifiers like any other source.
        put("baseParams", JsonArray(emptyList()))
        put("requiredLevel", 1); put("requiredStrength", 0); put("requiredDexterity", 0); put("requiredIntelligence", 0)
        if (kind == EquipmentKind.Weapon) { put("weaponType", "SWORD"); put("durability", 100) }
    }
}

fun requireId(id: String) { require(Regex("[0-9a-fA-F]{24}").matches(id)) { tr("ID должен содержать 24 шестнадцатеричных символа", "The id must be 24 hexadecimal characters") } }

/**
 * A content code: the key half of `equipment.<CODE>.name`.
 *
 * It has to survive a round trip through a locale key, so the separator of that key is the one
 * character it cannot contain.
 */
fun validateCode(code: String) {
    require(code.isNotBlank()) { tr("Введите код предмета", "Enter the item's code") }
    require(Regex("[A-Za-z0-9_]+").matches(code)) {
        tr("Код: латиница, цифры и подчёркивание", "The code takes Latin letters, digits and underscores")
    }
}

/** Only changed mutable fields: never sends the polymorphic discriminator to MongoDB. */
fun diff(original: JsonObject, edited: JsonObject): JsonObject = JsonObject(
    edited.filter { (key, value) -> key !in protectedFields && original[key] != value }
)

fun validate(document: JsonObject, catalog: Catalog) {
    // A character is named by its player, so it keeps a literal name; everything else is content
    // and carries a code whose text lives in the server's locale bundle.
    if (catalog == Catalog.CHARACTERS) require(document.text("name").isNotBlank()) { tr("Введите имя", "Enter a name") }
    else validateCode(document.text("code"))
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
    // Durability is the last number an item still keeps for itself; it is no character stat.
    if (kind == EquipmentKind.Weapon) {
        require(document.text("weaponType") in weapons) { tr("Неизвестный тип оружия", "Unknown weapon type") }
        require(document.text("durability").toIntOrNull()?.let { it >= 0 } == true) { tr("Прочность должна быть целой и неотрицательной", "Durability must be a non-negative whole number") }
    }
    validateRequirements(document)
    validateBaseParams(document)
    validateModifierPool(document)
}

/** What a character must reach to wear the item. The server decides whether they do. */
private fun validateRequirements(document: JsonObject) {
    require(document["requiredLevel"] == null || document.text("requiredLevel").toIntOrNull()?.let { it >= 1 } == true) {
        tr("Требуемый уровень — целое число от 1", "The required level is a whole number of 1 or more")
    }
    listOf("requiredStrength" to tr("силы", "strength"), "requiredDexterity" to tr("ловкости", "dexterity"),
           "requiredIntelligence" to tr("интеллекта", "intelligence")).forEach { (key, name) ->
        require(document[key] == null || document.text(key).toIntOrNull()?.let { it >= 0 } == true) {
            tr("Требование $name — целое неотрицательное число", "The $name requirement is a non-negative whole number")
        }
    }
}

/**
 * The item's own base, written as fixed modifiers.
 *
 * Nothing here is rolled — a base type's armour is not random in PoE either — so a base modifier
 * carries values and no tier. The values themselves belong to the server's definitions; the client
 * only refuses a reference it could not have come from.
 */
fun validateBaseParams(document: JsonObject) {
    val base = document["baseParams"] ?: return
    require(base is JsonArray) { tr("baseParams: требуется список модификаторов", "baseParams: a list of modifiers is required") }
    base.forEach { raw ->
        val modifier = requireNotNull(raw as? JsonObject) { tr("baseParams: требуется объект модификатора", "baseParams: a modifier object is required") }
        requireId(modifier.text("modifierId"))
        val values = modifier["values"] as? JsonArray
        require(values != null && values.isNotEmpty() && values.all { (it as? JsonPrimitive)?.doubleOrNull?.isFinite() == true }) {
            tr("baseParams: значения должны быть конечными числами", "baseParams: values must be finite numbers")
        }
        require(modifier.text("tierId").isBlank()) { tr("База предмета не роллится и тира не имеет", "An item's base is not rolled and carries no tier") }
    }
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

/**
 * An applied modifier, rolled or fixed.
 *
 * A rolled one names the tier it came from; a fixed one — a tree node's bonus, a class conversion,
 * an item's base — has none at all, and demanding a tier of it would reject what the server wrote.
 */
fun validateModifier(document: JsonObject) {
    val modifier = WireJson.decodeFromJsonElement(Modifier.serializer(), document)
    requireId(modifier.modifierId)
    require(if (modifier.rolled) modifier.tier > 0 else modifier.tier == 0) {
        tr("Зароленный модификатор указывает тир, фиксированный — нет", "A rolled modifier names its tier, a fixed one does not")
    }
    require(modifier.values.all { it.isFinite() }) { tr("Значения должны быть конечными числами", "Values must be finite numbers") }
}

/**
 * The version of a versioned record.
 *
 * Only `user`, `character` and `characterequipment` are VersionedEntity on this server; items,
 * equipment, recipes, codes and modifier documents carry no version, so never ask them for one.
 */
val JsonObject.entityVersion: Long get() = get("version")?.jsonPrimitive?.longOrNull?.takeIf { it >= 0 }
    ?: error(tr("Сервер не вернул версию записи. Обновите данные.", "The server returned no record version. Refresh the data."))

fun editableFields(catalog: Catalog): Set<String> = when (catalog) {
    Catalog.CHARACTERS -> setOf("name", "description")
    Catalog.ITEMS -> setOf("image", "category", "subCategory", "price")
    Catalog.EQUIPMENT -> setOf("image", "slot", "rarity", "itemLevel", "weaponType", "durability",
        "modifierIds", "baseParams", "requiredLevel", "requiredStrength", "requiredDexterity", "requiredIntelligence")
}

/**
 * Fields only accepted when the record is created; afterwards they are the server's.
 *
 * A character's class is one of them: the base it hands out is read at every calculation, so moving
 * a character to another class would silently rewrite their history. The server has no route for it.
 */
fun creationFields(catalog: Catalog): Set<String> = when (catalog) {
    Catalog.CHARACTERS -> setOf("userId", "classId")
    // A code names the row in the locale bundle, and renaming it would orphan every translation.
    Catalog.EQUIPMENT -> setOf("type", "code")
    Catalog.ITEMS -> setOf("code")
}
