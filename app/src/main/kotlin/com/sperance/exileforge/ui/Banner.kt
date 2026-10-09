package com.sperance.exileforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.Link
import com.sperance.exileforge.core.network.LinkState
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.LocalMailOpen
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.HeroLine
import com.sperance.exileforge.ui.screens.hero.rememberHeroHeader
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Шапка игры (3.80.24): сигил и аккаунт, имя героя, плашка ремесла, значок связи и меню «⋮». */
/**
 * Title banner: the sigil, the character being played, and the one switch an exile keeps at hand.
 *
 * The subtitle names the character rather than the league, because every button on every tab acts
 * on that one and nothing on screen would otherwise say which.
 *
 * The administrator's mode switch used to sit here too. It moved to the administrator's own tab in
 * 2.3.0: it is not something a player has, and the banner is the one thing on screen every player
 * sees on every tab. The language runes went the same way in 2.4.0, to the Account tab and to the
 * sign-in screen: with a third language they were a crowd, and a language is a setting, not an act.
 * Since 2.48.0 the hero's class is gone from it and the account sits in its corner — it left the bottom bar; the plaque
 * of the craft under way is gone too (3.91.0). Since 3.75.0 the inbox, the beetle and the
 * account share one «⋮»: with every badge up the name of the game no longer fit. С 4.3.0 почта - снова свой конверт ([MailBadge]).
 */
@Composable internal fun ForgeBanner(game: GameUi, route: Route, onBug: () -> Unit) {
    val feedback by koinViewModel<FeedbackViewModel>().feedback.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val glow = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Gold.copy(alpha = .10f), Color.Transparent, Gold.copy(alpha = .06f))))
    // Вкладка «Герой» (3.90.5): шапка игры и строка героя - одна строка, имя и уровень не повторяются дважды.
    val hero = rememberHeroHeader(game)?.takeIf { route.tab == TAB_HERO }
    if (hero != null) {
        HeroLine(hero, glow, onPortrait = { shell.tab(TAB_ACCOUNT) }) {
            GoldPrice(hero.money)
            LinkBadge(game.link, admin = game.isAdmin, onRetry = shell::retryLink, onReconnect = shell::reconnectLink)
            MailBadge(feedback.unread)
            BannerMenu(settingsOpen = false, onBug = onBug, onSettings = shell::openSettings)
        }
        return
    }
    Row(
        glow.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The game's sigil opens the account (3.77.0), where the menu's row was.
        val accountOpen = route.tab == TAB_ACCOUNT
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).border(1.dp, (if (accountOpen) GoldBright else Gold).copy(alpha = .5f), RoundedCornerShape(9.dp))
                .clickable(onClickLabel = ui("nav.account")) { shell.tab(TAB_ACCOUNT) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(ForgeGlyphs.Sigil, ui("nav.account"), tint = if (accountOpen) GoldBright else Gold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("EXILE FORGE", style = MaterialTheme.typography.titleLarge, color = GoldBright, maxLines = 1, softWrap = false)
            // The loaded hero names themself; before the snapshot lands, the menu's row does.
            val named = game.heroInfo != null || game.heroRow != null
            Text(
                if (named) game.heroName + ui("app.hero_level", game.heroLevel) else ui("app.title"),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LinkBadge(game.link, admin = game.isAdmin, onRetry = shell::retryLink, onReconnect = shell::reconnectLink)
        MailBadge(feedback.unread)
        BannerMenu(settingsOpen = route.tab == TAB_SETTINGS, onBug = onBug, onSettings = shell::openSettings)
    }
}

/**
 * The banner's «⋮» (3.75.0, the owner's pick «B» of three mockups): the beetle and the settings behind one button, so the name
 * of the game fits beside the badges. Почта с 4.3.0 - свой конверт рядом ([MailBadge]), у «⋮» поводов для отметки нет.
 */
@Composable internal fun BannerMenu(settingsOpen: Boolean, onBug: () -> Unit, onSettings: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, ui("common.more"), tint = if (settingsOpen) GoldBright else Gold, modifier = Modifier.size(24.dp))
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            fun pick(action: () -> Unit) {
                open = false
                action()
            }
            DropdownMenuItem(text = { Text(ui("bug.open")) }, onClick = { pick(onBug) }, leadingIcon = { Icon(Icons.Outlined.BugReport, null, tint = Gold) })
            DropdownMenuItem(
                text = { Text(ui("settings.title"), color = if (settingsOpen) GoldBright else Parchment) },
                onClick = { pick(onSettings) },
                leadingIcon = { Icon(Icons.Outlined.Settings, null, tint = Gold) },
            )
        }
    }
}

