package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.text.numbered
import java.lang.ref.WeakReference

/**
 * Тексты с числами контента ([numbered]) с памятью по (характеристика, шаблон): подстановка гоняет регулярное выражение по строке, а
 * экран строит одни и те же строки на каждую перекомпоновку. Память одна на приложение, не больше [CAPACITY] строк, и живёт, пока
 * контент тот же: новый контент - новые числа.
 */
internal object NumberedText {
    private const val CAPACITY = 512
    private var index = WeakReference<ContentIndex>(null)
    private val filled = object : LinkedHashMap<Key, String?>(CAPACITY * 2, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, String?>?): Boolean = size > CAPACITY
    }

    private data class Key(val stat: String, val text: String)

    @Synchronized
    fun fill(index: ContentIndex, text: String, stat: String): String? {
        if (this.index.get() !== index) {
            this.index = WeakReference(index)
            filled.clear()
        }
        val key = Key(stat, text)
        if (filled.containsKey(key)) return filled[key]
        return index.numbered(text, stat).also { filled[key] = it }
    }
}

/** [numbered] с памятью ([NumberedText]); без контента - как у правил. */
fun ContentIndex?.numberedText(text: String, stat: String = ""): String? = if (this == null) numbered(text, stat) else NumberedText.fill(this, text, stat)
