package com.sperance.exileforge.core.model.campaign

import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.modifier.BenchRecipe
import kotlinx.serialization.Serializable

/** The monster rarities, as the server names them; `UNIQUE` (server 0.32.0) is a map's boss alone. */
enum class MonsterRarity { NORMAL, MAGIC, RARE, UNIQUE }

/** One change to a monster's characteristic: `ADD`, `INCREASED`, `MORE` or `SET`, as on items. */
@Serializable data class MonsterEffect(val stat: String, val operation: String, val value: Double,
    /** The top of the tier's range (server 0.66.0); the roll lands between [value] and it. An older server's line is fixed. */
    val max: Double = value)

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
    /** The tier the map's level opened (server 0.66.0), 1 the best. */
    val tier: Int = 1,
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
    /** `MELEE` or `RANGED` (since server 0.61.0): which row it fights from. */
    val range: String = MELEE,
) {
    val ranged: Boolean get() = range == RANGED

    companion object {
        const val MELEE = "MELEE"
        const val RANGED = "RANGED"
    }
}

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
    /** Its row, as a monster's (since server 0.61.0). */
    val range: String = CampaignMonster.MELEE,
    /** Its pool at the map's level (server 0.66.0): [rolls] random lines of it, low and high, join the signature ones each encounter. */
    val pool: List<MonsterModifier> = emptyList(),
    val rolls: List<Int> = listOf(0, 0),
)

/**
 * A zone of the world map (server 0.67.0; a chapter's map before): its token stands at [x], [y] of
 * the world — `y` grows up from the start — in [region]. [from] are the zones whose links lead in,
 * [to] those its own lead to: a zone opens once the boss of any of [from] is slain. A zone with
 * nowhere to lead that is not its region's [finale] is a dead end.
 */
@Serializable data class CampaignMap(
    val code: String,
    val region: String = "",
    val x: Int = 0,
    val y: Int = 0,
    val from: List<String> = emptyList(),
    val to: List<String> = emptyList(),
    val finale: Boolean = false,
    val biome: String = "",
    val level: Int = 1,
    val monsterCount: List<Int> = listOf(10, 14),
    val monsters: List<CampaignMonster> = emptyList(),
    val modifiers: List<MonsterModifier> = emptyList(),
    /** How much the biome widens or narrows the hero's light (since server 0.30.0). */
    val light: Double = 1.0,
    /** The guardian of the exit (since server 0.32.0), or none from an older server. */
    val boss: CampaignBoss? = null,
    /** The map's side in cells (since server 0.40.0); an older server's maps keep 48. */
    val size: Int = 48,
    /** The corrupted zone's guardian this location can roll (since server 0.46.0), or none from an older server. */
    val corrupted: CampaignBoss? = null,
)

/** A point of the world map, in its units: `x` to the right, `y` up from the start. */
@Serializable data class WorldPoint(val x: Int = 0, val y: Int = 0)

/** The world's field (server 0.67.0): new regions are laid above, so a token's place never moves. */
@Serializable data class WorldRule(val width: Int = 1000, val height: Int = 1000)

/** A region of the world map (server 0.67.0; a chapter before): its zones and where its name is written. */
@Serializable data class CampaignRegion(val code: String, val label: WorldPoint = WorldPoint(), val zones: List<CampaignMap> = emptyList())

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
@Serializable data class RetreatRule(val delay: Double = 1.5)

/**
 * «Волк-одиночка» (since server 0.62.0): a hero fighting without a party deals [dealt] percent more
 * of every damage and takes [taken] percent less. A second fighter in the party ends it.
 */
@Serializable data class LoneWolfRule(val dealt: Double = 10.0, val taken: Double = 10.0)
@Serializable data class DeathRule(val fromLevel: Int = 10, val experienceShare: Double = 5.0)

/**
 * The numbers the fight is played by — the server's since 0.28.0, read with the world.
 *
 * The fight is the client's (rule 23) but its constants are not: armour, evasion, criticals,
 * stun, the energy shield's recharge, the innate spell and its mana, retreat, the
 * price of death and the six ailments all arrive here. The defaults are what the server ships
 * today, so a test can build a fight without a payload; a served value always wins.
 */
