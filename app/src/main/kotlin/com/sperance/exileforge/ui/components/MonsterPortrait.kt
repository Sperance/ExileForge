package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits

/**
 * Портрет монстра [code] формы [form] вне боя - рисунок сервера; пока его нет - пустой жетон. [shadow] - тень вместо лица (вид ещё не
 * открыт). [portraits] - сколько портретов сервер уже прислал: читается здесь, чтобы рисунок обновился, как только они придут.
 */
@Composable fun MonsterPortrait(code: String, form: String, portraits: Int, modifier: Modifier = Modifier, shadow: Boolean = false) {
    key(portraits) {
        Canvas(modifier) { Portraits.monster(this, code, form, 0f, wash = if (shadow) Color.Black else null, washAmount = if (shadow) SHADOW else 0f) }
    }
}

/** Насколько тень закрывает портрет (предел затемнения портрета). */
private const val SHADOW = .7f
