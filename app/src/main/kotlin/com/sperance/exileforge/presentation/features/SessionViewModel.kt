package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.PlayState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.TAB_ADMIN
import com.sperance.exileforge.presentation.state.TAB_HERO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun mode(mode: AppMode) { with(runtime) {
        if (state.value.busy || mode == AppMode.ADMIN && !(BuildConfig.DEBUG && state.value.isAdmin)) return
        mutable.update { it.copy(mode = mode, tab = if (mode == AppMode.ADMIN) TAB_ADMIN else TAB_HERO) }
    } }

    fun serverDraft(value: String) = update { it.copy(account = it.account.copy(serverDraft = value)) }

    fun connect() { with(runtime) { task {
        val server = normalizeServer(state.value.account.serverDraft)
        store.save(server)
        clearSession()
        connectionViewModel.reset()
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
        unconfirmed = null
        clearSession()
        try {
            api.manifest().requireWorkbench()
            signedIn(api.resume(saved), byDevice = store.deviceSession.first())
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (FailureState.from(e, writing = false) == FailureState.Offline) mutable.update { it.copy(account = it.account.copy(resumable = true)) }
        }
    } } }

    /** The kept token the fast start drew the hero with, until the server confirms it. */
    private var unconfirmed: String? = null

    /**
     * The fast start (3.30.0): with a kept session, the last hero played on this server opens straight from the
     * device — their copy, the content and the dictionary — and the session is confirmed behind it; commands
     * given meanwhile wait in the queue. No copy, another API revision or content missing: `false`, the usual start.
     */
    suspend fun fastStart(server: String, saved: String): Boolean { with(runtime) {
        val heroId = store.lastHero(server) ?: return false
        val copy = heroCopy(server, heroId) ?: return false
        if (copy.revision != API_REVISION || !contentFromDevice()) return false
        api.adopt(saved, copy.account)
        mutable.update { it.copy(mode = AppMode.PLAYER, phase = AppPhase.GAME, tab = TAB_HERO,
            account = it.account.copy(signedIn = true, resumable = false, profile = copy.account),
            play = PlayState(heroId = heroId, draftClass = it.play.draftClass, selectedOrb = it.play.selectedOrb)) }
        heroViewModel.restore(heroId, copy.snapshot)
        val foreign = state.value.play.heroOwner.let { it.isNotEmpty() && it != copy.account.id }
        if (state.value.hero == null || foreign) {
            clearSession()
            if (foreign) store.saveLastHero(server, null)
            return false
        }
        unconfirmed = saved
        confirm()
        return true
    } }

    private suspend fun heroCopy(server: String, heroId: String): HeroCopy? = runtime.store.heroCopy(server, heroId)?.let { text ->
        withContext(Dispatchers.Default) { runCatching { WireJson.decodeFromString(HeroCopy.serializer(), text) }.getOrNull() }
    }

    /**
     * The adopted session, asked of the server in the background. Out of reach, the hero stays on screen and the
     * link's probe asks again ([restored]); a 401 is the transport's to handle; anything else — another revision,
     * a disabled account, a token that turns out to be another account's — falls back to the usual start.
     */
    private fun confirm() { with(runtime) {
        if (confirming?.isActive == true) return
        confirming = scope.launch { confirmNow() }
    } }

    private var confirming: Job? = null

    private suspend fun confirmNow() { with(runtime) {
        val saved = unconfirmed ?: return
        val adopted = api.currentUser()?.id
        val profile = try {
            api.manifest(fresh = true).requireWorkbench()
            api.confirm()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            when {
                FailureState.from(e, writing = false) == FailureState.Offline -> connectionViewModel.lost()
                e is ApiFailure && e.status == 401 -> unconfirmed = null
                else -> fallBack(saved, foreign = false)
            }
            return
        }
        // The kept token answered for another account than the hero drawn: that hero is not this session's.
        if (profile.id != adopted || state.value.play.heroOwner.let { it.isNotEmpty() && it != profile.id }) return fallBack(saved, foreign = true)
        unconfirmed = null
        mutable.update { it.copy(account = it.account.copy(profile = profile)) }
        read(Reads.CHARACTERS, silent = true) { characterViewModel.readCharacters() }
        read(Reads.HERO, silent = true) {
            ensureContent(fresh = true)
            heroViewModel.readHero()
            expeditionViewModel.resume(heroId)
        }
        connectionViewModel.wake(now = true)
    } }

    /**
     * The fast start gives way to the usual one. The adopted session goes at once — nothing is held for it any
     * more — and the resume waits for a running task to end instead of being refused by it; a sign-in the player
     * made meanwhile stands. [foreign]: the last hero was another account's and is not opened again.
     */
    private suspend fun fallBack(saved: String, foreign: Boolean) { with(runtime) {
        unconfirmed = null
        val server = state.value.account.server
        clearSession()
        if (foreign) store.saveLastHero(server, null)
        state.first { !it.busy }
        if (!state.value.account.signedIn) resume(saved)
    } }

    /** The link came back: a session the fast start adopted is confirmed now. */
    fun restored() { if (unconfirmed != null) confirm() }

    fun retryResume() { with(runtime) { scope.launch {
        val saved = store.token(state.value.account.server)
        if (saved == null) mutable.update { it.copy(account = it.account.copy(resumable = false)) } else resume(saved)
    } } }

    /** When the app left the foreground, by the monotonic clock; null while it is in front. */
    @Volatile private var awaySince: Long? = null

    /** Leaving the foreground (3.28.0): the moment is kept, so the return knows how long the screen stood still. */
    fun away() { awaySince = System.nanoTime() }

    /**
     * Back in the foreground — from the background or a locked screen: the dead sockets go, and what the
     * last try could not reach is asked again — the kept session, or the screen the player is on. Since 3.28.0 a
     * return after more than [STALE_AWAY_MS] re-reads the screen quietly even without a failure: the session is
     * checked by that very read, and the screen shows what happened while the app was away.
     */
    fun reconnect() { with(runtime) {
        val stale = awaySince?.let { (System.nanoTime() - it) / 1_000_000 > STALE_AWAY_MS } == true
        awaySince = null
        scope.launch {
            withContext(Dispatchers.IO) { ForgeHttp.dropIdleConnections() }
            val now = state.value
            when {
                !now.account.signedIn -> if (now.account.resumable) retryResume()
                now.failure == FailureState.Offline || now.link.offline || stale -> {
                    if (now.failure == FailureState.Offline) mutable.update { it.copy(failure = null, message = null, error = false) }
                    // The probe waits no longer: the link is asked again with the screen.
                    if (now.link.offline || now.link.waiting.isNotEmpty()) connectionViewModel.wake(now = true)
                    when (now.phase) {
                        AppPhase.GAME -> read(Reads.HERO, silent = true) { heroViewModel.readHero() }
                        AppPhase.CHARACTERS -> read(Reads.CHARACTERS, silent = true) { characterViewModel.readCharacters() }
                        AppPhase.AUTH -> Unit
                    }
                }
            }
        }
    } }

    /** What every sign-in ends with: the account is the session, and the gate opens one step — the hero menu. */
    private suspend fun signedIn(profile: UserProfile, byDevice: Boolean) { with(runtime) {
        mutable.update { it.copy(mode = AppMode.PLAYER, phase = AppPhase.CHARACTERS, tab = TAB_HERO, account = it.account.copy(signedIn = true, resumable = false, profile = profile)) }
        store.saveDeviceSession(byDevice)
        store.saveToken(state.value.account.server, api.sessionToken())
        forgetForeignHero(state.value.account.server, profile.id)
        // What waited for a session goes out with this one; another account's commands are dropped on the way.
        connectionViewModel.wake(now = true)
        refreshLocale()
        refreshIcons()
        coroutineScope {
            launch { ensureContent() }
            runtime.characterViewModel.readCharacters(autoEnter = true)
        }
    } }

    /** The last hero of another account is not the one this account's next launch opens. */
    private suspend fun forgetForeignHero(server: String, account: String) { with(runtime) {
        val heroId = store.lastHero(server) ?: return
        if (heroCopy(server, heroId)?.account?.id != account) store.saveLastHero(server, null)
    } }

    /** Signing out is explicit, so the next launch must not sign straight back in. */
    fun logout() { with(runtime) {
        if (state.value.busy) return
        val server = state.value.account.server
        val leaving = api
        val token = leaving.sessionToken()
        unconfirmed = null
        clearSession()
        scope.launch {
            store.saveDeviceSession(false)
            store.saveToken(server, null)
            store.saveLastHero(server, null)
            // A sign-out is explicit: what this account left waiting is not sent for the next one.
            leaving.commands?.clear()
            token?.let { leaving.revoke(it) }
        }
    } }

    fun changePassword(current: String, replacement: String) { with(runtime) { task(writing = true) {
        require(current.isNotEmpty() && replacement.isNotEmpty()) { ui("api.credentials") }
        api.changePassword(current, replacement)
    } } }

    private companion object {
        /** How long away makes the screen stale enough to read again on return. */
        const val STALE_AWAY_MS = 30_000L
    }
}
