package com.sperance.exileforge.core.model

import kotlinx.serialization.Serializable

@Serializable data class CatalogFilter(val query: String = "", val slot: String = "", val rarity: String = "", val minLevel: String = "", val maxLevel: String = "", val stat: String = "", val minStat: String = "", val modifierId: String = "") {
    fun parameters(): Map<String, String> = mapOf("q" to query.trim(), "slot" to slot, "rarity" to rarity, "minLevel" to minLevel, "maxLevel" to maxLevel, "stat" to stat, "minStat" to minStat, "modifierId" to modifierId).filterValues { it.isNotBlank() }
}
