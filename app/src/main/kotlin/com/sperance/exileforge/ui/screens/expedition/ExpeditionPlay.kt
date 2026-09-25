package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.theme.*
import kotlin.math.hypot
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.features.key
import com.sperance.exileforge.ui.icons.StatIcon
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A run of the campaign, over the whole screen: the scene underneath, the overlay above.
 *
 * The scene draws and steps the world; everything with words or numbers in it — the bars, the
 * monster's name and modifiers, the hits, the ailments, the loot — is Compose, in the
 * app's dictionary and theme, laid over it. The stick is the overlay's too: a thumb anywhere in the
 * lower part of the screen sets it, and letting go stops the hero.
 */
@Composable fun ExpeditionPlay(s: ForgeState, vm: ForgeViewModel, run: ExpeditionRun) {
    val hud by run.hud.collectAsState()
    var gear by remember { mutableStateOf(false) }
    // Leaving a map gives up what is left on it, so it is asked first (2.48.0); the fight has its own retreat.
    var leaving by remember { mutableStateOf(false) }
    // A Vaal zone (2.65.0) has no way out but its guardian or a death: back does nothing on its map.
    val zone = VaalZones.isZone(run.map)
    BackHandler { when {
        hud.phase == RunPhase.GATE -> vm.runCommand(RunCommand.StepBack)
        hud.phase == RunPhase.MAP -> if (!zone) leaving = true
        else -> vm.runCommand(RunCommand.Leave)
    } }
    LaunchedEffect(hud.phase) { if (hud.phase == RunPhase.LEFT) vm.closeRun() }

    Box(Modifier.fillMaxSize().background(Ink)) {
        ExpeditionScene(run, s.heroClass?.code, Modifier.fillMaxSize())
        when (hud.phase) {
            RunPhase.MAP -> {
                Stick(run)
                MapBar(run, hud, onLeave = if (zone) null else ({ leaving = true }), onGear = { gear = true })
                if (gear) GearSheet(s, vm) { gear = false }
                if (hud.chestPending || hud.chestFailed || hud.chest != null) ChestLoot(s, hud) { vm.runCommand(RunCommand.DismissChest) }
                if (leaving) ConfirmSheet(title = ui("expedition.leave_q"), confirm = ui("expedition.leave"), danger = true,
                    subtitle = mapTitle(hud.mapCode),
                    ledger = listOf(LedgerLine(ui("expedition.leave_left"), ui(if (hud.sealed) "expedition.boss_alive" else "expedition.boss_slain"), Tone.SPEND)),
                    note = ui("expedition.leave_note"), onDismiss = { leaving = false }) { vm.runCommand(RunCommand.Leave) }
            }
            RunPhase.FIGHT -> hud.fight?.let { ArenaOverlay(s, hud, it, run.map.level, run.hero, run.rules, run.stance, onCommand = vm::runCommand) }
            // The fight is over: its report — the log, what it came to, and the loot of a victory.
            RunPhase.LOOT -> hud.report?.let { ReportScreen(s, hud, it) { vm.runCommand(RunCommand.Continue) } }
            RunPhase.DEAD -> hud.report?.let { ReportScreen(s, hud, it) { vm.runCommand(RunCommand.Continue) } }
                ?: Ending(ui("expedition.dead"), ui(if (zone) "vaal.dead_hint" else "expedition.dead_hint"), LifeRed, hud,
                    if (zone) ui("vaal.back") else ui("expedition.back_to_camp")) { vm.runCommand(RunCommand.Continue) }
            RunPhase.CLEARED -> if (zone) Ending(ui("vaal.done"), ui("vaal.done_hint"), Vital, hud, ui("vaal.back")) { vm.runCommand(RunCommand.Continue) }
                else Ending(ui("expedition.map_done"), ui("expedition.map_done_hint"), Vital, hud) { vm.runCommand(RunCommand.Continue) }
            RunPhase.GATE -> VaalGate(s, hud, run.map.corrupted?.code, onEnter = vm::enterVaal, onRefuse = vm::refuseVaal) { vm.runCommand(RunCommand.StepBack) }
            RunPhase.LEFT -> Unit
        }
        // A refusal of the gear (2.40.0) has to be read here too: the run has no bar and no banner.
        RefusalLine(s.refusal, vm::dismissMessage, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

// ==================== Walking ====================

/**
 * Life and shield, the map's name and whether its warden still lives, and the way out. Nothing
 * comes back on its own between fights (2.29.0) but a fountain. What the map still holds — foes, chests, fountains — is the walk's to find (2.56.1).
 */
@Composable private fun MapBar(run: ExpeditionRun, hud: RunHud, onLeave: (() -> Unit)?, onGear: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // The way out (2.56.1): a portal in a bronze ring, first thing in the corner, and it asks before it goes.
            onLeave?.let { RoundButton(ForgeGlyphs.Portal, ui("expedition.leave"), onClick = it) }
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val zone = VaalZones.isZone(run.map)
                Text(if (zone) ui("vaal.title", mapTitle(hud.mapCode)) else mapTitle(hud.mapCode), color = if (zone) Color(0xFFFF8A78) else GoldBright,
                    style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(ui(when { zone && hud.sealed -> "vaal.guardian_alive"; zone -> "vaal.guardian_slain"; hud.sealed -> "expedition.boss_alive"; else -> "expedition.boss_slain" }),
                    color = if (hud.sealed) LifeRed else Vital, style = MaterialTheme.typography.labelMedium)
                // Life under the map's name (2.72.0), out of the middle of the view.
                Vitals(hud.heroLife, hud.heroMaxLife, hud.heroShield, hud.heroMaxShield, Modifier.fillMaxWidth())
                RoundButton(ForgeGlyphs.Helm, ui("expedition.gear"), onClick = onGear)
            }
            // The minimap (2.51.0), opened as the map is explored; round and around the hero since 2.56.1,
            // with its own zoom and the whole map behind a tap since 2.72.0.
            MiniMap(run, hud)
        }
    }
}

/** A glyph in a bronze ring on dark glass: the map's buttons share one look (2.56.1). */
@Composable private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).background(Color(0xE60A0D12), CircleShape).border(1.5.dp, Bronze, CircleShape)) {
        Icon(icon, label, tint = GoldBright, modifier = Modifier.size(24.dp))
    }
}

