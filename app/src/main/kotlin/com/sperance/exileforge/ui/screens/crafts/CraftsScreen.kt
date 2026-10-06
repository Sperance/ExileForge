package com.sperance.exileforge.ui.screens.crafts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
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
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
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

/** Экран ремёсел (3.80.14): плашка работы, итоги и плитки профессий; окно профессии и работы - в соседних файлах. */
/**
 * The crafts tab (since 2.41.0, server 0.37.0): the work under way on a plaque over the tiles, one
 * tile per profession, and a profession's window behind each. The work runs on the server by time;
 * this screen asks when it opens, throws each cycle here the moment it ends (2.47.0) and asks the
 * server behind it, and runs the cycle's bar smoothly on the device's clock set by the server's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftsScreen() {
    val game by koinViewModel<CraftsViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val vm = koinViewModel<CraftsViewModel>()
    val held by vm.crafts.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val openCode by vm.profession.collectAsStateWithLifecycle()
    // A screen already holding the crafts asks again in silence: the cycle's alarm is the actions' (2.56.1).
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        heroModel.ensure()
        vm.load(silent = held.state != null)
    }
    val crafts = held.state
    val offset = held.offset
    val open = crafts?.professions?.firstOrNull { it.code == openCode }
    if (open != null) {
        ProfessionWindow(game, vm, held, open, offset)
        return
    }
    PullToRefreshBox(isRefreshing = activity.busy || Reads.CRAFTS in activity.loading, onRefresh = vm::load, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) { CollapsibleHeader { ScreenHeader(ui("crafts.title"), ui("crafts.subtitle"), ForgeGlyphs.Anvil) } }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (crafts == null) {
                    item { InfoCard(ui("common.loading"), ui("crafts.loading_hint")) }
                    return@LazyColumn
                }
                item { WorkPlaque(game, vm, held, offset) }
                // Gathering over crafting, with the materials flowing from one into the other (the owner's pick of five mockups, 2.44.0).
                val (crafting, gathering) = crafts.professions.partition { it.crafting }
                listOf("crafts.section_gather" to gathering, "crafts.section_craft" to crafting).filter { it.second.isNotEmpty() }.forEachIndexed { index, (title, group) ->
                    if (index > 0) item { FlowMark() }
                    item { SectionRule(ui(title)) }
                    items(group.chunked(TILES)) { row ->
                        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ProfessionTile(game, it, working = crafts.work?.profession == it.code, Modifier.weight(1f).fillMaxHeight()) { vm.openProfession(it.code) } }
                            repeat(TILES - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                // The rules travel with the answer; an answer without them says nothing about the offline cap.
                crafts.rules?.let { rules -> item { MutedText(ui("crafts.note", number(rules.offlineHours))) } }
            }
        }
    }
}

/**
 * The work under way, on every screen of the tab: which, how far the cycle is, and a stop — and
 * since 2.75.0 (server 0.66.0), in its own frame, what the work has come to since it started: how
 * long it runs, its cycles and experience, what it made, and every stack gathered and spent.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
                Text(workTitle(work), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(ui("crafts.work_line", professionTitle(work.profession), number(work.cycleMillis / 1000.0)), color = Rune, style = MaterialTheme.typography.labelMedium)
            }
            ForgeOutlinedButton(enabled = !game.busy, onClick = vm::stop) { Text(ui("crafts.stop")) }
        }
        CycleBar(work.settledAt, work.cycleMillis, offset, hourly = hourlyLine(crafts, work))
        levelLine(crafts, work, offset)?.let { Text(it, color = Vital, style = MaterialTheme.typography.labelMedium) }
        // The server's tally with the cycles this device threw ahead of its count: the rules' own sum.
        WorkTotals(work.startedAt, work.totals + crafts.pending, offset)
        crafts.last?.let { Text(gainsLine(it), color = Parchment, style = MaterialTheme.typography.bodySmall) }
        crafts.state?.running?.let { stockLine(game, work, it) }?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
    }
}

/**
 * What the work has come to since it started, framed inside its plaque: four figures in a row —
 * the time it runs (ticking by the server's clock), cycles, experience, pieces made — and under
 * them a chip per stack, gathered in green and spent in red.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun WorkTotals(startedAt: Long, totals: WorkTally, offset: Long) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        Modifier.fillMaxWidth().background(Abyss, shape).border(1.dp, Bronze.copy(alpha = .6f), shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(ui("crafts.totals_title"), color = Gold, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (startedAt > 0) {
                val now by produceState(System.currentTimeMillis()) {
                    while (true) {
                        value = System.currentTimeMillis()
                        kotlinx.coroutines.delay(1_000)
                    }
                }
                TotalFigure(ui("crafts.totals_time"), duration(now + offset - startedAt), Modifier.weight(1f))
            }
            TotalFigure(ui("crafts.totals_cycles"), if (totals.nothing > 0) "${totals.cycles} (−${totals.nothing})" else totals.cycles.toString(), Modifier.weight(1f))
            TotalFigure(ui("crafts.totals_experience"), "+" + number(totals.experience) + (if (totals.levels > 0) " ▲${totals.levels}" else ""), Modifier.weight(1f))
            if (totals.made > 0) TotalFigure(ui("crafts.totals_made"), totals.made.toString(), Modifier.weight(1f))
        }
        if (totals.items.isEmpty() && totals.spent.isEmpty()) {
            MutedText(ui("crafts.totals_empty"), style = MaterialTheme.typography.labelSmall)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                totals.items.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "+$amount", Vital) }
                totals.spent.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "−$amount", LifeRed) }
            }
        }
    }
}

@Composable internal fun TotalFigure(title: String, figure: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(figure, color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1)
        Text(title, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * What this session's crafting brought, summed (2.47.0): one line of cycles, the empty ones and the
 * experience, then a chip per stack — gathered in green, spent in red — and the pieces made.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SessionTally(totals: WorkGains) {
    if (totals.cycles == 0) {
        MutedText(ui("crafts.session_empty"), style = MaterialTheme.typography.labelMedium)
        return
    }
    MutedText(
        if (totals.nothing > 0) {
            ui("crafts.session_line", totals.cycles, totals.nothing, number(totals.experience))
        } else {
            ui("crafts.session_line_sure", totals.cycles, number(totals.experience))
        },
        style = MaterialTheme.typography.labelMedium,
    )
    if (totals.items.isNotEmpty() || totals.spent.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            totals.items.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "+$amount", Vital) }
            totals.spent.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "−$amount", LifeRed) }
        }
    }
    if (totals.equipment.isNotEmpty()) {
        Text(
            ui("crafts.made", totals.equipment.joinToString { equipmentTitle(it.template) }),
            color = Parchment,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (totals.starved) Text(ui("crafts.starved"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
}

@Composable internal fun TallyChip(title: String, figure: String, tone: Color) {
    val shape = RoundedCornerShape(2.dp)
    Row(
        Modifier.background(Abyss, shape).border(1.dp, tone.copy(alpha = .5f), shape).padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(figure, color = tone, style = MaterialTheme.typography.labelMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
 * A cycle's bar, filled smoothly (2.47.0): it reads the device's clock every frame, set by the
 * server's through [offset], and only the bar is redrawn — the screen around it is not recomposed.
 * Under it (3.24.0) the time the cycle has run of its whole, and what the work brings in an hour on average.
 */
