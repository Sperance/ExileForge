package com.sperance.exileforge.core.model.combat.world

import com.sperance.exileforge.core.i18n.tr

/** Scenery standing on the floor. Everything but [PropKind.BONES] blocks a walk. */
enum class PropKind { ROCK, PILLAR, BRAZIER, BONES }

data class Prop(val position: Vec2, val radius: Float, val kind: PropKind) {
    val blocking: Boolean get() = kind != PropKind.BONES
}

/**
 * The bounded piece of ground one expedition is fought on.
 *
 * Deterministic per zone: the same id always lays the same stones out, so a run looks the same on
 * every device and a screenshot test has something stable to draw. The layout decides where a walk
 * can go and nothing else — it never reaches a server number.
 */
class Battlefield(val width: Float, val height: Float, val props: List<Prop>) {
    init { require(width >= 4f && height >= 4f) { tr("Арена не может быть меньше четырёх клеток", "The arena cannot be smaller than four tiles") } }
    val centre: Vec2 get() = Vec2(width / 2f, height / 2f)

    /**
     * Pushes [point] out of anything a body of [radius] would stand inside, then back within the walls.
     *
     * Two passes, because being pushed clear of one stone can leave the body inside the next; the wall
     * clamp comes last so nothing can be shoved through it.
     */
    fun resolve(point: Vec2, radius: Float): Vec2 {
        var result = point
        repeat(2) {
            props.forEach { prop ->
                if(!prop.blocking) return@forEach
                val gap = prop.radius + radius
                val away = result - prop.position
                if(away.length < gap) result = prop.position + (if(away.length <= Vec2.EPSILON) Vec2(gap, 0f) else away.direction * gap)
            }
        }
        return Vec2(result.x.coerceIn(radius, width - radius), result.y.coerceIn(radius, height - radius))
    }

    companion object {
        const val PROPS = 30
        const val MARGIN = 1.8f
        /** The middle stays swept: the hero spawns there and a stone in the face is not a fight. */
        const val CLEARING = 3.2f
        fun of(zoneId: String, width: Float = 26f, height: Float = 26f): Battlefield {
            val roll = Lcg(zoneId.hashCode().toLong())
            val kinds = PropKind.entries
            val centre = Vec2(width / 2f, height / 2f)
            val props = (0 until PROPS).mapNotNull { index ->
                val point = Vec2(MARGIN + roll.next() * (width - 2 * MARGIN), MARGIN + roll.next() * (height - 2 * MARGIN))
                val kind = kinds[index % kinds.size]
                val radius = when(kind) { PropKind.PILLAR -> .62f; PropKind.ROCK -> .52f; PropKind.BRAZIER -> .34f; PropKind.BONES -> .26f }
                Prop(point, radius, kind).takeIf { point.distanceTo(centre) > CLEARING }
            }
            return Battlefield(width, height, props)
        }
    }
}

/** A seeded generator: the zone's stones must fall in the same places on every device. */
internal class Lcg(seed: Long) {
    private var state = (seed shl 1) or 1L
    fun next(): Float {
        state = state * 6364136223846793005L + 1442695040888963407L
        return ((state ushr 40).toInt() and 0xFFFFFF) / 16_777_215f
    }
}
