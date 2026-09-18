package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.command.ItemStack
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
        val tiers = api.modifierTiers(definitions.first { it.effects.isNotEmpty() }.id)
        assertTrue(tiers.isNotEmpty() && tiers.all { it.values.isNotEmpty() })

        val name = "EF-integration-${java.util.UUID.randomUUID()}"
        val character = api.create(Catalog.CHARACTERS, buildJsonObject { put("userId", admin.id); put("name", name); put("description", "Integration fixture") })
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
                val definition = definitions.single { it.id == rolled.modifierId }
                assertEquals(definition.effects.size, rolled.values.size)
            }
            assertFalse(instance.equipped)

            val worn = api.equip(id, instance.id)
            assertEquals("HELMET", worn.equippedSlot)
            assertEquals(listOf(instance.id), api.inventory(id).filter { it.equipped }.map { it.id })
            // Wearing the item is what changes the character sheet; the client recomputes nothing.
            val stats = api.stats(id)
            assertTrue(stats.isNotEmpty())
            assertEquals(stats, api.stats(id))
            assertFalse(api.unequip(id, instance.id).equipped)

            val item = api.referencePage(com.sperance.exileforge.core.model.EntitySource.ITEM, 0).items.first()
            assertEquals("Success", api.adjustItems(id, listOf(ItemStack(item.entityId, 5))))
            assertEquals(5L, api.bag(id).single { it.itemId == item.entityId }.amount)
            assertEquals("Success", api.adjustItems(id, listOf(ItemStack(item.entityId, -5))))
            assertTrue(api.bag(id).none { it.itemId == item.entityId })

            // A template is editable; an instance's rolls are not reachable from the catalogue at all.
            val edited = api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("description", "Integration description") })
            assertEquals("Integration description", edited.text("description"))
            assertTrue(edited.entityVersion > template.entityVersion)
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("params", JsonArray(emptyList())) }) }

            val player = GameApi(url)
            player.login(requireNotNull(System.getenv("EF_PLAYER_LOGIN")), requireNotNull(System.getenv("EF_PLAYER_PASSWORD")))
            assertEquals("USER", assertNotNull(player.currentUser()).role)
        } finally {
            api.delete(Catalog.CHARACTERS, id)
            assertNull(api.get(Catalog.CHARACTERS, id))
        }
    }
}
