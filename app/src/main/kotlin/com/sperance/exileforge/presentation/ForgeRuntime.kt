package com.sperance.exileforge.presentation

import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.PortraitBundle
import com.sperance.exileforge.core.display.PortraitSvg
import com.sperance.exileforge.core.display.serverPortraits
import com.sperance.exileforge.core.display.serverIcons
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.transportDetail
import com.sperance.exileforge.core.network.refusalLine
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.deviceLanguage
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.features.*
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.ADMIN_TABS
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import com.sperance.exileforge.core.model.sync.WorldTables
import kotlinx.coroutines.sync.withLock

class ForgeRuntime(val store: ServerStore, val journal: RequestJournal, val deviceId: String = "") {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    val mutable = MutableStateFlow(ForgeState())
    val state = mutable.asStateFlow()
    val logs = journal.entries
    lateinit var api: GameApi
    var localeJob: Job? = null
    private val reads = mutableMapOf<String, Job>()
    private var touching: Set<String> = emptySet()
    var iconJob: Job? = null
    val catalogViewModel = CatalogViewModel(this)
    val editorViewModel = EditorViewModel(this)
    val heroViewModel = HeroViewModel(this)
    val sessionViewModel = SessionViewModel(this)
    val checksViewModel = ChecksViewModel(this)
    val auctionViewModel = AuctionViewModel(this)
    val redemptionViewModel = RedemptionViewModel(this)
    val characterViewModel = CharacterViewModel(this)
    val expeditionViewModel = ExpeditionViewModel(this)
    val craftsViewModel = CraftsViewModel(this)

    /**
     * A refused token is forgotten, and a player who plays by device is signed in again without
     * being asked: the account is theirs by the device, so a lapsed token is no reason to stop. The
     * sign-in waits for the command that met the 401 to finish, because [task] refuses to start
     * while another one runs.
     */
    fun newApi(server: String): GameApi {
        lateinit var created: GameApi
        created = GameApi(server, journal, onUnauthorized = {
            if (::api.isInitialized && api === created) {
                clearSession()
                scope.launch {
                    store.saveToken(server, null)
                    if (store.deviceSession.first()) {
                        state.first { !it.busy }
                        if (api === created) sessionViewModel.playOnThisDevice(silent = true)
                    } else mutable.update { it.copy(message = ui("runtime.session_expired"), error = true) }
                }
            }
        })
        created.heroSync(heroViewModel::heldParts, heroViewModel::delivered)
        return created
    }

