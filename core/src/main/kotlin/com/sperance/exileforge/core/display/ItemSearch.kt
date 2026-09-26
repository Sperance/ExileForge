package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import kotlinx.serialization.json.JsonObject

/**
 * The stash's search (2.75.0): a query finds an item by its name in the current language and by
 * its English trade name alike, whatever the case — «кинжал», «КИНЖАЛ» and «dagger» find the same
 * dagger. Both sides are folded by [fold], so «ё» and «е», and runs of spaces, do not matter either.
 */
object ItemSearch {

    fun matches(document: JsonObject, query: String): Boolean {
        val wanted = fold(query)
        if (wanted.isEmpty()) return true
        return names(document).any { fold(it).contains(wanted) }
    }

    /** What an item answers to: its shown name, the template's own, and the English trade name. */
    private fun names(document: JsonObject): List<String> =
        listOfNotNull(document.text("name"), documentTitle(document), documentTrade(document), displayName(document.text("code")))

    internal fun fold(text: String): String =
        text.trim().lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ")
}
