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

class SessionViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun mode(mode: AppMode) { with(runtime) {

        if(state.value.busy || state.value.editorOpen || mode == AppMode.ADMIN && !state.value.isAdmin) return
        mutable.update { it.copy(mode = mode, catalog = if(mode == AppMode.PLAYER) Catalog.CHARACTERS else Catalog.EQUIPMENT, items = emptyList(), page = 0, tab = 0, filter = CatalogFilter(), query = "") }
        task {
            val saved = store.filters(state.value.server, state.value.catalog.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
            mutable.update { it.copy(filter = saved, query = saved.query) }; loadPage(0)
        }
    
    } }
    fun serverDraft(value: String) { with(runtime) {
 if (!state.value.busy) mutable.update { it.copy(serverDraft = value) } 
    } }
    fun connect() { with(runtime) {
task {
        metadataJob?.cancel()
        check(state.value.pending == null) { "Сначала подтвердите результат ожидающего запроса" }
        val server = normalizeServer(state.value.serverDraft)
        store.save(server)
        clearSession()
        api = newApi(server)
        journal.clear()
        mutable.update { it.copy(server = server, serverDraft = server, items = emptyList(), original = null, editorOpen = false, page = 0, total = 0, totalPages = 0, checks = emptyList(), definitions = emptyList(), inventoryBases = emptyMap(), inventoryDefinitions = emptyList(), signedIn = false, inventory = emptyList(), inventoryVersion = null, pending = null, characterId = "", currencies = emptyList(), health = "Проверка соединения…") }
        val health = api.health()
        mutable.update { it.copy(health = health.toString(), message = "Сервер доступен") }
    }
    } }
    fun health() { with(runtime) {
task {
        val result = api.health().toString()
        mutable.update { it.copy(health = result, message = "Проверка соединения завершена") }
    }
    } }
    fun login(login: String, password: String) { with(runtime) {
task {
        clearSession()
        api.capabilities().requireWorkbench()
        api.login(login, password)
        val profile = try { api.currentUser() } catch(e: Exception) { api.logout(); throw e }
        mutable.update { it.copy(signedIn = true, profile = profile, mode = AppMode.PLAYER, catalog = Catalog.CHARACTERS, message = "Вход выполнен", tab = 0) }
        val saved = store.filters(state.value.server, Catalog.CHARACTERS.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
        mutable.update { it.copy(filter = saved, query = saved.query) }
        loadPage(0)
    }
    } }
    fun logout() { with(runtime) {
 if(!state.value.busy) clearSession() 
    } }
    fun changePassword(current: String, replacement: String) { with(runtime) {
task {
        require(replacement.length in 12..128) { "Новый пароль: от 12 до 128 символов" }
        val profile = api.currentUser()
        api.changePassword(ChangePasswordCommand(profile.version, current, replacement))
        clearSession()
        mutable.update { it.copy(message = "Пароль изменён. Войдите снова.") }
    }
    } }
}
