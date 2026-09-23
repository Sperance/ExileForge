package com.sperance.exileforge.core.display

import kotlinx.serialization.Serializable
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/** `portraits/index.json`: the fingerprint of every portrait by its key, so only a changed file is fetched. */
@Serializable data class PortraitManifest(
    val hash: String = "",
    val width: Int = 300,
    val height: Int = 400,
    val portraits: Map<String, String> = emptyMap(),
)

/** A stop of a gradient: where along it, and the colour as ARGB. */
data class GradientStop(val offset: Float, val argb: Long)

/** What fills or strokes a shape. Coordinates are the portrait's own (user space), never shares. */
sealed interface PortraitPaint {
    data class Solid(val argb: Long) : PortraitPaint
    data class Linear(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val stops: List<GradientStop>) : PortraitPaint
    data class Radial(val cx: Float, val cy: Float, val r: Float, val stops: List<GradientStop>) : PortraitPaint
}

/** One shape, already turned into path data whatever element it was written as. */
data class PortraitShape(
    val d: String,
    val fill: PortraitPaint?,
    val fillAlpha: Float = 1f,
    val stroke: PortraitPaint? = null,
    val strokeAlpha: Float = 1f,
    val strokeWidth: Float = 1f,
    val round: Boolean = false,
)

/**
 * A portrait (since 2.31.0, server 0.29.0): a class's or a monster's bust, three by four, drawn
 * from the server's SVG by the client itself — rule 17 still holds, it is outlines, not a picture.
 * The face sits in the circle [TOKEN_X], [TOKEN_Y], [TOKEN_R] (shares of the width), which is what
 * the map cuts out as the fighter's round token.
 */
data class Portrait(val width: Float, val height: Float, val shapes: List<PortraitShape>) {
    companion object {
        const val TOKEN_X = .5f
        const val TOKEN_Y = 165f / 300f
        const val TOKEN_R = .4f
    }
}

/**
 * The subset of SVG the server's portraits are written in, and nothing more: `path`, `circle`,
 * `ellipse`, `rect`, `g`, and linear or radial gradients in user space. The server's `PortraitTest`
 * refuses a file that steps outside it; an element this parser does not know is skipped, never fatal.
 */
object PortraitSvg {

    fun parse(text: String): Portrait {
        val factory = DocumentBuilderFactory.newInstance()
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        val root = factory.newDocumentBuilder().parse(text.byteInputStream()).documentElement
        val box = root.getAttribute("viewBox").split(' ', ',').filter(String::isNotBlank).map(String::toFloat)
        require(box.size == 4) { "viewBox" }
        val gradients = mutableMapOf<String, PortraitPaint>()
        root.children().filter { it.tagName == "defs" }.flatMap { it.children() }.forEach { element ->
            val stops = element.children().filter { it.tagName == "stop" }.map {
                GradientStop(it.number("offset"), colour(it.getAttribute("stop-color"), it.number("stop-opacity", 1f)) ?: 0L)
            }
            when (element.tagName) {
                "linearGradient" -> gradients[element.getAttribute("id")] =
                    PortraitPaint.Linear(element.number("x1"), element.number("y1"), element.number("x2"), element.number("y2"), stops)
                "radialGradient" -> gradients[element.getAttribute("id")] =
                    PortraitPaint.Radial(element.number("cx"), element.number("cy"), element.number("r"), stops)
            }
        }
        val shapes = mutableListOf<PortraitShape>()
        fun walk(element: Element, inherited: Style) {
            val style = inherited.under(element)
            when (element.tagName) {
                "g", "svg" -> element.children().forEach { walk(it, style) }
                "path", "circle", "ellipse", "rect" -> outline(element)?.let { d ->
                    shapes += PortraitShape(d, paint(style.fill, gradients), style.opacity * style.fillOpacity,
                        paint(style.stroke, gradients), style.opacity * style.strokeOpacity, style.strokeWidth, style.round)
                }
            }
        }
        root.children().filter { it.tagName != "defs" }.forEach { walk(it, Style()) }
        return Portrait(box[2], box[3], shapes)
    }

