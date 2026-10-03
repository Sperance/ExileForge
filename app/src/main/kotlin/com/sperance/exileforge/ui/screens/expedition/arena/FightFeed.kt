package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.CombatDetailSheet
import com.sperance.exileforge.ui.theme.*

/** The latest blows while the fight runs, newest on top, each under the name of the foe it was about. */

/**
 * The live log (3.37.0): its shelves as chips over it, and a line tapped opens its card — the fight holds still while
 * it is read, and goes on as it was when the card is closed.
 */
@Composable internal fun FightFeed(s: ForgeState, fight: FightHud, names: Map<Int, String>, onCommand: (RunCommand) -> Unit, onLogFilter: (Set<LogKind>) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    var open by remember { mutableStateOf<CombatEvent?>(null) }
    var held by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Panel.copy(alpha = .75f), shape).border(1.dp, Bronze.copy(alpha = .4f), shape).padding(horizontal = 10.dp, vertical = 6.dp)) {
        LogShelves(s.logFilter, onLogFilter)
        if (fight.events.isEmpty()) MutedText(ui("expedition.log"), style = MaterialTheme.typography.labelSmall)
        FightLog(fight.events.filter { LogKind.of(it) in s.logFilter }, names) { event ->
            if (fight.started && !fight.paused && fight.outcome == null) {
                onCommand(RunCommand.Pause)
                held = true
            }
            open = event
        }
    }
    open?.let { event ->
        CombatDetailSheet(s, event, names[event.foe].orEmpty()) {
            open = null
            if (held) {
                onCommand(RunCommand.Pause)
                held = false
            }
        }
    }
}

/** The log's shelves as chips (3.37.0): each on or off, the choice kept on the device. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogShelves(shown: Set<LogKind>, onChange: (Set<LogKind>) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LogKind.entries.forEach { kind ->
            val on = kind in shown
            FilterChip(
                selected = on,
                onClick = { onChange(if (on) shown - kind else shown + kind) },
                label = { Text(ui("fight.log_shelf.${kind.name}"), style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(26.dp),
            )
        }
    }
}
