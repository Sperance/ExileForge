package com.sperance.exileforge.core.model.combat.arena

import com.sperance.exileforge.core.i18n.tr
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
 * resistance is already accounted for by the time the stage draws a number.
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
     * already resolved — so the stage must not play it a second time. A turn in which both sides
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
 * [before] is null for the first snapshot a stage sees, and a different battle id counts as a fresh
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

/** Which reward a mote is, read from the fields the server filled in rather than from its wording. */
fun lootKind(reward: BattleReward): LootKind = when {
    reward.equipmentUuid.isNotBlank() -> LootKind.EQUIPMENT
    reward.itemId.isNotBlank() -> LootKind.CURRENCY
    reward.name.contains("опыт", true) || reward.name.contains("exp", true) -> LootKind.EXPERIENCE
    else -> LootKind.GOLD
}

/**
 * Expands one turn into the beats that play it out.
 *
 * The rhythm is fixed per action so the stage always reads the same way: the hero closes, winds up and
 * strikes, the life the server took off the mob appears on impact, then the mob answers and the loot
 * the server granted drifts over. A beat with a zero amount is drawn as a miss, never as a hit for
 * nothing, because the client cannot tell the difference except by what the server changed.
 */
fun arenaScript(delta: BattleDelta, hero: String, monster: String, monsterName: String, boss: Boolean,
    element: String, rewardIcon: (BattleReward) -> String): List<ArenaBeat> {
    val beats = mutableListOf<ArenaBeat>()
    if(delta.fresh) {
        beats += ArenaBeat(0L, ArenaBeatKind.SPAWN, monster, element = element)
        beats += ArenaBeat(160L, ArenaBeatKind.BANNER, monster, text = monsterName, crit = boss)
        // Resuming a battle the server already settled: show where it ended rather than replay it, and
        // do not drift loot that reached the stash before the app was reopened.
        if(delta.status != BattleStatus.ACTIVE) {
            if(delta.monsterKilled) beats += ArenaBeat(420L, ArenaBeatKind.DEATH, monster)
            if(delta.heroKilled) beats += ArenaBeat(420L, ArenaBeatKind.DEATH, hero)
            beats += ArenaBeat(900L, ArenaBeatKind.BANNER, hero, text = outcome(delta.status),
                crit = delta.status == BattleStatus.VICTORY && boss)
        }
        return beats
    }
    val swung = delta.action == BattleAction.ATTACK || delta.action == BattleAction.POWER
    when(delta.action) {
        BattleAction.ATTACK, BattleAction.POWER -> {
            beats += ArenaBeat(ArenaTiming.CLOSE, ArenaBeatKind.ADVANCE, hero)
            beats += ArenaBeat(ArenaTiming.HERO_WINDUP, ArenaBeatKind.WINDUP, hero)
            beats += ArenaBeat(ArenaTiming.HERO_STRIKE,
                if(delta.action == BattleAction.POWER) ArenaBeatKind.CAST else ArenaBeatKind.STRIKE, hero, monster)
            beats += ArenaBeat(ArenaTiming.HERO_IMPACT,
                if(delta.heroMissed) ArenaBeatKind.MISS else ArenaBeatKind.IMPACT,
                hero, monster, delta.heroDamage, element, delta.heroCrit)
        }
        BattleAction.GUARD -> beats += ArenaBeat(ArenaTiming.CLOSE, ArenaBeatKind.GUARD, hero, text = tr("Защита", "Guard"))
        BattleAction.POTION -> beats += ArenaBeat(ArenaTiming.CLOSE, ArenaBeatKind.QUAFF, hero, amount = delta.heroHealed)
        BattleAction.FLEE -> {
            beats += ArenaBeat(ArenaTiming.CLOSE, ArenaBeatKind.RETREAT, hero, text = tr("Отступление", "Retreat"))
            beats += ArenaBeat(ArenaTiming.AFTERMATH, ArenaBeatKind.BANNER, hero, text = tr("Отступление", "Retreat"))
            return beats
        }
        null -> Unit
    }
    if(delta.manaSpent > 0.0) beats += ArenaBeat(ArenaTiming.HERO_WINDUP, ArenaBeatKind.MANA, hero, amount = -delta.manaSpent)
    if(delta.monsterKilled) beats += ArenaBeat(ArenaTiming.HERO_IMPACT + 110L, ArenaBeatKind.DEATH, monster)
    else {
        val windup = if(swung) ArenaTiming.MOB_WINDUP else ArenaTiming.DEFENSIVE_WINDUP
        val strike = if(swung) ArenaTiming.MOB_STRIKE else ArenaTiming.DEFENSIVE_STRIKE
        val impact = if(swung) ArenaTiming.MOB_IMPACT else ArenaTiming.DEFENSIVE_IMPACT
        if(delta.monsterDamage > 0.0 || delta.monsterMissed) {
            beats += ArenaBeat(windup, ArenaBeatKind.WINDUP, monster)
            beats += ArenaBeat(strike, ArenaBeatKind.STRIKE, monster, hero)
            beats += ArenaBeat(impact, if(delta.monsterDamage > 0.0) ArenaBeatKind.IMPACT else ArenaBeatKind.MISS,
                monster, hero, delta.monsterDamage, element, delta.monsterCrit)
        }
        if(delta.shieldAbsorbed > 0.0) beats += ArenaBeat(impact + 70L, ArenaBeatKind.ABSORB, hero, amount = delta.shieldAbsorbed)
        if(delta.heroKilled) beats += ArenaBeat(impact + 110L, ArenaBeatKind.DEATH, hero)
    }
    if(swung) beats += ArenaBeat(ArenaTiming.AFTERMATH, ArenaBeatKind.WITHDRAW, hero)
    if(delta.manaGained > 0.0) beats += ArenaBeat(ArenaTiming.AFTERMATH, ArenaBeatKind.MANA, hero, amount = delta.manaGained)
    // Regeneration is whatever the hero gained once the flask this turn is accounted for.
    if(delta.potionsSpent == 0 && delta.heroHealed > 0.0)
        beats += ArenaBeat(ArenaTiming.AFTERMATH, ArenaBeatKind.REGEN, hero, amount = delta.heroHealed)
    delta.rewards.forEachIndexed { index, reward ->
        beats += ArenaBeat(ArenaTiming.AFTERMATH + index * ArenaTiming.LOOT_STRIDE, ArenaBeatKind.LOOT, monster,
            amount = reward.amount.toDouble(), text = reward.name, loot = lootKind(reward), icon = rewardIcon(reward))
    }
    if(delta.status != BattleStatus.ACTIVE) beats += ArenaBeat(
        ArenaTiming.AFTERMATH + delta.rewards.size * ArenaTiming.LOOT_STRIDE + 200L, ArenaBeatKind.BANNER, hero,
        text = outcome(delta.status), crit = delta.status == BattleStatus.VICTORY && boss)
    return beats.sortedBy { it.at }
}

private fun outcome(status: BattleStatus) = when(status) {
    BattleStatus.VICTORY -> tr("Победа", "Victory")
    BattleStatus.DEFEAT -> tr("Поражение", "Defeat")
    else -> tr("Отступление", "Retreat")
}
