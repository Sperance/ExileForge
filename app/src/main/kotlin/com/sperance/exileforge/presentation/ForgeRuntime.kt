package com.sperance.exileforge.presentation

import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.serverIcons
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.serverLocaleEn
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.transportDetail
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.features.*
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ForgeRuntime(val store: ServerStore, val journal: RequestJournal, val deviceId: String = "") {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    val mutable = MutableStateFlow(ForgeState())
    val state = mutable.asStateFlow()
    val logs = journal.entries
    lateinit var api: GameApi
    var localeJob: Job? = null
    var iconJob: Job? = null
    val catalogViewModel = CatalogViewModel(this)
    val editorViewModel = EditorViewModel(this)
    val heroViewModel = HeroViewModel(this)
    val sessionViewModel = SessionViewModel(this)
    val checksViewModel = ChecksViewModel(this)
    val auctionViewModel = AuctionViewModel(this)
    val characterViewModel = CharacterViewModel(this)

    fun newApi(server: String): GameApi {
        lateinit var created: GameApi
        created = GameApi(server, journal, onUnauthorized = {
            if (::api.isInitialized && api === created) {
                clearSession()
                mutable.update { it.copy(message = tr("Сессия истекла. Войдите снова.", "The session has expired. Sign in again.")) }
            }
        })
        return created
    }

    init {
        scope.launch {
            try {
                val language = store.language.first()
                uiLanguage = language
                val server = store.server.first()
                api = newApi(server)
                mutable.update { it.copy(lang = language, server = server, serverDraft = server, busy = false, deviceId = deviceId) }
                refreshLocale()
                refreshIcons()
                // The session itself cannot be restored — the server issues no token — but it can be
                // made again without asking, and only for someone who last played on this device.
                if (store.deviceSession.first()) sessionViewModel.playOnThisDevice(silent = true)
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
        val untested = tr("Соединение ещё не проверено", "The connection has not been checked yet")
        uiLanguage = lang
        mutable.update { it.copy(lang = lang, health = if (it.health == untested) tr("Соединение ещё не проверено", "The connection has not been checked yet") else it.health) }
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
        val server = state.value.server
        val cached = store.locale(server, language.code)
        cached?.let { (hash, document) -> applyLocale(LocaleBundle.parse(language.code, hash, document)) }
        val manifest = api.localeManifest()
        val chosen = manifest.language(language.code) ?: manifest.language(manifest.default) ?: return
        if (cached == null || cached.first != chosen.hash || chosen.code != language.code)
            applyLocale(bundle(server, manifest, chosen.code) ?: return)
        // The showcase names a lot in English as well, because that is the language of the wiki and
        // of every trade site a lot is compared against. This is the only second dictionary the
        // client holds, and when the player already reads English it is the very same one.
        serverLocaleEn = if (chosen.code == Lang.EN.code) serverLocale
            else bundle(server, manifest, Lang.EN.code) ?: LocaleBundle()
    }

    /** One dictionary, off the store when its fingerprint still matches and off the server when not. */
    private suspend fun bundle(server: String, manifest: LocaleManifest, code: String): LocaleBundle? {
        val language = manifest.language(code) ?: return null
        store.locale(server, code)?.let { (hash, document) ->
            if (hash == language.hash) return LocaleBundle.parse(code, hash, document)
        }
        val document = api.localeDocument(code)
        store.saveLocale(server, code, language.hash, document)
        return LocaleBundle.parse(code, language.hash, document)
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
        val server = state.value.server
        val cached = store.icons(server)
        cached?.let { (hash, document) -> applyIcons(IconBundle.parse(hash, document)) }
        val manifest = api.iconManifest()
        if (manifest.hash.isBlank() || cached?.first == manifest.hash) return
        val document = api.iconDocument(manifest.file)
        store.saveIcons(server, manifest.hash, document)
        applyIcons(IconBundle.parse(manifest.hash, document))
    }

    /** Reading the icons is background work; a failure leaves the bundled emblems, not a banner. */
    fun refreshIcons() {
        iconJob?.cancel()
        iconJob = scope.launch {
            try { loadIcons() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
        }
    }

    private fun applyIcons(bundle: IconBundle) {
        serverIcons = bundle
        mutable.update { it.copy(iconKeys = bundle.size, iconSprites = bundle.spriteCount) }
    }

    /** The bundle is global because `core` renders from it; the state only reports what is loaded. */
    private fun applyLocale(bundle: LocaleBundle) {
        serverLocale = bundle
        mutable.update { it.copy(localeLanguage = bundle.language, localeStrings = bundle.size) }
    }

    /** The Checks tab runs writes against the server; it belongs to an administrator alone. */
    fun tab(tab: Int) { if (!state.value.adminTools && tab == 2) return; mutable.update { it.copy(tab = tab) } }
    suspend fun referencePage(source: EntitySource, page: Int, query: String) = api.referencePage(source, page, query)
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
        ensureEquipment()
        return state.value.inventoryBases[id]
    }

    /** The recipe form reads one document directly; it is never edited, only spent. */
    suspend fun recipeDocument(id: String): JsonObject =
        com.sperance.exileforge.core.contract.WireJson.encodeToJsonElement(com.sperance.exileforge.core.model.hero.RecipeDocument.serializer(), api.recipe(id)).jsonObject
    fun dismissMessage() { mutable.update { it.copy(message = null) } }

    /**
     * The standard action wrapper: one server call at a time, failures mapped to a message.
     *
     * [writing] marks a mutation, so an IO error or a 5xx becomes [FailureState.UncertainWrite]
     * instead of "offline": the write may have landed and only a refresh can tell.
     */
    fun task(writing: Boolean = false, block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, message = null, error = false, failure = null) }
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val problem = FailureState.from(e, writing)
                val prefix = if (e is ApiFailure) "HTTP ${e.status ?: "—"} ${e.code.orEmpty()}: " else ""
                // A refusal the dictionary knows whole is shown in the chosen language; one whose
                // template needs arguments the envelope never carried keeps the server's sentence.
                val refusal = if (e is ApiFailure) locError(e.code, e.message.orEmpty()) else e.message.orEmpty()
                mutable.update { it.copy(failure = problem, error = true, message = when (problem) {
                    FailureState.UncertainWrite -> tr("Ответ потерян. Запись могла сохраниться: обновите данные перед повтором.", "The response was lost. The write may have been applied: refresh before retrying.")
                    FailureState.Offline -> tr("Нет соединения: ", "No connection: ") + transportDetail(e)
                    else -> prefix + refusal.ifBlank { tr("Ошибка запроса", "Request failed") }
                }) }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }

    suspend fun loadPage(page: Int) {
        val result = api.search(state.value.catalog, page, state.value.filter.copy(query = state.value.query))
        mutable.update { it.copy(items = result.items, page = result.page, total = result.totalItems, totalPages = result.totalPages) }
    }

    fun setEditor(document: JsonObject, original: JsonObject?) {
        mutable.update { it.copy(original = original, editorOpen = true, draft = document, tab = 1) }
    }

    /** The modifier catalogue is small and shared; one read per session names every rolled value. */
    suspend fun ensureDefinitions() {
        if (state.value.definitions.isNotEmpty()) return
        mutable.update { it.copy(definitions = api.modifierDefinitions()) }
    }

    /**
     * The world's reference tables: the classes and the shared skill tree.
     *
     * Both are seeded and fixed for a session, and the tree is one graph rather than a page, so a
     * single read backs the character form and the tree screen alike.
     */
    suspend fun ensureProgression() {
        if (state.value.classes.isNotEmpty() && state.value.treeNodes.isNotEmpty()) return
        val classes = api.characterClasses()
        val nodes = api.skillTree()
        mutable.update { it.copy(classes = classes, treeNodes = nodes, draftClass = it.draftClass.ifBlank { classes.firstOrNull()?.id.orEmpty() }) }
    }

    /**
     * The equipment catalogue, read whole once per session.
     *
     * Since 0.16.0 an instance carries only what it rolled: armour, damage and every requirement
     * belong to the template and live in the catalogue in one copy. A card without its template
     * therefore has nothing to say, so this is a hard requirement like the classes and the tree —
     * not the background courtesy the per-item fetch used to be. The catalogue is some sixty
     * documents and the client already reads it whole for the Catalogue tab.
     */
    suspend fun ensureEquipment() {
        if (state.value.inventoryBases.isNotEmpty()) return
        val templates = api.equipmentCatalogue().associateBy { it.entityId }
        mutable.update { it.copy(inventoryBases = templates) }
    }

    /** The orbs the server seeded. The catalogue is fixed for a session, so one read covers it. */
    suspend fun ensureOrbs() {
        if (state.value.orbs.isNotEmpty()) return
        val orbs = api.currencyOrbs()
        mutable.update { it.copy(orbs = orbs, selectedOrb = it.selectedOrb.ifBlank { orbs.firstOrNull()?.id.orEmpty() }) }
    }

    fun clearSession() {
        api.logout(); journal.clear()
        mutable.update { it.copy(phase = AppPhase.AUTH, characters = emptyList(), charactersRead = false,
            signedIn = false, profile = null, sessionEpoch = it.sessionEpoch + 1,
            items = emptyList(), total = 0, page = 0, totalPages = 0, definitions = emptyList(),
            orbs = emptyList(), selectedOrb = "",
            classes = emptyList(), treeNodes = emptyList(), draftClass = "", selectedNode = "", nodeQuery = "",
            auctionTab = 0, showcase = com.sperance.exileforge.core.model.auction.AuctionPage(),
            auctionFilter = com.sperance.exileforge.core.model.auction.AuctionFilter(),
            showOwnLots = false, myLots = emptyList(), auctionLocked = null,
            original = null, draft = JsonObject(emptyMap()), editorOpen = false,
            characterId = "", characterOwner = "", hero = null, inventoryBases = emptyMap(), selectedEquipment = "",
            checks = emptyList(), tab = 3, mode = AppMode.PLAYER, failure = null) }
    }

    fun close() { scope.coroutineContext[Job]?.cancel() }
}
