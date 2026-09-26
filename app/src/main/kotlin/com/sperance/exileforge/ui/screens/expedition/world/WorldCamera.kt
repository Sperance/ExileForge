package com.sperance.exileforge.ui.screens.expedition.world

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import com.sperance.exileforge.core.model.campaign.WorldPoint
import com.sperance.exileforge.core.model.campaign.WorldRule

/**
 * The world map's camera (2.76.0): how near it is and where it looks.
 *
 * One world unit is one dp at [scale] 1. The world's `y` grows up from the start and the screen's
 * grows down, so the art is laid out «down» — `height - y` — and [toScreen] / [toWorld] turn a point
 * either way. The world never leaves the screen: a narrower world is centred, a wider one clamped.
 */
@Stable
class WorldCamera(val world: WorldRule, private val density: Float) {
    var scale by mutableFloatStateOf(1f)
        private set
    /** Where the world's top-left corner is on the screen, in pixels. */
    var offset by mutableStateOf(Offset.Zero)
        private set
    var viewport by mutableStateOf(IntSize.Zero)
    /** Whether the camera has been put anywhere yet: the first look goes to the frontier. */
    var placed by mutableStateOf(false)
        private set

    /** Pixels per world unit. */
    val unit: Float get() = density * scale

    /** As far out as shows the whole world, never nearer than [HOME]. */
    val minScale: Float get() =
        if (viewport == IntSize.Zero) MIN else minOf(viewport.width / (world.width * density), viewport.height / (world.height * density), HOME)

    fun toScreen(x: Float, y: Float): Offset = Offset(offset.x + x * unit, offset.y + (world.height - y) * unit)

    fun toWorld(point: Offset): Offset = Offset((point.x - offset.x) / unit, world.height - (point.y - offset.y) / unit)

    /** A pinch and a drag at once: the point under the fingers stays under them while the rest follows. */
    fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        val anchor = toWorld(centroid)
        scale = (scale * zoom).coerceIn(minScale, MAX)
        offset += centroid - toScreen(anchor.x, anchor.y) + pan
        clamp()
    }

    /** Puts [point] at [across] × [down] of the screen at [nearness], at once. */
    fun look(point: WorldPoint, nearness: Float, down: Float = .5f, across: Float = .5f) {
        scale = nearness.coerceIn(minScale, MAX)
        offset = Offset(viewport.width * across - point.x * unit, viewport.height * down - (world.height - point.y) * unit)
        clamp()
        placed = true
    }

    /** The same move as [look], flown. */
    suspend fun glide(point: WorldPoint, nearness: Float, down: Float = .5f) {
        val fromScale = scale
        val fromOffset = offset
        look(point, nearness, down)
        val toScale = scale
        val toOffset = offset
        scale = fromScale
        offset = fromOffset
        animate(0f, 1f, animationSpec = tween(GLIDE_MS, easing = FastOutSlowInEasing)) { t, _ ->
            scale = lerp(fromScale, toScale, t)
            offset = lerp(fromOffset, toOffset, t)
        }
    }

    /** Nearer or farther by [factor], about the middle of what is seen. */
    suspend fun zoomBy(factor: Float) {
        val middle = toWorld(Offset(viewport.width / 2f, viewport.height * .4f))
        glide(WorldPoint(middle.x.toInt(), middle.y.toInt()), (scale * factor).coerceIn(minScale, MAX), down = .4f)
    }

    /** Whether [point] is on the screen with [margin] pixels to spare above [bottom] (a share of its height). */
    fun sees(point: WorldPoint, margin: Float, bottom: Float = 1f): Boolean {
        val p = toScreen(point.x.toFloat(), point.y.toFloat())
        return p.x in margin..viewport.width - margin && p.y in margin..viewport.height * bottom - margin
    }

    val canZoomIn: Boolean get() = scale < MAX - EPSILON
    val canZoomOut: Boolean get() = scale > minScale + EPSILON

    private fun clamp() {
        val width = world.width * unit
        val height = world.height * unit
        offset = Offset(
            if (width <= viewport.width) (viewport.width - width) / 2 else offset.x.coerceIn(viewport.width - width, 0f),
            if (height <= viewport.height) (viewport.height - height) / 2 else offset.y.coerceIn(viewport.height - height, 0f),
        )
    }

    companion object {
        /** The nearness the map opens at and the frontier button returns to. */
        const val HOME = .85f
        const val MAX = 1.6f
        private const val MIN = .3f
        private const val EPSILON = .001f
        private const val GLIDE_MS = 550
        /** Below this nearness a token's name would be a smudge: names fade out. */
        const val NAMES_FROM = .6f
    }
}
