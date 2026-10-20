package com.sperance.exileforge.platform

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.sperance.exileforge.core.session.Buzz

/**
 * Вибрация игры своим вибромотором ([Vibrator]), а не виброоткликом окна: системный «отклик на касание» её не глушит - на части
 * телефонов он выключен с завода, а с Android 13 обойти его флагом окна нельзя. У каждого вида [Buzz] свой рисунок ([effect]).
 * Атрибуты - игровые (`USAGE_MEDIA`, до Android 13 - `USAGE_GAME`): касательные (`USAGE_TOUCH`) система гасит вместе с откликом на
 * касание. Что из видов включено, решают настройки игры в `Buzzes` ядра; телефон без вибромотора молчит.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = vibratorOf(context)?.takeIf { it.hasVibrator() }

    /** Отзывает вид [kind] вибрацией. */
    fun play(kind: Buzz) {
        val motor = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            motor.vibrate(effect(kind), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
        } else {
            @Suppress("DEPRECATION")
            motor.vibrate(effect(kind), GAME_AUDIO)
        }
    }

    /**
     * Рисунок вида: кнопка и ремесло - короткий щелчок (готовый `EFFECT_CLICK` с Android 10, раньше - импульс [CLICK_MS]), опасность в
     * бою - двойной тяжёлый удар на полной силе.
     */
    private fun effect(kind: Buzz): VibrationEffect = when (kind) {
        Buzz.BUTTON -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(CLICK_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }

        Buzz.DANGER -> VibrationEffect.createWaveform(DANGER_TIMINGS, DANGER_AMPLITUDES, NO_REPEAT)
    }

    private companion object {
        /** Длина щелчка кнопки до Android 10, мс. */
        const val CLICK_MS = 20L

        /** Двойной удар опасности: пауза и удар, мс, - дважды. */
        val DANGER_TIMINGS = longArrayOf(0, 70, 90, 70)

        /** Сила каждого шага [DANGER_TIMINGS]: удары - на полной. */
        val DANGER_AMPLITUDES = intArrayOf(0, 255, 0, 255)

        /** Рисунок не повторяется. */
        const val NO_REPEAT = -1

        /** Атрибуты игры для вибрации до Android 13. */
        val GAME_AUDIO: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()

        /** Вибромотор устройства: с Android 12 - основной у [VibratorManager]. */
        fun vibratorOf(context: Context): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
