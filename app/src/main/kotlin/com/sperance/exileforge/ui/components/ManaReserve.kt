package com.sperance.exileforge.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/** The share of a mana pool the auras hold: [reserved] of [free] + [reserved], none for an empty pool. */
internal fun reservedShare(free: Int, reserved: Int): Float = if (reserved > 0 && free + reserved > 0) reserved / (free + reserved).toFloat() else 0f

/**
 * A pool bar's reserved tail: its last [share] drawn darker, hatched with diagonal stripes of [tint] — the mana
 * the auras hold, which never fills while they stand. Laid on the track under the fill and the figures, so the
 * fill (now / free + reserved) runs up to the tail and stops there.
 */
internal fun Modifier.reservedTail(share: Float, tint: Color): Modifier = if (share <= 0f) {
    this
} else {
    drawBehind {
        val start = size.width * (1 - share.coerceIn(0f, 1f))
        val gap = 5.dp.toPx()
        clipRect(left = start) {
            drawRect(lerp(tint, Color.Black, .7f).copy(alpha = .85f), Offset(start, 0f), Size(size.width - start, size.height))
            var x = start - size.height
            while (x < size.width) {
                drawLine(tint.copy(alpha = .45f), Offset(x, size.height), Offset(x + size.height, 0f), 1.5.dp.toPx())
                x += gap
            }
        }
    }
}
