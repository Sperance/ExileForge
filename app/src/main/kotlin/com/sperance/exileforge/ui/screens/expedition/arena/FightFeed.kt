package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.CombatDetailSheet
import com.sperance.exileforge.ui.theme.*

/**
 * The live log: its shelves as chips over it, and a line tapped opens its card — the fight holds still while
 * it is read, and goes on as it was when the card is closed.
 *
 * Окно в центре арены - всегда журнал: до боя пустой, с приглашением коснуться врага ([FeedInvite]). Последние удары, пока бой идёт,
 * новые сверху, каждый под именем своего врага. Отбор полок - по новой строке или смене полок, не каждый
 * кадр.
 */
@Composable internal fun FightFeed(game: GameUi, fight: FightHud, names: Map<Int, String>, onCommand: (RunCommand) -> Unit, onLogFilter: (Set<LogKind>) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    var open by remember { mutableStateOf<CombatEvent?>(null) }
    var held by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Panel.copy(alpha = .75f), shape).border(1.dp, Bronze.copy(alpha = .4f), shape).padding(horizontal = 10.dp, vertical = 6.dp)) {
        LogShelves(game.logFilter, onLogFilter)
        if (fight.events.isEmpty() && !fight.started) {
            FeedInvite()
        } else {
            if (fight.events.isEmpty()) MutedText(ui("expedition.log"), style = MaterialTheme.typography.labelSmall)
            val shown = remember(fight.events, game.logFilter) { fight.events.filter { LogKind.of(it) in game.logFilter } }
            FightLog(shown, names) { event ->
                if (fight.started && !fight.paused && fight.outcome == null) {
                    onCommand(RunCommand.Pause)
                    held = true
                }
                open = event
            }
        }
    }
    open?.let { event ->
        CombatDetailSheet(game, event, names[event.foe].orEmpty()) {
            open = null
            if (held) {
                onCommand(RunCommand.Pause)
                held = false
            }
        }
    }
}

/** Пустой журнал до боя: знак касания и «Коснитесь врага, чтобы узнать о нём» посреди окна - разведка в листе врага. */
@Composable private fun ColumnScope.FeedInvite() {
    Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.TouchApp, null, tint = Muted, modifier = Modifier.size(28.dp))
        Text(ui("fight.feed_invite"), color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/** The log's shelves as chips: each on or off, the choice kept on the device. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogShelves(shown: Set<LogKind>, onChange: (Set<LogKind>) -> Unit) {
    ChoiceChipRow(LogKind.entries.map { ChipChoice(it, ui("fight.log_shelf.${it.name}")) }, { it in shown }, { kind -> onChange(if (kind in shown) shown - kind else shown + kind) }, mode = ChipMode.Filter)
}
