package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.hero.CharacterSheet
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.InactiveEquipment
import com.sperance.exileforge.core.model.hero.UnwearableEquipment
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.skilltree.CharacterSkillNode
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import com.sperance.exileforge.core.model.skilltree.StatContribution
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
import org.junit.After
import org.junit.Assert.assertEquals
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
        "item.CHAOS_ORB.name": "Сфера хаоса",
        "item.CHAOS_ORB.description": "Перекатывает аффиксы редкого предмета",
        "class.MARAUDER.name": "Мародёр",
        "skilltree.STR_START.name": "Мародёр",
        "skilltree.STR_LIFE_1.name": "Крепость",
        "skilltree.STR_LIFE_1.description": "Больше здоровья"}""")
        }

    @After fun forget() { serverLocale = LocaleBundle() }

    /** The equipped slot shows its template's name and hands back the instance id, not the slot. */
    @Test fun equippedSlotShowsItsTemplateAndEmitsTheInstanceId() {
        val instance = EquipmentInstance("ring-instance", "hero", "ring-base",
            listOf(RolledModifier("life-modifier", listOf(42.0), "tier-1", 1)), equippedSlot = "RING")
        val base = buildJsonObject { put("_id", "ring-base"); put("code", "HERO_RING"); put("slot", "RING"); put("rarity", "RARE") }
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
        // Life is a bar with the number on it: the sheet carries a maximum and no current value,
        // so the bar is always full and the figure is the whole of what it says.
        compose.onNodeWithText("88").performScrollTo().assertIsDisplayed()
        // Every stat the server sent is in the sheet the vitals open, with nothing folded inside it.
        compose.onNodeWithText("Все характеристики: 2 · нажмите").performScrollTo().performClick()
        compose.onNodeWithText("Здоровье").assertIsDisplayed()
        // Whole numbers, no dot: the 88.0 the server sent reads "88". It matches twice, because the
        // bar behind the sheet carries the same figure — the sheet did not round it differently.
        compose.onAllNodesWithText("88").assertCountEquals(2)
        // The second stat is checked by its value: "Броня" is also the name of the BODY slot, and
        // the empty slot is drawn right behind the sheet, so the label matches two nodes.
        compose.onNodeWithText("40").assertIsDisplayed()
    }

    /**
     * An equipped item the server refused keeps its slot and says why.
     *
     * The reasons are the server's own strings; the panel translates the requirement's name and
     * leaves both numbers exactly as they arrived.
     */
    @Test fun anItemWhoseRequirementsAreNotMetKeepsItsSlotAndSaysWhy() {
        val instance = EquipmentInstance("helm-instance", "hero", "helm-base", equippedSlot = "HELMET")
        val base = buildJsonObject { put("_id", "helm-base"); put("code", "IRON_HELMET"); put("slot", "HELMET"); put("rarity", "COMMON") }
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник", level = 3, classId = "marauder"), listOf(instance),
            CharacterSheet("hero", 3, mapOf("STOCK_HEALTH" to 60.0), emptyList(),
                listOf(InactiveEquipment("helm-instance", "IRON_HELMET", listOf("strength: need 30, have 14")))))
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).verticalScroll(rememberScrollState()).padding(12.dp)) {
            HeroEquipmentPanel(ForgeState(busy = false, signedIn = true, hero = hero, characterOwner = "owner",
                profile = com.sperance.exileforge.core.model.command.UserProfile("owner"),
                classes = listOf(CharacterClass("marauder", "MARAUDER", "STR_START")),
                inventoryBases = mapOf("helm-base" to base)), {})
        } } }
        compose.onNodeWithText("Мародёр").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Не работает").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Сила: нужно 30, есть 14").performScrollTo().assertIsDisplayed()
    }

    /** The tree draws the server's graph and sends back the node code the player tapped. */
    @Test fun theTreeShowsTheChosenNodeAndItsTwoCommands() {
        val start = SkillTreeNode("start-id", "STR_START", SkillNodeType.START, cost = 0,
            connections = listOf("STR_LIFE_1"), positionX = -40, positionY = 0)
        val life = SkillTreeNode("life-id", "STR_LIFE_1", SkillNodeType.NOTABLE, cost = 1,
            connections = listOf("STR_START"), positionX = -20, positionY = 10)
        val hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник", level = 5), emptyList(),
            tree = SkillTreeState("hero", total = 4, spent = 0, available = 4,
                nodes = listOf(CharacterSkillNode("STR_START", emptyList(), SkillNodeType.START, 0)),
                // The totals are the server's arithmetic, already done: the client only prints
                // them, and an INCREASED one stays a percentage because there is no base under it.
                totals = listOf(StatContribution("STOCK_HEALTH", "ADD", 40.0),
                    StatContribution("STOCK_ATTACK_PHYSICAL", "INCREASED", 25.0))))
        var allocated: String? = null
        compose.setContent { ForgeTheme {
            // The search reads the query off the state, as it does in the app, so the test has to
            // hold one: typing into a screen whose state never moves proves nothing.
            var state by remember { mutableStateOf(ForgeState(busy = false, signedIn = true, hero = hero,
                characterOwner = "owner", profile = com.sperance.exileforge.core.model.command.UserProfile("owner"),
                treeNodes = listOf(start, life), selectedNode = "STR_LIFE_1")) }
            Column(Modifier.fillMaxSize().background(Ink).padding(12.dp)) {
                SkillTreePanel(state, onSelect = { state = state.copy(selectedNode = it) },
                    onAllocate = { allocated = it }, onRefund = {}, onReset = {},
                    onQuery = { state = state.copy(nodeQuery = it) }, modifier = Modifier.weight(1f))
            }
        } }
        // The map takes the whole panel; the balance is on it and everything else opens over it.
        compose.onNodeWithText("Очки: 4 из 4").assertIsDisplayed()
        compose.onNodeWithText("Подробно").performClick()
        compose.onNodeWithText("Доступно").performScrollTo().assertIsDisplayed()
        // What the whole tree gives is counted by the server and printed whole, no dot — and a
        // percentage survives, which it did not when the total was taken over an empty base.
        compose.onNodeWithText("Здоровье").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("+40").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("+25%").performScrollTo().assertIsDisplayed()
        // Finding a node opens the node itself: searching and then not being shown it
        // would be a strange place to stop.
        compose.onNodeWithText("Название узла").performScrollTo().performTextInput("Креп")
        compose.onNodeWithText("Крепость · Нотабль").performScrollTo().performClick()
        compose.onNodeWithText("Нотабль").assertIsDisplayed()
        compose.onNodeWithText("Взять узел").performScrollTo().performClick()
        // Spending a point is asked about before it is spent, and the question names the price.
        compose.onNodeWithText("Взять узел?").assertIsDisplayed()
        // The button is held, not tapped; the gesture itself is ConfirmSheetTest's. This path is the
        // one a screen reader takes, which confirms at once.
        compose.onNodeWithText("УДЕРЖИВАЙТЕ, ЧТОБЫ ВЗЯТЬ").performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals("STR_LIFE_1", allocated) }
    }

    /**
     * A showcase line carries what a trader decides on: both names, what it is, what it rolled, the price.
     *
     * A stash line can be terse because its owner knows what they own; a showcase line cannot, so
     * the properties are on it and only the rest waits behind the tap. A seller cannot buy their
     * own lot, and the server says so — the card simply does not offer it.
     */
    @Test fun theShowcaseNamesThePriceInOrbsAndWillNotSellYouYourOwnLot() {
        val chaos = CurrencyItem("chaos-orb", "CHAOS_ORB", "CHAOS_ORB", 300)
        // Six properties on one item: the base is two and the rolls are four, so one is over the cap.
        val base = buildJsonObject {
            put("_id", "helmet-base"); put("code", "IRON_HELMET"); put("slot", "HELMET")
            put("requiredLevel", 25); put("requiredStrength", 40)
            putJsonArray("baseParams") {
                add(buildJsonObject { put("modifierId", "armour"); putJsonArray("values") { add(12.0) } })
                add(buildJsonObject { put("modifierId", "evasion"); putJsonArray("values") { add(8.0) } })
            }
        }
        val rolls = listOf("life", "mana", "fire", "cold").map { RolledModifier(it, listOf(7.0), "tier-1", 1) }
        val instance = EquipmentInstance("helmet-instance", "rival", "helmet-base", rolls, rarity = "RARE")
        val theirs = AuctionLot(id = "lot-1", sellerId = "rival", sellerName = "Соперник", kind = AuctionLotKind.EQUIPMENT,
            equipment = instance, itemCode = "IRON_HELMET", slot = "HELMET", rarity = "RARE", itemLevel = 30,
            priceOrbId = "chaos-orb", price = 4)
        // Different prices, so each line is its own: the price is per lot, not per showcase.
        val mine = theirs.copy(id = "lot-2", sellerId = "hero", sellerName = "Изгнанник", itemCode = "MY_HELMET",
            equipment = null, price = 7)
        var bought: String? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink)) {
            ShowcaseList(ForgeState(busy = false, signedIn = true, characterId = "hero", characterOwner = "owner",
                profile = com.sperance.exileforge.core.model.command.UserProfile("owner"), orbs = listOf(chaos),
                hero = HeroView(CharacterSummary("hero", "owner", "Изгнанник"),
                    sheet = CharacterSheet(unwearable = listOf(
                        UnwearableEquipment("helmet-base", reasons = listOf("strength: need 30, have 14"))))),
                auctionFilter = AuctionFilter(), showOwnLots = true,
                inventoryBases = mapOf("helmet-base" to base),
                showcase = AuctionPage(listOf(theirs, mine), 0, 20, 2, 1)),
                onBuy = { bought = it }, onPage = {})
        } } }
        // The name, in the chosen language and in it alone: no English twin beside it.
        compose.onNodeWithText("Железный шлем").performScrollTo().assertIsDisplayed()
        // The line shows only what the item is and its level; requirements belong to the opened card,
        // not the showcase list. Both lots say exactly that and nothing more, which is why there
        // are two of them: with the requirements gone the two lines became the same sentence.
        compose.onAllNodesWithText("Шлем · ур. 30").assertCountEquals(2)
        compose.onAllNodesWithText("Шлем · ур. 30 · треб. 25 ур., 40 сил").assertCountEquals(0)
        compose.onAllNodesWithText("Сила: нужно 30, есть 14").assertCountEquals(0)
        compose.onAllNodesWithText("Редкий").assertCountEquals(0)
        // The properties are a list, one per line — base first, then the rolls, five at most.
        compose.onNodeWithText("12").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ещё 1").performScrollTo().assertIsDisplayed()
        // The bottom line: the price on the left, the seller on the right.
        compose.onNodeWithText("4 × Сфера хаоса").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Соперник").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("7 × Сфера хаоса").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ваш лот").performScrollTo().assertIsDisplayed()
        // Nothing is bought from a line: the rolls are what is being paid for, so the card opens first.
        compose.onAllNodesWithText("Купить").assertCountEquals(0)
        compose.onNodeWithText("Железный шлем").performScrollTo().performClick()
        compose.onNodeWithText("Купить").performClick()
        // A purchase cannot be undone, so it is asked about before it happens.
        compose.onNodeWithText("Купить лот?").assertIsDisplayed()
        // The price is a ledger line, in the orb it was set in.
        compose.onNodeWithText("−4 × Сфера хаоса").assertIsDisplayed()
        compose.onNodeWithText("УДЕРЖИВАЙТЕ, ЧТОБЫ КУПИТЬ ЛОТ").performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals("lot-1", bought) }
    }

    /** The orb panel hands back the pair the server needs and says what the copy currently is. */
    @Test fun applyingAnOrbEmitsTheInstanceAndTheOrbItself() {
        val instance = EquipmentInstance("ring-instance", "hero", "ring-base", rarity = "UNCOMMON")
        val base = buildJsonObject { put("_id", "ring-base"); put("code", "HERO_RING"); put("slot", "RING"); put("rarity", "COMMON") }
        val chaos = CurrencyItem("chaos-orb", "CHAOS_ORB", "CHAOS_ORB", 300)
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
