package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import kotlin.math.ceil

/**
 * Under the hero: before «В бой» the call to fight and the way back; once it runs, pause and go on,
 * the speed and, on the pause (3.95.0), «Сдаться». Under an autorun (3.77.0) its wave reads above the row and its stop joins it, so
 * nothing floats over the speed.
 */
@Composable internal fun Controls(fight: FightHud, auto: AutoHud?, onCommand: (RunCommand) -> Unit) {
    val live = fight.outcome == null
    fight.interlude?.takeIf { !fight.started }?.let {
        StageBreak(fight, it, onCommand)
        return
    }
    if (!fight.started) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeButton(
                onClick = { onCommand(RunCommand.Begin) },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright),
            ) {
                Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui("expedition.begin"), style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }
    auto?.let {
        Text(
            ui("auto.wave", it.wave, it.waves),
            color = GoldBright,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeOutlinedButton(
            enabled = live,
            onClick = { onCommand(if (fight.paused) RunCommand.Begin else RunCommand.Pause) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(ui(if (fight.paused) "fight.resume" else "fight.pause"), style = MaterialTheme.typography.labelMedium)
        }
        ForgeOutlinedButton(onClick = { onCommand(RunCommand.Speed) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(ui("expedition.speed", fight.speed), style = MaterialTheme.typography.labelMedium)
        }
        if (auto != null) {
            ForgeOutlinedButton(onClick = { onCommand(RunCommand.StopAuto) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(ui("auto.stop"), color = LifeRed, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    // «Сдаться» (3.95.0): только на паузе, с подтверждением - бой, что сам не кончается, иначе не покинуть
    if (fight.paused && live) {
        var asking by remember { mutableStateOf(false) }
        ForgeTextButton(onClick = { asking = true }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("fight.surrender"), color = LifeRed, style = MaterialTheme.typography.labelMedium)
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

/**
 * The pause between the stages of a gathered fight (3.28.0): which stage stands up next, the seconds before it does
 * by itself and «Дальше» to begin at once; the belt above stays open for a draught. Отступления нет (3.88.8).
 */
@Composable private fun StageBreak(fight: FightHud, left: Double, onCommand: (RunCommand) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(ui("fight.stage", fight.stage, fight.stages), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(ui("fight.stage_countdown", ceil(left).toInt()), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeButton(
                onClick = { onCommand(RunCommand.Begin) },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright),
            ) {
                Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui("fight.stage_next"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
