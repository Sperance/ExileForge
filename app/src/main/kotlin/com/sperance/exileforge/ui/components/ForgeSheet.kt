package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.sperance.exileforge.ui.theme.Panel

/**
 * Every bottom sheet of the app (3.75.0), one way. A sheet with a half-open stop blinked on a hard drag: with content
 * near or under half the screen the half and the full stops nearly meet, and a fling tossed it between them frame after
 * frame. Here there is no half stop — open or gone — and the dismissal reaches the caller once, however many times the
 * gesture and the scrim report it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ForgeSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = Panel,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    var dismissed by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { if (!dismissed) { dismissed = true; dismiss() } },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = shape,
        containerColor = containerColor,
        dragHandle = dragHandle,
        content = content,
    )
}
