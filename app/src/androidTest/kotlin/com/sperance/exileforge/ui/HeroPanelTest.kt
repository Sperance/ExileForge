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
import com.sperance.exileforge.core.model.hero.CharacterSheet
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.InactiveEquipment
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.skilltree.CharacterSkillNode
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.modifier.Modifier as RolledModifier
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.ui.screens.hero.HeroEquipmentPanel
import com.sperance.exileforge.ui.screens.hero.OrbPanel
import com.sperance.exileforge.ui.screens.tree.SkillTreePanel
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionLotKind
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.ui.screens.auction.ShowcaseList
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
            listOf(RolledModifier("life-modifier", listOf(42.0), "tier-1", 1)), equippedSlot = "RING")
        val base = buildJsonObject { put("_id", "ring-base"); put("name", "Кольцо героя"); put("slot", "RING"); put("rarity", "RARE") }
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник", version = 3, level = 10),
            listOf(instance), CharacterSheet("hero", 10, mapOf("STOCK_HEALTH" to 88.0, "STOCK_ARMOR" to 40.0), listOf("ring-instance")))
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

    /**
     * An equipped item the server refused keeps its slot and says why.
     *
     * The reasons are the server's own strings; the panel translates the requirement's name and
     * leaves both numbers exactly as they arrived.
     */
    @Test fun anItemWhoseRequirementsAreNotMetKeepsItsSlotAndSaysWhy() {
        val instance = EquipmentInstance("helm-instance", "hero", "helm-base", equippedSlot = "HELMET")
        val base = buildJsonObject { put("_id", "helm-base"); put("name", "Железный шлем"); put("slot", "HELMET"); put("rarity", "COMMON") }
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник", level = 3, classId = "marauder"), listOf(instance),
            CharacterSheet("hero", 3, mapOf("STOCK_HEALTH" to 60.0), emptyList(),
                listOf(InactiveEquipment("helm-instance", "Железный шлем", listOf("strength: need 30, have 14")))))
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp)) {
            HeroEquipmentPanel(ForgeState(busy = false, signedIn = true, hero = hero, characterOwner = "owner",
                profile = com.sperance.exileforge.core.model.command.UserProfile("owner"),
                classes = listOf(CharacterClass("marauder", "MARAUDER", "Marauder", "STR_START")),
                inventoryBases = mapOf("helm-base" to base)), {})
        } } }
        compose.onNodeWithText("Marauder").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Не работает").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Сила: нужно 30, есть 14").performScrollTo().assertIsDisplayed()
    }

    /** The tree draws the server's graph and sends back the node code the player tapped. */
    @Test fun theTreeShowsTheChosenNodeAndItsTwoCommands() {
        val start = SkillTreeNode("start-id", "STR_START", "Мародёр", SkillNodeType.START, cost = 0,
            connections = listOf("STR_LIFE_1"), positionX = -40, positionY = 0)
        val life = SkillTreeNode("life-id", "STR_LIFE_1", "Крепость", SkillNodeType.NOTABLE, cost = 1,
            connections = listOf("STR_START"), positionX = -20, positionY = 10, description = "Больше здоровья")
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник", level = 5), emptyList(),
            tree = SkillTreeState("hero", total = 4, spent = 0, available = 4,
                nodes = listOf(CharacterSkillNode("STR_START", emptyList(), "Мародёр", SkillNodeType.START, 0))))
        var allocated: String? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp)) {
            SkillTreePanel(ForgeState(busy = false, signedIn = true, hero = hero, characterOwner = "owner",
                profile = com.sperance.exileforge.core.model.command.UserProfile("owner"),
                treeNodes = listOf(start, life), selectedNode = "STR_LIFE_1"),
                onSelect = {}, onAllocate = { allocated = it }, onRefund = {}, onReset = {}, onQuery = {})
        } } }
        compose.onNodeWithText("Крепость").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Нотабль").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Доступно").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Взять узел").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("STR_LIFE_1", allocated) }
    }

    /**
     * The showcase prints the lot's own snapshot and refuses only what the seller owns.
     *
     * A seller cannot buy their own lot, and the server says so; the card simply does not offer it.
     */
    @Test fun theShowcaseNamesThePriceInOrbsAndWillNotSellYouYourOwnLot() {
        val chaos = CurrencyItem("chaos-orb", "Chaos Orb", "CHAOS_ORB", "", 300)
        val theirs = AuctionLot(id = "lot-1", sellerId = "rival", sellerName = "Соперник", kind = AuctionLotKind.EQUIPMENT,
            title = "Железный шлем", slot = "HELMET", rarity = "RARE", itemLevel = 30, priceOrbId = "chaos-orb", price = 4)
        val mine = theirs.copy(id = "lot-2", sellerId = "hero", sellerName = "Изгнанник", title = "Мой шлем")
        var bought: String? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink)) {
            ShowcaseTab(ForgeState(busy = false, signedIn = true, characterId = "hero", characterOwner = "owner",
                profile = com.sperance.exileforge.core.model.command.UserProfile("owner"), orbs = listOf(chaos),
                auctionFilter = AuctionFilter(), showOwnLots = true,
                showcase = AuctionPage(listOf(theirs, mine), 0, 20, 2, 1)),
                onBuy = { bought = it }, onPage = {})
        } } }
        compose.onNodeWithText("Железный шлем").performScrollTo().assertIsDisplayed()
        // The price is always counted in orbs, and the orb catalogue gives it a name.
        compose.onNodeWithText("4 × Сфера хаоса").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ваш лот").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Купить").onFirst().performScrollTo().performClick()
        compose.runOnIdle { assertEquals("lot-1", bought) }
    }

    /** The orb panel hands back the pair the server needs and says what the copy currently is. */
    @Test fun applyingAnOrbEmitsTheInstanceAndTheOrbItself() {
        val instance = EquipmentInstance("ring-instance", "hero", "ring-base", rarity = "UNCOMMON")
        val base = buildJsonObject { put("_id", "ring-base"); put("name", "Кольцо героя"); put("slot", "RING"); put("rarity", "COMMON") }
        val chaos = CurrencyItem("chaos-orb", "Chaos Orb", "CHAOS_ORB", "Перекатывает аффиксы редкого предмета", 300)
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник"), listOf(instance),
            bag = listOf(com.sperance.exileforge.core.model.hero.CharacterItem("chaos-orb", 7)))
        var applied: Pair<String, String>? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp)) {
            OrbPanel(ForgeState(busy = false, signedIn = true, hero = hero, characterOwner = "owner",
                profile = com.sperance.exileforge.core.model.command.UserProfile("owner"),
                inventoryBases = mapOf("ring-base" to base), orbs = listOf(chaos), selectedOrb = "chaos-orb"),
                "ring-instance", {}, { item, orb -> applied = item to orb })
        } } }
        // The count the character owns rides along with the orb's own translated name.
        compose.onNodeWithText("Сфера хаоса · 7").performScrollTo().assertIsDisplayed()
        // The copy's own rarity is shown, not the COMMON its template drops as.
        compose.onNodeWithText("Необычный").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Применить сферу").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("ring-instance" to "chaos-orb", applied) }
    }
}
