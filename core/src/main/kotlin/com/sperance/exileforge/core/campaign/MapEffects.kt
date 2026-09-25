package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.MapRule
import com.sperance.exileforge.core.model.campaign.MonsterEffect
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * What a map does to a run (since 2.37.0, server 0.35.0). The fight is the client's, so the map's
 * summed effects — the server's [com.sperance.exileforge.core.model.campaign.ActiveMap.effects] —
 * are applied here, before the dice are thrown: monster buffs join every rarity's effects and fold
 * by the same formula as a modifier, the pack and the rarer monsters change the map's counts and
 * weights, and the hero's debuffs take their share of the sheet. What the map pays is the server's.
 */
object MapEffects {

    private val damage = DamageType.entries.map { it.attack }
    private val resists = DamageType.entries.mapNotNull { it.resist }

    fun map(map: CampaignMap, effects: Map<String, Double>): CampaignMap {
        val pack = 1 + (effects[MapRule.PACK_SIZE] ?: 0.0) / 100
        return if (pack == 1.0) map else map.copy(monsterCount = map.monsterCount.map { (it * pack).roundToInt() })
    }

    /** What a map adds to every monster on it, as effects folded by the same formula as a modifier. */
    fun buffs(effects: Map<String, Double>): List<MonsterEffect> = buildList {
        effects[MapRule.MONSTER_LIFE]?.let { add(MonsterEffect("STOCK_HEALTH", "INCREASED", it)) }
        effects[MapRule.MONSTER_DAMAGE]?.let { v -> damage.forEach { add(MonsterEffect(it, "INCREASED", v)) } }
        effects[MapRule.MONSTER_SPEED]?.let { v -> listOf("STOCK_ATTACK_SPEED", "STOCK_CAST_SPEED").forEach { add(MonsterEffect(it, "INCREASED", v)) } }
        effects[MapRule.MONSTER_RESIST]?.let { v -> resists.forEach { add(MonsterEffect(it, "ADD", v)) } }
        // Server 0.66.0: the buffs of Path of Exile's maps.
        effects[MapRule.MONSTER_PENETRATION]?.let { add(MonsterEffect("STOCK_PENETRATE_ELEMENTAL", "ADD", it)) }
        effects[MapRule.MONSTER_REFLECT]?.let { add(MonsterEffect("STOCK_REFLECT", "ADD", it)) }
        effects[MapRule.MONSTER_CRITICAL]?.let { add(MonsterEffect("STOCK_CRITICAL_CHANCE", "ADD", it)) }
        effects[MapRule.MONSTER_AILMENTS]?.let { v -> Ailment.entries.filter { it != Ailment.CHILLED }.forEach { add(MonsterEffect("STOCK_${it.word}_CHANCE", "ADD", v)) } }
        effects[MapRule.MONSTER_ARMOUR]?.let { v -> listOf("STOCK_ARMOR", "STOCK_EVASION").forEach { add(MonsterEffect(it, "INCREASED", v)) } }
        effects[MapRule.MONSTER_LEECH]?.let { add(MonsterEffect("STOCK_LEECH_ALL", "ADD", it)) }
        effects[MapRule.MONSTER_STUN]?.let { add(MonsterEffect("STOCK_AVOID_STUN", "ADD", it)) }
    }

    /** What a map does to its boss alone (server 0.66.0): so many percent more life and damage. */
    fun bossBuffs(effects: Map<String, Double>): List<MonsterEffect> = buildList {
        effects[MapRule.BOSS_POWER]?.takeIf { it > 0 }?.let { v -> (listOf("STOCK_HEALTH") + damage).forEach { add(MonsterEffect(it, "MORE", v)) } }
    }

    /** Fountains a map adds beyond the rule's (server 0.66.0). */
    fun fountains(effects: Map<String, Double>): Int = (effects[MapRule.FOUNTAINS] ?: 0.0).roundToInt().coerceAtLeast(0)

