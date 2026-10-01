package com.sperance.exileforge.ui.screens.crafts

import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionDescription
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.theme.*
import kotlin.math.ceil
import kotlinx.coroutines.delay

/** The dictionary's name of a work; the tab bar's badge (ForgeApp) names it by this package, so it stays here. */
fun jobTitle(code: String): String = com.sperance.exileforge.core.display.jobTitle(code)

/** The work under way by name, with its choice. */
fun workTitle(work: WorkView): String = com.sperance.exileforge.core.display.workTitle(work.job, work.choice)

/**
 * What an answer brought, as one line: «+2 Iron Ore, +1 Bark», the pieces a smith or a cartographer
 * made, that the bag ran dry, or that the cycles came up empty.
 */
fun gainsLine(gains: WorkGains): String = listOfNotNull(
    gains.items.entries.joinToString { (code, amount) -> ui("crafts.gain", amount, itemTitle(code)) }.ifBlank { null },
    gains.equipment.takeIf { it.isNotEmpty() }?.let { made -> ui("crafts.made", made.joinToString { equipmentTitle(it.template) }) },
    ui("crafts.starved").takeIf { gains.starved },
).joinToString(" · ").ifBlank { ui("crafts.gain_nothing", gains.cycles) }

/** How many of a stack the bag holds, by the item's code — a material, an orb, an essence or a book; 0 before the hero is read. */
fun bagCount(s: ForgeState, code: String): Long = s.bagAmount(code) ?: 0L

/** What a work makes, in words: the stack, the smith's range or the cartographer's location. */
fun jobProduct(job: JobView): String = when (job.kind) {
    JobKind.ITEM -> itemTitle(job.output)
    JobKind.EQUIPMENT -> ui("crafts.kind_equipment", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })
    JobKind.MAP -> ui("crafts.kind_map", regionTitle(job.region))
    JobKind.FLASK -> equipmentTitle(job.output)
    // Choosing works (3.45.0): the variant picked in the sheet is a plain ITEM work.
    JobKind.BOOK -> ui("crafts.kind_book", job.band.getOrElse(0) { 1 })
    JobKind.CONDENSE -> ui("crafts.kind_condense")
    JobKind.REFINE -> ui("crafts.kind_refine")
    JobKind.JEWEL -> ui("crafts.kind_jewel", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })
}

/** A crafting profession spends materials; a gathering one only brings them. The works say which, not a list of codes. */
val ProfessionView.crafting get() = jobs.any { it.inputs.isNotEmpty() }

/**
 * How long the bag keeps the work going: the input that runs out first, what the bag holds of it and
 * how many cycles that pays for — every additive the work was started with is spent each cycle too.
 * Display only: the server stops the work when a cycle cannot be paid.
 */
fun stockLine(s: ForgeState, work: WorkView, job: JobView): String? {
    val inputs = job.inputs + work.additives.map { JobInput(it, 1) }
    val scarce = inputs.filter { it.amount > 0 }.minByOrNull { bagCount(s, it.item) / it.amount } ?: return null
    val have = bagCount(s, scarce.item)
    val cycles = (have / scarce.amount).toInt()
    return if (cycles == 0) ui("crafts.stock_empty", itemTitle(scarce.item), have)
    else ui("crafts.stock", itemTitle(scarce.item), have, cycles, plural("crafts.cycles", cycles), eta(cycles * work.cycleMillis))
}

/**
 * How long the work still needs to lift its profession a level (3.2.0), counted here: the experience a
 * cycle brings past its «nothing» chance, the cycles that leaves, less what the running cycle has done.
 */
