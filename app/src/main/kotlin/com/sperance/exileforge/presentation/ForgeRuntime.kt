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
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.data.settings.deviceLanguage
import com.sperance.exileforge.presentation.features.AuctionViewModel
import com.sperance.exileforge.presentation.features.CharacterViewModel
import com.sperance.exileforge.presentation.features.ConnectionViewModel
import com.sperance.exileforge.presentation.features.CraftsViewModel
import com.sperance.exileforge.presentation.features.ExpeditionViewModel
import com.sperance.exileforge.presentation.features.FeedbackViewModel
import com.sperance.exileforge.presentation.features.GuildViewModel
import com.sperance.exileforge.presentation.features.HeroViewModel
import com.sperance.exileforge.presentation.features.QuestViewModel
import com.sperance.exileforge.presentation.features.RedemptionViewModel
import com.sperance.exileforge.presentation.features.SessionViewModel
import com.sperance.exileforge.presentation.features.TrialViewModel
import com.sperance.exileforge.presentation.state.ADMIN_TABS
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.presentation.state.GuildState
import com.sperance.exileforge.presentation.state.MarketState
import com.sperance.exileforge.presentation.state.Notice
import com.sperance.exileforge.presentation.state.NoticeKind
import com.sperance.exileforge.presentation.state.Phrase
import com.sperance.exileforge.presentation.state.PlayState
import com.sperance.exileforge.presentation.state.QuestState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ForgeRuntime(val store: ServerStore, val journal: RequestJournal, val prefs: com.sperance.exileforge.data.settings.PreferencesRepository) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val mutable = MutableStateFlow(ForgeState())
    val state = mutable.asStateFlow()
    val logs = journal.entries
    lateinit var api: GameApi

    /** The first [api] is made (3.74.0): the update check waits for it rather than asking no server at all. */
    val apiReady = kotlinx.coroutines.CompletableDeferred<Unit>()

    /** A sign-in met a server newer than this build (3.74.0): the update check runs at once. */
    val newerServer = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    var localeJob: Job? = null
    private val reads = mutableMapOf<String, Job>()
    private var touching: Set<String> = emptySet()
    var iconJob: Job? = null
    val heroViewModel = HeroViewModel(this)
    val sessionViewModel = SessionViewModel(this)
    val auctionViewModel = AuctionViewModel(this)
    val redemptionViewModel = RedemptionViewModel(this)
    val characterViewModel = CharacterViewModel(this)
    val expeditionViewModel = ExpeditionViewModel(this)
    val trialViewModel = TrialViewModel(this)
    val warmupViewModel = com.sperance.exileforge.presentation.features.WarmupViewModel(this)
    val craftsViewModel = CraftsViewModel(this)
    val guildViewModel = GuildViewModel(this)
    val feedbackViewModel = FeedbackViewModel(this)
    val questViewModel = QuestViewModel(this)
    val connectionViewModel = ConnectionViewModel(this)

    /**
     * A refused token is forgotten, and a player who plays by device is signed in again without being
     * asked. The sign-in waits for the command that met the 401 to finish, because [task] refuses to nest.
     */
    fun newApi(server: String): GameApi {
        lateinit var created: GameApi
        created = GameApi(server, journal, onUnauthorized = {
            if (::api.isInitialized && api === created) {
                clearSession()
                scope.launch {
                    store.saveToken(server, null)
                    // The copy of the last hero belongs to the refused session: the next launch does not open it.
                    store.saveLastHero(server, null)
                    if (store.deviceSession.first()) {
                        state.first { !it.busy }
                        if (api === created) sessionViewModel.playOnThisDevice(silent = true)
                    } else {
                        mutable.update { it.copy(message = phrase("runtime.session_expired"), error = true) }
                    }
                }
            }
        })
        created.heroSync(heroViewModel::heldParts, heroViewModel::delivered)
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

    init {
        // Настройки игрока живут в репозитории (3.80.6): общее состояние лишь отражает их для экранов, ещё не переведённых.
        scope.launch { prefs.settings.collect { value -> mutable.update { it.copy(settings = value) } } }
        scope.launch {
            try {
                val language = Lang.byCode(store.language.first()) ?: deviceLanguage()
                uiLanguage = language
                val server = store.server.first()
                api = newApi(server)
                apiReady.complete(Unit)
                val known = store.languages(server).mapNotNull { Lang.byCode(it) }
                val sort = StashSort.of(store.stashSort.first())
                val hideWorn = store.stashHideWorn.first()
                val settings = store.gameSettings.first()
                val logFilter = com.sperance.exileforge.core.campaign.LogKind.parse(store.logFilter.first())
                mutable.update { it.copy(lang = language, busy = false, stashSort = sort, stashHideWorn = hideWorn, settings = settings, logFilter = logFilter, account = it.account.copy(server = server, serverDraft = server), world = it.world.copy(languages = known.ifEmpty { it.world.languages })) }
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
                mutable.update { it.copy(busy = false, error = true, message = Phrase { refusalLine(e) }) }
            }
        }
    }

    /** Language is global: core validation messages and Compose both read it, so switch them together. */
    fun language(lang: Lang) {
        if (state.value.lang == lang) return
        val untested = ui("runtime.not_checked")
        uiLanguage = lang
        mutable.update { it.copy(lang = lang, account = it.account.copy(health = if (it.account.health == untested) ui("runtime.not_checked") else it.account.health)) }
        scope.launch { store.saveLanguage(lang) }
        refreshLocale(lang)
    }

    /**
     * The server's dictionary for one language: the stored copy first, so the app starts with names even
     * offline; the manifest's hash then says whether it is still the dictionary the server is serving.
     */
    suspend fun loadLocale(language: Lang) {
        val server = state.value.account.server
        val cached = store.locale(server, language.code)
        cached?.let { (hash, document) -> applyLocale(parsed { LocaleBundle.parse(language.code, hash, document) }) }
        val manifest = api.manifest().locale
        applyLanguages(server, manifest)
        val chosen = manifest.language(language.code) ?: manifest.language(manifest.default) ?: return
        if (cached == null || cached.first != chosen.hash || chosen.code != language.code) {
            applyLocale(bundle(server, manifest, chosen.code) ?: return)
        }
    }

    private suspend fun bundle(server: String, manifest: LocaleManifest, code: String): LocaleBundle? {
        val language = manifest.language(code) ?: return null
        store.locale(server, code)?.let { (hash, document) ->
            if (hash == language.hash) return parsed { LocaleBundle.parse(code, hash, document) }
        }
        val document = api.files.localeDocument(code)
        return parsed { LocaleBundle.parse(code, language.hash, document) }.also { store.saveLocale(server, code, language.hash, document) }
    }

    /** Reading the dictionary is background work and never an error banner. */
    fun refreshLocale(language: Lang = state.value.lang) {
        localeJob?.cancel()
        localeJob = scope.launch {
            try {
                loadLocale(language)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }

    suspend fun loadIcons() {
        val server = state.value.account.server
        val cached = store.icons(server)
        cached?.let { (hash, document) -> applyIcons(parsed { IconBundle.parse(hash, document) }) }
        val manifest = api.manifest().icons
        if (manifest.hash.isBlank() || cached?.first == manifest.hash) return
        val document = api.files.iconDocument(manifest.file)
        val bundle = parsed { IconBundle.parse(manifest.hash, document) }
        store.saveIcons(server, manifest.hash, document)
        applyIcons(bundle)
    }

    /** Reading the icons and the portraits is background work, side by side; a failure leaves the bundled emblems. */
    fun refreshIcons() {
        iconJob?.cancel()
        iconJob = scope.launch {
            launch { quietly { loadIcons() } }
            launch { quietly { loadPortraits() } }
        }
    }

    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { }
    }

    /** Parsing a served document is CPU work of its size, and never belongs on the main thread. */
    private suspend fun <T> parsed(parse: () -> T): T = withContext(Dispatchers.Default) { parse() }

    suspend fun loadPortraits() {
        val server = state.value.account.server
        val cached = store.portraits(server)
        applyPortraits(cached)
        val manifest = api.manifest().portraits
        val fresh = coroutineScope {
            manifest.portraits.mapValues { (key, hash) ->
                cached[key]?.takeIf { it.first == hash }?.let { CompletableDeferred(it) } ?: async { hash to api.files.portraitDocument(key) }
            }.mapValues { (_, file) -> file.await() }
        }
        if (fresh == cached) return
        store.savePortraits(server, fresh)
        applyPortraits(fresh)
    }

    private suspend fun applyPortraits(files: Map<String, Pair<String, String>>) {
        val parsed = withContext(Dispatchers.Default) {
            files.mapNotNull { (key, file) -> runCatching { key to PortraitSvg.parse(file.second) }.getOrNull() }.toMap()
        }
        serverPortraits = PortraitBundle(parsed)
        mutable.update { it.copy(world = it.world.copy(portraits = parsed.size)) }
    }

    private fun applyIcons(bundle: IconBundle) {
        serverIcons = bundle
        mutable.update { it.copy(world = it.world.copy(iconKeys = bundle.size, iconSprites = bundle.spriteCount)) }
    }

    private suspend fun applyLanguages(server: String, manifest: LocaleManifest) {
        val offered = manifest.languages.mapNotNull { Lang.byCode(it.code) }
        if (offered.isEmpty()) return
        store.saveLanguages(server, offered.map { it.code })
        mutable.update { it.copy(world = it.world.copy(languages = (offered + it.lang).distinct().sortedBy(Lang::ordinal))) }
    }

    private fun applyLocale(bundle: LocaleBundle) {
        serverLocale = bundle
        mutable.update { it.copy(world = it.world.copy(localeLanguage = bundle.language, localeStrings = bundle.size)) }
    }

    /** Opens a tab, refusing the ones a player has no business on. A refusal belongs to the screen it happened on. */
    fun tab(tab: Int) {
        if (!state.value.adminTools && tab in ADMIN_TABS) return
        mutable.update { it.copy(tab = tab, message = null, error = false) }
    }

    /** How many presses of the fight's speed button reach the settings' speed (3.77.0): 1 → 2 → 4. */
    val speedSteps: Int get() = GameSettings.SPEEDS.indexOf(state.value.settings.fightSpeed).coerceAtLeast(0)

    /** The tab «Настройки» were opened over (3.77.0). */
    var settingsReturn: Int = TAB_HERO

    fun saveSettings(value: GameSettings) {
        scope.launch { prefs.saveSettings(value) }
    }

    /** What the phone buzzes for, sent to the screen that holds the view (3.77.0); a switched-off kind is dropped here. */
    val buzzes = kotlinx.coroutines.flow.MutableSharedFlow<Buzz>(extraBufferCapacity = 4)
    fun buzz(kind: Buzz) {
        val set = state.value.settings
        if (if (kind == Buzz.DANGER) set.buzzDanger else set.buzzButtons) buzzes.tryEmit(kind)
    }

    fun dismissMessage() {
        mutable.update { it.copy(message = null, error = false) }
    }

    /** A success worth a toast: it replaces the one showing and leaves by itself. */
    fun toast(text: String, kind: NoticeKind = NoticeKind.DONE) {
        mutable.update { it.copy(notice = Notice(text, kind)) }
    }
    fun dismissNotice() {
        mutable.update { it.copy(notice = null) }
    }

    /**
     * A command: one at a time, and the only thing that disables controls. [writing] marks a mutation, so
     * an IO error becomes [FailureState.UncertainWrite]; [touches] names the reads the command redoes itself.
     */
    /** Files a bug report and says so (3.48.0). */

    /** The report, and [onSent] once the server has taken it (3.75.0: the draft goes only then). */
    fun reportBug(report: com.sperance.exileforge.core.model.command.BugReportRequest, onSent: suspend () -> Unit = {}) = task {
        api.reportBug(report)
        onSent()
        toast(ui("bug.sent"))
    }

    fun task(writing: Boolean = false, touches: Set<String> = emptySet(), block: suspend () -> Unit) {
        if (state.value.busy) return
        touches.forEach { reads.remove(it)?.cancel() }
        touching = touches
        mutable.update { it.copy(busy = true, loading = reads.keys.toSet(), message = null, error = false, failure = null) }
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(e, writing)
            } finally {
                touching = emptySet()
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    /**
     * A read: it never waits for a command and never holds one up. One per [key] at a time; [restart] drops
     * the one on its way; [silent] shows no strip and no spinner.
     */
    fun read(key: String, restart: Boolean = false, silent: Boolean = false, block: suspend () -> Unit) {
        if (key in touching) return
        if (restart) reads.remove(key)?.cancel()
        if (reads[key]?.isActive == true) return
        if (silent) quiet += key else quiet -= key
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(e, writing = false)
            } finally {
                if (reads[key] === coroutineContext[Job]) {
                    reads.remove(key)
                    quiet -= key
                }
                mutable.update { it.copy(loading = loading()) }
            }
        }
        reads[key] = job
        mutable.update { it.copy(loading = loading()) }
        job.start()
    }

    private fun loading(): Set<String> = reads.keys.filterNotTo(HashSet()) { it in quiet }

    private val quiet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun cancelReads() {
        reads.values.forEach { it.cancel() }
        reads.clear()
        mutable.update { it.copy(loading = emptySet()) }
    }

    /**
     * A failure, as the player should see it. A command that went into the queue is no failure at all; a lost
     * connection is the top bar's icon and the probe loop (3.30.0), not the red strip — which stays for what
     * the server refused, and for a write whose fate is unknown.
     */
    internal fun report(e: Exception, writing: Boolean) {
        if (e is CommandQueued) {
            // Queued because the network failed under it: the link is down, and the icon says so.
            if (e.cause != null && e.cause !is ApiFailure) connectionViewModel.lost()
            connectionViewModel.queued()
            return
        }
        val problem = FailureState.from(e, writing)
        if (problem is FailureState.Offline) {
            mutable.update { it.copy(failure = problem) }
            connectionViewModel.lost(e)
            // A write that met a dead server did not happen: it is said, by its cause, rather than lost without a word.
            if (writing) mutable.update { it.copy(error = true, message = Phrase { problem.cause.title + ". " + problem.cause.hint }) }
            return
        }
        if (problem == FailureState.UncertainWrite && e !is ApiFailure) connectionViewModel.lost(e)
        mutable.update {
            it.copy(
                failure = problem,
                error = true,
                message = when (problem) {
                    FailureState.UncertainWrite -> phrase("runtime.uncertain_write")
                    else -> Phrase { refusalLine(e) }
                },
            )
        }
    }

    private val contentLock = Mutex()
    private var contentStale = false

    /**
     * The world's content: every chunk kept on the device per server beside its fingerprint, fetched
     * again only when the start manifest's fingerprint for that chunk moves, and parsed by the rules.
     * A warm start reads none of it from the server. [fresh] asks the manifest again — on entering a hero.
     */
    suspend fun ensureContent(fresh: Boolean = false) = contentLock.withLock {
        val server = state.value.account.server
        val manifest = api.manifest(fresh || contentStale).content
        contentStale = false
        if (manifest.hash == state.value.world.contentHash && state.value.world.content != null) return@withLock
        // Each chunk says whether it came from the device or the network; the downloads are kept only once
        // the whole world has read, so a broken download is fetched again rather than stored.
        val chunks = coroutineScope {
            ContentFiles.ALL.associateWith { file ->
                async {
                    val wanted = manifest.chunks[file].orEmpty()
                    store.chunk(server, file)?.takeIf { it.first == wanted && wanted.isNotBlank() }?.let { it.second to false }
                        ?: (api.files.contentChunk(file) to true)
                }
            }.mapValues { it.value.await() }
        }
        val index = parsed { ContentLoader.load { chunks.getValue(it).first } }
        chunks.forEach { (file, chunk) -> if (chunk.second) store.saveChunk(server, file, manifest.chunks[file].orEmpty(), chunk.first) }
        mutable.update {
            it.copy(
                world = it.world.copy(content = index, contentHash = manifest.hash),
                play = it.play.copy(
                    draftClass = it.play.draftClass.ifBlank { index.classes.classes.firstOrNull()?.code.orEmpty() },
                    selectedOrb = it.play.selectedOrb.ifBlank { index.itemsByCategory[com.sperance.exileforge.rules.content.Item.CURRENCY]?.minByOrNull { o -> o.price }?.code.orEmpty() },
                ),
            )
        }
    }

    /**
     * The content from the device alone (3.30.0), as the kept manifest names it — no request: what the fast start
     * draws the last hero with. `false` when the manifest is of another revision or a chunk is missing or stale.
     */
    suspend fun contentFromDevice(): Boolean {
        contentLock.withLock {
            if (state.value.world.content != null) return true
            val server = state.value.account.server
            val manifest = store.manifest(server)?.let { text -> runCatching { WireJson.decodeFromString(StaticManifest.serializer(), text) }.getOrNull() }
                ?: return false
            if (manifest.revision != API_REVISION || manifest.rules != RULES_VERSION) return false
            val texts = ContentFiles.ALL.associateWith { file ->
                store.chunk(server, file)?.takeIf { it.first.isNotBlank() && it.first == manifest.content.chunks[file] }?.second ?: return false
            }
            val index = try {
                parsed { ContentLoader.load { texts.getValue(it) } }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return false
            }
            mutable.update {
                it.copy(
                    world = it.world.copy(content = index, contentHash = manifest.content.hash),
                    play = it.play.copy(
                        draftClass = it.play.draftClass.ifBlank { index.classes.classes.firstOrNull()?.code.orEmpty() },
                        selectedOrb = it.play.selectedOrb.ifBlank { index.itemsByCategory[com.sperance.exileforge.rules.content.Item.CURRENCY]?.minByOrNull { o -> o.price }?.code.orEmpty() },
                    ),
                )
            }
            return true
        }
    }

    fun clearSession() {
        api.logout()
        journal.clear()
        cancelReads()
        expeditionViewModel.drop()
        trialViewModel.drop()
        craftsViewModel.drop()
        heroViewModel.forget()
        mutable.update {
            it.copy(
                phase = AppPhase.AUTH, tab = TAB_HERO, mode = AppMode.PLAYER, failure = null,
                account = it.account.copy(resumable = false, characters = emptyList(), charactersRead = false, signedIn = false, profile = null, sessionEpoch = it.account.sessionEpoch + 1),
                admin = it.admin.copy(redemptions = emptyList()),
                play = PlayState(draftClass = it.play.draftClass, selectedOrb = it.play.selectedOrb),
                market = MarketState(), building = null, guild = GuildState(), quests = QuestState(),
            )
        }
    }

    fun close() {
        scope.coroutineContext[Job]?.cancel()
    }
}
