package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.theme.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class DesignPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Before fun dictionary() {
        serverLocale = TestWorld.russian
    }

    @After fun forget() {
        serverLocale = LocaleBundle()
    }

    @Test fun cardsDisplayIconsPropertiesAndActions() {
        val index = TestWorld.index
        val ring = checkNotNull(ItemView.of(TestWorld.roll("ring", Slot.RING, Rarity.RARE), index))
        val boots = checkNotNull(ItemView.of(TestWorld.roll("boots", Slot.BOOTS, Rarity.RARE, seed = 2L), index))
        // «Астролябия» (3.89.0): уникальная и мифическая карточки - первыми, на снимке арсенала.
        val unique = checkNotNull(ItemView.of(TestWorld.legend("unique", Rarity.UNIQUE, 41), index))
        val mythic = checkNotNull(ItemView.of(TestWorld.legend("mythic", Rarity.MYTHICAL, 3), index))
        compose.setContent {
            ForgeTheme {
                Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ItemCard(unique, detailed = true)
                    ItemCard(mythic, detailed = true)
                    ItemCard(ring, selected = true, detailed = true, actionLabel = "Свойства")
                    ItemCard(boots, actionLabel = "Свойства")
                }
            }
        }
        compose.onNodeWithText(unique.title).assertIsDisplayed()
        compose.onNodeWithText(mythic.title).assertExists()
        compose.onNodeWithText(ring.title).assertExists()
        compose.onNodeWithText(boots.title).assertExists()
        // The label is drawn in capitals, and only on the short card: a full one is a page, not a way in
        compose.onAllNodesWithText("СВОЙСТВА").assertCountEquals(1)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(dir, "arsenal.jpg").outputStream().use { Bitmap.createScaledBitmap(bitmap, 540, bitmap.height * 540 / bitmap.width, true).compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }
}
