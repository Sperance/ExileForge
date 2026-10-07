package com.sperance.exileforge.core.update

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Провод сборки или сервера: ревизия API и версия правил. Разные - друг с другом не играют. */
data class Wire(val api: Int, val rules: Int) {
    companion object {
        /** Провод этой сборки. */
        val OWN = Wire(API_REVISION, RULES_VERSION)
    }
}

/**
 * Сборка из релиза GitHub (3.86.0): `update.json` релиза - версия, провод, имя APK, размер, SHA-256 и заметки.
 */
@Serializable data class AppBuild(
    val versionCode: Int,
    val versionName: String,
    val apiRevision: Int,
    val rules: Int,
    val apk: String,
    val size: Long = 0,
    val sha256: String,
    val notes: String = "",
) {
    val wire: Wire get() = Wire(apiRevision, rules)
}

/** Заметки релиза (3.93.0): версия, дата публикации (ISO) и текст `RELEASE_NOTES.md` той версии. */
@Serializable data class ReleaseNotes(
    @kotlinx.serialization.SerialName("tag_name") val tag: String,
    @kotlinx.serialization.SerialName("published_at") val published: String? = null,
    val body: String = "",
) {
    val version: String get() = tag.removePrefix("v")
}

/** Найденная сборка: что это, откуда качать APK и страница релиза для браузера. Любая найденная обязательна. */
data class AvailableUpdate(val info: AppBuild, val apkUrl: String, val pageUrl: String)

/**
 * Нужна ли сборка. Обязательна любая новее этой, что говорит на проводе сервера (или этой сборки, пока сервер не
 * ответил): сервер на другом проводе - старая играть не может, на том же - всё равно ставится до входа. Сборка на
 * проводе, которого сервер ещё не знает, не предлагается: она не войдёт.
 */
object UpdatePolicy {
    fun required(build: AppBuild, versionCode: Int, own: Wire, server: Wire?): Boolean = build.versionCode > versionCode && build.wire == (server ?: own)
}

/**
 * Обновления только с GitHub Releases (3.86.0): последний релиз отдаёт `update.json` и APK по постоянным адресам,
 * GitHub уводит их редиректом на свой CDN. Нет релиза (404) - обновлений нет.
 */
class Updates(client: OkHttpClient = ForgeHttp.client) {
    // APK качается долго: без общего предела вызова, но с пределом молчания между байтами. Редиректы GitHub - свои.
    private val http = client.newBuilder().followRedirects(true).followSslRedirects(true)
        .readTimeout(60, TimeUnit.SECONDS).callTimeout(0, TimeUnit.SECONDS).build()

    // Описание сборки маленькое: вызов, который тянется, обрывается - проверка всегда возвращается.
    private val small = http.newBuilder().callTimeout(SMALL_CALL_S, TimeUnit.SECONDS).build()

    /** Обновление сборки [versionCode] для сервера на проводе [server] (null - не ответил); null - обновляться не на что. */
    suspend fun check(versionCode: Int, server: Wire?): AvailableUpdate? = withContext(Dispatchers.IO) {
        val text = small.newCall(Request.Builder().url(LATEST).build()).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.string()
        }
        val info = WireJson.decodeFromString(AppBuild.serializer(), text)
        info.takeIf { UpdatePolicy.required(it, versionCode, Wire.OWN, server) }?.available()
    }

    /** Заметки [count] последних релизов (3.93.0), новые первыми; GitHub не ответил - ошибка. */
    suspend fun recent(count: Int = RECENT): List<ReleaseNotes> = withContext(Dispatchers.IO) {
        val text = small.newCall(Request.Builder().url("$API_RELEASES?per_page=$count").header("Accept", "application/vnd.github+json").build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.string()
        }
        NOTES_JSON.decodeFromString(ListSerializer(ReleaseNotes.serializer()), text).take(count)
    }

    private fun AppBuild.available() = AvailableUpdate(this, "$RELEASES/download/v$versionName/$apk", "$RELEASES/tag/v$versionName")

    /**
     * APK [update] в [target], [progress] - прочитано байт из всех. Начатый файл докачивается с места обрыва (`Range`);
     * SHA-256 целого файла обязан совпасть с объявленным, иначе файл удаляется и загрузка падает.
     */
    suspend fun download(update: AvailableUpdate, target: File, progress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val have = target.length().takeIf { target.isFile && it in 1 until update.info.size } ?: 0L
        if (have == 0L) target.delete()
        val request = Request.Builder().url(update.apkUrl).apply { if (have > 0) header("Range", "bytes=$have-") }.build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            // Докачки нет (200 вместо 206) - файл пишется заново.
            val resumed = response.code == 206
            val start = if (resumed) have else 0L
            val total = update.info.size.takeIf { it > 0 } ?: (start + response.body.contentLength())
            response.body.byteStream().use { input ->
                java.io.FileOutputStream(target, resumed).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = start
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        progress(read, total)
                    }
                }
            }
        }
        if (!sha256(target).equals(update.info.sha256, ignoreCase = true)) {
            target.delete()
            throw IOException("SHA-256 mismatch")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Релизы приложения. */
        const val RELEASES = "https://github.com/Sperance/ExileForge/releases"

        /** Релизы через API GitHub (3.93.0): заметки последних версий. */
        const val API_RELEASES = "https://api.github.com/repos/Sperance/ExileForge/releases"

        /** Сколько последних версий показывает «Что нового». */
        const val RECENT = 3

        private val NOTES_JSON = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        /** Описание последнего релиза. */
        const val LATEST = "$RELEASES/latest/download/update.json"

        /** Дольше этого описание сборки не ждётся, с. */
        private const val SMALL_CALL_S = 20L

        /** Как часто запущенное приложение спрашивает снова. */
        const val PERIOD_MS = 60 * 60 * 1000L
    }
}
