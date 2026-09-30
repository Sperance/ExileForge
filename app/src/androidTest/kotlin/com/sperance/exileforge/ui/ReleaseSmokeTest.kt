package com.sperance.exileforge.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.sperance.exileforge.MainActivity
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.ModifierLine
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The app as it ships, run with `-PminifiedTests` against the `minifiedTest` build — the release one, shrunk by R8.
 * It walks what a class R8 took away breaks first: the activity starting, the content parsed through the rules'
 * serializers, a hero and an item through the wire's, and an item's card and a modifier's line drawn.
 */
class ReleaseSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Before fun dictionary() { serverLocale = TestWorld.russian }
    @After fun forget() { serverLocale = LocaleBundle() }

    @Test fun shrunkAppStartsDecodesAndDraws() {
        ActivityScenario.launch(MainActivity::class.java).use { assertEquals(Lifecycle.State.RESUMED, it.state) }
        val index = TestWorld.index
        val rolled = TestWorld.roll("smoke", Slot.RING, Rarity.RARE)
        val item = WireJson.decodeFromString(ItemInstance.serializer(), WireJson.encodeToString(ItemInstance.serializer(), rolled))
        assertEquals(rolled, item)
        val hero = WireJson.decodeFromString(HeroInfo.serializer(), """{"id":"hero","name":"Изгнанник","level":7,"money":120}""")
        assertEquals(7, hero.level)
        val view = checkNotNull(ItemView.of(item, index))
        // A rare copy always carries a modifier: its line is the one drawn on its own under the card
        val line = view.lines.first().let { Line(it.code, it.values) }
        compose.setContent { ForgeTheme { Column {
            ItemCard(view, detailed = true)
            ModifierLine(index, line)
        } } }
        compose.onNodeWithText(view.title).assertIsDisplayed()
        compose.onAllNodesWithText(lineText(index, line)).onFirst().assertExists()
    }
}
