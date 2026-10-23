package com.sperance.exileforge.ui.screens.contest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.contest.ContestModel
import com.sperance.exileforge.core.contest.ContestPick
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.contestGap
import com.sperance.exileforge.core.display.contestKindTitle
import com.sperance.exileforge.core.display.contestMetricLine
import com.sperance.exileforge.core.display.contestMetricTitle
import com.sperance.exileforge.core.display.contestTitle
import com.sperance.exileforge.core.display.contestValue
import com.sperance.exileforge.core.display.leagueLabel
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.prizeLine
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.display.timeLeft
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.ContestKind
import com.sperance.exileforge.rules.content.ContestMetric
import com.sperance.exileforge.rules.content.ContestRow
import com.sperance.exileforge.rules.content.ContestRules
import com.sperance.exileforge.rules.content.ContestStand
import com.sperance.exileforge.rules.content.ContestTable
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.roman
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Облик призовых мест таблицы, как у доски славы: первое - мифическая плита, второе - уникальная, третье - редкая. */
private val PODIUM = listOf(Rarity.MYTHICAL, Rarity.UNIQUE, Rarity.RARE)

/** Такт живых сроков состязаний: подписи - до минут. */
private const val TICK_MS = 10_000L

/**
 * Здание «Состязания»: сверху каменные вкладки «День» и «Неделя» (точка - у героя сейчас приз в этом периоде), под вкладкой стенд
 * выбранного периода тремя панелями ([StandPanels]); «Таблица лиги» открывает таблицу в облике доски славы ([ContestTableView]),
 * «Награды» - лист наград лиги. Лига героя - справа в строке цепочки. Сменился период - здание перечитывается само.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestScreen() {
    val vm = koinViewModel<ContestViewModel>().model
    val game by vm.game.collectAsStateWithLifecycle()
    val state by vm.contests.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf(ContestKind.DAY) }
    val rules = game.index?.rules?.contests
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        if (game.heroId.isBlank()) return@LaunchedEffect
        vm.load()
        if (state.pick != null) vm.loadTable()
    }
    DisposableEffect(Unit) { onDispose { vm.close() } }
    val board = state.board
    val now = rememberServerNow(TICK_MS)
    val over = board?.stands?.any { it.endsAt <= now } == true
    LaunchedEffect(over) {
        if (!over) return@LaunchedEffect
        vm.load(force = true)
        if (state.pick != null) vm.loadTable()
    }
    if (rules == null || board == null) {
        MutedText(ui("common.loading"), Modifier.padding(16.dp))
        return
    }
    board.stands.firstOrNull()?.let { TrailData(ui("contest.trail_league", leagueLabel(rules, it.league))) }
    state.pick?.let { pick ->
        val mine = board.stands.firstOrNull { it.kind == pick.kind }?.league
        TrailLevel(contestKindTitle(pick.kind), onBack = vm::close) { ContestTableView(game, vm, rules, pick, mine, state.table, Reads.CONTEST_TABLE in activity.loading) }
        return
    }
    var prizes by remember { mutableStateOf<ContestStand?>(null) }
    val stands = remember(board.stands) { board.stands.sortedBy { it.kind } }
    val tabs = remember(stands) { stands.map(::kindTab) }
    val stand = stands.firstOrNull { it.kind == kind } ?: stands.firstOrNull()
    Column(Modifier.fillMaxSize()) {
        StoneTabs(tabs, stands.indexOf(stand), { kind = stands[it].kind }, scrollable = false)
        PullToRefreshBox(isRefreshing = Reads.CONTESTS in activity.loading, onRefresh = { vm.load(force = true) }, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stand?.let { shown -> StandPanels(game, rules, shown, onTable = { vm.open(ContestPick(shown.kind)) }, onPrizes = { prizes = shown }) }
            }
        }
    }
    prizes?.let { PrizesSheet(rules, it) { prizes = null } }
}

/** Вкладка периода: у суток - солнце цветом предостережения, у недели - луна цветом золотой медали; точка - у героя сейчас приз. */
private fun kindTab(stand: ContestStand): StoneTab {
    val label = ui("contest.kind_short.${stand.kind.name}")
    return when (stand.kind) {
        ContestKind.DAY -> StoneTab(label, Icons.Outlined.WbSunny, Caution, stand.prizedNow)
        ContestKind.WEEK -> StoneTab(label, Icons.Outlined.DarkMode, medalOf(1), stand.prizedNow)
    }
}

