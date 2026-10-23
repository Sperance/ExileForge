package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.launch

/**
 * Круглая значок-кнопка необратимого действия с удержанием: [onHeld] срабатывает, только когда палец продержал её [millis]; кольцо по краю
 * наполняется по часовой стрелке цветом [tint] и пустеет, если отпустить раньше, конец - отклик вибрацией. [onHolding] - палец на кнопке
 * (рядом показывается строка последствий). Погашенная ([enabled] - нет) - серая, без кольца. Зона касания - 44 dp, кнопка - 36 dp.
 */
@Composable fun HoldRingButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: Color,
    onHolding: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    millis: Int = Hold.CONFIRM_MS,
    onHeld: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val buzzes = LocalBuzzes.current
    val held by rememberUpdatedState(onHeld)
    val holding by rememberUpdatedState(onHolding)
    val live by rememberUpdatedState(enabled)
    val ink = if (enabled) tint else Muted
    val holdText = ui("button.hold")
    Box(
        modifier.size(TOUCH)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = contentDescription
                if (enabled) {
                    onClick(holdText) {
                        held()
                        true
                    }
                } else {
                    disabled()
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    if (!live) {
                        waitForUpOrCancellation()?.consume()
                        return@awaitEachGesture
                    }
                    holding(true)
                    val filling = scope.launch {
                        progress.animateTo(1f, tween(millis, easing = LinearEasing))
                        buzzes?.buzz(Buzz.BUTTON)
                        held()
                        progress.snapTo(0f)
                    }
                    waitForUpOrCancellation()?.consume()
                    holding(false)
                    if (progress.value < 1f) {
                        filling.cancel()
                        scope.launch { progress.animateTo(0f, tween(RELEASE_MS)) }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(SIDE).background(ink.copy(alpha = .12f), CircleShape).border(1.dp, ink.copy(alpha = .45f), CircleShape)
                .drawWithCache {
                    val stroke = Stroke(RING.toPx(), cap = StrokeCap.Round)
                    val inset = RING.toPx() / 2
                    val arc = Size(size.width - 2 * inset, size.height - 2 * inset)
                    onDrawWithContent {
                        drawContent()
                        val share = progress.value
                        if (share > 0f) drawArc(ink, -90f, 360f * share, false, Offset(inset, inset), arc, style = stroke)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp))
        }
    }
}

private val TOUCH = 44.dp
private val SIDE = 36.dp
private val RING = 3.dp
private const val RELEASE_MS = 180
