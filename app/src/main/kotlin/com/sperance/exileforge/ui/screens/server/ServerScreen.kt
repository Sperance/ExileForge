package com.sperance.exileforge.ui.screens.server

import com.sperance.exileforge.core.display.Glyph

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.SERVER_BRANCH
import com.sperance.exileforge.core.contract.SERVER_COMMIT
import com.sperance.exileforge.core.contract.SERVER_VERSION
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.checks.LogCard
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel

@Composable internal fun ServerScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog> = emptyList()) {
    var promoOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(ui("account.title"), ui("account.subtitle"), ForgeGlyphs.Portal)
        ForgePanel {
            Engraved(ui("account.exile"))
            // A character is swapped by leaving the game, never from inside a tab: every button
            // below is bound to the one chosen in the menu, and this is the way back to it.
            s.character?.let { hero ->
                PropertyRow(ui("common.character"), hero.name + ui("app.hero_level", hero.level), Glyph.CHARACTER)
            }
            ForgeOutlinedButton(enabled = !s.busy && !s.admin.editorOpen, onClick = vm::leaveGame, modifier = Modifier.fillMaxWidth()) {
                Text(ui("account.change_character"))
            }
            if (s.admin.editorOpen) MutedText(ui("account.close_editor_first"))
            // A reward is paid to a character, not to an account, so the code is asked for where
            // the character being played is already named — and the dialog names them again.
            ForgeOutlinedButton(enabled = !s.busy && s.play.characterId.isNotBlank(), onClick = { promoOpen = true }, modifier = Modifier.fillMaxWidth()) {
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
            ForgeOutlinedButton(enabled = !s.busy, onClick = { vm.refreshLocale(); vm.refreshIcons() }, modifier = Modifier.fillMaxWidth()) { Text(ui("account.reread_bundles")) }
        }
        ForgePanel {
            Engraved(ui("account.server"))
            OutlinedTextField(s.account.serverDraft, vm::serverDraft, enabled = !s.busy, label = { Text(ui("account.server_address")) },
                supportingText = { Text(ui("account.address_hint")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            ForgeButton(enabled = !s.busy && !s.admin.editorOpen, onClick = vm::connect, modifier = Modifier.fillMaxWidth()) { Text(ui("account.save_connect")) }
            if (s.admin.editorOpen) Text(ui("account.close_editor_note"), color = Muted)
            ForgeOutlinedButton(enabled = !s.busy, onClick = vm::health, modifier = Modifier.fillMaxWidth()) { Text(ui("account.check_health")) }
        }
        InfoCard(ui("account.server_state"), s.account.health)
        InfoCard(ui("account.local_dev"),
            ui("account.local_dev_note"))
        InfoCard(ui("account.contract"),
            "$SERVER_VERSION · $SERVER_BRANCH · ${SERVER_COMMIT.take(12)}\n" +
            ui("account.contract_note"))
        InfoCard(ui("account.signin_section"),
            ui("account.signin_note"))
        RequestJournalPanel(vm, logs)
    }
    if (promoOpen) PromoCodeDialog(s, onDismiss = { promoOpen = false }) { code -> promoOpen = false; vm.redeem(code) }
}

/**
 * One promo code, for the character currently being played.
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
                MutedText(ui("account.promo_target", s.character?.name.orEmpty()))
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
 * The Checks tab needs an administrator, which is exactly what you do not have when the connection
 * itself is broken, so every failed attempt is readable here: method, path, status and both bodies.
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
