package com.sperance.exileforge.ui.screens.crafts

import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.sellPrice
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobInput
import com.sperance.exileforge.core.model.crafts.JobKind
import com.sperance.exileforge.core.model.crafts.JobView
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkGains
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay

fun professionTitle(code: String) = locOr(LocaleKey.professionName(code), displayName(code))
fun jobTitle(code: String) = locOr(LocaleKey.jobName(code), displayName(code))
fun materialTitle(code: String) = locOr(LocaleKey.itemName(code), displayName(code))

/**
 * What an answer brought, as one line: «+2 Iron Ore, +1 Bark», the pieces a smith or a cartographer
 * made, that the bag ran dry, or that the cycles came up empty.
 */
fun gainsLine(s: ForgeState, gains: WorkGains): String = listOfNotNull(
    gains.items.entries.joinToString { (code, amount) -> ui("crafts.gain", amount, materialTitle(code)) }.ifBlank { null },
    gains.equipment.takeIf { it.isNotEmpty() }?.let { made -> ui("crafts.made", made.joinToString { inventoryDocument(it, s.world.inventoryBases[it.equipmentId]).text("name") }) },
    ui("crafts.starved").takeIf { gains.starved },
).joinToString(" · ").ifBlank { ui("crafts.gain_nothing", gains.cycles) }

/** How many of a stack the bag holds, by the item's code — a material or an orb. */
fun bagCount(s: ForgeState, code: String): Long {
    val id = s.world.materials.firstOrNull { it.code == code }?.id ?: s.world.orbs.firstOrNull { it.code == code }?.id ?: return 0
    return s.play.hero?.bag?.firstOrNull { it.itemId == id }?.amount ?: 0
}

