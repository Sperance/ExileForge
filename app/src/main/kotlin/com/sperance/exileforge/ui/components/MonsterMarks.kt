package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Bestiary
import com.sperance.exileforge.core.display.BestiaryTally
import com.sperance.exileforge.core.display.MonsterMarkView
import com.sperance.exileforge.core.display.StrengthChip
import com.sperance.exileforge.core.hero.HeroReads
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MarkGroup
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.Acid
import com.sperance.exileforge.ui.theme.Boon
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Crafted
import com.sperance.exileforge.ui.theme.EmberText
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Seam
import com.sperance.exileforge.ui.theme.ShieldCyan
import com.sperance.exileforge.ui.theme.Vital
import org.koin.compose.koinInject

/**
 * Счёт Бестиария героя [heroId] по контенту [index] - по нему открываются слабости видов в бою и в досье; читается раз на героя
 * (общее свежее чтение `hero/stats`), null - ещё не прочитан или чтение не удалось.
 */
@Composable fun rememberHunt(index: ContentIndex?, heroId: String): BestiaryTally? {
    val reads = koinInject<HeroReads>()
    val stats by produceState<Map<String, Long>?>(null, heroId) { value = heroId.takeIf { it.isNotBlank() }?.let { reads.stats(it) } }
    return remember(index, stats) { index?.let { content -> stats?.let { BestiaryTally(Bestiary(content), it) } } }
}

/**
 * Силы и слабости вида чипами, как в шапке листа врага и в досье босса: силы - первой строкой, цветом своего смысла со значком; слабости -
 * второй, серым в пунктирной рамке со знаком «⊖»; пока слабости скрыты - чип «? слабость». Касание чипа - подсказка с числом.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkChips(view: MonsterMarkView, modifier: Modifier = Modifier) {
    if (view.empty) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (view.strengths.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                view.strengths.forEach { chip -> StrengthPill(chip) }
            }
        }
        if (view.weaknesses.isNotEmpty() || view.hidden) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                view.weaknesses.forEach { chip -> WeakPill(chip.title, ui("marks.weakness"), chip.hint) }
                view.hiddenHint?.let { hint -> WeakPill(ui("marks.hidden"), ui("marks.weakness"), hint) }
            }
        }
    }
}

/**
 * Силы и слабости вида значками - строка Бестиария: силы цветом своего смысла, за чертой слабости серым; скрытые слабости - «?».
 */
@Composable fun MarkGlyphs(view: MonsterMarkView, modifier: Modifier = Modifier) {
    if (view.empty) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        view.strengths.forEach { chip ->
            val tint = markTint(chip)
            StatIcon(chip.strength.core.code, tint, Modifier.size(GlyphSide).semantics { contentDescription = chip.title })
        }
        if (view.weaknesses.isNotEmpty() || view.hidden) {
            if (view.strengths.isNotEmpty()) Spacer(Modifier.width(1.dp).heightIn(min = 12.dp).background(Seam))
            view.weaknesses.forEach { chip -> StatIcon(chip.weakness.core.code, Quiet, Modifier.size(GlyphSide).semantics { contentDescription = chip.title }, muted = true) }
            if (view.hidden) Text("?", color = Quiet, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** Цвет силы по смыслу ([MarkGroup]); сила недугов - цветом своего недуга. */
fun markTint(chip: StrengthChip): Color = when (chip.strength.group) {
    MarkGroup.AILMENTS -> AILMENT_TINTS[chip.ailment] ?: Acid
    else -> GROUP_TINTS.getValue(chip.strength.group)
}

@Composable private fun StrengthPill(chip: StrengthChip) {
    val tint = markTint(chip)
    Tipped({ Tip(chip.title, chip.hint, tint) }, Modifier.semantics { contentDescription = "${ui("marks.strength")}: ${chip.title}" }) {
        Row(
            Modifier.background(PanelRaised, Pill).border(1.dp, tint.copy(alpha = .45f), Pill).heightIn(min = PillHeight).padding(start = 6.dp, end = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatIcon(chip.strength.core.code, tint, Modifier.size(PillIcon))
            Text(chip.title, color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun WeakPill(title: String, kind: String, hint: String) {
    Tipped({ Tip(title, hint, Quiet) }, Modifier.semantics { contentDescription = "$kind: $title" }) {
        Row(
            Modifier.dashedPill(Seam).heightIn(min = PillHeight).padding(start = 6.dp, end = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MinusRing(Quiet, PillIcon)
            Text(title, color = Quiet, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Знак «⊖» слабости: кольцо с чертой. */
@Composable private fun MinusRing(tint: Color, side: Dp) {
    Canvas(Modifier.size(side)) {
        val stroke = 1.4.dp.toPx()
        val r = size.minDimension / 2 - stroke
        drawCircle(tint, r, style = Stroke(stroke))
        drawLine(tint, Offset(center.x - r * .5f, center.y), Offset(center.x + r * .5f, center.y), stroke * 1.2f)
    }
}

/** Пунктирная рамка капсулы цвета [tint]: путь и штрих собраны раз на размер. */
private fun Modifier.dashedPill(tint: Color): Modifier = drawWithCache {
    val width = 1.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    val stroke = Stroke(width, pathEffect = dash)
    val radius = CornerRadius(size.height / 2, size.height / 2)
    onDrawBehind {
        drawRoundRect(tint, topLeft = Offset(width / 2, width / 2), size = size.copy(size.width - width, size.height - width), cornerRadius = radius, style = stroke)
    }
}

private val Pill = RoundedCornerShape(50)
private val PillHeight = 26.dp
private val PillIcon = 13.dp
private val GlyphSide = 14.dp

/** Хаос - свой лиловый оттенок; остальные цвета - палитры темы. */
private val ChaosTint = Color(0xFFC3A0FF)

private val GROUP_TINTS: Map<MarkGroup, Color> = mapOf(
    MarkGroup.LIFE to LossText,
    MarkGroup.ONSLAUGHT to EmberText,
    MarkGroup.SPEED to Vital,
    MarkGroup.GUARD to Brass,
    MarkGroup.ELEMENTS to Rune,
    MarkGroup.CHAOS to ChaosTint,
    MarkGroup.PRECISION to Boon,
    MarkGroup.SKILLS to Crafted,
    MarkGroup.SHIELD to ShieldCyan,
    MarkGroup.AILMENTS to Acid,
)

private val AILMENT_TINTS: Map<String, Color> = mapOf("POISON" to Acid, "IGNITE" to EmberText, "FREEZE" to Rune, "SHOCK" to Boon, "BLEED" to LossText)
