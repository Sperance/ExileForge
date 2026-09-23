package com.sperance.exileforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.AccountState
import com.sperance.exileforge.presentation.state.WorldState
import com.sperance.exileforge.presentation.state.MAX_CHARACTERS
import com.sperance.exileforge.ui.screens.session.CharacterMenu
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The gate: who is playing is decided here and nowhere else.
 *
 * The list is driven through callbacks rather than a view model, so what is checked is what the
 * screen offers — which character can be played, which slot is free, and that the class a
 * character carries is named from the server's dictionary like everything else.
 */
class CharacterGateTest {
    @get:Rule val compose = createComposeRule()

    @Before fun dictionary() {
        serverLocale = LocaleBundle.parse("ru", "sha", """{"class.MARAUDER.name": "Мародёр", "class.WITCH.name": "Ведьма"}""")
    }
    @After fun forget() { serverLocale = LocaleBundle() }

    private val marauder = CharacterClass("class-1", "MARAUDER", "STR_START")
    private val witch = CharacterClass("class-2", "WITCH", "INT_START")

    private fun state(vararg characters: CharacterSummary) = ForgeState(phase = AppPhase.CHARACTERS, busy = false, account = AccountState(signedIn = true, profile = UserProfile("owner", name = "", login = ""), deviceId = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001", characters = characters.toList(), charactersRead = true), world = WorldState(classes = listOf(marauder, witch)))

    @Test fun theMenuNamesEachCharacterAndPlaysTheOneTapped() {
        val exile = CharacterSummary("hero-1", "owner", "Изгнанник", level = 12, classId = "class-1")
        val novice = CharacterSummary("hero-2", "owner", "Ведунья", level = 1, classId = "class-2")
        var played: String? = null
        compose.setContent { ForgeTheme { Column(Modifier.fillMaxSize().background(Ink)) {
            CharacterMenu(state(exile, novice), onPlay = { played = it }, onDelete = {}) } } }
        // The class comes from the dictionary by its code, as every other name does.
        compose.onNodeWithText("Мародёр").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ведьма").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("12").performScrollTo().assertIsDisplayed()
        // Two characters means two slots used, and the third is still free.
        compose.onNodeWithText("Слотов свободно: 1 из $MAX_CHARACTERS").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Играть").onFirst().performScrollTo().performClick()
        compose.runOnIdle { assertEquals("hero-1", played) }
    }

    @Test fun afullAccountIsNotOfferedAFourthCharacter() {
        val held = (1..MAX_CHARACTERS).map { CharacterSummary("hero-$it", "owner", "Изгнанник $it", level = it, classId = "class-1") }
        compose.setContent { ForgeTheme { Column(Modifier.fillMaxSize().background(Ink)) {
            CharacterMenu(state(*held.toTypedArray()), onPlay = {}, onDelete = {}) } } }
        compose.onNodeWithText("Слотов свободно: 0 из $MAX_CHARACTERS").performScrollTo().assertIsDisplayed()
        // The server refuses a fourth with CH_005; the button does not ask for one.
        compose.onNodeWithText("Создать персонажа").performScrollTo().assertIsNotEnabled()
    }

    @Test fun anAccountWithNoNameIsShownByItsDevice() {
        compose.setContent { ForgeTheme { Column(Modifier.fillMaxSize().background(Ink)) {
            CharacterMenu(state(), onPlay = {}, onDelete = {}) } } }
        // A device registration leaves `name` and `login` empty; the account is still named.
        compose.onNodeWithText("Гость · …ff0001").performScrollTo().assertIsDisplayed()
    }
}
