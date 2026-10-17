package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.content.NodeLook
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Шаг контура рисунка узла: координаты клетки рисунка ([NodeArt.CELL_WIDTH] × [NodeArt.CELL_HEIGHT], y вниз). */
sealed interface PathOp {
    data class Move(val x: Float, val y: Float) : PathOp

    data class Line(val x: Float, val y: Float) : PathOp

    data class Quad(val x1: Float, val y1: Float, val x: Float, val y: Float) : PathOp

    data class Cubic(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x: Float, val y: Float) : PathOp

    data object Close : PathOp
}

/** Как художник кладёт часть: тело (заливка с контуром), свечение, линия или дыра-тень. */
enum class StrokeKind { BODY, GLOW, LINE, HOLE }

/** Часть рисунка узла: контур [path], цвет [color] (ARGB), прозрачность свечения [alpha], толщина линии [width] в точках клетки. */
data class NodeStroke(val kind: StrokeKind, val path: List<PathOp>, val color: Long, val alpha: Float = 1f, val width: Float = 1f)

/** Рисунок узла сбора в двух состояниях: целый и собранный. */
data class NodeSketch(val full: List<NodeStroke>, val gathered: List<NodeStroke>) {
    fun of(spent: Boolean): List<NodeStroke> = if (spent) gathered else full
}

/**
 * Реестр рисунков узлов сбора: у каждого облика контента ([NodeLook]) - целый и собранный рисунок из простых фигур (многоугольники,
 * эллипсы, кривые). Геометрия - в клетке [CELL_WIDTH] × [CELL_HEIGHT], центр пола клетки - ([FLOOR_X], [FLOOR_Y]), ромб пола -
 * [TILE_WIDTH] в ширину. Кисть (заливка, контур, ореол) выбирает художник приложения.
 */
object NodeArt {
    const val CELL_WIDTH = 80f
    const val CELL_HEIGHT = 84f
    const val FLOOR_X = 40f
    const val FLOOR_Y = 60f
    const val TILE_WIDTH = 64f

    private val sketches: Map<NodeLook, NodeSketch> by lazy { NodeLook.entries.associateWith { NodeSketch(Sketch().apply { full(it) }.strokes, Sketch().apply { gathered(it) }.strokes) } }

    /** Рисунок облика [look]. */
    fun of(look: NodeLook): NodeSketch = sketches.getValue(look)

    /** Тон [color], осветлённый к белому долей [f] (больше нуля) или притемнённый к чёрному (меньше нуля). */
    fun shade(color: Long, f: Double): Long {
        val channels = listOf(16, 8, 0).map { shift ->
            val v = (color shr shift and 0xFF).toDouble()
            (if (f >= 0) v + (255 - v) * f else v * (1 + f)).roundToInt().coerceIn(0, 255).toLong() shl shift
        }
        return (color and 0xFF000000) or channels.sum()
    }

