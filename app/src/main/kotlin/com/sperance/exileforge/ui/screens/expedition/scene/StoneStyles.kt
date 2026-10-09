package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.max
import kotlin.math.sin

/** I · Wet stone: flagstones split by mortar, puddles holding the torch, walls laid in courses. Капель с 4.2.0 нет - дождя на картах нет. */
internal class WetStone : MapStyle() {
    private val wet = Color(0xFFBED2E1)

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

    override fun wallHeight(rise: Float) = 1.5f + rise * .3f

    /** Мокрый блеск по кромке. */
    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = lerp(tone(tones.rim, light), tone(wet, light), .35f)
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

    override fun wallHeight(rise: Float) = 1.7f + rise * .9f

    /** Руна дышит в кромке обсидиана - волной по карте. */
    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = lerp(tone(tones.rim, .7f * light), palette.accent, ((.3f + .3f * sin(time * 1.6f + x + y)) * max(.3f, light)).coerceIn(0f, 1f))

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

    override fun wallHeight(rise: Float) = 1.2f + rise * .6f

    /** Кромка тлеет углями. */
    override fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float) = lerp(tone(tones.rim, .8f * light), core, ((.35f + .3f * sin(time * 2f + x * 3)) * light).coerceIn(0f, 1f))

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
