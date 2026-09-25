package com.sperance.exileforge.core.editor

import com.sperance.exileforge.core.contract.modifierOperations
import com.sperance.exileforge.core.contract.modifierSources
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.weapons
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.model.character.battleStats
import com.sperance.exileforge.core.model.character.boolStats
import com.sperance.exileforge.core.model.character.professionStats
import com.sperance.exileforge.core.model.character.stockStats
import kotlinx.serialization.json.*

/** UI metadata for the server wire contract; documents retain fields unknown to this client. */
private fun reference(key: String, label: String, source: EntitySource) = FormField(key, label, InputSpec.Reference(source), JsonPrimitive(""))
private fun text(key: String, label: String, default: String = "", options: List<String> = emptyList()) = FormField(key, label, InputSpec.Text(options), JsonPrimitive(default))
private fun num(key: String, label: String, default: Number = 0, integer: Boolean = false, min: Double? = null, max: Double? = null) = FormField(key, label, InputSpec.Number(integer, min, max), JsonPrimitive(default))
private fun choice(key: String, label: String, options: List<String>, default: String = options.first()) = FormField(key, label, InputSpec.Select(options), JsonPrimitive(default))
private fun flag(key: String, label: String, default: Boolean = false) = FormField(key, label, InputSpec.Flag, JsonPrimitive(default))
private fun list(key: String, label: String, spec: InputSpec) = FormField(key, label, InputSpec.ListOf(spec), JsonArray(emptyList()))

val itemCategories = listOf("WOOD_STOCK", "STONE_STOCK", "MATERIAL", "QUEST")

fun schemaFields(schema: String, document: JsonObject = JsonObject(emptyMap())): List<FormField> = when (schema) {
    // No name or description here since 0.14.0: a document carries a code and the words live in
    // the server's locale files, so renaming a thing is a translation change, not a write.
    "items" -> listOf(
        text("code", ui("account.code"), "EF_NEW_ITEM"),
        text("category", ui("grant.category"), "STONE_STOCK", itemCategories),
        text("subCategory", ui("field.subcategory"), "STONE"),
        num("price", ui("card.price"), 0, true, 0.0),
    )
    "equipment" -> buildList {
        addAll(listOf(
            text("code", ui("account.code"), "EF_NEW_EQUIPMENT"),
            choice("slot", ui("common.slot"), slots),
            choice("rarity", ui("common.rarity"), rarities),
            num("itemLevel", ui("card.item_level"), 1, true, 1.0),
        ))
        // Durability is the only number an item still keeps as a field of its own.
        if (EquipmentKind.of(document.text("type")) == EquipmentKind.Weapon) addAll(listOf(
            choice("weaponType", ui("card.weapon_type"), weapons),
            num("durability", ui("card.durability"), 100, true, 0.0)))
        addAll(listOf(
            num("requiredLevel", ui("field.required_level"), 1, true, 1.0),
            num("requiredStrength", ui("field.required_strength"), 0, true, 0.0),
            num("requiredDexterity", ui("field.required_dexterity"), 0, true, 0.0),
            num("requiredIntelligence", ui("field.required_intelligence"), 0, true, 0.0)))
        // The base — armour, damage, attack speed — is fixed modifiers rather than stat fields.
        add(list("baseParams", ui("field.base"), InputSpec.Object("fixedModifier")))
        // Implicits and a unique's lines: ModifierDefinition ids that sit on every copy.
        add(list("fixedModifierIds", ui("field.fixed_modifiers"), InputSpec.Reference(EntitySource.MODIFIER)))
        // Since server 0.39.0 a pool is a tag: the template names the ones it rolls affixes from, and
        // which pools it sits in itself. What lands on an instance is still rolled by the server.
        add(list("modifierPools", ui("field.modifier_pools"), InputSpec.Text()))
        add(FormField("pools", ui("field.pools"), InputSpec.Weights, JsonObject(emptyMap())))
    }
    "character" -> listOf(
        text("name", ui("common.name")),
        text("description", ui("form.description")),
        list("professionSkills", ui("field.professions"), InputSpec.Object("professionSkill")),
        list("battleSkills", ui("field.battle_skills"), InputSpec.Object("battleSkill")),
        list("boolSkills", ui("field.states"), InputSpec.Object("boolSkill")),
    )
    /** A modifier with values but no tier: an item's base, a class conversion, a tree node's bonus. */
    "fixedModifier" -> listOf(
        reference("modifierId", ui("field.modifier"), EntitySource.MODIFIER),
        list("values", ui("field.values"), InputSpec.Number()))
    "professionSkill", "battleSkill" -> listOf(
        choice("stat", ui("field.skill"), if (schema == "professionSkill") professionStats else battleStats),
        num("level", ui("common.level"), 0, true, 0.0, 127.0),
        num("experience", ui("grant.experience"), 0.0, min = 0.0))
    "boolSkill" -> listOf(choice("stat", ui("card.state"), boolStats), flag("value", ui("field.active")).copy(nullable = true, default = JsonNull))
    // Read-only shapes the catalogue renders; they are never posted back.
    "modifierDefinition" -> listOf(
        text("code", ui("account.code")),
        choice("source", ui("field.source"), modifierSources, "PREFIX"),
        flag("isLocal", ui("field.local")),
        list("effects", ui("field.effects"), InputSpec.Object("modifierEffect")),
        list("tags", ui("field.tags"), InputSpec.Text(emptyList())),
        FormField("pools", ui("field.pools"), InputSpec.Weights, JsonObject(emptyMap())))
    // An effect with perStat is a conversion: the value is multiplied by the whole steps of a source stat.
    "modifierEffect" -> listOf(
        text("stat", ui("field.stat"), stockStats.first(), stockStats),
        choice("operation", ui("field.operation"), modifierOperations),
        text("perStat", ui("field.per_stat"), "", stockStats).copy(nullable = true, default = JsonNull),
        num("perAmount", ui("field.per_amount"), 1.0, min = 0.000001))
    else -> error(ui("form.unknown", schema))
}

