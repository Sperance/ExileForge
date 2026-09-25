package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.crafts.CraftsState

/** Route root of the crafts, since server 0.37.0. */
private const val CRAFTS = "api/v1/character/crafts"

/**
 * The crafts (since server 0.37.0). The work runs on the server by time: every answer is the whole
 * state with the cycles counted up to now, whatever they brought already in the bag. The client
 * names a work and prints what came back.
 */
class CraftsClient internal constructor(private val http: Transport) {
    suspend fun state(characterId: String): CraftsState =
        http.get(CRAFTS, heroQuery(characterId))

    /** Starts [job], ending the work under way. Never retried. */
    suspend fun start(characterId: String, job: String, additives: List<String> = emptyList()): CraftsState {
        val query = heroQuery(characterId, "job" to job, "additives" to additives.takeIf { it.isNotEmpty() }?.joinToString(","))
        return http.post("$CRAFTS/start", query)
    }

    suspend fun stop(characterId: String): CraftsState =
        http.post("$CRAFTS/stop", heroQuery(characterId))
}
