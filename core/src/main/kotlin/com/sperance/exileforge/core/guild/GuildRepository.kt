package com.sperance.exileforge.core.guild

import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.model.guild.GuildLogEntry
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildStashView
import com.sperance.exileforge.core.model.paging.Page
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.rules.content.GuildRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Разделы гильдии, каждый - плитка её зала. [requires] - право таблицы [GuildPolicy], без которого раздела нет:
 * заявки - тем, кто вправе отвечать на них, настройки - главе; null - раздел открыт всем участникам.
 */
enum class GuildTab(val requires: GuildAction? = null) {
    MEMBERS,
    QUESTS,
    TREE,
    STASH,
    APPLICATIONS(GuildAction.RECRUIT),
    CONTRIBUTE,
    LOG,
    SETTINGS(GuildAction.SETTINGS),
    ;

    /** Виден ли раздел участнику роли [role]; не участнику (null) - только открытые всем. */
    fun openTo(role: GuildRole?): Boolean = requires?.let { action -> role != null && GuildPolicy.can(role, action) } ?: true
}

/** Гильдия героя, как сервер ответил последним: своя картина, поиск, журнал, хранилище. */
data class Guilds(
    val mine: GuildMine? = null,
    val query: String = "",
    val search: Page<GuildCard> = Page(),
    /** Поиск, с которым прочитан показанный список: набранный [query] уходит в список только поиском с клавиатуры. */
    val searched: String = "",
    /** Фракция, до которой сужен список; пусто - все. */
    val faction: String = "",
    /** Раздел, открытый над залом гильдии; null - сам зал. */
    val tab: GuildTab? = null,
    /** Журнал гильдии: прочитанные страницы подряд, новые сверху. */
    val log: Page<GuildLogEntry> = Page(),
    /** Хранилище гильдии, читается при входе в раздел. */
    val stash: GuildStashView? = null,
    /** Превью приглашения: кого позовёт введённый номер или имя; null - не искали или поле сменилось. */
    val recruit: com.sperance.exileforge.core.model.guild.GuildRecruit? = null,
) {
    /**
     * Приглашения, что ждут героя вне гильдии: число на вкладке «Гильдия»; в гильдии или до ответа сервера - ноль. Приходят с входом в
     * Город (`TownActions.visit`), а его делает и прогрев героя - вкладка знает число, не заходя в гильдию.
     */
    val invitesWaiting: Int get() = mine?.takeIf { it.guild == null }?.invites?.size ?: 0
}

/** Единственный источник правды о гильдии. */
class GuildRepository {
    private val mutable = MutableStateFlow(Guilds())
    val state: StateFlow<Guilds> = mutable

    fun update(transform: (Guilds) -> Guilds) = mutable.update(transform)

    fun clear() = update { Guilds() }
}
