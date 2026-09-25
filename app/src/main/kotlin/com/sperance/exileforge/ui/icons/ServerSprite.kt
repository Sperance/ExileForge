package com.sperance.exileforge.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
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
