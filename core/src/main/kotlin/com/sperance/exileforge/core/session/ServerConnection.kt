package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.network.GameApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Текущий [GameApi] (3.80.9): один на сервер, подменяется при смене сервера; модели экранов берут его отсюда. */
class ServerConnection {
    private val mutable = MutableStateFlow<GameApi?>(null)
    val current: StateFlow<GameApi?> = mutable

    /** Связь с сервером; до первого старта её нет. */
    val api: GameApi get() = mutable.value ?: error("the server connection is not made yet")

    val ready: Boolean get() = mutable.value != null

    fun set(api: GameApi) {
        mutable.value = api
    }
}

/** Ключи чтений: одно чтение на ключ за раз; команда называет чтения, которые делает сама. */
object Reads {
    const val FEEDBACK = "feedback"
    const val MODERATION = "moderation"
    const val DOSSIER = "dossier"
    const val SERVERS = "servers"
    const val NOTICE = "notice"
    const val MAIL = "mail"
    const val HERO = "hero"
    const val CHARACTERS = "characters"
    const val AUCTION = "auction"
    const val LOTS = "lots"
    const val CONTENT = "content"
    const val REDEMPTIONS = "redemptions"
    const val HEALTH = "health"
    const val CRAFTS = "crafts"
    const val GUILD = "guild"
    const val GUILD_SEARCH = "guild_search"
    const val GUILD_LOG = "guild_log"
    const val QUESTS = "quests"
    const val GUILD_QUESTS = "guild_quests"
    const val HALL = "hall"
}

/** Что сообщить сразу: вид выбирает цвет, [at] отличает два одинаковых текста. */
enum class NoticeKind { DONE, LOOT, CRAFT, ATLAS }

data class Notice(val text: String, val kind: NoticeKind = NoticeKind.DONE, val at: Long = System.nanoTime())

/** Тосты (3.80.9): успех, достойный строки; новая заменяет показанную и уходит сама. */
class Notices {
    private val mutable = MutableStateFlow<Notice?>(null)
    val state: StateFlow<Notice?> = mutable

    fun toast(text: String, kind: NoticeKind = NoticeKind.DONE) {
        mutable.value = Notice(text, kind)
    }

    fun dismiss() {
        mutable.value = null
    }
}

/**
 * События игры между моделями (3.80.9): герой изменился на сервере - кто его держит, перечитывает. Снаряжение в
 * заходе не меняется, поэтому поход свой лист не перечитывает.
 */
class GameEvents {
    val heroChanged = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun heroChanged() {
        heroChanged.tryEmit(Unit)
    }
}
