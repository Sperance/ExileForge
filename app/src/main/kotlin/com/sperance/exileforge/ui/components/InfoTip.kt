package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.sperance.exileforge.ui.theme.*

/**
 * What an icon says about itself (2.73.0): its name in its colour, the rule in words, and the
 * figures it carries — strength, time left, stacks — as label and value.
 */
data class Tip(val title: String, val body: String = "", val tint: Color = GoldBright, val facts: List<Pair<String, String>> = emptyList())

/**
 * An icon that explains itself (2.73.0): a tap opens [tip] as a small window growing out of the
 * icon, its pointer on it, below it or above when there is no room; a tap anywhere closes it.
 * A null [tip] leaves the icon as it was. The tip is read when opened, so its figures are fresh.
 */
@Composable fun Tipped(tip: (() -> Tip)?, modifier: Modifier = Modifier, contentAlignment: Alignment = Alignment.TopStart,
                       content: @Composable BoxScope.() -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.then(if (tip != null) Modifier.clickable(remember { MutableInteractionSource() }, null) { open = true } else Modifier),
        contentAlignment = contentAlignment) {
        content()
        if (open && tip != null) TipCallout(tip()) { open = false }
    }
}

/** The window itself, anchored to whatever holds it. */
@Composable fun TipCallout(tip: Tip, onClose: () -> Unit) {
    val density = LocalDensity.current
    val margin = with(density) { 8.dp.roundToPx() }
    val gap = with(density) { 2.dp.roundToPx() }
    var pointer by remember { mutableFloatStateOf(0f) }
    var below by remember { mutableStateOf(true) }
    val placing = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                val fits = anchorBounds.bottom + gap + popupContentSize.height <= windowSize.height - margin
                pointer = (anchorBounds.center.x - x).toFloat()
                below = fits || anchorBounds.top - gap - popupContentSize.height < margin
                return IntOffset(x, if (below) anchorBounds.bottom + gap else anchorBounds.top - gap - popupContentSize.height)
            }
        }
    }
    val grow = remember { Animatable(.4f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, spring(dampingRatio = .7f, stiffness = 700f)) }
    Popup(popupPositionProvider = placing, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        val arrow = 7.dp
        val shape = RoundedCornerShape(10.dp)
        Box(Modifier.padding(top = if (below) arrow else 0.dp, bottom = if (below) 0.dp else arrow)
            .graphicsLayer {
                scaleX = grow.value; scaleY = grow.value; alpha = grow.value.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin((pointer / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f), if (below) 0f else 1f)
            }
            .drawBehind {
                val a = arrow.toPx()
                val x = pointer.coerceIn(a * 2, size.width - a * 2)
                val tipY = if (below) -a else size.height + a
                val baseY = if (below) 1f else size.height - 1f
                val beak = Path().apply { moveTo(x - a, baseY); lineTo(x, tipY); lineTo(x + a, baseY); close() }
                drawPath(beak, Panel)
                drawLine(tip.tint.copy(alpha = .8f), Offset(x - a, baseY), Offset(x, tipY), 1.dp.toPx())
                drawLine(tip.tint.copy(alpha = .8f), Offset(x, tipY), Offset(x + a, baseY), 1.dp.toPx())
            }
            .widthIn(max = 280.dp).background(Panel, shape).border(1.dp, tip.tint.copy(alpha = .8f), shape)
            .clickable(onClick = onClose).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(tip.title, color = tip.tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (tip.body.isNotBlank()) Text(tip.body, color = Parchment, style = MaterialTheme.typography.bodySmall)
                tip.facts.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        Text(value, color = GoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
