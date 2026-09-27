package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.display.PortraitKey
import com.sperance.exileforge.core.display.PortraitManifest
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleLanguage
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.StaticManifest

/**
 * The server's static files: the manifest, the dictionaries, the icon set, the portraits and the
 * content chunks. Plain JSON outside the API envelope, readable without an account.
 */
class StaticClient internal constructor(private val http: Transport) {
    suspend fun manifest(): StaticManifest = http.fetch("static/index.json")

    suspend fun localeManifest(): LocaleManifest = http.fetch("locale/index.json")

    /** One language's dictionary, tagged with the fingerprint the manifest gave it. */
    suspend fun localeBundle(language: LocaleLanguage): LocaleBundle = LocaleBundle.parse(language.code, language.hash, localeDocument(language.code))

    /** The dictionary as it was served, unchecked: the caller parses it before it stores the very text. */
    suspend fun localeDocument(code: String): String {
        require(code.isNotBlank()) { ui("api.no_language") }
        return http.fetchText("locale/$code.json", validate = false)
    }

    suspend fun iconManifest(): IconManifest = http.fetch("icons/index.json")

    suspend fun iconDocument(file: String): String {
        require(file.isNotBlank()) { ui("api.no_icon_file") }
        return http.fetchText("icons/$file", validate = false)
    }

    suspend fun portraitManifest(): PortraitManifest = http.fetch("portraits/index.json")

    suspend fun portraitDocument(key: String): String {
        require('.' in key) { ui("api.no_portrait") }
        return http.fetchText(PortraitKey.path(key), json = false)
    }

    /** One chunk of the world's content (`content/<file>`), as served: parsed by the rules and stored verbatim under its fingerprint. */
    suspend fun contentChunk(file: String): String {
        require(file.isNotBlank()) { ui("api.no_world_file") }
        return http.fetchText("content/$file", validate = false)
    }
}
