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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.RunMenu
import com.sperance.exileforge.ui.components.RunMenuItem
import com.sperance.exileforge.ui.components.raritySpine
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.LifeFlowSheet
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.quests.QuestViewModel
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
 *
 * Снимок захода живьём - для арены; тело экрана видит его без полей кадра боя ([screenOnly]), тикающее на карте читают листовые элементы из
 * [live]. Уход с карты отдаёт оставшееся, поэтому спрашивается; зона Ваал выхода не имеет, кроме стража или гибели. Конец карты - итоги
 * перед лагерем, их закрытие закрывает заход один раз; «Продать и вернуться» продаёт отмеченную добычу уже после захода. Сундук не
 * останавливает героя: добыча летит в сумку, её лист - по плашке. Бой
 * кончен - его отчёт; гибель - сперва отчёт, затем итоги. Перед стражем - экран-вызов. Отказ снаряжения читается здесь: у захода нет плашки
 * и шапки.
 */
@Composable fun ExpeditionPlay(run: ExpeditionRun) {
    val game by koinViewModel<ExpeditionViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val expedition: ExpeditionViewModel = koinViewModel()
    val model = koinViewModel<ExpeditionViewModel>()
    val live = run.hud.collectAsState()
    val hud by remember(live) { derivedStateOf { live.value.screenOnly() } }
    var gear by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val zone = VaalZones.isZone(run.zone)
    val summary = hud.phase == RunPhase.LEFT || hud.phase == RunPhase.CLEARED || hud.phase == RunPhase.DEAD && hud.report == null
    var closed by remember(run) { mutableStateOf(false) }
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
            hud.phase == RunPhase.MAP -> if (!zone) leaving = true
            else -> model.runCommand(RunCommand.Leave)
        }
    }

    val quests by koinViewModel<QuestViewModel>().quests.collectAsStateWithLifecycle()
    val board = rememberRunBoard(run, model.state.collectAsStateWithLifecycle().value.quests, quests)
    var questsOpen by remember { mutableStateOf(false) }
    var bagOpen by remember(run) { mutableStateOf(false) }
    val clock = rememberSimClock(run)
    CompositionLocalProvider(LocalFightReport provides hud.lastFight) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            ExpeditionScene(run, clock, game.heroClass?.code, Modifier.fillMaxSize())
            BlightWatch(hud.blight, shell::announce)
            if (hud.phase == RunPhase.MAP) LiveHazards(live)
            when (hud.phase) {
                RunPhase.MAP -> {
                    MapDockHost { dock ->
                        Stick(run) { model.runCommand(RunCommand.OfferFountain(it)) }
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
                        if (!dock.occupied) MapBag(game, hud.drops, hud.bag.isNotEmpty(), hud.tally.loot.pieces()) { bagOpen = true }
                        if (bagOpen) RunBagSheet(game, model, run, hud.bag, hud.gold, hud.experience, hud.awaiting) { bagOpen = false }
                        val action = hud.action
                        if (action != null) {
                            DockPanel {
                                ObjectActionPanel(action, {
                                    clock.floatValue
                                    run.pulse.action
                                }) { live.value.action?.progress }
                            }
                        } else {
                            hud.feature?.let { FeatureSheet(game, it, onCommand = model::runCommand) }
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
                        game, frame, fight, fight.level.takeIf { level -> level > 0 } ?: run.run.levelOf(run.zone), run.rules, run.stance, clock,
                        onCommand = model::runCommand, onLogFilter = shell::logFilter, onBuzz = shell::buzz,
                        biome = run.zone.biome,
                        shares = { boss -> BuffSources.of(run.mapEffects, run.run.context.atlas, run.pacts, boss) },
                        mapEffects = run.mapEffects,
                        origins = run.origins,
                        pulse = run.pulse,
                    )
                }

                RunPhase.LOOT -> hud.report?.let { ReportScreen(game, model, hud, it) { model.runCommand(RunCommand.Continue) } }

                RunPhase.DEAD -> hud.report?.let { ReportScreen(game, model, hud, it) { model.runCommand(RunCommand.Continue) } }
                    ?: MapSummary(game, model, hud, onDone = close)

                RunPhase.CLEARED -> MapSummary(game, model, hud, onDone = close)

                RunPhase.GATE -> VaalGate(game, hud, run.zone.corrupted.takeIf { it.value.isNotBlank() }, onEnter = model::enterVaal) { model.runCommand(RunCommand.StepBack) }

                RunPhase.CRYSTAL -> hud.crystal?.let { CrystalSheet(game, it, onCommand = model::runCommand) }

                RunPhase.ABYSS -> hud.abyss?.let { AbyssSheet(game, hud, it, onCommand = model::runCommand) }

                RunPhase.LEFT -> MapSummary(game, model, hud, onDone = close)

                RunPhase.CHALLENGE -> hud.challenge?.let { BossChallenge(game, model, run, it, model::runCommand) }
            }
            ToastBelow(0.dp)
        }
    }
}