    private fun Sketch.full(look: NodeLook) {
        when (look) {
            NodeLook.COPPER_VEIN -> {
                rock(40, 18, 24, 0x5E554C)
                body(circle(33, 47, 2.8), 0xD9813A)
                body(circle(44, 42, 3.2), 0xE08E45)
                body(circle(49, 52, 2.4), 0xD9813A)
                body(circle(37, 54, 1.8), 0x4FA88A)
                body(circle(52, 45, 1.5), 0x4FA88A)
                glow(circle(43, 41, 1), 0xFFD9B0)
            }

            NodeLook.IRON_VEIN -> {
                rock(40, 19, 25, 0x4E4946)
                body(poly(23, 54, 56, 45, 58, 48.5, 24, 57.5), 0x9A4E2E)
                body(poly(27, 45, 51, 38, 52, 41, 28, 48), 0xB0603A)
            }

            NodeLook.TOPAZ_DEPOSIT -> {
                rock(40, 15, 9, 0x3A3A40)
                prism(32, 57, 12, 4.4, -2.5, 0xF6D365, 0xC8962A)
                prism(42, 55, 17, 5, 1, 0xF6D365, 0xB8862A)
                prism(51, 58, 9, 3.6, 3, 0xF2C94C, 0xA87A22)
                glow(star4(43, 33, 3), 0xFFF6D0)
            }

            NodeLook.SILVER_VEIN -> {
                rock(40, 18, 24, 0x4A5058)
                line(svg("M24 52L31 46L36 50L44 42L50 46L57 40"), 0xE6EEF5, 1.7)
                glow(star4(44, 42, 4), 0xFFFFFF)
            }

            NodeLook.SAPPHIRE_DEPOSIT -> {
                rock(40, 15, 9, 0x3A3A44)
                prism(33, 58, 15, 3, -4, 0x6FA0F0, 0x2A4FA0)
                prism(41, 55, 26, 3.4, 0, 0x7FB0FF, 0x2A4FA0)
                prism(48, 57, 18, 3, 3, 0x6FA0F0, 0x24448A)
                prism(54, 60, 9, 2.4, 5, 0x6FA0F0, 0x24448A)
            }

            NodeLook.MITHRIL_VEIN -> {
                rock(40, 19, 26, 0x363E4C)
                halo(41, 47, 13, 0x8FD3FF, .22)
                glow(MITHRIL_SEAM, 0x8FD3FF)
            }

            NodeLook.RUBY_DEPOSIT -> {
                rock(40, 15, 8, 0x3A3434)
                prism(30, 59, 11, 3, -7, 0xF06070, 0x9A1E2E)
                prism(35, 57, 17, 3.2, -3, 0xF06070, 0x9A1E2E)
                prism(42, 56, 22, 3.4, 1, 0xFF7080, 0x9A1E2E)
                prism(48, 57, 15, 3, 5, 0xF06070, 0x8A1A28)
                prism(53, 60, 10, 2.6, 8, 0xF06070, 0x8A1A28)
            }

            NodeLook.ADAMANT_VEIN -> {
                rock(40, 17, 20, 0x2E3430)
                prism(33, 47, 14, 3, -4, 0x3FA878, 0x1E5A40)
                prism(44, 44, 19, 3.6, 2, 0x4FC088, 0x1E5A40)
                prism(52, 51, 10, 2.6, 5, 0x3FA878, 0x1E5A40)
                line(svg("M44 44L46 25"), 0x7BE0A6, .8)
            }

            NodeLook.STARMETAL_VEIN -> {
                body(oval(40, 62, 24, 8), 0x1E1A28)
                body(oval(40, 50, 14, 12), 0x2A2638)
                body(oval(36, 46, 8, 5), 0x3E3854)
                hole(oval(44, 52, 3, 2))
                halo(40, 48, 18, 0xB4B4FF, .14)
                glow(star4(31, 41, 3.4), 0xD0C0FF)
                glow(star4(50, 40, 2.4), 0xFFFFFF)
                glow(star4(47, 56, 2), 0xD0C0FF)
            }

            NodeLook.ONYX_DEPOSIT -> {
                rock(40, 14, 7, 0x3A3A40)
                prism(39, 59, 28, 6, 0, 0x4A4A55, 0x16161C)
                line(svg("M33 44L39 46.1L45 44"), 0xEEF0F4, 1.1)
                prism(51, 61, 10, 3, 3, 0x4A4A55, 0x16161C)
            }

            NodeLook.WORMWOOD_PATCH -> repeat(11) { i ->
                val x = 25.0 + i * 3
                val h = 13 + 9 * abs(sin(i * 1.7))
                val lean = (x - 40) * .35
                body(poly(x - 1.4, 63, x + 1.4, 63, x + lean, 63 - h), listOf(0x8FA48AL, 0xA9BCA2L, 0x6E8A6AL)[i % 3])
                if (i % 3 == 1) body(circle(x + lean, 62 - h, 1.3), 0xD8C860)
            }

            NodeLook.CAVE_MUSHROOMS -> {
                shroom(30, 62, 8, 6, 0xA87A4A)
                shroom(39, 58, 13, 8, 0xB88654)
                shroom(48, 61, 10, 6.5, 0xA87A4A)
                shroom(54, 65, 5, 4, 0x9A6E44)
                shroom(35, 67, 4, 3.6, 0x9A6E44)
            }

            NodeLook.BLOODWORT_PATCH -> {
                line(svg("M40 63L40 40M40 52L31 44M40 50L50 42"), 0x5A2A2A, 1.4)
                leaf(40, 58, 13, -2.6, 4, 0x8A2E2E)
                leaf(40, 58, 13, -.5, 4, 0x8A2E2E)
                leaf(40, 50, 12, -2.2, 3.6, 0xA83A38)
                leaf(40, 48, 12, -.9, 3.6, 0xA83A38)
                leaf(40, 42, 10, -1.6, 3.2, 0xB84440)
                body(circle(31, 44, 2), 0xE0504A)
                body(circle(34, 42, 1.6), 0xE0504A)
                body(circle(50, 42, 2), 0xE0504A)
                body(circle(47, 40, 1.5), 0xE0504A)
                glow(circle(40, 38, 1.8), 0xFF6A60)
            }

            NodeLook.AMBER_TAPPING -> {
                body(poly(32, 63, 48, 63, 48, 30, 32, 30), 0x5A3A22)
                body(poly(41, 63, 48, 63, 48, 30, 41, 30), 0x46301C)
                body(oval(40, 30, 8, 3), 0xC09060)
                line(oval(40, 30, 4.2, 1.5), 0x8A6440, .7)
                hole(poly(35, 41, 45, 41, 40, 47))
                glow(svg("M40 46Q42.6 52 40 54.4Q37.4 52 40 46Z"), 0xF2A65A)
                body(poly(35, 57, 45, 57, 44, 63, 36, 63), 0x7A6A5A)
                glow(oval(40, 57, 5, 1.3), 0xF2A65A)
            }

            NodeLook.SHADEBLOOM_PATCH -> {
                halo(41, 43, 16, 0xA070FF, .16)
                line(svg("M32 63Q31 52 32 45M42 63Q43 50 42 39M50 63Q51 54 50 47"), 0x3A4A3A, 1.2)
                leaf(37, 62, 9, -2.7, 2.6, 0x2E3E30)
                leaf(44, 62, 9, -.4, 2.6, 0x2E3E30)
                listOf(Triple(32.0, 44.0, 4.0), Triple(42.0, 38.0, 5.0), Triple(50.0, 46.0, 4.0)).forEach { (x, y, r) ->
                    repeat(5) { i ->
                        val a = -PI / 2 + i * 1.2566
                        body(circle(x + r * .62 * cos(a), y + r * .62 * sin(a), r * .5), 0x7A4AC0)
                    }
                    body(circle(x, y, r * .32), 0x1A0E28)
                    glow(circle(x, y, r * .14), 0xD0B0FF)
                }
            }

            NodeLook.GLOWCAP_GROTTO -> {
                rock(40, 20, 34, 0x2A2E33)
                hole(oval(40, 53, 11, 10))
                halo(41, 48, 14, 0x63E0D0, .25)
                listOf(listOf(35.0, 60.0, 13.0, 3.2), listOf(41.0, 59.0, 19.0, 3.8), listOf(47.0, 61.0, 10.0, 2.8)).forEach { (x, y, h, r) ->
                    line(svg("M$x ${y}L$x ${y - h}"), 0x9AD8D0, 1)
                    glow(svg("M${x - r} ${y - h + 1}Q$x ${y - h - r * 2} ${x + r} ${y - h + 1}Z"), 0x63E0D0)
                }
            }

            NodeLook.MOON_LOTUS_POND -> {
                pond(0x3A4148, 0x16304A)
                body(oval(35, 60.5, 9, 2.2), 0x244A68)
                body(oval(29, 63, 5, 2), 0x2E6A44)
                body(oval(51, 64, 6, 2.2), 0x2E6A44)
                body(oval(46, 59, 4, 1.5), 0x3A7A50)
                halo(40, 53, 13, 0xB8C8FF, .2)
                (-2..2).forEach { i -> body(poly(40 + i * 2.6 - 2.4, 60, 40 + i * 2.6 + 2.4, 60, 40 + i * 3.6, 60 - (9 - abs(i) * 2)), if (abs(i) == 1) 0xB8A8E8 else 0xECE6FF) }
                glow(circle(40, 58.6, 1.6), 0xFFE8A0)
            }

            NodeLook.VOID_ROOT_BED -> {
                body(oval(40, 62, 21, 7), 0x2A2030)
                body(oval(40, 59.5, 15, 5), 0x3A2E44)
                halo(42, 44, 15, 0xA070FF, .16)
                line(svg("M29 60Q28 48 37 45"), 0x5A3A80, 2.6)
                line(svg("M45 60Q53 48 56 47"), 0x5A3A80, 2.6)
                line(svg("M38 60Q39 42 47 36"), 0x6A44A0, 2.8)
                glow(circle(37, 45, 1.7), 0xA070FF)
                glow(circle(56, 47, 1.7), 0xA070FF)
                glow(circle(47, 36, 2), 0xC0A0FF)
            }

            NodeLook.STARBLOOM_PATCH -> {
                line(svg("M30 63Q29 54 31 47M40 63Q41 50 39 40M49 63Q50 55 48 49M55 64Q56 60 55 56"), 0x3E5A3A, 1.1)
                leaf(36, 62, 8, -2.6, 2.2, 0x3E5A3A)
                leaf(45, 62, 8, -.5, 2.2, 0x3E5A3A)
                halo(40, 46, 16, 0xFFE8A0, .15)
                listOf(Triple(31.0, 46.0, 4.5), Triple(39.0, 39.0, 5.4), Triple(48.0, 48.0, 4.2), Triple(55.0, 55.0, 3.0)).forEach { (x, y, r) ->
                    body(star5(x, y, r), 0xFFE8A0)
                    glow(circle(x, y, r * .3), 0xFFFFFF)
                }
                glow(star4(25, 40, 1.8), 0xFFF6D0)
                glow(star4(52, 36, 1.6), 0xFFF6D0)
            }

            NodeLook.WORLDROOT_GROVE -> {
                body(svg("M19 63C20 28 60 28 61 63L53 63C52 40 28 40 27 63Z"), 0x6B4A2A)
                body(svg("M53 63C52 40 28 40 27 63L30 63C31 45 49 45 50 63Z"), 0x4E3420)
                line(svg("M23 50C26 36 40 33 46 34"), 0x8A6440, .9)
                halo(40, 38, 16, 0xB0D040, .12)
                listOf(24.0 to 46.0, 31.0 to 36.0, 41.0 to 33.5, 51.0 to 37.0, 57.0 to 47.0, 22.0 to 56.0).forEach { (x, y) -> glow(circle(x, y, 1.4), 0xB0D040) }
                body(oval(40, 63, 7, 2), 0x2A3A1E)
            }

            NodeLook.BIRCH_GROVE -> {
                trunk(40, 2.4, 34, 0xDCDCD2)
                listOf(Triple(38.4, 54.0, 3.0), Triple(40.6, 47.0, 2.4), Triple(38.8, 40.0, 2.6), Triple(40.4, 34.0, 2.0)).forEach { (x, y, w) ->
                    body(poly(x, y, x + w, y - .6, x + w, y + .6, x, y + 1), 0x2A2A2A)
                }
                body(oval(40, 24, 10, 14), 0x7AB050)
                body(oval(37, 20, 6, 8), 0x9ACC6A)
            }

            NodeLook.OAK_GROVE -> {
                trunk(40, 4.6, 22, 0x5A3A22)
                body(circle(30, 35, 10), 0x2F6A30)
                body(circle(50, 35, 10), 0x2F6A30)
                body(circle(40, 25, 12.5), 0x3F7A3A)
                body(circle(36, 21, 5.5), 0x5A9A4A)
                body(circle(26, 31, 3.5), 0x4A8A40)
            }

            NodeLook.YEW_GROVE -> {
                trunk(40, 2.6, 9, 0x6A3020)
                body(poly(25, 55, 55, 55, 40, 36), 0x1F4A30)
                body(poly(28, 45, 52, 45, 40, 26), 0x24563A)
                body(poly(31, 35, 49, 35, 40, 13), 0x2A6242)
                body(poly(25, 55, 40, 36, 40, 55), 0x2A5E3E)
                body(poly(31, 35, 40, 13, 40, 35), 0x367650)
            }

            NodeLook.IRONWOOD_GROVE -> {
                trunk(40, 3.4, 26, 0x6E7880)
                line(svg("M38.6 61L39.4 38"), 0xC8D0D8, .8)
                body(poly(24, 38, 30, 22, 42, 13, 54, 21, 57, 35, 46, 42, 32, 42), 0x46645A)
                body(poly(30, 22, 42, 13, 54, 21, 44, 28, 33, 29), 0x6A8A80)
                body(poly(46, 42, 57, 35, 54, 21, 44, 28), 0x36504A)
            }

            NodeLook.GHOST_ASH_GROVE -> {
                trunk(40, 2.2, 30, 0xA8C8D0)
                line(svg("M40 40L33 31M40 37L48 29"), 0xA8C8D0, 1.2)
                halo(40, 26, 18, 0xBFE3EF, .14)
                glow(oval(33, 28, 8, 5), 0xBFE3EF, .45)
                glow(oval(47, 25, 9, 6), 0xBFE3EF, .45)
                glow(oval(40, 18, 8, 6), 0xDDEEFF, .55)
            }

            NodeLook.ELDERHEART_GROVE -> {
                trunk(40, 4, 28, 0x4A2420)
                body(circle(31, 31, 9), 0x6A2420)
                body(circle(49, 31, 9), 0x6A2420)
                body(circle(40, 22, 11), 0x8A3A2A)
                body(circle(36, 18, 4.5), 0xA84A34)
                halo(40, 49, 8, 0xD9642E, .35)
                glow(poly(40, 53, 36.4, 49, 37.6, 46.4, 40, 47.6, 42.4, 46.4, 43.6, 49), 0xE8803E)
            }

            NodeLook.WORLDTREE_GROVE -> {
                body(poly(24, 64, 32, 60, 34, 64), 0x5A4028)
                body(poly(46, 64, 48, 60, 57, 64), 0x4A3420)
                trunk(40, 6, 30, 0x5A4028)
                body(oval(40, 24, 22, 12), 0x8A8A34)
                body(oval(30, 18, 11, 7), 0xB8A840)
                body(oval(50, 16, 12, 7), 0xB8A840)
                body(oval(40, 10, 11, 6.5), 0xD8C860)
                halo(40, 18, 15, 0xFFF0B0, .12)
                glow(star4(27, 14, 2.2), 0xFFF0B0)
                glow(star4(53, 9, 2), 0xFFF0B0)
                glow(star4(45, 26, 1.6), 0xFFF0B0)
            }
        }
    }

