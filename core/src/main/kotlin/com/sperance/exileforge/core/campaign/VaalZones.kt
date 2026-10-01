package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.Zone
import kotlin.math.roundToInt

/**
 * The Vaal zone behind a map's portal: a smaller map of its own biome, peopled from the same zone by the
 * run's own Vaal rolls, with the zone's guardian of corruption sealing its exit.
 */
object VaalZones {
    const val BIOME = "VAAL"
    private const val SHARE = 0.6
    private const val MIN_SIZE = 32

    fun isZone(zone: Zone) = zone.biome == BIOME

    /** The zone's map, cut from [location]: or null for a location with no guardian to stand at its end. */
    fun zone(location: Zone): Zone? {
        if (location.corrupted.isBlank()) return null
        return location.copy(biome = BIOME, size = (location.size * SHARE).roundToInt().coerceAtLeast(MIN_SIZE), boss = location.corrupted, corrupted = "")
    }
}
