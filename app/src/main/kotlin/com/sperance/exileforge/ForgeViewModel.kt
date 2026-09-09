package com.sperance.exileforge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

data class ForgeState(
    val tab: Int = 0, val catalog: Catalog = Catalog.EQUIPMENT,
    val server: String = "http://10.0.2.2:8080/", val serverDraft: String = "http://10.0.2.2:8080/",
    val busy: Boolean = true, val message: String? = null, val error: Boolean = false,
    val items: List<JsonObject> = emptyList(), val page: Int = 0, val totalPages: Int = 0, val total: Long = 0,
    val query: String = "", val lookupId: String = "",
    val original: JsonObject? = null, val editorOpen: Boolean = false,
    val draft: JsonObject = JsonObject(emptyMap()), val definitions: List<JsonObject> = presetDefinitions,
    val checks: List<CheckResult> = emptyList(), val health: String = "Соединение ещё не проверено"
)
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
    fun lookup(value: String) { mutable.update { it.copy(lookupId = value) } }
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
        val server = normalizeServer(state.value.serverDraft)
        store.save(server)
        api = GameApi(server, journal)
        journal.clear()
        mutable.update { it.copy(server = server, serverDraft = server, items = emptyList(), original = null, editorOpen = false, page = 0, total = 0, totalPages = 0, checks = emptyList(), definitions = presetDefinitions, health = "Проверка соединения…") }
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
        setEditor(doc, doc)
    }
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) {
        if (state.value.busy) return
        setEditor(template(state.value.catalog, kind), null)
    }
    private fun setEditor(document: JsonObject, original: JsonObject?) {
        mutable.update { it.copy(original = original, editorOpen = true, draft = document, tab = 1,
            definitions = (definitionsOf(document) + it.definitions).distinctBy { definition -> definition.text("id") }) }
    }
    fun closeEditor() { if (!state.value.busy) mutable.update { it.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap())) } }
    fun edit(document: JsonObject) { if (!state.value.busy) mutable.update { it.copy(draft = document) } }
    private suspend fun equipmentCatalog(): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        var page = 0
        do {
            val response = api.page(Catalog.EQUIPMENT, page)
            result += response.items
            page++
        } while(page < response.totalPages)
        return result
    }
    fun loadDefinitions() = task {
        val definitions = equipmentCatalog().flatMap(::definitionsOf)
        mutable.update { it.copy(definitions = (definitionsOf(it.draft) + definitions + presetDefinitions).distinctBy { d -> d.text("id") }, message = "Список модификаторов обновлён") }
    }
    fun randomItem() = task {
        check(!state.value.editorOpen) { "Сначала закройте редактор" }
        val bases = equipmentCatalog()
        val base = bases.randomOrNull() ?: template(Catalog.EQUIPMENT)
        val document = ItemGenerator().generate(base)
        mutable.update { it.copy(catalog = Catalog.EQUIPMENT, items = emptyList(), page = 0, total = 0, totalPages = 0) }
        setEditor(document, null)
        mutable.update { it.copy(message = "Случайный предмет готов. Нажмите «Сохранить», чтобы добавить его на сервер.") }
    }
    fun reroll() {
        if (state.value.busy || state.value.catalog != Catalog.EQUIPMENT) return
        try {
            val generated = ItemGenerator().generate(state.value.draft)
            mutable.update { it.copy(draft = JsonObject(it.draft + ("modifiers" to generated.getValue("modifiers"))), message = "Модификаторы сгенерированы по выбранным диапазонам") }
        } catch (e: Exception) { mutable.update { it.copy(message = e.message) } }
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
        CrudScenario(api).run(state.value.catalog) { result -> mutable.update { it.copy(checks = it.checks + result) } }
    }
    class Factory(private val app: ForgeApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ForgeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ForgeViewModel(app.serverStore, app.journal) as T
        }
    }
}
