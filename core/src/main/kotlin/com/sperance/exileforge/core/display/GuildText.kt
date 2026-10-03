package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.model.guild.GuildLogEntry
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.GuildRole

/**
 * The words of the guilds (3.22.0). The server's dictionary names them all — factions, ranks, arms, modes, roles and the
 * journal's lines; the client's own table stands in for the modes and roles until it has arrived.
 */
/** What a contribution names gold by, where every other thing given is an orb's item code. */
const val GUILD_GOLD = "GOLD"

object GuildText {
    /** A faction's name and word (3.28.0): the server's dictionary first, the app's own table for the three it ships with, then the code. */
    fun faction(code: String): String = locOr("guild.faction.$code.name", uiOr(uiLanguage, "guild.faction.$code.name", displayName(code)))
    fun factionLore(code: String): String = locOr("guild.faction.$code.description", uiOr(uiLanguage, "guild.faction.$code.description", ""))
    fun rank(code: String): String = locOr("guild.rank.$code", displayName(code))
    fun emblem(code: String): String = locOr("guild.emblem.$code", displayName(code))
    fun mode(mode: GuildMode): String = locOr("guild.mode.${mode.name}", ui("guild.mode.${mode.name}"))
    fun role(role: GuildRole): String = locOr("guild.role.${role.name}", ui("guild.role.${role.name}"))

    /** `Name [TAG]`, the way a guild is named everywhere. */
    fun title(name: String, tag: String): String = if (tag.isBlank()) name else "$name [$tag]"

    /** One journal line: `{0}` the hero, `{1}` the figure — a contribution's amount and what it was, a rank, a level, a name. */
    fun log(entry: GuildLogEntry): String {
        val template = locOr("guild.log.${entry.kind}", "{0} · ${displayName(entry.kind)} {1}")
        return template.replace("{0}", entry.heroName).replace("{1}", logValue(entry)).trim()
    }

    private fun logValue(entry: GuildLogEntry): String = when (entry.kind) {
        "CONTRIBUTED" -> contribution(entry.value.substringBefore(' '), entry.value.substringAfter(' ', ""))

        "RANK_UP" -> rank(entry.value)

        // 3.79.0: a tree node by its name; a stash line is a stack (`<amount> <code>`) or an item's template.
        "TREE_NODE" -> locOr("guild.node.${entry.value}.name", displayName(entry.value))

        "STASH_IN", "STASH_OUT" -> if (' ' in entry.value) contribution(entry.value.substringBefore(' '), entry.value.substringAfter(' ')) else equipmentTitle(entry.value)

        else -> entry.value
    }

    /** «1 500 золота», «3 Сфера хаоса»: what was given, as the journal keeps it — `<amount> <GOLD|orb code>`. */
    private fun contribution(amount: String, item: String): String {
        val figure = amount.toDoubleOrNull()?.let { number(it) } ?: amount
        val what = when (item) {
            "" -> ""
            GUILD_GOLD -> ui("guild.gold").lowercase()
            else -> itemTitle(item)
        }
        return "$figure $what".trim()
    }
}
