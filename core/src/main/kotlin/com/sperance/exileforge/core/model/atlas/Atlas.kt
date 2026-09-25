package com.sperance.exileforge.core.model.atlas

import kotlinx.serialization.Serializable

/** One bonus of an atlas node (server 0.60.0): a stat and how much of it. */
@Serializable data class AtlasEffect(val stat: String = "", val value: Double = 0.0)

/**
 * A node of the atlas tree as the server seeds it: where it stands — [y] grows up from the start at
 * the bottom — what it is, whose child it is and what it gives. The graph is the server's; the
 * client only draws it and names a node per command.
 */
@Serializable data class AtlasNode(
    val code: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val kind: String = SMALL,
    val parents: List<String> = emptyList(),
    val effects: List<AtlasEffect> = emptyList(),
) {
    /** Which of the four trunks it belongs to, read off its code: `ATLAS_<BRANCH>_…`. */
    val branch: AtlasBranch get() = AtlasBranch.entries.firstOrNull { code.startsWith("ATLAS_${it.prefix}") } ?: AtlasBranch.ROOT

    companion object {
        const val START = "START"
        const val SMALL = "SMALL"
        const val NOTABLE = "NOTABLE"
        const val KEYSTONE = "KEYSTONE"
    }
}

/** The trunks of the tree (the owner's four directions); [ROOT] is the start alone. */
enum class AtlasBranch(val prefix: String) { LOOT("LOOT"), VAAL("VAAL"), CONTENT("CONT"), BOSS("BOSS"), ROOT("START") }

/** What a respec costs: per node, plus per character level for each node. */
@Serializable data class AtlasRespec(val perNode: Long = 0, val perLevel: Long = 0) {
    fun perNode(level: Int): Long = perNode + perLevel * level
}

/** The whole tree, once a session: its nodes, what each kind of achievement pays and the respec price. */
@Serializable data class AtlasTree(
    val points: Map<String, Int> = emptyMap(),
    val respec: AtlasRespec = AtlasRespec(),
    val nodes: List<AtlasNode> = emptyList(),
)

/** One character's atlas: nodes taken (the start among them), achievements earned and the point balance. */
@Serializable data class AtlasState(
    val allocated: List<String> = emptyList(),
    val earned: List<String> = emptyList(),
    val points: Int = 0,
    val available: Int = 0,
)
