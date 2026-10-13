package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.ui.theme.*

/** Значок роли и его цвет; у обычного игрока значка нет. */
private val ROLE_MARKS: Map<AccountRole, Pair<ImageVector, Color>> = mapOf(
    AccountRole.ADMIN to (Icons.Outlined.WorkspacePremium to LifeRed),
    AccountRole.MODERATOR to (Icons.Outlined.Shield to Rune),
)

/**
 * Роль аккаунта значком (3.88.7): у администратора и модератора - свой знак своего цвета рядом с именем, у игрока - ничего;
 * нажатие называет роль. Тестировщик (4.12.x) - отметка героя: [TesterMark].
 */
@Composable fun RoleMark(role: AccountRole?, size: Dp = 16.dp) {
    val (icon, tint) = role?.let(ROLE_MARKS::get) ?: return
    val title = ui("moderation.role.${role.name}")
    Tipped({ Tip(title, tint = tint) }) { Icon(icon, title, tint = tint, modifier = Modifier.size(size)) }
}

/** Герой-тестировщик (4.12.x, сервер ROLE-01): свой знак рядом с именем героя; нажатие называет отметку. */
@Composable fun TesterMark(tester: Boolean, size: Dp = 16.dp) {
    if (!tester) return
    val title = ui("hero.tester_mark")
    Tipped({ Tip(title, tint = Elder) }) { Icon(Icons.Outlined.Science, title, tint = Elder, modifier = Modifier.size(size)) }
}

/** Роль по имени с сервера («ADMIN»); незнакомое имя - игрок. */
fun accountRole(name: String?): AccountRole? = AccountRole.entries.firstOrNull { it.name == name }
