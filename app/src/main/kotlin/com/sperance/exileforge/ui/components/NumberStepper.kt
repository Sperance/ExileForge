package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.GoldBright

/**
 * Степпер числа «−» N «+» с шагом 1 в границах [range]: на краю кнопка погашена, удержание листает с ускорением. [less] и [more] -
 * имена кнопок для чтения с экрана.
 */
@Composable fun NumberStepper(value: Int, range: IntRange, enabled: Boolean, less: String, more: String, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    StepperFrame(value.toLong(), range.first.toLong()..range.last.toLong(), enabled, less, more, modifier, { onChange(it.toInt()) }) {
        Text(value.toString(), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Поле количества «−» N «+» в границах [range]: тот же степпер, что [NumberStepper], но число в середине можно набрать - для стопок,
 * где шагом по одному не дойти. Набранное вне границ прижимается к ним, пустое поле значения не меняет. [label] - имя поля для
 * чтения с экрана, [less] и [more] - имена кнопок.
 */
@Composable fun AmountStepper(
    value: Long,
    range: LongRange,
    enabled: Boolean,
    label: String,
    less: String,
    more: String,
    modifier: Modifier = Modifier,
    onChange: (Long) -> Unit,
) {
    StepperFrame(value, range, enabled, less, more, modifier, onChange) {
        var text by remember(value) { mutableStateOf(value.toString()) }
        BasicTextField(
            text,
            { typed ->
                val digits = typed.filter(Char::isDigit).take(AMOUNT_DIGITS)
                text = digits
                digits.toLongOrNull()?.let { onChange(it.coerceIn(range)) }
            },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = GoldBright, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(GoldBright),
            modifier = Modifier.widthIn(min = 36.dp).semantics { contentDescription = label },
        )
    }
}

/** Рамка степпера: «−» и «+» с автоповтором вокруг колодца с числом [center]; шаг - [onStep] со значением в границах [range]. */
@Composable private fun StepperFrame(
    value: Long,
    range: LongRange,
    enabled: Boolean,
    less: String,
    more: String,
    modifier: Modifier,
    onStep: (Long) -> Unit,
    center: @Composable () -> Unit,
) {
    val well = RoundedCornerShape(10.dp)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        ForgeIconButton(
            Icons.Outlined.Remove,
            less,
            { onStep((value - 1).coerceIn(range)) },
            kind = ButtonKind.Secondary,
            state = ButtonState.of(enabled && value > range.first),
            repeatOnHold = true,
        )
        Box(
            Modifier.widthIn(min = 52.dp).height(ButtonSize.Wide.height).background(Abyss, well).border(1.dp, Bronze, well).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) { center() }
        ForgeIconButton(
            Icons.Outlined.Add,
            more,
            { onStep((value + 1).coerceIn(range)) },
            kind = ButtonKind.Secondary,
            state = ButtonState.of(enabled && value < range.last),
            repeatOnHold = true,
        )
    }
}

/** Сколько цифр принимает поле [AmountStepper]. */
private const val AMOUNT_DIGITS = 12
