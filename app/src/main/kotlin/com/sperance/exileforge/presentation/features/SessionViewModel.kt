package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppMode
import kotlinx.coroutines.flow.update

class SessionViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun mode(mode: AppMode) { with(runtime) {
        if (state.value.busy || state.value.editorOpen || mode == AppMode.ADMIN && !state.value.isAdmin) return
        mutable.update { it.copy(mode = mode, catalog = if (mode == AppMode.PLAYER) Catalog.CHARACTERS else Catalog.EQUIPMENT, items = emptyList(), page = 0, tab = 0, filter = CatalogFilter(), query = "") }
        task { restoreFilters(); loadPage(0) }
    } }

    fun serverDraft(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(serverDraft = value) } } }

    fun connect() { with(runtime) { task {
        metadataJob?.cancel()
        val server = normalizeServer(state.value.serverDraft)
        store.save(server)
        clearSession()
        api = newApi(server)
        journal.clear()
        mutable.update { it.copy(server = server, serverDraft = server, health = tr("Проверка соединения…", "Checking the connection…")) }
        val health = api.health()
        mutable.update { it.copy(health = health.toString(), message = tr("Сервер доступен", "The server is reachable")) }
        // A dictionary belongs to its server: the new one names its own things.
        refreshLocale()
    } } }

    fun health() { with(runtime) { task {
        val result = api.health().toString()
        mutable.update { it.copy(health = result, message = tr("Проверка соединения завершена", "Connection check finished")) }
    } } }

    /** The server answers a login with the account document itself: there is no token to keep. */
    fun login(login: String, password: String) { with(runtime) { task {
        clearSession()
        api.capabilities().requireWorkbench()
        val profile = api.login(login, password)
        mutable.update { it.copy(signedIn = true, profile = profile, mode = AppMode.PLAYER, catalog = Catalog.CHARACTERS,
            message = tr("Вход выполнен", "Signed in"), tab = 0) }
        restoreFilters()
        loadPage(0)
        ensureDefinitions()
        // The catalogue is codes without it, and the first attempt may have run before the server was up.
        refreshLocale()
    } } }

    fun logout() { with(runtime) { if (!state.value.busy) clearSession() } }

    fun changePassword(current: String, replacement: String) { with(runtime) { task(writing = true) {
        require(replacement.length in 6..64) { tr("Новый пароль: от 6 до 64 символов", "New password: 6 to 64 characters") }
        require(replacement.any { it.isDigit() } && replacement.any { it.isUpperCase() } && !replacement.contains(' ')) {
            tr("Пароль: цифра, заглавная буква и без пробелов", "Password: a digit, a capital letter and no spaces")
        }
        api.changePassword(current, replacement)
        clearSession()
        mutable.update { it.copy(message = tr("Пароль изменён. Войдите снова.", "The password was changed. Sign in again.")) }
    } } }

    private suspend fun restoreFilters() { with(runtime) {
        val saved = store.filters(state.value.server, state.value.catalog.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
        mutable.update { it.copy(filter = saved, query = saved.query) }
    } }
}
