package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.*

/*
 * Три кнопки игры в стиле «Мягкий» (3.88.3): таблетки без свечения - главную выделяет только заливка.
 * Два размера сами по месту: кнопка во всю отведённую ширину (fillMaxWidth, weight в ряду диалога) - 44 dp,
 * кнопка в строке и плотных местах - 36 dp. Экраны зовут эти, а не Material'овские.
 */

private val ButtonShape = RoundedCornerShape(50)

/** The padding a compact button carries unless its call site asks for its own. */
val CompactPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
private val MinHeight = 36.dp
private val WideHeight = 44.dp

/** Высота по месту (3.88.3): ширину кнопке задал родитель - она широкая, 44 dp; иначе компактная, 36 dp. */
private fun Modifier.forgeHeight(): Modifier = layout { measurable, constraints ->
    val wanted = (if (constraints.hasFixedWidth) WideHeight else MinHeight).roundToPx()
    val minHeight = wanted.coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = minHeight))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private fun label(content: @Composable RowScope.() -> Unit): @Composable RowScope.() -> Unit = {
    ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
}

/** Путь вперёд: зелёная заливка. */
@Composable fun ForgeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = CompactPadding,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) = Button(
    onClick,
    modifier.forgeHeight(),
    enabled,
    ButtonShape,
    colors ?: ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink, disabledContainerColor = Panel, disabledContentColor = Muted),
    ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    contentPadding = contentPadding,
    content = label(content),
)

/** Второй выбор: слова на полупрозрачном зелёном, без рамки. */
@Composable fun ForgeOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = CompactPadding,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) = OutlinedButton(
    onClick,
    modifier.forgeHeight(),
    enabled,
    ButtonShape,
    colors ?: ButtonDefaults.outlinedButtonColors(
        containerColor = Gold.copy(alpha = .12f),
        contentColor = Gold,
        disabledContainerColor = Panel,
        disabledContentColor = Muted,
    ),
    border = null,
    contentPadding = contentPadding,
    content = label(content),
)

/** Words that act: no frame at all. */
@Composable fun ForgeTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = CompactPadding,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) = TextButton(
    onClick,
    modifier.forgeHeight(),
    enabled,
    ButtonShape,
    colors ?: ButtonDefaults.textButtonColors(contentColor = Parchment, disabledContentColor = Muted),
    contentPadding = contentPadding,
    content = label(content),
)