@Composable private fun levelLine(s: ForgeState, work: WorkView, offset: Long): String? {
    val profession = s.play.crafts?.professions?.firstOrNull { it.code == work.profession } ?: return null
    val next = profession.next ?: return ui("crafts.level_top")
    val job = profession.job(work.job, work.choice) ?: return null
    val perCycle = job.experience * (1 + profession.bonus.experience.coerceAtLeast(0.0) / 100) * (1 - job.nothing / 100)
    if (perCycle <= 0 || work.cycleMillis <= 0) return null
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(1000); value = System.currentTimeMillis() } }
    val cycles = ceil((next - profession.experience).coerceAtLeast(0.0) / perCycle).toLong()
    val running = (now + offset - work.settledAt).coerceIn(0L, work.cycleMillis)
    return ui("crafts.level_eta", profession.level + 1, eta((cycles * work.cycleMillis - running).coerceAtLeast(0L)))
}

private fun eta(millis: Long): String {
    val seconds = (millis + 999) / 1000
    return when {
        seconds < 60 -> ui("crafts.eta_seconds", seconds)
        seconds < 3600 -> ui("crafts.eta_minutes", (seconds + 59) / 60)
        else -> ui("crafts.eta_hours", seconds / 3600, seconds % 3600 / 60)
    }
}

/**
 * The crafts tab (since 2.41.0, server 0.37.0): the work under way on a plaque over the tiles, one
 * tile per profession, and a profession's window behind each. The work runs on the server by time;
 * this screen asks when it opens, throws each cycle here the moment it ends (2.47.0) and asks the
 * server behind it, and runs the cycle's bar smoothly on the device's clock set by the server's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CraftsScreen(s: ForgeState, vm: ForgeViewModel) {
    // A screen already holding the crafts asks again in silence: the cycle's alarm is the view model's (2.56.1).
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) { vm.ensureHero(); vm.loadCrafts(silent = s.play.crafts != null) }
    val crafts = s.play.crafts
    val offset = crafts?.let { it.now - s.play.craftsAt } ?: 0L
    val open = crafts?.professions?.firstOrNull { it.code == s.play.craftsProfession }
    if (open != null) { ProfessionWindow(s, vm, open, offset); return }
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.CRAFTS), onRefresh = vm::loadCrafts, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader(ui("crafts.title"), ui("crafts.subtitle"), ForgeGlyphs.Anvil, guide = Guide.CRAFTS) }
            if (crafts == null) { item { InfoCard(ui("common.loading"), ui("crafts.loading_hint")) }; return@LazyColumn }
            item { WorkPlaque(s, vm, offset) }
            // Gathering over crafting, with the materials flowing from one into the other (the owner's pick of five mockups, 2.44.0).
            val (crafting, gathering) = crafts.professions.partition { it.crafting }
            listOf("crafts.section_gather" to gathering, "crafts.section_craft" to crafting).filter { it.second.isNotEmpty() }.forEachIndexed { index, (title, group) ->
                if (index > 0) item { FlowMark() }
                item { SectionRule(ui(title)) }
                items(group.chunked(TILES)) { row ->
                    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { ProfessionTile(s, it, working = crafts.work?.profession == it.code, Modifier.weight(1f).fillMaxHeight()) { vm.openProfession(it.code) } }
                        repeat(TILES - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            // The rules travel with the answer; an answer without them says nothing about the offline cap.
            crafts.rules?.let { rules -> item { MutedText(ui("crafts.note", number(rules.offlineHours))) } }
        }
    }
}

/**
 * The work under way, on every screen of the tab: which, how far the cycle is, and a stop — and
 * since 2.75.0 (server 0.66.0), in its own frame, what the work has come to since it started: how
 * long it runs, its cycles and experience, what it made, and every stack gathered and spent.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun WorkPlaque(s: ForgeState, vm: ForgeViewModel, offset: Long) {
    val work = s.play.crafts?.work
    ForgePanel {
        if (work == null) { MutedText(ui("crafts.idle"), style = MaterialTheme.typography.bodyMedium); return@ForgePanel }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workTitle(work), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(ui("crafts.work_line", professionTitle(work.profession), number(work.cycleMillis / 1000.0)), color = Rune, style = MaterialTheme.typography.labelMedium)
            }
            ForgeOutlinedButton(enabled = !s.busy, onClick = vm::stopWork) { Text(ui("crafts.stop")) }
        }
        CycleBar(work.settledAt, work.cycleMillis, offset, hourly = hourlyLine(s, work))
        levelLine(s, work, offset)?.let { Text(it, color = Vital, style = MaterialTheme.typography.labelMedium) }
        // The server's tally with the cycles this device threw ahead of its count: the rules' own sum.
        WorkTotals(work.startedAt, work.totals + s.play.craftsPending, offset)
        s.play.craftsLast?.let { Text(gainsLine(it), color = Parchment, style = MaterialTheme.typography.bodySmall) }
        s.play.crafts?.running?.let { stockLine(s, work, it) }?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
    }
}

/**
 * What the work has come to since it started, framed inside its plaque: four figures in a row —
 * the time it runs (ticking by the server's clock), cycles, experience, pieces made — and under
 * them a chip per stack, gathered in green and spent in red.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun WorkTotals(startedAt: Long, totals: WorkTally, offset: Long) {
    val shape = RoundedCornerShape(4.dp)
    Column(Modifier.fillMaxWidth().background(Abyss, shape).border(1.dp, Bronze.copy(alpha = .6f), shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("crafts.totals_title"), color = Gold, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (startedAt > 0) {
                val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); kotlinx.coroutines.delay(1_000) } }
                TotalFigure(ui("crafts.totals_time"), duration(now + offset - startedAt), Modifier.weight(1f))
            }
            TotalFigure(ui("crafts.totals_cycles"), if (totals.nothing > 0) "${totals.cycles} (−${totals.nothing})" else totals.cycles.toString(), Modifier.weight(1f))
            TotalFigure(ui("crafts.totals_experience"), "+" + number(totals.experience) + (if (totals.levels > 0) " ▲${totals.levels}" else ""), Modifier.weight(1f))
            if (totals.made > 0) TotalFigure(ui("crafts.totals_made"), totals.made.toString(), Modifier.weight(1f))
        }
        if (totals.items.isEmpty() && totals.spent.isEmpty()) MutedText(ui("crafts.totals_empty"), style = MaterialTheme.typography.labelSmall)
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            totals.items.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "+$amount", Vital) }
            totals.spent.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "−$amount", LifeRed) }
        }
    }
}

@Composable private fun TotalFigure(title: String, figure: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(figure, color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1)
        Text(title, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** How long a work runs: «2 ч 05 мин», «14 мин 03 с», «42 с». */
