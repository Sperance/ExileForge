package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.atlas.AtlasState
import com.sperance.exileforge.core.model.atlas.AtlasTree

/** Route root of the atlas (server 0.60.0). */
private const val ATLAS = "api/v1/character/atlas"

/**
 * The atlas tree: the graph once, a character's state, and the three commands that change it. Which
 * node may be taken or given back and what a respec costs are the server's; the client names a node.
 */
class AtlasClient internal constructor(private val http: Transport) {
    suspend fun tree(): AtlasTree = http.get("$ATLAS/tree")
    suspend fun state(characterId: String): AtlasState = http.get("$ATLAS/state", heroQuery(characterId))
    /** Never retried: a repeat would spend a second point. */
    suspend fun allocate(characterId: String, nodeCode: String): AtlasState = node("allocate", characterId, nodeCode)
    /** Costs gold (the respec price). Never retried. */
    suspend fun refund(characterId: String, nodeCode: String): AtlasState = node("refund", characterId, nodeCode)
    suspend fun reset(characterId: String): AtlasState = http.post("$ATLAS/reset", heroQuery(characterId))

    private suspend fun node(operation: String, characterId: String, nodeCode: String): AtlasState {
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        return http.post("$ATLAS/$operation", heroQuery(characterId, "nodeCode" to nodeCode))
    }
}
