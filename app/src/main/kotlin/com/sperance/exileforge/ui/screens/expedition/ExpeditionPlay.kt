package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.features.key
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector

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
    BackHandler { if (hud.phase == RunPhase.MAP) leaving = true else vm.runCommand(RunCommand.Leave) }
    LaunchedEffect(hud.phase) { if (hud.phase == RunPhase.LEFT) vm.closeRun() }

    Box(Modifier.fillMaxSize().background(Ink)) {
        ExpeditionScene(run, s.heroClass?.code, Modifier.fillMaxSize())
        when (hud.phase) {
            RunPhase.MAP -> {
                Stick(run)
                MapBar(run, hud, onLeave = { leaving = true }, onGear = { gear = true })
                if (gear) GearSheet(s, vm) { gear = false }
                if (hud.chestPending || hud.chestFailed || hud.chest != null) ChestLoot(s, hud) { vm.runCommand(RunCommand.DismissChest) }
                if (leaving) ConfirmSheet(title = ui("expedition.leave_q"), confirm = ui("expedition.leave"), danger = true,
                    subtitle = mapTitle(hud.mapCode),
                    ledger = listOf(LedgerLine(ui("expedition.leave_left"), ui(if (hud.sealed) "expedition.boss_alive" else "expedition.boss_slain"), Tone.SPEND)),
                    note = ui("expedition.leave_note"), onDismiss = { leaving = false }) { vm.runCommand(RunCommand.Leave) }
            }
            RunPhase.FIGHT -> hud.fight?.let { ArenaOverlay(s, hud, it, run.map.level, onCommand = vm::runCommand) }
            // The fight is over: its report — the log, what it came to, and the loot of a victory.
            RunPhase.LOOT -> hud.report?.let { ReportScreen(s, hud, it) { vm.runCommand(RunCommand.Continue) } }
            RunPhase.DEAD -> hud.report?.let { ReportScreen(s, hud, it) { vm.runCommand(RunCommand.Continue) } }
                ?: Ending(ui("expedition.dead"), ui("expedition.dead_hint"), LifeRed, hud) { vm.runCommand(RunCommand.Continue) }
            RunPhase.CLEARED -> Ending(ui("expedition.map_done"), ui("expedition.map_done_hint"), Vital, hud) { vm.runCommand(RunCommand.Continue) }
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
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text(mapTitle(hud.mapCode), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(ui(if (hud.sealed) "expedition.boss_alive" else "expedition.boss_slain"), color = if (hud.sealed) LifeRed else Vital,
                    style = MaterialTheme.typography.labelMedium)
            }
            RoundButton(ForgeGlyphs.Helm, ui("expedition.gear"), onClick = onGear)
            // The minimap (2.51.0), opened as the map is explored; round and around the hero since 2.56.1.
            MiniMap(run.world)
        }
        Vitals(hud.heroLife, hud.heroMaxLife, hud.heroShield, hud.heroMaxShield, Modifier.fillMaxWidth(.6f))
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
 * standing. It redraws a few times a second on its own tick — the scene's clock is the scene's.
 */
@Composable private fun MiniMap(world: ExpeditionWorld) {
    var tick by remember(world) { mutableIntStateOf(0) }
    LaunchedEffect(world) { while (true) { kotlinx.coroutines.delay(200); tick++ } }
    val map = world.map
    Canvas(Modifier.size(150.dp).clip(CircleShape).background(Color(0xE60A0D12))) {
        if (tick < 0) return@Canvas
        val cell = size.width / MINIMAP_CELLS
        val centre = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        val left = centre.x - world.heroX.toFloat() * cell
        val top = centre.y - world.heroY.toFloat() * cell
        fun at(x: Int, y: Int) = Offset(left + x * cell, top + y * cell)
        val square = androidx.compose.ui.geometry.Size(cell, cell)
        // Only the cells under the glass are drawn: the map may be far larger than the window.
        val xs = (kotlin.math.floor(-left / cell).toInt() - 1).coerceAtLeast(0)..(kotlin.math.ceil((size.width - left) / cell).toInt() + 1).coerceAtMost(map.width - 1)
        val ys = (kotlin.math.floor(-top / cell).toInt() - 1).coerceAtLeast(0)..(kotlin.math.ceil((size.height - top) / cell).toInt() + 1).coerceAtMost(map.height - 1)
        for (y in ys) for (x in xs) {
            if (!world.explored(x, y)) continue
            drawRect(if (map.walkable(x, y)) Parchment.copy(alpha = if (world.lit(x, y)) .55f else .3f) else Color(0xFF2A2B33), at(x, y), square)
        }
        val dot = cell * .5f
        fun mark(x: Int, y: Int, color: Color, size: Float = dot) = drawCircle(color, size, Offset(left + (x + .5f) * cell, top + (y + .5f) * cell))
        world.chests.filter { !it.opened && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x, it.cell.y, GoldBright) }
        world.fountains.filter { !it.used && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x, it.cell.y, ShieldCyan) }
        world.corruption?.takeIf { it.alive && world.explored(it.x.toInt(), it.y.toInt()) }?.let { mark(it.x.toInt(), it.y.toInt(), Rune, dot * 1.2f) }
        if (world.explored(map.exit.x, map.exit.y)) mark(map.exit.x, map.exit.y, if (world.sealed) LifeRed else Vital, dot * 1.4f)
        // The hero, and the glass: darker toward the rim, a bronze ring and a thin gold one inside it.
        drawCircle(Gold, dot * 1.4f, centre)
        drawCircle(Ink, dot * .5f, centre)
        drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xB30A0D12)), centre, radius), radius, centre)
        drawCircle(Bronze, radius - 1.5.dp.toPx(), centre, style = Stroke(3.dp.toPx()))
        drawCircle(GoldBright.copy(alpha = .35f), radius - 5.dp.toPx(), centre, style = Stroke(1.dp.toPx()))
    }
}

/** How many cells the minimap shows across: the hero's near ground, not the whole map. */
private const val MINIMAP_CELLS = 22f

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
@Composable private fun Ending(title: String, hint: String, accent: Color, hud: RunHud, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = .72f)), contentAlignment = Alignment.Center) {
        RunPanel(Modifier, accent) {
            Text(title, color = accent, style = MaterialTheme.typography.headlineSmall)
            Text(hint, color = Parchment, style = MaterialTheme.typography.bodyMedium)
            Text(ui("expedition.summary", hud.kills, hud.gold, number(hud.experience)), color = Muted, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(ui("expedition.back_to_camp")) }
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
