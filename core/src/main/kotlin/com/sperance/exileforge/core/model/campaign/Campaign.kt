package com.sperance.exileforge.core.model.campaign

import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import kotlinx.serialization.Serializable

/** The monster rarities, as the server names them; `UNIQUE` (server 0.32.0) is a map's boss alone. */
enum class MonsterRarity { NORMAL, MAGIC, RARE, UNIQUE }

/** One change to a monster's characteristic: `ADD`, `INCREASED`, `MORE` or `SET`, as on items. */
@Serializable data class MonsterEffect(val stat: String, val operation: String, val value: Double)

/**
 * How often a rarity appears, how many modifiers it brings and what it adds on its own.
 *
 * Since server 0.27.0 a tier raises every growing stat: the server expands that into [effects], so
 * it folds like anything else. [modifierPower] is how much stronger this tier's modifiers roll.
 */
@Serializable data class CampaignRarity(
    val rarity: String,
    val weight: Int,
    val modifiers: List<Int> = listOf(0, 0),
    val statScale: Double = 0.0,
    val modifierPower: Double = 1.0,
    val effects: List<MonsterEffect> = emptyList(),
    val quantity: Double = 1.0,
    val rarityBonus: Double = 0.0,
    val experience: Double = 1.0,
)

/**
 * A monster modifier, its values already raised to the map it came with. [minRarity] is the lowest
 * tier that may roll it: a rare monster draws from a wider pool than a magic one.
 */
@Serializable data class MonsterModifier(
    val code: String,
    val weight: Int,
    val minLevel: Int = 1,
    val minRarity: String = MonsterRarity.MAGIC.name,
    val effects: List<MonsterEffect> = emptyList(),
)

/**
 * How a monster walks the map before a fight (since server 0.30.0): the server's numbers, the
 * client's steps. [type] is `WANDER`, `PATROL`, `AMBUSH` or `SLEEP`; [sight] is how far it notices
 * a hero it can see, [wake] how close an ambusher or a sleeper lets one come, and [giveUp] how many
 * seconds it hunts a hero it has lost before it goes home. The defaults are what every monster did
 * before the server said.
 */
@Serializable data class BehaviourRule(
    val type: String = WANDER,
    val wanderSpeed: Double = 1.1,
    val chaseSpeed: Double = 2.2,
    val sight: Double = 4.5,
    val wanderRadius: Double = 3.0,
    val wake: Double = 0.0,
    val giveUp: Double = 3.0,
) {
    companion object {
        const val WANDER = "WANDER"
        const val PATROL = "PATROL"
        const val AMBUSH = "AMBUSH"
        const val SLEEP = "SLEEP"
    }
}

/** A monster of one map: stats at the map's level, the silhouette it is drawn as and how it walks. */
@Serializable data class CampaignMonster(
    val code: String,
    val form: String = "",
    val stats: Map<String, Double> = emptyMap(),
    val behaviour: BehaviourRule = BehaviourRule(),
)

/**
 * A map's boss (since server 0.32.0), the guardian of its exit: stats and its fixed modifiers
 * already raised to the map's level. It rolls nothing — its rarity is `UNIQUE` and its modifiers
 * are the same every time.
 */
@Serializable data class CampaignBoss(
    val code: String,
    val form: String = "",
    val stats: Map<String, Double> = emptyMap(),
    val behaviour: BehaviourRule = BehaviourRule(),
    val modifiers: List<MonsterModifier> = emptyList(),
)

@Serializable data class CampaignMap(
    val code: String,
    val chapter: String = "",
    val order: Int = 0,
    val biome: String = "",
    val level: Int = 1,
    val monsterCount: List<Int> = listOf(10, 14),
    val monsters: List<CampaignMonster> = emptyList(),
    val modifiers: List<MonsterModifier> = emptyList(),
    /** How much the biome widens or narrows the hero's light (since server 0.30.0). */
    val light: Double = 1.0,
    /** The guardian of the exit (since server 0.32.0), or none from an older server. */
    val boss: CampaignBoss? = null,
)

@Serializable data class CampaignChapter(val code: String, val maps: List<CampaignMap> = emptyList())

/**
 * One ailment, as the server rules it (0.28.0): which damage [type] inflicts it, with what [chance]
 * per hit that dealt some, and what it does — a damage-over-time ailment deals [magnitude] percent
 * of that hit's damage of the type over [duration] seconds; a chill slows the target's actions by
 * [magnitude] percent; a shock makes it take [magnitude] percent more damage; a freeze only stops
 * it. [threshold] is the share of the target's life the hit must take off first (a freeze needs a
 * heavy blow), and a [stacks] ailment (poison) adds up where the others refresh.
 */
