package com.sperance.exileforge.core.i18n

import java.util.Locale

/**
 * The language to start in when the player has never chosen one.
 *
 * Only the language subtag is read. Anything the client has no table for falls to English, which
 * is the manifest's own default — and so does a player whose saved language the app no longer offers.
 */
fun deviceLanguage(locale: Locale = Locale.getDefault()): Lang = when (locale.language.lowercase()) {
    "ru" -> Lang.RU
    else -> Lang.EN
}
