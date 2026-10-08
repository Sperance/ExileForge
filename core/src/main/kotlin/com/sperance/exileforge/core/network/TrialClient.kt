package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.campaign.RiftReport
import com.sperance.exileforge.core.model.campaign.TrialReport
import com.sperance.exileforge.core.model.campaign.TrialStart
import com.sperance.exileforge.rules.content.RiftStyle
import com.sperance.exileforge.rules.content.TrialBoard
import com.sperance.exileforge.rules.content.TrialEvent
import com.sperance.exileforge.rules.content.TrialTable
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftBoard
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonNull
import java.util.UUID

private const val TRIALS = "api/v1/hero/trials"

/**
 * The trials (server 1.47.0): the boss rush of a cleared region and the endless tower. Entering spends the key and
 * answers the trial and the frozen context; its journal — a boss down, a floor cleared, the end — is played back by
 * the server, each number once, and the answer names what each event brought.
 */
class TrialClient internal constructor(private val http: Transport) {
    /** Enters the rush of [region] at [tier] (server 1.83.0, from 0): a rush key is spent. Never retried: a repeat spends another key. */
    suspend fun rush(heroId: String, region: String, tier: Int = 0): TrialStart = http.post("$TRIALS/rush", heroQuery(heroId, "region" to region, "tier" to tier.toString()))

    /** Таблица испытаний (сервер 1.83.0): [board] с разделом [scope] в лиге [league] (null - лига героя). */
    suspend fun table(heroId: String, board: TrialBoard, scope: String = "", league: Int? = null): TrialTable = http.get("$TRIALS/table", heroQuery(heroId, "board" to board.name, "scope" to scope, *listOfNotNull(league?.let { "league" to it.toString() }).toTypedArray()))

    /** Доска Разлома недели (сервер 1.83.0). */
    suspend fun riftBoard(heroId: String): RiftBoard = http.get("$TRIALS/rift/board", heroQuery(heroId))

    /** Забег в Разлом: сверх бесплатных тратит ключ; открытый забег возвращается как есть. [styles] - стиль героя для даров. */
    suspend fun riftStart(heroId: String, styles: List<RiftStyle>): RiftBoard = http.post("$TRIALS/rift/start", heroQuery(heroId, "styles" to styles.joinToString(",") { it.name }))

    /** Действие забега [runId] под ключом [key]: повтор с тем же ключом сервер не выполняет второй раз. */
    suspend fun riftAct(heroId: String, runId: String, act: RiftAct, key: String = UUID.randomUUID().toString()): RiftReport = WireJson.decodeFromJsonElement(
        RiftReport.serializer(),
        http.request(
            "POST",
            "$TRIALS/rift/act",
            heroQuery(heroId, "runId" to runId),
            WireJson.encodeToJsonElement(RiftAct.serializer(), act),
            authenticated = true,
            headers = mapOf(IDEMPOTENCY_HEADER to key),
        ),
    )

    /** Forges a rush key from five crest fragments (server 1.48.0); the hero's bag comes back with it. */
    suspend fun forgeKey(heroId: String): Map<String, Long> = http.post("$TRIALS/key", heroQuery(heroId))

    /** Enters the tower from its last checkpoint: a tower seal is spent. */
    suspend fun tower(heroId: String): TrialStart = http.post("$TRIALS/tower", heroQuery(heroId))

    /**
     * A batch of the journal of trial [runId], in order; a lost answer is asked for again under the same [key], and the
     * server repeats its stored report. Null - the batch landed, but its report was not kept.
     */
    suspend fun events(heroId: String, runId: String, events: List<TrialEvent>, key: String = UUID.randomUUID().toString()): TrialReport? = http.request(
        "POST",
        "$TRIALS/events",
        heroQuery(heroId, "runId" to runId),
        WireJson.encodeToJsonElement(ListSerializer(TrialEvent.serializer()), events),
        authenticated = true,
        headers = mapOf(IDEMPOTENCY_HEADER to key),
    )
        .takeIf { it !is JsonNull }?.let { WireJson.decodeFromJsonElement(TrialReport.serializer(), it) }
}
