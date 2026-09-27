package com.sperance.exileforge.core.model.tree

import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.sheet.StatContribution
import kotlinx.serialization.Serializable

/** The state of one hero's tree, as the server reports it: the point balance and what the taken nodes give, by stat and operation. */
@Serializable data class TreeState(
    val total: Int = 0,
    val spent: Int = 0,
    val available: Int = 0,
    val nodes: List<TakenNode> = emptyList(),
    val totals: List<StatContribution> = emptyList(),
) {
    val takenCodes: Set<String> get() = nodes.mapTo(HashSet()) { it.code }
}
