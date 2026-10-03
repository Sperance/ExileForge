package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.atlas.AtlasFog
import com.sperance.exileforge.core.display.atlasNodeTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.AtlasNode
import com.sperance.exileforge.rules.content.AtlasNodeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The night the atlas is drawn on (2.68.0, the owner's mockup I «Звёздная карта»). */
private object Sky {
    val deep = Color(0xFF030408)
    val night = Color(0xFF070A12)
    val dawn = Color(0xFF10182A)
    val line = Color(0xFF2B3A57)
    val faint = Color(0xFF96A5C8)
    val text = Color(0xFFC9D8EF)
    val take = Color(0xFF3B6FA8)
}

/** Each trunk's own light, so a player reads which direction a star belongs to before its name. */
private fun AtlasBranch.hue(): Color = when (this) {
    AtlasBranch.LOOT -> Color(0xFFE8CF94)
    AtlasBranch.MAPS -> Color(0xFF8EC5FF)
    AtlasBranch.TIERS -> Color(0xFFBFE3EF)
    AtlasBranch.BOSSES -> Color(0xFFB48CFF)
    AtlasBranch.ABYSS -> Color(0xFFA070FF)
    AtlasBranch.VAAL -> Color(0xFFFF5A46)
    AtlasBranch.CRYSTALS -> Color(0xFF9FD8E8)
    AtlasBranch.EXPEDITION -> Color(0xFF7FC8A0)
    AtlasBranch.CRAFT -> Color(0xFFE8A05A)
    AtlasBranch.POWER -> Color(0xFFD24A43)
    AtlasBranch.INFLUENCE -> Color(0xFF9FD2F0)
    AtlasBranch.ROOT -> Color.White
}

/** Which trunk a node belongs to, read off its code. */
private val AtlasNode.branch: AtlasBranch get() = AtlasFog.branch(code)

/**
 * The atlas tree (2.68.0) as a night sky: every node a star, the taken ones burning and joined by lit
 * threads, the ones a point could take next twinkling. The tree is the content's (3.0.0) and the hero's
 * nodes and points are the hero's own; [AtlasFog] only foresees the server's rule so a refused command
 * shows no button. The sky is dragged and pinched; a tap picks the nearest star within reach, and its
 * sheet takes or gives it.
 */
@Composable fun AtlasScreen(s: ForgeState, vm: ForgeViewModel) {
    val atlas = s.play.atlas ?: return
    BackHandler(onBack = vm::closeAtlas)
    val index = s.index
    val state = s.atlasState
    var resetting by remember { mutableStateOf(false) }
    // A node given back asks first, held to confirm (3.2.0): the points come back, the gold does not
    var refunding by remember { mutableStateOf<String?>(null) }
    // «Итого» (3.54.0): every taken node's lines added up, by mechanic.
    var summary by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Sky.deep, Sky.night, Sky.dawn)))) {
        if (index == null || state == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Sky.text)
        } else {
            val taken = state.allocated.toSet()
            Sky(index, taken, atlas.selected, Modifier.fillMaxSize(), onSelect = vm::selectAtlasNode)
            // The points float over the sky, stacked above the node's sheet so neither hides the other.
            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                val node = index.atlasGraph.node(atlas.selected)
                PointsPill(state.available, state.points, if (node == null) Modifier.navigationBarsPadding().padding(bottom = 16.dp) else Modifier)
                node?.let {
                    NodeSheet(
                        index, it, taken, state.available, index.atlas.respec.price(s.heroLevel, 1), enabled = !s.busy,
                        onTake = { vm.allocateAtlas(it.code) }, onRefund = { refunding = it.code }, modifier = Modifier,
                    )
                }
            }
        }
        // The start is nobody's to give back: what was spent is every taken node but it.
        val spent = (state?.allocated?.size ?: 1) - 1
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = vm::closeAtlas) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.close"), tint = Sky.text) }
            Text(ui("atlas.title"), color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FirstVisit(Guide.ATLAS)
            AtlasMenu(summary = spent > 0, reset = spent > 0 && !s.busy, onSummary = { summary = true }, onReset = { resetting = true })
        }
        val money = s.hero?.money ?: 0L
        val regrets = s.hero?.count(Orb.ORB_OF_REGRET.name) ?: 0L
        if (resetting && index != null && state != null) {
            val nodes = state.allocated.size - 1
            RespecSheet(
                ui("atlas.reset_q"),
                null,
                ui("atlas.reset"),
                nodes,
                index.atlas.respec.price(s.heroLevel, nodes),
                money,
                regrets,
                onDismiss = { resetting = false },
            ) { regret ->
                resetting = false
                vm.resetAtlas(regret)
            }
        }
        refunding?.let { code ->
            if (index != null) {
                RespecSheet(
                    ui("atlas.refund_q"),
                    atlasNodeTitle(code),
                    ui("atlas.refund"),
                    1,
                    index.atlas.respec.price(s.heroLevel, 1),
                    money,
                    regrets,
                    onDismiss = { refunding = null },
                ) { regret ->
                    refunding = null
                    vm.refundAtlas(code, regret)
                }
            }
        }
        if (summary && index != null && state != null) AtlasSummary(index, state.allocated.toSet()) { summary = false }
        ToastHost(s, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp))
    }
}

