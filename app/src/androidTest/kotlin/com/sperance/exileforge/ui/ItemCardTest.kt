package com.sperance.exileforge.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import com.sperance.exileforge.core.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class ItemCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun cardDisplaysNameAndOpensItem() {
        var opened = false
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) { opened = true } } }
        compose.onNodeWithText("Наследие изгнанника").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
}
