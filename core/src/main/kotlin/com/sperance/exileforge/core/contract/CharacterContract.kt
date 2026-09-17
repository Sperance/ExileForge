package com.sperance.exileforge.core.contract

import kotlinx.serialization.json.*
import com.sperance.exileforge.core.i18n.tr

fun validateCharacter(doc: JsonObject) {
    requireId(doc.text("userId"))
    (doc["params"] as? JsonArray).orEmpty().forEach { validateModifier(it.jsonObject) }
    // Equipment and owned units live in their own collections: the character document carries neither.
    (doc["equipped"] as? JsonObject).orEmpty().values.forEach { requireId(it.jsonPrimitive.content) }
    (doc["recipeAccess"] as? JsonArray).orEmpty().forEach { requireId(it.jsonPrimitive.content) }
    (doc["gainedRedemptionCodes"] as? JsonArray).orEmpty().forEach {
        requireId(it.jsonObject.text("redemptionCodeId"))
        java.time.LocalDateTime.parse(it.jsonObject.text("dateGained"))
    }
    listOf("professionSkills", "stockSkills", "battleSkills", "boolSkills").forEach { field ->
        val stats = (doc[field] as? JsonArray).orEmpty().map { it.jsonObject.text("stat") }
        require(stats.distinct().size == stats.size) { tr("$field: характеристика указана несколько раз", "$field: the stat is listed more than once") }
    }
}
