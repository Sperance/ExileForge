package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.RunMenu
import com.sperance.exileforge.ui.components.RunMenuButton
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Under the hero: before «В бой» the call to fight and the way back; once it runs, pause and go on,
 * the speed and, on the pause, «Сдаться».
 *
 * Меню забега посреди боя - бой стоит, пока открыто. «Сдаться» - только на паузе, с подтверждением.
 */
@Composable internal fun Controls(fight: FightHud, onCommand: (RunCommand) -> Unit) {
    val live = fight.outcome == null
    if (!fight.started) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeButton(
                onClick = { onCommand(RunCommand.Begin) },
                modifier = Modifier.weight(1f),
                size = ButtonSize.Wide,
                icon = ForgeGlyphs.Swords,
                accent = Blood,
            ) {
                Text(ui("expedition.begin"), style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeButton(
            enabled = live,
            onClick = { onCommand(if (fight.paused) RunCommand.Begin else RunCommand.Pause) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            kind = ButtonKind.Secondary,
        ) {
            Text(ui(if (fight.paused) "fight.resume" else "fight.pause"), style = MaterialTheme.typography.labelMedium)
        }
        ForgeButton(onClick = { onCommand(RunCommand.Speed) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp), kind = ButtonKind.Secondary) {
            Text(ui("expedition.speed", fight.speed), style = MaterialTheme.typography.labelMedium)
        }
        RunMenu(
            items = emptyList(),
            hold = { ArenaHold(onCommand) },
            trigger = { onClick -> RunMenuButton(onClick = onClick) },
        )
    }
    if (fight.paused && live) {
        var asking by remember { mutableStateOf(false) }
        ForgeButton(onClick = { asking = true }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Danger, icon = Icons.Outlined.Flag) {
            Text(ui("fight.surrender"))
        }
        if (asking) {
            ConfirmSheet(
                title = ui("fight.surrender_q"),
                confirm = ui("fight.surrender"),
                danger = true,
                note = ui("fight.surrender_note"),
                onDismiss = { asking = false },
            ) {
                asking = false
                onCommand(RunCommand.Surrender)
            }
        }
    }
}

/** Держит бой арены, пока лист на экране. */
@Composable private fun ArenaHold(onCommand: (RunCommand) -> Unit) {
    DisposableEffect(Unit) {
        onCommand(RunCommand.Hold(true))
        onDispose { onCommand(RunCommand.Hold(false)) }
    }
}
