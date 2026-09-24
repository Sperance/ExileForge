package com.sperance.exileforge.ui.screens.crafts

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobKind
import com.sperance.exileforge.core.model.crafts.JobView
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkGains
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

/**
 * The crafts tab (since 2.41.0, server 0.37.0): the work under way on a plaque over the tiles, one
 * tile per profession, and a profession's window behind each. The work runs on the server by time;
 * this screen asks when it opens and when the next cycle is due, and runs the cycle's bar between
 * answers on the device's clock set by the server's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CraftsScreen(s: ForgeState, vm: ForgeViewModel) {
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero(); vm.loadCrafts() }
    val crafts = s.play.crafts
    val offset = crafts?.let { it.now - s.play.craftsAt } ?: 0L
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(TICK); clock = System.currentTimeMillis() } }
    // The next cycle is due: ask, and the answer carries it and whatever came before it.
    LaunchedEffect(crafts?.work?.nextAt, s.play.craftsAt) {
        val next = crafts?.work?.nextAt ?: return@LaunchedEffect
        delay((next - offset - System.currentTimeMillis()).coerceAtLeast(0) + SETTLE_GRACE)
        vm.loadCrafts()
    }
    val serverNow = clock + offset
    val open = crafts?.professions?.firstOrNull { it.code == s.play.craftsProfession }
    if (open != null) { ProfessionWindow(s, vm, open, serverNow); return }
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.CRAFTS), onRefresh = vm::loadCrafts, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader(ui("crafts.title"), ui("crafts.subtitle"), ForgeGlyphs.Anvil) }
            if (crafts == null) { item { InfoCard(ui("common.loading"), ui("crafts.loading_hint")) }; return@LazyColumn }
            item { WorkPlaque(s, vm, serverNow) }
            items(crafts.professions.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { ProfessionTile(s, it, working = crafts.work?.profession == it.code, Modifier.weight(1f)) { vm.openProfession(it.code) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { Text(ui("crafts.note", number(crafts.rules.offlineHours)), color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/** The work under way, on every screen of the tab: which, how far the cycle is, what it last brought, and a stop. */
@Composable private fun WorkPlaque(s: ForgeState, vm: ForgeViewModel, serverNow: Long) {
    val work = s.play.crafts?.work
    ForgePanel {
        if (work == null) { Text(ui("crafts.idle"), color = Muted, style = MaterialTheme.typography.bodyMedium); return@ForgePanel }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(jobTitle(work.job), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(professionTitle(work.profession), color = Rune, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(enabled = !s.busy, onClick = vm::stopWork) { Text(ui("crafts.stop")) }
        }
        CycleBar(work.settledAt, work.cycleMillis, serverNow)
        s.play.craftsLog.firstOrNull()?.let { Text(gainsLine(s, it), color = Parchment, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun CycleBar(settledAt: Long, cycleMillis: Long, serverNow: Long, height: Int = 6) {
    val share = if (cycleMillis > 0) ((serverNow - settledAt).toFloat() / cycleMillis).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth().height(height.dp), color = Gold, trackColor = PanelRaised)
    Text(ui("crafts.cycle", number(cycleMillis / 1000.0)), color = Muted, style = MaterialTheme.typography.labelSmall)
}

/** A profession as a tile: its tool's drawing, name, level and how far to the next one. */
@Composable private fun ProfessionTile(s: ForgeState, profession: ProfessionView, working: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.background(Panel, shape).border(if (working) 2.dp else 1.dp, if (working) GoldBright else PanelRaised, shape)
        .clickable(role = Role.Button, onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        ToolIcon(s, profession, 44)
        Text(professionTitle(profession.code), color = GoldBright, style = MaterialTheme.typography.titleSmall)
        Text(ui("crafts.level", profession.level), color = Rune, style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(progress = { share(profession) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Vital, trackColor = PanelRaised)
        if (working) Text(ui("crafts.working"), color = Gold, style = MaterialTheme.typography.labelSmall)
    }
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
@Composable private fun ProfessionWindow(s: ForgeState, vm: ForgeViewModel, profession: ProfessionView, serverNow: Long) {
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
                else ItemRow(inventoryDocument(tool, s.world.inventoryBases[tool.equipmentId]), definitions = s.world.definitions, enabled = !s.busy) { picking = true }
                OutlinedButton(enabled = !s.busy, onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("crafts.change_tool")) }
                Text(ui("crafts.bonus", number(profession.bonus.speed), number(profession.bonus.yield), number(profession.bonus.luck),
                    number(profession.bonus.experience), number(profession.bonus.find)), color = Parchment, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (work != null) item {
            ForgePanel(accent = GoldBright) {
                Engraved(ui("crafts.now", jobTitle(work.job)))
                CycleBar(work.settledAt, work.cycleMillis, serverNow, height = 10)
                val log = s.play.craftsLog
                Text(ui("crafts.session", log.sumOf { it.cycles }, log.sumOf { it.nothing }), color = Muted, style = MaterialTheme.typography.labelMedium)
                log.take(FEED).forEach { Text(gainsLine(s, it), color = Parchment, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { Engraved(ui("crafts.works")) }
        items(profession.jobs, key = { it.code }) { job ->
            JobRow(job, locked = job.level > profession.level || !job.open, current = work?.job == job.code) { chosen = job }
        }
    }
    chosen?.let { job -> JobSheet(s, vm, profession, job, current = work?.job == job.code) { chosen = null } }
    if (picking) ToolPicker(s, profession, onDismiss = { picking = false }) { id -> picking = false; vm.equipTool(id) }
}

@Composable private fun JobRow(job: JobView, locked: Boolean, current: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().background(Panel, shape).border(if (current) 2.dp else 1.dp, if (current) GoldBright else PanelRaised, shape)
        .clickable(role = Role.Button, onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(jobTitle(job.code), color = if (locked) Muted else GoldBright, style = MaterialTheme.typography.titleSmall)
            Text(ui("crafts.job_line", jobProduct(job), number(job.cycleMillis / 1000.0), number(job.nothing)), color = Muted, style = MaterialTheme.typography.bodySmall)
            if (job.inputs.isNotEmpty()) Text(job.inputs.joinToString { ui("crafts.gain", it.amount, materialTitle(it.item)).removePrefix("+") },
                color = Muted, style = MaterialTheme.typography.labelSmall)
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
                    unwearable = s.play.hero?.sheet?.unwearableBy?.get(instance.equipmentId).orEmpty()) { onPick(instance.id) }
            }
        }
    }
}

private const val TICK = 200L
/** The server counts a cycle when its moment has passed; asking a little after it lands the cycle. */
private const val SETTLE_GRACE = 400L
private const val FEED = 8
