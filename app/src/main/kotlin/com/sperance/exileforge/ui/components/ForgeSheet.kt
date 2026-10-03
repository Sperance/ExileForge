package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Panel

/**
 * Every bottom sheet of the app: Material's own `ModalBottomSheet` (back since 3.75.5), held in by three bounds.
 *
 * - **One rest.** No half-open stop (`skipPartiallyExpanded`): with content near half the screen the two stops nearly met
 *   and a fling tossed the sheet between them.
 * - **Never near the top.** The content is at most [MAX_SHARE] of the app's window, read outside the sheet's own window,
 *   so the open sheet never reaches the status bar; and the sheet adds no insets of its own (`contentWindowInsets` none),
 *   so no inset can switch on and off as it moves. A sheet as tall as the screen took the status bar's inset as padding at
 *   the top, grew, slid back, lost it — and rocked between the two without a touch. The gesture bar and the keyboard are
 *   padded here, once, inside the sheet's colour.
 * - **A fling stays in the list.** What a fling in scrolling content leaves over at the list's top is eaten here, not
 *   handed to the sheet: on material3 1.4 that velocity dragged the sheet toward dismissal and back, a jitter. A drag by
 *   the finger still reaches the sheet, so pulling down from the list's top closes it as before.
 *
 * The dismissal reaches the caller once, however many times the gesture, the scrim and «back» report it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = Panel,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    var dismissed by remember { mutableStateOf(false) }
    val window = LocalWindowInfo.current.containerSize.height
    val limit: Dp = with(LocalDensity.current) { if (window > 0) (window * MAX_SHARE).toDp() else Dp.Infinity }
    ModalBottomSheet(
        onDismissRequest = {
            if (!dismissed) {
                dismissed = true
                dismiss()
            }
        },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = shape,
        containerColor = containerColor,
        dragHandle = dragHandle,
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = limit)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .nestedScroll(remember { FlingStaysInList() }),
            content = content,
        )
    }
}

/**
 * Eats what a fling leaves over at the content's edge; a finger's drag passes on to the sheet untouched. A release after
 * the finger moved the sheet passes its velocity on too, so the sheet settles — back open or closed — instead of stopping
 * half-way; a fling that only scrolled the list ends in the list.
 */
private class FlingStaysInList : NestedScrollConnection {
    private var sheetMoved = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = when {
        source == NestedScrollSource.SideEffect -> available

        available.y != 0f -> {
            sheetMoved = true
            Offset.Zero
        }

        else -> Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = if (sheetMoved) {
        sheetMoved = false
        Velocity.Zero
    } else {
        available
    }
}

/** The most of the app window's height a sheet's content may take: well clear of the status bar. */
private const val MAX_SHARE = .85f
