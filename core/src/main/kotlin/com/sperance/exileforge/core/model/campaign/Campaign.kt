package com.sperance.exileforge.core.model.campaign

import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import kotlinx.serialization.Serializable

/** The three monster rarities, as the server names them. */
enum class MonsterRarity { NORMAL, MAGIC, RARE }

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

/** A monster of one map: stats at the map's level, and the silhouette it is drawn as. */
@Serializable data class CampaignMonster(val code: String, val form: String = "", val stats: Map<String, Double> = emptyMap())

@Serializable data class CampaignMap(
    val code: String,
    val chapter: String = "",
    val order: Int = 0,
    val biome: String = "",
    val level: Int = 1,
    val monsterCount: List<Int> = listOf(10, 14),
    val monsters: List<CampaignMonster> = emptyList(),
    val modifiers: List<MonsterModifier> = emptyList(),
)

@Serializable data class CampaignChapter(val code: String, val maps: List<CampaignMap> = emptyList())

/** The whole campaign as the server serves it, read once per session. */
@Serializable data class CampaignView(val chapters: List<CampaignChapter> = emptyList(), val rarities: List<CampaignRarity> = emptyList())

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
