package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Место под один из вариантов [variants] высотой самого высокого из них: показан [shown] (сверху места), прочие измерены, но не
 * размещены - не рисуются, касаний и чтения с экрана у них нет. Смена варианта не меняет высоту места, и лист, что встаёт по низу,
 * не двигает верх. [shown] вне [variants] - показан он, место - по нему и вариантам.
 */
@Composable fun <T> TallestOf(variants: List<T>, shown: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val all = if (shown in variants) variants else variants + shown
    val at = all.indexOf(shown)
    Layout(
        content = {
            all.forEachIndexed { i, variant ->
                key(i) { Box(if (i == at) Modifier else Modifier.clearAndSetSemantics {}) { content(variant) } }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val free = constraints.copy(minHeight = 0)
        val placeables = measurables.map { it.measure(free) }
        val width = placeables.maxOf { it.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = placeables.maxOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) { placeables[at].place(0, 0) }
    }
}
