package com.sperance.exileforge.platform

import android.app.Activity
import android.os.Build
import android.view.Display
import kotlin.math.abs

/**
 * Предел частоты кадров окна: 60 Гц. Экран 90/120 Гц гонял бы весь Compose вдвое чаще без видимой пользы для игры, а
 * батарею и тепло - нет. Окно просит режим экрана того же разрешения с частотой ближе всего к [TARGET_HZ]; экран только
 * на 60 Гц не меняется.
 */
object FrameRateCap {
    private const val TARGET_HZ = 60f

    fun apply(activity: Activity) {
        val display = display(activity) ?: return
        val current = display.mode
        val mode = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .minByOrNull { abs(it.refreshRate - TARGET_HZ) } ?: return
        val window = activity.window
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = mode.modeId
            preferredRefreshRate = mode.refreshRate
        }
    }

    private fun display(activity: Activity): Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.display
    } else {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay
    }
}
