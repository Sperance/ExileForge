package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.TAB_EXPEDITION
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.rules.content.CampaignFile
import com.sperance.exileforge.rules.content.WorldGraph
import com.sperance.exileforge.rules.content.WorldPoint
import com.sperance.exileforge.ui.TAB_SHIFT_MS
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.world.WorldArt
import com.sperance.exileforge.ui.screens.expedition.world.WorldCamera
import com.sperance.exileforge.ui.screens.expedition.world.WorldCanvas
import com.sperance.exileforge.ui.screens.quests.QuestViewModel
import com.sperance.exileforge.ui.screens.quests.StoryButton
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Where a picked zone's token is flown to while its card covers the map's foot: this share down the screen. */
private const val CARD_DOWN = .28f

/** How far down the map a token can sit before the card would hide it. */
private const val CARD_TOP = .48f

/**
 * The expedition tab: the world map, the owner's pick «Пергамент» of three mockups.
 *
 * The zones are tokens on a parchment chart, linked from the start upward: a passed one is ticked,
 * an open one glows, the «???» one past it is dark with its level, and the fog hides the rest. The
 * map opens on the frontier; it drags and pinches, «+» and «−» zoom it and the crosshair flies back
 * to the frontier. A tapped token raises its card over the map's foot — the way into the zone.
 *
 * Сюжет - над картой с первого уровня, доска заданий читается и без раздела, если её ещё нет. Вкладка нажата снова - лист зоны закрывается.
 * Первый кадр вкладки - голая карта, карта собирается, когда смена вкладки доехала ([TAB_SHIFT_MS]). Карта открывается там, где герой её
 * оставил (на устройстве), впервые - на фронтире; камера запоминается после паузы, не на каждый кадр жеста. Зона шага сюжета - руническое
 * кольцо без запросов.
 */
@Composable fun ExpeditionScreen() {
    val game by koinViewModel<ExpeditionViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<ExpeditionViewModel>()
    val expedition by vm.state.collectAsStateWithLifecycle()
    val questModel = koinViewModel<QuestViewModel>()
    val quests by questModel.quests.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        vm.loadCampaign()
        if (game.heroId.isNotBlank()) questModel.glance()
    }
    OnReselect(TAB_EXPEDITION) { vm.closeZone() }
    val index = game.index
    val progress = game.progress
    if (index == null || progress == null) {
        Box(Modifier.fillMaxSize().padding(16.dp)) { InfoCard(ui("common.loading"), ui("expedition.loading_hint")) }
        return
    }
    val campaign = index.campaign
    val world = remember(index, progress.cleared, progress.unlocked) { LastWorld.of(campaign, index.world, progress) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(TAB_SHIFT_MS.toLong())
        settled = true
    }
    val art by produceState(WorldArt.cached(campaign), campaign) { value = WorldArt.of(campaign) }
    val density = LocalDensity.current.density
    val camera = remember(campaign.world, density) { WorldCamera(campaign.world, density) }
    val scope = rememberCoroutineScope()
    val launch = expedition.launch?.takeIf { world.token(it.mapCode) != null }
    val stash = remember(game.hero?.items, index) { stashCounts(game) }
    BackHandler(launch != null) { vm.closeZone() }
    LaunchedEffect(camera, camera.viewport) {
        if (camera.placed || camera.viewport == IntSize.Zero) return@LaunchedEffect
        val kept = vm.worldView(game.heroId)?.takeIf { launch == null }
        if (kept != null) camera.restore(kept) else camera.look(world.frontier(), WorldCamera.HOME, if (launch != null) CARD_DOWN else .5f)
    }
    LaunchedEffect(camera, game.heroId) {
        snapshotFlow { if (camera.placed) camera.view() else null }.filterNotNull().distinctUntilChanged().collectLatest { view ->
            delay(KEEP_VIEW_MS)
            vm.keepWorldView(game.heroId, view)
        }
    }
    LaunchedEffect(launch?.mapCode, camera.placed) {
        val zone = launch?.let { world.token(it.mapCode) }?.zone ?: return@LaunchedEffect
        val at = WorldPoint(zone.x, zone.y)
        if (camera.placed && !camera.sees(at, 48f, CARD_TOP)) camera.glide(at, maxOf(camera.scale, WorldCamera.HOME), CARD_DOWN)
    }
    Box(Modifier.fillMaxSize()) {
        if (!settled) {
            Box(Modifier.fillMaxSize().background(CHART_BARE))
        } else {
            val story = quests.board?.let { board -> game.index?.quests?.story?.getOrNull(board.chapter)?.steps?.getOrNull(board.step)?.zone?.value?.takeIf { it != com.sperance.exileforge.rules.content.MapCode.NONE.value } }
            WorldCanvas(world, art, camera, launch?.mapCode?.value, stash, Modifier.fillMaxSize(), story = story) { code -> if (code == null) vm.closeZone() else vm.selectZone(code) }
        }
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            WorldBar(
                Modifier,
                onFrontier = { scope.launch { camera.glide(world.frontier(), WorldCamera.HOME, if (launch != null) CARD_DOWN else .5f) } },
            )
            Column(Modifier.align(Alignment.End).padding(end = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StoryButton(game, quests.board, questModel)
                ZoomButtons(camera, Modifier) { factor -> scope.launch { camera.zoomBy(factor) } }
            }
        }
        launch?.let { ZoneCard(game, vm, world, it, expedition.forced, Modifier.align(Alignment.BottomCenter)) }
    }
}

