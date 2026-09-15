package com.sperance.exileforge.core.i18n

/** Interface language. Russian stays the default so server-independent messages never change silently. */
enum class Lang(val code: String, val title: String, val short: String) {
    RU("ru", "Русский", "RU"), EN("en", "English", "EN");
    companion object { fun of(code: String?) = entries.firstOrNull { it.code == code } ?: RU }
}

fun Lang.pick(ru: String, en: String): String = if (this == Lang.EN) en else ru

/**
 * Language used outside Compose: validation messages, view-model snackbars, wire-contract checks.
 * Compose reads LocalLang instead, so both are updated together when the player switches language.
 */
@Volatile var uiLanguage: Lang = Lang.RU

fun tr(ru: String, en: String): String = uiLanguage.pick(ru, en)
