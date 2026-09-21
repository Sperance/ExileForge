package com.sperance.exileforge.presentation

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.transportDetail
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.features.*
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ForgeRuntime(val store: ServerStore, val journal: RequestJournal) {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    val mutable = MutableStateFlow(ForgeState())
    val state = mutable.asStateFlow()
    val logs = journal.entries
    lateinit var api: GameApi
    var metadataJob: Job? = null
    val catalogViewModel = CatalogViewModel(this)
    val editorViewModel = EditorViewModel(this)
    val heroViewModel = HeroViewModel(this)
    val sessionViewModel = SessionViewModel(this)
    val checksViewModel = ChecksViewModel(this)
    val auctionViewModel = AuctionViewModel(this)

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
                mutable.update { it.copy(lang = language, server = server, serverDraft = server, busy = false,
                    message = tr("Войдите в аккаунт для загрузки каталога", "Sign in to load the catalogue")) }
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
    }

    /** The Checks tab runs writes against the server; it belongs to an administrator alone. */
    fun tab(tab: Int) { if (!state.value.adminTools && tab == 2) return; mutable.update { it.copy(tab = tab) } }
    suspend fun referencePage(source: EntitySource, page: Int, query: String) = api.referencePage(source, page, query)
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
                mutable.update { it.copy(failure = problem, error = true, message = when (problem) {
                    FailureState.UncertainWrite -> tr("Ответ потерян. Запись могла сохраниться: обновите данные перед повтором.", "The response was lost. The write may have been applied: refresh before retrying.")
                    FailureState.Offline -> tr("Нет соединения: ", "No connection: ") + transportDetail(e)
                    else -> prefix + (e.message ?: tr("Ошибка запроса", "Request failed"))
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

    /** The orbs the server seeded. The catalogue is fixed for a session, so one read covers it. */
    suspend fun ensureOrbs() {
        if (state.value.orbs.isNotEmpty()) return
        val orbs = api.currencyOrbs()
        mutable.update { it.copy(orbs = orbs, selectedOrb = it.selectedOrb.ifBlank { orbs.firstOrNull()?.id.orEmpty() }) }
    }

    fun clearSession() {
        metadataJob?.cancel(); api.logout(); journal.clear()
        mutable.update { it.copy(signedIn = false, profile = null, sessionEpoch = it.sessionEpoch + 1,
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
