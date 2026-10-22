package com.sperance.exileforge.core.model.paging

import kotlinx.serialization.Serializable

/**
 * Страница серверного списка, что дочитывается листанием (сервер `Page`): строки [items] страницы [page], всего строк [total] (null - сервер
 * не считал) и есть ли следующая [more]. У клиента - строки всех прочитанных страниц подряд ([followedBy]).
 */
@Serializable data class Page<T>(val items: List<T> = emptyList(), val page: Int = 0, val total: Long? = null, val more: Boolean = false) {
    /** [next] - следующая страница под этой: строки подряд, строка со стыка страниц - одна по ключу [key]. */
    fun followedBy(next: Page<T>, key: (T) -> Any): Page<T> = next.copy(items = items.followedBy(next.items, key), total = next.total ?: total)

    /** Страница, где строка с повторённым ключом [key] осталась одна - первая: ключ строки списка на экране обязан быть уникален. */
    fun distinctBy(key: (T) -> Any): Page<T> = copy(items = items.distinctBy(key))

    /** Страница без строк, что не проходят [keep], - всего на столько меньше. */
    fun without(keep: (T) -> Boolean): Page<T> {
        val kept = items.filter(keep)
        return copy(items = kept, total = total?.let { (it - (items.size - kept.size)).coerceAtLeast(0) })
    }
}

/**
 * Строки прочитанных страниц серверного списка и следующая страница [next] под ними: строка, что пришла второй раз на стыке страниц
 * (список сдвинулся между чтениями) или дважды внутри [next], остаётся одна - первая по ключу [key].
 */
fun <T, K> List<T>.followedBy(next: List<T>, key: (T) -> K): List<T> {
    val shown = mapTo(HashSet(), key)
    return this + next.filter { shown.add(key(it)) }
}
