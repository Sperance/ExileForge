package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max
import kotlin.math.sin

/** I · Wet stone: flagstones split by mortar, puddles holding the torch, walls laid in courses, drips falling. */
internal class WetStone : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.95f + noise(spot.x, spot.y) * .3f) * light)
        pen.diamond(cx, cy, u, u / 2)
        pen.color = Color.Black.copy(alpha = .3f)
        pen.line(cx - u / 2, cy + u / 4, cx + u / 2, cy - u / 4, u * .03f)
        pen.color = Color.Black.copy(alpha = .45f)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .025f)
        if (spot.walls == 0 && noise(spot.x, spot.y, 3) < .12f) {
            val w = u * .55f * (.7f + noise(spot.x, spot.y, 4) * .5f)
            pen.color = tone(palette.accent, .3f * light, alpha = .7f)
            pen.ellipse(cx - w, cy - w / 2, w * 2, w)
            pen.color = Palettes.torch.copy(alpha = light * (.3f + .2f * sin(time * 3f + spot.x)))
            pen.ellipse(cx - w * .55f, cy, w * .7f, w * .2f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * 1.5f
        val grain = .9f + noise(spot.x, spot.y, 2) * .25f
        block(
            spot,
            h,
            tone(palette.wallSide, 1.15f * grain * light, alpha = alpha),
            tone(palette.wallSide, .75f * grain * light, alpha = alpha),
            tone(palette.wallTop, 1.1f * grain * light, alpha = alpha),
        )
        pen.color = Color.Black.copy(alpha = .45f * alpha)
        val joint = unit * .03f
        when (variant(spot)) {
            // Courses laid level.
            0 -> for (i in 1..2) {
                val lift = h * i / 3f
                pen.polyline(spot.cx - unit, spot.cy + lift, spot.cx, spot.cy - unit / 2 + lift, spot.cx + unit, spot.cy + lift, width = joint)
            }

            // Ashlar: four courses with their joints staggered.
            1 -> for (i in 1..3) {
                val lift = h * i / 4f
                pen.polyline(spot.cx - unit, spot.cy + lift, spot.cx, spot.cy - unit / 2 + lift, spot.cx + unit, spot.cy + lift, width = joint)
                val shift = if (i % 2 == 0) .25f else .6f
                for (face in listOf<(Float, Float) -> FloatArray>({ t, z -> left(spot, t, z) }, { t, z -> right(spot, t, z) })) {
                    seam(face, joint, shift, lift - h / 4f, shift, lift)
                }
            }

            // Rubble: odd stones pressed into mortar, a green stain of damp at the foot.
            else -> {
                repeat(4) { k ->
                    val t = .15f + noise(spot.x, spot.y, 40 + k) * .7f
                    val z = h * (.2f + noise(spot.x, spot.y, 44 + k) * .6f)
                    val (x, y) = (if (k % 2 == 0) left(spot, t, z) else right(spot, t, z)).let { it[0] to it[1] }
                    pen.color = tone(palette.wallSide, (if (k % 2 == 0) .85f else .55f) * light, alpha = alpha)
                    pen.ellipse(x - unit * .16f, y - unit * .09f, unit * .32f, unit * .18f)
                    pen.color = Color.Black.copy(alpha = .4f * alpha)
                    pen.line(x - unit * .16f, y - unit * .09f, x + unit * .14f, y - unit * .09f, joint)
                }
                pen.color = Color(0xFF4F6B45).copy(alpha = .35f * light * alpha)
                seam({ t, z -> left(spot, t, z) }, unit * .09f, 0f, unit * .06f, .5f, unit * .1f, 1f, unit * .05f)
            }
        }
        // The wet shine along the cap.
        frontEdges(spot, h, Color(0xFFBED2E1).copy(alpha = .3f * light * alpha), unit * .04f)
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        val drop = h / 90f
        repeat(14) { i ->
            val x = spread(i, 1) * w
            val y = (time * h / 6f + spread(i, 2) * h * 3) % (h * 1.2f)
            scope.drawLine(palette.accent.copy(alpha = .3f), Offset(x, y), Offset(x, y + drop), strokeWidth = 1.5f)
        }
    }
}

