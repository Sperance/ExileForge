package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FoeView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Parchment
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * Отметка выбора цели на враге: [TARGET] - фокус игрока, и удар уйдёт ему; [QUEUED] - фокус за живым провокатором, ляжет после его
 * смерти; [TAUNT] - сам провокатор, пока стоит, бить можно только его. Подпись - `fight.mark.<вид>`, цвета - чернила [ink] на
 * заливке [fill] в окантовке [edge].
 */
internal enum class FocusMark(val ink: Color, val fill: Color, val edge: Color) {
    TARGET(Color(0xFF2A1A06), FrameGoldBright, FrameGoldBright),
    QUEUED(Color(0xFFBFE6FF), Color(0xFF0F2633), Color(0xFF8FD3FF)),
    TAUNT(Color(0xFFFFC9C2), Color(0xFF3A1210), Color(0xFFD24A43)),
    ;

    val label: String get() = ui("fight.mark.${name.lowercase()}")
}

/**
 * Отметка врага [foe] в бою [fight]: фокус досягаемого - цель, за провокатором - очередь; провокатор - своей отметкой, если [taunt]
 * (у карточки врага провокатор отмечен своим знаком). Павший и конец боя - без отметки.
 */
internal fun focusMark(foe: FoeView, fight: FightHud, taunt: Boolean = true): FocusMark? = when {
    !foe.alive || fight.outcome != null -> null
    fight.focus == foe.index -> if (foe.reachable) FocusMark.TARGET else FocusMark.QUEUED
    taunt && foe.taunt -> FocusMark.TAUNT
    else -> null
}

/** Ярлык отметки [mark] над врагом: капсула заглавных букв. */
@Composable internal fun FocusBadge(mark: FocusMark, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Text(
        mark.label,
        color = mark.ink,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.background(mark.fill, shape).border(1.dp, mark.edge, shape).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** Уголки рамки цели цветом отметки [mark]: скоба в каждом углу поверх содержимого; без отметки и у провокатора - без них. */
internal fun Modifier.focusCorners(mark: FocusMark?): Modifier = if (mark == null || mark == FocusMark.TAUNT) {
    this
} else {
    drawWithContent {
        drawContent()
        val l = 12.dp.toPx()
        val w = 2.dp.toPx()
        val tint = mark.edge
        listOf(Offset(0f, 0f) to Offset(1f, 1f), Offset(size.width, 0f) to Offset(-1f, 1f), Offset(0f, size.height) to Offset(1f, -1f), Offset(size.width, size.height) to Offset(-1f, -1f))
            .forEach { (corner, dir) ->
                drawLine(tint, corner, corner + Offset(l * dir.x, 0f), w, StrokeCap.Square)
                drawLine(tint, corner, corner + Offset(0f, l * dir.y), w, StrokeCap.Square)
            }
    }
}

/**
 * Подсказка арены о выборе цели: фокус за провокатором ([TauntFirst] - кто станет целью и после кого) или касание тотема ([Totem] -
 * он неуязвим и рассыпется через столько секунд).
 */
internal sealed interface FocusHint {
    data class TauntFirst(val target: String, val taunter: String) : FocusHint

    data class Totem(val left: Double) : FocusHint
}

/** Сколько подсказка выбора цели стоит на арене, мс. */
private const val HINT_MS = 3_000L

/**
 * Карточка подсказки [hint] над ареной: знак в кружке, строка и пояснение; уходит сама через [HINT_MS] или касанием. Бой при
 * этом идёт.
 */
@Composable internal fun FocusHintCard(hint: FocusHint, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(hint) {
        delay(HINT_MS)
        onDismiss()
    }
    val (glyph, tint, title, note) = when (hint) {
        is FocusHint.TauntFirst -> HintFace("⛨", FocusMark.TAUNT, ui("fight.hint.taunt_first"), ui("fight.hint.taunt_first_note", hint.target, hint.taunter))
        is FocusHint.Totem -> HintFace("🔒", FocusMark.TARGET, ui("fight.hint.totem", ceil(hint.left).toInt()), null)
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).background(HintFill, shape).border(1.dp, HintEdge, shape)
            .clickable(onClickLabel = ui("common.close"), onClick = onDismiss).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).background(tint.fill, CircleShape).border(1.dp, tint.edge, CircleShape), contentAlignment = Alignment.Center) {
            Text(glyph, color = tint.ink, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Parchment, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            note?.let { Text(it, color = HintNote, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** Облик подсказки: знак, цвета отметки, строка и пояснение. */
private data class HintFace(val glyph: String, val tint: FocusMark, val title: String, val note: String?)

private val HintFill = Color(0xF21A242B)
private val HintEdge = Color(0xFF3A4A55)
private val HintNote = Color(0xFFA9B8C2)
