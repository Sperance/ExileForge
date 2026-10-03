package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.WorldPoint
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.world.WorldArt
import com.sperance.exileforge.ui.screens.expedition.world.WorldCamera
import com.sperance.exileforge.ui.screens.expedition.world.WorldCanvas
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.launch

/** Where a picked zone's token is flown to while its card covers the map's foot: this share down the screen. */
private const val CARD_DOWN = .28f

/** How far down the map a token can sit before the card would hide it. */
private const val CARD_TOP = .48f

/**
 * The expedition tab (2.76.0): the world map, the owner's pick «Пергамент» of three mockups.
 *
 * The zones are tokens on a parchment chart, linked from the start upward: a passed one is ticked,
 * an open one glows, the «???» one past it is dark with its level, and the fog hides the rest. The
 * map opens on the frontier; it drags and pinches, «+» and «−» zoom it and the crosshair flies back
 * to the frontier. A tapped token raises its card over the map's foot — the way into the zone.
 */
@Composable fun ExpeditionScreen(s: ForgeState, vm: ForgeViewModel) {
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) {
        vm.ensureHero()
        vm.loadCampaign()
    }
    FirstVisit(Guide.EXPEDITION)
    val index = s.index
    val progress = s.progress
    if (index == null || progress == null) {
        Box(Modifier.fillMaxSize().padding(16.dp)) { InfoCard(ui("common.loading"), ui("expedition.loading_hint")) }
        return
    }
    val campaign = index.campaign
    val world = remember(index, progress.cleared, progress.unlocked) { WorldMap(campaign, index.world, progress) }
    // The tab answers the tap at once (3.77.0): its first frame is the bare chart, the map is laid out on the next.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        settled = true
    }
    val art by produceState(WorldArt.cached(campaign), campaign) { value = WorldArt.of(campaign) }
    val density = LocalDensity.current.density
    val camera = remember(campaign.world, density) { WorldCamera(campaign.world, density) }
    val scope = rememberCoroutineScope()
    val launch = s.play.launch?.takeIf { world.token(it.mapCode) != null }
    val stash = remember(s.hero?.items, index) { stashCounts(s) }
    BackHandler(launch != null) { vm.closeZone() }
    // The map opens on the frontier; a zone picked elsewhere — a map's sheet in the stash — is flown to above its card.
    LaunchedEffect(camera, camera.viewport) {
        if (!camera.placed && camera.viewport != IntSize.Zero) camera.look(world.frontier(), WorldCamera.HOME, if (launch != null) CARD_DOWN else .5f)
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
            WorldCanvas(world, art, camera, launch?.mapCode, stash, Modifier.fillMaxSize()) { code -> if (code == null) vm.closeZone() else vm.selectZone(code) }
        }
        WorldBar(
            world,
            Modifier.align(Alignment.TopCenter),
            onFrontier = { scope.launch { camera.glide(world.frontier(), WorldCamera.HOME, if (launch != null) CARD_DOWN else .5f) } },
        )
        ZoomButtons(camera, Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 12.dp)) { factor -> scope.launch { camera.zoomBy(factor) } }
        launch?.let { ZoneCard(s, vm, world, it, Modifier.align(Alignment.BottomCenter)) }
    }
}

/** The map's head: how much of the world is passed and the way back to the frontier; the atlas and the trials live in «Развитие». */
@Composable private fun WorldBar(world: WorldMap, modifier: Modifier, onFrontier: () -> Unit) {
    Row(
        modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .92f), Ink.copy(alpha = 0f))))
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(ui("expedition.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Text(ui("expedition.passed", world.passedCount, world.total), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
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

/** The chart's own colour, shown for the one frame before the map is laid out. */
private val CHART_BARE = androidx.compose.ui.graphics.Color(0xFF1B150D)
