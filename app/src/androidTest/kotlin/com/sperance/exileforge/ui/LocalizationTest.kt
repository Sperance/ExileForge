package com.sperance.exileforge.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * The two halves of the language, and which owns what.
 *
 * The client's own labels come from `ui` and switch with [uiLanguage]. The name of a thing in the
 * game comes from the server's dictionary, because since 0.14.0 the document carries only a code —
 * so switching the language means loading the other dictionary, not re-reading the same document.
 *
 * Since 2.13.0 the name of an item is English in every dictionary, as in PoE, so a Russian card
 * carries an English name under Russian labels — the split is what makes that possible.
 */
class LocalizationTest {
    @get:Rule val compose = createComposeRule()
    @After fun restore() { uiLanguage = Lang.RU; serverLocale = LocaleBundle() }

    private fun dictionary(language: String, name: String) {
        serverLocale = LocaleBundle.parse(language, "sha", """{"equipment.EF_TEST_LEGACY.name": "$name"}""")
    }

    @Test fun englishLabelsReplaceRussianOnes() {
        uiLanguage = Lang.EN
        dictionary("en", "Exile's Legacy")
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) } }
        compose.onNodeWithText("Exile's Legacy").assertIsDisplayed()
        compose.onNodeWithText("lvl 30").assertIsDisplayed()
        compose.onNodeWithText("OPEN").assertIsDisplayed()
    }

    @Test fun russianRemainsTheDefault() {
        uiLanguage = Lang.RU
        dictionary("ru", "Наследие изгнанника")
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) } }
        compose.onNodeWithText("Наследие изгнанника").assertIsDisplayed()
        compose.onNodeWithText("ур. 30").assertIsDisplayed()
        compose.onNodeWithText("ОТКРЫТЬ").assertIsDisplayed()
    }

    @Test fun anEnglishNameSitsUnderRussianLabels() {
        uiLanguage = Lang.RU
        dictionary("ru", "Exile's Legacy")
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) } }
        compose.onNodeWithText("Exile's Legacy").assertIsDisplayed()
        compose.onNodeWithText("ур. 30").assertIsDisplayed()
        compose.onNodeWithText("ОТКРЫТЬ").assertIsDisplayed()
    }

    /** Without the server's dictionary the code stands in: a hole shows rather than an empty card. */
    @Test fun aNameWithoutADictionaryFallsBackToTheCode() {
        uiLanguage = Lang.RU
        compose.setContent { ForgeTheme { ItemCard(template(Catalog.EQUIPMENT)) } }
        compose.onNodeWithText("EF TEST LEGACY").assertIsDisplayed()
    }
}
