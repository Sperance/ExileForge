package com.sperance.exileforge.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sperance.exileforge.core.display.icons.IconSet
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.core.display.svg.SvgIcon
import com.sperance.exileforge.core.display.svg.SvgPaint
import com.sperance.exileforge.core.display.svg.SvgShape
import kotlinx.serialization.json.JsonObject

/** The icon set of the connected server. Empty until it is loaded, and empty on an older server. */
val LocalForgeIcons = staticCompositionLocalOf { IconSet() }

/**
 * Draws one icon of the server set.
 *
 * The picture is the server's vector art, rendered as it was authored: the palette belongs to the
 * set, so [tint] is for places that deliberately mute it, such as an empty equipment slot. When the
 * set has no drawing — an old server, or icons not loaded yet — [fallback] draws the bundled emblem.
 */
@Composable fun ForgeIcon(id: String?, modifier: Modifier = Modifier, framed: Boolean = true, tint: Color? = null,
    description: String? = null, fallback: @Composable () -> Unit = {}) {
    val art = rememberServerArt(id, framed)
    if(art == null) fallback()
    else Canvas(modifier.semantics { if(description != null) contentDescription = description }) {
        drawServerArt(art, Offset(size.width / 2, size.height / 2), size.minDimension, tint = tint)
    }
}

/**
 * The server's drawing with its path data already parsed.
 *
 * Parsing is the expensive part of an icon, so it is done once per id and reused by every frame that
 * draws it — a stage that redraws a pack of mobs sixty times a second cannot re-parse their art.
 */
class ServerArt internal constructor(internal val icon: SvgIcon, internal val shapes: List<Pair<Path, SvgShape>>)

/** Parsed art of the loaded set, or null when the set has no drawing and the caller must fall back. */
@Composable fun rememberServerArt(id: String?, framed: Boolean = true): ServerArt? {
    val icons = LocalForgeIcons.current
    val icon = icons.drawing(id)
    return remember(icons.version, icon?.id, framed) { icon?.let { serverArt(it, framed) } }
}

/** Parses one drawing outside composition, for callers that cache the art themselves. */
fun serverArt(icons: IconSet, id: String?, framed: Boolean = true): ServerArt? =
    icons.drawing(id)?.let { serverArt(it, framed) }

private fun serverArt(icon: SvgIcon, framed: Boolean) = ServerArt(icon, icon.shapes(framed).mapNotNull { shape ->
    runCatching { PathParser().parsePathString(shape.data).toPath() }.getOrNull()?.let { it to shape }
})

/**
 * Draws parsed server art centred on [center] inside a [side]×[side] square.
 *
 * Lets a canvas of its own — the battle arena — place the same pictures the icon widgets use, at any
 * size and transparency, without a second copy of the SVG reader.
 */
fun DrawScope.drawServerArt(art: ServerArt, center: Offset, side: Float, alpha: Float = 1f, tint: Color? = null) {
    if(side <= 0f || alpha <= 0f) return
    val factor = side / art.icon.viewBox
    withTransform({
        translate(center.x - side / 2f, center.y - side / 2f)
        scale(factor, factor, Offset.Zero)
    }) {
        art.shapes.forEach { (path, shape) -> drawShape(path, shape, art.icon, tint, alpha) }
    }
}

private fun DrawScope.drawShape(path: Path, shape: SvgShape, icon: SvgIcon, tint: Color?, alpha: Float = 1f) {
    shape.fill?.let { drawPath(path, brush(it, icon, tint), alpha = alpha) }
    shape.stroke?.let { drawPath(path, brush(it, icon, tint), alpha = alpha, style = Stroke(shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
}

/** Gradients keep the coordinates the server drew with; the canvas transform scales them along. */
private fun brush(paint: SvgPaint, icon: SvgIcon, tint: Color?): Brush = when(paint) {
    is SvgPaint.Solid -> SolidColor(colour(paint.argb, tint))
    is SvgPaint.Ref -> icon.gradients[paint.id]?.takeIf { it.stops.isNotEmpty() }?.let { gradient ->
        val stops = gradient.stops.map { it.offset to colour(it.argb, tint) }.toTypedArray()
        if(gradient.radial) Brush.radialGradient(*stops, center = Offset(gradient.x1, gradient.y1), radius = gradient.radius.coerceAtLeast(.01f))
        else Brush.linearGradient(*stops, start = Offset(gradient.x1, gradient.y1), end = Offset(gradient.x2, gradient.y2))
    } ?: SolidColor(Color.Transparent)
}

private fun colour(argb: Long, tint: Color?): Color = Color(argb.toInt()).let { drawn -> tint?.copy(alpha = drawn.alpha) ?: drawn }

/** An item, a character or an instance projection: the server's picture, else the bundled emblem. */
@Composable fun ItemIcon(document: JsonObject, color: Color, modifier: Modifier = Modifier, framed: Boolean = true, tint: Color? = null) {
    ForgeIcon(LocalForgeIcons.current.forDocument(document), modifier, framed, tint, com.sperance.exileforge.core.i18n.tr("Иконка", "Icon")) {
        ItemEmblem(itemVisualKind(document), tint ?: color, modifier)
    }
}

/** A rolled property, a stat or a modifier: the set's rune, else the bundled glyph. */
@Composable fun PropertyIcon(key: String, tint: Color, modifier: Modifier = Modifier) {
    ForgeIcon(LocalForgeIcons.current.forProperty(key), modifier, framed = false) {
        Icon(propertyIcon(key), null, tint = tint, modifier = modifier)
    }
}
