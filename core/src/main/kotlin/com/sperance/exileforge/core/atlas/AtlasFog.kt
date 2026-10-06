package com.sperance.exileforge.core.atlas

import com.sperance.exileforge.rules.content.AtlasGraph
import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FountainRule
import com.sperance.exileforge.rules.content.MapStat

/**
 * The atlas as the screen reads it: which nodes a tap could take or give back. The whole sky is shown (3.54.0: no fog);
 * the server decides every command, this only draws the same rule in advance.
 */
object AtlasFog {
    fun canTake(graph: AtlasGraph, code: String, taken: Set<String>): Boolean = code !in taken && code != graph.start && graph.isAdjacentTo(code, taken)

    fun canRefund(graph: AtlasGraph, code: String, taken: Set<String>): Boolean = code in taken && code != graph.start && graph.isConnected(taken - code)

    /** Which of the ten branches a node belongs to, read off its code: `ATLAS_<BRANCH>_…`. */
    fun branch(code: String): AtlasBranch = AtlasBranch.entries.firstOrNull { code.startsWith("ATLAS_${it.prefix}") } ?: AtlasBranch.ROOT

    /**
     * The constellation a node belongs to (3.53.0, server 1.51.0): `ATLAS_<mechanic>_<tier letter><n>` — the mechanic and the tier;
     * a starlane's node (`P<n>`) and the start belong to none.
     */
    fun constellation(code: String): String? {
        val tail = code.substringAfterLast('_')
        if (branch(code) == AtlasBranch.ROOT || tail.isEmpty() || tail[0] == 'P') return null
        return code.substringBeforeLast('_') + "_" + tail[0]
    }
}

/** The branches of the tree (3.43.0, server 1.41.0: ten, each in its own wedge); [ROOT] is the start alone. */
enum class AtlasBranch(val prefix: String) {
    LOOT("LOOT_"),
    MAPS("MAPS_"),
    TIERS("TIERS_"),
    BOSSES("BOSSES_"),
    ABYSS("ABYSS_"),
    VAAL("VAAL_"),
    CRYSTALS("CRYSTALS_"),
    EXPEDITION("EXPEDITION_"),
    CRAFT("CRAFT_"),
    POWER("POWER_"),
    INFLUENCE("INFLUENCE_"),

    /** Ветви объектов карты (3.89.1, сервер 1.81.3): алтари, торговец, ловушки, тайники, узлы ремёсел. */
    ALTAR("ALTAR_"),
    MERCHANT("MERCHANT_"),
    TRAPS("TRAPS_"),
    SECRETS("SECRETS_"),
    GATHER("GATHER_"),
    ROOT("START"),
}

/**
 * The atlas bonuses the client rolls with itself: the pack and the rare monsters as a map item's effects,
 * the Vaal portal's chance and the fountains' count. Loot is rolled by the rules from the run's context.
 */
object AtlasEffects {
    val VAAL_CHANCE = AtlasStat.VAAL_CHANCE.code
    val FOUNTAINS = AtlasStat.FOUNTAINS.code
    val PACK_SIZE = AtlasStat.PACK_SIZE.code
    val RARE_MONSTERS = AtlasStat.RARE_MONSTERS.code
    val MONSTER_MODS = AtlasStat.MONSTER_MODS.code
    val GUARDIAN_POWER = AtlasStat.GUARDIAN_POWER.code
    val ABYSS_POWER = AtlasStat.ABYSS_POWER.code
    val FLASK_RARE = AtlasStat.FLASK_RARE.code

    /** The atlas's gifts to the hero on a map, each to the sheet's stat it adds to. */
    val hero = mapOf(
        AtlasStat.MANA_REGEN.code to CoreStat.MANA_REGEN.code,
        AtlasStat.SKILL_LEVEL.code to CoreStat.SKILL_LEVEL.code,
        AtlasStat.FLASK_CHARGES.code to CoreStat.FLASK_CHARGES_GAINED.code,
        AtlasStat.FLASK_DURATION.code to CoreStat.FLASK_DURATION.code,
        FLASK_RARE to FLASK_RARE,
    )

    /** Stats counted in units rather than percent. */
    val flat = setOf(FOUNTAINS, AtlasStat.CHESTS.code, AtlasStat.VAAL_MIN_MODS.code, MONSTER_MODS, AtlasStat.CRYSTALS.code, AtlasStat.SKILL_LEVEL.code, FLASK_RARE, AtlasStat.ABYSS_DEPTH.code)

    /** [effects] of the entered map with the atlas's pack and rare monsters added, as one more map item's worth. */
    fun map(effects: Map<String, Double>, atlas: Map<String, Double>): Map<String, Double> {
        val extra = (
            mapOf(MapStat.PACK_SIZE.code to (atlas[PACK_SIZE] ?: 0.0), MapStat.RARE_MONSTERS.code to (atlas[RARE_MONSTERS] ?: 0.0)) +
                (hero.keys + GUARDIAN_POWER + ABYSS_POWER + com.sperance.exileforge.rules.content.BrewStat.ALL).associateWith { atlas[it] ?: 0.0 }
            ).filterValues { it != 0.0 }
        return (effects.keys + extra.keys).associateWith { (effects[it] ?: 0.0) + (extra[it] ?: 0.0) }
    }

    fun portalChance(index: ContentIndex, atlas: Map<String, Double>): Double = (index.campaign.corruption.chance * (1 + (atlas[VAAL_CHANCE] ?: 0.0) / 100)).coerceIn(0.0, 1.0)

    fun fountains(rule: FountainRule, atlas: Map<String, Double>): FountainRule {
        val more = (atlas[FOUNTAINS] ?: 0.0).toInt()
        return if (more == 0) rule else rule.copy(count = rule.count.map { (it + more).coerceAtLeast(0) })
    }
}
