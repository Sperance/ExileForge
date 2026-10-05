package com.sperance.exileforge.core.session

/** Где связь с сервером на старте (3.86.0). */
enum class Reach { CONNECTING, ANSWERED, UNREACHABLE }

/** Решение окна запуска: ждать, отпустить в игру, сказать, что сервер молчит, или показать обязательное обновление. */
enum class StartVerdict { HOLD, RELEASE, UNREACHABLE, UPDATE }

/**
 * Окно запуска (3.86.0): держится только до ответа сервера - ответил, и окна нет, что бы ещё ни догружалось: мир и герой
 * догружаются на своих экранах. Обязательное обновление перекрывает всё. Сервер молчит - окно так и говорит и повторяет.
 * 3.87.0: дольше [LIMIT_MS] окно не держится ни при каком состоянии - бесконечной загрузки нет по построению, а связь
 * дальше показывает значок в игре.
 */
object StartGate {
    const val LIMIT_MS = 20_000L

    fun verdict(reach: Reach, mandatoryUpdate: Boolean, elapsedMs: Long = 0): StartVerdict = when {
        mandatoryUpdate -> StartVerdict.UPDATE
        reach == Reach.ANSWERED || elapsedMs >= LIMIT_MS -> StartVerdict.RELEASE
        reach == Reach.UNREACHABLE -> StartVerdict.UNREACHABLE
        else -> StartVerdict.HOLD
    }
}
