package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.atlas.AtlasState

private const val ATLAS = "api/v1/hero/atlas"

/** The atlas: a hero's state and the commands that change it; the graph itself is content. */
class AtlasClient internal constructor(private val http: Transport) {
    suspend fun state(heroId: String): AtlasState = http.get("$ATLAS/state", heroQuery(heroId))
    /** Never retried: a repeat would spend a second point. */
    suspend fun allocate(heroId: String, nodeCode: String): AtlasState = node("allocate", heroId, nodeCode)
    /** Costs gold (the respec price). Never retried. */
    suspend fun refund(heroId: String, nodeCode: String): AtlasState = node("refund", heroId, nodeCode)
    suspend fun reset(heroId: String): AtlasState = http.post("$ATLAS/reset", heroQuery(heroId))

    private suspend fun node(operation: String, heroId: String, nodeCode: String): AtlasState {
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        return http.post("$ATLAS/$operation", heroQuery(heroId, "nodeCode" to nodeCode))
    }
}
