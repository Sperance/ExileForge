package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.charted
import com.sperance.exileforge.rules.content.ChartLevels
import com.sperance.exileforge.rules.content.ChartStep
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.ui.components.NumberStepper
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/** Ступень цены в листе: сама ступень [step] и [short] - сумке не хватает на цикл даже у нижнего уровня ступени. */
private class StepView(val step: ChartStep, val short: Boolean)

/**
 * Выбор уровня карты в листе работы картографа (правило [ChartLevels]): «Уровень карты» и число с «−»/«+» (удержание листает), ниже ряд
 * ступеней цены «×1 · 1–10», «×2 · 11–20»…: касание ступени ставит её верхний уровень, выбранная - в золотой рамке, ступень, на нижний
 * уровень которой сумке не хватает входов, - цветом потери с пометкой «мало». Уровни - от низа работы [work] до уровня героя.
 */
@Composable
internal fun ChartLevelPicker(game: GameUi, levels: ChartLevels, work: JobView, level: Int, onLevel: (Int) -> Unit) {
    val chart = work.job as? Job.Chart ?: return
    val heroLevel = game.hero?.level ?: 1
    val range = remember(levels, chart, heroLevel) { levels.levels(chart, heroLevel) }
    val steps = remember(levels, work, heroLevel, game.hero?.bag) {
        levels.steps(chart, heroLevel).map { step ->
            val cost = work.charted(levels, step.levels.first)?.cycleCost()
            StepView(step, cost?.let { game.shortfall(it)?.isEmpty == false } == true)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(ui("crafts.map_level"), color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            NumberStepper(level, range, enabled = !game.busy, less = ui("crafts.map_level_down"), more = ui("crafts.map_level_up"), onChange = onLevel)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.forEach { view -> PriceStep(view, selected = level in view.step.levels, enabled = !game.busy, modifier = Modifier.weight(1f)) { onLevel(view.step.levels.last) } }
        }
    }
}

/** Ступень цены: множитель входов и её уровни; [selected] - в ней выбранный уровень. */
@Composable
private fun PriceStep(view: StepView, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val tone = if (view.short) LossText else if (selected) GoldBright else Parchment
    val levels = view.step.levels
    val span = if (levels.first == levels.last) levels.first.toString() else ui("crafts.map_level_span", levels.first, levels.last)
    Column(
        modifier.heightIn(min = 44.dp).clip(shape).background(Abyss, shape).border(if (selected) 2.dp else 1.dp, if (selected) Gold else Bronze, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(ui("crafts.map_level_times", view.step.multiplier), color = tone, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(span, color = if (view.short) LossText else Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
        if (view.short) Text(ui("crafts.map_level_short"), color = LossText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
