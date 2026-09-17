package com.sperance.exileforge.core.editor

import com.sperance.exileforge.core.contract.modifierSources
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.weapons
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.character.battleStats
import com.sperance.exileforge.core.model.character.boolStats
import com.sperance.exileforge.core.model.character.professionStats
import com.sperance.exileforge.core.model.character.stockStats
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import com.sperance.exileforge.core.model.modifier.ModifierScope
import com.sperance.exileforge.core.i18n.tr
import kotlinx.serialization.json.*

/** UI metadata for the server wire contract; documents retain fields unknown to this client. */
private fun reference(key: String, label: String, source: EntitySource) = FormField(key, label, InputSpec.Reference(source), JsonPrimitive(""))
private fun text(key: String, label: String, default: String = "", options: List<String> = emptyList()) = FormField(key, label, InputSpec.Text(options), JsonPrimitive(default))
private fun num(key: String, label: String, default: Number = 0, integer: Boolean = false, min: Double? = null, max: Double? = null) = FormField(key, label, InputSpec.Number(integer, min, max), JsonPrimitive(default))
private fun choice(key: String, label: String, options: List<String>, default: String = options.first()) = FormField(key, label, InputSpec.Select(options), JsonPrimitive(default))
private fun flag(key: String, label: String, default: Boolean = false) = FormField(key, label, InputSpec.Flag, JsonPrimitive(default))
private fun list(key: String, label: String, spec: InputSpec) = FormField(key, label, InputSpec.ListOf(spec), JsonArray(emptyList()))
private fun obj(key: String, label: String, schema: String) = FormField(key, label, InputSpec.Object(schema), JsonObject(emptyMap()))
val statSuggestions = listOf("life", "mana", "energy", "strength", "agility", "intellect", "constitution", "armor", "evasion", "energy_shield", "physical_damage", "spell_damage", "fire_damage", "cold_damage", "lightning_damage", "chaos_damage", "attack_speed", "cast_speed", "movement_speed", "critical_chance", "critical_multiplier", "fire_resistance", "cold_resistance", "lightning_resistance", "chaos_resistance", "life_regen", "mana_regen", "block_chance", "rarity", "quantity")
val tagSuggestions = listOf("life", "mana", "attribute", "strength", "defence", "attack", "spell", "physical", "fire", "cold", "lightning", "chaos", "critical", "speed")
private val damageTypes = listOf("physical", "fire", "cold", "lightning", "chaos")
private fun stat(key: String, label: String) = text(key, label, "life", statSuggestions)
private fun expression(key: String, label: String) = obj(key, label, "expression")
private fun operation() = choice("operation", tr("Операция", "Operation"), ModifierOperation.entries.map { it.name })
val effectVariants get() = linkedMapOf("stat" to tr("Характеристика", "Stat"), "derived_stat" to tr("Зависимая характеристика", "Derived stat"), "damage_conversion" to tr("Конвертация урона", "Damage conversion"), "damage_taken_as" to tr("Получаемый урон как", "Damage taken as"), "penetration" to tr("Пробивание сопротивления", "Resistance penetration"), "gain_resource" to tr("Получение ресурса", "Gain resource"), "chance_to_apply" to tr("Шанс эффекта", "Chance to apply"), "add_tag" to tr("Добавить тег", "Add tag"), "remove_tag" to tr("Удалить тег", "Remove tag"), "grant_effect" to tr("Предоставить эффект", "Grant effect"))
val expressionVariants get() = linkedMapOf("constant" to tr("Константа", "Constant"), "stat" to tr("Характеристика", "Stat"), "modifier_value" to tr("Значение модификатора", "Modifier value"), "add" to tr("Сложение", "Addition"), "subtract" to tr("Вычитание", "Subtraction"), "multiply" to tr("Умножение", "Multiplication"), "divide" to tr("Деление", "Division"), "percentage" to tr("Процент", "Percentage"), "min" to tr("Минимум", "Minimum"), "max" to tr("Максимум", "Maximum"))
val conditionVariants get() = linkedMapOf("always" to tr("Всегда", "Always"), "stat_at_least" to tr("Характеристика не меньше", "Stat at least"), "stat_at_most" to tr("Характеристика не больше", "Stat at most"), "has_tag" to tr("Тег персонажа", "Character tag"), "target_has_tag" to tr("Тег цели", "Target tag"), "full_life" to tr("Полное здоровье", "Full life"), "low_life" to tr("Мало здоровья", "Low life"), "has_effect" to tr("Есть эффект", "Has effect"), "and" to tr("Все условия", "All conditions"), "or" to tr("Любое условие", "Any condition"), "not" to tr("Отрицание", "Negation"))

