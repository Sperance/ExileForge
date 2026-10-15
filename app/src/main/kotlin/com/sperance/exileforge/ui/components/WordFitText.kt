package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Подпись в узком месте, что не рвёт слов (0.14.1): до [maxLines] строк, кегль от [max] вниз до [min] шагом [step] - первый, при
 * котором текст встаёт в ширину места переносами только между словами; разрядка букв в узком месте снимается. Не встал и на
 * [min] - [min] с многоточием. Строк всегда не меньше [minLines]: соседние плитки ряда держат одну высоту.
 */
@Composable fun WordFitText(
    text: String,
    color: Color,
    style: TextStyle,
    max: TextUnit,
    min: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    minLines: Int = 1,
    step: TextUnit = FIT_STEP,
) {
    val measurer = rememberTextMeasurer()
    val tight = style.copy(letterSpacing = 0.sp)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val width = constraints.maxWidth
        val size = remember(text, tight, width, max, min, maxLines, step) { fittedSize(measurer, text, tight, width, max, min, maxLines, step) }
        Text(
            text,
            color = color,
            style = tight.copy(fontSize = size, lineHeight = (size.value * LINE_HEIGHT).sp, textAlign = TextAlign.Center),
            maxLines = maxLines,
            minLines = minLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Наибольший кегль из [max]..[min], при котором [text] в ширину [width] встаёт в [maxLines] строк без разрыва слова. */
private fun fittedSize(measurer: TextMeasurer, text: String, style: TextStyle, width: Int, max: TextUnit, min: TextUnit, maxLines: Int, step: TextUnit): TextUnit {
    if (width == Constraints.Infinity || text.isEmpty()) return max
    var size = max.value
    while (size > min.value) {
        if (fits(measurer, text, style.copy(fontSize = size.sp, lineHeight = (size * LINE_HEIGHT).sp), width, maxLines)) return size.sp
        size -= step.value
    }
    return min
}

/** Встал ли [text] стилем [style]: без обрезки и каждая строка, кроме последней, кончается на границе слова. */
private fun fits(measurer: TextMeasurer, text: String, style: TextStyle, width: Int, maxLines: Int): Boolean {
    val layout = measurer.measure(text, style, maxLines = maxLines, constraints = Constraints(maxWidth = width))
    if (layout.hasVisualOverflow) return false
    return (0 until layout.lineCount - 1).all { line ->
        val end = layout.getLineEnd(line)
        end >= text.length || text[end - 1].isWhitespace() || text[end - 1] == '-' || text[end].isWhitespace()
    }
}

private val FIT_STEP = 0.5.sp

/** Высота строки к кеглю: две строки имени держатся плотно. */
private const val LINE_HEIGHT = 1.15f
