package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.guildEntryLevel
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildPolicy
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
 * Состав таблицей («Плотная таблица»): над ней поиск по имени и чипы отбора
 * ([RosterFilter]), порядок - кнопкой «⇅» у поиска: «Ур.», «Неделя», «Всего» (повтор - обратный порядок), без выбора - глава,
 * офицеры, участники, внутри по вкладу. Нажатие строки открывает карточку игрока; у кого есть права, тот находит в ней
 * лист званий. Офицер и глава зовут по имени.
 *
 * Места офицеров - из `guilds.json`: без прочитанного контента повышение не предлагается. У ролей с приёмом справа от поиска -
 * «+ Пригласить»: лист [InviteSheet].
 */
@Composable internal fun MembersTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    var chosen by remember { mutableStateOf<GuildMember?>(null) }
    var pending by remember { mutableStateOf<Pair<GuildMember, MemberCommand>?>(null) }
    var inviting by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf<RosterOrder?>(null) }
    var filter by remember { mutableStateOf(RosterFilter.ALL) }
    var query by remember { mutableStateOf("") }
    val openPlayer = rememberPlayerCard()
    val officerSeat = game.index?.guilds?.officerSeat(guild.officers) == true
    val recruits = me != null && GuildPolicy.can(me.role, GuildAction.RECRUIT)
    val scope = rememberRosterScope(game, guild.members) ?: return
    val roll = remember(guild.members, order) { order?.sort(guild.members) ?: RosterOrder.byRole(guild.members) }
    val shown = remember(roll, filter, query, scope) { roll.filter { filter.keeps(it, scope) && it.named(query) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            RosterSearch(
                game,
                query,
                order,
                onQuery = { query = it },
                onOrder = { order = it },
                trailing = if (recruits) {
                    {
                        ToolButton(Icons.Outlined.Add, ui("guild.invite"), Gold, enabled = !game.busy) {
                            vm.openInvite()
                            inviting = true
                        }
                    }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(6.dp))
            RosterFilters(guild.members, filter, scope) { filter = it }
            Spacer(Modifier.height(4.dp))
        }
        item { RosterHeader() }
        items(shown, key = { it.heroId }) { member ->
            val commands = commandsOn(me, member)
            val manage = commands.takeIf { it.isNotEmpty() }?.let { PlayerAction(ui("guild.manage")) { chosen = member } }
            MemberRow(game, member, isMe = member.heroId == me?.heroId) { openPlayer(member.heroId, manage) }
        }
        if (shown.isEmpty()) item { MutedText(ui("guild.roster_empty"), Modifier.fillMaxWidth().padding(vertical = 16.dp)) }
        item { RosterFooter(guild.members) }
    }
    if (inviting) {
        InviteSheet(game, vm, guild) {
            vm.closeInvite()
            inviting = false
        }
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

/**
 * Лист «Пригласить героя»: код вступления гильдии с копированием и «Новый код», подсказка к нему; номер героя из шести цифр
 * или имя и «Найти» - одно чтение превью (имя, класс, уровень, номер). Подвал - «Пригласить «Имя…»»: пока героя нет, он
 * в чужой гильдии или ниже порога гильдии, кнопка погашена, причина под ней. Приглашение закрывает лист.
 *
 * Поле живёт с листом; найденного героя забывают [GuildViewModel.openInvite] и [GuildViewModel.closeInvite] у зовущего.
 */
@Composable private fun InviteSheet(game: GameUi, vm: GuildViewModel, guild: GuildView, onDismiss: () -> Unit) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    var target by remember { mutableStateOf("") }
    val found = guilds.recruit
    val need = guildEntryLevel(game.index?.rules, guild.minLevel)
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = {
            val reason = when {
                found == null -> ui("guild.invite_find_first")
                found.guild != null -> ui("guild.recruit_taken", found.guild)
                found.level < need -> ui("guild.recruit_low", need)
                else -> null
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ForgeButton(
                    enabled = !game.busy && reason == null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        found?.let { hero -> vm.invite(hero.number.ifBlank { hero.name }) }
                        onDismiss()
                    },
                ) { Text(GuildText.invite(found?.name), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                reason?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
            }
        },
    ) {
        Text(ui("guild.invite_title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
        guild.code?.let { code ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShortCodeChip(ui("guild.code"), code, Modifier.weight(1f, fill = false))
                ForgeTextButton(onClick = vm::renewCode, enabled = !game.busy) { Text(ui("guild.code_renew"), color = Muted) }
            }
            MutedText(ui("guild.code_hint"))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                target,
                {
                    target = it.take(game.inputs.heroName)
                    if (found != null) vm.clearRecruit()
                },
                label = { Text(ui("guild.hero_target")) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            ForgeOutlinedButton(enabled = !game.busy && target.isNotBlank(), onClick = { vm.lookup(target) }) { Text(ui("guild.find")) }
        }
        found?.let { hero ->
            ForgePanel {
                Text(hero.name, color = GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("guild.recruit_line", classTitle(hero.heroClass), hero.level, hero.number))
            }
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
