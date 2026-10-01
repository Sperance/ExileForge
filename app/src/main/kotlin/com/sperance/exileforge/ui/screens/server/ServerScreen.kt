package com.sperance.exileforge.ui.screens.server

import com.sperance.exileforge.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.contract.SERVER_BRANCH
import com.sperance.exileforge.core.contract.SERVER_COMMIT
import com.sperance.exileforge.core.contract.SERVER_VERSION
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.ContentFiles
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** The screens behind the account's rows: each a page of its own, «back» leading to the list. */
private enum class AccountPage(val title: String) {
    SIGN_IN("account.signin_section"), LANGUAGE("account.language"), SERVER("account.server"), CLIENT("account.client"), JOURNAL("account.journal")
}

/**
 * «Врата мира» (variant A, «Списки и плитки»): who is playing, then grouped rows with chevrons — the game on top, the
 * server and the technical parts below, each a page of its own. Everything is in sight without scrolling; a setting
 * is one tap further than it was.
 */
@Composable internal fun ServerScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog> = emptyList()) {
    var page by rememberSaveable { mutableStateOf<AccountPage?>(null) }
    var promoOpen by remember { mutableStateOf(false) }
    val open = page
    if (open == null) AccountHome(s, vm, logs, onPage = { page = it }, onPromo = { promoOpen = true })
    else Column(Modifier.fillMaxSize()) {
        BackRow(ui("account.title")) { page = null }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(ui(open.title), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            when (open) {
                AccountPage.SIGN_IN -> SignInPage(s, vm)
                AccountPage.LANGUAGE -> ForgePanel {
                    LanguagePicker(s.lang, s.world.languages, enabled = !s.busy, onLanguage = vm::language)
                    MutedText(ui("account.language_note"))
                }
                AccountPage.SERVER -> ServerPage(s, vm)
                AccountPage.CLIENT -> ClientPage(s, vm)
                AccountPage.JOURNAL -> RequestJournalPanel(vm, logs)
            }
        }
    }
    if (promoOpen) PromoCodeDialog(s, onDismiss = { promoOpen = false }) { code -> promoOpen = false; vm.redeem(code) }
}

@Composable private fun AccountHome(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>, onPage: (AccountPage) -> Unit, onPromo: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(ui("account.title"), ui("account.subtitle"), ForgeGlyphs.Portal)
        ProfileCard(s)
        RowGroup(ui("account.group_game")) {
            // A hero is swapped by leaving the game, never from inside a tab: every button elsewhere is
            // bound to the one chosen in the menu, and this is the way back to it.
            AccountRow(Icons.Outlined.SwapHoriz, ui("account.change_character"), enabled = !s.busy, onClick = vm::leaveGame)
            // A reward is paid to a hero, not to an account, so the dialog names the hero being played.
            AccountRow(Icons.Outlined.CardGiftcard, ui("account.enter_promo"), enabled = !s.busy && s.play.heroId.isNotBlank(), onClick = onPromo)
            AccountRow(Icons.Outlined.Language, ui("account.language"), value = s.lang.title) { onPage(AccountPage.LANGUAGE) }
            AccountRow(Icons.Outlined.Person, ui("account.signin_section"),
                value = if (s.account.signedIn) s.accountTitle else ui("account.signed_out")) { onPage(AccountPage.SIGN_IN) }
            // The first-visit guides (3.14.0) come back on every screen once reset.
            LocalGuideDesk.current?.let { desk -> AccountRow(Icons.Outlined.Lightbulb, ui("guide.reset"), chevron = false, onClick = desk::reset) }
            // Every administrator tool moved to its own tab in 2.3.0. What stays here is the way back
            // into it: turning the tools off hides that tab, so the switch cannot live only inside it.
            if (BuildConfig.DEBUG && s.isAdmin && !s.adminTools)
                AccountRow(Icons.Outlined.AdminPanelSettings, ui("account.tools_back"), enabled = !s.busy, chevron = false) { vm.mode(AppMode.ADMIN) }
        }
        RowGroup(ui("account.server")) {
            AccountRow(Icons.Outlined.Dns, ui("account.server"), value = ui(if (s.link.offline) "account.offline" else "account.online"),
                dot = if (s.link.offline) LifeRed else Vital) { onPage(AccountPage.SERVER) }
            AccountRow(Icons.Outlined.Info, ui("account.client"), value = "API $API_REVISION") { onPage(AccountPage.CLIENT) }
            AccountRow(Icons.AutoMirrored.Outlined.ReceiptLong, ui("account.journal"), value = logs.size.toString()) { onPage(AccountPage.JOURNAL) }
        }
    }
}

