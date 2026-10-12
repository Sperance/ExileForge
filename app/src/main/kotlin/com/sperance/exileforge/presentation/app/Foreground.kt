package com.sperance.exileforge.presentation.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Игра на переднем плане или в фоне (4.9.0): фоновые циклы опроса (проба офлайна, повтор манифеста старта, будильник
 * цикла ремесла) ждут возврата, а не будят сеть, пока игрока нет. Отправка очереди команд и журнала похода паузы не
 * знает: уход в фон как раз отправляет их.
 */
class Foreground {
    private val mutable = MutableStateFlow(true)
    val inFront: StateFlow<Boolean> = mutable.asStateFlow()

    /** Игра ушла с переднего плана. */
    fun away() {
        mutable.value = false
    }

    /** Игра снова на переднем плане: ждущие циклы идут дальше. */
    fun back() {
        mutable.value = true
    }

    /** Ждёт переднего плана; на нём - сразу. */
    suspend fun awaitFront() {
        mutable.first { it }
    }
}
