package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.max
import kotlin.math.sin
import com.sperance.exileforge.core.campaign.MapStyle as Layout

/** V · Moss and roots: a living floor with grass swaying, mossy banks with roots and ferns, soft shade by the rock, fireflies. */
internal class Overgrown : MapStyle() {
    private val grass = Color(0xFF6F9E4C)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val cx = spot.cx
        val cy = spot.cy
        val u = unit
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

    override fun wallHeight(rise: Float) = .9f + rise * .5f

    /** Лес, болото, джунгли - «Заросли» при любой раскладке. */
    override fun texture(layout: Layout): WallTexture = Thicket

    /** Земляной вал под листвой: бока - земля, крышка и фаска - листва цвета декора. */
    override fun wallTones(palette: Palette) = WallTones.of(palette).let {
        WallTones(
            face = tone(palette.wallSide, 1.1f),
            cap = tone(palette.decor, .8f),
            rim = tone(palette.decor, 1.45f),
            ground = it.ground,
            light = tone(palette.decor, 1.8f),
            growth = tone(palette.decor, 1.05f),
            bloom = tone(palette.decor, 1.5f),
            root = it.root,
            stone = it.stone,
        )
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val w = scope.size.width
        val h = scope.size.height
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
 * veins pulse in a wave, pools of blood in the hollows, blocks weeping blood, red haze.
 */
internal class BloodAltar : MapStyle() {
    private val vein = Color(0xFFFF3C28)
    private val blood = Color(0xFF5A0406)
    private val weep = Color(0xFFAA0C0C)
    private val haze = unitGlow(Color.Transparent, Color(0xFF500000).copy(alpha = .55f))
    private val mist = unitGlow(Color(0xFFC81414).copy(alpha = .09f))

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val cx = spot.cx
        val cy = spot.cy
        val u = unit
        pen.color = tone(palette.floor, (.85f + noise(spot.x, spot.y) * .35f) * light)
        pen.diamond(cx, cy, u, u / 2)
        pen.color = Color.Black.copy(alpha = .5f)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .025f)
        if (noise(spot.x, spot.y, 3) < .38f) {
            // The veins pulse as one wave rolling across the floor, not each on its own.
            val a = (.4f + .4f * sin(time * 2.2f - (spot.x + spot.y) * .6f)) * max(.4f, light)
            val bend = (noise(spot.x, spot.y, 5) - .5f) * u * .3f
            val across = noise(spot.x, spot.y, 4) < .5f
            // Жила поперёк или вдоль клетки: три точки, без массива на клетку
            val y1 = if (across) cy + u / 4 else cy - u / 4
            val x2 = if (across) cx + bend else cx
            val y2 = if (across) cy else cy + bend * .6f
            val y3 = if (across) cy - u / 4 else cy + u / 4
            pen.color = vein.copy(alpha = a * .3f)
            pen.polyline(cx - u / 2, y1, x2, y2, cx + u / 2, y3, width = u * .12f)
            pen.color = vein.copy(alpha = a)
            pen.polyline(cx - u / 2, y1, x2, y2, cx + u / 2, y3, width = u * .035f)
        }
        if (spot.walls == 0 && noise(spot.x, spot.y, 6) < .07f) {
            val w = u * .5f
            pen.color = tone(blood, light.coerceAtLeast(.5f), alpha = .9f)
            pen.ellipse(cx - w, cy - w / 2, w * 2, w)
            pen.color = Color(0xFFFF5A46).copy(alpha = (.3f + .15f * sin(time * 3f + spot.x)) * light)
            pen.ellipse(cx - w * .6f, cy, w * .6f, w * .2f)
        }
    }

    override fun wallHeight(rise: Float) = 1.3f + rise * .5f

    /** Кромка - алая жила, пульсирует волной по карте, как жилы пола. */
    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = lerp(tone(tones.rim, .7f * light), vein, ((.35f + .25f * sin(time * 2.2f - (x + y) * .6f)) * light).coerceIn(0f, 1f))

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val w = scope.size.width
        val h = scope.size.height
        // Красная мгла к краям: круг единичного радиуса, растянутый на экран
        val reach = max(w, h) * .75f
        scope.withTransform({
            translate(w / 2, h * .55f)
            scale(reach, reach, Offset.Zero)
        }) { drawRect(haze, Offset(-w / 2 / reach, -h * .55f / reach), Size(w / reach, h / reach)) }
        repeat(5) { i ->
            val x = (spread(i, 3) * w * 1.6f + time * w * (.015f + i * .005f)) % (w * 1.6f) - w * .3f
            val y = h * (.2f + spread(i, 4) * .7f)
            val r = w * .3f
            scope.blot(mist, x, y, r)
        }
    }
}

/** VI · Dunes (2.77.0): sand rippled by the wind, sandstone laid in bands, grains blowing across the screen. */
internal class Dunes : MapStyle() {
    private val glare = PaletteBrush { unitFade(it.accent.copy(alpha = .06f), Color.Transparent) }

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val cx = spot.cx
        val cy = spot.cy
        val u = unit
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

    override fun wallHeight(rise: Float) = 1f + rise * .7f

    /** Залитый солнцем гребень песчаника. */
    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = lerp(tone(tones.rim, light), tone(palette.accent, light), .4f)

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val w = scope.size.width
        val h = scope.size.height
        scope.band(glare(palette), 0f, h * .6f)
        val grain = w / 260f
        repeat(60) { i ->
            val drift = (time * (.08f + spread(i, 21) * .08f) + spread(i, 22)) % 1f
            val y = spread(i, 23) * h + sin(time * 1.5f + i) * grain * 4
            scope.drawCircle(palette.accent.copy(alpha = .25f + .25f * spread(i, 24)), grain, Offset(drift * w * 1.2f - w * .1f, y))
        }
    }
}
