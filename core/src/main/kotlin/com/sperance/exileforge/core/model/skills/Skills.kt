package com.sperance.exileforge.core.model.skills

import com.sperance.exileforge.core.model.modifier.ModifierOperation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.ceil

/** The item category of the skill books in the bag (server 0.69.0); an item's sub-category is its class. */
const val BOOK_CATEGORY = "BOOK"

/** Active skills go into one of three slots, passive ones into one of two (server 0.69.0). */
enum class SkillKind { ACTIVE, PASSIVE }

/** What a skill is: how an active one acts in a fight, or whether a passive is an aura, a free bonus or a trigger. */
@Serializable enum class SkillType(val kind: SkillKind) {
    ATTACK(SkillKind.ACTIVE), SPELL(SkillKind.ACTIVE), WARCRY(SkillKind.ACTIVE), CURSE(SkillKind.ACTIVE),
    HEAL(SkillKind.ACTIVE), GUARD(SkillKind.ACTIVE), AURA(SkillKind.PASSIVE), BONUS(SkillKind.PASSIVE), TRIGGER(SkillKind.PASSIVE),
}

/**
 * When the fight uses a slot by itself: [MANA_30] is a flask's only, [MANUAL] waits for a tap. A skill
 * brings its own default; the player changes it on the slot.
 */
@Serializable enum class SlotCondition {
    READY, FIGHT_START, RARE_OR_BOSS, LIFE_50, LIFE_35, LIFE_20, MANA_30, SHIELD_BROKEN, ENEMIES_3, AILING, MANUAL;

    val flaskOnly: Boolean get() = this == MANA_30
}

/** What a passive answers to. */
@Serializable enum class SkillEvent { KILL, SPELL_KILL, CRIT, SPELL_CRIT, EVADE, BLOCK, HIT_TAKEN, LOW_LIFE, SHIELD_BROKEN, HEALED, SKILL_USE }

/**
 * A skill's number at level 1 and at level 20, straight in between and beyond — levels from gear go past
 * twenty. On the wire it is `[at 1, at 20]`, or one number a skill does not grow.
 */
@Serializable(with = ScaleSerializer::class)
data class Scale(val from: Double, val to: Double = from) {
    fun at(level: Int): Double = from + (to - from) * (level - 1).coerceAtLeast(0) / (MAX_LEVEL - 1)

    companion object { const val MAX_LEVEL = 20 }
}

object ScaleSerializer : KSerializer<Scale> {
    private val list = ListSerializer(Double.serializer())
    override val descriptor: SerialDescriptor = SerialDescriptor("Scale", list.descriptor)
    override fun serialize(encoder: Encoder, value: Scale) = encoder.encodeSerializableValue(list, listOf(value.from, value.to))
    override fun deserialize(decoder: Decoder): Scale = decoder.decodeSerializableValue(list).let { Scale(it.first(), it.getOrElse(1) { _ -> it.first() }) }
}

/** A line of a skill: a modifier whose value grows with the level. */
@Serializable data class SkillStat(val stat: String, val operation: ModifierOperation = ModifierOperation.ADD, val value: Scale)

/** A chance to inflict an ailment; `ELEMENT` is the ailment of the element that struck. */
@Serializable data class SkillAilment(val ailment: String, val chance: Scale)

/** A spell's own damage: its element and base spread, what every increase multiplies. */
@Serializable data class SpellDamage(val element: String, val min: Scale, val max: Scale)

/**
 * A skill's blow. [targets] foes, 0 for all; [weapon] a percent of the weapon's damage, or [spell] its own.
 * [element] with [convert] is the share of the weapon's damage turned to an element (`RANDOM` picks one);
 * [finisher] the percent against a target under an ailment or below 30 % of its life; [stun] a chance.
 */
@Serializable data class SkillHit(
    val targets: Int = 1,
    val hits: Int = 1,
    val weapon: Scale? = null,
    val spell: SpellDamage? = null,
    val element: String? = null,
    val convert: Scale? = null,
    val ailments: List<SkillAilment> = emptyList(),
    val stats: List<SkillStat> = emptyList(),
    val finisher: Scale? = null,
    val stun: Scale? = null,
)

