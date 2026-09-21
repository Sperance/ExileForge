package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.text
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** `icons/index.json`: the fingerprint of the set, and what it holds. */
@Serializable data class IconManifest(
    val hash: String = "",
    val file: String = "icons.json",
    val sprites: Int = 0,
    val icons: Int = 0,
)

/** One filled outline of a sprite. [alpha] is the only shading there is: a sprite has no colour. */
@Serializable data class IconPath(val d: String = "", val alpha: Float = 1f)

/** One drawing, in a square grid [viewBox] wide. */
@Serializable data class IconSprite(val viewBox: Float = 24f, val paths: List<IconPath> = emptyList())

@Serializable private data class IconDocument(
    val sprites: Map<String, IconSprite> = emptyMap(),
    val icons: Map<String, String> = emptyMap(),
)

/**
 * The server's icon set: drawings, and which code is drawn by which.
 *
 * The two are separate because most codes share a drawing — every helmet is the same helmet — and
 * pointing one more code at an existing sprite should not cost another outline. It also means a
 * single item can be given its own drawing later by changing one line of the table.
 *
 * A sprite carries no colour, only alpha. The client tints it by the item's rarity exactly as it
 * tints its own emblems, and a server-chosen colour would fight the dark theme it lands on.
 */
class IconBundle(
    val hash: String = "",
    private val sprites: Map<String, IconSprite> = emptyMap(),
    private val icons: Map<String, String> = emptyMap(),
) {
    val spriteCount: Int get() = sprites.size
    val size: Int get() = icons.size
    val isEmpty: Boolean get() = icons.isEmpty()

    /** The drawing for one key, or null — and null means "draw your own", never "draw nothing". */
    operator fun get(key: String): IconSprite? = icons[key]?.let { sprites[it] }
    fun contains(key: String): Boolean = get(key) != null
    /** Which drawing a code resolves to; the checks screen reports coverage with it. */
    fun spriteOf(key: String): String? = icons[key]

    companion object {
        /**
         * A set read from the file the server serves.
         *
         * The document is kept as text on the way in so it can be stored verbatim and parsed again
         * on the next start: the manifest's [hash] is what decides whether it is still current.
         */
        fun parse(hash: String, document: String): IconBundle {
            val parsed = WireJson.decodeFromJsonElement(IconDocument.serializer(),
                WireJson.parseToJsonElement(document).jsonObject)
            // A code pointing at a sprite nobody drew is dropped here rather than on screen: the
            // card then falls back to the bundled emblem instead of leaving a hole where art was.
            return IconBundle(hash, parsed.sprites, parsed.icons.filterValues { it in parsed.sprites })
        }
    }
}

/**
 * How an icon key is built from a code.
 *
 * It mirrors `LocaleKey`'s sections so one code answers both questions — what a thing is called and
 * how it is drawn — without a second naming scheme to keep in step.
 */
object IconKey {
    const val EQUIPMENT = "equipment"
    const val ITEM = "item"
    const val STAT = "stat"

    fun equipment(code: String) = "$EQUIPMENT.$code"
    fun item(code: String) = "$ITEM.$code"
    fun stat(stat: String) = "$STAT.$stat"
}

/**
 * The set the app is currently drawing from.
 *
 * It sits beside `serverLocale` and is loaded the same way, but it is not keyed by language: a
 * drawing says the same thing in both.
 */
@Volatile var serverIcons: IconBundle = IconBundle()

/** The drawing for a key, or null when the client should fall back to its own emblem. */
fun icon(key: String): IconSprite? = serverIcons[key]

/**
 * The drawing for a catalogue document, found by the shape rules that find its name.
 *
 * A character is a player's, not content, so it is never looked up — it keeps the client's own
 * exile emblem. Anything without a code predates the set and falls back the same way.
 */
fun documentIcon(document: JsonObject): IconSprite? {
    val code = document.text("code")
    if (code.isBlank() || document["userId"] != null) return null
    val equipment = document["slot"] != null || document.text("type").isNotBlank()
    return serverIcons[if (equipment) IconKey.equipment(code) else IconKey.item(code)]
}
