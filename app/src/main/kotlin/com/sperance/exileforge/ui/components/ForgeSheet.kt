package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.sperance.exileforge.ui.theme.Panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Every bottom sheet of the app, one way. Since 3.75.3 it is the app's own and not Material's `ModalBottomSheet`: that one
 * could be flung above its top and then bounced between its stops, now and then without end. Here the sheet has one rest —
 * open — and a drag only ever takes it down: a short one springs back, a long one or a fast one closes it. Content that
 * scrolls scrolls first; past its top the drag moves the sheet. The scrim, «back» and the drag all close it the same way,
 * and the dismissal reaches the caller once. No sheet is taller than [MAX_SHARE] of the window.
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
    val scope = rememberCoroutineScope()
    val sheet = remember { SheetMotion() }
    val close: () -> Unit = { scope.launch { sheet.close { dismiss() } } }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // The scrim is drawn here, so the window's own dimming goes.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val limit = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * MAX_SHARE).toDp() }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = SCRIM * sheet.shown))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = close))
            Column(modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .offset { IntOffset(0, sheet.offset.value.roundToInt()) }
                .onSizeChanged { sheet.height = it.height }
                .fillMaxWidth()
                .heightIn(max = limit)
                .clip(shape)
                .background(containerColor)
                // Under the gesture bar the sheet's colour, above it the content; a sheet that pads itself pads nothing twice.
                .windowInsetsPadding(WindowInsets.navigationBars)
                // A tap on the sheet itself is not a tap on the scrim.
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .nestedScroll(remember(sheet, scope) { sheet.connection(scope, close) })
                .draggable(rememberDraggableState { sheet.dragBy(it, scope) }, Orientation.Vertical,
                    onDragStopped = { velocity -> sheet.settle(velocity, close) })) {
                dragHandle?.let { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { it() } }
                content()
            }
        }
        LaunchedEffect(sheet.height) { if (sheet.height > 0) sheet.open() }
    }
}

/** Where the sheet stands: [offset] pixels below its rest, 0 open, [height] gone. */
private class SheetMotion {
    val offset = Animatable(Float.MAX_VALUE / 4)
    var height by mutableIntStateOf(0)
    private var opened by mutableStateOf(false)
    private var closing = false

    /** How much of the sheet is out, for the scrim. */
    val shown: Float get() = if (height == 0 || !opened) 0f else (1f - offset.value / height).coerceIn(0f, 1f)

    suspend fun open() {
        if (opened || closing) return
        offset.snapTo(height.toFloat())
        opened = true
        offset.animateTo(0f, tween(OPEN_MS))
    }

    /** Never above the rest: an upward drag stops at it. */
    fun dragBy(delta: Float, scope: CoroutineScope) {
        if (closing || !opened) return
        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, height.toFloat())) }
    }

    suspend fun settle(velocity: Float, close: () -> Unit) {
        if (closing || !opened) return
        if (velocity > FLING_PX_S || offset.value > height * CLOSE_SHARE) close() else offset.animateTo(0f, tween(SETTLE_MS))
    }

    suspend fun close(then: () -> Unit) {
        if (closing) return
        closing = true
        offset.animateTo(height.toFloat(), tween(CLOSE_MS))
        then()
    }

    /** Content scrolls first; what it leaves of a downward drag moves the sheet, and an upward one takes the sheet back first. */
    fun connection(scope: CoroutineScope, close: () -> Unit) = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y >= 0f || offset.value <= 0f) return Offset.Zero
            val taken = maxOf(available.y, -offset.value)
            dragBy(taken, scope)
            return Offset(0f, taken)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            dragBy(available.y, scope)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset.value <= 0f) return Velocity.Zero
            settle(available.y, close)
            return available
        }
    }

    private companion object {
        const val OPEN_MS = 220
        const val SETTLE_MS = 180
        const val CLOSE_MS = 180
        /** A drag past this share of the sheet's height closes it. */
        const val CLOSE_SHARE = .3f
        /** A downward fling faster than this, in pixels a second, closes it however short. */
        const val FLING_PX_S = 1800f
    }
}

/** The most of the window's height a sheet may take, its handle included: well clear of the status bar. */
private const val MAX_SHARE = .88f

/** How dark the scrim is with the sheet fully out. */
private const val SCRIM = .5f
