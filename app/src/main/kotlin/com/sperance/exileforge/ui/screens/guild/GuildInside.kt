package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.guild.GuildTab
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.model.guild.levelProgress
import com.sperance.exileforge.core.model.guild.manages
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.quests.GuildQuestsTab
import com.sperance.exileforge.ui.theme.*

/**
 * Inside a guild (variant A, «Списки и плитки»): its arms and name over everything, the level filling, the roll against
 * the ceiling and the leader's word; under them a grid of the guild's sections, each with its figure and a badge where
 * something waits. A tile opens its section over the hub, «back» leading to it. The applications are only for those
 * who may answer them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.GuildInside(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    val manages = me?.role?.manages == true
    val tabs = GuildTab.entries.filter { it != GuildTab.APPLICATIONS || manages }
    val tab = guilds.tab?.takeIf { it in tabs }
    if (tab == null) {
        PullToRefreshBox(isRefreshing = Reads.GUILD in game.loading, onRefresh = vm::load, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuildHeader(game, guild)
                tabs.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { each ->
                            SectionTile(
                                each,
                                sectionFigure(each, guild, me),
                                badge = if (each == GuildTab.APPLICATIONS) guild.applications.size else 0,
                                modifier = Modifier.weight(1f),
                            ) { vm.tab(each) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        return
    }
    BackRow("${GuildText.title(guild.name, guild.tag)} · ${tabTitle(tab)}") { vm.tab(null) }
    PullToRefreshBox(isRefreshing = Reads.GUILD in game.loading, onRefresh = vm::load, modifier = Modifier.weight(1f)) {
        when (tab) {
            GuildTab.MEMBERS -> MembersTab(game, vm, guild, me)
            GuildTab.QUESTS -> GuildQuestsTab(game, guild)
            GuildTab.TREE -> TreeTab(game, vm, guild, me)
            GuildTab.STASH -> StashTab(game, vm, me)
            GuildTab.APPLICATIONS -> ApplicationsTab(game, vm, guild)
            GuildTab.CONTRIBUTE -> ContributeTab(game, vm, guild, me)
            GuildTab.LOG -> LogTab(game, vm)
            GuildTab.SETTINGS -> SettingsTab(game, vm, guild, me)
        }
    }
}

private fun tabTitle(tab: GuildTab): String = ui(
    when (tab) {
        GuildTab.MEMBERS -> "guild.tab_members"
        GuildTab.QUESTS -> "guild.tab_quests"
        GuildTab.TREE -> "guild.tab_tree"
        GuildTab.STASH -> "guild.tab_stash"
        GuildTab.APPLICATIONS -> "guild.tab_applications"
        GuildTab.CONTRIBUTE -> "guild.tab_contribute"
        GuildTab.LOG -> "guild.tab_log"
        GuildTab.SETTINGS -> "guild.tab_settings"
    },
)

private fun sectionIcon(tab: GuildTab): ImageVector = when (tab) {
    GuildTab.MEMBERS -> Icons.Outlined.Groups
    GuildTab.QUESTS -> ForgeGlyphs.Scroll
    GuildTab.TREE -> ForgeGlyphs.Constellation
    GuildTab.STASH -> ForgeGlyphs.Stash
    GuildTab.APPLICATIONS -> Icons.Outlined.MailOutline
    GuildTab.CONTRIBUTE -> ForgeGlyphs.Coins
    GuildTab.LOG -> Icons.AutoMirrored.Outlined.ReceiptLong
    GuildTab.SETTINGS -> Icons.Outlined.Settings
}

/** The one figure a tile carries: the roll, the new applications, the hero's rank and role; none where the section has none at hand. */
private fun sectionFigure(tab: GuildTab, guild: GuildView, me: GuildMember?): String? = when (tab) {
    GuildTab.MEMBERS -> "${guild.members.size}/${guild.capacity}".takeIf { guild.capacity > 0 } ?: guild.members.size.toString()
    GuildTab.APPLICATIONS -> ui("guild.applications_new", guild.applications.size)
    GuildTab.CONTRIBUTE -> me?.rank?.takeIf { it.isNotBlank() }?.let(GuildText::rank)
    GuildTab.SETTINGS -> me?.role?.let(GuildText::role)
    GuildTab.TREE -> guild.treePoints.takeIf { it > 0 }?.let { ui("guild.tree_free", it) }
    GuildTab.QUESTS, GuildTab.LOG, GuildTab.STASH -> null
}

/** One section of the hub: its glyph, its name, its figure under it, and a badge in the corner when something waits. */
@Composable private fun SectionTile(tab: GuildTab, figure: String?, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.clip(shape).background(Panel, shape).border(1.dp, PanelRaised, shape).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(sectionIcon(tab), null, tint = Gold, modifier = Modifier.size(22.dp))
            Text(tabTitle(tab), color = Parchment, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(figure.orEmpty(), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (badge > 0) Badge(containerColor = Vital, contentColor = Ink, modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp)) { Text(badge.toString()) }
    }
}

@Composable private fun GuildHeader(game: GameUi, guild: GuildView) {
    val rules = game.index?.guilds
    val capacity = guild.capacity.takeIf { it > 0 } ?: rules?.capacity(guild.level) ?: 0
    ForgePanel(accent = guildColor(guild.color)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GuildEmblem(guild.emblem, guild.color, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(GuildText.title(guild.name, guild.tag), color = GoldBright, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FactionLine(guild.faction, rules, ui("guild.header_line", guild.members.size, capacity))
            }
            GuideButton(Guide.GUILD)
        }
        val progress = rules?.levelProgress(guild.level, guild.experience)
            ?: if (guild.next > 0) (guild.experience.toFloat() / guild.next).coerceIn(0f, 1f) else 1f
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ui("guild.level", guild.level), color = Rune, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            MutedText(if (guild.next > 0) ui("guild.xp", number(guild.experience.toDouble()), number(guild.next.toDouble())) else ui("guild.xp_max"))
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = guildColor(guild.color), trackColor = Abyss)
        // The leader's word in a dark band under its flag, one glance from the arms.
        guild.announcement.takeIf { it.isNotBlank() }?.let {
            Row(
                Modifier.fillMaxWidth().background(Abyss, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.Flag, ui("guild.announcement"), tint = Ember, modifier = Modifier.size(16.dp))
                Text(it, color = Parchment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
