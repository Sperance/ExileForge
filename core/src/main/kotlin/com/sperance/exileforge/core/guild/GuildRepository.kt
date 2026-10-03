package com.sperance.exileforge.core.guild

import com.sperance.exileforge.core.model.guild.GuildLogEntry
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildPage
import com.sperance.exileforge.core.model.guild.GuildStashView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Разделы гильдии, каждый - плитка её зала; [APPLICATIONS] только тем, кто вправе отвечать на заявки. */
enum class GuildTab { MEMBERS, QUESTS, TREE, STASH, APPLICATIONS, CONTRIBUTE, LOG, SETTINGS }

/** Гильдия героя, как сервер ответил последним: своя картина, поиск, журнал, хранилище. */
data class Guilds(
    val mine: GuildMine? = null,
    val query: String = "",
    val search: GuildPage = GuildPage(),
    /** Фракция, до которой сужен список (3.28.0); пусто - все. */
    val faction: String = "",
    /** Раздел, открытый над залом гильдии; null - сам зал. */
    val tab: GuildTab? = null,
    val log: List<GuildLogEntry> = emptyList(),
    val logPage: Int = 0,
    val logEnd: Boolean = false,
    /** Хранилище гильдии (3.79.0), читается при входе в раздел. */
    val stash: GuildStashView? = null,
)

/** Единственный источник правды о гильдии (3.80.13). */
class GuildRepository {
    private val mutable = MutableStateFlow(Guilds())
    val state: StateFlow<Guilds> = mutable

    fun update(transform: (Guilds) -> Guilds) = mutable.update(transform)

    fun clear() = update { Guilds() }
}
