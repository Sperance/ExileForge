package com.sperance.exileforge.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** За что телефон вибрирует, каждое включается отдельно. */
enum class Buzz { DANGER, BUTTON }

/**
 * Вибрации: все вибрации игры идут отсюда - экран с видом слушает [flow] и отзывает его вибромотором; выключенный в настройках вид
 * отбрасывается здесь по [allowed].
 */
class Buzzes {
    private val mutable = MutableSharedFlow<Buzz>(extraBufferCapacity = 4)
    val flow: SharedFlow<Buzz> = mutable

    /** Какие виды включены - ставит тот, кто держит настройки. */
    var allowed: (Buzz) -> Boolean = { true }

    fun buzz(kind: Buzz) {
        if (allowed(kind)) mutable.tryEmit(kind)
    }

    /** Пробная вибрация вида [kind], когда игрок включает его в настройках: мимо [allowed] - включение ещё в пути к настройкам. */
    fun probe(kind: Buzz) {
        mutable.tryEmit(kind)
    }
}
