package com.sperance.exileforge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.*

/*
 * The app's three buttons in «Эфир» (2.80.0): compact — a line of text, not a slab — and the one
 * primary action glowing faintly, so a screen shows at a glance which button is the way forward.
 * Every screen calls these rather than Material's own, which stay at 40 dp and 24 dp of padding.
 */

private val ButtonShape = RoundedCornerShape(8.dp)
/** The padding a compact button carries unless its call site asks for its own. */
val CompactPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
private val MinHeight = 32.dp

private fun label(content: @Composable RowScope.() -> Unit): @Composable RowScope.() -> Unit = {
    ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
}

/** The way forward: ether fill, a soft halo while it can be pressed. */
@Composable fun ForgeButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = CompactPadding, colors: ButtonColors? = null, content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier.defaultMinSize(minHeight = MinHeight).glow(colors?.containerColor ?: Gold, on = enabled, radius = 8.dp, shape = ButtonShape), enabled, ButtonShape,
        colors ?: ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink, disabledContainerColor = PanelRaised, disabledContentColor = Muted),
        ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp), contentPadding = contentPadding, content = label(content))

/** A second choice: a hairline of ether around the words. */
@Composable fun ForgeOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = CompactPadding, colors: ButtonColors? = null, content: @Composable RowScope.() -> Unit) =
    OutlinedButton(onClick, modifier.defaultMinSize(minHeight = MinHeight), enabled, ButtonShape,
        colors ?: ButtonDefaults.outlinedButtonColors(contentColor = Gold, disabledContentColor = Muted),
        border = BorderStroke(1.dp, if (enabled) Gold.copy(alpha = .45f) else Bronze), contentPadding = contentPadding, content = label(content))

/** Words that act: no frame at all. */
@Composable fun ForgeTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = CompactPadding, colors: ButtonColors? = null, content: @Composable RowScope.() -> Unit) =
    TextButton(onClick, modifier.defaultMinSize(minHeight = MinHeight), enabled, ButtonShape,
        colors ?: ButtonDefaults.textButtonColors(contentColor = Gold, disabledContentColor = Muted), contentPadding = contentPadding, content = label(content))
