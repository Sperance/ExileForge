package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.sperance.exileforge.ui.theme.Panel

/**
 * Every bottom sheet of the app (3.75.0), one way. A sheet with a half-open stop blinked on a hard drag: with content
 * near or under half the screen the half and the full stops nearly meet, and a fling tossed it between them frame after
 * frame. Here there is no half stop — open or gone — and the dismissal reaches the caller once, however many times the
 * gesture and the scrim report it.
 *
 * Since 3.75.2 no sheet is taller than [MAX_SHARE] of the window: one as tall as the screen reached the status bar, took
 * its inset as padding, grew, slid back, lost the padding — and rocked between the two on its own, the scrim gone.
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
    ) {
        val window = LocalWindowInfo.current.containerSize.height
        val limit = with(LocalDensity.current) { (window * MAX_SHARE).toDp() }
        Column(Modifier.fillMaxWidth().heightIn(max = limit), content = content)
    }
}

/** The most of the window's height a sheet may take, its handle included: well clear of the status bar. */
private const val MAX_SHARE = .88f
