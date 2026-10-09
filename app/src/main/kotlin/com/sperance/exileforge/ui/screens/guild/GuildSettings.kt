package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** Выход из гильдии: участник уходит, глава распускает её (уйти главе, пока есть другие, нельзя - сначала передать главенство). */
private enum class Exit(val label: String, val question: String) {
    LEAVE("guild.leave", "guild.leave_q"),
    DISBAND("guild.disband", "guild.disband_q"),
}

/**
 * Меню «⋮» в шапке зала гильдии (4.4.x): выход - покинуть гильдию или, тому, кому таблица [GuildPolicy] даёт роспуск,
 * распустить её; каждое спрашивается ещё раз ([ConfirmSheet]).
 */
@Composable internal fun GuildExitMenu(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    me ?: return
    val rules = game.index?.guilds
    val way = if (GuildPolicy.can(me.role, GuildAction.DISBAND)) Exit.DISBAND else Exit.LEAVE
    var open by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, ui("common.more"), tint = Muted) }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            DropdownMenuItem(
                text = { Text(ui(way.label), color = LifeRed) },
                enabled = !game.busy,
                onClick = {
                    open = false
                    asking = true
                },
            )
        }
    }
    if (asking) {
        ConfirmSheet(
            ui(way.question),
            ui(way.label),
            onDismiss = { asking = false },
            subtitle = GuildText.title(guild.name, guild.tag),
            note = when (way) {
                Exit.LEAVE -> ui("guild.leave_note", rules?.rejoinHours ?: 24)
                Exit.DISBAND -> ui("guild.disband_note")
            },
            danger = true,
            blocked = game.busy,
            onConfirm = {
                when (way) {
                    Exit.LEAVE -> vm.leave()
                    Exit.DISBAND -> vm.disband()
                }
            },
        )
    }
}

/**
 * The guild's settings (с 4.4.x раздел только главы - [com.sperance.exileforge.core.guild.GuildTab.SETTINGS]): the way in, the
 * level to join, the arms and the word to all. Выход - в меню шапки зала ([GuildExitMenu]).
 */
@Composable internal fun SettingsTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val rules = game.index?.guilds
    val leader = me != null && GuildPolicy.can(me.role, GuildAction.SETTINGS)
    var mode by remember(guild.mode) { mutableStateOf(guild.mode) }
    var minLevel by remember(guild.minLevel) { mutableStateOf(guild.minLevel.toString()) }
    var emblem by remember(guild.emblem) { mutableStateOf(guild.emblem) }
    var color by remember(guild.color) { mutableStateOf(guild.color) }
    var announcement by remember(guild.announcement) { mutableStateOf(guild.announcement) }
    val limit = rules?.announcement ?: 200
    val editable = leader && !game.busy
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Engraved(ui("guild.entry"))
                ModePicker(mode, editable) { mode = it }
                MinLevelField(minLevel, editable) { minLevel = it }
            }
        }
        rules?.let { r ->
            item {
                ForgePanel {
                    Engraved(ui("guild.arms"))
                    ArmsPicker(r.emblems, r.colors, emblem, color, editable, { emblem = it }, { color = it })
                }
            }
        }
        item {
            ForgePanel {
                Engraved(ui("guild.announcement"))
                OutlinedTextField(
                    announcement,
                    { announcement = it.take(limit) },
                    enabled = editable,
                    minLines = 2,
                    maxLines = 5,
                    supportingText = { Text("${announcement.length}/$limit") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (leader) {
                    ForgeButton(enabled = !game.busy, onClick = {
                        vm.settings(mode, (minLevel.toIntOrNull() ?: 1).coerceAtLeast(1), emblem, color, announcement.trim())
                    }, modifier = Modifier.fillMaxWidth()) { Text(ui("guild.save")) }
                }
            }
        }
    }
}
