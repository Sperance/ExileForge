package com.sperance.exileforge.presentation.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.ForgeApplication
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.editor.conflict.ThreeWayMerge
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.contract.entityVersion
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.diff
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.referenceKey
import com.sperance.exileforge.core.contract.starterModifier
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.generation.modifierFromDefinition
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.core.verification.CrudScenario
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.PendingInventoryAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*

import com.sperance.exileforge.presentation.ForgeRuntime

class HeroViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun characterId(value: String) { with(runtime) {

        if(state.value.busy || state.value.pending != null) return
        mutable.update { it.copy(characterId = value, hero = null, comparison = null, craftOptions = null, craftBefore = null, craftAfter = null, equipmentView = null, characterOwner = "", inventory = emptyList(), inventoryVersion = null, selectedEquipment = "") }
    
    } }
    fun selectEquipment(value: String) { with(runtime) {
task {
        mutable.update { it.copy(selectedEquipment = value, comparison = null, craftOptions = null) }
        val item = state.value.inventory.firstOrNull { it.text("uuid") == value } ?: return@task
        val options = api.craftOptions(state.value.characterId, value)
        mutable.update { it.copy(craftOptions = options) }
        val definitions = pinnedDefinitions(buildJsonObject { put("params", item["params"] ?: JsonArray(emptyList())) })
        mutable.update { it.copy(inventoryDefinitions = (it.inventoryDefinitions + definitions).distinctBy(::definitionKey)) }
    }
    } }
    fun showCharacterInventory(id: String) { with(runtime) {
task {
        check(state.value.pending == null || state.value.characterId == id) { "Сначала подтвердите предыдущую операцию" }
        mutable.update { it.copy(tab = 4, characterId = id, equipmentView = null, characterOwner = "", inventory = emptyList(), inventoryVersion = null) }
        readInventory()
        val currencies = api.currencies()
        mutable.update { it.copy(currencies = currencies, selectedCurrency = currencies.firstOrNull()?.text("id").orEmpty()) }
    }
    } }
    fun selectCurrency(value: String) { with(runtime) {
 if(!state.value.busy) mutable.update { it.copy(selectedCurrency = value) } 
    } }
    fun compareEquipment(uuid: String, slot: EquipmentSlot) { with(runtime) {
task {
        val result = api.compareEquipment(state.value.characterId, EquipCommand(requireNotNull(state.value.inventoryVersion), uuid, slot))
        mutable.update { it.copy(comparison = result, compareUuid = uuid, compareSlot = slot) }
    }
    } }
    fun dismissComparison() { with(runtime) {
 mutable.update { it.copy(comparison = null) } 
    } }
    fun equipCompared() { with(runtime) {

        val s = state.value
        if(s.comparison?.allowed != true || s.comparison.characterVersion != s.inventoryVersion) return
        equip(s.compareUuid, requireNotNull(s.compareSlot))
    
    } }
    fun equip(uuid: String, slot: EquipmentSlot) { with(runtime) {
characterCommand { id, version -> api.equip(id, EquipCommand(version, uuid, slot)) }
    } }
    fun unequip(slot: EquipmentSlot) { with(runtime) {
characterCommand { id, version -> api.unequip(id, UnequipCommand(version, slot)) }
    } }
    fun grant(equipmentId: String) { with(runtime) {
characterCommand { id, version -> check(state.value.isAdmin); api.grant(id, GrantEquipmentCommand(version, equipmentId)) }
    } }
    fun adjustItems(itemId: String, amount: Long) { with(runtime) {
characterCommand { id, version -> check(state.value.isAdmin); require(amount != 0L); api.adjustItems(id, AdjustItemsCommand(version, listOf(ItemStack(itemId, amount)))) }
    } }
    fun redeem(code: String) { with(runtime) {
characterCommand { id, version -> api.redeem(id, RedeemCommand(version, code.trim())) }
    } }
    fun useRecipe(recipe: JsonObject, ingredients: List<String>, amount: Long) { with(runtime) {
characterCommand { id, version -> api.useRecipe(id, UseRecipeCommand(version, recipe.entityId, recipe.entityVersion, ingredients, amount)) }
    } }
    fun loadInventory() { with(runtime) {
task {
        readInventory()
        val currencies = api.currencies()
        mutable.update { it.copy(currencies = currencies, selectedCurrency = it.selectedCurrency.ifBlank { currencies.firstOrNull()?.text("id").orEmpty() }) }
    }
    } }
    fun randomItem() { with(runtime) {
 if(state.value.busy || state.value.editorOpen) return; mutable.update { it.copy(tab = 4) } 
    } }
    fun inventoryAction(operation: String) { with(runtime) {
task(writing = true) {
        check(state.value.pending == null) { "Сначала разрешите результат предыдущей операции" }
        val state = state.value
        check(state.ownsCharacter) { "Сферы и дроп доступны только владельцу персонажа" }
        if(operation == "drop") check(state.isAdmin) { "Дроп доступен администратору" }
        val version = requireNotNull(state.inventoryVersion) { "Загрузите инвентарь" }
        val payload = buildJsonObject {
            put("requestId", java.util.UUID.randomUUID().toString()); put("expectedVersion", version)
            if(operation == "craft") {
                require(state.selectedEquipment.isNotBlank() && state.selectedCurrency.isNotBlank()) { "Выберите экипировку и сферу" }
                put("equipmentUuid", state.selectedEquipment); put("currency", state.selectedCurrency)
            }
        }
        if(operation == "craft") mutable.update { it.copy(craftBefore = it.inventory.firstOrNull { item -> item.text("uuid") == state.selectedEquipment }, craftAfter = null) }
        val pending = PendingInventoryAction(state.characterId.trim(), operation, payload)
        store.savePending(buildJsonObject { put("characterId", pending.characterId); put("operation", operation); put("payload", payload) }.toString())
        mutable.update { it.copy(pending = pending) }
        executePending()
    }
    } }
    fun retryInventoryAction() { with(runtime) {
task { executePending() }
    } }
}
