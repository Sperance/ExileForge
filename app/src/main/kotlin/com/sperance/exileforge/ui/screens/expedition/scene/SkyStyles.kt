package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max
import kotlin.math.sin

/** VI · Sky glass (3.43.0): ground fused to glass with the sky in it, pillars of crystal, light falling like rain. */
internal class SkyGlass : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.85f + noise(spot.x, spot.y) * .3f) * light)
        pen.diamond(cx, cy, u, u / 2)
        // The sky caught in the glass: one pale streak across the tile, drifting with the clouds.
        val glint = .25f + .2f * sin(time * .8f + spot.x * .7f + spot.y * .3f)
        pen.color = tone(palette.accent, light, alpha = glint * .35f)
        pen.line(cx - u * .5f, cy - u * .05f, cx + u * .1f, cy + u * .25f, u * .06f)
        pen.color = Color.Black.copy(alpha = .25f)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .02f)
        // Now and then a crack full of light.
        if (noise(spot.x, spot.y, 50) < .08f) {
            pen.color = palette.accent.copy(alpha = (.4f + .3f * sin(time * 2.5f + spot.x)) * max(.3f, light))
            pen.polyline(cx - u * .4f, cy - u * .1f, cx - u * .1f, cy + u * .05f, cx + u * .05f, cy - u * .12f, cx + u * .4f, cy + u * .08f, width = u * .03f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.6f + noise(spot.x, spot.y, 51) * 1.1f)
        block(
            spot,
            h,
            tone(palette.wallSide, 1.1f * light, alpha = alpha),
            tone(palette.wallSide, .75f * light, alpha = alpha),
            tone(palette.wallTop, 1.15f * light, alpha = alpha),
        )
        // A facet: a bright diagonal down the left face, as on cut crystal.
        pen.color = tone(palette.accent, light, alpha = .3f * alpha)
        seam({ t, lift -> left(spot, t, lift) }, unit * .05f, .15f, h * .9f, .85f, h * .2f)
        when (variant(spot)) {
            // A shard growing from the cap.
            0 -> {
                pen.color = tone(palette.accent, .9f * light, alpha = .8f * alpha)
                pen.triangle(spot.cx - unit * .25f, spot.cy + h, spot.cx + unit * .2f, spot.cy + h, spot.cx - unit * .05f, spot.cy + h + unit * 1.1f)
            }

            // Lightning sleeping in the rock.
            1 -> if (noise(spot.x, spot.y, 52) < .4f) {
                pen.color = palette.accent.copy(alpha = (.3f + .4f * sin(time * 7f + spot.y)).coerceAtLeast(0f) * alpha)
                seam({ t, lift -> right(spot, t, lift) }, unit * .03f, .3f, h * .85f, .55f, h * .6f, .4f, h * .45f, .65f, h * .15f)
            }

            else -> frontEdges(spot, h, tone(palette.accent, light, alpha = .45f * alpha), unit * .04f)
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        scope.drawRect(Brush.verticalGradient(listOf(palette.accent.copy(alpha = .08f), Color.Transparent), startY = 0f, endY = h * .5f))
        repeat(24) { i ->
            val x = spread(i, 60) * w
            val fall = h * (.04f + spread(i, 61) * .05f)
            val y = (time * h * (.25f + spread(i, 62) * .2f) + spread(i, 63) * h * 2) % (h * 1.2f)
            scope.drawLine(palette.accent.copy(alpha = .18f + .18f * spread(i, 64)), Offset(x, y), Offset(x - fall * .3f, y + fall), strokeWidth = 1.2f)
        }
    }
}

