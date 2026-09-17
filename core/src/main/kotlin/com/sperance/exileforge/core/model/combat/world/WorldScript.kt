package com.sperance.exileforge.core.model.combat.world

import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.BattleAction
import com.sperance.exileforge.core.model.combat.BattleReward
import com.sperance.exileforge.core.model.combat.BattleStatus
import com.sperance.exileforge.core.model.combat.Monster

/** What the world needs from the server's icon set, without dragging the set itself into the engine. */
class WorldArt(
    val forMonster: (Monster) -> String = { "" },
    val forReward: (BattleReward) -> String = { "" },
    /** Rarity of a dropped item once the stash has been read back; blank paints a plain drop. */
    val rarityOf: (BattleReward) -> String = { "" },
    val hero: String = ""
)

/** Beats are the timeline the simulation plays back; one server turn expands into several. */
enum class WorldBeatKind {
    SPAWN, WINDUP, STRIKE, CAST, IMPACT, MISS, GUARD, QUAFF, ABSORB, REGEN, MANA, DEATH, DROP, BANNER, RETREAT
}

/**
 * One scheduled moment of the fight.
 *
 * [amount] only ever carries a number the server sent — a life, mana or shield difference between two
 * snapshots — so playback cannot show a value the server did not produce.
 */
data class WorldBeat(
    val at: Long, val kind: WorldBeatKind, val actor: String, val target: String = "",
    val amount: Double = 0.0, val element: String = "physical", val crit: Boolean = false,
    val text: String = "", val loot: LootKind = LootKind.CURRENCY, val icon: String = "",
    val rarity: String = "", val reward: BattleReward? = null
)

/** Millisecond offsets inside a turn, so the whole rhythm is tuned in one place. */
object WorldTiming {
    const val HERO_WINDUP = 0L
    const val HERO_STRIKE = 170L
    const val HERO_IMPACT = 240L
    const val MOB_WINDUP = 500L
    const val MOB_STRIKE = 680L
    const val MOB_IMPACT = 760L
    const val AFTERMATH = 900L
    const val DROP_STRIDE = 90L
    /** Guard and flask skip the hero's swing, so the mob answers sooner. */
    const val DEFENSIVE_WINDUP = 260L
    const val DEFENSIVE_STRIKE = 440L
    const val DEFENSIVE_IMPACT = 520L
}

/**
 * Expands one turn into the beats that play it out.
 *
 * The rhythm is fixed per action so a fight always reads the same way: the hero winds up and strikes,
 * the life the server took off the mob appears on impact, the mob answers, then the rewards the server
 * granted fall where it died. A beat with a zero amount is drawn as a miss, never as a hit for nothing,
 * because the client cannot tell the difference except by what the server changed.
 */
