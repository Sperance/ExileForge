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
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unlocked
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
@Composable fun TrialsBoard(game: GameUi, vm: ExpeditionViewModel, modifier: Modifier = Modifier) {
    val index = game.index ?: return
    val rules = index.campaign.trials ?: run {
        Box(modifier.padding(16.dp)) { InfoCard(ui("trials.title"), ui("trials.none")) }
        return
    }
    val hero = game.hero ?: return
    val trials = hero.campaign.trials
    val crests = hero.bag[TrialRules.CREST] ?: 0L
    val keys = hero.bag[TrialRules.KEY] ?: 0L
    val seals = hero.bag[TrialRules.SEAL] ?: 0L
    val idle = !game.busy
    val rift by vm.riftState.collectAsState()
    rift.table?.takeIf { !rift.open }?.let { TrialTableSheet(it) { vm.trialTable(null) } }
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeyGrid(game, listOf(TrialRules.CREST to crests, TrialRules.KEY to keys, TrialRules.SEAL to seals, com.sperance.exileforge.rules.content.RiftRules.KEY to (hero.bag[com.sperance.exileforge.rules.content.RiftRules.KEY] ?: 0L)))
        // Разлом недели (3.96.0): своя доска на весь экран
        rules.rift?.let { riftRules ->
            RiftPlate(RiftColors.Rift) {
                Text(ui("rift.title"), color = RiftColors.Rift, style = MaterialTheme.typography.titleMedium)
                MutedText(ui("rift.hint", riftRules.acts, riftRules.layout.rows, riftRules.free))
                val league = riftRules.league(hero.level)
                ForgeButton(onClick = vm::openRift, enabled = idle && league != null, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.open")) }
                if (league == null) Text(ui("rift.league_none", riftRules.leagues.first()), color = LifeRed, style = MaterialTheme.typography.labelSmall)
            }
        }
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
            if (conquered) {
                Text(ui("trials.tower_conquered", tower.maxFloor), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(ui("trials.tower_record", trials.towerBest, tower.start(trials.towerBest)), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                // Строки десятков (3.96.0): выбранные героем, по ним идёт вход
                tower.mods(tower.start(trials.towerBest) + tower.modEvery - 1, trials.towerPicks).takeIf { it.isNotEmpty() }?.let { mods ->
                    mods.forEach { Text(SkillText.statLine(it.stat, it.op, it.value), color = LifeRed, style = MaterialTheme.typography.labelSmall) }
                }
            }
            ForgeButton(onClick = vm::enterTower, enabled = idle && seals >= 1 && trials.run == null && !conquered, modifier = Modifier.fillMaxWidth()) {
                Text(ui("trials.tower_enter", itemTitle(TrialRules.SEAL)))
            }
            ForgeOutlinedButton(onClick = { vm.trialTable(com.sperance.exileforge.rules.content.TrialBoard.TOWER) }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(ui("trials.table")) }
        }
        Plate(GoldBright) {
            Text(ui("trials.rush_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("trials.rush_hint", rules.rush.key, rules.rush.life.toInt(), rules.rush.seconds.toInt()))
            val cleared = hero.campaign.cleared
            val unlocked = index.campaign.regions.filter { RushPlan.open(it, cleared) }
            // Ковка (3.94.0): пока ни один раш не открыт, ключ ковать незачем - вместо кнопки ближайший регион и сколько зон осталось
            if (unlocked.isEmpty()) {
                index.campaign.regions.minByOrNull { region -> region.zones.count { it.code !in cleared } }?.let { near ->
                    Text(ui("trials.rush_locked", regionTitle(near.code), near.zones.count { it.code !in cleared }), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                ForgeOutlinedButton(onClick = vm::forgeRushKey, enabled = idle && crests >= rules.rush.key, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("trials.key_forge_count", minOf(crests, rules.rush.key.toLong()), rules.rush.key))
                }
                if (crests < rules.rush.key) MutedText(ui("trials.key_short", rules.rush.key - crests), style = MaterialTheme.typography.labelSmall)
            }
        }
        val cleared = hero.campaign.cleared
        // A rush still locked is not shown (3.67.0): the board lists only what can be run, or says when one opens.
        val unlocked = index.campaign.regions.filter { RushPlan.open(it, cleared) }
        unlocked.forEach { region ->
            val best = trials.rushBest[region.code]
            // Ступени (3.96.0): открыта следующая за зачищенными; выбранная - по умолчанию высшая открытая
            val open = (trials.rushTiers[region.code] ?: 0).coerceAtMost(rules.rush.tierCount - 1)
            var tier by remember(region.code, open) { mutableStateOf(open) }
            Plate(Gold) {
                if (rules.rush.tierCount > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until rules.rush.tierCount).forEach { t ->
                            val unlockedTier = t <= open
                            val shape = RoundedCornerShape(8.dp)
                            Box(
                                Modifier.border(
                                    1.dp,
                                    if (t == tier) {
                                        GoldBright
                                    } else if (unlockedTier) {
                                        Gold.copy(alpha = .5f)
                                    } else {
                                        Muted.copy(alpha = .3f)
                                    },
                                    shape,
                                )
                                    .clickable(enabled = unlockedTier) { tier = t }.padding(horizontal = 10.dp, vertical = 4.dp),
                            ) { Text(roman(t + 1), color = if (unlockedTier) Parchment else Muted, style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                    if (open < rules.rush.tierCount - 1) MutedText(ui("trials.rush_tier_next", roman(open + 2), roman(open + 1)), style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(regionTitle(region.code), color = Parchment, style = MaterialTheme.typography.titleSmall)
                        MutedText(
                            listOfNotNull(
                                ui("trials.rush_bosses", minOf(region.zones.size, rules.rush.bosses)),
                                best?.let { ui("trials.rush_best", clock(it.toDouble())) },
                                ui("trials.rush_cleared").takeIf { region.code in trials.rushCleared },
                            ).joinToString(" · "),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        ForgeOutlinedButton(onClick = { vm.enterRush(region.code, tier) }, enabled = idle && keys >= 1 && trials.run == null) {
                            Text(ui("trials.rush_enter"))
                        }
                        ForgeTextButton(onClick = { vm.trialTable(com.sperance.exileforge.rules.content.TrialBoard.RUSH, "${region.code}:$tier") }, enabled = idle) { Text(ui("trials.table")) }
                        // Причина у неактивной кнопки (3.94.0)
                        when {
                            trials.run != null -> MutedText(ui("trials.rush_busy"), style = MaterialTheme.typography.labelSmall)
                            keys < 1 -> MutedText(ui("trials.rush_no_key"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/** The keys at hand as a compact grid: an icon and its count, [KEYS_PER_ROW] to a row, a tap opens the key's sheet. */
@Composable private fun KeyGrid(game: GameUi, stacks: List<Pair<String, Long>>) {
    var info by remember { mutableStateOf<String?>(null) }
    info?.let { code -> StackInfoSheet(game, code) { info = null } }
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

/** Номер ступени раша римскими цифрами (3.96.0). */
internal fun roman(n: Int): String = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I").fold(n to "") { (left, out), (value, sign) ->
    (left % value) to (out + sign.repeat(left / value))
}.second

@Composable private fun Plate(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().depthPanel(shape).border(1.dp, accent.copy(alpha = .45f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
