package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.tree.TreeState
import com.sperance.exileforge.rules.content.TakenNode

private const val TREE = "api/v1/hero/skilltree"

/** Команды пассивного дерева героя; правила у сервера, ответ - только снимок героя (3.94.1), состояние дерева - его часть. */
class TreeClient internal constructor(private val http: Transport) {
    /** [choice] is the picked option of a MASTERY or ATTRIBUTE node; every other node takes none. */
    suspend fun allocate(heroId: String, nodeCode: String, choice: Int? = null): Unit = node("allocate", heroId, nodeCode, choice)

    suspend fun refund(heroId: String, nodeCode: String): Unit = node("refund", heroId, nodeCode)

    /** The node and everything that hangs on it (server 1.52.0), an Orb of Regret each. */
    suspend fun refundBranch(heroId: String, nodeCode: String): Unit = node("refundBranch", heroId, nodeCode)

    /** Another option of a taken ATTRIBUTE node, paid with one Chaos Orb; the node stays taken. */
    suspend fun rechoose(heroId: String, nodeCode: String, choice: Int): Unit = node("rechoose", heroId, nodeCode, choice)
    suspend fun reset(heroId: String) {
        http.request("POST", "$TREE/reset", heroQuery(heroId), authenticated = true)
    }

    private suspend fun node(operation: String, heroId: String, nodeCode: String, choice: Int? = null) {
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        http.request("POST", "$TREE/$operation", heroQuery(heroId, "nodeCode" to nodeCode, "choice" to choice?.toString()), authenticated = true)
    }
}