/**
 * Giving [nodes] atlas nodes back: [gold] at the respec price or, since server 1.65.0 and when the bag holds [regrets],
 * an Orb of Regret per node — the player picks; a payment the hero cannot make keeps the button off and says why.
 */
@Composable private fun RespecSheet(
    title: String,
    subtitle: String?,
    confirm: String,
    nodes: Int,
    gold: Long,
    money: Long,
    regrets: Long,
    onDismiss: () -> Unit,
    onConfirm: (regret: Boolean) -> Unit,
) {
    var regret by remember { mutableStateOf(false) }
    val short = if (regret) regrets < nodes else money < gold
    val cost = if (regret) "$nodes × ${itemTitle(Orb.ORB_OF_REGRET.name)}" else ui("atlas.gold", gold)
    ConfirmSheet(
        title = title, subtitle = subtitle, confirm = confirm, danger = true, onDismiss = onDismiss,
        ledger = listOf(LedgerLine(ui("atlas.reset_cost"), cost, Tone.SPEND), LedgerLine(ui("atlas.reset_back"), nodes.toString(), Tone.GAIN)),
        blocked = short,
        warning = when {
            !short -> null
            regret -> ui("atlas.no_regret")
            else -> ui("atlas.no_gold")
        },
        options = if (regrets > 0) {
            (
                {
                    PillTabs(listOf(ui("atlas.pay_gold"), ui("atlas.pay_regret", regrets, nodes)), if (regret) 1 else 0, { regret = it == 1 }, segmented = true)
                }
                )
        } else {
            null
        },
    ) { onConfirm(regret) }
}

/** «Доступно очков: X из Y», a pill floating over the bottom of the sky. */
@Composable private fun PointsPill(available: Int, points: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        ui("atlas.points", available, points),
        color = if (available > 0) Color.White else Sky.text,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.background(Color(0xE6080C16), shape).border(1.dp, Sky.line, shape).padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** The header's ⋮: the totals, the reset, the guide and the bug report, so the title keeps the bar. */
@Composable private fun AtlasMenu(summary: Boolean, reset: Boolean, onSummary: () -> Unit, onReset: () -> Unit) {
    val guides = LocalGuideDesk.current
    val bug = LocalBugReport.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, ui("common.more"), tint = Sky.text) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val entries = listOfNotNull(
                Triple(ui("atlas.summary"), summary, onSummary),
                Triple(ui("atlas.reset"), reset, onReset),
                guides?.let { desk -> Triple(ui("guide.help"), true) { desk.show(Guide.ATLAS) } },
                bug?.let { Triple(ui("bug.open"), true, it) },
            )
            entries.forEach { (label, enabled, action) ->
                DropdownMenuItem(text = { Text(label) }, enabled = enabled, onClick = {
                    open = false
                    action()
                })
            }
        }
    }
}

