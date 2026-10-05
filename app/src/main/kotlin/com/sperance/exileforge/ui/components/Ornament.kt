package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.*

/**
 * Панель «Мягкого» стиля (3.88.3): ровный приподнятый фон, скругление 18 dp, без рамки и линии света. Свой [accent]
 * (гильдия, роль, опасность) лишь чуть окрашивает фон; у обычной панели, с акцентом по умолчанию, фон чистый.
 */
@Composable fun ForgePanel(modifier: Modifier = Modifier, accent: Color = Gold, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val fill = if (accent == Gold) PanelRaised else accent.copy(alpha = .06f).compositeOver(PanelRaised)
    Column(
        modifier.fillMaxWidth().background(fill, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Title band of a screen: sigil, engraved name, and a rule that fades into the dark. */
@Composable fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = Gold,
    action: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon?.let { Icon(it, null, tint = accent, modifier = Modifier.size(26.dp)) }
            Text(title, style = MaterialTheme.typography.headlineLarge, color = GoldBright, modifier = if (action != null) Modifier.weight(1f) else Modifier)
            action?.invoke()
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
    Box(
        Modifier.width(width).fillMaxHeight()
            .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = .12f)))),
    )
}

/** A small rotated square, the marker a rolled modifier is listed under. */
@Composable fun Rhombus(accent: Color = Rune, side: Dp = 5.dp) {
    Canvas(Modifier.size(side)) {
        val w = size.width
        val h = size.height
        drawPath(
            Path().apply {
                moveTo(w / 2, 0f)
                lineTo(w, h / 2)
                lineTo(w / 2, h)
                lineTo(0f, h / 2)
                close()
            },
            accent.copy(alpha = .75f),
        )
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

/** Small engraved caption used above grouped controls. */
@Composable fun Engraved(text: String, accent: Color = Gold, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = accent.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall, modifier = modifier)
}

/** The dark ground of every screen, with a faint ether glow from the upper corner. */
fun Modifier.voidBackdrop(): Modifier = this.background(voidBrush()).drawBehind {
    drawCircle(
        Brush.radialGradient(listOf(Gold.copy(alpha = .06f), Color.Transparent), center = Offset(size.width * .15f, 0f), radius = size.width * .9f),
        radius = size.width * .9f,
        center = Offset(size.width * .15f, 0f),
    )
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .45f)), startY = size.height * .55f, endY = size.height))
}
