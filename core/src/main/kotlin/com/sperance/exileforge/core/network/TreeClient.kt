package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.tree.TreeState
import com.sperance.exileforge.rules.content.TakenNode

private const val TREE = "api/v1/hero/skilltree"

/** One hero's passive tree: its state and the commands that change it; every rule is the server's. */
class TreeClient internal constructor(private val http: Transport) {
    suspend fun state(heroId: String): TreeState = http.get("$TREE/state", heroQuery(heroId))

    /** [choice] is the picked option of a MASTERY or ATTRIBUTE node; every other node takes none. */
    suspend fun allocate(heroId: String, nodeCode: String, choice: Int? = null): TreeState = node("allocate", heroId, nodeCode, choice)

    /** The shortest path to [nodeCode] at once (server 1.37.0): the rules' path, its summed cost; [choice] is the target's own option. */
    suspend fun path(heroId: String, nodeCode: String, choice: Int? = null): TreeState = node("path", heroId, nodeCode, choice)
    suspend fun refund(heroId: String, nodeCode: String): TreeState = node("refund", heroId, nodeCode)

    /** The node and everything that hangs on it (server 1.52.0), an Orb of Regret each. */
    suspend fun refundBranch(heroId: String, nodeCode: String): TreeState = node("refundBranch", heroId, nodeCode)

    /** Another option of a taken ATTRIBUTE node, paid with one Chaos Orb; the node stays taken. */
    suspend fun rechoose(heroId: String, nodeCode: String, choice: Int): TreeState = node("rechoose", heroId, nodeCode, choice)
    suspend fun reset(heroId: String): TreeState = http.post("$TREE/reset", heroQuery(heroId))

    private suspend fun node(operation: String, heroId: String, nodeCode: String, choice: Int? = null): TreeState {
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        return http.post("$TREE/$operation", heroQuery(heroId, "nodeCode" to nodeCode, "choice" to choice?.toString()))
    }
}
