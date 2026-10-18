package com.sperance.exileforge.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

/**
 * Часы живых подписей времени (цикл работы, время в работе, срок до уровня, ожидание запуска): каждая подпись читает «сейчас» отсюда,
 * своих циклов `delay` не заводит. В игре часы идут ([TickingClock]), на снимках экранов стоят ([StillClock]) - снимок не ждёт тишины вечно.
 */
interface WallClock {
    /** Текущее время в мс, обновляется не реже, чем раз в [tickMs]. */
    @Composable fun now(tickMs: Long): State<Long>
}

/** Часы игры: время устройства, обновление раз в такт подписи. */
object TickingClock : WallClock {
    @Composable override fun now(tickMs: Long): State<Long> = produceState(System.currentTimeMillis(), tickMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(tickMs)
        }
    }
}

/** Стоящие часы снимков: «сейчас» всегда [at]. */
class StillClock(private val at: Long) : WallClock {
    @Composable override fun now(tickMs: Long): State<Long> = remember { mutableLongStateOf(at) }
}

/** Часы экрана; по умолчанию - [TickingClock]. */
val LocalWallClock = staticCompositionLocalOf<WallClock> { TickingClock }

/** «Сейчас» для живой подписи с тактом [tickMs]. */
@Composable fun rememberNow(tickMs: Long): State<Long> = LocalWallClock.current.now(tickMs)
