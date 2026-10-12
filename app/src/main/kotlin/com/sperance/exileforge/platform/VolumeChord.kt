package com.sperance.exileforge.platform

import android.view.KeyEvent

/**
 * Аккорд громкости (4.8.0): Громкость↑ и Громкость↓ нажаты вместе - отчёт об ошибке со снимком экрана. Одиночное нажатие
 * уходит системе как обычно ([onKey] его не берёт); вторая клавиша, нажатая не позже [WINDOW_MS] после первой, пока первая ещё
 * держится, - аккорд: её нажатие и отпускание съедаются, громкость второй клавишей не меняется.
 */
class VolumeChord(private val onChord: () -> Unit) {
    /** Время нажатия держащихся клавиш громкости по коду клавиши. */
    private val held = HashMap<Int, Long>()

    /** Вторая клавиша аккорда, пока держится: её события система не видит. */
    private val swallowed = HashSet<Int>()

    /** Событие клавиши [event]; true - это аккорд, событие съедено. */
    fun onKey(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code !in KEYS) return false
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return code in swallowed
                val other = held.entries.firstOrNull { it.key != code }
                held[code] = event.eventTime
                if (other != null && event.eventTime - other.value <= WINDOW_MS) {
                    swallowed += code
                    onChord()
                    true
                } else {
                    false
                }
            }

            KeyEvent.ACTION_UP -> {
                held -= code
                swallowed.remove(code)
            }

            else -> false
        }
    }

    companion object {
        /** Окно одновременности аккорда, мс: вторая клавиша громкости позже этого после первой - уже не аккорд. */
        const val WINDOW_MS = 400L

        private val KEYS = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
    }
}
