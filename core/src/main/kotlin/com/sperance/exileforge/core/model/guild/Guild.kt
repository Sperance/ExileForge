package com.sperance.exileforge.core.model.guild

import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.GuildRole
import kotlinx.serialization.Serializable

/*
 * The guild routes' answers (3.22.0, server 1.20.0). The rules — `guilds.json`, the modes and roles, the patron's bonus
 * on the hero ([com.sperance.exileforge.rules.content.GuildBonus]) — are the shared module's; these are only the wire.
 */

/** Whether a role answers applications and shows members out: the leader and the officers. */
val GuildRole.manages: Boolean get() = this != GuildRole.MEMBER

/** One row of the guild list: enough to choose and to knock. */
@Serializable data class GuildCard(
    val id: String, val name: String = "", val tag: String = "", val emblem: String = "", val color: String = "",
    val patron: String = "", val level: Int = 1, val members: Int = 0, val capacity: Int = 0,
    val mode: GuildMode = GuildMode.OPEN, val minLevel: Int = 1,
) {
    val full: Boolean get() = capacity in 1..members
}

/** A page of the guild list, paged by the server as the auction's showcase is. */
@Serializable data class GuildPage(val items: List<GuildCard> = emptyList(), val page: Int = 0, val totalItems: Long = 0, val totalPages: Int = 0)

/** A member as the guild sees them; [contribution] is all-time in this guild and decides [rank], [lastSeenAt] is epoch millis. */
@Serializable data class GuildMember(
    val heroId: String, val name: String = "", val heroClass: String = "", val level: Int = 1,
    val role: GuildRole = GuildRole.MEMBER, val contribution: Long = 0, val weekContribution: Long = 0,
    val rank: String = "", val joinedAt: Long = 0, val lastSeenAt: Long = 0,
)

/** A hero asking to join an APPLY guild; only the leader and the officers are shown them. */
@Serializable data class GuildApplicant(val heroId: String, val name: String = "", val heroClass: String = "", val level: Int = 1, val at: Long = 0)

/** An invitation waiting for this hero: the guild it comes from as a card, who sent it ([by], a hero's name) and when. */
@Serializable data class GuildInviteView(val guild: GuildCard, val by: String = "", val at: Long = 0)

/**
 * The hero's guild, whole. [experience] is the guild's own and [next] the experience of the next level (0 at the top);
 * the treasury keeps gold and orbs apart, [weekly] is this week's contribution by hero id.
 */
@Serializable data class GuildView(
    val id: String, val name: String = "", val tag: String = "", val emblem: String = "", val color: String = "",
    val patron: String = "", val level: Int = 1, val experience: Long = 0, val next: Long = 0, val capacity: Int = 0,
    val mode: GuildMode = GuildMode.OPEN, val minLevel: Int = 1, val announcement: String = "",
    val treasuryGold: Long = 0, val treasuryOrbs: Map<String, Long> = emptyMap(),
    val members: List<GuildMember> = emptyList(), val applications: List<GuildApplicant> = emptyList(),
    val weekly: Map<String, Long> = emptyMap(),
) {
    val leader: GuildMember? get() = members.firstOrNull { it.role == GuildRole.LEADER }
    val officers: Int get() = members.count { it.role == GuildRole.OFFICER }
    fun member(heroId: String): GuildMember? = members.firstOrNull { it.heroId == heroId }
}

/** `guild/mine`: the guild or none, the hero in it, the invitations waiting, and when a leaver may join again (epoch millis). */
@Serializable data class GuildMine(
    val guild: GuildView? = null, val me: GuildMember? = null,
    val invites: List<GuildInviteView> = emptyList(), val rejoinAt: Long? = null,
)

/** A contribution landed: the guild after it, the hero's row and the gold the hero has left. */
@Serializable data class GuildContribution(val guild: GuildView, val me: GuildMember, val money: Long = 0)

/** One line of the guild's journal: what happened ([kind]), to whom, and the figure it carries. */
@Serializable data class GuildLogEntry(val at: Long = 0, val kind: String = "", val heroName: String = "", val value: String = "")

/** One chat message; [at] is epoch millis and the cursor the next poll asks `after`. */
@Serializable data class GuildMessage(val id: String, val at: Long = 0, val heroId: String = "", val heroName: String = "", val text: String = "")
