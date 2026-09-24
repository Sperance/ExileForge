package com.sperance.exileforge.core.contract

import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.core.editor.formSchema
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.model.modifier.Modifier
import kotlinx.serialization.json.*

val WireJson = Json { ignoreUnknownKeys = true }
fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
val JsonObject.entityId: String get() = text("_id").ifBlank { text("id") }
/** Fields MongoDB owns. `type` is the polymorphic discriminator: allowed on create, never on update. */
val protectedFields = setOf("_id", "id", "version", "deleted", "createdAt", "updatedAt", "type")

val rarities = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "UNIQUE", "MYTHICAL")
// JEWEL is last on purpose: it is not worn on the body but sits in a socket on the tree,
// and `CharacterEquipment.socketCode` says which one.
val slots = listOf("HELMET", "BODY", "GLOVES", "RING", "BOOTS", "WINGS", "BELT", "WEAPON_1H", "WEAPON_2H", "QUIVER", "SHIELD", "AMULET", "JEWEL", "MAP", "TOOL_MINING", "TOOL_HERBALISM", "TOOL_WOODCUTTING", "TOOL_SMITHING", "TOOL_ALCHEMY", "TOOL_CARTOGRAPHY")
/**
 * One line of the equipment ledger: a place on the body and the template slots that fill it.
 *
 * Since 2.18.0 the hands are two places rather than four slots — a one- or two-handed weapon in
 * the main hand, a shield or a quiver in the other — because the server never lets both of a pair
 * be worn at once, and four cells for two hands read as four things to fill. The rings are two
 * places filled from one template slot: [ring] is the place the server is asked to put a ring in
 * (`RING`, `RING_2`), null everywhere else, where the template's own slot decides.
 */
data class BodyPlace(val code: String, val fits: List<String>, val ring: String? = null) {
    /** What is worn here, out of the hero's items keyed by `equippedSlot`. */
    fun <T> wornIn(equipped: Map<String, T>): T? = if (ring != null) equipped[ring] else fits.firstNotNullOfOrNull { equipped[it] }
    /** The off hand is taken whenever a two-handed weapon is — by the server's rule, shown here. */
    fun blockedBy(equipped: Map<String, *>): Boolean = code == OFF_HAND && "WEAPON_2H" in equipped
    companion object {
        const val MAIN_HAND = "MAIN_HAND"
        const val OFF_HAND = "OFF_HAND"
    }
}

/** The ledger's lines, top to bottom: the hands first, then the body, then the jewellery. */
val bodyPlaces = listOf(
    BodyPlace(BodyPlace.MAIN_HAND, listOf("WEAPON_1H", "WEAPON_2H")),
    BodyPlace(BodyPlace.OFF_HAND, listOf("SHIELD", "QUIVER")),
) + listOf("HELMET", "BODY", "GLOVES", "BOOTS", "AMULET").map { BodyPlace(it, listOf(it)) } +
    listOf(BodyPlace("RING", listOf("RING"), "RING"), BodyPlace("RING_2", listOf("RING"), "RING_2")) +
    listOf("BELT", "WINGS").map { BodyPlace(it, listOf(it)) }

val weapons = listOf("SWORD", "LONGSWORD", "BOW", "WAND", "AXE", "DOUBLEAXE", "DOUBLESWORD", "BLADE")
val modifierSources = listOf("IMPLICIT", "PREFIX", "SUFFIX", "UNIQUE", "ENCHANTMENT", "CORRUPTION", "PASSIVE")
val skillNodeTypes = listOf("START", "SMALL", "NOTABLE", "KEYSTONE", "JEWEL_SOCKET")
val lotKinds = listOf("EQUIPMENT", "ITEM")
val modifierOperations = listOf("ADD", "INCREASED", "MORE", "SET")
const val SERVER_COMMIT = "d7598c508ad1731dc8f9c1eaf75b21d05257a8a2"
const val SERVER_BRANCH = "claude/tender-pasteur-a36kj2"
const val SERVER_VERSION = "0.48.2"