fun schemaFields(schema: String, document: JsonObject = JsonObject(emptyMap())): List<FormField> = when(schema) {
    "reference" -> listOf(text("definitionId", tr("Модификатор", "Modifier")), num("revision", tr("Версия", "Revision"), 1, true, 1.0))
    "modifier" -> listOf(num("definitionRevision", tr("Версия определения", "Definition revision"), 1, true, 1.0), text("definitionId", tr("Модификатор", "Modifier"), "life"), list("values", tr("Значения", "Values"), InputSpec.Object("rolledValue")), num("tier", tr("Уровень (tier)", "Tier"), 1, true, 1.0), choice("source", tr("Источник", "Source"), modifierSources, "PREFIX"), list("tags", tr("Теги", "Tags"), InputSpec.Text(tagSuggestions)))
    "rolledValue" -> listOf(num("value", tr("Значение", "Value"), 1.0))
    "definition" -> listOf(text("id", tr("Идентификатор", "Identifier"), "life", statSuggestions), text("name", tr("Название", "Name"), tr("Максимум здоровья", "Maximum life")), choice("source", tr("Источник", "Source"), modifierSources, "PREFIX"), choice("scope", tr("Область действия", "Scope"), ModifierScope.entries.map { it.name }), choice("affixType", tr("Префикс / суффикс", "Prefix / suffix"), listOf("PREFIX", "SUFFIX")).copy(nullable = true, default = JsonNull), list("tiers", tr("Уровни и диапазоны", "Tiers and ranges"), InputSpec.Object("tier")), list("tags", tr("Теги", "Tags"), InputSpec.Text(tagSuggestions)), list("conditions", tr("Условия", "Conditions"), InputSpec.Object("condition")), list("effects", tr("Эффекты", "Effects"), InputSpec.Object("effect")), num("priority", tr("Приоритет", "Priority"), 0, true), flag("rollable", tr("Участвует в случайной генерации", "Included in random generation"), true), flag("stackable", tr("Допускает повторение", "Can repeat")))
    "tier" -> listOf(num("tier", tr("Уровень (tier)", "Tier"), 1, true, 1.0), num("minItemLevel", tr("Минимальный уровень предмета", "Minimum item level"), 1, true, 1.0), num("weight", tr("Вес выпадения", "Drop weight"), 100, true, 0.0), list("values", tr("Диапазоны значений", "Value ranges"), InputSpec.Object("range")))
    "range" -> listOf(num("min", tr("Минимум", "Minimum"), 1.0), num("max", tr("Максимум", "Maximum"), 10.0))
    "effect" -> listOf(FormField("type", tr("Вид эффекта", "Effect kind"), InputSpec.Union(effectVariants), JsonPrimitive("stat"))) + when(document.text("type").ifBlank { "stat" }) {
        "stat" -> listOf(stat("stat", tr("Характеристика", "Stat")), operation(), expression("value", tr("Величина", "Amount")))
        "derived_stat" -> listOf(stat("targetStat", tr("Целевая характеристика", "Target stat")), stat("sourceStat", tr("Исходная характеристика", "Source stat")), operation(), expression("value", tr("Величина", "Amount")))
        "damage_conversion", "damage_taken_as" -> listOf(text("from", tr("Из типа урона", "From damage type"), "physical", damageTypes), text("to", tr("В тип урона", "To damage type"), "fire", damageTypes), expression("percentage", tr("Процент", "Percentage")))
        "penetration" -> listOf(text("damageType", tr("Тип урона", "Damage type"), "fire", damageTypes), expression("percentage", tr("Процент", "Percentage")))
        "gain_resource" -> listOf(text("resource", tr("Ресурс", "Resource"), "life", listOf("life", "mana", "energy")), expression("amount", tr("Количество", "Amount")))
        "chance_to_apply" -> listOf(text("effectId", tr("Эффект", "Effect"), "burning"), expression("chance", tr("Шанс", "Chance")))
        "add_tag", "remove_tag" -> listOf(text("tag", tr("Тег", "Tag"), "fire", tagSuggestions))
        "grant_effect" -> listOf(text("effectId", tr("Эффект", "Effect"), "burning"))
        else -> emptyList()
    }
    "expression" -> listOf(FormField("type", tr("Вычисление", "Calculation"), InputSpec.Union(expressionVariants), JsonPrimitive("constant"))) + when(document.text("type").ifBlank { "constant" }) {
        "constant" -> listOf(num("value", tr("Константа", "Constant"), 1.0))
        "stat" -> listOf(stat("stat", tr("Характеристика", "Stat")))
        "modifier_value" -> listOf(num("index", tr("Номер значения (с нуля)", "Value index (zero based)"), 0, true, 0.0))
        "percentage" -> listOf(expression("expression", tr("Выражение", "Expression")))
        else -> listOf(expression("left", tr("Левая часть", "Left side")), expression("right", tr("Правая часть", "Right side")))
    }
    "condition" -> listOf(FormField("type", tr("Вид условия", "Condition kind"), InputSpec.Union(conditionVariants), JsonPrimitive("always"))) + when(document.text("type").ifBlank { "always" }) {
        "stat_at_least", "stat_at_most" -> listOf(stat("stat", tr("Характеристика", "Stat")), num("value", tr("Порог", "Threshold"), 1.0))
        "has_tag", "target_has_tag" -> listOf(text("tag", tr("Тег", "Tag"), "attack", tagSuggestions))
        "has_effect" -> listOf(text("effectId", tr("Эффект", "Effect"), "burning"))
        "and", "or" -> listOf(list("conditions", tr("Условия", "Conditions"), InputSpec.Object("condition")))
        "not" -> listOf(obj("condition", tr("Условие", "Condition"), "condition"))
        else -> emptyList()
    }
    "equipment", "items" -> buildList {
        addAll(listOf(text("name", tr("Название", "Name"), tr("Новый предмет", "New item")), text("description", tr("Описание", "Description")), text("image", tr("Изображение (URL)", "Image (URL)")).copy(nullable = true, default = JsonNull)))
        // Suggestions come from the loaded manifest: an id outside the server set is rejected with 400.
        add(text("icon", tr("Иконка набора", "Set icon"), options = com.sperance.exileforge.core.display.icons.iconSuggestions).copy(nullable = true, default = JsonNull))
        if(schema == "items") {
            addAll(listOf(text("category", tr("Категория", "Category"), "Currency"), text("subCategory", tr("Подкатегория", "Sub-category"), "Shard"), num("price", tr("Цена", "Price"), 0, true, 0.0)))
        } else {
            addAll(listOf(choice("slot", tr("Слот", "Slot"), slots), choice("rarity", tr("Редкость", "Rarity"), rarities), num("itemLevel", tr("Уровень предмета", "Item level"), 1, true, 1.0)))
            addAll(when(document.text("type").substringAfterLast('.')) {
                "Weapon" -> listOf(choice("weaponType", tr("Тип оружия", "Weapon type"), weapons), num("damage_min", tr("Минимальный урон", "Minimum damage"), 1.0, min = 0.0), num("damage_max", tr("Максимальный урон", "Maximum damage"), 10.0, min = 0.0), num("attackSpeed", tr("Скорость атаки", "Attack speed"), 1.0, min = 0.000001), num("durability", tr("Прочность", "Durability"), 100, true, 0.0))
                "Armor" -> listOf(num("defense", tr("Защита", "Defence"), 1, true, 0.0))
                else -> emptyList()
            })
            addAll(listOf(list("modifierDefinitionRefs", tr("Доступные модификаторы", "Available modifiers"), InputSpec.Object("reference")), list("stockModifierDefinitionRefs", tr("Встроенные модификаторы", "Implicit modifiers"), InputSpec.Object("reference"))))
        }
    }
    "character" -> listOf(text("name", tr("Имя", "Name")), text("description", tr("Описание", "Description")))
    "characterEquipment" -> listOf(reference("equipmentId", tr("Предмет экипировки", "Equipment item"), EntitySource.EQUIPMENT), text("uuid", tr("ID экземпляра", "Instance id"), newEntityId()), list("params", tr("Модификаторы экземпляра", "Instance modifiers"), InputSpec.Object("modifier")))
    "characterItem" -> listOf(reference("itemId", tr("Предмет", "Item"), EntitySource.ITEM), num("amount", tr("Количество", "Amount"), 1, true, 0.0))
    "professionSkill", "battleSkill" -> listOf(choice("stat", tr("Навык", "Skill"), if(schema == "professionSkill") professionStats else battleStats), num("level", tr("Уровень", "Level"), 0, true, 0.0, 127.0), num("experience", tr("Опыт", "Experience"), 0.0, min = 0.0))
    "stockSkill" -> listOf(choice("stat", tr("Характеристика", "Stat"), stockStats), num("value", tr("Значение", "Value"), 0, true, Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()))
    "boolSkill" -> listOf(choice("stat", tr("Состояние", "State"), boolStats), flag("value", tr("Активно", "Active")).copy(nullable = true, default = JsonNull))
    "redemption" -> listOf(reference("redemptionCodeId", tr("Промокод", "Promo code"), EntitySource.REDEMPTION), text("dateGained", tr("Дата получения (ГГГГ-ММ-ДДTчч:мм:сс)", "Date obtained (YYYY-MM-DDThh:mm:ss)"), "2026-01-01T00:00:00"))
    else -> error(tr("Неизвестная форма: $schema", "Unknown form: $schema"))
}

