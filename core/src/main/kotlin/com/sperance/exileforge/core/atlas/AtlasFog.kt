package com.sperance.exileforge.core.atlas

import com.sperance.exileforge.rules.content.AtlasGraph
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.FountainRule

/**
 * The atlas as the screen reads it: which nodes a tap could take or give back, and what the fog leaves
 * in sight. The server decides every command; this only draws the same rule in advance.
 */
object AtlasFog {
    /** How many links ahead of the taken nodes the atlas shows. */
    const val SIGHT = 3

    fun canTake(graph: AtlasGraph, code: String, taken: Set<String>): Boolean = code !in taken && code != graph.start && graph.isAdjacentTo(code, taken)

    fun canRefund(graph: AtlasGraph, code: String, taken: Set<String>): Boolean = code in taken && code != graph.start && graph.isConnected(taken - code)

    /** Every node no more than [depth] links from one already taken, or from the start; the rest of the tree is not drawn. */
    fun visible(graph: AtlasGraph, taken: Set<String>, depth: Int = SIGHT): Set<String> {
        val seen = (taken + graph.start).filterTo(LinkedHashSet()) { it in graph.byCode }
        var frontier = seen.toList()
        repeat(depth) { frontier = frontier.flatMap(graph::neighbours).filter(seen::add) }
        return seen
    }

    /** Which of the four trunks a node belongs to, read off its code: `ATLAS_<BRANCH>_…`. */
    fun branch(code: String): AtlasBranch = AtlasBranch.entries.firstOrNull { code.startsWith("ATLAS_${it.prefix}") } ?: AtlasBranch.ROOT
}

/** The trunks of the tree (the owner's four directions); [ROOT] is the start alone. */
enum class AtlasBranch(val prefix: String) { LOOT("LOOT"), VAAL("VAAL"), CONTENT("CONT"), BOSS("BOSS"), ROOT("START") }

/**
 * The atlas bonuses the client rolls with itself: the pack and the rare monsters as a map item's effects,
 * the Vaal portal's chance and the fountains' count. Loot is rolled by the rules from the run's context.
 */
object AtlasEffects {
    const val VAAL_CHANCE = "ATLAS_VAAL_CHANCE"
    const val FOUNTAINS = "ATLAS_FOUNTAINS"
    const val PACK_SIZE = "ATLAS_PACK_SIZE"
    const val RARE_MONSTERS = "ATLAS_RARE_MONSTERS"
    const val MONSTER_MODS = "ATLAS_MONSTER_MODS"
    const val GUARDIAN_POWER = "ATLAS_GUARDIAN_POWER"
    const val ABYSS_POWER = "ATLAS_ABYSS_POWER"
    const val FLASK_RARE = "ATLAS_FLASK_RARE"
    const val ABYSS_KEEP = "ATLAS_ABYSS_KEEP"

    /** The atlas's gifts to the hero on a map, each to the sheet's stat it adds to. */
    val hero = mapOf("ATLAS_MANA_REGEN" to "STOCK_MANA_REGEN", "ATLAS_SKILL_LEVEL" to "STOCK_SKILL_LEVEL",
        "ATLAS_FLASK_CHARGES" to "STOCK_FLASK_CHARGES_GAINED", "ATLAS_FLASK_DURATION" to "STOCK_FLASK_DURATION", FLASK_RARE to FLASK_RARE)

    /** Stats counted in units rather than percent. */
    val flat = setOf(FOUNTAINS, "ATLAS_CHESTS", "ATLAS_VAAL_MIN_MODS", MONSTER_MODS, "ATLAS_CRYSTALS", "ATLAS_SKILL_LEVEL", FLASK_RARE, "ATLAS_ABYSS_DEPTH")

    /** [effects] of the entered map with the atlas's pack and rare monsters added, as one more map item's worth. */
    fun map(effects: Map<String, Double>, atlas: Map<String, Double>): Map<String, Double> {
        val extra = (mapOf("MAP_PACK_SIZE" to (atlas[PACK_SIZE] ?: 0.0), "MAP_RARE_MONSTERS" to (atlas[RARE_MONSTERS] ?: 0.0)) +
            (hero.keys + GUARDIAN_POWER + ABYSS_POWER).associateWith { atlas[it] ?: 0.0 }).filterValues { it != 0.0 }
        return (effects.keys + extra.keys).associateWith { (effects[it] ?: 0.0) + (extra[it] ?: 0.0) }
    }

    fun portalChance(index: ContentIndex, atlas: Map<String, Double>): Double =
        (index.campaign.corruption.chance * (1 + (atlas[VAAL_CHANCE] ?: 0.0) / 100)).coerceIn(0.0, 1.0)

    fun fountains(rule: FountainRule, atlas: Map<String, Double>): FountainRule {
        val more = (atlas[FOUNTAINS] ?: 0.0).toInt()
        return if (more == 0) rule else rule.copy(count = rule.count.map { (it + more).coerceAtLeast(0) })
    }

    /** The share of the Abyss's hoard a fall keeps, 0..1. */
    fun abyssKeep(atlas: Map<String, Double>): Double = (atlas[ABYSS_KEEP] ?: 0.0).coerceIn(0.0, 100.0) / 100
}
