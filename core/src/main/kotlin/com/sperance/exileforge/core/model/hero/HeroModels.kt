package com.sperance.exileforge.core.model.hero

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.command.*

@Serializable data class EquipmentInstance(val uuid: String, val equipmentId: String, val params: List<Modifier> = emptyList(), val poe: PoeState? = null, val baseSnapshot: JsonObject? = null) {
    fun document(): JsonObject = WireJson.encodeToJsonElement(this).jsonObject
    operator fun get(key: String): JsonElement? = document()[key]
}
@Serializable data class PoeState(val baseId: String, val itemLevel: Int, val rarity: String, val quality: Int = 0, val corrupted: Boolean = false, val mirrored: Boolean = false, val implicits: List<PoeRoll> = emptyList(), val explicits: List<PoeRoll> = emptyList())
@Serializable data class PoeRoll(val id: String, val values: List<Int>, val revision: Int = 1, val fractured: Boolean = false)
@Serializable data class CharacterSummary(@SerialName("_id") val id: String, val userId: String, val name: String, val version: Long, val level: Int = 1, val experience: Double = 0.0, val money: Long = 0)
@Serializable data class RecipeDocument(@SerialName("_id") val id: String, val version: Long, val name: String, val arrayIn: List<RecipeInput> = emptyList(), val arrayOut: List<RecipeOutput> = emptyList(), val timeWork: Double = 1.0, val requirement: List<JsonObject>? = null, val needOpenRecipe: Boolean = false) {
    fun document(): JsonObject = WireJson.encodeToJsonElement(this).jsonObject
}
@Serializable data class RecipeInput(val itemId: String? = null, val category: String? = null, val subCategory: String? = null, val amount: Double)
@Serializable data class RecipeOutput(val itemId: String, val amount: Double, val chance: Double = 1.0)
@Serializable data class EquipmentComparison(val characterVersion: Long, val allowed: Boolean, val reason: String? = null, val before: CalculatedStats, val after: CalculatedStats? = null)
@Serializable data class CraftOption(val currency: String, val name: String, val itemId: String, val amount: Long, val available: Boolean, val reason: String? = null)
@Serializable data class CraftOptions(val characterVersion: Long, val options: List<CraftOption>)
