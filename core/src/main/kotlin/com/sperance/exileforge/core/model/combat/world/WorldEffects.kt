package com.sperance.exileforge.core.model.combat.world

import com.sperance.exileforge.core.model.combat.BattleReward
import kotlin.math.abs

/** Colour family of a floating number or a spark; the renderer owns the actual palette. */
enum class WorldTint { HERO_DAMAGE, MOB_DAMAGE, HEAL, MANA, SHIELD, MISS, GUARD, LOOT }

/** What kind of reward a drop carries, decided from the fields the server filled in. */
enum class LootKind { EXPERIENCE, GOLD, EQUIPMENT, CURRENCY }

/** Anything that is born, ages and is swept off the ground once its time is up. */
abstract class WorldParticle(val life: Long) {
    var age: Long = 0L; private set
    val progress: Float get() = (age.toFloat() / life).coerceIn(0f, 1f)
    internal fun spent(dt: Long): Boolean { age += dt; return age >= life }
}

/** A number rising off a body. [text] is already formatted — the world never rounds twice. */
class DamageNumber(val text: String, val origin: Vec2, val tint: WorldTint, val crit: Boolean = false) :
    WorldParticle(if(crit) 1200L else 950L)

/** Impact debris. Angle and speed are fixed at birth, so a burst keeps its shape across frames. */
class Spark(val origin: Vec2, val angle: Float, val speed: Float, val tint: WorldTint) : WorldParticle(520L)

/** What is left where a mob fell. It darkens to a stain and holds its spot for the rest of the run. */
class Corpse(val icon: String, val position: Vec2, val boss: Boolean) : WorldParticle(Long.MAX_VALUE) {
    val fade: Float get() = (1f - age / 3200f).coerceIn(.16f, 1f)
}

/** A call across the screen: a mob's name, a boss, victory or defeat. */
class Banner(val text: String, val boss: Boolean) : WorldParticle(2400L)

/**
 * A reward the server already granted, lying where the mob fell.
 *
 * Walking over it only plays the pickup: the item reached the stash the moment the server answered,
 * so nothing here can add, lose or re-roll one. [rarity] and [icon] are looked up for the picture.
 */
class GroundLoot(val reward: BattleReward, val kind: LootKind, val rarity: String, val icon: String,
    val position: Vec2) {
    val title: String get() = reward.name
    val amount: Long get() = reward.amount
    val equipmentUuid: String get() = reward.equipmentUuid
}

/** A claimed drop on its way to the hero's pack. */
class LootMote(val loot: GroundLoot, private val hero: WorldActor) : WorldParticle(620L) {
    val position: Vec2 get() = loot.position.lerp(hero.position, progress)
}

/** Whole numbers stay whole: the server's doubles are shown as it rolled them, not re-rounded. */
internal fun formatAmount(value: Double): String =
    if(abs(value - value.toLong()) < .05) abs(value.toLong()).toString() else "%.1f".format(abs(value))
