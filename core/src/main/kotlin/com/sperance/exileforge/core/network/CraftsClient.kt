package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.model.crafts.CraftsState
import kotlinx.serialization.json.decodeFromJsonElement

/** Route root of the crafts, since server 0.37.0. */
private const val CRAFTS = "api/v1/character/crafts"

/**
 * The crafts (since server 0.37.0). The work runs on the server by time: every answer is the whole
 * state with the cycles counted up to now, whatever they brought already in the bag. The client
 * names a work and prints what came back.
 */
class CraftsClient internal constructor(private val http: Transport) {
    suspend fun state(characterId: String): CraftsState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", CRAFTS, mapOf("characterId" to characterId), authenticated = true))
    }

    /** Starts [job], ending the work under way. Never retried. */
    suspend fun start(characterId: String, job: String): CraftsState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$CRAFTS/start", mapOf("characterId" to characterId, "job" to job), authenticated = true))
    }

    suspend fun stop(characterId: String): CraftsState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$CRAFTS/stop", mapOf("characterId" to characterId), authenticated = true))
    }
}