/** What a work makes, in words: the stack, the smith's range or the cartographer's location. */
fun jobProduct(job: JobView): String = when (job.kind) {
    JobKind.ITEM -> materialTitle(job.output)
    JobKind.EQUIPMENT -> ui("crafts.kind_equipment", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })
    JobKind.MAP -> ui("crafts.kind_map", com.sperance.exileforge.core.campaign.mapTitle(job.map))
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
    return if (cycles == 0) ui("crafts.stock_empty", materialTitle(scarce.item), have)
    else ui("crafts.stock", materialTitle(scarce.item), have, cycles, plural("crafts.cycles", cycles), eta(cycles * work.cycleMillis))
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
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero(); vm.loadCrafts() }
    val crafts = s.play.crafts
    val offset = crafts?.let { it.now - s.play.craftsAt } ?: 0L
    // The next cycle is due (2.47.0): it is thrown here and paid into the bag at once, and the server is asked behind it.
    LaunchedEffect(crafts?.work?.nextAt, s.play.craftsAt) {
        val next = crafts?.work?.nextAt ?: return@LaunchedEffect
        delay((next - offset - System.currentTimeMillis()).coerceAtLeast(0))
        vm.craftsCycleDue()
    }
    val open = crafts?.professions?.firstOrNull { it.code == s.play.craftsProfession }
    if (open != null) { ProfessionWindow(s, vm, open, offset); return }
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.CRAFTS), onRefresh = vm::loadCrafts, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader(ui("crafts.title"), ui("crafts.subtitle"), ForgeGlyphs.Anvil) }
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
            item { Text(ui("crafts.note", number(crafts.rules.offlineHours)), color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/** The work under way, on every screen of the tab: which, how far the cycle is, what it last brought, and a stop. */
@Composable private fun WorkPlaque(s: ForgeState, vm: ForgeViewModel, offset: Long) {
    val work = s.play.crafts?.work
    ForgePanel {
        if (work == null) { Text(ui("crafts.idle"), color = Muted, style = MaterialTheme.typography.bodyMedium); return@ForgePanel }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(jobTitle(work.job), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(ui("crafts.work_line", professionTitle(work.profession), number(work.cycleMillis / 1000.0)), color = Rune, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(enabled = !s.busy, onClick = vm::stopWork) { Text(ui("crafts.stop")) }
        }
        CycleBar(work.settledAt, work.cycleMillis, offset, caption = false)
        s.play.craftsLast?.let { Text(gainsLine(s, it), color = Parchment, style = MaterialTheme.typography.bodySmall) }
        s.play.crafts?.professions?.firstOrNull { it.code == work.profession }?.jobs?.firstOrNull { it.code == work.job }
            ?.let { stockLine(s, work, it) }?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
    }
}

/**
 * What this session's crafting brought, summed (2.47.0): one line of cycles, the empty ones and the
 * experience, then a chip per stack — gathered in green, spent in red — and the pieces made.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun SessionTally(s: ForgeState, totals: WorkGains) {
    if (totals.cycles == 0) { Text(ui("crafts.session_empty"), color = Muted, style = MaterialTheme.typography.labelMedium); return }
    Text(ui("crafts.session_line", totals.cycles, totals.nothing, number(totals.experience)), color = Muted, style = MaterialTheme.typography.labelMedium)
    if (totals.items.isNotEmpty() || totals.spent.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        totals.items.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(materialTitle(code), "+$amount", Vital) }
        totals.spent.entries.sortedByDescending { it.value }.forEach { (code, amount) -> TallyChip(materialTitle(code), "−$amount", LifeRed) }
    }
    if (totals.equipment.isNotEmpty()) Text(ui("crafts.made", totals.equipment.joinToString { inventoryDocument(it, s.world.inventoryBases[it.equipmentId]).text("name") }),
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
 */
@Composable private fun CycleBar(settledAt: Long, cycleMillis: Long, offset: Long, height: Int = 6, caption: Boolean = true) {
    val now by produceState(System.currentTimeMillis()) { while (true) withFrameMillis { value = System.currentTimeMillis() } }
    LinearProgressIndicator(progress = { if (cycleMillis > 0) ((now + offset - settledAt).toFloat() / cycleMillis).coerceIn(0f, 1f) else 0f },
        modifier = Modifier.fillMaxWidth().height(height.dp), color = Gold, trackColor = PanelRaised)
    if (caption) Text(ui("crafts.cycle", number(cycleMillis / 1000.0)), color = Muted, style = MaterialTheme.typography.labelSmall)
}

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
        .background(Brush.radialGradient(listOf(PanelRaised, Ink)), CutCornerShape(2.dp)).border(1.dp, frame, CutCornerShape(2.dp)),
        contentAlignment = Alignment.Center) { Box(Modifier.rotate(-45f)) { content() } }
}

private fun share(profession: ProfessionView): Float = profession.next?.takeIf { it > 0 }?.let { (profession.experience / it).toFloat().coerceIn(0f, 1f) } ?: 1f

@Composable private fun ToolIcon(s: ForgeState, profession: ProfessionView, size: Int) {
    val tool = profession.equipped
    if (tool != null) ItemIcon(inventoryDocument(tool, s.world.inventoryBases[tool.equipmentId]), rarityColor(tool.rarity), Modifier.size(size.dp))
    else Icon(ForgeGlyphs.Anvil, null, tint = Muted, modifier = Modifier.size(size.dp))
}

/**
 * A profession's window: its level, the tool in its slot and what the tool and the tree give, the
 * work under way here with what this session brought, and every work with the numbers the server
 * worked out for this hero — a locked one says the level it wants.
 */
@Composable private fun ProfessionWindow(s: ForgeState, vm: ForgeViewModel, profession: ProfessionView, offset: Long) {
    BackHandler { vm.openProfession("") }
    var picking by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<JobView?>(null) }
    val work = s.play.crafts?.work?.takeIf { it.profession == profession.code }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.openProfession("") }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), tint = Gold) }
                Text(professionTitle(profession.code), color = GoldBright, style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            ForgePanel {
                Text(locOr(LocaleKey.professionDescription(profession.code), ""), color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(profession.next?.let { ui("crafts.level_progress", profession.level, number(profession.experience), number(it)) } ?: ui("crafts.level_last", profession.level),
                    color = Rune, style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { share(profession) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Vital, trackColor = PanelRaised)
            }
        }
        item {
            ForgePanel {
                Engraved(ui("crafts.tool"))
                val tool = profession.equipped
                if (tool == null) Text(ui("crafts.no_tool"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                else ItemRow(inventoryDocument(tool, s.world.inventoryBases[tool.equipmentId]), definitions = s.world.definitions, enabled = !s.busy, price = s.sellPrice(tool)) { picking = true }
                OutlinedButton(enabled = !s.busy, onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("crafts.change_tool")) }
                BonusChips(profession)
            }
        }
        if (work != null) item {
            ForgePanel(accent = GoldBright) {
                Engraved(ui("crafts.now", jobTitle(work.job)))
                CycleBar(work.settledAt, work.cycleMillis, offset, height = 10)
                SessionTally(s, s.play.craftsTotals)
            }
        }
        item { Engraved(ui("crafts.works")) }
        items(profession.jobs, key = { it.code }) { job ->
            JobRow(s, job, locked = job.level > profession.level || !job.open, current = work?.job == job.code) { chosen = job }
        }
    }
    chosen?.let { job -> JobSheet(s, vm, profession, job, current = work?.job == job.code) { chosen = null } }
    if (picking) ToolPicker(s, profession, onDismiss = { picking = false }) { id -> picking = false; vm.equipTool(id) }
}

/** What the tool and the tree give this profession, a chip per figure that is not zero. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun BonusChips(profession: ProfessionView) {
    val bonus = profession.bonus
    val figures = listOf("crafts.bonus_speed" to bonus.speed, "crafts.bonus_yield" to bonus.yield, "crafts.bonus_luck" to bonus.luck,
        "crafts.bonus_experience" to bonus.experience, "crafts.bonus_find" to bonus.find).filter { it.second != 0.0 }
    if (figures.isEmpty()) { Text(ui("crafts.bonus_none"), color = Muted, style = MaterialTheme.typography.bodySmall); return }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        figures.forEach { (key, value) ->
            Text(ui(key, number(value)), color = Parchment, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(PanelRaised, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}

/** A material a cycle spends against what the bag holds: green while one cycle is paid for, red when it is not. */
@Composable private fun InputChip(s: ForgeState, input: JobInput) {
    val have = bagCount(s, input.item)
    val tone = if (have >= input.amount) Vital else LifeRed
    val shape = RoundedCornerShape(2.dp)
    Row(Modifier.background(Panel, shape).border(1.dp, tone.copy(alpha = .6f), shape).padding(horizontal = 7.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(materialTitle(input.item), color = Parchment, style = MaterialTheme.typography.labelSmall)
        Text(ui("crafts.ratio", have, input.amount), color = tone, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun JobRow(s: ForgeState, job: JobView, locked: Boolean, current: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Row(Modifier.fillMaxWidth().alpha(if (locked) .5f else 1f).background(Panel, shape).border(if (current) 2.dp else 1.dp, if (current) GoldBright else PanelRaised, shape)
        .clickable(role = Role.Button, onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(jobTitle(job.code), color = if (locked) Muted else GoldBright, style = MaterialTheme.typography.titleSmall)
            Text(ui("crafts.job_line", jobProduct(job), number(job.cycleMillis / 1000.0), number(job.nothing)), color = Muted, style = MaterialTheme.typography.bodySmall)
            if (job.inputs.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                job.inputs.forEach { InputChip(s, it) }
            }
        }
        if (locked) Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(16.dp))
            Text(ui("crafts.level", job.level), color = Muted, style = MaterialTheme.typography.labelMedium)
        } else if (current) Text(ui("crafts.working"), color = Gold, style = MaterialTheme.typography.labelMedium)
    }
}

/** A work, opened: what it brings, how long, how often in vain, what it teaches and finds on the side — and its button. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun JobSheet(s: ForgeState, vm: ForgeViewModel, profession: ProfessionView, job: JobView, current: Boolean, onDismiss: () -> Unit) {
    val crafts = s.play.crafts
    var additives by remember(job.code) { mutableStateOf(emptyList<String>()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(jobTitle(job.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Text(professionTitle(profession.code), color = Rune, style = MaterialTheme.typography.labelMedium)
            PropertyRow(ui("crafts.output"), jobProduct(job), com.sperance.exileforge.core.display.Glyph.ITEM)
            if (job.inputs.isNotEmpty()) {
                Engraved(ui("crafts.inputs"))
                job.inputs.forEach { input ->
                    val have = bagCount(s, input.item)
                    PropertyRow(materialTitle(input.item), ui("crafts.have", have, input.amount), com.sperance.exileforge.core.display.Glyph.CRAFT)
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
                            label = { Text(ui("crafts.gain", bagCount(s, code), materialTitle(code)).removePrefix("+")) })
                    }
                }
            }
            PropertyRow(ui("crafts.cycle_label"), ui("crafts.seconds", number(job.cycleMillis / 1000.0), number(job.seconds)), com.sperance.exileforge.core.display.Glyph.SPEED)
            PropertyRow(ui("crafts.nothing"), ui("crafts.percent", number(job.nothing)), com.sperance.exileforge.core.display.Glyph.INFO)
            PropertyRow(ui("crafts.experience"), number(job.experience), com.sperance.exileforge.core.display.Glyph.LEVEL)
            job.extra.forEach { PropertyRow(ui("crafts.find", materialTitle(it.item)), ui("crafts.percent", number(it.chance)), com.sperance.exileforge.core.display.Glyph.ITEM) }
            Spacer(Modifier.height(4.dp))
            when {
                job.level > profession.level -> Text(ui("crafts.needs_level", job.level), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                !job.open -> Text(ui("crafts.locked_map"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                current -> OutlinedButton(enabled = !s.busy, onClick = { onDismiss(); vm.stopWork() }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.stop")) }
                // A cycle the bag cannot feed is not started (2.46.0): the chips above say what is short.
                job.inputs.any { bagCount(s, it.item) < it.amount } -> Text(ui("crafts.short_inputs"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                else -> Button(enabled = !s.busy, onClick = { onDismiss(); vm.startWork(job.code, additives) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.start")) }
            }
            if (profession.equipped == null && job.level <= profession.level) Text(ui("crafts.no_tool"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The stash's tools of this profession; tapping one puts it in the slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ToolPicker(s: ForgeState, profession: ProfessionView, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val tools = s.play.hero?.inventory.orEmpty().filter { !it.equipped && !it.socketed }
        .map { it to inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
        .filter { (_, document) -> document.text("slot") == profession.tool }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.7f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("crafts.pick_tool")) }
            if (tools.isEmpty()) item { InfoCard(ui("crafts.no_tools"), ui("crafts.no_tools_hint")) }
            items(tools, key = { it.first.id }) { (instance, document) ->
                ItemRow(document, definitions = s.world.definitions, enabled = !s.busy,
                    unwearable = s.unmetFor(instance.equipmentId), price = s.sellPrice(instance)) { onPick(instance.id) }
            }
        }
    }
}

private const val TILES = 3
