package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.MonsterEffect
import kotlin.math.max
import kotlin.math.roundToInt

/** The stats of a map item, by name, as the client reads them; the registry's MAP group holds them all. */
object MapStats {
    const val QUANTITY = "MAP_QUANTITY"
    const val RARITY = "MAP_RARITY"
    const val EXPERIENCE = "MAP_EXPERIENCE"
    const val PACK_SIZE = "MAP_PACK_SIZE"
    const val MONSTER_RARITY = "MAP_MONSTER_RARITY"
    const val MAGIC_MONSTERS = "MAP_MAGIC_MONSTERS"
    const val RARE_MONSTERS = "MAP_RARE_MONSTERS"
    const val MONSTER_LIFE = "MAP_MONSTER_LIFE"
    const val MONSTER_DAMAGE = "MAP_MONSTER_DAMAGE"
    const val MONSTER_SPEED = "MAP_MONSTER_SPEED"
    const val MONSTER_RESIST = "MAP_MONSTER_RESIST"
    const val HERO_LIGHT = "MAP_HERO_LIGHT"
    const val HERO_RESIST = "MAP_HERO_RESIST"
    const val HERO_REGEN = "MAP_HERO_REGEN"
    const val HERO_SLOW = "MAP_HERO_SLOW"
    const val HERO_DAMAGE_TAKEN = "MAP_HERO_DAMAGE_TAKEN"
    const val HERO_RECOVERY = "MAP_HERO_RECOVERY"
    const val HERO_MAX_RESIST = "MAP_HERO_MAX_RESIST"
    const val HERO_DEFENCES = "MAP_HERO_DEFENCES"
    const val HERO_BLOCK = "MAP_HERO_BLOCK"
    const val HERO_CRIT = "MAP_HERO_CRIT"
    const val MONSTER_PENETRATION = "MAP_MONSTER_PENETRATION"
    const val MONSTER_REFLECT = "MAP_MONSTER_REFLECT"
    const val MONSTER_CRITICAL = "MAP_MONSTER_CRITICAL"
    const val MONSTER_AILMENTS = "MAP_MONSTER_AILMENTS"
    const val MONSTER_ARMOUR = "MAP_MONSTER_ARMOUR"
    const val MONSTER_LEECH = "MAP_MONSTER_LEECH"
    const val MONSTER_STUN = "MAP_MONSTER_STUN"
    const val MONSTER_MAGIC_MIN = "MAP_MONSTER_MAGIC_MIN"
    const val HERO_HASTE = "MAP_HERO_HASTE"
    const val HERO_ATTACK_SPEED = "MAP_HERO_ATTACK_SPEED"
    const val HERO_LIFE = "MAP_HERO_LIFE"
    const val HERO_LEECH = "MAP_HERO_LEECH"
    const val GOLD = "MAP_GOLD"
    const val FOUNTAINS = "MAP_FOUNTAINS"
    const val CHESTS = "MAP_CHESTS"
    const val BOSS_POWER = "MAP_BOSS_POWER"
    const val FLASK_CHARGES = "MAP_FLASK_CHARGES"
    const val HERO_MANA_REGEN = "MAP_HERO_MANA_REGEN"
    const val MONSTER_CAST = "MAP_MONSTER_CAST"
    const val SKILL_COST = "MAP_SKILL_COST"
    const val CRYSTALS = "MAP_CRYSTALS"
    const val BOOKS = "MAP_BOOKS"
    const val ABYSS_CRACKS = "MAP_ABYSS_CRACKS"
    const val ABYSS_DEPTH = "MAP_ABYSS_DEPTH"
    const val ABYSS_HOARD = "MAP_ABYSS_HOARD"
    const val ABYSS_UNIQUE = "MAP_ABYSS_UNIQUE"
    const val ABYSS_ORBS = "MAP_ABYSS_ORBS"
    const val ABYSS_RARE = "MAP_ABYSS_RARE"
    const val ABYSS_LIFE = "MAP_ABYSS_LIFE"
    const val ABYSS_DAMAGE = "MAP_ABYSS_DAMAGE"
    const val ABYSS_SWARM = "MAP_ABYSS_SWARM"
    const val ABYSS_LEADER = "MAP_ABYSS_LEADER"

