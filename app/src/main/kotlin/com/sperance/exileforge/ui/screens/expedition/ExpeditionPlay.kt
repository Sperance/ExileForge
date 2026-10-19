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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.mapTitle
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
 * останавливает героя: добыча летит в сумку, её лист - по кнопке сумки в шапке. Бой
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
    val close: (sell: List<String>, keep: List<String>) -> Unit = { sell, keep ->
        if (!closed) {
            closed = true
            model.closeRun(sell, keep)
        }
    }
    BackHandler {
        when {
            summary -> Unit
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
                    val flight = rememberBagFlight(hud.drops)
                    Stick(run) { model.runCommand(RunCommand.OfferFountain(it)) }
                    MapBar(
                        game,
                        run,
                        hud,
                        live,
                        bag = { MapBag(flight, hud.drops, hud.bag.isNotEmpty(), hud.tally.loot.pieces(), MINIMAP_SIDE) { bagOpen = true } },
                        onLeave = if (zone) null else ({ leaving = true }),
                        onGear = { gear = true },
                        onStats = { sheet = true },
                        onDrink = { model.runCommand(RunCommand.Drink(it)) },
                        onRetry = model::flushRun,
                        onQuests = { questsOpen = true },
                    )
                    BagFan(flight)
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
                    hud.fountain?.let { FountainOffer(it, onTake = { model.runCommand(RunCommand.TakeFountain) }) { model.runCommand(RunCommand.StepOff) } }
                    if (bagOpen) RunBagSheet(game, model, run, hud.bag, hud.gold, hud.experience, hud.awaiting) { bagOpen = false }
                    val action = hud.action
                    if (action != null) {
                        ObjectActionPanel(action, {
                            clock.floatValue
                            run.pulse.action
                        }) { live.value.action?.progress }
                    } else {
                        hud.feature?.let { FeatureSheet(game, it, onCommand = model::runCommand) }
                    }
                    if (leaving) {
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

                RunPhase.FIGHT -> RunFight(live) { frame, fight ->
                    ArenaOverlay(
                        game, frame, fight, fight.level.takeIf { level -> level > 0 } ?: run.run.levelOf(run.zone), run.rules, run.stance, clock,
                        onCommand = model::runCommand, onLogFilter = shell::logFilter, onBuzz = shell::buzz,
                        biome = run.zone.biome,
                        shares = { boss -> BuffSources.of(run.mapSources, run.runLines, run.pacts, boss) },
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

/** Сторона фляги пояса в шапке карты. */
private val BELT_FLASK = 40.dp

/** Пояс на карте из живого снимка. */
@Composable private fun LiveFlasks(live: State<RunHud>, onDrink: (Int) -> Unit) {
    val flasks by remember(live) { derivedStateOf { live.value.flasks } }
    MapFlasks(flasks, BELT_FLASK, RoundedCornerShape(10.dp), onDrink)
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
 * Шапка карты - пульт и сведения на одной вуали, что гаснет к карте; низ экрана целиком под стик ([Stick]). Первый ряд - пульт:
 * меню захода (выход), имя карты, снаряжение, герой, задания. Ниже слева - метки состояния ([MapMarks]: страж, печать, ворота,
 * Скверна, журнал), призыв павшего стража за золото ([GuardianSummon]), полосы героя с итогом здоровья в секунду, пояс и плитки
 * несомого; справа - мини-карта со своим масштабом и кнопка сумки ([bag]). Нижние панели карты шапку не прячут.
 */
@Composable internal fun MapBar(
    game: GameUi,
    run: ExpeditionRun,
    hud: RunHud,
    live: State<RunHud>,
    bag: @Composable () -> Unit,
    onLeave: (() -> Unit)?,
    onGear: () -> Unit,
    onStats: () -> Unit,
    onDrink: (Int) -> Unit,
    onRetry: () -> Unit,
    onQuests: () -> Unit,
) {
    val zone = VaalZones.isZone(run.zone)
    val expedition: ExpeditionViewModel = koinViewModel()
    Column(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(0f to VeilTop, .72f to VeilMid, 1f to Color.Transparent))
            .statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MapPult(run, hud, zone, onLeave, onGear, onStats, onQuests)
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MapMarks(run, hud, zone, onRetry)
                hud.summon?.let { GuardianSummon(game, run, it, mapTitle(hud.mapCode), expedition::runCommand) }
                MapVitals(live, Modifier.fillMaxWidth())
                if (hud.flasks.any { it != null }) LiveFlasks(live, onDrink)
                RoadEffects(live)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniMap(run, hud)
                bag()
            }
        }
    }
}

/** Пульт шапки карты: меню захода, имя карты (до двух строк, полное - подсказкой по касанию), снаряжение, герой, задания. */
@Composable private fun MapPult(
    run: ExpeditionRun,
    hud: RunHud,
    zone: Boolean,
    onLeave: (() -> Unit)?,
    onGear: () -> Unit,
    onStats: () -> Unit,
    onQuests: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RunMenu(
            items = listOfNotNull(onLeave?.let { RunMenuItem(ForgeGlyphs.Portal, ui("expedition.leave"), danger = true, onClick = it) }),
            hold = { HoldsRun(run) },
            trigger = { open -> PultButton(Icons.Outlined.Menu, ui("run.menu"), ButtonSize.Compact, open) },
        )
        MapName(if (zone) ui("vaal.title", mapTitle(hud.mapCode)) else mapTitle(hud.mapCode), if (zone) VaalName else GoldBright, Modifier.weight(1f))
        PultButton(ForgeGlyphs.Helm, ui("expedition.gear"), ButtonSize.Wide, onGear)
        PultButton(ForgeGlyphs.Tome, ui("expedition.stats_hero"), ButtonSize.Wide, onStats)
        PultButton(ForgeGlyphs.Scroll, ui("run.quests"), ButtonSize.Wide, onQuests)
    }
}

/** Имя карты в пульте: мельче заголовка, до двух строк; касание - полное имя подсказкой. */
@Composable private fun MapName(name: String, tint: Color, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.heightIn(min = 44.dp).clickable { open = true }.padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            name,
            color = tint,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 13.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (open) TipCallout(Tip(name, tint = tint)) { open = false }
    }
}

/** Кнопка пульта: тональный скруглённый квадрат [size] в зоне касания 44 dp. */
@Composable private fun PultButton(icon: ImageVector, label: String, size: ButtonSize, onClick: () -> Unit) {
    ForgeIconButton(icon, label, onClick = onClick, kind = ButtonKind.Secondary, size = size)
}

/** Вуаль под шапкой карты: плотная у края экрана, гаснет к карте. */
private val VeilTop = Color(0xF0080B0E)
private val VeilMid = Color(0xDB080B0E)

/** Имя карты зоны Ваал. */
private val VaalName = Color(0xFFFF8A78)

/**
 * The belt on the map: a draught on the road brings its life or mana back at once, and a
 * utility flask's lines run as the hero walks. A flask is filled to its charges and ringed while it runs.
 * [cell] - сторона фляги, [shape] - её форма: на карте - квадрат 40dp в шапке, в листе Бездны - малый круг.
 */
@Composable internal fun MapFlasks(flasks: List<FlaskView?>, cell: Dp = 34.dp, shape: Shape = CircleShape, onDrink: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (cell > 34.dp) 6.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
        flasks.forEach { view ->
            if (view == null) return@forEach
            val tint = flaskTint(view.kind)
            FlaskGain(view.charges, tint) {
                Box(
                    Modifier.size(cell).clip(shape).background(Color(0xE60A0D12))
                        .border(if (view.active > 0f) 2.dp else 1.dp, if (view.active > 0f) GoldBright else Bronze, shape)
                        .clickable(enabled = view.usable) { onDrink(view.slot) }.semantics { contentDescription = ui("expedition.drink") },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val fill = if (view.maxCharges > 0) view.charges / view.maxCharges.toFloat() else 0f
                        drawRect(tint.copy(alpha = if (view.usable) .55f else .25f), topLeft = Offset(0f, size.height * (1 - fill)), size = Size(size.width, size.height * fill))
                        if (view.active > 0f) drawArc(GoldBright, -90f, 360f * view.active, false, style = Stroke(2.5.dp.toPx()))
                    }
                    Text("${view.charges}", color = GoldBright, fontSize = if (cell > 34.dp) 12.sp else 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** The run stands still for as long as this is in the composition: a window over the map pauses it. */
@Composable internal fun HoldsRun(run: ExpeditionRun) {
    DisposableEffect(run) {
        run.send(RunCommand.Hold(true))
        onDispose { run.send(RunCommand.Hold(false)) }
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
            Text(ui("fountain.offer", fountain.heal.roundToInt(), fountain.flasks.roundToInt()), color = ShieldCyan, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ForgeButton(onClick = onLeave, modifier = Modifier.weight(1f), kind = ButtonKind.Secondary) { Text(ui("fountain.leave")) }
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
