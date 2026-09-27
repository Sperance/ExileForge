package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
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

    @Before fun dictionary() { serverLocale = TestWorld.russian }
    @After fun forget() { serverLocale = LocaleBundle() }

    @Test fun cardsDisplayIconsPropertiesAndActions() {
        val index = TestWorld.index
        val ring = checkNotNull(ItemView.of(TestWorld.roll("ring", Slot.RING, Rarity.RARE), index))
        val boots = checkNotNull(ItemView.of(TestWorld.roll("boots", Slot.BOOTS, Rarity.RARE, seed = 2L), index))
        compose.setContent { ForgeTheme {
            Column(Modifier.fillMaxSize().background(Ink).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("EXILE FORGE", color = Gold, style = MaterialTheme.typography.labelLarge)
                Text("Арсенал героя", style = MaterialTheme.typography.headlineLarge)
                Text("Снаряжение, которое меняет игру", color = Muted)
                ItemCard(ring, selected = true, detailed = true, actionLabel = "Свойства")
                ItemCard(boots, actionLabel = "Свойства")
            }
        } }
        compose.onNodeWithText(ring.title).assertIsDisplayed()
        compose.onNodeWithText(boots.title).assertIsDisplayed()
        compose.onAllNodesWithText("Свойства").assertCountEquals(2)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(dir, "arsenal.jpg").outputStream().use { Bitmap.createScaledBitmap(bitmap, 540, bitmap.height * 540 / bitmap.width, true).compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }
}
