package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.DEFAULT_SERVER
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.VersionLabel
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.server.AccountRow
import com.sperance.exileforge.ui.screens.server.LoginSheet
import com.sperance.exileforge.ui.screens.server.RowGroup
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The way in, and the first screen the app ever shows.
 *
 * Two doors: the device's own account, which needs nothing typed, and a login for
 * whoever has one. The first is the game - крупная «Играть»; the second is how an administrator reaches the tools - строкой с листом,
 * в одном облике со страницей «Вход» Врат мира.
 *
 * The server is fixed; only the way back to it lives here, so an administrator's wrong address never
 * locks the app behind a gate that needs a session.
 *
 * Беда - по причине: нет сети, медленный сервер, перезапуск, чужой ответ; сервер спрашивается снова сам.
 */
@Composable fun AuthScreen() {
    val game by koinViewModel<SessionViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<SessionViewModel>()
    Scaffold(containerColor = Ink) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().voidBackdrop()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                LanguageCorner(game.lang, game.world.languages, vm::language)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(84.dp).border(1.dp, Gold.copy(alpha = .5f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(48.dp))
            }
            Text("EXILE FORGE", style = MaterialTheme.typography.headlineMedium, color = GoldBright)
            Text(ui("app.title"), style = MaterialTheme.typography.labelSmall, color = Muted)
            if (game.busy || game.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)

            if (game.session.resumable) {
                val outage = game.link.outage
                InfoCard(outage?.title ?: ui("auth.offline_title"), listOfNotNull(outage?.hint, ui("auth.offline_note"), game.link.detail?.takeIf { game.isAdmin }).joinToString("\n"), failure = true)
                ForgeButton(state = ButtonState.of(!game.busy), onClick = vm::retryResume, modifier = Modifier.fillMaxWidth()) { Text(ui("auth.retry")) }
            }

            ForgeButton(state = ButtonState.of(!game.busy), onClick = vm::playOnThisDevice, modifier = Modifier.fillMaxWidth(), size = ButtonSize.Wide) {
                Icon(ForgeGlyphs.Portal, null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(ui("auth.play"), style = MaterialTheme.typography.titleMedium)
            }
            MutedText(ui("auth.device_note"))

            LoginRow(game, vm)
            ServerReset(game, vm)

            game.refusal?.let { InfoCard(ui("auth.failed"), it.read(), failure = true) }
            VersionLabel()
        }
    }
}

/**
 * Вход по логину - строкой-действием в облике строк Врат мира: «Войти по логину» и «Если аккаунт уже есть»; касание открывает лист с
 * полями ([LoginSheet]), отказ входа - под полями листа.
 */
@Composable private fun LoginRow(game: GameUi, vm: SessionViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    RowGroup(null) {
        AccountRow(ForgeGlyphs.Exile, ui("auth.by_login"), note = ui("auth.by_login_note"), enabled = !game.busy) { open = true }
    }
    if (open) LoginSheet(game.inputs, game.busy, game.refusal?.read(), vm::login) { open = false }
}

/**
 * The way back to the one server: there is no address to type before the gate, but an administrator who pointed
 * the device elsewhere from the Server page must not be locked out by a wrong one. Nothing while the device is on it.
 */
@Composable private fun ServerReset(game: GameUi, vm: SessionViewModel) {
    if (game.session.server == DEFAULT_SERVER) return
    MutedText(ui("auth.other_server", game.session.server), style = MaterialTheme.typography.labelSmall)
    ForgeButton(state = ButtonState.of(!game.busy), onClick = vm::resetServer, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) { Text(ui("auth.server_reset")) }
}

/** The language switch, which on this screen has no banner to live in. */
@Composable private fun LanguageCorner(lang: Lang, offered: List<Lang>, onLanguage: (Lang) -> Unit) {
    Row(Modifier.border(1.dp, Gold.copy(alpha = .35f), RoundedCornerShape(6.dp)), verticalAlignment = Alignment.CenterVertically) {
        offered.forEach { option ->
            val active = option == lang
            Text(
                option.short,
                color = if (active) Ink else Muted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.background(if (active) Gold else Color.Transparent)
                    .clickable(enabled = !active) { onLanguage(option) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}
