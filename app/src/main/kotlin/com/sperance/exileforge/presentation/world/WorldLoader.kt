package com.sperance.exileforge.presentation.world

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.PortraitBundle
import com.sperance.exileforge.core.display.PortraitSvg
import com.sperance.exileforge.core.display.serverIcons
import com.sperance.exileforge.core.display.serverPortraits
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.model.sync.StaticManifest
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.rules.content.ContentFiles
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Мир сервера на устройстве (3.80.43, из `ForgeRuntime`): контент кусками с отпечатками, словарь выбранного языка,
 * иконки и портреты. Всё хранится по серверу и качается заново, только когда манифест говорит, что кусок сменился;
 * словарь, иконки и портреты - фоновая работа, их отказ не красная полоса.
 */
class WorldLoader(
    private val store: ServerStore,
    private val connection: ServerConnection,
    private val sessions: SessionRepository,
    private val world: WorldRepository,
    private val languages: LanguageRepository,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api
    private var localeJob: Job? = null
    private var iconJob: Job? = null

    /** Language is global: core validation messages and Compose both read it, so switch them together. */
    fun language(lang: Lang) {
        if (languages.lang.value == lang) return
        val untested = ui("runtime.not_checked")
        languages.set(lang)
        sessions.update { it.copy(health = if (it.health == untested) ui("runtime.not_checked") else it.health) }
        scope.launch { store.saveLanguage(lang) }
        refreshLocale(lang)
    }

    /**
     * The server's dictionary for one language: the stored copy first, so the app starts with names even
     * offline; the manifest's hash then says whether it is still the dictionary the server is serving.
     */
    suspend fun loadLocale(language: Lang) {
        val server = sessions.state.value.server
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
    fun refreshLocale(language: Lang = languages.lang.value) {
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
        val server = sessions.state.value.server
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
        val server = sessions.state.value.server
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
        world.update { it.copy(portraits = parsed.size) }
    }

    private fun applyIcons(bundle: IconBundle) {
        serverIcons = bundle
        world.update { it.copy(iconKeys = bundle.size, iconSprites = bundle.spriteCount) }
    }

    private suspend fun applyLanguages(server: String, manifest: LocaleManifest) {
        val offered = manifest.languages.mapNotNull { Lang.byCode(it.code) }
        if (offered.isEmpty()) return
        store.saveLanguages(server, offered.map { it.code })
        val current = languages.lang.value
        world.update { it.copy(languages = (offered + current).distinct().sortedBy(Lang::ordinal)) }
    }

    private fun applyLocale(bundle: LocaleBundle) {
        serverLocale = bundle
        world.update { it.copy(localeLanguage = bundle.language, localeStrings = bundle.size) }
    }

    private val contentLock = Mutex()

    /**
     * The world's content: every chunk kept on the device per server beside its fingerprint, fetched
     * again only when the start manifest's fingerprint for that chunk moves, and parsed by the rules.
     * A warm start reads none of it from the server. [fresh] asks the manifest again — on entering a hero.
     */
    suspend fun ensureContent(fresh: Boolean = false) = contentLock.withLock {
        val server = sessions.state.value.server
        val manifest = api.manifest(fresh).content
        if (manifest.hash == world.state.value.contentHash && world.state.value.content != null) return@withLock
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
        world.update { it.copy(content = index, contentHash = manifest.hash) }
    }

    /**
     * The content from the device alone (3.30.0), as the kept manifest names it — no request: what the fast start
     * draws the last hero with. `false` when the manifest is of another revision or a chunk is missing or stale.
     */
    suspend fun contentFromDevice(): Boolean {
        contentLock.withLock {
            if (world.state.value.content != null) return true
            val server = sessions.state.value.server
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
            world.update { it.copy(content = index, contentHash = manifest.content.hash) }
            return true
        }
    }
}
