package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.contract.entityVersion
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.presentation.state.PendingInventoryAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private fun loadInventoryMetadata() { with(runtime) {
metadataJob?.cancel()
        val snapshot = state.value
        val currentApi = api
        metadataJob = scope.launch {
            val limit = Semaphore(4)
            snapshot.inventory.map { it.text("equipmentId") }.distinct().filter { it !in snapshot.inventoryBases }.map { id ->
                async {
                    limit.withPermit {
                        try {
                            val base = currentApi.get(Catalog.EQUIPMENT, id) ?: return@withPermit
                            mutable.update { if(it.server == snapshot.server && it.characterId == snapshot.characterId) it.copy(inventoryBases = it.inventoryBases + (id to base)) else it }
                        } catch(e: CancellationException) { throw e } catch(_: Exception) { /* Fallback emblem/name stays available; refresh retries metadata. */ }
                    }
                }
            }.awaitAll()
        }

    } }

    private fun applyEquipmentView(result: EquipmentView) { with(runtime) {
mutable.update { it.copy(equipmentView = result, comparison = null, craftOptions = null, inventory = result.inventory.map { item -> item.document() }, inventoryVersion = result.characterVersion, conflict = false,
            inventoryBases = it.inventoryBases + result.inventory.mapNotNull { item -> (item["baseSnapshot"] as? JsonObject)?.let { base -> item.text("equipmentId") to base } },
            selectedEquipment = it.selectedEquipment.takeIf { id -> result.inventory.any { e -> e.text("uuid") == id } } ?: result.inventory.firstOrNull()?.text("uuid").orEmpty()) }
        loadInventoryMetadata()

    } }

    private suspend fun readInventory() { with(runtime) {
val id = state.value.characterId.trim()
        val character = api.character(id)
        val result = api.equipment(id)
        mutable.update { it.copy(characterOwner = character.userId, hero = character) }
        applyEquipmentView(result)
        val selected = state.value.selectedEquipment
        if(selected.isNotBlank()) { val options = api.craftOptions(id, selected); mutable.update { it.copy(craftOptions = options) } }

    } }

    private fun characterCommand(block: suspend (String, Long) -> EquipmentView) { with(runtime) {
task(writing = true) {
        check(state.value.pending == null) { "Сначала подтвердите предыдущую операцию" }
        val version = requireNotNull(state.value.inventoryVersion) { "Обновите экипировку" }
        try { applyEquipmentView(block(state.value.characterId, version)); mutable.update { it.copy(message = "Изменения сохранены") } }
        catch(e: Exception) { mutable.update { it.copy(inventoryVersion = null) }; throw e }
    }
    } }

    private suspend fun executePending() { with(runtime) {
val pending = requireNotNull(state.value.pending)
        val character = api.get(Catalog.CHARACTERS, pending.characterId) ?: error("Персонаж недоступен")
        check(character.text("userId") == state.value.profile?.id) { "Войдите в аккаунт владельца ожидающей операции" }
        val result = try { api.mutateInventory(pending.characterId, pending.operation, pending.payload) }
        catch(e: ApiFailure) {
            // Only explicit client rejections are definitive. Network/5xx can hide a committed write.
            if(e.status in listOf(400, 403, 404, 409, 422)) {
                store.savePending(null)
                mutable.update { it.copy(pending = null, inventoryVersion = null) }
            }
            throw e
        }
        val equipment = result.getValue("equipment").jsonObject
        store.savePending(null)
        mutable.update { it.copy(pending = null, craftAfter = if(pending.operation == "craft") equipment else it.craftAfter, inventoryVersion = result.getValue("characterVersion").jsonPrimitive.long,
            inventory = it.inventory.filterNot { item -> item.text("uuid") == equipment.text("uuid") } + equipment,
            selectedEquipment = equipment.text("uuid"), message = "Операция выполнена" + (result["currencyRemaining"]?.let { amount -> ". Осталось сфер: $amount" } ?: "")) }
        try { readInventory() } catch(e: CancellationException) { throw e }
        catch(_: Exception) { mutable.update { it.copy(inventoryVersion = null, message = "Операция выполнена. Обновите инвентарь перед следующей.") } }

    } }

}
