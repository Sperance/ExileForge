package com.sperance.exileforge.core.contract

import kotlinx.serialization.json.*

fun validateCharacter(doc: JsonObject) {
    requireId(doc.text("userId"))
    (doc["params"] as? JsonArray).orEmpty().forEach { validateModifier(it.jsonObject) }
    (doc["equipments"] as? JsonArray).orEmpty().forEach {
        val equipment = it.jsonObject
        requireId(equipment.text("equipmentId")); requireId(equipment.text("uuid"))
        (equipment["params"] as? JsonArray).orEmpty().forEach { mod -> validateModifier(mod.jsonObject) }
    }
    (doc["items"] as? JsonArray).orEmpty().forEach { requireId(it.jsonObject.text("itemId")) }
    (doc["recipeAccess"] as? JsonArray).orEmpty().forEach { requireId(it.jsonPrimitive.content) }
    (doc["gainedRedemptionCodes"] as? JsonArray).orEmpty().forEach {
        requireId(it.jsonObject.text("redemptionCodeId"))
        java.time.LocalDateTime.parse(it.jsonObject.text("dateGained"))
    }
    listOf("professionSkills", "stockSkills", "battleSkills", "boolSkills").forEach { field ->
        val stats = (doc[field] as? JsonArray).orEmpty().map { it.jsonObject.text("stat") }
        require(stats.distinct().size == stats.size) { "$field: характеристика указана несколько раз" }
    }
}
