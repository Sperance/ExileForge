package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.model.guild.manages
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * What [me] may do to [target], by the roles alone: the leader everything but to themself, an officer only to show a
 * member out. The server decides all the same; the list only keeps from offering what it would refuse on sight.
 */
private fun commandsOn(me: GuildMember?, target: GuildMember, officersFull: Boolean): List<MemberCommand> {
    if (me == null || target.heroId == me.heroId) return emptyList()
    return when (me.role) {
        GuildRole.LEADER -> listOfNotNull(
            MemberCommand.PROMOTE.takeIf { target.role == GuildRole.MEMBER && !officersFull },
            MemberCommand.DEMOTE.takeIf { target.role == GuildRole.OFFICER },
            MemberCommand.TRANSFER, MemberCommand.KICK)
        GuildRole.OFFICER -> listOfNotNull(MemberCommand.KICK.takeIf { target.role == GuildRole.MEMBER })
        GuildRole.MEMBER -> emptyList()
    }
}

/** The roll: the leader first, then the officers, each group by contribution; an officer or the leader may invite by name. */
@Composable internal fun MembersTab(s: ForgeState, vm: ForgeViewModel, guild: GuildView, me: GuildMember?) {
    var chosen by remember { mutableStateOf<GuildMember?>(null) }
    var pending by remember { mutableStateOf<Pair<GuildMember, MemberCommand>?>(null) }
    val officersFull = guild.officers >= (s.index?.guilds?.officers ?: MAX_OFFICERS)
    val roll = guild.members.sortedWith(compareBy<GuildMember> { it.role.ordinal }.thenByDescending { it.contribution })
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (me?.role?.manages == true) item { InviteField(s, vm) }
        items(roll, key = { it.heroId }) { member ->
            val commands = commandsOn(me, member, officersFull)
            MemberRow(member, isMe = member.heroId == me?.heroId, onClick = if (commands.isEmpty()) null else ({ chosen = member }))
        }
    }
    chosen?.let { member ->
        MemberSheet(s, member, commandsOn(me, member, officersFull), onDismiss = { chosen = null }) { command -> chosen = null; pending = member to command }
    }
    pending?.let { (member, command) ->
        ConfirmSheet(title = confirmTitle(command, member.name), confirm = commandTitle(command), onDismiss = { pending = null },
            subtitle = GuildText.role(member.role), note = confirmNote(command), danger = command == MemberCommand.KICK || command == MemberCommand.TRANSFER,
            blocked = s.busy) { vm.guildMember(command, member.heroId) }
    }
}

@Composable private fun InviteField(s: ForgeState, vm: ForgeViewModel) {
    var name by remember { mutableStateOf("") }
    ForgePanel {
        Engraved(ui("guild.invite_title"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(ui("guild.hero_name")) }, singleLine = true, modifier = Modifier.weight(1f))
            ForgeButton(enabled = !s.busy && name.isNotBlank(), onClick = { vm.inviteToGuild(name); name = "" }) { Text(ui("guild.invite")) }
        }
    }
}

/** One member: name and role, rank, level and class, when last seen, and what they gave. */
@Composable private fun MemberRow(member: GuildMember, isMe: Boolean, onClick: (() -> Unit)?) {
    ForgePanel(Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), accent = roleColor(member.role)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (isMe) ui("guild.me", member.name) else member.name, color = if (isMe) GoldBright else Parchment,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MutedText(ui("guild.member_line", GuildText.role(member.role), GuildText.rank(member.rank), member.level, classTitle(member.heroClass)))
                MutedText(seenText(member.lastSeenAt))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(number(member.contribution.toDouble()), color = GoldBright, style = MaterialTheme.typography.labelLarge)
                MutedText(ui("guild.contribution_short"))
            }
        }
    }
}

private fun roleColor(role: GuildRole) = when (role) { GuildRole.LEADER -> Gold; GuildRole.OFFICER -> Rune; GuildRole.MEMBER -> Bronze }

/** A member's card with the commands this hero may give about them; each one asks again before it goes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MemberSheet(s: ForgeState, member: GuildMember, commands: List<MemberCommand>, onDismiss: () -> Unit, onCommand: (MemberCommand) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(member.name, color = GoldBright, style = MaterialTheme.typography.titleLarge)
            MutedText(ui("guild.member_line", GuildText.role(member.role), GuildText.rank(member.rank), member.level, classTitle(member.heroClass)))
            MutedText(ui("guild.member_joined", clockText(member.joinedAt), number(member.contribution.toDouble())))
            commands.forEach { command ->
                ForgeOutlinedButton(enabled = !s.busy, onClick = { onCommand(command) }, modifier = Modifier.fillMaxWidth()) { Text(commandTitle(command)) }
            }
        }
    }
}

private fun commandTitle(command: MemberCommand): String = when (command) {
    MemberCommand.PROMOTE -> ui("guild.promote")
    MemberCommand.DEMOTE -> ui("guild.demote")
    MemberCommand.TRANSFER -> ui("guild.transfer")
    MemberCommand.KICK -> ui("guild.kick")
}

private fun confirmTitle(command: MemberCommand, name: String): String = when (command) {
    MemberCommand.PROMOTE -> ui("guild.promote_q", name)
    MemberCommand.DEMOTE -> ui("guild.demote_q", name)
    MemberCommand.TRANSFER -> ui("guild.transfer_q", name)
    MemberCommand.KICK -> ui("guild.kick_q", name)
}

private fun confirmNote(command: MemberCommand): String? = when (command) {
    MemberCommand.TRANSFER -> ui("guild.transfer_note")
    MemberCommand.KICK -> ui("guild.kick_note")
    else -> null
}

/** Those asking in: who, of what class and level, since when; the leader and the officers take them in or turn them away. */
@Composable internal fun ApplicationsTab(s: ForgeState, vm: ForgeViewModel, guild: GuildView) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (guild.applications.isEmpty()) item { InfoCard(ui("guild.no_applications"), ui("guild.no_applications_hint")) }
        items(guild.applications, key = { it.heroId }) { applicant ->
            ForgePanel {
                Text(applicant.name, color = Parchment, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("guild.applicant_line", applicant.level, classTitle(applicant.heroClass), clockText(applicant.at)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForgeOutlinedButton(enabled = !s.busy, onClick = { vm.declineApplicant(applicant.heroId) }, modifier = Modifier.weight(1f)) { Text(ui("guild.decline")) }
                    ForgeButton(enabled = !s.busy, onClick = { vm.acceptApplicant(applicant.heroId) }, modifier = Modifier.weight(1f)) { Text(ui("guild.accept")) }
                }
            }
        }
    }
}

/** The officers' ceiling when `guilds.json` has not been read. */
private const val MAX_OFFICERS = 3
