package com.sperance.exileforge.core.session

/** Где связь с сервером на старте (3.86.0). */
enum class Reach { CONNECTING, ANSWERED, UNREACHABLE }

/** Решение окна запуска: ждать, отпустить в игру, сказать, что сервер молчит, или показать обязательное обновление. */
enum class StartVerdict { HOLD, RELEASE, UNREACHABLE, UPDATE }

/**
 * Окно запуска (3.86.0): держится только до ответа сервера - ответил, и окна нет, что бы ещё ни догружалось: мир и герой
 * догружаются на своих экранах. Обязательное обновление перекрывает всё. Сервер молчит - окно так и говорит и повторяет.
 */
object StartGate {
    fun verdict(reach: Reach, mandatoryUpdate: Boolean): StartVerdict = when {
        mandatoryUpdate -> StartVerdict.UPDATE
        reach == Reach.ANSWERED -> StartVerdict.RELEASE
        reach == Reach.UNREACHABLE -> StartVerdict.UNREACHABLE
        else -> StartVerdict.HOLD
    }
}
