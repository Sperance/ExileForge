package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.character.MightShift
import com.sperance.exileforge.core.character.MightStanding
import com.sperance.exileforge.core.character.Mights
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.mightChange
import com.sperance.exileforge.core.display.mightLine
import com.sperance.exileforge.core.display.mightStandingText
import com.sperance.exileforge.core.display.mightText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.sheet.MightExtras
import com.sperance.exileforge.rules.sheet.MightReading
import com.sperance.exileforge.ui.theme.*

/** Цвет числа Мощи врага по его стойке против героя: серый - слабее вдвое, золото - сопоставим, красный - сильнее. */
fun MightStanding.tint(): Color = when (this) {
    MightStanding.WEAKER -> Muted
    MightStanding.MATCHED -> Caution
    MightStanding.STRONGER -> LifeRed
}

/**
 * Печать Мощи: кольцо, где дуга натиска ([MightReading.offenseShare], Ember) и стойкости (Rune) делят круг, ⚜ в середине.
 *
 * Мощь: печать - кольцо долей «натиск / стойкость» с ⚜ внутри; на листе героя - чип «⚜ 12.5k», у врага - мелко с числом
 * цвета его стойки, в списках - строка «⚜ 12.5k». Не прогноз боя.
 */
@Composable fun MightSeal(reading: MightReading, modifier: Modifier = Modifier, size: Dp = 92.dp) {
    val offense = reading.offenseShare.toFloat().coerceIn(0f, 1f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = maxOf(this.size.minDimension * RING_SHARE, MIN_RING.toPx())
            val inset = width / 2
            val arc = Size(this.size.width - width, this.size.height - width)
            val origin = Offset(inset, inset)
            drawArc(Bronze, 0f, FULL, false, origin, arc, style = Stroke(width))
            drawArc(Ember, START, FULL * offense, false, origin, arc, style = Stroke(width, cap = StrokeCap.Butt))
            drawArc(Rune, START + FULL * offense, FULL * (1 - offense), false, origin, arc, style = Stroke(width, cap = StrokeCap.Butt))
        }
        if (size >= GLYPH_FROM) Text(MIGHT_GLYPH, color = Caution, fontSize = (size.value * GLYPH_SHARE).sp)
    }
}

/**
 * Мощь героя на его листе: чип «⚜ 12.5k» высотой 28dp в строке с номером героя - одно число, без черты долей. Касание открывает
 * разбор ([MightBreakdown]). [reading] null - Мощь ещё не посчитана: «…» вместо числа, без разбора.
 */
