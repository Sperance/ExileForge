package com.sperance.exileforge.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemCardTest {
    @get:Rule val compose = createComposeRule()

    /** A template carries only a code since 0.14.0; the card's title is the dictionary's. */
    @Before fun dictionary() {
        serverLocale = LocaleBundle.parse("ru", "sha", """{"equipment.EF_TEST_LEGACY.name": "Наследие изгнанника"}""")
    }
    @After fun forget() { serverLocale = LocaleBundle() }

    @Test fun cardDisplaysNameAndOpensItem() {
        var opened = false
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) { opened = true } } }
        compose.onNodeWithText("Наследие изгнанника").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
}
