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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.presentation.session.SessionViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.BugAction
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.LengthCounter
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.VersionLabel
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The way in, and the first screen the app ever shows.
 *
 * Two doors, side by side: the device's own account, which needs nothing typed, and a login for
 * whoever has one. The first is the game; the second is how an administrator reaches the tools.
 *
 * The server is fixed since 3.75.0; only the way back to it lives here, so an administrator's wrong address never
 * locks the app behind a gate that needs a session.
 */
@Composable fun AuthScreen(s: ForgeState) {
    val vm = koinViewModel<SessionViewModel>()
    Scaffold(containerColor = Ink) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().voidBackdrop()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                BugAction()
                LanguageCorner(s.lang, s.world.languages, vm::language)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(84.dp).border(1.dp, Gold.copy(alpha = .5f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(48.dp))
            }
            Text("EXILE FORGE", style = MaterialTheme.typography.headlineMedium, color = GoldBright)
            Text(ui("app.title"), style = MaterialTheme.typography.labelSmall, color = Muted)
            if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)

            if (s.account.resumable) {
                // By its cause (3.79.0): no network, a slow server, a restart, an answer not the game's; the server is asked again by itself.
                val outage = s.link.outage
                InfoCard(outage?.title ?: ui("auth.offline_title"), listOfNotNull(outage?.hint, ui("auth.offline_note"), s.link.detail?.takeIf { s.isAdmin }).joinToString("\n"), failure = true)
                ForgeButton(enabled = !s.busy, onClick = vm::retryResume, modifier = Modifier.fillMaxWidth()) { Text(ui("auth.retry")) }
            }

            ForgeButton(enabled = !s.busy, onClick = vm::playOnThisDevice, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(ForgeGlyphs.Portal, null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(ui("auth.play"), style = MaterialTheme.typography.titleMedium)
            }
            MutedText(ui("auth.device_note"))

            LoginPanel(s, vm)
            ServerReset(s, vm)

            s.refusal?.let { InfoCard(ui("auth.failed"), it.read(), failure = true) }
            // The build, dim at the foot of the way in (3.72.0).
            VersionLabel()
            // The id of the account names it in a support log; the secret of the device is never shown (3.48.0).
            s.account.profile?.id?.let { MutedText(ui("auth.device", it.takeLast(12)), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** The login half: an administrator's way in, and anyone who was given credentials. */
@Composable private fun LoginPanel(s: ForgeState, vm: SessionViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    var login by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    ForgePanel {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !s.busy) { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ForgeGlyphs.Exile, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(ui("auth.by_login"), modifier = Modifier.weight(1f))
            Text(if (open) "−" else "+", color = Gold, style = MaterialTheme.typography.titleMedium)
        }
        if (open) {
            OutlinedTextField(
                login,
                { login = it.take(s.inputs.login) },
                enabled = !s.busy,
                label = { Text(ui("account.login")) },
                supportingText = { LengthCounter(login, s.inputs.login) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                password,
                { password = it.take(s.inputs.password) },
                enabled = !s.busy,
                label = { Text(ui("account.password")) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            ForgeButton(
                enabled = !s.busy && login.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    vm.login(login, password)
                    password = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(ui("account.sign_in"))
            }
            // Where the password goes and how long the session lasts are worth saying out loud.
            MutedText(ui("auth.password_note"))
        }
    }
}

/**
 * The way back to the one server (3.75.0): there is no address to type before the gate, but an administrator who pointed
 * the device elsewhere from the Server page must not be locked out by a wrong one. Nothing while the device is on it.
 */
@Composable private fun ServerReset(s: ForgeState, vm: SessionViewModel) {
    if (s.account.server == DEFAULT_SERVER) return
    MutedText(ui("auth.other_server", s.account.server), style = MaterialTheme.typography.labelSmall)
    ForgeOutlinedButton(enabled = !s.busy, onClick = vm::resetServer, modifier = Modifier.fillMaxWidth()) { Text(ui("auth.server_reset")) }
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
