package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.display.documentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.icon
import com.sperance.exileforge.core.display.serverIcons
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.command.ItemStack
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLotKind
import com.sperance.exileforge.core.model.auction.AuctionLotStatus
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.core.model.hero.CharacterSheet
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
import com.sperance.exileforge.core.network.*
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in: CI launches a real backend and MongoDB replica set; no transport mocks. */
class ServerIntegrationTest {

    /**
     * How far a template's requirements are out of the character's reach, by the sheet the server
     * just sent. Zero means wearable.
     *
     * This is a fixture choice, not a rule: the server decides who may wear what, and the test only
     * needs a base it will accept and one it will refuse.
     */
    private fun demand(template: JsonObject, sheet: CharacterSheet): Int {
        fun short(key: String, have: Double) = ((template.text(key).toIntOrNull() ?: 0) - have.toInt()).coerceAtLeast(0)
        return short("requiredLevel", sheet.level.toDouble()) +
            short("requiredStrength", sheet.stats["STOCK_STRENGTH"] ?: 0.0) +
            short("requiredDexterity", sheet.stats["STOCK_AGILITY"] ?: 0.0) +
            short("requiredIntelligence", sheet.stats["STOCK_INTELLECT"] ?: 0.0)
    }

    @Test fun realServerClientContract(): Unit = runBlocking {
        val url = System.getenv("EF_LIVE_URL")
        assumeTrue("Enabled only by the isolated client/server job", !url.isNullOrBlank())
        val api = GameApi(requireNotNull(url))
        api.capabilities().requireWorkbench()
        val admin = api.login("admin", requireNotNull(System.getenv("EF_ADMIN_PASSWORD")))
        assertEquals("ADMIN", admin.role)
        assertEquals(admin.id, assertNotNull(api.currentUser()).id)

        // The dictionary the whole client reads: since 0.14.0 no document carries text, so a
        // mismatch between the key this client builds and the one the server wrote would show up
        // nowhere but here — a mocked bundle agrees with itself by construction.
        val manifest = api.localeManifest()
        assertTrue(manifest.languages.map { it.code }.containsAll(listOf("ru", "en")), "languages: ${manifest.languages}")
        for (language in manifest.languages) {
            val bundle = api.localeBundle(language)
            assertEquals(language.code, bundle.language)
            assertTrue(bundle.size > 100, "${language.code} holds only ${bundle.size} strings")
        }
        serverLocale = api.localeBundle(assertNotNull(manifest.language("ru")))
        assertTrue(serverLocale.contains("system.success"), "the server's own success key is missing")

        // The icon set, which only the live server can prove: the manifest's fingerprint is
        // computed from the file, so a set edited without touching the manifest is still noticed.
        val iconManifest = api.iconManifest()
        assertTrue(iconManifest.hash.isNotBlank(), "the server served no icon fingerprint")
        val iconBundle = IconBundle.parse(iconManifest.hash, api.iconDocument(iconManifest.file))
        assertEquals(iconManifest.icons, iconBundle.size, "the manifest and the set disagree on how many codes there are")
        assertEquals(iconManifest.sprites, iconBundle.spriteCount, "the manifest and the set disagree on how many drawings there are")
        // Every code in the table has to name a drawing that exists, or `parse` would have dropped
        // it — which is the quiet failure this whole check is here to catch.
        assertTrue(iconBundle.size > 100, "only ${iconBundle.size} codes carry an icon")
        serverIcons = iconBundle

        // The seeded modifier catalogue is what every rolled value on an instance points back at.
        val definitions = api.modifierDefinitions()
        assertTrue(definitions.isNotEmpty())
        assertTrue(definitions.any { it.composite })
        val described = definitions.firstOrNull { it.effects.isNotEmpty() } ?: fail("no definition carries an effect: $definitions")
        val tiers = api.modifierTiers(described.id)
        assertTrue(tiers.isNotEmpty() && tiers.all { it.values.isNotEmpty() }, "tiers of ${described.code}: $tiers")
        // The key this client builds has to be the key the server wrote, or the template is the code.
        assertTrue(serverLocale.contains(LocaleKey.modifierName(described.code)), "no text for ${described.code}")
        assertNotEquals(described.code, described.template)

        // A character is nothing without a class: it carries the whole stat base and the tree's root.
        val classes = api.characterClasses()
        val chosenClass = classes.firstOrNull() ?: fail("the server seeded no character classes")
        assertTrue(chosenClass.baseStats.isNotEmpty(), "${chosenClass.code} has no base: $chosenClass")
        assertTrue(chosenClass.startNodeCode.isNotBlank(), "${chosenClass.code} names no start node")
        assertTrue(chosenClass.params.none { it.rolled }, "a class conversion must not be rolled: ${chosenClass.params}")
        assertNotEquals(chosenClass.code, chosenClass.title, "the class has no name in the dictionary")
        val levels = api.experienceLevels()
        assertTrue(levels.isNotEmpty() && levels.first().level == 1, "progression table: $levels")

        val tree = api.skillTree()
        assertTrue(tree.isNotEmpty(), "the server seeded no skill tree")
        val start = tree.firstOrNull { it.code == chosenClass.startNodeCode } ?: fail("${chosenClass.startNodeCode} is not in the tree")
        assertEquals(SkillNodeType.START, start.type)
        assertNotEquals(start.code, start.title, "${start.code} has no name in the dictionary")

        val name = "EF-integration-${java.util.UUID.randomUUID()}"
        val character = api.create(Catalog.CHARACTERS, buildJsonObject {
            put("userId", admin.id); put("name", name); put("description", "Integration fixture"); put("classId", chosenClass.id)
        })
        val id = character.entityId
        try {
            assertEquals(name, api.character(id).name)
            assertEquals(1, api.search(Catalog.CHARACTERS, 0, CatalogFilter(query = name)).items.size)
            assertTrue(api.inventory(id).isEmpty())

            // The character menu reads one account's characters, not the whole collection.
            val mine = api.charactersOf(admin.id)
            assertTrue(mine.any { it.id == id }, "the account's own character is missing: ${mine.map { it.name }}")
            assertTrue(mine.all { it.userId == admin.id }, "someone else's character came back: $mine")
            assertTrue(mine.size <= 3, "an account cannot hold more than three characters: ${mine.size}")
            // An unknown account is refused rather than answered with an empty list.
            assertFailsWith<ApiFailure> { api.charactersOf("0123456789abcdef01234567") }

            // Registration by device, which is the whole sign-up: the first attempt is refused with
            // US_015 and turns into the POST that creates the account.
            val guest = GameApi(url)
            val device = "ef-integration-${java.util.UUID.randomUUID()}"
            val registered = guest.loginByDevice(device)
            assertEquals("USER", registered.role, "a device account must not be privileged")
            assertEquals(0, registered.countCharacters, "a fresh account already has characters")
            // The second sign-in finds the same account rather than making a second one.
            assertEquals(registered.id, GameApi(url).loginByDevice(device).id)
            assertTrue(guest.charactersOf(registered.id).isEmpty(), "a new account starts with no characters")
            // The account is left behind: a game client has no route that deletes one, and the
            // client-server job seeds a fresh database for every run anyway.

            // A random grant of a chosen rarity and category: the client names a base, the server rolls it.
            val template = api.randomTemplate("RARE", "HELMET")
            assertEquals("HELMET", template.text("slot"))
            assertEquals("RARE", template.text("rarity"))
            // A template carries a code and no text: its name has to come back from the dictionary.
            assertTrue("name" !in template, "the server still writes text into a template: $template")
            assertNotEquals(template.text("code"), equipmentTitle(template), "no name for ${template.text("code")}")
            // The same code that finds the name has to find the drawing.
            assertNotNull(documentIcon(template), "no icon for ${template.text("code")}")
            val instance = api.grant(id, template.entityId)
            assertEquals(template.entityId, instance.equipmentId)
            assertTrue(instance.params.isNotEmpty(), "the server rolled no modifiers")
            assertTrue(instance.params.all { it.modifierId in definitions.map { definition -> definition.id } })
            // One value per effect of the description: a composite modifier rolls all of them at once.
            instance.params.forEach { rolled ->
                val definition = definitions.firstOrNull { it.id == rolled.modifierId } ?: fail("rolled $rolled names no known definition")
                assertEquals(definition.effects.size, rolled.values.size, "${definition.code} rolled ${rolled.values}")
            }
            assertFalse(instance.equipped)

            // Points come from levels, so the character is levelled before wearing or spending anything.
            assertTrue(api.addExperience(id, levels.last().experience).level > 1)
            val base = api.stats(id)
            assertTrue(base.stats.isNotEmpty(), "the server returned no stats")
            assertEquals(character.text("_id"), base.characterId)

            // Requirements are checked twice by the server, and the two checks are not the same rule:
            // an item out of reach cannot be put on at all, while one already worn merely stops
            // counting. The wearable base is chosen here from what the sheet already says.
            val helmets = api.search(Catalog.EQUIPMENT, 0, CatalogFilter(slot = "HELMET")).items
            assertTrue(helmets.isNotEmpty(), "no helmet templates to wear")
            val wearable = helmets.minByOrNull { demand(it, base) } ?: fail("no helmet templates")
            val wornInstance = api.grant(id, wearable.entityId)
            val worn = api.equip(id, wornInstance.id)
            assertEquals("HELMET", worn.equippedSlot)
            assertEquals(listOf(wornInstance.id), api.inventory(id).filter { it.equipped }.map { it.id })
            // Wearing the item is what changes the character sheet; the client recomputes nothing.
            val sheet = api.stats(id)
            assertEquals(sheet, api.stats(id))
            assertEquals(listOf(wornInstance.id), sheet.active, "the worn item was not counted: $sheet")
            assertTrue(sheet.inactive.isEmpty(), "nothing should be refused here: ${sheet.inactive}")

            // The other half of the rule: a base the sheet cannot reach is refused outright.
            helmets.maxByOrNull { demand(it, base) }?.takeIf { demand(it, base) > 0 }?.let { heavy ->
                val granted = api.grant(id, heavy.entityId)
                val refused = assertFailsWith<ApiFailure> { api.equip(id, granted.id) }
                assertTrue(refused.message.orEmpty().contains(heavy.text("name")), "refusal names no item: ${refused.message}")
            }
            assertFalse(api.unequip(id, wornInstance.id).equipped)

            // Currency is the same collection as every other item, told apart by its category alone.
            val orbs = api.currencyOrbs()
            assertTrue(orbs.isNotEmpty(), "the server seeded no currency")
            assertTrue(orbs.all { it.orb != null }, "unknown orbs: ${orbs.filter { it.orb == null }.map { it.subCategory }}")
            val chaos = orbs.firstOrNull { it.orb == CurrencyOrb.CHAOS_ORB } ?: fail("no Chaos Orb among ${orbs.map { it.code }}")
            assertNotEquals(chaos.code, chaos.title(), "the orb has no name in the dictionary")
            assertNotNull(icon(IconKey.item(chaos.code)), "no icon for ${chaos.code}")

            // The orb is spent from the bag, so it is handed over first; the rerolls are the server's.
            assertEquals("system.success", api.adjustItems(id, listOf(ItemStack(chaos.id, 1))))
            val rerolled = api.applyOrb(id, instance.id, chaos.id)
            // The server sends a key and arguments that are keys themselves: what has to come out
            // is a sentence naming the item, not the key it was built from.
            assertTrue(serverLocale.contains(rerolled.messageKey), "no text for ${rerolled.messageKey}")
            assertNotEquals(rerolled.messageKey, rerolled.message)
            assertFalse(rerolled.message.contains("{0}"), "an argument was never filled: ${rerolled.message}")
            assertEquals(instance.id, rerolled.item.id)
            assertEquals("RARE", rerolled.item.rarity, "a Chaos Orb must leave the rarity alone: ${rerolled.message}")
            assertNull(rerolled.created, "only a mirror creates a second item")
            assertTrue(api.bag(id).none { it.itemId == chaos.id }, "the orb was not spent")
            // A refusal costs nothing: with no orb left the server rejects the call and keeps the item.
            assertFailsWith<ApiFailure> { api.applyOrb(id, instance.id, chaos.id) }
            assertEquals(rerolled.item.params, api.inventory(id).single { it.id == instance.id }.params)

            val items = api.referencePage(com.sperance.exileforge.core.model.EntitySource.ITEM, 0)
            val item = items.items.firstOrNull() ?: fail("the items collection is empty: $items")
            assertEquals("system.success", api.adjustItems(id, listOf(ItemStack(item.entityId, 5))))
            val bag = api.bag(id)
            assertEquals(5L, (bag.firstOrNull { it.itemId == item.entityId } ?: fail("${item.entityId} is not in the bag: $bag")).amount)
            assertEquals("system.success", api.adjustItems(id, listOf(ItemStack(item.entityId, -5))))
            assertTrue(api.bag(id).none { it.itemId == item.entityId })

            // A template is editable; an instance's rolls are not reachable from the catalogue at all.
            // Its words are not editable either: they live in the locale files, not in the document.
            val edited = api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("requiredLevel", 7) })
            assertEquals("7", edited.text("requiredLevel"))
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("description", "by hand") }) }
            // `image` was removed in 0.15.1; it is refused before the request is built, like any
            // field the server does not have. An older client's write is simply ignored instead.
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("image", "http://old/url.png") }) }
            // An equipment template is a StockEntity: the server keeps no version on it.
            assertFalse("version" in edited, "equipment gained a version: $edited")
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("params", JsonArray(emptyList())) }) }

            // Since 0.12.0 the class's start node comes with the character, free of charge.
            val started = api.characterTree(id)
            assertEquals(setOf(start.code), started.takenCodes, "a new character is not on its class node: $started")
            assertTrue(started.total > 0, "a levelled character has no skill points: $started")
            assertEquals(started.nodes.sumOf { node -> node.cost }, started.spent)

            val neighbour = tree.firstOrNull { it.code in start.connections } ?: fail("${start.code} has no neighbour")
            val grown = api.allocateNode(id, neighbour.code)
            assertEquals(setOf(start.code, neighbour.code), grown.takenCodes)
            assertEquals(started.available - neighbour.cost, grown.available)
            // A node's bonuses are a snapshot taken when it was allocated, and they are never rolled.
            assertTrue(grown.nodes.flatMap { node -> node.params }.none { param -> param.rolled })
            // Taking what is already taken is refused, and the start node is the tree's root.
            assertFailsWith<ApiFailure> { api.allocateNode(id, start.code) }
            assertFailsWith<ApiFailure> { api.refundNode(id, start.code) }
            assertEquals(setOf(start.code), api.refundNode(id, neighbour.code).takenCodes)
            // A reset is a respec: it leaves the character standing on its class node, not on nothing.
            val respec = api.resetTree(id)
            assertEquals(setOf(start.code), respec.takenCodes, "a reset emptied the tree: $respec")
            assertEquals(started.available, respec.available)

            // The auction needs two characters: the server refuses to let one buy its own lot.
            val buyerName = "EF-buyer-${java.util.UUID.randomUUID()}"
            val buyer = api.create(Catalog.CHARACTERS, buildJsonObject {
                put("userId", admin.id); put("name", buyerName); put("description", "Auction buyer"); put("classId", chosenClass.id)
            }).entityId
            try {
                // Both ends have to clear the level the auction opens at; the server names it itself.
                api.addExperience(buyer, levels.last().experience)

                val listed = api.sellEquipment(id, wornInstance.id, chaos.id, 3)
                assertEquals(AuctionLotStatus.ACTIVE, listed.status)
                assertEquals(AuctionLotKind.EQUIPMENT, listed.kind)
                // The lot stores a code, and the name comes back through the same dictionary.
                assertEquals(equipmentTitle(wearable), listed.title)
                assertEquals(wearable.text("code"), listed.itemCode)
                // While it is listed the goods live in the lot, not with the seller.
                assertTrue(api.inventory(id).none { it.id == wornInstance.id }, "the listed item stayed in the inventory")
                assertEquals(listOf(listed.id), api.myLots(id).filter { it.onSale }.map { it.id })

                // The showcase is the server's own search, and it hides the seller's own lots.
                val own = api.auctionSearch(id, AuctionFilter(excludeSellerId = id), 0)
                assertTrue(own.items.none { it.id == listed.id }, "the seller sees their own lot: $own")
                // A lot has no name to match, so a text search is resolved to codes on the server —
                // which is why the language the player typed in travels with it.
                val shown = api.auctionSearch(buyer, AuctionFilter(title = listed.title, lang = serverLocale.language), 0)
                assertTrue(shown.items.any { it.id == listed.id }, "the lot is not on the showcase: $shown")
                assertFailsWith<ApiFailure> { api.buyLot(id, listed.id) }

                // Paying: the orbs go to the seller, the goods to the buyer, in one transaction.
                assertEquals("system.success", api.adjustItems(buyer, listOf(ItemStack(chaos.id, 3))))
                val sold = api.buyLot(buyer, listed.id)
                assertEquals(AuctionLotStatus.SOLD, sold.status)
                assertEquals(buyer, sold.buyerId)
                assertTrue(api.inventory(buyer).any { it.equipmentId == wearable.entityId }, "the buyer never got the item")
                assertTrue(api.bag(buyer).none { it.itemId == chaos.id }, "the buyer kept the orbs")
                assertEquals(3L, (api.bag(id).firstOrNull { it.itemId == chaos.id } ?: fail("the seller was not paid")).amount)
                // A closed lot is history: it never returns to the showcase and cannot be bought twice.
                assertFailsWith<ApiFailure> { api.buyLot(buyer, listed.id) }

                // Withdrawing returns the goods; a stack lot travels the same way an instance does.
                val stack = api.sellItem(id, chaos.id, 2, chaos.id, 1)
                assertEquals(2L, stack.amount)
                assertEquals(1L, (api.bag(id).firstOrNull { it.itemId == chaos.id } ?: fail("the stack was not debited")).amount)
                assertFalse(api.cancelLot(id, stack.id).onSale)
                assertEquals(3L, (api.bag(id).firstOrNull { it.itemId == chaos.id } ?: fail("the stack never came back")).amount)
            } finally {
                api.delete(Catalog.CHARACTERS, buyer)
            }

            val player = GameApi(url)
            player.login(requireNotNull(System.getenv("EF_PLAYER_LOGIN")), requireNotNull(System.getenv("EF_PLAYER_PASSWORD")))
            assertEquals("USER", assertNotNull(player.currentUser()).role)
        } finally {
            api.delete(Catalog.CHARACTERS, id)
            assertNull(api.get(Catalog.CHARACTERS, id))
        }
    }
}