private fun duration(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return when {
        seconds >= 3600 -> ui("crafts.duration_hours", seconds / 3600, "%02d".format(seconds % 3600 / 60))
        seconds >= 60 -> ui("crafts.duration_minutes", seconds / 60, "%02d".format(seconds % 60))
        else -> ui("crafts.duration_seconds", seconds)
    }
}

/**
 * What this session's crafting brought, summed (2.47.0): one line of cycles, the empty ones and the
 * experience, then a chip per stack — gathered in green, spent in red — and the pieces made.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun SessionTally(totals: WorkGains) {
    if (totals.cycles == 0) { MutedText(ui("crafts.session_empty"), style = MaterialTheme.typography.labelMedium); return }
    MutedText(if (totals.nothing > 0) ui("crafts.session_line", totals.cycles, totals.nothing, number(totals.experience))
        else ui("crafts.session_line_sure", totals.cycles, number(totals.experience)), style = MaterialTheme.typography.labelMedium)
    if (totals.items.isNotEmpty() || totals.spent.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        totals.items.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "+$amount", Vital) }
        totals.spent.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(itemTitle(code), "−$amount", LifeRed) }
    }
    if (totals.equipment.isNotEmpty()) Text(ui("crafts.made", totals.equipment.joinToString { equipmentTitle(it.template) }),
        color = Parchment, style = MaterialTheme.typography.bodySmall)
    if (totals.starved) Text(ui("crafts.starved"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
}

@Composable private fun TallyChip(title: String, figure: String, tone: Color) {
    val shape = RoundedCornerShape(2.dp)
    Row(Modifier.background(Abyss, shape).border(1.dp, tone.copy(alpha = .5f), shape).padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(figure, color = tone, style = MaterialTheme.typography.labelMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(title, color = Parchment, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** An engraved caption with a bronze rule running out of it, heading a group of tiles. */
