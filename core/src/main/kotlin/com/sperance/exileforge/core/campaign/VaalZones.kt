package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.VaalRule
import com.sperance.exileforge.rules.content.Zone
import kotlin.math.roundToInt

/**
 * The Vaal zone behind a map's portal: a smaller map of its own biome, peopled from the same zone by the
 * run's own Vaal rolls, with the zone's guardian of corruption sealing its exit.
 */
object VaalZones {
    const val BIOME = "VAAL"

    fun isZone(zone: Zone) = zone.biome == BIOME

    /**
     * The zone's map, cut from [location] by the rule [vaal] (доля и наименьший размер - контент `vaal.zoneShare|zoneMinSize`):
     * or null for a location with no guardian to stand at its end.
     */
    fun zone(location: Zone, vaal: VaalRule): Zone? {
        if (location.corrupted.value.isBlank()) return null
        return location.copy(biome = BIOME, size = (location.size * vaal.zoneShare).roundToInt().coerceAtLeast(vaal.zoneMinSize), boss = location.corrupted, corrupted = MonsterCode(""))
    }
}
