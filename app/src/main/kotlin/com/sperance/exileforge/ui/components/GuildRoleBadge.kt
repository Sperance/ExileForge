package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Rune

/** Цвет роли в гильдии (4.5.1): глава - золото, офицер - руна, участник - латунь. */
val GuildRole.tint: Color
    get() = when (this) {
        GuildRole.LEADER -> Caution
        GuildRole.OFFICER -> Rune
        GuildRole.MEMBER -> Brass
    }

/** Плашка роли в гильдии коротким словом - «ГЛ», «ОФ», «УЧ» - на подложке её цвета (состав, карточка игрока). */
@Composable fun GuildRoleBadge(role: GuildRole, modifier: Modifier = Modifier) {
    Text(
        ui("guild.role_short.${role.name}"),
        color = role.tint,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier.background(role.tint.copy(alpha = .15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