@Composable private fun SectionRule(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Engraved(title)
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Bronze, Color.Transparent))))
    }
}

/** Between gathering and crafting: what the first brings is what the second spends. */
@Composable private fun FlowMark() {
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
@Composable private fun CycleBar(settledAt: Long, cycleMillis: Long, offset: Long, height: Int = 6, hourly: String? = null) {
    val now by produceState(System.currentTimeMillis()) { while (true) withFrameMillis { value = System.currentTimeMillis() } }
    LinearProgressIndicator(progress = { if (cycleMillis > 0) ((now + offset - settledAt).toFloat() / cycleMillis).coerceIn(0f, 1f) else 0f },
        modifier = Modifier.fillMaxWidth().height(height.dp), color = Gold, trackColor = PanelRaised)
    CycleClock(settledAt, cycleMillis, offset, Modifier.fillMaxWidth())
    hourly?.let { Text(it, color = Vital, style = MaterialTheme.typography.labelSmall) }
}

/** The running cycle as figures — «12.4 / 30 s» — a few times a second, so the text alone is redrawn, not the bar's frame. */
@Composable private fun CycleClock(settledAt: Long, cycleMillis: Long, offset: Long, modifier: Modifier) {
    val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(CLOCK_TICK) } }
    val elapsed = (now + offset - settledAt).coerceIn(0L, cycleMillis.coerceAtLeast(0L))
    MutedText(ui("crafts.cycle_progress", fineNumber(elapsed / 1000.0), number(cycleMillis / 1000.0)), modifier, style = MaterialTheme.typography.labelSmall)
}

/** How often the cycle's figures are redrawn. */
private const val CLOCK_TICK = 200L

/**
 * What the work under way brings in an hour on average (3.24.0), counted from the job as the server
 * sends it — the cycle, the «nothing» chance and each find's chance already made of the hero's gear —
 * and the yield bonus's extra units: the cycles an hour holds, less the empty ones, times what one brings.
 * Display only: the server's dice decide.
 */
private fun hourlyLine(s: ForgeState, work: WorkView): String? {
    val profession = s.play.crafts?.professions?.firstOrNull { it.code == work.profession } ?: return null
    val job = profession.job(work.job, work.choice) ?: return null
    if (work.cycleMillis <= 0) return null
    val landed = landed(work.cycleMillis, job)
    val made = jobProduct(job) to landed * yieldFactor(profession)
    val finds = job.extra.map { itemTitle(it.item) to landed * it.chance / 100 }
    val figures = (listOf(made) + finds).filter { it.second > 0 }.takeIf { it.isNotEmpty() } ?: return null
    return ui("crafts.per_hour", figures.joinToString(" · ") { (name, amount) -> ui("crafts.per_hour_item", fineNumber(amount), name) })
}

/** The cycles an hour of [cycleMillis] brings that do not come back empty. */
private fun landed(cycleMillis: Long, job: JobView): Double = HOUR_MILLIS / cycleMillis.toDouble() * (1 - job.nothing / 100).coerceAtLeast(0.0)

/** The profession's chance to double a cycle's yield, as the factor it multiplies the average by. */
private fun yieldFactor(profession: ProfessionView): Double = 1 + profession.bonus.yield.coerceAtLeast(0.0) / 100

/** About how many of its product a work brings an hour, by its own cycle; null for one that has no cycle yet. */
private fun perHour(job: JobView, profession: ProfessionView): Double? =
    job.cycleMillis.takeIf { it > 0 }?.let { landed(it, job) * yieldFactor(profession) }

