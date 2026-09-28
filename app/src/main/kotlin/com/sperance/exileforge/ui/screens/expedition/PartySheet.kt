package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.party.MateView
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.party.PartyStatus
import com.sperance.exileforge.rules.party.PartyView
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The co-op lobby on the world map (3.25.0): a strip under the map's head while the hero is in one — its code,
 * who is in it, whether the link is up — and the sheet behind it, or behind «Кооператив» outside one: the way in
 * by a code, the guild's lobbies still gathering, and the lobby itself with its members.
 */
@Composable fun PartyStrip(s: ForgeState, vm: ForgeViewModel, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val party = s.play.party.view
    val shape = RoundedCornerShape(12.dp)
    Row(modifier.padding(horizontal = 12.dp).background(Abyss.copy(alpha = .9f), shape).border(1.dp, Gold.copy(alpha = .4f), shape)
        .clickable { open = true; vm.refreshParty() }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(ForgeGlyphs.Banner, null, tint = GoldBright, modifier = Modifier.size(18.dp))
        if (party == null) Text(ui("party.title"), color = GoldBright, style = MaterialTheme.typography.labelLarge)
        else {
            Text(party.code, color = GoldBright, style = MaterialTheme.typography.labelLarge)
            Text(ui("party.count", party.members.size, party.maxSize), color = Parchment, style = MaterialTheme.typography.labelMedium)
            Text(mapTitle(party.zone), color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            Box(Modifier.size(8.dp).background(if (s.play.party.online) Vital else LifeRed, CircleShape))
        }
    }
    if (open) PartySheet(s, vm) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PartySheet(s: ForgeState, vm: ForgeViewModel, onDismiss: () -> Unit) {
    val party = s.play.party.view
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Engraved(ui("party.title"))
            if (party != null) Lobby(s, vm, party) else {
                MutedText(ui("party.hint"))
                JoinByCode(s, vm)
                GuildLobbies(s, vm)
            }
        }
    }
}

/** The lobby the hero is in: its code to pass on, its zone and map, the members — the host can send a guest away — and the way out. */
@Composable private fun Lobby(s: ForgeState, vm: ForgeViewModel, party: PartyView) {
    val host = party.isHost(s.play.heroId)
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("party.code"), color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(party.code, color = GoldBright, style = MaterialTheme.typography.headlineSmall)
        }
        Text(mapTitle(party.zone) + if (party.mapItem != null) " · " + ui("party.with_map") else "", color = Parchment, style = MaterialTheme.typography.bodyMedium)
        MutedText(ui(when {
            party.status == PartyStatus.RUNNING -> "party.running"
            host -> "party.host_hint"
            else -> "party.guest_hint"
        }))
    }
    party.members.forEach { member ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(if (member.online) Vital else Muted, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(member.name + if (member.heroId == party.hostId) " · " + ui("party.host") else "", color = Parchment, style = MaterialTheme.typography.bodyMedium)
                MutedText(ui("party.member", classTitle(member.heroClass), member.level))
            }
            if (host && member.heroId != party.hostId) TextButton(onClick = { vm.kickFromParty(member.heroId) }, enabled = !s.busy) { Text(ui("party.kick"), color = LifeRed) }
        }
    }
    ForgeOutlinedButton(onClick = vm::leaveParty, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) {
        Text(ui(if (host) "party.disband" else "party.leave"))
    }
}

@Composable private fun JoinByCode(s: ForgeState, vm: ForgeViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(code, { code = it.filter(Char::isLetterOrDigit).take(CODE_LENGTH).uppercase() }, label = { Text(ui("party.code")) }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), modifier = Modifier.weight(1f))
        ForgeButton(onClick = { vm.joinParty(code) }, enabled = code.length == CODE_LENGTH && !s.busy, modifier = Modifier.height(52.dp)) { Text(ui("party.join")) }
    }
}

/** The guild's lobbies still gathering: a guildmate's party is joined in one tap. */
@Composable private fun GuildLobbies(s: ForgeState, vm: ForgeViewModel) {
    LaunchedEffect(Unit) { vm.loadGuildParties() }
    val lobbies = s.play.party.guild
    Engraved(ui("party.guild"))
    if (lobbies.isEmpty()) MutedText(ui("party.guild_none"))
    lobbies.forEach { lobby ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(lobby.host?.name.orEmpty() + " · " + mapTitle(lobby.zone), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                MutedText(ui("party.count", lobby.members.size, lobby.maxSize))
            }
            ForgeOutlinedButton(onClick = { vm.joinParty(lobby.code) }, enabled = !s.busy) { Text(ui("party.join")) }
        }
    }
}

/**
 * The lobby on a zone's card: outside one, the host gathers it here with the picked map; the host of this
 * zone's lobby sets out with «В путь» once somebody has come, and a guest waits for them.
 */
@Composable fun ZoneParty(s: ForgeState, vm: ForgeViewModel, zone: String, picked: String?) {
    val party = s.play.party.view
    when {
        party == null -> ForgeOutlinedButton(onClick = { vm.createParty(zone, picked) }, enabled = s.hero != null && !s.busy, modifier = Modifier.fillMaxWidth()) {
            Icon(ForgeGlyphs.Banner, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(ui("party.create"))
        }
        party.zone != zone -> MutedText(ui("party.elsewhere", mapTitle(party.zone)))
        party.isHost(s.play.heroId) -> MutedText(ui(if (party.members.size < 2) "party.waiting_guests" else "party.ready_to_go", party.code))
        else -> MutedText(ui("party.waiting_host"))
    }
}

/** The party over the map and the fight (3.25.0): each hero's name and life, the fallen greyed. */
@Composable fun PartyBar(party: List<MateView>, modifier: Modifier = Modifier) {
    if (party.size < 2) return
    Column(modifier.background(Abyss.copy(alpha = .8f), RoundedCornerShape(10.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        party.forEach { mate ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(mate.name, color = if (mate.alive) Parchment else Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 96.dp))
                val share = if (mate.maxLife > 0) (mate.life.toFloat() / mate.maxLife).coerceIn(0f, 1f) else 0f
                Box(Modifier.size(64.dp, 6.dp).background(Ink, RoundedCornerShape(3.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(share).background(if (mate.alive) LifeRed else Muted, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

private const val CODE_LENGTH = 6
