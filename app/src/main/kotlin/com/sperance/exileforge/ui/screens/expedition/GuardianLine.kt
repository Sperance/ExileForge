package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.screens.crafts.duration
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Vital

/**
 * Строка о страже карты: одна на полосу захода, карту и вопрос об уходе. [mark] - короткое слово метки над картой (полный
 * [text] - в её подсказке), [state] - жив, отдыхает или повержен.
 */
internal data class GuardianLine(val text: String, val color: Color, val mark: String, val state: GuardianState) {
    companion object {
        /**
         * Страж жив, повержен в этом заходе или отдыхает после прошлой победы - с отсчётом до возвращения: отдыхающий
         * не зовётся поверженным, иначе пустой пост у портала казался бы ошибкой. [zone] - страж порчи зоны Ваал.
         */
        fun of(hud: RunHud, zone: Boolean = false): GuardianLine {
            val rest = hud.guardianRest
            return when {
                zone && hud.sealed -> GuardianLine(ui("vaal.guardian_alive"), LifeRed, ui("expedition.mark.vaal_alive"), GuardianState.ALIVE)
                zone -> GuardianLine(ui("vaal.guardian_slain"), Vital, ui("expedition.mark.vaal_slain"), GuardianState.SLAIN)
                hud.sealed -> GuardianLine(ui("expedition.boss_alive"), LifeRed, ui("expedition.mark.boss_alive"), GuardianState.ALIVE)
                rest != null -> duration(rest * 1000L).let { GuardianLine(ui("expedition.boss_rest", it), Muted, ui("expedition.mark.boss_rest", it), GuardianState.RESTING) }
                else -> GuardianLine(ui("expedition.boss_slain"), Vital, ui("expedition.mark.boss_slain"), GuardianState.SLAIN)
            }
        }
    }
}

/** Каков страж карты для метки над картой: жив, отдыхает после прошлой победы или повержен в этом заходе. */
internal enum class GuardianState { ALIVE, RESTING, SLAIN }
