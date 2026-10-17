package com.sperance.exileforge.presentation.world

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Один полёт загрузки: пока загрузка с ключом идёт, второй вызов с тем же ключом не шлёт своих запросов, а ждёт
 * её. Загрузка идёт в [scope] приложения: отменённый ждущий не обрывает её остальным.
 */
internal class SingleFlight<K>(private val scope: CoroutineScope) {
    private val lock = Mutex()
    private val flying = HashMap<K, Deferred<Unit>>()

    suspend fun run(key: K, block: suspend () -> Unit) {
        val flight = lock.withLock {
            flying[key]?.takeIf { it.isActive } ?: scope.async { block() }.also { flying[key] = it }
        }
        flight.await()
    }
}
