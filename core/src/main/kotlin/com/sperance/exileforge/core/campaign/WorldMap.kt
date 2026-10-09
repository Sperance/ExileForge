package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.rules.content.CampaignFile
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.Region
import com.sperance.exileforge.rules.content.WorldGraph
import com.sperance.exileforge.rules.content.WorldPoint
import com.sperance.exileforge.rules.content.Zone

/** What a zone's token shows: its boss slain, open to enter, or «???» behind an open one. */
enum class TokenState { PASSED, OPEN, LOCKED }

/** A link between two shown tokens: a road already walked, the way ahead, or a path no one took yet. */
enum class RoadState { WALKED, AHEAD, UNTRODDEN }

data class WorldToken(val zone: Zone, val state: TokenState, val deadEnd: Boolean)

data class WorldRoad(val from: WorldToken, val to: WorldToken, val state: RoadState)

/**
 * The world map as one hero sees it: the content's zones and the fog over them. A zone the hero passed or
 * may enter is shown, and so is a zone one of those leads to — as «???» with its level; the rest is fog.
 */
class WorldMap(val campaign: CampaignFile, private val world: WorldGraph, progress: CampaignProgress) {
    private val passed = progress.cleared.toHashSet()
    private val open = progress.unlocked.toHashSet()

    val tokens: List<WorldToken> = campaign.zones.mapNotNull { zone ->
        val state = when {
            zone.code in passed -> TokenState.PASSED
            zone.code in open -> TokenState.OPEN
            zone.from.any { it in open } -> TokenState.LOCKED
            else -> return@mapNotNull null
        }
        WorldToken(zone, state, world.next(zone.code).isEmpty() && !zone.finale)
    }

    private val byCode: Map<MapCode, WorldToken> = tokens.associateBy { it.zone.code }

    fun token(code: MapCode): WorldToken? = byCode[code]

    /** Links between shown tokens: walked where both are passed, ahead from a passed one to one still open. */
    val roads: List<WorldRoad> = tokens.flatMap { to ->
        to.zone.from.mapNotNull(byCode::get).map { from ->
            WorldRoad(
                from,
                to,
                when {
                    from.state != TokenState.PASSED -> RoadState.UNTRODDEN
                    to.state == TokenState.PASSED -> RoadState.WALKED
                    else -> RoadState.AHEAD
                },
            )
        }
    }

    /** The regions the hero has seen a token of; the rest are fog, name and all. */
    val knownRegions: List<Region> = campaign.regions.filter { region -> region.zones.any { it.code in byCode } }

    /** The highest token the hero can see: the fog thickens above it. */
    val fogLine: Int = tokens.maxOfOrNull { it.zone.y } ?: 0

    /** Where the camera looks first: the middle of the open zones of the highest level, or of every shown token. */
    fun frontier(): WorldPoint {
        val openTokens = tokens.filter { it.state == TokenState.OPEN }
        val top = openTokens.maxOfOrNull { it.zone.level }
        val ahead = openTokens.filter { it.zone.level == top }.ifEmpty { tokens }
        if (ahead.isEmpty()) return WorldPoint(campaign.world.width / 2, 0)
        return WorldPoint(ahead.sumOf { it.zone.x } / ahead.size, ahead.sumOf { it.zone.y } / ahead.size)
    }

    /** The zones that lead into [code] and are shown open or passed: whose boss will open it. */
    fun keysTo(code: MapCode): List<Zone> = byCode[code]?.zone?.from.orEmpty().mapNotNull(byCode::get).filter { it.state != TokenState.LOCKED }.map { it.zone }

    companion object {
        /** Where one region ends and the next begins: halfway between the one's top zone and the other's lowest. */
        fun borders(campaign: CampaignFile): List<Int> = campaign.regions.filter { it.zones.isNotEmpty() }
            .zipWithNext { lower, upper -> (lower.zones.maxOf { it.y } + upper.zones.minOf { it.y }) / 2 }
    }
}
