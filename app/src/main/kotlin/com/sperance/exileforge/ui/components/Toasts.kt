package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.NoticeKind
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay

/** One toast on screen: its words, its colour, and how it is sent away. */
private data class Toast(val text: String, val tint: Color, val key: Any, val onDismiss: () -> Unit)

/** How long a toast stays before it leaves by itself. */
private const val TOAST_MS = 4_000L

/**
 * The toasts (2.80.0, «Эфир»): one card floating under the banner — a refusal in red, a success in
 * ether, loot in the rare item's yellow, a finished craft in frost. It slides in, stays four seconds
 * and leaves by itself; a tap sends it away sooner. A refusal outranks a success.
 */
@Composable fun ToastHost(s: ForgeState, onRefusal: () -> Unit, onNotice: () -> Unit, modifier: Modifier = Modifier) {
    val toast = s.refusal?.let { Toast(it, LifeRed, it, onRefusal) }
        ?: s.notice?.let { Toast(it.text, it.kind.tint(), it.at, onNotice) }
    toast?.let { LaunchedEffect(it.key) { delay(TOAST_MS); it.onDismiss() } }
    AnimatedContent(toast, modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), label = "toast",
        contentKey = { it?.key },
        transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) }) { shown ->
        if (shown != null) ToastCard(shown)
    }
}

private fun NoticeKind.tint(): Color = when (this) {
    NoticeKind.DONE -> Gold
    NoticeKind.LOOT -> rarityColor("RARE")
    NoticeKind.CRAFT -> Rune
}

@Composable private fun ToastCard(toast: Toast) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().glow(toast.tint.copy(alpha = .6f), radius = 12.dp, shape = shape)
        .background(PanelRaised, shape).border(1.dp, toast.tint.copy(alpha = .35f), shape)
        .drawBehind { drawLine(toast.tint, Offset(0f, 10f), Offset(0f, size.height - 10f), 5f) }
        .clickable(onClickLabel = ui("common.close"), onClick = toast.onDismiss)
        .padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).glow(toast.tint, radius = 6.dp, shape = CircleShape).background(toast.tint, CircleShape))
        Text(toast.text, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Close, null, tint = Muted, modifier = Modifier.size(14.dp))
    }
}
