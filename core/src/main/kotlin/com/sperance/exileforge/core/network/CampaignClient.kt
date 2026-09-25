package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.model.campaign.BossState
import com.sperance.exileforge.core.model.campaign.CampaignFall
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.CampaignReward
import com.sperance.exileforge.core.model.campaign.CampaignView
import com.sperance.exileforge.core.model.campaign.ChestState
import com.sperance.exileforge.core.model.campaign.MapLaunch
import com.sperance.exileforge.core.model.campaign.MapServiceOutcome
import com.sperance.exileforge.core.model.campaign.MonsterRarity

/** Route root of the campaign, since server 0.26.0. */
private const val CAMPAIGN = "api/v1/character/campaign"

/**
 * The campaign: its chapters, a character's progress, and the two things a run reports.
 *
 * Since 0.26.0 the fight is the client's — the owner's decision — and what it earns is not:
 * [kill] names the map, the monster and the rarity the client rolled, and the server checks the
 * map is open and the monster lives there before it rolls the experience and the loot itself.
 * Since 0.28.0 the numbers the fight is played by arrive in [chapters] as `combat`, and a death is
 * reported with [fall]: the server takes its share of the level's experience, never the level.
 */
class CampaignClient internal constructor(private val http: Transport) {
    /** The chapters with every monster and modifier already raised to its map's level. */
    suspend fun chapters(): CampaignView =
        http.get("$CAMPAIGN/chapters")

    suspend fun progress(characterId: String): CampaignProgress =
        http.get("$CAMPAIGN/progress", heroQuery(characterId))

    /** A monster the client fought and won against. Never retried: a repeat would pay twice. */
    suspend fun kill(characterId: String, mapCode: String, monsterCode: String, rarity: MonsterRarity): CampaignReward =
        http.post("$CAMPAIGN/kill", heroQuery(characterId, "mapCode" to mapCode, "monsterCode" to monsterCode, "rarity" to rarity.name))

    /** The hero reached the exit: the map is cleared and the next one opens. */
    suspend fun complete(characterId: String, mapCode: String): CampaignProgress =
        http.post("$CAMPAIGN/complete", heroQuery(characterId, "mapCode" to mapCode))

    /** The hero fell: the server prices the death by its own rule. Never retried — a repeat would charge it twice. */
    suspend fun fall(characterId: String, mapCode: String): CampaignFall =
        http.post("$CAMPAIGN/fall", heroQuery(characterId, "mapCode" to mapCode))

    /** How many chests stand on [mapCode] for this hero now (since 0.31.0): a six-hour window of the server's. */
    suspend fun chests(characterId: String, mapCode: String): ChestState =
        http.get("$CAMPAIGN/chests", heroQuery(characterId, "mapCode" to mapCode))

    /** The hero opened a chest: the server takes one from the window and rolls its loot. Never retried. */
    suspend fun openChest(characterId: String, mapCode: String): CampaignReward =
        http.post("$CAMPAIGN/chest", heroQuery(characterId, "mapCode" to mapCode))

    /** Whether the map's boss stands for this hero now (since 0.32.0): slain, it is back an hour later. */
    suspend fun boss(characterId: String, mapCode: String): BossState =
        http.get("$CAMPAIGN/boss", heroQuery(characterId, "mapCode" to mapCode))

    /** The map's boss was slain: the server rolls its loot and opens the exit. Never retried. */
    suspend fun slayBoss(characterId: String, mapCode: String): CampaignReward =
        http.post("$CAMPAIGN/boss", heroQuery(characterId, "mapCode" to mapCode))

    /** The corrupted zone's guardian was slain (server 0.46.0): its own loot table, a chance at a unique. Never retried. */
    suspend fun corrupt(characterId: String, mapCode: String, monsterCode: String): CampaignReward =
        http.post("$CAMPAIGN/corrupt", heroQuery(characterId, "mapCode" to mapCode, "monsterCode" to monsterCode))

    /** A treasure map (0.34.0): one more chest on the map this window, for gold. Never retried. */
    suspend fun treasure(characterId: String, mapCode: String): MapServiceOutcome =
        http.post("$CAMPAIGN/treasure", heroQuery(characterId, "mapCode" to mapCode))

    /** Summons a slain guardian back to the exit (0.34.0), for gold. Never retried. */
    suspend fun summon(characterId: String, mapCode: String): MapServiceOutcome =
        http.post("$CAMPAIGN/summon", heroQuery(characterId, "mapCode" to mapCode))

    /**
     * Entering a location (since 0.35.0), with a map of its level from the stash or without one. The
     * map is spent: the server keeps its effects for this location until the exit or a death. Never retried.
     */
    suspend fun start(characterId: String, mapCode: String, itemId: String? = null): MapLaunch {
        itemId?.let(::requireId)
        return http.post("$CAMPAIGN/start", heroQuery(characterId, "mapCode" to mapCode, "itemId" to itemId))
    }
}
