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
    AccountRole.TESTER to (Icons.Outlined.Science to Elder),
)

/**
 * Роль аккаунта значком (3.88.7): у администратора, модератора и тестировщика - свой знак своего цвета рядом с именем,
 * у игрока - ничего; нажатие называет роль.
 */
@Composable fun RoleMark(role: AccountRole?, size: Dp = 16.dp) {
    val (icon, tint) = role?.let(ROLE_MARKS::get) ?: return
    val title = ui("moderation.role.${role.name}")
    Tipped({ Tip(title, tint = tint) }) { Icon(icon, title, tint = tint, modifier = Modifier.size(size)) }
}

/** Роль по имени с сервера («ADMIN»); незнакомое имя - игрок. */
fun accountRole(name: String?): AccountRole? = AccountRole.entries.firstOrNull { it.name == name }
