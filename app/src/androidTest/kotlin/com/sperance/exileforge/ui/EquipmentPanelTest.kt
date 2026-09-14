package com.sperance.exileforge.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
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
        val view = EquipmentView(3, mapOf(EquipmentSlot.RING_LEFT to "ring-instance"), listOf(instance), emptyList(), CalculatedStats(3, mapOf("maximum_life" to 88.0)))
        var removed: EquipmentSlot? = null
        compose.setContent { ForgeTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            CharacterEquipmentPanel(ForgeState(busy = false, signedIn = true, equipmentView = view, inventoryVersion = 3), { removed = it })
        } } }
        compose.onNodeWithText("Кольцо героя").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Снять").performClick()
        compose.runOnIdle { assertEquals(EquipmentSlot.RING_LEFT, removed) }
        compose.onNodeWithText("Надето: 1 · свернуть").performScrollTo().performClick()
        compose.onNodeWithText("Характеристики · показать").performClick()
        compose.onNodeWithText("Здоровье").assertIsDisplayed()
        compose.onNodeWithText("88.00").assertIsDisplayed()
    }
}
