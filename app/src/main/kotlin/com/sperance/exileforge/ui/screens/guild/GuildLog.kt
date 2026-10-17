package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * The guild's journal, newest first, a page at a time.
 *
 * Ключи по содержимому: свежие строки сверху не сдвигают ключи прежних. Старшие страницы дочитываются прокруткой: последняя строка
 * у края - следующая страница, пока сервер говорит, что старше есть (`logEnd`); кнопки «Ещё» нет, внизу во время чтения - кольцо.
 */
@Composable internal fun LogTab(game: GameUi, vm: GuildViewModel) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId) { vm.loadLog() }
    val log = guilds.log
    val keys = remember(log) { contentKeys(log) { "${it.at}|${it.kind}|${it.heroName}|${it.value}" } }
    val list = rememberLazyListState()
    val loading = Reads.GUILD_LOG in game.loading
    val nearEnd by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= list.layoutInfo.totalItemsCount - LOAD_AHEAD } == true } }
    LaunchedEffect(nearEnd, log.size, guilds.logEnd, loading) { if (nearEnd && log.isNotEmpty() && !guilds.logEnd && !loading) vm.loadLog(more = true) }
    LazyColumn(Modifier.fillMaxSize(), state = list, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (log.isEmpty() && !loading) item { InfoCard(ui("guild.log_empty"), ui("guild.log_empty_hint")) }
        items(log.size, key = { keys[it] }) { i ->
            val entry = log[i]
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutedText(clockText(entry.at), modifier = Modifier.width(78.dp))
                Text(GuildText.log(entry), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
        if (log.isNotEmpty() && !guilds.logEnd) {
            item(key = "more") {
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Gold, strokeWidth = 2.dp)
                }
            }
        }
    }
}

/** За сколько строк до конца журнала читается следующая страница. */
private const val LOAD_AHEAD = 3
