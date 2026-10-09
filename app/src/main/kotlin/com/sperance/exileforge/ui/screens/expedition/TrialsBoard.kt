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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Region
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.content.RushPlan
import com.sperance.exileforge.rules.content.RushTier
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.screens.hero.compactCount
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Испытания (4.0.0): страница «Похода» - доска читает героя, как прежде на странице «Развития». */
@Composable fun TrialsPage() {
    val vm = koinViewModel<ExpeditionViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    LaunchedEffect(game.heroId, game.sessionEpoch) { heroModel.ensure() }
    TrialsBoard(game, vm, Modifier.fillMaxSize())
}

/**
 * «Испытания» (3.49.0; 4.0.0 - страница «Похода», макет «Скрижали»): ключи под рукой, незавершённое испытание и три режима
 * плитами [TrialMode] - Разлом недели, Башня и Босс-раш; плита раскрывается касанием, внутри - вход и подробности.
 * Тестировщику (4.0.0) замки прогресса не мешают: лига Разлома, зачистка региона и ступень раша открыты.
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
    val free = game.isTester
    val rift by vm.riftState.collectAsState()
    var opened by rememberSaveable { mutableStateOf<TrialMode?>(null) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeyGrid(listOf(TrialRules.CREST to crests, TrialRules.KEY to keys, TrialRules.SEAL to seals, RiftRules.KEY to (hero.bag[RiftRules.KEY] ?: 0L)))
        trials.run?.let { open ->
            Plate(LifeRed) {
                Text(ui("trials.open_title"), color = LifeRed, style = MaterialTheme.typography.titleSmall)
                MutedText(ui(if (open.kind == TrialKind.RUSH) "trials.open_rush" else "trials.open_tower", if (open.kind == TrialKind.RUSH) regionTitle(open.region) else open.floor))
                ForgeOutlinedButton(onClick = vm::abandonTrial, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(ui("trials.abandon")) }
            }
        }
        val toggle = { mode: TrialMode -> opened = mode.takeIf { it != opened } }
        // Разлом недели (3.96.0): своя доска на весь экран
        rules.rift?.let { riftRules ->
            val left = rift.board?.free
            TrialModeCard(TrialMode.RIFT, ui("trials.mode_rift_note"), left?.let { ui("trials.chip_free", it, riftRules.free) }, opened == TrialMode.RIFT, 0f, { toggle(TrialMode.RIFT) }) {
                MutedText(ui("rift.hint", riftRules.acts, riftRules.layout.rows, riftRules.free))
                val league = riftRules.league(hero.level) ?: 0.takeIf { free }
                ForgeButton(onClick = vm::openRift, enabled = idle && league != null, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.open")) }
                if (league == null) Text(ui("rift.league_none", riftRules.leagues.first()), color = LifeRed, style = MaterialTheme.typography.labelSmall)
            }
        }
        val tower = rules.tower
        // Покорена (3.71.0): после последнего этажа входить некуда, печать не тратится
        val conquered = tower.start(trials.towerBest) > tower.maxFloor
        val towerNote = if (conquered) ui("trials.tower_conquered", tower.maxFloor) else ui("trials.tower_record", trials.towerBest, tower.start(trials.towerBest))
        TrialModeCard(TrialMode.TOWER, towerNote, ui("trials.chip_seals", seals), opened == TrialMode.TOWER, 1f / 3, { toggle(TrialMode.TOWER) }) {
            MutedText(
                ui(
                    "trials.tower_hint", tower.levelOffset, tower.levelStep, index.rules.fight.maxFoes, tower.life.toInt(), number(tower.flaskCharges),
                    tower.hoardEvery, tower.modEvery, tower.checkpoint,
                ),
            )
            if (!conquered) {
                // Строки десятков (3.96.0): выбранные героем, по ним идёт вход
                tower.mods(tower.start(trials.towerBest) + tower.modEvery - 1, trials.towerPicks).forEach {
                    Text(SkillText.statLine(it.stat, it.op, it.value), color = LifeRed, style = MaterialTheme.typography.labelSmall)
                }
            }
            ForgeButton(onClick = vm::enterTower, enabled = idle && seals >= 1 && trials.run == null && !conquered, modifier = Modifier.fillMaxWidth()) {
                Text(ui("trials.tower_enter", itemTitle(TrialRules.SEAL)))
            }
        }
        val cleared = hero.campaign.cleared
        // Раш, ещё закрытый, не показывается (3.67.0); тестировщику открыты все регионы
        val unlocked = index.campaign.regions.filter { free || RushPlan.open(it, cleared) }
        TrialModeCard(TrialMode.RUSH, ui("trials.mode_rush_note"), ui("trials.chip_keys", keys), opened == TrialMode.RUSH, 2f / 3, { toggle(TrialMode.RUSH) }) {
            MutedText(ui("trials.rush_hint", rules.rush.key, rules.rush.life.toInt(), rules.rush.seconds.toInt()))
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
            unlocked.forEach { region -> RushRegion(game, vm, rules, region, keys, free) }
        }
    }
}

/** Регион раша: ступени, лучшее время и вход; тестировщику (4.0.0) открыты все ступени. */
@Composable private fun RushRegion(game: GameUi, vm: ExpeditionViewModel, rules: TrialRules, region: Region, keys: Long, free: Boolean) {
    val trials = game.hero?.campaign?.trials ?: return
    val idle = !game.busy
    val best = trials.rushBest[region.code]
    // Ступени (3.96.0): открыта следующая за зачищенными. Выбранная по умолчанию (4.2.0) - низшая непройденная, и у
    // тестировщика тоже: ему открыты все, но начинает он не с высшей
    val lowest = (trials.rushTiers[region.code] ?: 0).coerceAtMost(rules.rush.tierCount - 1)
    val open = if (free) rules.rush.tierCount - 1 else lowest
    var tier by remember(region.code, lowest) { mutableStateOf(lowest) }
    Plate(Gold) {
        if (rules.rush.tierCount > 1) {
            MutedText(ui("trials.rush_tier_title"), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until rules.rush.tierCount).forEach { t ->
                    val unlockedTier = t <= open
                    val shape = RoundedCornerShape(8.dp)
                    val edge = when {
                        t == tier -> GoldBright
                        unlockedTier -> Gold.copy(alpha = .5f)
                        else -> Muted.copy(alpha = .3f)
                    }
                    Box(Modifier.border(1.dp, edge, shape).clickable(enabled = unlockedTier) { tier = t }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(roman(t + 1), color = if (unlockedTier) Parchment else Muted, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Text(rushTierText(rules.rush.tier(tier)), color = Parchment, style = MaterialTheme.typography.bodySmall)
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
                // Причина у неактивной кнопки (3.94.0)
                when {
                    trials.run != null -> MutedText(ui("trials.rush_busy"), style = MaterialTheme.typography.labelSmall)
                    keys < 1 -> MutedText(ui("trials.rush_no_key"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** Чем ступень раша тяжелее и щедрее (4.2.0), из её записи в контенте: «Боссы +X% здоровья и урона · +N строк · Сферы ×R». */
private fun rushTierText(tier: RushTier): String = listOfNotNull(
    ui("trials.rush_tier_power", number(tier.power)).takeIf { tier.power > 0 },
    ui("trials.rush_tier_mods", tier.mods, plural("trials.rush_tier_line", tier.mods)).takeIf { tier.mods > 0 },
).ifEmpty { listOf(ui("trials.rush_tier_plain")) }.plus(ui("trials.rush_tier_reward", number(tier.reward))).joinToString(" · ")

/** The keys at hand as a compact grid: an icon and its count, [KEYS_PER_ROW] to a row, a tap opens the key's sheet. */
@Composable private fun KeyGrid(stacks: List<Pair<String, Long>>) {
    val inspect = rememberInspect()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stacks.chunked(KEYS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (code, count) -> Key(code, count, Modifier.weight(1f)) { inspect(Inspect.Stack(code)) } }
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
