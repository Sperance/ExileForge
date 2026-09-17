package com.sperance.exileforge.core.model.combat.world

import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.BattleAction
import com.sperance.exileforge.core.model.combat.BattleReward
import com.sperance.exileforge.core.model.combat.BattleStatus

/**
 * What one server turn changed, as differences between two server snapshots.
 *
 * Every number here is `before - after` over values the server sent. The client has no combat
 * formula: it does not know this hit's armour, resistances or critical multiplier and must not guess
 * them. A hit is worth exactly the life the server took off, which is why every modifier, passive and
 * resistance is already accounted for by the time the world draws a number.
 */
data class BattleDelta(
    val action: BattleAction?, val turn: Int, val status: BattleStatus,
    val heroDamage: Double, val monsterDamage: Double, val heroHealed: Double,
    val shieldAbsorbed: Double, val manaSpent: Double, val manaGained: Double, val potionsSpent: Int,
    val monsterKilled: Boolean, val heroKilled: Boolean, val fresh: Boolean, val turnAdvanced: Boolean,
    val log: List<String>, val rewards: List<BattleReward>,
    val heroCrit: Boolean, val monsterCrit: Boolean, val heroMissed: Boolean, val monsterMissed: Boolean
) {
    /**
     * Nothing moved, nothing was said and the turn did not advance.
     *
     * That is exactly the answer to a replayed idempotent command — the server returns the battle it
     * already resolved — so the world must not play it a second time. A turn in which both sides
     * merely missed still advances [turnAdvanced] and is therefore played.
     */
    val silent: Boolean get() = !fresh && !turnAdvanced && log.isEmpty() && rewards.isEmpty() && potionsSpent == 0 &&
        !monsterKilled && !heroKilled && status == BattleStatus.ACTIVE &&
        heroDamage == 0.0 && monsterDamage == 0.0 && heroHealed == 0.0 &&
        manaSpent == 0.0 && manaGained == 0.0 && shieldAbsorbed == 0.0
}

/**
 * Words that may appear in the server's own log line.
 *
 * They drive a flourish only — a bigger number, a wider burst. A line need not name the fighter, and
 * then the whole turn is searched and the flourish can land on the wrong side; the damage itself never
 * comes from here, so a wrong guess costs nothing but a sparkle.
 */
private val CRIT = listOf("крит", "crit")
private val EVADED = listOf("промах", "уклон", "мимо", "miss", "evad", "dodg")

private fun List<String>.mentions(words: List<String>, subject: String): Boolean {
    val line = firstOrNull { subject.isNotBlank() && it.contains(subject, ignoreCase = true) } ?: joinToString(" ")
    return words.any { line.contains(it, ignoreCase = true) }
}

/**
 * Diff of two snapshots of the same battle.
 *
 * [before] is null for the first snapshot a world sees, and a different battle id counts as a fresh
 * encounter rather than a turn in which nothing happened.
 */
fun battleDelta(before: Battle?, after: Battle, action: BattleAction?): BattleDelta {
    val old = before?.takeIf { it.id == after.id }
    val fresh = old == null
    val heroDamage = ((old?.enemy?.life ?: after.enemy.life) - after.enemy.life).coerceAtLeast(0.0)
    val monsterDamage = ((old?.hero?.life ?: after.hero.life) - after.hero.life).coerceAtLeast(0.0)
    val healed = (after.hero.life - (old?.hero?.life ?: after.hero.life)).coerceAtLeast(0.0)
    val mana = (old?.hero?.mana ?: after.hero.mana) - after.hero.mana
    val log = old?.let { after.log.drop(minOf(it.log.size, after.log.size)) } ?: after.log.takeLast(1)
    val swung = action == BattleAction.ATTACK || action == BattleAction.POWER
    return BattleDelta(
        action = action, turn = after.turn, status = after.status,
        heroDamage = heroDamage, monsterDamage = monsterDamage, heroHealed = healed,
        shieldAbsorbed = ((old?.hero?.shield ?: after.hero.shield) - after.hero.shield).coerceAtLeast(0.0),
        manaSpent = mana.coerceAtLeast(0.0), manaGained = (-mana).coerceAtLeast(0.0),
        potionsSpent = ((old?.potions ?: after.potions) - after.potions).coerceAtLeast(0),
        monsterKilled = after.enemy.life <= 0.0 && (old == null || old.enemy.life > 0.0),
        heroKilled = after.hero.life <= 0.0 && (old == null || old.hero.life > 0.0),
        fresh = fresh, turnAdvanced = old != null && after.turn != old.turn, log = log,
        rewards = old?.let { after.rewards.drop(minOf(it.rewards.size, after.rewards.size)) } ?: after.rewards,
        heroCrit = swung && heroDamage > 0.0 && log.mentions(CRIT, after.hero.name),
        monsterCrit = monsterDamage > 0.0 && log.mentions(CRIT, after.enemy.name),
        heroMissed = swung && heroDamage == 0.0,
        monsterMissed = !fresh && monsterDamage == 0.0 && after.status == BattleStatus.ACTIVE &&
            (log.mentions(EVADED, after.hero.name) || (healed == 0.0 && log.isEmpty()))
    )
}

/** Which reward a drop is, read from the fields the server filled in rather than from its wording. */
fun lootKind(reward: BattleReward): LootKind = when {
    reward.equipmentUuid.isNotBlank() -> LootKind.EQUIPMENT
    reward.itemId.isNotBlank() -> LootKind.CURRENCY
    reward.name.contains("опыт", true) || reward.name.contains("exp", true) -> LootKind.EXPERIENCE
    else -> LootKind.GOLD
}
