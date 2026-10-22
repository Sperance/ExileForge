package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.feedback.ReportTrail
import com.sperance.exileforge.core.hero.HeroCopy
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.ClientIdentity
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandStore
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.HttpLink
import com.sperance.exileforge.core.network.ManifestCache
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.network.ServerAlarm
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.WorldLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionActions(
    repositories: Repositories,
    actions: Actions,
    commands: CommandRunner,
    connection: ServerConnection,
    store: DeviceStore,
    scope: CoroutineScope,
    private val screens: Screens,
    private val notices: Notices,
    private val loader: WorldLoader,
    private val journal: RequestJournal,
    private val lazyConnection: Lazy<ConnectionActions>,
    private val lazyCharacters: Lazy<CharacterActions>,
    private val lazyWarmup: Lazy<WarmupActions>,
    /** HTTP-клиент процесса (из Koin): все серверы делят его пул и TLS-сессии. */
    private val http: HttpLink,
    private val trace: StartupTrace,
    /** Сборка и среда устройства для отчётов. */
    private val device: ClientDevice,
    /** Переходы экранов и последние действия для отчётов. */
    private val trail: ReportTrail,
) : AppService(repositories, actions, commands, connection, store, scope) {
    private val connectionActions: ConnectionActions get() = lazyConnection.value
    private val characterActions: CharacterActions get() = lazyCharacters.value
    private val warmupActions: WarmupActions get() = lazyWarmup.value

    /** Подключение к адресу [draft] из поля экрана (черновик живёт на экране). */
    fun connect(draft: String) = connectTo(normalizeServer(draft))

    /** Back to the one server: the administrator's address is forgotten, not overwritten with the default. */
    fun resetServer() = connectTo(null)

    private fun connectTo(override: String?) {
        run {
            task {
                store.save(override)
                val server = override ?: DEFAULT_SERVER
                clearSession()
                connectionActions.reset()
                api = newApi(server)
                journal.clear()
                sessions.update { it.copy(server = server, health = ui("session.checking")) }
                world.update { it.copy(content = null, contentHash = "") }
                val health = api.health()
                sessions.update { it.copy(health = health.toString()) }
                loader.refreshLocale()
                loader.refreshIcons()
            }
        }
    }

    fun health() {
        run {
            read(Reads.HEALTH) {
                val result = api.health().toString()
                sessions.update { it.copy(health = result) }
            }
        }
    }

    /** A sign-in answers the account and a token; the token is kept per server for the next launch. */
    fun login(login: String, password: String) {
        run {
            task {
                clearSession()
                api.workbench()
                signedIn(api.login(login, password), byDevice = false)
            }
        }
    }

    /** The account this device owns, registered on the way in if the server has never seen it. [silent] is the relaunch path. */
    fun playOnThisDevice(silent: Boolean = false) {
        run {
            task {
                clearSession()
                try {
                    trace.step(StartStage.SESSION, "start.step.workbench") { api.workbench() }
                    val server = sessions.state.value.server
                    val profile = trace.step(StartStage.SESSION, "start.step.device") { api.loginByDevice(store.deviceSecret(server), store.deviceFingerprint()) }
                    api.deviceSecret?.let { store.saveDeviceSecret(server, it) }
                    signedIn(profile, byDevice = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (!silent) throw e
                }
            }
        }
    }

    /** A session kept from an earlier launch; a launch that cannot reach the server keeps the token and says so. */
    fun resume(saved: String) {
        run {
            task {
                unconfirmed = null
                clearSession()
                try {
                    trace.step(StartStage.SESSION, "start.step.workbench") { api.workbench() }
                    signedIn(trace.step(StartStage.SESSION, "start.step.resume") { api.resume(saved) }, byDevice = store.deviceSession.first())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (FailureState.from(e, writing = false) is FailureState.Offline) {
                        sessions.update { it.copy(resumable = true) }
                        connectionActions.lost(e)
                    }
                }
            }
        }
    }

    /** The kept token the fast start drew the hero with, until the server confirms it. */
    private var unconfirmed: String? = null

    /**
     * The fast start: with a kept session, the last hero played on this server opens straight from the
     * device — their copy, the content and the dictionary — and the session is confirmed behind it; commands
     * given meanwhile wait in the queue. No copy, another API revision or content missing: `false`, the usual start.
     *
     * Лист героя складывается вне главного потока: без ожидания рисунка герой здесь ещё пуст, и быстрый старт падал бы в обычный вход.
     */
    suspend fun fastStart(server: String, saved: String): Boolean {
        run {
            val (heroId, copy) = trace.step(StartStage.SESSION, "start.step.copy") {
                store.lastHero(server)?.let { heroId -> heroCopy(server, heroId)?.let { heroId to it } }
            } ?: return false
            if (copy.revision != API_REVISION || !loader.contentFromDevice()) return false
            api.adopt(saved, copy.account)
            sessions.update { it.copy(signedIn = true, resumable = false, profile = copy.account) }
            heroes.select(heroId)
            expeditions.clear()
            screens.reset(Landing.HERO)
            warmupActions.clear()
            heroSync.restore(heroId, copy.snapshot)
            heroSync.drawn()
            val foreign = heroes.state.value.owner.let { it.isNotEmpty() && it != copy.account.id }
            if (heroes.state.value.hero == null || foreign) {
                clearSession()
                if (foreign) store.saveLastHero(server, null)
                return false
            }
            unconfirmed = saved
            confirm()
            return true
        }
    }

    private suspend fun heroCopy(server: String, heroId: String): HeroCopy? = store.heroCopy(server, heroId)?.let { text ->
        withContext(Dispatchers.Default) { runCatching { WireJson.decodeFromString(HeroCopy.serializer(), text) }.getOrNull() }
    }

    /**
     * The adopted session, asked of the server in the background. Out of reach, the hero stays on screen and the
     * link's probe asks again ([restored]); a 401 is the transport's to handle; anything else — another revision,
     * a disabled account, a token that turns out to be another account's — falls back to the usual start.
     */
    private fun confirm() {
        run {
            if (confirming?.isActive == true) return
            confirming = scope.launch { confirmNow() }
        }
    }

    private var confirming: Job? = null

    /** Сохранённый токен ответил за другой аккаунт, чем нарисованный герой: тот герой не этой сессии. */
    private suspend fun confirmNow() {
        run {
            val saved = unconfirmed ?: return
            val adopted = api.currentUser()?.id
            val profile = try {
                api.workbench()
                api.confirm()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when {
                    FailureState.from(e, writing = false) is FailureState.Offline -> connectionActions.lost(e)
                    e is ApiFailure && e.status == 401 -> unconfirmed = null
                    else -> fallBack(saved, foreign = false)
                }
                return
            }
            if (profile.id != adopted || heroes.state.value.owner.let { it.isNotEmpty() && it != profile.id }) return fallBack(saved, foreign = true)
            unconfirmed = null
            sessions.update { it.copy(profile = profile) }
            read(Reads.CHARACTERS, silent = true) { characterActions.readCharacters() }
            read(Reads.HERO, silent = true) {
                loader.ensureContent(fresh = true)
                heroSync.readHero()
                resumeRuns(heroId)
            }
            connectionActions.wake(now = true)
        }
    }

    /**
     * The fast start gives way to the usual one. The adopted session goes at once — nothing is held for it any
     * more — and the resume waits for a running task to end instead of being refused by it; a sign-in the player
     * made meanwhile stands. [foreign]: the last hero was another account's and is not opened again.
     */
    private suspend fun fallBack(saved: String, foreign: Boolean) {
        run {
            unconfirmed = null
            val server = sessions.state.value.server
            clearSession()
            if (foreign) store.saveLastHero(server, null)
            commands.state.first { !it.busy }
            if (!sessions.state.value.signedIn) resume(saved)
        }
    }

    /**
     * The server answers again: the session waiting for it is confirmed, or a kept one that could not resume is tried again.
     *
     * Связь вернулась: сессия, принятая быстрым стартом, подтверждается сейчас.
     */
    fun restored() {
        if (unconfirmed != null) {
            confirm()
        } else {
            if (!sessions.state.value.signedIn && sessions.state.value.resumable && !commands.state.value.busy) retryResume()
        }
    }

    fun retryResume() {
        run {
            scope.launch {
                val saved = store.token(sessions.state.value.server)
                if (saved == null) sessions.update { it.copy(resumable = false) } else resume(saved)
            }
        }
    }

    /** When the app left the foreground, by the monotonic clock; null while it is in front. */
    @Volatile private var awaySince: Long? = null

    /** Leaving the foreground: the moment is kept, so the return knows how long the screen stood still. */
    fun away() {
        awaySince = System.nanoTime()
    }

    /**
     * Back in the foreground — from the background or a locked screen: the dead sockets go, and what the
     * last try could not reach is asked again — the kept session, or the screen the player is on. A
     * return after more than [STALE_AWAY_MS] re-reads the screen quietly even without a failure: the session is
     * checked by that very read, and the screen shows what happened while the app was away.
     *
     * Связь проверяется при каждом возврате, а не первым запросом экрана: живая по старой памяти, она могла умереть в фоне. Следом
     * перечитывается экран - оно и проверяет связь, отдельной пробы нет; чтение, повисшее на умершем сокете, заменяется новым.
     */
    fun reconnect() {
        run {
            val stale = awaySince?.let { (System.nanoTime() - it) / 1_000_000 > STALE_AWAY_MS } == true
            awaySince = null
            scope.launch {
                withContext(Dispatchers.IO) { http.dropIdleConnections() }
                val session = sessions.state.value
                val link = links.state.value
                val reread = session.signedIn && screens.spot.phase != AppPhase.AUTH &&
                    (commands.state.value.failure is FailureState.Offline || link.offline || stale)
                if (session.signedIn) connectionActions.wake(now = true, verify = !reread)
                when {
                    !session.signedIn -> if (session.resumable) retryResume()

                    reread -> {
                        commands.clearOffline()
                        when (screens.spot.phase) {
                            AppPhase.GAME -> read(Reads.HERO, restart = true, silent = true) { heroSync.readHero() }
                            AppPhase.CHARACTERS -> read(Reads.CHARACTERS, restart = true, silent = true) { characterActions.readCharacters() }
                            AppPhase.AUTH -> Unit
                        }
                    }
                }
            }
        }
    }

    /**
     * Касание красного облака: полное переподключение сразу - как возврат в игру, но проба связи без паузы повтора.
     * До её итога облако «переподключается»; удача - связь в сети, неудача - снова красное облако и тост с причиной.
     */
    fun reconnectLink() {
        links.update { it.reconnect() }
        connectionActions.wake(now = true, verify = true)
        reconnect()
    }

    /**
     * What every sign-in ends with: the account is the session, and the gate opens one step — the hero menu.
     *
     * Маршруты клиента - у вошедшего: без входа манифест их не называет. Что ждало сессию, уходит с этой; команды другого аккаунта
     * отбрасываются.
     */
    private suspend fun signedIn(profile: UserProfile, byDevice: Boolean) {
        run {
            api.requireRoutes()
            sessions.update { it.copy(signedIn = true, resumable = false, profile = profile) }
            screens.reset(Landing.CHARACTERS)
            store.saveDeviceSession(byDevice)
            store.saveToken(sessions.state.value.server, api.sessionToken())
            forgetForeignHero(sessions.state.value.server, profile.id)
            connectionActions.wake(now = true)
            loader.refreshLocale()
            loader.refreshIcons()
            coroutineScope {
                launch { loader.ensureContent() }
                trace.step(StartStage.SESSION, "start.step.characters") { characterActions.readCharacters(autoEnter = true) }
            }
        }
    }

    /** The last hero of another account is not the one this account's next launch opens. */
    private suspend fun forgetForeignHero(server: String, account: String) {
        run {
            val heroId = store.lastHero(server) ?: return
            if (heroCopy(server, heroId)?.account?.id != account) store.saveLastHero(server, null)
        }
    }

    /**
     * Signing out is explicit, so the next launch must not sign straight back in.
     *
     * Выход явный: что этот аккаунт оставил в ожидании, для следующего не отправляется.
     */
    fun logout() {
        run {
            if (commands.state.value.busy) return
            val server = sessions.state.value.server
            val leaving = api
            val token = leaving.sessionToken()
            unconfirmed = null
            clearSession()
            scope.launch {
                store.saveDeviceSession(false)
                store.saveToken(server, null)
                store.saveLastHero(server, null)
                leaving.commands?.clear()
                token?.let { leaving.revoke(it) }
            }
        }
    }

    /**
     * Гостевой аккаунт получает логин и пароль: ответ - аккаунт с логином, он и становится профилем
     * сессии; токен и вход по устройству остаются. Занятый раннер не глотает нажатие: «занято, повторите».
     */
    fun bind(login: String, password: String) {
        val started = task(writing = true) {
            val profile = api.bind(login, password)
            sessions.update { it.copy(profile = profile) }
            notices.toast(ui("account.bound", profile.login))
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    fun changePassword(current: String, replacement: String) {
        run {
            task(writing = true) {
                require(current.isNotEmpty() && replacement.isNotEmpty()) { ui("api.credentials") }
                api.changePassword(current, replacement)
            }
        }
    }

    private companion object {
        /** How long away makes the screen stale enough to read again on return. */
        const val STALE_AWAY_MS = 30_000L
    }

    /** Первый [api] создан: проверка обновлений ждёт его, а не спрашивает никакой сервер. */
    val apiReady = CompletableDeferred<Unit>()

    /** Вход встретил сервер новее сборки: проверка обновлений идёт сразу. */
    val newerServer = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Сервер ответил ошибкой или не ответил вовсе: проверка обновлений с порогом. */
    val serverFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * A refused token is forgotten, and a player who plays by device is signed in again without being
     * asked. The sign-in waits for the command that met the 401 to finish, because [task] refuses to nest.
     *
     * Копия последнего героя принадлежит отказанной сессии - следующий запуск её не открывает. Проверку сборки поднимают 426 сервера, сбой
     * сервера (5xx, битый ответ) и обрыв без ответа; отказ правила (4xx) - слово сервера, не повод искать обновление. Отчёты подписаны
     * устройством, версией и активным героем; заблокированный герой уводит к выбору, закрытый санкцией доступ гасит сессию и показывает
     * экран санкции. Команды сервера, что ждут сети, живут на устройстве.
     */
    fun newApi(server: String): GameApi {
        lateinit var created: GameApi
        created = GameApi(server, journal, http, onUnauthorized = {
            if (connection.ready && api === created) {
                clearSession()
                scope.launch {
                    store.saveToken(server, null)
                    store.saveLastHero(server, null)
                    if (store.deviceSession.first()) {
                        commands.state.first { !it.busy }
                        if (api === created) playOnThisDevice(silent = true)
                    } else {
                        commands.refuse(phrase("runtime.session_expired"))
                    }
                }
            }
        })
        created.heroSync(heroSync::heldParts, heroSync::delivered)
        created.onNewerServer = { newerServer.tryEmit(Unit) }
        created.onServerError = { alarm ->
            when (alarm) {
                ServerAlarm.OUTDATED -> newerServer.tryEmit(Unit)
                ServerAlarm.FAILED, ServerAlarm.UNREACHED -> serverFailed.tryEmit(Unit)
            }
        }
        created.identity = ClientIdentity(device.version, store::deviceFingerprint, { heroes.heroId }, device.model, device::environment)
        created.trail = trail
        created.onHeroBlocked = { heroId, failure -> if (api === created) characterActions.blocked(heroId, failure) }
        created.onSanctioned = { sanctionId, token -> if (api === created) sanctioned(server, sanctionId, token) }
        created.manifestCache = object : ManifestCache {
            override suspend fun read(): String? = store.manifest(server)
            override suspend fun write(text: String) = store.saveManifest(server, text)
        }
        created.commandStore(object : CommandStore {
            override suspend fun read(): String? = store.commands(server)
            override suspend fun write(text: String) = store.saveCommands(server, text)
        })
        connectionActions.attach(created)
        return created
    }

    /**
     * Сервер закрыл доступ санкцией (`AUTH_006`): бан аккаунта или устройства, удаление аккаунта. Сессия и её токен
     * забываются, приложение уходит ко входу, а поверх - экран санкции; повторный отказ, пока экран открыт, его не перезапускает.
     * [token] - токен апелляции из отказа: экран санкции подаёт с ним апелляцию.
     */
    private fun sanctioned(server: String, sanctionId: String, token: String) {
        if (sessions.state.value.notice != null) return
        clearSession()
        sessions.update { it.copy(notice = sanctionId, noticeToken = token) }
        scope.launch {
            store.saveToken(server, null)
            store.saveLastHero(server, null)
        }
    }

    fun clearSession() {
        api.logout()
        screens.reset(Landing.AUTH)
        journal.clear()
        cancelReads()
        expedition.drop()
        trial.drop()
        crafts.drop()
        heroSync.forget()
        sessions.clear()
        feedbacks.clear()
        heroes.clear()
        boards.clear()
        story.clear()
        markets.clear()
        guilds.clear()
        commands.clearFailure()
        admins.clear()
        warmupActions.clear()
    }
}
