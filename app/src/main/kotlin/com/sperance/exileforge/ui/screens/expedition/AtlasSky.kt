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

/** Небо атласа (3.80.24): звёзды узлов, нити, масштаб и расстановка. */
/** The sky itself: faint stars behind, the threads, then the nodes; dragged, pinched and tapped. */
@Composable internal fun Sky(index: ContentIndex, taken: Set<String>, selected: String, modifier: Modifier, onSelect: (String) -> Unit) {
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
internal class Strand(val parent: AtlasNode, val node: AtlasNode)

/** The gradients of the sky, made once per hue and radius (3.56.0): a shader was built for every glow and halo on every frame. */
internal class SkyBrushes {
    private val glows = HashMap<Pair<Color, Float>, Brush>()
    private val halos = HashMap<Pair<Color, Float>, Brush>()

    /** A constellation's glow of [hue] spreading [r] pixels, centred at the origin: drawn under a translation. */
    fun glow(hue: Color, r: Float): Brush = glows.getOrPut(hue to r) { Brush.radialGradient(listOf(hue.copy(alpha = .16f), Color.Transparent), Offset.Zero, r) }

    /** A taken or chosen star's halo of [hue] reaching [r] pixels, centred at the origin. */
    fun halo(hue: Color, r: Float): Brush = halos.getOrPut(hue to r) { Brush.radialGradient(listOf(hue.copy(alpha = .55f), Color.Transparent), Offset.Zero, r) }
}

/** How often the sky's clock ticks. */
internal const val CLOCK_FPS = 20
internal const val CLOCK_STEP_NS = 1_000_000_000L / CLOCK_FPS

/**
 * How far the sky zooms out and in, and where it opens. With [SPREAD] the closest pair of the server's atlas (4.6 units,
 * server 1.65.0) is some 30 dp apart at [OPEN_ZOOM] on a phone and a hundred at [MAX_ZOOM]; [MIN_ZOOM] shows the whole sky.
 */
internal const val MIN_ZOOM = .4f
internal const val OPEN_ZOOM = 1.5f
internal const val MAX_ZOOM = 5f

/** How far apart the stars sit against the fitted sky: the shape is kept, the nodes stop crowding each other. */
internal const val SPREAD = 3f

/** How far from a star a tap still picks it: no more than the closest pair's spacing at [OPEN_ZOOM]. */
internal val TAP_RADIUS = 20.dp

/** The least pixels per sky unit, whatever room the view leaves. */
internal const val MIN_FIT = .5f

/** Where a node lands on screen: the start at the bottom middle above the sheet, y up, fitted to the width. */
internal class Placement(
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

internal class SkyBounds(val midX: Double, val spanX: Float, val spanY: Float) {
    companion object {
        fun of(nodes: List<AtlasNode>): SkyBounds {
            val minX = nodes.minOfOrNull { it.x } ?: 0.0
            val maxX = nodes.maxOfOrNull { it.x } ?: 1.0
            return SkyBounds((minX + maxX) / 2, max(1.0, maxX - minX).toFloat(), max(1.0, nodes.maxOfOrNull { it.y } ?: 1.0).toFloat())
        }
    }
}

/** The far stars: fixed places, each breathing on its own phase. */
internal fun DrawScope.stars(clock: Float) {
    repeat(90) { i ->
        val a = .25f + .25f * sin(clock * 1.3f + i)
        drawCircle(Color(0xFFC8DCFF).copy(alpha = a), 1.dp.toPx() * .7f, Offset((i * 97 % 1000) / 1000f * size.width, (i * 577 % 1000) / 1000f * size.height))
    }
}

/** A star's radius on screen by its kind: the same at every zoom, so a closer look parts the stars instead of swelling them. */
internal fun DrawScope.starRadius(node: AtlasNode): Float = when (node.kind) {
    AtlasNodeKind.KEYSTONE -> 13.dp
    AtlasNodeKind.NOTABLE -> 9.dp
    AtlasNodeKind.START -> 11.dp
    AtlasNodeKind.SMALL -> 5.dp
}.toPx()

/**
 * The still part of one node (3.56.0): a halo once taken or chosen, a burning white core once taken, a dim core while
 * no point could take it, a ring on a notable, a dashed halo on the chosen one. The twinkle and the motes are [starLive]'s.
 */
internal fun DrawScope.starStill(node: AtlasNode, at: Offset, taken: Boolean, open: Boolean, selected: Boolean, brushes: SkyBrushes) {
    val r = starRadius(node)
    val hue = node.branch.hue()
    if (taken || selected) translate(at.x, at.y) { drawCircle(brushes.halo(hue, r * 3), r * 3, Offset.Zero) }
    if (taken || !open) drawCircle(if (taken) Color.White else Sky.faint.copy(alpha = .45f), r * .55f, at)
    val rim = if (taken) hue else Sky.faint.copy(alpha = .5f)
    if (node.kind != AtlasNodeKind.SMALL) drawCircle(rim, r, at, style = Stroke(1.5.dp.toPx()))
    if (selected) drawCircle(Color.White, r + 6.dp.toPx(), at, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
}

/** The breathing part of one node: a twinkling core while a point could take it, six orbiting motes on a keystone. */
internal fun DrawScope.starLive(node: AtlasNode, at: Offset, taken: Boolean, open: Boolean, clock: Float) {
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

/** How far down the view the atlas's start is framed: its branches rise above it. */
internal const val START_DOWN = .8f

/** A constellation's glow (3.53.0): its middle, how far it spreads in atlas units, its mechanic's hue and its nodes. */
internal class Glow(val center: AtlasNode, val radius: Float, val hue: Color, val codes: Set<String>) {
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
internal fun placeScale(place: Placement): Float = place.unit
