package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.model.guild.levelProgress
import com.sperance.exileforge.core.model.guild.manages
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.GuildTab
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.quests.GuildQuestsTab
import com.sperance.exileforge.ui.theme.*

/**
 * Inside a guild: its arms and name over everything, the level filling, the roll against the ceiling and the leader's
 * word; under them the tabs. The applications' tab is only for those who may answer them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ColumnScope.GuildInside(s: ForgeState, vm: ForgeViewModel, guild: GuildView, me: GuildMember?) {
    val manages = me?.role?.manages == true
    val tabs = GuildTab.entries.filter { it != GuildTab.APPLICATIONS || manages }
    val tab = s.guild.tab.takeIf { it in tabs } ?: GuildTab.MEMBERS
    GuildHeader(s, guild)
    ScrollableTabRow(selectedTabIndex = tabs.indexOf(tab), containerColor = Abyss, edgePadding = 0.dp) {
        tabs.forEach { each ->
            Tab(selected = each == tab, onClick = { vm.guildTab(each) }, text = { Text(tabTitle(each, guild), style = MaterialTheme.typography.labelLarge) })
        }
    }
    PullToRefreshBox(isRefreshing = Reads.GUILD in s.loading, onRefresh = vm::loadGuild, modifier = Modifier.weight(1f)) {
        when (tab) {
            GuildTab.MEMBERS -> MembersTab(s, vm, guild, me)
            GuildTab.QUESTS -> GuildQuestsTab(s, vm, guild)
            GuildTab.APPLICATIONS -> ApplicationsTab(s, vm, guild)
            GuildTab.CONTRIBUTE -> ContributeTab(s, vm, guild, me)
            GuildTab.LOG -> LogTab(s, vm)
            GuildTab.SETTINGS -> SettingsTab(s, vm, guild, me)
        }
    }
}

private fun tabTitle(tab: GuildTab, guild: GuildView): String = when (tab) {
    GuildTab.MEMBERS -> ui("guild.tab_members")
    GuildTab.QUESTS -> ui("guild.tab_quests")
    GuildTab.APPLICATIONS -> if (guild.applications.isEmpty()) ui("guild.tab_applications") else ui("guild.tab_applications_n", guild.applications.size)
    GuildTab.CONTRIBUTE -> ui("guild.tab_contribute")
    GuildTab.LOG -> ui("guild.tab_log")
    GuildTab.SETTINGS -> ui("guild.tab_settings")
}

@Composable private fun GuildHeader(s: ForgeState, guild: GuildView) {
    val rules = s.index?.guilds
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("guild.level", guild.level), color = Rune, style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(5.dp), color = Vital, trackColor = PanelRaised)
            MutedText(if (guild.next > 0) ui("guild.xp", number(guild.experience.toDouble()), number(guild.next.toDouble())) else ui("guild.xp_max"))
        }
        guild.announcement.takeIf { it.isNotBlank() }?.let {
            Engraved(ui("guild.announcement"))
            Text(it, color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
    }
}