    /** The lines that give without asking. */
    val rewards: Set<String> = setOf(QUANTITY, RARITY, EXPERIENCE, GOLD, CHESTS, FOUNTAINS, HERO_HASTE, HERO_ATTACK_SPEED, HERO_LIFE, HERO_LEECH,
        CRYSTALS, BOOKS, ABYSS_CRACKS, ABYSS_DEPTH, ABYSS_HOARD, ABYSS_UNIQUE, ABYSS_ORBS, ABYSS_RARE)

    /** What a map's stat does, for its line's mark: a harm pays by the rules' risk, a reward pays itself, the rest is content. */
    fun kindOf(index: ContentIndex, stat: String): MapLineKind = when {
        stat in rewards -> MapLineKind.REWARD
        stat in index.campaign.maps.risk -> MapLineKind.HARM
        else -> MapLineKind.CONTENT
    }

    /** How many percent one rolled value of [stat] pays, by the rules' weight; zero for what is not a risk. */
    fun riskOf(index: ContentIndex, stat: String, value: Double): Double = value * (index.campaign.maps.risk[stat] ?: 0.0)

    /** The template a zone's map is: `MAP_<zone code>`. */
    fun templateCode(mapCode: String) = "MAP_$mapCode"
}

/** What a line of a map is, as the launch window marks it. */
enum class MapLineKind { HARM, CONTENT, REWARD }

/**
 * What a map does to a run. The fight is the client's, so the map's summed effects — the run context's
 * `active.effects` — are applied here: monster buffs fold by the modifier formula over the rolled stats, the
 * hero's debuffs take their share of the sheet. What the map pays is rolled by the rules from the same context.
 */
object MapEffects {
    private val damage = DamageType.entries.map { it.attack }
    private val resists = DamageType.entries.mapNotNull { it.resist }

    /** What a map adds to every monster on it, as effects folded by the same formula as a modifier. */
    fun buffs(effects: Map<String, Double>): List<MonsterEffect> = buildList {
        effects[MapStats.MONSTER_LIFE]?.let { add(MonsterEffect("STOCK_HEALTH", Op.INCREASED, it)) }
        effects[MapStats.MONSTER_DAMAGE]?.let { v -> damage.forEach { add(MonsterEffect(it, Op.INCREASED, v)) } }
        effects[MapStats.MONSTER_SPEED]?.let { v -> listOf("STOCK_ATTACK_SPEED", "STOCK_CAST_SPEED").forEach { add(MonsterEffect(it, Op.INCREASED, v)) } }
        effects[MapStats.MONSTER_RESIST]?.let { v -> resists.forEach { add(MonsterEffect(it, Op.ADD, v)) } }
        effects[MapStats.MONSTER_PENETRATION]?.let { add(MonsterEffect("STOCK_PENETRATE_ELEMENTAL", Op.ADD, it)) }
        effects[MapStats.MONSTER_REFLECT]?.let { add(MonsterEffect("STOCK_REFLECT", Op.ADD, it)) }
        effects[MapStats.MONSTER_CRITICAL]?.let { add(MonsterEffect("STOCK_CRITICAL_CHANCE", Op.ADD, it)) }
        effects[MapStats.MONSTER_AILMENTS]?.let { v -> Ailment.entries.filter { it != Ailment.CHILLED }.forEach { add(MonsterEffect("STOCK_${it.word}_CHANCE", Op.ADD, v)) } }
        effects[MapStats.MONSTER_ARMOUR]?.let { v -> listOf("STOCK_ARMOR", "STOCK_EVASION").forEach { add(MonsterEffect(it, Op.INCREASED, v)) } }
        effects[MapStats.MONSTER_LEECH]?.let { add(MonsterEffect("STOCK_LEECH_ALL", Op.ADD, it)) }
        effects[MapStats.MONSTER_STUN]?.let { add(MonsterEffect("STOCK_AVOID_STUN", Op.ADD, it)) }
        effects[MapStats.MONSTER_CAST]?.let { add(MonsterEffect("STOCK_COOLDOWN_RECOVERY", Op.ADD, it)) }
    }

    /** A crystal's guardian beyond its rarity: so many percent more life and damage when a Vaal orb made it [stronger], and by the atlas's power of guardians. */
    fun guardianBuffs(stronger: Boolean, strongerBy: Double, effects: Map<String, Double>): List<MonsterEffect> {
        val power = (if (stronger) strongerBy else 0.0) + (effects[AtlasEffects.GUARDIAN_POWER] ?: 0.0)
        return if (power <= 0) emptyList() else (listOf("STOCK_HEALTH") + damage).map { MonsterEffect(it, Op.MORE, power) }
    }