fun template(catalog: Catalog, kind: EquipmentKind = EquipmentKind.Weapon): JsonObject = when (catalog) {
    Catalog.CHARACTERS -> defaultObject("character")
    // No text since 0.14.0: a document carries a code and its words live in the locale bundle.
    Catalog.ITEMS -> buildJsonObject {
        put("code", "EF_TEST_SHARD")
        put("category", "STONE_STOCK"); put("subCategory", "STONE"); put("price", 1L)
    }
    Catalog.EQUIPMENT -> buildJsonObject {
        put("type", kind.type)
        put("code", "EF_TEST_LEGACY")
        put("slot", when (kind) { EquipmentKind.Weapon -> "WEAPON_1H"; EquipmentKind.Armor -> "BODY"; EquipmentKind.Accessory -> "RING" })
        put("rarity", "RARE"); put("itemLevel", 30)
        put("fixedModifierIds", JsonArray(emptyList())); put("modifierPools", JsonArray(emptyList())); put("pools", JsonObject(emptyMap()))
        // Armour, damage and attack speed are implicit modifiers since 0.10.0: the item has no
        // stat fields of its own, so its base is a list of fixed modifiers like any other source.
        put("baseParams", JsonArray(emptyList()))
        put("requiredLevel", 1); put("requiredStrength", 0); put("requiredDexterity", 0); put("requiredIntelligence", 0)
        if (kind == EquipmentKind.Weapon) { put("weaponType", "SWORD"); put("durability", 100) }
    }
}

/**
 * The document that makes a character.
 *
 * It is written out rather than taken from [template], because a template is a *form's* seed: it
 * carries every field the editor draws, and the skill lists among them belong to the server. A
 * creation may only carry the fields a creation is allowed, so the menu builds exactly those and
 * lets the server default the rest.
 */
fun characterDocument(userId: String, name: String, classId: String): JsonObject = buildJsonObject {
    put("userId", userId); put("name", name.trim()); put("classId", classId)
}

fun requireId(id: String) { require(Regex("[0-9a-fA-F]{24}").matches(id)) { ui("contract.bad_id") } }

/**
 * A content code: the key half of `equipment.<CODE>.name`.
 *
 * It has to survive a round trip through a locale key, so the separator of that key is the one
 * character it cannot contain.
 */
fun validateCode(code: String) {
    require(code.isNotBlank()) { ui("contract.enter_code") }
    require(Regex("[A-Za-z0-9_]+").matches(code)) {
        ui("contract.code_format")
    }
}

/** Only changed mutable fields: never sends the polymorphic discriminator to MongoDB. */
fun diff(original: JsonObject, edited: JsonObject): JsonObject = JsonObject(
    edited.filter { (key, value) -> key !in protectedFields && original[key] != value }
)

fun validate(document: JsonObject, catalog: Catalog) {
    // A character is named by its player, so it keeps a literal name; everything else is content
    // and carries a code whose text lives in the server's locale bundle.
    if (catalog == Catalog.CHARACTERS) require(document.text("name").isNotBlank()) { ui("contract.enter_name") }
    else validateCode(document.text("code"))
    validateForm(formSchema(catalog), document)
    when (catalog) {
        Catalog.CHARACTERS -> validateCharacter(document)
        Catalog.ITEMS -> {
            require(document.text("category").isNotBlank()) { ui("contract.enter_category") }
            require(document.text("subCategory").isNotBlank()) { ui("contract.enter_subcategory") }
            require((document["price"] as? JsonPrimitive)?.longOrNull?.let { it >= 0 } == true) { ui("contract.price_whole") }
        }
        Catalog.EQUIPMENT -> validateEquipment(document)
    }
}