/**
 * The map in small: round, framed in bronze, the hero at its centre and the explored ground moving
 * under them (2.56.1) — rock darker than floor, the exit once seen, the chests and fountains still
 * standing. Two small buttons under it zoom in and out (2.72.0), and a tap opens the whole map.
 * It redraws a few times a second on its own tick — the scene's clock is the scene's.
 */
@Composable private fun MiniMap(run: ExpeditionRun, hud: RunHud) {
    val world = run.world
    var tick by remember(world) { mutableIntStateOf(0) }
    var cells by rememberSaveable { mutableFloatStateOf(MINIMAP_CELLS) }
    var full by remember { mutableStateOf(false) }
    LaunchedEffect(world) { while (true) { kotlinx.coroutines.delay(200); tick++ } }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(150.dp).clip(CircleShape).background(Color(0xE60A0D12)).clickable { full = true }) {
            if (tick < 0) return@Canvas
            val cell = size.width / cells
            drawExplored(world, Offset(size.width / 2 - world.heroX.toFloat() * cell, size.height / 2 - world.heroY.toFloat() * cell), cell, monsters = false)
            val centre = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xB30A0D12)), centre, radius), radius, centre)
            drawCircle(Bronze, radius - 1.5.dp.toPx(), centre, style = Stroke(3.dp.toPx()))
            drawCircle(GoldBright.copy(alpha = .35f), radius - 5.dp.toPx(), centre, style = Stroke(1.dp.toPx()))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZoomButton("−", ui("expedition.zoom_out")) { cells = (cells * 1.4f).coerceAtMost(MINIMAP_MAX) }
            ZoomButton("+", ui("expedition.zoom_in")) { cells = (cells / 1.4f).coerceAtLeast(MINIMAP_MIN) }
        }
    }
    if (full) FullMap(run, hud, tick) { full = false }
}