    /** What a map does to its boss alone: so many percent more life and damage. */
    fun bossBuffs(effects: Map<String, Double>): List<MonsterEffect> =
        effects[MapStats.BOSS_POWER]?.takeIf { it > 0 }?.let { v -> (listOf("STOCK_HEALTH") + damage).map { MonsterEffect(it, Op.MORE, v) } }.orEmpty()

    /** Fountains a map adds beyond the rule's. */
    fun fountains(effects: Map<String, Double>): Int = (effects[MapStats.FOUNTAINS] ?: 0.0).roundToInt().coerceAtLeast(0)

    fun hero(stats: Map<String, Double>, effects: Map<String, Double>): Map<String, Double> {
        if (effects.isEmpty()) return stats
        val sheet = stats.toMutableMap()
        effects[MapStats.HERO_LIGHT]?.let { v -> sheet[CoreStat.LIGHT_RADIUS.code] = (stats[CoreStat.LIGHT_RADIUS.code]?.takeIf { it > 0 } ?: ExpeditionWorld.DEFAULT_LIGHT) * (1 - v / 100) }
        effects[MapStats.HERO_RESIST]?.let { v -> resists.forEach { sheet[it] = (stats[it] ?: 0.0) - v } }
        effects[MapStats.HERO_REGEN]?.let { v -> sheet["STOCK_HEALTH_REGEN"] = (stats["STOCK_HEALTH_REGEN"] ?: 0.0) * max(0.0, 1 - v / 100) }
        effects[MapStats.HERO_SLOW]?.let { v -> sheet["STOCK_MOVEMENT_SPEED"] = (stats["STOCK_MOVEMENT_SPEED"] ?: 0.0) - v }
        fun add(stat: String, v: Double) { sheet[stat] = (sheet[stat] ?: 0.0) + v }
        fun scale(stat: String, share: Double) { sheet[stat] = (sheet[stat] ?: 0.0) * max(0.0, 1 + share / 100) }
        effects[MapStats.HERO_DAMAGE_TAKEN]?.let { add("STOCK_DAMAGE_TAKEN", it) }
        effects[MapStats.HERO_RECOVERY]?.let { add("STOCK_RECOVERY_RATE", -it) }
        effects[MapStats.HERO_MAX_RESIST]?.let { v -> add("STOCK_RESIST_MAX_ALL", -v); add("STOCK_RESIST_MAX_CHAOS", -v) }
        effects[MapStats.HERO_DEFENCES]?.let { v -> listOf("STOCK_ARMOR", "STOCK_EVASION", "STOCK_ENERGY_SHIELD").forEach { scale(it, -v) } }
        effects[MapStats.HERO_BLOCK]?.let { add("STOCK_BLOCK_CHANCE", -it) }
        effects[MapStats.HERO_CRIT]?.let { scale("STOCK_CRITICAL_CHANCE", -it) }
        effects[MapStats.HERO_HASTE]?.let { add("STOCK_MOVEMENT_SPEED", it) }
        effects[MapStats.HERO_ATTACK_SPEED]?.let { scale("STOCK_ATTACK_SPEED", it) }
        effects[MapStats.HERO_LIFE]?.let { scale("STOCK_HEALTH", it) }
        effects[MapStats.HERO_LEECH]?.let { add("STOCK_LEECH_ALL", it) }
        effects[MapStats.HERO_MANA_REGEN]?.let { v -> sheet["STOCK_MANA_REGEN"] = (100 + (sheet["STOCK_MANA_REGEN"] ?: 0.0)) * max(0.0, 1 - v / 100) - 100 }
        effects[MapStats.SKILL_COST]?.let { add("STOCK_SKILL_COST", -it) }
        effects[MapStats.FLASK_CHARGES]?.let { add("STOCK_FLASK_CHARGES_GAINED", -it) }
        AtlasEffects.hero.forEach { (atlas, stat) -> effects[atlas]?.let { add(stat, it) } }
        return sheet
    }

    /** Two maps of effects summed per stat: a Vaal zone's lines over the map's. */
    fun sum(a: Map<String, Double>, b: Map<String, Double>): Map<String, Double> = (a.keys + b.keys).associateWith { (a[it] ?: 0.0) + (b[it] ?: 0.0) }
}
