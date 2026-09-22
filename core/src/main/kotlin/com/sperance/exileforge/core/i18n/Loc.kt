package com.sperance.exileforge.core.i18n

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Interface language.
 *
 * The list here is what the client *can* speak. Which of them a player may pick is decided by the
 * server's own manifest, because every name of a thing in the game lives there: an interface in a
 * language the server cannot name items in would be half a translation.
 *
 * [short] is the rune drawn in a picker, [title] the language's name written in itself.
 */
enum class Lang(val code: String, val title: String, val short: String) {
    RU("ru", "Русский", "RU"), EN("en", "English", "EN"), ZH("zh", "简体中文", "中");
    companion object {
        /** The language with this code, or nothing: used where an unknown code must not become one. */
        fun byCode(code: String?) = entries.firstOrNull { it.code == code }
        fun of(code: String?) = byCode(code) ?: RU
    }
}

/**
 * Language used outside Compose: validation messages, view-model snackbars, wire-contract checks.
 * Compose reads LocalLang instead, so both are updated together when the player switches language.
 */
@Volatile var uiLanguage: Lang = Lang.RU

/**
 * The client's own labels, one flat table per language.
 *
 * They are a resource of `:core` rather than a download: a label is needed before a server has been
 * reached at all - on the sign-in screen, in a validation message, in a failure that says why the
 * connection did not happen. The server owns the names of things (see [loc]); this owns everything
 * the client wrote itself.
 *
 * A missing key returns itself, exactly as [LocaleBundle] does, so a hole shows up on the screen
 * instead of being papered over by another language.
 */
object UiStrings {
    private val json = Json { ignoreUnknownKeys = true }
    private val tables = HashMap<Lang, Map<String, String>>()

    /** The whole table for one language; read from the jar once and kept. */
    @Synchronized fun table(lang: Lang): Map<String, String> = tables.getOrPut(lang) { read(lang) }

    /** Every key the table holds - what the completeness test walks. */
    fun keys(lang: Lang): Set<String> = table(lang).keys

    private fun read(lang: Lang): Map<String, String> =
        javaClass.classLoader?.getResourceAsStream("i18n/ui_${lang.code}.json")
            ?.bufferedReader()?.use { it.readText() }
            ?.let { json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), it) }
            ?: emptyMap()
}

/**
 * A label in the language the app is showing, with its numbered holes filled.
 *
 * `ui("hero.unequip")` and `ui("tree.points", taken, total)` are the only two shapes. A template
 * keeps its `{0}`, `{1}` in every language, so a sentence is never assembled by appending a number
 * to a label - word order differs, and in Chinese it differs the most.
 */
fun ui(key: String, vararg args: Any?): String = ui(uiLanguage, key, *args)

/** The same label in a language named explicitly: tables of slots, rarities and stats need this. */
fun ui(lang: Lang, key: String, vararg args: Any?): String =
    args.foldIndexed(UiStrings.table(lang)[key] ?: key) { index, text, argument ->
        text.replace("{$index}", argument.toString())
    }

/**
 * A label, or a fallback when the table has no such key.
 *
 * Used where a key the client cannot know might be asked for: a stat or a slot the server added
 * since this build, which is better shown as a humanised code than as `enum.slot.FOO`.
 */
fun uiOr(lang: Lang, key: String, fallback: String): String = UiStrings.table(lang)[key] ?: fallback

/**
 * The key of a counted noun.
 *
 * Russian counts its nouns in three forms, English in two and Chinese in none, so the rule belongs
 * to the language rather than to the screen that needed it. Every form exists in every table - in
 * Chinese they are simply the same word.
 */
fun pluralKey(key: String, n: Int, lang: Lang = uiLanguage): String = when (lang) {
    Lang.RU -> when {
        n % 10 == 1 && n % 100 != 11 -> "$key.one"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "$key.few"
        else -> "$key.many"
    }
    Lang.EN -> if (n == 1) "$key.one" else "$key.many"
    Lang.ZH -> "$key.one"
}

/** A counted noun in the current language: `plural("tree.node", 3)`. */
fun plural(key: String, n: Int, lang: Lang = uiLanguage): String = ui(lang, pluralKey(key, n, lang))