/** The sky itself: faint stars behind, the threads, then the nodes; dragged, pinched and tapped. */
@Composable private fun Sky(index: ContentIndex, taken: Set<String>, selected: String, modifier: Modifier, onSelect: (String) -> Unit) {
    val graph = index.atlasGraph
    val nodes = index.atlas.nodes
    val bounds = remember(nodes) { SkyBounds.of(nodes) }
    val density = LocalDensity.current
    val floor = with(density) { 240.dp.toPx() }
    val margin = with(density) { 28.dp.toPx() }
    // The sky opens close (3.24.0): at [OPEN_ZOOM], the start in the middle of what the sheet leaves open.
    var scale by remember { mutableFloatStateOf(OPEN_ZOOM) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var framed by remember { mutableStateOf(false) }
    // The clock (3.56.0) ticks [CLOCK_FPS] times a second, not on every frame: a twinkle needs no 120 Hz, and only the
    // layers that breathe are drawn on its tick — the glows, threads and rings are drawn again on a pan, a zoom or a take.
    val clock by produceState(0f) {
        var start = 0L
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                if (now - last >= CLOCK_STEP_NS) {
                    last = now
                    value = (now - start) / 1e9f
                }
            }
        }
    }
    val reachable = remember(taken, graph) { nodes.filter { AtlasFog.canTake(graph, it.code, taken) }.map { it.code }.toSet() }
    // No fog (3.54.0): the whole sky is drawn and tappable; what cannot be taken yet is only dim.
    val shown = nodes
    val glows = remember(nodes) { Glow.of(nodes) }
    // The threads once (3.56.0): both ends of every link, not a graph lookup per link per frame.
    val strands = remember(nodes, graph) { nodes.flatMap { node -> node.parents.mapNotNull { parent -> graph.node(parent)?.let { Strand(it, node) } } } }
    val brushes = remember(nodes) { SkyBrushes() }
    // The tap reads the zoom and the pan as they are when the finger lands (3.56.0): the gesture is not restarted on every frame of a drag.
    Box(
        modifier.clipToBounds()
            .onSizeChanged { size ->
                if (framed || size.width == 0) return@onSizeChanged
                val start = nodes.firstOrNull { it.kind == AtlasNodeKind.START } ?: return@onSizeChanged
                val width = size.width.toFloat()
                val height = size.height.toFloat()
                val at = Placement(bounds, width, height, floor, margin, scale, Offset.Zero)(start)
                // The atlas grows upward from its start (3.52.0): the start sits low, the branches fan out above it.
                pan = Offset(width / 2 - at.x, (height - floor) * START_DOWN - at.y)
                framed = true
            }
            .pointerInput(nodes) {
                detectTransformGestures { centroid, drag, zoom, _ ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val next = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    // A pinch zooms about the fingers — the point under them stays there — and the drag pans on top of it.
                    val anchor = Offset(width / 2, height - floor)
                    val moved = centroid - anchor - (centroid - anchor - pan) * (next / scale) + drag
                    scale = next
                    pan = Placement(bounds, width, height, floor, margin, next, moved).held()
                }
            }
            .pointerInput(shown) {
                detectTapGestures { tap ->
                    val place = Placement(bounds, size.width.toFloat(), size.height.toFloat(), floor, margin, scale, pan)
                    val hit = shown.minByOrNull { (place(it) - tap).getDistanceSquared() } ?: return@detectTapGestures
                    if ((place(hit) - tap).getDistance() <= TAP_RADIUS.toPx()) onSelect(hit.code)
                }
            },
    ) {
        // The far stars breathe on their own layer, behind everything.
        Canvas(Modifier.matchParentSize()) { stars(clock) }
        // The still sky: the glows, the threads, the rings and the cores of what is taken or dim — no clock is read here,
        // so this layer is drawn again only when the view, the taken set or the choice moves.
        Canvas(Modifier.matchParentSize()) {
            val place = Placement(bounds, size.width, size.height, floor, margin, scale, pan)
            // The constellations (3.53.0): a soft glow of its mechanic's hue under every one.
            glows.forEach { glow ->
                val c = place(glow.center)
                val r = glow.radius * placeScale(place)
                translate(c.x, c.y) { drawCircle(brushes.glow(glow.hue, r), r, Offset.Zero) }
            }
            strands.forEach { strand ->
                val lit = strand.node.code in taken && strand.parent.code in taken
                drawLine(if (lit) strand.node.branch.hue() else Sky.faint.copy(alpha = .22f), place(strand.parent), place(strand.node), (if (lit) 2.dp else 1.dp).toPx())
            }
            shown.forEach { starStill(it, place(it), it.code in taken, it.code in reachable, it.code == selected, brushes) }
        }
        // The breathing layer: the twinkle of what a point could take and the keystones' orbiting motes.
        Canvas(Modifier.matchParentSize()) {
            val place = Placement(bounds, size.width, size.height, floor, margin, scale, pan)
            shown.forEach { starLive(it, place(it), it.code in taken, it.code in reachable, clock) }
        }
    }
}

