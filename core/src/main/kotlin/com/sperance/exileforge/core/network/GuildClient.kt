package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildContribution
import com.sperance.exileforge.core.model.guild.GuildLogEntry
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildPage
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.rules.content.GuildMode

private const val GUILD = "api/v1/guild"

/** The member commands, each about one other hero of the guild by `memberId`. */
enum class MemberCommand(val path: String) { KICK("kick"), PROMOTE("promote"), DEMOTE("demote"), TRANSFER("transfer") }

/**
 * Guilds (3.22.0, server 1.20.0). Every rule is the server's; the client names a guild or a member and prints the refusal.
 *
 * Every command is a POST on the hero and never retried — a repeat would be refused at best and paid twice at worst.
 * One that moves the hero in or out of a guild answers [GuildMine], one inside a guild the [GuildView] after it;
 * both bring the hero's snapshot along, as every command on the hero does.
 */
class GuildClient internal constructor(private val http: Transport) {
    suspend fun mine(heroId: String): GuildMine = http.get("$GUILD/mine", heroQuery(heroId))

    /** The guilds by name or tag; a blank [faction] lists every faction's, a code only that faction's (3.28.0, server 1.26.0). */
    suspend fun search(heroId: String, text: String, page: Int, faction: String = ""): GuildPage {
        requirePage(page)
        return http.get("$GUILD/search", heroQuery(heroId, "text" to text.trim().takeIf { it.isNotEmpty() },
            "faction" to faction.takeIf { it.isNotBlank() }, "page" to page.toString()))
    }

    suspend fun create(heroId: String, name: String, tag: String, faction: String, emblem: String, color: String, mode: GuildMode, minLevel: Int): GuildMine {
        require(name.isNotBlank() && tag.isNotBlank()) { ui("guild.api.name_tag") }
        require(faction.isNotBlank()) { ui("guild.api.faction") }
        return http.post("$GUILD/create", heroQuery(heroId, "name" to name.trim(), "tag" to tag.trim(), "faction" to faction, "emblem" to emblem, "color" to color,
            "mode" to mode.name, "minLevel" to minLevel.toString()))
    }

    suspend fun join(heroId: String, guildId: String): GuildMine = onGuild("join", heroId, guildId)
    suspend fun apply(heroId: String, guildId: String): GuildMine = onGuild("apply", heroId, guildId)
    suspend fun acceptInvite(heroId: String, guildId: String): GuildMine = onGuild("invites/accept", heroId, guildId)
    suspend fun declineInvite(heroId: String, guildId: String): GuildMine = onGuild("invites/decline", heroId, guildId)

    suspend fun acceptApplicant(heroId: String, applicantId: String): GuildView = onApplicant("accept", heroId, applicantId)
    suspend fun declineApplicant(heroId: String, applicantId: String): GuildView = onApplicant("decline", heroId, applicantId)

    suspend fun invite(heroId: String, name: String): GuildView {
        require(name.isNotBlank()) { ui("guild.api.hero_name") }
        return http.post("$GUILD/invite", heroQuery(heroId, "name" to name.trim()))
    }

    suspend fun member(heroId: String, command: MemberCommand, memberId: String): GuildView {
        requireId(memberId)
        return http.post("$GUILD/${command.path}", heroQuery(heroId, "memberId" to memberId))
    }

    suspend fun leave(heroId: String): GuildMine = http.post("$GUILD/leave", heroQuery(heroId))
    suspend fun disband(heroId: String): GuildMine = http.post("$GUILD/disband", heroQuery(heroId))

    /** Every setting is sent, the announcement as a parameter — an empty one clears it. */
    suspend fun settings(heroId: String, mode: GuildMode, minLevel: Int, emblem: String, color: String, announcement: String): GuildView =
        http.post("$GUILD/settings", heroQuery(heroId, "mode" to mode.name, "minLevel" to minLevel.toString(), "emblem" to emblem,
            "color" to color, "announcement" to announcement))

    /** Gold or an orb into the treasury; [item] is `GOLD` or the orb's item code. */
    suspend fun contribute(heroId: String, item: String, amount: Long): GuildContribution {
        require(item.isNotBlank()) { ui("api.choose_item") }
        require(amount > 0) { ui("api.amount_positive") }
        return http.post("$GUILD/contribute", heroQuery(heroId, "item" to item, "amount" to amount.toString()))
    }

    suspend fun log(heroId: String, page: Int): List<GuildLogEntry> {
        requirePage(page)
        return http.get("$GUILD/log", heroQuery(heroId, "page" to page.toString()))
    }

    private suspend fun onGuild(operation: String, heroId: String, guildId: String): GuildMine {
        requireId(guildId)
        return http.post("$GUILD/$operation", heroQuery(heroId, "guildId" to guildId))
    }

    private suspend fun onApplicant(operation: String, heroId: String, applicantId: String): GuildView {
        requireId(applicantId)
        return http.post("$GUILD/applications/$operation", heroQuery(heroId, "applicantId" to applicantId))
    }
}
