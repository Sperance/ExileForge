package com.sperance.exileforge.core.quests

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Шаг сюжета [step] главы [chapter], чей текст «после» ждёт показа поверх игры. */
data class StoryAfter(val chapter: Int, val step: String)

/**
 * Очередь показа окна сюжета поверх игры: текст «после» сданного или перечитываемого шага ([shown]). Живёт только в памяти: окно,
 * закрытое игроком, уходом в заход или закрытием игры, уже показано - на устройстве ничего не хранится, а пересоздание экрана
 * окно не теряет.
 */
class StoryOverlay {
    private val pending = MutableStateFlow<StoryAfter?>(null)

    /** Шаг, чей текст стоит к показу; null - окна нет. */
    val shown: StateFlow<StoryAfter?> = pending.asStateFlow()

    /** Показать текст «после» шага [after]; прежнее окно, если было, сменяется им. */
    fun show(after: StoryAfter) {
        pending.value = after
    }

    /**
     * Окно шага [after] закрыто игроком. Сравнение с заменой: если к показу уже встал другой шаг (сдача следующего, пока закрывалось
     * это), он остаётся.
     */
    fun dismiss(after: StoryAfter) {
        pending.compareAndSet(after, null)
    }

    /** Окно закрыто без игрока, каким бы оно ни было: уход в заход или смена героя. */
    fun clear() {
        pending.value = null
    }
}
