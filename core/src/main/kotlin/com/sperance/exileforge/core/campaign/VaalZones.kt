package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.CampaignMap
import kotlin.math.roundToInt

/**
 * The Vaal zone behind a map's portal (since 2.65.0, server 0.57.0): a smaller map of its own biome,
 * peopled from the same location, with the location's guardian of corruption sealing its exit. Its
 * modifiers are the server's roll and lie on the zone's run as a map item's effects do.
 */
object VaalZones {
    const val BIOME = "VAAL"
    /** How much of the location's size and crowd the zone keeps. */
    private const val SHARE = 0.6
    private const val MIN_SIZE = 32
    private const val MIN_MONSTERS = 5

    /** Share of full life a hero who fell in the zone wakes with by the portal. */
    const val WAKE_LIFE = 0.3

    fun isZone(map: CampaignMap) = map.biome == BIOME

    /** The zone's map, cut from [location]: or null for a location with no guardian to stand at its end. */
    fun map(location: CampaignMap): CampaignMap? {
        val guardian = location.corrupted ?: return null
        return location.copy(
            biome = BIOME,
            size = (location.size * SHARE).roundToInt().coerceAtLeast(MIN_SIZE),
            monsterCount = location.monsterCount.map { (it * SHARE).roundToInt().coerceAtLeast(MIN_MONSTERS) },
            boss = guardian,
            corrupted = null,
        )
    }
}
