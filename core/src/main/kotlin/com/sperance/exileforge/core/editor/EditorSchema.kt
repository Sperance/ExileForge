package com.sperance.exileforge.core.editor

import com.sperance.exileforge.core.contract.modifierOperations
import com.sperance.exileforge.core.contract.modifierSources
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.weapons
import com.sperance.exileforge.core.i18n.tr
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

val itemCategories = listOf("WOOD_STOCK", "STONE_STOCK", "CONSUMABLE", "MATERIAL", "QUEST")

fun schemaFields(schema: String, document: JsonObject = JsonObject(emptyMap())): List<FormField> = when (schema) {
    // No name or description here since 0.14.0: a document carries a code and the words live in
    // the server's locale files, so renaming a thing is a translation change, not a write.
    "items" -> listOf(
        text("code", tr("Код", "Code"), "EF_NEW_ITEM"),
        text("category", tr("Категория", "Category"), "STONE_STOCK", itemCategories),
        text("subCategory", tr("Подкатегория", "Sub-category"), "STONE"),
        num("price", tr("Цена", "Price"), 0, true, 0.0),
    )
    "equipment" -> buildList {
        addAll(listOf(
            text("code", tr("Код", "Code"), "EF_NEW_EQUIPMENT"),
            choice("slot", tr("Слот", "Slot"), slots),
            choice("rarity", tr("Редкость", "Rarity"), rarities),
            num("itemLevel", tr("Уровень предмета", "Item level"), 1, true, 1.0),
        ))
        // Durability is the only number an item still keeps as a field of its own.
        if (EquipmentKind.of(document.text("type")) == EquipmentKind.Weapon) addAll(listOf(
            choice("weaponType", tr("Тип оружия", "Weapon type"), weapons),
            num("durability", tr("Прочность", "Durability"), 100, true, 0.0)))
        addAll(listOf(
            num("requiredLevel", tr("Требуемый уровень", "Required level"), 1, true, 1.0),
            num("requiredStrength", tr("Требуется силы", "Strength required"), 0, true, 0.0),
            num("requiredDexterity", tr("Требуется ловкости", "Dexterity required"), 0, true, 0.0),
            num("requiredIntelligence", tr("Требуется интеллекта", "Intelligence required"), 0, true, 0.0)))
        // The base — armour, damage, attack speed — is fixed modifiers rather than stat fields.
        add(list("baseParams", tr("База предмета", "Item base"), InputSpec.Object("fixedModifier")))
        // The pool is a list of ModifierDefinition ids; what lands on an instance is rolled by the server.
        add(list("modifierIds", tr("Пул модификаторов", "Modifier pool"), InputSpec.Reference(EntitySource.MODIFIER)))
    }
    "character" -> listOf(
        text("name", tr("Имя", "Name")),
        text("description", tr("Описание", "Description")),
        list("professionSkills", tr("Профессии", "Professions"), InputSpec.Object("professionSkill")),
        list("battleSkills", tr("Боевые навыки", "Battle skills"), InputSpec.Object("battleSkill")),
        list("boolSkills", tr("Состояния", "States"), InputSpec.Object("boolSkill")),
    )
    /** A modifier with values but no tier: an item's base, a class conversion, a tree node's bonus. */
    "fixedModifier" -> listOf(
        reference("modifierId", tr("Модификатор", "Modifier"), EntitySource.MODIFIER),
        list("values", tr("Значения", "Values"), InputSpec.Number()))
    "professionSkill", "battleSkill" -> listOf(
        choice("stat", tr("Навык", "Skill"), if (schema == "professionSkill") professionStats else battleStats),
        num("level", tr("Уровень", "Level"), 0, true, 0.0, 127.0),
        num("experience", tr("Опыт", "Experience"), 0.0, min = 0.0))
    "boolSkill" -> listOf(choice("stat", tr("Состояние", "State"), boolStats), flag("value", tr("Активно", "Active")).copy(nullable = true, default = JsonNull))
    // Read-only shapes the catalogue renders; they are never posted back.
    "modifierDefinition" -> listOf(
        text("code", tr("Код", "Code")),
        choice("source", tr("Источник", "Source"), modifierSources, "PREFIX"),
        flag("isLocal", tr("Локальный", "Local")),
        list("effects", tr("Эффекты", "Effects"), InputSpec.Object("modifierEffect")),
        list("tags", tr("Теги", "Tags"), InputSpec.Text(emptyList())))
    // An effect with perStat is a conversion: the value is multiplied by the whole steps of a source stat.
    "modifierEffect" -> listOf(
        text("stat", tr("Характеристика", "Stat"), stockStats.first(), stockStats),
        choice("operation", tr("Операция", "Operation"), modifierOperations),
        text("perStat", tr("За каждые (характеристика)", "Per (stat)"), "", stockStats).copy(nullable = true, default = JsonNull),
        num("perAmount", tr("Шаг конверсии", "Conversion step"), 1.0, min = 0.000001))
    else -> error(tr("Неизвестная форма: $schema", "Unknown form: $schema"))
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
                    require(p != null && !p.isString && n != null && n.isFinite() && (!spec.integer || p.longOrNull != null) && (spec.min == null || n >= spec.min) && (spec.max == null || n <= spec.max)) { tr("${field.label}: некорректное число", "${field.label}: invalid number") }
                }
                is InputSpec.Reference -> requireId(element.jsonPrimitive.content)
                is InputSpec.Text -> require(element is JsonPrimitive && element.isString) { tr("${field.label}: требуется текст", "${field.label}: text required") }
                InputSpec.Flag -> require(element is JsonPrimitive && !element.isString && element.booleanOrNull != null) { tr("${field.label}: требуется да/нет", "${field.label}: yes/no required") }
                is InputSpec.Select -> require(element is JsonPrimitive && element.content in spec.options) { tr("${field.label}: выберите значение", "${field.label}: choose a value") }
                is InputSpec.Object -> validateForm(spec.schema, element.jsonObject)
                is InputSpec.ListOf -> element.jsonArray.forEach { checkValue(spec.element, it) }
            }
        }
        checkValue(field.spec, value)
    }
}
