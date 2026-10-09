package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.theme.*

/**
 * Что [me] может сделать с [target] - по общей таблице прав [GuildPolicy]: себя целью не выбирают. Сервер решает так же;
 * список лишь не предлагает того, что он отверг бы сразу. Повышение без свободного места офицера остаётся в списке
 * неактивным - с причиной в карточке.
 */
private fun commandsOn(me: GuildMember?, target: GuildMember): List<MemberCommand> {
    if (me == null || target.heroId == me.heroId) return emptyList()
    return MemberCommand.entries.filter { GuildPolicy.can(me.role, it.action, target.role) }
}

/**
 * Состав таблицей (4.5.1, утверждён макет «Таблица»): заголовки «Ур.», «Неделя», «Всего» сортируют по нажатию (повтор - обратный
 * порядок), без выбора - глава, офицеры, участники, внутри по вкладу. Нажатие строки открывает карточку игрока; у кого есть
 * права, тот находит в ней прежний лист званий. Офицер и глава зовут по имени.
 */
@Composable internal fun MembersTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    var chosen by remember { mutableStateOf<GuildMember?>(null) }
    var pending by remember { mutableStateOf<Pair<GuildMember, MemberCommand>?>(null) }
    var order by remember { mutableStateOf<RosterOrder?>(null) }
    val openPlayer = rememberPlayerCard()
    // Места офицеров - из `guilds.json`; без прочитанного контента повышение не предлагается
    val officerSeat = game.index?.guilds?.officerSeat(guild.officers) == true
    val roll = remember(guild.members, order) { order?.sort(guild.members) ?: RosterOrder.byRole(guild.members) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (me != null && GuildPolicy.can(me.role, GuildAction.RECRUIT)) {
            item {
                InviteField(game, vm)
                Spacer(Modifier.height(8.dp))
            }
        }
        item { RosterHeader(order) { column -> order = order.next(column) } }
        items(roll, key = { it.heroId }) { member ->
            val commands = commandsOn(me, member)
            val manage = commands.takeIf { it.isNotEmpty() }?.let { PlayerAction(ui("guild.manage")) { chosen = member } }
            MemberRow(member, isMe = member.heroId == me?.heroId) { openPlayer(member.heroId, manage) }
        }
        item { RosterFooter(guild.members) }
    }
    chosen?.let { member ->
        MemberSheet(game, member, commandsOn(me, member), officerSeat, onDismiss = { chosen = null }) { command ->
            chosen = null
            pending = member to command
        }
    }
    pending?.let { (member, command) ->
        ConfirmSheet(
            title = confirmTitle(command, member.name),
            confirm = commandTitle(command),
            onDismiss = { pending = null },
            subtitle = GuildText.role(member.role),
            note = confirmNote(command),
            danger = command == MemberCommand.KICK || command == MemberCommand.TRANSFER,
            blocked = game.busy,
        ) { vm.member(command, member.heroId) }
    }
}

/** Столбец состава, по которому его можно упорядочить. */
private enum class RosterColumn(val title: String, val value: (GuildMember) -> Long) {
    LEVEL("guild.col_level", { it.level.toLong() }),
    WEEK("guild.col_week", { it.weekContribution }),
    TOTAL("guild.col_total", { it.contribution }),
}

/** Порядок состава: столбец и направление; равные - по имени. */
private data class RosterOrder(val column: RosterColumn, val descending: Boolean = true) {
    fun sort(members: List<GuildMember>): List<GuildMember> {
        val by = compareBy<GuildMember> { column.value(it) }.let { if (descending) it.reversed() else it }
        return members.sortedWith(by.thenBy { it.name.lowercase() })
    }

    companion object {
        /** Без выбора: глава, офицеры, участники, внутри - по вкладу за всё время. */
        fun byRole(members: List<GuildMember>): List<GuildMember> = members.sortedWith(compareBy<GuildMember> { it.role.ordinal }.thenByDescending { it.contribution })
    }
}

/** Нажатие на заголовок [column]: новый столбец - по убыванию, тот же - обратный порядок. */
private fun RosterOrder?.next(column: RosterColumn): RosterOrder = if (this?.column == column) copy(descending = !descending) else RosterOrder(column)

@Composable private fun RosterHeader(order: RosterOrder?, onSort: (RosterColumn) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(ui("guild.col_member"), Modifier.weight(1f), TextAlign.Start)
        RosterColumn.entries.forEach { column ->
            val mark = when {
                order?.column != column -> ""
                order.descending -> " ↓"
                else -> " ↑"
            }
            HeaderCell(ui(column.title) + mark, Modifier.width(column.width).clickable { onSort(column) }, TextAlign.End, active = order?.column == column)
        }
    }
    HorizontalDivider(color = Bronze, thickness = 1.dp)
}

