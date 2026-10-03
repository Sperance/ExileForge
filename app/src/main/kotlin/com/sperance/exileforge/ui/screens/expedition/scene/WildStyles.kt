package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max
import kotlin.math.sin

/** V · Moss and roots: a living floor with grass swaying, rounded boulders with roots and moss, soft shade by the rock, fireflies. */
internal class Overgrown : MapStyle() {
    private val grass = Color(0xFF6F9E4C)
    private val root = Color(0xFF4A3624)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.9f + noise(spot.x, spot.y) * .3f) * light)
        pen.diamond(cx, cy, u, u / 2)
        if (spot.walls > 0) {
            pen.color = Color.Black.copy(alpha = .1f * spot.walls)
            pen.diamond(cx, cy, u, u / 2)
        }
        if (noise(spot.x, spot.y, 16) >= .35f) return
        pen.color = tone(grass, 1.1f * light)
        repeat(4) { k ->
            val bx = cx + (noise(spot.x, spot.y, 17 + k) - .5f) * u
            val by = cy + (noise(spot.x, spot.y, 21 + k) - .5f) * u * .4f
            val sway = sin(time * 2f + bx * .05f) * u * .06f
            pen.polyline(bx, by, bx + sway * .5f, by + u * .2f, bx + sway, by + u * (.35f + .2f * noise(spot.x, spot.y, 25 + k)), width = u * .04f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val (cx, cy) = spot.cx to spot.cy
        val r = unit * (.95f + noise(spot.x, spot.y, 31) * .3f)
        val h = unit * (.9f + noise(spot.x, spot.y, 32) * .5f)
        val half = h * .75f + r * .2f
        pen.color = Color.Black.copy(alpha = .35f * alpha)
        pen.ellipse(cx - r * 1.05f, cy - r * .5f, r * 2.1f, r)
        pen.color = tone(palette.wallSide, light, alpha = alpha)
        pen.ellipse(cx - r, cy + h * .45f - half, r * 2, half * 2)
        pen.color = tone(palette.wallTop, 1.2f * light, alpha = alpha * .8f)
        pen.ellipse(cx - r * .75f, cy + h * .7f, r * 1.1f, h * .6f)
        pen.color = tone(palette.decor, .95f * light, alpha = alpha)
        pen.ellipse(cx - r * .8f, cy + h * .95f, r * 1.4f, r * .5f)
        when (variant(spot)) {
            // A boulder, now and then with a root over it.
            0 -> if (noise(spot.x, spot.y, 33) < .4f) {
                pen.color = tone(root, light, alpha = alpha)
                pen.polyline(cx + r * .3f, cy + h * .7f, cx + r * .6f, cy + h * .3f, cx + r * .4f, cy + h * .05f, cx + r * .7f, cy - unit * .1f, width = unit * .05f)
            }

            // A second, smaller stone settled on the first, its own moss cap.
            1 -> {
                val small = r * .55f
                pen.color = tone(palette.wallSide, .9f * light, alpha = alpha)
                pen.ellipse(cx - small * .7f, cy + h * .95f, small * 2, small * 1.1f)
                pen.color = tone(palette.decor, 1.05f * light, alpha = alpha)
                pen.ellipse(cx - small * .5f, cy + h * .95f + small * .6f, small * 1.4f, small * .45f)
            }

            // Ferns spilling from the moss.
            else -> {
                pen.color = tone(grass, light, alpha = alpha)
                repeat(3) { k ->
                    val bx = cx + (k - 1) * r * .35f
                    val by = cy + h * 1.1f
                    val lean = (k - 1) * r * .25f + sin(time * 1.5f + k) * unit * .03f
                    pen.polyline(bx, by, bx + lean * .5f, by + unit * .25f, bx + lean, by + unit * .4f, width = unit * .05f)
                }
            }
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        val fly = Color(0xFFD2FF8C)
        val dot = w / 220f
        repeat(18) { i ->
            val at = Offset(w * spread(i, 40) + sin(time * .6f + i) * dot * 18, h * spread(i, 41) + sin(time * .5f + i * 2 + 1.6f) * dot * 12)
            val a = .4f + .4f * sin(time * 3f + i)
            scope.drawCircle(fly.copy(alpha = a * .2f), dot * 4, at)
            scope.drawCircle(fly.copy(alpha = a), dot, at)
        }
    }
}

/**
 * VI · Blood altar, the Vaal zones' own (2.65.0, the owner's mockup I): black obsidian where scarlet
 * veins pulse in a wave, pools of blood in the hollows, stepped blocks weeping blood, red haze.
 */
internal class BloodAltar : MapStyle() {
    private val vein = Color(0xFFFF3C28)
    private val blood = Color(0xFF5A0406)
    private val weep = Color(0xFFAA0C0C)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.85f + noise(spot.x, spot.y) * .35f) * light)
        pen.diamond(cx, cy, u, u / 2)
        pen.color = Color.Black.copy(alpha = .5f)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .025f)
        if (noise(spot.x, spot.y, 3) < .38f) {
            // The veins pulse as one wave rolling across the floor, not each on its own.
            val a = (.4f + .4f * sin(time * 2.2f - (spot.x + spot.y) * .6f)) * max(.4f, light)
            val bend = (noise(spot.x, spot.y, 5) - .5f) * u * .3f
            val across = noise(spot.x, spot.y, 4) < .5f
            val line = if (across) {
                floatArrayOf(cx - u / 2, cy + u / 4, cx + bend, cy, cx + u / 2, cy - u / 4)
            } else {
                floatArrayOf(cx - u / 2, cy - u / 4, cx, cy + bend * .6f, cx + u / 2, cy + u / 4)
            }
            pen.color = vein.copy(alpha = a * .3f)
            pen.polyline(*line, width = u * .12f)
            pen.color = vein.copy(alpha = a)
            pen.polyline(*line, width = u * .035f)
        }
        if (spot.walls == 0 && noise(spot.x, spot.y, 6) < .07f) {
            val w = u * .5f
            pen.color = tone(blood, light.coerceAtLeast(.5f), alpha = .9f)
            pen.ellipse(cx - w, cy - w / 2, w * 2, w)
            pen.color = Color(0xFFFF5A46).copy(alpha = (.3f + .15f * sin(time * 3f + spot.x)) * light)
            pen.ellipse(cx - w * .6f, cy, w * .6f, w * .2f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.3f + noise(spot.x, spot.y, 8) * .5f)
        block(spot, h, tone(palette.wallSide, light, alpha = alpha), tone(palette.wallSide, .65f * light, alpha = alpha), tone(palette.wallTop, light, alpha = alpha))
        val kind = variant(spot)
        // Its teeth: a crown of spikes instead of a step.
        if (kind == 2) {
            pen.color = tone(palette.wallTop, 1.2f * light, alpha = alpha)
            for (t in listOf(.25f, .75f)) {
                val (lx, ly) = left(spot, t, h).let { it[0] to it[1] }
                pen.triangle(lx - unit * .14f, ly, lx, ly + unit * .45f, lx + unit * .14f, ly)
                val (rx, ry) = right(spot, t, h).let { it[0] to it[1] }
                pen.triangle(rx - unit * .14f, ry, rx, ry + unit * .45f, rx + unit * .14f, ry)
            }
            return@with
        }
        // A band of carved glyphs round the block, lit by the veins.
        if (kind == 1) {
            pen.color = vein.copy(alpha = ((.35f + .25f * sin(time * 2.2f - (spot.x + spot.y) * .6f)) * light * alpha).coerceIn(0f, 1f))
            for (face in listOf<(Float, Float) -> FloatArray>({ t, z -> left(spot, t, z) }, { t, z -> right(spot, t, z) })) {
                seam(face, unit * .03f, 0f, h * .55f, .2f, h * .65f, .4f, h * .55f, .6f, h * .65f, .8f, h * .55f, 1f, h * .65f)
            }
        }
        // A second, narrower step on top: the altar's terraces.
        val step = TileSpot(spot.x, spot.y, spot.cx, spot.cy + h, spot.walls)
        val inset = unit * .3f
        pen.color = tone(palette.wallSide, 1.1f * light, alpha = alpha)
        pen.quad(step.cx - unit + inset, step.cy, step.cx, step.cy - unit / 2 + inset / 2, step.cx, step.cy - unit / 2 + inset / 2 + unit * .35f, step.cx - unit + inset, step.cy + unit * .35f)
        pen.color = tone(palette.wallSide, .7f * light, alpha = alpha)
        pen.quad(step.cx, step.cy - unit / 2 + inset / 2, step.cx + unit - inset, step.cy, step.cx + unit - inset, step.cy + unit * .35f, step.cx, step.cy - unit / 2 + inset / 2 + unit * .35f)
        pen.color = tone(palette.wallTop, 1.25f * light, alpha = alpha)
        pen.diamond(step.cx, step.cy + unit * .35f, unit - inset, (unit - inset) / 2)
        if (noise(spot.x, spot.y, 9) < .35f) {
            pen.color = weep.copy(alpha = ((.5f + .3f * sin(time * 1.5f + spot.x)) * light * alpha).coerceIn(0f, 1f))
            val x = spot.cx + unit * (.2f + noise(spot.x, spot.y, 10) * .6f)
            val top = spot.cy - unit / 2 + h - unit * .1f
            pen.line(x, top, x, top - h * (.4f + noise(spot.x, spot.y, 11) * .4f), unit * .06f)
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        val centre = Offset(w / 2, h * .55f)
        scope.drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF500000).copy(alpha = .55f)), centre, max(w, h) * .75f))
        repeat(5) { i ->
            val x = (spread(i, 3) * w * 1.6f + time * w * (.015f + i * .005f)) % (w * 1.6f) - w * .3f
            val y = h * (.2f + spread(i, 4) * .7f)
            val r = w * .3f
            scope.drawCircle(Brush.radialGradient(listOf(Color(0xFFC81414).copy(alpha = .09f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
    }
}

/** VI · Dunes (2.77.0): sand rippled by the wind, sandstone laid in bands, grains blowing across the screen. */
internal class Dunes : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.95f + noise(spot.x, spot.y) * .25f) * light)
        pen.diamond(cx, cy, u, u / 2)
        // The wind's hand: two or three soft ripples across the tile.
        pen.color = tone(palette.decor, .9f * light, alpha = .35f)
        val ripples = 2 + (noise(spot.x, spot.y, 40) * 2).toInt()
        repeat(ripples) { k ->
            val y = cy + ((k + 1f) / (ripples + 1) - .5f) * u * .8f + (noise(spot.x, spot.y, 41 + k) - .5f) * u * .1f
            pen.polyline(cx - u * .55f, y, cx, y + u * .08f, cx + u * .55f, y, width = u * .03f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1f + noise(spot.x, spot.y, 42) * .7f)
        block(
            spot,
            h,
            tone(palette.wallSide, 1.15f * light, alpha = alpha),
            tone(palette.wallSide, .8f * light, alpha = alpha),
            tone(palette.wallTop, 1.05f * light, alpha = alpha),
        )
        // Strata: the bands the sandstone was laid in, running across both faces.
        pen.color = tone(palette.wallTop, .75f * light, alpha = .5f * alpha)
        val bands = if (variant(spot) == 0) listOf(.3f, .6f) else listOf(.25f, .5f, .75f)
        bands.forEach { z ->
            seam({ t, lift -> left(spot, t, lift) }, unit * .04f, 0f, h * z, 1f, h * z)
            seam({ t, lift -> right(spot, t, lift) }, unit * .04f, 0f, h * z, 1f, h * z)
        }
        // A sunlit crest on some of the rock.
        if (variant(spot) == 2) frontEdges(spot, h, tone(palette.accent, light, alpha = .35f * alpha), unit * .04f)
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        scope.drawRect(Brush.verticalGradient(listOf(palette.accent.copy(alpha = .06f), Color.Transparent), startY = 0f, endY = h * .6f))
        val grain = w / 260f
        repeat(60) { i ->
            val drift = (time * (.08f + spread(i, 21) * .08f) + spread(i, 22)) % 1f
            val y = spread(i, 23) * h + sin(time * 1.5f + i) * grain * 4
            scope.drawCircle(palette.accent.copy(alpha = .25f + .25f * spread(i, 24)), grain, Offset(drift * w * 1.2f - w * .1f, y))
        }
    }
}
