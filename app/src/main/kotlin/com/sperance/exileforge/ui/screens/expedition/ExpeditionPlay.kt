package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.RunMenu
import com.sperance.exileforge.ui.components.RunMenuItem
import com.sperance.exileforge.ui.components.raritySpine
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.LifeFlowSheet
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.expedition.scene.rememberRunClock
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * A run of the campaign, over the whole screen: the scene underneath, the overlay above.
 *
 * The scene draws and steps the world; everything with words or numbers in it — the bars, the
 * monster's name and modifiers, the hits, the ailments, the loot — is Compose, in the
 * app's dictionary and theme, laid over it. The stick is the overlay's too: a thumb anywhere in the
 * lower part of the screen sets it, and letting go stops the hero.
 */
@Composable fun ExpeditionPlay(run: ExpeditionRun) {
    val game by koinViewModel<ExpeditionViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val expedition: ExpeditionViewModel = koinViewModel()
    val model = koinViewModel<ExpeditionViewModel>()
    // Снимок захода живьём - для арены; тело экрана видит его без полей кадра боя ([screenOnly], 4.9.0): бой меняет снимок
    // каждый кадр, а перекомпонуется от этого только арена ([RunFight]), не весь экран похода со сценой и панелями. На карте
    // то же: тикающее (здоровье, пояс, несомое, урон ловушек, вход в комнату, сбор) читают свои листовые элементы из [live].
    val live = run.hud.collectAsState()
    val hud by remember(live) { derivedStateOf { live.value.screenOnly() } }
    // The first run explains the fight before the first pack is met (3.14.0).
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
    // «Продать и вернуться» (3.90.4): [sell] - отмеченная в итогах добыча, проданная уже после захода.
    val close: (sell: List<String>) -> Unit = { sell ->
        if (!closed) {
            closed = true
            if (sell.isEmpty()) model.closeRun() else model.closeRunSelling(sell)
        }
    }
    BackHandler {
        when {
            summary -> close(emptyList())

            hud.phase == RunPhase.GATE -> model.runCommand(RunCommand.StepBack)

            hud.phase == RunPhase.CRYSTAL || hud.phase == RunPhase.ABYSS || hud.phase == RunPhase.MAP && (hud.fountain != null || hud.feature != null) -> model.runCommand(RunCommand.StepOff)

            // С карты вне боя уходят всегда (3.88.9); зона Ваал держит до конца.
            hud.phase == RunPhase.MAP -> if (!zone) leaving = true

            else -> model.runCommand(RunCommand.Leave)
        }
    }

    // Задания захода (3.95.0): доска героя, снятая на входе (4.10.0), и счёт этого захода
    val quests by koinViewModel<QuestViewModel>().quests.collectAsStateWithLifecycle()
    val board = rememberRunBoard(run, model.state.collectAsStateWithLifecycle().value.quests, quests)
    var questsOpen by remember { mutableStateOf(false) }
    // Добыча сундука, открытая с плашки сумки (4.10.0); null - закрыта
    var bagOpen by remember(run) { mutableStateOf(false) }
    // Часы захода идут и без сцены (3.95.1): лента автопробега карты не рисует
    val clock = rememberRunClock(run)
    // «Журнал боя» меню забега (4.8.0) - последний бой захода, и на карте, и посреди следующего боя
    CompositionLocalProvider(LocalFightReport provides hud.lastFight) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            // Автопроход - лента боёв (3.94.0): карты на экране нет, между боями - только счёт пути.
            val ribbon = hud.auto?.takeIf { hud.phase == RunPhase.MAP }
            if (ribbon == null) ExpeditionScene(run, clock, game.heroClass?.code, Modifier.fillMaxSize())
            BlightWatch(hud.blight, shell::announce)
            if (hud.phase == RunPhase.MAP && ribbon == null) LiveHazards(live)
            // Вход в комнату (4.9.0): трещина поддаётся или дверь заперта - строкой над героем, кольцо рисует сцена
            if (hud.phase == RunPhase.MAP && ribbon == null) LiveEntrance(live)
            when (hud.phase) {
                RunPhase.MAP -> if (ribbon != null) {
                    AutoRibbon(run, hud, ribbon, live) { model.runCommand(RunCommand.FinishAuto) }
                } else {
                    MapDockHost { dock ->
                        // An autorun walks by itself (3.2.0): no stick under the thumb while it runs
                        if (hud.auto == null) Stick(run) { model.runCommand(RunCommand.OfferFountain(it)) }
                        MapBar(
                            game,
                            run,
                            hud,
                            live,
                            onLeave = if (zone) null else ({ leaving = true }),
                            onGear = { gear = true },
                            onStats = { sheet = true },
                            onDrink = { model.runCommand(RunCommand.Drink(it)) },
                            onRetry = model::flushRun,
                            onQuests = { questsOpen = true },
                        )
                        if (questsOpen) {
                            HoldsRun(run)
                            RunQuestsSheet(board, hud.questTally) { questsOpen = false }
                        }
                        if (gear) {
                            HoldsRun(run)
                            GearSheet(game) { gear = false }
                        }
                        if (sheet) {
                            HoldsRun(run)
                            StatsSheet(game, run.mapEffects, run.origins) { sheet = false }
                        }
                        hud.fountain?.let { DockPanel { FountainOffer(it, onTake = { model.runCommand(RunCommand.TakeFountain) }) { model.runCommand(RunCommand.StepOff) } } }
                        // Сундук не останавливает героя (4.10.0): добыча летит в сумку, её лист - по нажатию на плашку
                        // «Сумка захода» (4.10.x, вариант A): касание плашки - вся добыча захода, новое сверху; нижняя панель её прячет
                        if (hud.auto == null && !dock.occupied) MapBag(game, hud.drops, hud.bag.isNotEmpty(), hud.tally.loot.pieces()) { bagOpen = true }
                        if (bagOpen) RunBagSheet(game, model, run, hud.bag, hud.gold, hud.experience, hud.awaiting) { bagOpen = false }
                        // Лист объекта карты (3.90.0): алтарь, торговец, узел ремесла
                        // Полоса сбора (4.12.x, ASANA-06) - доля пульса этого кадра: чтение часов захода подписывает её рисование на кадр
                        hud.feature?.let {
                            FeatureSheet(game, it, {
                                clock.floatValue
                                run.pulse.gather
                            }, onCommand = model::runCommand)
                        }
                        if (leaving) {
                            DockPanel {
                                ConfirmSheet(
                                    title = ui("expedition.leave_q"),
                                    confirm = ui("expedition.leave"),
                                    danger = true,
                                    subtitle = mapTitle(hud.mapCode),
                                    ledger = listOf(LedgerLine(ui("expedition.leave_left"), GuardianLine.of(hud).text, Tone.SPEND)),
                                    note = ui("expedition.leave_note"),
                                    onDismiss = { leaving = false },
                                ) { model.runCommand(RunCommand.Leave) }
                            }
                        }
                    }
                }

                RunPhase.FIGHT -> RunFight(live) { frame, fight ->
                    ArenaOverlay(
                        game, frame, fight, fight.level.takeIf { level -> level > 0 } ?: run.run.levelOf(run.zone), run.rules, run.stance,
                        onCommand = model::runCommand, onLogFilter = shell::logFilter, onBuzz = shell::buzz,
                        biome = run.zone.biome,
                        shares = { boss -> BuffSources.of(run.mapEffects, run.run.context.atlas, run.pacts, boss) },
                        mapEffects = run.mapEffects,
                        origins = run.origins,
                        pulse = run.pulse,
                    )
                }

                // The fight is over: its report — the log, what it came to, and the loot of a victory.
                RunPhase.LOOT -> hud.report?.let { ReportScreen(game, model, hud, it) { model.runCommand(RunCommand.Continue) } }

                // A fall: the fight's report first, then the map's summary (its «Вернуться» leaves the map).
                RunPhase.DEAD -> hud.report?.let { ReportScreen(game, model, hud, it) { model.runCommand(RunCommand.Continue) } }
                    ?: MapSummary(game, model, hud, onDone = close)

                RunPhase.CLEARED -> MapSummary(game, model, hud, onDone = close)

                RunPhase.GATE -> VaalGate(game, hud, run.zone.corrupted.takeIf { it.value.isNotBlank() }, onEnter = model::enterVaal) { model.runCommand(RunCommand.StepBack) }

                RunPhase.CRYSTAL -> hud.crystal?.let { CrystalSheet(game, it, onCommand = model::runCommand) }

                RunPhase.ABYSS -> hud.abyss?.let { AbyssSheet(game, hud, it, onCommand = model::runCommand) }

                RunPhase.LEFT -> MapSummary(game, model, hud, onDone = close)

                // Экран-вызов перед стражем (3.92.0)
                RunPhase.CHALLENGE -> hud.challenge?.let { BossChallenge(game, model, run, it, model::runCommand) }
            }
            // A refusal of the gear (2.40.0) has to be read here too: the run has no bar and no banner.
            ToastHost(game, shell::dismissMessage, shell::dismissNotice, Modifier.align(Alignment.TopCenter).statusBarsPadding())
        }
    }
}

