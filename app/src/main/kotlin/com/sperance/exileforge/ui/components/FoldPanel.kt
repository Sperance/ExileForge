package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Quiet

/**
 * Сворачиваемая панель: [ForgePanel], свёрнутая в строку-кнопку не ниже [FoldMetrics.head] - заголовок [title] цветом [titleColor],
 * сводка [summary] (чипы [SummaryChip]) и шеврон цветом [accent]. Касание строки раскрывает и сворачивает [content] с анимацией;
 * пока панель свёрнута, содержимое не компонуется.
 *
 * Раскрытие живёт, пока экран открыт (переживает поворот и прокрутку списка), и не запоминается между входами: новый вход -
 * снова [initiallyOpen], по умолчанию свёрнуто.
 */
@Composable fun FoldPanel(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = Gold,
    titleColor: Color = accent,
    initiallyOpen: Boolean = false,
    summary: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    Column(modifier.forgePanelSurface(accent).clip(PanelShape)) {
        Row(
            Modifier.fillMaxWidth().foldToggle(open, title) { open = !open }.heightIn(min = FoldMetrics.head).padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = titleColor, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            summary()
            FoldChevron(open, accent)
        }
        FoldBody(open) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

/**
 * Чип сводки свёрнутой панели: без [accent] - серый (число, счёт), с ним - заливка цветом акцента и тёмные или светлые слова по
 * её яркости (то, что ждёт действия). Не кнопка: касание уходит строке панели.
 */
@Composable fun SummaryChip(text: String, accent: Color? = null) {
    val look = if (accent == null) {
        Modifier.background(Color.White.copy(alpha = .06f), ChipShape).border(1.dp, Color.White.copy(alpha = .06f), ChipShape)
    } else {
        Modifier.background(Brush.verticalGradient(listOf(lerp(accent, Color.White, .25f), accent)), ChipShape)
    }
    Box(Modifier.heightIn(min = FoldMetrics.chip).then(look).padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = accent?.let(::inkOn) ?: Quiet,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (accent != null) FontWeight.Bold else null,
            maxLines = 1,
        )
    }
}

/**
 * Строка-свёртка: касание - [onToggle]; для чтения с экрана - кнопка с действием [label] и состоянием «Развёрнуто»/«Свёрнуто» по [open].
 * Общая для [FoldPanel] и свёрток внутри панелей, что держат своё раскрытие сами.
 */
fun Modifier.foldToggle(open: Boolean, label: String? = null, onToggle: () -> Unit): Modifier =
    clickable(role = Role.Button, onClickLabel = label, onClick = onToggle)
        .semantics { stateDescription = ui(if (open) "fold.open" else "fold.closed") }

/** Шеврон свёртки: вниз - свёрнуто, поворотом вверх - раскрыто. */
@Composable fun FoldChevron(open: Boolean, tint: Color = Muted, size: Dp = 18.dp) {
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "foldChevron")
    Icon(Icons.Outlined.ExpandMore, null, tint = tint, modifier = Modifier.size(size).rotate(turn))
}

/** Содержимое свёртки: раскрывается и сворачивается по вертикали с растворением; свёрнутое не компонуется. */
@Composable fun ColumnScope.FoldBody(open: Boolean, verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(open, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = verticalArrangement, content = content)
    }
}

/** Размеры свёртки: строка свёрнутой панели [head] (зона касания с запасом над 44 dp), высота чипа сводки [chip]. */
private object FoldMetrics {
    val head = 52.dp
    val chip = 24.dp
}

private val ChipShape = RoundedCornerShape(12.dp)
