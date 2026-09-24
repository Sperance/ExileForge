package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.BehaviourRule
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignMonster
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.MonsterEffect
import com.sperance.exileforge.core.model.campaign.MonsterModifier
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import kotlin.random.Random

/** A monster as it stands on the map: its rarity, what it rolled, and the stats that came of it. */
data class RolledMonster(
    val code: String,
    val form: String,
    val rarity: MonsterRarity,
    val modifiers: List<MonsterModifier>,
    val stats: Map<String, Double>,
    val behaviour: BehaviourRule = BehaviourRule(),
    /** What the entered map added to it (since 2.45.0): already in [stats], kept apart so the arena can say so. */
    val mapBuffs: List<MonsterEffect> = emptyList(),
    /** The corrupted zone's guardian (since 2.53.0): reported through `corrupt`, never `kill`/`boss`. */
    val corrupted: Boolean = false,
)

/**
 * Rarity and modifiers of a monster — the client's roll since server 0.26.0.
 *
 * Since 0.27.0 the tier does three things, all from the server's tables: its effects raise every
 * growing stat, it opens modifiers whose `minRarity` it reaches, and it multiplies the values of
 * what it rolls by its `modifierPower` — so the modifier lines on screen print the stronger number.
 *
 * The client fights, so it is the client that has to know what it is fighting. The weights, the
 * counts and the effects are the server's tables; only the dice are thrown here, and the rarity
 * rolled is what the kill reports, because the server pays by it.
 */
object MonsterRoller {

    fun roll(map: CampaignMap, rarities: List<CampaignRarity>, random: Random): RolledMonster {
        val monster = map.monsters[random.nextInt(map.monsters.size)]
        val rule = weighted(rarities, random) { it.weight } ?: CampaignRarity(MonsterRarity.NORMAL.name, 1)
        val rarity = MonsterRarity.entries.firstOrNull { it.name == rule.rarity } ?: MonsterRarity.NORMAL
        val count = rule.modifiers.let { (low, high) -> if (high > low) random.nextInt(low, high + 1) else low }
        // The tier widens the pool and strengthens what is drawn from it.
        val tier = rarity.ordinal
        val pool = map.modifiers.filter { it.minLevel <= map.level && (MonsterRarity.entries.firstOrNull { r -> r.name == it.minRarity }?.ordinal ?: 1) <= tier }.toMutableList()
        val picked = List(count) { weighted(pool, random) { it.weight }?.also { pool.remove(it) } }.filterNotNull()
            .map { modifier -> modifier.copy(effects = modifier.effects.map { it.copy(value = it.value * rule.modifierPower) }) }
        return RolledMonster(monster.code, monster.form, rarity, picked, fold(monster, rule.effects + picked.flatMap { it.effects }), monster.behaviour)
    }

    /**
     * The map's boss as it stands (since server 0.32.0): nothing is thrown — its rarity is `UNIQUE`
     * and its modifiers are fixed — and it folds by the same formula, the tier's power included.
     */
    fun boss(map: CampaignMap, rarities: List<CampaignRarity>): RolledMonster? {
        val boss = map.boss ?: return null
        val rule = rarities.firstOrNull { it.rarity == MonsterRarity.UNIQUE.name } ?: CampaignRarity(MonsterRarity.UNIQUE.name, 0)
        val modifiers = boss.modifiers.map { modifier -> modifier.copy(effects = modifier.effects.map { it.copy(value = it.value * rule.modifierPower) }) }
        val body = CampaignMonster(boss.code, boss.form, boss.stats, boss.behaviour)
        return RolledMonster(boss.code, boss.form, MonsterRarity.UNIQUE, modifiers, fold(body, rule.effects + modifiers.flatMap { it.effects }), boss.behaviour)
    }

    /**
     * The corrupted zone's guardian (since server 0.46.0): [chance] whether this run rolled one at
     * all — nothing stands there otherwise. Folds the same way as [boss], `UNIQUE` and fixed.
     */
    fun corruption(map: CampaignMap, rarities: List<CampaignRarity>, chance: Double, random: Random): RolledMonster? {
        val guardian = map.corrupted ?: return null
        if (random.nextDouble() >= chance) return null
        val rule = rarities.firstOrNull { it.rarity == MonsterRarity.UNIQUE.name } ?: CampaignRarity(MonsterRarity.UNIQUE.name, 0)
        val modifiers = guardian.modifiers.map { modifier -> modifier.copy(effects = modifier.effects.map { it.copy(value = it.value * rule.modifierPower) }) }
        val body = CampaignMonster(guardian.code, guardian.form, guardian.stats, guardian.behaviour)
        return RolledMonster(guardian.code, guardian.form, MonsterRarity.UNIQUE, modifiers, fold(body, rule.effects + modifiers.flatMap { it.effects }), guardian.behaviour, corrupted = true)
    }

    /**
     * A monster's stats after its rarity and modifiers: `(base + ΣADD) × (1 + ΣINCREASED/100) ×
     * Π(1 + MORE/100)`, and `SET` last — the formula the server folds item modifiers with.
     */
    fun fold(monster: CampaignMonster, effects: List<MonsterEffect>): Map<String, Double> {
        val byStat = effects.groupBy { it.stat }
        return (monster.stats.keys + byStat.keys).associateWith { stat ->
            val own = byStat[stat].orEmpty()
            own.lastOrNull { it.operation == "SET" }?.value ?: run {
                val added = (monster.stats[stat] ?: 0.0) + own.filter { it.operation == "ADD" }.sumOf { it.value }
                val increased = 1 + own.filter { it.operation == "INCREASED" }.sumOf { it.value } / 100
                val more = own.filter { it.operation == "MORE" }.fold(1.0) { product, it -> product * (1 + it.value / 100) }
                added * increased * more
            }
        }
    }

    private fun <T> weighted(items: List<T>, random: Random, weight: (T) -> Int): T? {
        val live = items.filter { weight(it) > 0 }
        if (live.isEmpty()) return null
        var point = random.nextInt(live.sumOf(weight))
        live.forEach { item -> point -= weight(item); if (point < 0) return item }
        return live.last()
    }
}
