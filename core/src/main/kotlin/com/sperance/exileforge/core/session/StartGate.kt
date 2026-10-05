package com.sperance.exileforge.core.session

/** Что окно запуска знает о старте в этот миг (3.84.4). */
data class StartSignals(
    /** Свои шаги запуска позади: язык, сервер, сессия - начата или её нет. */
    val startupDone: Boolean,
    /** Идёт команда: вход, чтение героев. */
    val sessionBusy: Boolean,
    /** Контент мира ещё в пути. */
    val contentLoading: Boolean,
    /** Контент мира прочитан. */
    val contentReady: Boolean,
    /** Сервер ответил хотя бы на один сетевой шаг старта. */
    val serverAnswered: Boolean,
    /** Сетевой шаг старта упал: нет связи, таймаут, отказ. */
    val serverFailed: Boolean,
    /** Шагов запуска, что ещё идут. */
    val running: Int,
    /** Сколько идёт запуск. */
    val elapsedMs: Long,
)

/** Решение окна запуска: держать, отпустить в игру или сказать, что сервер не отвечает. */
enum class StartVerdict { HOLD, RELEASE, UNREACHABLE }

/**
 * Окно запуска (3.84.4): отпускает, как только старт сделал своё, - или сервер ответил, контент на месте и ни один шаг не
 * идёт, даже если какой-то флаг занятости задержался: окно не ждёт вечно того, чего в журнале уже нет. Сервер молчит
 * [UNREACHABLE_AFTER_MS] или его шаг упал раньше любого ответа - окно говорит это сразу, а не крутится.
 */
object StartGate {
    const val UNREACHABLE_AFTER_MS = 8_000L

    fun verdict(s: StartSignals): StartVerdict = when {
        s.startupDone && !s.sessionBusy && !s.contentLoading -> StartVerdict.RELEASE
        s.serverAnswered && s.contentReady && s.running == 0 -> StartVerdict.RELEASE
        !s.serverAnswered && (s.serverFailed || s.elapsedMs >= UNREACHABLE_AFTER_MS) -> StartVerdict.UNREACHABLE
        else -> StartVerdict.HOLD
    }
}
