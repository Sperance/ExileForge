package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.model.guild.GuildLogEntry
import com.sperance.exileforge.core.model.guild.GuildStashTab
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.GuildRole

/**
 * The words of the guilds. The server's dictionary names them all — factions, ranks, arms, modes, roles and the
 * journal's lines; the client's own table stands in for the modes and roles until it has arrived.
 */
/** What a contribution names gold by, where every other thing given is an orb's item code. */
const val GUILD_GOLD = "GOLD"

object GuildText {
    /**
     * A faction's name and word: the server's dictionary first, the app's own table for the three it ships with; нет ни там,
     * ни там - заглушка и дыра словаря, как у звания, герба и узла древа.
     */
    fun faction(code: String): String = locOr("guild.faction.$code.name", uiOr(uiLanguage, "guild.faction.$code.name") { loc("guild.faction.$code.name") })
    fun factionLore(code: String): String = locOr("guild.faction.$code.description", uiOr(uiLanguage, "guild.faction.$code.description", ""))
    fun rank(code: String): String = loc("guild.rank.$code")
    fun emblem(code: String): String = loc("guild.emblem.$code")
    fun mode(mode: GuildMode): String = locOr("guild.mode.${mode.name}", ui("guild.mode.${mode.name}"))
    fun role(role: GuildRole): String = locOr("guild.role.${role.name}", ui("guild.role.${role.name}"))

    /**
     * Подпись кнопки приглашения: «Пригласить «Имя»» - имя целиком, если в нём не больше [INVITE_NAME] знаков; длинное -
     * до первого пробела и не длиннее [INVITE_NAME] знаков, с «…». Знак - кодовая точка, суррогатная пара не режется.
     * Героя нет - просто «Пригласить».
     */
    fun invite(name: String?): String {
        if (name == null) return ui("guild.invite")
        if (name.codePointCount(0, name.length) <= INVITE_NAME) return ui("guild.invite_named", name)
        val word = name.substringBefore(' ')
        val head = word.substring(0, word.offsetByCodePoints(0, minOf(INVITE_NAME, word.codePointCount(0, word.length))))
        return ui("guild.invite_named", "$head…")
    }

    /** Наибольшая длина имени в подписи кнопки приглашения, в кодовых точках. */
    private const val INVITE_NAME = 14

    /** Имя вкладки хранилища [at] (с нуля): данное главой или «Вкладка N». */
    fun stashTab(tab: GuildStashTab?, at: Int): String = tab?.name?.takeIf { it.isNotBlank() } ?: ui("guild.stash_tab", at + 1)

    /** `Name [TAG]`, the way a guild is named everywhere. */
    fun title(name: String, tag: String): String = if (tag.isBlank()) name else "$name [$tag]"

    /**
     * One journal line: `{0}` the hero, `{1}` the figure — a contribution's amount and what it was, a rank, a level, a name.
     *
     * Нет шаблона - герой и число остаются, на месте действия - заглушка и дыра словаря, не код.
     */
    fun log(entry: GuildLogEntry): String {
        val key = "guild.log.${entry.kind}"
        val template = locOr(key, "").ifBlank { "{0} · ${loc(key)} {1}" }
        return template.replace("{0}", entry.heroName).replace("{1}", logValue(entry)).trim()
    }

    /** Узел древа - по имени; строка хранилища - стопка (`<amount> <code>`) или шаблон вещи. */
    private fun logValue(entry: GuildLogEntry): String = when (entry.kind) {
        "CONTRIBUTED" -> contribution(entry.value.substringBefore(' '), entry.value.substringAfter(' ', ""))
        "RANK_UP" -> rank(entry.value)
        "TREE_NODE" -> loc("guild.node.${entry.value}.name")
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
