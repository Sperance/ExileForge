package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.atlas.AtlasState

private const val ATLAS = "api/v1/hero/atlas"

/** Команды атласа героя; граф - контент, ответ - только снимок героя (3.94.1). */
class AtlasClient internal constructor(private val http: Transport) {
    /** Never retried: a repeat would spend a second point. */
    suspend fun allocate(heroId: String, nodeCode: String): Unit = node("allocate", heroId, nodeCode)

    /** Costs gold (the respec price) or, with [regret] (server 1.65.0), an Orb of Regret per node. Never retried. */
    suspend fun refund(heroId: String, nodeCode: String, regret: Boolean = false): Unit = node("refund", heroId, nodeCode, payment(regret))
    suspend fun reset(heroId: String, regret: Boolean = false) {
        http.request("POST", "$ATLAS/reset", heroQuery(heroId, payment(regret)), authenticated = true)
    }

    private suspend fun node(operation: String, heroId: String, nodeCode: String, vararg more: Pair<String, String?>) {
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        http.request("POST", "$ATLAS/$operation", heroQuery(heroId, "nodeCode" to nodeCode, *more), authenticated = true)
    }

    /** The payment in Orbs of Regret as a query pair; gold, the default, sends nothing. */
    private fun payment(regret: Boolean): Pair<String, String?> = "regret" to "true".takeIf { regret }
}
