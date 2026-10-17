package com.sperance.exileforge.ui.screens.crafts

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_CRAFTS
import com.sperance.exileforge.core.character.view
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.Harvest
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemShelf
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionDescription
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.crafts.CraftsViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.ui.TAB_SHIFT_MS
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.ceil

/** Экран ремёсел: плашка работы, итоги и плитки профессий; окно профессии и работы - в соседних файлах. */
/**
 * The crafts tab: the work under way on a plaque over the tiles, one
 * tile per profession, and a profession's window behind each. The work runs on the server by time;
 * this screen asks when it opens, throws each cycle here the moment it ends and asks the
 * server behind it, and runs the cycle's bar smoothly on the device's clock set by the server's.
 *
 * Экран с ремёслами спрашивает снова тихо; досчёт - когда смена вкладки доехала ([TAB_SHIFT_MS]) и только если прошлый ответ старше срока
 * свежести. Разделы плиток считаются раз на ответ. Вкладка нажата снова - окно профессии закрывается, плитки наверх. Досчёт после
 * отлучки, ещё не показанный, открывается листом «Пока вас не было» ([AwaySheet]) поверх плиток и окна профессии.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftsScreen() {
    val game by koinViewModel<CraftsViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<CraftsViewModel>()
    val held by vm.crafts.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val openCode by vm.profession.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        withFrameNanos { }
        delay(TAB_SHIFT_MS.toLong())
        vm.load(silent = held.state != null, fresh = true)
    }
    DisposableEffect(Unit) {
        vm.watch(true)
        onDispose { vm.watch(false) }
    }
    val crafts = held.state
    val offset = held.offset
    val list = rememberLazyListState()
    val sections = remember(crafts?.professions) {
        val (crafting, gathering) = crafts?.professions.orEmpty().partition { it.crafting }
        listOf("crafts.section_gather" to gathering, "crafts.section_craft" to crafting).filter { it.second.isNotEmpty() }.map { (title, group) -> title to group.chunked(TILES) }
    }
    OnReselect(TAB_CRAFTS) {
        vm.openProfession("")
        list.animateScrollToItem(0)
    }
    held.away?.let { away -> AwaySheet(game, away, onSeen = vm::seen) }
    val open = crafts?.professions?.firstOrNull { it.code == openCode }
    if (open != null) {
        ProfessionWindow(game, vm, held, open, offset)
        return
    }
    PullToRefreshBox(isRefreshing = activity.busy || Reads.CRAFTS in activity.loading, onRefresh = vm::load, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) { CollapsibleHeader { ScreenHeader(ui("crafts.title"), ui("crafts.subtitle"), ForgeGlyphs.Anvil) { CraftsGlossaryButton(crafts?.rules) } } }
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (crafts == null) {
                    item { InfoCard(ui("common.loading"), ui("crafts.loading_hint")) }
                    return@LazyColumn
                }
                item { WorkPlaque(game, vm, held, offset) }
                sections.forEachIndexed { index, (title, rows) ->
                    if (index > 0) item { FlowMark() }
                    item { SectionRule(ui(title)) }
                    items(rows, key = { it.first().code }) { row ->
                        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ProfessionTile(game, it, working = crafts.work?.profession == it.code, Modifier.weight(1f).fillMaxHeight()) { vm.openProfession(it.code) } }
                            repeat(TILES - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                crafts.rules?.let { rules -> item { MutedText(ui("crafts.note", number(rules.offlineHours))) } }
            }
        }
    }
}

/**
 * The work under way, on every screen of the tab: which, how far the cycle is, and a stop — and
 * in its own frame, what the work has come to since it started: how
 * long it runs, its cycles and experience, what it made, and every stack gathered and spent. Итог - книгой учёта ([WorkLedger]); сделанное
 * с начала работы - вещи героя с меткой происхождения этой работы ([com.sperance.exileforge.core.model.crafts.WorkView.made]), кнопка
 * под ними открывает тайник с фильтром «Сделано ремеслом».
 *
 * Вещь в работе (Осквернение) не видна среди вещей героя, пока работа идёт.
 */
@Composable
internal fun WorkPlaque(game: GameUi, vm: CraftsViewModel, crafts: Crafts, offset: Long) {
    val work = crafts.state?.work
    ForgePanel {
        if (work == null) {
            MutedText(ui("crafts.idle"), style = MaterialTheme.typography.bodyMedium)
            return@ForgePanel
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workTitle(game, work), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(ui("crafts.work_line", professionTitle(work.profession), number(work.cycleMillis / 1000.0)), color = Rune, style = MaterialTheme.typography.labelMedium)
            }
            ForgeOutlinedButton(enabled = !game.busy, onClick = vm::stop) { Text(ui("crafts.stop")) }
        }
        game.heldWork?.let { held -> game.view(held)?.let { HeldItem(it) } }
        CycleBar(work, offset, crafts.last, hourly = hourlyLine(crafts, work))
        LevelLine(crafts, work, offset)
        val sums = remember(work.totals) { WorkSums.of(work.totals) }
        val hero = game.hero
        val made = remember(work, hero?.items, hero?.overflow) { work.made(hero?.items.orEmpty() + hero?.overflow.orEmpty()) }
        val heroModel = koinViewModel<HeroViewModel>()
        WorkLedger(
            game,
            sums,
            made,
            title = ui("crafts.totals_title"),
            startedAt = work.startedAt,
            offset = offset,
            makes = crafts.state?.running?.kind?.makesItems == true,
            onShow = { heroModel.showStash(ItemShelf.STASH.defaults.copy(crafted = true)) },
        )
        crafts.state?.running?.let { StockRow(game, work, it) }
    }
}

