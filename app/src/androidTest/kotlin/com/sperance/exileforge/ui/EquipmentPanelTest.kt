package com.sperance.exileforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Ink
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.screens.inventory.CharacterEquipmentPanel
import com.sperance.exileforge.ui.theme.ForgeTheme
import kotlinx.serialization.json.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class EquipmentPanelTest {
    @get:Rule val compose = createComposeRule()
    @Test fun equippedSlotsShowSnapshotAndEmitExactSlot() {
        val instance = buildJsonObject { put("uuid", "ring-instance"); put("baseSnapshot", buildJsonObject { put("name", "Кольцо героя"); put("slot", "RING") }) }
        // Equipped items arrive in full even when the paged stash is empty on this page.
        val equipped = listOf(com.sperance.exileforge.core.contract.WireJson.decodeFromJsonElement<com.sperance.exileforge.core.model.hero.EquipmentInstance>(JsonObject(instance + ("equipmentId" to JsonPrimitive("base")))))
        val view = EquipmentView(3, mapOf(EquipmentSlot.RING_LEFT to "ring-instance"), equipped, InventoryPage(size = 50, total = 1), OwnedItemsPage(size = 50), CalculatedStats(3, mapOf("maximum_life" to 88.0)))
        var removed: EquipmentSlot? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp)) {
            CharacterEquipmentPanel(ForgeState(busy = false, signedIn = true, equipmentView = view, inventoryVersion = 3, hero = com.sperance.exileforge.core.model.hero.CharacterSummary("hero", "owner", "Изгнанник", 3, 10)), { removed = it })
        } } }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "hero.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        compose.onNodeWithText("Кольцо героя").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Снять").performClick()
        compose.runOnIdle { assertEquals(EquipmentSlot.RING_LEFT, removed) }
        compose.onNodeWithText("Надето: 1 · свернуть").performScrollTo().performClick()
        compose.onNodeWithText("Характеристики · показать").performClick()
        compose.onNodeWithText("Здоровье").assertIsDisplayed()
        compose.onNodeWithText("88.00").assertIsDisplayed()
    }
}
