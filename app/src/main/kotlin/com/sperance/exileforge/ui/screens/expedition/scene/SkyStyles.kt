package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.max
import kotlin.math.sin
import com.sperance.exileforge.core.campaign.MapStyle as Layout

/** VI · Sky glass (3.43.0): ground fused to glass with the sky in it, pillars of crystal, a pale glow of sky overhead (без дождя с 4.2.0). */
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

    override fun wallHeight(rise: Float) = 1.6f + rise * 1.1f

    /** Грань кристалла ловит небо; изредка в ней вспыхивает спящая молния. */
    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = lerp(tone(tones.rim, light), palette.accent, (.45f + (if (cellNoise(x, y, 52) < .2f) .4f * sin(time * 7f + y) else 0f)).coerceIn(0f, 1f))

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        scope.drawRect(Brush.verticalGradient(listOf(palette.accent.copy(alpha = .08f), Color.Transparent), startY = 0f, endY = h * .5f))
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

    override fun wallHeight(rise: Float) = 1.4f + rise * .9f

    /** Колонны утонувшей империи - «Обточенный камень»; по кромке - коралл. */
    override fun texture(layout: Layout): WallTexture = Ashlar

    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = lerp(tone(tones.rim, light), tone(palette.decor, 1.1f * light), .5f)

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

    override fun wallHeight(rise: Float) = 1.9f + rise * .8f

    /** Чертоги богов - «Обточенный камень»: мрамор с золотой кромкой. */
    override fun texture(layout: Layout): WallTexture = Ashlar

    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = gold.copy(alpha = .9f * max(.4f, light))

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
