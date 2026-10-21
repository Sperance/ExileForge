package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.contentKeys
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * The guild's journal, newest first, a page at a time.
 *
 * Ключи по содержимому: свежие строки сверху не сдвигают ключи прежних. Старшие страницы дочитываются листанием ([PagedColumn]), пока
 * сервер говорит, что старше есть (`logEnd`); кнопки «Ещё» нет, внизу во время чтения - кольцо.
 */
@Composable internal fun LogTab(game: GameUi, vm: GuildViewModel) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId) { vm.loadLog() }
    val log = guilds.log
    val keys = remember(log) { contentKeys(log) { "${it.at}|${it.kind}|${it.heroName}|${it.value}" } }
    val loading = Reads.GUILD_LOG in game.loading
    PagedColumn(log.size, log.isNotEmpty() && !guilds.logEnd, loading, { vm.loadLog(more = true) }, Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (log.isEmpty() && !loading) item { InfoCard(ui("guild.log_empty"), ui("guild.log_empty_hint")) }
        items(log.size, key = { keys[it] }) { i ->
            val entry = log[i]
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutedText(clockText(entry.at), modifier = Modifier.width(78.dp))
                Text(GuildText.log(entry), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
    }
}