/**
 * Стенд периода тремя панелями: вид и срок, мера (у «босса» - портрет и имя босса) и под чертой анонс следующей меры периода; своё
 * место с тем, что дальше, и нынешний приз; лига героя с мини-пьедесталом, «Награды» и «Таблица лиги».
 */
@Composable
private fun StandPanels(game: GameUi, rules: ContestRules, stand: ContestStand, onTable: () -> Unit, onPrizes: () -> Unit) {
    MetricPanel(game, stand)
    ForgePanel {
        OwnStanding(stand)
        if (stand.ownPlace != null) PrizeNow(rules, stand)
    }
    LeaguePanel(rules, stand, onTable, onPrizes)
}

/** Мера периода: вид и срок, мера (у «босса» - портрет и имя босса; что считается - в ⓘ), под пунктиром - «Завтра:» или «С 27.10:» и следующая мера. */
@Composable private fun MetricPanel(game: GameUi, stand: ContestStand) {
    val left = rememberServerLeft(stand.endsAt, TICK_MS)
    ForgePanel(accent = metricHue(stand.metric)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ui("contest.kind_card.${stand.kind.name}"), color = Caution, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(timeLeft(left), color = Parchment, style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stand.boss?.let { BossPortrait(game, it) }
            Column(Modifier.weight(1f)) {
                Text(contestMetricTitle(stand.metric), style = relicName(19).copy(color = GoldBright), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stand.boss?.let { ui("contest.boss_try", monsterTitle(it)) } ?: ui("contest.metric_sum.${stand.metric.name}"), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
            InfoMark(ui("contest.metric.${stand.metric.name}"), { Tip(contestMetricTitle(stand.metric), ui("contest.metric_desc.${stand.metric.name}")) })
        }
        DashLine(Bronze, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("contest.next.${stand.kind.name}", dayOf(stand.endsAt)), color = Caution, style = MaterialTheme.typography.bodySmall)
            Text(contestMetricLine(stand.next, stand.nextBoss), color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

/** Лига героя: «Лига 20-39 · ваша», мини-пьедестал, «Награды» и «Таблица лиги». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeaguePanel(rules: ContestRules, stand: ContestStand, onTable: () -> Unit, onPrizes: () -> Unit) {
    ForgePanel {
        MutedText(ui("contest.league_mine", leagueLabel(rules, stand.league)))
        MiniPodium(stand)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ForgeButton(onPrizes, size = ButtonSize.Small, kind = ButtonKind.Text) { Text(ui("contest.prizes"), maxLines = 1) }
            ForgeButton(onTable, size = ButtonSize.Small, kind = ButtonKind.Secondary, icon = ForgeGlyphs.Trophy) { Text(ui("contest.table", leagueLabel(rules, stand.league)), maxLines = 1) }
        }
    }
}

/** Портрет босса периода и его подпись для чтения с экрана - рисунок сервера; пока портретов нет, жетон пуст. */
@Composable private fun BossPortrait(game: GameUi, boss: MonsterCode) {
    val form = game.index?.monster(boss)?.form ?: return
    MonsterPortrait(boss.value, form, game.world.portraits, Modifier.size(38.dp, 46.dp).clip(RoundedCornerShape(6.dp)).background(Abyss))
}

/** Мини-пьедестал лиги: до трёх строк «I · имя · мера», касание строки - карточка игрока; лига пуста - строкой. */
@Composable private fun MiniPodium(stand: ContestStand) {
    if (stand.podium.isEmpty()) {
        MutedText(ui("contest.empty_short"))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        stand.podium.forEachIndexed { i, row ->
            Row(Modifier.fillMaxWidth().opensPlayer(row.id).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(roman(i + 1), color = medalOf(i + 1), style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 24.dp))
                Text(row.name, color = if (row.id == stand.own?.id) Vital else Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(contestValue(stand.metric, row.value), color = GoldBright, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Своё место: «Ваше место 23 из 212 · 4» и что дальше - в призах (место пьедестала и сколько до первого), в лучших 10% или сколько до
 * них; без строки - как попасть в таблицу.
 */
@Composable private fun OwnStanding(stand: ContestStand) {
    val own = stand.own
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().background(Abyss, shape).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (own == null || stand.place <= 0) {
            Text(ui("contest.own_none"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            MutedText(joinHint(stand))
            return@Column
        }
        Text(ui("contest.own", stand.place, stand.size, contestValue(stand.metric, own.value)), color = Vital, style = MaterialTheme.typography.bodyMedium)
        MutedText(nextStep(stand, own))
    }
}

/** Как попасть в таблицу меры периода: найти уникальную вещь, убить босса периода или получить опыт. */
private fun joinHint(stand: ContestStand): String = stand.boss?.let { ui("contest.join.BOSS", monsterTitle(it)) } ?: ui("contest.join.${stand.metric.name}")

/** Что дальше у строки героя [own]: до первого места, «в лучших 10%» или сколько до их границы. */
private fun nextStep(stand: ContestStand, own: ContestRow): String {
    val place = stand.place.toInt()
    val leader = stand.podium.firstOrNull()
    return when {
        place == 1 -> ui("contest.leading")
        place <= stand.podium.size && leader != null -> ui("contest.to_first", roman(place), contestGap(stand.metric, gap(stand.metric, own.value, leader.value)))
        place <= stand.prized -> ui("contest.in_top")
        else -> stand.cutoff?.let { ui("contest.to_top", contestGap(stand.metric, gap(stand.metric, own.value, it))) } ?: ui("contest.in_top")
    }
}

/** Сколько не хватает мере [mine], чтобы обойти [target]: при равенстве выше раньше достигший, поэтому - строго лучше. */
private fun gap(metric: ContestMetric, mine: Long, target: Long): Long = if (metric.lower) mine - target + 1 else target - mine + 1

/**
 * Лист наград лиги стенда [stand]: места - сколько сфер ценной таблицы, у первых мест - ещё титул и рамка; лучшие 10% - по нынешнему
 * числу героев лиги, но не меньше места за пьедесталом. Награды приходят письмом по итогам; титул и рамка - до конца следующего периода.
 */
@Composable private fun PrizesSheet(rules: ContestRules, stand: ContestStand, onDismiss: () -> Unit) {
    val prizes = rules.periods[stand.kind]?.prizes.orEmpty()
    ForgeSheet(onDismissRequest = onDismiss, body = SheetBody.Scroll(spacing = 6.dp)) {
        Text(ui("contest.prizes_title", contestKindTitle(stand.kind), leagueLabel(rules, stand.league)), style = relicName(18).copy(color = GoldBright))
        MutedText(ui("contest.prizes_when.${stand.kind.name}"))
        var from = 1
        prizes.forEach { prize ->
            val last = prize.last(stand.size.toInt()).coerceAtLeast(from)
            val places = if (last == from) ui("contest.place_one", from) else ui("contest.place_range", from, last)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (prize.percent > 0) ui("contest.top_share", prize.percent.toInt()) else roman(from), color = if (rules.honoured(from)) medalOf(from) else Rune, style = MaterialTheme.typography.titleSmall, modifier = Modifier.widthIn(min = 40.dp))
                Text(places, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(prizeLine(prize, rules.honoured(from)), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
            }
            from = last + 1
        }
        MutedText(ui("contest.prizes_honour", contestTitle(stand.metric, stand.kind), stampText(rules.until(stand.kind, stand.period))))
    }
}

/**
 * Таблица лиги в облике доски славы: выбор лиги (своя помечена), строка меры, числа героев и срока; первые три - плиты с рамкой и лучшим
 * титулом героя, дальше список с призом в каждой строке, пунктир - граница лучших 10%. Строка героя с его нынешним призом - закреплена
 * внизу; пустая лига и герой вне таблицы - словами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContestTableView(game: GameUi, vm: ContestModel, rules: ContestRules, pick: ContestPick, mine: Int?, table: ContestTable?, loading: Boolean) {
    val shown = table?.stand?.league ?: pick.league ?: mine ?: 0
    Column(Modifier.fillMaxSize()) {
        BronzePills(
            rules.leagues.indices.map { if (it == mine) ui("contest.league_mine", leagueLabel(rules, it)) else leagueLabel(rules, it) },
            shown,
            { vm.open(pick.copy(league = it)) },
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        if (table == null) {
            MutedText(ui("common.loading"), Modifier.padding(16.dp))
            return
        }
        val stand = table.stand
        TableHeader(stand)
        PullToRefreshBox(isRefreshing = loading, onRefresh = vm::loadTable, modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (table.top.isEmpty()) {
                    item { EmptyLeague(rules, stand) }
                } else {
                    item { Podium(game, rules, stand, table.top) }
                    items(table.top.drop(PODIUM.size).withIndex().toList(), key = { it.value.id }) { (i, row) ->
                        val place = PODIUM.size + i + 1
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TableRow(game, rules, stand, row, place, own = row.id == stand.own?.id)
                            if (place == stand.prized && place < stand.size) TopBorder()
                        }
                    }
                }
            }
        }
        OwnBar(game, rules, stand, mine)
    }
}

/** Строка над таблицей: мера (с боссом), сколько героев в лиге и срок периода. */
@Composable private fun TableHeader(stand: ContestStand) {
    val left = rememberServerLeft(stand.endsAt, TICK_MS)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("contest.header", contestMetricLine(stand.metric, stand.boss), stand.size), color = Parchment, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(timeLeft(left), color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

/** Пустая лига: «Пока никого» и что даст первое место; три свободных места пьедестала. */
@Composable private fun EmptyLeague(rules: ContestRules, stand: ContestStand) {
    ForgePanel {
        Text(ui("contest.empty"), style = relicName(18).copy(color = GoldBright))
        MutedText(ui("contest.empty_hint", leagueLabel(rules, stand.league), joinHint(stand)))
        PODIUM.indices.forEach { i ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(roman(i + 1), color = medalOf(i + 1), style = MaterialTheme.typography.titleSmall, modifier = Modifier.widthIn(min = 28.dp))
                MutedText(ui("contest.free"))
            }
        }
    }
}

/** Три призовых места: первое - плита во всю ширину, второе и третье рядом; у каждой - рамка и лучший титул героя, мера и приз. */
@Composable private fun Podium(game: GameUi, rules: ContestRules, stand: ContestStand, top: List<ContestRow>) {
    val podium = top.take(PODIUM.size)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        podium.firstOrNull()?.let { Plate(game, rules, stand, it, 1, Modifier.fillMaxWidth(), big = true) }
        if (podium.size > 1) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                podium.drop(1).forEachIndexed { i, row -> Plate(game, rules, stand, row, i + 2, Modifier.weight(1f).fillMaxHeight(), big = false) }
                if (podium.size == 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Плита места [place]: подложка редкости места, портрет класса в рамке титула, титул, имя, класс и уровень, мера и приз места. */
@Composable private fun Plate(game: GameUi, rules: ContestRules, stand: ContestStand, row: ContestRow, place: Int, modifier: Modifier, big: Boolean) {
    val look = relicLook(PODIUM[place - 1])
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.relicGround(look, shape, selected = row.id == stand.own?.id).clip(shape).opensPlayer(row.id).padding(horizontal = 12.dp, vertical = if (big) 14.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(roman(place), style = look.titleStyle(if (big) 26 else 20))
        ClassPortrait(row.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(if (big) 44.dp else 34.dp), round = true, ring = honourRing(row.honour))
        HonourLine(row)
        Text(row.name, style = look.titleStyle(if (big) 19 else 14), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(about(row), color = look.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(contestValue(stand.metric, row.value), color = look.gold, style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall)
        PrizeTag(rules, stand, place)
    }
}

/** Лучший действующий титул героя строки - мелко капителью цветом его медали; без титула ничего. */
@Composable private fun HonourLine(row: ContestRow) {
    val honour = row.honour ?: return
    Text(contestTitle(honour).uppercase(), color = medalOf(honour.place), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Строка списка после пьедестала: место, портрет в рамке титула, имя с титулом, мера и приз места; [own] - строка героя. */
@Composable private fun TableRow(game: GameUi, rules: ContestRules, stand: ContestStand, row: ContestRow, place: Int, own: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().depthPanel(shape).then(if (own) Modifier.border(1.dp, Vital.copy(alpha = .6f), shape) else Modifier)
            .clip(shape).opensPlayer(row.id).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(place.toString(), color = if (own) Vital else Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.widthIn(min = 28.dp))
        ClassPortrait(row.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(28.dp), round = true, ring = honourRing(row.honour))
        Column(Modifier.weight(1f)) {
            HonourLine(row)
            Text(row.name, color = if (own) Vital else Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(about(row), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(contestValue(stand.metric, row.value), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
            PrizeTag(rules, stand, place)
        }
    }
}

/** Приз места [place] нынешней лиги: «×5» сфер, у первых мест - «×12 · титул»; без приза ничего. */
@Composable private fun PrizeTag(rules: ContestRules, stand: ContestStand, place: Int) {
    val prize = rules.prize(stand.kind, place, stand.size.toInt()) ?: return
    Text(prizeLine(prize, rules.honoured(place)), color = Rune, style = MaterialTheme.typography.labelSmall, maxLines = 1)
}

/** Граница лучших 10% - пунктир с подписью под последним местом с наградой. */
@Composable private fun TopBorder() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DashLine(Rune, Modifier.weight(1f))
        Text(ui("contest.top_border"), color = Rune, style = MaterialTheme.typography.labelSmall)
    }
}

/** Пунктирная черта цвета [color] толщиной в 1 dp. */
@Composable private fun DashLine(color: Color, modifier: Modifier) {
    Canvas(modifier.height(1.dp)) {
        drawLine(color, center.copy(x = 0f), center.copy(x = size.width), strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    }
}

/**
 * Строка героя, закреплённая под таблицей: место из скольких, результат и нынешний приз (сферы, у первых мест - титул и рамка); героя нет
 * в лиге - чья его лига или как попасть в таблицу.
 */
@Composable private fun OwnBar(game: GameUi, rules: ContestRules, stand: ContestStand, mine: Int?) {
    val own = stand.own
    val place = stand.ownPlace
    Column(Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            own != null && place != null -> {
                MutedText(ui("contest.own_bar", place, stand.size, contestValue(stand.metric, own.value)), style = MaterialTheme.typography.labelMedium)
                if (place > PODIUM.size) TableRow(game, rules, stand, own, place, own = true)
                PrizeNow(rules, stand)
            }

            mine != null && mine != stand.league -> MutedText(ui("contest.other_league", leagueLabel(rules, mine)))

            else -> {
                Text(ui("contest.own_none"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                MutedText(joinHint(stand))
            }
        }
    }
}

/** Строка «Приз сейчас» героя на его месте ([ContestStand.ownPlace]): подпись слева, приз места справа. */
@Composable private fun PrizeNow(rules: ContestRules, stand: ContestStand) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MutedText(ui("contest.prize_label"), style = MaterialTheme.typography.labelMedium)
        Text(ownPrize(rules, stand), color = GoldBright, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/**
 * Нынешний приз героя: «×8 сфер, титул «…», серебряная рамка» или что приза пока нет - по тому же правилу, что точка вкладки
 * ([ContestStand.prizedNow]).
 */
private fun ownPrize(rules: ContestRules, stand: ContestStand): String {
    val place = stand.ownPlace?.takeIf { stand.prizedNow } ?: return ui("contest.prize_none")
    val prize = rules.prize(stand.kind, place, stand.size.toInt()) ?: return ui("contest.prize_none")
    if (!rules.honoured(place)) return ui("contest.prize_now", prize.orbs)
    return ui("contest.prize_now_honour", prize.orbs, contestTitle(stand.metric, stand.kind), ui("contest.frame.$place"))
}

/** Класс и уровень героя строки. */
private fun about(row: ContestRow): String = listOfNotNull(row.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle), ui("hall.level", row.level)).joinToString(" · ")

/**
 * Цвет стенда меры: находки и сундуки - уголь уникальной, боссы - кровь, опыт - руна, охота - живая зелень, зоны - бронза, ремёсла -
 * золото профессий, сферы - золото печати; незнакомая мера - приглушённый.
 */
private fun metricHue(metric: ContestMetric): Color = when (metric) {
    ContestMetric.UNIQUES, ContestMetric.ZONE_CHESTS -> ModeHue.Uniques
    ContestMetric.BOSS, ContestMetric.BOSS_ANY -> ModeHue.Bosses
    ContestMetric.EXPERIENCE -> Rune
    ContestMetric.KILL -> Vital
    ContestMetric.ZONE_RUNS -> ModeHue.Tower
    ContestMetric.JOB -> ModeHue.Professions
    ContestMetric.SPENT -> ModeHue.Might
    ContestMetric.OTHER -> Muted
}

/** День начала следующего периода в поясе устройства: «27.10». */
private fun dayOf(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DAY)

private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