@Composable private fun ZoomButton(sign: String, label: String, onClick: () -> Unit) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xE60A0D12)).border(1.dp, Bronze, CircleShape)
        .clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Text(sign, color = GoldBright, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The explored ground from [origin] at [cell] pixels a cell, only what falls on the canvas: floor
 * lighter where lit, rock dark, the chests, fountains, portal and exit, the hero — and, on the whole
 * map, the monsters standing in the hero's light, in their rarity's colour.
 */
private fun DrawScope.drawExplored(world: ExpeditionWorld, origin: Offset, cell: Float, monsters: Boolean) {
    val map = world.map
    val square = Size(cell, cell)
    val xs = (floor(-origin.x / cell).toInt() - 1).coerceAtLeast(0)..(ceil((size.width - origin.x) / cell).toInt() + 1).coerceAtMost(map.width - 1)
    val ys = (floor(-origin.y / cell).toInt() - 1).coerceAtLeast(0)..(ceil((size.height - origin.y) / cell).toInt() + 1).coerceAtMost(map.height - 1)
    for (y in ys) for (x in xs) {
        if (!world.explored(x, y)) continue
        drawRect(if (map.walkable(x, y)) Parchment.copy(alpha = if (world.lit(x, y)) .55f else .3f) else Color(0xFF2A2B33), Offset(origin.x + x * cell, origin.y + y * cell), square)
    }
    val dot = (cell * .5f).coerceAtLeast(2.5f)
    fun mark(x: Double, y: Double, color: Color, size: Float = dot) = drawCircle(color, size, Offset(origin.x + x.toFloat() * cell, origin.y + y.toFloat() * cell))
    world.chests.filter { !it.opened && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, GoldBright) }
    world.fountains.filter { !it.used && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, ShieldCyan) }
    world.portal?.takeIf { world.explored(it.x, it.y) }?.let { mark(it.x + .5, it.y + .5, LifeRed, dot * 1.2f) }
    if (world.explored(map.exit.x, map.exit.y)) mark(map.exit.x + .5, map.exit.y + .5, if (world.sealed) LifeRed else Vital, dot * 1.4f)
    if (monsters) world.agents.filter { it.alive && world.lit(it.x.toInt(), it.y.toInt()) }.forEach { agent ->
        mark(agent.x, agent.y, Color.Black, dot * 1.25f)
        mark(agent.x, agent.y, rarityTint(agent.monster.rarity), dot)
    }
    mark(world.heroX, world.heroY, Gold, dot * 1.4f)
    mark(world.heroX, world.heroY, Ink, dot * .5f)
}

/**
 * The whole map (2.72.0), over everything: all the explored ground fitted to the screen, the
 * monsters in the hero's light, a legend of what each mark is, what the map still holds, and what
 * the map item and the atlas lay on it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun FullMap(run: ExpeditionRun, hud: RunHud, tick: Int, onClose: () -> Unit) {
    val world = run.world
    val map = world.map
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Ink.copy(alpha = .96f)).statusBarsPadding().navigationBarsPadding().padding(12.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mapTitle(hud.mapCode), color = GoldBright, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, ui("common.close"), tint = Gold) }
            }
            Canvas(Modifier.fillMaxWidth().aspectRatio(map.width / map.height.toFloat()).background(Color(0xFF07090C), RoundedCornerShape(8.dp))
                .border(1.dp, Bronze, RoundedCornerShape(8.dp))) {
                if (tick < 0) return@Canvas
                drawExplored(world, Offset.Zero, minOf(size.width / map.width, size.height / map.height), monsters = true)
            }
            val explored = (0 until map.height).sumOf { y -> (0 until map.width).count { x -> map.walkable(x, y) && world.explored(x, y) } }
            val floorCells = (0 until map.height).sumOf { y -> (0 until map.width).count { x -> map.walkable(x, y) } }.coerceAtLeast(1)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Counter(ui("map.monsters_left", hud.alive, hud.total), LifeRed)
                Counter(ui("map.chests_left", hud.chestsLeft), GoldBright)
                Counter(ui("map.fountains_left", hud.fountainsLeft), ShieldCyan)
                Counter(ui(if (hud.sealed) "expedition.boss_alive" else "expedition.boss_slain"), if (hud.sealed) LifeRed else Vital)
                Counter(ui("map.explored", explored * 100 / floorCells), Parchment)
            }
            Engraved(ui("map.legend"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Legend(Gold, ui("map.legend_hero")); Legend(GoldBright, ui("map.legend_chest")); Legend(ShieldCyan, ui("map.legend_fountain"))
                Legend(Vital, ui("map.legend_exit")); Legend(LifeRed, ui("map.legend_sealed")); Legend(Color(0xFFFF8A78), ui("map.legend_portal"))
                MonsterRarity.entries.forEach { Legend(rarityTint(it), ui(it.key())) }
            }
            if (run.mapEffects.isNotEmpty()) {
                Engraved(ui("map.modifiers"))
                run.mapEffects.forEach { (stat, value) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatIcon(stat, Rune, Modifier.size(16.dp))
                        Text(statTitle(stat), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(statNumber(stat, value), color = Rune, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else MutedText(ui("map.no_modifiers"))
        }
    }
}

@Composable private fun Counter(text: String, tint: Color) {
    Text(text, color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(tint.copy(alpha = .1f), RoundedCornerShape(6.dp)).border(1.dp, tint.copy(alpha = .4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable private fun Legend(tint: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(tint, CircleShape))
        Text(text, color = Parchment, style = MaterialTheme.typography.labelSmall)
    }
}

/** How many cells the minimap shows across by default, and how near and how far its zoom goes. */
private const val MINIMAP_CELLS = 22f
private const val MINIMAP_MIN = 10f
private const val MINIMAP_MAX = 60f

