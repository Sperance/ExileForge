package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireItemId
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.RunReport
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunStart
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonNull
import java.util.UUID

private const val CAMPAIGN = "api/v1/hero/campaign"

/**
 * The campaign by seed: entering a zone answers the seed and the frozen context the client rolls the
 * run by; the run's journal is played back by the server, each number once, and its answer is the truth.
 */
class CampaignClient internal constructor(private val http: Transport) {
    suspend fun progress(heroId: String): CampaignProgress = http.get("$CAMPAIGN/progress", heroQuery(heroId))

    /**
     * Enters [mapCode], with a map item spent on it or without one. Never retried: a repeat opens another run. A [potion]
     * goes into the run and [scarabs] with the map (server 1.74.0), both spent there.
     */
    suspend fun start(heroId: String, mapCode: MapCode, itemId: String? = null, potion: String? = null, scarabs: List<String> = emptyList()): RunStart {
        itemId?.let(::requireItemId)
        return http.post(
            "$CAMPAIGN/start",
            heroQuery(
                heroId,
                "mapCode" to mapCode.value,
                "itemId" to itemId,
                "potion" to potion,
                "scarabs" to scarabs.joinToString(",").ifEmpty { null },
            ),
        )
    }

    /**
     * Abandons the open run [runId] (3.89.0, server 1.81.0): it is closed as a leave is, the loot it gave stays. Safe to repeat — a run
     * already closed or replaced is left alone.
     */
    suspend fun abandon(heroId: String, runId: String): CampaignProgress = http.post("$CAMPAIGN/abandon", heroQuery(heroId, "runId" to runId))

    /**
     * A batch of the journal of run [runId], in order; safe to repeat — numbers already applied are skipped, and a batch
     * of another run is refused (CP_026, server 1.68.0). The answer carries every event's reward (server 1.30.0), so a
     * batch whose answer was lost is sent again with the same [key]: the server repeats its stored report. Null - the
     * batch landed, but its report was not kept: the hero is read again, and the batch's loot is in it.
     */
    suspend fun events(heroId: String, runId: String, events: List<RunEvent>, key: String = UUID.randomUUID().toString()): RunReport? = http.request(
        "POST",
        "$CAMPAIGN/events",
        heroQuery(heroId, "runId" to runId),
        WireJson.encodeToJsonElement(ListSerializer(RunEvent.serializer()), events),
        authenticated = true,
        headers = mapOf(IDEMPOTENCY_HEADER to key),
    )
        .takeIf { it !is JsonNull }?.let { WireJson.decodeFromJsonElement(RunReport.serializer(), it) }
}
