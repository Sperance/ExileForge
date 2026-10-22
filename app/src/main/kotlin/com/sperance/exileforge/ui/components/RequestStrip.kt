package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.CallState
import com.sperance.exileforge.core.session.CallTimes
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Muted

/**
 * Что показывает полоса запроса ([RequestStrip]): ничего, ожидание с [Waiting.since] (попытка [Waiting.attempt] из [Waiting.attempts],
 * без счёта - null), нет связи с пояснением [Offline.note] и «Повторить» ([Offline.onRetry], без кнопки - null), связь восстановлена.
 */
sealed interface StripState {
    data object None : StripState

    data class Waiting(val since: Long, val attempt: Int, val attempts: Int?) : StripState

    data class Offline(val note: String, val onRetry: (() -> Unit)?) : StripState

    data object Restored : StripState
}

/**
 * Краски полосы в палитре экрана: [line] - бегущая линия ожидания, [track] - её дорожка, [restored] - тон вести «связь восстановлена»,
 * [muted] - пояснение второй строкой. «Долго отвечает» - тоном осторожности, «нет связи» - тревоги - у всех экранов одни.
 */
@Immutable data class StripTones(val line: Color, val track: Color, val restored: Color, val muted: Color = Muted)

/**
 * Полоса шага, что повторяется сам ([CallState]): «нет связи» с «Повторить» ([onRetry]) и пояснением [offlineNote] - по умолчанию
 * «выбор сохранён».
 */
fun CallState.strip(onRetry: () -> Unit, offlineNote: String = ui("call.offline_saved")): StripState = when (this) {
    CallState.Idle -> StripState.None
    is CallState.Waiting -> StripState.Waiting(since, attempt, attempts)
    is CallState.Lost -> StripState.Offline(offlineNote, onRetry)
    CallState.Restored -> StripState.Restored
}

/**
 * Нажатая кнопка [tag] крутит спиннер: её запрос ждёт ответа дольше [CallTimes.SPINNER_MS]. Часы читаются, только пока запрос этой
 * кнопки в полёте.
 */
@Composable fun CallState.spins(tag: Any?): Boolean {
    val waiting = this as? CallState.Waiting ?: return false
    if (waiting.tag != tag) return false
    val now by rememberNow(TICK_MS)
    return now - waiting.since >= CallTimes.SPINNER_MS
}

/**
 * Полоса состояния запроса под шапкой экрана, во всю ширину. В ожидании - тонкая бегущая линия; с [CallTimes.SLOW_MS] от нажатия -
 * полоса «Сервер долго отвечает · попытка N из M»; нет связи - «Нет связи», пояснение и «Повторить» справа (без повтора - только
 * пояснение); «Связь восстановлена» - коротко, пока её держит модель. Ничего - пустое место нулевой высоты.
 */
@Composable fun RequestStrip(state: StripState, tones: StripTones, modifier: Modifier = Modifier) {
    AnimatedContent(state, modifier.fillMaxWidth(), transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it::class }, label = "strip") { shown ->
        when (shown) {
            StripState.None -> Spacer(Modifier.fillMaxWidth())
            is StripState.Waiting -> WaitingStrip(shown, tones)
            is StripState.Offline -> StatusRow(Alarm, Alarm.copy(alpha = .14f)) {
                Icon(Icons.Outlined.CloudOff, null, tint = Alarm, modifier = Modifier.size(18.dp))
                Lines(ui("call.offline"), shown.note, Alarm, tones.muted, Modifier.weight(1f))
                shown.onRetry?.let { retry ->
                    ForgeButton(onClick = retry, kind = ButtonKind.Secondary, size = ButtonSize.Compact, accent = Alarm, icon = Icons.Outlined.Refresh) { Text(ui("call.retry"), maxLines = 1) }
                }
            }

            StripState.Restored -> StatusRow(tones.restored, tones.restored.copy(alpha = .12f)) {
                Icon(Icons.Outlined.Check, null, tint = tones.restored, modifier = Modifier.size(18.dp))
                Lines(ui("call.restored"), ui("call.restored_note"), tones.restored, tones.muted, Modifier.weight(1f))
            }
        }
    }
}

/** Ожидание: до [CallTimes.SLOW_MS] - тонкая бегущая линия, дальше - полоса «Сервер долго отвечает» с попыткой. */
@Composable private fun WaitingStrip(state: StripState.Waiting, tones: StripTones) {
    val now by rememberNow(TICK_MS)
    if (now - state.since < CallTimes.SLOW_MS) {
        LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = tones.line, trackColor = tones.track)
        return
    }
    StatusRow(Caution, Caution.copy(alpha = .12f)) {
        CircularProgressIndicator(Modifier.size(16.dp), color = Caution, strokeWidth = 2.dp)
        val attempt = state.attempts?.let { ui("call.attempt", state.attempt, it) } ?: ui("call.attempt_open", state.attempt)
        Lines(ui("call.slow"), attempt, Caution, tones.muted, Modifier.weight(1f))
    }
}

/** Строка полосы: фон [fill], черта цвета [rim] сверху и снизу, содержимое в ряд по центру. */
@Composable private fun StatusRow(rim: Color, fill: Color, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).background(fill)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(rim.copy(alpha = .45f), Offset(0f, 0f), Offset(size.width, 0f), stroke)
                drawLine(rim.copy(alpha = .45f), Offset(0f, size.height), Offset(size.width, size.height), stroke)
            }
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 14.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        content = content,
    )
}

/** Заголовок [title] цветом [tint] и пояснение [note] второй строкой; пустое пояснение не рисуется. */
@Composable private fun Lines(title: String, note: String, tint: Color, muted: Color, modifier: Modifier) {
    Column(modifier) {
        Text(title, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (note.isNotEmpty()) Text(note, color = muted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
    }
}

/** Тон «нет связи»: один на все экраны. */
private val Alarm = Color(0xFFFF8E74)

/** Такт часов ожидания, мс. */
private const val TICK_MS = 250L
