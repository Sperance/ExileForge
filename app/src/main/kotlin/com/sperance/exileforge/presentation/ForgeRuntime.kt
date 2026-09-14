package com.sperance.exileforge.presentation

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

class ForgeRuntime(val store: ServerStore, val journal: RequestJournal) {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    val mutable = MutableStateFlow(ForgeState())
    val state = mutable.asStateFlow()
    val logs = journal.entries
    lateinit var api: GameApi
    var metadataJob: Job? = null
    val catalogViewModel = com.sperance.exileforge.presentation.features.CatalogViewModel(this)
    val editorViewModel = com.sperance.exileforge.presentation.features.EditorViewModel(this)
    val heroViewModel = com.sperance.exileforge.presentation.features.HeroViewModel(this)
    val sessionViewModel = com.sperance.exileforge.presentation.features.SessionViewModel(this)
    val checksViewModel = com.sperance.exileforge.presentation.features.ChecksViewModel(this)
    fun newApi(server: String): GameApi {
        lateinit var created: GameApi
        created = GameApi(server, journal, onUnauthorized = {
            if(::api.isInitialized && api === created) {
                clearSession()
                mutable.update { it.copy(message = "Сессия истекла. Войдите снова.") }
            }
        })
        return created
    }
    init {
        scope.launch {
            try {
                val server = store.server.first()
                api = newApi(server)
                val restored = store.pending.first()?.let { raw ->
                    val saved = WireJson.parseToJsonElement(raw).jsonObject
                    PendingInventoryAction(saved.text("characterId"), saved.text("operation"), saved.getValue("payload").jsonObject)
                }
                mutable.update { it.copy(pending = restored, characterId = restored?.characterId.orEmpty()) }
                mutable.update { it.copy(server = server, serverDraft = server, busy = false) }
                mutable.update { it.copy(message = "Войдите в аккаунт для загрузки каталога") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                api = newApi("http://10.0.2.2:8080/")
                mutable.update { it.copy(busy = false, error = true, message = e.message) }
            }
        }
    }
    fun tab(tab: Int) { if(!state.value.adminTools && tab == 2) return; mutable.update { it.copy(tab = tab) } }
    suspend fun referencePage(source: EntitySource, page: Int, query: String) = api.referencePage(source, page, query)
    fun dismissMessage() { mutable.update { it.copy(message = null) } }
    fun task(writing: Boolean = false, block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, message = null, error = false, failure = null) }
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if(e is ApiFailure && e.status == 409) mutable.update { it.copy(conflict = true, inventoryVersion = null) }
                val problem = FailureState.from(e, writing)
                mutable.update { it.copy(failure = problem) }
                val prefix = if(e is ApiFailure && e.status == 409) "Запись изменена. Черновик сохранён; обновите данные и проверьте изменения. " else if (e is ApiFailure) "HTTP ${e.status ?: "—"} ${e.code.orEmpty()}: " else ""
                mutable.update { it.copy(error = true, message = if(problem == FailureState.UncertainWrite) "Ответ потерян. Запись могла сохраниться: обновите данные перед повтором." else if(problem == FailureState.Offline) "Нет соединения. Проверьте сеть и повторите загрузку." else prefix + (e.message ?: "Ошибка запроса")) }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    suspend fun loadPage(page: Int) {
        val result = api.search(state.value.catalog, page, state.value.filter.copy(query = state.value.query))
        mutable.update { it.copy(items = result.items, page = result.page, total = result.totalItems, totalPages = result.totalPages) }
    }
    fun setEditor(document: JsonObject, original: JsonObject?) {
        mutable.update { it.copy(conflict = false, mergeReview = null, mergeRemote = null, original = original, editorOpen = true, draft = document, tab = 1,
            definitions = it.definitions) }
    }
    suspend fun pinnedDefinitions(document: JsonObject): List<JsonObject> {
        val refs = listOf("modifierDefinitionRefs", "stockModifierDefinitionRefs").flatMap { (document[it] as? JsonArray).orEmpty() }.map { it.jsonObject }
        val rolls = listOf("modifiers", "params").flatMap { (document[it] as? JsonArray).orEmpty() }.map { raw ->
            val mod = raw.jsonObject
            buildJsonObject { put("definitionId", mod.text("definitionId")); put("revision", mod.text("definitionRevision").toIntOrNull() ?: 1) }
        }
        return (refs + rolls).distinctBy { referenceKey(it) }.map { api.definition(it.text("definitionId"), it.text("revision").toIntOrNull() ?: 1) }
    }
    fun clearSession() {
        metadataJob?.cancel(); api.logout(); journal.clear()
        mutable.update { it.copy(signedIn = false, profile = null, sessionEpoch = it.sessionEpoch + 1, items = emptyList(), total = 0, page = 0, totalPages = 0,
            original = null, draft = JsonObject(emptyMap()), editorOpen = false, inventory = emptyList(), inventoryVersion = null, equipmentView = null,
            inventoryBases = emptyMap(), inventoryDefinitions = emptyList(), characterOwner = "", characterId = it.pending?.characterId.orEmpty(),
            selectedEquipment = "", selectedCurrency = "", currencies = emptyList(), checks = emptyList(), tab = 3, conflict = false, mode = AppMode.PLAYER, hero = null, comparison = null, craftOptions = null, craftBefore = null, craftAfter = null, mergeReview = null, mergeRemote = null, failure = null) }
    }
    fun loadInventoryMetadata() {
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
    }
    fun applyEquipmentView(result: EquipmentView) {
        mutable.update { it.copy(equipmentView = result, comparison = null, craftOptions = null, inventory = result.inventory.map { item -> item.document() }, inventoryVersion = result.characterVersion, conflict = false,
            inventoryBases = it.inventoryBases + result.inventory.mapNotNull { item -> (item["baseSnapshot"] as? JsonObject)?.let { base -> item.text("equipmentId") to base } },
            selectedEquipment = it.selectedEquipment.takeIf { id -> result.inventory.any { e -> e.text("uuid") == id } } ?: result.inventory.firstOrNull()?.text("uuid").orEmpty()) }
        loadInventoryMetadata()
    }
    suspend fun readInventory() {
        val id = state.value.characterId.trim()
        val character = api.character(id)
        val result = api.equipment(id)
        mutable.update { it.copy(characterOwner = character.userId, hero = character) }
        applyEquipmentView(result)
    }
    fun characterCommand(block: suspend (String, Long) -> EquipmentView) = task(writing = true) {
        check(state.value.pending == null) { "Сначала подтвердите предыдущую операцию" }
        val version = requireNotNull(state.value.inventoryVersion) { "Обновите экипировку" }
        try { applyEquipmentView(block(state.value.characterId, version)); mutable.update { it.copy(message = "Изменения сохранены") } }
        catch(e: Exception) { mutable.update { it.copy(inventoryVersion = null) }; throw e }
    }
    suspend fun recipe(id: String) = api.recipe(id).document()
    suspend fun executePending() {
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
        try { readInventory(); if(pending.operation == "craft") { val options = api.craftOptions(pending.characterId, equipment.text("uuid")); mutable.update { it.copy(craftOptions = options) } } } catch(e: CancellationException) { throw e }
        catch(_: Exception) { mutable.update { it.copy(inventoryVersion = null, message = "Операция выполнена. Обновите инвентарь перед следующей.") } }
    }
    fun query(value: String) = catalogViewModel.query(value)
    fun catalog(value: Catalog) = catalogViewModel.catalog(value)
    fun filter(value: CatalogFilter) = catalogViewModel.filter(value)
    fun applyFilters() = catalogViewModel.applyFilters()
    fun refresh(page: Int = state.value.page) = catalogViewModel.refresh(page)
    fun count() = catalogViewModel.count()
    fun open(id: String) = catalogViewModel.open(id)
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) = editorViewModel.create(kind)
    fun closeEditor() = editorViewModel.closeEditor()
    fun edit(document: JsonObject) = editorViewModel.edit(document)
    fun publishDefinition(document: JsonObject, expectedRevision: Int) = editorViewModel.publishDefinition(document, expectedRevision)
    fun definitionQuery(value: String) = editorViewModel.definitionQuery(value)
    fun loadDefinitions(page: Int = 0) = editorViewModel.loadDefinitions(page)
    fun reviewConflict() = editorViewModel.reviewConflict()
    fun resolveConflict(choices: Map<String, Boolean>) = editorViewModel.resolveConflict(choices)
    fun reloadEditor() = editorViewModel.reloadEditor()
    fun save() = editorViewModel.save()
    fun delete() = editorViewModel.delete()
    fun editInventoryBase(id: String) = editorViewModel.editInventoryBase(id)
    fun characterId(value: String) = heroViewModel.characterId(value)
    fun selectEquipment(value: String) = heroViewModel.selectEquipment(value)
    fun showCharacterInventory(id: String) = heroViewModel.showCharacterInventory(id)
    fun selectCurrency(value: String) = heroViewModel.selectCurrency(value)
    fun compareEquipment(uuid: String, slot: EquipmentSlot) = heroViewModel.compareEquipment(uuid, slot)
    fun dismissComparison() = heroViewModel.dismissComparison()
    fun equipCompared() = heroViewModel.equipCompared()
    fun equip(uuid: String, slot: EquipmentSlot) = heroViewModel.equip(uuid, slot)
    fun unequip(slot: EquipmentSlot) = heroViewModel.unequip(slot)
    fun grant(equipmentId: String) = heroViewModel.grant(equipmentId)
    fun adjustItems(itemId: String, amount: Long) = heroViewModel.adjustItems(itemId, amount)
    fun redeem(code: String) = heroViewModel.redeem(code)
    fun useRecipe(recipe: JsonObject, ingredients: List<String>, amount: Long) = heroViewModel.useRecipe(recipe, ingredients, amount)
    fun loadInventory() = heroViewModel.loadInventory()
    fun randomItem() = heroViewModel.randomItem()
    fun inventoryAction(operation: String) = heroViewModel.inventoryAction(operation)
    fun retryInventoryAction() = heroViewModel.retryInventoryAction()
    fun mode(mode: AppMode) = sessionViewModel.mode(mode)
    fun serverDraft(value: String) = sessionViewModel.serverDraft(value)
    fun connect() = sessionViewModel.connect()
    fun health() = sessionViewModel.health()
    fun login(login: String, password: String) = sessionViewModel.login(login, password)
    fun logout() = sessionViewModel.logout()
    fun changePassword(current: String, replacement: String) = sessionViewModel.changePassword(current, replacement)
    fun runChecks() = checksViewModel.runChecks()
    fun clearLogs() = checksViewModel.clearLogs()
    fun close() { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
}
