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
    "items" -> listOf(
        text("name", tr("Название", "Name"), tr("Новый предмет", "New item")),
        text("description", tr("Описание", "Description")),
        text("image", tr("Изображение (URL)", "Image (URL)")).copy(nullable = true, default = JsonNull),
        text("category", tr("Категория", "Category"), "STONE_STOCK", itemCategories),
        text("subCategory", tr("Подкатегория", "Sub-category"), "STONE"),
        num("price", tr("Цена", "Price"), 0, true, 0.0),
    )
    "equipment" -> buildList {
        addAll(listOf(
            text("name", tr("Название", "Name"), tr("Новый предмет", "New item")),
            text("description", tr("Описание", "Description")),
            text("image", tr("Изображение (URL)", "Image (URL)")).copy(nullable = true, default = JsonNull),
            choice("slot", tr("Слот", "Slot"), slots),
            choice("rarity", tr("Редкость", "Rarity"), rarities),
            num("itemLevel", tr("Уровень предмета", "Item level"), 1, true, 1.0),
        ))
        addAll(when (EquipmentKind.of(document.text("type"))) {
            EquipmentKind.Weapon -> listOf(
                choice("weaponType", tr("Тип оружия", "Weapon type"), weapons),
                num("damage_min", tr("Минимальный урон", "Minimum damage"), 1.0, min = 0.0),
                num("damage_max", tr("Максимальный урон", "Maximum damage"), 10.0, min = 0.0),
                num("attackSpeed", tr("Скорость атаки", "Attack speed"), 1.0, min = 0.000001),
                num("durability", tr("Прочность", "Durability"), 100, true, 0.0))
            EquipmentKind.Armor -> listOf(num("defense", tr("Защита", "Defence"), 1, true, 0.0))
            else -> emptyList()
        })
        // The pool is a list of ModifierDefinition ids; what lands on an instance is rolled by the server.
        add(list("modifierIds", tr("Пул модификаторов", "Modifier pool"), InputSpec.Reference(EntitySource.MODIFIER)))
    }
    "character" -> listOf(
        text("name", tr("Имя", "Name")),
        text("description", tr("Описание", "Description")),
        list("stockSkills", tr("Базовые характеристики", "Base stats"), InputSpec.Object("stockSkill")),
        list("professionSkills", tr("Профессии", "Professions"), InputSpec.Object("professionSkill")),
        list("battleSkills", tr("Боевые навыки", "Battle skills"), InputSpec.Object("battleSkill")),
        list("boolSkills", tr("Состояния", "States"), InputSpec.Object("boolSkill")),
    )
    "professionSkill", "battleSkill" -> listOf(
        choice("stat", tr("Навык", "Skill"), if (schema == "professionSkill") professionStats else battleStats),
        num("level", tr("Уровень", "Level"), 0, true, 0.0, 127.0),
        num("experience", tr("Опыт", "Experience"), 0.0, min = 0.0))
    "stockSkill" -> listOf(choice("stat", tr("Характеристика", "Stat"), stockStats), num("value", tr("Значение", "Value"), 0, true, Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()))
    "boolSkill" -> listOf(choice("stat", tr("Состояние", "State"), boolStats), flag("value", tr("Активно", "Active")).copy(nullable = true, default = JsonNull))
    // Read-only shapes the catalogue renders; they are never posted back.
    "modifierDefinition" -> listOf(
        text("code", tr("Код", "Code")), text("name", tr("Название", "Name")),
        choice("source", tr("Источник", "Source"), modifierSources, "PREFIX"),
        list("effects", tr("Эффекты", "Effects"), InputSpec.Object("modifierEffect")),
        list("tags", tr("Теги", "Tags"), InputSpec.Text(emptyList())))
    "modifierEffect" -> listOf(text("stat", tr("Характеристика", "Stat"), stockStats.first(), stockStats), choice("operation", tr("Операция", "Operation"), modifierOperations))
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
