package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.campaign.TrialReport
import com.sperance.exileforge.core.model.campaign.TrialStart
import com.sperance.exileforge.rules.content.TrialEvent
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer

private const val TRIALS = "api/v1/hero/trials"

/**
 * The trials (server 1.47.0): the boss rush of a cleared region and the endless tower. Entering spends the key and
 * answers the trial and the frozen context; its journal — a boss down, a floor cleared, the end — is played back by
 * the server, each number once, and the answer names what each event brought.
 */
class TrialClient internal constructor(private val http: Transport) {
    /** Enters the rush of [region]: a rush key is spent. Never retried: a repeat spends another key. */
    suspend fun rush(heroId: String, region: String): TrialStart = http.post("$TRIALS/rush", heroQuery(heroId, "region" to region))

    /** Forges a rush key from five crest fragments (server 1.48.0); the hero's bag comes back with it. */
    suspend fun forgeKey(heroId: String): Map<String, Long> = http.post("$TRIALS/key", heroQuery(heroId))

    /** Enters the tower from its last checkpoint: a tower seal is spent. */
    suspend fun tower(heroId: String): TrialStart = http.post("$TRIALS/tower", heroQuery(heroId))

    /** A batch of the trial's journal, in order; a lost answer is asked for again under the same [key]. */
    suspend fun events(heroId: String, events: List<TrialEvent>, key: String = UUID.randomUUID().toString()): TrialReport =
        WireJson.decodeFromJsonElement(TrialReport.serializer(), http.request("POST", "$TRIALS/events", heroQuery(heroId),
            WireJson.encodeToJsonElement(ListSerializer(TrialEvent.serializer()), events), authenticated = true, headers = mapOf(IDEMPOTENCY_HEADER to key)))
}
