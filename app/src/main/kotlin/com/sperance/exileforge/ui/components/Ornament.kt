package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.*

/** «Эфир» panel (2.80.0): a hairline frame, a soft lift at the top and a thread of light along the top edge. */
@Composable fun ForgePanel(modifier: Modifier = Modifier, accent: Color = Gold, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.fillMaxWidth().background(panelBrush(accent), shape).border(1.dp, Bronze, shape)
        .drawBehind {
            val edge = size.width * .18f
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = .45f), Color.Transparent), edge, size.width - edge),
                Offset(edge, .5f), Offset(size.width - edge, .5f), 1f)
        }
        .padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

/** Title band of a screen: sigil, engraved name, and a rule that fades into the dark. */
@Composable fun ScreenHeader(title: String, subtitle: String? = null, icon: ImageVector? = null, accent: Color = Gold) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon?.let { Icon(it, null, tint = accent, modifier = Modifier.size(26.dp)) }
            Text(title, style = MaterialTheme.typography.headlineLarge, color = GoldBright)
        }
        subtitle?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
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

/** A hairline of light that fades at both ends, a spark at its middle — the divider across the app. */
@Composable fun OrnateDivider(accent: Color = Gold, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(9.dp)) {
        val middle = size.height / 2
        val centre = Offset(size.width / 2, middle)
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = .4f), Color.Transparent)), Offset(0f, middle), Offset(size.width, middle), 1f)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .45f), Color.Transparent), centre, 9f), 9f, centre)
        drawCircle(accent, 1.6f, centre)
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
    val shape = RoundedCornerShape(50)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(46.dp))
        Box(Modifier.weight(1f).height(6.dp).background(PanelRaised, shape)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().glow(color, radius = 6.dp, shape = shape)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = .55f), color)), shape))
        }
        Text(value, color = GoldBright, style = MaterialTheme.typography.labelLarge.copy(fontFamily = Numeric), textAlign = TextAlign.End)
    }
}

/** Small engraved caption used above grouped controls. */
@Composable fun Engraved(text: String, accent: Color = Gold, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = accent.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall, modifier = modifier)
}

/** The dark ground of every screen, with a faint ether glow from the upper corner. */
fun Modifier.voidBackdrop(): Modifier = this.background(voidBrush()).drawBehind {
    drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .06f), Color.Transparent), center = Offset(size.width * .15f, 0f), radius = size.width * .9f),
        radius = size.width * .9f, center = Offset(size.width * .15f, 0f))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .45f)), startY = size.height * .55f, endY = size.height))
}
