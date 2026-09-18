package com.sperance.exileforge.core.contract

import com.sperance.exileforge.core.i18n.tr
import kotlinx.serialization.json.*

fun validateCharacter(doc: JsonObject) {
    doc["userId"]?.let { requireId(it.jsonPrimitive.content) }
    (doc["params"] as? JsonArray).orEmpty().forEach { validateModifier(it.jsonObject) }
    // Equipment lives in its own collection: the character document never carries an instance.
    require("equipped" !in doc) { tr("Экипировка хранится отдельной коллекцией", "Equipment is stored in its own collection") }
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
    listOf("professionSkills", "stockSkills", "battleSkills", "boolSkills").forEach { field ->
        val stats = (doc[field] as? JsonArray).orEmpty().map { it.jsonObject.text("stat") }
        require(stats.distinct().size == stats.size) { tr("$field: характеристика указана несколько раз", "$field: the stat is listed more than once") }
    }
}
