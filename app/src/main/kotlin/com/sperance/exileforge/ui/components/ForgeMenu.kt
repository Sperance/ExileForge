package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Всплывающее меню игры: `DropdownMenu` с аккордом громкости поверх своего окна ([ChordOverWindow]) - меню игры идут только через
 * него, и Громкость↑+↓ открывает отчёт, даже пока меню открыто.
 */
@Composable fun ForgeMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MenuDefaults.containerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(expanded, onDismissRequest, modifier, containerColor = containerColor) {
        ChordOverWindow()
        content()
    }
}
