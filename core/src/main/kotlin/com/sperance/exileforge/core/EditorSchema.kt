package com.sperance.exileforge.core

import kotlinx.serialization.json.*

/** UI metadata for the server wire contract; documents retain fields unknown to this client. */
sealed interface InputSpec {
    data class Text(val suggestions: List<String> = emptyList()) : InputSpec
    data class Number(val integer: Boolean = false, val min: Double? = null, val max: Double? = null) : InputSpec
    data object Flag : InputSpec
    data class Select(val options: List<String>) : InputSpec
    data class Object(val schema: String) : InputSpec
    data class ListOf(val element: InputSpec) : InputSpec
    data class Union(val variants: Map<String, String>) : InputSpec
}
data class FormField(val key: String, val label: String, val spec: InputSpec, val default: JsonElement, val nullable: Boolean = false)
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
private fun operation() = choice("operation", "Операция", ModifierOperation.entries.map { it.name })
val effectVariants = linkedMapOf("stat" to "Характеристика", "derived_stat" to "Зависимая характеристика", "damage_conversion" to "Конвертация урона", "damage_taken_as" to "Получаемый урон как", "penetration" to "Пробивание сопротивления", "gain_resource" to "Получение ресурса", "chance_to_apply" to "Шанс эффекта", "add_tag" to "Добавить тег", "remove_tag" to "Удалить тег", "grant_effect" to "Предоставить эффект")
val expressionVariants = linkedMapOf("constant" to "Константа", "stat" to "Характеристика", "modifier_value" to "Значение модификатора", "add" to "Сложение", "subtract" to "Вычитание", "multiply" to "Умножение", "divide" to "Деление", "percentage" to "Процент", "min" to "Минимум", "max" to "Максимум")
val conditionVariants = linkedMapOf("always" to "Всегда", "stat_at_least" to "Характеристика не меньше", "stat_at_most" to "Характеристика не больше", "has_tag" to "Тег персонажа", "target_has_tag" to "Тег цели", "full_life" to "Полное здоровье", "low_life" to "Мало здоровья", "has_effect" to "Есть эффект", "and" to "Все условия", "or" to "Любое условие", "not" to "Отрицание")

