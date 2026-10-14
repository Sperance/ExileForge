package com.sperance.exileforge.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.core.character.MightShift
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.gearVerdict
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Vital
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Вердикт вещи для героя (3.89.0), сложенный вне главного потока и запомненный на вещь и лист героя: строки длинного
 * списка не держат прокрутку. Null - пока считается или когда вещь не надеть. [place] (3.90.3) - место пары, на которое
 * вещь мерится; без него - лучшее.
 */
@Composable fun rememberGearVerdict(game: GameUi, item: ItemInstance, place: Slot? = null): GearVerdict? {
    val verdict by produceState<GearVerdict?>(null, item, game.hero, game.index, place) {
        value = withContext(Dispatchers.Default) { game.gearVerdict(item, place) }
    }
    return verdict
}

private fun Shift.tint(): Color = when (this) {
    Shift.UP -> Vital
    Shift.DOWN -> LifeRed
    Shift.EVEN -> Muted
}

/**
 * Изменение Мощи «+1.2k» / «−340» / «=» (0.14.0, ASANA-N04): Мощь героя с вещью минус нынешняя, коротким числом со знаком;
 * меньше полупроцента - «=». Урона и защиты стрелками больше нет - одна мера, та же, что «было → стало» карточки.
 */
private fun MightShift.figure(): Pair<String, Shift> {
    val shift = Shift.of(change)
    return (if (shift == Shift.EVEN) "=" else signedNumber(after - before) { compactNumber(it) }) to shift
}

/** Итог над «Если надеть» в карточке вещи (0.14.0, ASANA-N04): «⚜ Мощь +1.2k» - у снаряжения и у фляги. */
@Composable fun GearVerdictSummary(verdict: GearVerdict, modifier: Modifier = Modifier) {
    val (figure, shift) = verdict.might?.figure() ?: return
    Text(ui("verdict.might", figure), modifier, color = shift.tint(), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
}

/** Значок строки списка (0.14.0, ASANA-N04): «⚜ +1.2k» - сумка, тайники, склад, добыча; у фляги тоже. */
@Composable fun GearVerdictBadge(verdict: GearVerdict, modifier: Modifier = Modifier) {
    val (figure, shift) = verdict.might?.figure() ?: return
    Text(ui("might.short", figure), modifier, color = shift.tint(), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
}
