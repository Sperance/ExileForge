package com.sperance.exileforge.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.TAB_CATALOG
import com.sperance.exileforge.presentation.state.TAB_CHECKS
import com.sperance.exileforge.presentation.state.TAB_EDITOR
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.AdminGrantPanel
import com.sperance.exileforge.ui.theme.Muted

/**
 * Everything an administrator can do, in one place.
 *
 * It is the only tab a player does not have, which is the point: the editor, the checks and the
 * catalogue used to be scattered — two of them behind the Account tab, the grant panel sitting
 * inside the Hero tab where a player would otherwise be reading their own stash. Gathering them
 * here leaves every other screen the same for everyone, which is what makes "look at it as a
 * player" below a real check rather than a guess.
 */
@Composable fun AdminScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(tr("Администратор", "Administrator"),
            tr("Инструменты, которых у игрока нет", "Tools a player does not have"), ForgeGlyphs.Scroll)

        ForgePanel {
            Engraved(tr("Экраны", "Screens"))
            OutlinedButton(enabled = !s.busy, onClick = { vm.tab(TAB_CATALOG) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Каталог", "Catalogue"))
            }
            OutlinedButton(enabled = !s.busy, onClick = { vm.tab(TAB_EDITOR) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Редактор", "Editor"))
            }
            OutlinedButton(enabled = !s.busy, onClick = { vm.tab(TAB_CHECKS) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Проверки", "Checks"))
            }
            Text(tr("Нижняя панель остаётся игровой: эти экраны открываются отсюда и закрываются ею же.",
                    "The bottom bar stays the game's: these open from here and the bar is the way back."),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }

        // Granting is done to a character, so it needs one chosen — which is why it says so itself
        // rather than being hidden when there is none.
        AdminGrantPanel(s, vm)

        ForgePanel {
            Engraved(tr("Режим", "Mode"))
            Text(if (s.adminTools) tr("Сейчас видно всё, включая эту вкладку.", "Everything is visible, this tab included.")
                 else tr("Сейчас приложение выглядит так, как его видит игрок.", "The app looks the way a player sees it."),
                color = Muted, style = MaterialTheme.typography.bodySmall)
            // Leaving admin mode hides this very tab, so the switch says where it lands you: the
            // way back is the same switch on the Account tab, and nothing else can turn it on.
            OutlinedButton(enabled = !s.busy, modifier = Modifier.fillMaxWidth(),
                onClick = { vm.mode(if (s.adminTools) AppMode.PLAYER else AppMode.ADMIN) }) {
                Text(if (s.adminTools) tr("Смотреть как игрок", "Look at it as a player")
                     else tr("Вернуть инструменты", "Bring the tools back"))
            }
            if (s.adminTools) Text(
                tr("Вкладка исчезнет из панели — вернуть её можно во вкладке «Аккаунт».",
                   "The tab will leave the bar — the Account tab brings it back."),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