/** One thread of the sky: a node and the parent it hangs from. */
private class Strand(val parent: AtlasNode, val node: AtlasNode)

/** The gradients of the sky, made once per hue and radius (3.56.0): a shader was built for every glow and halo on every frame. */
private class SkyBrushes {
    private val glows = HashMap<Pair<Color, Float>, Brush>()
    private val halos = HashMap<Pair<Color, Float>, Brush>()

    /** A constellation's glow of [hue] spreading [r] pixels, centred at the origin: drawn under a translation. */
    fun glow(hue: Color, r: Float): Brush = glows.getOrPut(hue to r) { Brush.radialGradient(listOf(hue.copy(alpha = .16f), Color.Transparent), Offset.Zero, r) }

    /** A taken or chosen star's halo of [hue] reaching [r] pixels, centred at the origin. */
    fun halo(hue: Color, r: Float): Brush = halos.getOrPut(hue to r) { Brush.radialGradient(listOf(hue.copy(alpha = .55f), Color.Transparent), Offset.Zero, r) }
}

/** How often the sky's clock ticks. */
private const val CLOCK_FPS = 20
private const val CLOCK_STEP_NS = 1_000_000_000L / CLOCK_FPS

/**
 * How far the sky zooms out and in, and where it opens. With [SPREAD] the closest pair of the server's atlas (4.6 units,
 * server 1.65.0) is some 30 dp apart at [OPEN_ZOOM] on a phone and a hundred at [MAX_ZOOM]; [MIN_ZOOM] shows the whole sky.
 */
private const val MIN_ZOOM = .4f
private const val OPEN_ZOOM = 1.5f
private const val MAX_ZOOM = 5f

/** How far apart the stars sit against the fitted sky: the shape is kept, the nodes stop crowding each other. */
private const val SPREAD = 3f

/** How far from a star a tap still picks it: no more than the closest pair's spacing at [OPEN_ZOOM]. */
private val TAP_RADIUS = 20.dp

/** The least pixels per sky unit, whatever room the view leaves. */
private const val MIN_FIT = .5f

/** Where a node lands on screen: the start at the bottom middle above the sheet, y up, fitted to the width. */
private class Placement(
    private val b: SkyBounds,
    private val width: Float,
    private val height: Float,
    private val floor: Float,
    margin: Float,
    private val scale: Float,
    private val pan: Offset,
) {
    /** Kept above [MIN_FIT]: a sky shorter than the sheet and margins would turn it negative and flip the pan limits. */
    private val fit = (min((width - margin * 2) / b.spanX, (height - floor - margin * 3) / b.spanY) * SPREAD).coerceAtLeast(MIN_FIT)
    val unit: Float get() = fit * scale
    operator fun invoke(node: AtlasNode) = Offset(
        width / 2 + ((node.x - b.midX) * fit * scale).toFloat() + pan.x,
        height - floor - (node.y * fit * scale).toFloat() + pan.y,
    )

    /**
     * The pan held so the sky never leaves the view: any star can be brought to the middle of what the sheet leaves open
     * — the farthest left, right, the start, the top — and no further.
     */
    fun held(): Offset {
        val halfX = abs(b.spanX / 2 * unit)
        val middle = (height - floor) / 2 - (height - floor)
        val top = middle + b.spanY * unit
        return Offset(pan.x.coerceIn(-halfX, halfX), pan.y.coerceIn(minOf(middle, top), maxOf(middle, top)))
    }
}

private class SkyBounds(val midX: Double, val spanX: Float, val spanY: Float) {
    companion object {
        fun of(nodes: List<AtlasNode>): SkyBounds {
            val minX = nodes.minOfOrNull { it.x } ?: 0.0
            val maxX = nodes.maxOfOrNull { it.x } ?: 1.0
            return SkyBounds((minX + maxX) / 2, max(1.0, maxX - minX).toFloat(), max(1.0, nodes.maxOfOrNull { it.y } ?: 1.0).toFloat())
        }
    }
}

/** The far stars: fixed places, each breathing on its own phase. */
private fun DrawScope.stars(clock: Float) {
    repeat(90) { i ->
        val a = .25f + .25f * sin(clock * 1.3f + i)
        drawCircle(Color(0xFFC8DCFF).copy(alpha = a), 1.dp.toPx() * .7f, Offset((i * 97 % 1000) / 1000f * size.width, (i * 577 % 1000) / 1000f * size.height))
    }
}

