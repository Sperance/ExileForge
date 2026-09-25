package com.sperance.exileforge.core.atlas

import com.sperance.exileforge.core.model.atlas.AtlasNode
import com.sperance.exileforge.core.model.atlas.AtlasTree
import com.sperance.exileforge.core.model.campaign.CampaignView
import com.sperance.exileforge.core.model.campaign.MapRule
import com.sperance.exileforge.core.model.campaign.FountainRule

/**
 * The atlas as the screen reads it (2.68.0, server 0.60.0): which nodes a tap could take or give
 * back. The server decides every command; this only draws the same rule in advance so a node that
 * would be refused never shows its button — adjacency both ways, and a refund that leaves every
 * other taken node still joined to the start.
 */
class AtlasGraph(tree: AtlasTree) {
    val nodes: List<AtlasNode> = tree.nodes
    val byCode: Map<String, AtlasNode> = nodes.associateBy { it.code }
    private val neighbours: Map<String, Set<String>> = buildMap<String, MutableSet<String>> {
        nodes.forEach { node -> node.parents.forEach { parent ->
            getOrPut(node.code) { mutableSetOf() }.add(parent)
            getOrPut(parent) { mutableSetOf() }.add(node.code)
        } }
    }
    val start: String? = nodes.firstOrNull { it.kind == AtlasNode.START }?.code

    fun neighbours(code: String): Set<String> = neighbours[code].orEmpty()

    fun canTake(code: String, taken: Set<String>): Boolean = code !in taken && neighbours(code).any { it in taken }

    fun canRefund(code: String, taken: Set<String>): Boolean {
        if (code !in taken || code == start) return false
        val rest = taken - code
        val seen = mutableSetOf(start ?: return false)
        val queue = ArrayDeque(seen)
        while (queue.isNotEmpty()) neighbours(queue.removeFirst()).forEach { if (it in rest && seen.add(it)) queue.add(it) }
        return seen.size == rest.size
    }
}

/**
 * The atlas bonuses the client rolls itself (server 0.60.0 sends them with the entry): the pack and
 * the rare monsters as a map item's effects, the Vaal portal's chance and the fountains' count.
 * Loot, bosses and the Vaal zone's reward are the server's and never pass through here.
 */
object AtlasEffects {
    const val VAAL_CHANCE = "ATLAS_VAAL_CHANCE"
    const val FOUNTAINS = "ATLAS_FOUNTAINS"
    const val PACK_SIZE = "ATLAS_PACK_SIZE"
    const val RARE_MONSTERS = "ATLAS_RARE_MONSTERS"

    /** Stats counted in units rather than percent. */
    val flat = setOf(FOUNTAINS, "ATLAS_CHESTS", "ATLAS_VAAL_MIN_MODS")

    /** [effects] of the entered map with the atlas's pack and rare monsters added, as one more map item's worth. */
    fun map(effects: Map<String, Double>, atlas: Map<String, Double>): Map<String, Double> {
        val extra = mapOf(MapRule.PACK_SIZE to (atlas[PACK_SIZE] ?: 0.0), MapRule.RARE_MONSTERS to (atlas[RARE_MONSTERS] ?: 0.0)).filterValues { it != 0.0 }
        return (effects.keys + extra.keys).associateWith { (effects[it] ?: 0.0) + (extra[it] ?: 0.0) }
    }

    fun portalChance(view: CampaignView, atlas: Map<String, Double>): Double =
        (view.corruption.chance * (1 + (atlas[VAAL_CHANCE] ?: 0.0) / 100)).coerceIn(0.0, 1.0)

    fun fountains(rule: FountainRule, atlas: Map<String, Double>): FountainRule {
        val more = (atlas[FOUNTAINS] ?: 0.0).toInt()
        return if (more == 0) rule else rule.copy(count = rule.count.map { (it + more).coerceAtLeast(0) })
    }
}