    private fun Sketch.gathered(look: NodeLook) {
        when (look) {
            NodeLook.COPPER_VEIN -> {
                rock(40, 16, 13, 0x4E4740)
                hole(oval(36, 54, 2.6, 1.5))
                hole(oval(45, 53, 2.2, 1.3))
                rubble(0x4E4740)
                body(circle(57, 66, 1.6), 0x9A6A3A)
            }

            NodeLook.IRON_VEIN -> {
                rock(31, 9, 16, 0x45403D)
                rock(51, 8, 12, 0x45403D)
                body(poly(24, 56, 36, 53, 36.5, 55, 24.5, 58), 0x6A3E2A)
                pebble(41, 67, 2.8, 0x45403D)
                pebble(58, 67, 2.2, 0x45403D)
            }

            NodeLook.TOPAZ_DEPOSIT -> {
                rock(40, 15, 9, 0x34343A)
                stub(32, 57, 3, 4.4, -.5, 0xB89A4A, 0x7E6428)
                stub(42, 55, 4, 5, .3, 0xB89A4A, 0x7E6428)
                stub(51, 58, 2.5, 3.6, .6, 0xB89A4A, 0x7E6428)
                glow(star4(58, 66, 1.4), 0xF2C94C, .8)
            }

            NodeLook.SILVER_VEIN -> {
                rock(40, 15, 13, 0x41464D)
                line(svg("M27 57L32 54M37 56L43 52"), 0x7E8890, 1.3)
                rubble(0x41464D)
            }

            NodeLook.SAPPHIRE_DEPOSIT -> {
                rock(40, 15, 9, 0x34343E)
                stub(33, 58, 3, 3, -1, 0x5070A0, 0x2A3C66)
                stub(41, 55, 5, 3.4, 0, 0x5070A0, 0x2A3C66)
                stub(48, 57, 3, 3, .6, 0x5070A0, 0x2A3C66)
                glow(star4(25, 66, 1.4), 0x6FA0F0, .8)
            }

            NodeLook.MITHRIL_VEIN -> {
                rock(40, 18, 22, 0x30363F)
                hole(MITHRIL_SEAM)
                pebble(58, 66, 2.4, 0x30363F)
                glow(circle(26, 67, .9), 0x8FD3FF, .5)
            }

            NodeLook.RUBY_DEPOSIT -> {
                rock(40, 15, 8, 0x332E2E)
                listOf(Triple(30.0, 59.0, -1.5), Triple(35.0, 57.0, -.6), Triple(42.0, 56.0, .2), Triple(48.0, 57.0, 1.0), Triple(53.0, 60.0, 1.6)).forEach { (x, y, l) ->
                    stub(x, y, 2.6, 2.8, l, 0x8A4048, 0x5A2028)
                }
                glow(star4(58, 67, 1.4), 0xF06070, .8)
            }

            NodeLook.ADAMANT_VEIN -> {
                rock(40, 16, 17, 0x2A2F2B)
                stub(33, 47, 2.5, 3, -.8, 0x3A6A54, 0x1E3A2E)
                stub(44, 44, 3, 3.6, .4, 0x3A6A54, 0x1E3A2E)
                rubble(0x2A2F2B)
            }

            NodeLook.STARMETAL_VEIN -> {
                body(oval(40, 62, 22, 7.5), 0x1E1A28)
                hole(oval(40, 61, 12, 4))
                pebble(30, 63, 3, 0x2A2638)
                pebble(52, 64, 3.4, 0x2A2638)
                glow(star4(40, 60, 1.6), 0x8A80B0, .7)
            }

            NodeLook.ONYX_DEPOSIT -> {
                rock(40, 14, 7, 0x33333A)
                stub(39, 59, 5, 6, 0, 0x3A3A44, 0x141418)
                line(svg("M33 56L39 58.1L45 56"), 0x8A8C90, .9)
                pebble(55, 66, 2.6, 0x22222A)
            }

            NodeLook.WORMWOOD_PATCH -> {
                repeat(13) { i ->
                    val x = 23 + i * 2.8
                    body(poly(x - 1.2, 64, x + 1.2, 64, x + .3, 64 - 3 - i % 3), if (i % 2 == 1) 0x7E8E70 else 0x6A7A60)
                }
                line(svg("M44 69L60 65"), 0x8FA48A, 1.3)
                line(svg("M40 71L56 68"), 0xA9BCA2, 1.1)
                body(circle(60, 64.6, 1.1), 0xB0A050)
            }

            NodeLook.CAVE_MUSHROOMS -> {
                cutStem(30, 62, 2.5)
                cutStem(39, 58, 3)
                cutStem(48, 61, 2.5)
                shroom(54, 65, 5, 4, 0x9A6E44)
                shroom(35, 67, 4, 3.6, 0x9A6E44)
            }

            NodeLook.BLOODWORT_PATCH -> {
                line(svg("M40 63L40 50M40 56L34 51M40 55L46 50"), 0x7A3A36, 1.4)
                leaf(40, 62, 6, -2.8, 2, 0x7A3030)
                leaf(52, 66, 7, -.3, 2.4, 0x6A2C2A)
                body(circle(46, 50, 1.2), 0xA83A38)
            }

            NodeLook.AMBER_TAPPING -> {
                body(poly(32, 63, 48, 63, 48, 30, 32, 30), 0x4E3420)
                body(poly(41, 63, 48, 63, 48, 30, 41, 30), 0x3E2A18)
                body(oval(40, 30, 8, 3), 0xA07A50)
                hole(poly(35, 41, 45, 41, 40, 47))
                line(svg("M40 47L40 50"), 0x8A5A2A, 1)
                body(poly(50, 64, 58, 61, 60, 66, 53, 68), 0x6A5C4E)
                hole(oval(55, 62.6, 3.8, 1.1))
            }

            NodeLook.SHADEBLOOM_PATCH -> {
                line(svg("M32 63Q31 56 30 52M42 63Q44 55 46 52M50 63Q51 58 53 56"), 0x33402F, 1.2)
                body(oval(30, 51, 1.6, 2.4), 0x4A3A6A)
                body(oval(46, 51, 1.6, 2.4), 0x4A3A6A)
                body(oval(53, 55, 1.4, 2), 0x4A3A6A)
                leaf(37, 62, 8, -2.7, 2.2, 0x2A3428)
            }

            NodeLook.GLOWCAP_GROTTO -> {
                rock(40, 20, 34, 0x262A2F)
                hole(oval(40, 53, 11, 10))
                listOf(35, 41).forEach { x -> line(svg("M$x 60L$x 57.5"), 0x5A8884, 1) }
                line(svg("M47 61L47 56"), 0x5A8884, .9)
                glow(svg("M45.6 56.6Q47 54.2 48.4 56.6Z"), 0x3A8A84, .9)
            }

            NodeLook.MOON_LOTUS_POND -> {
                pond(0x353B42, 0x142A40)
                body(oval(29, 63, 5, 2), 0x28583A)
                body(oval(51, 64, 6, 2.2), 0x28583A)
                body(oval(46, 59, 4, 1.5), 0x2E6040)
                line(oval(40, 61, 6, 1.8), 0x3A6A8A, .7)
                line(oval(40, 61, 10, 3), 0x2C5470, .6)
                body(poly(56, 66.5, 60, 65.5, 58, 67.6), 0x9A90B8)
            }

            NodeLook.VOID_ROOT_BED -> {
                body(oval(40, 62, 21, 7), 0x2A2030)
                hole(oval(40, 61, 11, 3.8))
                pebble(26, 63, 3, 0x3A2E44)
                pebble(55, 64, 3.6, 0x3A2E44)
                line(svg("M50 68Q54 66 57 68"), 0x4A3466, 1.6)
                glow(circle(57, 68, .9), 0x7A5AB0, .7)
            }

            NodeLook.STARBLOOM_PATCH -> {
                line(svg("M30 63Q29 58 27 55M40 63Q42 57 45 55M49 63Q50 59 52 58M55 64Q56 61 57 60"), 0x36502F, 1.1)
                leaf(36, 62, 7, -2.6, 2, 0x344E30)
                body(star5(57, 67, 2.4), 0x8A7A50)
            }

            NodeLook.WORLDROOT_GROVE -> {
                body(poly(18, 64, 27, 64, 29, 50, 23, 47), 0x5E4026)
                body(oval(26, 48.5, 3.6, 2), 0xC8A070)
                line(oval(26, 48.5, 1.8, .9), 0x8A6440, .6)
                body(poly(53, 64, 62, 64, 58, 47, 52, 50), 0x5E4026)
                body(oval(55, 48.5, 3.6, 2), 0xC8A070)
                line(oval(55, 48.5, 1.8, .9), 0x8A6440, .6)
                body(poly(33, 68, 46, 64, 47.5, 67, 34.5, 71), 0x6B4A2A)
                body(oval(47, 65.4, 1.6, 1.8), 0xC8A070)
                glow(circle(24, 56, 1), 0x6A7A30, .6)
            }

            NodeLook.BIRCH_GROVE -> {
                stump(40, 4, 6, 0xDCDCD2, 0xE8D8B0)
                body(poly(38, 60, 40, 59.6, 40, 60.6), 0x2A2A2A)
                chips(0xE8D8B0)
                line(svg("M48 69L60 64"), 0xC8C8BE, 1.6)
            }

            NodeLook.OAK_GROVE -> {
                stump(40, 8, 7, 0x5A3A22, 0xC09060)
                chips(0xC09060)
            }

            NodeLook.YEW_GROVE -> {
                stump(40, 4.4, 5, 0x6A3020, 0xB05A3A)
                chips(0xB05A3A)
                body(poly(47, 67, 60, 63, 57, 61), 0x1F4A30)
            }

            NodeLook.IRONWOOD_GROVE -> {
                stump(40, 5.4, 6, 0x6E7880, 0xA8B0B0)
                line(svg("M37 58L43 58"), 0x5A6268, .7)
                chips(0x8A9298)
                glow(star4(48, 55, 1.6), 0xE6EEF5, .8)
            }

            NodeLook.GHOST_ASH_GROVE -> {
                stump(40, 4, 6, 0xA8C8D0, 0xDDE8EC)
                glow(oval(40, 52, 6, 2.4), 0xBFE3EF, .3)
                glow(oval(44, 46, 4, 1.6), 0xBFE3EF, .2)
                chips(0xC8D8DE)
            }

            NodeLook.ELDERHEART_GROVE -> {
                stump(40, 6.4, 7, 0x4A2420, 0x7A4A30)
                glow(circle(40, 57, 1.8), 0xB04A1E, .9)
                halo(40, 57, 5, 0xD9642E, .18)
                chips(0x7A4A30)
            }

            NodeLook.WORLDTREE_GROVE -> {
                body(poly(24, 65, 32, 61, 34, 65), 0x5A4028)
                stump(40, 11, 7, 0x5A4028, 0xD0A868)
                glow(oval(40, 57, 6, 2.5), 0xE8C860, .45)
                line(oval(40, 57, 8.2, 3.4), 0xA07A40, .7)
                chips(0xD0A868)
            }
        }
    }

