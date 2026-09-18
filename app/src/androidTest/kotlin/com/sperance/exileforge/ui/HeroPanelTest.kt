package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.modifier.Modifier as RolledModifier
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.screens.hero.HeroEquipmentPanel
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HeroPanelTest {
    @get:Rule val compose = createComposeRule()

    /** The equipped slot shows its template's name and hands back the instance id, not the slot. */
    @Test fun equippedSlotShowsItsTemplateAndEmitsTheInstanceId() {
        val instance = EquipmentInstance("ring-instance", "hero", "ring-base",
            listOf(RolledModifier("life-modifier", "tier-1", 1, listOf(42.0))), equippedSlot = "RING")
        val base = buildJsonObject { put("_id", "ring-base"); put("name", "Кольцо героя"); put("slot", "RING"); put("rarity", "RARE") }
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник", version = 3, level = 10),
            listOf(instance), mapOf("STOCK_HEALTH" to 88.0, "STOCK_ARMOR" to 40.0))
        var removed: String? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp)) {
            HeroEquipmentPanel(ForgeState(busy = false, signedIn = true, hero = hero, characterOwner = "owner",
                profile = com.sperance.exileforge.core.model.command.UserProfile("owner"),
                inventoryBases = mapOf("ring-base" to base)), { removed = it })
        } } }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "hero.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        compose.onNodeWithText("Кольцо героя").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Снять").performClick()
        compose.runOnIdle { assertEquals("ring-instance", removed) }
        compose.onNodeWithText("Надето: 1 · свернуть").performScrollTo().performClick()
        compose.onNodeWithText("Характеристики · показать").performClick()
        compose.onNodeWithText("Здоровье").assertIsDisplayed()
        compose.onNodeWithText("88.0").assertIsDisplayed()
    }
}
