package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.display.PortraitKey
import com.sperance.exileforge.core.display.PortraitManifest
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleLanguage
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.StaticManifest
import kotlinx.serialization.json.*

/**
 * The server's static files: the dictionaries and the icon set.
 *
 * Plain JSON outside the API envelope, and readable without an account — a language has to be
 * there before anyone has signed in.
 */
class StaticClient internal constructor(private val http: Transport) {
    /**
     * The locale manifest and one dictionary.
     *
     * These are static resources, not API routes: they come back as plain JSON with no
     * `{success, data}` envelope around them, so they are fetched rather than requested. Nor do
     * they need an account — a language has to be readable before anyone has signed in.
     */
    suspend fun localeManifest(): LocaleManifest =
        http.fetch("locale/index.json")

    /**
     * One language's dictionary, tagged with the fingerprint the manifest gave it.
     *
     * The fingerprint travels with the bundle rather than being looked up again later: that is what
     * lets a stored dictionary be reused without downloading it to compare.
     */
    suspend fun localeBundle(language: LocaleLanguage): LocaleBundle =
        LocaleBundle.parse(language.code, language.hash, localeDocument(language.code))

    /** The dictionary as it was served, unchecked: the caller parses it before it stores the very text. */
    suspend fun localeDocument(code: String): String {
        require(code.isNotBlank()) { ui("api.no_language") }
        return http.fetchText("locale/$code.json", validate = false)
    }

    /**
     * The icon manifest and the set itself.
     *
     * Static content like the dictionaries: plain JSON, no envelope, no account. The manifest
     * carries the fingerprint the server computed from the file, so a set that was edited is
     * always noticed and one that was not is never downloaded twice.
     */
    suspend fun iconManifest(): IconManifest = http.fetch("icons/index.json")

    /** The set as it was served, unchecked: the caller parses it before it stores the very text. */
    suspend fun iconDocument(file: String): String {
        require(file.isNotBlank()) { ui("api.no_icon_file") }
        return http.fetchText("icons/$file", validate = false)
    }

    /**
     * The portraits (since server 0.29.0): a manifest with a fingerprint per file, then each SVG on
     * its own, so a client fetches only what changed. Static, public, no envelope, like the icons.
     */
    suspend fun portraitManifest(): PortraitManifest = http.fetch("portraits/index.json")

    /** One portrait's SVG as it was served, so it can be stored verbatim and parsed again offline. */
    suspend fun portraitDocument(key: String): String {
        require('.' in key) { ui("api.no_portrait") }
        return http.fetchText(PortraitKey.path(key), json = false)
    }

    /** `static/index.json` (server 0.48.0): the routes and every fingerprint, in one public read. */
    suspend fun manifest(): StaticManifest = http.fetch("static/index.json")

    /** Every reference table as it was served, for a signed-in caller: unchecked, parsed and then stored verbatim like the icons. */
    suspend fun worldDocument(file: String): String {
        require(file.isNotBlank()) { ui("api.no_world_file") }
        return http.fetchText("world/$file", authenticated = true, validate = false)
    }
}
