package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.MonsterEffect
import kotlin.math.max
import kotlin.math.roundToInt

/** The stats of a map item, by name, as the client reads them: each the rules' [MapStat], checked against the registry at load. */
object MapStats {
    val QUANTITY = MapStat.QUANTITY.code
    val RARITY = MapStat.RARITY.code
    val EXPERIENCE = MapStat.EXPERIENCE.code
    val PACK_SIZE = MapStat.PACK_SIZE.code
    val MONSTER_RARITY = MapStat.MONSTER_RARITY.code
    val MAGIC_MONSTERS = MapStat.MAGIC_MONSTERS.code
    val RARE_MONSTERS = MapStat.RARE_MONSTERS.code
    val MONSTER_LIFE = MapStat.MONSTER_LIFE.code
    val MONSTER_DAMAGE = MapStat.MONSTER_DAMAGE.code
    val MONSTER_SPEED = MapStat.MONSTER_SPEED.code
    val MONSTER_RESIST = MapStat.MONSTER_RESIST.code
    val HERO_LIGHT = MapStat.HERO_LIGHT.code
    val HERO_RESIST = MapStat.HERO_RESIST.code
    val HERO_REGEN = MapStat.HERO_REGEN.code
    val HERO_SLOW = MapStat.HERO_SLOW.code
    val HERO_DAMAGE_TAKEN = MapStat.HERO_DAMAGE_TAKEN.code
    val HERO_RECOVERY = MapStat.HERO_RECOVERY.code
    val HERO_MAX_RESIST = MapStat.HERO_MAX_RESIST.code
    val HERO_DEFENCES = MapStat.HERO_DEFENCES.code
    val HERO_BLOCK = MapStat.HERO_BLOCK.code
    val HERO_CRIT = MapStat.HERO_CRIT.code
    val MONSTER_PENETRATION = MapStat.MONSTER_PENETRATION.code
    val MONSTER_REFLECT = MapStat.MONSTER_REFLECT.code
    val MONSTER_CRITICAL = MapStat.MONSTER_CRITICAL.code
    val MONSTER_AILMENTS = MapStat.MONSTER_AILMENTS.code
    val MONSTER_ARMOUR = MapStat.MONSTER_ARMOUR.code
    val MONSTER_LEECH = MapStat.MONSTER_LEECH.code
    val MONSTER_STUN = MapStat.MONSTER_STUN.code
    val MONSTER_MAGIC_MIN = MapStat.MONSTER_MAGIC_MIN.code
    val HERO_HASTE = MapStat.HERO_HASTE.code
    val HERO_ATTACK_SPEED = MapStat.HERO_ATTACK_SPEED.code
    val HERO_LIFE = MapStat.HERO_LIFE.code
    val HERO_LEECH = MapStat.HERO_LEECH.code
    val GOLD = MapStat.GOLD.code
    val FOUNTAINS = MapStat.FOUNTAINS.code
    val CHESTS = MapStat.CHESTS.code
    val BOSS_POWER = MapStat.BOSS_POWER.code
    val FLASK_CHARGES = MapStat.FLASK_CHARGES.code
    val HERO_MANA_REGEN = MapStat.HERO_MANA_REGEN.code
    val MONSTER_CAST = MapStat.MONSTER_CAST.code
    val SKILL_COST = MapStat.SKILL_COST.code
    val CRYSTALS = MapStat.CRYSTALS.code
    val BOOKS = MapStat.BOOKS.code
    val ABYSS_CRACKS = MapStat.ABYSS_CRACKS.code
    val ABYSS_DEPTH = MapStat.ABYSS_DEPTH.code
    val ABYSS_HOARD = MapStat.ABYSS_HOARD.code
    val ABYSS_UNIQUE = MapStat.ABYSS_UNIQUE.code
    val ABYSS_ORBS = MapStat.ABYSS_ORBS.code
    val ABYSS_RARE = MapStat.ABYSS_RARE.code
    val ABYSS_LIFE = MapStat.ABYSS_LIFE.code
    val ABYSS_DAMAGE = MapStat.ABYSS_DAMAGE.code
    val ABYSS_SWARM = MapStat.ABYSS_SWARM.code
    val ABYSS_LEADER = MapStat.ABYSS_LEADER.code

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
        effects[MapStat.HERO_DEGEN.code]?.let { add("STOCK_LIFE_DEGEN_PERCENT", it) }
        effects[MapStats.HERO_MANA_REGEN]?.let { v -> sheet["STOCK_MANA_REGEN"] = (100 + (sheet["STOCK_MANA_REGEN"] ?: 0.0)) * max(0.0, 1 - v / 100) - 100 }
        effects[MapStats.SKILL_COST]?.let { add("STOCK_SKILL_COST", -it) }
        effects[MapStats.FLASK_CHARGES]?.let { add("STOCK_FLASK_CHARGES_GAINED", -it) }
        AtlasEffects.hero.forEach { (atlas, stat) -> effects[atlas]?.let { add(stat, it) } }
        return sheet
    }

    /** Two maps of effects summed per stat: a Vaal zone's lines over the map's. */
    fun sum(a: Map<String, Double>, b: Map<String, Double>): Map<String, Double> = (a.keys + b.keys).associateWith { (a[it] ?: 0.0) + (b[it] ?: 0.0) }
}
