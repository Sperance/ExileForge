package com.sperance.exileforge.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn glyph set in the Path of Exile key art idiom: struck metal, orbs and sigils.
 * Bundled vectors only — the client never fetches icon art from the network.
 */
private fun glyph(name: String, vararg strokes: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        strokes.forEach { stroke ->
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.White), strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = stroke)
        }
    }.build()

private fun PathBuilder.poly(vararg points: Pair<Float, Float>) {
    moveTo(points[0].first, points[0].second)
    points.drop(1).forEach { lineTo(it.first, it.second) }
    close()
}
private fun PathBuilder.line(x: Float, y: Float, xx: Float, yy: Float) { moveTo(x, y); lineTo(xx, yy) }
private fun PathBuilder.ring(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, r * 2, 0f)
    arcToRelative(r, r, 0f, true, true, -r * 2, 0f)
    close()
}

object ForgeGlyphs {
    /** Anvil under a raised hammer — the crafting bench. */
    val Anvil = glyph("Anvil", { poly(3f to 12f, 21f to 12f, 17f to 16f, 7f to 16f) }, { poly(9f to 16f, 15f to 16f, 17f to 21f, 7f to 21f) }, { line(13f, 3f, 20f, 8f); line(19f, 5f, 21f, 7f) })
    /** Crossed blades: the campaign. */
    val Swords = glyph("Swords", { line(4f, 20f, 16f, 5f); poly(15f to 3f, 19f to 4f, 17f to 8f) }, { line(20f, 20f, 8f, 5f); poly(9f to 3f, 5f to 4f, 7f to 8f) }, { line(3f, 17f, 7f, 21f); line(21f, 17f, 17f, 21f) })
    /** Currency orb with an inner sigil. */
    val Orb = glyph("Orb", { ring(12f, 12f, 8f) }, { poly(12f to 6f, 17f to 12f, 12f to 18f, 7f to 12f) }, { ring(12f, 12f, 2.2f) })
    /** Cut skill gem. */
    val Gem = glyph("Gem", { poly(12f to 2f, 20f to 9f, 16f to 21f, 8f to 21f, 4f to 9f) }, { line(4f, 9f, 20f, 9f); line(12f, 2f, 8f, 9f); line(12f, 2f, 16f, 9f); line(8f, 9f, 12f, 21f); line(16f, 9f, 12f, 21f) })
    /** Great helm: the hero sheet. */
    val Helm = glyph("Helm", { poly(5f to 20f, 5f to 10f, 9f to 4f, 15f to 4f, 19f to 10f, 19f to 20f, 14f to 17f, 12f to 21f, 10f to 17f) }, { line(12f, 6f, 12f, 16f); line(7f, 12f, 10f, 13f); line(14f, 13f, 17f, 12f) })
    /** Kite shield with a rune bar. */
    val Kite = glyph("Kite", { poly(12f to 2f, 20f to 6f, 18f to 15f, 12f to 22f, 6f to 15f, 4f to 6f) }, { line(12f, 5f, 12f, 19f); line(7f, 9f, 17f, 9f) })
    /** Stash chest. */
    val Stash = glyph("Stash", { poly(3f to 9f, 21f to 9f, 21f to 20f, 3f to 20f) }, { poly(3f to 9f, 5f to 4f, 19f to 4f, 21f to 9f) }, { line(3f, 14f, 21f, 14f); poly(10f to 12f, 14f to 12f, 14f to 17f, 10f to 17f) })
    /** Unfurled scroll: reports and logs. */
    val Scroll = glyph("Scroll", { poly(6f to 3f, 18f to 3f, 18f to 21f, 6f to 21f) }, { line(6f, 6f, 18f, 6f); line(6f, 18f, 18f, 18f) }, { line(9f, 10f, 15f, 10f); line(9f, 13f, 15f, 13f) })
    /** Waypoint portal: servers and travel. */
    val Portal = glyph("Portal", { poly(12f to 2f, 20f to 12f, 12f to 22f, 4f to 12f) }, { poly(12f to 6f, 17f to 12f, 12f to 18f, 7f to 12f) }, { ring(12f, 12f, 1.6f) })
    /** Crosshair: back to the frontier of the world map (2.76.0). */
    val Target = glyph("Target", { ring(12f, 12f, 6.5f) }, { ring(12f, 12f, 1.6f) }, { line(12f, 2.5f, 12f, 6.5f); line(12f, 17.5f, 12f, 21.5f); line(2.5f, 12f, 6.5f, 12f); line(17.5f, 12f, 21.5f, 12f) })
    /** Nearer and farther on the world map (2.76.0). */
    val Plus = glyph("Plus", { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) })
    val Minus = glyph("Minus", { line(5f, 12f, 19f, 12f) })
    /** Constellation: linked nodes and progression. */
    val Constellation = glyph("Constellation", { line(6f, 18f, 11f, 11f); line(11f, 11f, 17f, 13f); line(11f, 11f, 13f, 4f); line(17f, 13f, 19f, 19f) }, { ring(11f, 11f, 2.2f) }, { ring(6f, 18f, 1.4f); ring(13f, 4f, 1.4f); ring(17f, 13f, 1.4f); ring(19f, 19f, 1.4f) })
    /** Monster skull: bestiary and losses. */
    val Skull = glyph("Skull", { poly(5f to 14f, 5f to 9f, 8f to 4f, 16f to 4f, 19f to 9f, 19f to 14f, 15f to 17f, 15f to 20f, 9f to 20f, 9f to 17f) }, { ring(9f, 11f, 2f); ring(15f, 11f, 2f) }, { line(12f, 14f, 12f, 17f) })
    /** Exalted mark used for rare drops and highlights. */
    val Sigil = glyph("Sigil", { poly(12f to 2f, 14f to 9f, 22f to 12f, 14f to 15f, 12f to 22f, 10f to 15f, 2f to 12f, 10f to 9f) })
    /** Hooded exile. */
    val Exile = glyph("Exile", { poly(12f to 3f, 17f to 8f, 16f to 14f, 8f to 14f, 7f to 8f) }, { line(7f, 21f, 9f, 14f); line(17f, 21f, 15f, 14f); line(9f, 14f, 15f, 14f) }, { line(10f, 9f, 11f, 10f); line(13f, 10f, 14f, 9f) })
    /** Map device / atlas. */
    val Atlas = glyph("Atlas", { poly(3f to 6f, 9f to 4f, 15f to 7f, 21f to 5f, 21f to 18f, 15f to 20f, 9f to 17f, 3f to 20f) }, { line(9f, 4f, 9f, 17f); line(15f, 7f, 15f, 20f) })
    /** Bound tome: definitions and rules. */
    val Tome = glyph("Tome", { poly(4f to 4f, 11f to 6f, 11f to 21f, 4f to 19f) }, { poly(20f to 4f, 13f to 6f, 13f to 21f, 20f to 19f) }, { line(12f, 6f, 12f, 21f) })
    /** Torch: highlights and hints. */
    val Torch = glyph("Torch", { poly(12f to 2f, 15f to 7f, 12f to 11f, 9f to 7f) }, { poly(10f to 11f, 14f to 11f, 13f to 21f, 11f to 21f) })
    /** Balance scales: comparison. */
    val Scales = glyph("Scales", { line(12f, 4f, 12f, 20f); line(5f, 7f, 19f, 7f); line(8f, 20f, 16f, 20f) }, { poly(2f to 13f, 8f to 13f, 5f to 7f) }, { poly(16f to 13f, 22f to 13f, 19f to 7f) })
    /** Chain link: bound references. */
    val Chain = glyph("Chain", { ring(8f, 8f, 4f) }, { ring(16f, 16f, 4f) }, { line(10.5f, 10.5f, 13.5f, 13.5f) })
    /** Banner: leagues and modes. */
    val Banner = glyph("Banner", { line(6f, 3f, 6f, 21f) }, { poly(6f to 4f, 19f to 4f, 16f to 9f, 19f to 14f, 6f to 14f) })
    /** A cracked crystal: an affix a Fracturing Orb fixed for good. */
    val Shard = glyph("Shard", { poly(12f to 2f, 19f to 10f, 12f to 22f, 5f to 10f) }, { line(12f, 2f, 10.5f, 9f); line(10.5f, 9f, 13.5f, 13f); line(13.5f, 13f, 12f, 22f) })

    /** A stack of coins — what the merchant pays. */
    val Coins = glyph("Coins", { ring(10f, 9f, 6f) }, { ring(14f, 15f, 6f) }, { line(14f, 12.5f, 14f, 17.5f) })
}
