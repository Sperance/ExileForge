package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.model.campaign.CampaignFall
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.CampaignReward
import com.sperance.exileforge.core.model.campaign.CampaignView
import com.sperance.exileforge.core.model.campaign.ChestState
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import kotlinx.serialization.json.decodeFromJsonElement

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
        WireJson.decodeFromJsonElement(http.request("GET", "$CAMPAIGN/chapters", authenticated = true))

    suspend fun progress(characterId: String): CampaignProgress {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", "$CAMPAIGN/progress", mapOf("characterId" to characterId), authenticated = true))
    }

    /** A monster the client fought and won against. Never retried: a repeat would pay twice. */
    suspend fun kill(characterId: String, mapCode: String, monsterCode: String, rarity: MonsterRarity): CampaignReward {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$CAMPAIGN/kill",
            mapOf("characterId" to characterId, "mapCode" to mapCode, "monsterCode" to monsterCode, "rarity" to rarity.name), authenticated = true))
    }

    /** The hero reached the exit: the map is cleared and the next one opens. */
    suspend fun complete(characterId: String, mapCode: String): CampaignProgress {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$CAMPAIGN/complete",
            mapOf("characterId" to characterId, "mapCode" to mapCode), authenticated = true))
    }

    /** The hero fell: the server prices the death by its own rule. Never retried — a repeat would charge it twice. */
    suspend fun fall(characterId: String, mapCode: String): CampaignFall {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$CAMPAIGN/fall",
            mapOf("characterId" to characterId, "mapCode" to mapCode), authenticated = true))
    }

    /** How many chests stand on [mapCode] for this hero now (since 0.31.0): a six-hour window of the server's. */
    suspend fun chests(characterId: String, mapCode: String): ChestState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", "$CAMPAIGN/chests",
            mapOf("characterId" to characterId, "mapCode" to mapCode), authenticated = true))
    }

    /** The hero opened a chest: the server takes one from the window and rolls its loot. Never retried. */
    suspend fun openChest(characterId: String, mapCode: String): CampaignReward {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$CAMPAIGN/chest",
            mapOf("characterId" to characterId, "mapCode" to mapCode), authenticated = true))
    }
}
