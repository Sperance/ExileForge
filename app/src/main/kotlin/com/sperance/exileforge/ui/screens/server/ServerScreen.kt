package com.sperance.exileforge.ui.screens.server

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
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.contract.SERVER_BRANCH
import com.sperance.exileforge.core.contract.SERVER_COMMIT
import com.sperance.exileforge.core.contract.SERVER_VERSION
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.presentation.server.ServerViewModel
import com.sperance.exileforge.presentation.session.SessionViewModel
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.rules.content.ContentFiles
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** The screens behind the account's rows: each a page of its own, «back» leading to the list. */
private enum class AccountPage(val title: String) { SIGN_IN("account.signin_section"), }

/**
 * «Врата мира» (variant A, «Списки и плитки»): who is playing, then the hero's and the account's rows. Since 3.77.0 it
 * opens from the game's sigil in the banner, and the language, the guides and the developers' tools live in «Настройки».
 */
@Composable internal fun ServerScreen() {
    val account by koinViewModel<ServerViewModel>().ui.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    var page by rememberSaveable { mutableStateOf<AccountPage?>(null) }
    var promoOpen by remember { mutableStateOf(false) }
    val open = page
    if (open == null) {
        AccountHome(account, onPage = { page = it }, onPromo = { promoOpen = true })
    } else {
        Column(Modifier.fillMaxSize()) {
            BackRow(ui("account.title")) { page = null }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(ui(open.title), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                when (open) {
                    AccountPage.SIGN_IN -> SignInPage(account)
                }
            }
        }
    }
    if (promoOpen) {
        PromoCodeDialog(account, onDismiss = { promoOpen = false }) { code ->
            promoOpen = false
            heroModel.redeem(code)
        }
    }
}

@Composable private fun AccountHome(account: AccountUi, onPage: (AccountPage) -> Unit, onPromo: () -> Unit) {
    val sessionModel: SessionViewModel = koinViewModel()
    val shell: ShellViewModel = koinViewModel()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(ui("account.title"), ui("account.subtitle"), ForgeGlyphs.Portal)
        ProfileCard(account)
        RowGroup(ui("account.group_game")) {
            // A hero is swapped by leaving the game, never from inside a tab: every button elsewhere is
            // bound to the one chosen in the menu, and this is the way back to it.
            AccountRow(Icons.Outlined.SwapHoriz, ui("account.change_character"), enabled = !account.busy, onClick = sessionModel::leaveGame)
            // A reward is paid to a hero, not to an account, so the dialog names the hero being played.
            AccountRow(Icons.Outlined.CardGiftcard, ui("account.enter_promo"), enabled = !account.busy && account.heroId.isNotBlank(), onClick = onPromo)
            AccountRow(
                Icons.Outlined.Person,
                ui("account.signin_section"),
                value = if (account.session.signedIn) account.session.title else ui("account.signed_out"),
            ) { onPage(AccountPage.SIGN_IN) }
            AccountRow(ForgeGlyphs.Sigil, ui("settings.title")) { shell.openSettings() }
        }
    }
}

/** Who is playing: the class's portrait, the name and level, the class; the account under it once signed in. */
@Composable private fun ProfileCard(account: AccountUi) {
    val model = koinViewModel<ServerViewModel>()
    val session by model.session.collectAsStateWithLifecycle()
    val world by model.world.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(14.dp)
    val classCode = account.heroClass
    Row(
        Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Gold.copy(alpha = .16f).compositeOver(Panel), Panel)), shape)
            .border(1.dp, PanelRaised, shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ClassPortrait(classCode, world.portraits, Modifier.size(46.dp), round = true)
        Column(Modifier.weight(1f)) {
            if (account.heroName.isNotBlank()) {
                Text(
                    account.heroName + ui("app.hero_level", account.heroLevel),
                    color = GoldBright,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            classCode?.takeIf { it.isNotBlank() }?.let { MutedText(classTitle(it)) }
            if (session.signedIn) {
                MutedText(
                    "${session.title} · ${ui(
                        when {
                            session.isAdmin -> "account.administrator"
                            session.isTester -> "account.tester"
                            else -> "account.player"
                        },
                    )}",
                )
            }
        }
    }
}

/** Rows under a small caption, in one rounded panel, hairlines between them. */
@Composable internal fun RowGroup(title: String, rows: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel, shape).border(1.dp, PanelRaised, shape)) {
        Engraved(title, Muted, Modifier.padding(start = 12.dp, top = 9.dp, bottom = 2.dp))
        rows()
    }
}