/**
 * Снимок захода для тела экрана похода (4.9.0): в бою - без того, что бой меняет каждый кадр (сам бой, пулы и пояс героя,
 * опасности), - их читает только арена ([RunFight]). На карте - без тикающего: здоровья, щита и маны, долей пояса, несомого,
 * урона ловушек, входа в комнату и доли сбора; их читают свои листовые элементы из живого снимка ([MapVitals], [RoadEffects],
 * [LiveFlasks], [LiveHazards], [LiveEntrance]), и экран не перекомпонуется на каждом их шаге. Прочие фазы - как есть.
 */
private fun RunHud.screenOnly(): RunHud = when (phase) {
    RunPhase.FIGHT -> copy(
        fight = null, heroLife = 0, heroShield = 0, heroMaxShield = 0, heroMana = 0, heroMaxMana = 0, heroReserved = 0,
        flasks = emptyList(), hazards = emptyList(), heroAilments = emptyList(), heroEffects = emptyList(), heroCharges = emptyList(), lifeFlow = null,
    )

    RunPhase.MAP -> copy(
        heroLife = 0, heroShield = 0, heroMana = 0, heroReserved = 0,
        flasks = flasks.map { it?.copy(charges = 0, active = 0f) }, hazards = emptyList(), heroAilments = emptyList(), heroEffects = emptyList(),
        heroCharges = emptyList(), lifeFlow = null, entrance = null, feature = feature?.let { it.copy(gathering = it.gathering?.let { 0f }) },
    )

    else -> this
}