@Composable fun MightMark(reading: MightReading?, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(modifier) {
        Row(
            Modifier.height(MARK_HEIGHT).clip(shape).background(Panel).border(1.dp, Bronze, shape)
                .clickable(enabled = reading != null, onClickLabel = ui("might.title")) { open = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(MIGHT_GLYPH, color = Caution, fontSize = 12.sp, lineHeight = 14.sp)
            Text(reading?.let { compactNumber(it.value) } ?: PENDING, color = GoldBright, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        if (open && reading != null) TipCallout(Caution, onClose = { open = false }) { MightBreakdown(reading) }
    }
}

/**
 * Разбор Мощи героя: «⚜ Мощь» и число целиком, одна короткая строка, что это за число, и одной строкой натиск и стойкость
 * своими цветами: «⚔ Натиск 12.5k · 🛡 Стойкость 40k».
 */
@Composable private fun ColumnScope.MightBreakdown(reading: MightReading) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(MIGHT_GLYPH, color = Caution, style = MaterialTheme.typography.titleSmall)
        Text(ui("might.title"), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(mightText(reading.value), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
    Text(ui("might.note"), color = Muted, style = MaterialTheme.typography.labelSmall)
    Text(
        buildAnnotatedString {
            side(OFFENSE_GLYPH, ui("might.offense"), compactNumber(reading.offense), Ember)
            withStyle(SpanStyle(color = Muted)) { append(SIDE_SEPARATOR) }
            side(DEFENSE_GLYPH, ui("might.defense"), compactNumber(reading.defense), Rune)
        },
        color = Parchment,
        style = MaterialTheme.typography.labelMedium,
    )
}

/** Сторона Мощи в строке разбора: знак, имя и короткое число цвета [hue]. */
private fun AnnotatedString.Builder.side(glyph: String, name: String, value: String, hue: Color) {
    append("$glyph $name ")
    withStyle(SpanStyle(color = hue, fontWeight = FontWeight.Bold)) { append(value) }
}

/** Мощь врага мелко: печать и короткое число цвета его стойки против героя ([FightMight]); без Мощи героя - золото. */
@Composable fun FoeMight(reading: MightReading, standing: MightStanding, modifier: Modifier = Modifier, seal: Dp = 12.dp, fontSize: TextUnit = 10.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        MightSeal(reading, size = seal)
        Text(compactNumber(reading.value), color = standing.tint(), fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** Мощь строкой списка «⚜ 12.5k»: карточка игрока, состав гильдии; ноль - сервер ещё не посчитал её: «⚜ …». */
@Composable fun MightLine(value: Double, modifier: Modifier = Modifier, tint: Color = Caution, fontSize: TextUnit = 11.sp) {
    Text(if (value > 0) mightLine(value) else ui("might.short", PENDING), color = tint, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = modifier)
}

/**
 * Сдвиг Мощи героя с вещью «⚜ Мощь было → **стало**» с плашкой разницы - строкой заголовка сравнения ([StatComparison]);
 * зовут, только когда сдвиг стоит показа ([MightShift.shown]).
 */
@Composable internal fun MightShiftLine(shift: MightShift, muted: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(MIGHT_GLYPH, color = Caution, style = MaterialTheme.typography.bodySmall)
        Text(ui("might.title"), color = Parchment, style = MaterialTheme.typography.bodySmall)
        ComparisonFigures(mightText(shift.before), mightText(shift.after), muted)
        DiffPill(mightChange(shift), shift.shift == GearVerdict.Shift.UP, muted)
    }
}

/**
 * Мощь в бою: Мощь героя [hero] по его телу боя - под эффектами карты - и Мощь каждого врага, сосчитанная один раз на
 * монстра. Одна на оверлей боя ([LocalFightMight]).
 */
@Stable
class FightMight internal constructor(
    private val index: ContentIndex,
    private val level: Int,
    val hero: MightReading?,
    private val known: MutableMap<RolledMonster, MightReading?> = HashMap(),
) {

    /** Мощь врага [monster]; у босса - null, её не показывают ([Mights.of]). */
    fun of(monster: RolledMonster): MightReading? = known.getOrPut(monster) { Mights.of(index, monster, level) }

    /** Стойка врага с Мощью [foe] против героя. */
    fun standing(foe: MightReading): MightStanding = MightStanding.of(foe.value, hero?.value ?: 0.0)

    /** Стойка словами: «сильнее на 27%». */
    fun standingText(foe: MightReading): String = mightStandingText(foe.value, hero?.value ?: 0.0)

    /** Та же память врагов с новой Мощью героя. */
    internal fun with(hero: MightReading?): FightMight = FightMight(index, level, hero, known)
}

/** Мощь боя на оверлее; null - контента ещё нет или экран не бой. */
val LocalFightMight = compositionLocalOf<FightMight?> { null }

/**
 * Мощь боя на уровне врагов [level]: враги - один раз на монстра, герой - заново, когда меняется его тело боя [hero]; [extras] -
 * что сверх листа из боевого листа героя: урон умений, резерв маны, фляги (`CombatSheet.mightExtras`).
 */
@Composable fun rememberFightMight(index: ContentIndex?, hero: Combatant?, level: Int, extras: MightExtras): FightMight? {
    index ?: return null
    val base = remember(index, level) { FightMight(index, level, null) }
    val reading = remember(index, hero?.stats, hero?.level, extras) { hero?.let { Mights.hero(index, it, extras) } }
    return remember(base, reading) { base.with(reading) }
}

private const val MIGHT_GLYPH = "⚜"

/** Мощь, которую сервер ещё не посчитал: многоточие вместо нуля. */
private const val PENDING = "…"

/** Высота чипа Мощи в строке с номером героя. */
private val MARK_HEIGHT = 28.dp

/** Знаки сторон Мощи в разборе и черта между ними. */
private const val OFFENSE_GLYPH = "⚔"
private const val DEFENSE_GLYPH = "🛡"
private const val SIDE_SEPARATOR = " · "

/** Толщина кольца печати долей её размера. */
private const val RING_SHARE = .076f

/** Тоньше кольцо не бывает: мелкая печать врага. */
private val MIN_RING = 2.dp

/** Кегль ⚜ долей размера печати; мельче [GLYPH_FROM] печать - одно кольцо. */
private const val GLYPH_SHARE = .17f
private val GLYPH_FROM = 40.dp

private const val START = -90f
private const val FULL = 360f
