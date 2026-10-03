package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.atlas.AtlasState

private const val ATLAS = "api/v1/hero/atlas"

/** The atlas: a hero's state and the commands that change it; the graph itself is content. */
class AtlasClient internal constructor(private val http: Transport) {
    suspend fun state(heroId: String): AtlasState = http.get("$ATLAS/state", heroQuery(heroId))

    /** Never retried: a repeat would spend a second point. */
    suspend fun allocate(heroId: String, nodeCode: String): AtlasState = node("allocate", heroId, nodeCode)

    /** Costs gold (the respec price) or, with [regret] (server 1.65.0), an Orb of Regret per node. Never retried. */
    suspend fun refund(heroId: String, nodeCode: String, regret: Boolean = false): AtlasState = node("refund", heroId, nodeCode, payment(regret))
    suspend fun reset(heroId: String, regret: Boolean = false): AtlasState = http.post("$ATLAS/reset", heroQuery(heroId, payment(regret)))

    private suspend fun node(operation: String, heroId: String, nodeCode: String, vararg more: Pair<String, String?>): AtlasState {
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        return http.post("$ATLAS/$operation", heroQuery(heroId, "nodeCode" to nodeCode, *more))
    }

    /** The payment in Orbs of Regret as a query pair; gold, the default, sends nothing. */
    private fun payment(regret: Boolean): Pair<String, String?> = "regret" to "true".takeIf { regret }
}