    /** Шов мифрила: светится в целой жиле и зияет в собранной. */
    private val MITHRIL_SEAM = poly(29, 56, 37, 46, 35, 44, 45, 36, 43, 43, 50, 41, 40, 52, 42, 54)
}

/** Сборка рисунка: части по порядку, от задней к передней. */
private class Sketch {
    val strokes = mutableListOf<NodeStroke>()

    fun body(path: List<PathOp>, color: Long) = add(StrokeKind.BODY, path, color)

    fun glow(path: List<PathOp>, color: Long, alpha: Double = 1.0) = add(StrokeKind.GLOW, path, color, alpha)

    fun line(path: List<PathOp>, color: Long, width: Number) = add(StrokeKind.LINE, path, color, width = width.toDouble())

    fun hole(path: List<PathOp>) = add(StrokeKind.HOLE, path, 0)

    fun halo(x: Number, y: Number, r: Number, color: Long, alpha: Double) = glow(circle(x, y, r), color, alpha)

    private fun add(kind: StrokeKind, path: List<PathOp>, color: Long, alpha: Double = 1.0, width: Double = 1.0) {
        strokes += NodeStroke(kind, path, 0xFF000000 or color, alpha.toFloat(), width.toFloat())
    }

    /** Глыба: тело, тёмная грань справа и светлый верх; [base] - линия её подошвы. */
    fun rock(cx: Number, w: Number, h: Number, color: Long, base: Double = 61.0) {
        val x = cx.toDouble()
        val wd = w.toDouble()
        val y = { f: Double -> base - h.toDouble() * f }
        body(poly(x - wd, base, x - wd - 2, y(.3), x - wd * .6, y(.75), x - wd * .1, y(1.0), x + wd * .5, y(.85), x + wd, y(.4), x + wd + 1, base - 1, x + wd * .4, base + 4, x - wd * .5, base + 4), color)
        body(poly(x + wd * .5, y(.85), x + wd, y(.4), x + wd + 1, base - 1, x + wd * .4, base + 4, x + wd * .2, y(.5)), NodeArt.shade(color, -.3))
        body(poly(x - wd * .6, y(.75), x - wd * .1, y(1.0), x + wd * .5, y(.85), x + wd * .2, y(.5), x - wd * .4, y(.48)), NodeArt.shade(color, .22))
    }

