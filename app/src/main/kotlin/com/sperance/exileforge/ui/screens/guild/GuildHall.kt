package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildBranch
import com.sperance.exileforge.rules.content.GuildNode
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * The guild's bonus tree (3.79.0, server 1.74.0): three branches, each node with its ranks; a point for every guild level,
 * spent by the leader, the second row open once the branch holds the rules' points. Combat and loot ride on every member's
 * runs, economy on the guild itself. A reset gives every point back, free once a week.
 */
@Composable internal fun TreeTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val rule = game.index?.guilds?.tree ?: return
    val leader = me != null && GuildPolicy.can(me.role, GuildAction.TREE)
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                PropertyRow(ui("guild.tree_points"), "${guild.treePoints} / ${rule.points(guild.level)}", com.sperance.exileforge.core.display.Glyph.LEVEL)
                MutedText(ui(if (leader) "guild.tree_hint_leader" else "guild.tree_hint"), style = MaterialTheme.typography.bodySmall)
                if (leader) {
                    val free = System.currentTimeMillis() >= guild.respecAt
                    ForgeOutlinedButton(enabled = !game.busy && free && guild.tree.isNotEmpty(), onClick = vm::resetTree, modifier = Modifier.fillMaxWidth()) {
                        Text(if (free) ui("guild.tree_reset") else ui("guild.tree_reset_at", java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(guild.respecAt))))
                    }
                }
            }
        }
        GuildBranch.entries.forEach { branch ->
            item(key = branch.name) {
                ForgePanel {
                    Engraved(loc("guild.branch.${branch.name}") + " · " + rule.inBranch(guild.tree, branch))
                    rule.nodes.filter { it.branch == branch }.sortedBy { it.row }.forEach { node ->
                        NodeRow(
                            node,
                            guild.tree[node.code] ?: 0,
                            open = node.row <= 1 || rule.inBranch(guild.tree, branch) >= rule.rowGate,
                            canTake = leader && !game.busy && rule.canTake(guild.tree, guild.level, node.code),
                        ) { vm.takeNode(node.code) }
                    }
                }
            }
        }
    }
}

@Composable private fun NodeRow(node: GuildNode, ranks: Int, open: Boolean, canTake: Boolean, onTake: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                loc("guild.node.${node.code}.name") + "  $ranks/${node.max}",
                color = if (ranks > 0) {
                    GoldBright
                } else if (open) {
                    Parchment
                } else {
                    Muted
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(loc("guild.node.${node.code}.desc", listOf(number(node.perRank))), color = Muted, style = MaterialTheme.typography.labelSmall)
            if (!open) Text(ui("guild.tree_row_locked"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        if (canTake) TextButton(onClick = onTake) { Text("+1", color = GoldBright) }
    }
}