    init {
        scope.launch {
            try {
                // No choice stored means a first run, and a first run follows the device rather
                // than the client's own default: an English phone should not open in Russian.
                val language = Lang.byCode(store.language.first()) ?: deviceLanguage()
                uiLanguage = language
                val server = store.server.first()
                api = newApi(server)
                val known = store.languages(server).mapNotNull { Lang.byCode(it) }
                mutable.update { it.copy(lang = language, busy = false, account = it.account.copy(server = server, serverDraft = server, deviceId = deviceId), world = it.world.copy(languages = known.ifEmpty { it.world.languages })) }
                refreshLocale()
                refreshIcons()
                launch { quietly { store.dropLegacyDocuments() } }
                // A kept token comes back as it was; without one, a session is made again without
                // asking, and only for someone who last played on this device.
                val saved = store.token(server)
                if (saved != null) sessionViewModel.resume(saved)
                else if (store.deviceSession.first()) sessionViewModel.playOnThisDevice(silent = true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                api = newApi("http://10.0.2.2:8080/")
                mutable.update { it.copy(busy = false, error = true, message = e.message) }
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
        // The server's half of the language lives in its dictionary, so the two are switched together.
        refreshLocale(lang)
    }

    /**
     * The server's dictionary for one language.
     *
     * Since 0.14.0 the documents carry codes and the text is served as a static file, so this is
     * what turns `equipment.IRON_SKULLCAP.name` back into a name. The stored copy is read first, so
     * the app starts with names even offline; the manifest's hash then says whether it is still the
     * dictionary the server is serving, and only a changed hash costs a download.
     *
     * A dictionary belongs to a server as well as to a language: two servers may seed different text.
     */
    suspend fun loadLocale(language: Lang) {
        val server = state.value.account.server
        val cached = store.locale(server, language.code)
        cached?.let { (hash, document) -> applyLocale(parsed { LocaleBundle.parse(language.code, hash, document) }) }
        val manifest = api.manifest().locale
        applyLanguages(server, manifest)
        val chosen = manifest.language(language.code) ?: manifest.language(manifest.default) ?: return
        if (cached == null || cached.first != chosen.hash || chosen.code != language.code)
            applyLocale(bundle(server, manifest, chosen.code) ?: return)
    }

    /** One dictionary, off the store when its fingerprint still matches and off the server when not. */
    private suspend fun bundle(server: String, manifest: LocaleManifest, code: String): LocaleBundle? {
        val language = manifest.language(code) ?: return null
        store.locale(server, code)?.let { (hash, document) ->
            if (hash == language.hash) return parsed { LocaleBundle.parse(code, hash, document) }
        }
        val document = api.files.localeDocument(code)
        // Parsed before it is kept: a file that does not read is never stored under a good hash.
        return parsed { LocaleBundle.parse(code, language.hash, document) }
            .also { store.saveLocale(server, code, language.hash, document) }
    }

    /**
     * Reading the dictionary is background work and never an error banner.
     *
     * A server that will not serve it leaves codes on screen — which is the honest picture, and the
     * Account tab reports how many strings are loaded — rather than a failure the player cannot act
     * on before they have even signed in.
     */
    fun refreshLocale(language: Lang = state.value.lang) {
        localeJob?.cancel()
        localeJob = scope.launch {
            try { loadLocale(language) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
        }
    }

    /**
     * The server's icon set.
     *
     * The same shape as the dictionary and for the same reason: the file is big, the fingerprint
     * is small, and the manifest is what says whether the body is worth fetching. It is not keyed
     * by language — a drawing reads the same in both — so only the server decides which set it is.
     *
     * Nothing here is required. A set that never arrives leaves the client's own emblems on screen,
     * which is what they were for before the server had any.
     */
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

    /**
     * Reading the icons and the portraits is background work, side by side; a failure of either
     * leaves the bundled emblems and busts, not a banner.
     */
    fun refreshIcons() {
        iconJob?.cancel()
        iconJob = scope.launch {
            launch { quietly { loadIcons() } }
            launch { quietly { loadPortraits() } }
        }
    }

    /** Runs optional background work whose failure only means the bundled fallback stays on screen. */
    private suspend fun quietly(block: suspend () -> Unit) {
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
    }

    /** Parsing a served document is CPU work of its size, and never belongs on the main thread. */
    private suspend fun <T> parsed(parse: () -> T): T = withContext(Dispatchers.Default) { parse() }

    /**
     * The server's portraits (since 2.31.0): the manifest names a fingerprint per file, and only a
     * file whose fingerprint moved is fetched again. What is stored is shown at once, before the
     * manifest answers. A portrait that will not parse is dropped, and the client draws its own bust.
     */
    suspend fun loadPortraits() {
        val server = state.value.account.server
        val cached = store.portraits(server)
        applyPortraits(cached)
        val manifest = api.manifest().portraits
        // The changed files come down together; OkHttp caps how many go to one host at once.
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

    /**
     * Which languages this server offers.
     *
     * A language the client has no table of labels for is dropped: the server would name the items
     * but every button would still be a key. The choice already made is kept whatever the manifest
     * says, so a server that stops serving a language does not silently move a player off it.
     */
    private suspend fun applyLanguages(server: String, manifest: LocaleManifest) {
        val offered = manifest.languages.mapNotNull { Lang.byCode(it.code) }
        if (offered.isEmpty()) return
        store.saveLanguages(server, offered.map { it.code })
        mutable.update { it.copy(world = it.world.copy(languages = (offered + it.lang).distinct().sortedBy(Lang::ordinal))) }
    }

    /** The bundle is global because `core` renders from it; the state only reports what is loaded. */
    private fun applyLocale(bundle: LocaleBundle) {
        serverLocale = bundle
        mutable.update { it.copy(world = it.world.copy(localeLanguage = bundle.language, localeStrings = bundle.size)) }
    }

    /** The Checks tab runs writes against the server; it belongs to an administrator alone. */
    /**
     * Opens a tab, refusing the ones a player has no business on.
     *
     * The catalogue joined the editor and the checks behind this gate in 2.3.0: it is a view of
     * the world's reference tables, which is an administrator's concern, and a player's four
     * destinations are the game itself.
     */
    fun tab(tab: Int) {
        if (!state.value.adminTools && tab in ADMIN_TABS) return
        // A refusal belongs to the screen it happened on.
        mutable.update { it.copy(tab = tab, message = null, error = false) }
    }
    suspend fun referencePage(source: EntitySource, page: Int, query: String) = api.catalog.referencePage(source, page, query)
    /**
     * One equipment template, through the cache the inventory already fills.
     *
     * An auction lot carries the instance but not its template, and armour, damage and every
     * requirement live in the template. Since the whole catalogue is read once per session this
     * costs no request at all — it is a lookup with the read behind it, kept as one call so a
     * caller never has to remember which came first.
     */
    suspend fun equipmentBase(id: String): JsonObject? {
        if (id.isBlank()) return null
        ensureWorld()
        return state.value.world.inventoryBases[id]
    }
    fun dismissMessage() { mutable.update { it.copy(message = null, error = false) } }

    /**
     * A command: one at a time, and the only thing that disables controls.
     *
     * [writing] marks a mutation, so an IO error or a 5xx becomes [FailureState.UncertainWrite]
     * instead of "offline": the write may have landed and only a refresh can tell. [touches] names
     * the reads the command redoes itself: one already on its way is cancelled, because it would
     * land after the command with what was true before, and one asked for meanwhile is skipped.
     */
    fun task(writing: Boolean = false, touches: Set<String> = emptySet(), block: suspend () -> Unit) {
        if (state.value.busy) return
        touches.forEach { reads.remove(it)?.cancel() }
        touching = touches
        mutable.update { it.copy(busy = true, loading = reads.keys.toSet(), message = null, error = false, failure = null) }
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { report(e, writing) }
            finally { touching = emptySet(); mutable.update { it.copy(busy = false) } }
        }
    }

    /**
     * A read: it never waits for a command and never holds one up.
     *
     * One per [key] at a time — a second pull on the same list is the first one still coming —
     * and none while a command that redoes it is running. [restart] is for a read whose question
     * changed, a new filter or page: the one on its way answers the old question, so it is dropped.
     * [silent] is for a read nobody asked for — a craft cycle checking with the server — and it
     * shows no strip and no spinner.
     * A failure is reported like a command's, so a list that cannot be read says why rather than
     * staying quietly empty.
     */
    fun read(key: String, restart: Boolean = false, silent: Boolean = false, block: suspend () -> Unit) {
        if (key in touching) return
        if (restart) reads.remove(key)?.cancel()
        if (reads[key]?.isActive == true) return
        if (silent) quiet += key else quiet -= key
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { report(e, writing = false) }
            finally {
                if (reads[key] === coroutineContext[Job]) { reads.remove(key); quiet -= key }
                mutable.update { it.copy(loading = loading()) }
            }
        }
        reads[key] = job
        mutable.update { it.copy(loading = loading()) }
        job.start()
    }

    /** Reads on their way that the player should see; a [read] asked for in silence is not one (2.56.1). */
    private fun loading(): Set<String> = reads.keys.filterNotTo(HashSet()) { it in quiet }

    private val quiet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** Every read of the session that is ending: what it would bring back belongs to nobody now. */
    fun cancelReads() {
        reads.values.forEach { it.cancel() }
        reads.clear()
        mutable.update { it.copy(loading = emptySet()) }
    }

    internal fun report(e: Exception, writing: Boolean) {
        val problem = FailureState.from(e, writing)
        // A refusal the dictionary knows whole is shown in the chosen language; one whose
        // template needs arguments the envelope never carried keeps the server's sentence.
        val refusal = if (e is ApiFailure) locError(e.code, e.message.orEmpty(), e.args) else e.message.orEmpty()
        mutable.update { it.copy(failure = problem, error = true, message = when (problem) {
            FailureState.UncertainWrite -> ui("runtime.uncertain_write")
            FailureState.Offline -> ui("runtime.offline") + transportDetail(e)
            else -> refusalLine(e, refusal.ifBlank { ui("runtime.request_failed") }, detailed = it.isAdmin)
        }) }
    }

    suspend fun loadPage(page: Int) {
        val result = api.catalog.search(state.value.admin.catalog, page, state.value.admin.filter.copy(query = state.value.admin.query))
        mutable.update { it.copy(admin = it.admin.copy(items = result.items, page = result.page, total = result.totalItems, totalPages = result.totalPages)) }
    }

    fun setEditor(document: JsonObject, original: JsonObject?) {
        mutable.update { it.copy(tab = 1, admin = it.admin.copy(original = original, editorOpen = true, draft = document)) }
    }

    private val worldLock = kotlinx.coroutines.sync.Mutex()
    private var worldHash = ""
    private var worldStale = false

    /**
     * Every reference table of the world (server 0.48.0): modifiers, classes, tree, levels,
     * equipment bases, orbs, materials and the sheet's tables, one file kept on the device per
     * server and fetched again only when the start manifest's fingerprint moves. A warm start reads
     * none of it. [fresh] asks the manifest again — on entering a character, and after an
     * administrator's edit, which is what moves the fingerprint.
     *
     * Since 0.16.0 an instance carries only what it rolled, so a card without its base has nothing
     * to say: this is a prerequisite of every hero read, not a courtesy.
     */
    suspend fun ensureWorld(fresh: Boolean = false) = worldLock.withLock {
        val server = state.value.account.server
        val manifest = api.manifest(fresh || worldStale).world
        worldStale = false
        if (manifest.hash == worldHash && state.value.world.inventoryBases.isNotEmpty()) return@withLock
        val stored = store.world(server)?.takeIf { it.first == manifest.hash }?.second
        val document = stored ?: api.files.worldDocument(manifest.file)
        val tables = parsed { WorldTables.parse(manifest.hash, document) }
        // Kept only once it has read: a broken download is fetched again rather than stored.
        if (stored == null) store.saveWorld(server, manifest.hash, document)
        worldHash = tables.hash
        mutable.update { it.copy(
            world = it.world.copy(definitions = tables.modifiers, classes = tables.classes, treeNodes = tables.tree, levels = tables.levels,
                inventoryBases = tables.equipment, statTables = tables.stats, orbs = tables.orbs, materials = tables.materials, pools = tables.pools,
                skills = tables.skills, books = tables.books, essenceBook = tables.essenceBook, essences = tables.essences),
            play = it.play.copy(draftClass = it.play.draftClass.ifBlank { tables.classes.firstOrNull()?.id.orEmpty() },
                selectedOrb = it.play.selectedOrb.ifBlank { tables.orbs.firstOrNull()?.id.orEmpty() })) }
    }

    /** An administrator changed a reference table: the next [ensureWorld] asks the manifest again. */
    fun staleWorld() { worldStale = true }

    fun clearSession() {
        api.logout(); journal.clear(); cancelReads(); expeditionViewModel.drop(); craftsViewModel.drop(); worldHash = ""; heroViewModel.forget()
        mutable.update { it.copy(phase = AppPhase.AUTH, tab = 3, mode = AppMode.PLAYER, failure = null, account = it.account.copy(resumable = false, characters = emptyList(), charactersRead = false, signedIn = false, profile = null, sessionEpoch = it.account.sessionEpoch + 1), admin = it.admin.copy(items = emptyList(), total = 0, page = 0, totalPages = 0, original = null, draft = JsonObject(emptyMap()), editorOpen = false, checks = emptyList()), world = it.world.copy(definitions = emptyList(), orbs = emptyList(), bench = emptyList(), classes = emptyList(), treeNodes = emptyList(), inventoryBases = emptyMap(), campaign = null, statTables = com.sperance.exileforge.core.character.StatTables()), play = it.play.copy(selectedOrb = "", draftClass = "", selectedNode = "", nodeQuery = "", characterId = "", characterOwner = "", hero = null, selectedEquipment = "", forgeLine = "", campaign = null), market = it.market.copy(tab = 0, showcase = com.sperance.exileforge.core.model.auction.AuctionPage(), filter = com.sperance.exileforge.core.model.auction.AuctionFilter(), showOwnLots = false, myLots = emptyList(), locked = null)) }
    }

    fun close() { scope.coroutineContext[Job]?.cancel() }
}