@Serializable data class AilmentRule(
    val ailment: String, val type: String, val chance: Double, val magnitude: Double = 0.0,
    val duration: Double, val threshold: Double = 0.0, val stacks: Boolean = false,
    /** The hero's own base instead of [chance] (since server 0.36.0): as in PoE, igniting, shocking, poisoning and bleeding come from gear alone. */
    val heroChance: Double? = null,
)

@Serializable data class UnarmedRule(val damage: Double = 4.0, val speed: Double = 1.2)
@Serializable data class CriticalRule(val chance: Double = 5.0, val multiplier: Double = 150.0)
@Serializable data class ArmourRule(val factor: Double = 5.0, val cap: Double = 90.0)
@Serializable data class EvasionRule(val base: Double = 150.0, val perLevel: Double = 40.0, val cap: Double = 75.0)
@Serializable data class StunRule(val share: Double = 15.0, val duration: Double = 0.4)
@Serializable data class ShieldRule(val rechargeDelay: Double = 2.0, val rechargePerSecond: Double = 20.0)
@Serializable data class SpellRule(val innateDamage: Double = 2.0, val innatePerLevel: Double = 0.5, val castSpeed: Double = 0.8, val manaCost: Double = 12.0, val manaRegenShare: Double = 1.75)
@Serializable data class FlaskRule(val charges: Int = 3, val perKill: Int = 1, val heal: Double = 40.0, val duration: Double = 3.0)
@Serializable data class RetreatRule(val delay: Double = 1.5)
@Serializable data class DeathRule(val fromLevel: Int = 10, val experienceShare: Double = 5.0)

/**
 * The numbers the fight is played by — the server's since 0.28.0, read with the chapters.
 *
 * The fight is the client's (rule 23) but its constants are not: armour, evasion, criticals,
 * stun, the energy shield's recharge, the innate spell and its mana, the life flask, retreat, the
 * price of death and the six ailments all arrive here. The defaults are what the server ships
 * today, so a test can build a fight without a payload; a served value always wins.
 */
@Serializable data class CombatRules(
    val timeLimit: Double = 60.0, val variance: Double = 20.0, val resistCap: Double = 75.0, val blockCap: Double = 75.0, val spellBlockShare: Double = 50.0,
    val unarmed: UnarmedRule = UnarmedRule(), val critical: CriticalRule = CriticalRule(), val armour: ArmourRule = ArmourRule(),
    val evasion: EvasionRule = EvasionRule(), val stun: StunRule = StunRule(), val shield: ShieldRule = ShieldRule(),
    val spell: SpellRule = SpellRule(), val flask: FlaskRule = FlaskRule(), val retreat: RetreatRule = RetreatRule(), val death: DeathRule = DeathRule(),
    /** Since server 0.36.0: no "+% to maximum resistance" lifts a resistance past [resistHardCap], and no reduction shortens an ailment by more than [ailmentDurationCap] percent. */
    val resistHardCap: Double = 90.0, val ailmentDurationCap: Double = 75.0,
    val ailments: List<AilmentRule> = listOf(
        AilmentRule("BURNING", "STOCK_ATTACK_FIRE", 30.0, 60.0, 4.0, heroChance = 0.0),
        AilmentRule("CHILLED", "STOCK_ATTACK_COLD", 100.0, 15.0, 2.0),
        AilmentRule("FROZEN", "STOCK_ATTACK_COLD", 50.0, 0.0, 0.8, threshold = 15.0),
        AilmentRule("SHOCKED", "STOCK_ATTACK_LIGHTNING", 35.0, 20.0, 3.0, heroChance = 0.0),
        AilmentRule("POISONED", "STOCK_ATTACK_CHAOS", 40.0, 30.0, 3.0, stacks = true, heroChance = 0.0),
        AilmentRule("BLEEDING", "STOCK_ATTACK_PHYSICAL", 15.0, 50.0, 4.0, heroChance = 0.0),
    ),
)

/** The whole campaign as the server serves it, read once per session: the chapters, the rarities and the rules of the fight. */
@Serializable data class CampaignView(val chapters: List<CampaignChapter> = emptyList(), val rarities: List<CampaignRarity> = emptyList(), val combat: CombatRules = CombatRules(),
    val services: ServiceRule = ServiceRule(), val maps: MapRule = MapRule())

/**
 * Maps (since server 0.35.0): an item of slot `MAP` per location, `MAP_<code>`, spent on entry.
 * [risk] is how many percent of quantity, rarity and experience one point of each harmful modifier
 * pays — the server's numbers, printed by the launch window as the server will count them.
 */
