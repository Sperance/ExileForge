package com.sperance.exileforge.core.model.skilltree

import com.sperance.exileforge.core.model.modifier.Modifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What a node is worth on the tree, as the server grades them. */
@Serializable enum class SkillNodeType { START, SMALL, NOTABLE, KEYSTONE }

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
    val name: String = "",
    val type: SkillNodeType = SkillNodeType.SMALL,
    val params: List<Modifier> = emptyList(),
    val connections: List<String> = emptyList(),
    val cost: Int = 1,
    val positionX: Int = 0,
    val positionY: Int = 0,
    val description: String? = null,
)

/**
 * A node one character has taken (collection `CharacterSkillNode`).
 *
 * The bonuses are a snapshot taken when the node was allocated, not a reference to the tree: a
 * later rebalance leaves an already-levelled character with what they actually took.
 */
@Serializable data class CharacterSkillNode(
    @SerialName("_id") val id: String = "",
    val characterId: String = "",
    val nodeCode: String = "",
    val params: List<Modifier> = emptyList(),
    val name: String = "",
    val type: SkillNodeType = SkillNodeType.SMALL,
    val cost: Int = 1,
    val version: Long = 0,
)

/**
 * The state of one character's tree, as `GET /api/v1/characterskillnode/byCharacter` reports it.
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
) {
    val takenCodes: Set<String> get() = nodes.mapTo(mutableSetOf()) { it.nodeCode }
}