    /** Камешек с бликом. */
    fun pebble(x: Number, y: Number, r: Number, color: Long) {
        val (px, py, pr) = Triple(x.toDouble(), y.toDouble(), r.toDouble())
        body(poly(px - pr, py, px - pr * .6, py - pr * .9, px + pr * .5, py - pr, px + pr, py - pr * .2, px + pr * .4, py + pr * .4), color)
        body(poly(px - pr * .6, py - pr * .9, px + pr * .5, py - pr, px + pr * .2, py - pr * .4), NodeArt.shade(color, .25))
    }

    /** Крошка камня у собранной жилы. */
    fun rubble(color: Long) {
        pebble(25, 66, 3.2, color)
        pebble(55, 66, 3.6, color)
        pebble(60, 61, 2.4, color)
        pebble(30, 70, 2.2, color)
    }

    /** Кристалл-призма: светлая и тёмная грани с остриём. */
    fun prism(x: Number, y: Number, h: Number, w: Number, lean: Number, light: Long, dark: Long) {
        val f = Facets(x, y, h, w, lean)
        body(poly(f.bl, f.bm, f.tm, f.tip, f.tl), light)
        body(poly(f.bm, f.br, f.tr, f.tip, f.tm), dark)
    }

    /** Обломок призмы: две грани и срез сверху. */
    fun stub(x: Number, y: Number, h: Number, w: Number, lean: Number, light: Long, dark: Long) {
        val f = Facets(x, y, h, w, lean)
        body(poly(f.bl, f.bm, f.tm, f.tl), light)
        body(poly(f.bm, f.br, f.tr, f.tm), dark)
        body(poly(f.tl, f.tm, f.tr, f.back), NodeArt.shade(light, .35))
    }

