package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildStashEntry
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.GuildBranch
import com.sperance.exileforge.rules.content.GuildNode
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/**
 * The guild's bonus tree (3.79.0, server 1.74.0): three branches, each node with its ranks; a point for every guild level,
 * spent by the leader, the second row open once the branch holds the rules' points. Combat and loot ride on every member's
 * runs, economy on the guild itself. A reset gives every point back, free once a week.
 */
@Composable internal fun TreeTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val rule = game.index?.guilds?.tree ?: return
    val leader = me?.role == GuildRole.LEADER
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

/**
 * The guild stash (3.79.0, server 1.74.0): tabs of the rules' places; anyone puts in, taking needs the tab's rank and,
 * for a member, one of the day's takes — a stack is one take. What lies here is the guild's.
 */
@Composable internal fun StashTab(game: GameUi, vm: GuildViewModel, me: GuildMember?) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadStash() }
    val stash = guilds.stash ?: return
    val ranks = game.index?.guilds?.ranks.orEmpty()
    var tab by remember { mutableIntStateOf(0) }
    var depositing by remember { mutableStateOf(false) }
    val shown = stash.entries.filter { it.tab == tab }
    val minRank = stash.tabs.getOrNull(tab)?.minRank ?: 0
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                if (stash.tabs.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stash.tabs.indices.toList()) { i ->
                            FilterChip(selected = i == tab, onClick = { tab = i }, label = { Text(ui("guild.stash_tab", i + 1)) })
                        }
                    }
                }
                PropertyRow(ui("guild.stash_places"), "${shown.size} / ${stash.tabSize}", com.sperance.exileforge.core.display.Glyph.ITEM)
                PropertyRow(ui("guild.stash_rank"), ranks.getOrNull(minRank)?.code?.let(GuildText::rank).orEmpty(), com.sperance.exileforge.core.display.Glyph.CHARACTER)
                Text(if (stash.takesLeft < 0) ui("guild.stash_takes_free") else ui("guild.stash_takes", stash.takesLeft), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (me?.role == GuildRole.LEADER && ranks.isNotEmpty()) {
                    Spinner(ui("guild.stash_rank_set"), minRank.toString(), ranks.indices.associate { it.toString() to GuildText.rank(ranks[it].code) }, !game.busy) {
                        vm.tabRank(tab, it.toInt())
                    }
                }
                ForgeButton(enabled = !game.busy && shown.size < stash.tabSize, onClick = { depositing = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("guild.stash_put")) }
            }
        }
        if (shown.isEmpty()) item { MutedText(ui("guild.stash_empty")) }
        items(shown, key = { it.id }) { entry -> EntryRow(game, entry, enabled = !game.busy && stash.takesLeft != 0) { vm.take(entry.id) } }
    }
    if (depositing) {
        DepositSheet(game, onDismiss = { depositing = false }) { itemId, code, amount ->
            depositing = false
            vm.deposit(tab, itemId, code, amount)
        }
    }
}

@Composable private fun EntryRow(game: GameUi, entry: GuildStashEntry, enabled: Boolean, onTake: () -> Unit) {
    val view = entry.item?.let { game.view(it) }
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (view != null) ItemIcon(view, rarityColor(view.rarity.name), Modifier.size(32.dp)) else BagIcon(entry.code, Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    view?.title ?: "${itemTitle(entry.code)} × ${entry.amount}",
                    color = view?.let { rarityColor(it.rarity.name) } ?: Parchment,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(ui("guild.stash_by", entry.by), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onTake, enabled = enabled) { Text(ui("guild.stash_take"), color = Gold) }
        }
    }
}

/** What the hero may put in: a loose item of the stash, or part of a stack of the bag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepositSheet(game: GameUi, onDismiss: () -> Unit, onPut: (itemId: String?, code: String?, amount: Long) -> Unit) {
    val hero = game.hero ?: return
    val loose = hero.items.filter { !it.equipped && !it.socketed && !it.locked }
    val stacks = hero.bag.filterValues { it > 0 }.keys.sortedBy { itemTitle(it) }
    var stack by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("1") }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("guild.stash_put")) }
            stack?.let { code ->
                item {
                    ForgePanel {
                        Text(itemTitle(code), color = Parchment, style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            amount,
                            { amount = it.filter(Char::isDigit).take(12) },
                            label = { Text(ui("sell.amount_owned", hero.bag[code] ?: 0L)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val count = amount.toLongOrNull() ?: 0L
                        ForgeButton(enabled = !game.busy && count in 1..(hero.bag[code] ?: 0L), onClick = { onPut(null, code, count) }, modifier = Modifier.fillMaxWidth()) {
                            Text(ui("guild.stash_put"))
                        }
                    }
                }
            }
            items(loose, key = { it.id }) { item ->
                val view = game.view(item) ?: return@items
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !game.busy) { onPut(item.id, null, 1) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ItemIcon(view, rarityColor(view.rarity.name), Modifier.size(28.dp))
                    Text(view.title, color = rarityColor(view.rarity.name), style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(stacks, key = { "bag:$it" }) { code ->
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !game.busy) {
                        stack = code
                        amount = "1"
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BagIcon(code, Modifier.size(28.dp))
                    Text("${itemTitle(code)} × ${hero.bag[code]}", color = Parchment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
