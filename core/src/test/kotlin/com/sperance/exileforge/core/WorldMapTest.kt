package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.RoadState
import com.sperance.exileforge.core.campaign.TokenState
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.CampaignRegion
import com.sperance.exileforge.core.model.campaign.CampaignView
import kotlin.test.Test
import kotlin.test.assertEquals

/** The world map as a hero sees it (2.76.0): what is shown, what the fog keeps, how the roads read. */
class WorldMapTest {

    private fun zone(code: String, level: Int, x: Int, y: Int, vararg from: String) = CampaignMap(code, x = x, y = y, from = from.toList(), level = level)

    //        D   E            level 5
    //       / \ /
    //      B   C              level 3
    //       \ /
    //        A                level 1
    private val view = CampaignView(regions = listOf(CampaignRegion("R1", zones = listOf(
        zone("A", 1, 100, 0), zone("B", 3, 50, 100, "A"), zone("C", 3, 150, 100, "A"), zone("D", 5, 100, 200, "B", "C"), zone("E", 5, 200, 200, "C")))))

    @Test
    fun the_fog_keeps_all_but_the_open_zones_and_the_ones_they_lead_to() {
        assertEquals(mapOf("A" to TokenState.OPEN, "B" to TokenState.LOCKED, "C" to TokenState.LOCKED),
            WorldMap(view, CampaignProgress(unlocked = listOf("A"))).tokens.associate { it.zone.code to it.state })
        assertEquals(mapOf("A" to TokenState.PASSED, "B" to TokenState.OPEN, "C" to TokenState.OPEN, "D" to TokenState.LOCKED, "E" to TokenState.LOCKED),
            WorldMap(view, CampaignProgress(cleared = listOf("A"), unlocked = listOf("A", "B", "C"))).tokens.associate { it.zone.code to it.state })
    }

    @Test
    fun a_road_is_walked_ahead_or_untrodden() {
        val world = WorldMap(view, CampaignProgress(cleared = listOf("A", "B"), unlocked = listOf("A", "B", "C", "D")))
        val roads = world.roads.associate { "${it.from.zone.code}${it.to.zone.code}" to it.state }
        assertEquals(mapOf("AB" to RoadState.WALKED, "AC" to RoadState.AHEAD, "BD" to RoadState.AHEAD, "CD" to RoadState.UNTRODDEN, "CE" to RoadState.UNTRODDEN), roads)
        assertEquals(listOf("B", "C"), world.keysTo("D").map { it.code })
    }

    @Test
    fun the_frontier_is_the_middle_of_the_highest_open_zones() {
        val world = WorldMap(view, CampaignProgress(cleared = listOf("A", "C"), unlocked = listOf("A", "B", "C", "D", "E")))
        assertEquals(150 to 200, world.frontier().let { it.x to it.y })
    }
}
