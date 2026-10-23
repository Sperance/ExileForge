package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.AwakeningDay
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.rules.content.AwakenedWins
import com.sperance.exileforge.rules.content.AwakeningRule
import com.sperance.exileforge.rules.content.AwakeningStanding
import com.sperance.exileforge.rules.content.Awakenings
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.dashedBorder
import com.sperance.exileforge.ui.components.rememberServerLeft
import com.sperance.exileforge.ui.components.rememberServerNow
import com.sperance.exileforge.ui.theme.Awakened
import com.sperance.exileforge.ui.theme.AwakenedDim
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Vital
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Пробуждения суток по часам сервера с победами героя [wins]: тройка дня считается правилом [awakenings] без запроса и меняется ровно в
 * смену суток - одно ожидание до смены, без тикера.
 */
@Composable internal fun rememberAwakeningDay(awakenings: Awakenings, wins: AwakenedWins): AwakeningDay {
    val opened = rememberServerNow(DAY_TICK_MS)
    val day by produceState(awakenings.day(opened), awakenings, opened) {
        while (true) {
            delay((awakenings.changeAt(value) - ServerClock.now()).coerceAtLeast(0) + CHANGE_GRACE_MS)
            value = awakenings.day(ServerClock.now())
        }
    }
    return remember(awakenings, day, wins) { AwakeningDay.of(awakenings, day, wins) }
}

/** Смена пробуждений словами: местное время смены [clock] («03:00») и сколько до неё [left] («5 ч 12 мин»). */
internal data class AwakeningChange(val clock: String, val left: String)

/** Смена пробуждений [changeAt] (мс эпохи сервера) живой подписью: остаток идёт по часам сервера. */
@Composable internal fun awakeningChange(changeAt: Long): AwakeningChange {
    val left = rememberServerLeft(changeAt, CHANGE_TICK_MS)
    val minutes = (left + MINUTE_MS - 1) / MINUTE_MS
    val clock = remember(changeAt) { CLOCK.format(Instant.ofEpochMilli(changeAt - ServerClock.offset).atZone(ZoneId.systemDefault())) }
    return AwakeningChange(clock, ui("time.hours_minutes", minutes / 60, minutes % 60))
}

/**
 * Печать-глаз пробуждения в мере [radius] вокруг [center]: ждёт - глаз цвета пробуждения; побеждён сегодня - ромб с галочкой
 * ([Vital]) - другой формы, чем круглая галочка «пройден»; [muted] - пробуждение неоткрытой зоны, пунктиром и приглушённо.
 */
internal fun DrawScope.awakeningSeal(center: Offset, radius: Float, standing: AwakeningStanding, muted: Boolean) {
    val beaten = standing == AwakeningStanding.BEATEN
    val ink = when {
        muted -> AwakenedDim
        beaten -> Vital
        else -> Awakened
    }
    val rim = Stroke(radius * .16f, pathEffect = if (muted) PathEffect.dashPathEffect(floatArrayOf(radius * .2f, radius * .2f)) else null)
    if (beaten) {
        val diamond = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius, center.y)
            close()
        }
        drawPath(diamond, SEAL_FILL)
        drawPath(diamond, ink, style = rim)
        val tick = Path().apply {
            moveTo(center.x - radius * .42f, center.y + radius * .02f)
            lineTo(center.x - radius * .1f, center.y + radius * .32f)
            lineTo(center.x + radius * .45f, center.y - radius * .3f)
        }
        drawPath(tick, ink, style = Stroke(radius * .2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        return
    }
    drawCircle(SEAL_FILL, radius, center)
    drawCircle(ink, radius, center, style = rim)
    val w = radius * .7f
    val eye = Path().apply {
        moveTo(center.x - w, center.y)
        quadraticTo(center.x, center.y - w * 1.4f, center.x + w, center.y)
        quadraticTo(center.x, center.y + w * 1.4f, center.x - w, center.y)
        close()
    }
    drawPath(eye, ink, style = Stroke(radius * .15f))
    drawCircle(ink, radius * .24f, center)
}

/** Печать пробуждения [standing] размером [size] - в ленте карт, в поиске и в чипе отбора. */
@Composable internal fun AwakeningSeal(standing: AwakeningStanding, size: Dp, modifier: Modifier = Modifier) {
    val label = ui("awakening.seal")
    Canvas(modifier.size(size).semantics { contentDescription = label }) { awakeningSeal(center, this.size.minDimension / 2 - 1.dp.toPx(), standing, muted = false) }
}

/**
 * Полоса пробуждения над плитой выбранной карты, что будит стража ([Awakenings.wakes]): ждёт - «Пробуждает стража» и числа правила
 * [rule] (здоровье, урон, ярость, добыча), одна победа в сутки и смена [changeAt]; побеждён сегодня - что карта даст обычного стража
 * и когда снова.
 */
@Composable internal fun AwakeningBand(rule: AwakeningRule, standing: AwakeningStanding, changeAt: Long) {
    val change = awakeningChange(changeAt)
    val shape = RoundedCornerShape(10.dp)
    val beaten = standing == AwakeningStanding.BEATEN
    Column(
        Modifier.fillMaxWidth()
            .background(if (beaten) SolidColor(BEATEN_TINT) else Brush.horizontalGradient(listOf(Awakened.copy(alpha = .15f), Awakened.copy(alpha = .03f))), shape)
            .border(1.dp, (if (beaten) Vital else Awakened).copy(alpha = .35f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AwakeningSeal(standing, 16.dp)
            if (beaten) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ui("awakening.beaten"), color = Vital, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    MutedText(ui("awakening.beaten_note", change.clock, change.left), style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(ui("awakening.wakes"), color = Awakened, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(ui("awakening.limit") + "\n" + ui("awakening.change", change.clock, change.left), color = Quiet, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
            }
        }
        if (!beaten) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Figure(ui("awakening.times", number(rule.health)), ui("awakening.health"))
                Figure(ui("awakening.times", number(rule.damage)), ui("awakening.damage"))
                Figure(ui("awakening.seconds", number(rule.enrage)), ui("awakening.rage"))
                Figure(ui("awakening.times", number(rule.loot)), ui("awakening.loot"))
            }
        }
    }
}

/** Подсказка зоны дня, когда выбранная карта стража не будит: какая карта его будит. */
@Composable internal fun AwakeningHint(rule: AwakeningRule, lang: Lang) {
    val text = ui("awakening.hint", rarityTitle(rule.rarity, lang))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).dashedBorder(Awakened.copy(alpha = .55f), RectangleShape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AwakeningSeal(AwakeningStanding.WAITING, 16.dp)
        Text(text, color = Parchment, style = MaterialTheme.typography.bodySmall)
    }
}

/** Число полосы пробуждения: значение ярко, подпись под ним. */
@Composable private fun Figure(value: String, label: String) {
    Column {
        Text(value, color = Awakened, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

private val SEAL_FILL = Color(0xFF1A1109)
private val BEATEN_TINT = Color(0x147BE0A6)
private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Такт «сейчас» на открытии: дальше смену суток ждёт одно ожидание. */
private const val DAY_TICK_MS = 86_400_000L

/** Такт подписи «через N ч M мин». */
private const val CHANGE_TICK_MS = 30_000L

/** Запас после смены суток: часы сервера сверены до задержки ответа. */
private const val CHANGE_GRACE_MS = 1_000L
private const val MINUTE_MS = 60_000L