    fun rarities(rarities: List<CampaignRarity>, effects: Map<String, Double>): List<CampaignRarity> {
        val rarer = 1 + (effects[MapRule.MONSTER_RARITY] ?: 0.0) / 100
        val magic = 1 + (effects[MapRule.MAGIC_MONSTERS] ?: 0.0) / 100
        val rare = 1 + (effects[MapRule.RARE_MONSTERS] ?: 0.0) / 100
        val buffs = buffs(effects)
        // "All monsters are at least magic" (server 0.66.0): the plain ones never roll.
        val magicFloor = (effects[MapRule.MONSTER_MAGIC_MIN] ?: 0.0) > 0
        return rarities.map { rarity ->
            val weight = when (rarity.rarity) {
                MonsterRarity.MAGIC.name -> (rarity.weight * rarer * magic).roundToInt()
                MonsterRarity.RARE.name -> (rarity.weight * rarer * rare).roundToInt()
                MonsterRarity.NORMAL.name -> if (magicFloor) 0 else rarity.weight
                MonsterRarity.UNIQUE.name -> rarity.weight
                else -> (rarity.weight * rarer).roundToInt()
            }
            rarity.copy(weight = weight, effects = rarity.effects + buffs)
        }
    }

    fun hero(stats: Map<String, Double>, effects: Map<String, Double>): Map<String, Double> {
        if (effects.isEmpty()) return stats
        val sheet = stats.toMutableMap()
        effects[MapRule.HERO_LIGHT]?.let { v -> sheet["STOCK_LIGHT_RADIUS"] = (stats["STOCK_LIGHT_RADIUS"]?.takeIf { it > 0 } ?: ExpeditionWorld.DEFAULT_LIGHT) * (1 - v / 100) }
        effects[MapRule.HERO_RESIST]?.let { v -> resists.forEach { sheet[it] = (stats[it] ?: 0.0) - v } }
        effects[MapRule.HERO_REGEN]?.let { v -> sheet["STOCK_HEALTH_REGEN"] = (stats["STOCK_HEALTH_REGEN"] ?: 0.0) * max(0.0, 1 - v / 100) }
        effects[MapRule.HERO_SLOW]?.let { v -> sheet["STOCK_MOVEMENT_SPEED"] = (stats["STOCK_MOVEMENT_SPEED"] ?: 0.0) - v }
        // Server 0.66.0: the harms of Path of Exile's maps, and the rewards this game gives the hero.
        fun add(stat: String, v: Double) { sheet[stat] = (stats[stat] ?: 0.0) + v }
        fun scale(stat: String, share: Double) { sheet[stat] = (stats[stat] ?: 0.0) * max(0.0, 1 + share / 100) }
        effects[MapRule.HERO_DAMAGE_TAKEN]?.let { add("STOCK_DAMAGE_TAKEN", it) }
        effects[MapRule.HERO_RECOVERY]?.let { add("STOCK_RECOVERY_RATE", -it) }
        effects[MapRule.HERO_MAX_RESIST]?.let { v -> add("STOCK_RESIST_MAX_ALL", -v); add("STOCK_RESIST_MAX_CHAOS", -v) }
        effects[MapRule.HERO_DEFENCES]?.let { v -> listOf("STOCK_ARMOR", "STOCK_EVASION", "STOCK_ENERGY_SHIELD").forEach { scale(it, -v) } }
        effects[MapRule.HERO_BLOCK]?.let { add("STOCK_BLOCK_CHANCE", -it) }
        effects[MapRule.HERO_CRIT]?.let { scale("STOCK_CRITICAL_CHANCE", -it) }
        effects[MapRule.HERO_HASTE]?.let { add("STOCK_MOVEMENT_SPEED", it) }
        effects[MapRule.HERO_ATTACK_SPEED]?.let { scale("STOCK_ATTACK_SPEED", it) }
        effects[MapRule.HERO_LIFE]?.let { scale("STOCK_HEALTH", it) }
        effects[MapRule.HERO_LEECH]?.let { add("STOCK_LEECH_ALL", it) }
        return sheet
    }

    /** A map item's rolls summed per stat, as the server sums them on entry: display, never sent back. */
    fun of(params: List<Modifier>, definitions: List<ModifierDefinition>): Map<String, Double> {
        val byCode = definitions.associateBy { it.code }
        val effects = mutableMapOf<String, Double>()
        params.forEach { modifier ->
            byCode[modifier.modifierCode]?.effects?.forEachIndexed { index, effect -> effects.merge(effect.stat, modifier.values.getOrElse(index) { 0.0 }, Double::plus) }
        }
        return effects
    }
}