    /** Гриб: ножка, шляпка и блик. */
    fun shroom(x: Number, y: Number, h: Number, r: Number, cap: Long, stem: Long = STEM) {
        val (px, py, ph) = Triple(x.toDouble(), y.toDouble(), h.toDouble())
        val pr = r.toDouble()
        body(poly(px - 1.5, py, px + 1.5, py, px + 1.1, py - ph, px - 1.1, py - ph), stem)
        body(svg("M${px - pr} ${py - ph + 1}C${px - pr} ${py - ph - pr * 1.1} ${px + pr} ${py - ph - pr * 1.1} ${px + pr} ${py - ph + 1}Z"), cap)
        body(oval(px - pr * .3, py - ph - pr * .45, pr * .35, pr * .18), NodeArt.shade(cap, .35))
    }

    /** Срезанная ножка гриба. */
    fun cutStem(x: Number, y: Number, h: Number, color: Long = STEM) {
        val (px, py, ph) = Triple(x.toDouble(), y.toDouble(), h.toDouble())
        body(poly(px - 1.5, py, px + 1.5, py, px + 1.3, py - ph, px - 1.3, py - ph), color)
        body(oval(px, py - ph, 1.4, .7), NodeArt.shade(color, .4))
    }

    /** Лист: две квадратичные дуги от [x], [y] длиной [len] под углом [angle] (радианы). */
    fun leaf(x: Number, y: Number, len: Number, angle: Double, w: Number, color: Long) {
        val (px, py) = x.toDouble() to y.toDouble()
        val tx = px + len.toDouble() * cos(angle)
        val ty = py + len.toDouble() * sin(angle)
        val nx = -sin(angle) * w.toDouble()
        val ny = cos(angle) * w.toDouble()
        val mx = (px + tx) / 2
        val my = (py + ty) / 2
        body(svg("M$px ${py}Q${mx + nx} ${my + ny} $tx ${ty}Q${mx - nx} ${my - ny} $px ${py}Z"), color)
    }

