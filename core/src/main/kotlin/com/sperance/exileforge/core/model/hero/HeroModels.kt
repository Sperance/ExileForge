package com.sperance.exileforge.core.model.hero

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.modifier.Modifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** Character document of the `character` collection, read-only for everything the server owns. */
@Serializable data class CharacterSummary(
    @SerialName("_id") val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val version: Long = 0,
    val level: Int = 1,
    val experience: Double = 0.0,
    val money: Long = 0,
    val params: List<Modifier> = emptyList(),
    val items: List<String> = emptyList(),
    val recipeAccess: List<String> = emptyList(),
)

/**
 * One instance of an item in a character's inventory (collection `CharacterEquipment`).
 *
 * The template ([com.sperance.exileforge.core.model.Catalog.EQUIPMENT]) is shared by every copy;
 * [params], [rarity], [corrupted] and [equippedSlot] belong to this one. `equippedSlot == null`
 * means "in the stash".
 *
 * [rarity] starts as the template's and is then the orbs' to change: the template only decides what
 * the item drops as. [corrupted] is final — the server refuses every further orb on such an item.
 */
@Serializable data class EquipmentInstance(
    @SerialName("_id") val id: String,
    val characterId: String = "",
    val equipmentId: String = "",
    val params: List<Modifier> = emptyList(),
    val rarity: String = "COMMON",
    val corrupted: Boolean = false,
    val equippedSlot: String? = null,
    val version: Long = 0,
) {
    val equipped: Boolean get() = equippedSlot != null
    fun document(): JsonObject = WireJson.encodeToJsonElement(this).jsonObject
}

/**
 * What `POST /api/v1/characterequipment/applyOrb` answered.
 *
 * [message] is the server's own account of what the orb did — the client prints it rather than
 * inferring the outcome, because only the server knows what it rolled. [created] is the copy a
 * Mirror of Kalandra made; every other orb leaves it null.
 */
@Serializable data class OrbOutcome(
    val message: String = "",
    val item: EquipmentInstance,
    val created: EquipmentInstance? = null,
)

/** A stacking item in the bag. The server stores it as the flat string "itemId:amount". */
@Serializable data class CharacterItem(val itemId: String, val amount: Long)

@Serializable data class RecipeDocument(
    @SerialName("_id") val id: String,
    val name: String,
    val arrayIn: List<RecipeInput> = emptyList(),
    val arrayOut: List<RecipeOutput> = emptyList(),
    val requirement: List<JsonObject>? = null,
    val timeWork: Double = 1.0,
    val needOpenRecipe: Boolean = false,
    val globalUses: Long = 0,
)
@Serializable data class RecipeInput(val itemId: String? = null, val category: String? = null, val subCategory: String? = null, val amount: Double = 1.0)
@Serializable data class RecipeOutput(val itemId: String, val amount: Double = 1.0, val chance: Double = 1.0)

/** Everything one hero screen needs, assembled from the four character routes the server offers. */
data class HeroView(
    val character: CharacterSummary,
    val inventory: List<EquipmentInstance> = emptyList(),
    val stats: Map<String, Double> = emptyMap(),
    val bag: List<CharacterItem> = emptyList(),
) {
    val equipped: Map<String, EquipmentInstance> get() = inventory.filter { it.equipped }.associateBy { it.equippedSlot!! }
}