fun worldScript(delta: BattleDelta, hero: String, monster: String, monsterName: String, boss: Boolean,
    element: String, art: WorldArt): List<WorldBeat> {
    val beats = mutableListOf<WorldBeat>()
    if(delta.fresh) {
        beats += WorldBeat(0L, WorldBeatKind.SPAWN, monster, element = element)
        beats += WorldBeat(160L, WorldBeatKind.BANNER, monster, text = monsterName, crit = boss)
        // Resuming a battle the server already settled: show where it ended rather than replay it, and
        // do not drop loot that reached the stash before the app was reopened.
        if(delta.status != BattleStatus.ACTIVE) {
            if(delta.monsterKilled) beats += WorldBeat(420L, WorldBeatKind.DEATH, monster)
            if(delta.heroKilled) beats += WorldBeat(420L, WorldBeatKind.DEATH, hero)
            beats += WorldBeat(900L, WorldBeatKind.BANNER, hero, text = outcome(delta.status),
                crit = delta.status == BattleStatus.VICTORY && boss)
        }
        return beats
    }
    val swung = delta.action == BattleAction.ATTACK || delta.action == BattleAction.POWER
    when(delta.action) {
        BattleAction.ATTACK, BattleAction.POWER -> {
            beats += WorldBeat(WorldTiming.HERO_WINDUP, WorldBeatKind.WINDUP, hero, monster)
            beats += WorldBeat(WorldTiming.HERO_STRIKE,
                if(delta.action == BattleAction.POWER) WorldBeatKind.CAST else WorldBeatKind.STRIKE, hero, monster)
            beats += WorldBeat(WorldTiming.HERO_IMPACT,
                if(delta.heroMissed) WorldBeatKind.MISS else WorldBeatKind.IMPACT,
                hero, monster, delta.heroDamage, element, delta.heroCrit)
        }
        BattleAction.GUARD -> beats += WorldBeat(0L, WorldBeatKind.GUARD, hero, text = tr("Защита", "Guard"))
        BattleAction.POTION -> beats += WorldBeat(0L, WorldBeatKind.QUAFF, hero, amount = delta.heroHealed)
        BattleAction.FLEE -> {
            beats += WorldBeat(0L, WorldBeatKind.RETREAT, hero, text = tr("Отступление", "Retreat"))
            beats += WorldBeat(WorldTiming.AFTERMATH, WorldBeatKind.BANNER, hero, text = tr("Отступление", "Retreat"))
            return beats
        }
        null -> Unit
    }
    if(delta.manaSpent > 0.0) beats += WorldBeat(WorldTiming.HERO_STRIKE, WorldBeatKind.MANA, hero, amount = -delta.manaSpent)
    if(delta.monsterKilled) beats += WorldBeat(WorldTiming.HERO_IMPACT + 110L, WorldBeatKind.DEATH, monster)
    else {
        val windup = if(swung) WorldTiming.MOB_WINDUP else WorldTiming.DEFENSIVE_WINDUP
        val strike = if(swung) WorldTiming.MOB_STRIKE else WorldTiming.DEFENSIVE_STRIKE
        val impact = if(swung) WorldTiming.MOB_IMPACT else WorldTiming.DEFENSIVE_IMPACT
        if(delta.monsterDamage > 0.0 || delta.monsterMissed) {
            beats += WorldBeat(windup, WorldBeatKind.WINDUP, monster, hero)
            beats += WorldBeat(strike, WorldBeatKind.STRIKE, monster, hero)
            beats += WorldBeat(impact, if(delta.monsterDamage > 0.0) WorldBeatKind.IMPACT else WorldBeatKind.MISS,
                monster, hero, delta.monsterDamage, element, delta.monsterCrit)
        }
        if(delta.shieldAbsorbed > 0.0) beats += WorldBeat(impact + 70L, WorldBeatKind.ABSORB, hero, amount = delta.shieldAbsorbed)
        if(delta.heroKilled) beats += WorldBeat(impact + 110L, WorldBeatKind.DEATH, hero)
    }
    if(delta.manaGained > 0.0) beats += WorldBeat(WorldTiming.AFTERMATH, WorldBeatKind.MANA, hero, amount = delta.manaGained)
    // Regeneration is whatever the hero gained once the flask this turn is accounted for.
    if(delta.potionsSpent == 0 && delta.heroHealed > 0.0)
        beats += WorldBeat(WorldTiming.AFTERMATH, WorldBeatKind.REGEN, hero, amount = delta.heroHealed)
    delta.rewards.forEachIndexed { index, reward ->
        beats += WorldBeat(WorldTiming.AFTERMATH + index * WorldTiming.DROP_STRIDE, WorldBeatKind.DROP, monster,
            amount = reward.amount.toDouble(), text = reward.name, loot = lootKind(reward),
            icon = art.forReward(reward), rarity = art.rarityOf(reward), reward = reward)
    }
    if(delta.status != BattleStatus.ACTIVE) beats += WorldBeat(
        WorldTiming.AFTERMATH + delta.rewards.size * WorldTiming.DROP_STRIDE + 240L, WorldBeatKind.BANNER, hero,
        text = outcome(delta.status), crit = delta.status == BattleStatus.VICTORY && boss)
    return beats.sortedBy { it.at }
}

private fun outcome(status: BattleStatus) = when(status) {
    BattleStatus.VICTORY -> tr("Победа", "Victory")
    BattleStatus.DEFEAT -> tr("Поражение", "Defeat")
    else -> tr("Отступление", "Retreat")
}
