package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.creationFields
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.contract.validateModifierPool
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleLanguage
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.*
import com.sperance.exileforge.core.model.modifier.BenchRecipe
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierTier
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * One character: what it is, what it carries and wears, and every command that changes that.
 *
 * Each command names the character and the thing; every rule — requirements, sockets, prices,
 * what an orb does — is the server's, and a refusal comes back as it was said.
 */
class HeroClient internal constructor(private val http: Transport, private val catalog: CatalogClient) {
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
    suspend fun stats(characterId: String): CharacterSheet {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", "api/v1/character/inventory/stats", mapOf("characterId" to characterId), authenticated = true))
    }

    /** Grants experience; the server decides whether that crosses a level threshold. */
    suspend fun addExperience(characterId: String, amount: Double): CharacterSummary {
        requireId(characterId)
        require(amount > 0 && amount.isFinite()) { ui("api.xp_positive") }
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/character/inventory/experience",
            mapOf("characterId" to characterId, "amount" to amount.toString()), authenticated = true))
    }
    suspend fun bag(characterId: String): List<CharacterItem> {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(CharacterItem.serializer()), http.request("GET", "api/v1/character/inventory/items", mapOf("characterId" to characterId), authenticated = true))
    }
    /** Adds or removes stacking items; a negative amount removes them. Answers with a status word. */
    suspend fun adjustItems(characterId: String, items: List<ItemStack>): String {
        requireId(characterId)
        require(items.isNotEmpty()) { ui("api.empty_items") }
        val body = JsonArray(items.map { buildJsonObject { put("itemId", it.itemId); put("amount", it.amount) } })
        return http.request("POST", "api/v1/character/inventory/addItem", mapOf("characterId" to characterId), body, authenticated = true).jsonPrimitive.content
    }

    /**
     * Creates one instance of a template in the character's inventory.
     *
     * The rolls belong to the server: it picks prefixes and suffixes in the count the rarity allows
     * and a tier inside each. The client only names the template.
     */
    suspend fun grant(characterId: String, equipmentId: String): EquipmentInstance {
        requireId(characterId); requireId(equipmentId)
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/character/inventory/itemToInventory",
            mapOf("characterId" to characterId, "equipmentId" to equipmentId), authenticated = true))
    }

    /**
     * Puts an item on. Everything it displaces — the other hand, the ring in its place — is taken
     * off by the server, whose rules those are. [slot] names which of the two rings (`RING`,
     * `RING_2`) to take; without it the server takes a free one.
     */
    suspend fun equip(characterId: String, inventoryId: String, slot: String? = null): EquipmentInstance {
        requireId(characterId); requireId(inventoryId)
        val query = mapOf("characterId" to characterId, "inventoryId" to inventoryId) + listOfNotNull(slot?.let { "slot" to it })
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/characterequipment/equip", query, authenticated = true))
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
        requireId(characterId); requireId(inventoryId)
        require(nodeCode.isNotBlank()) { ui("api.choose_socket") }
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/characterequipment/socket",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId, "nodeCode" to nodeCode), authenticated = true))
    }

    /**
     * Sells an item to a merchant for gold.
     *
     * The price is the server's alone — template, rarity and how many affixes rolled — and the
     * instance is gone when this returns. A worn or socketed item is refused, as the auction
     * refuses one.
     */
    suspend fun sellForGold(characterId: String, inventoryId: String): SellOutcome {
        requireId(characterId); requireId(inventoryId)
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/characterequipment/sell",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId), authenticated = true))
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
        requireId(characterId); requireId(inventoryId); requireId(orbItemId)
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/characterequipment/applyOrb",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId, "orbItemId" to orbItemId), authenticated = true))
    }

    /** The crafting bench: every crafted modifier in every tier, with its price in orbs. Fixed per server. */
    suspend fun bench(): List<BenchRecipe> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(BenchRecipe.serializer()),
            http.request("GET", "api/v1/characterequipment/bench", authenticated = true))

    /**
     * Places one bench modifier on one item, paid in orbs.
     *
     * The server checks everything — one crafted modifier per item, a free place of the right kind,
     * no twin of the same group, the slot, the price — and debits the orbs in the same transaction,
     * so a refusal costs nothing. The answer is shaped like an orb's: the item and a sentence.
     */
    suspend fun craft(characterId: String, inventoryId: String, recipe: String): OrbOutcome {
        requireId(characterId); requireId(inventoryId)
        require(recipe.isNotBlank()) { ui("api.choose_recipe") }
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/characterequipment/craft",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId, "recipe" to recipe), authenticated = true))
    }

    /** Takes the bench modifier back off, for the server's price (an Orb of Scouring). */
    suspend fun uncraft(characterId: String, inventoryId: String): OrbOutcome {
        requireId(characterId); requireId(inventoryId)
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/characterequipment/uncraft",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId), authenticated = true))
    }

    suspend fun useRecipe(characterId: String, recipeId: String, command: UseRecipeCommand): JsonElement {
        requireId(characterId); requireId(recipeId)
        command.ingridientsId.forEach(::requireId)
        return http.request("POST", "api/v1/recipe/useRecipe", mapOf("characterId" to characterId, "recipeId" to recipeId), WireJson.encodeToJsonElement(command), authenticated = true)
    }
    suspend fun redeem(characterId: String, code: String): JsonElement {
        requireId(characterId)
        require(code.isNotBlank()) { ui("api.enter_promo") }
        return http.request("POST", "api/v1/redemptioncodes/useRedeptionCode", mapOf("characterId" to characterId, "redemptionCode" to code.trim()), authenticated = true)
    }

    private suspend fun wear(operation: String, characterId: String, inventoryId: String): EquipmentInstance {
        requireId(characterId); requireId(inventoryId)
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/characterequipment/$operation",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId), authenticated = true))
    }
    private suspend fun instances(path: String, characterId: String): List<EquipmentInstance> {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(EquipmentInstance.serializer()),
            http.request("GET", path, mapOf("characterId" to characterId), authenticated = true))
    }
}
