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
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.admin.AdminViewModel
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.TAB_REDEMPTION
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.AdminGrantPanel
import com.sperance.exileforge.ui.theme.Muted
import org.koin.compose.viewmodel.koinViewModel

/**
 * Everything an administrator can do, in one place.
 *
 * It is the only tab a player does not have, which is the point: the promo codes open from here,
 * and the grant panel sits here rather than inside the Hero tab where a player would otherwise be
 * reading their own stash. The catalogue, the editor and the checks left with the content (3.0.0):
 * the world is a set of files the server serves, not records to edit. Gathering what remains here
 * leaves every other screen the same for everyone, which is what makes "look at it as a player"
 * below a real check rather than a guess.
 */
@Composable fun AdminScreen() {
    val vm = koinViewModel<AdminViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            ui("account.administrator"),
            ui("admin.subtitle"),
            ForgeGlyphs.Scroll,
        )

        ForgePanel {
            Engraved(ui("admin.screens"))
            ForgeOutlinedButton(enabled = !game.busy, onClick = {
                vm.tab(TAB_REDEMPTION)
                vm.loadRedemptions()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("redemption.title"))
            }
            MutedText(ui("admin.screens_note"))
        }

        // Granting is done to a hero, so it needs one chosen — which is why it says so itself
        // rather than being hidden when there is none.
        AdminGrantPanel(game, vm)

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
            // Leaving admin mode hides this very tab, so the switch says where it lands you: the
            // way back is the same switch on the Account tab, and nothing else can turn it on.
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
