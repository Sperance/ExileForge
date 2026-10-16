package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.rules.content.AbyssRule
import com.sperance.exileforge.rules.content.AbyssWave
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.LeaderPower
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssHoardView
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.GuardianFloor
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.Run
import kotlin.math.roundToInt

/** One depth of the Abyss as its sheet shows it: the level its wave stands at, the wave's shares and the hoard waiting at its end. */
data class AbyssDepth(val level: Int, val count: List<Int>, val magic: Double, val rare: Double, val leader: MonsterCode?, val hoard: AbyssHoardView)

/**
 * The waves of the Abyss: the dice of a depth are the client's, as every fight's. A wave is the rule's own
 * monsters, as many as its count and the map's swarm say, magic and rare by the depth's shares, with the map's
 * and the Abyss's buffs on them, and the depth's leader last. Волна режется на бои по `fight.maxFoes` врагов (4.2.0).
 */
class AbyssWaves(private val index: ContentIndex, private val run: Run) {
    private val monsters = MonsterRoller(index)
    private val spawns = Spawns(index, run)
    private val campaign get() = index.campaign

    /** Every depth of the rule from the zone's level, with the hoard as this run's bonuses grow it. */
    fun depths(rule: AbyssRule, zone: Zone): List<AbyssDepth> {
        val bonus = run.hoardBonus(rule)
        val rifts = AbyssRifts(index)
        return rule.waves.mapIndexed { i, wave -> AbyssDepth(run.levelOf(zone) + wave.level, wave.count, wave.magic, wave.rare, wave.leader, rifts.view(rule, i + 1, bonus)) }
    }

    /** The fights of depth [depth] of the crack at [place], in order; [effects] are the map's and the atlas's. */
    fun wave(rule: AbyssRule, depth: Int, place: Int, zone: Zone, effects: Map<String, Double>, extraRareMods: Int): List<List<RolledMonster>> {
        val floor = rule.waves.getOrNull(depth - 1) ?: return emptyList()
        return fights(rule, floor, run.levelOf(zone) + floor.level, run.streams.of("abyssWave", place * 64 + depth), effects, extraRareMods)
    }

    /** The fights of [floor]: its [foes] cut by `fight.maxFoes` (4.2.0) - a depth's wave on a map. */
    fun fights(
        rule: AbyssRule,
        floor: AbyssWave,
        level: Int,
        dice: Dice,
        effects: Map<String, Double>,
        extraRareMods: Int,
    ): List<List<RolledMonster>> = index.rules.fight.fights(foes(rule, floor, level, dice, effects, extraRareMods))

    /**
     * The monsters of [floor] at [level] on [dice] in order, the leader last: a depth's wave, or a floor of the tower (3.49.0;
     * 4.8.0 - one fight with reinforcements) with [extra] — the floor's growth — on every monster, its leader included;
     * [leaderPower] - the tower leader's own strength (4.6.5).
     */
    fun foes(
        rule: AbyssRule,
        floor: AbyssWave,
        level: Int,
        dice: Dice,
        effects: Map<String, Double>,
        extraRareMods: Int,
        extra: List<MonsterEffect> = emptyList(),
        leaderPower: LeaderPower? = null,
    ): List<RolledMonster> {
        if (rule.monsters.isEmpty()) return emptyList()
        val swarm = 1 + (effects[MapStat.ABYSS_SWARM.code] ?: 0.0) / 100
        val count = (dice.between(floor.count) * swarm).roundToInt().coerceAtLeast(1)
        val buffs = MapEffects.buffs(effects) + buffs(effects) + extra
        val pool = monsters.pool(rule.modifiers, level)
        val foes = List(count) {
            val rarity = when {
                dice.percent(floor.rare) -> MonsterRarity.RARE
                dice.percent(floor.magic) -> MonsterRarity.MAGIC
                else -> MonsterRarity.NORMAL
            }
            val ruleOf = campaign.rarity(rarity)
            var mods = dice.between(ruleOf.modifiers)
            if (rarity == MonsterRarity.RARE && mods > 0) mods += extraRareMods
            val monster = index.monster(dice.pick(rule.monsters))!!
            spawns.skilled(spawns.buffed(spawns.sworn(monsters.build(monster, level, ruleOf, monsters.draw(pool, level, ruleOf, mods, dice))), buffs), dice)
        }
        val leader = floor.leader?.takeIf { index.monster(it) != null }?.let { code ->
            val power = leaderPower?.power ?: rule.leaderAt(level)
            spawns.skilled(spawns.buffed(spawns.sworn(monsters.boss(monsters.guardian(code, level, power), leaderBuffs(effects))), buffs), dice)
        }
        return foes + listOfNotNull(leader)
    }

    /** What the map's lines and the atlas do to every monster of the Abyss: more life, more damage, both. */
    fun buffs(effects: Map<String, Double>): List<MonsterEffect> = buildList {
        effects[MapStat.ABYSS_LIFE.code]?.let { add(MonsterEffect(CoreStat.HEALTH.code, Op.INCREASED, it)) }
        effects[MapStat.ABYSS_DAMAGE.code]?.let { v -> GuardianFloor.STRIKES.forEach { add(MonsterEffect(it, Op.INCREASED, v)) } }
        effects[AtlasEffects.ABYSS_POWER]?.takeIf { it > 0 }?.let { v -> (listOf(CoreStat.HEALTH.code) + GuardianFloor.STRIKES).forEach { add(MonsterEffect(it, Op.MORE, v)) } }
    }

    /** What the map's «stronger leaders» does to a leader alone. */
    fun leaderBuffs(effects: Map<String, Double>): List<MonsterEffect> = effects[MapStat.ABYSS_LEADER.code]?.takeIf { it > 0 }?.let { v -> (listOf(CoreStat.HEALTH.code) + GuardianFloor.STRIKES).map { MonsterEffect(it, Op.MORE, v) } }.orEmpty()
}
