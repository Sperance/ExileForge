package com.sperance.exileforge.core.contract

import com.sperance.exileforge.core.i18n.tr
import kotlinx.serialization.json.*

fun validateCharacter(doc: JsonObject) {
    doc["userId"]?.let { requireId(it.jsonPrimitive.content) }
    // The class is the whole base since 0.10.0: without it the server has nothing to count from.
    doc["classId"]?.let { requireId(it.jsonPrimitive.content) }
    // Equipment lives in its own collection: the character document never carries an instance.
    require("equipped" !in doc) { tr("Экипировка хранится отдельной коллекцией", "Equipment is stored in its own collection") }
    // Base stats moved to the class, and the tree is its own collection.
    require("stockSkills" !in doc) { tr("Базовые характеристики задаёт класс персонажа", "The base stats come from the character's class") }
    require("params" !in doc) { tr("Модификаторы персонажа считает сервер по классу и дереву", "A character's modifiers come from the class and the tree, on the server") }
    (doc["recipeAccess"] as? JsonArray).orEmpty().forEach { requireId(it.jsonPrimitive.content) }
    // Bag items are the flat storage strings "itemId:amount" the server reads back itself.
    (doc["items"] as? JsonArray).orEmpty().forEach { raw ->
        val entry = raw.jsonPrimitive.content
        require(entry.substringAfterLast(':').toLongOrNull() != null && entry.substringBeforeLast(':').isNotBlank()) {
            tr("Предмет сумки записывается как «itemId:количество»", "A bag item is written as \"itemId:amount\"")
        }
    }
    (doc["gainedRedemptionCodes"] as? JsonArray).orEmpty().forEach {
        requireId(it.jsonObject.text("redemptionCodeId"))
    }
    listOf("professionSkills", "battleSkills", "boolSkills").forEach { field ->
        val stats = (doc[field] as? JsonArray).orEmpty().map { it.jsonObject.text("stat") }
        require(stats.distinct().size == stats.size) { tr("$field: характеристика указана несколько раз", "$field: the stat is listed more than once") }
    }
}
