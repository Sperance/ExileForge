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
    /** Fights from the back row and strikes from the first second (2.70.0, server 0.61.0). */
    val ranged: Boolean = false,
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

    fun roll(map: CampaignMap, rarities: List<CampaignRarity>, random: Random, extraRareMods: Int = 0): RolledMonster {
        val monster = map.monsters[random.nextInt(map.monsters.size)]
        val rule = weighted(rarities, random) { it.weight } ?: CampaignRarity(MonsterRarity.NORMAL.name, 1)
        val rarity = MonsterRarity.entries.firstOrNull { it.name == rule.rarity } ?: MonsterRarity.NORMAL
        var count = rule.modifiers.let { (low, high) -> if (high > low) random.nextInt(low, high + 1) else low }
        // The atlas (server 0.66.0) hangs more on a rare monster than its rule does.
        if (rarity == MonsterRarity.RARE && count > 0) count += extraRareMods
        // The tier widens the pool and strengthens what is drawn from it.
        val tier = rarity.ordinal
        val pool = map.modifiers.filter { it.minLevel <= map.level && (MonsterRarity.entries.firstOrNull { r -> r.name == it.minRarity }?.ordinal ?: 1) <= tier }.toMutableList()
        val picked = List(count) { weighted(pool, random) { it.weight }?.also { pool.remove(it) } }.filterNotNull()
            .map { modifier -> rolled(modifier, rule.modifierPower, random) }
        return RolledMonster(monster.code, monster.form, rarity, picked, fold(monster, rule.effects + picked.flatMap { it.effects }), monster.behaviour,
            ranged = monster.ranged)
    }

    /**
     * A modifier's values thrown inside its tier (server 0.66.0): one quality for every effect, as an item's
     * composite rolls, times the rarity's power. A line whose range is a point is that point.
     */
    private fun rolled(modifier: MonsterModifier, power: Double, random: Random): MonsterModifier {
        val quality = random.nextDouble()
        return modifier.copy(effects = modifier.effects.map { effect ->
            val value = (effect.value + (effect.max - effect.value) * quality) * power
            effect.copy(value = Math.round(value * 10) / 10.0, max = Math.round(value * 10) / 10.0)
        })
    }

    /**
     * A jetton as a pack (since 2.54.0): usually one monster, 15% of the time 1–3 of them, mixed
     * species from the map's pool, each rolled on its own. [packRandom] is a stream of its own —
     * never the one [random] feeds — so a plain spawn's roll is untouched whether or not this ever
     * ran: the leader (first, fought first) comes from [random] exactly as [roll] alone would give it.
     */
    fun rollPack(map: CampaignMap, rarities: List<CampaignRarity>, random: Random, packRandom: Random, extraRareMods: Int = 0): List<RolledMonster> {
        val leader = roll(map, rarities, random, extraRareMods)
        val size = if (packRandom.nextDouble() < PACK_CHANCE) 1 + packRandom.nextInt(PACK_MAX) else 1
        return listOf(leader) + List(size - 1) { roll(map, rarities, packRandom, extraRareMods) }
    }

    /**
     * The map's boss as it stands (since server 0.32.0): its rarity is `UNIQUE`, its signature modifiers
     * are fixed, and since server 0.66.0 it draws `rolls` more from its own pool at every encounter, each
     * thrown inside its tier; [extra] is what the entered map does to the boss alone. It folds by the
     * same formula as any monster, the tier's power included.
     */
    fun boss(map: CampaignMap, rarities: List<CampaignRarity>, random: Random = Random(0), extra: List<MonsterEffect> = emptyList()): RolledMonster? {
        val boss = map.boss ?: return null
        val rule = rarities.firstOrNull { it.rarity == MonsterRarity.UNIQUE.name } ?: CampaignRarity(MonsterRarity.UNIQUE.name, 0)
        val signature = boss.modifiers.map { rolled(it, rule.modifierPower, random) }
        val count = boss.rolls.let { (low, high) -> if (high > low) random.nextInt(low, high + 1) else low }.coerceAtMost(boss.pool.size)
        val pool = boss.pool.toMutableList()
        val drawn = List(count) { weighted(pool, random) { it.weight }?.also { pool.remove(it) } }.filterNotNull().map { rolled(it, rule.modifierPower, random) }
        val modifiers = signature + drawn
        val body = CampaignMonster(boss.code, boss.form, boss.stats, boss.behaviour, boss.range)
        return RolledMonster(boss.code, boss.form, MonsterRarity.UNIQUE, modifiers, fold(body, rule.effects + modifiers.flatMap { it.effects } + extra), boss.behaviour,
            ranged = body.ranged)
    }

    /**
     * Whether this run hides a Vaal portal (since 2.65.0, server 0.57.0): [chance] of it, on a map
     * whose location has a guardian of corruption to stand at the zone's end.
     */
    fun portal(map: CampaignMap, chance: Double, random: Random): Boolean = map.corrupted != null && random.nextDouble() < chance

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

    private const val PACK_CHANCE = 0.15
    private const val PACK_MAX = 3

    private fun <T> weighted(items: List<T>, random: Random, weight: (T) -> Int): T? {
        val live = items.filter { weight(it) > 0 }
        if (live.isEmpty()) return null
        var point = random.nextInt(live.sumOf(weight))
        live.forEach { item -> point -= weight(item); if (point < 0) return item }
        return live.last()
    }
}
