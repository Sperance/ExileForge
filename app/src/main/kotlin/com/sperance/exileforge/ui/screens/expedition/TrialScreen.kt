package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.TrialHud
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * A trial over the whole screen (3.49.0): the arena's cards as in any fight, a plate on top with the boss or the floor, the
 * foes' level, the clock and the floor's lines, and — once it is over — what it came to. The arena is stepped here, a frame
 * at a time; back walks away between two fights, and closes the screen once the trial is over.
 */
@Composable fun TrialScreen(s: ForgeState, vm: ForgeViewModel, arena: TrialArena) {
    val hud by arena.hud.collectAsState()
    LaunchedEffect(arena) {
        var last = 0L
        while (true) withFrameNanos { now -> if (last != 0L) arena.update(((now - last) / 1e9).coerceAtMost(.05)); last = now }
    }
    BackHandler { if (hud.phase == TrialPhase.FIGHT) vm.trialCommand(com.sperance.exileforge.core.campaign.RunCommand.Leave) else vm.closeTrial() }
    Box(Modifier.fillMaxSize().background(Ink)) {
        hud.fight?.takeIf { hud.phase == TrialPhase.FIGHT }?.let { fight ->
            ArenaOverlay(s, hud.run, fight, hud.level, arena.rules, arena.stance, onCommand = vm::trialCommand, onLogFilter = vm::logFilter)
        }
        if (hud.phase == TrialPhase.FIGHT) TrialPlate(hud, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 44.dp, end = 8.dp))
        else TrialEnding(s, vm, hud)
    }
}

/** The trial's plate over the arena: which boss of how many or which floor, the clock against the rush's limit, the floor's lines. */
@Composable private fun TrialPlate(hud: TrialHud, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.widthIn(max = 180.dp).background(Panel.copy(alpha = .9f), shape).border(1.dp, Gold.copy(alpha = .4f), shape).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(if (hud.kind == TrialKind.RUSH) ui("trials.boss_of", hud.step, hud.steps) else ui("trials.floor", hud.step),
            color = GoldBright, style = MaterialTheme.typography.labelLarge)
        val time = clock(hud.elapsed)
        Text(if (hud.limit > 0) ui("trials.clock_limit", time, clock(hud.limit)) else time,
            color = if (hud.limit > 0 && hud.elapsed > hud.limit) Muted else Parchment, style = MaterialTheme.typography.labelSmall)
        hud.mods.forEach { Text(SkillText.statLine(it.stat, it.op, it.value), color = LifeRed, style = MaterialTheme.typography.labelSmall) }
        if (hud.awaiting > 0) Receiving()
    }
}

/** The trial over — fallen, finished or left: how far it went, the clock, and what the server's answers brought all told. */
@Composable private fun TrialEnding(s: ForgeState, vm: ForgeViewModel, hud: TrialHud) {
    var looked by remember(hud.gained) { mutableStateOf<ItemView?>(null) }
    val fallen = hud.phase == TrialPhase.DEAD
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(ui(if (fallen) "trials.fallen" else "trials.done"), color = if (fallen) LifeRed else Vital, style = MaterialTheme.typography.headlineSmall)
        MutedText(if (hud.kind == TrialKind.RUSH) ui("trials.rush_result", hud.cleared, hud.steps, clock(hud.elapsed))
            else ui("trials.tower_result", hud.cleared, clock(hud.elapsed)))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RunFigures(hud.summary)
            RewardLines(s, hud.gained, { looked = it }, awaiting = hud.awaiting > 0)
        }
        ForgeButton(onClick = vm::closeTrial, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(ui("expedition.back_to_camp")) }
    }
    looked?.let { item -> LootSheet(s, vm, item, onDismiss = { looked = null }) }
}

/** Seconds as `m:ss`. */
internal fun clock(seconds: Double): String {
    val whole = seconds.toLong().coerceAtLeast(0)
    return "${whole / 60}:${(whole % 60).toString().padStart(2, '0')}"
}
