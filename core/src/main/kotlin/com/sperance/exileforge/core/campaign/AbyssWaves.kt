package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.model.campaign.AbyssLaunch
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.MapRule
import com.sperance.exileforge.core.model.campaign.MonsterEffect
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.core.model.skills.SkillBook
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The waves of the Abyss (2.82.0, server 0.72.0): the dice of a depth are the client's, as every monster's.
 * A wave is its depth's own monsters, as many as the server's count and the map's swarm say, magic and rare
 * by the depth's shares, with the map's and the Abyss's buffs on them, and the depth's leader last. A wave
 * larger than [FIGHT] is fought as fights one after another, so no fight holds more than the arena shows.
 */
object AbyssWaves {

    /** The most foes one fight of a wave holds. */
    const val FIGHT = 4

    /**
     * The fights of depth [depth] of [launch], in order. [zone] is the zone entered, [rarities] its rules
     * as the map changed them, [effects] the map's and the atlas's effects; the leader comes last.
     */
    fun wave(launch: AbyssLaunch, depth: Int, zone: CampaignMap, rarities: List<CampaignRarity>, effects: Map<String, Double>,
             skills: SkillBook, extraRareMods: Int, random: Random): List<List<RolledMonster>> {
        val floor = launch.depths.getOrNull(depth - 1) ?: return emptyList()
        if (floor.monsters.isEmpty()) return emptyList()
        val ground = zone.copy(level = floor.level, monsters = floor.monsters, modifiers = launch.modifiers, boss = floor.leader)
        val swarm = 1 + (effects[MapRule.ABYSS_SWARM] ?: 0.0) / 100
        val count = ((random.nextInt(floor.count[0], floor.count[1] + 1)) * swarm).roundToInt().coerceAtLeast(1)
        val shares = rarities(rarities, floor.magic, floor.rare, buffs(effects))
        val foes = List(count) { MonsterRoller.skilled(MonsterRoller.roll(ground, shares, random, extraRareMods), skills, random) }
        // The leader's rule is the unique one of [shares], the Abyss's buffs already on it: only its own is added.
        val leader = floor.leader?.let { MonsterRoller.boss(ground, shares, random, leaderBuffs(effects)) }?.let { MonsterRoller.skilled(it, skills, random) }
        return (foes + listOfNotNull(leader)).chunked(FIGHT)
    }

    /** The zone's rarity rules with the wave's shares for weights — magic and rare in percent, the rest plain — and the Abyss's buffs. */
    fun rarities(rules: List<CampaignRarity>, magic: Double, rare: Double, buffs: List<MonsterEffect>): List<CampaignRarity> = rules.map { rule ->
        val weight = when (rule.rarity) {
            MonsterRarity.NORMAL.name -> (100 - magic - rare).coerceAtLeast(0.0)
            MonsterRarity.MAGIC.name -> magic
            MonsterRarity.RARE.name -> rare
            else -> 0.0
        }
        rule.copy(weight = weight.roundToInt(), effects = rule.effects + buffs)
    }

    /** What the map's lines and the atlas do to every monster of the Abyss: more life, more damage, both. */
    fun buffs(effects: Map<String, Double>): List<MonsterEffect> = buildList {
        effects[MapRule.ABYSS_LIFE]?.let { add(MonsterEffect("STOCK_HEALTH", "INCREASED", it)) }
        effects[MapRule.ABYSS_DAMAGE]?.let { v -> DamageType.entries.forEach { add(MonsterEffect(it.attack, "INCREASED", v)) } }
        effects[AtlasEffects.ABYSS_POWER]?.takeIf { it > 0 }?.let { v -> (listOf("STOCK_HEALTH") + DamageType.entries.map { it.attack }).forEach { add(MonsterEffect(it, "MORE", v)) } }
    }

    /** What the map's «stronger leaders» does to a leader alone: so many percent more life and damage. */
    fun leaderBuffs(effects: Map<String, Double>): List<MonsterEffect> =
        effects[MapRule.ABYSS_LEADER]?.takeIf { it > 0 }?.let { v -> (listOf("STOCK_HEALTH") + DamageType.entries.map { it.attack }).map { MonsterEffect(it, "MORE", v) } }.orEmpty()
}
