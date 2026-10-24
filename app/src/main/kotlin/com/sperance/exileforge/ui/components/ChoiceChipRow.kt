package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Ink

/**
 * Чип ряда [ChoiceChipRow]: значение [value] и подпись [label]; [accent] - цвет места (фракция, редкость), [count] - число в таблетке,
 * [leading] - свой знак слева (герб, замок).
 */
@Immutable
class ChipChoice<T>(
    val value: T,
    val label: String,
    val accent: Color? = null,
    val count: Int? = null,
    val leading: (@Composable () -> Unit)? = null,
)

/**
 * Ряд чипов [choices], что листается вбок: ленивый ряд с подсказкой прокрутки ([ScrollCue]) на дорожке цвета [track], чипы - [ForgeChip]
 * вида [mode] и размера [size]. Отмечены те, что [selected]; первый отмеченный ряд доводит в видимую часть сам. Касание - [onClick] со
 * значением чипа; [state] гасит весь ряд.
 */
@Composable fun <T> ChoiceChipRow(
    choices: List<ChipChoice<T>>,
    selected: (T) -> Boolean,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    mode: ChipMode = ChipMode.Choice,
    size: ChipSize = ChipSize.Regular,
    state: ButtonState = ButtonState.Enabled,
    track: Color = Ink,
) {
    val cue = rememberLazyRowScrollCue(rememberLazyListState(), choices.indexOfFirst { selected(it.value) }.takeIf { it >= 0 })
    LazyRow(modifier.scrollCue(cue, track), state = cue.state, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(choices) { choice ->
            ForgeChip(
                choice.label,
                selected(choice.value),
                { onClick(choice.value) },
                mode = mode,
                size = size,
                state = state,
                accent = choice.accent,
                count = choice.count,
                leading = choice.leading,
            )
        }
    }
}