/** A life bar with the shield laid over it, and the figure in words. */
@Composable private fun Vitals(life: Int, maxLife: Int, shield: Int, maxShield: Int, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(3.dp)
    val lifeShare by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0xCC0A0D12), shape).border(1.dp, LifeRed.copy(alpha = .8f), shape)) {
            Box(Modifier.fillMaxWidth(lifeShare.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
            if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).align(Alignment.TopStart).background(ShieldCyan.copy(alpha = .85f)))
        }
        Text(if (maxShield > 0) ui("expedition.vitals_shield", life, maxLife, shield) else ui("expedition.vitals", life, maxLife),
            color = Parchment, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * The stick: wherever the thumb lands in the lower part of the screen, dragging from there walks.
 * The direction is sent in screen axes; the run turns it into the map's.
 */
@Composable private fun Stick(run: ExpeditionRun) {
    var centre by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val radius = with(LocalDensity.current) { 56.dp.toPx() }
    // A fight can start under a thumb still on the glass; the hero must not walk off after it.
    DisposableEffect(run) { onDispose { run.stickX = 0.0; run.stickY = 0.0 } }
    Box(Modifier.fillMaxSize().onSizeChanged { area = it }.pointerInput(run) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (down.position.y < area.height * .35f) return@awaitEachGesture
            centre = down.position
            knob = down.position
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val delta = change.position - down.position
                val length = hypot(delta.x, delta.y)
                val clamped = if (length > radius) delta * (radius / length) else delta
                knob = down.position + clamped
                run.stickX = (clamped.x / radius).toDouble()
                run.stickY = (clamped.y / radius).toDouble()
                change.consume()
            } while (change.pressed)
            run.stickX = 0.0
            run.stickY = 0.0
            centre = null
        }
    }) {
        centre?.let { c ->
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Gold.copy(alpha = .18f), radius, c)
                drawCircle(Gold.copy(alpha = .5f), radius, c, style = Stroke(2.dp.toPx()))
                drawCircle(GoldBright.copy(alpha = .75f), radius * .4f, knob)
            }
        }
        if (centre == null) Text(ui("expedition.stick_hint"), color = Muted.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
    }
}

/**
 * What a chest brought (since 2.33.0), at the foot of the map while the hero walks on: the
 * server's roll, awaited, or its absence said plainly, and a button that puts it away.
 */
@Composable private fun ChestLoot(s: ForgeState, hud: RunHud, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        RunPanel(Modifier, GoldBright) {
            Text(ui("expedition.chest"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            val reward = hud.chest
            when {
                hud.chestPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
                    Text(ui("expedition.loot_pending"), color = Muted)
                }
                hud.chestFailed -> Text(ui("expedition.chest_failed"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
                reward != null -> RewardLines(s, reward)
            }
            OutlinedButton(enabled = !hud.chestPending, onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(ui("common.close")) }
        }
    }
}

// ==================== After ====================

/** A run that ended — by death or by the exit — and what it brought all told. */
@Composable private fun Ending(title: String, hint: String, accent: Color, hud: RunHud, done: String = ui("expedition.back_to_camp"), onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = .72f)), contentAlignment = Alignment.Center) {
        RunPanel(Modifier, accent) {
            Text(title, color = accent, style = MaterialTheme.typography.headlineSmall)
            Text(hint, color = Parchment, style = MaterialTheme.typography.bodyMedium)
            MutedText(ui("expedition.summary", hud.kills, hud.gold, number(hud.experience)))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(done) }
        }
    }
}

@Composable private fun RunPanel(modifier: Modifier, accent: Color = Gold, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    Row(modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth().height(IntrinsicSize.Min).background(Panel, shape).border(1.dp, accent.copy(alpha = .5f), shape)) {
        RaritySpine(accent, 4.dp)
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
