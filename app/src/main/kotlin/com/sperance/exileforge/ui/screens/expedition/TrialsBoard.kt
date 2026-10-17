package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Region
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.content.RushPlan
import com.sperance.exileforge.rules.content.RushTier
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.hero.compactCount
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Испытания: страница «Похода» - доска читает героя сама. */
@Composable fun TrialsPage() {
    val vm = koinViewModel<ExpeditionViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    LaunchedEffect(game.heroId, game.sessionEpoch) { heroModel.ensure() }
    TrialsBoard(game, vm, Modifier.fillMaxSize())
}

/**
 * «Испытания» (страница «Похода»): ключи под рукой, незавершённое испытание и три режима
 * плитами [TrialMode] - Разлом недели, Башня и Босс-раш; плита раскрывается касанием, внутри - вход и подробности.
 * Тестировщику замки прогресса не мешают: вход в Разлом, зачистка региона и ступень раша открыты.
 *
 * Разлом недели - своя доска на весь экран. Покорённая башня печать не тратит. Закрытый раш не показывается (тестировщику открыты все
 * регионы); ключ не куётся, пока ни один раш не открыт; ближайший закрытый регион - один, с тем, что осталось зачистить.
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
        rules.rift?.let { riftRules ->
            val left = rift.board?.free
            TrialModeCard(TrialMode.RIFT, ui("trials.mode_rift_note"), left?.let { ui("trials.chip_free", it, riftRules.free) }, opened == TrialMode.RIFT, 0f, { toggle(TrialMode.RIFT) }) {
                MutedText(ui("rift.hint", riftRules.acts, riftRules.layout.rows, riftRules.free))
                val open = game.unlocked(Feature.RIFT)
                ForgeButton(onClick = vm::openRift, enabled = idle && open, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.open")) }
                if (!open) Text(ui("unlock.locked", ui(Feature.RIFT.title), Feature.RIFT.level(index.rules)), color = LifeRed, style = MaterialTheme.typography.labelSmall)
            }
        }
        val tower = rules.tower
        val conquered = tower.start(trials.towerBest) > tower.maxFloor
        val towerNote = if (conquered) ui("trials.tower_conquered", tower.maxFloor) else ui("trials.tower_record", trials.towerBest, tower.start(trials.towerBest))
        TrialModeCard(TrialMode.TOWER, towerNote, ui("trials.chip_seals", seals), opened == TrialMode.TOWER, 1f / 3, { toggle(TrialMode.TOWER) }) {
            MutedText(
                ui(
                    "trials.tower_hint", tower.firstLevel, tower.levelStep, index.rules.fight.maxFoes, tower.life.toInt(), number(tower.flaskCharges),
                    tower.hoardEvery, tower.modEvery, tower.checkpoint,
                ),
            )
            if (!conquered) {
                tower.mods(tower.start(trials.towerBest) + tower.modEvery - 1, trials.towerPicks).forEach {
                    Text(SkillText.statLine(it.stat, it.op, it.value), color = LifeRed, style = MaterialTheme.typography.labelSmall)
                }
            }
            ForgeButton(onClick = vm::enterTower, enabled = idle && seals >= 1 && trials.run == null && !conquered, modifier = Modifier.fillMaxWidth()) {
                Text(ui("trials.tower_enter", itemTitle(TrialRules.SEAL)))
            }
        }
        val cleared = hero.campaign.cleared
        val unlocked = index.campaign.regions.filter { free || RushPlan.open(it, cleared) }
        TrialModeCard(TrialMode.RUSH, ui("trials.mode_rush_note"), ui("trials.chip_keys", keys), opened == TrialMode.RUSH, 2f / 3, { toggle(TrialMode.RUSH) }) {
            MutedText(ui("trials.rush_hint", rules.rush.key, rules.rush.life.toInt(), rules.rush.seconds.toInt()))
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
            index.campaign.regions.filterNot { it in unlocked }.minByOrNull { region -> region.zones.count { it.code !in cleared } }?.let { near ->
                LockedRegion(near, near.zones.count { it.code !in cleared })
            }
        }
    }
}

/**
 * Регион раша: имя и «зачищен», боссы и лучшее время, лента ступеней с подписью «Сложность» - все пройденные
 * (зелёные) и ровно одна следующая (золотая, выбрана по умолчанию); дальше ступени не рисуются. Под лентой - цифры
 * выбранной ступени с подписями ([RushTierFacts]), её имя и «Войти» с числом ключей. Тестировщику (`ProgressGate`) лента показывает все ступени.
 *
 * Следующая ступень - первая непройденная, её и выбирают по умолчанию.
 */
