package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import kotlinx.serialization.json.*

/** Route root of every tree command, kept in one place so a move is one edit. */
private const val TREE = "api/v1/character/skilltree"

/** One character's passive tree: its state and the three commands that change it. */
class TreeClient internal constructor(private val http: Transport) {
    /**
     * The character's own tree.
     *
     * Since 0.12.0 the taken nodes live inside the character document rather than a collection of
     * their own, so the whole tree travels under `character/skilltree` with the character.
     */
    suspend fun state(characterId: String): SkillTreeState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", "$TREE/state",
            mapOf("characterId" to characterId), authenticated = true))
    }

    /**
     * Takes, gives back or drops tree nodes; every one of them answers with the whole tree state.
     *
     * Which node may be taken, whether a refund would leave the rest hanging and what a node costs
     * are the server's rules: the client names a node and reports the refusal it gets.
     */
    /** [choice] is the picked option of a MASTERY or ATTRIBUTE node (server 0.52.0); every other node takes none. */
    suspend fun allocate(characterId: String, nodeCode: String, choice: Int? = null): SkillTreeState = node("allocate", characterId, nodeCode, choice)
    suspend fun refund(characterId: String, nodeCode: String): SkillTreeState = node("refund", characterId, nodeCode)
    suspend fun reset(characterId: String): SkillTreeState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$TREE/reset",
            mapOf("characterId" to characterId), authenticated = true))
    }
    private suspend fun node(operation: String, characterId: String, nodeCode: String, choice: Int? = null): SkillTreeState {
        requireId(characterId)
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        val query = mapOf("characterId" to characterId, "nodeCode" to nodeCode) + listOfNotNull(choice?.let { "choice" to it.toString() })
        return WireJson.decodeFromJsonElement(http.request("POST", "$TREE/$operation", query, authenticated = true))
    }
}
