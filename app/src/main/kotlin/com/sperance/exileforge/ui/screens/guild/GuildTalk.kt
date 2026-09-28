package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildMessage
import com.sperance.exileforge.core.model.guild.manages
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay

/** How often the chat asks for what came while its tab is open. */
private const val CHAT_POLL_MS = 5_000L

/** The guild's journal, newest first, a page at a time. */
@Composable internal fun LogTab(s: ForgeState, vm: ForgeViewModel) {
    LaunchedEffect(s.play.heroId) { vm.loadGuildLog() }
    val log = s.guild.log
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (log.isEmpty() && Reads.GUILD_LOG !in s.loading) item { InfoCard(ui("guild.log_empty"), ui("guild.log_empty_hint")) }
        items(log) { entry ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutedText(clockText(entry.at), modifier = Modifier.width(78.dp))
                Text(GuildText.log(entry), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
        if (log.isNotEmpty() && !s.guild.logEnd) item {
            ForgeOutlinedButton(enabled = Reads.GUILD_LOG !in s.loading, onClick = { vm.loadGuildLog(more = true) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("guild.log_more"))
            }
        }
    }
}

/**
 * The guild's chat: the kept history, then a poll every few seconds by the last message's moment while the tab is open.
 * A hero speaks once in a few seconds — the button counts the wait down; the leader and the officers take a message
 * away by holding it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun ChatTab(s: ForgeState, vm: ForgeViewModel, me: GuildMember?) {
    // The poll lives while the app is in sight: a chat in the background asks nothing.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(s.play.heroId, lifecycle) {
        vm.pollGuildChat(fresh = true)
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { delay(CHAT_POLL_MS); vm.pollGuildChat() } }
    }
    val chat = s.guild.chat
    val limit = s.index?.guilds?.chat?.length ?: 200
    val list = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<GuildMessage?>(null) }
    LaunchedEffect(chat.lastOrNull()?.id) { if (chat.isNotEmpty()) list.animateScrollToItem(chat.lastIndex) }
    val wait = quietSeconds(s.guild.chatQuietUntil)
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            if (chat.isEmpty()) item { MutedText(ui("guild.chat_empty")) }
            items(chat, key = { it.id }) { message ->
                val mine = message.heroId == me?.heroId
                Column(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = if (me?.role?.manages == true) ({ removing = message }) else null)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message.heroName, color = if (mine) GoldBright else Rune, style = MaterialTheme.typography.labelLarge)
                        MutedText(clockText(message.at))
                    }
                    Text(message.text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(draft, { draft = it.take(limit) }, placeholder = { Text(ui("guild.chat_hint")) }, maxLines = 3,
                supportingText = { Text(if (wait > 0) ui("guild.chat_wait", wait) else "${draft.length}/$limit") }, modifier = Modifier.weight(1f))
            IconButton(enabled = !s.busy && wait == 0L && draft.isNotBlank(), onClick = { vm.sayInGuild(draft); draft = "" }) {
                Icon(Icons.AutoMirrored.Outlined.Send, ui("guild.chat_send"), tint = if (wait == 0L && draft.isNotBlank()) Gold else Muted)
            }
        }
    }
    removing?.let { message ->
        ConfirmSheet(title = ui("guild.chat_delete_q"), confirm = ui("guild.chat_delete"), onDismiss = { removing = null },
            subtitle = message.heroName, note = message.text, danger = true, blocked = s.busy) { vm.unsayInGuild(message.id) }
    }
}

/** Whole seconds until the hero may speak again, counted down while there are any. */
@Composable private fun quietSeconds(until: Long): Long {
    val left by produceState(0L, until) {
        while (true) {
            value = ((until - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
            if (value == 0L) break
            delay(250)
        }
    }
    return left
}
