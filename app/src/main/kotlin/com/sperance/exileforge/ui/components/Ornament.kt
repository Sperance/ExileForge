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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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

/** Life / mana globe: a filled orb with a glass highlight, read straight from server stats. */
@Composable fun StatGlobe(label: String, value: String, ratio: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Canvas(Modifier.size(64.dp)) {
            val radius = size.minDimension / 2 - 2f
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(Color(0xFF0A0D12), radius, center)
            val fill = ratio.coerceIn(0f, 1f)
            if (fill > 0f) {
                val top = center.y + radius - radius * 2 * fill
                drawRect(Brush.verticalGradient(listOf(color.copy(alpha = .95f), color.copy(alpha = .55f)), startY = top, endY = center.y + radius),
                    topLeft = Offset(center.x - radius, top), size = Size(radius * 2, radius * 2 * fill))
            }
            drawCircle(color.copy(alpha = .85f), radius, center, style = Stroke(2f))
            drawCircle(Color.White.copy(alpha = .10f), radius * .55f, center - Offset(radius * .3f, radius * .35f))
        }
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
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
