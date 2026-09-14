package com.sperance.exileforge.presentation

import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.referenceKey
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.PendingInventoryAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
    suspend fun recipe(id: String) = api.recipe(id).document()
    fun close() { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
}
