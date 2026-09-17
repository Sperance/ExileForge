package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.display.icons.IconSet
import com.sperance.exileforge.core.display.icons.iconSuggestions
import com.sperance.exileforge.core.display.svg.parseSvgSprite
import com.sperance.exileforge.core.model.command.ApiCapabilities
import com.sperance.exileforge.core.model.icons.IconBindingTables
import com.sperance.exileforge.core.model.icons.IconManifest
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** What one server's icon set costs to keep: three public responses and the ETag of the sprite. */
@Serializable private data class CachedIcons(val version: String, val etag: String, val manifest: IconManifest,
    val bindings: IconBindingTables, val sprite: String)

/** On-device cache only, so it is compact rather than the readable wire format. */
private val CacheJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

/**
 * The icon set of the connected server.
 *
 * The whole set arrives as one sprite, is stored against `iconSetVersion` and is re-read from
 * DataStore while that version holds, so a restart draws the server's icons without a request.
 * Icons are decoration: a failure here leaves the bundled emblems in place and never fails a screen.
 */
class IconViewModel(private val runtime: ForgeRuntime) {
    fun load(capabilities: ApiCapabilities? = null, force: Boolean = false) { with(runtime) {
        iconJob?.cancel()
        iconJob = scope.launch {
            val server = state.value.server
            try {
                val reported = capabilities ?: api.capabilities()
                if(!reported.hasIcons()) { publish(server, IconSet()); return@launch }
                if(!force && state.value.icons.ready && state.value.icons.version == reported.iconSetVersion) return@launch
                val cached = store.icons(server)?.let { runCatching { CacheJson.decodeFromString(CachedIcons.serializer(), it) }.getOrNull() }
                    ?.takeIf { it.version == reported.iconSetVersion }
                val set = cached?.takeIf { !force }
                    ?: download(reported.iconSetVersion, cached).also { store.saveIcons(server, CacheJson.encodeToString(CachedIcons.serializer(), it)) }
                publish(server, IconSet(set.manifest, set.bindings, withContext(Dispatchers.Default) { parseSvgSprite(set.sprite) }))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Icons are decoration: keep whatever is already on screen. */ }
        }
    } }

    /** A forced refresh revalidates the sprite with its stored ETag: an unchanged set costs one 304. */
    private suspend fun download(version: String, cached: CachedIcons?): CachedIcons = with(runtime) {
        val sprite = api.iconSprite(cached?.etag.orEmpty())
        val drawn = if(sprite.unchanged) requireNotNull(cached).sprite else sprite.svg
        CachedIcons(version, sprite.etag, api.icons(), api.iconBindings(), drawn)
    }

    /** The server may have changed while the set was downloading; a stale answer is dropped. */
    private fun publish(server: String, icons: IconSet) { with(runtime) {
        if(state.value.server != server) return
        iconSuggestions = icons.manifest?.icons.orEmpty().map { it.id }
        mutable.update { it.copy(icons = icons) }
    } }
}
