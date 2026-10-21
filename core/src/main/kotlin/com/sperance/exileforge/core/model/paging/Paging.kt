package com.sperance.exileforge.core.model.paging

/**
 * Строки прочитанных страниц серверного списка и следующая страница [next] под ними: строка, что пришла второй раз на стыке страниц
 * (список сдвинулся между чтениями), остаётся одна - по ключу [key].
 */
fun <T, K> List<T>.followedBy(next: List<T>, key: (T) -> K): List<T> {
    val shown = mapTo(HashSet(), key)
    return this + next.filterNot { key(it) in shown }
}
