package com.sperance.exileforge.core.model.combat.world

import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.BattleAction
import com.sperance.exileforge.core.model.combat.BattleStatus

/**
 * What the hero does while nobody is pressing anything.
 *
 * It answers with one of the same [BattleAction]s a tap would send: it picks *which* command leaves the
 * client and never what the command is worth. Everything it reads is the server's own snapshot, plus
 * [inRange], which the world contributes and which decides timing alone — the server has no notion of
 * where anyone stands, so standing closer cannot change a single number it rolls.
 */
object AutoPilot {
    /** The heavy strike's price, as the server charges it. */
    const val POWER_MANA = 8.0
    /** Every third swing is the heavy one, so mana is spent at a rhythm the player can read. */
    const val POWER_EVERY = 3
    const val FLASK_BELOW = .35
    const val GUARD_BELOW = .22

    fun choose(battle: Battle, inRange: Boolean, swings: Int): BattleAction? {
        if(battle.status != BattleStatus.ACTIVE) return null
        val ratio = battle.hero.life / battle.hero.maxLife.coerceAtLeast(1.0)
        // A flask can be drunk on the run, so it is decided before the reach of the weapon is.
        if(ratio <= FLASK_BELOW && battle.potions > 0) return BattleAction.POTION
        if(!inRange) return null
        // Nothing left to drink and almost nothing left to lose: brace instead of trading blows.
        if(ratio <= GUARD_BELOW) return BattleAction.GUARD
        return if(battle.hero.mana >= POWER_MANA && swings % POWER_EVERY == POWER_EVERY - 1) BattleAction.POWER
        else BattleAction.ATTACK
    }
}
