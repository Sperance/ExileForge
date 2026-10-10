package com.sperance.exileforge.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Rune

/** Цвет роли в гильдии (4.5.1): глава - золото, офицер - руна, участник - латунь. */
val GuildRole.tint: Color
    get() = when (this) {
        GuildRole.LEADER -> Caution
        GuildRole.OFFICER -> Rune
        GuildRole.MEMBER -> Brass
    }

/** Кольцо портрета роли в составе (4.6.3): глава - золото, офицер - руна, участник - бронзовая нить без выделения. */
val GuildRole.ring: Color
    get() = when (this) {
        GuildRole.LEADER -> Caution
        GuildRole.OFFICER -> Rune
        GuildRole.MEMBER -> Bronze
    }

/** Знак роли перед именем в составе (4.6.3): корона главы, щит офицера; у участника знака нет. */
private val GuildRole.glyph: String?
    get() = when (this) {
        GuildRole.LEADER -> "♛"
        GuildRole.OFFICER -> "⛨"
        GuildRole.MEMBER -> null
    }

/** Знак роли [role] её цветом; у роли без знака - ничего. */
@Composable fun GuildRoleMark(role: GuildRole, modifier: Modifier = Modifier) {
    role.glyph?.let { Text(it, modifier, color = role.tint, style = MaterialTheme.typography.labelLarge) }
}
