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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.lockedText
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.romanNumeral
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.pluralKey
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
 * Тестировщику со свободным прогрессом замки прогресса не мешают: вход в Разлом, зачистка региона и ступень раша открыты.
 *
 * Каждое число на доске - один раз: запасы ключей - только в сетке сверху, правила режимов - в ⓘ плиты, причина запрета входа -
 * на погашенной кнопке. Разлом недели - своя доска на весь экран. Покорённая башня печать не тратит и не раскрывается. Закрытый раш
 * не показывается (тестировщику со свободным прогрессом открыты все регионы); фрагменты герба сами собираются в ключ на сервере - сетка показывает, сколько
 * их к следующему ключу; ближайший закрытый регион - один, с тем, что осталось зачистить.
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
    val freeProgress = game.freeProgress
    val rift by vm.riftState.collectAsState()
    var opened by rememberSaveable { mutableStateOf<TrialMode?>(null) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeyGrid(
            listOf(
                TrialRules.CREST to "$crests/${rules.rush.key}",
                TrialRules.KEY to stackCount(keys),
                TrialRules.SEAL to stackCount(seals),
                RiftRules.KEY to stackCount(hero.bag[RiftRules.KEY] ?: 0L),
            ),
        )
        trials.run?.let { open -> OpenTrial(open.kind, if (open.kind == TrialKind.RUSH) regionTitle(open.region) else open.floor.toString(), idle, vm::abandonTrial) }
        val toggle = { mode: TrialMode -> opened = mode.takeIf { it != opened } }
        rules.rift?.let { riftRules ->
            val left = rift.board?.free
            TrialModeCard(
                TrialMode.RIFT,
                note = null,
                chip = left?.let { ui("trials.chip_free", it, riftRules.free) },
                info = { Tip(TrialMode.RIFT.label, ui("rift.hint", riftRules.acts, riftRules.layout.rows, riftRules.free), TrialMode.RIFT.accent) },
                open = opened == TrialMode.RIFT,
                phase = 0f,
                onToggle = { toggle(TrialMode.RIFT) },
            ) {
                val open = game.unlocked(Feature.RIFT)
                ForgeButton(onClick = vm::openRift, state = ButtonState.of(idle && open), modifier = Modifier.fillMaxWidth(), size = ButtonSize.Wide) {
                    Text(if (open) ui("rift.open") else Feature.RIFT.lockedText(index.rules), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val tower = rules.tower
        val conquered = tower.start(trials.towerBest) > tower.maxFloor
        val towerNote = if (conquered) ui("trials.tower_conquered", tower.maxFloor) else ui("trials.tower_record", trials.towerBest, tower.start(trials.towerBest))
        val towerHint = {
            Tip(
                TrialMode.TOWER.label,
                ui(
                    "trials.tower_hint", tower.firstLevel, tower.levelStep, index.rules.fight.maxFoes, tower.life.toInt(), number(tower.flaskCharges),
                    tower.hoardEvery, tower.modEvery, tower.checkpoint,
                ),
                TrialMode.TOWER.accent,
            )
        }
        val towerEntry: @Composable ColumnScope.() -> Unit = {
            tower.mods(tower.start(trials.towerBest) + tower.modEvery - 1, trials.towerPicks).flatMap { it.lines }.forEach {
                Text(SkillText.statLine(it.stat, it.op, it.value, index), color = LifeRed, style = MaterialTheme.typography.labelSmall)
            }
            EnterButton(ui("trials.tower_enter", itemTitle(TrialRules.SEAL)), entryBlock(trials.run != null, seals), idle, vm::enterTower)
        }
        TrialModeCard(
            TrialMode.TOWER,
            note = towerNote,
            chip = null,
            info = towerHint,
            open = opened == TrialMode.TOWER,
            phase = 1f / 3,
            onToggle = { toggle(TrialMode.TOWER) },
            details = towerEntry.takeUnless { conquered },
        )
        val cleared = hero.campaign.cleared
        val unlocked = index.campaign.regions.filter { freeProgress || RushPlan.open(it, cleared) }
        TrialModeCard(
            TrialMode.RUSH,
            note = null,
            chip = null,
            info = { Tip(TrialMode.RUSH.label, ui("trials.rush_hint", rules.rush.key, rules.rush.life.toInt(), rules.rush.seconds.toInt()) + "\n\n" + ui("trials.rush_tiers_hint"), TrialMode.RUSH.accent) },
            open = opened == TrialMode.RUSH,
            phase = 2f / 3,
            onToggle = { toggle(TrialMode.RUSH) },
        ) {
            unlocked.forEach { region -> RushRegion(game, vm, rules, region, keys, freeProgress) }
            index.campaign.regions.filterNot { it in unlocked }.minByOrNull { region -> region.zones.count { it.code !in cleared } }?.let { near ->
                LockedRegion(near, near.zones.count { it.code !in cleared })
            }
        }
    }
}

/** Причина, по которой вход в испытание закрыт: идёт другое испытание [busy] или нет ключа - при [keys] меньше одного; null - вход открыт. */
private fun entryBlock(busy: Boolean, keys: Long): String? = when {
    busy -> ui("trials.why_busy")
    keys < 1 -> ui("trials.why_no_key")
    else -> null
}

/** Вход в испытание во всю ширину: подпись [label], а при запрете [why] - погашена и пишет причину вместо подписи. */
@Composable private fun EnterButton(label: String, why: String?, idle: Boolean, onClick: () -> Unit) {
    ForgeButton(onClick = onClick, state = ButtonState.of(idle && why == null), modifier = Modifier.fillMaxWidth(), size = ButtonSize.Wide) {
        Text(why ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Незавершённое испытание строкой: малый медальон режима [kind], что идёт ([place] - регион раша или этаж башни), что достанется
 * при завершении, и «Завершить».
 */
@Composable private fun OpenTrial(kind: TrialKind, place: String, idle: Boolean, onAbandon: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val rush = kind == TrialKind.RUSH
    Row(
        Modifier.fillMaxWidth().depthPanel(shape).border(1.dp, LifeRed.copy(alpha = .45f), shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TrialMedallion(if (rush) TrialMode.RUSH else TrialMode.TOWER, size = 36)
        Column(Modifier.weight(1f)) {
            Text(ui(if (rush) "trials.open_rush" else "trials.open_tower", place), color = LifeRed, fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            MutedText(ui(if (rush) "trials.open_rush_keep" else "trials.open_tower_keep"), style = MaterialTheme.typography.labelSmall)
        }
        ForgeButton(onClick = onAbandon, state = ButtonState.of(idle), kind = ButtonKind.Secondary, size = ButtonSize.Compact) { Text(ui("trials.abandon")) }
    }
}

/**
 * Регион раша: имя и «зачищен», боссы и лучшее время, лента ступеней - все пройденные (зелёные), ровно одна следующая (золотая,
 * выбрана по умолчанию) и за ней одна закрытая с замком - касание объясняет, как её открыть; дальше ступени не рисуются. Под лентой -
 * цифры выбранной ступени с подписями ([RushTierFacts]) и широкая «Войти · ступень N»; без ключа или при идущем испытании она погашена
 * и пишет причину. Тестировщику со свободным прогрессом (`ProgressGate`) лента показывает все ступени открытыми.
 */
@Composable private fun RushRegion(game: GameUi, vm: ExpeditionViewModel, rules: TrialRules, region: Region, keys: Long, freeProgress: Boolean) {
    val trials = game.hero?.campaign?.trials ?: return
    val best = trials.rushBest[region.code]
    val count = rules.rush.tierCount
    val passed = (trials.rushTiers[region.code] ?: 0).coerceIn(0, count)
    val lowest = passed.coerceAtMost(count - 1)
    val shown = if (freeProgress) count else minOf(passed + 1, count)
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
        val bosses = minOf(region.zones.size, rules.rush.bosses)
        MutedText(
            listOfNotNull(ui(pluralKey("trials.rush_bosses", bosses), bosses), best?.let { ui("trials.rush_best", clock(it.toDouble())) }).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
        )
        if (count > 1) {
            val cue = rememberRowScrollCue()
            Row(Modifier.scrollCue(cue, Panel).horizontalScroll(cue.state), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (0 until shown).forEach { t -> TierPlate(t, done = t < passed, selected = t == tier, modifier = Modifier.cueSelected(cue, t == tier)) { tier = t } }
                if (shown < count) LockedTier(shown)
            }
        }
        val shownTier = rules.rush.tier(tier)
        RushTierFacts(shownTier, game.index?.rules?.loot?.uniqueOdds(shownTier.unique) ?: shownTier.unique)
        EnterButton(ui("trials.rush_enter", roman(tier + 1)), entryBlock(trials.run != null, keys), !game.busy) { vm.enterRush(region.code, tier) }
    }
}

/** Плашка ступени ленты: пройденная - зелёная, следующая - золотая; выбранная - в яркой нити. */
@Composable private fun TierPlate(tier: Int, done: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    val tint = if (done) Vital else Brass
    Box(
        modifier.heightIn(min = 26.dp).widthIn(min = 30.dp).clip(shape).background(tint.copy(alpha = if (done) .08f else .12f))
            .border(if (selected) 2.dp else 1.dp, if (selected) Brass else tint.copy(alpha = .45f), shape).clickable(onClick = onClick).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(roman(tier + 1), color = if (done) Vital else GoldBright, fontFamily = RelicSerif, fontSize = 12.sp)
    }
}

/** Закрытая ступень [tier] после следующей: замок и номер приглушённо, касание - подсказка, какую ступень пройти, чтобы её открыть. */
@Composable private fun LockedTier(tier: Int) {
    val shape = RoundedCornerShape(7.dp)
    val title = ui("trials.rush_tier_locked", roman(tier + 1))
    Tipped(
        { Tip(title, ui("trials.rush_tier_locked_hint", roman(tier)), Muted) },
        Modifier.heightIn(min = 26.dp).widthIn(min = 30.dp).clip(shape).dashedBorder(Bronze, shape).semantics { contentDescription = title },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(11.dp))
            Text(roman(tier + 1), color = Muted, fontFamily = RelicSerif, fontSize = 12.sp)
        }
    }
}

/** Ближайший закрытый регион раша одной строкой: замок, имя и справа сколько зон осталось зачистить, пунктирная рамка без входа. */
@Composable private fun LockedRegion(region: Region, left: Int) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().alpha(.8f).dashedBorder(Bronze, shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(16.dp))
        Text(regionTitle(region.code), color = Muted, fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        MutedText(ui(pluralKey("trials.rush_zones_left", left), left), style = MaterialTheme.typography.labelMedium)
    }
}

/** Рамка закрытого региона пунктиром. */
private fun Modifier.dashedBorder(color: Color, shape: RoundedCornerShape): Modifier = drawBehind {
    drawOutline(shape.createOutline(size, layoutDirection, this), color, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
}

private val TagShape = RoundedCornerShape(6.dp)

/**
 * Цифры выбранной ступени раша из её записи в контенте - строками «подпись - значение»: здоровье и урон боссов со знаком
 * (ниже обычного - зелёным, выше - красным, ноль - «как обычно»), насколько раньше ярость боссов, множитель сфер сундука и шанс
 * уникалки за полную зачистку [unique] - шанс ступени под множителем правил (`LootRules.uniqueOdds`), как его катит сервер.
 */
@Composable private fun RushTierFacts(tier: RushTier, unique: Double) {
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
        TierFact(ui("trials.rush_unique_label"), ui("trials.rush_unique_value", number(unique)), GoldBright)
    }
}

/** Строка цифр ступени: подпись слева приглушённо, значение справа своим цветом [tint]. */
@Composable private fun TierFact(label: String, value: String, tint: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MutedText(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(value, color = tint, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Ключи под рукой компактной сеткой, [KEYS_PER_ROW] в ряд: значок и подпись - число стопки или, у фрагментов герба, сколько их
 * к следующему ключу; касание открывает лист стопки.
 */
@Composable private fun KeyGrid(stacks: List<Pair<String, String>>) {
    val inspect = rememberInspect()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stacks.chunked(KEYS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (code, label) -> Key(code, label, Modifier.weight(1f)) { inspect(Inspect.Stack(code)) } }
                repeat(KEYS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable private fun Key(code: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clickable(onClickLabel = itemTitle(code), onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BagIcon(code, Modifier.size(22.dp))
            Text(label, color = Parchment, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private const val KEYS_PER_ROW = 4

/** Номер ступени раша римскими цифрами. */
internal fun roman(n: Int): String = romanNumeral(n)
