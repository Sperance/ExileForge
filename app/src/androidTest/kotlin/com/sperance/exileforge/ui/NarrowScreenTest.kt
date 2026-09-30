package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.features.WarmStep
import com.sperance.exileforge.presentation.features.Warmup
import com.sperance.exileforge.presentation.state.AccountState
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.PlayState
import com.sperance.exileforge.presentation.state.WorldState
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.WarmupScreen
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.screens.hero.HeroHeader
import com.sperance.exileforge.ui.screens.hero.HeroSummary
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The narrowest phone the game is laid out for (3.54.0): 320 dp. The hero's panel, an item card and the loading screen are drawn
 * at that width and kept as `design/narrow.jpg`, printed into the verification log — what is cut or runs over is seen there.
 */
class NarrowScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun dictionary() { serverLocale = TestWorld.russian }
    @After fun forget() { serverLocale = LocaleBundle() }

    @Test fun theHeroPanelACardAndTheLoadingScreenFitThreeHundredTwentyDp() {
        val index = TestWorld.index
        val heroClass = index.classes.classes.first().code
        val ring = TestWorld.roll("ring-narrow", Slot.RING, Rarity.RARE).apply { slot = Slot.RING }
        val boots = checkNotNull(ItemView.of(TestWorld.roll("boots-narrow", Slot.BOOTS, Rarity.RARE, seed = 3L), index))
        val info = HeroInfo("hero", "owner", "Изгнанник с очень длинным именем", heroClass = heroClass, level = 42)
        val hero = HeroView(info, listOf(ring), sheet = Sheets.calculate(index, 42, heroClass, emptyList(), listOf(ring)))
        val state = ForgeState(busy = false, account = AccountState(profile = UserProfile("owner"), signedIn = true),
            world = WorldState(content = index, contentHash = index.hash), play = PlayState(heroId = "hero", heroOwner = "owner", hero = hero))
        compose.setContent { ForgeTheme {
            Column(Modifier.width(320.dp).background(Ink).verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroHeader(state)
                HeroSummary(state)
                EquipmentLedger(state) { _, _ -> }
                ItemCard(boots, detailed = true, actionLabel = "Свойства")
                Box(Modifier.height(360.dp)) { WarmupScreen(Warmup("hero", setOf(WarmStep.CONTENT, WarmStep.LOCALE))) }
            }
        } }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "narrow.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }
    }
}