/**
 * Снимок захода для тела экрана похода: в бою - без того, что бой меняет каждый кадр (сам бой, пулы и пояс героя,
 * опасности), - их читает только арена ([RunFight]). На карте - без тикающего: здоровья, щита и маны, долей пояса, несомого,
 * урона ловушек и доли действия у объекта; их читают свои листовые элементы из живого снимка ([MapVitals], [RoadEffects],
 * [LiveFlasks], [LiveHazards], [ObjectActionPanel]), и экран не перекомпонуется на каждом их шаге. Прочие фазы - как есть.
 */
private fun RunHud.screenOnly(): RunHud = when (phase) {
    RunPhase.FIGHT -> copy(
        fight = null, heroLife = 0, heroShield = 0, heroMaxShield = 0, heroMana = 0, heroMaxMana = 0, heroReserved = 0,
        flasks = emptyList(), hazards = emptyList(), heroAilments = emptyList(), heroEffects = emptyList(), heroCharges = emptyList(), lifeFlow = null,
    )

    RunPhase.MAP -> copy(
        heroLife = 0, heroShield = 0, heroMana = 0, heroReserved = 0,
        flasks = flasks.map { it?.copy(charges = 0, active = 0f) }, hazards = emptyList(), heroAilments = emptyList(), heroEffects = emptyList(),
        heroCharges = emptyList(), lifeFlow = null, action = action?.let { it.copy(progress = it.progress?.let { 0f }) },
    )

    else -> this
}

/** Полосы героя на карте из живого снимка: тикают сами, не трогая панель. */
@Composable private fun MapVitals(live: State<RunHud>, modifier: Modifier) {
    val now by remember(live) { derivedStateOf { live.value.let { VitalsView(it.heroLife, it.heroMaxLife, it.heroShield, it.heroMaxShield, it.heroMana, it.heroMaxMana, it.heroReserved, it.lifeFlow, it.heroLifeReserved) } } }
    var breakdown by remember { mutableStateOf(false) }
    Vitals(now.life, now.maxLife, now.shield, now.maxShield, modifier, now.mana, now.maxMana, now.reserved, flow = now.flow, lifeReserved = now.lifeReserved) { breakdown = true }
    if (breakdown) LifeFlowSheet({ live.value.lifeFlow }) { breakdown = false }
}

/** Полосы героя одним значением: перекомпоновка - лишь когда сменилось число. */
private data class VitalsView(val life: Int, val maxLife: Int, val shield: Int, val maxShield: Int, val mana: Int, val maxMana: Int, val reserved: Int, val flow: LifeFlow?, val lifeReserved: Int)

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

/** Арена захода по живому снимку [live]: кадр боя перекомпонует только её. */
@Composable private fun RunFight(live: State<RunHud>, content: @Composable (RunHud, FightHud) -> Unit) {
    val hud = live.value
    hud.fight?.let { content(hud, it) }
}

