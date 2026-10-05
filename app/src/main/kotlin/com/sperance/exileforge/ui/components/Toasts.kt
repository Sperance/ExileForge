package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.NoticeKind
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay

/** One toast on screen: its words, its colour, and how it is sent away. */
private data class Toast(val text: String, val tint: Color, val mark: String, val key: Any, val onDismiss: () -> Unit)

/** How long a toast stays before it leaves by itself. */
private const val TOAST_MS = 4_000L

/**
 * The toasts (2.80.0, «Эфир»): one card floating under the banner — a refusal in red, a success in
 * ether, loot in the rare item's yellow, a finished craft in frost. It slides in, stays four seconds
 * and leaves by itself; a tap sends it away sooner. A refusal outranks a success.
 */
@Composable fun ToastHost(game: GameUi, onRefusal: () -> Unit, onNotice: () -> Unit, modifier: Modifier = Modifier) {
    val toast = game.refusal?.let { Toast(it.read(), LifeRed, "!", it, onRefusal) }
        ?: game.notice?.let { Toast(it.text, it.kind.tint(), it.kind.mark(), it.at, onNotice) }
    toast?.let {
        LaunchedEffect(it.key) {
            delay(TOAST_MS)
            it.onDismiss()
        }
    }
    AnimatedContent(
        toast,
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        label = "toast",
        contentKey = { it?.key },
        transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
    ) { shown ->
        if (shown != null) ToastCard(shown)
    }
}

/** Значок в кружке тоста (3.88.3): ✓ - сделано, i - сообщение; отказ - «!». */
private fun NoticeKind.mark(): String = when (this) {
    NoticeKind.ATLAS -> "i"
    else -> "✓"
}

private fun NoticeKind.tint(): Color = when (this) {
    NoticeKind.DONE -> Gold
    NoticeKind.LOOT -> rarityColor("RARE")
    NoticeKind.CRAFT -> Rune
    NoticeKind.ATLAS -> GoldBright
}

/** Тост «Мягкого» стиля (3.88.3): капсула по центру, значок в кружке цвета тоста; касание убирает его. */
@Composable private fun ToastCard(toast: Toast) {
    val shape = RoundedCornerShape(50)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(shape).background(ToastFill, shape)
                .clickable(onClickLabel = ui("common.close"), onClick = toast.onDismiss)
                .padding(start = 9.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(22.dp).background(toast.tint, CircleShape), contentAlignment = Alignment.Center) {
                Text(toast.mark, color = Ink, style = MaterialTheme.typography.labelLarge)
            }
            Text(toast.text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Фон капсулы тоста: чуть светлее приподнятой панели, чтобы тост читался над любым экраном. */
private val ToastFill = Color(0xFF1D2B35)
