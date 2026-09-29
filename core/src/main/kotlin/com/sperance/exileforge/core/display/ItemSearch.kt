package com.sperance.exileforge.core.display

/**
 * The stash's search: a query finds an item by its name in the current language and by its English
 * trade name alike, whatever the case — «кинжал», «КИНЖАЛ» and «dagger» find the same dagger. Since 3.30.0 it
 * reads the copy's modifier lines too, as the current language words them: «сопротивление» finds every resist.
 */
object ItemSearch {
    fun matches(item: ItemView, query: String): Boolean {
        val wanted = fold(query)
        if (wanted.isEmpty()) return true
        return listOfNotNull(item.title, item.trade, displayName(item.code)).any { fold(it).contains(wanted) }
            || item.lines.any { fold(it.text).contains(wanted) }
    }

    /** The same for a stacking item of the bag, by its code. */
    fun matches(code: String, query: String): Boolean {
        val wanted = fold(query)
        if (wanted.isEmpty()) return true
        return listOfNotNull(itemTitle(code), tradeName(code, equipment = false), displayName(code)).any { fold(it).contains(wanted) }
    }

    internal fun fold(text: String): String = text.trim().lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ")
}
