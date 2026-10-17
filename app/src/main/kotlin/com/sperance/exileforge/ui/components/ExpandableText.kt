package com.sperance.exileforge.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted

/** Сколько строк описания видно свёрнутым. */
private const val FOLDED_LINES = 2

/**
 * Текст, что раскрывается нажатием на месте: свёрнутый - [folded] строк с многоточием (и строкой «Ещё…» под ним, если [more]),
 * развёрнутый - целиком, без обрезки. Нажимается, только если текст в свёрнутом виде не уместился; общее для знамений,
 * катализаторов, строк верстака и текста отчётов.
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Muted,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    folded: Int = FOLDED_LINES,
    more: Boolean = false,
) {
    var open by remember(text) { mutableStateOf(false) }
    var clipped by remember(text) { mutableStateOf(false) }
    val toggle = if (open || clipped) {
        Modifier.clickable(role = Role.Button, onClickLabel = ui(if (open) "common.collapse" else "common.expand")) { open = !open }
    } else {
        Modifier
    }
    Column(modifier.then(toggle).animateContentSize()) {
        Text(
            text,
            color = color,
            style = style,
            maxLines = if (open) Int.MAX_VALUE else folded,
            overflow = if (open) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { if (!open) clipped = it.hasVisualOverflow },
        )
        if (more && clipped && !open) Text(ui("common.read_more"), color = Gold, style = MaterialTheme.typography.labelSmall)
    }
}
