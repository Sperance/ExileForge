package com.sperance.exileforge.core.model.skilltree

import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.model.modifier.Modifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One line of what a set of modifiers gives: a characteristic, an operation and a number.
 *
 * The operation is what stops the number from lying. `+60` and `+10%` to the same characteristic
 * are two different statements, and printing them as one would be a third, untrue one.
 */
@Serializable data class StatContribution(
    val stat: String = "",
    val operation: String = "ADD",
    val value: Double = 0.0,
)

/** What a node is worth on the tree, as the server grades them. */
@Serializable enum class SkillNodeType { START, SMALL, NOTABLE, KEYSTONE, JEWEL_SOCKET }

/**
 * One node of the shared skill tree (collection `SkillTreeNode`).
 *
 * The tree is a graph: a node names its neighbours by code, and a node can only be taken next to
 * one already taken. Its bonuses are fixed rather than rolled, so they arrive as [Modifier] with
 * no tier. [positionX] and [positionY] exist for drawing and carry no rule.
 */
@Serializable data class SkillTreeNode(
    @SerialName("_id") val id: String = "",
    val code: String = "",
    val type: SkillNodeType = SkillNodeType.SMALL,
    val params: List<Modifier> = emptyList(),
    val connections: List<String> = emptyList(),
    val cost: Int = 1,
    val positionX: Int = 0,
    val positionY: Int = 0,
) {
    val title: String get() = locOr(LocaleKey.skillNodeName(code), code)
    val details: String get() = locOr(LocaleKey.skillNodeDescription(code), "")
}

/**
 * A node one character has taken, embedded in their `Character.skillNodes`.
 *
 * It is a snapshot rather than a reference, so one hero's copy of a node can be tuned without
 * touching the tree or anyone else. The tree's *shape* is deliberately not copied: adjacency and
 * coordinates are how the world is built, and the server checks them against the live tree.
 *
 * The class's start node sits here from the moment the character is created.
 */
@Serializable data class CharacterSkillNode(
    val code: String = "",
    val params: List<Modifier> = emptyList(),
    val type: SkillNodeType = SkillNodeType.SMALL,
    val cost: Int = 1,
) {
    val title: String get() = locOr(LocaleKey.skillNodeName(code), code)
}

/**
 * The state of one character's tree, as `GET /api/v1/character/skilltree/state` reports it.
 *
 * The point balance is the server's: it comes from the progression table and the costs actually
 * paid, and the client prints it rather than adding the costs up itself.
 */
@Serializable data class SkillTreeState(
    val characterId: String = "",
    val total: Int = 0,
    val spent: Int = 0,
    val available: Int = 0,
    val nodes: List<CharacterSkillNode> = emptyList(),

    /**
     * What the taken tree gives, one line per characteristic and kind of operation.
     *
     * This is the tree's *contribution*, not the character's total: there is no base under it, so
     * "+40% physical damage" stays a percentage. It used to be a total over an empty base, where
     * every percentage collapsed — nothing times 1.4 is nothing — and vanished from the answer.
     *
     * Counted by the server, not here. Modifiers carry operations and they do not add alike —
     * two INCREASED add together, two MORE multiply — so summing the taken snapshots on the
     * client would be wrong exactly where a player is deciding whether a node is worth a point.
     */
    val totals: List<StatContribution> = emptyList(),
) {
    val takenCodes: Set<String> get() = nodes.mapTo(mutableSetOf()) { it.code }
}

/**
 * Nodes one step away from what a character has already taken.
 *
 * With nothing taken the only way in is a START node, which is how the server's own rule reads.
 * This is a reading of the `connections` the server seeded, not a second copy of the rule: the
 * allocate call still decides, and a node highlighted here can still be refused — for the cost,
 * for the class it belongs to, or for anything else the server weighs.
 */
fun reachableFrom(nodes: List<SkillTreeNode>, taken: Set<String>): Set<String> {
    if (taken.isEmpty()) return nodes.filter { it.type == SkillNodeType.START }.mapTo(mutableSetOf()) { it.code }
    val byCode = nodes.associateBy { it.code }
    val adjacent = mutableSetOf<String>()
    taken.forEach { code -> byCode[code]?.connections?.let(adjacent::addAll) }
    // An edge is declared on both ends, but a one-sided one would otherwise stay invisible.
    nodes.forEach { node -> if (node.connections.any { it in taken }) adjacent.add(node.code) }
    return adjacent - taken
}
