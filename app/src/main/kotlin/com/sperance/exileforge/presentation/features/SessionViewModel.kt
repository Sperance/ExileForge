package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.TAB_ADMIN
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.AppPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
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
        mutable.update { it.copy(server = server, serverDraft = server, health = ui("session.checking")) }
        val health = api.health()
        mutable.update { it.copy(health = health.toString(), message = ui("session.reachable")) }
        // A dictionary and an icon set belong to their server: the new one has its own.
        refreshLocale()
        refreshIcons()
    } } }

    fun health() { with(runtime) { task {
        val result = api.health().toString()
        mutable.update { it.copy(health = result, message = ui("session.check_done")) }
    } } }

    /** A sign-in answers the account and a token; the token is kept per server for the next launch. */
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
     * A session kept from an earlier launch. Silent like the device path: a launch that cannot
     * reach the server leaves the token in place for the next one and shows the sign-in screen,
     * and a token the server refused is handled by `newApi`'s 401 path rather than here.
     */
    fun resume(saved: String) { with(runtime) { task {
        clearSession()
        try {
            api.capabilities().requireWorkbench()
            signedIn(api.resume(saved), byDevice = store.deviceSession.first())
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {}
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
            phase = AppPhase.CHARACTERS, message = ui("session.signed_in"), tab = 0) }
        store.saveDeviceSession(byDevice)
        store.saveToken(state.value.server, api.sessionToken())
        restoreFilters()
        ensureDefinitions()
        // The catalogue is codes without it, and the first attempt may have run before the server was up.
        refreshLocale()
        refreshIcons()
        // The one place a single character is entered without being chosen: arriving is not leaving.
        runtime.characterViewModel.readCharacters(autoEnter = true)
    } }

    /**
     * Signing out is explicit, so the next launch must not sign straight back in. The token is
     * taken before the local session drops it, and revoked on the server without waiting.
     */
    fun logout() { with(runtime) {
        if (state.value.busy) return
        val server = state.value.server
        val leaving = api
        val token = leaving.sessionToken()
        clearSession()
        scope.launch {
            store.saveDeviceSession(false)
            store.saveToken(server, null)
            token?.let { leaving.revoke(it) }
        }
    } }

    fun changePassword(current: String, replacement: String) { with(runtime) { task(writing = true) {
        require(replacement.length in 6..64) { ui("session.new_password_rule") }
        require(replacement.any { it.isDigit() } && replacement.any { it.isUpperCase() } && !replacement.contains(' ')) {
            ui("session.password_rule")
        }
        // Every other session of the account ends; this one stays, so there is nothing to sign into again.
        api.changePassword(current, replacement)
        mutable.update { it.copy(message = ui("session.password_changed")) }
    } } }

    private suspend fun restoreFilters() { with(runtime) {
        val saved = store.filters(state.value.server, state.value.catalog.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
        mutable.update { it.copy(filter = saved, query = saved.query) }
    } }
}
