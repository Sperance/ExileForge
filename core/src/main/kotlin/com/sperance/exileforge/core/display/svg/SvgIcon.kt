package com.sperance.exileforge.core.display.svg

/** A colour (0xAARRGGBB) or a reference to a gradient declared in the document's `defs`. */
sealed interface SvgPaint {
    data class Solid(val argb: Long) : SvgPaint
    data class Ref(val id: String) : SvgPaint
}
data class SvgStop(val offset: Float, val argb: Long)
/**
 * Gradient in view-box coordinates (`userSpaceOnUse`).
 *
 * The server draws with plain lines, whose bounding box has zero area: with bounding-box units the
 * gradient would be invalid and the stroke would not be drawn at all.
 */
data class SvgGradient(val radial: Boolean, val x1: Float, val y1: Float, val x2: Float, val y2: Float,
    val radius: Float, val stops: List<SvgStop>)
/** One drawn shape. Circles are converted to path data, so a renderer only has to handle paths. */
data class SvgShape(val data: String, val fill: SvgPaint? = null, val stroke: SvgPaint? = null, val strokeWidth: Float = 1f)
/**
 * One parsed icon of the server set.
 *
 * The plate is kept apart from the drawing, so a single download serves both the framed icon and
 * the `variant=plain` artwork a client places inside frames of its own.
 */
data class SvgIcon(val id: String, val viewBox: Float, val frame: List<SvgShape>, val art: List<SvgShape>,
    val gradients: Map<String, SvgGradient>) {
    fun shapes(framed: Boolean): List<SvgShape> = if(framed) frame + art else art
}

private val TAG = Regex("<(/?)([A-Za-z][A-Za-z0-9]*)([^>]*)>")
private val ATTR = Regex("([A-Za-z][A-Za-z0-9-]*)\\s*=\\s*\"([^\"]*)\"")

/** One icon document, `GET /api/v1/icons/{id}.svg`: the file carries no id of its own. */
fun parseSvgIcon(text: String, id: String): SvgIcon? = parseSvgIcons(text, id).firstOrNull()
/** The whole set in one response, `GET /api/v1/icons/sprite.svg`, as `<symbol id="icon-...">`. */
fun parseSvgSprite(text: String): Map<String, SvgIcon> = parseSvgIcons(text, "").associateBy { it.id }

/**
 * Reader for the subset the server renders: paths, circles, groups and the two gradient kinds.
 *
 * Shapes outside the drawing group are the plate; shapes inside it are the artwork. Nothing else is
 * interpreted, so an unknown element is skipped rather than failing the whole set.
 */
private fun parseSvgIcons(text: String, fallbackId: String): List<SvgIcon> {
    val icons = mutableListOf<SvgIcon>()
    var id: String? = null
    var viewBox = 64f
    val frame = mutableListOf<SvgShape>()
    val art = mutableListOf<SvgShape>()
    val gradients = mutableMapOf<String, SvgGradient>()
    var gradientId = ""
    var gradientAttributes = emptyMap<String, String>()
    var radial = false
    val stops = mutableListOf<SvgStop>()
    var depth = 0
    var inherited = emptyMap<String, String>()
    val groups = ArrayDeque<Map<String, String>>()
    fun flush() {
        if(id != null && (frame.isNotEmpty() || art.isNotEmpty())) icons += SvgIcon(id!!, viewBox, frame.toList(), art.toList(), gradients.toMap())
        id = null; frame.clear(); art.clear(); gradients.clear(); groups.clear(); inherited = emptyMap(); depth = 0
    }
    fun open(name: String, attributes: Map<String, String>) {
        flush()
        id = name.ifBlank { fallbackId }
        viewBox = attributes["viewbox"]?.trim()?.split(Regex("[\\s,]+"))?.getOrNull(3)?.toFloatOrNull() ?: 64f
    }
    fun shape(attributes: Map<String, String>, data: String) {
        if(data.isBlank() || id == null) return
        val merged = inherited + attributes
        val figure = SvgShape(data, paint(merged["fill"], merged["fill-opacity"]), paint(merged["stroke"], merged["stroke-opacity"]),
            merged["stroke-width"]?.toFloatOrNull() ?: 1f)
        if(figure.fill == null && figure.stroke == null) return
        (if(depth > 0) art else frame) += figure
    }
    TAG.findAll(text).forEach { match ->
        val closing = match.groupValues[1] == "/"
        val name = match.groupValues[2].lowercase()
        val attributes = ATTR.findAll(match.groupValues[3]).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        when(name) {
            "svg" -> if(closing) flush() else open(fallbackId, attributes)
            "symbol" -> if(closing) flush() else open(attributes["id"].orEmpty().removePrefix("icon-"), attributes)
            "lineargradient", "radialgradient" -> if(closing) {
                gradients[gradientId] = gradient(radial, gradientAttributes, stops.toList()); stops.clear(); gradientId = ""
            } else {
                radial = name == "radialgradient"; gradientId = attributes["id"].orEmpty(); gradientAttributes = attributes; stops.clear()
            }
            "stop" -> if(!closing) stops += SvgStop(attributes["offset"]?.toFloatOrNull() ?: 0f,
                colour(attributes["stop-color"], attributes["stop-opacity"]) ?: 0xFF000000L)
            "g" -> if(closing) { depth = (depth - 1).coerceAtLeast(0); inherited = groups.removeLastOrNull() ?: emptyMap() }
                else { groups.addLast(inherited); inherited = inherited + attributes; depth++ }
            "path" -> if(!closing) shape(attributes, attributes["d"].orEmpty())
            "circle" -> if(!closing) shape(attributes, circle(attributes))
        }
    }
    flush()
    return icons
}

private fun gradient(radial: Boolean, attributes: Map<String, String>, stops: List<SvgStop>): SvgGradient {
    fun number(key: String, fallback: Float) = attributes[key]?.toFloatOrNull() ?: fallback
    return if(radial) SvgGradient(true, number("cx", 0f), number("cy", 0f), 0f, 0f, number("r", 1f), stops)
    else SvgGradient(false, number("x1", 0f), number("y1", 0f), number("x2", 1f), number("y2", 0f), 0f, stops)
}

/** A circle as path data: the arc pair every SVG path parser understands. */
private fun circle(attributes: Map<String, String>): String {
    val cx = attributes["cx"]?.toFloatOrNull() ?: return ""
    val cy = attributes["cy"]?.toFloatOrNull() ?: return ""
    val r = attributes["r"]?.toFloatOrNull()?.takeIf { it > 0f } ?: return ""
    return "M${cx - r} $cy a $r $r 0 1 1 ${r * 2} 0 a $r $r 0 1 1 ${-r * 2} 0 Z"
}

private fun paint(value: String?, opacity: String?): SvgPaint? {
    val raw = value?.trim().orEmpty()
    if(raw.startsWith("url(#")) return SvgPaint.Ref(raw.removePrefix("url(#").removeSuffix(")"))
    return colour(raw, opacity)?.let(SvgPaint::Solid)
}

/** `#rgb` and `#rrggbb` with an optional opacity; anything else (including `none`) paints nothing. */
private fun colour(value: String?, opacity: String?): Long? {
    val raw = value?.trim().orEmpty()
    if(!raw.startsWith("#")) return null
    val hex = raw.removePrefix("#").let { if(it.length == 3) it.map { digit -> "$digit$digit" }.joinToString("") else it }
    if(hex.length != 6) return null
    val rgb = hex.toLongOrNull(16) ?: return null
    val alpha = ((opacity?.toFloatOrNull() ?: 1f).coerceIn(0f, 1f) * 255).toLong()
    return (alpha shl 24) or rgb
}
