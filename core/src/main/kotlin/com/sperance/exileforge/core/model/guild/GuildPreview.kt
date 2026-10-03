package com.sperance.exileforge.core.model.guild

import com.sperance.exileforge.rules.content.GuildRank
import com.sperance.exileforge.rules.content.GuildRules

/*
 * What the screens read off `guilds.json` beyond what the server computes with it (3.22.0): the form's lengths, the
 * level bar and the way to the next rank.
 */

val GuildRules.nameLength: IntRange get() = (name.getOrNull(0) ?: 3)..(name.getOrNull(1) ?: 24)
val GuildRules.tagLength: IntRange get() = (tag.getOrNull(0) ?: 2)..(tag.getOrNull(1) ?: 4)

/** How far a guild of [level] with [experience] is through its level, 0..1; full at the top. */
fun GuildRules.levelProgress(level: Int, experience: Long): Float {
    val next = next(level).takeIf { it > 0 } ?: return 1f
    val start = levels.getOrNull(level - 1) ?: 0
    return if (next <= start) 1f else ((experience - start).toFloat() / (next - start)).coerceIn(0f, 1f)
}

/** The rank [contribution] has not reached yet, none past the last. */
fun GuildRules.nextRank(contribution: Long): GuildRank? = ranks.firstOrNull { contribution < it.from }
