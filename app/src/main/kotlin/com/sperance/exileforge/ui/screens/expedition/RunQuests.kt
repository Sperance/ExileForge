package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.core.quests.live
import com.sperance.exileforge.core.quests.running
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.QuestCounter
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.screens.quests.QuestRow

/**
 * Доска заданий захода [run]: снятая на входе [snapshot]; без неё (доска ещё не была прочитана) - первая, что пришла при
 * этом экране, и дальше она не меняется: перечитанная доска уже несёт события захода.
 *
 * Ключ - «доска прочитана»: первая прочитанная запоминается, перечитывания ключа не меняют.
 */
@Composable internal fun rememberRunBoard(run: ExpeditionRun, snapshot: Quests?, live: Quests): Quests {
    val first = remember(run, live.board != null) { live }
    return snapshot ?: first
}

/** Лист «Задания» на карте: несданные задания со счётом этого захода в зоне [zone] (в Ваал-зоне - зона захода); боевые - сверху. */
@Composable internal fun RunQuestsSheet(quests: Quests, tally: Map<String, Long>, zone: MapCode, onDismiss: () -> Unit) {
    val shown = remember(quests, tally, zone) {
        quests.running().filterNot { it.claimed }.map { it.live(tally, zone) }.sortedBy { it.counter !in QuestCounter.COMBAT }
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