fun schemaFields(schema: String, document: JsonObject = JsonObject(emptyMap())): List<FormField> = when(schema) {
    "modifier" -> listOf(text("definitionId", "Модификатор", "life"), list("values", "Значения", InputSpec.Object("rolledValue")), num("tier", "Уровень (tier)", 1, true, 1.0), choice("source", "Источник", modifierSources, "PREFIX"), list("tags", "Теги", InputSpec.Text(tagSuggestions)))
    "rolledValue" -> listOf(num("value", "Значение", 1.0))
    "definition" -> listOf(text("id", "Идентификатор", "life", statSuggestions), text("name", "Название", "Максимум здоровья"), choice("source", "Источник", modifierSources, "PREFIX"), choice("scope", "Область действия", ModifierScope.entries.map { it.name }), choice("affixType", "Префикс / суффикс", listOf("PREFIX", "SUFFIX")).copy(nullable = true, default = JsonNull), list("tiers", "Уровни и диапазоны", InputSpec.Object("tier")), list("tags", "Теги", InputSpec.Text(tagSuggestions)), list("conditions", "Условия", InputSpec.Object("condition")), list("effects", "Эффекты", InputSpec.Object("effect")), num("priority", "Приоритет", 0, true), flag("rollable", "Участвует в случайной генерации", true), flag("stackable", "Допускает повторение"))
    "tier" -> listOf(num("tier", "Уровень (tier)", 1, true, 1.0), num("minItemLevel", "Минимальный уровень предмета", 1, true, 1.0), num("weight", "Вес выпадения", 100, true, 0.0), list("values", "Диапазоны значений", InputSpec.Object("range")))
    "range" -> listOf(num("min", "Минимум", 1.0), num("max", "Максимум", 10.0))
    "effect" -> listOf(FormField("type", "Вид эффекта", InputSpec.Union(effectVariants), JsonPrimitive("stat"))) + when(document.text("type").ifBlank { "stat" }) {
        "stat" -> listOf(stat("stat", "Характеристика"), operation(), expression("value", "Величина"))
        "derived_stat" -> listOf(stat("targetStat", "Целевая характеристика"), stat("sourceStat", "Исходная характеристика"), operation(), expression("value", "Величина"))
        "damage_conversion", "damage_taken_as" -> listOf(text("from", "Из типа урона", "physical", damageTypes), text("to", "В тип урона", "fire", damageTypes), expression("percentage", "Процент"))
        "penetration" -> listOf(text("damageType", "Тип урона", "fire", damageTypes), expression("percentage", "Процент"))
        "gain_resource" -> listOf(text("resource", "Ресурс", "life", listOf("life", "mana", "energy")), expression("amount", "Количество"))
        "chance_to_apply" -> listOf(text("effectId", "Эффект", "burning"), expression("chance", "Шанс"))
        "add_tag", "remove_tag" -> listOf(text("tag", "Тег", "fire", tagSuggestions))
        "grant_effect" -> listOf(text("effectId", "Эффект", "burning"))
        else -> emptyList()
    }
    "expression" -> listOf(FormField("type", "Вычисление", InputSpec.Union(expressionVariants), JsonPrimitive("constant"))) + when(document.text("type").ifBlank { "constant" }) {
        "constant" -> listOf(num("value", "Константа", 1.0))
        "stat" -> listOf(stat("stat", "Характеристика"))
        "modifier_value" -> listOf(num("index", "Номер значения (с нуля)", 0, true, 0.0))
        "percentage" -> listOf(expression("expression", "Выражение"))
        else -> listOf(expression("left", "Левая часть"), expression("right", "Правая часть"))
    }
    "condition" -> listOf(FormField("type", "Вид условия", InputSpec.Union(conditionVariants), JsonPrimitive("always"))) + when(document.text("type").ifBlank { "always" }) {
        "stat_at_least", "stat_at_most" -> listOf(stat("stat", "Характеристика"), num("value", "Порог", 1.0))
        "has_tag", "target_has_tag" -> listOf(text("tag", "Тег", "attack", tagSuggestions))
        "has_effect" -> listOf(text("effectId", "Эффект", "burning"))
        "and", "or" -> listOf(list("conditions", "Условия", InputSpec.Object("condition")))
        "not" -> listOf(obj("condition", "Условие", "condition"))
        else -> emptyList()
    }
    "equipment", "items" -> buildList {
        addAll(listOf(text("name", "Название", "Новый предмет"), text("description", "Описание"), text("image", "Изображение (URL)").copy(nullable = true, default = JsonNull)))
        if(schema == "items") {
            addAll(listOf(text("category", "Категория", "Currency"), text("subCategory", "Подкатегория", "Shard"), num("price", "Цена", 0, true, 0.0)))
        } else {
            addAll(listOf(choice("slot", "Слот", slots), choice("rarity", "Редкость", rarities), num("itemLevel", "Уровень предмета", 1, true, 1.0)))
            addAll(when(document.text("type").substringAfterLast('.')) {
                "Weapon" -> listOf(choice("weaponType", "Тип оружия", weapons), num("damage_min", "Минимальный урон", 1.0, min = 0.0), num("damage_max", "Максимальный урон", 10.0, min = 0.0), num("attackSpeed", "Скорость атаки", 1.0, min = 0.000001), num("durability", "Прочность", 100, true, 0.0))
                "Armor" -> listOf(num("defense", "Защита", 1, true, 0.0))
                else -> emptyList()
            })
            addAll(listOf(list("modifiers", "Модификаторы", InputSpec.Object("modifier")).copy(nullable = true), list("modifierDefinitions", "Доступные модификаторы", InputSpec.Object("definition")).copy(nullable = true), list("modifierDefinitionsStock", "Встроенные модификаторы", InputSpec.Object("definition")).copy(nullable = true)))
        }
    }
    "character" -> listOf(text("name", "Имя"), text("description", "Описание"), text("userId", "Владелец (ID пользователя)"), num("level", "Уровень", 1, true, 1.0, 32767.0), num("experience", "Опыт", 0.0, min = 0.0), num("money", "Деньги", 0, true, 0.0), list("params", "Модификаторы персонажа", InputSpec.Object("modifier")), list("equipments", "Экипировка", InputSpec.Object("characterEquipment")), list("items", "Предметы в инвентаре", InputSpec.Object("characterItem")), list("professionSkills", "Профессии", InputSpec.Object("professionSkill")), list("stockSkills", "Характеристики", InputSpec.Object("stockSkill")), list("battleSkills", "Боевые навыки", InputSpec.Object("battleSkill")), list("boolSkills", "Состояния", InputSpec.Object("boolSkill")), list("recipeAccess", "Доступные рецепты (ID)", InputSpec.Text()), list("gainedRedemptionCodes", "Полученные промокоды", InputSpec.Object("redemption")))
    "characterEquipment" -> listOf(text("equipmentId", "Предмет экипировки (ID)"), text("uuid", "ID экземпляра", newEntityId()), list("params", "Модификаторы экземпляра", InputSpec.Object("modifier")))
    "characterItem" -> listOf(text("itemId", "Предмет (ID)"), num("amount", "Количество", 1, true, 0.0))
    "professionSkill", "battleSkill" -> listOf(choice("stat", "Навык", if(schema == "professionSkill") professionStats else battleStats), num("level", "Уровень", 0, true, 0.0, 127.0), num("experience", "Опыт", 0.0, min = 0.0))
    "stockSkill" -> listOf(choice("stat", "Характеристика", stockStats), num("value", "Значение", 0, true, Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()))
    "boolSkill" -> listOf(choice("stat", "Состояние", boolStats), flag("value", "Активно").copy(nullable = true, default = JsonNull))
    "redemption" -> listOf(text("redemptionCodeId", "ID промокода"), text("dateGained", "Дата получения (ГГГГ-ММ-ДДTчч:мм:сс)", "2026-01-01T00:00:00"))
    else -> error("Неизвестная форма: $schema")
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
                    require(p != null && !p.isString && n != null && n.isFinite() && (!spec.integer || p.longOrNull != null) && (spec.min == null || n >= spec.min) && (spec.max == null || n <= spec.max)) { "${field.label}: некорректное число" }
                }
                is InputSpec.Text -> require(element is JsonPrimitive && element.isString) { "${field.label}: требуется текст" }
                InputSpec.Flag -> require(element is JsonPrimitive && !element.isString && element.booleanOrNull != null) { "${field.label}: требуется да/нет" }
                is InputSpec.Select -> require(element is JsonPrimitive && element.content in spec.options) { "${field.label}: выберите значение" }
                is InputSpec.Union -> require(element is JsonPrimitive && element.content in spec.variants) { "${field.label}: выберите вид" }
                is InputSpec.Object -> validateForm(spec.schema, element.jsonObject)
                is InputSpec.ListOf -> element.jsonArray.forEach { checkValue(spec.element, it) }
            }
        }
        checkValue(field.spec, value)
    }
}