@Serializable data class MapRule(
    val dropChance: Double = 0.0,
    val bossChance: Double = 0.0,
    val nextChance: Double = 0.0,
    val rarities: Map<String, Int> = emptyMap(),
    val risk: Map<String, Double> = emptyMap(),
) {
    /** What a map's summed effects add to the loot, in percent: the risk, then each direct bonus on top. */
    fun bonus(effects: Map<String, Double>): MapBonus {
        val risk = Math.round(effects.entries.sumOf { (stat, value) -> value * (risk[stat] ?: 0.0) } * 10) / 10.0
        return MapBonus(risk + (effects[QUANTITY] ?: 0.0), risk + (effects[RARITY] ?: 0.0), risk + (effects[EXPERIENCE] ?: 0.0))
    }

    /** What a map's stat does, for its line's mark: a harm pays by [risk], a reward pays itself, the rest is content. */
    fun kindOf(stat: String): MapLineKind = when {
        stat == QUANTITY || stat == RARITY || stat == EXPERIENCE -> MapLineKind.REWARD
        stat in risk -> MapLineKind.HARM
        else -> MapLineKind.CONTENT
    }

    /** How many percent one rolled value of [stat] pays, by the server's weight; zero for what is not a risk. */
    fun riskOf(stat: String, value: Double): Double = value * (risk[stat] ?: 0.0)

    companion object {
        const val SLOT = "MAP"
        const val QUANTITY = "MAP_QUANTITY"
        const val RARITY = "MAP_RARITY"
        const val EXPERIENCE = "MAP_EXPERIENCE"
        const val PACK_SIZE = "MAP_PACK_SIZE"
        const val MONSTER_RARITY = "MAP_MONSTER_RARITY"
        /** Since 0.38.0, from the alchemist's orbs: magic and rare monsters apart. */
        const val MAGIC_MONSTERS = "MAP_MAGIC_MONSTERS"
        const val RARE_MONSTERS = "MAP_RARE_MONSTERS"
        const val MONSTER_LIFE = "MAP_MONSTER_LIFE"
        const val MONSTER_DAMAGE = "MAP_MONSTER_DAMAGE"
        const val MONSTER_SPEED = "MAP_MONSTER_SPEED"
        const val MONSTER_RESIST = "MAP_MONSTER_RESIST"
        const val HERO_LIGHT = "MAP_HERO_LIGHT"
        const val HERO_FLASK = "MAP_HERO_FLASK"
        const val HERO_RESIST = "MAP_HERO_RESIST"
        const val HERO_REGEN = "MAP_HERO_REGEN"

        /** The template a location's map is: `MAP_<location code>`. */
        fun templateCode(mapCode: String) = "MAP_$mapCode"
    }
}

/** What a line of a map is, as the launch window marks it. */
enum class MapLineKind { HARM, CONTENT, REWARD }

/** A map's pay, in percent of the loot's quantity, rarity and experience. */
@Serializable data class MapBonus(val quantity: Double = 0.0, val rarity: Double = 0.0, val experience: Double = 0.0)

/** The map a hero entered with (since server 0.35.0): its effects summed per stat and what they pay. */
@Serializable data class ActiveMap(
    val mapCode: String = "",
    val effects: Map<String, Double> = emptyMap(),
    val quantity: Double = 0.0,
    val rarity: Double = 0.0,
    val experience: Double = 0.0,
)

/** What entering a location answered: the map spent on it, if any, and its chests as they stand now. */
@Serializable data class MapLaunch(val map: ActiveMap? = null, val chests: ChestState = ChestState())

/**
 * The map's services for gold (since server 0.34.0): a treasure map is one more chest in the
 * window, once a window; summoning brings a slain guardian back. Both cost so much per map level.
 */
@Serializable data class ServiceRule(val treasurePerLevel: Long = 60, val summonPerLevel: Long = 150)

/** What a map service left behind: the hero's gold, and the map's chests and boss as they stand now. */
@Serializable data class MapServiceOutcome(val money: Long = 0, val chests: ChestState = ChestState(), val boss: BossState = BossState())

/** Which maps the character has cleared, and which are open to them. */
@Serializable data class CampaignProgress(val cleared: List<String> = emptyList(), val unlocked: List<String> = emptyList())

/** What one kill brought, rolled by the server, and where the character stands after it. */
@Serializable data class CampaignReward(
    val experience: Double = 0.0,
    val gold: Long = 0,
    val items: List<CharacterItem> = emptyList(),
    val equipment: List<EquipmentInstance> = emptyList(),
    val level: Int = 1,
    val totalExperience: Double = 0.0,
    val money: Long = 0,
)

/**
 * The chests of one map for one hero (since server 0.31.0): how many are still standing and when
 * the server rolls them again, in epoch milliseconds. A run places [left] of them on its map.
 */
/** A map's boss for one hero (since server 0.32.0): alive, or slain until [respawnAt] (epoch milliseconds). */
@Serializable data class BossState(val alive: Boolean = true, val respawnAt: Long = 0)

@Serializable data class ChestState(val left: Int = 0, val refreshAt: Long = 0, val bought: Boolean = false)

/** What a death cost (server 0.28.0): the experience taken, and the level, which never falls. */
@Serializable data class CampaignFall(val lost: Double = 0.0, val level: Int = 1, val totalExperience: Double = 0.0)
