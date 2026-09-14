package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.*
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.network.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import kotlin.test.*

/** Opt-in: CI launches a real backend and MongoDB replica set; no transport mocks. */
class ServerIntegrationTest {
    @Test fun realServerClientContract(): Unit = runBlocking {
        val url = System.getenv("EF_LIVE_URL")
        assumeTrue("Enabled only by the isolated client/server job", !url.isNullOrBlank())
        val api = GameApi(requireNotNull(url))
        api.capabilities().requireWorkbench()
        api.login("admin", requireNotNull(System.getenv("EF_ADMIN_PASSWORD")))
        assertEquals("ADMIN", api.currentUser().role)
        val name = "EF-integration-${java.util.UUID.randomUUID()}"
        val character = api.create(Catalog.CHARACTERS, buildJsonObject { put("name", name); put("description", "Integration fixture") })
        val id = character.entityId
        assertEquals(name, api.character(id).name)
        assertEquals(1, api.search(Catalog.CHARACTERS, 0, CatalogFilter(query = name)).items.size)
        val ring = api.search(Catalog.EQUIPMENT, 0, CatalogFilter(query = "Coral Ring", slot = "RING")).items.single()
        val initial = api.grant(id, GrantEquipmentCommand(0, ring.entityId))
        val instance = initial.inventory.single()
        val preview = api.compareEquipment(id, EquipCommand(initial.characterVersion, instance.uuid, EquipmentSlot.RING_LEFT))
        assertTrue(preview.allowed)
        assertTrue(requireNotNull(preview.after).values.getValue("maximum_life") > preview.before.values.getValue("maximum_life"))
        assertEquals(initial, api.equipment(id))
        assertFalse(api.compareEquipment(id, EquipCommand(initial.characterVersion, instance.uuid, EquipmentSlot.HELMET)).allowed)
        val equipped = api.equip(id, EquipCommand(initial.characterVersion, instance.uuid, EquipmentSlot.RING_LEFT))
        assertEquals(preview.after!!.values, equipped.stats.values)
        assertEquals(409, assertFailsWith<ApiFailure> { api.unequip(id, UnequipCommand(initial.characterVersion, EquipmentSlot.RING_LEFT)) }.status)
        val currency = api.currencies().single { it.text("id") == "TRANSMUTATION" }
        val funded = api.adjustItems(id, AdjustItemsCommand(equipped.characterVersion, listOf(ItemStack(currency.text("itemId"), 2))))
        assertTrue(api.craftOptions(id, instance.uuid).options.single { it.currency == "TRANSMUTATION" }.available)
        val request = buildJsonObject { put("requestId", java.util.UUID.randomUUID().toString()); put("expectedVersion", funded.characterVersion); put("equipmentUuid", instance.uuid); put("currency", "TRANSMUTATION") }
        val result = api.mutateInventory(id, "craft", request)
        assertEquals(result, api.mutateInventory(id, "craft", request))
        val crafted = api.equipment(id)
        assertEquals("MAGIC", crafted.inventory.single().poe?.rarity)
        assertEquals(1, crafted.items.single { it.itemId == currency.text("itemId") }.amount.toInt())
        assertFalse(api.craftOptions(id, instance.uuid).options.single { it.currency == "TRANSMUTATION" }.available)
        val player = GameApi(url)
        player.login(requireNotNull(System.getenv("EF_PLAYER_LOGIN")), requireNotNull(System.getenv("EF_PLAYER_PASSWORD")))
        assertEquals("USER", player.currentUser().role)
        assertNull(player.get(Catalog.CHARACTERS, id))
        val own = player.create(Catalog.CHARACTERS, buildJsonObject { put("name", "Player-$name") })
        assertEquals(setOf(own.entityId), player.page(Catalog.CHARACTERS, 0).items.map { it.entityId }.toSet())
        assertEquals(403, assertFailsWith<ApiFailure> { player.grant(own.entityId, GrantEquipmentCommand(0, ring.entityId)) }.status)
        player.delete(Catalog.CHARACTERS, own.entityId, own.entityVersion)
        api.delete(Catalog.CHARACTERS, id, crafted.characterVersion)
        assertNull(api.get(Catalog.CHARACTERS, id))
    }
}
