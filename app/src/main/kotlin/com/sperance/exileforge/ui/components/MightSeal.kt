package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.combat.Combatant
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
import com.sperance.exileforge.rules.sheet.MightReading
import com.sperance.exileforge.ui.theme.*

/*
 * Мощь (4.8.0, макет A+B): печать - кольцо долей «натиск / стойкость» (Ember / Rune) с ⚜ внутри; на листе героя крупно с числом
 * Cinzel, у врага - мелко с числом цвета его стойки против героя; в списках - строка «⚜ 12.5k». Не прогноз боя.
 */

/** Цвет числа Мощи врага по его стойке против героя: серый - слабее вдвое, золото - сопоставим, красный - сильнее. */
fun MightStanding.tint(): Color = when (this) {
    MightStanding.WEAKER -> Muted
    MightStanding.MATCHED -> Caution
    MightStanding.STRONGER -> LifeRed
}

/**
 * Печать Мощи: кольцо, где дуга натиска ([MightReading.offenseShare], Ember) и стойкости (Rune) делят круг, ⚜ в середине. [glow] -
 * тихое свечение кольца по часам декора (только крупная печать листа героя; без анимаций стоит).
 */
@Composable fun MightSeal(reading: MightReading, modifier: Modifier = Modifier, size: Dp = 92.dp, glow: Boolean = false) {
    val phase by motionPhase(GLOW_PERIOD_MS, still = .5f)
    val offense = reading.offenseShare.toFloat().coerceIn(0f, 1f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = maxOf(this.size.minDimension * RING_SHARE, MIN_RING.toPx())
            val inset = width / 2
            val arc = Size(this.size.width - width, this.size.height - width)
            val origin = Offset(inset, inset)
            if (glow) {
                val pulse = .5f + .5f * kotlin.math.sin(phase * 2 * Math.PI.toFloat())
                drawCircle(Brush.radialGradient(listOf(Caution.copy(alpha = .10f + .08f * pulse), Color.Transparent)), radius = this.size.minDimension / 2)
            }
            drawArc(Bronze, 0f, FULL, false, origin, arc, style = Stroke(width))
            drawArc(Ember, START, FULL * offense, false, origin, arc, style = Stroke(width, cap = StrokeCap.Butt))
            drawArc(Rune, START + FULL * offense, FULL * (1 - offense), false, origin, arc, style = Stroke(width, cap = StrokeCap.Butt))
        }
        if (size >= GLYPH_FROM) Text(MIGHT_GLYPH, color = Caution, fontSize = (size.value * GLYPH_SHARE).sp)
    }
}

/**
 * Мощь героя на его листе (4.8.0, макет A): крупная печать, подпись «МОЩЬ», число Cinzel и доли «⚔ натиск 18.2k/с · ⛨ стойкость
 * 8.6k». Касание - короткий разбор: что такое натиск и стойкость; шансов боя здесь нет.
 */
@Composable fun MightPanel(reading: MightReading, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Tipped(
        {
            Tip(
                ui("might.title"),
                ui("might.tip"),
                Caution,
                listOf(ui("might.offense_fact") to compactNumber(reading.offense), ui("might.defense_fact") to compactNumber(reading.defense)),
            )
        },
        modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(PanelRaised, Panel))).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MightSeal(reading, glow = true)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(ui("might.title").uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                Text(mightText(reading.value), color = GoldBright, style = relicName(32))
                MightShares(reading, MaterialTheme.typography.labelSmall.fontSize)
            }
        }
    }
}

/** Доли Мощи строкой: «⚔ натиск 18.2k/с · ⛨ стойкость 8.6k» цветами своих дуг. */
@Composable private fun MightShares(reading: MightReading, size: TextUnit) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Ember)) { append(ui("might.offense_line", compactNumber(reading.offense))) }
            withStyle(SpanStyle(color = Muted)) { append(" · ") }
            withStyle(SpanStyle(color = Rune)) { append(ui("might.defense_line", compactNumber(reading.defense))) }
        },
        fontSize = size,
    )
}

/** Мощь врага мелко (4.8.0): печать и короткое число цвета его стойки против героя ([FightMight]); без Мощи героя - золото. */
@Composable fun FoeMight(reading: MightReading, standing: MightStanding, modifier: Modifier = Modifier, seal: Dp = 12.dp, fontSize: TextUnit = 10.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        MightSeal(reading, size = seal)
        Text(compactNumber(reading.value), color = standing.tint(), fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** Мощь строкой списка «⚜ 12.5k» (4.8.0): карточка игрока, состав гильдии; пусто - Мощь ещё не посчитана сервером. */
@Composable fun MightLine(value: Double, modifier: Modifier = Modifier, tint: Color = Caution, fontSize: TextUnit = 11.sp) {
    if (value <= 0) return
    Text(mightLine(value), color = tint, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = modifier)
}

/** Мощь героя «было → стало» с вещью (4.8.0) - строкой сравнения, как у [StatComparison], с плашкой разницы. */
@Composable fun MightComparison(shift: MightShift, look: RelicLook? = null) {
    val muted = look?.muted ?: Muted
    val faint = look?.faint ?: Bronze
    val better = when {
        mightText(shift.after) == mightText(shift.before) -> null
        else -> shift.after > shift.before
    }
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

/** Мощь боя на уровне врагов [level]: враги - один раз на монстра, герой - заново, когда меняется его тело боя [hero]. */
@Composable fun rememberFightMight(index: ContentIndex?, hero: Combatant?, level: Int): FightMight? {
    index ?: return null
    val base = remember(index, level) { FightMight(index, level, null) }
    val reading = remember(index, hero?.stats, hero?.level) { hero?.let { Mights.of(index, it) } }
    return remember(base, reading) { base.with(reading) }
}

private const val MIGHT_GLYPH = "⚜"

/** Толщина кольца печати долей её размера. */
private const val RING_SHARE = .076f

/** Тоньше кольцо не бывает: мелкая печать врага. */
private val MIN_RING = 2.dp

/** Кегль ⚜ долей размера печати; мельче [GLYPH_FROM] печать - одно кольцо. */
private const val GLYPH_SHARE = .17f
private val GLYPH_FROM = 40.dp

private const val START = -90f
private const val FULL = 360f

/** Период тихого свечения печати листа героя. */
private const val GLOW_PERIOD_MS = 4_000