@Serializable data class CombatRules(
    val variance: Double = 20.0, val resistCap: Double = 75.0, val blockCap: Double = 75.0,
    val unarmed: UnarmedRule = UnarmedRule(), val critical: CriticalRule = CriticalRule(), val armour: ArmourRule = ArmourRule(),
    val evasion: EvasionRule = EvasionRule(), val stun: StunRule = StunRule(), val shield: ShieldRule = ShieldRule(),
    val retreat: RetreatRule = RetreatRule(), val death: DeathRule = DeathRule(),
    /** Since server 0.36.0: no "+% to maximum resistance" lifts a resistance past [resistHardCap], and no reduction shortens an ailment by more than [ailmentDurationCap] percent. */
    val resistHardCap: Double = 90.0, val ailmentDurationCap: Double = 75.0,
    val loneWolf: LoneWolfRule = LoneWolfRule(),
    val ailments: List<AilmentRule> = listOf(
        AilmentRule("BURNING", "STOCK_ATTACK_FIRE", 30.0, 60.0, 4.0, heroChance = 0.0),
        AilmentRule("CHILLED", "STOCK_ATTACK_COLD", 100.0, 15.0, 2.0),
        AilmentRule("FROZEN", "STOCK_ATTACK_COLD", 50.0, 0.0, 0.8, threshold = 15.0),
        AilmentRule("SHOCKED", "STOCK_ATTACK_LIGHTNING", 35.0, 20.0, 3.0, heroChance = 0.0),
        AilmentRule("POISONED", "STOCK_ATTACK_CHAOS", 40.0, 30.0, 3.0, stacks = true, heroChance = 0.0),
        AilmentRule("BLEEDING", "STOCK_ATTACK_PHYSICAL", 15.0, 50.0, 4.0, heroChance = 0.0),
    ),
)

/** The whole campaign as the server serves it, read once per session: the world map, the rarities and the rules of the fight. */
@Serializable data class CampaignView(val world: WorldRule = WorldRule(), val regions: List<CampaignRegion> = emptyList(), val rarities: List<CampaignRarity> = emptyList(),
    val combat: CombatRules = CombatRules(), val services: ServiceRule = ServiceRule(), val maps: MapRule = MapRule(), val fountains: FountainRule = FountainRule(),
    val corruption: CorruptionRule = CorruptionRule()) {
    /** Every zone of the world, region by region. */
    val zones: List<CampaignMap> get() = regions.flatMap { it.zones }

    fun zone(code: String): CampaignMap? = zones.firstOrNull { it.code == code }
}

/** Fountains (since server 0.43.0): how many a map holds, low and high, and what share of life each gives back, once. */
@Serializable data class FountainRule(val count: List<Int> = listOf(0, 2), val heal: Double = 30.0)

/**
 * The corrupted zone (since server 0.46.0): a random portal on the map, 0-1 a run, by [chance] and
 * the seed. Its guardian is a small boss of its own — [uniqueChance] at a unique from [uniquePools].
 */
@Serializable data class CorruptionRule(val chance: Double = 0.0, val uniqueChance: Double = 0.0, val uniquePools: List<String> = emptyList())

/**
 * Maps (since server 0.35.0): an item of slot `MAP` per location, `MAP_<code>`, spent on entry.
 * [risk] is how many percent of quantity, rarity and experience one point of each harmful modifier
 * pays — the server's numbers, printed by the launch window as the server will count them.
 */
