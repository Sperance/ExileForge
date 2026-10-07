package com.sperance.exileforge.core.session

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ответ чтения на срок (3.94.1): для того же [tag] (обычно id героя) моложе [maxAgeMs] он отдаётся из памяти, иначе читается
 * заново; неудача не запоминается. Два экрана с одними данными и повторное открытие спрашивают сервер раз в срок.
 */
class Remembered<T : Any>(private val maxAgeMs: Long = READ_FRESH_MS) {
    private val lock = Mutex()
    private var held: Held<T>? = null

    private class Held<T>(val tag: String, val at: Long, val value: T)

    suspend fun get(tag: String, load: suspend () -> T): T = lock.withLock {
        val now = System.currentTimeMillis()
        held?.takeIf { it.tag == tag && now - it.at < maxAgeMs }?.value ?: load().also { held = Held(tag, now, it) }
    }

    fun forget() {
        held = null
    }
}
