package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestCondition
import com.sperance.exileforge.rules.content.QuestCounter
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.screens.quests.QuestRow
import com.sperance.exileforge.ui.screens.quests.questTitle

/**
 * Задание, каким оно будет, когда сервер примет события захода (3.95.0): боевой счётчик прибавляет [tally] этого захода;
 * «за один заход» считает только его. Прочие задания - как на доске.
 */
internal fun Quest.live(tally: Map<String, Long>): Quest {
    if (claimed || counter !in QuestCounter.COMBAT) return this
    val run = tally[counter] ?: 0L
    val now = if (QuestCondition.ONE_RUN in conditions) run else progress + run
    return copy(progress = now.coerceAtMost(target))
}

/** Задания героя, что двигает заход: личные и гильдейские, ещё не сданные. */
internal fun Quests.running(): List<Quest> = listOfNotNull(board?.story) + board?.contracts.orEmpty() + board?.daily.orEmpty() +
    board?.weekly.orEmpty() + guild?.personal.orEmpty()

/** Лист «Задания» на карте (3.95.0): несданные задания со счётом этого захода; боевые - сверху. */
@Composable internal fun RunQuestsSheet(quests: Quests, tally: Map<String, Long>, onDismiss: () -> Unit) {
    val shown = remember(quests, tally) {
        quests.running().filterNot { it.claimed }.map { it.live(tally) }.sortedBy { it.counter !in QuestCounter.COMBAT }
    }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.8f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("run.quests")) }
            item { MutedText(ui("run.quests_hint")) }
            if (shown.isEmpty()) item { MutedText(ui("run.quests_none")) }
            items(shown, key = { it.id }) { QuestRow(it) }
        }
    }
}

/**
 * Тост «Задание выполнено» по ходу захода (3.95.0): задание, что этот заход довёл до цели, называется раз; сдаётся оно, как
 * прежде, по концу захода.
 */
@Composable internal fun RunQuestWatch(quests: Quests, tally: Map<String, Long>, announce: (String) -> Unit) {
    val told = remember { HashSet<String>() }
    val before = remember(quests) { quests.running().filter { it.done }.mapTo(HashSet()) { it.id } }
    LaunchedEffect(quests, tally) {
        quests.running().filterNot { it.claimed || it.id in before || it.id in told }.map { it.live(tally) }.filter { it.done }.forEach {
            told += it.id
            announce(ui("run.quest_done", questTitle(it)))
        }
    }
}
