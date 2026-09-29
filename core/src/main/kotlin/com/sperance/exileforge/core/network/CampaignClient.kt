package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireItemId
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.RunReport
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunStart
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

    /** A batch of the run's journal, in order; safe to repeat — numbers already applied are skipped. */
    suspend fun events(heroId: String, events: List<RunEvent>): RunReport =
        http.post("$CAMPAIGN/events", heroQuery(heroId), WireJson.encodeToJsonElement(ListSerializer(RunEvent.serializer()), events))
}
