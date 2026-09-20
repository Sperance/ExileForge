package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.command.ItemStack
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
import com.sperance.exileforge.core.network.*
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in: CI launches a real backend and MongoDB replica set; no transport mocks. */
class ServerIntegrationTest {
    @Test fun realServerClientContract(): Unit = runBlocking {
        val url = System.getenv("EF_LIVE_URL")
        assumeTrue("Enabled only by the isolated client/server job", !url.isNullOrBlank())
        val api = GameApi(requireNotNull(url))
        api.capabilities().requireWorkbench()
        val admin = api.login("admin", requireNotNull(System.getenv("EF_ADMIN_PASSWORD")))
        assertEquals("ADMIN", admin.role)
        assertEquals(admin.id, assertNotNull(api.currentUser()).id)

        // The seeded modifier catalogue is what every rolled value on an instance points back at.
        val definitions = api.modifierDefinitions()
        assertTrue(definitions.isNotEmpty())
        assertTrue(definitions.any { it.composite })
        val described = definitions.firstOrNull { it.effects.isNotEmpty() } ?: fail("no definition carries an effect: $definitions")
        val tiers = api.modifierTiers(described.id)
        assertTrue(tiers.isNotEmpty() && tiers.all { it.values.isNotEmpty() }, "tiers of ${described.code}: $tiers")

        // A character is nothing without a class: it carries the whole stat base and the tree's root.
        val classes = api.characterClasses()
        val chosenClass = classes.firstOrNull() ?: fail("the server seeded no character classes")
        assertTrue(chosenClass.baseStats.isNotEmpty(), "${chosenClass.code} has no base: $chosenClass")
        assertTrue(chosenClass.startNodeCode.isNotBlank(), "${chosenClass.code} names no start node")
        assertTrue(chosenClass.params.none { it.rolled }, "a class conversion must not be rolled: ${chosenClass.params}")
        val levels = api.experienceLevels()
        assertTrue(levels.isNotEmpty() && levels.first().level == 1, "progression table: $levels")

        val tree = api.skillTree()
        assertTrue(tree.isNotEmpty(), "the server seeded no skill tree")
        val start = tree.firstOrNull { it.code == chosenClass.startNodeCode } ?: fail("${chosenClass.startNodeCode} is not in the tree")
        assertEquals(SkillNodeType.START, start.type)

        val name = "EF-integration-${java.util.UUID.randomUUID()}"
        val character = api.create(Catalog.CHARACTERS, buildJsonObject {
            put("userId", admin.id); put("name", name); put("description", "Integration fixture"); put("classId", chosenClass.id)
        })
        val id = character.entityId
        try {
            assertEquals(name, api.character(id).name)
            assertEquals(1, api.search(Catalog.CHARACTERS, 0, CatalogFilter(query = name)).items.size)
            assertTrue(api.inventory(id).isEmpty())

            // A random grant of a chosen rarity and category: the client names a base, the server rolls it.
            val template = api.randomTemplate("RARE", "HELMET")
            assertEquals("HELMET", template.text("slot"))
            assertEquals("RARE", template.text("rarity"))
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

            val worn = api.equip(id, instance.id)
            assertEquals("HELMET", worn.equippedSlot)
            assertEquals(listOf(instance.id), api.inventory(id).filter { it.equipped }.map { it.id })
            // Wearing the item is what changes the character sheet; the client recomputes nothing.
            val sheet = api.stats(id)
            assertTrue(sheet.stats.isNotEmpty(), "the server returned no stats")
            assertEquals(sheet, api.stats(id))
            assertEquals(character.text("_id"), sheet.characterId)
            // The server names the items it counted and the ones whose requirements are not met.
            assertTrue(instance.id in sheet.active || sheet.inactive.any { it.inventoryId == instance.id },
                "the worn item is in neither list: $sheet")
            sheet.inactive.forEach { refused -> assertTrue(refused.reasons.isNotEmpty(), "refused without a reason: $refused") }
            assertFalse(api.unequip(id, instance.id).equipped)

            // Currency is the same collection as every other item, told apart by its category alone.
            val orbs = api.currencyOrbs()
            assertTrue(orbs.isNotEmpty(), "the server seeded no currency")
            assertTrue(orbs.all { it.orb != null }, "unknown orbs: ${orbs.filter { it.orb == null }.map { it.subCategory }}")
            val chaos = orbs.firstOrNull { it.orb == CurrencyOrb.CHAOS_ORB } ?: fail("no Chaos Orb among ${orbs.map { it.name }}")

            // The orb is spent from the bag, so it is handed over first; the rerolls are the server's.
            assertEquals("Success", api.adjustItems(id, listOf(ItemStack(chaos.id, 1))))
            val rerolled = api.applyOrb(id, instance.id, chaos.id)
            assertTrue(rerolled.message.isNotBlank(), "the server said nothing about what the orb did")
            assertEquals(instance.id, rerolled.item.id)
            assertEquals("RARE", rerolled.item.rarity, "a Chaos Orb must leave the rarity alone: ${rerolled.message}")
            assertNull(rerolled.created, "only a mirror creates a second item")
            assertTrue(api.bag(id).none { it.itemId == chaos.id }, "the orb was not spent")
            // A refusal costs nothing: with no orb left the server rejects the call and keeps the item.
            assertFailsWith<ApiFailure> { api.applyOrb(id, instance.id, chaos.id) }
            assertEquals(rerolled.item.params, api.inventory(id).single { it.id == instance.id }.params)

            val items = api.referencePage(com.sperance.exileforge.core.model.EntitySource.ITEM, 0)
            val item = items.items.firstOrNull() ?: fail("the items collection is empty: $items")
            assertEquals("Success", api.adjustItems(id, listOf(ItemStack(item.entityId, 5))))
            val bag = api.bag(id)
            assertEquals(5L, (bag.firstOrNull { it.itemId == item.entityId } ?: fail("${item.entityId} is not in the bag: $bag")).amount)
            assertEquals("Success", api.adjustItems(id, listOf(ItemStack(item.entityId, -5))))
            assertTrue(api.bag(id).none { it.itemId == item.entityId })

            // A template is editable; an instance's rolls are not reachable from the catalogue at all.
            val edited = api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("description", "Integration description") })
            assertEquals("Integration description", edited.text("description"))
            // An equipment template is a StockEntity: the server keeps no version on it.
            assertFalse("version" in edited, "equipment gained a version: $edited")
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("params", JsonArray(emptyList())) }) }

            // The tree: the start node first, then a neighbour of it, then back again.
            assertTrue(api.characterTree(id).nodes.isEmpty())
            // Points come from levels, so the character is levelled first and the server re-reads it.
            assertTrue(api.addExperience(id, levels.last().experience).level > 1)
            val started = api.allocateNode(id, start.code)
            assertEquals(setOf(start.code), started.takenCodes)
            assertTrue(started.total > 0, "a levelled character has no skill points: $started")
            assertEquals(started.spent, started.nodes.sumOf { node -> node.cost })

            val neighbour = tree.firstOrNull { it.code in start.connections } ?: fail("${start.code} has no neighbour")
            val grown = api.allocateNode(id, neighbour.code)
            assertEquals(setOf(start.code, neighbour.code), grown.takenCodes)
            assertEquals(started.available - neighbour.cost, grown.available)
            // A node's bonuses are a snapshot taken when it was allocated, and they are never rolled.
            assertTrue(grown.nodes.flatMap { node -> node.params }.none { param -> param.rolled })
            // The start node is the tree's root: only a full reset gives it back.
            assertFailsWith<ApiFailure> { api.refundNode(id, start.code) }
            assertEquals(setOf(start.code), api.refundNode(id, neighbour.code).takenCodes)
            assertEquals(0, api.resetTree(id).spent)

            val player = GameApi(url)
            player.login(requireNotNull(System.getenv("EF_PLAYER_LOGIN")), requireNotNull(System.getenv("EF_PLAYER_PASSWORD")))
            assertEquals("USER", assertNotNull(player.currentUser()).role)
        } finally {
            api.delete(Catalog.CHARACTERS, id)
            assertNull(api.get(Catalog.CHARACTERS, id))
        }
    }
}
