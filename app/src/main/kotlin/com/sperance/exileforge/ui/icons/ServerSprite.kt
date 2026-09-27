package com.sperance.exileforge.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.IconPath
import com.sperance.exileforge.core.display.IconSprite
import java.util.concurrent.ConcurrentHashMap

/**
 * A drawing from the server, turned into something Compose can paint.
 *
 * Path data is parsed rather than fetched as an image: the file carries outlines, not pictures, so
 * an icon stays a tintable vector that takes the item's rarity colour like the bundled emblems do.
 *
 * Parsing is memoised by the sprite itself. A stash screen draws the same helmet a dozen times and
 * re-parsing that outline per frame would be the most expensive thing on it.
 */
private val parsed = ConcurrentHashMap<IconSprite, ImageVector>()

/** A traced outline's width, as a share of the sprite's box: 1.8 in the usual 24. */
private const val STROKE_SHARE = .075f

/** The lead between two panes of glass, as a share of the sprite's box: 2.4 in the glass 64. */
private const val LEAD_SHARE = .0375f
private val Lead = SolidColor(Color(0xFF0B0907))

/**
 * A server sprite drawn the way its style asks: glass as it is, over a halo of [color]; mono tinted
 * by [color]. Returns false when there is nothing to draw, so the caller can put its own emblem there.
 */
@Composable fun SpriteIcon(sprite: IconSprite?, color: Color, modifier: Modifier = Modifier, halo: Boolean = true): Boolean {
    if (sprite == null) return false
    val vector = spriteVector(sprite) ?: return false
    if (sprite.isGlass) Image(vector, null, if (halo) modifier.rarityHalo(color) else modifier)
    else Icon(vector, null, tint = color, modifier = modifier)
    return true
}

/** The rarity of a glass icon: a soft disc of its colour behind the panes. */
private fun Modifier.rarityHalo(color: Color): Modifier = drawBehind {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .55f), Color.Transparent), center, size.minDimension / 2))
}

/**
 * The sprite as an `ImageVector`, or null when its outlines cannot be read.
 *
 * A malformed path costs exactly its own icon: the caller falls back to the bundled emblem, and
 * the rest of the set keeps drawing. One typo in a hand-written file should not blank an app.
 */
fun spriteVector(sprite: IconSprite): ImageVector? {
    parsed[sprite]?.let { return it }
    if (sprite.paths.isEmpty() || sprite.viewBox <= 0f) return null
    return try {
        val outlines = sprite.paths.map { it to PathParser().parsePathString(it.d).toNodes() }
        // An outline that parsed to nothing is as good as a hole, and some malformed strings come
        // back empty rather than throwing. Treating both the same keeps the fallback predictable.
        if (outlines.any { (_, nodes) -> nodes.isEmpty() }) return null
        val built = ImageVector.Builder(
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = sprite.viewBox, viewportHeight = sprite.viewBox,
        ).apply {
            if (sprite.isGlass) { glass(sprite.viewBox, outlines); return@apply }
            // Every outline is filled and also traced (2.72.0): a sprite drawn with bare lines — the
            // snowflake of cold, a slash, a row of bars — has no area to fill and was invisible.
            val line = sprite.viewBox * STROKE_SHARE
            outlines.forEach { (path, nodes) ->
                val alpha = path.alpha.coerceIn(0f, 1f)
                addPath(nodes, fill = SolidColor(Color.Black), fillAlpha = alpha, stroke = SolidColor(Color.Black), strokeAlpha = alpha,
                    strokeLineWidth = line, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()
        parsed[sprite] = built
        built
    } catch (_: Exception) { null }
}

/**
 * Stained glass (3.6.0): a piece is its colour with a diagonal sheen and a lead line round it, a
 * glaze (alpha below one) is a highlight or a shade laid over the pieces, a line is lead with colour
 * inside. The same three rules as `scripts/gen_icons.py` on the server that draws them.
 */
private fun ImageVector.Builder.glass(box: Float, outlines: List<Pair<IconPath, List<PathNode>>>) {
    val lead = box * LEAD_SHARE
    val sheen = Brush.linearGradient(0f to Color.White.copy(alpha = .45f), .5f to Color.Transparent, 1f to Color.Black.copy(alpha = .35f),
        start = Offset.Zero, end = Offset(box, box))
    outlines.forEach { (path, nodes) ->
        val paint = SolidColor(hexColor(path.color))
        val fill = if (path.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
        when {
            path.line > 0f -> {
                addPath(nodes, stroke = Lead, strokeLineWidth = path.line + lead * .66f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
                addPath(nodes, stroke = paint, strokeLineWidth = path.line * .55f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
            path.alpha < 1f -> addPath(nodes, pathFillType = fill, fill = paint, fillAlpha = path.alpha.coerceIn(0f, 1f))
            else -> {
                addPath(nodes, pathFillType = fill, fill = paint)
                addPath(nodes, pathFillType = fill, fill = sheen)
                addPath(nodes, pathFillType = fill, stroke = Lead, strokeLineWidth = lead, strokeLineJoin = StrokeJoin.Round)
            }
        }
    }
}

/** `#rrggbb` as a colour; anything unreadable is white glass rather than a failed icon. */
private fun hexColor(hex: String?): Color =
    hex?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: Color.White
