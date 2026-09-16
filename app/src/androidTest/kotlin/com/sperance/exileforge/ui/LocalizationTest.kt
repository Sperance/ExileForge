package com.sperance.exileforge.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** The interface ships in two tongues; Russian stays the default for every screen. */
class LocalizationTest {
    @get:Rule val compose = createComposeRule()
    @After fun restoreDefaultLanguage() { uiLanguage = Lang.RU }

    @Test fun englishLabelsReplaceRussianOnes() {
        uiLanguage = Lang.EN
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) } }
        compose.onNodeWithText("Exile's Legacy").assertIsDisplayed()
        compose.onNodeWithText("Item level").assertIsDisplayed()
        compose.onNodeWithText("OPEN").assertIsDisplayed()
    }

    @Test fun russianRemainsTheDefault() {
        uiLanguage = Lang.RU
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) } }
        compose.onNodeWithText("Наследие изгнанника").assertIsDisplayed()
        compose.onNodeWithText("Уровень предмета").assertIsDisplayed()
    }
}
