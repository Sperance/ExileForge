package com.sperance.exileforge.core.i18n

/** Строка, читаемая на языке игрока в момент показа, а не создания (3.79.0): смена языка перечитывает её. */
fun interface Phrase {
    fun read(): String
}

/** [Phrase] словаря: [key] с [args], ищется заново при каждом чтении. */
fun phrase(key: String, vararg args: Any?): Phrase = Phrase { ui(key, *args) }