/** Who is playing: the class's portrait, the name and level, the class; the account under it once signed in. */
@Composable private fun ProfileCard(s: ForgeState) {
    val shape = RoundedCornerShape(14.dp)
    val classCode = s.heroInfo?.heroClass ?: s.heroRow?.heroClass
    Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Gold.copy(alpha = .16f).compositeOver(Panel), Panel)), shape)
        .border(1.dp, PanelRaised, shape).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ClassPortrait(classCode, s.world.portraits, Modifier.size(46.dp), round = true)
        Column(Modifier.weight(1f)) {
            if (s.heroName.isNotBlank()) Text(s.heroName + ui("app.hero_level", s.heroLevel), color = GoldBright, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            classCode?.takeIf { it.isNotBlank() }?.let { MutedText(classTitle(it)) }
            if (s.account.signedIn) MutedText("${s.accountTitle} · ${ui(if (s.isAdmin) "account.administrator" else "account.player")}")
        }
    }
}

/** Rows under a small caption, in one rounded panel, hairlines between them. */
@Composable private fun RowGroup(title: String, rows: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel, shape).border(1.dp, PanelRaised, shape)) {
        Engraved(title, Muted, Modifier.padding(start = 12.dp, top = 9.dp, bottom = 2.dp))
        rows()
    }
}

/** One row of a group: the glyph, the name, a value on the right — with a status dot when given — and a chevron where it opens a page. */
@Composable private fun AccountRow(icon: ImageVector, label: String, value: String? = null, dot: Color? = null, enabled: Boolean = true,
    chevron: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = if (enabled) Gold else Muted, modifier = Modifier.size(20.dp))
        Text(label, color = if (enabled) Parchment else Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        dot?.let { Box(Modifier.size(8.dp).background(it, CircleShape)) }
        value?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        if (chevron) Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** The account: signing in, the password, signing out — and how the server keeps a session. */
@Composable private fun SignInPage(s: ForgeState, vm: ForgeViewModel) {
    ForgePanel { LoginForm(s, vm) }
    InfoCard(ui("account.signin_section"), ui("account.signin_note"))
}

/** The server: where it is, the way to connect and to ask after its health, and the answer. */
@Composable private fun ServerPage(s: ForgeState, vm: ForgeViewModel) {
    ForgePanel {
        OutlinedTextField(s.account.serverDraft, { vm.serverDraft(it.take(s.inputs.server)) }, enabled = !s.busy, label = { Text(ui("account.server_address")) },
            supportingText = { Text(ui("account.address_hint")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        ForgeButton(enabled = !s.busy, onClick = vm::connect, modifier = Modifier.fillMaxWidth()) { Text(ui("account.save_connect")) }
        ForgeOutlinedButton(enabled = !s.busy, onClick = vm::health, modifier = Modifier.fillMaxWidth()) { Text(ui("account.check_health")) }
    }
    InfoCard(ui("account.server_state"), s.account.health)
    InfoCard(ui("account.local_dev"), ui("account.local_dev_note"))
}

/** What the client holds of the server: the dictionary, the drawings, the world's tables and the contract it speaks. */
@Composable private fun ClientPage(s: ForgeState, vm: ForgeViewModel) {
    ForgePanel {
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
    }
    // The build and its updates (3.72.0): the version, and a check by hand.
    UpdateCard()
    // The wire this client speaks: the API revision it refuses to differ from, and the server it was built against.
    InfoCard(ui("account.contract"),
        "API $API_REVISION · $SERVER_VERSION · $SERVER_BRANCH · ${SERVER_COMMIT.take(12)}\n" +
        ui("account.contract_note"))
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
                OutlinedTextField(code, { code = it.take(s.inputs.code) }, label = { Text(ui("account.code")) },
                    supportingText = { LengthCounter(code, s.inputs.code) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { ForgeTextButton(enabled = !s.busy && code.isNotBlank(), onClick = { onRedeem(code) }) {
            Text(ui("account.claim")) } },
        dismissButton = { ForgeTextButton(onClick = onDismiss) { Text(ui("common.cancel")) } })
}

/**
 * The request journal, a page beside the server's.
 *
 * A broken connection is exactly when nothing else on screen can be reached, so every failed
 * attempt is readable here, before any sign-in: method, path, status and both bodies.
 */
@Composable private fun RequestJournalPanel(vm: ForgeViewModel, logs: List<RequestLog>) {
    ForgePanel {
        MutedText(ui("account.journal_note", logs.size))
        ForgeTextButton(enabled = logs.isNotEmpty(), onClick = vm::clearLogs) { Text(ui("common.clear")) }
        if (logs.isEmpty()) Text(ui("account.journal_empty"), color = Muted)
        logs.take(12).forEach { LogCard(it) }
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