/**
 * Последняя карта мира: вход на вкладку с тем же прогрессом берёт её же, и с ней - уже измеренные надписи
 * холста, так что «Поход» не строит и не меряет мир на каждое нажатие.
 */
private object LastWorld {
    @Volatile private var last: Triple<WorldGraph, CampaignProgress, WorldMap>? = null

    fun of(campaign: CampaignFile, graph: WorldGraph, progress: CampaignProgress): WorldMap = last
        ?.takeIf { (g, p, map) -> map.campaign === campaign && g === graph && p == progress }?.third
        ?: WorldMap(campaign, graph, progress).also { last = Triple(graph, progress, it) }
}

/** The map's head: the title and the way back to the frontier. */
@Composable private fun WorldBar(modifier: Modifier, onFrontier: () -> Unit) {
    Row(
        modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .92f), Ink.copy(alpha = 0f))))
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(ui("expedition.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        OutlinedIconButton(onClick = onFrontier, border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .4f)), modifier = Modifier.size(38.dp)) {
            Icon(ForgeGlyphs.Target, ui("expedition.frontier"), tint = GoldBright, modifier = Modifier.size(20.dp))
        }
    }
}

/** «+» and «−»: nearer and farther by a step, about the middle of what is seen. */
@Composable private fun ZoomButtons(camera: WorldCamera, modifier: Modifier, onZoom: (Float) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.background(Abyss.copy(alpha = .88f), shape).border(1.dp, Gold.copy(alpha = .4f), shape)) {
        IconButton(onClick = { onZoom(ZOOM_STEP) }, enabled = camera.canZoomIn, modifier = Modifier.size(40.dp)) {
            Icon(ForgeGlyphs.Plus, ui("expedition.zoom_in"), tint = if (camera.canZoomIn) GoldBright else Muted, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(Modifier.width(40.dp), color = Gold.copy(alpha = .25f))
        IconButton(onClick = { onZoom(1 / ZOOM_STEP) }, enabled = camera.canZoomOut, modifier = Modifier.size(40.dp)) {
            Icon(ForgeGlyphs.Minus, ui("expedition.zoom_out"), tint = if (camera.canZoomOut) GoldBright else Muted, modifier = Modifier.size(20.dp))
        }
    }
}

private const val ZOOM_STEP = 1.35f

/** Сколько камера стоит, прежде чем запомниться, мс. */
private const val KEEP_VIEW_MS = 600L

/** The chart's own colour, shown for the one frame before the map is laid out. */
private val CHART_BARE = androidx.compose.ui.graphics.Color(0xFF1B150D)
