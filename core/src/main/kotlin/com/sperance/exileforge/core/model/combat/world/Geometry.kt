package com.sperance.exileforge.core.model.combat.world

import kotlin.math.sqrt

/**
 * World space is a flat grid of tiles: x grows to the south-east, y to the south-west, one unit is one tile.
 *
 * Nothing in this package is an input to an outcome. The server has no notion of where anyone stands,
 * so a position may decide *when* the client asks for an action and never what the action is worth.
 * See [WorldSimulation].
 */
data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(factor: Float) = Vec2(x * factor, y * factor)
    val length: Float get() = sqrt(x * x + y * y)
    /** Unit vector, or the zero vector when there is no direction to speak of. */
    val direction: Vec2 get() = length.let { if(it <= EPSILON) ZERO else Vec2(x / it, y / it) }
    fun distanceTo(other: Vec2) = (this - other).length
    fun lerp(other: Vec2, t: Float) = Vec2(x + (other.x - x) * t, y + (other.y - y) * t)
    fun clamp(maxLength: Float) = if(length <= maxLength) this else direction * maxLength
    companion object { val ZERO = Vec2(); const val EPSILON = 1e-4f }
}

/**
 * A finger's screen movement read as a direction on the ground.
 *
 * The inverse of the isometric projection, so a swipe up the screen walks up the screen rather than
 * along a tile edge. [tile] is the size of one tile in pixels.
 */
fun groundDirection(delta: Vec2, tile: Vec2): Vec2 =
    Vec2(delta.x / tile.x + delta.y / tile.y, delta.y / tile.y - delta.x / tile.x).direction

/**
 * The isometric camera: world tiles to screen pixels and back.
 *
 * [focus] is the point the viewport is centred on, which is how the hero stays in the middle while the
 * ground slides underneath. Rebuilt every frame from the simulation, so it holds no state of its own.
 */
data class IsoCamera(val focus: Vec2, val viewport: Vec2, val tile: Vec2 = DEFAULT_TILE) {
    private fun project(point: Vec2) = Vec2((point.x - point.y) * tile.x / 2f, (point.x + point.y) * tile.y / 2f)
    private val origin = Vec2(viewport.x / 2f, viewport.y / 2f) - project(focus)
    fun toScreen(point: Vec2) = origin + project(point)
    fun toWorld(point: Vec2): Vec2 = (point - origin).let { Vec2(it.x / tile.x + it.y / tile.y, it.y / tile.y - it.x / tile.x) }
    fun toGround(delta: Vec2) = groundDirection(delta, tile)
    /** Painter's order: what stands further down the screen is drawn last. */
    fun depth(point: Vec2) = point.x + point.y
    companion object { val DEFAULT_TILE = Vec2(112f, 56f) }
}