/** VII · Drowned (3.43.0): mosaic under water with light rippling over it, columns grown over with coral, bubbles rising. */
internal class Drowned : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.85f + noise(spot.x, spot.y) * .25f) * light)
        pen.diamond(cx, cy, u, u / 2)
        // The empire's mosaic: a smaller tile set in every other one.
        if ((spot.x + spot.y) % 2 == 0) {
            pen.color = tone(palette.decor, .8f * light, alpha = .35f)
            pen.diamond(cx, cy, u * .45f, u * .22f)
        }
        // Light through the water, swaying.
        val sway = sin(time * 1.3f + spot.x * .9f + spot.y * .6f)
        pen.color = tone(palette.accent, light, alpha = .1f + .08f * sway)
        pen.ellipse(cx - u * .45f + sway * u * .1f, cy - u * .12f, u * .7f, u * .2f)
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.4f + noise(spot.x, spot.y, 53) * .9f)
        block(
            spot,
            h,
            tone(palette.wallSide, .95f * light, alpha = alpha),
            tone(palette.wallSide, .65f * light, alpha = alpha),
            tone(palette.wallTop, 1f * light, alpha = alpha),
        )
        when (variant(spot)) {
            // Coral grown over the cap and the edge.
            0 -> {
                pen.color = tone(palette.decor, 1.1f * light, alpha = .85f * alpha)
                pen.circle(spot.cx - unit * .3f, spot.cy + h + unit * .1f, unit * .22f)
                pen.circle(spot.cx + unit * .1f, spot.cy + h + unit * .18f, unit * .3f)
                pen.circle(spot.cx + unit * .45f, spot.cy + h + unit * .02f, unit * .18f)
            }

            // Kelp hanging down the right face.
            1 -> {
                pen.color = tone(Color(0xFF3A7A4A), light, alpha = .8f * alpha)
                val right = { t: Float, z: Float -> right(spot, t, z) }
                seam(right, unit * .05f, .3f, h * .95f, .35f, h * .6f, .28f, h * .3f)
                seam(right, unit * .05f, .7f, h * .95f, .64f, h * .55f, .72f, h * .2f)
            }

            // Barnacles along the left face.
            else -> {
                pen.color = tone(palette.wallTop, 1.3f * light, alpha = .7f * alpha)
                listOf(.2f to .3f, .45f to .55f, .7f to .25f, .35f to .8f).forEach { (t, z) ->
                    val p = left(spot, t, h * z)
                    pen.circle(p[0], p[1], unit * .07f)
                }
            }
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        scope.drawRect(Brush.verticalGradient(listOf(Color.Transparent, palette.accent.copy(alpha = .07f)), startY = h * .3f, endY = h))
        repeat(40) { i ->
            val rise = (time * (.05f + spread(i, 70) * .06f) + spread(i, 71)) % 1f
            val x = spread(i, 72) * w + sin(time * 2f + i) * w * .01f
            val y = h * (1.1f - rise * 1.2f)
            val r = w / 300f * (1f + spread(i, 73) * 2f)
            scope.drawCircle(palette.accent.copy(alpha = .3f), r, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
        }
    }
}

/** VIII · Divine (3.43.0): marble with gold set in it, fluted pillars capped in gold, motes of starlight drifting. */
internal class Divine : MapStyle() {
    private val gold = Color(0xFFE8C860)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.9f + noise(spot.x, spot.y) * .2f) * light)
        pen.diamond(cx, cy, u, u / 2)
        // Veins of the marble.
        pen.color = tone(palette.wallTop, 1.2f * light, alpha = .25f)
        pen.line(cx - u * .6f, cy + u * (noise(spot.x, spot.y, 54) - .5f) * .3f, cx + u * .5f, cy + u * (noise(spot.x, spot.y, 55) - .5f) * .3f, u * .02f)
        // Gold inlay: a star on some tiles, the seams on all.
        pen.color = gold.copy(alpha = .25f * light)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .025f)
        if (noise(spot.x, spot.y, 56) < .07f) {
            pen.color = gold.copy(alpha = (.5f + .25f * sin(time * 1.5f + spot.x)) * max(.3f, light))
            pen.line(cx - u * .3f, cy, cx + u * .3f, cy, u * .04f)
            pen.line(cx, cy - u * .15f, cx, cy + u * .15f, u * .04f)
            pen.line(cx - u * .18f, cy - u * .09f, cx + u * .18f, cy + u * .09f, u * .03f)
            pen.line(cx + u * .18f, cy - u * .09f, cx - u * .18f, cy + u * .09f, u * .03f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.9f + noise(spot.x, spot.y, 57) * .8f)
        block(
            spot,
            h,
            tone(palette.wallSide, 1.05f * light, alpha = alpha),
            tone(palette.wallSide, .75f * light, alpha = alpha),
            tone(palette.wallTop, 1.1f * light, alpha = alpha),
        )
        // Fluting: the vertical grooves of a column.
        pen.color = Color.Black.copy(alpha = .2f * alpha)
        listOf(.25f, .5f, .75f).forEach { t ->
            seam({ tt, lift -> left(spot, tt, lift) }, unit * .03f, t, h * .1f, t, h * .9f)
            seam({ tt, lift -> right(spot, tt, lift) }, unit * .03f, t, h * .1f, t, h * .9f)
        }
        // A gold cap on every column, and a gold band on some.
        frontEdges(spot, h, gold.copy(alpha = .7f * alpha * max(.4f, light)), unit * .06f)
        if (variant(spot) == 1) {
            pen.color = gold.copy(alpha = .5f * alpha)
            seam({ t, lift -> left(spot, t, lift) }, unit * .05f, 0f, h * .55f, 1f, h * .55f)
            seam({ t, lift -> right(spot, t, lift) }, unit * .05f, 0f, h * .55f, 1f, h * .55f)
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        repeat(50) { i ->
            val x = (spread(i, 80) * w + time * w * (.005f + spread(i, 81) * .01f)) % w
            val y = (spread(i, 82) * h + sin(time * .7f + i) * h * .02f)
            val twinkle = .2f + .3f * (sin(time * 2f + i * 1.7f) * .5f + .5f)
            scope.drawCircle(gold.copy(alpha = twinkle), w / 400f * (1f + spread(i, 83)), Offset(x, y))
        }
    }
}
