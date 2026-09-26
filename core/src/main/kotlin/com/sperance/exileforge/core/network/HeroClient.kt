package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.modifier.BenchRecipe
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.hero.*
import com.sperance.exileforge.core.model.skills.HeroSkills
import kotlinx.serialization.json.*

/** Route root of the class skills (server 0.69.0). */
private const val SKILLS = "api/v1/character/skills"

/**
 * One character: what it is, what it carries and wears, and every command that changes that.
 *
 * Each command names the character and the thing; every rule — requirements, sockets, prices,
 * what an orb does — is the server's, and a refusal comes back as it was said.
 */
class HeroClient internal constructor(private val http: Transport, private val catalog: CatalogClient) {
    /**
     * The hero in one read (server 0.48.0): only the parts whose fingerprints [parts] does not hold,
     * or `null` when the server answers 304 because nothing moved since [parts] was taken.
     */
    suspend fun view(characterId: String, parts: HeroParts): HeroSnapshot? {
        val headers = buildMap {
            put(HeroParts.HEADER, parts.header())
            if (parts.complete && parts.version.isNotBlank()) put("If-None-Match", "\"${parts.version}\"")
        }
        val answer = http.request("GET", "api/v1/character/view", heroQuery(characterId), authenticated = true, headers = headers)
        return if (answer is JsonNull) null else WireJson.decodeFromJsonElement(HeroSnapshot.serializer(), answer)
    }

    suspend fun character(id: String): CharacterSummary {
        val document = catalog.get(Catalog.CHARACTERS, id) ?: error(ui("api.no_character"))
        return WireJson.decodeFromJsonElement(document)
    }

    /**
     * The characters one account owns — what the character menu offers.
     *
     * The server narrows this itself rather than the client reading the whole collection: a player
     * has at most a handful, and nobody else's characters need to leave the server to show them.
     */
    suspend fun charactersOf(userId: String): List<CharacterSummary> {
        requireId(userId)
        return http.request("GET", "api/v1/character/byUser", mapOf("userId" to userId), authenticated = true)
            .jsonArray.map { WireJson.decodeFromJsonElement(it) }
    }

    suspend fun inventory(characterId: String): List<EquipmentInstance> =
        instances("api/v1/character/inventory/equipments", characterId)
    /**
     * The character sheet.
     *
     * Since 0.10.0 this is an object, not a flat map: alongside the numbers the server reports the
     * equipped items it counted and the ones it refused, with the requirement each of them misses.
     * An item whose requirements stopped being met keeps its slot and stops working — that verdict
     * is the server's and arrives here already made.
     */
    suspend fun stats(characterId: String): CharacterSheet =
        http.get("api/v1/character/inventory/stats", heroQuery(characterId))

    /** Grants experience; the server decides whether that crosses a level threshold. */
    suspend fun addExperience(characterId: String, amount: Double): CharacterSummary {
        require(amount > 0 && amount.isFinite()) { ui("api.xp_positive") }
        return http.post("api/v1/character/inventory/experience", heroQuery(characterId, "amount" to amount.toString()))
    }
    suspend fun bag(characterId: String): List<CharacterItem> =
        http.get<List<CharacterItem>>("api/v1/character/inventory/items", heroQuery(characterId))
    /** Adds or removes stacking items; a negative amount removes them. Answers with a status word. */
    suspend fun adjustItems(characterId: String, items: List<ItemStack>): String {
        require(items.isNotEmpty()) { ui("api.empty_items") }
        val body = JsonArray(items.map { buildJsonObject { put("itemId", it.itemId); put("amount", it.amount) } })
        return http.request("POST", "api/v1/character/inventory/addItem", heroQuery(characterId), body, authenticated = true).jsonPrimitive.content
    }

    /**
     * Creates one instance of a template in the character's inventory.
     *
     * The rolls belong to the server: it picks prefixes and suffixes in the count the rarity allows
     * and a tier inside each. The client only names the template.
     */
    suspend fun grant(characterId: String, equipmentId: String): EquipmentInstance {
        requireId(equipmentId)
        return http.post("api/v1/character/inventory/itemToInventory", heroQuery(characterId, "equipmentId" to equipmentId))
    }

    /**
     * Puts an item on. Everything it displaces — the other hand, the ring in its place — is taken
     * off by the server, whose rules those are. [slot] names which of the two rings (`RING`,
     * `RING_2`) to take; without it the server takes a free one.
     */
    suspend fun equip(characterId: String, inventoryId: String, slot: String? = null): EquipmentInstance {
        requireId(inventoryId)
        val query = heroQuery(characterId, "inventoryId" to inventoryId, "slot" to slot)
        return http.post("api/v1/characterequipment/equip", query)
    }
    suspend fun unequip(characterId: String, inventoryId: String): EquipmentInstance = wear("unequip", characterId, inventoryId)
    /**
     * Puts a jewel into a socket on the passive tree, and takes it back out.
     *
     * A jewel is an ordinary equipment instance, so everything else about it — rolls, rarity, orbs,
     * the auction — already worked. What differs is where it is worn: the tree has many sockets and
     * the node's code says which one, so this is not `equip` with a different slot.
     *
     * Every rule is the server's: that the node exists, that it is a socket, that the character has
     * taken it, and that it is free. The client names the pair and prints the refusal.
     */
    suspend fun socket(characterId: String, inventoryId: String, nodeCode: String): EquipmentInstance {
        requireId(inventoryId)
        require(nodeCode.isNotBlank()) { ui("api.choose_socket") }
        return http.post("api/v1/characterequipment/socket", heroQuery(characterId, "inventoryId" to inventoryId, "nodeCode" to nodeCode))
    }

