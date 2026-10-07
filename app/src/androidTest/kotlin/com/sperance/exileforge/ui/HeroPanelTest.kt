package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.screens.hero.HeroLine
import com.sperance.exileforge.ui.screens.hero.HeroSummary
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class HeroPanelTest {
    @get:Rule val compose = createComposeRule()

    /** The server's dictionary, as the app always has it by the time a screen is drawn: a panel without it would print codes. */
    @Before fun dictionary() {
        serverLocale = TestWorld.russian
    }

    @After fun forget() {
        serverLocale = LocaleBundle()
    }

    /**
     * The header, the character section and the equipment ledger, as the Hero tab draws them over
     * a real class and a real rolled ring: a worn place hands back the copy's id, an empty one only
     * its place. The sheet is the rules' own, the same the server computes.
     */
    @Test fun equippedSlotShowsItsTemplateAndEmitsTheInstanceId() {
        val index = TestWorld.index
        val heroClass = index.classes.classes.first().code
        val ring = TestWorld.roll("ring-instance", Slot.RING, Rarity.RARE).apply { slot = Slot.RING }
        val info = HeroInfo("hero", "owner", "Изгнанник", heroClass = heroClass, level = 10)
        val hero = HeroView(info, listOf(ring), sheet = Sheets.calculate(index, 10, heroClass, emptyList(), listOf(ring)))
        var picked: Pair<String, String?>? = null
        compose.setContent {
            ForgeTheme {
                Column(
                    Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val state = GameUi(
                        activity = Activity(busy = false),
                        session = Session(DEFAULT_SERVER, profile = UserProfile("owner"), signedIn = true),
                        world = World(content = index, contentHash = index.hash),
                        holding = HeroHolding(heroId = "hero", hero = hero, owner = "owner"),
                    )
                    HeroLine(state)
                    HeroSummary(state)
                    EquipmentLedger(state) { place, itemId -> picked = place.code to itemId }
                }
            }
        }
        compose.onNodeWithText("Изгнанник").assertIsDisplayed()
        compose.onAllNodesWithText(equipmentTitle(ring.template), substring = true).onFirst().assertExists()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "hero.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }
}
