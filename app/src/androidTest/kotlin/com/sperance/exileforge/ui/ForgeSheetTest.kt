package com.sperance.exileforge.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.SheetHostLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The app's own bottom sheet (3.75.5): it opens, never goes above its rest, springs back from a short drag and closes on a long one. */
class ForgeSheetTest {
    @get:Rule val compose = createComposeRule()

    private var dismissed = 0

    private fun open() {
        compose.setContent {
            ForgeTheme {
                SheetHostLayer {
                    var shown by remember { mutableStateOf(true) }
                    // Called from deep inside a list, as the screens do: the sheet is drawn by the layer over everything.
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Text("screen", Modifier.testTag(SCREEN))
                            if (shown) ForgeSheet(onDismissRequest = { dismissed++; shown = false }) {
                                Column(Modifier.fillMaxWidth().height(BODY_DP.dp).testTag(BODY)) { Text("sheet") }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun top() = compose.onNodeWithTag(BODY, useUnmergedTree = true).getBoundsInRoot().top.value

    @Test fun opens() {
        open()
        compose.onNodeWithTag(BODY, useUnmergedTree = true).assertIsDisplayed()
        // At the foot of the screen, below the list it was called from.
        assertTrue(top() > compose.onNodeWithTag(SCREEN, useUnmergedTree = true).getBoundsInRoot().bottom.value)
    }

    @Test fun theScrimClosesIt() {
        open()
        compose.onNodeWithTag(SCREEN, useUnmergedTree = true).performTouchInput { click(Offset(1f, 1f)) }
        compose.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test fun aFlingUpLeavesItAtRest() {
        open()
        val rest = top()
        compose.onNodeWithTag(BODY, useUnmergedTree = true).performTouchInput { swipeUp(durationMillis = 80) }
        compose.waitForIdle()
        assertEquals(rest, top(), 1f)
        assertEquals(0, dismissed)
    }

    @Test fun aShortDragSpringsBack() {
        open()
        val rest = top()
        compose.onNodeWithTag(BODY, useUnmergedTree = true).performTouchInput { swipeDown(startY = centerY, endY = centerY + height * .1f, durationMillis = 1_000) }
        compose.waitForIdle()
        assertEquals(rest, top(), 1f)
        assertEquals(0, dismissed)
    }

    @Test fun aLongDragClosesItOnce() {
        open()
        compose.onNodeWithTag(BODY, useUnmergedTree = true).performTouchInput { swipeDown(startY = top + 1f, endY = bottom + height, durationMillis = 300) }
        compose.waitForIdle()
        assertEquals(1, dismissed)
        assertTrue(compose.onAllNodes(hasTestTag(BODY), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    private companion object {
        const val BODY = "sheet-body"
        const val SCREEN = "screen"
        const val BODY_DP = 300
    }
}
