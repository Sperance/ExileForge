package com.sperance.exileforge.core.model.hero

import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.skills.HeroSkills
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*

/**
 * Character document of the `character` collection, read-only for everything the server owns.
 *
 * Since 0.10.0 the base stats are not here: the character references a [CharacterClass] by
 * [classId] and the server takes the base from it at the character's level.
 */
@Serializable data class CharacterSummary(
    @SerialName("_id") val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val version: Long = 0,
    val level: Int = 1,
    val experience: Double = 0.0,
    val money: Long = 0,
    val classId: String = "",
    /** The bag as the server keeps it since 0.49.0: item id to amount. */
    val bag: Map<String, Long> = emptyMap(),
    val recipeAccess: List<String> = emptyList(),
    /** The class skills (server 0.69.0): learned levels, the slots and the belt's conditions. */
    val skills: HeroSkills = HeroSkills(),
)

/**
 * An equipped item whose requirements the character does not meet, with the server's reasons.
 *
 * [code] names the template rather than the item: the text has lived in the locale bundle since
 * 0.14.0, and the panel already has the instance in hand, so this is only the server's verdict.
 */
@Serializable data class InactiveEquipment(
    val inventoryId: String = "",
    val code: String = "",
    val reasons: List<String> = emptyList(),
)

/**
 * The character sheet, as `GET /api/v1/character/inventory/stats` now answers it.
 *
 * It is no longer a flat map: the server reports which equipped items it actually counted and
 * which it refused, because an item whose requirements stopped being met stays in its slot and
 * simply stops working. Both lists are the server's verdict — the client never re-checks a
 * requirement, it prints the reasons that came back.
 */
@Serializable data class CharacterSheet(
    val characterId: String = "",
    val level: Int = 1,
    val stats: Map<String, Double> = emptyMap(),
    val active: List<String> = emptyList(),
    val inactive: List<InactiveEquipment> = emptyList(),
    /**
     * Templates this character cannot wear right now, with the server's reasons.
     *
     * The verdict is on the *template*, because that is where a requirement lives, so one answer
     * marks a stash line and a stranger's lot alike. The client never re-checks a requirement —
     * it asks whether the template is in here.
     */
    val unwearable: List<UnwearableEquipment> = emptyList(),
    /**
     * What the sheet was added up from (2.78.0), for a fight to lay a buff, a curse or a flask over it
     * the way the server would; the client's own, never on the wire.
     */
    @Transient val model: SheetModel? = null,
) {
    /** Reasons a template is out of reach, keyed by its id; absent means it can be worn. */
    val unwearableBy: Map<String, List<String>> get() = unwearable.associate { it.equipmentId to it.reasons }
}

/** An equipment template the character cannot currently meet the requirements of. */
@Serializable data class UnwearableEquipment(
    val equipmentId: String = "",
    val code: String = "",
    val reasons: List<String> = emptyList(),
)

/**
 * What a merchant paid for an item.
 *
 * The instance is gone by the time this arrives, so it carries what the player needs to see:
 * what was sold, what it fetched, and what the purse holds now. The price is the server's —
 * the client never works one out.
 */
@Serializable data class SellOutcome(
    val inventoryId: String = "",
    val code: String = "",
    val gold: Long = 0,
    val money: Long = 0,
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
    /** A copy a Mirror of Kalandra made. Refuses every orb, as a corrupted item does. */
    val mirrored: Boolean = false,
    val equippedSlot: String? = null,
    /**
     * The code of the tree socket this jewel sits in; null for everything else.
     *
     * The slot alone cannot say where a jewel is worn — the tree has many sockets and each can
     * be filled — so the slot answers what it is and this answers where.
     */
    val socketCode: String? = null,
    /** Since 0.23.0: `SHAPER` or `ELDER` when an influence orb touched this copy; null otherwise. */
    val influence: String? = null,
    val version: Long = 0,
    /** A flask's quality (server 0.69.0), in percent: each one a percent more effect or recovery. */
    val quality: Int = 0,
) {
    val equipped: Boolean get() = equippedSlot != null
    /** A jewel is "worn" in a socket, and only counts while that socket is taken. */
    val socketed: Boolean get() = !socketCode.isNullOrBlank()
    fun document(): JsonObject = WireJson.encodeToJsonElement(this).jsonObject
}

/**
 * What `POST /api/v1/characterequipment/applyOrb` answered.
 *
 * Since 0.14.0 the server sends no sentence, only the key of one and its arguments: the client
 * assembles the phrase so the word order can differ per language. The arguments are themselves
 * locale keys — an orb names the item by `equipment.<code>.name`, not by text.
 *
 * What happened is still entirely the server's account, because only it knows what it rolled.
 * [created] is the copy a Mirror of Kalandra made; every other orb leaves it null.
 */
@Serializable data class OrbOutcome(
    val messageKey: String = "",
    val messageArgs: List<String> = emptyList(),
    val item: EquipmentInstance,
    val created: EquipmentInstance? = null,
) {
    val message: String get() = loc(messageKey, messageArgs)
}

/** A stacking item in the bag. The server stores it as the flat string "itemId:amount". */
@Serializable data class CharacterItem(val itemId: String, val amount: Long)


/** Everything one hero screen needs, assembled from the character routes the server offers. */
data class HeroView(
    val character: CharacterSummary,
    val inventory: List<EquipmentInstance> = emptyList(),
    val sheet: CharacterSheet = CharacterSheet(),
    val bag: List<CharacterItem> = emptyList(),
    val tree: SkillTreeState = SkillTreeState(),
) {
    /**
     * What is worn on the body, one item per slot.
     *
     * Jewels are deliberately out: they all share the slot JEWEL and would collapse into one
     * entry here, and they are not worn on the body at all — see [jewels].
     */
    val equipped: Map<String, EquipmentInstance> get() =
        inventory.filter { it.equipped && !it.socketed }.associateBy { it.equippedSlot!! }
    /** Jewels sitting in tree sockets, keyed by the code of the socket each one fills. */
    val jewels: Map<String, EquipmentInstance> get() =
        inventory.filter { it.socketed }.associateBy { it.socketCode!! }
    val stats: Map<String, Double> get() = sheet.stats
    /** Reasons an equipped item is not counted, keyed by instance id; empty means it works. */
    val inactive: Map<String, List<String>> get() = sheet.inactive.associate { it.inventoryId to it.reasons }
}