/** Полосы героя на карте из живого снимка: тикают сами, не трогая панель. */
@Composable private fun MapVitals(live: State<RunHud>, modifier: Modifier) {
    val now by remember(live) { derivedStateOf { live.value.let { VitalsView(it.heroLife, it.heroMaxLife, it.heroShield, it.heroMaxShield, it.heroMana, it.heroMaxMana, it.heroReserved, it.lifeFlow) } } }
    var breakdown by remember { mutableStateOf(false) }
    Vitals(now.life, now.maxLife, now.shield, now.maxShield, modifier, now.mana, now.maxMana, now.reserved, flow = now.flow) { breakdown = true }
    if (breakdown) LifeFlowSheet({ live.value.lifeFlow }) { breakdown = false }
}

/** Полосы героя одним значением: перекомпоновка - лишь когда сменилось число. */
private data class VitalsView(val life: Int, val maxLife: Int, val shield: Int, val maxShield: Int, val mana: Int, val maxMana: Int, val reserved: Int, val flow: LifeFlow?)

/** Пояс на карте из живого снимка. */
@Composable private fun LiveFlasks(live: State<RunHud>, onDrink: (Int) -> Unit) {
    val flasks by remember(live) { derivedStateOf { live.value.flasks } }
    MapFlasks(flasks, onDrink)
}

/** Урон карты над героем из живого снимка. */
@Composable private fun LiveHazards(live: State<RunHud>) {
    val hazards by remember(live) { derivedStateOf { live.value.hazards } }
    HazardFloat(hazards)
}

/** Строка входа в комнату из живого снимка. */
@Composable private fun LiveEntrance(live: State<RunHud>) {
    val cue by remember(live) { derivedStateOf { live.value.entrance } }
    EntranceFloat(cue)
}

/** Арена захода по живому снимку [live] (4.9.0): кадр боя перекомпонует только её. */
@Composable private fun RunFight(live: State<RunHud>, content: @Composable (RunHud, FightHud) -> Unit) {
    val hud = live.value
    hud.fight?.let { content(hud, it) }
}

