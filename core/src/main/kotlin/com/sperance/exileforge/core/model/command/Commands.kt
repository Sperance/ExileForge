package com.sperance.exileforge.core.model.command

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.pick
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.i18n.uiLanguage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Server page bounds; the client never asks for a page the server would reject. */
const val INVENTORY_PAGE_SIZE = 50
const val MAX_INVENTORY_PAGE_SIZE = 200
const val MAX_ITEM_UNITS = 10_000L

@Serializable data class UpdateCommand(val expectedVersion: Long, val changes: JsonObject)
@Serializable data class DeleteCommand(val expectedVersion: Long)
@Serializable data class CreateCharacterCommand(val name: String, val description: String = "")
@Serializable data class EquipCommand(val expectedVersion: Long, val equipmentUuid: String, val slot: EquipmentSlot)
@Serializable data class UnequipCommand(val expectedVersion: Long, val slot: EquipmentSlot)
@Serializable data class GrantEquipmentCommand(val expectedVersion: Long, val equipmentId: String)
@Serializable data class ItemStack(val itemId: String, val amount: Long)
/** Units are documents, not a stack counter: one command creates or destroys at most [MAX_ITEM_UNITS] of them. */
@Serializable data class AdjustItemsCommand(val expectedVersion: Long, val items: List<ItemStack>) {
    init {
        require(items.isNotEmpty() && items.none { it.amount == 0L }) { tr("Укажите ненулевое количество", "Enter a non-zero amount") }
        require(items.sumOf { kotlin.math.abs(it.amount) } <= MAX_ITEM_UNITS) { tr("За одну команду — не больше $MAX_ITEM_UNITS единиц", "One command handles at most $MAX_ITEM_UNITS units") }
    }
}
@Serializable data class RedeemCommand(val expectedVersion: Long, val code: String)
@Serializable data class UseRecipeCommand(val expectedVersion: Long, val recipeId: String, val recipeVersion: Long, val ingredientIds: List<String>, val amount: Long = 1)
@Serializable data class ChangePasswordCommand(val expectedVersion: Long, val currentPassword: String, val newPassword: String)
@Serializable enum class EquipmentSlot(private val ru: String, private val en: String) {
    HELMET("Шлем", "Helmet"), BODY("Броня", "Body armour"), GLOVES("Перчатки", "Gloves"), BOOTS("Сапоги", "Boots"),
    BELT("Пояс", "Belt"), AMULET("Амулет", "Amulet"), RING_LEFT("Левое кольцо", "Left ring"), RING_RIGHT("Правое кольцо", "Right ring"),
    MAIN_HAND("Основная рука", "Main hand"), OFF_HAND("Вторая рука", "Off hand"), WINGS("Крылья", "Wings");
    fun title(lang: Lang = uiLanguage) = lang.pick(ru, en)
    companion object {
        fun forItem(slot: String): List<EquipmentSlot> = when (slot) {
            "RING" -> listOf(RING_LEFT, RING_RIGHT)
            "WEAPON_1H" -> listOf(MAIN_HAND, OFF_HAND)
            "WEAPON_2H" -> listOf(MAIN_HAND)
            "SHIELD", "QUIVER" -> listOf(OFF_HAND)
            else -> entries.filter { it.name == slot }
        }
    }
}
@Serializable data class WeaponStats(val minimumPhysical: Double, val maximumPhysical: Double, val attacksPerSecond: Double,
    val criticalChance: Double, val accuracy: Double, val averageHit: Double, val dps: Double)
@Serializable data class CalculatedStats(val version: Long, val values: Map<String, Double>, val weapons: Map<EquipmentSlot, WeaponStats> = emptyMap(), val unsupported: List<String> = emptyList())
/** Cursor page: [next] goes back as the `after` parameter, `null` means the list ended. */
@Serializable data class InventoryPage(val items: List<com.sperance.exileforge.core.model.hero.EquipmentInstance> = emptyList(), val size: Int = 0, val total: Long = 0, val next: String? = null)
/** One owned unit. There is no `amount`: the server stores a thousand orbs as a thousand records. */
@Serializable data class OwnedItem(val id: String, val itemId: String)
@Serializable data class OwnedItemsPage(val items: List<OwnedItem> = emptyList(), val size: Int = 0, val total: Long = 0, val next: String? = null)
/** The inventory is unbounded, so it arrives one page at a time; equipped items always come in full. */
@Serializable data class EquipmentView(val characterVersion: Long, val equipped: Map<EquipmentSlot, String>,
    val equippedItems: List<com.sperance.exileforge.core.model.hero.EquipmentInstance> = emptyList(),
    val inventory: InventoryPage = InventoryPage(), val items: OwnedItemsPage = OwnedItemsPage(), val stats: CalculatedStats)
@Serializable data class UserProfile(val id: String, val version: Long, val name: String, val login: String, val role: String, val countCharacters: Int = 0)
@Serializable data class ApiCapabilities(val apiRevision: Int = 0, val versionedCrud: Boolean = false, val characterCommands: Boolean = false, val profile: String = "", val equipmentComparison: Boolean = false, val catalogSearch: Boolean = false, val craftOptions: Boolean = false, val combat: Boolean = false, val passiveTree: Boolean = false,
    val icons: Boolean = false, val iconSet: String = "", val iconSetRevision: Int = 0, val iconSetVersion: String = "", val iconCount: Int = 0, val iconsEndpoint: String = "") {
    fun requireWorkbench() { requireCompatible(); require(apiRevision >= 3 && equipmentComparison && catalogSearch && craftOptions) { tr("Обновите сервер до API revision 3 (0.10.0)", "Update the server to API revision 3 (0.10.0)") } }
    fun requireCompatible() { require(apiRevision >= 2 && versionedCrud && characterCommands) { tr("Нужен ktor-bestgame 0.9.0 с командами персонажа и контролем версий", "ktor-bestgame 0.9.0 with character commands and version control is required") } }
    /** Icons are additive: an older server simply leaves the client on its bundled emblems. */
    fun hasIcons() = icons && apiRevision >= 4 && iconSetVersion.isNotBlank()
}
