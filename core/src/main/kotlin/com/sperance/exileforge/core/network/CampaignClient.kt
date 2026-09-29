package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireItemId
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.RunReport
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunStart
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer

private const val CAMPAIGN = "api/v1/hero/campaign"

/**
 * The campaign by seed: entering a zone answers the seed and the frozen context the client rolls the
 * run by; the run's journal is played back by the server, each number once, and its answer is the truth.
 */
class CampaignClient internal constructor(private val http: Transport) {
    suspend fun progress(heroId: String): CampaignProgress = http.get("$CAMPAIGN/progress", heroQuery(heroId))

    /** Enters [mapCode], with a map item spent on it or without one. Never retried: a repeat opens another run. */
    suspend fun start(heroId: String, mapCode: String, itemId: String? = null): RunStart {
        itemId?.let(::requireItemId)
        return http.post("$CAMPAIGN/start", heroQuery(heroId, "mapCode" to mapCode, "itemId" to itemId))
    }

    /**
     * A batch of the run's journal, in order; safe to repeat — numbers already applied are skipped. The answer
     * carries every event's reward (server 1.30.0), so a batch whose answer was lost is sent again with the same
     * [key]: the server repeats its stored answer, rewards and all, instead of an empty one.
     */
    suspend fun events(heroId: String, events: List<RunEvent>, key: String = UUID.randomUUID().toString()): RunReport =
        WireJson.decodeFromJsonElement(RunReport.serializer(), http.request("POST", "$CAMPAIGN/events", heroQuery(heroId),
            WireJson.encodeToJsonElement(ListSerializer(RunEvent.serializer()), events), authenticated = true, headers = mapOf(IDEMPOTENCY_HEADER to key)))
}
