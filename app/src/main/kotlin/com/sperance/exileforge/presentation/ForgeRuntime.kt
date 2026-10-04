package com.sperance.exileforge.presentation

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.PortraitBundle
import com.sperance.exileforge.core.display.PortraitSvg
import com.sperance.exileforge.core.display.serverIcons
import com.sperance.exileforge.core.display.serverPortraits
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.model.sync.StaticManifest
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueued
import com.sperance.exileforge.core.network.CommandStore
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.ManifestCache
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.network.refusalLine
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.data.settings.deviceLanguage
import com.sperance.exileforge.presentation.features.CharacterViewModel
import com.sperance.exileforge.presentation.features.ConnectionViewModel
import com.sperance.exileforge.presentation.features.RedemptionViewModel
import com.sperance.exileforge.presentation.features.SessionViewModel
import com.sperance.exileforge.presentation.state.ADMIN_TABS
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.presentation.state.Notice
import com.sperance.exileforge.presentation.state.NoticeKind
import com.sperance.exileforge.presentation.state.Phrase
import com.sperance.exileforge.presentation.state.StashSort
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.TAB_SETTINGS
import com.sperance.exileforge.presentation.state.phrase
import com.sperance.exileforge.rules.content.ContentFiles
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ForgeRuntime(
    val store: ServerStore,
    val journal: RequestJournal,
    val prefs: com.sperance.exileforge.data.settings.PreferencesRepository,
    val connection: com.sperance.exileforge.core.session.ServerConnection,
    val commands: CommandRunner,
    private val connectionHub: com.sperance.exileforge.core.session.ConnectionEventsHub,
    val notices: com.sperance.exileforge.core.session.Notices,
    val events: com.sperance.exileforge.core.session.GameEvents,
    private val content: com.sperance.exileforge.core.world.ContentLoader,
    private val buzzer: com.sperance.exileforge.core.session.Buzzes,
    private val repositories: Repositories,
    private val actions: Actions,
    val navigator: com.sperance.exileforge.presentation.nav.Navigator,
    private val loader: com.sperance.exileforge.presentation.world.WorldLoader,
) {
    val sessions get() = repositories.sessions
    val world get() = repositories.world
    val heroes get() = repositories.heroes
    val boards get() = repositories.boards
    val markets get() = repositories.markets
    val guilds get() = repositories.guilds
    val feedbacks get() = repositories.feedbacks
    val craftsRepository get() = repositories.crafts
    val quests get() = actions.quests
    val market get() = actions.market
    val guild get() = actions.guild
    val crafts get() = actions.crafts
    val hero get() = actions.hero
    val heroSync get() = actions.heroSync
    val expedition get() = actions.expedition
    val trial get() = actions.trial
    val expeditions get() = repositories.expeditions
    val languages get() = repositories.languages
    val links get() = repositories.links
    val admins get() = repositories.admins
    val modes get() = repositories.modes
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val logs = journal.entries
    var api: GameApi
        get() = connection.api
        set(value) = connection.set(value)

    /** The first [api] is made (3.74.0): the update check waits for it rather than asking no server at all. */
    val apiReady = kotlinx.coroutines.CompletableDeferred<Unit>()

    /** A sign-in met a server newer than this build (3.74.0): the update check runs at once. */
    val newerServer = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionViewModel = SessionViewModel(this)
    val redemptionViewModel = RedemptionViewModel(this)
    val characterViewModel = CharacterViewModel(this)
    val warmupViewModel = com.sperance.exileforge.presentation.features.WarmupViewModel(this)
    val connectionViewModel = ConnectionViewModel(this)

    /**
     * A refused token is forgotten, and a player who plays by device is signed in again without being
     * asked. The sign-in waits for the command that met the 401 to finish, because [task] refuses to nest.
     */
    fun newApi(server: String): GameApi {
        lateinit var created: GameApi
        created = GameApi(server, journal, onUnauthorized = {
            if (connection.ready && api === created) {
                clearSession()
                scope.launch {
                    store.saveToken(server, null)
                    // The copy of the last hero belongs to the refused session: the next launch does not open it.
                    store.saveLastHero(server, null)
                    if (store.deviceSession.first()) {
                        commands.state.first { !it.busy }
                        if (api === created) sessionViewModel.playOnThisDevice(silent = true)
                    } else {
                        commands.refuse(phrase("runtime.session_expired"))
                    }
                }
            }
        })
        created.heroSync(heroSync::heldParts, heroSync::delivered)
        created.onNewerServer = { newerServer.tryEmit(Unit) }
        created.manifestCache = object : ManifestCache {
            override suspend fun read(): String? = store.manifest(server)
            override suspend fun write(text: String) = store.saveManifest(server, text)
        }
        // The commands of this server that wait for the network live on the device (3.30.0).
        created.commandStore(object : CommandStore {
            override suspend fun read(): String? = store.commands(server)
            override suspend fun write(text: String) = store.saveCommands(server, text)
        })
        connectionViewModel.attach(created)
        return created
    }

    /**
     * Запуск после сборки (3.80.12): отражения репозиториев и старт сессии. Из конструктора корутины не запускаются -
     * R8 в release-сборке переносил запись полей конструктора ниже лямбд, что их читали, и приложение падало на старте.
     */
    private var started = false

    fun start() {
        if (started) return
        started = true
        connectionHub.delegate = connectionViewModel
        expedition.start()
        trial.start()
        buzzer.allowed = { kind -> prefs.settings.value.let { if (kind == Buzz.DANGER) it.buzzDanger else it.buzzButtons } }
        content.delegate = { fresh -> ensureContent(fresh) }
        // Герой изменился на сервере по чужой команде: перечитывается тихо, отказ остаётся команде, что его просила.
        scope.launch {
            events.heroChanged.collect {
                try {
                    heroSync.readHero()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) { }
            }
        }
        scope.launch {
            try {
                languages.set(Lang.byCode(store.language.first()) ?: deviceLanguage())
                val server = store.server.first()
                api = newApi(server)
                apiReady.complete(Unit)
                val known = store.languages(server).mapNotNull { Lang.byCode(it) }
                sessions.update { it.copy(server = server) }
                world.update { it.copy(languages = known.ifEmpty { it.languages }) }
                commands.ready()
                refreshLocale()
                refreshIcons()
                val saved = store.token(server)
                // The fast start (3.30.0): the last hero from the device at once, the session confirmed behind it.
                if (saved != null) {
                    if (!sessionViewModel.fastStart(server, saved)) sessionViewModel.resume(saved)
                } else if (store.deviceSession.first()) {
                    sessionViewModel.playOnThisDevice(silent = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                api = newApi(DEFAULT_SERVER)
                apiReady.complete(Unit)
                commands.ready()
                commands.refuse(Phrase { refusalLine(e) })
            }
        }
    }

    // Мир сервера - контент, словарь, иконки и портреты - грузит `WorldLoader` (3.80.43); фичи пока зовут его отсюда.
    fun language(lang: Lang) = loader.language(lang)
    suspend fun loadLocale(language: Lang) = loader.loadLocale(language)
    fun refreshLocale(language: Lang = languages.lang.value) = loader.refreshLocale(language)
    suspend fun loadIcons() = loader.loadIcons()
    fun refreshIcons() = loader.refreshIcons()
    suspend fun loadPortraits() = loader.loadPortraits()
    suspend fun ensureContent(fresh: Boolean = false) = loader.ensureContent(fresh)
    suspend fun contentFromDevice(): Boolean = loader.contentFromDevice()

    /** Открывает вкладку по прежнему номеру, отказывая игроку в административных. Отказ принадлежит экрану, где случился. */
    fun tab(tab: Int) {
        if (!adminTools() && tab in ADMIN_TABS) return
        commands.dismissMessage()
        navigator.tab(com.sperance.exileforge.presentation.nav.Route.ofTab(tab))
    }

    /** How many presses of the fight's speed button reach the settings' speed (3.77.0): 1 → 2 → 4. */
    val speedSteps: Int get() = GameSettings.SPEEDS.indexOf(prefs.settings.value.fightSpeed).coerceAtLeast(0)

    fun saveSettings(value: GameSettings) {
        scope.launch { prefs.saveSettings(value) }
    }

    /** Вибрации (3.77.0) идут экрану с видом; выключенный вид отбрасывает [buzzer]. */
    val buzzes: kotlinx.coroutines.flow.SharedFlow<Buzz> get() = buzzer.flow
    fun buzz(kind: Buzz) = buzzer.buzz(kind)

    fun dismissMessage() = commands.dismissMessage()

    /** A success worth a toast: it replaces the one showing and leaves by itself. */
    fun toast(text: String, kind: NoticeKind = NoticeKind.DONE) = notices.toast(text, kind)

    fun dismissNotice() = notices.dismiss()

    /** Files a bug report and says so (3.48.0). */

    /** The report, and [onSent] once the server has taken it (3.75.0: the draft goes only then). */
    fun reportBug(report: com.sperance.exileforge.core.model.command.BugReportRequest, onSent: suspend () -> Unit = {}) = task {
        api.reportBug(report)
        onSent()
        toast(ui("bug.sent"))
    }

    fun task(writing: Boolean = false, touches: Set<String> = emptySet(), block: suspend () -> Unit) = commands.task(writing, touches, block)

    fun read(key: String, restart: Boolean = false, silent: Boolean = false, block: suspend () -> Unit) = commands.read(key, restart, silent, block)

    fun cancelReads() = commands.cancelReads()

    internal fun report(e: Exception, writing: Boolean) = commands.report(e, writing)

    fun clearSession() {
        api.logout()
        navigator.reset(com.sperance.exileforge.presentation.nav.Route.Auth)
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
        markets.clear()
        guilds.clear()
        commands.clearFailure()
        admins.clear()
        modes.set(AppMode.PLAYER)
        warmupViewModel.clear()
    }

    /** Администратор в инструментах: отладочная сборка, роль и режим вместе. */
    private fun adminTools(): Boolean = com.sperance.exileforge.BuildConfig.DEBUG && sessions.state.value.isAdmin && modes.mode.value == AppMode.ADMIN
}
