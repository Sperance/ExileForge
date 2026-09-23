package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
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
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The way in, and the first screen the app ever shows.
 *
 * Two doors, side by side: the device's own account, which needs nothing typed, and a login for
 * whoever has one. The first is the game; the second is how an administrator reaches the tools.
 *
 * The server's address lives here too, and that is not a convenience. Everything below this screen
 * is gated on a session, so a wrong address would otherwise lock the app with no way to correct it.
 */
@Composable fun AuthScreen(s: ForgeState, vm: ForgeViewModel, snackbar: SnackbarHostState) {
    Scaffold(containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbar) { data -> Snackbar(data, containerColor = PanelRaised, contentColor = Parchment, actionColor = Gold, shape = MaterialTheme.shapes.small) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().voidBackdrop()
            .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { LanguageCorner(s.lang, s.languages, vm::language) }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(84.dp).border(1.dp, Gold.copy(alpha = .5f), CutCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(48.dp))
            }
            Text("EXILE FORGE", style = MaterialTheme.typography.headlineMedium, color = GoldBright)
            Text(ui("app.title"), style = MaterialTheme.typography.labelSmall, color = Muted)
            if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)

            if (s.resumable) {
                InfoCard(ui("auth.offline_title"), ui("auth.offline_note"), failure = true)
                Button(enabled = !s.busy, onClick = vm::retryResume, modifier = Modifier.fillMaxWidth()) { Text(ui("auth.retry")) }
            }

            Button(enabled = !s.busy, onClick = vm::playOnThisDevice, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(ForgeGlyphs.Portal, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                Text(ui("auth.play"), style = MaterialTheme.typography.titleMedium)
            }
            Text(ui("auth.device_note"),
                color = Muted, style = MaterialTheme.typography.bodySmall)

            LoginPanel(s, vm)
            ServerPanel(s, vm)

            if (s.error && s.message != null) InfoCard(ui("auth.failed"), s.message, failure = true)
        }
    }
}

/** The login half: an administrator's way in, and anyone who was given credentials. */
@Composable private fun LoginPanel(s: ForgeState, vm: ForgeViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    var login by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    ForgePanel {
        Row(Modifier.fillMaxWidth().clickable(enabled = !s.busy) { open = !open },
            verticalAlignment = Alignment.CenterVertically) {
            Icon(ForgeGlyphs.Exile, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(ui("auth.by_login"), modifier = Modifier.weight(1f))
            Text(if (open) "−" else "+", color = Gold, style = MaterialTheme.typography.titleMedium)
        }
        if (open) {
            OutlinedTextField(login, { login = it }, enabled = !s.busy, label = { Text(ui("account.login")) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, enabled = !s.busy, label = { Text(ui("account.password")) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(enabled = !s.busy && login.isNotBlank() && password.isNotEmpty(),
                onClick = { vm.login(login, password); password = "" }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("account.sign_in"))
            }
            // Where the password goes and how long the session lasts are worth saying out loud.
            Text(ui("auth.password_note"),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The address, reachable before the gate so a wrong one never locks the app. */
@Composable private fun ServerPanel(s: ForgeState, vm: ForgeViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    ForgePanel {
        Row(Modifier.fillMaxWidth().clickable(enabled = !s.busy) { open = !open },
            verticalAlignment = Alignment.CenterVertically) {
            Icon(ForgeGlyphs.Portal, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(ui("account.server"))
                Text(s.server, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Text(if (open) "−" else "+", color = Gold, style = MaterialTheme.typography.titleMedium)
        }
        if (open) {
            OutlinedTextField(s.serverDraft, vm::serverDraft, enabled = !s.busy,
                label = { Text(ui("account.server_address")) },
                supportingText = { Text(ui("account.address_hint")) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = !s.busy, onClick = vm::connect, modifier = Modifier.fillMaxWidth()) {
                Text(ui("account.save_connect"))
            }
            Text(s.health, color = Muted, style = MaterialTheme.typography.bodySmall)
            // The identifier is not a secret, and naming an account in a support log needs it.
            Text(ui("auth.device", s.deviceId.takeLast(12)),
                color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** The language switch, which on this screen has no banner to live in. */
@Composable private fun LanguageCorner(lang: Lang, offered: List<Lang>, onLanguage: (Lang) -> Unit) {
    Row(Modifier.border(1.dp, Gold.copy(alpha = .35f), CutCornerShape(6.dp)), verticalAlignment = Alignment.CenterVertically) {
        offered.forEach { option ->
            val active = option == lang
            Text(option.short, color = if (active) Ink else Muted, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.background(if (active) Gold else Color.Transparent)
                    .clickable(enabled = !active) { onLanguage(option) }
                    .padding(horizontal = 9.dp, vertical = 6.dp))
        }
    }
}
