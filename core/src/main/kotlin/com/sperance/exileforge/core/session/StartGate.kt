package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.update.Wire

/** Где связь с сервером на старте. */
enum class Reach { CONNECTING, ANSWERED, UNREACHABLE }

/**
 * Провод сервера против провода этой сборки: [MATCH] - играют вместе; [GAME_BEHIND] - сервер новее (игре нужно
 * обновление); [SERVER_BEHIND] - сборка новее сервера (сервер ещё обновляется).
 */
enum class WireFit {
    MATCH,
    GAME_BEHIND,
    SERVER_BEHIND,
    ;

    companion object {
        /** Провод сервера [server] против [own]: ревизия API решает первой, равная - версия правил. */
        fun of(server: Wire, own: Wire = Wire.OWN): WireFit = when {
            server == own -> MATCH
            server.api > own.api || server.api == own.api && server.rules > own.rules -> GAME_BEHIND
            else -> SERVER_BEHIND
        }
    }
}

/**
 * Что окно запуска показывает сейчас (одно окно запуска): [HOLD] - запуск идёт (связь, версия,
 * мир устройства), [WARMING] - мир героя догружается, [UNREACHABLE] - сервер молчит, [UPDATE] - обязательное обновление,
 * [AWAIT_GAME] / [AWAIT_SERVER] - провода не совпали, ждём новую сборку или сервер; [RELEASE] - окна нет.
 */
enum class StartVerdict(val shown: Boolean = true) { HOLD, WARMING, RELEASE(shown = false), UNREACHABLE, UPDATE, AWAIT_GAME, AWAIT_SERVER }

/**
 * Что знает окно запуска: связь [reach] и провод ответившего сервера [fit]; найдена ли обязательная сборка [mandatoryUpdate] и
 * идёт ли проверка [checking]; дочитан ли мир с устройства [deviceReady]; идёт ли прогрев героя [warming]; сколько идёт старт
 * [elapsedMs] и отпущен ли он уже [released]; идёт ли бой [busy] - бой окно не закрывает.
 */
data class StartInput(
    val reach: Reach,
    val fit: WireFit? = null,
    val mandatoryUpdate: Boolean = false,
    val checking: Boolean = false,
    val deviceReady: Boolean = true,
    val warming: Boolean = false,
    val elapsedMs: Long = 0,
    val released: Boolean = false,
    val busy: Boolean = false,
)

/**
 * Окно запуска (одно окно запуска, обновления и прогрева): старт держится, пока сервер не ответил, сборка не
 * проверена и мир устройства не дочитан - но не дольше [LIMIT_MS] (бесконечной загрузки нет по построению). Обязательное
 * обновление и несовпавший провод сервера держат окно сколько нужно: игра с таким сервером не играет. Прогрев героя - тоже окно.
 * Отпущенный старт ([StartInput.released]) больше не возвращается; бой ([StartInput.busy]) окно не закрывает - обновление ждёт его
 * конца.
 */
object StartGate {
    const val LIMIT_MS = 20_000L

    fun verdict(input: StartInput): StartVerdict = with(input) {
        when {
            busy -> StartVerdict.RELEASE
            mandatoryUpdate -> StartVerdict.UPDATE
            fit == WireFit.GAME_BEHIND -> StartVerdict.AWAIT_GAME
            fit == WireFit.SERVER_BEHIND -> StartVerdict.AWAIT_SERVER
            warming -> StartVerdict.WARMING
            released || elapsedMs >= LIMIT_MS -> StartVerdict.RELEASE
            reach == Reach.ANSWERED -> if (checking || !deviceReady) StartVerdict.HOLD else StartVerdict.RELEASE
            reach == Reach.UNREACHABLE -> StartVerdict.UNREACHABLE
            else -> StartVerdict.HOLD
        }
    }
}
