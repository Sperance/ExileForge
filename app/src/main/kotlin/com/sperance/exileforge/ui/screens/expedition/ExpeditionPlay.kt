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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
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

/**
 * A run of the campaign, over the whole screen: the scene underneath, the overlay above.
 *
 * The scene draws and steps the world; everything with words or numbers in it — the bars, the
 * monster's name and modifiers, the hits, the ailments, the flask, the loot — is Compose, in the
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
                MapBar(run, hud, onLeave = { leaving = true }, onFlask = { vm.runCommand(RunCommand.Flask) }, onGear = { gear = true })
                if (gear) GearSheet(s, vm) { gear = false }
                if (hud.chestPending || hud.chestFailed || hud.chest != null) ChestLoot(s, hud) { vm.runCommand(RunCommand.DismissChest) }
                if (leaving) ConfirmSheet(title = ui("expedition.leave_q"), confirm = ui("expedition.leave"), danger = true,
                    subtitle = mapTitle(hud.mapCode),
                    ledger = listOf(LedgerLine(ui("expedition.leave_left"), ui("expedition.monsters_left", hud.alive, hud.total), Tone.SPEND)),
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
 * Life, shield and the flask, what is left on the map, and the way out. Nothing comes back on
 * its own between fights (2.29.0), so the flask is here too: the same charge, the same heal.
 */
@Composable private fun MapBar(run: ExpeditionRun, hud: RunHud, onLeave: (() -> Unit)?, onFlask: () -> Unit, onGear: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(mapTitle(hud.mapCode), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(ui("expedition.monsters_left", hud.alive, hud.total), color = Muted, style = MaterialTheme.typography.labelMedium)
                if (hud.chestsLeft > 0) Text(ui("expedition.chests_left", hud.chestsLeft), color = GoldBright, style = MaterialTheme.typography.labelMedium)
                if (hud.fountainsLeft > 0) Text(ui("expedition.fountains_left", hud.fountainsLeft), color = ShieldCyan, style = MaterialTheme.typography.labelMedium)
                if (hud.sealed) Text(ui("expedition.exit_sealed"), color = LifeRed, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onGear) { Icon(ForgeGlyphs.Helm, ui("expedition.gear"), tint = Gold, modifier = Modifier.size(24.dp)) }
            // The minimap (2.51.0), opened as the map is explored, and the way out under it.
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MiniMap(run.world)
                onLeave?.let {
                    IconButton(onClick = it, modifier = Modifier.size(36.dp).background(Color(0xCC0A0D12), CircleShape).border(1.dp, LifeRed.copy(alpha = .7f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, ui("expedition.leave"), tint = LifeRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Vitals(hud.heroLife, hud.heroMaxLife, hud.heroShield, hud.heroMaxShield, Modifier.fillMaxWidth(.6f))
        Button(enabled = hud.flasks > 0 && !hud.flaskActive, onClick = onFlask, modifier = Modifier.fillMaxWidth(.6f).height(34.dp), contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright, disabledContainerColor = Panel, disabledContentColor = Muted)) {
            Icon(ForgeGlyphs.Flask, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(ui(if (hud.flaskActive) "expedition.flask_drinking" else "expedition.flask", hud.flasks, hud.maxFlasks), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The map in small (2.51.0): only what the hero has explored, rock darker than floor, the exit once
 * seen, the chests and fountains still standing, and the hero. It redraws a few times a second on
 * its own tick — the scene's clock is the scene's.
 */
@Composable private fun MiniMap(world: ExpeditionWorld) {
    var tick by remember(world) { mutableIntStateOf(0) }
    LaunchedEffect(world) { while (true) { kotlinx.coroutines.delay(200); tick++ } }
    val map = world.map
    val side = 108.dp
    val shape = RoundedCornerShape(6.dp)
    Canvas(Modifier.size(side).background(Color(0xCC0A0D12), shape).border(1.dp, Bronze.copy(alpha = .7f), shape).padding(4.dp)) {
        if (tick < 0) return@Canvas
        val cell = minOf(size.width / map.width, size.height / map.height)
        val left = (size.width - cell * map.width) / 2
        val top = (size.height - cell * map.height) / 2
        fun at(x: Int, y: Int) = Offset(left + x * cell, top + y * cell)
        val square = androidx.compose.ui.geometry.Size(cell, cell)
        for (y in 0 until map.height) for (x in 0 until map.width) {
            if (!world.explored(x, y)) continue
            drawRect(if (map.walkable(x, y)) Parchment.copy(alpha = if (world.lit(x, y)) .55f else .3f) else Color(0xFF2A2B33), at(x, y), square)
        }
        val dot = cell.coerceAtLeast(2.5f)
        fun mark(x: Int, y: Int, color: Color, radius: Float = dot) = drawCircle(color, radius, Offset(left + (x + .5f) * cell, top + (y + .5f) * cell))
        world.chests.filter { !it.opened && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x, it.cell.y, GoldBright) }
        world.fountains.filter { !it.used && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x, it.cell.y, ShieldCyan) }
        world.corruption?.takeIf { it.alive && world.explored(it.x.toInt(), it.y.toInt()) }?.let { mark(it.x.toInt(), it.y.toInt(), Rune, dot * 1.2f) }
        if (world.explored(map.exit.x, map.exit.y)) mark(map.exit.x, map.exit.y, if (world.sealed) LifeRed else Vital, dot * 1.4f)
        drawCircle(Gold, dot * 1.3f, Offset(left + world.heroX.toFloat() * cell, top + world.heroY.toFloat() * cell))
    }
}

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