@Composable private fun RushRegion(game: GameUi, vm: ExpeditionViewModel, rules: TrialRules, region: Region, keys: Long, free: Boolean) {
    val trials = game.hero?.campaign?.trials ?: return
    val idle = !game.busy
    val best = trials.rushBest[region.code]
    val count = rules.rush.tierCount
    val passed = (trials.rushTiers[region.code] ?: 0).coerceIn(0, count)
    val lowest = passed.coerceAtMost(count - 1)
    val shown = if (free) count else minOf(passed + 1, count)
    var tier by remember(region.code, lowest) { mutableStateOf(lowest) }
    val cleared = region.code in trials.rushCleared
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, if (cleared) Vital.copy(alpha = .3f) else Brass.copy(alpha = .3f), shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(regionTitle(region.code), color = GoldBright, fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (cleared) {
                Text(
                    "✓ " + ui("trials.rush_cleared"),
                    color = Vital,
                    fontSize = 10.sp,
                    modifier = Modifier.background(Vital.copy(alpha = .1f), TagShape).border(1.dp, Vital.copy(alpha = .35f), TagShape).padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
        MutedText(
            listOfNotNull(ui("trials.rush_bosses", minOf(region.zones.size, rules.rush.bosses)), best?.let { ui("trials.rush_best", clock(it.toDouble())) }).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
        )
        if (count > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MutedText(ui("trials.rush_tier_pick"), style = MaterialTheme.typography.labelSmall)
                (0 until shown).forEach { t -> TierPlate(t, done = t < passed, selected = t == tier) { tier = t } }
                if (passed == 0 && !free) MutedText(ui("trials.rush_tier_first"), style = MaterialTheme.typography.labelSmall)
            }
        }
        RushTierFacts(rules.rush.tier(tier))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("trials.rush_tier", roman(tier + 1)), color = Parchment, fontFamily = RelicSerif, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            ForgeOutlinedButton(onClick = { vm.enterRush(region.code, tier) }, enabled = idle && keys >= 1 && trials.run == null, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text(ui("trials.rush_enter"))
                Spacer(Modifier.width(6.dp))
                BagIcon(TrialRules.KEY, Modifier.size(16.dp))
                Text(compactCount(keys))
            }
        }
        when {
            trials.run != null -> MutedText(ui("trials.rush_busy"), style = MaterialTheme.typography.labelSmall)
            keys < 1 -> MutedText(ui("trials.rush_no_key"), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Плашка ступени ленты: пройденная - зелёная, следующая - золотая; выбранная - в яркой нити. */
@Composable private fun TierPlate(tier: Int, done: Boolean, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    val tint = if (done) Vital else Brass
    Box(
        Modifier.heightIn(min = 26.dp).widthIn(min = 30.dp).clip(shape).background(tint.copy(alpha = if (done) .08f else .12f))
            .border(if (selected) 2.dp else 1.dp, if (selected) Brass else tint.copy(alpha = .45f), shape).clickable(onClick = onClick).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(roman(tier + 1), color = if (done) Vital else GoldBright, fontFamily = RelicSerif, fontSize = 12.sp)
    }
}

/** Ближайший закрытый регион раша: замок, имя и сколько зон осталось зачистить, пунктирная рамка без входа. */
@Composable private fun LockedRegion(region: Region, left: Int) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().alpha(.8f).dashedBorder(Bronze, shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(16.dp))
            Text(regionTitle(region.code), color = Muted, fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MutedText(ui("trials.rush_locked_zones", left), style = MaterialTheme.typography.labelSmall)
    }
}

/** Рамка закрытого региона пунктиром. */
private fun Modifier.dashedBorder(color: Color, shape: RoundedCornerShape): Modifier = drawBehind {
    drawOutline(shape.createOutline(size, layoutDirection, this), color, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
}

private val TagShape = RoundedCornerShape(6.dp)

/**
 * Цифры выбранной ступени раша из её записи в контенте - строками «подпись - значение»: здоровье и урон боссов со знаком
 * (ниже обычного - зелёным, выше - красным, ноль - «как обычно»), насколько раньше ярость боссов и множитель сфер сундука.
 */
@Composable private fun RushTierFacts(tier: RushTier) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val power = if (tier.power == 0.0) ui("trials.rush_power_usual") else ui("trials.rush_power_value", signedNumber(tier.power))
        val tint = when {
            tier.power < 0 -> Vital
            tier.power > 0 -> LifeRed
            else -> Parchment
        }
        TierFact(ui("trials.rush_power_label"), power, tint)
        val enrage = if (tier.enrage > 0) ui("trials.rush_enrage_value", number(tier.enrage)) else ui("trials.rush_enrage_usual")
        TierFact(ui("trials.rush_enrage_label"), enrage, if (tier.enrage > 0) Ember else Parchment)
        TierFact(ui("trials.rush_reward_label"), ui("trials.rush_reward_value", number(tier.reward)), GoldBright)
    }
}

/** Строка цифр ступени: подпись слева приглушённо, значение справа своим цветом [tint]. */
@Composable private fun TierFact(label: String, value: String, tint: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MutedText(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(value, color = tint, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
    }
}

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

/** Номер ступени раша римскими цифрами. */
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