// ==================== Walking ====================

/**
 * Life and shield, the map's name and whether its warden still lives, and the way out. Между боями здоровье восстанавливается само
 * (4.10.0, `offCombatRegen`). What the map still holds — foes, chests, fountains — is the walk's to find (2.56.1).
 * The journal's events the server has not taken for a while are counted under the name, a tap sends them now; a slain guardian is not bought back (3.2.0) — it returns in its time.
 */
@Composable internal fun MapBar(
    game: GameUi,
    run: ExpeditionRun,
    hud: RunHud,
    live: State<RunHud>,
    onLeave: (() -> Unit)?,
    onGear: () -> Unit,
    onStats: () -> Unit,
    onDrink: (Int) -> Unit,
    onRetry: () -> Unit,
    onQuests: () -> Unit,
) {
    val expedition: ExpeditionViewModel = koinViewModel()
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // The way out (2.56.1): a portal in a bronze ring, first thing in the corner, and it asks before it goes.
            // The gear right under it, on the same line (2.73.0), and the hero's figures under the gear (2.75.0).
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Меню захода (4.7.0) первым в углу: выход, настройки и жук - в нём, а не отдельными кнопками
                RunMenu(
                    items = listOfNotNull(onLeave?.let { RunMenuItem(ForgeGlyphs.Portal, ui("expedition.leave"), danger = true, onClick = it) }),
                    hold = { HoldsRun(run) },
                )
                RoundButton(ForgeGlyphs.Helm, ui("expedition.gear"), onClick = onGear)
                RoundButton(ForgeGlyphs.Tome, ui("expedition.stats_hero"), onClick = onStats)
                // Задания по ходу захода (3.95.0) - свитком, как доска заданий в городе (3.95.3): флаг читался как «сдаться»
                RoundButton(ForgeGlyphs.Scroll, ui("run.quests"), onClick = onQuests)
            }
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val zone = VaalZones.isZone(run.zone)
                Text(
                    if (zone) ui("vaal.title", mapTitle(hud.mapCode)) else mapTitle(hud.mapCode),
                    color = if (zone) Color(0xFFFF8A78) else GoldBright,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                val guardian = GuardianLine.of(hud, zone)
                Text(guardian.text, color = guardian.color, style = MaterialTheme.typography.labelMedium)
                // Босс карты пал (4.10.0): шаг быстрее - значок под стражем, след за героем рисует сцена
                if (hud.swift) SwiftBadge(run.index.campaign.expedition.bossSlainSpeed)
                // Печать стража (3.93.0): сколько редких пало из нужных
                run.seal?.takeIf { !it.open }?.let { seal ->
                    Text(ui("expedition.seal_progress", seal.killed, seal.need), color = Color(0xFFC9A0FF), style = MaterialTheme.typography.labelSmall)
                }
                // Ворота арены кольца (4.12.8): сколько стай зачищено из нужных
                run.arenaGate?.takeIf { !it.open }?.let { gate ->
                    Text(ui("expedition.gate_progress", gate.cleared, gate.need), color = Color(0xFFF2C94C), style = MaterialTheme.typography.labelSmall)
                }
                Journal(hud, onRetry)
                // Life under the map's name (2.72.0), out of the middle of the view; the mana and the belt under it (2.78.0).
                MapVitals(live, Modifier.fillMaxWidth())
                // Несомое из боя и от ловушек (макет «Эффекты героя на карте», вариант C) - плитками полки, как в бою
                RoadEffects(live)
                // Очаг Скверны (4.0.0): точка, монстры вокруг неё, сундук
                BlightLine(hud.blight)
                if (hud.flasks.any { it != null }) LiveFlasks(live, onDrink)
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
    val expedition: ExpeditionViewModel = koinViewModel()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        flasks.forEach { view ->
            if (view == null) return@forEach
            val tint = flaskTint(view.kind)
            FlaskGain(view.charges, tint) {
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
}

/**
 * The journal, quietly (3.0.0): how many events the server has not taken yet, and how many it refused.
 * Nothing while everything is counted — the run does not talk about its bookkeeping unprompted.
 */
@Composable internal fun Journal(hud: RunHud, onRetry: (() -> Unit)? = null) {
    val expedition: ExpeditionViewModel = koinViewModel()
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

/**
 * Лента боёв автопрохода (3.94.0) - между боями вместо карты: зона, волна из скольких полосой, открытые сундуки и павшие,
 * жизнь героя (между боями она восстанавливается, 4.10.0) и «Завершить» (3.95.2) - уход с собранным, после вопроса.
 */
@Composable internal fun AutoRibbon(run: ExpeditionRun, hud: RunHud, auto: AutoHud, live: State<RunHud>, onFinish: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Panel, Ink))).systemBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("auto.title"), color = Muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
            Text(mapTitle(hud.mapCode), color = GoldBright, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(ui("auto.wave_short", auto.wave, auto.waves), color = Parchment, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { if (auto.waves > 0) auto.wave.toFloat() / auto.waves else 0f },
                modifier = Modifier.fillMaxWidth(.8f).height(6.dp).clip(RoundedCornerShape(50)),
                color = Gold,
                trackColor = Bronze.copy(alpha = .3f),
            )
            Text(ui("auto.tally", auto.chests, hud.kills), color = Muted, style = MaterialTheme.typography.bodyMedium)
            // Нет связи (4.0.0): автопробег стоит и сам пойдёт дальше, когда сервер ответит
            if (auto.offline) Text(ui("auto.offline"), color = LifeRed, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            // Отдых перед волной (MOB-BAL, В-15): фляги и восстановление вне боя, как в ручном заходе
            if (auto.resting) Text(ui("auto.resting"), color = Vital, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            val life by remember(live) { derivedStateOf { live.value.heroLife } }
            Text(ui("auto.life", life, hud.heroMaxLife), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
            AutoFinish(hud, auto, hold = run, onFinish = onFinish) { enabled, label, onClick ->
                ForgeTextButton(onClick = onClick, enabled = enabled) { Text(label, color = LifeRed) }
            }
        }
    }
}

/**
 * «Завершить» автопробег (3.95.2) с вопросом: карта потрачена, и уход оставляет непройденные волны и стража - лист называет
 * их. Завершённый доигрывает идущий бой, кнопка гаснет с причиной. [hold] - заход, что стоит, пока лист открыт.
 */
@Composable internal fun AutoFinish(
    hud: RunHud,
    auto: AutoHud,
    hold: ExpeditionRun? = null,
    onFinish: () -> Unit,
    trigger: @Composable (enabled: Boolean, label: String, onClick: () -> Unit) -> Unit,
) {
    var asking by remember { mutableStateOf(false) }
    trigger(!auto.finishing, ui(if (auto.finishing) "auto.finishing" else "auto.finish")) { asking = true }
    if (!asking) return
    hold?.let { HoldsRun(it) }
    ConfirmSheet(
        title = ui("auto.finish_q"),
        confirm = ui("auto.finish"),
        danger = true,
        ledger = listOf(
            LedgerLine(ui("auto.finish_waves"), ui("auto.wave_short", auto.wave, auto.waves)),
            LedgerLine(ui("auto.finish_guardian"), GuardianLine.of(hud).text, if (hud.sealed) Tone.SPEND else Tone.PLAIN),
        ),
        note = ui("auto.finish_note"),
        onDismiss = { asking = false },
    ) {
        asking = false
        onFinish()
    }
}

@Composable internal fun RunPanel(
    modifier: Modifier,
    accent: Color = Gold,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    // Панель не выше доли экрана (4.7.0): длинное тело (сундук с горой добычи) прокручивается, а [footer] - кнопки - всегда на виду
    val cap = LocalConfiguration.current.screenHeightDp.dp * PANEL_SHARE
    Column(
        modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth().heightIn(max = cap).depthPanel(shape).border(1.dp, accent.copy(alpha = .5f), shape)
            .raritySpine(accent).padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        footer?.invoke(this)
    }
}

/** Доля высоты экрана, выше которой панель забега не растёт. */
private const val PANEL_SHARE = .6f

/** Значок «быстрее» под стражем (4.10.0): босс карты пал - шаг героя × [factor] до конца захода. */
@Composable private fun SwiftBadge(factor: Double) {
    Text(
        ui("run.swift", number(factor)),
        color = Rune,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(Rune.copy(alpha = .12f), RoundedCornerShape(50)).border(1.dp, Rune.copy(alpha = .4f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