    /**
     * Sells an item to a merchant for gold.
     *
     * The price is the server's alone — template, rarity and how many affixes rolled — and the
     * instance is gone when this returns. A worn or socketed item is refused, as the auction
     * refuses one.
     */
    suspend fun sellForGold(characterId: String, inventoryId: String): SellOutcome {
        requireId(inventoryId)
        return http.post("api/v1/characterequipment/sell", heroQuery(characterId, "inventoryId" to inventoryId))
    }

    suspend fun unsocket(characterId: String, inventoryId: String): EquipmentInstance =
        wear("unsocket", characterId, inventoryId)

    /**
     * Spends one orb of the character's on one item of their inventory.
     *
     * What the orb does is entirely the server's: it checks the rarity the orb demands, rolls new
     * affixes, tiers and values, and answers with the item as it now stands plus a sentence saying
     * what happened. The orb is debited in the same transaction, so a refusal costs nothing.
     */
    suspend fun applyOrb(characterId: String, inventoryId: String, orbItemId: String): OrbOutcome {
        requireId(inventoryId); requireId(orbItemId)
        return http.post("api/v1/characterequipment/applyOrb", heroQuery(characterId, "inventoryId" to inventoryId, "orbItemId" to orbItemId))
    }

    /**
     * Spends one essence of the character's on one item (server 0.69.0): a common item becomes rare with the
     * essence's line guaranteed, a rare one is rolled anew around it from its step up. The answer is an orb's.
     */
    suspend fun applyEssence(characterId: String, inventoryId: String, essenceItemId: String): OrbOutcome {
        requireId(inventoryId); requireId(essenceItemId)
        return http.post("api/v1/characterequipment/applyEssence", heroQuery(characterId, "inventoryId" to inventoryId, "essenceItemId" to essenceItemId))
    }

    /** Reads a skill book of the class (server 0.69.0): the first teaches the skill, each next one a level, by the book's requirements. */
    suspend fun learnSkill(characterId: String, skill: String): HeroSkills =
        http.post("$SKILLS/learn", heroQuery(characterId, "skill" to skill))

    /**
     * Puts a learned skill into slot [index] of [kind] — `ACTIVE` or `PASSIVE` — or empties it without [skill];
     * [condition] is when an active slot fires by itself. The slots open with the hero's level (server 0.69.0).
     */
    suspend fun slotSkill(characterId: String, kind: String, index: Int, skill: String?, condition: String? = null): HeroSkills =
        http.post("$SKILLS/slot", heroQuery(characterId, "kind" to kind, "index" to index.toString(), "skill" to skill, "condition" to condition))

    /** When the flask of belt place [index] is drunk by itself (server 0.69.0); none gives it back to its kind's own. */
    suspend fun flaskCondition(characterId: String, index: Int, condition: String?): HeroSkills =
        http.post("$SKILLS/flask", heroQuery(characterId, "index" to index.toString(), "condition" to condition))

    /** Trades [books] — skill codes, one book each — and the server's gold for one book of the class's [skill] (server 0.69.0). */
    suspend fun exchangeBooks(characterId: String, books: List<String>, skill: String): HeroSkills {
        require(books.isNotEmpty() && skill.isNotBlank()) { ui("skills.choose_books") }
        return http.post("$SKILLS/exchange", heroQuery(characterId, "books" to books.joinToString(","), "skill" to skill))
    }

    /** The crafting bench lines the character has found on maps (since 0.46.0); the rest stay hidden. */
    suspend fun bench(characterId: String): List<BenchRecipe> =
        http.get<List<BenchRecipe>>("api/v1/characterequipment/bench", heroQuery(characterId))

    /**
     * Places one bench modifier on one item, paid in orbs.
     *
     * The server checks everything — one crafted modifier per item, a free place of the right kind,
     * no twin of the same group, the slot, the price — and debits the orbs in the same transaction,
     * so a refusal costs nothing. The answer is shaped like an orb's: the item and a sentence.
     */
    suspend fun craft(characterId: String, inventoryId: String, recipe: String): OrbOutcome {
        requireId(inventoryId)
        require(recipe.isNotBlank()) { ui("api.choose_recipe") }
        return http.post("api/v1/characterequipment/craft", heroQuery(characterId, "inventoryId" to inventoryId, "recipe" to recipe))
    }

    /** Takes the bench modifier back off, for the server's price (an Orb of Scouring). */
    suspend fun uncraft(characterId: String, inventoryId: String): OrbOutcome {
        requireId(inventoryId)
        return http.post("api/v1/characterequipment/uncraft", heroQuery(characterId, "inventoryId" to inventoryId))
    }

    suspend fun redeem(characterId: String, code: String): JsonElement {
        require(code.isNotBlank()) { ui("api.enter_promo") }
        return http.request("POST", "api/v1/redemptioncodes/useRedeptionCode", heroQuery(characterId, "redemptionCode" to code.trim()), authenticated = true)
    }

    private suspend fun wear(operation: String, characterId: String, inventoryId: String): EquipmentInstance {
        requireId(inventoryId)
        return http.post("api/v1/characterequipment/$operation", heroQuery(characterId, "inventoryId" to inventoryId))
    }
    private suspend fun instances(path: String, characterId: String): List<EquipmentInstance> =
        http.get<List<EquipmentInstance>>(path, heroQuery(characterId))
}
