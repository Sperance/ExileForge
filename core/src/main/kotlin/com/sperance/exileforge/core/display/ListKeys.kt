package com.sperance.exileforge.core.display

/**
 * Ключи строк ленивого списка по содержимому: у строк без своего номера - их [key], повтор того же - с порядковым
 * `#2`, `#3`. Строка, сдвинутая новой сверху, остаётся при своём ключе и не перекомпонуется; повторы не роняют список.
 */
fun <T> contentKeys(items: List<T>, key: (T) -> String): List<String> {
    val seen = HashMap<String, Int>()
    return items.map { item ->
        val base = key(item)
        val n = seen.merge(base, 1, Int::plus) ?: 1
        if (n == 1) base else "$base#$n"
    }
}
