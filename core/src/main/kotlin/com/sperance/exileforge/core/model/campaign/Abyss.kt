package com.sperance.exileforge.core.model.campaign

import kotlinx.serialization.Serializable

/**
 * The hoard of the Abyss at the end of a depth (server 0.72.0), all the depths above it counted in: so
 * many items — magic, [rare] percent of them rare — and orbs, low to high, a [unique]'s chance in percent
 * and the [experience] it pays, the map's, the atlas's and the hero's bonuses already in.
 */
@Serializable data class AbyssHoard(
    val items: List<Int> = listOf(0, 0),
    val rare: Double = 0.0,
    val orbs: List<Int> = listOf(0, 0),
    val unique: Double = 0.0,
    val experience: Double = 0.0,
)

/**
 * One depth of the Abyss (server 0.72.0): its wave — [count] monsters, [magic] and [rare] percent of them
 * magic and rare — of its own [monsters] raised to its [level], the [leader] that stands up with it at
 * the depths that have one, and the [hoard] waiting at its end.
 */
@Serializable data class AbyssDepth(
    val level: Int = 1,
    val count: List<Int> = listOf(1, 1),
    val magic: Double = 0.0,
    val rare: Double = 0.0,
    val monsters: List<CampaignMonster> = emptyList(),
    val leader: CampaignBoss? = null,
    val hoard: AbyssHoard = AbyssHoard(),
)

/**
 * The zone's Abyss for this entry (server 0.72.0): how deep each of its [cracks] leads with the map
 * entered, the [depths] in order, the modifiers its magic and rare monsters draw from at the zone's
 * level, and the share of the hoard in percent a fall does not burn, [keep].
 */
@Serializable data class AbyssLaunch(
    val cracks: List<Int> = emptyList(),
    val refreshAt: Long = 0,
    val depths: List<AbyssDepth> = emptyList(),
    val modifiers: List<MonsterModifier> = emptyList(),
    val keep: Double = 0.0,
)

/** A crack opened (server 0.72.0): how deep the descent goes, and the zone's cracks still standing after it. */
@Serializable data class AbyssOpened(val depth: Int = 0, val cracks: List<Int> = emptyList(), val refreshAt: Long = 0)
