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
import com.sperance.exileforge.core.model.campaign.VaalZone
import com.sperance.exileforge.core.model.essences.CrystalState
import com.sperance.exileforge.core.model.essences.CrystalVaal

/** Route root of the campaign, since server 0.26.0. */
private const val CAMPAIGN = "api/v1/character/campaign"

/**
 * The campaign: its world map, a character's progress, and the two things a run reports.
 *
 * Since 0.26.0 the fight is the client's — the owner's decision — and what it earns is not:
 * [kill] names the map, the monster and the rarity the client rolled, and the server checks the
 * map is open and the monster lives there before it rolls the experience and the loot itself.
 * Since 0.28.0 the numbers the fight is played by arrive in [world] as `combat`, and a death is
 * reported with [fall]: the server takes its share of the level's experience, never the level.
 */
class CampaignClient internal constructor(private val http: Transport) {
    /** The world map (server 0.67.0), every monster and modifier already raised to its zone's level. */
    suspend fun world(): CampaignView =
        http.get("$CAMPAIGN/world")

    suspend fun progress(characterId: String): CampaignProgress =
        http.get("$CAMPAIGN/progress", heroQuery(characterId))

    /** A monster the client fought and won against. Never retried: a repeat would pay twice. */
    suspend fun kill(characterId: String, mapCode: String, monsterCode: String, rarity: MonsterRarity, vaal: Boolean = false): CampaignReward =
        http.post("$CAMPAIGN/kill", heroQuery(characterId, "mapCode" to mapCode, "monsterCode" to monsterCode, "rarity" to rarity.name,
            "vaal" to if (vaal) "true" else null))

    /** The hero left through the exit back to the world map (server 0.67.0): the boss has passed the zone already. */
    suspend fun leave(characterId: String, mapCode: String): CampaignProgress =
        http.post("$CAMPAIGN/leave", heroQuery(characterId, "mapCode" to mapCode))

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

    /** The map's boss was slain: the server rolls its loot, opens the exit and passes the zone (0.67.0). Never retried. */
    suspend fun slayBoss(characterId: String, mapCode: String): CampaignReward =
        http.post("$CAMPAIGN/boss", heroQuery(characterId, "mapCode" to mapCode))

    /**
     * The guardian at the Vaal zone's end was slain (server 0.57.0; the corrupted zone's since 0.46.0):
     * its own loot table with the zone's bonus, a chance at a unique, and the zone is closed. Never retried.
     */
    suspend fun corrupt(characterId: String, mapCode: String, monsterCode: String): CampaignReward =
        http.post("$CAMPAIGN/corrupt", heroQuery(characterId, "mapCode" to mapCode, "monsterCode" to monsterCode))

    /** The Vaal zone behind this run's portal (server 0.57.0): rolled once a run, the same answer until it is closed. */
    suspend fun vaal(characterId: String, mapCode: String): VaalZone =
        http.post("$CAMPAIGN/vaal", heroQuery(characterId, "mapCode" to mapCode))

    /** The Vaal zone closed without its guardian — refused at the gate or died in (server 0.57.0). Never retried. */
    suspend fun vaalLeave(characterId: String, mapCode: String): CampaignFall =
        http.post("$CAMPAIGN/vaal/leave", heroQuery(characterId, "mapCode" to mapCode))

    /** The crystals of essences in [mapCode] for this hero now (server 0.69.0): a window like the chests'. */
    suspend fun crystals(characterId: String, mapCode: String): CrystalState =
        http.get("$CAMPAIGN/crystals", heroQuery(characterId, "mapCode" to mapCode))

    /**
     * The guardian of crystal [index] was slain (server 0.69.0): its essences go to the bag, its loot is a
     * rare monster's, and the crystal leaves the window. Never retried — a repeat would pay twice.
     */
    suspend fun freeCrystal(characterId: String, mapCode: String, index: Int): CampaignReward =
        http.post("$CAMPAIGN/crystal", heroQuery(characterId, "mapCode" to mapCode, "index" to index.toString()))

    /** A Vaal orb on crystal [index] (server 0.69.0), once a crystal: its essences higher, one special, or a stronger guardian. Never retried. */
    suspend fun vaalCrystal(characterId: String, mapCode: String, index: Int): CrystalVaal =
        http.post("$CAMPAIGN/crystal/vaal", heroQuery(characterId, "mapCode" to mapCode, "index" to index.toString()))

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
