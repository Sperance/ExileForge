package com.sperance.exileforge.core.update

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Сборка, выложенная на сервере (3.82.0): `app/latest.json` - его пишет `scripts/apk.sh` сервера из `update.json` релиза
 * GitHub, дополнив заметками релиза. Версия сборки, провод и правила, на которых она говорит, имя APK, размер и SHA-256.
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
)

/** Обновление, которое клиент обязан взять: сборка и адрес её APK на сервере. */
data class AvailableUpdate(val info: AppBuild, val apkUrl: String)

/**
 * Обновления только с сервера игры (3.82.0): GitHub приложение больше не спрашивает - сборку туда кладёт администратор
 * командой `apk.sh`. Сервер без выложенной сборки отвечает 404: обновлений нет. Сборка на чужом проводе или правилах
 * (выложенная с `--force` раньше сервера) не предлагается, пока сервер её не догонит.
 */
class Updates(client: OkHttpClient = ForgeHttp.client) {
    // APK качается долго: без общего предела вызова, но с пределом молчания между байтами.
    private val http = client.newBuilder().readTimeout(60, TimeUnit.SECONDS).callTimeout(0, TimeUnit.SECONDS).build()

    // Описание сборки маленькое (3.81.1): вызов, который тянется, обрывается - проверка всегда возвращается.
    private val small = http.newBuilder().callTimeout(SMALL_CALL_S, TimeUnit.SECONDS).build()

    /**
     * Обновление сборки [versionCode] с сервера [server] (адрес с `/` на конце); [wire] - провод и правила живого сервера,
     * null - этой сборки. null - обновляться не на что. Бросает, когда сервер недоступен.
     */
    suspend fun check(server: String, versionCode: Int, wire: Pair<Int, Int>?): AvailableUpdate? = withContext(Dispatchers.IO) {
        val base = server.toHttpUrl()
        val text = small.newCall(Request.Builder().url(base.resolve(LATEST)!!).build()).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.string()
        }
        val info = WireJson.decodeFromString(AppBuild.serializer(), text)
        if (info.versionCode <= versionCode || (info.apiRevision to info.rules) != (wire ?: (API_REVISION to RULES_VERSION))) return@withContext null
        AvailableUpdate(info, base.resolve("$FOLDER/${info.apk}")!!.toString())
    }

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
            // Сервер не докачивает (200 вместо 206) - файл пишется заново.
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
        /** Каталог сборок на сервере и их описание. */
        const val FOLDER = "app"
        const val LATEST = "$FOLDER/latest.json"

        /** Дольше этого описание сборки не ждётся, с. */
        private const val SMALL_CALL_S = 20L

        /** Как часто запущенное приложение спрашивает снова. */
        const val PERIOD_MS = 60 * 60 * 1000L
    }
}
