package com.sperance.exileforge.data.settings

import com.sperance.exileforge.core.i18n.Lang
import java.util.Locale

/**
 * The language to start in when the player has never chosen one.
 *
 * Only the language subtag is read. Chinese is served in simplified form alone, so every `zh`
 * locale maps to it: a reader of traditional characters is closer to simplified than to English.
 * Anything the client has no table for falls to English, which is the manifest's own default.
 */
fun deviceLanguage(locale: Locale = Locale.getDefault()): Lang = when (locale.language.lowercase()) {
    "ru" -> Lang.RU
    "zh" -> Lang.ZH
    else -> Lang.EN
}