/** A star's radius on screen by its kind: the same at every zoom, so a closer look parts the stars instead of swelling them. */
private fun DrawScope.starRadius(node: AtlasNode): Float = when (node.kind) {
    AtlasNodeKind.KEYSTONE -> 13.dp
    AtlasNodeKind.NOTABLE -> 9.dp
    AtlasNodeKind.START -> 11.dp
    AtlasNodeKind.SMALL -> 5.dp
}.toPx()

/**
 * The still part of one node (3.56.0): a halo once taken or chosen, a burning white core once taken, a dim core while
 * no point could take it, a ring on a notable, a dashed halo on the chosen one. The twinkle and the motes are [starLive]'s.
 */
private fun DrawScope.starStill(node: AtlasNode, at: Offset, taken: Boolean, open: Boolean, selected: Boolean, brushes: SkyBrushes) {
    val r = starRadius(node)
    val hue = node.branch.hue()
    if (taken || selected) translate(at.x, at.y) { drawCircle(brushes.halo(hue, r * 3), r * 3, Offset.Zero) }
    if (taken || !open) drawCircle(if (taken) Color.White else Sky.faint.copy(alpha = .45f), r * .55f, at)
    val rim = if (taken) hue else Sky.faint.copy(alpha = .5f)
    if (node.kind != AtlasNodeKind.SMALL) drawCircle(rim, r, at, style = Stroke(1.5.dp.toPx()))
    if (selected) drawCircle(Color.White, r + 6.dp.toPx(), at, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
}

/** The breathing part of one node: a twinkling core while a point could take it, six orbiting motes on a keystone. */
private fun DrawScope.starLive(node: AtlasNode, at: Offset, taken: Boolean, open: Boolean, clock: Float) {
    val r = starRadius(node)
    if (open && !taken) drawCircle(Color.White.copy(alpha = .45f + .35f * sin(clock * 4f + node.code.hashCode() % 7)), r * .55f, at)
    if (node.kind == AtlasNodeKind.KEYSTONE) {
        val rim = if (taken) node.branch.hue() else Sky.faint.copy(alpha = .5f)
        repeat(6) { k ->
            val a = k * PI / 3 + clock * .3f
            drawCircle(rim, 1.6.dp.toPx(), Offset(at.x + (cos(a) * r * 1.5f).toFloat(), at.y + (sin(a) * r * 1.5f).toFloat()))
        }
    }
}

/** The chosen star: what it is, what it gives, and the one command the server would accept for it. */
@Composable private fun NodeSheet(
    index: ContentIndex,
    node: AtlasNode,
    taken: Set<String>,
    available: Int,
    price: Long,
    enabled: Boolean,
    onTake: () -> Unit,
    onRefund: () -> Unit,
    modifier: Modifier,
) {
    val graph = index.atlasGraph
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.navigationBarsPadding().padding(10.dp).fillMaxWidth().background(Color(0xE6080C16), shape).border(1.dp, Sky.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val kind = ui("atlas.kind.${node.kind.name}")
        Text(
            if (node.branch == AtlasBranch.ROOT) kind else ui("atlas.kind_branch", kind, ui("atlas.branch.${node.branch.name}")).uppercase(),
            color = Sky.text.copy(alpha = .75f),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(atlasNodeTitle(node.code), color = Color.White, style = MaterialTheme.typography.titleMedium)
        val isTaken = node.code in taken
        // «Было → станет» (3.54.0): each line beside the atlas's whole of its modifier now and with this node taken.
        val totals = remember(taken) { AtlasTotals.of(index, taken) }
        node.lines.forEach { line ->
            effectLines(index, line).forEach { Text("•  $it", color = ModBlue, style = MaterialTheme.typography.bodySmall) }
            if (node.kind != AtlasNodeKind.START) {
                val now = totals[line.code]?.firstOrNull() ?: 0.0
                val value = line.values.firstOrNull() ?: 0.0
                val after = if (isTaken) now - value else now + value
                Text(
                    ui(if (isTaken) "atlas.compare_refund" else "atlas.compare_take", number(now), number(after)),
                    color = Sky.text.copy(alpha = .7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
        val canTake = AtlasFog.canTake(graph, node.code, taken)
        val canRefund = AtlasFog.canRefund(graph, node.code, taken)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MutedText(
                when {
                    node.kind == AtlasNodeKind.START -> ui("atlas.start_hint")
                    isTaken && canRefund -> ui("atlas.refund_hint", price)
                    isTaken -> ui("atlas.holds_hint")
                    canTake && available <= 0 -> ui("atlas.no_points")
                    canTake -> ui("atlas.take_hint")
                    else -> ui("atlas.far_hint")
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            when {
                canTake -> ForgeButton(
                    onClick = onTake,
                    enabled = enabled && available > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Sky.take, contentColor = Color.White),
                ) { Text(ui("atlas.take")) }

                canRefund -> ForgeOutlinedButton(onClick = onRefund, enabled = enabled) { Text(ui("atlas.refund"), color = Sky.text) }
            }
        }
    }
}

/**
 * «+4% Количество предметов на картах», one per stat of a node's line; a count — chests, fountains, Vaal
 * modifiers — carries no percent. A line whose modifier the content does not hold reads as its sentence.
 */
private fun effectLines(index: ContentIndex, line: Line): List<String> {
    val def = index.modifier(line.code) ?: return listOf(lineText(index, line))
    return def.effects.mapIndexed { i, effect ->
        val value = line.values.getOrElse(i) { 0.0 }
        val sign = if (value >= 0) "+" else "−"
        val unit = if (effect.stat in AtlasEffects.flat) "" else "%"
        // The dictionary's «Атлас: …» prefix is the screen's own title here (2.73.0).
        "$sign${number(abs(value))}$unit ${statTitle(effect.stat).substringAfter(": ")}"
    }
}

/** How far down the view the atlas's start is framed: its branches rise above it. */
private const val START_DOWN = .8f

/** A constellation's glow (3.53.0): its middle, how far it spreads in atlas units, its mechanic's hue and its nodes. */
private class Glow(val center: AtlasNode, val radius: Float, val hue: Color, val codes: Set<String>) {
    companion object {
        fun of(nodes: List<AtlasNode>): List<Glow> = nodes.groupBy { AtlasFog.constellation(it.code) }.mapNotNull { (key, members) ->
            key ?: return@mapNotNull null
            val x = members.sumOf { it.x } / members.size
            val y = members.sumOf { it.y } / members.size
            val spread = members.maxOf { kotlin.math.hypot(it.x - x, it.y - y) }.toFloat() + 2f
            Glow(members.first().copy(x = x, y = y), spread, members.first().branch.hue(), members.mapTo(HashSet()) { it.code })
        }
    }
}

/** Screen pixels per atlas unit at the placement's fit and zoom. */
private fun placeScale(place: Placement): Float = place.unit

/** The atlas's lines added up (3.54.0): by modifier code, value by value, over every taken node. */
private object AtlasTotals {
    fun of(index: ContentIndex, taken: Set<String>): Map<String, List<Double>> = sum(index.atlas.nodes.filter { it.code in taken }.flatMap { it.lines })

    fun sum(lines: List<Line>): Map<String, List<Double>> = lines.groupBy { it.code }.mapValues { (_, same) ->
        List(same.maxOf { it.values.size }) { i -> same.sumOf { it.values.getOrElse(i) { 0.0 } } }
    }
}

/** «Итого» (3.54.0): what the taken nodes give, added up and grouped by mechanic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtlasSummary(index: ContentIndex, taken: Set<String>, onDismiss: () -> Unit) {
    val groups = remember(taken) {
        index.atlas.nodes.filter { it.code in taken }.groupBy { it.branch }
            .mapValues { (_, nodes) -> AtlasTotals.sum(nodes.flatMap { it.lines }) }.filterValues { it.isNotEmpty() }
    }
    ForgeSheet(onDismissRequest = onDismiss, containerColor = Color(0xF20A0E18)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(ui("atlas.summary_title"), color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (groups.isEmpty()) Text(ui("atlas.summary_empty"), color = Sky.text, style = MaterialTheme.typography.bodySmall)
            groups.forEach { (branch, totals) ->
                Text(ui("atlas.branch.${branch.name}").uppercase(), color = branch.hue(), style = MaterialTheme.typography.labelMedium)
                totals.forEach { (code, values) -> effectLines(index, Line(code, values)).forEach { Text("•  $it", color = ModBlue, style = MaterialTheme.typography.bodySmall) } }
            }
        }
    }
}
