package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.run.Reward

/** How a map run ended, as its summary is headed: the hero fell, walked out through the exit, or left by the portal. */
enum class MapEnd { FELL, CLEARED, LEFT }

/**
 * What one map came to, for the summary shown before the camp: how it ended, the seconds spent on it, the foes and
 * guardians slain, the deaths, everything the server granted for it — gold, experience, stacks and pieces — and the
 * fights' figures. [awaiting] of its rewarding events are not answered yet: the loot is still on its way.
 * A Vaal zone entered from the map is counted into the map once the hero is back ([ZoneShare]).
 */
data class MapTally(
    val end: MapEnd? = null,
    val seconds: Double = 0.0,
    val kills: Int = 0,
    val bosses: Int = 0,
    val deaths: Int = 0,
    val loot: Reward = Reward.NONE,
    val figures: RunSummary = RunSummary(),
    val awaiting: Int = 0,
) {
    val gold: Long get() = loot.gold
    val experience: Double get() = loot.experience
    val receiving: Boolean get() = awaiting > 0
    val empty: Boolean get() = loot.items.isEmpty() && loot.equipment.isEmpty() && loot.recipe == null
}

/**
 * What a Vaal zone hands the map it was entered from: its rewarding events by number — with what the server granted
 * for each, or null while the answer is still on its way — and its own seconds, kills, guardians, deaths and figures.
 */
data class ZoneShare(
    val events: Map<Int, Reward?>,
    val seconds: Double,
    val kills: Int,
    val bosses: Int,
    val deaths: Int,
    val figures: RunSummary,
)
