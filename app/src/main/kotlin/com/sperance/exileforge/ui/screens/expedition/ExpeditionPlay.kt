package com.sperance.exileforge.ui.screens.expedition
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.statDescription
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.rarityTint
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.expedition.scene.SCENE_UNIT
import com.sperance.exileforge.ui.screens.expedition.scene.sceneToWorld
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * A run of the campaign, over the whole screen: the scene underneath, the overlay above.
 *
 * The scene draws and steps the world; everything with words or numbers in it — the bars, the
 * monster's name and modifiers, the hits, the ailments, the loot — is Compose, in the
 * app's dictionary and theme, laid over it. The stick is the overlay's too: a thumb anywhere in the
 * lower part of the screen sets it, and letting go stops the hero.
 */
@Composable fun ExpeditionPlay(s: ForgeState, vm: ForgeViewModel, run: ExpeditionRun) {
    val model = koinViewModel<ExpeditionViewModel>()
    val hud by run.hud.collectAsState()
    // The first run explains the fight before the first pack is met (3.14.0).
    FirstVisit(Guide.FIGHT)
    var gear by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    // Leaving a map gives up what is left on it, so it is asked first (2.48.0); the fight has its own retreat.
    var leaving by remember { mutableStateOf(false) }
    // A Vaal zone (2.65.0) has no way out but its guardian or a death: back does nothing on its map.
    val zone = VaalZones.isZone(run.zone)
    // The map is over — the portal out, the exit, a fall once its fight's report is read: its summary stands before the camp,
    // and closing it closes the run, once.
    val summary = hud.phase == RunPhase.LEFT || hud.phase == RunPhase.CLEARED || hud.phase == RunPhase.DEAD && hud.report == null
    var closed by remember(run) { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!closed) {
            closed = true
            model.closeRun()
        }
    }
    BackHandler {
        when {
            summary -> close()
            hud.phase == RunPhase.GATE -> model.runCommand(RunCommand.StepBack)
            hud.phase == RunPhase.CRYSTAL || hud.phase == RunPhase.ABYSS || hud.phase == RunPhase.MAP && hud.fountain != null -> model.runCommand(RunCommand.StepOff)
            hud.phase == RunPhase.MAP -> if (!zone) leaving = true
            else -> model.runCommand(RunCommand.Leave)
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        ExpeditionScene(run, s.heroClass?.code, Modifier.fillMaxSize())
        when (hud.phase) {
            RunPhase.MAP -> {
                // An autorun walks by itself (3.2.0): no stick under the thumb while it runs
                if (hud.auto == null) Stick(run) { model.runCommand(RunCommand.OfferFountain(it)) }
                MapBar(
                    s,
                    run,
                    hud,
                    onLeave = if (zone) null else ({ leaving = true }),
                    onGear = { gear = true },
                    onStats = { sheet = true },
                    onDrink = { model.runCommand(RunCommand.Drink(it)) },
                    onRetry = model::flushRun,
                )
                if (gear) {
                    HoldsRun(run)
                    GearSheet(s, model) { gear = false }
                }
                if (sheet) {
                    HoldsRun(run)
                    StatsSheet(s, run.mapEffects) { sheet = false }
                }
                hud.fountain?.let { FountainOffer(it, onTake = { model.runCommand(RunCommand.TakeFountain) }) { model.runCommand(RunCommand.StepOff) } }
                hud.chest?.let { ChestLoot(s, model, run, it, hud.chestAwaiting) { model.runCommand(RunCommand.DismissChest) } }
                if (leaving) {
                    ConfirmSheet(
                        title = ui("expedition.leave_q"),
                        confirm = ui("expedition.leave"),
                        danger = true,
                        subtitle = mapTitle(hud.mapCode),
                        ledger = listOf(LedgerLine(ui("expedition.leave_left"), ui(if (hud.sealed) "expedition.boss_alive" else "expedition.boss_slain"), Tone.SPEND)),
                        note = ui("expedition.leave_note"),
                        onDismiss = { leaving = false },
                    ) { model.runCommand(RunCommand.Leave) }
                }
            }

            RunPhase.FIGHT -> hud.fight?.let { ArenaOverlay(s, hud, it, it.level.takeIf { level -> level > 0 } ?: run.zone.level, run.rules, run.stance, onCommand = model::runCommand, onLogFilter = vm::logFilter, onBuzz = vm::buzz) }

            // The fight is over: its report — the log, what it came to, and the loot of a victory.
            RunPhase.LOOT -> hud.report?.let { ReportScreen(s, vm, model, hud, it) { model.runCommand(RunCommand.Continue) } }

            // A fall: the fight's report first, then the map's summary (its «Вернуться» leaves the map).
            RunPhase.DEAD -> hud.report?.let { ReportScreen(s, vm, model, hud, it) { model.runCommand(RunCommand.Continue) } }
                ?: MapSummary(s, model, hud, onDone = close)

            RunPhase.CLEARED -> MapSummary(s, model, hud, onDone = close)

            RunPhase.GATE -> VaalGate(s, hud, run.zone.corrupted.takeIf { it.isNotBlank() }, onEnter = model::enterVaal, onRefuse = model::refuseVaal) { model.runCommand(RunCommand.StepBack) }

            RunPhase.CRYSTAL -> hud.crystal?.let { CrystalSheet(s, it, onCommand = model::runCommand) }

            RunPhase.ABYSS -> hud.abyss?.let { AbyssSheet(s, hud, it, onCommand = model::runCommand) }

            RunPhase.LEFT -> MapSummary(s, model, hud, onDone = close)
        }
        // In a fight the arena's own row carries the autorun (3.77.0); the plate floats only over the map.
        hud.auto?.takeIf { hud.phase == RunPhase.MAP }?.let { auto ->
            AutoBar(auto, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)) { model.runCommand(RunCommand.StopAuto) }
        }
        // A refusal of the gear (2.40.0) has to be read here too: the run has no bar and no banner.
        ToastHost(s, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

// ==================== Walking ====================

/**
 * Life and shield, the map's name and whether its warden still lives, and the way out. Nothing
 * comes back on its own between fights (2.29.0) but a fountain. What the map still holds — foes, chests, fountains — is the walk's to find (2.56.1).
 * The journal's events the server has not taken for a while are counted under the name, a tap sends them now; a slain guardian is not bought back (3.2.0) — it returns in its time.
 */
@Composable internal fun MapBar(
    s: ForgeState,
    run: ExpeditionRun,
    hud: RunHud,
    onLeave: (() -> Unit)?,
    onGear: () -> Unit,
    onStats: () -> Unit,
    onDrink: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // The way out (2.56.1): a portal in a bronze ring, first thing in the corner, and it asks before it goes.
            // The gear right under it, on the same line (2.73.0), and the hero's figures under the gear (2.75.0).
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                onLeave?.let { RoundButton(ForgeGlyphs.Portal, ui("expedition.leave"), onClick = it) }
                RoundButton(ForgeGlyphs.Helm, ui("expedition.gear"), onClick = onGear)
                RoundButton(ForgeGlyphs.Scroll, ui("expedition.stats_hero"), onClick = onStats)
                BugAction()
            }
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val zone = VaalZones.isZone(run.zone)
                Text(
                    if (zone) ui("vaal.title", mapTitle(hud.mapCode)) else mapTitle(hud.mapCode),
                    color = if (zone) Color(0xFFFF8A78) else GoldBright,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                Text(
                    ui(
                        when {
                            zone && hud.sealed -> "vaal.guardian_alive"
                            zone -> "vaal.guardian_slain"
                            hud.sealed -> "expedition.boss_alive"
                            else -> "expedition.boss_slain"
                        },
                    ),
                    color = if (hud.sealed) LifeRed else Vital,
                    style = MaterialTheme.typography.labelMedium,
                )
                Journal(hud, onRetry)
                // Life under the map's name (2.72.0), out of the middle of the view; the mana and the belt under it (2.78.0).
                Vitals(hud.heroLife, hud.heroMaxLife, hud.heroShield, hud.heroMaxShield, Modifier.fillMaxWidth(), hud.heroMana, hud.heroMaxMana, hud.heroReserved)
                if (hud.flasks.any { it != null }) MapFlasks(hud.flasks, onDrink)
            }
            // The minimap (2.51.0), opened as the map is explored; round and around the hero since 2.56.1,
            // with its own zoom and the whole map behind a tap since 2.72.0.
            MiniMap(run, hud)
        }
    }
}

