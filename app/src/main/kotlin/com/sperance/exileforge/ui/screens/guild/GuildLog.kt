package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

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
        if (log.isNotEmpty() && !s.guild.logEnd) {
            item {
                ForgeOutlinedButton(enabled = Reads.GUILD_LOG !in s.loading, onClick = { vm.loadGuildLog(more = true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("guild.log_more"))
                }
            }
        }
    }
}