/** Вещь, что держит работа: строка плиты с подписью «в работе»; касание открывает её карточку. */
@Composable internal fun HeldItem(view: ItemView) {
    val inspect = rememberInspect()
    ItemRow(view, facts = listOf(ui("crafts.held"))) { inspect(Inspect.Copy(view.item)) }
}

/** Чип стопки или вещи: [figure] в тоне [tone] (null - без числа) и название; касание - [onClick]. */
@Composable internal fun TallyChip(title: String, figure: String?, tone: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    Row(
        Modifier.background(Abyss, shape).border(1.dp, tone.copy(alpha = .5f), shape).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        figure?.let { Text(it, color = tone, style = MaterialTheme.typography.labelMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
        Text(title, color = Parchment, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** An engraved caption with a bronze rule running out of it, heading a group of tiles. */
@Composable internal fun SectionRule(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Engraved(title)
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Bronze, Color.Transparent))))
    }
}

/** Between gathering and crafting: what the first brings is what the second spends. */
@Composable internal fun FlowMark() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Rhombus(Bronze, 6.dp)
        Engraved(ui("crafts.flow"), accent = Bronze)
        Rhombus(Bronze, 6.dp)
    }
}

/**
 * A cycle's bar, filled smoothly: it reads the device's clock, set by the server's through [offset], and only
 * the bar is redrawn — the screen around it is not recomposed. Часы полосы тикают не каждый кадр, а с шагом
 * в один пиксель её ширины ([cycleStepMs]): на глаз так же плавно, а экран ремёсел не держит полный FPS.
 * Под ней одной строкой - сколько прошло цикла из всего и что работа даёт в час.
 * Полоса идёт по кругу ([WorkView.phase]) и не встаёт на 100%, пока сервер не пересчитал работу; над ней
 * всплывает, что принёс пересчёт на границе цикла ([last]).
 */
@Composable internal fun CycleBar(work: WorkView, offset: Long, last: Harvest?, height: Int = 6, hourly: String? = null) {
    var widthPx by remember { mutableIntStateOf(0) }
    val step = cycleStepMs(work.cycleMillis, widthPx)
    val now by produceState(System.currentTimeMillis(), step) {
        while (true) {
            value = System.currentTimeMillis()
            delay(step)
        }
    }
    Box(Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { if (work.cycleMillis > 0) work.phase(now + offset).toFloat() / work.cycleMillis else 0f },
            modifier = Modifier.fillMaxWidth().height(height.dp).onSizeChanged { widthPx = it.width },
            color = Gold,
            trackColor = PanelRaised,
        )
        HarvestPopup(last)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CycleClock(work, offset, Modifier.weight(1f))
        hourly?.let { Text(it, color = Vital, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, maxLines = 2) }
    }
}

/** Идущий цикл часами - «Цикл 3:41 из 5:19» - несколько раз в секунду: перерисовывается только текст, не полоса. */
@Composable internal fun CycleClock(work: WorkView, offset: Long, modifier: Modifier) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(CLOCK_TICK)
        }
    }
    MutedText(ui("crafts.cycle_clock", clock(work.phase(now + offset)), clock(work.cycleMillis)), modifier, style = MaterialTheme.typography.labelSmall)
}

/**
 * Всплывашка сбора над полосой цикла: что принёс пересчёт сервера - «+1 Медная руда · +2 опыта» или «пусто» -
 * на [HARVEST_SHOWN] мс. Сбор, что уже был при открытии экрана, не всплывает; без анимаций в настройках - без плавности.
 */
@Composable private fun HarvestPopup(last: Harvest?) {
    val seen = remember { last?.seq ?: 0L }
    var text by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(last?.seq) {
        val fresh = last?.takeIf { it.seq > seen } ?: return@LaunchedEffect
        text = harvestLine(fresh.gains)
        visible = true
        delay(HARVEST_SHOWN)
        visible = false
    }
    val motion = LocalMotion.current
    val lift = with(LocalDensity.current) { 30.dp.roundToPx() }
    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -lift)) {
        AnimatedVisibility(
            visible,
            enter = if (motion) fadeIn() + slideInVertically { it / 2 } else EnterTransition.None,
            exit = if (motion) fadeOut() else ExitTransition.None,
        ) {
            Text(
                text,
                color = Vital,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.background(Abyss, RoundedCornerShape(50)).border(1.dp, Vital.copy(alpha = .5f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

/** Сколько всплывашка сбора держится над полосой цикла. */
private const val HARVEST_SHOWN = 2_500L

/** Шаг часов полосы цикла: время одного пикселя её ширины, в пределах [CYCLE_STEP_MIN]..[CYCLE_STEP_MAX] мс. */
internal fun cycleStepMs(cycleMillis: Long, widthPx: Int): Long = if (cycleMillis <= 0 || widthPx <= 0) CYCLE_STEP_MAX else (cycleMillis / widthPx).coerceIn(CYCLE_STEP_MIN, CYCLE_STEP_MAX)

/** Чаще кадра полоса не тикает. */
private const val CYCLE_STEP_MIN = 16L

/** Реже - заметно на глаз, даже если пиксель ещё не набежал. */
private const val CYCLE_STEP_MAX = 250L