    /** Ствол с тёмной правой половиной. */
    fun trunk(x: Number, w: Number, h: Number, color: Long) {
        val (px, pw, ph) = Triple(x.toDouble(), w.toDouble(), h.toDouble())
        body(poly(px - pw - 2, 63, px + pw + 2, 63, px + pw * .75, 63 - ph, px - pw * .75, 63 - ph), color)
        body(poly(px + pw * .1, 63, px + pw + 2, 63, px + pw * .75, 63 - ph, px + pw * .05, 63 - ph), NodeArt.shade(color, -.28))
    }

    /** Пень: кора, тёмная половина, срез и годовое кольцо. */
    fun stump(x: Number, w: Number, h: Number, bark: Long, wood: Long) {
        val (px, pw, ph) = Triple(x.toDouble(), w.toDouble(), h.toDouble())
        body(poly(px - pw - 1.5, 64, px + pw + 1.5, 64, px + pw, 64 - ph, px - pw, 64 - ph), bark)
        body(poly(px + pw * .1, 64, px + pw + 1.5, 64, px + pw, 64 - ph, px + pw * .1, 64 - ph), NodeArt.shade(bark, -.3))
        body(oval(px, 64 - ph, pw, pw * .42), wood)
        line(oval(px, 64 - ph, pw * .55, pw * .22), NodeArt.shade(wood, -.3), .7)
    }

