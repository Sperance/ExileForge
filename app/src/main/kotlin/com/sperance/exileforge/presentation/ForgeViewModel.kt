package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.ForgeApplication
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
import kotlinx.serialization.json.*

class ForgeViewModel(private val store: ServerStore, private val journal: RequestJournal) : ViewModel() {
    private val mutable = MutableStateFlow(ForgeState())
    val state = mutable.asStateFlow()
    val logs = journal.entries
    private lateinit var api: GameApi
    init {
        viewModelScope.launch {
            try {
                val server = store.server.first()
                api = GameApi(server, journal)
                val restored = store.pending.first()?.let { raw ->
                    val saved = WireJson.parseToJsonElement(raw).jsonObject
                    PendingInventoryAction(saved.text("characterId"), saved.text("operation"), saved.getValue("payload").jsonObject)
                }
                mutable.update { it.copy(pending = restored, characterId = restored?.characterId.orEmpty()) }
                mutable.update { it.copy(server = server, serverDraft = server, busy = false) }
                refresh()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                api = GameApi("http://10.0.2.2:8080/", journal)
                mutable.update { it.copy(busy = false, error = true, message = e.message) }
            }
        }
    }
    fun tab(tab: Int) { mutable.update { it.copy(tab = tab) } }
    fun query(value: String) { mutable.update { it.copy(query = value) } }
    suspend fun referencePage(source: EntitySource, page: Int) = api.referencePage(source, page)
    fun serverDraft(value: String) { if (!state.value.busy) mutable.update { it.copy(serverDraft = value) } }
    fun dismissMessage() { mutable.update { it.copy(message = null) } }
    fun clearLogs() = journal.clear()
    fun catalog(value: Catalog) {
        if (state.value.busy || state.value.editorOpen) return
        mutable.update { it.copy(catalog = value, items = emptyList(), page = 0, total = 0, totalPages = 0, query = "") }
        refresh()
    }
    private fun task(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, message = null, error = false) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val prefix = if (e is ApiFailure) "HTTP ${e.status ?: "—"} ${e.code.orEmpty()}: " else ""
                mutable.update { it.copy(error = true, message = prefix + (e.message ?: "Ошибка запроса")) }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    private suspend fun loadPage(page: Int) {
        val result = api.page(state.value.catalog, page)
        mutable.update { it.copy(items = result.items, page = result.page, total = result.totalItems, totalPages = result.totalPages) }
    }
    fun refresh(page: Int = state.value.page) = task { loadPage(page) }
    fun connect() = task {
        check(state.value.pending == null) { "Сначала подтвердите результат ожидающего запроса" }
        val server = normalizeServer(state.value.serverDraft)
        store.save(server)
        api = GameApi(server, journal)
        journal.clear()
        mutable.update { it.copy(server = server, serverDraft = server, items = emptyList(), original = null, editorOpen = false, page = 0, total = 0, totalPages = 0, checks = emptyList(), definitions = emptyList(), signedIn = false, inventory = emptyList(), inventoryVersion = null, pending = null, characterId = "", currencies = emptyList(), health = "Проверка соединения…") }
        val health = api.health()
        mutable.update { it.copy(health = health.toString(), message = "Сервер доступен") }
        loadPage(0)
    }
    fun health() = task {
        val result = api.health().toString()
        mutable.update { it.copy(health = result, message = "Проверка соединения завершена") }
    }
    fun count() = task {
        val result = api.count(state.value.catalog)
        mutable.update { it.copy(message = "Количество: $result") }
    }
    fun open(id: String) = task {
        val doc = api.get(state.value.catalog, id) ?: error("Предмет не найден")
        val pinned = pinnedDefinitions(doc)
        setEditor(doc, doc)
        mutable.update { it.copy(definitions = (pinned + it.definitions).distinctBy(::definitionKey)) }
    }
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) {
        if (state.value.busy) return
        setEditor(template(state.value.catalog, kind), null)
    }
    private fun setEditor(document: JsonObject, original: JsonObject?) {
        mutable.update { it.copy(original = original, editorOpen = true, draft = document, tab = 1,
            definitions = it.definitions) }
    }
    fun closeEditor() { if (!state.value.busy) mutable.update { it.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap())) } }
    fun edit(document: JsonObject) { if (!state.value.busy) mutable.update { it.copy(draft = document) } }
    private suspend fun pinnedDefinitions(document: JsonObject): List<JsonObject> {
        val refs = listOf("modifierDefinitionRefs", "stockModifierDefinitionRefs").flatMap { (document[it] as? JsonArray).orEmpty() }.map { it.jsonObject }
        val rolls = listOf("modifiers", "params").flatMap { (document[it] as? JsonArray).orEmpty() }.map { raw ->
            val mod = raw.jsonObject
            buildJsonObject { put("definitionId", mod.text("definitionId")); put("revision", mod.text("definitionRevision").toIntOrNull() ?: 1) }
        }
        return (refs + rolls).distinctBy { referenceKey(it) }.map { api.definition(it.text("definitionId"), it.text("revision").toIntOrNull() ?: 1) }
    }
    fun publishDefinition(document: JsonObject, expectedRevision: Int) = task {
        validateForm("definition", document)
        val saved = api.publishDefinition(document, expectedRevision)
        mutable.update { it.copy(definitions = (listOf(saved) + it.definitions).distinctBy(::definitionKey), message = "Опубликовано ${saved.text("id")} v${saved.text("revision")}") }
    }
    fun definitionQuery(value: String) { mutable.update { it.copy(definitionQuery = value) } }
    fun loadDefinitions(page: Int = 0) = task {
        val result = api.definitions(state.value.definitionQuery.trim(), page)
        val definitions = (pinnedDefinitions(state.value.draft) + result.getValue("items").jsonArray.map { it.jsonObject }).distinctBy(::definitionKey)
        mutable.update { it.copy(definitions = definitions, definitionPage = page, definitionTotal = result.getValue("total").jsonPrimitive.int) }
    }
    fun login(login: String, password: String) = task {
        mutable.update { it.copy(signedIn = false, inventoryVersion = null, inventory = emptyList()) }
        api.login(login, password)
        mutable.update { it.copy(signedIn = true, message = "Вход выполнен") }
    }
    fun logout() {
        if(state.value.busy) return
        api.logout()
        mutable.update { it.copy(signedIn = false, inventory = emptyList(), inventoryVersion = null, ) }
    }
    fun characterId(value: String) {
        if(state.value.busy || state.value.pending != null) return
        mutable.update { it.copy(characterId = value, inventory = emptyList(), inventoryVersion = null, selectedEquipment = "") }
    }
    fun selectEquipment(value: String) { if(!state.value.busy) mutable.update { it.copy(selectedEquipment = value) } }
    fun selectCurrency(value: String) { if(!state.value.busy) mutable.update { it.copy(selectedCurrency = value) } }
    private suspend fun readInventory() {
        val result = api.inventory(state.value.characterId.trim())
        val equipment = result.getValue("equipment").jsonArray.map { it.jsonObject }
        mutable.update { it.copy(inventory = equipment, inventoryVersion = result.getValue("version").jsonPrimitive.long,
            selectedEquipment = it.selectedEquipment.takeIf { selected -> equipment.any { e -> e.text("uuid") == selected } } ?: equipment.firstOrNull()?.text("uuid").orEmpty()) }
    }
    fun loadInventory() = task {
        readInventory()
        val currencies = api.currencies()
        mutable.update { it.copy(currencies = currencies, selectedCurrency = it.selectedCurrency.ifBlank { currencies.firstOrNull()?.text("id").orEmpty() }) }
    }
    fun randomItem() { if(state.value.busy || state.value.editorOpen) return; mutable.update { it.copy(tab = 1, editorOpen = false, original = null) } }
    fun inventoryAction(operation: String) = task {
        check(state.value.pending == null) { "Сначала разрешите результат предыдущей операции" }
        val state = state.value
        val version = requireNotNull(state.inventoryVersion) { "Загрузите инвентарь" }
        val payload = buildJsonObject {
            put("requestId", java.util.UUID.randomUUID().toString()); put("expectedVersion", version)
            if(operation == "craft") {
                require(state.selectedEquipment.isNotBlank() && state.selectedCurrency.isNotBlank()) { "Выберите экипировку и сферу" }
                put("equipmentUuid", state.selectedEquipment); put("currency", state.selectedCurrency)
            }
        }
        val pending = PendingInventoryAction(state.characterId.trim(), operation, payload)
        store.savePending(buildJsonObject { put("characterId", pending.characterId); put("operation", operation); put("payload", payload) }.toString())
        mutable.update { it.copy(pending = pending) }
        executePending()
    }
    fun retryInventoryAction() = task { executePending() }
    private suspend fun executePending() {
        val pending = requireNotNull(state.value.pending)
        val result = try { api.mutateInventory(pending.characterId, pending.operation, pending.payload) }
        catch(e: ApiFailure) {
            // Only explicit client rejections are definitive. Network/5xx can hide a committed write.
            if(e.status in listOf(400, 409, 422)) {
                store.savePending(null)
                mutable.update { it.copy(pending = null, inventoryVersion = null) }
            }
            throw e
        }
        val equipment = result.getValue("equipment").jsonObject
        store.savePending(null)
        mutable.update { it.copy(pending = null, inventoryVersion = result.getValue("characterVersion").jsonPrimitive.long,
            inventory = it.inventory.filterNot { item -> item.text("uuid") == equipment.text("uuid") } + equipment,
            selectedEquipment = equipment.text("uuid"), message = "Операция выполнена" + (result["currencyRemaining"]?.let { amount -> ". Осталось сфер: $amount" } ?: "")) }
        try { readInventory() } catch(e: CancellationException) { throw e }
        catch(_: Exception) { mutable.update { it.copy(inventoryVersion = null, message = "Операция выполнена. Обновите инвентарь перед следующей.") } }
    }
    fun save() = task {
        val document = state.value.draft
        val catalog = state.value.catalog
        validate(document, catalog)
        val original = state.value.original
        val saved = if (original == null) api.create(catalog, JsonObject(document.filterKeys { it !in protectedFields || it == "type" })) else {
            val changes = diff(original, document)
            require(catalog != Catalog.CHARACTERS || "userId" !in changes) { "Владельца существующего персонажа менять нельзя" }
            require(changes.isNotEmpty()) { "Нет изменений для сохранения" }
            val latest = api.get(catalog, original.entityId) ?: error("Предмет уже удалён")
            require(changes.keys.all { original[it] == latest[it] }) { "Изменяемые поля обновлены другим клиентом. Откройте предмет заново." }
            api.update(catalog, original.entityId, changes)
        }
        setEditor(saved, saved)
        mutable.update { it.copy(message = "Сохранено: ${saved.entityId}") }
        // List refresh failure must not imply that the successful mutation failed.
        try { loadPage(state.value.page) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = "Сохранено. Обновите список вручную.") } }
    }
    fun delete() = task {
        val original = state.value.original ?: error("Сначала сохраните предмет")
        api.delete(state.value.catalog, original.entityId)
        mutable.update { it.copy(editorOpen = false, original = null, tab = 0, items = it.items.filterNot { item -> item.entityId == original.entityId }, message = "Предмет удалён") }
        try { loadPage(0) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = "Удалено. Обновите список вручную.") } }
    }
    fun runChecks() = task {
        require(state.value.catalog != Catalog.CHARACTERS) { "CRUD-сценарий предназначен для предметов" }
        mutable.update { it.copy(checks = emptyList()) }
        val modifier = if(state.value.catalog == Catalog.EQUIPMENT) {
            val definitions = api.definitions("", 0).getValue("items").jsonArray
            modifierFromDefinition(definitions.firstOrNull()?.jsonObject ?: error("Каталог модификаторов пуст. Запустите Seeder сервера"))
        } else starterModifier()
        CrudScenario(api, modifier).run(state.value.catalog) { result -> mutable.update { it.copy(checks = it.checks + result) } }
    }
    class Factory(private val app: ForgeApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ForgeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ForgeViewModel(app.serverStore, app.journal) as T
        }
    }
}
