package com.sperance.exileforge.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
            outlines.forEach { (path, nodes) ->
                addPath(nodes, fill = SolidColor(Color.Black), fillAlpha = path.alpha.coerceIn(0f, 1f))
            }
        }.build()
        parsed[sprite] = built
        built
    } catch (_: Exception) { null }
}
