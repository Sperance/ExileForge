package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Muted

/** Что открывает «Осмотр» удержанием: своя строка подсказки на каждое; новое - запись перечисления. */
enum class HoldSubject {
    /** Карточка умения (`LocalSkillInspector`). */
    SKILL,

    /** Карточка предмета. */
    ITEM,
    ;

    /** Строка подсказки: ключи словаря - буквально, их полноту видит `UiStringsTest`. */
    val text: String get() = when (this) {
        SKILL -> ui("hint.hold.skill")
        ITEM -> ui("hint.hold.item")
    }
}

/**
 * «Осмотр»: где касание уже действует (выбирает, отмечает, применяет), сведения открывает удержание - и место
 * говорит это серой строкой со значком касания. Одна подсказка на список или лист, над ним или под ним - не на каждой строке.
 */
@Composable fun HoldHint(subject: HoldSubject, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.TouchApp, null, tint = Muted, modifier = Modifier.size(14.dp))
        MutedText(subject.text, style = MaterialTheme.typography.labelSmall)
    }
}
