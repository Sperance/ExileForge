package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.atlas.AtlasFog
import com.sperance.exileforge.core.display.atlasBranchTitle
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.AtlasNode
import com.sperance.exileforge.rules.content.AtlasNodeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Небо атласа: звёзды узлов, нити, масштаб и расстановка. */
/**
 * The sky itself: faint stars behind, the threads, then the nodes; dragged, pinched and tapped.
 *
 * Небо открывается близко, старт - посреди открытого листом места. Часы декора - состоянием, читаются только в дышащих слоях: мерцание
 * стоит без анимаций и вне экрана; нити, кольца дорог и подписи ветвей рисуются заново лишь со сдвигом, масштабом или взятием. Тумана нет:
 * всё небо видно и нажимается, недоступное лишь тусклее. Нити считаются раз, дышащее - раз на взятие. Касание читает масштаб и сдвиг на миг
 * касания, жест не перезапускается каждый кадр. Атлас - колесо вокруг старта: пунктирные кольца - кольцевые дороги раскладки, у каждой
 * ветви - подпись снаружи её звёзд; щипок масштабирует вокруг пальцев. Каждый новый [centerOn] (найденный поиском узел) ставит выбранную
 * звезду [selected] посреди открытого листом места.
 */
@Composable internal fun Sky(index: ContentIndex, taken: Set<String>, selected: String, modifier: Modifier, centerOn: Int = 0, onSelect: (String) -> Unit) {
    val graph = index.atlasGraph
    val nodes = index.atlas.nodes
    val bounds = remember(nodes) { SkyBounds.of(nodes) }
    val density = LocalDensity.current
    val floor = with(density) { 240.dp.toPx() }
    val margin = with(density) { 28.dp.toPx() }
    var scale by remember { mutableFloatStateOf(OPEN_ZOOM) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var framed by remember { mutableStateOf(false) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(centerOn, area) {
        if (centerOn == 0 || area == IntSize.Zero) return@LaunchedEffect
        val node = graph.node(selected) ?: return@LaunchedEffect
        val width = area.width.toFloat()
        val height = area.height.toFloat()
        val at = Placement(bounds, width, height, floor, margin, scale, Offset.Zero)(node)
        pan = Placement(bounds, width, height, floor, margin, scale, Offset(width / 2 - at.x, (height - floor) / 2 - at.y)).held()
    }
    val clock = com.sperance.exileforge.ui.components.decorTime()
    val reachable = remember(taken, graph) { nodes.filter { AtlasFog.canTake(graph, it.code, taken) }.map { it.code }.toSet() }
    val shown = nodes
    val rings = remember(index) { index.atlas.layout.rings.map { it.radius } }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val labels = remember(nodes, labelStyle) {
        BranchLabel.of(nodes).map { label ->
            label to measurer.measure(atlasBranchTitle(label.branch), labelStyle.copy(color = label.tint(), fontStyle = if (label.mini) FontStyle.Italic else FontStyle.Normal))
        }
    }
    val strands = remember(nodes, graph) { nodes.flatMap { node -> node.parents.mapNotNull { parent -> graph.node(parent)?.let { Strand(it, node) } } } }
    val brushes = remember(nodes) { SkyBrushes() }
    val live = remember(nodes, taken, reachable) { LiveStar.of(nodes, taken, reachable) }
    Box(
        modifier.clipToBounds()
            .onSizeChanged { size ->
                area = size
                if (framed || size.width == 0) return@onSizeChanged
                val start = nodes.firstOrNull { it.kind == AtlasNodeKind.START } ?: return@onSizeChanged
                val width = size.width.toFloat()
                val height = size.height.toFloat()
                val at = Placement(bounds, width, height, floor, margin, scale, Offset.Zero)(start)
                pan = Offset(width / 2 - at.x, (height - floor) / 2 - at.y)
                framed = true
            }
            .pointerInput(nodes) {
                detectTransformGestures { centroid, drag, zoom, _ ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val next = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    val anchor = Offset(width / 2, (height - floor) / 2)
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
        Canvas(Modifier.matchParentSize()) { stars(clock.value) }
        Canvas(Modifier.matchParentSize()) {
            val place = Placement(bounds, size.width, size.height, floor, margin, scale, pan)
            val origin = place(0.0, 0.0)
            val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
            rings.forEach { drawCircle(Sky.faint.copy(alpha = .16f), (it * place.unit).toFloat(), origin, style = Stroke(1.dp.toPx(), pathEffect = dash)) }
            strands.forEach { strand ->
                val lit = strand.node.code in taken && strand.parent.code in taken
                drawLine(if (lit) strand.node.branch.hue() else Sky.faint.copy(alpha = .22f), place(strand.parent), place(strand.node), (if (lit) 2.dp else 1.dp).toPx())
            }
            shown.forEach { starStill(it, place(it), it.code in taken, it.code in reachable, it.code == selected, brushes) }
            labels.forEach { (label, text) ->
                val at = place(label.x, label.y)
                drawText(text, topLeft = Offset(at.x - text.size.width / 2f, at.y - text.size.height / 2f))
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val place = Placement(bounds, size.width, size.height, floor, margin, scale, pan)
            val now = clock.value
            val reach = 2 * margin
            live.forEach { star ->
                val at = place(star.node)
                if (at.x > -reach && at.y > -reach && at.x < size.width + reach && at.y < size.height + reach) starLive(star, at, now)
            }
        }
    }
}

/** One thread of the sky: a node and the parent it hangs from. */
internal class Strand(val parent: AtlasNode, val node: AtlasNode)

/** The gradients of the sky, made once per hue and radius, not a shader for every halo on every frame. */
internal class SkyBrushes {
    private val halos = HashMap<Pair<Color, Float>, Brush>()

    /** A taken or chosen star's halo of [hue] reaching [r] pixels, centred at the origin. */
    fun halo(hue: Color, r: Float): Brush = halos.getOrPut(hue to r) { Brush.radialGradient(listOf(hue.copy(alpha = .55f), Color.Transparent), Offset.Zero, r) }
}

/**
 * How far the sky zooms out and in, and where it opens. With [SPREAD] the closest pair of the server's atlas (8 units of a wheel some 400 across)
 * is some 30 dp apart at [OPEN_ZOOM] on a phone and a hundred at [MAX_ZOOM]; [MIN_ZOOM] shows the whole sky.
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

/** Where a node lands on screen: the middle of the sky in the middle of the room above the sheet, y up, fitted to the width. */
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
    operator fun invoke(node: AtlasNode) = invoke(node.x, node.y)

    /** Where the sky point ([x], [y]) lands on screen. */
    operator fun invoke(x: Double, y: Double) = Offset(
        width / 2 + ((x - b.midX) * fit * scale).toFloat() + pan.x,
        (height - floor) / 2 - ((y - b.midY) * fit * scale).toFloat() + pan.y,
    )

    /**
     * The pan held so the sky never leaves the view: any star can be brought to the middle of what the sheet leaves open
     * — the farthest left, right, top and bottom — and no further.
     */
    fun held(): Offset {
        val halfX = abs(b.spanX / 2 * unit)
        val halfY = abs(b.spanY / 2 * unit)
        return Offset(pan.x.coerceIn(-halfX, halfX), pan.y.coerceIn(-halfY, halfY))
    }
}

/** The box of the sky: its middle and spans in atlas units. */
internal class SkyBounds(val midX: Double, val midY: Double, val spanX: Float, val spanY: Float) {
    companion object {
        fun of(nodes: List<AtlasNode>): SkyBounds {
            val minX = nodes.minOfOrNull { it.x } ?: 0.0
            val maxX = nodes.maxOfOrNull { it.x } ?: 1.0
            val minY = nodes.minOfOrNull { it.y } ?: 0.0
            val maxY = nodes.maxOfOrNull { it.y } ?: 1.0
            return SkyBounds((minX + maxX) / 2, (minY + maxY) / 2, max(1.0, maxX - minX).toFloat(), max(1.0, maxY - minY).toFloat())
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
 * The still part of one node: a halo once taken or chosen, a burning white core once taken, a dim core while
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
internal fun DrawScope.starLive(star: LiveStar, at: Offset, clock: Float) {
    val r = starRadius(star.node)
    if (star.twinkles) drawCircle(Color.White.copy(alpha = .45f + .35f * sin(clock * 4f + star.seed)), r * .55f, at)
    val rim = star.rim ?: return
    val mote = 1.6.dp.toPx()
    repeat(6) { k ->
        val a = k * PI / 3 + clock * .3f
        drawCircle(rim, mote, Offset(at.x + (cos(a) * r * 1.5f).toFloat(), at.y + (sin(a) * r * 1.5f).toFloat()))
    }
}

/** Звезда, что дышит: мерцает ли (открыта, не взята), обод мотыльков ключевой (null - не ключевая) и сдвиг фазы. */
internal class LiveStar(val node: AtlasNode, val twinkles: Boolean, val rim: Color?, val seed: Int) {
    companion object {
        fun of(nodes: List<AtlasNode>, taken: Set<String>, reachable: Set<String>): List<LiveStar> = nodes.mapNotNull { node ->
            val took = node.code in taken
            val twinkles = node.code in reachable && !took
            val rim = if (node.kind != AtlasNodeKind.KEYSTONE) {
                null
            } else if (took) {
                node.branch.hue()
            } else {
                Sky.faint.copy(alpha = .5f)
            }
            if (twinkles || rim != null) LiveStar(node, twinkles, rim, node.code.hashCode() % 7) else null
        }
    }
}

/**
 * A branch's name on the sky: at the middle of its stars pushed out from the start ([PUSH], a map-object branch [MINI_PUSH]) and kept
 * inside the wheel's rim; a map-object branch (no keystones) is written in italics.
 */
internal class BranchLabel(val branch: AtlasBranch, val x: Double, val y: Double, val mini: Boolean) {
    fun tint(): Color = if (mini) branch.hue().copy(alpha = .75f) else Sky.text.copy(alpha = .75f)

    companion object {
        private const val PUSH = 1.2
        private const val MINI_PUSH = 1.1
        private const val RIM = .94

        fun of(nodes: List<AtlasNode>): List<BranchLabel> {
            val rim = nodes.maxOfOrNull { hypot(it.x, it.y) } ?: 1.0
            return nodes.groupBy { it.branch }.filterKeys { it != AtlasBranch.ROOT }.map { (branch, members) ->
                val x = members.sumOf { it.x } / members.size
                val y = members.sumOf { it.y } / members.size
                val mini = members.none { it.kind == AtlasNodeKind.KEYSTONE }
                val r = max(hypot(x, y), 1.0)
                val out = min(r * (if (mini) MINI_PUSH else PUSH), rim * RIM) / r
                BranchLabel(branch, x * out, y * out, mini)
            }
        }
    }
}