/** Damage over time: [min]..[max] over the whole [duration]. */
@Serializable data class SkillDot(val targets: Int = 1, val element: String, val min: Scale, val max: Scale, val duration: Double,
                                  val ailments: List<SkillAilment> = emptyList())

/** A buff on the hero for [duration] seconds; [counter] answers a block with that percent of the weapon, [nextCrit] makes the next blow critical. */
@Serializable data class SkillBuff(val duration: Double, val stats: List<SkillStat> = emptyList(), val counter: Scale? = null, val nextCrit: Boolean = false)

/** A curse: [stats] on [targets] foes, 0 for all, for [duration] seconds. */
@Serializable data class SkillCurse(val duration: Double, val targets: Int = 0, val stats: List<SkillStat> = emptyList())

/** Healing in percent of the maximum; [cleanse] lifts one ailment. */
@Serializable data class SkillHeal(val life: Scale? = null, val mana: Scale? = null, val cleanse: Boolean = false)

/** A barrier that soaks [life] percent of the maximum life until [duration] runs out. */
@Serializable data class SkillBarrier(val life: Scale, val duration: Double)

/** A passive's answer to [on]: with [chance] (always without one), at most once per [cooldown]. */
@Serializable data class SkillTrigger(
    val on: SkillEvent,
    val chance: Scale? = null,
    val cooldown: Double = 0.0,
    val heal: SkillHeal? = null,
    val shield: Scale? = null,
    val barrier: SkillBarrier? = null,
    val buff: SkillBuff? = null,
    val hit: SkillHit? = null,
    val flaskCharges: Int = 0,
    val ailment: String? = null,
    val twice: Boolean = false,
    val refund: Boolean = false,
)

/** A class skill as the server's book has it (0.69.0); its book is the item `BOOK_<code>`. */
@Serializable data class SkillDefinition(
    val code: String,
    val heroClass: String,
    val type: SkillType,
    val unlock: Int,
    val icon: String = "",
    val mana: Scale? = null,
    val cooldown: Double = 0.0,
    val reserve: Double = 0.0,
    val condition: SlotCondition = SlotCondition.READY,
    val hit: SkillHit? = null,
    val dot: SkillDot? = null,
    val buff: SkillBuff? = null,
    val curse: SkillCurse? = null,
    val heal: SkillHeal? = null,
    val shield: Scale? = null,
    val barrier: SkillBarrier? = null,
    val stats: List<SkillStat> = emptyList(),
    val lowLife: Boolean = false,
    val trigger: SkillTrigger? = null,
) {
    val kind: SkillKind get() = type.kind
    val book: String get() = BOOK_PREFIX + code
    /** A spell pays in cast speed, an attack in its weapon. */
    val spell: Boolean get() = type == SkillType.SPELL

    companion object { const val BOOK_PREFIX = "BOOK_" }
}

/** A monster's skill: a boss or a caster pays for it in its own mana; a blow is a share of its own swing in [SkillHit.element]. */
@Serializable data class MonsterSkill(
    val code: String,
    val icon: String = "",
    val mana: Double = 0.0,
    val cooldown: Double = 1.0,
    val spell: Boolean = true,
    val hit: SkillHit? = null,
    val buff: SkillBuff? = null,
    val curse: SkillCurse? = null,
    val heal: SkillHeal? = null,
    val manaBurn: Double = 0.0,
)

/** A class in the book: the attributes its skills ask for and the mana it starts with. */
@Serializable data class ClassSkills(val code: String, val attributes: List<String> = emptyList(), val mana: Double = 0.0)

@Serializable data class BookRule(val boss: Double = 0.0, val bossOwnClass: Double = 0.0, val rare: Double = 0.0, val guardian: Double = 0.0, val reach: Int = 5)

@Serializable data class ExchangeRule(val books: Int = 3, val goldPerLevel: Long = 100)

@Serializable data class AttributeRule(val single: List<Double> = listOf(2.2, 8.0), val dual: List<Double> = listOf(1.4, 5.0), val triple: List<Double> = listOf(0.9, 3.0))

@Serializable data class SkillBookRules(
    val activeSlots: List<Int> = listOf(1, 12, 28),
    val passiveSlots: List<Int> = listOf(1, 20),
    val manaPerLevel: Double = 5.0,
    val attributes: AttributeRule = AttributeRule(),
    val books: BookRule = BookRule(),
    val exchange: ExchangeRule = ExchangeRule(),
    val casterSpells: Map<String, String> = emptyMap(),
)

