package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a pool holds (server 0.56.0): item modifiers, monster modifiers, or equipment templates —
 * ordinary bases, uniques and mythic items, whose pools of one tag merge into one draw.
 */
@Serializable enum class PoolKind { MODIFIER, MONSTER, EQUIPMENT, UNIQUE, MYTHIC }

/**
 * One pool (collection `Pool`, catalogue "Pools"): a tag of one kind and the codes it holds with
 * their weights. A source — a template, a loot table, a boss, an orb — names tags in priority
 * order, and the first pool holding an entry decides its weight; 0 keeps it out. The server draws.
 */
@Serializable data class Pool(
    @SerialName("_id") val id: String = "",
    val code: String = "",
    val kind: PoolKind = PoolKind.MODIFIER,
    val entries: Map<String, Int> = emptyMap(),
)