/** One row of a group: the glyph, the name, a value on the right — with a status dot when given — and a chevron where it opens a page. */
@Composable internal fun AccountRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    dot: Color? = null,
    enabled: Boolean = true,
    chevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = if (enabled) Gold else Muted, modifier = Modifier.size(20.dp))
        Text(
            label,
            color = if (enabled) Parchment else Muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        dot?.let { Box(Modifier.size(8.dp).background(it, CircleShape)) }
        value?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        if (chevron) Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** The account: signing in, the password, signing out — and how the server keeps a session. */
@Composable private fun SignInPage(account: AccountUi) {
    ForgePanel { LoginForm(account) }
    InfoCard(ui("account.signin_section"), ui("account.signin_note"))
}

/** The server: where it is, the way to connect and to ask after its health, and the answer. */
@Composable internal fun ServerPage(account: AccountUi) {
    val sessionModel: SessionViewModel = koinViewModel()
    val model = koinViewModel<ServerViewModel>()
    val session by model.session.collectAsStateWithLifecycle()
    ForgePanel {
        // The address is fixed for players (3.75.0); only an administrator points the device at another server.
        if (session.isAdmin) {
            var draft by rememberSaveable(session.server) { mutableStateOf(session.server) }
            OutlinedTextField(
                draft,
                { draft = it.take(account.inputs.server) },
                enabled = !account.busy,
                label = { Text(ui("account.server_address")) },
                supportingText = { Text(ui("account.address_hint")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            ForgeButton(enabled = !account.busy, onClick = { sessionModel.connect(draft) }, modifier = Modifier.fillMaxWidth()) { Text(ui("account.save_connect")) }
        } else {
            PropertyRow(ui("account.server_address"), session.server, Glyph.TEXT)
        }
        ForgeOutlinedButton(enabled = !account.busy, onClick = sessionModel::health, modifier = Modifier.fillMaxWidth()) { Text(ui("account.check_health")) }
    }
    InfoCard(ui("account.server_state"), session.health)
    if (session.isAdmin) InfoCard(ui("account.local_dev"), ui("account.local_dev_note"))
}

/** What the client holds of the server: the dictionary, the drawings, the world's tables and the contract it speaks. */
@Composable internal fun ClientPage(account: AccountUi) {
    val sessionModel: SessionViewModel = koinViewModel()
    val model = koinViewModel<ServerViewModel>()
    val world by model.world.collectAsStateWithLifecycle()
    ForgePanel {
        // Names of things belong to the server since 0.14.0: without its dictionary the screens
        // print codes, so how much of it arrived is worth saying out loud.
        if (world.localeStrings > 0) {
            PropertyRow(
                ui("account.dictionary"),
                "${world.localeLanguage.uppercase()} · " + ui("account.strings", world.localeStrings),
                Glyph.TEXT,
            )
        } else {
            MutedText(ui("account.dictionary_missing"))
        }
        // Drawings come from the server too, and a missing set is invisible by design: every
        // hole falls back to a bundled emblem, so the count is the only way to notice one.
        if (world.iconKeys > 0) {
            PropertyRow(
                ui("account.icons"),
                ui("account.icons_count", world.iconKeys, world.iconSprites),
                Glyph.IMAGE,
            )
        } else {
            MutedText(ui("account.icons_missing"))
        }
        if (world.portraits > 0) PropertyRow(ui("account.portraits"), world.portraits.toString(), Glyph.IMAGE)
        // The world's tables come the same way (3.0.0): chunks fetched once per fingerprint and
        // parsed by the rules. The fingerprint is what tells a changed world from the one on screen.
        if (world.content != null) {
            PropertyRow(
                ui("account.content"),
                "${world.contentHash.take(12)} · " + ui("account.chunks", ContentFiles.ALL.size),
                Glyph.SERVER,
            )
        } else {
            MutedText(ui("account.content_missing"))
        }
        ForgeOutlinedButton(enabled = !account.busy, onClick = {
            sessionModel.refreshLocale()
            sessionModel.refreshIcons()
        }, modifier = Modifier.fillMaxWidth()) { Text(ui("account.reread_bundles")) }
    }
    // The build and its updates (3.72.0): the version, and a check by hand.
    UpdateCard()
    // Работающий сервер - из его манифеста, закреплённый - из сборки (3.88.2): расхождение видно сразу.
    val live = if (world.serverVersion.isNotBlank()) {
        ui("account.contract_live", world.serverVersion, world.serverRevision, world.serverCommit.take(12).ifBlank { "-" })
    } else {
        ui("account.contract_live_unknown")
    }
    InfoCard(
        ui("account.contract"),
        live + "\n" + ui("account.contract_built", SERVER_VERSION, API_REVISION, SERVER_BRANCH, SERVER_COMMIT.take(12)) + "\n" +
            ui("account.contract_note"),
    )
}

/**
 * One promo code, for the hero currently being played.
 *
 * The account may hold three exiles and the server pays the reward to exactly one of them, so the
 * dialog says which before anything is typed rather than after the goods have landed.
 */
@Composable private fun PromoCodeDialog(account: AccountUi, onDismiss: () -> Unit, onRedeem: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        titleContentColor = Gold,
        title = { Text(ui("account.promo")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MutedText(ui("account.promo_target", account.heroName))
                OutlinedTextField(
                    code,
                    { code = it.take(account.inputs.code) },
                    label = { Text(ui("account.code")) },
                    supportingText = { LengthCounter(code, account.inputs.code) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            ForgeTextButton(enabled = !account.busy && code.isNotBlank(), onClick = { onRedeem(code) }) {
                Text(ui("account.claim"))
            }
        },
        dismissButton = { ForgeTextButton(onClick = onDismiss) { Text(ui("common.cancel")) } },
    )
}

/**
 * The request journal, a page beside the server's.
 *
 * A broken connection is exactly when nothing else on screen can be reached, so every failed
 * attempt is readable here, before any sign-in: method, path, status and both bodies.
 */
@Composable internal fun RequestJournalPanel(logs: List<RequestLog>) {
    val shell: ShellViewModel = koinViewModel()
    ForgePanel {
        MutedText(ui("account.journal_note", logs.size))
        ForgeTextButton(enabled = logs.isNotEmpty(), onClick = shell::clearLogs) { Text(ui("common.clear")) }
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
        if (expanded) {
            SelectionContainer {
                Column {
                    if (log.request.isNotBlank()) Text("REQUEST\n${log.request}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("RESPONSE\n${log.response}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
                }
            }
        }
    }
}
