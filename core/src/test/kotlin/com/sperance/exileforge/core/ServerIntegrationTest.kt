package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot

import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.PortraitSvg
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.display.documentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.icon
import com.sperance.exileforge.core.display.serverIcons
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.modifier.PoolKind
import com.sperance.exileforge.core.model.command.ItemStack
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.model.command.RedemptionKind
import com.sperance.exileforge.core.model.command.RedemptionReward
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLotKind
import com.sperance.exileforge.core.model.auction.AuctionLotStatus
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.core.model.hero.CharacterSheet
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlinx.serialization.json.*
import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.network.*
import kotlin.test.*

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

    /**
     * The floor of a rarity (server 0.65.0): a magic item carries at least one affix, a rare one
     * four, a rare jewel three. Affixes are the lines a card draws in blue under the base; an
     * implicit, a corruption or an enchantment does not count.
     */
    private fun assertFloor(item: com.sperance.exileforge.core.model.hero.EquipmentInstance, slot: String?,
        definitions: List<com.sperance.exileforge.core.model.modifier.ModifierDefinition>) {
        val floor = when (item.rarity) { "UNCOMMON" -> 1; "RARE" -> if (slot == "JEWEL") 3 else 4; else -> return }
        val affixes = item.params.count { param -> definitions.firstOrNull { it.code == param.modifierCode }?.source?.name in setOf("PREFIX", "SUFFIX") }
        assertTrue(affixes >= floor, "${item.rarity} ${item.equipmentId} ($slot) carries $affixes affixes, below $floor: ${item.params}")
    }

    /**
     * Since 0.21.0 the server knows who asks: a player is refused whatever is not theirs, the
     * catalogue's generic writes belong to an administrator alone, and a session is its token.
     * Kept apart from the contract test because that one is already as large as a JVM method gets.
     */
    /**
     * The bench, fracturing and influence (server 0.23.0), against the real rules.
     *
     * Each step is set up so the server's answer is certain rather than likely: a magic item is
     * scoured, transmuted and, when it rolled two, annulled down to one affix, so exactly one place of the other kind is
     * free for the bench; a rare is fractured and then rerolled, and must keep what was fractured.
     */
    /**
     * Two rings and two hands (server 0.24.0): the second ring goes to the free place or the one
     * named, and a two-handed weapon takes the off-hand with it. The lightest bases are chosen, so
     * the character's own attributes are enough to wear them.
     */
    private suspend fun handsAreTheServers(api: GameApi, id: String) {
        val sheet = api.hero.stats(id)
        val catalogue = api.catalog.equipment().filter { it.text("rarity") != "UNIQUE" }
        fun lightest(slot: String) = catalogue.filter { it.text("slot") == slot }.minByOrNull { demand(it, sheet) }
            ?: fail("no $slot base in the catalogue")

        val ring = lightest("RING").entityId
        val first = api.hero.equip(id, api.hero.grant(id, ring).id)
        val second = api.hero.equip(id, api.hero.grant(id, ring).id)
        assertEquals(setOf("RING", "RING_2"), setOf(first.equippedSlot, second.equippedSlot), "two rings did not take two places")
        val third = api.hero.equip(id, api.hero.grant(id, ring).id, "RING_2")
        assertEquals("RING_2", third.equippedSlot)
        val worn = api.hero.inventory(id).filter { it.equipped }
        assertEquals(1, worn.count { it.equippedSlot == "RING_2" }, "the second place holds two rings")
        assertTrue(worn.any { it.id == first.id || it.id == second.id }, "the other ring was taken off too")

        val shield = api.hero.equip(id, api.hero.grant(id, lightest("SHIELD").entityId).id)
        val twoHanded = api.hero.equip(id, api.hero.grant(id, lightest("WEAPON_2H").entityId).id)
        assertEquals("WEAPON_2H", twoHanded.equippedSlot)
        assertTrue(api.hero.inventory(id).none { it.id == shield.id && it.equipped }, "a two-handed weapon left the shield on")
    }

    /**
     * The campaign: the client fights, the server pays. A kill in a closed zone or of a monster that
     * does not live there is refused, and a slain boss passes its zone and opens the ones it leads to (0.67.0).
     * Since 0.68.0 the world holds six regions and 105 zones, up to level 70.
     */
    private suspend fun campaignIsTheServers(api: GameApi, id: String, definitions: List<com.sperance.exileforge.core.model.modifier.ModifierDefinition>) {
        val view = api.campaign.world()
        val maps = view.zones
        assertEquals(105, maps.size)
        assertEquals(70, maps.maxOf { it.level })
        maps.forEach { map ->
            assertTrue(map.monsters.size in 2..4, "${map.code}: ${map.monsters.size} monsters")
            assertTrue(serverLocale.contains(com.sperance.exileforge.core.i18n.LocaleKey.mapName(map.code)), "no name for ${map.code}")
            map.monsters.forEach { assertTrue(serverLocale.contains(com.sperance.exileforge.core.i18n.LocaleKey.monsterName(it.code)), "no name for ${it.code}") }
        }
        val first = maps.single { it.from.isEmpty() }
        assertEquals(listOf(first.code), api.campaign.progress(id).unlocked)
        val reward = api.campaign.kill(id, first.code, first.monsters.first().code, com.sperance.exileforge.core.model.campaign.MonsterRarity.RARE)
        assertTrue(reward.experience > 0, "a kill gave no experience")
        val slots = api.catalog.equipment().associate { it.entityId to it.text("slot") }
        reward.equipment.forEach { assertFloor(it, slots[it.equipmentId], definitions) }
        val bag = api.hero.bag(id)
        reward.items.forEach { stack -> assertTrue(bag.any { it.itemId == stack.itemId }, "the looted ${stack.itemId} is not in the bag") }
        assertEquals("CP_004", assertFailsWith<ApiFailure> { api.campaign.kill(id, first.code, maps.last().monsters.first().code, com.sperance.exileforge.core.model.campaign.MonsterRarity.NORMAL) }.code)
        val closed = maps.first { it.code != first.code }
        assertEquals("CP_003", assertFailsWith<ApiFailure> { api.campaign.kill(id, closed.code, closed.monsters.first().code, com.sperance.exileforge.core.model.campaign.MonsterRarity.NORMAL) }.code)
        // Chests (0.31.0): the window says how many, and one more than that is refused.
        val chests = api.campaign.chests(id, first.code)
        repeat(chests.left) { api.campaign.openChest(id, first.code) }
        assertEquals(0, api.campaign.chests(id, first.code).left)
        assertEquals("CP_006", assertFailsWith<ApiFailure> { api.campaign.openChest(id, first.code) }.code)
        // The boss (0.32.0): the exit is sealed while it lives, and a slain one cannot be slain again.
        assertTrue(api.campaign.boss(id, first.code).alive)
        assertEquals("CP_007", assertFailsWith<ApiFailure> { api.campaign.leave(id, first.code) }.code)
        val slain = api.campaign.slayBoss(id, first.code)
        assertTrue(slain.experience > 0)
        val passed = assertNotNull(slain.progress, "the boss answered no progress")
        assertEquals(listOf(first.code), passed.cleared)
        assertTrue(first.to.isNotEmpty() && first.to.all { it in passed.unlocked }, "the boss opened ${passed.unlocked}, not ${first.to}")
        assertEquals("CP_008", assertFailsWith<ApiFailure> { api.campaign.slayBoss(id, first.code) }.code)
        assertNotNull(first.boss, "the map has no boss")
        assertEquals(passed, api.campaign.leave(id, first.code))
        mapsAreTheServers(api, id)
    }

    /** Maps (0.35.0): the rule is served, and a location is entered without one — its whole zone answered (0.68.1); another location's map is refused. */
    private suspend fun mapsAreTheServers(api: GameApi, id: String) {
        val view = api.campaign.world()
        assertTrue(view.maps.risk.isNotEmpty(), "no risk weights")
        val first = view.zones.first()
        val launch = api.campaign.start(id, first.code)
        assertNull(launch.map)
        // Server 0.68.1: the world's token leaves the modifier pools out and the entry brings the zone whole.
        assertTrue(first.modifiers.isEmpty() && first.boss?.pool.orEmpty().isEmpty(), "the world still carries ${first.code}'s pools")
        val zone = assertNotNull(launch.zone, "the entry brought no zone")
        assertEquals(first.code, zone.code)
        assertTrue(zone.modifiers.isNotEmpty() && zone.boss?.pool.orEmpty().isNotEmpty(), "the entry's ${zone.code} carries no pools")
        assertEquals(api.campaign.chests(id, first.code).left, launch.chests.left)
        assertEquals("CH_008", assertFailsWith<ApiFailure> { api.campaign.start(id, first.code, "0".repeat(24)) }.code)
        craftsAreTheServers(api, id)
    }

    /** The crafts (0.37.0, six since 0.38.0, seven with enchanting since 0.66.0): a starter tool in every profession's slot, a first-level work started and stopped, a locked one refused. */
    private suspend fun craftsAreTheServers(api: GameApi, id: String) {
        val state = api.crafts.state(id)
        assertEquals(7, state.professions.size)
        state.professions.forEach { assertNotNull(it.equipped, "${it.code}: no starter tool") }
        val mining = state.professions.first { it.code == "MINING" }
        assertEquals("MINING", api.crafts.start(id, mining.jobs.first { it.level == 1 }.code).work?.profession)
        assertEquals("CF_003", assertFailsWith<ApiFailure> { api.crafts.start(id, mining.jobs.maxBy { it.level }.code) }.code)
        assertNull(api.crafts.stop(id).work)
    }

    private suspend fun craftingIsTheServers(api: GameApi, id: String, templateId: String,
        definitions: List<com.sperance.exileforge.core.model.modifier.ModifierDefinition>) {
        val orbs = api.world.orbs().associateBy { it.orb }
        fun orb(which: CurrencyOrb) = orbs[which]?.id ?: fail("no $which among ${orbs.keys}")
        suspend fun give(which: CurrencyOrb, amount: Long) = api.hero.adjustItems(id, listOf(ItemStack(orb(which), amount)))
        suspend fun owned(itemId: String) = api.hero.bag(id).firstOrNull { it.itemId == itemId }?.amount ?: 0L
        fun affixes(params: List<com.sperance.exileforge.core.model.modifier.Modifier>) = params.mapNotNull { param ->
            definitions.firstOrNull { it.code == param.modifierCode }?.takeIf { it.source.name in setOf("PREFIX", "SUFFIX") }
        }

        give(CurrencyOrb.ORB_OF_SCOURING, 2); give(CurrencyOrb.ORB_OF_TRANSMUTATION, 1); give(CurrencyOrb.ORB_OF_ANNULMENT, 2)
        val magic = api.hero.grant(id, templateId).id
        api.hero.applyOrb(id, magic, orb(CurrencyOrb.ORB_OF_SCOURING))
        // Since server 0.53.0 a magic item rolls one or two affixes, and an Orb of Annulment never takes
        // it below one: only a second affix is taken off, so a single place is left free either way.
        val transmutedItem = api.hero.applyOrb(id, magic, orb(CurrencyOrb.ORB_OF_TRANSMUTATION)).item
        assertFloor(transmutedItem, "HELMET", definitions)
        val transmuted = transmutedItem.params
        val kept = affixes(if (affixes(transmuted).size == 2) api.hero.applyOrb(id, magic, orb(CurrencyOrb.ORB_OF_ANNULMENT)).item.params
            else transmuted).single()

        // Since 0.46.0 a bench line is found on a map, so a fresh hero knows none: the administrator
        // writes every crafted modifier's tiers into the character (`<code>_T<tier>`, unknown ones ignored).
        val known = definitions.filter { it.crafted }.flatMap { definition -> (1..10).map { "${definition.code}_T$it" } }
        com.sperance.exileforge.core.network.Transport(requireNotNull(System.getenv("EF_LIVE_URL")), com.sperance.exileforge.core.network.RequestJournal(),
            okhttp3.OkHttpClient()) {}.apply { token = api.sessionToken() }
            .request("PUT", "api/v1/character", mapOf("id" to id), buildJsonObject { put("knownBenchRecipes", JsonArray(known.map(::JsonPrimitive))) }, authenticated = true)
        val bench = api.hero.bench(id)
        assertTrue(bench.isNotEmpty(), "the server has no bench")
        val recipe = bench.firstOrNull { it.fits("HELMET") && it.source != kept.source && it.group != kept.groupKey }
            ?: fail("no bench line for the free ${kept.source} place beside ${kept.code}")
        give(CurrencyOrb.valueOf(recipe.orb), recipe.amount * 2)
        val crafted = api.hero.craft(id, magic, recipe.code)
        assertTrue(serverLocale.contains(crafted.messageKey), "no text for ${crafted.messageKey}")
        assertTrue(crafted.item.params.any { it.modifierCode == recipe.modifierCode && it.tier == recipe.tier }, "the bench placed nothing: ${crafted.item.params}")
        assertTrue(definitions.single { it.code == recipe.modifierCode }.crafted)
        assertEquals(recipe.amount, owned(recipe.orbItemId), "the bench took the wrong price")
        // One crafted modifier per item, and a refusal costs nothing.
        assertEquals("CR_015", assertFailsWith<ApiFailure> { api.hero.craft(id, magic, recipe.code) }.code)
        assertEquals(recipe.amount, owned(recipe.orbItemId), "a refused craft was paid for")
        val uncrafted = api.hero.uncraft(id, magic)
        assertTrue(uncrafted.item.params.none { it.modifierCode == recipe.modifierCode }, "the crafted modifier stayed")

        // A fractured affix survives a reroll.
        give(CurrencyOrb.FRACTURING_ORB, 1); give(CurrencyOrb.CHAOS_ORB, 1); give(CurrencyOrb.SHAPERS_ORB, 1)
        val rare = api.hero.grant(id, templateId)
        assertTrue(affixes(rare.params).size >= 4, "a rare template rolled fewer than four affixes: ${rare.params}")
        val fractured = api.hero.applyOrb(id, rare.id, orb(CurrencyOrb.FRACTURING_ORB)).item.params.single { it.fractured }
        val rerolledItem = api.hero.applyOrb(id, rare.id, orb(CurrencyOrb.CHAOS_ORB)).item
        assertFloor(rerolledItem, "HELMET", definitions)
        val rerolled = rerolledItem.params
        assertTrue(fractured in rerolled, "a Chaos Orb moved a fractured affix")

        // An influence needs a free place. A rare rolls four to six affixes since server 0.53.0, so a place
        // is made only when all six are taken; the Shaper fills it from their own pool.
        if (affixes(rerolled).size >= 6) api.hero.applyOrb(id, rare.id, orb(CurrencyOrb.ORB_OF_ANNULMENT))
        val shaped = api.hero.applyOrb(id, rare.id, orb(CurrencyOrb.SHAPERS_ORB)).item
        assertEquals("SHAPER", shaped.influence)
        assertTrue(shaped.params.any { param -> definitions.firstOrNull { it.code == param.modifierCode }?.influence == "SHAPER" },
            "the Shaper's Orb added nothing of the Shaper's: ${shaped.params}")
        assertTrue(fractured in shaped.params, "the fractured affix was lost")
    }

    /** Pools are a catalogue of their own since server 0.56.0: read by anyone, written by an administrator. */
    private suspend fun poolsAreTheServers(api: GameApi, definitions: List<com.sperance.exileforge.core.model.modifier.ModifierDefinition>) {
        val pools = api.world.pools()
        assertTrue(pools.any { it.kind == PoolKind.MODIFIER && it.code == "helmet" && it.entries.isNotEmpty() }, "no helmet pool among ${pools.map { it.code }}")
        assertTrue(pools.filter { it.kind == PoolKind.MODIFIER }.flatMap { it.entries.keys }.all { code -> definitions.any { it.code == code } },
            "a modifier pool names a modifier the server does not have")
        val tag = "ef_test:${System.nanoTime()}"
        val pool = api.catalog.create(Catalog.POOLS, JsonObject(template(Catalog.POOLS) + ("code" to JsonPrimitive(tag))))
        assertEquals(tag, pool.text("code"))
        val weighed = api.catalog.update(Catalog.POOLS, pool.entityId, buildJsonObject { put("entries", buildJsonObject { put(definitions.first().code, 3) }) })
        assertEquals("3", (weighed["entries"] as? JsonObject)?.text(definitions.first().code), "the pool kept no weight: $weighed")
        assertEquals(1, api.catalog.search(Catalog.POOLS, 0, CatalogFilter(query = tag)).items.size)
        api.catalog.delete(Catalog.POOLS, pool.entityId)
        assertNull(api.catalog.get(Catalog.POOLS, pool.entityId))
    }

    /**
     * Every stat a modifier of any pool — an item's, a monster's, a map's, a flask's — an atlas node or a
     * skill names has the client's own words in both languages (2.77.0, every pool since 2.78.0): no line
     * goes unnamed on the client, checked against the live server.
     */
    private suspend fun statsAreNamed(api: GameApi, definitions: List<com.sperance.exileforge.core.model.modifier.ModifierDefinition>) {
        val manifest = api.manifest(fresh = true)
        val world = com.sperance.exileforge.core.model.sync.WorldTables.parse(manifest.world.hash, api.files.worldDocument(manifest.world.file))
        val skillStats = world.skills.skills.flatMap { skill ->
            skill.stats + listOfNotNull(skill.hit, skill.trigger?.hit).flatMap { it.stats } + listOfNotNull(skill.buff, skill.trigger?.buff).flatMap { it.stats } +
                skill.curse?.stats.orEmpty()
        } + world.skills.monsterSkills.flatMap { it.buff?.stats.orEmpty() + it.curse?.stats.orEmpty() }
        val stats = (definitions.flatMap { it.effects }.map { it.stat } + api.atlas.tree().nodes.flatMap { it.effects }.map { it.stat } +
            skillStats.map { it.stat }).distinct()
        assertTrue(stats.any { it.startsWith("ATLAS_") }, "no atlas stat among $stats")
        com.sperance.exileforge.core.i18n.Lang.entries.forEach { lang ->
            val missing = stats.filter { "enum.stat.$it" !in com.sperance.exileforge.core.i18n.UiStrings.keys(lang) }
            assertTrue(missing.isEmpty(), "${lang.code}: no client name for $missing")
        }
    }

    /**
     * The class skills, the belt and the crystals (server 0.69.0): a new hero knows the class's first active
     * and passive skill, both in their slots, and wears a small life flask; the slots and the belt take a
     * condition; a book not in the bag is refused; the crystals of a zone are a window like the chests.
     */
    private suspend fun skillsAreTheServers(api: GameApi, id: String) {
        val manifest = api.manifest(fresh = true)
        val world = com.sperance.exileforge.core.model.sync.WorldTables.parse(manifest.world.hash, api.files.worldDocument(manifest.world.file))
        assertTrue(world.skills.classes.size >= 7 && world.skills.skills.size >= 7 * 14, "skills: ${world.skills.skills.size}")
        assertTrue(world.books.size == world.skills.skills.size, "a skill has no book: ${world.books.size} of ${world.skills.skills.size}")
        assertTrue(world.essenceBook.kinds.isNotEmpty() && world.essences.size == world.essenceBook.kinds.size * world.essenceBook.tiers.size + world.essenceBook.specials.size,
            "essences: ${world.essences.size}")
        world.skills.skills.forEach { assertTrue(serverLocale.contains("skill.${it.code}.name"), "no name for the skill ${it.code}") }
        val character = api.hero.character(id)
        val skills = character.skills
        assertEquals(2, skills.learned.size, "the starter skills: $skills")
        val active = assertNotNull(skills.active.firstOrNull(), "no active skill in the first slot: $skills")
        assertNotNull(skills.passive.firstOrNull(), "no passive skill in the first slot: $skills")
        val flask = api.hero.inventory(id).firstOrNull { it.equippedSlot == "FLASK" }
        assertNotNull(flask, "the new hero wears no flask")
        val manual = api.hero.slotSkill(id, "ACTIVE", 0, active.skill, "MANUAL")
        assertEquals(com.sperance.exileforge.core.model.skills.SlotCondition.MANUAL, manual.active.first()?.condition)
        val belt = api.hero.flaskCondition(id, 0, "LIFE_35")
        assertEquals(com.sperance.exileforge.core.model.skills.SlotCondition.LIFE_35, belt.flasks.first())
        assertFailsWith<ApiFailure> { api.hero.learnSkill(id, active.skill) }
        val zone = api.campaign.world().zones.first()
        api.campaign.crystals(id, zone.code).crystals.flatMap { it.essences }.forEach { code ->
            assertNotNull(world.essenceBook.essence(code), "a crystal holds an unknown essence $code")
        }
    }

    /** The merchant's shelf and the map services (0.34.0): the server's rolls, prices and refusals. */
    private suspend fun merchantIsTheServers(api: GameApi, id: String, definitions: List<com.sperance.exileforge.core.model.modifier.ModifierDefinition>) {
        val stock = api.merchant.stock(id)
        // Twelve to sixteen pieces of gear and, since server 0.69.0, one or two flasks beside them.
        assertTrue(stock.offers.size in 12..18, "the merchant laid out ${stock.offers.size} items")
        assertTrue(stock.offers.all { it.price > 0 && it.item.rarity in setOf("COMMON", "UNCOMMON", "RARE") })
        // Server 0.66.2: nothing magic or rare below its floor on the shelf.
        val slots = api.catalog.equipment().associate { it.entityId to it.text("slot") }
        stock.offers.forEach { assertFloor(it.item, slots[it.item.equipmentId], definitions) }
        assertEquals(stock, api.merchant.stock(id), "the shelf changed before its window ended")
        val first = api.campaign.world().zones.first()
        val chests = api.campaign.chests(id, first.code)
        assertTrue(!chests.bought)
    }

    /** Every portrait the server serves is one the client can draw, and every class has one. */
    private suspend fun portraitsAreDrawable(api: GameApi) {
        val portraits = api.files.portraitManifest()
        assertTrue(portraits.portraits.keys.count { it.startsWith("class.") } >= 7, "a class has no portrait")
        portraits.portraits.keys.forEach { key ->
            assertTrue(PortraitSvg.parse(api.files.portraitDocument(key)).shapes.isNotEmpty(), "$key drew nothing")
        }
    }

    private suspend fun accessIsTheServers(url: String, guest: GameApi, guestId: String, adminId: String, adminCharacter: String) {
        assertEquals(403, assertFailsWith<ApiFailure> { guest.hero.character(adminCharacter) }.status)
        assertEquals(403, assertFailsWith<ApiFailure> { guest.hero.charactersOf(adminId) }.status)
        assertEquals(403, assertFailsWith<ApiFailure> { guest.catalog.update(Catalog.CHARACTERS, adminCharacter, buildJsonObject { put("name", "stolen") }) }.status)
        assertEquals(403, assertFailsWith<ApiFailure> { guest.hero.stats(adminCharacter) }.status)
        // A kept token restores the session on a fresh client, and a revoked one is refused.
        val kept = requireNotNull(guest.sessionToken())
        assertEquals(guestId, GameApi(url).resume(kept).id)
        guest.revoke(kept)
        assertEquals(401, assertFailsWith<ApiFailure> { GameApi(url).resume(kept) }.status)
    }

    /** The tree (0.12.0, 0.16.0): a neighbour taken, what is taken refused again, a refund paid for with an Orb of Regret, a reset back to the class node. */
    private suspend fun treeIsTheServers(api: GameApi, id: String, tree: List<com.sperance.exileforge.core.model.skilltree.SkillTreeNode>,
                                         start: com.sperance.exileforge.core.model.skilltree.SkillTreeNode,
                                         started: com.sperance.exileforge.core.model.skilltree.SkillTreeState,
                                         orbs: List<com.sperance.exileforge.core.model.currency.CurrencyItem>) {
        val neighbour = tree.firstOrNull { it.code in start.connections } ?: fail("${start.code} has no neighbour")
        val grown = api.tree.allocate(id, neighbour.code)
        assertEquals(setOf(start.code, neighbour.code), grown.takenCodes)
        assertEquals(started.available - neighbour.cost, grown.available)
        // A node's bonuses are a snapshot taken when it was allocated, and they are never rolled.
        assertTrue(grown.nodes.flatMap { node -> node.params }.none { param -> param.rolled })
        // What the whole tree gives is the server's arithmetic, sent with the state: the client
        // adds nothing up, so a node with bonuses has to show up here.
        if (grown.nodes.any { node -> node.params.isNotEmpty() })
            assertTrue(grown.totals.isNotEmpty(), "the tree gives nothing after a node with bonuses: $grown")
        // Taking what is already taken is refused, and the start node is the tree's root.
        assertFailsWith<ApiFailure> { api.tree.allocate(id, start.code) }
        assertFailsWith<ApiFailure> { api.tree.refund(id, start.code) }
        // Since 0.16.0 giving a node back costs an Orb of Regret, so an empty bag is a refusal
        // and not a free undo. The campaign above may have dropped one, so the bag is emptied first.
        val regret = orbs.firstOrNull { it.orb == CurrencyOrb.ORB_OF_REGRET } ?: fail("no Orb of Regret among ${orbs.map { it.code }}")
        api.hero.bag(id).firstOrNull { it.itemId == regret.id }?.let { api.hero.adjustItems(id, listOf(ItemStack(regret.id, -it.amount))) }
        assertFailsWith<ApiFailure> { api.tree.refund(id, neighbour.code) }
        assertEquals("system.success", api.hero.adjustItems(id, listOf(ItemStack(regret.id, 1))))
        assertEquals(setOf(start.code), api.tree.refund(id, neighbour.code).takenCodes)
        // The orb is spent, not merely checked: a second refund would have to be paid for again.
        assertTrue(api.hero.bag(id).none { it.itemId == regret.id }, "the orb of regret was not spent")
        // A reset is a respec: it leaves the character standing on its class node, not on nothing.
        // Nothing is left to give back here, so it costs no orb.
        val respec = api.tree.reset(id)
        assertEquals(setOf(start.code), respec.takenCodes, "a reset emptied the tree: $respec")
        assertEquals(started.available, respec.available)
    }

    /**
     * Fewer requests (server 0.48.0): the world file holds what the tables did, the hero comes back
     * whole on a read and as a 304 when nothing moved, and a command brings back only what it changed.
     */
    private suspend fun snapshotsAreTheServers(api: GameApi, id: String) {
        val manifest = api.manifest(fresh = true)
        manifest.requireWorkbench()
        val world = com.sperance.exileforge.core.model.sync.WorldTables.parse(manifest.world.hash, api.files.worldDocument(manifest.world.file))
        assertEquals(api.catalog.equipment().size, world.equipment.size)
        assertEquals(api.world.modifiers().size, world.modifiers.size)
        // Since server 0.56.0 the pools ride along: the same list the catalogue "Pools" serves.
        assertEquals(api.world.pools().map { it.kind to it.code }.toSet(), world.pools.map { it.kind to it.code }.toSet())
        assertTrue(world.orbs.isNotEmpty() && world.classes.isNotEmpty() && world.tree.isNotEmpty() && world.stats.stats.isNotEmpty())
        val first = assertNotNull(api.hero.view(id, HeroParts(id)))
        val held = HeroParts(id).merge(first)
        assertTrue(held.complete, "parts: ${first.parts.keys}")
        assertEquals(api.hero.inventory(id).map { it.id }.toSet(), held.inventory.map { it.id }.toSet())
        assertNull(api.hero.view(id, held), "an unchanged hero was sent again")
        var delivered: HeroSnapshot? = null
        api.heroSync({ if (it == id) held.header() else null }, { _, snapshot -> delivered = snapshot })
        try {
            api.hero.addExperience(id, 1.0)
            val moved = assertNotNull(delivered, "the command came back without the hero")
            assertTrue(HeroParts.CHARACTER in moved.parts && HeroParts.INVENTORY !in moved.parts, "parts: ${moved.parts.keys}")
        } finally { api.heroSync({ null }, { _, _ -> }) }
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
        val manifest = api.files.localeManifest()
        assertTrue(manifest.languages.map { it.code }.containsAll(listOf("ru", "en")), "languages: ${manifest.languages}")
        for (language in manifest.languages) {
            val bundle = api.files.localeBundle(language)
            assertEquals(language.code, bundle.language)
            assertTrue(bundle.size > 100, "${language.code} holds only ${bundle.size} strings")
        }
        serverLocale = api.files.localeBundle(assertNotNull(manifest.language("ru")))
        assertTrue(serverLocale.contains("system.success"), "the server's own success key is missing")

        // The icon set, which only the live server can prove: the manifest's fingerprint is
        // computed from the file, so a set edited without touching the manifest is still noticed.
        val iconManifest = api.files.iconManifest()
        assertTrue(iconManifest.hash.isNotBlank(), "the server served no icon fingerprint")
        val iconBundle = IconBundle.parse(iconManifest.hash, api.files.iconDocument(iconManifest.file))
        assertEquals(iconManifest.icons, iconBundle.size, "the manifest and the set disagree on how many codes there are")
        assertEquals(iconManifest.sprites, iconBundle.spriteCount, "the manifest and the set disagree on how many drawings there are")
        // Every code in the table has to name a drawing that exists, or `parse` would have dropped
        // it — which is the quiet failure this whole check is here to catch.
        assertTrue(iconBundle.size > 100, "only ${iconBundle.size} codes carry an icon")
        serverIcons = iconBundle
        portraitsAreDrawable(api)

        // The seeded modifier catalogue is what every rolled value on an instance points back at.
        val definitions = api.world.modifiers()
        assertTrue(definitions.isNotEmpty())
        assertTrue(definitions.any { it.composite })
        val described = definitions.firstOrNull { it.effects.isNotEmpty() } ?: fail("no definition carries an effect: $definitions")
        // Since server 0.56.0 the tiers sit inside the definition: a `[min, max]` per effect, best first.
        val tiers = described.tiers
        assertTrue(tiers.isNotEmpty() && tiers.all { it.values.size == described.effects.size && it.values.all { range -> range.size == 2 } },
            "tiers of ${described.code}: $tiers")
        assertTrue(tiers.zipWithNext().all { (better, worse) -> better.level >= worse.level }, "tiers of ${described.code} are not best first: $tiers")

        poolsAreTheServers(api, definitions)
        statsAreNamed(api, definitions)
        // The key this client builds has to be the key the server wrote, or the template is the code.
        assertTrue(serverLocale.contains(LocaleKey.modifierName(described.code)), "no text for ${described.code}")
        assertNotEquals(described.code, described.template)

        // A character is nothing without a class: it carries the whole stat base and the tree's root.
        val classes = api.world.classes()
        val chosenClass = classes.firstOrNull() ?: fail("the server seeded no character classes")
        assertTrue(chosenClass.baseStats.isNotEmpty(), "${chosenClass.code} has no base: $chosenClass")
        assertTrue(chosenClass.startNodeCode.isNotBlank(), "${chosenClass.code} names no start node")
        assertTrue(chosenClass.params.none { it.rolled }, "a class conversion must not be rolled: ${chosenClass.params}")
        assertNotEquals(chosenClass.code, chosenClass.title, "the class has no name in the dictionary")
        val levels = api.world.levels()
        assertTrue(levels.isNotEmpty() && levels.first().level == 1, "progression table: $levels")

        val tree = api.world.tree()
        assertTrue(tree.isNotEmpty(), "the server seeded no skill tree")
        val start = tree.firstOrNull { it.code == chosenClass.startNodeCode } ?: fail("${chosenClass.startNodeCode} is not in the tree")
        assertEquals(SkillNodeType.START, start.type)
        assertNotEquals(start.code, start.title, "${start.code} has no name in the dictionary")

        val name = "EF-integration-${java.util.UUID.randomUUID()}"
        val character = api.catalog.create(Catalog.CHARACTERS, buildJsonObject {
            put("userId", admin.id); put("name", name); put("description", "Integration fixture"); put("classId", chosenClass.id)
        })
        val id = character.entityId
        try {
            assertEquals(name, api.hero.character(id).name)
            assertEquals(1, api.catalog.search(Catalog.CHARACTERS, 0, CatalogFilter(query = name)).items.size)
            // A new hero owns one thing (server 0.69.0): the small life flask, already on the belt's first place.
            val kit = api.hero.inventory(id)
            assertEquals(listOf("FLASK"), kit.map { it.equippedSlot }, "a new hero's things: $kit")
            val starter = kit.single()

            // The character menu reads one account's characters, not the whole collection.
            val mine = api.hero.charactersOf(admin.id)
            assertTrue(mine.any { it.id == id }, "the account's own character is missing: ${mine.map { it.name }}")
            assertTrue(mine.all { it.userId == admin.id }, "someone else's character came back: $mine")
            assertTrue(mine.size <= 3, "an account cannot hold more than three characters: ${mine.size}")
            // An unknown account is refused rather than answered with an empty list.
            assertFailsWith<ApiFailure> { api.hero.charactersOf("0123456789abcdef01234567") }

            // Registration by device, which is the whole sign-up: the first attempt is refused with
            // US_015 and turns into the POST that creates the account.
            val guest = GameApi(url)
            val device = "ef-integration-${java.util.UUID.randomUUID()}"
            val registered = guest.loginByDevice(device)
            assertEquals("USER", registered.role, "a device account must not be privileged")
            assertEquals(0, registered.countCharacters, "a fresh account already has characters")
            // The second sign-in finds the same account rather than making a second one.
            assertEquals(registered.id, GameApi(url).loginByDevice(device).id)
            assertTrue(guest.hero.charactersOf(registered.id).isEmpty(), "a new account starts with no characters")
            accessIsTheServers(url, guest, registered.id, admin.id, id)
            // The account is left behind: a game client has no route that deletes one, and the
            // client-server job seeds a fresh database for every run anyway.

            // A random grant of a chosen rarity and category: the client names a base, the server rolls it.
            val template = api.catalog.randomTemplate("RARE", "HELMET")
            assertEquals("HELMET", template.text("slot"))
            assertEquals("RARE", template.text("rarity"))
            // A template carries a code and no text: its name has to come back from the dictionary.
            assertTrue("name" !in template, "the server still writes text into a template: $template")
            assertNotEquals(template.text("code"), equipmentTitle(template), "no name for ${template.text("code")}")
            // The same code that finds the name has to find the drawing.
            assertNotNull(documentIcon(template), "no icon for ${template.text("code")}")
            val instance = api.hero.grant(id, template.entityId)
            assertEquals(template.entityId, instance.equipmentId)
            assertTrue(instance.params.isNotEmpty(), "the server rolled no modifiers")
            assertTrue(instance.params.all { it.modifierCode in definitions.map { definition -> definition.code } })
            // One value per effect of the description: a composite modifier rolls all of them at once.
            instance.params.forEach { rolled ->
                val definition = definitions.firstOrNull { it.code == rolled.modifierCode } ?: fail("rolled $rolled names no known definition")
                assertEquals(definition.effects.size, rolled.values.size, "${definition.code} rolled ${rolled.values}")
            }
            assertFalse(instance.equipped)
            snapshotsAreTheServers(api, id)

            // Points come from levels, so the character is levelled before wearing or spending anything.
            assertTrue(api.hero.addExperience(id, levels.last().experience).level > 1)
            val base = api.hero.stats(id)
            assertTrue(base.stats.isNotEmpty(), "the server returned no stats")
            assertEquals(character.text("_id"), base.characterId)

            // Requirements are checked twice by the server, and the two checks are not the same rule:
            // an item out of reach cannot be put on at all, while one already worn merely stops
            // counting. The wearable base is chosen here from what the sheet already says.
            val helmets = api.catalog.search(Catalog.EQUIPMENT, 0, CatalogFilter(slot = "HELMET")).items
            assertTrue(helmets.isNotEmpty(), "no helmet templates to wear")
            val wearable = helmets.minByOrNull { demand(it, base) } ?: fail("no helmet templates")
            val wornInstance = api.hero.grant(id, wearable.entityId)
            val worn = api.hero.equip(id, wornInstance.id)
            assertEquals("HELMET", worn.equippedSlot)
            assertEquals(setOf(starter.id, wornInstance.id), api.hero.inventory(id).filter { it.equipped }.map { it.id }.toSet())
            // Wearing the item is what changes the character sheet; the client recomputes nothing.
            val sheet = api.hero.stats(id)
            assertEquals(sheet, api.hero.stats(id))
            assertEquals(listOf(wornInstance.id), sheet.active, "the worn item was not counted: $sheet")
            assertTrue(sheet.inactive.isEmpty(), "nothing should be refused here: ${sheet.inactive}")

            // The other half of the rule: a base the sheet cannot reach is refused outright.
            helmets.maxByOrNull { demand(it, base) }?.takeIf { demand(it, base) > 0 }?.let { heavy ->
                val granted = api.hero.grant(id, heavy.entityId)
                val refused = assertFailsWith<ApiFailure> { api.hero.equip(id, granted.id) }
                assertTrue(refused.message.orEmpty().contains(heavy.text("name")), "refusal names no item: ${refused.message}")
            }
            assertFalse(api.hero.unequip(id, wornInstance.id).equipped)

            // Currency is the same collection as every other item, told apart by its category alone.
            val orbs = api.world.orbs()
            assertTrue(orbs.isNotEmpty(), "the server seeded no currency")
            assertTrue(orbs.all { it.orb != null }, "unknown orbs: ${orbs.filter { it.orb == null }.map { it.subCategory }}")
            val chaos = orbs.firstOrNull { it.orb == CurrencyOrb.CHAOS_ORB } ?: fail("no Chaos Orb among ${orbs.map { it.code }}")
            assertNotEquals(chaos.code, chaos.title(), "the orb has no name in the dictionary")
            assertNotNull(icon(IconKey.item(chaos.code)), "no icon for ${chaos.code}")

            // The orb is spent from the bag, so it is handed over first; the rerolls are the server's.
            assertEquals("system.success", api.hero.adjustItems(id, listOf(ItemStack(chaos.id, 1))))
            val rerolled = api.hero.applyOrb(id, instance.id, chaos.id)
            // The server sends a key and arguments that are keys themselves: what has to come out
            // is a sentence naming the item, not the key it was built from.
            assertTrue(serverLocale.contains(rerolled.messageKey), "no text for ${rerolled.messageKey}")
            assertNotEquals(rerolled.messageKey, rerolled.message)
            assertFalse(rerolled.message.contains("{0}"), "an argument was never filled: ${rerolled.message}")
            assertEquals(instance.id, rerolled.item.id)
            assertEquals("RARE", rerolled.item.rarity, "a Chaos Orb must leave the rarity alone: ${rerolled.message}")
            assertNull(rerolled.created, "only a mirror creates a second item")
            assertTrue(api.hero.bag(id).none { it.itemId == chaos.id }, "the orb was not spent")
            // A refusal costs nothing: with no orb left the server rejects the call and keeps the item.
            assertFailsWith<ApiFailure> { api.hero.applyOrb(id, instance.id, chaos.id) }
            assertEquals(rerolled.item.params, api.hero.inventory(id).single { it.id == instance.id }.params)
            craftingIsTheServers(api, id, template.entityId, definitions)
            handsAreTheServers(api, id)
            campaignIsTheServers(api, id, definitions)
            skillsAreTheServers(api, id)
            merchantIsTheServers(api, id, definitions)

            val items = api.catalog.referencePage(com.sperance.exileforge.core.model.EntitySource.ITEM, 0)
            val item = items.items.firstOrNull() ?: fail("the items collection is empty: $items")
            assertEquals("system.success", api.hero.adjustItems(id, listOf(ItemStack(item.entityId, 5))))
            val bag = api.hero.bag(id)
            assertEquals(5L, (bag.firstOrNull { it.itemId == item.entityId } ?: fail("${item.entityId} is not in the bag: $bag")).amount)
            assertEquals("system.success", api.hero.adjustItems(id, listOf(ItemStack(item.entityId, -5))))
            assertTrue(api.hero.bag(id).none { it.itemId == item.entityId })

            // A template is editable; an instance's rolls are not reachable from the catalogue at all.
            // Its words are not editable either: they live in the locale files, not in the document.
            val edited = api.catalog.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("requiredLevel", 7) })
            assertEquals("7", edited.text("requiredLevel"))
            assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("description", "by hand") }) }
            // `image` was removed in 0.15.1; it is refused before the request is built, like any
            // field the server does not have. An older client's write is simply ignored instead.
            assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("image", "http://old/url.png") }) }
            // An equipment template is a StockEntity: the server keeps no version on it.
            assertFalse("version" in edited, "equipment gained a version: $edited")
            assertFailsWith<IllegalArgumentException> { api.catalog.update(Catalog.EQUIPMENT, template.entityId, buildJsonObject { put("params", JsonArray(emptyList())) }) }

            // Since 0.12.0 the class's start node comes with the character, free of charge.
            val started = api.tree.state(id)
            assertEquals(setOf(start.code), started.takenCodes, "a new character is not on its class node: $started")
            assertTrue(started.total > 0, "a levelled character has no skill points: $started")
            assertEquals(started.nodes.sumOf { node -> node.cost }, started.spent)

            treeIsTheServers(api, id, tree, start, started, orbs)

            // The auction needs two characters: the server refuses to let one buy its own lot.
            val buyerName = "EF-buyer-${java.util.UUID.randomUUID()}"
            val buyer = api.catalog.create(Catalog.CHARACTERS, buildJsonObject {
                put("userId", admin.id); put("name", buyerName); put("description", "Auction buyer"); put("classId", chosenClass.id)
            }).entityId
            try {
                // Both ends have to clear the level the auction opens at; the server names it itself.
                api.hero.addExperience(buyer, levels.last().experience)

                val listed = api.auction.sellEquipment(id, wornInstance.id, chaos.id, 3)
                assertEquals(AuctionLotStatus.ACTIVE, listed.status)
                assertEquals(AuctionLotKind.EQUIPMENT, listed.kind)
                // The lot stores a code, and the name comes back through the same dictionary.
                assertEquals(equipmentTitle(wearable), listed.title)
                assertEquals(wearable.text("code"), listed.itemCode)
                // While it is listed the goods live in the lot, not with the seller.
                assertTrue(api.hero.inventory(id).none { it.id == wornInstance.id }, "the listed item stayed in the inventory")
                assertEquals(listOf(listed.id), api.auction.myLots(id).filter { it.onSale }.map { it.id })

                // The showcase is the server's own search, and it hides the seller's own lots.
                val own = api.auction.search(id, AuctionFilter(excludeSellerId = id), 0)
                assertTrue(own.items.none { it.id == listed.id }, "the seller sees their own lot: $own")
                // A lot has no name to match, so a text search is resolved to codes on the server —
                // which is why the language the player typed in travels with it.
                val shown = api.auction.search(buyer, AuctionFilter(title = listed.title, lang = serverLocale.language), 0)
                assertTrue(shown.items.any { it.id == listed.id }, "the lot is not on the showcase: $shown")
                // The English trade name finds it too, whatever the language (server 0.45.0).
                val trade = serverLocale[LocaleKey.equipmentTrade(wearable.text("code"))]
                assertNotEquals(listed.title, trade, "the Russian name is still the English one")
                val byTrade = api.auction.search(buyer, AuctionFilter(title = trade, lang = serverLocale.language), 0)
                assertTrue(byTrade.items.any { it.id == listed.id }, "the English name does not find the lot: $byTrade")
                assertFailsWith<ApiFailure> { api.auction.buy(id, listed.id) }

                // Paying: the orbs go to the seller, the goods to the buyer, in one transaction.
                assertEquals("system.success", api.hero.adjustItems(buyer, listOf(ItemStack(chaos.id, 3))))
                val sold = api.auction.buy(buyer, listed.id)
                assertEquals(AuctionLotStatus.SOLD, sold.status)
                assertEquals(buyer, sold.buyerId)
                assertTrue(api.hero.inventory(buyer).any { it.equipmentId == wearable.entityId }, "the buyer never got the item")
                assertTrue(api.hero.bag(buyer).none { it.itemId == chaos.id }, "the buyer kept the orbs")
                assertEquals(3L, (api.hero.bag(id).firstOrNull { it.itemId == chaos.id } ?: fail("the seller was not paid")).amount)
                // A closed lot is history: it never returns to the showcase and cannot be bought twice.
                assertFailsWith<ApiFailure> { api.auction.buy(buyer, listed.id) }

                // Withdrawing returns the goods; a stack lot travels the same way an instance does.
                val stack = api.auction.sellItem(id, chaos.id, 2, chaos.id, 1)
                assertEquals(2L, stack.amount)
                assertEquals(1L, (api.hero.bag(id).firstOrNull { it.itemId == chaos.id } ?: fail("the stack was not debited")).amount)
                assertFalse(api.auction.cancel(id, stack.id).onSale)
                assertEquals(3L, (api.hero.bag(id).firstOrNull { it.itemId == chaos.id } ?: fail("the stack never came back")).amount)
            } finally {
                api.catalog.delete(Catalog.CHARACTERS, buyer)
            }

            // A promo code pays out for real, which is the whole of what 0.19.0 changed: it used
            // to mark itself used and grant nothing. Experience, gold and a stack in one code,
            // because the point is that the reward lands as one transaction rather than in parts.
            val before = api.hero.character(id)
            val code = "EF_TEST_" + System.nanoTime()
            val promo = api.promo.create(RedemptionCode(code = code, description = "integration",
                treasure = listOf(
                    RedemptionReward(RedemptionKind.EXPERIENCE, amount = 250.0),
                    RedemptionReward(RedemptionKind.GOLD, amount = 70.0),
                    RedemptionReward(RedemptionKind.ITEM, chaos.id, 2.0))))
            try {
                assertTrue(api.promo.codes().any { it.code == code }, "the code was not stored")
                api.hero.redeem(id, code)
                val after = api.hero.character(id)
                assertEquals(before.experience + 250.0, after.experience, "experience was not granted")
                assertEquals(before.money + 70, after.money, "gold was not granted")
                assertEquals(5L, (api.hero.bag(id).firstOrNull { it.itemId == chaos.id } ?: fail("the orbs never arrived")).amount)
                // One code, one account: the second attempt is a refusal, not a second reward.
                assertFailsWith<ApiFailure> { api.hero.redeem(id, code) }
            } finally {
                api.promo.delete(promo.id)
            }

            val player = GameApi(url)
            player.login(requireNotNull(System.getenv("EF_PLAYER_LOGIN")), requireNotNull(System.getenv("EF_PLAYER_PASSWORD")))
            assertEquals("USER", assertNotNull(player.currentUser()).role)
        } finally {
            api.catalog.delete(Catalog.CHARACTERS, id)
            assertNull(api.catalog.get(Catalog.CHARACTERS, id))
        }
    }
}
