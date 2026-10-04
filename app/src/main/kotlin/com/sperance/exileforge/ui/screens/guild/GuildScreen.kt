package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.model.guild.GuildInviteView
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.GuildRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The guild (3.22.0, server 1.20.0), a building of the City. A hero outside one finds a guild, answers an invitation or
 * founds a guild of their own; a hero inside sees their guild under its arms, in tabs. Every rule is the server's —
 * the screen only says in advance what a button would be refused for, from `guilds.json`.
 */
@Composable fun GuildScreen() {
    val game by koinViewModel<GuildViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val vm = koinViewModel<GuildViewModel>()
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        if (game.heroId.isNotBlank()) {
            heroModel.ensure()
            vm.load()
        }
    }
    val mine = guilds.mine
    val guild = mine?.guild
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        when {
            guild != null -> {
                FirstVisit(Guide.GUILD)
                GuildInside(game, vm, guild, mine?.me)
            }

            mine != null -> {
                ScreenHeader(ui("guild.title"), ui("guild.outside_subtitle"), ForgeGlyphs.Banner, guide = Guide.GUILD)
                GuildOutside(game, vm, mine)
            }

            else -> {
                ScreenHeader(ui("guild.title"), null, ForgeGlyphs.Banner, guide = Guide.GUILD)
                if (Reads.GUILD in game.loading) {
                    MutedText(ui("guild.loading"))
                } else {
                    ForgeOutlinedButton(enabled = !game.busy, onClick = vm::load, modifier = Modifier.fillMaxWidth()) { Text(ui("auction.check_again")) }
                }
            }
        }
    }
}

/** Outside a guild: the wait after leaving, the invitations, the founding and the list of guilds to knock at. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.GuildOutside(game: GameUi, vm: GuildViewModel, mine: GuildMine) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    var founding by remember { mutableStateOf(false) }
    LaunchedEffect(game.heroId) { vm.search(0) }
    val waiting = mine.rejoinAt?.takeIf { it > System.currentTimeMillis() }
    PullToRefreshBox(
        isRefreshing = Reads.GUILD in game.loading || Reads.GUILD_SEARCH in game.loading,
        onRefresh = {
            vm.load()
            vm.search(guilds.search.page)
        },
        modifier = Modifier.weight(1f),
    ) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            waiting?.let { item { InfoCard(ui("guild.rejoin_title"), ui("guild.rejoin_text", clockText(it))) } }
            if (mine.invites.isNotEmpty()) {
                item {
                    ForgePanel(accent = Rune) {
                        Engraved(ui("guild.invites", mine.invites.size), Rune)
                        mine.invites.forEach { InviteRow(game, it, vm) }
                    }
                }
            }
            item {
                ForgePanel {
                    Engraved(ui("guild.found_title"))
                    MutedText(ui("guild.found_note"))
                    ForgeButton(enabled = !game.busy, onClick = { founding = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("guild.found")) }
                }
            }
            item { SearchField(game, vm) }
            game.index?.guilds?.takeIf { it.factions.isNotEmpty() }?.let { rules -> item { FactionFilter(guilds.faction, rules, !game.busy, vm::filterFaction) } }
            val page = guilds.search
            if (page.items.isEmpty() && Reads.GUILD_SEARCH !in game.loading) item { InfoCard(ui("guild.none_found"), ui("guild.none_found_hint")) }
            items(page.items, key = { it.id }) { card -> GuildCardRow(game, card, blocked = joinBlock(game, card, waiting != null)) { vm.join(card) } }
            if (page.totalPages > 1) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForgeOutlinedButton(enabled = page.page > 0, onClick = { vm.search(page.page - 1) }) { Text(ui("guild.back_page")) }
                        MutedText(ui("auction.page", page.page + 1, page.totalPages), modifier = Modifier.weight(1f))
                        ForgeOutlinedButton(enabled = page.page + 1 < page.totalPages, onClick = { vm.search(page.page + 1) }) { Text(ui("auction.forward")) }
                    }
                }
            }
        }
    }
    if (founding) {
        FoundingSheet(game, onDismiss = { founding = false }) { name, tag, faction, emblem, color, mode, minLevel ->
            founding = false
            vm.create(name, tag, faction, emblem, color, mode, minLevel)
        }
    }
}

@Composable private fun SearchField(game: GameUi, vm: GuildViewModel) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    OutlinedTextField(
        guilds.query, { vm.query(it.take(game.inputs.search)) }, label = { Text(ui("guild.search")) }, singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.search(0) }),
        trailingIcon = { ForgeTextButton(onClick = { vm.search(0) }) { Text(ui("guild.find")) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The list's faction chips (3.28.0): all, then each faction under its sign and name in its colour. */
