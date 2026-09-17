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
import com.sperance.exileforge.core.i18n.tr

class HeroViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun characterId(value: String) { with(runtime) {

        if(state.value.busy || state.value.pending != null) return
        mutable.update { it.copy(passiveState = null, passiveTree = null, passivePending = null, passiveCharacterId = "", battleView = null, battlePending = null, battleCharacterId = "", battleAction = null, battleLoot = emptyMap(), characterId = value, hero = null, comparison = null, craftOptions = null, craftBefore = null, craftAfter = null, equipmentView = null, characterOwner = "", inventory = emptyList(), inventoryVersion = null, inventoryNext = null, inventoryTotal = 0, itemTotals = emptyMap(), selectedEquipment = "") }

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
        check(state.value.pending == null || state.value.characterId == id) { tr("Сначала подтвердите предыдущую операцию", "Confirm the previous operation first") }
        mutable.update { it.copy(passiveState = null, passiveTree = null, passivePending = null, passiveCharacterId = "", battleView = null, battlePending = null, battleCharacterId = "", battleAction = null, battleLoot = emptyMap(), tab = 4, characterId = id, equipmentView = null, characterOwner = "", inventory = emptyList(), inventoryVersion = null, inventoryNext = null, inventoryTotal = 0, itemTotals = emptyMap()) }
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
characterCommand(bag = true) { id, version -> check(state.value.isAdmin); api.adjustItems(id, AdjustItemsCommand(version, listOf(ItemStack(itemId, amount)))) }
    } }
    fun redeem(code: String) { with(runtime) {
characterCommand(bag = true) { id, version -> api.redeem(id, RedeemCommand(version, code.trim())) }
    } }
    fun useRecipe(recipe: JsonObject, ingredients: List<String>, amount: Long) { with(runtime) {
characterCommand(bag = true) { id, version -> api.useRecipe(id, UseRecipeCommand(version, recipe.entityId, recipe.entityVersion, ingredients, amount)) }
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
        check(state.value.pending == null) { tr("Сначала разрешите результат предыдущей операции", "Resolve the result of the previous operation first") }
        val state = state.value
        check(state.ownsCharacter) { tr("Сферы и дроп доступны только владельцу персонажа", "Orbs and drops are available to the character's owner only") }
        if(operation == "drop") check(state.isAdmin) { tr("Дроп доступен администратору", "Drops are available to administrators") }
        val version = requireNotNull(state.inventoryVersion) { tr("Загрузите инвентарь", "Load the inventory") }
        val payload = buildJsonObject {
            put("requestId", java.util.UUID.randomUUID().toString()); put("expectedVersion", version)
            if(operation == "craft") {
                require(state.selectedEquipment.isNotBlank() && state.selectedCurrency.isNotBlank()) { tr("Выберите экипировку и сферу", "Choose an item and an orb") }
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
task(writing = true) { executePending() }
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

    // Equipped items always arrive in full; the rest of the stash is one cursor page of an unbounded list.
    private fun applyEquipmentView(result: EquipmentView) { with(runtime) {
val instances = (result.equippedItems + result.inventory.items).distinctBy { it.uuid }.map { it.document() }
        mutable.update { it.copy(equipmentView = result, comparison = null, craftOptions = null, inventory = instances, inventoryVersion = result.characterVersion, conflict = false,
            inventoryNext = result.inventory.next, inventoryTotal = result.inventory.total, inventoryBases = it.inventoryBases + bases(instances),
            selectedEquipment = it.selectedEquipment.takeIf { id -> instances.any { e -> e.text("uuid") == id } } ?: instances.firstOrNull()?.text("uuid").orEmpty()) }
        loadInventoryMetadata()

    } }
    private fun bases(instances: List<JsonObject>) = instances.mapNotNull { item -> (item["baseSnapshot"] as? JsonObject)?.let { base -> item.text("equipmentId") to base } }
    fun loadMoreInventory() { with(runtime) {
task {
        val after = state.value.inventoryNext ?: return@task
        val result = api.equipment(state.value.characterId.trim(), after = after)
        // A newer version means the page no longer matches what is already shown: start over from it.
        if(result.characterVersion != state.value.inventoryVersion) return@task applyEquipmentView(result)
        val added = result.inventory.items.map { it.document() }
        mutable.update { it.copy(inventory = (it.inventory + added).distinctBy { doc -> doc.text("uuid") },
            inventoryNext = result.inventory.next, inventoryTotal = result.inventory.total, inventoryBases = it.inventoryBases + bases(added)) }
        loadInventoryMetadata()
    }
    } }
    /** Without stacks an amount exists only as a count of documents, so the server recomputes it on request. */
    private suspend fun refreshItemTotals(id: String) { with(runtime) {
val totals = api.itemTotals(id)
        mutable.update { if(it.characterId.trim() == id) it.copy(itemTotals = totals) else it }
    } }

    private suspend fun readInventory() { with(runtime) {
val id = state.value.characterId.trim()
        val character = api.character(id)
        val result = api.equipment(id)
        mutable.update { it.copy(characterOwner = character.userId, hero = character) }
        applyEquipmentView(result)
        refreshItemTotals(id)
        val selected = state.value.selectedEquipment
        if(selected.isNotBlank()) { val options = api.craftOptions(id, selected); mutable.update { it.copy(craftOptions = options) } }

    } }

    /** [bag] marks the commands that can change owned units, whose totals only a fresh count can tell. */
    private fun characterCommand(bag: Boolean = false, block: suspend (String, Long) -> EquipmentView) { with(runtime) {
task(writing = true) {
        check(state.value.pending == null) { tr("Сначала подтвердите предыдущую операцию", "Confirm the previous operation first") }
        val version = requireNotNull(state.value.inventoryVersion) { tr("Обновите экипировку", "Refresh the equipment") }
        val id = state.value.characterId
        try { applyEquipmentView(block(id, version)); if(bag) refreshItemTotals(id.trim()); mutable.update { it.copy(message = tr("Изменения сохранены", "Changes saved")) } }
        catch(e: Exception) { mutable.update { it.copy(inventoryVersion = null) }; throw e }
    }
    } }

    private suspend fun executePending() { with(runtime) {
val pending = requireNotNull(state.value.pending)
        val character = api.get(Catalog.CHARACTERS, pending.characterId) ?: error(tr("Персонаж недоступен", "The character is unavailable"))
        check(character.text("userId") == state.value.profile?.id) { tr("Войдите в аккаунт владельца ожидающей операции", "Sign in as the owner of the pending operation") }
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
            selectedEquipment = equipment.text("uuid"), message = tr("Операция выполнена", "Operation complete") + (result["currencyRemaining"]?.let { amount -> tr(". Осталось сфер: $amount", ". Orbs left: $amount") } ?: "")) }
        try { readInventory() } catch(e: CancellationException) { throw e }
        catch(_: Exception) { mutable.update { it.copy(inventoryVersion = null, message = tr("Операция выполнена. Обновите инвентарь перед следующей.", "Operation complete. Refresh the inventory before the next one.")) } }

    } }

}
