package com.sperance.exileforge.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sperance.exileforge.core.display.workTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.ui.components.BugSheet
import com.sperance.exileforge.ui.components.ExilePathPlate
import com.sperance.exileforge.ui.components.GuideDesk
import com.sperance.exileforge.ui.components.GuideHost
import com.sperance.exileforge.ui.components.LocalBugReport
import com.sperance.exileforge.ui.components.LocalGuideDesk
import com.sperance.exileforge.ui.components.LocalMailOpen
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import com.sperance.exileforge.ui.components.LocalUpdates
import com.sperance.exileforge.ui.components.MailSheet
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.SuggestionsSheet
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.UpdateGate
import com.sperance.exileforge.ui.components.WarmupScreen
import com.sperance.exileforge.ui.components.destination
import com.sperance.exileforge.ui.components.pathStep
import com.sperance.exileforge.ui.components.pulse
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.admin.AdminScreen
import com.sperance.exileforge.ui.screens.city.CityScreen
import com.sperance.exileforge.ui.screens.crafts.CraftsAwayHost
import com.sperance.exileforge.ui.screens.crafts.CraftsScreen
import com.sperance.exileforge.ui.screens.expedition.AtlasScreen
import com.sperance.exileforge.ui.screens.expedition.ExpeditionPlay
import com.sperance.exileforge.ui.screens.expedition.ExpeditionScreen
import com.sperance.exileforge.ui.screens.expedition.TrialScreen
import com.sperance.exileforge.ui.screens.expedition.world.WorldArt
import com.sperance.exileforge.ui.screens.hero.HeroScreen
import com.sperance.exileforge.ui.screens.hero.HeroTab
import com.sperance.exileforge.ui.screens.hero.HeroTabStrip
import com.sperance.exileforge.ui.screens.progress.ProgressPlace
import com.sperance.exileforge.ui.screens.progress.ProgressPlaceScreen
import com.sperance.exileforge.ui.screens.progress.ProgressScreen
import com.sperance.exileforge.ui.screens.redemption.RedemptionScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.server.SettingsScreen
import com.sperance.exileforge.ui.screens.session.AuthScreen
import com.sperance.exileforge.ui.screens.session.CharacterSelectScreen
import com.sperance.exileforge.ui.screens.skills.GrimoireScreen
import com.sperance.exileforge.ui.screens.tree.SkillTreeScreen
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
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
 * Since 2.48.0 the hero's class is gone from it, a short plaque of the craft under way opens the
 * crafts, and the account sits in its corner — it left the bottom bar. Since 3.75.0 the inbox, the beetle and the
 * account share one «⋮»: with every badge up the name of the game no longer fit.
 */
@Composable internal fun ForgeBanner(s: ForgeState, onBug: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Gold.copy(alpha = .10f), Color.Transparent, Gold.copy(alpha = .06f))))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The game's sigil opens the account (3.77.0), where the menu's row was.
        val accountOpen = s.tab == TAB_ACCOUNT
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
            val named = s.heroInfo != null || s.heroRow != null
            Text(
                if (named) s.heroName + ui("app.hero_level", s.heroLevel) else ui("app.title"),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        WorkBadge(s) { shell.tab(TAB_CRAFTS) }
        LinkBadge(s.link, admin = s.isAdmin, onRetry = shell::retryLink)
        BannerMenu(s.feedback.unread, settingsOpen = s.tab == TAB_SETTINGS, onMail = LocalMailOpen.current, onBug = onBug, onSettings = shell::openSettings)
    }
}

/**
 * The banner's «⋮» (3.75.0, the owner's pick «B» of three mockups): the inbox, the beetle and the account behind one button,
 * so the name of the game fits beside the badges. A letter unread marks the button itself with a dot.
 */
@Composable internal fun BannerMenu(unread: Int, settingsOpen: Boolean, onMail: (() -> Unit)?, onBug: () -> Unit, onSettings: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Box {
                Icon(Icons.Outlined.MoreVert, ui("common.more"), tint = if (settingsOpen) GoldBright else Gold, modifier = Modifier.size(24.dp))
                if (unread > 0) Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(LifeRed, CircleShape))
            }
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            fun pick(action: () -> Unit) {
                open = false
                action()
            }
            onMail?.let { mail ->
                DropdownMenuItem(
                    text = { Text(ui("mail.title")) },
                    onClick = { pick(mail) },
                    leadingIcon = { Icon(Icons.Outlined.Mail, null, tint = Gold) },
                    trailingIcon = {
                        if (unread > 0) {
                            Text(
                                if (unread > 9) "9+" else unread.toString(),
                                color = Ink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(LifeRed, CircleShape).padding(horizontal = 6.dp),
                            )
                        }
                    },
                )
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
 * The link to the server (3.30.0): a small crossed cloud while it cannot be reached, and how many commands wait
 * to be sent. Nothing at all while the server answers and nothing waits. A tap on a cloud that only waits asks
 * the server again at once; on a crossed one (3.79.0) it says why — by cause, with the transport's words for an
 * administrator — and offers «Повторить». The server is asked again by itself meanwhile.
 */
@Composable internal fun LinkBadge(link: LinkState, admin: Boolean, onRetry: () -> Unit) {
    if (!link.offline && link.waiting.isEmpty()) return
    val tint = if (link.offline) LifeRed else Gold
    val label = if (link.offline) link.outage?.title ?: ui("link.offline") else ui("link.waiting", link.waiting.size)
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clickable(onClickLabel = ui("link.retry")) { if (link.offline) open = true else onRetry() }.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(if (link.offline) Icons.Outlined.CloudOff else Icons.Outlined.CloudUpload, label, tint = tint, modifier = Modifier.size(18.dp))
            if (link.waiting.isNotEmpty()) Text(link.waiting.size.toString(), color = tint, fontSize = 11.sp)
        }
        DropdownMenu(open && link.offline, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            Column(Modifier.widthIn(max = 280.dp).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, color = LifeRed, style = MaterialTheme.typography.titleSmall)
                link.outage?.let { Text(it.hint, color = Parchment, style = MaterialTheme.typography.bodySmall) }
                Text(ui("link.auto_retry"), color = Muted, style = MaterialTheme.typography.labelSmall)
                if (link.waiting.isNotEmpty()) Text(ui("link.waiting", link.waiting.size), color = Gold, style = MaterialTheme.typography.labelSmall)
                if (admin) link.detail?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = {
                    open = false
                    onRetry()
                }) { Text(ui("link.retry_now"), color = GoldBright) }
            }
        }
    }
}

/** The craft under way, very short: its name and the cycle filling, every frame. Nothing when the hero works at nothing. */
@Composable internal fun WorkBadge(s: ForgeState, onClick: () -> Unit) {
    val crafts = s.play.crafts ?: return
    val work = crafts.work ?: return
    val offset = crafts.now - s.play.craftsAt
    val now by produceState(System.currentTimeMillis()) { while (true) withFrameMillis { value = System.currentTimeMillis() } }
    Column(Modifier.widthIn(max = 120.dp).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(ForgeGlyphs.Anvil, null, tint = Gold, modifier = Modifier.size(12.dp))
            Text(workTitle(work.job, work.choice), color = Parchment, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LinearProgressIndicator(
            progress = { if (work.cycleMillis > 0) ((now + offset - work.settledAt).toFloat() / work.cycleMillis).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 1.dp),
            color = Gold,
            trackColor = PanelRaised,
        )
    }
}
