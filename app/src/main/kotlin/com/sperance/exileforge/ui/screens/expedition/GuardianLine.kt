package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.screens.crafts.duration
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Vital

/** Строка о страже карты (3.95.2): одна на полосу захода, карту и вопрос об уходе. */
internal data class GuardianLine(val text: String, val color: Color) {
    companion object {
        /**
         * Страж жив, повержен в этом заходе или отдыхает после прошлой победы - с отсчётом до возвращения: прежде отдыхающий
         * звался поверженным, и пустой пост у портала казался ошибкой. [zone] - страж порчи зоны Ваал.
         */
        fun of(hud: RunHud, zone: Boolean = false): GuardianLine {
            val rest = hud.guardianRest
            return when {
                zone && hud.sealed -> GuardianLine(ui("vaal.guardian_alive"), LifeRed)
                zone -> GuardianLine(ui("vaal.guardian_slain"), Vital)
                hud.sealed -> GuardianLine(ui("expedition.boss_alive"), LifeRed)
                rest != null -> GuardianLine(ui("expedition.boss_rest", duration(rest * 1000L)), Muted)
                else -> GuardianLine(ui("expedition.boss_slain"), Vital)
            }
        }
    }
}
