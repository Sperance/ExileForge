package com.sperance.exileforge.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The app's bottom sheet (Material's, bounded): it opens, a fling up leaves it where it rests, a short drag springs back,
 * a long one closes it once, a fling in a list inside neither moves nor closes it, and a tall one stays clear of the top.
 */
class ForgeSheetTest {
    @get:Rule val compose = createComposeRule()

    private var dismissed = 0

    private fun open(body: @Composable ColumnScope.() -> Unit = { Column(Modifier.fillMaxWidth().height(300.dp).testTag(BODY)) { Text("sheet") } }) {
        compose.setContent {
            ForgeTheme {
                var shown by remember { mutableStateOf(true) }
                if (shown) ForgeSheet(onDismissRequest = { dismissed++; shown = false }, content = body)
            }
        }
        compose.waitForIdle()
    }

    private fun node(tag: String = BODY) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun top(tag: String = BODY) = node(tag).getBoundsInRoot().top.value
    private fun gone() = compose.onAllNodes(hasTestTag(BODY), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()

    @Test fun opens() {
        open()
        node().assertIsDisplayed()
        assertEquals(0, dismissed)
    }

    @Test fun aFlingUpLeavesItAtRest() {
        open()
        val rest = top()
        repeat(3) {
            node().performTouchInput { swipeUp(durationMillis = 60) }
            compose.waitForIdle()
            assertEquals(rest, top(), 1f)
        }
        assertEquals(0, dismissed)
    }

    @Test fun aShortDragSpringsBack() {
        open()
        val rest = top()
        node().performTouchInput { swipeDown(startY = centerY, endY = centerY + height * .1f, durationMillis = 1_000) }
        compose.waitForIdle()
        assertEquals(rest, top(), 1f)
        assertEquals(0, dismissed)
    }

    @Test fun aLongDragClosesItOnce() {
        open()
        node().performTouchInput { swipeDown(startY = top + 1f, endY = bottom + height, durationMillis = 300) }
        compose.waitForIdle()
        assertEquals(1, dismissed)
        assertTrue(gone())
    }

    @Test fun aFlingInTheListNeitherMovesNorClosesIt() {
        open {
            LazyColumn(Modifier.fillMaxWidth().height(400.dp).testTag(LIST)) {
                items(60) { Text("row $it", Modifier.fillMaxWidth().height(40.dp).testTag(if (it == 0) BODY else "row$it")) }
            }
        }
        val rest = top(LIST)
        node(LIST).performScrollToIndex(20)
        compose.waitForIdle()
        // A hard fling back to the top: the list stops there, the sheet does not take what is left of it.
        node(LIST).performTouchInput { swipeDown(startY = top + 10f, endY = bottom - 10f, durationMillis = 50) }
        compose.waitForIdle()
        assertEquals(rest, top(LIST), 1f)
        assertEquals(0, dismissed)
    }

    @Test fun aTallSheetStaysClearOfTheTop() {
        open { Column(Modifier.fillMaxWidth().height(3_000.dp).testTag(BODY)) { Text("tall") } }
        val metrics = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics
        val screen = metrics.heightPixels / metrics.density
        assertTrue("top ${top()} of $screen", top() >= screen * .1f)
        // And it stays there under flings both ways that do not close it.
        val rest = top()
        node().performTouchInput { swipeUp(durationMillis = 60) }
        compose.waitForIdle()
        assertEquals(rest, top(), 1f)
    }

    private companion object {
        const val BODY = "sheet-body"
        const val LIST = "sheet-list"
    }
}
