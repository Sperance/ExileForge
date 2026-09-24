package com.sperance.exileforge.core.model

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.documentTitle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Catalogue filter.
 *
 * The server serves `/paged` with nothing but `page` and `size`, so filtering is a display concern
 * here: when a filter is set the client reads the whole collection once and narrows it itself. No
 * game number is computed — only fields the server already wrote are compared.
 */
@Serializable data class CatalogFilter(
    val query: String = "", val slot: String = "", val rarity: String = "",
    val minLevel: String = "", val maxLevel: String = "", val weaponType: String = "",
    /** A pool tag: templates that roll from it or sit in it (since server 0.39.0). */
    val pool: String = "",
) {
    val isEmpty: Boolean get() = listOf(query, slot, rarity, minLevel, maxLevel, weaponType, pool).all { it.isBlank() }

    fun matches(document: JsonObject): Boolean {
        val level = document.text("itemLevel").toIntOrNull()
        // Content carries a code, not a name, so the text is matched against what the player is
        // actually shown — the locale bundle's string — as well as the code and the raw fields.
        return (query.isBlank() || listOf("name", "code", "category", "subCategory").any { document.text(it).contains(query.trim(), true) }
                || documentTitle(document).contains(query.trim(), true))
            && (slot.isBlank() || document.text("slot") == slot)
            && (rarity.isBlank() || document.text("rarity") == rarity)
            && (weaponType.isBlank() || document.text("weaponType") == weaponType)
            && (minLevel.toIntOrNull()?.let { level != null && level >= it } ?: true)
            && (maxLevel.toIntOrNull()?.let { level != null && level <= it } ?: true)
            && (pool.isBlank() || (document["modifierPools"] as? JsonArray).orEmpty().any { it.jsonPrimitive.content == pool } || (document["pools"] as? JsonObject)?.containsKey(pool) == true)
    }
}