@Composable private fun FactionFilter(chosen: String, rules: GuildRules, enabled: Boolean, onChoose: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(selected = chosen.isBlank(), enabled = enabled, onClick = { onChoose("") }, label = { Text(ui("common.all")) }) }
        items(rules.factions, key = { it.code }) { faction ->
            FilterChip(
                selected = chosen == faction.code,
                enabled = enabled,
                onClick = { onChoose(faction.code) },
                leadingIcon = { FactionIcon(faction.code, rules, Modifier.size(18.dp)) },
                label = { Text(GuildText.faction(faction.code), color = factionColor(faction)) },
            )
        }
    }
}

/** Why this hero cannot get into [card] now, or null when the button may be pressed. */
private fun joinBlock(game: GameUi, card: GuildCard, waiting: Boolean): String? = when {
    waiting -> ui("guild.block_rejoin")
    card.full -> ui("guild.block_full")
    game.heroLevel < card.minLevel -> ui("guild.block_level", card.minLevel)
    card.mode == GuildMode.INVITE -> ui("guild.block_invite")
    else -> null
}

/** One guild of the list: arms, name, faction and level, the roll and the way in; the button asks or joins by the guild's mode. */
@Composable private fun GuildCardRow(game: GameUi, card: GuildCard, blocked: String?, onJoin: () -> Unit) {
    ForgePanel(accent = guildColor(card.color)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GuildEmblem(card.emblem, card.color)
            Column(Modifier.weight(1f)) {
                Text(GuildText.title(card.name, card.tag), color = GoldBright, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FactionLine(card.faction, game.index?.guilds, ui("guild.card_line", card.level))
                MutedText(ui("guild.card_roll", card.members, card.capacity, GuildText.mode(card.mode), card.minLevel))
            }
        }
        if (blocked != null) {
            MutedText(blocked)
        } else {
            ForgeOutlinedButton(enabled = !game.busy, onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                Text(if (card.mode == GuildMode.OPEN) ui("guild.join") else ui("guild.apply"))
            }
        }
    }
}

/** An invitation: whose guild, who sent it, and the two answers. */
@Composable private fun InviteRow(game: GameUi, invite: GuildInviteView, vm: GuildViewModel) {
    val card = invite.guild
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GuildEmblem(card.emblem, card.color, 36.dp)
        Column(Modifier.weight(1f)) {
            Text(GuildText.title(card.name, card.tag), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            FactionLine(
                card.faction,
                game.index?.guilds,
                listOfNotNull(
                    ui("guild.card_line", card.level),
                    invite.by.takeIf { it.isNotBlank() }?.let { ui("guild.invite_from", it) },
                ).joinToString(" · "),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeOutlinedButton(enabled = !game.busy, onClick = { vm.declineInvite(card.id) }, modifier = Modifier.weight(1f)) { Text(ui("guild.decline")) }
        ForgeButton(enabled = !game.busy, onClick = { vm.acceptInvite(card.id) }, modifier = Modifier.weight(1f)) { Text(ui("guild.accept")) }
    }
}
