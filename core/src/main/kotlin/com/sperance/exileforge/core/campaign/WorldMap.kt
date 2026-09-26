package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.CampaignRegion
import com.sperance.exileforge.core.model.campaign.CampaignView
import com.sperance.exileforge.core.model.campaign.WorldPoint

/** What a zone's token shows (2.76.0): its boss slain, open to enter, or «???» behind an open one. */
enum class TokenState { PASSED, OPEN, LOCKED }

/** A link between two shown tokens: a road already walked, the way ahead, or a path no one took yet. */
enum class RoadState { WALKED, AHEAD, UNTRODDEN }

data class WorldToken(val zone: CampaignMap, val state: TokenState) {
    /** Nowhere to lead and not its region's finale: a dead end. */
    val deadEnd: Boolean get() = zone.to.isEmpty() && !zone.finale
}

data class WorldRoad(val from: WorldToken, val to: WorldToken, val state: RoadState)

/**
 * The world map as one hero sees it (2.76.0, server 0.67.0): the server's zones and the fog over them.
 *
 * A zone the hero passed or may enter is shown, and so is a zone one of those leads to — as «???»
 * with its level; everything past that is fog. A zone opens once the boss of any zone leading to it
 * is slain, which is the server's rule and its `unlocked`: nothing here decides who may enter.
 */
class WorldMap(val view: CampaignView, progress: CampaignProgress) {
    private val passed = progress.cleared.toHashSet()
    private val open = progress.unlocked.toHashSet()

    val tokens: List<WorldToken> = view.zones.mapNotNull { zone ->
        when {
            zone.code in passed -> WorldToken(zone, TokenState.PASSED)
            zone.code in open -> WorldToken(zone, TokenState.OPEN)
            zone.from.any { it in open } -> WorldToken(zone, TokenState.LOCKED)
            else -> null
        }
    }

    private val byCode: Map<String, WorldToken> = tokens.associateBy { it.zone.code }

    fun token(code: String): WorldToken? = byCode[code]

    /** Links between shown tokens: walked where both are passed, ahead from a passed one to one still open. */
    val roads: List<WorldRoad> = tokens.flatMap { to ->
        to.zone.from.mapNotNull(byCode::get).map { from ->
            WorldRoad(from, to, when {
                from.state != TokenState.PASSED -> RoadState.UNTRODDEN
                to.state == TokenState.PASSED -> RoadState.WALKED
                else -> RoadState.AHEAD
            })
        }
    }

    val passedCount: Int = view.zones.count { it.code in passed }
    val total: Int = view.zones.size

    /** The regions the hero has seen a token of; the rest are fog, name and all. */
    val knownRegions: List<CampaignRegion> = view.regions.filter { region -> region.zones.any { it.code in byCode } }

    /** The highest token the hero can see: the fog thickens above it. */
    val fogLine: Int = tokens.maxOfOrNull { it.zone.y } ?: 0


    /**
     * Where the camera looks first: the middle of the open zones of the highest level — the frontier —
     * or of every shown token when nothing is open.
     */
    fun frontier(): WorldPoint {
        val openTokens = tokens.filter { it.state == TokenState.OPEN }
        val top = openTokens.maxOfOrNull { it.zone.level }
        val ahead = openTokens.filter { it.zone.level == top }.ifEmpty { tokens }
        if (ahead.isEmpty()) return WorldPoint(view.world.width / 2, 0)
        return WorldPoint(ahead.sumOf { it.zone.x } / ahead.size, ahead.sumOf { it.zone.y } / ahead.size)
    }

    /** The zones that lead into [code] and are shown open or passed: whose boss will open it. */
    fun keysTo(code: String): List<CampaignMap> =
        byCode[code]?.zone?.from.orEmpty().mapNotNull(byCode::get).filter { it.state != TokenState.LOCKED }.map { it.zone }

    companion object {
        /** Where one region ends and the next begins: halfway between the one's top zone and the other's lowest. */
        fun borders(view: CampaignView): List<Int> = view.regions.filter { it.zones.isNotEmpty() }
            .zipWithNext { lower, upper -> (lower.zones.maxOf { it.y } + upper.zones.minOf { it.y }) / 2 }
    }
}
