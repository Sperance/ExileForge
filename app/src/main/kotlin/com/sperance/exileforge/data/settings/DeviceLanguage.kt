package com.sperance.exileforge.data.settings

import com.sperance.exileforge.core.i18n.Lang
import java.util.Locale

/**
 * The language to start in when the player has never chosen one.
 *
 * Only the language subtag is read. Anything the client has no table for falls to English, which
 * is the manifest's own default — and so does a player who had Chinese before 2.13.0 removed it.
 */
fun deviceLanguage(locale: Locale = Locale.getDefault()): Lang = when (locale.language.lowercase()) {
    "ru" -> Lang.RU
    else -> Lang.EN
}