/**
 * The belt on the map (2.78.0): a draught on the road brings its life or mana back at once, and a
 * utility flask's lines run as the hero walks. A flask is filled to its charges and ringed while it runs.
 */
@Composable internal fun MapFlasks(flasks: List<FlaskView?>, onDrink: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        flasks.forEach { view ->
            if (view == null) return@forEach
            val tint = flaskTint(view.kind)
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color(0xE60A0D12))
                    .border(if (view.active > 0f) 2.dp else 1.dp, if (view.active > 0f) GoldBright else Bronze, CircleShape)
                    .clickable(enabled = view.usable) { onDrink(view.slot) }.semantics { contentDescription = ui("expedition.drink") },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val fill = if (view.maxCharges > 0) view.charges / view.maxCharges.toFloat() else 0f
                    drawRect(tint.copy(alpha = if (view.usable) .55f else .25f), topLeft = Offset(0f, size.height * (1 - fill)), size = Size(size.width, size.height * fill))
                    if (view.active > 0f) drawArc(GoldBright, -90f, 360f * view.active, false, style = Stroke(2.5.dp.toPx()))
                }
                Text("${view.charges}", color = GoldBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * The journal, quietly (3.0.0): how many events the server has not taken yet, and how many it refused.
 * Nothing while everything is counted — the run does not talk about its bookkeeping unprompted.
 */
@Composable internal fun Journal(hud: RunHud, onRetry: (() -> Unit)? = null) {
    // Only an oldest event the server has not taken for a while is worth a word: a batch in flight is not news.
    var overdue by remember { mutableStateOf(false) }
    LaunchedEffect(hud.pending > 0, hud.applied) {
        overdue = false
        if (hud.pending > 0) {
            delay(PENDING_GRACE)
            overdue = true
        }
    }
    val waiting = hud.pending > 0 && overdue
    if (!waiting && hud.rejected == 0) return
    Text(
        listOfNotNull(ui("expedition.pending", hud.pending).takeIf { waiting }, ui("expedition.rejected", hud.rejected).takeIf { hud.rejected > 0 }).joinToString(" · "),
        color = if (hud.rejected > 0) LifeRed.copy(alpha = .85f) else Muted,
        style = MaterialTheme.typography.labelSmall,
        modifier = onRetry?.takeIf { waiting }?.let { Modifier.clickable(onClick = it) } ?: Modifier,
    )
}

/** How long the journal's oldest unsent event waits before the run says so. */
internal const val PENDING_GRACE = 10_000L

/** The run stands still for as long as this is in the composition (2.73.0): a window over the map pauses it. */
@Composable internal fun HoldsRun(run: ExpeditionRun) {
    DisposableEffect(run) {
        run.send(RunCommand.Hold(true))
        onDispose { run.send(RunCommand.Hold(false)) }
    }
}

/** A glyph in a bronze ring on dark glass: the map's buttons share one look (2.56.1). */
@Composable internal fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).background(Color(0xE60A0D12), CircleShape).border(1.5.dp, Bronze, CircleShape)) {
        Icon(icon, label, tint = GoldBright, modifier = Modifier.size(24.dp))
    }
}

