package com.sperance.exileforge.core.i18n

import kotlin.random.Random

/**
 * Строки окна загрузки: подсказки игры и лор мира - ключи словаря клиента с [PREFIXES] (`launch.tip.*`, `launch.lore.*`), во всех
 * языках. Строка берётся наугад и никогда не повторяет прошлую ([next]): какую показали, помнит устройство.
 */
object LaunchLines {
    val PREFIXES = listOf("launch.tip.", "launch.lore.")

    /** Ключи строк окна загрузки словаря языка [lang] по порядку. */
    fun keys(lang: Lang): List<String> = UiStrings.keys(lang).filter { key -> PREFIXES.any(key::startsWith) }.sorted()

    /** Следующая строка из [keys]: наугад, не [last] (если строк больше одной); строк нет - null. */
    fun next(keys: List<String>, last: String, random: Random = Random.Default): String? {
        val pool = keys.filter { it != last }.ifEmpty { keys }
        return pool.takeIf { it.isNotEmpty() }?.random(random)
    }
}
