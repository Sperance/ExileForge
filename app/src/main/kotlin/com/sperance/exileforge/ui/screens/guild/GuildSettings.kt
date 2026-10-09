package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/** The two ways out: a member leaves, the leader disbands. */
private enum class Exit { LEAVE, DISBAND }

/**
 * The guild's settings: the leader changes the way in, the level to join, the arms and the word to all; everyone else
 * sees them read-only. At the bottom the way out — leaving for a member, disbanding for the leader, each asked again.
 */
@Composable internal fun SettingsTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val rules = game.index?.guilds
    val leader = me != null && GuildPolicy.can(me.role, GuildAction.SETTINGS)
    val disbands = me != null && GuildPolicy.can(me.role, GuildAction.DISBAND)
    var mode by remember(guild.mode) { mutableStateOf(guild.mode) }
    var minLevel by remember(guild.minLevel) { mutableStateOf(guild.minLevel.toString()) }
    var emblem by remember(guild.emblem) { mutableStateOf(guild.emblem) }
    var color by remember(guild.color) { mutableStateOf(guild.color) }
    var announcement by remember(guild.announcement) { mutableStateOf(guild.announcement) }
    var exit by remember { mutableStateOf<Exit?>(null) }
    val limit = rules?.announcement ?: 200
    val editable = leader && !game.busy
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Engraved(ui("guild.entry"))
                if (!leader) MutedText(ui("guild.settings_leader_only"))
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
        item {
            ForgePanel(accent = LifeRed) {
                Engraved(ui("guild.exit"), LifeRed)
                if (disbands) {
                    MutedText(ui("guild.leader_exit_note"))
                    ForgeOutlinedButton(enabled = !game.busy, onClick = { exit = Exit.DISBAND }, modifier = Modifier.fillMaxWidth()) { Text(ui("guild.disband")) }
                } else {
                    MutedText(ui("guild.leave_note", rules?.rejoinHours ?: 24))
                    ForgeOutlinedButton(enabled = !game.busy, onClick = { exit = Exit.LEAVE }, modifier = Modifier.fillMaxWidth()) { Text(ui("guild.leave")) }
                }
            }
        }
    }
    exit?.let { chosen ->
        val title = GuildText.title(guild.name, guild.tag)
        when (chosen) {
            Exit.LEAVE -> ConfirmSheet(
                ui("guild.leave_q"),
                ui("guild.leave"),
                onDismiss = { exit = null },
                subtitle = title,
                note = ui("guild.leave_note", rules?.rejoinHours ?: 24),
                danger = true,
                blocked = game.busy,
                onConfirm = vm::leave,
            )

            Exit.DISBAND -> ConfirmSheet(
                ui("guild.disband_q"),
                ui("guild.disband"),
                onDismiss = { exit = null },
                subtitle = title,
                note = ui("guild.disband_note"),
                danger = true,
                blocked = game.busy,
                onConfirm = vm::disband,
            )
        }
    }
}
