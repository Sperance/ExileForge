package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.display.icons.IconSet
import com.sperance.exileforge.core.display.svg.parseSvgSprite
import com.sperance.exileforge.core.model.icons.IconBindingTables
import com.sperance.exileforge.core.model.icons.IconDescriptor
import com.sperance.exileforge.core.model.icons.IconManifest
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** The pictures come from the server: this renders its sprite and keeps the emblem as the fallback. */
class ServerIconTest {
    @get:Rule val compose = createComposeRule()

    private fun symbol(id: String, tint: String, deep: String, body: String) =
        """<symbol id="icon-$id" viewBox="0 0 64 64"><title>$id</title><defs>""" +
        """<linearGradient id="edge-$id" gradientUnits="userSpaceOnUse" x1="32" y1="6" x2="32" y2="58">""" +
        """<stop offset="0" stop-color="$tint"/><stop offset="1" stop-color="$deep"/></linearGradient>""" +
        """<radialGradient id="core-$id" gradientUnits="userSpaceOnUse" cx="32" cy="28" r="28">""" +
        """<stop offset="0" stop-color="$tint" stop-opacity="0.55"/><stop offset="1" stop-color="$deep" stop-opacity="0.12"/></radialGradient>""" +
        """<linearGradient id="plate-$id" gradientUnits="userSpaceOnUse" x1="10" y1="6" x2="48" y2="58">""" +
        """<stop offset="0" stop-color="#171d25"/><stop offset="1" stop-color="#080b10"/></linearGradient></defs>""" +
        """<path d="M6 13 L13 6 H51 L58 13 V51 L51 58 H13 L6 51 Z" fill="url(#plate-$id)" stroke="$deep" stroke-width="1.6"/>""" +
        """<circle cx="13" cy="13" r="1.3" fill="$tint" fill-opacity="0.45"/>""" +
        """<g fill="none" stroke="url(#edge-$id)" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">$body</g></symbol>"""

    private val sprite = """<svg xmlns="http://www.w3.org/2000/svg" width="0" height="0" style="display:none">""" +
        symbol("jewellery-ring", "#f5da8c", "#a8791f", """<path d="M32 26 a 10 10 0 1 1 0.1 0 Z" fill="url(#core-jewellery-ring)"/><path d="M24 20 L32 10 L40 20 Z" fill="url(#core-jewellery-ring)"/>""") +
        symbol("armour-boots", "#c2ccd8", "#56606e", """<path d="M26 14 L40 14 L38 40 L52 48 L52 56 L20 56 L20 44 Z" fill="url(#core-armour-boots)"/><path d="M26 24 L38 24"/>""") +
        symbol("ui-unknown", "#eef2f7", "#7d8896", """<path d="M32 44 L32 40"/><path d="M26 26 a 6 6 0 1 1 6 8"/>""") + "</svg>"

    private val icons = IconSet(
        IconManifest(set = "forge-vector", version = "test", icons = listOf("jewellery-ring", "armour-boots", "ui-unknown").map { IconDescriptor(it) }),
        IconBindingTables(slots = mapOf("RING" to "jewellery-ring", "BOOTS" to "armour-boots")),
        parseSvgSprite(sprite))

    private fun item(name: String, slot: String, icon: String?) = buildJsonObject {
        put("name", name); put("slot", slot); put("rarity", "RARE"); put("itemLevel", 84)
        if(icon != null) put("icon", icon)
    }

    @Test fun serverIconsReplaceBundledEmblems() {
        compose.setContent { ForgeTheme { CompositionLocalProvider(LocalForgeIcons provides icons) {
            Column(Modifier.fillMaxSize().background(Ink).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("EXILE FORGE", color = Gold, style = MaterialTheme.typography.labelLarge)
                Text("Иконки сервера", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ForgeIcon("jewellery-ring", Modifier.size(64.dp), description = "Кольцо")
                    ForgeIcon("armour-boots", Modifier.size(64.dp), framed = false, description = "Сапоги")
                    ForgeIcon("no-such-icon", Modifier.size(64.dp), description = "Запасная")
                }
                // The icon the server sent, and a document without one resolved through the tables.
                ItemCard(item("Печать изгнанника", "RING", "jewellery-ring"), actionLabel = "Свойства")
                ItemCard(item("Поступь пепла", "BOOTS", null), actionLabel = "Свойства")
            }
        } } }
        compose.onNodeWithContentDescription("Кольцо").assertIsDisplayed()
        compose.onNodeWithContentDescription("Сапоги").assertIsDisplayed()
        compose.onNodeWithContentDescription("Запасная").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Иконка").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Иконка: RING").assertCountEquals(0)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(dir, "icons.jpg").outputStream().use { Bitmap.createScaledBitmap(bitmap, 540, bitmap.height * 540 / bitmap.width, true).compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }

    @Test fun bundledEmblemsRemainWithoutTheSet() {
        compose.setContent { ForgeTheme { ItemCard(item("Поступь пепла", "BOOTS", "armour-boots"), actionLabel = "Свойства") } }
        compose.onNodeWithContentDescription("Иконка: BOOTS").assertIsDisplayed()
    }
}