/**
 * What a chest brought (since 2.33.0), at the foot of the map while the hero walks on: the server's roll,
 * shown as its answer arrives (server 1.30.0), and a button that puts it away. A piece opens its
 * comparison with what is worn and can be worn at once (3.24.0); the map holds still while it is open.
 */
@Composable internal fun ChestLoot(s: ForgeState, vm: ExpeditionViewModel, run: ExpeditionRun, reward: Reward, awaiting: Boolean, onClose: () -> Unit) {
    var looked by remember(reward) { mutableStateOf<ItemView?>(null) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        RunPanel(Modifier, GoldBright) {
            Text(ui("expedition.chest"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            RewardLines(s, reward, { looked = it }, awaiting)
            ForgeOutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(ui("common.close")) }
        }
    }
    looked?.let { item ->
        HoldsRun(run)
        LootSheet(s, vm, item, onDismiss = { looked = null })
    }
}

/** How near, in tiles, a tap must land to a fountain to name it. */
internal const val FOUNTAIN_TAP = .9

/**
 * A fountain offered (3.70.0), walked onto or tapped on the map: what it gives, and the choice — drink it now,
 * or step away and leave it standing for later. The map holds still until the answer.
 */
@Composable internal fun FountainOffer(fountain: FountainView, onTake: () -> Unit, onLeave: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        RunPanel(Modifier, ShieldCyan) {
            Text(ui("fountain.offer", fountain.heal.roundToInt()), color = ShieldCyan, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ForgeOutlinedButton(onClick = onLeave, modifier = Modifier.weight(1f)) { Text(ui("fountain.leave")) }
                ForgeButton(onClick = onTake, modifier = Modifier.weight(1f)) { Text(ui("fountain.take")) }
            }
        }
    }
}

// ==================== After ====================

/** The autorun's plate (3.2.0): the wave under way of how many, and a stop that hands the run back to the stick. */
@Composable internal fun AutoBar(auto: AutoHud, modifier: Modifier, onStop: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.background(Panel.copy(alpha = .92f), shape).border(1.dp, Gold.copy(alpha = .5f), shape).padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(ui("auto.wave", auto.wave, auto.waves), color = GoldBright, style = MaterialTheme.typography.labelLarge)
        ForgeTextButton(onClick = onStop) { Text(ui("auto.stop"), color = LifeRed) }
    }
}

@Composable internal fun RunPanel(modifier: Modifier, accent: Color = Gold, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    Row(modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth().height(IntrinsicSize.Min).background(Panel, shape).border(1.dp, accent.copy(alpha = .5f), shape)) {
        RaritySpine(accent, 4.dp)
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