/**
 * Life and shield, the map's name and whether its warden still lives, and the way out. Между боями здоровье восстанавливается само
 * (`offCombatRegen`). What the map still holds — foes, chests, fountains — is the walk's to find.
 * The journal's events the server has not taken for a while are counted under the name, a tap sends them now; a slain guardian returns in its time
 * или призывается за золото (один призыв за заход, [GuardianSummon]).
 *
 * Выход - портал в бронзовом кольце в углу, со спросом; меню захода (выход, настройки, жук) - первым в углу. Задания захода - свитком.
 * Призыв павшего стража за золото - в ручном заходе. Под стражем - значок «быстрее», печать стража и ворота кольца - счёт нужного. Жизнь -
 * под именем карты, мана и пояс - под ней, несомое - плитками полки. Мини-карта открывается по мере разведки, со своим масштабом и полной
 * картой по касанию.
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RunMenu(
                    items = listOfNotNull(onLeave?.let { RunMenuItem(ForgeGlyphs.Portal, ui("expedition.leave"), danger = true, onClick = it) }),
                    hold = { HoldsRun(run) },
                )
                RoundButton(ForgeGlyphs.Helm, ui("expedition.gear"), onClick = onGear)
                RoundButton(ForgeGlyphs.Tome, ui("expedition.stats_hero"), onClick = onStats)
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
                hud.summon?.let { GuardianSummon(game, run, it, mapTitle(hud.mapCode), expedition::runCommand) }
                if (hud.swift) SwiftBadge(run.index.campaign.expedition.bossSlainSpeed)
                run.seal?.takeIf { !it.open }?.let { seal ->
                    Text(ui("expedition.seal_progress", seal.killed, seal.need), color = Color(0xFFC9A0FF), style = MaterialTheme.typography.labelSmall)
                }
                run.arenaGate?.takeIf { !it.open }?.let { gate ->
                    Text(ui("expedition.gate_progress", gate.cleared, gate.need), color = Color(0xFFF2C94C), style = MaterialTheme.typography.labelSmall)
                }
                Journal(hud, onRetry)
                MapVitals(live, Modifier.fillMaxWidth())
                RoadEffects(live)
                BlightLine(hud.blight)
                if (hud.flasks.any { it != null }) LiveFlasks(live, onDrink)
            }
            MiniMap(run, hud)
        }
    }
}

/**
 * The belt on the map: a draught on the road brings its life or mana back at once, and a
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
 * The journal, quietly: how many events the server has not taken yet, and how many it refused.
 * Nothing while everything is counted — the run does not talk about its bookkeeping unprompted.
 *
 * Слова стоит лишь старейшее событие, что сервер давно не берёт: пачка в полёте - не новость.
 */
@Composable internal fun Journal(hud: RunHud, onRetry: (() -> Unit)? = null) {
    val expedition: ExpeditionViewModel = koinViewModel()
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

/** The run stands still for as long as this is in the composition: a window over the map pauses it. */
@Composable internal fun HoldsRun(run: ExpeditionRun) {
    DisposableEffect(run) {
        run.send(RunCommand.Hold(true))
        onDispose { run.send(RunCommand.Hold(false)) }
    }
}

/** A glyph in a bronze ring on dark glass: the map's buttons share one look. */
@Composable internal fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).background(Color(0xE60A0D12), CircleShape).border(1.5.dp, Bronze, CircleShape)) {
        Icon(icon, label, tint = GoldBright, modifier = Modifier.size(24.dp))
    }
}

/** How near, in tiles, a tap must land to a fountain to name it. */
internal const val FOUNTAIN_TAP = .9

/**
 * A fountain offered, walked onto or tapped on the map: what it gives, and the choice — drink it now,
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

/** Панель не выше доли экрана: длинное тело прокручивается, а [footer] с кнопками всегда на виду. */
@Composable internal fun RunPanel(
    modifier: Modifier,
    accent: Color = Gold,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
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

/** Значок «быстрее» под стражем: босс карты пал - шаг героя × [factor] до конца захода. */
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