@Composable private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign, active: Boolean = false) {
    Text(
        text.uppercase(),
        color = if (active) GoldBright else Muted,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
        maxLines = 1,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

private val RosterColumn.width: Dp get() = if (this == RosterColumn.LEVEL) 36.dp else 64.dp

/**
 * Строка состава: точка присутствия, роль, имя, под ним «класс · ранг · был N назад» (у себя - суточный лимит вклада); справа
 * уровень, вклад за неделю и за всё время. Своя строка подсвечена.
 */
@Composable private fun MemberRow(member: GuildMember, isMe: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(if (isMe) Gold.copy(alpha = .06f) else Color.Transparent).clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PresenceDot(member.online, member.lastSeenAt, 7.dp)
                    GuildRoleBadge(member.role)
                    Text(
                        if (isMe) ui("guild.me", member.name) else member.name,
                        color = GoldBright,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(memberLine(member, isMe), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            NumberCell(member.level.toString(), RosterColumn.LEVEL)
            NumberCell(number(member.weekContribution.toDouble()), RosterColumn.WEEK)
            NumberCell(number(member.contribution.toDouble()), RosterColumn.TOTAL)
        }
        HorizontalDivider(color = Bronze.copy(alpha = .5f), thickness = 1.dp)
    }
}

@Composable private fun NumberCell(text: String, column: RosterColumn) {
    Text(text, color = Parchment, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(column.width))
}

/** «класс · ранг · был N назад»; у себя вместо присутствия - сколько вклада внесено из суточного потолка. */
private fun memberLine(member: GuildMember, isMe: Boolean): String {
    val tail = if (isMe && member.dayLimit > 0) {
        ui("guild.day_limit", number((member.dayLimit - member.dayLeft).coerceAtLeast(0).toDouble()), number(member.dayLimit.toDouble()))
    } else {
        seenAgoText(member.online, member.lastSeenAt)
    }
    return listOf(classTitle(member.heroClass), GuildText.rank(member.rank), tail).joinToString(" · ")
}

/** Итог состава: «В сети X из N · Неделя: Σ». */
@Composable private fun RosterFooter(members: List<GuildMember>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)) {
        MutedText(ui("guild.roster_online", members.count { it.online }, members.size), Modifier.weight(1f))
        MutedText(ui("guild.roster_week", number(members.sumOf { it.weekContribution }.toDouble())))
    }
}

@Composable private fun InviteField(game: GameUi, vm: GuildViewModel) {
    var name by remember { mutableStateOf("") }
    ForgePanel {
        Engraved(ui("guild.invite_title"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(game.inputs.heroName) }, label = { Text(ui("guild.hero_name")) }, singleLine = true, modifier = Modifier.weight(1f))
            ForgeButton(enabled = !game.busy && name.isNotBlank(), onClick = {
                vm.invite(name)
                name = ""
            }) { Text(ui("guild.invite")) }
        }
    }
}

/** A member's card with the commands this hero may give about them; each one asks again before it goes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSheet(game: GameUi, member: GuildMember, commands: List<MemberCommand>, officerSeat: Boolean, onDismiss: () -> Unit, onCommand: (MemberCommand) -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(member.name, color = GoldBright, style = MaterialTheme.typography.titleLarge)
            MutedText(ui("guild.member_line", GuildText.role(member.role), GuildText.rank(member.rank), member.level, classTitle(member.heroClass)))
            MutedText(ui("guild.member_joined", clockText(member.joinedAt), number(member.contribution.toDouble())))
            commands.forEach { command ->
                val seatless = command == MemberCommand.PROMOTE && !officerSeat
                ForgeOutlinedButton(enabled = !game.busy && !seatless, onClick = { onCommand(command) }, modifier = Modifier.fillMaxWidth()) { Text(commandTitle(command)) }
                if (seatless) MutedText(ui("guild.officers_full", game.index?.guilds?.officers ?: 0))
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
@Composable internal fun ApplicationsTab(game: GameUi, vm: GuildViewModel, guild: GuildView) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (guild.applications.isEmpty()) item { InfoCard(ui("guild.no_applications"), ui("guild.no_applications_hint")) }
        items(guild.applications, key = { it.heroId }) { applicant ->
            ForgePanel {
                Text(applicant.name, color = Parchment, style = MaterialTheme.typography.titleSmall, modifier = Modifier.opensPlayer(applicant.heroId))
                MutedText(ui("guild.applicant_line", applicant.level, classTitle(applicant.heroClass), clockText(applicant.at)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForgeOutlinedButton(enabled = !game.busy, onClick = { vm.declineApplicant(applicant.heroId) }, modifier = Modifier.weight(1f)) { Text(ui("guild.decline")) }
                    ForgeButton(enabled = !game.busy, onClick = { vm.acceptApplicant(applicant.heroId) }, modifier = Modifier.weight(1f)) { Text(ui("guild.accept")) }
                }
            }
        }
    }
}