/** II · Rune dark: near-black ground where runes breathe, obsidian obelisks with glowing seams, crawling fog. */
internal class RuneDark : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.7f + noise(spot.x, spot.y) * .3f) * light)
        pen.diamond(cx, cy, u, u / 2)
        pen.color = palette.accent.copy(alpha = .08f * light)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .02f)
        if (noise(spot.x, spot.y, 5) >= .1f) return
        val a = (.35f + .35f * sin(time * 2f + noise(spot.x, spot.y, 6) * 6f)) * max(.3f, light)
        for ((glow, stroke) in listOf(.3f to .12f, 1f to .04f)) {
            pen.color = palette.accent.copy(alpha = a * glow)
            pen.ring(cx - u * .42f, cy - u * .21f, u * .84f, u * .42f, u * stroke)
            val w = u * stroke
            when ((noise(spot.x, spot.y, 7) * 3).toInt()) {
                0 -> {
                    pen.line(cx, cy - u * .15f, cx, cy + u * .15f, w)
                    pen.line(cx - u * .2f, cy - u * .05f, cx + u * .2f, cy + u * .05f, w)
                }

                1 -> pen.polyline(cx - u * .25f, cy - u * .1f, cx, cy + u * .15f, cx + u * .25f, cy - u * .1f, width = w)

                else -> {
                    pen.line(cx - u * .2f, cy - u * .1f, cx + u * .2f, cy + u * .1f, w)
                    pen.line(cx + u * .2f, cy - u * .1f, cx - u * .2f, cy + u * .1f, w)
                }
            }
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.7f + noise(spot.x, spot.y, 8) * .9f)
        block(
            spot,
            h,
            tone(palette.wallSide, .8f * light, alpha = alpha),
            tone(palette.wallSide, .55f * light, alpha = alpha),
            tone(palette.wallTop, .9f * light, alpha = alpha),
        )
        val a = (.3f + .3f * sin(time * 1.6f + spot.x + spot.y)) * max(.3f, light) * alpha
        when (variant(spot)) {
            // An obelisk whose edge seam glows.
            0 -> if (noise(spot.x, spot.y, 9) < .3f) {
                for ((glow, stroke) in listOf(.3f to .14f, 1f to .04f)) {
                    pen.color = palette.accent.copy(alpha = a * glow)
                    pen.line(spot.cx, spot.cy - unit / 2 + unit * .1f, spot.cx, spot.cy - unit / 2 + h - unit * .1f, unit * stroke)
                }
            }

            // A carved panel: a rune cut in the right face, breathing.
            1 -> for ((glow, stroke) in listOf(.3f to .12f, 1f to .035f)) {
                pen.color = palette.accent.copy(alpha = a * glow * .8f)
                val right = { t: Float, z: Float -> right(spot, t, z) }
                seam(right, unit * stroke, .5f, h * .25f, .5f, h * .75f)
                seam(right, unit * stroke, .3f, h * .6f, .5f, h * .45f, .7f, h * .6f)
            }

            // Cracked obsidian: a split down the left face, a sliver of light in it.
            else -> {
                pen.color = Color.Black.copy(alpha = .6f * alpha)
                val left = { t: Float, z: Float -> left(spot, t, z) }
                seam(left, unit * .05f, .55f, h * .95f, .4f, h * .65f, .6f, h * .4f, .45f, h * .1f)
                pen.color = palette.accent.copy(alpha = a * .5f)
                seam(left, unit * .015f, .55f, h * .95f, .4f, h * .65f, .6f, h * .4f, .45f, h * .1f)
            }
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        repeat(6) { i ->
            val x = (spread(i, 3) * w * 1.6f + time * w * (.02f + i * .008f)) % (w * 1.6f) - w * .3f
            val y = h * (.25f + spread(i, 4) * .6f)
            val r = w * (.25f + spread(i, 5) * .2f)
            scope.drawCircle(Brush.radialGradient(listOf(Color(0xFF78A0B4).copy(alpha = .1f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
    }
}

/** III · Ash and embers: burnt ground with lava pulsing in its cracks, charred rock with a smouldering rim, sparks rising. */
internal class Ashen : MapStyle() {
    private val lava = Color(0xFFFF6A20)
    private val core = Color(0xFFFF8A3C)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.85f + noise(spot.x, spot.y) * .35f) * light)
        pen.diamond(cx, cy, u, u / 2)
        if (noise(spot.x, spot.y, 10) >= .35f) return
        val a = (.45f + .35f * sin(time * 1.3f + noise(spot.x, spot.y, 11) * 9f)) * max(.35f, light)
        val crack = floatArrayOf(
            cx - u * .6f,
            cy + (noise(spot.x, spot.y, 12) - .5f) * u * .15f,
            cx - u * .1f,
            cy + u * .06f,
            cx + u * .15f,
            cy - u * .05f,
            cx + u * .6f,
            cy + (noise(spot.x, spot.y, 13) - .3f) * u * .15f,
        )
        pen.color = lava.copy(alpha = a * .3f)
        pen.polyline(*crack, width = u * .12f)
        pen.color = core.copy(alpha = a)
        pen.polyline(*crack, width = u * .035f)
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.2f + noise(spot.x, spot.y, 14) * .6f)
        block(
            spot,
            h,
            tone(palette.wallSide, 1.1f * light, alpha = alpha),
            tone(palette.wallSide, .75f * light, alpha = alpha),
            tone(palette.wallTop, 1.05f * light, alpha = alpha),
        )
        val ember = ((.35f + .3f * sin(time * 2f + spot.x * 3)) * light * alpha).coerceIn(0f, 1f)
        frontEdges(spot, h, core.copy(alpha = ember), unit * .05f)
        when (variant(spot)) {
            // Plain charred rock.
            0 -> Unit

            // Basalt: columns split top to bottom.
            1 -> {
                pen.color = Color.Black.copy(alpha = .45f * alpha)
                for (t in listOf(.33f, .66f)) {
                    seam({ a, z -> left(spot, a, z) }, unit * .03f, t, 0f, t, h)
                    seam({ a, z -> right(spot, a, z) }, unit * .03f, t, 0f, t, h)
                }
            }

            // Burning through: a glowing fissure down the left face.
            else -> {
                val left = { t: Float, z: Float -> left(spot, t, z) }
                pen.color = lava.copy(alpha = ember * .35f)
                seam(left, unit * .12f, .3f, h * .9f, .55f, h * .55f, .4f, h * .15f)
                pen.color = core.copy(alpha = ember)
                seam(left, unit * .035f, .3f, h * .9f, .55f, h * .55f, .4f, h * .15f)
            }
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        scope.drawRect(Brush.verticalGradient(listOf(Color.Transparent, lava.copy(alpha = .1f)), startY = h * .5f, endY = h))
        val spark = w / 180f
        repeat(40) { i ->
            val life = (time * .25f + spread(i, 6)) % 1f
            val x = spread(i, 7) * w + sin(time + i) * spark * 6
            scope.drawCircle(Color(1f, (120 + 80 * spread(i, 8)) / 255f, 60 / 255f, (1 - life) * .8f), spark, Offset(x, h * (1.05f - life * 1.1f)))
        }
    }
}
