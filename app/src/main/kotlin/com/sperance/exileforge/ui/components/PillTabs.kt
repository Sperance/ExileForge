package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.ui.theme.*

/**
 * Число у подписи вкладки («Мои лоты 3/5»): приглушённое, [alert] - красное (мест нет); [due] - во вкладке есть дело, что ждёт
 * действия: жёлтая точка после числа (без числа - после подписи).
 */
data class TabBadge(val text: String, val alert: Boolean = false, val due: Boolean = false)

/** Ширина вкладки листающегося ряда, дальше подпись режется многоточием. */
private val SCROLL_TAB_MAX = 200.dp

/** Размер жёлтой точки дела у подписи вкладки. */
private val DUE_DOT_SIZE = 9.sp

/**
 * Вкладки экрана таблетками («Мягкий»): открытая - на приподнятом фоне светлым текстом, зелёный остаётся главной
 * кнопке. [segmented] кладёт их долями одной капсулы-дорожки - два вида одной полки; [trailing] замыкает ряд («?» экрана).
 * [onReselect] - нажатие на уже открытую вкладку (перечитать её); null - открытая вкладка не нажимается. [badges] - числа у подписей
 * по индексу вкладки ([TabBadge]); нет числа - null. [scroll] - ряд не влезает в ширину: вкладки листаются вбок с подсказкой края
 * ([scrollCue]), длинная подпись режется многоточием, [trailing] стоит у правого края вне прокрутки.
 */
@Composable fun PillTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    segmented: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
    onReselect: (() -> Unit)? = null,
    badges: List<TabBadge?> = emptyList(),
    scroll: Boolean = false,
) {
    val pill = RoundedCornerShape(50)
    val cue = if (scroll && !segmented) rememberRowScrollCue() else null
    val track = if (segmented) {
        Modifier.depthInset(pill).padding(4.dp)
    } else {
        Modifier
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val lane = when {
            segmented -> Modifier.weight(1f).then(track)
            cue != null -> Modifier.weight(1f).scrollCue(cue).horizontalScroll(cue.state)
            else -> Modifier
        }
        Row(lane, horizontalArrangement = Arrangement.spacedBy(if (segmented) 0.dp else 6.dp)) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                val fill = when {
                    on && segmented -> Modifier.depthRaised(pill)
                    on -> Modifier.depthPrimary(pill)
                    segmented -> Modifier
                    else -> Modifier.depthRaised(pill, elevation = 2.dp)
                }
                val badge = badges.getOrNull(index)
                Text(
                    buildAnnotatedString {
                        append(label)
                        badge?.takeIf { it.text.isNotEmpty() }?.let { withStyle(SpanStyle(color = if (it.alert) LifeRed else Muted, fontWeight = FontWeight.Normal)) { append(" " + it.text) } }
                        if (badge?.due == true) withStyle(SpanStyle(color = Caution, fontSize = DUE_DOT_SIZE)) { append(" ●") }
                    },
                    color = if (on && !segmented) {
                        Ink
                    } else if (on) {
                        GoldBright
                    } else {
                        Muted
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = (if (segmented) Modifier.weight(1f) else if (cue != null) Modifier.widthIn(max = SCROLL_TAB_MAX) else Modifier).then(fill).clip(pill)
                        .selectable(selected = on, enabled = enabled, role = Role.Tab) { if (!on) onSelect(index) else onReselect?.invoke() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        if (!segmented && cue == null) Spacer(Modifier.weight(1f))
        trailing()
    }
}
