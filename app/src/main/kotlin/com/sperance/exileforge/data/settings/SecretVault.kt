package com.sperance.exileforge.data.settings

import com.sperance.exileforge.core.i18n.ui
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/** Хранилище ключей устройства не ответило вовремя (3.81.3): секрет есть, но прочесть или запечатать его сейчас нельзя. */
class SecretsUnavailable : IOException(ui("secrets.unavailable"))

/**
 * [SecretBox] за ограниченным вызовом (3.81.3). Каждое запечатывание и вскрытие - binder-вызов в хранилище ключей
 * устройства; на части телефонов он виснет насовсем, а на главном потоке при запуске замораживал весь старт - окно
 * запуска стояло над мёртвым кадром. Теперь вызов идёт в своём потоке и ждётся не дольше [LIMIT_MS]: зависший
 * остаётся позади (binder-вызов не отменить), а вместо ответа бросается [SecretsUnavailable].
 */
class SecretVault(private val box: Box = Box.Keystore) {
    /** Что запечатывает и вскрывает: хранилище ключей на устройстве, в тестах - подмена. */
    interface Box {
        fun seal(plain: String): String
        fun open(sealed: String): String?

        object Keystore : Box {
            override fun seal(plain: String) = SecretBox.seal(plain)
            override fun open(sealed: String) = SecretBox.open(sealed)
        }
    }

    private val calls = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun seal(plain: String): String = bounded { box.seal(plain) }

    /** null - значение больше не вскрывается (ключ ушёл вместе с данными приложения), как и прежде. */
    suspend fun open(sealed: String): String? = bounded { box.open(sealed) }

    private suspend fun <T> bounded(call: () -> T): T {
        val running = calls.async { runCatching(call) }
        return (withTimeoutOrNull(LIMIT_MS) { running.await() } ?: throw SecretsUnavailable()).getOrThrow()
    }

    private companion object {
        /** Дольше этого вызов хранилища ключей не ждётся. */
        const val LIMIT_MS = 5_000L
    }
}
