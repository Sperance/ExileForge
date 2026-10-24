package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FeedbackSort
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Список отчётов - один облик у модерации и у игрока: над строками поиск, ряд чипов отбора со счётом ([ReportChipRow], [ReportChip]) и
 * порядок сегментами ([ReportSortTabs]); строка отчёта свёрнута в две линии ([ReportLine]), касание раскрывает на месте тикет
 * ([ReportTicket]). Что отбирает чип и где лежат отчёты, решает экран: у модерации - очередь сервера страницами, у игрока - свои отчёты
 * целиком.
 */
@Composable fun ReportChipRow(content: @Composable RowScope.() -> Unit) {
    val cue = rememberRowScrollCue()
    Row(Modifier.scrollCue(cue).horizontalScroll(cue.state), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

/**
 * Чип отбора списка отчётов [title] со счётом [count] (null - счёта ещё нет) и красной точкой [dot] - в отборе есть новые ответы; для
 * чтеца экрана точку называет [dotDescription].
 */
@Composable fun ReportChip(title: String, count: Int?, chosen: Boolean, dot: Boolean = false, dotDescription: String? = null, onClick: () -> Unit) {
    ForgeChip(
        title,
        chosen,
        onClick,
        mode = ChipMode.Choice,
        count = count,
        leading = ReplyDot.takeIf { dot },
        contentDescription = dotDescription.takeIf { dot },
    )
}

/** Красная точка чипа: в отборе есть ответы, что ещё не видены. */
private val ReplyDot: @Composable () -> Unit = { Box(Modifier.size(8.dp).background(LifeRed, CircleShape)) }

/** Порядок списка отчётов сегментами без подписи: свежие первыми, по номеру Issue. */
@Composable fun ReportSortTabs(sort: FeedbackSort, onSort: (FeedbackSort) -> Unit) {
    PillTabs(FeedbackSort.entries.map { ui(it.key) }, sort.ordinal, { onSort(FeedbackSort.entries[it]) }, segmented = true)
}

/** Подпись сегмента порядка отчётов. */
private val FeedbackSort.key: String
    get() = when (this) {
        FeedbackSort.NEWEST -> "feedback.order.newest"
        FeedbackSort.ISSUE -> "feedback.order.issue"
    }

/**
 * Свёрнутая строка отчёта в две линии, судьба - полосой цвета [fate] по левому краю: первая непустая строка текста [text] и справа
 * [corner] (дата, повторы); ниже - вид отчёта значком и словом ([ReportKindLabel]) и метки места [marks]. Касание раскрывает тикет
 * ([onOpen]); для чтеца экрана строка - кнопка в состоянии «свёрнут».
 */
@Composable fun ReportLine(text: String, kind: FeedbackKind, fate: ReportFate, corner: String, onOpen: () -> Unit, marks: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val collapsed = ui("feedback.line.collapsed")
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, Bronze, shape).drawBehind { drawRect(fate.tint, size = Size(FATE_STRIP.toPx(), size.height)) }
            .clickable(role = Role.Button, onClickLabel = ui("feedback.line.expand"), onClick = onOpen)
            .semantics { stateDescription = collapsed }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(corner, color = Muted, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportKindLabel(kind)
            marks()
        }
    }
}

/** Метка строки отчёта - [text] на подложке своего цвета [tint]: «новый ответ», «Закрыт автором». */
@Composable fun LineMark(text: String, tint: Color) {
    ToneTag(text, tint, look = TagLook.Filled)
}

/** Ширина полосы судьбы по левому краю строки отчёта. */
private val FATE_STRIP = 3.dp