private fun validateEquipment(document: JsonObject) {
    require(document.text("rarity") in rarities) { ui("api.unknown_rarity") }
    require(document.text("slot") in slots) { ui("contract.unknown_slot") }
    require((document["itemLevel"] as? JsonPrimitive)?.intOrNull?.let { it >= 1 } == true) { ui("contract.level_whole") }
    val kind = requireNotNull(EquipmentKind.of(document.text("type"))) { ui("contract.unknown_equipment") }
    // Durability is the last number an item still keeps for itself; it is no character stat.
    if (kind == EquipmentKind.Weapon) {
        require(document.text("weaponType") in weapons) { ui("contract.unknown_weapon") }
        require(document.text("durability").toIntOrNull()?.let { it >= 0 } == true) { ui("contract.durability_whole") }
    }
    validateRequirements(document)
    validateBaseParams(document)
    validateModifierPool(document)
}

/** What a character must reach to wear the item. The server decides whether they do. */
private fun validateRequirements(document: JsonObject) {
    require(document["requiredLevel"] == null || document.text("requiredLevel").toIntOrNull()?.let { it >= 1 } == true) {
        ui("contract.required_level")
    }
    listOf("requiredStrength" to ui("contract.strength"), "requiredDexterity" to ui("contract.dexterity"),
           "requiredIntelligence" to ui("contract.intelligence")).forEach { (key, name) ->
        require(document[key] == null || document.text(key).toIntOrNull()?.let { it >= 0 } == true) {
            ui("contract.requirement_whole", name)
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
    require(base is JsonArray) { ui("contract.base_list") }
    base.forEach { raw ->
        val modifier = requireNotNull(raw as? JsonObject) { ui("contract.base_object") }
        requireId(modifier.text("modifierId"))
        val values = modifier["values"] as? JsonArray
        require(values != null && values.isNotEmpty() && values.all { (it as? JsonPrimitive)?.doubleOrNull?.isFinite() == true }) {
            ui("contract.base_finite")
        }
        require(modifier.text("tierId").isBlank()) { ui("contract.base_no_tier") }
    }
}

/**
 * A template names pools and fixed references, never inline definitions and never rolled values.
 *
 * Since server 0.39.0 a pool is a tag: `modifierPools` are the tags its affixes roll from and `pools`
 * the tags it sits in itself, each with a weight. Which modifiers land on an instance is the server's
 * decision; the fixed ones (implicits, a unique's lines) sit on every copy.
 */
fun validateModifierPool(document: JsonObject) {
    document["fixedModifierIds"]?.let { fixed ->
        require(fixed is JsonArray) { ui("contract.ids_list") }
        fixed.forEach { requireId((it as? JsonPrimitive)?.contentOrNull.orEmpty()) }
    }
    document["modifierPools"]?.let { tags ->
        require(tags is JsonArray && tags.all { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content?.isNotBlank() == true }) { ui("contract.pool_tags") }
    }
    document["pools"]?.let { pools ->
        require(pools is JsonObject && pools.all { (tag, weight) -> tag.isNotBlank() && (weight as? JsonPrimitive)?.longOrNull?.let { it >= 0 } == true }) { ui("contract.pool_weights") }
    }
    require("params" !in document) { ui("contract.rolled_instance") }
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
        ui("contract.tier_rule")
    }
    require(modifier.values.all { it.isFinite() }) { ui("contract.values_finite") }
}

/**
 * The version of a versioned record.
 *
 * Only `user`, `character` and `characterequipment` are VersionedEntity on this server; items,
 * equipment, recipes, codes and modifier documents carry no version, so never ask them for one.
 */
val JsonObject.entityVersion: Long get() = get("version")?.jsonPrimitive?.longOrNull?.takeIf { it >= 0 }
    ?: error(ui("contract.no_version"))

fun editableFields(catalog: Catalog): Set<String> = when (catalog) {
    Catalog.CHARACTERS -> setOf("name", "description")
    Catalog.ITEMS -> setOf("category", "subCategory", "price")
    Catalog.EQUIPMENT -> setOf("slot", "rarity", "itemLevel", "weaponType", "durability",
        "fixedModifierIds", "modifierPools", "pools", "baseParams", "requiredLevel", "requiredStrength", "requiredDexterity", "requiredIntelligence")
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
