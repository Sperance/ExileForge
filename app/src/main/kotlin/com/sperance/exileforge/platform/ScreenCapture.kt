package com.sperance.exileforge.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

/** Сжатый снимок экрана: байты [image] к отчёту и кадр [preview] того же размера для миниатюры и просмотра. */
class EncodedShot(val image: ByteArray, val preview: Bitmap)

/**
 * Снимок экрана к отчёту об ошибке: [capture] - кадр окна через `PixelCopy` (с окном диалога или листа поверх), [encode] - сжатие в пределы
 * правил `feedback`: длинная сторона ужимается до `shotSide`, затем качество понижается ступенями, пока снимок не впишется в `shotBytes`.
 * Формат - WebP с потерями (Android 11+), на старых - JPEG.
 */
object ScreenCapture {
    /** Ступени качества, от лучшего; снимок, что не вписался и в последнюю, не прикладывается. */
    private val QUALITIES = 85 downTo 30 step 11

    /** Полная непрозрачность канала цвета. */
    private const val OPAQUE = 255

    /**
     * Кадр окна Activity [base] с окном диалога [top] поверх: диалог ложится на своё место экрана, под ним - затемнение, если окно его просит
     * (`FLAG_DIM_BEHIND`: его рисует система, ни в одном кадре его нет). [top] null - только [base]; null - окно не нарисовано или копия
     * не удалась. Зовётся с главного потока.
     */
    suspend fun capture(base: Window, top: Window?): Bitmap? {
        val under = copy(base) ?: return top?.let { copy(it) }
        val over = top?.let { copy(it) } ?: return under
        val canvas = Canvas(under)
        val attributes = top.attributes
        if (attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0) {
            canvas.drawColor(Color.argb((attributes.dimAmount.coerceIn(0f, 1f) * OPAQUE).roundToInt(), 0, 0, 0))
        }
        val at = IntArray(2).also(top.decorView::getLocationOnScreen)
        val origin = IntArray(2).also(base.decorView::getLocationOnScreen)
        canvas.drawBitmap(over, (at[0] - origin[0]).toFloat(), (at[1] - origin[1]).toFloat(), null)
        over.recycle()
        return under
    }

    /** Кадр одного окна [window]; null - окно ещё не нарисовано или копия не удалась. */
    private suspend fun copy(window: Window): Bitmap? {
        val view = window.decorView
        if (view.width <= 0 || view.height <= 0) return null
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { done ->
            runCatching {
                PixelCopy.request(window, bitmap, { result ->
                    if (result != PixelCopy.SUCCESS) bitmap.recycle()
                    done.resume(bitmap.takeIf { result == PixelCopy.SUCCESS })
                }, Handler(Looper.getMainLooper()))
            }.onFailure {
                bitmap.recycle()
                done.resume(null)
            }
        }
    }

    /**
     * Кадр [frame] в пределах: длинная сторона не больше [side] пикселей, сжатый - не больше [maxBytes] байт; null - не вписался и в худшее
     * качество. Тяжёлое - зовётся вне главного потока; [frame] не трогает.
     */
    fun encode(frame: Bitmap, side: Int, maxBytes: Int): EncodedShot? {
        val scale = side.toFloat() / max(frame.width, frame.height)
        val sized = if (scale < 1f) {
            Bitmap.createScaledBitmap(frame, (frame.width * scale).roundToInt().coerceAtLeast(1), (frame.height * scale).roundToInt().coerceAtLeast(1), true)
        } else {
            frame.copy(Bitmap.Config.ARGB_8888, false)
        }
        val out = ByteArrayOutputStream()
        val bytes = QUALITIES.firstNotNullOfOrNull { quality ->
            out.reset()
            sized.compress(format(), quality, out)
            out.toByteArray().takeIf { it.size <= maxBytes }
        }
        if (bytes == null) sized.recycle()
        return bytes?.let { EncodedShot(it, sized) }
    }

    /** Формат сжатия: WebP с потерями, где он есть, иначе JPEG. */
    private fun format(): Bitmap.CompressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.JPEG
}
