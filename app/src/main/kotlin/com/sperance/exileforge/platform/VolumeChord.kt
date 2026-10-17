package com.sperance.exileforge.platform

import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.core.view.ViewCompat

/**
 * Аккорд громкости: Громкость↑ и Громкость↓ нажаты вместе - лист отчёта об ошибке. Одиночное нажатие
 * уходит системе как обычно ([onKey] его не берёт); вторая клавиша, нажатая не позже [WINDOW_MS] после первой, пока первая ещё
 * держится, - аккорд: её нажатие и отпускание съедаются, громкость второй клавишей не меняется.
 *
 * Уровня приложения: клавиши окна Activity приходят из неё, окна диалога (лист `ForgeSheet`, `Dialog`, `AlertDialog`) -
 * через его обёрнутый callback ([attach] окна), окна всплывающего меню и попапа (`DropdownMenu`, `Popup`) - слушателем
 * необработанных клавиш его вида ([attach] вида); [onChord] получает окно диалога, где сыгран аккорд (null - окно Activity или
 * попапа). Окно, потерявшее фокус, сбрасывает состояние ([reset]): отпускание клавиши в чужом окне не оставит её «держащейся».
 */
class VolumeChord(private val onChord: (top: Window?) -> Unit) {
    /** Время нажатия держащихся клавиш громкости по коду клавиши. */
    private val held = HashMap<Int, Long>()

    /** Вторая клавиша аккорда, пока держится: её события система не видит. */
    private val swallowed = HashSet<Int>()

    /** Событие клавиши [event] окна [top] (null - окно Activity); true - это аккорд, событие съедено. */
    fun onKey(event: KeyEvent, top: Window? = null): Boolean {
        val code = event.keyCode
        if (code !in KEYS) return false
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return code in swallowed
                val other = held.entries.firstOrNull { it.key != code }
                held[code] = event.eventTime
                if (other != null && event.eventTime - other.value <= WINDOW_MS) {
                    swallowed += code
                    onChord(top)
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

    /** Окно потеряло фокус: ни одна клавиша больше не считается держащейся. */
    fun reset() {
        held.clear()
        swallowed.clear()
    }

    /**
     * Аккорд в окне диалога [window]: его callback оборачивается - клавиши громкости идут сюда раньше диалога. Вернёт
     * отмену: прежний callback на место, если его с тех пор никто не сменил.
     */
    fun attach(window: Window): () -> Unit {
        val base = window.callback ?: return {}
        val wrapped = ChordCallback(base, window, this)
        window.callback = wrapped
        return { if (window.callback === wrapped) window.callback = base }
    }

    /**
     * Аккорд в окне попапа с видом [view] (у попапа нет своего `Window`): клавиши громкости, что не взял сам попап, идут сюда
     * раньше системы; потеря фокуса окном сбрасывает аккорд. Вернёт отмену.
     */
    fun attach(view: View): () -> Unit {
        val keys = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event -> onKey(event) }
        val focus = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus -> if (!hasFocus) reset() }
        ViewCompat.addOnUnhandledKeyEventListener(view, keys)
        view.viewTreeObserver.addOnWindowFocusChangeListener(focus)
        return {
            ViewCompat.removeOnUnhandledKeyEventListener(view, keys)
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focus)
        }
    }

    companion object {
        /** Окно одновременности аккорда, мс: вторая клавиша громкости позже этого после первой - уже не аккорд. */
        const val WINDOW_MS = 400L

        private val KEYS = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
    }
}

/** Callback окна диалога с аккордом: клавиши - сначала [chord], потеря фокуса сбрасывает его; прочее - как у [base]. */
private class ChordCallback(private val base: Window.Callback, private val window: Window, private val chord: VolumeChord) : Window.Callback by base {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = chord.onKey(event, window) || base.dispatchKeyEvent(event)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) chord.reset()
        base.onWindowFocusChanged(hasFocus)
    }
}