/**
 * Конверт почты в шапке (4.3.0): всегда под рукой, рядом со значком связи; непрочитанные - красным числом в углу (больше
 * девяти - «9+»). Открывает почту тот, кого дало приложение ([LocalMailOpen]); без него конверта нет.
 */
@Composable internal fun MailBadge(unread: Int) {
    val open = LocalMailOpen.current ?: return
    IconButton(onClick = open) {
        Box {
            Icon(Icons.Outlined.Mail, ui("mail.title"), tint = if (unread > 0) GoldBright else Gold, modifier = Modifier.size(22.dp))
            if (unread > 0) {
                Text(
                    if (unread > MAIL_SHOWN) "$MAIL_SHOWN+" else unread.toString(),
                    color = Ink,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-5).dp).background(LifeRed, CircleShape).padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** Больше скольких непрочитанных конверт пишет «N+». */
private const val MAIL_SHOWN = 9

/**
 * The link to the server (3.30.0): nothing while the server answers and nothing waits. С 4.4.x - по [LinkState]: команды ждут -
 * облако с числом, нажатие спрашивает сервер сразу ([onRetry]); нет связи - красное перечёркнутое облако, нажатие - полное
 * переподключение ([onReconnect]); переподключается - жёлтое облако, не нажимается; неудача вернёт красное и скажет тостом
 * почему. Долгое нажатие открывает подробности: причина, что делать, ждущие команды, слова транспорта для администратора.
 */
@Composable internal fun LinkBadge(link: Link, admin: Boolean, onRetry: () -> Unit, onReconnect: () -> Unit) {
    val state = link.state
    val look = state.look ?: return
    var open by remember { mutableStateOf(false) }
    val tap: (() -> Unit)? = when (state) {
        is LinkState.Offline -> onReconnect
        is LinkState.Queued -> onRetry
        LinkState.Online, LinkState.Reconnecting -> null
    }
    Box {
        Row(
            Modifier.combinedClickable(enabled = tap != null, onClickLabel = look.label, onLongClick = { open = true }) { tap?.invoke() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(look.icon, look.label, tint = look.tint, modifier = Modifier.size(18.dp))
            if (link.waiting.isNotEmpty()) Text(link.waiting.size.toString(), color = look.tint, fontSize = 11.sp)
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            Column(Modifier.widthIn(max = 280.dp).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(look.label, color = look.tint, style = MaterialTheme.typography.titleSmall)
                (state as? LinkState.Offline)?.let { Text(it.outage.hint, color = Parchment, style = MaterialTheme.typography.bodySmall) }
                Text(ui("link.auto_retry"), color = Muted, style = MaterialTheme.typography.labelSmall)
                if (link.waiting.isNotEmpty()) Text(ui("link.waiting", link.waiting.size), color = Gold, style = MaterialTheme.typography.labelSmall)
                if (admin) link.detail?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
                tap?.let {
                    TextButton(onClick = {
                        open = false
                        it()
                    }) { Text(ui("link.retry_now"), color = GoldBright) }
                }
            }
        }
    }
}

/** Как значок связи рисует своё состояние: рисунок, цвет и подпись; в сети значка нет. */
private class LinkLook(val icon: ImageVector, val tint: Color, val label: String)

private val LinkState.look: LinkLook?
    get() = when (this) {
        LinkState.Online -> null
        is LinkState.Queued -> LinkLook(Icons.Outlined.CloudUpload, Gold, ui("link.waiting", count))
        is LinkState.Offline -> LinkLook(Icons.Outlined.CloudOff, LifeRed, outage.title)
        LinkState.Reconnecting -> LinkLook(Icons.Outlined.CloudSync, Caution, ui("link.reconnecting"))
    }