    private data class Style(
        val fill: String = "#000000", val stroke: String = "none", val opacity: Float = 1f, val fillOpacity: Float = 1f,
        val strokeOpacity: Float = 1f, val strokeWidth: Float = 1f, val round: Boolean = false,
    ) {
        fun under(e: Element) = Style(
            fill = e.attr("fill") ?: fill, stroke = e.attr("stroke") ?: stroke, opacity = opacity * e.number("opacity", 1f),
            fillOpacity = e.attr("fill-opacity")?.toFloat() ?: fillOpacity, strokeOpacity = e.attr("stroke-opacity")?.toFloat() ?: strokeOpacity,
            strokeWidth = e.attr("stroke-width")?.toFloat() ?: strokeWidth, round = e.attr("stroke-linecap")?.let { it == "round" } ?: round,
        )
    }

    /** Every element as path data, so the painter has one kind of shape to draw. */
    private fun outline(e: Element): String? = when (e.tagName) {
        "path" -> e.attr("d")
        "circle" -> ellipse(e.number("cx"), e.number("cy"), e.number("r"), e.number("r"))
        "ellipse" -> ellipse(e.number("cx"), e.number("cy"), e.number("rx"), e.number("ry"))
        "rect" -> "M${e.number("x")} ${e.number("y")} h${e.number("width")} v${e.number("height")} h${-e.number("width")} Z"
        else -> null
    }

    private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) =
        "M${cx - rx} $cy A$rx $ry 0 1 0 ${cx + rx} $cy A$rx $ry 0 1 0 ${cx - rx} $cy Z"

    private fun paint(value: String, gradients: Map<String, PortraitPaint>): PortraitPaint? = when {
        value.startsWith("url(#") -> gradients[value.removePrefix("url(#").removeSuffix(")")]
        else -> colour(value, 1f)?.let(PortraitPaint::Solid)
    }

    /** `#rgb` or `#rrggbb` with an alpha; `none` and anything else is no paint at all. */
    internal fun colour(value: String, alpha: Float): Long? {
        val hex = value.trim().removePrefix("#").takeIf { value.trim().startsWith("#") } ?: return null
        val rgb = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }.toLongOrNull(16) ?: return null
        return ((alpha.coerceIn(0f, 1f) * 255).toLong() shl 24) or rgb
    }

    private fun Element.attr(name: String): String? = getAttribute(name).takeIf { hasAttribute(name) }
    private fun Element.number(name: String, fallback: Float = 0f): Float = attr(name)?.trim()?.toFloatOrNull() ?: fallback
    private fun Element.children(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
}

/** How a portrait key is built: the same sections the server files them under. */
object PortraitKey {
    const val CLASS = "class"
    const val FORM = "form"
    const val MONSTER = "monster"

    fun characterClass(code: String) = "$CLASS.$code"
    fun form(form: String) = "$FORM.$form"
    fun monster(code: String) = "$MONSTER.$code"
    /** Where the file is served: `portraits/class/WITCH.svg`. */
    fun path(key: String) = "portraits/${key.substringBefore('.')}/${key.substringAfter('.')}.svg"
}

/** The portraits this server drew, by key. */
class PortraitBundle(val portraits: Map<String, Portrait> = emptyMap()) {
    val size: Int get() = portraits.size
    operator fun get(key: String): Portrait? = portraits[key]
}

/** The set the app is drawing from, global as `serverIcons` is. */
@Volatile var serverPortraits: PortraitBundle = PortraitBundle()

/** A class's portrait, or null for the client's own bust. */
fun classPortrait(code: String?): Portrait? = code?.takeIf(String::isNotBlank)?.let { serverPortraits[PortraitKey.characterClass(it)] }

/** A monster's own portrait first, then its form's, then null for the client's own drawing. */
fun monsterPortrait(code: String, form: String): Portrait? =
    serverPortraits[PortraitKey.monster(code)] ?: serverPortraits[PortraitKey.form(form)]
