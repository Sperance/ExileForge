package com.sperance.exileforge.core.i18n

import com.sperance.exileforge.core.contract.WireJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * One language in the server's manifest.
 *
 * [hash] is the dictionary's fingerprint: a bundle is downloaded again only when it differs from
 * the one already stored. [label] is always written in the language it names, so a picker reads
 * correctly before anything has been chosen.
 */
@Serializable data class LocaleLanguage(val code: String, val label: String, val hash: String)

/** `locale/index.json`: which languages exist and which to offer before the player chooses. */
@Serializable data class LocaleManifest(val default: String = "en", val languages: List<LocaleLanguage> = emptyList()) {
    fun language(code: String): LocaleLanguage? = languages.firstOrNull { it.code == code }
}

/**
 * The server's dictionary for one language — a flat table of key to string.
 *
 * Since 0.14.0 no document in Mongo carries text: an entity stores a code and the string lives
 * here, under a key built from the entity's section and that code. [LocaleKey] builds those keys
 * the same way the server does, so the two cannot drift.
 *
 * A missing key returns itself. That is deliberate: a hole shows up immediately instead of being
 * papered over by a silent fall back to another language.
 */
class LocaleBundle(val language: String = "", val hash: String = "", private val strings: Map<String, String> = emptyMap()) {
    val size: Int get() = strings.size
    val isEmpty: Boolean get() = strings.isEmpty()
    fun contains(key: String): Boolean = key in strings
    operator fun get(key: String): String = strings[key] ?: key

    /**
     * A string with its numbered placeholders filled.
     *
     * Every argument is resolved through the bundle first, because the server passes locale keys as
     * arguments — an orb's message names the item by `equipment.<code>.name` rather than by text.
     * An argument that is not a key comes back unchanged, so a plain number stays a number.
     */
    fun format(key: String, args: List<String>): String =
        args.foldIndexed(get(key)) { index, text, argument -> text.replace("{$index}", get(argument)) }

    /** Codes of one section whose name contains [text]; the catalogue narrows itself with this. */
    fun codesMatching(section: String, text: String): Set<String> {
        val needle = text.trim()
        if (needle.isEmpty()) return emptySet()
        val prefix = "$section."
        val suffix = ".${LocaleKey.NAME}"
        return strings.asSequence()
            .filter { it.key.startsWith(prefix) && it.key.endsWith(suffix) && it.value.contains(needle, true) }
            .mapTo(mutableSetOf()) { it.key.removePrefix(prefix).removeSuffix(suffix) }
    }

    companion object {
        /**
         * A dictionary read from the file the server serves.
         *
         * The document is kept as text on the way in so it can be stored verbatim and parsed again
         * on the next start: the manifest's [hash] is what decides whether it is still current.
         */
        fun parse(language: String, hash: String, document: String): LocaleBundle = LocaleBundle(
            language, hash, WireJson.parseToJsonElement(document).jsonObject
                .mapValues { (_, value) -> (value as? JsonPrimitive)?.contentOrNull.orEmpty() })
    }
}

/**
 * How a key is built from an entity's section and its code.
 *
 * This mirrors the server's own `LocaleKey`: both sides must compute the same string or a lookup
 * silently returns the key instead of a name.
 *
 * Only what the server *seeds* is looked up here. The bundle also carries an `enum.` section, but
 * the client keeps its own tables for slots, rarities and stats: those take an explicit language,
 * and a dictionary holds one language at a time, so reading them from it would answer a request for
 * English in Russian. Names of things the client cannot know come from the server; labels it wrote
 * itself stay its own.
 */
object LocaleKey {
    const val EQUIPMENT = "equipment"
    const val ITEM = "item"
    const val MODIFIER = "modifier"
    const val SKILL_NODE = "skilltree"
    const val CHARACTER_CLASS = "class"
    const val ERROR = "error"
    const val NAME = "name"
    const val DESCRIPTION = "description"

    fun equipmentName(code: String) = key(EQUIPMENT, code, NAME)
    fun equipmentDescription(code: String) = key(EQUIPMENT, code, DESCRIPTION)
    fun itemName(code: String) = key(ITEM, code, NAME)
    fun itemDescription(code: String) = key(ITEM, code, DESCRIPTION)
    fun modifierName(code: String) = key(MODIFIER, code, NAME)
    fun skillNodeName(code: String) = key(SKILL_NODE, code, NAME)
    fun skillNodeDescription(code: String) = key(SKILL_NODE, code, DESCRIPTION)
    fun className(code: String) = key(CHARACTER_CLASS, code, NAME)
    fun classDescription(code: String) = key(CHARACTER_CLASS, code, DESCRIPTION)
    fun error(code: String) = "$ERROR.$code"
    private fun key(section: String, code: String, field: String) = "$section.$code.$field"
}

/**
 * The dictionary the app is currently showing.
 *
 * It sits beside [uiLanguage] rather than inside it: the two answer different questions. The client
 * owns its own labels through [tr]; the server owns every name of a thing in the game, and those
 * arrive here. Both are switched together when the player picks a language.
 */
@Volatile var serverLocale: LocaleBundle = LocaleBundle()

/** A server string by key. Without a dictionary, or without the key, the key itself comes back. */
fun loc(key: String): String = serverLocale[key]

/** A server string with its arguments filled; the arguments are themselves keys. */
fun loc(key: String, args: List<String>): String = serverLocale.format(key, args)

/**
 * A server string, or a fallback when the dictionary has no such key.
 *
 * Used where the client would otherwise print a raw key at the player: an entity whose code is not
 * in the dictionary is better shown by its code than by `equipment.FOO.name`.
 */
fun locOr(key: String, fallback: String): String = if (serverLocale.contains(key)) serverLocale[key] else fallback

/**
 * A server refusal in the player's language.
 *
 * The server keeps its error text under `error.<code>` too, but the error envelope carries only the
 * finished sentence and the code — never the arguments that filled the template. So a template with
 * holes in it cannot be rebuilt here and the server's own sentence stands; a template without holes
 * is already a whole sentence and is shown in the chosen language.
 */
fun locError(code: String?, message: String): String {
    if (code.isNullOrBlank()) return message
    val key = LocaleKey.error(code)
    if (!serverLocale.contains(key)) return message
    val text = serverLocale[key]
    return if (text.contains("{0}")) message else text
}
