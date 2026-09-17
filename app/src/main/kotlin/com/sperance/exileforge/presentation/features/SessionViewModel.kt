package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.normalizeServer
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.core.i18n.tr

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
        check(state.value.pending == null) { tr("Сначала подтвердите результат ожидающего запроса", "Confirm the result of the pending request first") }
        val server = normalizeServer(state.value.serverDraft)
        store.save(server)
        clearSession()
        api = newApi(server)
        journal.clear()
        mutable.update { it.copy(server = server, serverDraft = server, items = emptyList(), original = null, editorOpen = false, page = 0, total = 0, totalPages = 0, checks = emptyList(), definitions = emptyList(), inventoryBases = emptyMap(), inventoryDefinitions = emptyList(), signedIn = false, inventory = emptyList(), inventoryVersion = null, pending = null, characterId = "", currencies = emptyList(), icons = com.sperance.exileforge.core.display.icons.IconSet(), health = tr("Проверка соединения…", "Checking the connection…")) }
        val health = api.health()
        mutable.update { it.copy(health = health.toString(), message = tr("Сервер доступен", "The server is reachable")) }
        iconViewModel.load()
    }
    } }
    fun health() { with(runtime) {
task {
        val result = api.health().toString()
        mutable.update { it.copy(health = result, message = tr("Проверка соединения завершена", "Connection check finished")) }
    }
    } }
    fun login(login: String, password: String) { with(runtime) {
task {
        clearSession()
        val capabilities = api.capabilities()
        capabilities.requireWorkbench()
        iconViewModel.load(capabilities)
        api.login(login, password)
        val profile = try { api.currentUser() } catch(e: Exception) { api.logout(); throw e }
        mutable.update { it.copy(signedIn = true, profile = profile, mode = AppMode.PLAYER, catalog = Catalog.CHARACTERS, message = tr("Вход выполнен", "Signed in"), tab = 0) }
        val saved = store.filters(state.value.server, Catalog.CHARACTERS.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
        mutable.update { it.copy(filter = saved, query = saved.query) }
        loadPage(0)
    }
    } }
    fun logout() { with(runtime) {
 if(!state.value.busy) clearSession()
    } }
    fun changePassword(current: String, replacement: String) { with(runtime) {
task(writing = true) {
        require(replacement.length in 12..128) { tr("Новый пароль: от 12 до 128 символов", "New password: 12 to 128 characters") }
        val profile = api.currentUser()
        api.changePassword(ChangePasswordCommand(profile.version, current, replacement))
        clearSession()
        mutable.update { it.copy(message = tr("Пароль изменён. Войдите снова.", "The password was changed. Sign in again.")) }
    }
    } }
}
