package com.sperance.exileforge.core.atlas

import com.sperance.exileforge.rules.content.AtlasGraph
import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FountainRule
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.roll.LootRoller

/**
 * The atlas as the screen reads it: which nodes a tap could take or give back. The whole sky is shown (no fog);
 * the server decides every command, this only draws the same rule in advance.
 */
object AtlasFog {
    /** Взять можно соседний свободный узел, если его не исключает уже взятый («только Зодчий» / «только Пустота»). */
    fun canTake(graph: AtlasGraph, code: String, taken: Set<String>): Boolean = code !in taken && code != graph.start && graph.isAdjacentTo(code, taken) && !graph.excluded(code, taken)

    /** Взятый узел, что не даёт взять [code], - его имя называет причина. */
    fun excludedBy(graph: AtlasGraph, code: String, taken: Set<String>): String? = graph.node(code)?.excludes?.firstOrNull { it in taken }

    fun canRefund(graph: AtlasGraph, code: String, taken: Set<String>): Boolean = code in taken && code != graph.start && graph.isConnected(taken - code)

    /** Which of the ten branches a node belongs to, read off its code: `ATLAS_<BRANCH>_…`. */
    fun branch(code: String): AtlasBranch = AtlasBranch.entries.firstOrNull { code.startsWith("ATLAS_${it.prefix}") } ?: AtlasBranch.ROOT
}

/** The branches of the tree (ten, each in its own wedge); [ROOT] is the start alone. */
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

    /** Ветви объектов карты: алтари, ловушки, тайники, узлы ремёсел и Скверна (на месте торговца). */
    ALTAR("ALTAR_"),
    BLIGHT("BLIGHT_"),
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
        AtlasStat.FLASK_CHARGES.code to CoreStat.FLASK_CHARGES_GAINED.code,
        AtlasStat.FLASK_DURATION.code to CoreStat.FLASK_DURATION.code,
        AtlasStat.SUPPRESSION.code to CoreStat.RECOVERY_SUPPRESSION.code,
        FLASK_RARE to FLASK_RARE,
    )

    /** [effects] of the entered map with the atlas's pack and rare monsters added, as one more map item's worth. */
    fun map(effects: Map<String, Double>, atlas: Map<String, Double>): Map<String, Double> {
        val extra = extras(atlas)
        return (effects.keys + extra.keys).associateWith { (effects[it] ?: 0.0) + (extra[it] ?: 0.0) }
    }

    /**
     * Строки атласа [atlas], которые правила кладут строками карты-предмета источника атласа ([LootRoller.atlasPowers]): окно карты
     * показывает их строками карты, а не строками атласа.
     */
    fun laid(index: ContentIndex, atlas: Map<String, Double>): Set<String> = LootRoller(index).let { loot -> atlas.keys.filterTo(HashSet()) { loot.atlasPowers(mapOf(it to 1.0)).isNotEmpty() } }

    /** Что атлас захода [atlas] (с долями гильдии и зелья) кладёт в строки карты ([map]); нулевые не входят. */
    private fun extras(atlas: Map<String, Double>): Map<String, Double> = (
        mapOf(MapStat.PACK_SIZE.code to (atlas[PACK_SIZE] ?: 0.0), MapStat.RARE_MONSTERS.code to (atlas[RARE_MONSTERS] ?: 0.0)) +
            (hero.keys + GUARDIAN_POWER + ABYSS_POWER + com.sperance.exileforge.rules.content.BrewStat.ALL).associateWith { atlas[it] ?: 0.0 }
        ).filterValues { it != 0.0 }

    /**
     * Основа строки Атласа в правилах: узлы прибавляют к ней свои числа, итог - основа плюс сумма узлов.
     * `null` - у строки основы нет (итог - сама сумма узлов).
     */
    fun base(index: ContentIndex, stat: String): Double? = index.campaign.maps.influence.let { rule ->
        when (stat) {
            AtlasStat.MAP_INFLUENCE.code -> rule.chance
            AtlasStat.INFLUENCE_ITEMS.code -> rule.items
            AtlasStat.INFLUENCE_POWER.code -> rule.power
            else -> null
        }
    }

    fun portalChance(index: ContentIndex, atlas: Map<String, Double>): Double = (index.campaign.corruption.chance * (1 + (atlas[VAAL_CHANCE] ?: 0.0) / 100)).coerceIn(0.0, 1.0)

    fun fountains(rule: FountainRule, atlas: Map<String, Double>): FountainRule {
        val more = (atlas[FOUNTAINS] ?: 0.0).toInt()
        return if (more == 0) rule else rule.copy(count = rule.count.map { (it + more).coerceAtLeast(0) })
    }
}
