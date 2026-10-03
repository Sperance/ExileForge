package com.sperance.exileforge.core.world

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.rules.content.ContentIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Мир сервера на устройстве: контент, прочитанный по кускам, и статичные файлы - словарь, иконки, портреты. */
data class World(
    /** Контент - все таблицы игры, разобранные правилами; null, пока куски не прочитаны. */
    val content: ContentIndex? = null,
    /** Отпечаток контента на экране: сменившийся мир замечен, неизменный не качается. */
    val contentHash: String = "",
    /** Словарь сервера для текущего языка и сколько строк в нём; сам словарь глобален. */
    val localeLanguage: String = "",
    val localeStrings: Int = 0,
    /** Какие языки игрок может выбрать: решает манифест сервера, таблицы клиента сужают. */
    val languages: List<Lang> = listOf(Lang.RU, Lang.EN),
    val iconKeys: Int = 0,
    val iconSprites: Int = 0,
    val portraits: Int = 0,
)

/** Единственный источник правды о мире сервера (3.80.7). */
class WorldRepository {
    private val mutable = MutableStateFlow(World())
    val state: StateFlow<World> = mutable

    fun update(transform: (World) -> World) = mutable.update(transform)
}
