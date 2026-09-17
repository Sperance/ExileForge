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
    val icons = LocalForgeIcons.current
    val icon = icons.drawing(id)
    if(icon == null) fallback()
    else {
        val shapes = remember(icons.version, icon.id, framed) {
            icon.shapes(framed).mapNotNull { shape -> runCatching { PathParser().parsePathString(shape.data).toPath() }.getOrNull()?.let { it to shape } }
        }
        Canvas(modifier.semantics { if(description != null) contentDescription = description }) {
            val factor = size.minDimension / icon.viewBox
            withTransform({
                translate((size.width - icon.viewBox * factor) / 2f, (size.height - icon.viewBox * factor) / 2f)
                scale(factor, factor, Offset.Zero)
            }) {
                shapes.forEach { (path, shape) -> drawShape(path, shape, icon, tint) }
            }
        }
    }
}

private fun DrawScope.drawShape(path: Path, shape: SvgShape, icon: SvgIcon, tint: Color?) {
    shape.fill?.let { drawPath(path, brush(it, icon, tint)) }
    shape.stroke?.let { drawPath(path, brush(it, icon, tint), style = Stroke(shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
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