    /** Щепа у пня. */
    fun chips(color: Long) {
        body(poly(26, 66, 31, 64.5, 32, 66, 27, 67.5), color)
        body(poly(50, 68, 55, 67, 55.5, 68.4, 50.5, 69.4), color)
        body(poly(56, 62, 59, 60.5, 60, 61.6, 57, 63), NodeArt.shade(color, -.15))
    }

    /** Пруд: берег и тёмная вода. */
    fun pond(shore: Long, water: Long) {
        body(oval(40, 62, 23, 8.5), shore)
        body(oval(40, 62, 20, 7), water)
    }

    private companion object {
        const val STEM = 0xD8C8A8L
    }
}

/** Углы призмы и обломка: низ слева, посередине и справа, верх, остриё и задний край среза. */
private class Facets(x: Number, y: Number, h: Number, w: Number, lean: Number) {
    private val px = x.toDouble()
    private val py = y.toDouble()
    private val ph = h.toDouble()
    private val pw = w.toDouble()
    private val pl = lean.toDouble()
    val bl = px - pw to py
    val bm = px to py + pw * .35
    val br = px + pw to py
    val tl = px - pw + pl to py - ph
    val tm = px + pl to py - ph + pw * .35
    val tr = px + pw + pl to py - ph
    val tip = px + pl to py - ph - pw * 1.5
    val back = px + pl to py - ph - pw * .35
}

/** Замкнутый многоугольник по парам координат. */
private fun poly(vararg xy: Number): List<PathOp> = xy.toList().chunked(2) { (x, y) -> x.toFloat() to y.toFloat() }.let(::closed)

/** Замкнутый многоугольник по точкам. */
private fun poly(vararg points: Pair<Double, Double>): List<PathOp> = closed(points.map { (x, y) -> x.toFloat() to y.toFloat() })

private fun closed(points: List<Pair<Float, Float>>): List<PathOp> = points.mapIndexed { i, (x, y) -> if (i == 0) PathOp.Move(x, y) else PathOp.Line(x, y) } + PathOp.Close

/** Эллипс четырьмя кубическими дугами. */
private fun oval(cx: Number, cy: Number, rx: Number, ry: Number): List<PathOp> {
    val (x, y) = cx.toFloat() to cy.toFloat()
    val (a, b) = rx.toFloat() to ry.toFloat()
    val k = .5523f
    return listOf(
        PathOp.Move(x - a, y),
        PathOp.Cubic(x - a, y - b * k, x - a * k, y - b, x, y - b),
        PathOp.Cubic(x + a * k, y - b, x + a, y - b * k, x + a, y),
        PathOp.Cubic(x + a, y + b * k, x + a * k, y + b, x, y + b),
        PathOp.Cubic(x - a * k, y + b, x - a, y + b * k, x - a, y),
        PathOp.Close,
    )
}

private fun circle(x: Number, y: Number, r: Number): List<PathOp> = oval(x, y, r, r)

/** Четырёхлучевая искра. */
private fun star4(x: Number, y: Number, r: Number): List<PathOp> {
    val (px, py, pr) = Triple(x.toDouble(), y.toDouble(), r.toDouble())
    val q = pr * .25
    return poly(px, py - pr, px + q, py - q, px + pr, py, px + q, py + q, px, py + pr, px - q, py + q, px - pr, py, px - q, py - q)
}

/** Пятилучевая звезда. */
private fun star5(x: Number, y: Number, r: Number): List<PathOp> = closed(
    List(10) { i ->
        val a = -PI / 2 + i * PI / 5
        val q = if (i % 2 == 1) r.toDouble() * .45 else r.toDouble()
        (x.toDouble() + q * cos(a)).toFloat() to (y.toDouble() + q * sin(a)).toFloat()
    },
)

/** Контур из строки пути SVG: абсолютные команды M, L, Q, C и Z. */
private fun svg(d: String): List<PathOp> {
    val tokens = Regex("[MLQCZ]|-?\\d+(?:\\.\\d+)?(?:E-?\\d+)?").findAll(d).map { it.value }.toList()
    val ops = mutableListOf<PathOp>()
    var i = 0
    var command = 'M'
    fun next(): Float = tokens[i++].toFloat()
    while (i < tokens.size) {
        if (tokens[i][0].isLetter()) command = tokens[i++][0]
        ops += when (command) {
            'M' -> PathOp.Move(next(), next()).also { command = 'L' }
            'L' -> PathOp.Line(next(), next())
            'Q' -> PathOp.Quad(next(), next(), next(), next())
            'C' -> PathOp.Cubic(next(), next(), next(), next(), next(), next())
            else -> PathOp.Close
        }
    }
    return ops
}
