package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.hero.CharacterSheet
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.modifier.Modifier as RolledModifier
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.AccountState
import com.sperance.exileforge.presentation.state.PlayState
import com.sperance.exileforge.presentation.state.WorldState
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.screens.hero.HeroHeader
import com.sperance.exileforge.ui.screens.hero.HeroSummary
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import java.io.File
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HeroPanelTest {
    @get:Rule val compose = createComposeRule()

    /**
     * The server's dictionary, as the app always has it by the time a screen is drawn.
     *
     * Since 0.14.0 no document carries text: a template, a node and a class all store a code, and
     * every name on these screens is looked up here. A panel drawn without it would print codes.
     */
    @Before fun dictionary() { serverLocale = LocaleBundle.parse("ru", "sha", """{
        "equipment.HERO_RING.name": "Кольцо героя",
        "equipment.IRON_HELMET.name": "Железный шлем",
        "equipment.MY_HELMET.name": "Мой шлем",
        "item.CHAOS_ORB.name": "Chaos Orb",
        "item.CHAOS_ORB.description": "Перекатывает аффиксы редкого предмета",
        "class.MARAUDER.name": "Мародёр",
        "skilltree.STR_START.name": "Мародёр",
        "skilltree.STR_LIFE_1.name": "Крепость",
        "skilltree.STR_LIFE_1.description": "Больше здоровья"}""")
        }

    @After fun forget() { serverLocale = LocaleBundle() }

    /**
     * The header, the character section and the equipment ledger, as the Hero tab draws them.
     *
     * A worn place hands back the instance, so the item's sheet can open; an empty one hands back
     * only its place, so the stash can be narrowed to what fits it.
     */
    @Test fun equippedSlotShowsItsTemplateAndEmitsTheInstanceId() {
        val instance = EquipmentInstance("ring-instance", "hero", "ring-base",
            listOf(RolledModifier("life-modifier", listOf(42.0), "tier-1", 1)), equippedSlot = "RING")
        val base = buildJsonObject { put("_id", "ring-base"); put("code", "HERO_RING"); put("slot", "RING"); put("rarity", "RARE") }
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник", version = 3, level = 10),
            listOf(instance), CharacterSheet("hero", 10, mapOf("STOCK_HEALTH" to 88.0, "STOCK_ARMOR" to 40.0), listOf("ring-instance")))
        var picked: Pair<String, String?>? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val state = ForgeState(busy = false, account = AccountState(signedIn = true, profile = com.sperance.exileforge.core.model.command.UserProfile("owner")), play = PlayState(hero = hero, characterOwner = "owner"), world = WorldState(inventoryBases = mapOf("ring-base" to base)))
            HeroHeader(state, onTree = {}) {}
            HeroSummary(state)
            EquipmentLedger(state) { place, instance -> picked = place.code to instance }
        } } }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "hero.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }
}
