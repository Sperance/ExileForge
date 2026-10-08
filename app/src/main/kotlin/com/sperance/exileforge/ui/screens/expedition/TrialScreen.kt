package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.TrialHud
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * A trial over the whole screen (3.49.0): the arena's cards as in any fight, a plate on top with the boss or the floor, the
 * foes' level, the clock and the floor's lines, and — once it is over — what it came to. The arena is stepped here, a frame
 * at a time; back walks away between two fights, and closes the screen once the trial is over.
 */
@Composable fun TrialScreen(arena: TrialArena) {
    val game by koinViewModel<ExpeditionViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val model = koinViewModel<ExpeditionViewModel>()
    val hud by arena.hud.collectAsState()
    LaunchedEffect(arena) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) arena.update(((now - last) / 1e9).coerceAtMost(.05))
                last = now
            }
        }
    }
    BackHandler { if (hud.phase == TrialPhase.FIGHT) model.trialCommand(com.sperance.exileforge.core.campaign.run.RunCommand.Leave) else model.closeTrial() }
    Box(Modifier.fillMaxSize().background(Ink)) {
        hud.fight?.takeIf { hud.phase == TrialPhase.FIGHT }?.let { fight ->
            ArenaOverlay(game, hud.run, fight, hud.level, arena.rules, arena.stance, onCommand = model::trialCommand, onLogFilter = shell::logFilter, onBuzz = shell::buzz)
        }
        if (hud.phase == TrialPhase.FIGHT) {
            TrialPlate(hud, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 44.dp, end = 8.dp))
        } else {
            TrialEnding(game, model, hud)
        }
        // Строка угрозы десятка башни (3.96.0): выбор до первого боя десятка
        hud.choice?.takeIf { hud.phase == TrialPhase.FIGHT }?.let { options -> TowerChoice(hud.step, options) { model.trialCommand(com.sperance.exileforge.core.campaign.run.RunCommand.PickLine(it)) } }
        // Досье босса (3.92.0) перед его этапом, как у стража карты: «В бой» начинает этап
        val fight = hud.fight?.takeIf { hud.phase == TrialPhase.FIGHT && !it.started }
        val boss = fight?.boss
        val stage = hud.step to fight?.stage
        var seen by remember { mutableStateOf<Pair<Int, Int?>?>(null) }
        if (fight != null && boss != null && seen != stage) {
            fight.foes.firstOrNull { it.index == boss.index }?.let { foe ->
                BossDossier(
                    game,
                    foe.monster,
                    hud.level,
                    foe.maxLife.toDouble(),
                    boss.phase,
                    boss.marks,
                    ui(if (hud.kind == TrialKind.RUSH) "trials.boss_of" else "trials.floor", hud.step, hud.steps),
                    fight.heroBody,
                    arena.rules,
                    odds = { arena.bossOdds() },
                    record = { model.bossRecord(foe.monster.code.value) },
                ) {
                    seen = stage
                    model.trialCommand(com.sperance.exileforge.core.campaign.run.RunCommand.Begin)
                }
            }
        }
    }
}

/** Выбор строки угрозы десятка башни (3.96.0): варианты по силе, у каждого - прибавка к кладу. */
@Composable private fun TowerChoice(floor: Int, options: List<com.sperance.exileforge.rules.content.TowerMod>, onPick: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(ui("trials.tower_choice_title", floor), color = GoldBright, style = MaterialTheme.typography.titleLarge)
        MutedText(ui("trials.tower_choice_hint"))
        options.forEachIndexed { i, mod ->
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.fillMaxWidth().depthPanel(shape).border(1.dp, LifeRed.copy(alpha = .3f + .2f * i), shape).clickable { onPick(i) }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(SkillText.statLine(mod.stat, mod.op, mod.value), color = LifeRed, style = MaterialTheme.typography.titleSmall)
                Text(ui("trials.tower_choice_hoard", mod.hoard.toInt()), color = GoldBright, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** The trial's plate over the arena: which boss of how many or which floor, the clock against the rush's limit, the floor's lines. */
@Composable private fun TrialPlate(hud: TrialHud, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.widthIn(max = 180.dp).background(Panel.copy(alpha = .9f), shape).border(1.dp, Gold.copy(alpha = .4f), shape).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            if (hud.kind == TrialKind.RUSH) ui("trials.boss_of", hud.step, hud.steps) else ui("trials.floor", hud.step),
            color = GoldBright,
            style = MaterialTheme.typography.labelLarge,
        )
        if (hud.kind == TrialKind.RUSH && hud.tier > 0) Text(ui("trials.rush_tier", roman(hud.tier + 1)), color = Rune, style = MaterialTheme.typography.labelSmall)
        val time = clock(hud.elapsed)
        Text(
            if (hud.limit > 0) ui("trials.clock_limit", time, clock(hud.limit)) else time,
            color = if (hud.limit > 0 && hud.elapsed > hud.limit) Muted else Parchment,
            style = MaterialTheme.typography.labelSmall,
        )
        hud.mods.forEach { Text(SkillText.statLine(it.stat, it.op, it.value), color = LifeRed, style = MaterialTheme.typography.labelSmall) }
        if (hud.awaiting > 0) Receiving()
    }
}

/** The trial over — fallen, finished or left: how far it went, the clock, and what the server's answers brought all told. */
@Composable private fun TrialEnding(game: GameUi, model: ExpeditionViewModel, hud: TrialHud) {
    val expedition: ExpeditionViewModel = koinViewModel()
    var looked by remember(hud.gained) { mutableStateOf<ItemView?>(null) }
    val fallen = hud.phase == TrialPhase.DEAD
    Column(
        Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(ui(if (fallen) "trials.fallen" else "trials.done"), color = if (fallen) LifeRed else Vital, style = MaterialTheme.typography.headlineSmall)
        MutedText(
            if (hud.kind == TrialKind.RUSH) {
                ui("trials.rush_result", hud.cleared, hud.steps, clock(hud.elapsed))
            } else {
                ui("trials.tower_result", hud.cleared, clock(hud.elapsed))
            },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RunFigures(hud.summary)
            RewardLines(game, hud.gained, { looked = it }, awaiting = hud.awaiting > 0)
        }
        ForgeButton(onClick = model::closeTrial, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(ui("expedition.back_to_camp")) }
    }
    looked?.let { item -> LootSheet(game, model, item, onDismiss = { looked = null }) }
}

/** Seconds as a clock: `m:ss`, from an hour on `h:mm:ss`. */
internal fun clock(seconds: Double): String {
    val whole = seconds.toLong().coerceAtLeast(0)
    val (h, m, game) = Triple(whole / 3600, whole % 3600 / 60, whole % 60)
    return if (h > 0) "%d:%02d:%02d".format(h, m, game) else "%d:%02d".format(m, game)
}
