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
import com.sperance.exileforge.core.character.ToolVerdict
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
 * Изменение Мощи «+1.2k» / «−340» (0.14.0, ASANA-N04): Мощь героя с вещью минус нынешняя, коротким числом со знаком. Урона и защиты
 * стрелками больше нет - одна мера, та же, что «было → стало» карточки; сдвиг, что не стоит показа ([MightShift.shown], 0.14.5,
 * #44), не пишется вовсе.
 */
private fun MightShift.figure(): String = signedNumber(after - before) { compactNumber(it) }

/**
 * Значок строки списка (0.14.0, ASANA-N04): «⚜ +1.2k» - сумка, тайники, склад, добыча; у фляги тоже. У инструмента (0.15.0, #34)
 * Мощи нет - «⚒ лучше» / «⚒ хуже» по итогу ремесла против инструмента его профессии, без разницы - ничего.
 */
@Composable fun GearVerdictBadge(verdict: GearVerdict, modifier: Modifier = Modifier) {
    if (verdict is ToolVerdict) {
        verdict.shift.takeIf { it != Shift.EVEN }?.let { shift ->
            Text(ui("verdict.tool.${shift.name}"), modifier, color = shift.tint(), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
        }
        return
    }
    val might = verdict.might?.takeIf { it.shown } ?: return
    Text(ui("might.short", might.figure()), modifier, color = might.shift.tint(), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
}
