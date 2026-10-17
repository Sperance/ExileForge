package com.sperance.exileforge.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.AppMode
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_REDEMPTION
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.AdminGrantPanel
import com.sperance.exileforge.ui.theme.Muted
import org.koin.compose.viewmodel.koinViewModel

/**
 * Everything an administrator can do, in one place.
 *
 * Экран в основной сборке: вкладку видят администратор и модератор в режиме инструментов, промокоды и выдачи -
 * только администратор; права на каждое действие проверяет сервер.
 *
 * It is the only tab a player does not have, which is the point: the promo codes open from here,
 * and the grant panel sits here rather than inside the Hero tab where a player would otherwise be
 * reading their own stash. The catalogue, the editor and the checks left with the content:
 * the world is a set of files the server serves, not records to edit. Gathering what remains here
 * leaves every other screen the same for everyone, which is what makes "look at it as a player"
 * below a real check rather than a guess.
 */
@Composable fun AdminScreen() {
    val vm = koinViewModel<AdminViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    AdminPanels(game, vm)
}

/**
 * Панели админки по состоянию [game] (снимок экрана рисует их с ролью администратора).
 *
 * Промокоды и выдачи любому герою - только администратору, модератору - режим игрока. Выдача требует выбранного героя и говорит об этом
 * сама. Выход из режима администратора прячет эту вкладку, поэтому переключатель говорит, куда он ведёт: назад - тем же переключателем во
 * вкладке «Аккаунт».
 */
@Composable internal fun AdminPanels(game: GameUi, vm: AdminViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (game.isAdmin) {
            ForgePanel {
                Engraved(ui("admin.screens"))
                ForgeOutlinedButton(enabled = !game.busy, onClick = {
                    vm.openRedemptions()
                    vm.loadRedemptions()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("redemption.title"))
                }
                MutedText(ui("admin.screens_note"))
            }

            AdminGrantPanel(game, vm)
        }

        ForgePanel {
            Engraved(ui("admin.mode"))
            Text(
                if (game.adminTools) {
                    ui("admin.mode_all")
                } else {
                    ui("admin.mode_player")
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            ForgeOutlinedButton(
                enabled = !game.busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.mode(if (game.adminTools) AppMode.PLAYER else AppMode.ADMIN) },
            ) {
                Text(
                    if (game.adminTools) {
                        ui("admin.as_player")
                    } else {
                        ui("account.tools_back")
                    },
                )
            }
            if (game.adminTools) MutedText(ui("admin.as_player_note"))
        }
    }
}
