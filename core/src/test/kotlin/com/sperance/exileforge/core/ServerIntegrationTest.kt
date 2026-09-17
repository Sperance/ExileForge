package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.*
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.model.passives.*
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
        val capabilities = api.capabilities()
        capabilities.requireWorkbench()
        // Icons are public: the whole set, its bindings and a conditional refresh, all before login.
        assertTrue(capabilities.hasIcons())
        val manifest = api.icons()
        assertEquals(capabilities.iconSetVersion, manifest.version)
        assertEquals(capabilities.iconCount, manifest.icons.size)
        val sprite = api.iconSprite()
        val drawings = com.sperance.exileforge.core.display.svg.parseSvgSprite(sprite.svg)
        assertEquals(manifest.icons.map { it.id }.toSet(), drawings.keys)
        assertTrue(drawings.values.all { it.art.isNotEmpty() && it.frame.isNotEmpty() })
        assertTrue(api.iconSprite(sprite.etag).unchanged)
        val icons = com.sperance.exileforge.core.display.icons.IconSet(manifest, api.iconBindings(), drawings)
        assertTrue(icons.ready)
        assertNotNull(icons.drawing(icons.fallback))
        val plain = assertNotNull(com.sperance.exileforge.core.display.svg.parseSvgIcon(api.iconSvg("ui-unknown", plain = true).svg, "ui-unknown"))
        assertTrue(plain.frame.isEmpty() && plain.art.isNotEmpty())
        assertEquals(404, assertFailsWith<ApiFailure> { api.iconSvg("no-such-icon") }.status)
        api.login("admin", requireNotNull(System.getenv("EF_ADMIN_PASSWORD")))
        assertEquals("ADMIN", api.currentUser().role)
        val name = "EF-integration-${java.util.UUID.randomUUID()}"
        val character = api.create(Catalog.CHARACTERS, buildJsonObject { put("name", name); put("description", "Integration fixture") })
        val id = character.entityId
        assertEquals(name, api.character(id).name)
        assertEquals(1, api.search(Catalog.CHARACTERS, 0, CatalogFilter(query = name)).items.size)
        val ring = api.search(Catalog.EQUIPMENT, 0, CatalogFilter(query = "Coral Ring", slot = "RING")).items.single()
        // Every entity the server ships resolves to a drawing the client already holds.
        assertNotNull(icons.drawing(icons.forDocument(ring)))
        assertEquals(ring.text("icon"), icons.forDocument(ring))
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
        val zones = player.combatCatalog().zones
        assertEquals(3, zones.size)
        assertTrue(zones.all { zone -> zone.icon != null && (zone.monsters + zone.boss).all { icons.drawing(icons.forMonster(it)) != null } })
        assertEquals(404, assertFailsWith<ApiFailure> { player.battle(id) }.status)
        val start = StartBattleCommand(own.entityVersion, java.util.UUID.randomUUID().toString(), "coast")
        var combat = player.startBattle(own.entityId, start)
        assertEquals(combat, player.startBattle(own.entityId, start))
        assertEquals(player.equipment(own.entityId).stats.values.getValue("maximum_life"), combat.battle!!.hero.maxLife)
        while(combat.battle!!.status == BattleStatus.ACTIVE) {
            val b = combat.battle!!
            val action = if(b.hero.life < b.hero.maxLife * .65 && b.potions > 0) BattleAction.POTION
                else if(b.hero.mana >= 8) BattleAction.POWER else BattleAction.ATTACK
            val turn = BattleActionCommand(combat.characterVersion, java.util.UUID.randomUUID().toString(), b.id, action)
            combat = player.actBattle(own.entityId, turn)
            assertEquals(combat, player.actBattle(own.entityId, turn))
            assertEquals(409, assertFailsWith<ApiFailure> {
                player.actBattle(own.entityId, turn.copy(requestId = java.util.UUID.randomUUID().toString()))
            }.status)
        }
        assertEquals(BattleStatus.VICTORY, combat.battle!!.status)
        assertTrue(combat.battle!!.rewards.any { it.name == "Опыт" && it.amount > 0 })
        assertTrue(player.character(own.entityId).experience > 0)
        assertEquals(combat, player.battle(own.entityId))
        // Earn the first level through actual gameplay, not a privileged stat mutation.
        repeat(3) { index ->
            combat = player.startBattle(own.entityId, StartBattleCommand(combat.characterVersion, java.util.UUID.randomUUID().toString(), "coast", index == 2))
            while(combat.battle!!.status == BattleStatus.ACTIVE) {
                val b = combat.battle!!
                val action = if(b.hero.life < b.hero.maxLife * .65 && b.potions > 0) BattleAction.POTION
                    else if(b.hero.mana >= 8) BattleAction.POWER else BattleAction.ATTACK
                combat = player.actBattle(own.entityId, BattleActionCommand(combat.characterVersion, java.util.UUID.randomUUID().toString(), b.id, action))
            }
            assertEquals(BattleStatus.VICTORY, combat.battle!!.status)
        }
        assertTrue(player.capabilities().passiveTree)
        val nodes = player.passiveTree().nodes
        assertEquals(115, nodes.size)
        assertTrue(nodes.all { it.icon != null && icons.drawing(icons.forNode(it)) != null })
        assertEquals(404, assertFailsWith<ApiFailure> { player.passiveState(id) }.status)
        var skills = player.passiveState(own.entityId)
        assertTrue(skills.availablePoints >= 1)
        val beforeLife = skills.stats.values.getValue("maximum_life")
        val allocate = PassiveCommand(skills.characterVersion, skills.treeRevision, java.util.UUID.randomUUID().toString(), PassiveAction.ALLOCATE, "origin")
        skills = player.changePassives(own.entityId, allocate)
        assertEquals(beforeLife + 5, skills.stats.values.getValue("maximum_life"))
        assertEquals(skills, player.changePassives(own.entityId, allocate))
        assertEquals(skills, player.passiveState(own.entityId))
        assertEquals(409, assertFailsWith<ApiFailure> { player.changePassives(own.entityId, allocate.copy(requestId = java.util.UUID.randomUUID().toString())) }.status)
        assertTrue(api.passiveState(id).allocated.isEmpty())
        assertEquals(skills.stats, player.equipment(own.entityId).stats)
        combat = player.startBattle(own.entityId, StartBattleCommand(skills.characterVersion, java.util.UUID.randomUUID().toString(), "coast"))
        assertEquals(skills.stats.values.getValue("maximum_life"), combat.battle!!.hero.maxLife)
        val locked = player.passiveState(own.entityId)
        assertNotNull(locked.lockedReason)
        assertEquals(400, assertFailsWith<ApiFailure> { player.changePassives(own.entityId, PassiveCommand(locked.characterVersion, locked.treeRevision, java.util.UUID.randomUUID().toString(), PassiveAction.RESET)) }.status)
        combat = player.actBattle(own.entityId, BattleActionCommand(combat.characterVersion, java.util.UUID.randomUUID().toString(), combat.battle!!.id, BattleAction.FLEE))
        skills = player.changePassives(own.entityId, PassiveCommand(combat.characterVersion, skills.treeRevision, java.util.UUID.randomUUID().toString(), PassiveAction.RESET))
        assertTrue(skills.allocated.isEmpty())
        assertEquals(beforeLife, skills.stats.values.getValue("maximum_life"))
        player.delete(Catalog.CHARACTERS, own.entityId, skills.characterVersion)
        api.delete(Catalog.CHARACTERS, id, crafted.characterVersion)
        assertNull(api.get(Catalog.CHARACTERS, id))
    }
}
