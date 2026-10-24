package com.sperance.exileforge.platform

import android.graphics.Bitmap
import android.view.Window
import com.sperance.exileforge.core.app.GameSlice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Снимок экрана к листу отчёта об ошибке - на каком он шаге. */
sealed interface BugShot {
    /** Снимка нет и не будет: лист закрыт или правила `feedback` ещё не прочитаны - строки снимка в листе нет. */
    data object Absent : BugShot

    /** Кадр снят, идёт сжатие. */
    data object Taking : BugShot

    /** Снять или сжать не вышло: отчёт уйдёт без снимка. */
    data object Failed : BugShot

    /** Готов: сжатые байты [image] к отчёту и кадр [preview] для миниатюры и просмотра. */
    class Ready(val image: ByteArray, val preview: Bitmap) : BugShot {
        /** Отношение ширины кадра к высоте - пропорции миниатюры. */
        val aspect: Float get() = preview.width.toFloat() / preview.height
    }
}

/**
 * Снимок экрана к отчёту об ошибке - один на приложение: аккорд громкости ([VolumeChord]) и строка настроек «Сообщить об ошибке» снимают
 * окно здесь ([snap]) до того, как откроется лист, - в кадре то, о чём жалоба, а не сам лист. Кадр снимается на главном потоке, сжатие
 * ([ScreenCapture.encode]) идёт в [scope] на `Dispatchers.Default` в пределах правил `feedback` контента [slice]; новый снимок сменяет
 * идущее сжатие. Закрытый лист снимок бросает ([drop]).
 */
class BugShots(private val slice: GameSlice, private val scope: CoroutineScope) {
    private val current = MutableStateFlow<BugShot>(BugShot.Absent)
    val state: StateFlow<BugShot> = current

    private var encoding: Job? = null

    /** Снять окно Activity [base] с окном диалога [top] поверх (null - без него). Зовётся с главного потока, до открытия листа. */
    suspend fun snap(base: Window, top: Window?) {
        drop()
        val rules = slice.ui.value.index?.rules?.feedback ?: return
        current.value = BugShot.Taking
        val frame = ScreenCapture.capture(base, top) ?: run {
            current.value = BugShot.Failed
            return
        }
        encoding = scope.launch(Dispatchers.Default) {
            val shot = try {
                ScreenCapture.encode(frame, rules.shotSide, rules.shotBytes)
            } finally {
                frame.recycle()
            }
            ensureActive()
            current.compareAndSet(BugShot.Taking, shot?.let { BugShot.Ready(it.image, it.preview) } ?: BugShot.Failed)
        }
    }

    /** Лист закрыт: снимок не нужен, идущее сжатие останавливается. */
    fun drop() {
        encoding?.cancel()
        encoding = null
        current.value = BugShot.Absent
    }
}