@Composable internal fun CycleBar(settledAt: Long, cycleMillis: Long, offset: Long, height: Int = 6, hourly: String? = null) {
    val now by produceState(System.currentTimeMillis()) { while (true) withFrameMillis { value = System.currentTimeMillis() } }
    LinearProgressIndicator(
        progress = { if (cycleMillis > 0) ((now + offset - settledAt).toFloat() / cycleMillis).coerceIn(0f, 1f) else 0f },
        modifier = Modifier.fillMaxWidth().height(height.dp),
        color = Gold,
        trackColor = PanelRaised,
    )
    CycleClock(settledAt, cycleMillis, offset, Modifier.fillMaxWidth())
    hourly?.let { Text(it, color = Vital, style = MaterialTheme.typography.labelSmall) }
}

/** The running cycle as figures — «12.4 / 30 s» — a few times a second, so the text alone is redrawn, not the bar's frame. */
@Composable internal fun CycleClock(settledAt: Long, cycleMillis: Long, offset: Long, modifier: Modifier) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(CLOCK_TICK)
        }
    }
    val elapsed = (now + offset - settledAt).coerceIn(0L, cycleMillis.coerceAtLeast(0L))
    MutedText(ui("crafts.cycle_progress", fineNumber(elapsed / 1000.0), number(cycleMillis / 1000.0)), modifier, style = MaterialTheme.typography.labelSmall)
}