private const val HOUR_MILLIS = 3_600_000.0

/** A profession as a tile, three to a row: its tool in a medallion, name, level, how far to the next one, and what stands out. */
@Composable private fun ProfessionTile(s: ForgeState, profession: ProfessionView, working: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    val toolless = profession.equipped == null
    Column(modifier.background(Panel, shape).border(if (working) 2.dp else 1.dp, if (working) GoldBright else PanelRaised, shape)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Medallion(if (toolless) LifeRed else Bronze) { ToolIcon(s, profession, 26) }
        Box(Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
            Text(professionTitle(profession.code), color = GoldBright, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(ui("crafts.level", profession.level), color = Rune, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(progress = { share(profession) }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Vital, trackColor = PanelRaised)
        when {
            working -> Text(ui("crafts.working"), color = Gold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            toolless -> Text(ui("crafts.no_tool_short"), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

/** A square stood on its corner, as the tree's nodes are, holding a drawing upright. */
@Composable private fun Medallion(frame: Color, content: @Composable () -> Unit) {
    Box(Modifier.padding(6.dp).size(40.dp).rotate(45f)
        .background(Brush.radialGradient(listOf(PanelRaised, Ink)), RoundedCornerShape(2.dp)).border(1.dp, frame, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center) { Box(Modifier.rotate(-45f)) { content() } }
}

private fun share(profession: ProfessionView): Float = profession.next?.takeIf { it > 0 }?.let { (profession.experience / it).toFloat().coerceIn(0f, 1f) } ?: 1f

/** The tool in the slot, drawn as the cards draw it — the server's outline tinted by rarity, the bundled emblem behind it — or the anvil for an empty slot. */
@Composable private fun ToolIcon(s: ForgeState, profession: ProfessionView, size: Int) {
    val modifier = Modifier.size(size.dp)
    val tool = profession.equipped?.let { s.view(it) }
    if (tool == null) { Icon(ForgeGlyphs.Anvil, null, tint = Muted, modifier = modifier); return }
    val paint = rarityColor(tool.rarity.name)
    if (!SpriteIcon(equipmentIcon(tool.code), paint, modifier)) ItemEmblem(tool.visualKind, paint, modifier)
}

/**
 * A profession's window: its level, the tool in its slot and what the tool and the tree give, the
 * work under way here with what this session brought, and every work with the numbers the server
 * worked out for this hero — a locked one says the level it wants.
 */
@Composable private fun ProfessionWindow(s: ForgeState, vm: ForgeViewModel, profession: ProfessionView, offset: Long) {
    BackHandler { vm.openProfession("") }
    var picking by remember { mutableStateOf(false) }
    var toolOpen by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<JobView?>(null) }
    val work = s.play.crafts?.work?.takeIf { it.profession == profession.code }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.openProfession("") }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), tint = Gold) }
                Text(professionTitle(profession.code), color = GoldBright, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                ToolButton(s, profession) { toolOpen = true }
            }
        }
        item {
            ForgePanel {
                MutedText(professionDescription(profession.code))
                Text(profession.next?.let { ui("crafts.level_progress", profession.level, number(profession.experience), number(it)) } ?: ui("crafts.level_last", profession.level),
                    color = Rune, style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { share(profession) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Vital, trackColor = PanelRaised)
            }
        }
        if (work != null) item {
            ForgePanel(accent = GoldBright) {
                Engraved(ui("crafts.now", workTitle(work)))
                CycleBar(work.settledAt, work.cycleMillis, offset, height = 10, hourly = hourlyLine(s, work))
                SessionTally(s.play.craftsTotals)
            }
        }
        item { Engraved(ui("crafts.works")) }
        // Open works, and the one after them as «???» with its level (2.48.0, as the expedition does).
        val locked = { job: JobView -> job.level > profession.level || !job.open }
        val next = profession.jobs.firstOrNull(locked)
        items(profession.jobs.filter { !locked(it) || it == next }, key = { it.code }) { job ->
            JobRow(s, profession, job, locked = locked(job), current = work?.job == job.code) { if (!locked(job)) chosen = job }
        }
    }
    chosen?.let { job -> JobSheet(s, vm, profession, job, current = work?.job == job.code) { chosen = null } }
    if (toolOpen) ToolSheet(s, profession, onDismiss = { toolOpen = false }) { toolOpen = false; picking = true }
    if (picking) ToolPicker(s, profession, onDismiss = { picking = false }) { id -> picking = false; vm.equipTool(id) }
}

/** The tool in the window's header: its icon in a frame of its rarity, or an empty «+» cell for none. */
@Composable private fun ToolButton(s: ForgeState, profession: ProfessionView, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val view = profession.equipped?.let { s.view(it) }
    val frame = when {
        profession.equipped == null -> LifeRed
        view != null -> rarityColor(view.rarity.name)
        else -> Bronze
    }
    Box(Modifier.size(44.dp).background(Abyss, shape).border(1.dp, frame, shape)
        .clickable(role = Role.Button, onClickLabel = ui("crafts.tool"), onClick = onClick), contentAlignment = Alignment.Center) {
        if (profession.equipped == null) Icon(Icons.Outlined.Add, ui("crafts.tool"), tint = LifeRed, modifier = Modifier.size(22.dp))
        else ToolIcon(s, profession, 30)
    }
}

/** The tool behind the header's button: its full card, what it and the tree give, and the way to another. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ToolSheet(s: ForgeState, profession: ProfessionView, onDismiss: () -> Unit, onChange: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Engraved(ui("crafts.tool"))
            val tool = profession.equipped
            val view = tool?.let { s.view(it) }
            when {
                tool == null -> Text(ui("crafts.no_tool"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                view != null -> ItemCard(view, detailed = true, price = s.sellPrice(view.item))
                // A tool whose template the content does not hold yet: its name, and nothing to open.
                else -> Text(equipmentTitle(tool.template), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            }
            BonusChips(profession)
            ForgeButton(enabled = !s.busy, onClick = onChange, modifier = Modifier.fillMaxWidth()) { Text(ui("crafts.change_tool")) }
        }
    }
}

/** What the tool and the tree give this profession, a chip per figure that is not zero. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun BonusChips(profession: ProfessionView) {
    val bonus = profession.bonus
    val figures = listOf("crafts.bonus_speed" to bonus.speed, "crafts.bonus_yield" to bonus.yield, "crafts.bonus_luck" to bonus.luck,
        "crafts.bonus_experience" to bonus.experience, "crafts.bonus_find" to bonus.find).filter { it.second != 0.0 }
    if (figures.isEmpty()) { MutedText(ui("crafts.bonus_none")); return }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        figures.forEach { (key, value) ->
            Text(ui(key, number(value)), color = Parchment, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(PanelRaised, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}

/**
 * A work as a card: what it makes under its name, then three figures to compare works by — about how much an hour
 * brings, how long a cycle runs, and how a cycle may turn out (doubled, in vain, or sure) — and at the foot what a
 * cycle costs against the bag and what it teaches. A work the profession has not reached is a dimmed «???» with its level.
 */
@Composable private fun JobRow(s: ForgeState, profession: ProfessionView, job: JobView, locked: Boolean, current: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(Modifier.fillMaxWidth().alpha(if (locked) .5f else 1f).background(Panel, shape).border(if (current) 2.dp else 1.dp, if (current) GoldBright else PanelRaised, shape)
        .clickable(role = Role.Button, onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(Abyss, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                if (locked) Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(20.dp)) else JobIcon(job, Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(if (locked) ui("expedition.hidden") else jobTitle(job.code), color = if (locked) Muted else GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(if (locked) ui("crafts.opens_at", job.level) else "→ ${jobProduct(job)}", style = MaterialTheme.typography.labelMedium)
            }
            if (current) Text(ui("crafts.running"), color = Ink, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.background(Gold, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        if (locked) return@Column
        val double = profession.bonus.yield.coerceAtLeast(0.0)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val cell = Modifier.weight(1f)
            JobMetric(perHour(job, profession)?.let { ui("crafts.about", fineNumber(it)) } ?: "—", ui("crafts.metric_hour"), Parchment, cell)
            JobMetric(ui("crafts.duration_seconds", number(job.cycleMillis / 1000.0)), ui("crafts.metric_cycle"), Parchment, cell)
            when {
                double > 0 -> JobMetric(ui("crafts.percent", number(double)), ui("crafts.metric_double"), Vital, cell)
                job.nothing > 0 -> JobMetric(ui("crafts.percent", number(job.nothing)), ui("crafts.metric_waste"), LifeRed, cell)
                else -> JobMetric("✓", ui("crafts.metric_sure"), Vital, cell)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(cycleCost(s, job), style = MaterialTheme.typography.labelMedium, color = Muted, modifier = Modifier.weight(1f))
            Text(ui("crafts.experience_gain", number(job.experience)), color = Rune, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A figure of a work's card: the number large, what it is small under it. */
@Composable private fun JobMetric(value: String, label: String, tint: Color, modifier: Modifier) {
    Column(modifier.background(Abyss, RoundedCornerShape(6.dp)).padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        MutedText(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** What a work makes, drawn: a stack's glass, an equipment's sprite, or the glyph of the kind when it makes something unnamed. */
@Composable private fun JobIcon(job: JobView, modifier: Modifier) {
    when (job.kind) {
        JobKind.ITEM, JobKind.CONDENSE, JobKind.REFINE -> if (job.output.isNotBlank()) BagIcon(job.output, modifier) else GlyphIcon(Glyph.CRAFT, Gold, modifier)
        JobKind.FLASK, JobKind.EQUIPMENT, JobKind.JEWEL -> if (!SpriteIcon(equipmentIcon(job.output), Gold, modifier, halo = false)) GlyphIcon(Glyph.ITEM, Gold, modifier)
        JobKind.MAP -> GlyphIcon(Glyph.MAP, Gold, modifier)
        JobKind.BOOK -> GlyphIcon(Glyph.TEXT, Gold, modifier)
    }
}

/** A cycle's price against the bag: each material as have/need, short ones in red; «бесплатно» for a work that spends nothing. */
private fun cycleCost(s: ForgeState, job: JobView): AnnotatedString = buildAnnotatedString {
    if (job.inputs.isEmpty()) { append(ui("crafts.free")); return@buildAnnotatedString }
    job.inputs.forEachIndexed { i, input ->
        if (i > 0) append(" + ")
        val have = bagCount(s, input.item)
        withStyle(SpanStyle(color = if (have >= input.amount) Parchment else LifeRed)) {
            append("${itemTitle(input.item)} ${ui("crafts.ratio", have, input.amount)}")
        }
    }
}

/** A work, opened: what it brings, how long, how often in vain, what it teaches and finds on the side — and its button. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun JobSheet(s: ForgeState, vm: ForgeViewModel, profession: ProfessionView, work: JobView, current: Boolean, onDismiss: () -> Unit) {
    val crafts = s.play.crafts
    var additives by remember(work.code) { mutableStateOf(emptyList<String>()) }
    // A choosing work (3.45.0) is started as one of its variants: the sheet shows the one picked.
    val choices = choices(s, profession, work)
    var picked by remember(work.code) { mutableStateOf(choices.firstOrNull()?.choice.orEmpty()) }
    val job = work.options.firstOrNull { it.choice == picked } ?: work
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(jobTitle(job.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Text(professionTitle(profession.code), color = Rune, style = MaterialTheme.typography.labelMedium)
            if (work.options.isNotEmpty()) {
                Engraved(ui("crafts.choose"))
                if (choices.isEmpty()) MutedText(ui("crafts.no_choice"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    choices.forEach { option ->
                        FilterChip(selected = option.choice == picked, onClick = { picked = option.choice }, label = { Text(choiceTitle(option.choice)) })
                    }
                }
            }
            PropertyRow(ui("crafts.output"), jobProduct(job), Glyph.ITEM)
            if (job.inputs.isNotEmpty()) {
                Engraved(ui("crafts.inputs"))
                job.inputs.forEach { input ->
                    val have = bagCount(s, input.item)
                    PropertyRow(itemTitle(input.item), ui("crafts.have", have, input.amount), Glyph.CRAFT)
                }
            }
            // The smith's additives (2.42.0): each is spent every smelt and guarantees its handcrafted line.
            if (job.additives && crafts != null && crafts.maxAdditives > 0) {
                Engraved(ui("crafts.additives", crafts.maxAdditives))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    crafts.additives.keys.forEach { code ->
                        val picked = code in additives
                        FilterChip(selected = picked, enabled = picked || (bagCount(s, code) > 0 && additives.size < crafts.maxAdditives),
                            onClick = { additives = if (picked) additives - code else additives + code },
                            label = { Text(ui("crafts.gain", bagCount(s, code), itemTitle(code)).removePrefix("+")) })
                    }
                }
            }
            PropertyRow(ui("crafts.cycle_label"), ui("crafts.seconds", number(job.cycleMillis / 1000.0), number(job.seconds)), Glyph.SPEED)
            if (job.nothing > 0) PropertyRow(ui("crafts.nothing"), ui("crafts.percent", number(job.nothing)), Glyph.INFO)
            PropertyRow(ui("crafts.experience"), number(job.experience), Glyph.LEVEL)
            job.extra.forEach { PropertyRow(ui("crafts.find", itemTitle(it.item)), ui("crafts.percent", number(it.chance)), Glyph.ITEM) }
            Spacer(Modifier.height(4.dp))
            when {
                job.level > profession.level -> Text(ui("crafts.needs_level", job.level), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                job.kind.chosen -> Text(ui("crafts.no_choice"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                !job.open -> Text(ui("crafts.locked_map"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                current -> ForgeOutlinedButton(enabled = !s.busy, onClick = { onDismiss(); vm.stopWork() }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.stop")) }
                // A cycle the bag cannot feed is not started (2.46.0): the chips above say what is short.
                job.inputs.any { bagCount(s, it.item) < it.amount } -> Text(ui("crafts.short_inputs"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                else -> ForgeButton(enabled = !s.busy, onClick = { onDismiss(); vm.startWork(job.code, job.choice, additives) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.start")) }
            }
            if (profession.equipped == null && job.level <= profession.level) Text(ui("crafts.no_tool"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The stash's tools of this profession — the copies whose template sits in the profession's tool slot; tapping one puts it in the slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ToolPicker(s: ForgeState, profession: ProfessionView, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val slot = Slot.of(profession.tool)
    val tools = s.hero?.stash.orEmpty().filter { !it.socketed }
        .mapNotNull { instance -> s.view(instance)?.takeIf { slot != null && it.slot == slot }?.let { instance to it } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.7f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("crafts.pick_tool")) }
            if (tools.isEmpty()) item { InfoCard(ui("crafts.no_tools"), ui("crafts.no_tools_hint")) }
            items(tools, key = { it.first.id }) { (instance, view) ->
                ItemRow(view, enabled = !s.busy, unwearable = s.unmetFor(instance.template), price = s.sellPrice(instance)) { onPick(instance.id) }
            }
        }
    }
}

private const val TILES = 3

/**
 * The variants a choosing work offers now: those the profession's level reaches — and, for the condensing,
 * only the essences the bag can feed a cycle of, else a hundred and forty chips would bury the few that can run.
 */
private fun choices(s: ForgeState, profession: ProfessionView, work: JobView): List<JobView> =
    work.options.filter { option -> option.level <= profession.level && (work.kind != JobKind.CONDENSE || option.inputs.all { bagCount(s, it.item) >= it.amount }) }