/** `skills` of `world.json` (server 0.69.0): the rules, the classes, their skills and the monsters'. */
@Serializable data class SkillBook(
    val rules: SkillBookRules = SkillBookRules(),
    val classes: List<ClassSkills> = emptyList(),
    val skills: List<SkillDefinition> = emptyList(),
    val monsterSkills: List<MonsterSkill> = emptyList(),
) {
    val byCode: Map<String, SkillDefinition> by lazy { skills.associateBy { it.code } }
    val monsters: Map<String, MonsterSkill> by lazy { monsterSkills.associateBy { it.code } }
    fun ofClass(heroClass: String): List<SkillDefinition> = skills.filter { it.heroClass == heroClass }
    /** The skill a book item teaches; null when [itemCode] is no book. */
    fun byBook(itemCode: String): SkillDefinition? = itemCode.takeIf { it.startsWith(SkillDefinition.BOOK_PREFIX) }?.let { byCode[it.removePrefix(SkillDefinition.BOOK_PREFIX)] }

    /** The hero's level that [level] of [skill] asks: from its opening straight to seventy for the twentieth — the server's `SkillRules`. */
    fun heroLevel(skill: SkillDefinition, level: Int): Int =
        ceil(skill.unlock + (MAX_HERO_LEVEL - skill.unlock) * (level - 1) / (Scale.MAX_LEVEL - 1.0) - 1e-9).toInt()

    /** What [level] of [skill] asks: the hero's level and so much of each of the class's attributes. */
    fun need(skill: SkillDefinition, level: Int): SkillNeed {
        val heroLevel = heroLevel(skill, level)
        val attributes = classes.firstOrNull { it.code == skill.heroClass }?.attributes.orEmpty()
        val (factor, plus) = when (attributes.size) {
            1 -> rules.attributes.single
            2 -> rules.attributes.dual
            else -> rules.attributes.triple
        }.let { it.getOrElse(0) { 0.0 } to it.getOrElse(1) { 0.0 } }
        val amount = ceil(factor * heroLevel + plus - 1e-9).toInt()
        return SkillNeed(heroLevel, attributes.associateWith { amount })
    }

    /** How many active and passive slots are open at [heroLevel]. */
    fun activeSlots(heroLevel: Int): Int = rules.activeSlots.count { it <= heroLevel }
    fun passiveSlots(heroLevel: Int): Int = rules.passiveSlots.count { it <= heroLevel }

    /** The class's mana without intelligence at [level]: its base and what every level adds. */
    fun mana(heroClass: String, level: Int): Double = (classes.firstOrNull { it.code == heroClass }?.mana ?: 0.0) + rules.manaPerLevel * level

    companion object {
        /** The hero's level the twentieth level of every skill asks. */
        const val MAX_HERO_LEVEL = 70
    }
}

/** What a level of a skill asks (server 0.69.0): the hero's level and each attribute, by the sheet's stat. */
data class SkillNeed(val heroLevel: Int, val attributes: Map<String, Int>) {
    /** What of it [level] and [stats] leave unmet, as pairs of what and how much; empty — the book can be read. */
    fun unmet(level: Int, stats: Map<String, Double>): List<Pair<String, Int>> =
        listOfNotNull(("LEVEL" to heroLevel).takeIf { level < heroLevel }) + attributes.filter { (stat, amount) -> (stats[stat] ?: 0.0) < amount }.toList()
}

/** An active slot: what is in it and when the fight uses it. */
@Serializable data class ActiveSlot(val skill: String, val condition: SlotCondition)

/**
 * The hero's skills on the character document (server 0.69.0): learned levels, the slots by index —
 * three active and two passive, null for empty — and the conditions of the three belt places, null for
 * the flask's own kind.
 */
@Serializable data class HeroSkills(
    val learned: Map<String, Int> = emptyMap(),
    val active: List<ActiveSlot?> = emptyList(),
    val passive: List<String?> = emptyList(),
    val flasks: List<SlotCondition?> = emptyList(),
) {
    fun level(code: String): Int = learned[code] ?: 0
}
