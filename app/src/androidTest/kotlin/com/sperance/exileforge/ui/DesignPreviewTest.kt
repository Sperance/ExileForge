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
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DesignPreviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun cardsDisplayIconsPropertiesAndActions() {
        fun item(name: String, slot: String, stat: String, value: Int) = buildJsonObject {
            put("name", name); put("slot", slot); put("rarity", "RARE"); put("itemLevel", 85); put("quality", 20)
            put("modifiers", buildJsonArray { add(buildJsonObject { put("definitionId", stat); put("values", buildJsonArray { add(buildJsonObject { put("value", value) }) }) })
        }
        compose.setContent { ForgeTheme {
            Column(Modifier.fillMaxSize().background(Ink).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("EXILE FORGE", color = Gold, style = MaterialTheme.typography.labelLarge)
                Text("Арсенал героя", style = MaterialTheme.typography.headlineLarge)
                Text("Снаряжение, которое меняет игру", color = Muted)
                ItemCard(item("Печать изгнанника", "RING", "MaximumLife", 72), selected = true, actionLabel = "Свойства и крафт")
                ItemCard(item("Поступь пепла", "BOOTS", "FireResistance", 38), actionLabel = "Свойства и крафт")
            }
        } }
        compose.onNodeWithText("Печать изгнанника").assertIsDisplayed()
        compose.onNodeWithContentDescription("Иконка: RING").assertIsDisplayed()
        compose.onNodeWithText("72").assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(dir, "arsenal.jpg").outputStream().use { Bitmap.createScaledBitmap(bitmap, 540, bitmap.height * 540 / bitmap.width, true).compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }
}
