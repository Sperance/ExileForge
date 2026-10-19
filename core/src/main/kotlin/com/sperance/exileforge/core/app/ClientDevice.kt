package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.model.command.BugReportRequest

/** Сборка и устройство глазами ядра: то, что знает только `:app` (`BuildConfig`, `android.os.Build`, службы системы). */
interface ClientDevice {
    /** Версия сборки (`versionName`). */
    val version: String

    /** Модель телефона. */
    val model: String

    /** Среда устройства для отчёта: модель, система, экран, язык, сеть, память; не прочиталось - ключа нет. */
    fun environment(): Map<String, String>
}

/** Отчёт прошлого запуска, что ещё не ушёл: падение или зависание, записанное на устройстве; [sent] стирает его запись. */
class LeftReport(val report: BugReportRequest, val sent: suspend () -> Unit)

/** Записи прошлых запусков на устройстве, что ждут отправки: падения или зависания. */
fun interface LeftReports {
    suspend fun pending(): List<LeftReport>

    /** Раз на старте, до [pending]: стирает устаревшие записи; по умолчанию стирать нечего. */
    suspend fun sweep() = Unit
}