fun newEntityId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(24)
fun defaultObject(schema: String, variant: String? = null): JsonObject {
    val seed = if(variant == null) JsonObject(emptyMap()) else buildJsonObject { put("type", variant) }
    return JsonObject(schemaFields(schema, seed).associate { it.key to if(it.key == "type" && variant != null) JsonPrimitive(variant) else defaultValue(it.spec, it.default) })
}
fun defaultValue(spec: InputSpec, fallback: JsonElement = JsonNull): JsonElement = when(spec) {
    is InputSpec.Object -> defaultObject(spec.schema)
    else -> fallback
}
fun formSchema(catalog: Catalog) = when(catalog) { Catalog.ITEMS -> "items"; Catalog.EQUIPMENT -> "equipment"; Catalog.CHARACTERS -> "character" }
fun validateForm(schema: String, document: JsonObject) {
    schemaFields(schema, document).forEach { field ->
        val value = document[field.key] ?: return@forEach
        if(value == JsonNull && field.nullable) return@forEach
        fun checkValue(spec: InputSpec, element: JsonElement) {
            when(spec) {
                is InputSpec.Number -> {
                    val p = element as? JsonPrimitive
                    val n = p?.doubleOrNull
                    require(p != null && !p.isString && n != null && n.isFinite() && (!spec.integer || p.longOrNull != null) && (spec.min == null || n >= spec.min) && (spec.max == null || n <= spec.max)) { tr("${field.label}: некорректное число", "${field.label}: invalid number") }
                }
                is InputSpec.Reference -> requireId(element.jsonPrimitive.content)
                is InputSpec.Text -> require(element is JsonPrimitive && element.isString) { tr("${field.label}: требуется текст", "${field.label}: text required") }
                InputSpec.Flag -> require(element is JsonPrimitive && !element.isString && element.booleanOrNull != null) { tr("${field.label}: требуется да/нет", "${field.label}: yes/no required") }
                is InputSpec.Select -> require(element is JsonPrimitive && element.content in spec.options) { tr("${field.label}: выберите значение", "${field.label}: choose a value") }
                is InputSpec.Union -> require(element is JsonPrimitive && element.content in spec.variants) { tr("${field.label}: выберите вид", "${field.label}: choose a kind") }
                is InputSpec.Object -> validateForm(spec.schema, element.jsonObject)
                is InputSpec.ListOf -> element.jsonArray.forEach { checkValue(spec.element, it) }
            }
        }
        checkValue(field.spec, value)
    }
}
