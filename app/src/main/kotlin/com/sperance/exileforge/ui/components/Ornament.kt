package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.*

/** Beaten-metal plate: lit top bevel, bronze frame, notched corners. */
@Composable fun ForgePanel(modifier: Modifier = Modifier, accent: Color = Gold, content: @Composable ColumnScope.() -> Unit) {
    val shape = CutCornerShape(10.dp)
    Column(modifier.fillMaxWidth().background(panelBrush(accent), shape).border(1.dp, accent.copy(alpha = .38f), shape)
        .drawBehind {
            val inset = 5f
            drawLine(accent.copy(alpha = .18f), Offset(inset, inset), Offset(size.width - inset, inset), 1f)
            drawLine(accent.copy(alpha = .08f), Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1f)
        }
        .padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

/** Title band of a screen: sigil, engraved name, and a rule that fades into the dark. */
@Composable fun ScreenHeader(title: String, subtitle: String? = null, icon: ImageVector? = null, accent: Color = Gold) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon?.let { Icon(it, null, tint = accent, modifier = Modifier.size(26.dp)) }
            Text(title.uppercase(), style = MaterialTheme.typography.headlineLarge, color = accent)
        }
        subtitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium) }
        OrnateDivider(accent)
    }
}

/**
 * The band of rarity colour down the left edge of an item.
 *
 * An item used to be framed in its rarity; the frame is gone, and this carries it instead — a
 * bookmark in the edge of a page rather than a box drawn round it. It fades downwards so a long
 * card does not end in a stripe, and it needs its parent `Row` to be `height(IntrinsicSize.Min)`.
 */
@Composable fun RaritySpine(accent: Color, width: Dp = 4.dp) {
    Box(Modifier.width(width).fillMaxHeight()
        .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = .12f)))))
}

/** A small rotated square, the marker a rolled modifier is listed under. */
@Composable fun Rhombus(accent: Color = Rune, side: Dp = 5.dp) {
    Canvas(Modifier.size(side)) {
        val w = size.width
        val h = size.height
        drawPath(Path().apply {
            moveTo(w / 2, 0f); lineTo(w, h / 2); lineTo(w / 2, h); lineTo(0f, h / 2); close()
        }, accent.copy(alpha = .75f))
    }
}

/** Bronze rule with a central rhombus, the divider used across the stash UI. */
@Composable fun OrnateDivider(accent: Color = Gold, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val middle = size.height / 2
        val gap = 12f
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = .55f))), Offset(0f, middle), Offset(size.width / 2 - gap, middle), 1.5f)
        drawLine(Brush.horizontalGradient(listOf(accent.copy(alpha = .55f), Color.Transparent)), Offset(size.width / 2 + gap, middle), Offset(size.width, middle), 1.5f)
        val rhombus = Path().apply {
            moveTo(size.width / 2, middle - 5f); lineTo(size.width / 2 + 6f, middle)
            lineTo(size.width / 2, middle + 5f); lineTo(size.width / 2 - 6f, middle); close()
        }
        drawPath(rhombus, accent.copy(alpha = .30f)); drawPath(rhombus, accent, style = Stroke(1.2f))
    }
}

/**
 * A vital: life, mana or energy shield, as a bar with its number on it.
 *
 * The bar is full because the number *is* the whole of it — the server's sheet carries a maximum
 * and no current value, there being nothing yet that spends one. Drawing a partly empty bar would
 * claim a reserve the server never reported, so the fill stays honest and the number does the work.
 */
@Composable fun StatBar(label: String, value: String, color: Color, modifier: Modifier = Modifier,
    /**
     * How much of the bar is filled, 0..1.
     *
     * Full by default, because a vital is a maximum and nothing reports a current value — a
     * half-empty life bar would claim a reserve the server never sent. Experience is the one
     * thing here that really does fill up, so it passes its own share.
     */
    fraction: Float = 1f) {
    val shape = CutCornerShape(4.dp)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(46.dp))
        Box(Modifier.weight(1f).height(18.dp).background(Color(0xFF0A0D12), shape)
            .border(1.dp, color.copy(alpha = .8f), shape)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = .75f), color.copy(alpha = .35f))), shape))
        }
        Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
    }
}

/** Small engraved caption used above grouped controls. */
@Composable fun Engraved(text: String, accent: Color = Gold, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = accent.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall, modifier = modifier)
}

/** Layered void backdrop: a stone vignette that keeps every screen anchored in the dark. */
fun Modifier.voidBackdrop(): Modifier = this.background(voidBrush()).drawBehind {
    drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .05f), Color.Transparent), center = Offset(size.width / 2, 0f), radius = size.width * .9f),
        radius = size.width * .9f, center = Offset(size.width / 2, 0f))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .45f)), startY = size.height * .55f, endY = size.height))
}
