package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.crafts.CraftsState

private const val CRAFTS = "api/v1/hero/crafts"

/** The crafts: the work runs on the server by time, every answer is the whole state with the cycles counted up to now. */
class CraftsClient internal constructor(private val http: Transport) {
    suspend fun state(heroId: String): CraftsState = http.get(CRAFTS, heroQuery(heroId))

    /** Starts [job] — as its variant [choice] when it is a choosing work — ending the work under way. Never retried. */
    suspend fun start(heroId: String, job: String, choice: String = "", additives: List<String> = emptyList()): CraftsState =
        http.post("$CRAFTS/start", heroQuery(heroId, "job" to job, "choice" to choice.ifEmpty { null }, "additives" to additives.takeIf { it.isNotEmpty() }?.joinToString(",")))

    suspend fun stop(heroId: String): CraftsState = http.post("$CRAFTS/stop", heroQuery(heroId))
}
