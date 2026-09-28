package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.contract.SERVER_BRANCH
import com.sperance.exileforge.core.contract.SERVER_COMMIT
import com.sperance.exileforge.core.contract.SERVER_VERSION
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.ContentFiles
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel

@Composable internal fun ServerScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog> = emptyList()) {
    var promoOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(ui("account.title"), ui("account.subtitle"), ForgeGlyphs.Portal)
        ForgePanel {
            Engraved(ui("account.exile"))
            // A hero is swapped by leaving the game, never from inside a tab: every button below is
            // bound to the one chosen in the menu, and this is the way back to it. The loaded hero
            // names themself; before the snapshot lands, the menu's row does.
            if (s.heroInfo != null || s.heroRow != null) {
                PropertyRow(ui("common.character"), s.heroName + ui("app.hero_level", s.heroLevel), Glyph.CHARACTER)
            }
            ForgeOutlinedButton(enabled = !s.busy, onClick = vm::leaveGame, modifier = Modifier.fillMaxWidth()) {
                Text(ui("account.change_character"))
            }
            // A reward is paid to a hero, not to an account, so the code is asked for where the hero
            // being played is already named — and the dialog names them again.
            ForgeOutlinedButton(enabled = !s.busy && s.play.heroId.isNotBlank(), onClick = { promoOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("account.enter_promo"))
            }
            LoginForm(s, vm)
        }
        // Every administrator tool moved to its own tab in 2.3.0. What stays here is the way back
        // into it: turning the tools off hides that tab, so the switch cannot live only inside it.
        if (s.isAdmin && !s.adminTools) ForgePanel {
            Engraved(ui("account.administrator"))
            MutedText(ui("account.tools_hidden"))
            ForgeOutlinedButton(enabled = !s.busy, onClick = { vm.mode(AppMode.ADMIN) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("account.tools_back"))
            }
        }
        ForgePanel {
            Engraved(ui("account.language"))
            LanguagePicker(s.lang, s.world.languages, enabled = !s.busy, onLanguage = vm::language)
            MutedText(ui("account.language_note"))
            // Names of things belong to the server since 0.14.0: without its dictionary the screens
            // print codes, so how much of it arrived is worth saying out loud.
            if (s.world.localeStrings > 0) PropertyRow(ui("account.dictionary"),
                "${s.world.localeLanguage.uppercase()} · " + ui("account.strings", s.world.localeStrings), Glyph.TEXT)
            else MutedText(ui("account.dictionary_missing"))
            // Drawings come from the server too, and a missing set is invisible by design: every
            // hole falls back to a bundled emblem, so the count is the only way to notice one.
            if (s.world.iconKeys > 0) PropertyRow(ui("account.icons"),
                ui("account.icons_count", s.world.iconKeys, s.world.iconSprites), Glyph.IMAGE)
            else MutedText(ui("account.icons_missing"))
            if (s.world.portraits > 0) PropertyRow(ui("account.portraits"), s.world.portraits.toString(), Glyph.IMAGE)
            // The world's tables come the same way (3.0.0): chunks fetched once per fingerprint and
            // parsed by the rules. The fingerprint is what tells a changed world from the one on screen.
            if (s.index != null) PropertyRow(ui("account.content"),
                "${s.world.contentHash.take(12)} · " + ui("account.chunks", ContentFiles.ALL.size), Glyph.SERVER)
            else MutedText(ui("account.content_missing"))
            ForgeOutlinedButton(enabled = !s.busy, onClick = { vm.refreshLocale(); vm.refreshIcons() }, modifier = Modifier.fillMaxWidth()) { Text(ui("account.reread_bundles")) }
            // The first-visit guides (3.14.0) come back on every screen once reset.
            LocalGuideDesk.current?.let { desk ->
                ForgeOutlinedButton(onClick = desk::reset, modifier = Modifier.fillMaxWidth()) { Text(ui("guide.reset")) }
            }
        }
        ForgePanel {
            Engraved(ui("account.server"))
            OutlinedTextField(s.account.serverDraft, vm::serverDraft, enabled = !s.busy, label = { Text(ui("account.server_address")) },
                supportingText = { Text(ui("account.address_hint")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            ForgeButton(enabled = !s.busy, onClick = vm::connect, modifier = Modifier.fillMaxWidth()) { Text(ui("account.save_connect")) }
            ForgeOutlinedButton(enabled = !s.busy, onClick = vm::health, modifier = Modifier.fillMaxWidth()) { Text(ui("account.check_health")) }
        }
        InfoCard(ui("account.server_state"), s.account.health)
        InfoCard(ui("account.local_dev"),
            ui("account.local_dev_note"))
        // The wire this client speaks: the API revision it refuses to differ from, and the server it was built against.
        InfoCard(ui("account.contract"),
            "API $API_REVISION · $SERVER_VERSION · $SERVER_BRANCH · ${SERVER_COMMIT.take(12)}\n" +
            ui("account.contract_note"))
        InfoCard(ui("account.signin_section"),
            ui("account.signin_note"))
        RequestJournalPanel(vm, logs)
    }
    if (promoOpen) PromoCodeDialog(s, onDismiss = { promoOpen = false }) { code -> promoOpen = false; vm.redeem(code) }
}

/**
 * One promo code, for the hero currently being played.
 *
 * The account may hold three exiles and the server pays the reward to exactly one of them, so the
 * dialog says which before anything is typed rather than after the goods have landed.
 */
@Composable private fun PromoCodeDialog(s: ForgeState, onDismiss: () -> Unit, onRedeem: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Panel, titleContentColor = Gold,
        title = { Text(ui("account.promo")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MutedText(ui("account.promo_target", s.heroName))
                OutlinedTextField(code, { code = it.take(100) }, label = { Text(ui("account.code")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { ForgeTextButton(enabled = !s.busy && code.isNotBlank(), onClick = { onRedeem(code) }) {
            Text(ui("account.claim")) } },
        dismissButton = { ForgeTextButton(onClick = onDismiss) { Text(ui("common.cancel")) } })
}

/**
 * The request journal, right where a connection is set up.
 *
 * A broken connection is exactly when nothing else on screen can be reached, so every failed
 * attempt is readable here, before any sign-in: method, path, status and both bodies.
 */
@Composable private fun RequestJournalPanel(vm: ForgeViewModel, logs: List<RequestLog>) {
    var expanded by remember { mutableStateOf(false) }
    ForgePanel {
        Engraved(ui("account.journal"))
        MutedText(ui("account.journal_note", logs.size))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeTextButton(onClick = { expanded = !expanded }) { Text(if (expanded) ui("common.hide") else ui("common.show")) }
            ForgeTextButton(enabled = logs.isNotEmpty(), onClick = vm::clearLogs) { Text(ui("common.clear")) }
        }
        if (expanded) {
            if (logs.isEmpty()) Text(ui("account.journal_empty"), color = Muted)
            logs.take(12).forEach { LogCard(it) }
        }
    }
}

/** One request of the journal: the verb, the status and the time on one line, the path under it, both bodies on a tap. */
@Composable private fun LogCard(log: RequestLog) {
    var expanded by remember(log) { mutableStateOf(false) }
    val accent = if (log.ok) Gold else MaterialTheme.colorScheme.error
    ForgePanel(Modifier.clickable { expanded = !expanded }, accent = accent) {
        Text("${log.method}  ${log.status ?: "NETWORK"}  ·  ${log.elapsedMs} ms", color = accent, style = MaterialTheme.typography.labelLarge)
        Text(log.path, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
        if (expanded) SelectionContainer {
            Column {
                if (log.request.isNotBlank()) Text("REQUEST\n${log.request}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("RESPONSE\n${log.response}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
            }
        }
    }
}
