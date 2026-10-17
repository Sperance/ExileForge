package com.sperance.exileforge.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.app.ClientDevice
import com.sperance.exileforge.core.feedback.ReportTrail
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.reportPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Что о приложении и устройстве уходит в отчёт: среда ([environment] - ключи контекста, что сервер выносит в публичный
 * Issue: модель, система, экран, язык, сеть, память, время с запуска, энергосбережение), версия сборки и переходы экранов - в
 * след отчёта ([ReportTrail], с журналом запросов - последние действия). Ничего личного: ни аккаунта, ни имён, ни адресов.
 */
class ReportEvidence(context: Context, navigator: Navigator, trail: ReportTrail, scope: CoroutineScope) : ClientDevice {
    private val app = context.applicationContext

    override val version: String = BuildConfig.VERSION_NAME

    override val model: String = Build.MODEL.orEmpty()

    init {
        scope.launch { navigator.current.collect { trail.screen(reportPlace(it).name) } }
    }

    /** Среда устройства сейчас; не прочиталось - ключа нет. */
    override fun environment(): Map<String, String> = buildMap {
        put("model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        put("android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        runCatching {
            val metrics = app.resources.displayMetrics
            put("display", "${(metrics.widthPixels / metrics.density).toInt()}×${(metrics.heightPixels / metrics.density).toInt()} dp, ×${"%.2f".format(metrics.density)}")
        }
        put("locale", uiLanguage.code)
        runCatching { put("net", network()) }
        val runtime = Runtime.getRuntime()
        put("memory", "${(runtime.totalMemory() - runtime.freeMemory()) / MB} / ${runtime.maxMemory() / MB} МБ")
        put("uptime", "${(SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime()) / MINUTE} мин")
        runCatching { put("powerSave", if ((app.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode) "вкл" else "выкл") }
    }

    /** Вид сети: Wi-Fi, мобильная, проводная или нет сети. */
    private fun network(): String {
        val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return "нет"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "мобильная"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "проводная"
            else -> "другая"
        } + if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "" else " (без интернета)"
    }

    private companion object {
        const val MB = 1024 * 1024
        const val MINUTE = 60_000
    }
}
