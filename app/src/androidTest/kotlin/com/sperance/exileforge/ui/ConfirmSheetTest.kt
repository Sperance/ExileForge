package com.sperance.exileforge.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sperance.exileforge.ui.components.ConfirmSheet
import com.sperance.exileforge.ui.components.HOLD_TO_CONFIRM_MS
import com.sperance.exileforge.ui.components.LedgerLine
import com.sperance.exileforge.ui.components.Tone
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The confirmation is held, not tapped.
 *
 * A tap is what a thumb does on its way somewhere else; the whole point of the sheet is that such
 * a tap spends nothing. So the tests are about time: a short press leaves everything as it was,
 * a press held to the end confirms without waiting for the finger to lift, and the accessibility
 * click — which a screen reader user cannot turn into a hold — confirms at once.
 */
class ConfirmSheetTest {
    @get:Rule val compose = createComposeRule()

    private var confirmed = 0
    private var dismissed = 0

    private fun show() {
        compose.setContent { ForgeTheme {
            ConfirmSheet(title = "Сбросить дерево?", subtitle = "14 узлов", danger = true,
                ledger = listOf(
                    LedgerLine("Спишется", "−14 × Сфера сожаления", Tone.SPEND),
                    LedgerLine("Вернётся", "+14 узлов", Tone.GAIN)),
                confirm = "Сбросить", onDismiss = { dismissed++ }) { confirmed++ }
        } }
    }

    private val button get() = compose.onNodeWithText("УДЕРЖИВАЙТЕ, ЧТОБЫ СБРОСИТЬ")

    @Test fun theLedgerSaysWhatIsTakenAndWhatComesBack() {
        show()
        compose.onNodeWithText("Сбросить дерево?").assertIsDisplayed()
        compose.onNodeWithText("−14 × Сфера сожаления").assertIsDisplayed()
        compose.onNodeWithText("+14 узлов").assertIsDisplayed()
    }

    @Test fun aShortPressConfirmsNothing() {
        show()
        compose.mainClock.autoAdvance = false
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(HOLD_TO_CONFIRM_MS / 3L)
        button.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(500L)
        compose.runOnIdle { assertEquals(0, confirmed) }
        // Still open: letting go early is changing one's mind, not cancelling.
        button.assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, dismissed) }
    }

    @Test fun holdingToTheEndConfirmsOnce() {
        show()
        compose.mainClock.autoAdvance = false
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(HOLD_TO_CONFIRM_MS + 200L)
        compose.runOnIdle {
            assertEquals(1, confirmed)
            assertTrue("the sheet closes as it confirms", dismissed >= 1)
        }
    }

    @Test fun aScreenReaderConfirmsWithoutHolding() {
        show()
        button.performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals(1, confirmed) }
    }

    @Test fun cancelConfirmsNothing() {
        show()
        compose.onNodeWithText("ОТМЕНА").performClick()
        compose.runOnIdle {
            assertEquals(0, confirmed)
            assertFalse(dismissed == 0)
        }
    }
}
