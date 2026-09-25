package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * The fight's ground — the cave of the owner's mockup VI (2.57.0): a vault, two ridges and the
 * torches between them drifting at their own depths, the biome's floor with its seams running to
 * the horizon, fog along it. Since 2.70.0 nobody stands on it: the fighters are the overlay's cards
 * (mockup B «карточки против карточек»), and the cave is only what they are laid over.
 */
internal fun DrawScope.fightBackdrop(palette: Palette, time: Float) {
    val w = size.width
    val h = size.height
    val floor = h * FLOOR
    val sway = sin(time * .23f) * w * .02f
    drawRect(Brush.verticalGradient(listOf(palette.void, tone(palette.wallSide, .55f), tone(palette.floor, .6f)), 0f, floor), size = Size(w, floor))
    val vault = Offset(w / 2, -h * .05f)
    drawCircle(Brush.radialGradient(listOf(palette.accent.copy(alpha = .10f), Color.Transparent), vault, w * .8f), w * .8f, vault)
    stalactites(tone(palette.wallSide, .45f), sway * .2f)
    ridge(3, floor - h * .20f, h * .10f, tone(palette.wallSide, .75f), sway * .3f)
    listOf(.1f, .9f).forEachIndexed { i, share -> torch(w * share + sway * .55f, floor - h * .15f, time, i) }
    ridge(11, floor - h * .05f, h * .05f, tone(palette.wallTop, .55f), sway * .55f)
    ground(floor, palette, sway)
    // Darker than it was: the cards carry the fight now, and the cave only sets the mood behind them.
    drawRect(Brush.radialGradient(listOf(Color.Black.copy(alpha = .35f), Color.Black.copy(alpha = .75f)), Offset(w / 2, h / 2), maxOf(w, h) * .75f))
}

/** Where the cave's floor meets its walls, as a share of the height. */
private const val FLOOR = .66f

/** A stable pseudo-random share in 0..1 for particle [i], so a rock keeps its place from frame to frame. */
private fun lane(i: Int): Float = ((sin(i * 127.1f + 311.7f) * 43758.547f) % 1f).let { abs(it) }

/** Icicles of rock along the vault. */
private fun DrawScope.stalactites(colour: Color, shift: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(-40f, 0f)
        for (i in 0..14) {
            val x = -40f + (w + 80f) * i / 14 + shift
            lineTo(x - 7.dp.toPx(), 0f); lineTo(x, size.height * (.03f + lane(i + 50) * .14f)); lineTo(x + 7.dp.toPx(), 0f)
        }
        lineTo(w + 40f, 0f); close()
    }
    drawPath(path, colour)
}

/** A jagged skyline from [seed], so a biome's cave keeps its shape all fight long. */
private fun DrawScope.ridge(seed: Int, top: Float, amplitude: Float, colour: Color, shift: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(-60f, size.height)
        for (i in 0..16) lineTo(-60f + (w + 120f) * i / 16 + shift, top - lane(seed + i) * amplitude)
        lineTo(w + 60f, size.height); close()
    }
    drawPath(path, colour)
}

/** A torch on its bracket, breathing, and the warm light it throws. */
private fun DrawScope.torch(x: Float, y: Float, time: Float, seed: Int) {
    val flicker = .82f + .12f * sin(time * 17 + seed) + .08f * sin(time * 29 + seed * 3)
    val glow = 40.dp.toPx() * flicker
    drawCircle(Brush.radialGradient(listOf(Palettes.torch.copy(alpha = .18f), Color.Transparent), Offset(x, y), glow), glow, Offset(x, y))
    drawRect(Color(0xFF2A2418), Offset(x - 2.dp.toPx(), y), Size(4.dp.toPx(), 18.dp.toPx()))
    drawRect(Palettes.bronze, Offset(x - 5.dp.toPx(), y - 2.dp.toPx()), Size(10.dp.toPx(), 4.dp.toPx()))
    val s = 6.dp.toPx()
    val flame = Path().apply {
        moveTo(x - s * .5f, y); quadraticTo(x - s * .6f, y - s * 1.1f * flicker, x + sin(time * 9 + seed) * s * .2f, y - s * 2.1f * flicker)
        quadraticTo(x + s * .6f, y - s * 1.1f * flicker, x + s * .5f, y); close()
    }
    drawPath(flame, Brush.verticalGradient(listOf(Color(0x00F0E2C0), Color(0xFFF0E2C0), Palettes.torch), y - s * 2.1f, y))
}

/** The floor: the biome's ground falling into the dark, its seams running to the horizon, fog drifting along it. */
private fun DrawScope.ground(floor: Float, palette: Palette, sway: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(tone(palette.floor, .9f), palette.void), floor, h), Offset(0f, floor), Size(w, h - floor))
    val seam = Palettes.bronze.copy(alpha = .16f)
    for (i in -7..7) drawLine(seam, Offset(w / 2 + i * w * .06f + sway * .8f, floor), Offset(w / 2 + i * w * .2f + sway, h), 1f)
    for (k in 1..4) { val y = floor + (h - floor) * (k / 5f).let { it * it }; drawLine(seam, Offset(0f, y), Offset(w, y), 1f) }
    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Palettes.bronze.copy(alpha = .6f), Color.Transparent)), Offset(0f, floor), Offset(w, floor), 1.4f)
    withTransform({ scale(3f, 1f, Offset(w / 2, floor + (h - floor) * .25f)) }) {
        drawCircle(Brush.radialGradient(listOf(palette.accent.copy(alpha = .05f), Color.Transparent), Offset(w / 2 + sway, floor + (h - floor) * .25f), w * .2f),
            w * .2f, Offset(w / 2 + sway, floor + (h - floor) * .25f))
    }
}
