package com.sperance.exileforge.core.contract

import com.sperance.exileforge.core.i18n.ui
import kotlinx.serialization.json.*

fun validateCharacter(doc: JsonObject) {
    doc["userId"]?.let { requireId(it.jsonPrimitive.content) }
    // The class is the whole base since 0.10.0: without it the server has nothing to count from.
    doc["classId"]?.let { requireId(it.jsonPrimitive.content) }
    // Equipment lives in its own collection: the character document never carries an instance.
    require("equipped" !in doc) { ui("contract.equipment_elsewhere") }
    // Base stats moved to the class, and the tree is its own collection.
    require("stockSkills" !in doc) { ui("contract.stats_from_class") }
    require("params" !in doc) { ui("contract.modifiers_server") }
    (doc["recipeAccess"] as? JsonArray).orEmpty().forEach { requireId(it.jsonPrimitive.content) }
    // The bag is a map of item id to amount since server 0.49.0; the old "itemId:amount" strings are gone.
    require("items" !in doc) { ui("contract.bag_format") }
    (doc["bag"] as? JsonObject).orEmpty().forEach { (itemId, amount) ->
        require(itemId.isNotBlank() && amount.jsonPrimitive.content.toLongOrNull() != null) { ui("contract.bag_format") }
    }
    (doc["gainedRedemptionCodes"] as? JsonArray).orEmpty().forEach {
        requireId(it.jsonObject.text("redemptionCodeId"))
    }
    listOf("professionSkills", "battleSkills", "boolSkills").forEach { field ->
        val stats = (doc[field] as? JsonArray).orEmpty().map { it.jsonObject.text("stat") }
        require(stats.distinct().size == stats.size) { ui("contract.stat_twice", field) }
    }
}