@Serializable data class MapRule(
    val rarities: Map<String, Int> = emptyMap(),
    val risk: Map<String, Double> = emptyMap(),
    /** Since server 0.42.0, as in PoE: how many affixes a map of each rarity rolls, low and high — magic 1–2, rare 4–6. */
    val affixes: Map<String, List<Int>> = emptyMap(),
    /** Since server 0.42.0: what the map's own rarity adds to quantity and rarity, in percent. */
    val rarityBonus: Map<String, Double> = emptyMap(),
) {
    /**
     * What a map's summed effects add to the loot, in percent: the risk, then each direct bonus on
     * top, and the map's own [rarity] to quantity and rarity — the server's `CampaignMaps.active`.
     */
    fun bonus(effects: Map<String, Double>, rarity: String = "COMMON"): MapBonus {
        val risk = Math.round(effects.entries.sumOf { (stat, value) -> value * (risk[stat] ?: 0.0) } * 10) / 10.0
        val own = rarityBonus[rarity] ?: 0.0
        return MapBonus(risk + own + (effects[QUANTITY] ?: 0.0), risk + own + (effects[RARITY] ?: 0.0), risk + (effects[EXPERIENCE] ?: 0.0))
    }

    /** What a map's stat does, for its line's mark: a harm pays by [risk], a reward pays itself, the rest is content. */
    fun kindOf(stat: String): MapLineKind = when {
        stat in rewards -> MapLineKind.REWARD
        stat in risk -> MapLineKind.HARM
        else -> MapLineKind.CONTENT
    }

    /** The lines that give without asking (server 0.66.0 added gold, chests, fountains and the hero's own boons). */
    private val rewards: Set<String> get() = setOf(QUANTITY, RARITY, EXPERIENCE, GOLD, "MAP_CHESTS", FOUNTAINS, HERO_HASTE, HERO_ATTACK_SPEED, HERO_LIFE, HERO_LEECH)

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
        const val HERO_RESIST = "MAP_HERO_RESIST"
        const val HERO_REGEN = "MAP_HERO_REGEN"
        /** Since server 0.65.0: the hero walks the map so many percent slower. */
        const val HERO_SLOW = "MAP_HERO_SLOW"
        // Since server 0.66.0: the harms and buffs of Path of Exile, and rewards to the hero.
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
        const val BOSS_POWER = "MAP_BOSS_POWER"

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

/**
 * What entering a location answered: the map spent on it, if any, its chests as they stand now, and
 * (server 0.60.0) the atlas's summed effects, stat → value.
 */
@Serializable data class MapLaunch(val map: ActiveMap? = null, val chests: ChestState = ChestState(), val atlas: Map<String, Double> = emptyMap())

/**
 * The map's service for gold (since server 0.34.0): summoning brings a slain guardian back, at so
 * much per map level. The treasure map is gone since 2.73.0 (server 0.64.0).
 */
@Serializable data class ServiceRule(val summonPerLevel: Long = 150)

/** What a map service left behind: the hero's gold, and the map's chests and boss as they stand now. */
@Serializable data class MapServiceOutcome(val money: Long = 0, val chests: ChestState = ChestState(), val boss: BossState = BossState())

/** Which zones the character has passed — slain their boss — and which are open to them. */
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
    /** A bench recipe just found on the map (server 0.46.0); null almost always. */
    val recipeFound: BenchRecipe? = null,
    /** The world map's progress after a boss (server 0.67.0): the boss passes its zone and opens the ones it leads to. */
    val progress: CampaignProgress? = null,
)

/**
 * The chests of one map for one hero (since server 0.31.0): how many are still standing and when
 * the server rolls them again, in epoch milliseconds. A run places [left] of them on its map.
 */
/** A map's boss for one hero (since server 0.32.0): alive, or slain until [respawnAt] (epoch milliseconds). */
@Serializable data class BossState(val alive: Boolean = true, val respawnAt: Long = 0)

@Serializable data class ChestState(val left: Int = 0, val refreshAt: Long = 0, val bought: Boolean = false)

/** What a death cost (server 0.28.0): the experience taken, and the level, which never falls. */
/**
 * A Vaal zone as the server rolled it (since server 0.57.0), for the gate before it: its modifiers in
 * the order they rolled, their effects summed per stat — laid on the zone's run as a map's are — and
 * what it adds, in percent, to the loot of everything slain inside and of its guardian.
 */
@Serializable data class VaalZone(
    val mapCode: String = "",
    val level: Int = 1,
    val modifiers: List<com.sperance.exileforge.core.model.modifier.Modifier> = emptyList(),
    val effects: Map<String, Double> = emptyMap(),
    val quantity: Double = 0.0,
    val rarity: Double = 0.0,
    val experience: Double = 0.0,
)

@Serializable data class CampaignFall(val lost: Double = 0.0, val level: Int = 1, val totalExperience: Double = 0.0)
