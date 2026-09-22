package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.TAB_ADMIN
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.AppPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun mode(mode: AppMode) { with(runtime) {
        if (state.value.busy || state.value.editorOpen || mode == AppMode.ADMIN && !state.value.isAdmin) return
        // Dropping the tools closes the tabs that come with them, so the switch lands on the hero
        // rather than on a screen that is about to refuse to draw.
        mutable.update { it.copy(mode = mode, catalog = Catalog.EQUIPMENT, items = emptyList(), page = 0,
            tab = if (mode == AppMode.ADMIN) TAB_ADMIN else TAB_HERO, filter = CatalogFilter(), query = "") }
        task { restoreFilters(); loadPage(0) }
    } }

    fun serverDraft(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(serverDraft = value) } } }

    fun connect() { with(runtime) { task {
        val server = normalizeServer(state.value.serverDraft)
        store.save(server)
        clearSession()
        api = newApi(server)
        journal.clear()
        mutable.update { it.copy(server = server, serverDraft = server, health = tr("Проверка соединения…", "Checking the connection…")) }
        val health = api.health()
        mutable.update { it.copy(health = health.toString(), message = tr("Сервер доступен", "The server is reachable")) }
        // A dictionary and an icon set belong to their server: the new one has its own.
        refreshLocale()
        refreshIcons()
    } } }

    fun health() { with(runtime) { task {
        val result = api.health().toString()
        mutable.update { it.copy(health = result, message = tr("Проверка соединения завершена", "Connection check finished")) }
    } } }

    /** The server answers a login with the account document itself: there is no token to keep. */
    fun login(login: String, password: String) { with(runtime) { task {
        clearSession()
        api.capabilities().requireWorkbench()
        signedIn(api.login(login, password), byDevice = false)
    } } }

    /**
     * The account this device owns, registered on the way in if the server has never seen it.
     *
     * No credentials are typed and none are stored: the identifier *is* the account, which is why
     * losing it loses the characters. [silent] is the relaunch path — it must not leave an error
     * banner over the sign-in screen the player is already looking at.
     */
    fun playOnThisDevice(silent: Boolean = false) { with(runtime) { task {
        clearSession()
        try {
            api.capabilities().requireWorkbench()
            signedIn(api.loginByDevice(state.value.deviceId), byDevice = true)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { if (!silent) throw e }
    } } }

    /**
     * What every sign-in ends with: the account is the session, and the gate opens one step.
     *
     * The character is never chosen here. Which characters exist is the next screen's question,
     * and it is the same question for a player and for an administrator — the editor and the
     * checks are reached from inside the game, so everyone passes through the menu.
     */
    private suspend fun signedIn(profile: com.sperance.exileforge.core.model.command.UserProfile, byDevice: Boolean) { with(runtime) {
        mutable.update { it.copy(signedIn = true, profile = profile, mode = AppMode.PLAYER, catalog = Catalog.EQUIPMENT,
            phase = AppPhase.CHARACTERS, message = tr("Вход выполнен", "Signed in"), tab = 0) }
        store.saveDeviceSession(byDevice)
        restoreFilters()
        ensureDefinitions()
        // The catalogue is codes without it, and the first attempt may have run before the server was up.
        refreshLocale()
        refreshIcons()
        // The one place a single character is entered without being chosen: arriving is not leaving.
        runtime.characterViewModel.readCharacters(autoEnter = true)
    } }

    /** Signing out is explicit, so the next launch must not sign straight back in. */
    fun logout() { with(runtime) {
        if (state.value.busy) return
        clearSession()
        scope.launch { store.saveDeviceSession(false) }
    } }

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
