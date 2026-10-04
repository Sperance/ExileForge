package com.sperance.exileforge.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Связь с сервером (3.30.0): [offline], пока он недоступен, - значок, а не красная полоса, - и команды, отданные без
 * связи, ждущие отправки по порядку. Ничто не рисуется сделанным, пока сервер этого не сказал.
 */
data class Link(
    val offline: Boolean = false,
    val waiting: List<QueuedCommand> = emptyList(),
    /** Почему сервер не отвечает (3.79.0), и слова транспорта для администратора. */
    val outage: Outage? = null,
    val detail: String? = null,
) {
    /** Вещи, о которых ждёт команда: их карточки говорят «ждёт отправки». */
    val waitingItems: Set<String> get() = waiting.mapNotNullTo(HashSet()) { it.itemId }

    /** Связь потеряна из-за [error] (3.79.0): причина для игрока, слова транспорта для администратора. */
    fun down(error: Throwable?): Link = copy(offline = true, outage = error?.let(Outage::of) ?: outage ?: Outage.NO_NETWORK, detail = error?.let(::transportDetail) ?: detail)

    fun up(): Link = copy(offline = false, outage = null, detail = null)
}

/** Единственный источник состояния связи (3.80.32): пишет цикл проверки связи, читают экраны. */
class LinkRepository {
    private val mutable = MutableStateFlow(Link())
    val state: StateFlow<Link> = mutable.asStateFlow()

    fun update(transform: (Link) -> Link) = mutable.update(transform)
}
