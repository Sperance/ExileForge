package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.atlas.AtlasFog
import com.sperance.exileforge.core.display.atlasNodeTitle
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
import com.sperance.exileforge.ui.components.*
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
        if (index == null || state == null) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Sky.text)
        else {
            val taken = state.allocated.toSet()
            Sky(index, taken, atlas.selected, Modifier.fillMaxSize(), onSelect = vm::selectAtlasNode)
            index.atlasGraph.node(atlas.selected)?.let { node ->
                NodeSheet(index, node, taken, state.available, index.atlas.respec.price(s.heroLevel, 1), enabled = !s.busy,
                    onTake = { vm.allocateAtlas(node.code) }, onRefund = { refunding = node.code },
                    modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = vm::closeAtlas) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.close"), tint = Sky.text) }
            Column(Modifier.weight(1f)) {
                Text(ui("atlas.title"), color = Color.White, style = MaterialTheme.typography.titleMedium)
                state?.let { Text(ui("atlas.points", it.available, it.points), color = Sky.text, style = MaterialTheme.typography.labelMedium) }
            }
            FirstVisit(Guide.ATLAS)
            GuideButton(Guide.ATLAS)
            ForgeOutlinedButton(onClick = { summary = true }, enabled = (state?.allocated?.size ?: 0) > 1) { Text(ui("atlas.summary"), color = Sky.text) }
            // The start is nobody's to give back: what was spent is every taken node but it.
            val spent = (state?.allocated?.size ?: 1) - 1
            ForgeOutlinedButton(onClick = { resetting = true }, enabled = spent > 0 && !s.busy) { Text(ui("atlas.reset"), color = Sky.text) }
        }
        if (resetting && index != null && state != null) {
            val nodes = state.allocated.size - 1
            val cost = index.atlas.respec.price(s.heroLevel, nodes)
            val money = s.hero?.money ?: 0L
            ConfirmSheet(title = ui("atlas.reset_q"), confirm = ui("atlas.reset"), danger = true, onDismiss = { resetting = false },
                ledger = listOf(LedgerLine(ui("atlas.reset_cost"), ui("atlas.gold", cost), Tone.SPEND), LedgerLine(ui("atlas.reset_back"), nodes.toString(), Tone.GAIN)),
                blocked = money < cost, warning = if (money < cost) ui("atlas.no_gold") else null) { resetting = false; vm.resetAtlas() }
        }
        refunding?.let { code -> if (index != null) {
            val cost = index.atlas.respec.price(s.heroLevel, 1)
            val money = s.hero?.money ?: 0L
            ConfirmSheet(title = ui("atlas.refund_q"), subtitle = atlasNodeTitle(code), confirm = ui("atlas.refund"), danger = true, onDismiss = { refunding = null },
                ledger = listOf(LedgerLine(ui("atlas.reset_cost"), ui("atlas.gold", cost), Tone.SPEND), LedgerLine(ui("atlas.reset_back"), "1", Tone.GAIN)),
                blocked = money < cost, warning = if (money < cost) ui("atlas.no_gold") else null) { refunding = null; vm.refundAtlas(code) }
        } }
        if (summary && index != null && state != null) AtlasSummary(index, state.allocated.toSet()) { summary = false }
        ToastHost(s, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp))
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
    // The sky opens close (3.24.0): at the nearest zoom, the start in the middle of what the sheet leaves open.
    var scale by remember { mutableFloatStateOf(MAX_ZOOM) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var framed by remember { mutableStateOf(false) }
    // The clock (3.56.0) ticks [CLOCK_FPS] times a second, not on every frame: a twinkle needs no 120 Hz, and only the
    // layers that breathe are drawn on its tick — the glows, threads and rings are drawn again on a pan, a zoom or a take.
    val clock by produceState(0f) {
        var start = 0L
        var last = 0L
        while (true) withFrameNanos { now ->
            if (start == 0L) start = now
            if (now - last >= CLOCK_STEP_NS) { last = now; value = (now - start) / 1e9f }
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
    Box(modifier.clipToBounds()
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
            detectTransformGestures { _, drag, zoom, _ ->
                scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                val limit = size.width.toFloat() * scale
                pan = Offset((pan.x + drag.x).coerceIn(-limit, limit), (pan.y + drag.y).coerceIn(-limit, limit * 2))
            }
        }
        .pointerInput(shown) {
            detectTapGestures { tap ->
                val place = Placement(bounds, size.width.toFloat(), size.height.toFloat(), floor, margin, scale, pan)
                val hit = shown.minByOrNull { (place(it) - tap).getDistanceSquared() } ?: return@detectTapGestures
                if ((place(hit) - tap).getDistance() <= 28.dp.toPx()) onSelect(hit.code)
            }
        }) {
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
            val zoom = scale.coerceIn(.8f, 1.6f)
            shown.forEach { starStill(it, place(it), it.code in taken, it.code in reachable, it.code == selected, zoom, brushes) }
        }
        // The breathing layer: the twinkle of what a point could take and the keystones' orbiting motes.
        Canvas(Modifier.matchParentSize()) {
            val place = Placement(bounds, size.width, size.height, floor, margin, scale, pan)
            val zoom = scale.coerceIn(.8f, 1.6f)
            shown.forEach { starLive(it, place(it), it.code in taken, it.code in reachable, clock, zoom) }
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

/** How far the sky zooms out and in. */
private const val MIN_ZOOM = .5f
private const val MAX_ZOOM = 3f

/** Where a node lands on screen: the start at the bottom middle above the sheet, y up, fitted to the width. */
private class Placement(private val b: SkyBounds, private val width: Float, private val height: Float, private val floor: Float,
                        margin: Float, private val scale: Float, private val pan: Offset) {
    private val fit = min((width - margin * 2) / b.spanX, (height - floor - margin * 3) / b.spanY)
    val unit: Float get() = fit * scale
    operator fun invoke(node: AtlasNode) = Offset(width / 2 + ((node.x - b.midX) * fit * scale).toFloat() + pan.x,
        height - floor - (node.y * fit * scale).toFloat() + pan.y)
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

/** A star's radius on screen by its kind at [zoom]. */
private fun DrawScope.starRadius(node: AtlasNode, zoom: Float): Float =
    when (node.kind) { AtlasNodeKind.KEYSTONE -> 13.dp; AtlasNodeKind.NOTABLE -> 9.dp; AtlasNodeKind.START -> 11.dp; AtlasNodeKind.SMALL -> 5.dp }.toPx() * zoom

/**
 * The still part of one node (3.56.0): a halo once taken or chosen, a burning white core once taken, a dim core while
 * no point could take it, a ring on a notable, a dashed halo on the chosen one. The twinkle and the motes are [starLive]'s.
 */
private fun DrawScope.starStill(node: AtlasNode, at: Offset, taken: Boolean, open: Boolean, selected: Boolean, zoom: Float, brushes: SkyBrushes) {
    val r = starRadius(node, zoom)
    val hue = node.branch.hue()
    if (taken || selected) translate(at.x, at.y) { drawCircle(brushes.halo(hue, r * 3), r * 3, Offset.Zero) }
    if (taken || !open) drawCircle(if (taken) Color.White else Sky.faint.copy(alpha = .45f), r * .55f, at)
    val rim = if (taken) hue else Sky.faint.copy(alpha = .5f)
    if (node.kind != AtlasNodeKind.SMALL) drawCircle(rim, r, at, style = Stroke(1.5.dp.toPx()))
    if (selected) drawCircle(Color.White, r + 6.dp.toPx(), at, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
}

/** The breathing part of one node: a twinkling core while a point could take it, six orbiting motes on a keystone. */
private fun DrawScope.starLive(node: AtlasNode, at: Offset, taken: Boolean, open: Boolean, clock: Float, zoom: Float) {
    val r = starRadius(node, zoom)
    if (open && !taken) drawCircle(Color.White.copy(alpha = .45f + .35f * sin(clock * 4f + node.code.hashCode() % 7)), r * .55f, at)
    if (node.kind == AtlasNodeKind.KEYSTONE) {
        val rim = if (taken) node.branch.hue() else Sky.faint.copy(alpha = .5f)
        repeat(6) { k ->
            val a = k * PI / 3 + clock * .3f
            drawCircle(rim, 1.6.dp.toPx() * zoom, Offset(at.x + (cos(a) * r * 1.5f).toFloat(), at.y + (sin(a) * r * 1.5f).toFloat()))
        }
    }
}

/** The chosen star: what it is, what it gives, and the one command the server would accept for it. */
@Composable private fun NodeSheet(index: ContentIndex, node: AtlasNode, taken: Set<String>, available: Int, price: Long, enabled: Boolean,
                                  onTake: () -> Unit, onRefund: () -> Unit, modifier: Modifier) {
    val graph = index.atlasGraph
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.navigationBarsPadding().padding(10.dp).fillMaxWidth().background(Color(0xE6080C16), shape).border(1.dp, Sky.line, shape)
        .padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val kind = ui("atlas.kind.${node.kind.name}")
        Text(if (node.branch == AtlasBranch.ROOT) kind else ui("atlas.kind_branch", kind, ui("atlas.branch.${node.branch.name}")).uppercase(),
            color = Sky.text.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
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
                Text(ui(if (isTaken) "atlas.compare_refund" else "atlas.compare_take", number(now), number(after)), color = Sky.text.copy(alpha = .7f),
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 14.dp))
            }
        }
        val canTake = AtlasFog.canTake(graph, node.code, taken)
        val canRefund = AtlasFog.canRefund(graph, node.code, taken)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MutedText(when {
                node.kind == AtlasNodeKind.START -> ui("atlas.start_hint")
                isTaken && canRefund -> ui("atlas.refund_hint", price)
                isTaken -> ui("atlas.holds_hint")
                canTake && available <= 0 -> ui("atlas.no_points")
                canTake -> ui("atlas.take_hint")
                else -> ui("atlas.far_hint")
            }, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            when {
                canTake -> ForgeButton(onClick = onTake, enabled = enabled && available > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Sky.take, contentColor = Color.White)) { Text(ui("atlas.take")) }
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
@Composable private fun AtlasSummary(index: ContentIndex, taken: Set<String>, onDismiss: () -> Unit) {
    val groups = remember(taken) {
        index.atlas.nodes.filter { it.code in taken }.groupBy { it.branch }
            .mapValues { (_, nodes) -> AtlasTotals.sum(nodes.flatMap { it.lines }) }.filterValues { it.isNotEmpty() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xF20A0E18), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ui("atlas.summary_title"), color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (groups.isEmpty()) Text(ui("atlas.summary_empty"), color = Sky.text, style = MaterialTheme.typography.bodySmall)
            groups.forEach { (branch, totals) ->
                Text(ui("atlas.branch.${branch.name}").uppercase(), color = branch.hue(), style = MaterialTheme.typography.labelMedium)
                totals.forEach { (code, values) -> effectLines(index, Line(code, values)).forEach { Text("•  $it", color = ModBlue, style = MaterialTheme.typography.bodySmall) } }
            }
        }
    }
}