fun defaultObject(schema: String): JsonObject =
    JsonObject(schemaFields(schema).associate { it.key to defaultValue(it.spec, it.default) })

fun defaultValue(spec: InputSpec, fallback: JsonElement = JsonNull): JsonElement = when (spec) {
    is InputSpec.Object -> defaultObject(spec.schema)
    else -> fallback
}

fun formSchema(catalog: Catalog) = when (catalog) { Catalog.ITEMS -> "items"; Catalog.EQUIPMENT -> "equipment"; Catalog.CHARACTERS -> "character" }

fun validateForm(schema: String, document: JsonObject) {
    schemaFields(schema, document).forEach { field ->
        val value = document[field.key] ?: return@forEach
        if (value == JsonNull && field.nullable) return@forEach
        fun checkValue(spec: InputSpec, element: JsonElement) {
            when (spec) {
                is InputSpec.Number -> {
                    val p = element as? JsonPrimitive
                    val n = p?.doubleOrNull
                    require(p != null && !p.isString && n != null && n.isFinite() && (!spec.integer || p.longOrNull != null) && (spec.min == null || n >= spec.min) && (spec.max == null || n <= spec.max)) { ui("form.invalid_number", field.label) }
                }
                is InputSpec.Reference -> requireId(element.jsonPrimitive.content)
                is InputSpec.Text -> require(element is JsonPrimitive && element.isString) { ui("form.text_required", field.label) }
                InputSpec.Weights -> require(element is JsonObject && element.all { (tag, weight) -> tag.isNotBlank() && (weight as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull?.let { it >= 0 } == true }) { ui("form.weights_required", field.label) }
                InputSpec.Flag -> require(element is JsonPrimitive && !element.isString && element.booleanOrNull != null) { ui("form.bool_required", field.label) }
                is InputSpec.Select -> require(element is JsonPrimitive && element.content in spec.options) { ui("form.choice_required", field.label) }
                is InputSpec.Object -> validateForm(spec.schema, element.jsonObject)
                is InputSpec.ListOf -> element.jsonArray.forEach { checkValue(spec.element, it) }
            }
        }
        checkValue(field.spec, value)
    }
}
