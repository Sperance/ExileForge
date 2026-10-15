package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.sperance.exileforge.core.display.mightFactor
import com.sperance.exileforge.core.display.mightLine
import com.sperance.exileforge.core.display.mightStandingText
import com.sperance.exileforge.core.display.mightText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.sheet.MightDefense
import com.sperance.exileforge.rules.sheet.MightExtras
import com.sperance.exileforge.rules.sheet.MightReading
import com.sperance.exileforge.ui.theme.*

/*
 * Мощь (4.8.0, макет A+B; 4.9.x - чип, макет A): печать - кольцо долей «натиск / стойкость» (Ember / Rune) с ⚜ внутри; на листе
 * героя - чип «⚜ 12.5k» с чертой долей в строке с номером героя, у врага - мелко с числом цвета его стойки против героя; в списках -
 * строка «⚜ 12.5k». Не прогноз боя.
 */

/** Цвет числа Мощи врага по его стойке против героя: серый - слабее вдвое, золото - сопоставим, красный - сильнее. */
fun MightStanding.tint(): Color = when (this) {
    MightStanding.WEAKER -> Muted
    MightStanding.MATCHED -> Caution
    MightStanding.STRONGER -> LifeRed
}

/** Печать Мощи: кольцо, где дуга натиска ([MightReading.offenseShare], Ember) и стойкости (Rune) делят круг, ⚜ в середине. */
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
 * Мощь героя на его листе (4.9.x, макет «Мощь героя», вариант A): чип «⚜ 12.5k» высотой 28dp в строке с номером героя, под числом -
 * черта 2dp из долей натиска (Ember) и стойкости (Rune). Касание открывает разбор ([MightBreakdown]). [reading] null - Мощь ещё не
 * посчитана: «…» вместо числа, без черты и разбора; [defense] - из чего сложена стойкость, без него разбор без частей.
 */
@Composable fun MightMark(reading: MightReading?, defense: MightDefense?, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(modifier) {
        Column(
            Modifier.height(MARK_HEIGHT).width(IntrinsicSize.Max).clip(shape).background(Panel).border(1.dp, Bronze, shape)
                .clickable(enabled = reading != null, onClickLabel = ui("might.title")) { open = true }
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(MIGHT_GLYPH, color = Caution, fontSize = 12.sp, lineHeight = 14.sp)
                Text(reading?.let { compactNumber(it.value) } ?: PENDING, color = GoldBright, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.weight(1f))
            reading?.let { MightShares(it.offenseShare, Modifier.fillMaxWidth().height(2.dp)) }
        }
        if (open && reading != null) TipCallout(Caution, onClose = { open = false }) { MightBreakdown(reading, defense) }
    }
}

/** Двухцветная черта долей Мощи: натиск ([offense], Ember) слева, стойкость (Rune) справа, между ними просвет 1dp. */
@Composable private fun MightShares(offense: Double, modifier: Modifier) {
    val share = offense.toFloat().coerceIn(MIN_SHARE, 1 - MIN_SHARE)
    Row(modifier.clip(RoundedCornerShape(1.dp)), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        Box(Modifier.weight(share).fillMaxHeight().background(Ember))
        Box(Modifier.weight(1 - share).fillMaxHeight().background(Rune))
    }
}

/** Часть стойкости в разборе: подпись, число и цвет метки. */
private class DefensePart(val label: String, val value: String, val hue: Color)

/** Части стойкости по порядку разбора: запас, мана перед здоровьем, затем множители эффективного здоровья. */
private fun MightDefense.parts(): List<DefensePart> = listOf(
    DefensePart(ui("might.part.pool"), mightText(pool), PoolHue),
    DefensePart(ui("might.part.mana"), "+" + mightText(manaShield), ManaShieldHue),
    DefensePart(ui("might.part.avoidance"), mightFactor(1 / avoidance), Rune),
    DefensePart(ui("might.part.less"), mightFactor(1 / lessTaken), LessTakenHue),
    DefensePart(ui("might.part.sustain"), mightFactor(sustainFactor), Vital),
)

/**
 * Разбор Мощи героя (макет A): «⚜ Мощь» и число целиком, формула «√ натиск × стойкость», натиск (урон в секунду) и стойкость
 * (эффективное здоровье) своими цветами; под стойкостью - её части из [defense] правил (`Might.defense`): здоровье и щит, мана перед
 * здоровьем, множители подавления и отражения, меньшего получаемого урона и пополнения (реген, вампиризм, флаконы). Внизу - «не прогноз боя».
 */
