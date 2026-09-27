package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.TAB_ADMIN
import com.sperance.exileforge.presentation.state.TAB_HERO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun mode(mode: AppMode) { with(runtime) {
        if (state.value.busy || mode == AppMode.ADMIN && !state.value.isAdmin) return
        mutable.update { it.copy(mode = mode, tab = if (mode == AppMode.ADMIN) TAB_ADMIN else TAB_HERO) }
    } }

    fun serverDraft(value: String) = update { it.copy(account = it.account.copy(serverDraft = value)) }

    fun connect() { with(runtime) { task {
        val server = normalizeServer(state.value.account.serverDraft)
        store.save(server)
        clearSession()
        api = newApi(server)
        journal.clear()
        mutable.update { it.copy(account = it.account.copy(server = server, serverDraft = server, health = ui("session.checking")), world = it.world.copy(content = null, contentHash = "")) }
        val health = api.health()
        mutable.update { it.copy(account = it.account.copy(health = health.toString())) }
        refreshLocale()
        refreshIcons()
    } } }

    fun health() { with(runtime) { read(Reads.HEALTH) {
        val result = api.health().toString()
        mutable.update { it.copy(account = it.account.copy(health = result)) }
    } } }

    /** A sign-in answers the account and a token; the token is kept per server for the next launch. */
    fun login(login: String, password: String) { with(runtime) { task {
        clearSession()
        api.manifest().requireWorkbench()
        signedIn(api.login(login, password), byDevice = false)
    } } }

    /** The account this device owns, registered on the way in if the server has never seen it. [silent] is the relaunch path. */
    fun playOnThisDevice(silent: Boolean = false) { with(runtime) { task {
        clearSession()
        try {
            api.manifest().requireWorkbench()
            signedIn(api.loginByDevice(state.value.account.deviceId), byDevice = true)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { if (!silent) throw e }
    } } }

    /** A session kept from an earlier launch; a launch that cannot reach the server keeps the token and says so. */
    fun resume(saved: String) { with(runtime) { task {
        clearSession()
        try {
            api.manifest().requireWorkbench()
            signedIn(api.resume(saved), byDevice = store.deviceSession.first())
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (FailureState.from(e, writing = false) == FailureState.Offline) mutable.update { it.copy(account = it.account.copy(resumable = true)) }
        }
    } } }

    fun retryResume() { with(runtime) { scope.launch {
        val saved = store.token(state.value.account.server)
        if (saved == null) mutable.update { it.copy(account = it.account.copy(resumable = false)) } else resume(saved)
    } } }

    /** What every sign-in ends with: the account is the session, and the gate opens one step — the hero menu. */
    private suspend fun signedIn(profile: UserProfile, byDevice: Boolean) { with(runtime) {
        mutable.update { it.copy(mode = AppMode.PLAYER, phase = AppPhase.CHARACTERS, tab = TAB_HERO, account = it.account.copy(signedIn = true, resumable = false, profile = profile)) }
        store.saveDeviceSession(byDevice)
        store.saveToken(state.value.account.server, api.sessionToken())
        refreshLocale()
        refreshIcons()
        coroutineScope {
            launch { ensureContent() }
            runtime.characterViewModel.readCharacters(autoEnter = true)
        }
    } }

    /** Signing out is explicit, so the next launch must not sign straight back in. */
    fun logout() { with(runtime) {
        if (state.value.busy) return
        val server = state.value.account.server
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
        require(current.isNotEmpty() && replacement.isNotEmpty()) { ui("api.credentials") }
        api.changePassword(current, replacement)
    } } }
}
