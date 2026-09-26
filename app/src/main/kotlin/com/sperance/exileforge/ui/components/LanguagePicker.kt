package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The tongue every label speaks.
 *
 * Each language is written in itself, so a picker reads correctly to somebody who cannot read the
 * one currently shown. The list comes from the server's manifest rather than from [Lang]: the
 * client could speak a language the server cannot name a single item in, and half a translation
 * is worse than none.
 *
 * It used to be two runes in the banner. Three languages is already a crowd there, and a language
 * is a setting rather than an action, so it sits where settings sit - on the Account tab, and on
 * the sign-in screen beside the server address, because behind the gate there is no way back.
 */
@Composable fun LanguagePicker(current: Lang, offered: List<Lang>, enabled: Boolean = true, onLanguage: (Lang) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        offered.forEach { option ->
            val active = option == current
            val accent = if (active) Gold else Bronze
            Row(Modifier.fillMaxWidth()
                .border(1.dp, accent.copy(alpha = if (active) .55f else .25f), RoundedCornerShape(8.dp))
                .background(if (active) Gold.copy(alpha = .12f) else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable(enabled = enabled && !active) { onLanguage(option) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(option.short, style = MaterialTheme.typography.labelSmall, color = if (active) GoldBright else Muted,
                    modifier = Modifier.widthIn(min = 26.dp))
                Text(option.title, style = MaterialTheme.typography.labelLarge, color = if (active) Parchment else Muted,
                    modifier = Modifier.weight(1f))
                if (active) Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(16.dp))
            }
        }
    }
}
