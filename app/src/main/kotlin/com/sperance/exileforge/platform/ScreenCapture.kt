package com.sperance.exileforge.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import com.sperance.exileforge.rules.content.FeedbackRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

/** Снимок экрана к отчёту об ошибке (4.8.0): кадр окна как есть, ещё не сжатый. */
class ScreenShot(val bitmap: Bitmap)

/**
 * Снимок экрана к отчёту об ошибке (4.8.0) - один на приложение: кнопки громкости ([VolumeChord]), жук и меню забега снимают
 * окно здесь, до открытия листа отчёта. [capture] - кадр окна через `PixelCopy`; [encode] - JPEG в base64 в пределах правил
 * сервера (`feedback.shotSide`, `feedback.shotBytes`): длинная сторона ужимается, затем качество JPEG понижается, пока снимок не
 * впишется.
 */
object ScreenCapture {
    /** Ступени качества JPEG, от лучшего; снимок, что не вписался и в последнюю, не прикладывается. */
    private val QUALITIES = 90 downTo 30 step 10

    /** Кадр окна [window]; null - окно ещё не нарисовано или копия не удалась. */
    suspend fun capture(window: Window): ScreenShot? {
        val view = window.decorView
        if (view.width <= 0 || view.height <= 0) return null
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { done ->
            runCatching {
                PixelCopy.request(window, bitmap, { result ->
                    done.resume(ScreenShot(bitmap).takeIf { result == PixelCopy.SUCCESS })
                }, Handler(Looper.getMainLooper()))
            }.onFailure { done.resume(null) }
        }
    }

    /** Кадр окна Activity, к которой ведёт [context]; null - Activity нет. */
    suspend fun capture(context: Context): ScreenShot? = context.activity()?.window?.let { capture(it) }

    /** Снимок [shot] в пределах [rules]: JPEG в base64; null - не вписался и в худшее качество. */
    suspend fun encode(shot: ScreenShot, rules: FeedbackRules): String? = withContext(Dispatchers.Default) {
        val source = shot.bitmap
        val scale = rules.shotSide.toFloat() / max(source.width, source.height)
        val sized = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).roundToInt().coerceAtLeast(1), (source.height * scale).roundToInt().coerceAtLeast(1), true)
        } else {
            source
        }
        val out = ByteArrayOutputStream()
        val bytes = QUALITIES.firstNotNullOfOrNull { quality ->
            out.reset()
            sized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray().takeIf { it.size <= rules.shotBytes }
        }
        if (sized !== source) sized.recycle()
        bytes?.let { Base64.getEncoder().encodeToString(it) }
    }

    private tailrec fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
}
