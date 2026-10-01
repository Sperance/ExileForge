package com.sperance.exileforge.ui.screens.expedition

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.RushPlan
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.screens.hero.compactCount
import com.sperance.exileforge.ui.theme.*

/**
 * «Испытания» (3.49.0): the tab of the expedition beside the world map. The keys at hand — crest fragments, the rush keys
 * forged of them (3.50.0) and tower seals — the tower with its record and the floor the next entry starts at, and every region: the rush of a region is
 * open once each of its zones is cleared. A trial the app lost mid-fight is ended here, what it brought kept.
 */
@Composable fun TrialsBoard(s: ForgeState, vm: ForgeViewModel, modifier: Modifier = Modifier) {
    val index = s.index ?: return
    val rules = index.campaign.trials ?: run { Box(modifier.padding(16.dp)) { InfoCard(ui("trials.title"), ui("trials.none")) }; return }
    val hero = s.hero ?: return
    val trials = hero.campaign.trials
    val crests = hero.bag[TrialRules.CREST] ?: 0L
    val keys = hero.bag[TrialRules.KEY] ?: 0L
    val seals = hero.bag[TrialRules.SEAL] ?: 0L
    val idle = !s.busy
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeyGrid(s, listOf(TrialRules.CREST to crests, TrialRules.KEY to keys, TrialRules.SEAL to seals))
        trials.run?.let { open ->
            Plate(LifeRed) {
                Text(ui("trials.open_title"), color = LifeRed, style = MaterialTheme.typography.titleSmall)
                MutedText(ui(if (open.kind == TrialKind.RUSH) "trials.open_rush" else "trials.open_tower", if (open.kind == TrialKind.RUSH) regionTitle(open.region) else open.floor))
                ForgeOutlinedButton(onClick = vm::abandonTrial, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(ui("trials.abandon")) }
            }
        }
        val tower = rules.tower
        Plate(Rune) {
            Text(ui("trials.tower_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("trials.tower_hint", tower.growth.toInt(), tower.hoardEvery, tower.modEvery, tower.checkpoint))
            // Conquered (3.71.0): past the last floor there is nothing to enter, and no seal is spent on it
            val conquered = tower.start(trials.towerBest) > tower.maxFloor
            if (conquered) Text(ui("trials.tower_conquered", tower.maxFloor), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
            else {
                Text(ui("trials.tower_record", trials.towerBest, tower.start(trials.towerBest)), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                tower.mods(tower.start(trials.towerBest) + tower.modEvery - 1).takeIf { it.isNotEmpty() }?.let { mods ->
                    mods.forEach { Text(SkillText.statLine(it.stat, it.op, it.value), color = LifeRed, style = MaterialTheme.typography.labelSmall) }
                }
            }
            ForgeButton(onClick = vm::enterTower, enabled = idle && seals >= 1 && trials.run == null && !conquered, modifier = Modifier.fillMaxWidth()) {
                Text(ui("trials.tower_enter", itemTitle(TrialRules.SEAL)))
            }
        }
        Plate(GoldBright) {
            Text(ui("trials.rush_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("trials.rush_hint", rules.rush.key, rules.rush.life.toInt(), rules.rush.seconds.toInt()))
            ForgeOutlinedButton(onClick = vm::forgeRushKey, enabled = idle && crests >= rules.rush.key, modifier = Modifier.fillMaxWidth()) {
                Text(ui("trials.key_forge", rules.rush.key))
            }
        }
        val cleared = hero.campaign.cleared
        // A rush still locked is not shown (3.67.0): the board lists only what can be run, or says when one opens.
        val unlocked = index.campaign.regions.filter { RushPlan.open(it, cleared) }
        if (unlocked.isEmpty()) MutedText(ui("trials.rush_none"))
        unlocked.forEach { region ->
            val best = trials.rushBest[region.code]
            Plate(Gold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(regionTitle(region.code), color = Parchment, style = MaterialTheme.typography.titleSmall)
                        MutedText(listOfNotNull(ui("trials.rush_bosses", region.zones.size),
                            best?.let { ui("trials.rush_best", clock(it.toDouble())) },
                            ui("trials.rush_cleared").takeIf { region.code in trials.rushCleared }).joinToString(" · "))
                    }
                    ForgeOutlinedButton(onClick = { vm.enterRush(region.code) }, enabled = idle && keys >= 1 && trials.run == null) {
                        Text(ui("trials.rush_enter"))
                    }
                }
            }
        }
    }
}

/** The keys at hand as a compact grid: an icon and its count, [KEYS_PER_ROW] to a row, a tap opens the key's sheet. */
@Composable private fun KeyGrid(s: ForgeState, stacks: List<Pair<String, Long>>) {
    var info by remember { mutableStateOf<String?>(null) }
    info?.let { code -> StackInfoSheet(s, code) { info = null } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stacks.chunked(KEYS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (code, count) -> Key(code, count, Modifier.weight(1f)) { info = code } }
                repeat(KEYS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable private fun Key(code: String, count: Long, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clickable(onClickLabel = itemTitle(code), onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BagIcon(code, Modifier.size(22.dp))
            Text(compactCount(count), color = Parchment, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private const val KEYS_PER_ROW = 4

@Composable private fun Plate(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, accent.copy(alpha = .45f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}