@Composable private fun ColumnScope.MightBreakdown(reading: MightReading, defense: MightDefense?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(MIGHT_GLYPH, color = Caution, style = MaterialTheme.typography.titleSmall)
        Text(ui("might.title"), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(mightText(reading.value), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
    Text(ui("might.formula"), color = Muted, style = MaterialTheme.typography.labelSmall)
    MightSide(ui("might.offense"), ui("might.offense_unit"), compactNumber(reading.offense), Ember)
    MightSide(ui("might.defense"), ui("might.defense_unit"), compactNumber(reading.defense), Rune)
    defense?.parts()?.forEach { part ->
        Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(part.hue))
            Text(part.label, color = Parchment, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(part.value, color = Rune, style = MaterialTheme.typography.labelMedium)
        }
    }
    HorizontalDivider(color = Bronze)
    Text(ui("might.note"), color = Muted, style = MaterialTheme.typography.labelSmall)
}

/** Сторона Мощи в разборе: точка цвета [hue], имя, что она мерит, и короткое число. */
@Composable private fun MightSide(name: String, unit: String, value: String, hue: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(hue))
        Text(name, color = Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(unit, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = hue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** Мощь врага мелко (4.8.0): печать и короткое число цвета его стойки против героя ([FightMight]); без Мощи героя - золото. */
@Composable fun FoeMight(reading: MightReading, standing: MightStanding, modifier: Modifier = Modifier, seal: Dp = 12.dp, fontSize: TextUnit = 10.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        MightSeal(reading, size = seal)
        Text(compactNumber(reading.value), color = standing.tint(), fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** Мощь строкой списка «⚜ 12.5k» (4.8.0): карточка игрока, состав гильдии; ноль - сервер ещё не посчитал её (4.9.0): «⚜ …». */
@Composable fun MightLine(value: Double, modifier: Modifier = Modifier, tint: Color = Caution, fontSize: TextUnit = 11.sp) {
    Text(if (value > 0) mightLine(value) else ui("might.short", PENDING), color = tint, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = modifier)
}

/**
 * Мощь героя «было → стало» с вещью (4.8.0) - строкой сравнения, как у [StatComparison], с плашкой разницы; одна строка Мощи в
 * карточке (0.14.5, #44), и только когда сдвиг стоит показа ([MightShift.shown]).
 */
@Composable fun MightComparison(shift: MightShift, look: RelicLook? = null) {
    if (!shift.shown) return
    val muted = look?.muted ?: Muted
    val faint = look?.faint ?: Bronze
    val better = shift.shift == GearVerdict.Shift.UP
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(MIGHT_GLYPH, color = Caution, style = MaterialTheme.typography.bodySmall)
        Text(ui("might.title"), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        ComparisonFigures(mightText(shift.before), mightText(shift.after), muted, faint)
        DiffPill(mightChange(shift), better, muted)
    }
}

/**
 * Мощь в бою (4.8.0): Мощь героя [hero] по его телу боя - под эффектами карты - и Мощь каждого врага, сосчитанная один раз на
 * монстра. Одна на оверлей боя ([LocalFightMight]).
 */
@Stable
class FightMight internal constructor(
    private val index: ContentIndex,
    private val level: Int,
    val hero: MightReading?,
    private val known: MutableMap<RolledMonster, MightReading> = HashMap(),
) {

    /** Мощь врага [monster]. */
    fun of(monster: RolledMonster): MightReading = known.getOrPut(monster) { Mights.of(index, monster, level) }

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
 * Мощь боя на уровне врагов [level]: враги - один раз на монстра, герой - заново, когда меняется его тело боя [hero]; [extras] (4.9.x) -
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

/** Высота чипа Мощи в строке с номером героя (вариант A). */
private val MARK_HEIGHT = 28.dp

/** Тоньше доли черта натиска и стойкости не бывает: обе стороны видны всегда. */
private const val MIN_SHARE = .04f

/** Метки частей стойкости в разборе (макет A): запас - сталь, мана перед здоровьем - бледная синь, меньший урон - лёд. */
private val PoolHue = Color(0xFF5E7F95)
private val ManaShieldHue = Color(0xFF7FB2D4)
private val LessTakenHue = Color(0xFFB4E2FF)

/** Толщина кольца печати долей её размера. */
private const val RING_SHARE = .076f

/** Тоньше кольцо не бывает: мелкая печать врага. */
private val MIN_RING = 2.dp

/** Кегль ⚜ долей размера печати; мельче [GLYPH_FROM] печать - одно кольцо. */
private const val GLYPH_SHARE = .17f
private val GLYPH_FROM = 40.dp

private const val START = -90f
private const val FULL = 360f
