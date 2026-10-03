package com.sperance.exileforge.core.update

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.model.sync.StaticManifest
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * `update.json` (3.72.0): what CI publishes beside the APK of every release — the build's version, the wire and rules
 * it speaks, and the APK's name, size and SHA-256. The client takes a release only through it: an older release
 * without one is never offered.
 */
@Serializable data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apiRevision: Int,
    val rules: Int,
    val apk: String,
    val size: Long = 0,
    val sha256: String,
)

/** One version's notes: its name and the release text. */
data class ReleaseNotes(val version: String, val text: String)

/** The update the client must take: the build, its APK, its release page and the notes of every version it skips. */
data class AvailableUpdate(val info: UpdateInfo, val apkUrl: String, val page: String, val notes: List<ReleaseNotes>)

@Serializable internal data class GitAsset(val name: String, @SerialName("browser_download_url") val url: String, val size: Long = 0)

@Serializable internal data class GitRelease(
    @SerialName("tag_name") val tag: String,
    @SerialName("html_url") val page: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitAsset> = emptyList(),
)

/**
 * Updates from GitHub Releases alone (3.72.0): the list of releases is one call of the public API (no token, 60 an hour
 * per address), the `update.json` and the APK are plain downloads. The newest release newer than this build whose wire
 * and rules match the live server is the one to take — a release whose server is not deployed yet waits; without the
 * server's manifest, only a release on this build's own wire and rules is taken.
 */
class Updates(
    private val repo: String = REPO,
    client: OkHttpClient = ForgeHttp.client,
) {
    // GitHub answers assets by a redirect to its storage: unlike the game's own calls, these follow it.
    private val http = client.newBuilder().followRedirects(true).followSslRedirects(true)
        .readTimeout(60, TimeUnit.SECONDS).callTimeout(0, TimeUnit.SECONDS).build()

    /** The update for build [versionCode] named [versionName] against [server]; null - this build is the one. Throws when GitHub is out of reach. */
    suspend fun check(versionCode: Int, versionName: String, server: StaticManifest?): AvailableUpdate? = withContext(Dispatchers.IO) {
        val releases = WireJson.decodeFromString(ListSerializer(GitRelease.serializer()), text("https://api.github.com/repos/$repo/releases?per_page=$PAGE", api = true))
            .filter { !it.draft && !it.prerelease && Version.of(it.tag) > Version.of(versionName) }
            .sortedByDescending { Version.of(it.tag) }
        val wire = server?.let { it.revision to it.rules } ?: (API_REVISION to RULES_VERSION)
        for (release in releases) {
            coroutineContext.ensureActive()
            val meta = release.assets.firstOrNull { it.name == META } ?: continue
            val info = WireJson.decodeFromString(UpdateInfo.serializer(), text(meta.url))
            if (info.versionCode <= versionCode || (info.apiRevision to info.rules) != wire) continue
            val apk = release.assets.firstOrNull { it.name == info.apk } ?: continue
            val skipped = releases.filter { Version.of(it.tag) <= Version.of(release.tag) }
                .map { ReleaseNotes(it.tag.removePrefix("v"), it.body.orEmpty().lines().filterNot { line -> line.startsWith("## ") }.joinToString("\n").trim()) }
            return@withContext AvailableUpdate(info, apk.url, release.page, skipped)
        }
        null
    }

    /**
     * The APK of [update] into [target], [progress] in bytes read of all; its SHA-256 must be the one `update.json` names,
     * or the file is deleted and the download fails.
     */
    suspend fun download(update: AvailableUpdate, target: File, progress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val digest = MessageDigest.getInstance("SHA-256")
        http.newCall(Request.Builder().url(update.apkUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val total = response.body.contentLength().takeIf { it > 0 } ?: update.info.size
            response.body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        read += n
                        progress(read, total)
                    }
                }
            }
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        if (!sha.equals(update.info.sha256, ignoreCase = true)) {
            target.delete()
            throw IOException("SHA-256 mismatch")
        }
    }

    private fun text(url: String, api: Boolean = false): String {
        val request = Request.Builder().url(url).apply { if (api) header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28") }.build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.string()
        }
    }

    companion object {
        const val REPO = "Sperance/ExileForge"

        /** The asset CI publishes beside the APK. */
        const val META = "update.json"

        /** Releases read at once: more than any player skips. */
        private const val PAGE = 30

        /** How often a running app looks again. */
        const val PERIOD_MS = 60 * 60 * 1000L
    }
}

/** A `major.minor.patch` version, `v` or not; anything unreadable is the lowest. */
data class Version(val parts: List<Int>) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val c = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    companion object {
        fun of(text: String): Version = Version(text.removePrefix("v").split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 })
    }
}
