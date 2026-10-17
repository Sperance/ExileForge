package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildContribution
import com.sperance.exileforge.core.model.guild.GuildLogEntry
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildPage
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildMode

private const val GUILD = "api/v1/guild"

/** Страница журнала гильдии, что клиент просит у сервера; страница короче - конец журнала. */
const val GUILD_LOG_PAGE_SIZE = 20

/**
 * Команда над участником: путь сервера и действие таблицы прав `GuildPolicy`, по которой клиент её показывает.
 *
 * Команды участника - каждая о другом герое гильдии по `memberId`.
 */
enum class MemberCommand(val path: String, val action: GuildAction) {
    PROMOTE("promote", GuildAction.PROMOTE),
    DEMOTE("demote", GuildAction.DEMOTE),
    TRANSFER("transfer", GuildAction.TRANSFER),
    KICK("kick", GuildAction.KICK),
}

/**
 * Guilds. Every rule is the server's; the client names a guild or a member and prints the refusal.
 *
 * Every command is a POST on the hero and never retried — a repeat would be refused at best and paid twice at worst.
 * One that moves the hero in or out of a guild answers [GuildMine], one inside a guild the [GuildView] after it;
 * both bring the hero's snapshot along, as every command on the hero does.
 */
class GuildClient internal constructor(private val http: Transport) {
    /** Гильдия героя с заходом (сервер `POST guild/visit`): отметка захода и обслуживание гильдии - командой, не чтением. */
    suspend fun mine(heroId: String): GuildMine = http.post("$GUILD/visit", heroQuery(heroId))

    /** The guilds by name or tag; a blank [faction] lists every faction's, a code only that faction's. */
    suspend fun search(heroId: String, text: String, page: Int, faction: String = ""): GuildPage {
        requirePage(page)
        return http.get(
            "$GUILD/search",
            heroQuery(
                heroId,
                "text" to text.trim().takeIf { it.isNotEmpty() },
                "faction" to faction.takeIf { it.isNotBlank() },
                "page" to page.toString(),
            ),
        )
    }

    suspend fun create(heroId: String, name: String, tag: String, faction: String, emblem: String, color: String, mode: GuildMode, minLevel: Int): GuildMine {
        require(name.isNotBlank() && tag.isNotBlank()) { ui("guild.api.name_tag") }
        require(faction.isNotBlank()) { ui("guild.api.faction") }
        return http.post(
            "$GUILD/create",
            heroQuery(
                heroId,
                "name" to name.trim(),
                "tag" to tag.trim(),
                "faction" to faction,
                "emblem" to emblem,
                "color" to color,
                "mode" to mode.name,
                "minLevel" to minLevel.toString(),
            ),
        )
    }

    suspend fun join(heroId: String, guildId: String): GuildMine = onGuild("join", heroId, guildId)
    suspend fun apply(heroId: String, guildId: String): GuildMine = onGuild("apply", heroId, guildId)
    suspend fun acceptInvite(heroId: String, guildId: String): GuildMine = onGuild("invites/accept", heroId, guildId)
    suspend fun declineInvite(heroId: String, guildId: String): GuildMine = onGuild("invites/decline", heroId, guildId)

    suspend fun acceptApplicant(heroId: String, applicantId: String): GuildView = onApplicant("accept", heroId, applicantId)
    suspend fun declineApplicant(heroId: String, applicantId: String): GuildView = onApplicant("decline", heroId, applicantId)

    /** Приглашение по номеру героя или имени: сервер сам различит шесть цифр. */
    suspend fun invite(heroId: String, target: String): GuildView {
        require(target.isNotBlank()) { ui("guild.api.hero_name") }
        return http.post("$GUILD/invite", heroQuery(heroId, "name" to target.trim()))
    }

    /** Кого позовёт приглашение: превью до отправки. */
    suspend fun lookup(heroId: String, target: String): com.sperance.exileforge.core.model.guild.GuildRecruit = http.get("$GUILD/lookup", heroQuery(heroId, "name" to target.trim()))

    /** Новый код вступления: прежний перестаёт действовать. */
    suspend fun renewCode(heroId: String): GuildView = http.post("$GUILD/code", heroQuery(heroId))

    /** Вступление по коду гильдии. */
    suspend fun joinByCode(heroId: String, code: String): GuildMine = http.post("$GUILD/join-code", heroQuery(heroId, "code" to code.trim()))

    suspend fun member(heroId: String, command: MemberCommand, memberId: String): GuildView {
        requireId(memberId)
        return http.post("$GUILD/${command.path}", heroQuery(heroId, "memberId" to memberId))
    }

    suspend fun leave(heroId: String): GuildMine = http.post("$GUILD/leave", heroQuery(heroId))
    suspend fun disband(heroId: String): GuildMine = http.post("$GUILD/disband", heroQuery(heroId))

    /** The leader takes one more rank of a tree node, or resets the tree. Never retried. */
    suspend fun takeNode(heroId: String, node: String): GuildMine = http.post("$GUILD/tree/take", heroQuery(heroId, "node" to node))
    suspend fun resetTree(heroId: String): GuildMine = http.post("$GUILD/tree/reset", heroQuery(heroId))

    /** The guild stash: read, put an item or a stack in, take one out, set a tab's rank. Writes never retried. */
    suspend fun stash(heroId: String): com.sperance.exileforge.core.model.guild.GuildStashView = http.get("$GUILD/stash", heroQuery(heroId))
    suspend fun deposit(heroId: String, tab: Int, itemId: String?, code: String?, amount: Long): com.sperance.exileforge.core.model.guild.GuildStashView = http.post("$GUILD/stash/deposit", heroQuery(heroId, "tab" to tab.toString(), "itemId" to itemId, "code" to code, "amount" to amount.toString()))
    suspend fun take(heroId: String, entryId: String): com.sperance.exileforge.core.model.guild.GuildStashView = http.post("$GUILD/stash/take", heroQuery(heroId, "entryId" to entryId))
    suspend fun tabRank(heroId: String, tab: Int, minRank: Int): com.sperance.exileforge.core.model.guild.GuildStashView = http.post("$GUILD/stash/tab", heroQuery(heroId, "tab" to tab.toString(), "minRank" to minRank.toString()))

    /** Every setting is sent, the announcement as a parameter — an empty one clears it. */
    suspend fun settings(heroId: String, mode: GuildMode, minLevel: Int, emblem: String, color: String, announcement: String): GuildView = http.post(
        "$GUILD/settings",
        heroQuery(
            heroId,
            "mode" to mode.name,
            "minLevel" to minLevel.toString(),
            "emblem" to emblem,
            "color" to color,
            "announcement" to announcement,
        ),
    )

    /** Золото в казну (вклад только золотом). */
    suspend fun contribute(heroId: String, amount: Long): GuildContribution {
        require(amount > 0) { ui("api.amount_positive") }
        return http.post("$GUILD/contribute", heroQuery(heroId, "amount" to amount.toString()))
    }

    suspend fun log(heroId: String, page: Int): List<GuildLogEntry> {
        requirePage(page)
        return http.get("$GUILD/log", heroQuery(heroId, "page" to page.toString(), "size" to GUILD_LOG_PAGE_SIZE.toString()))
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
