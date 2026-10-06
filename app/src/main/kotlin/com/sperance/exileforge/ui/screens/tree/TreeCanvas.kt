package com.sperance.exileforge.ui.screens.tree

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.nodeTypeTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.presentation.tree.TreeViewModel
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TreeAllocation
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.StatContribution
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Холст древа (3.80.16): колесо, медальоны узлов, масштаб и сдвиг карты. */
/**
 * The graph, drawn from the coordinates the content seeded.
 *
 * Panning and zooming are the only interaction beyond a tap: the layout is fixed data, so the
 * canvas never moves a node, it only chooses where to look. It clips, because a canvas does not:
 * without that the outer nodes are painted over whatever the map happens to be sitting on. And the
 * pan is held to the tree's own half-extent, so a stray flick cannot drag the whole graph away and
 * leave an empty rectangle with no way back but the reset.
 */
@Composable internal fun TreeCanvas(
    nodes: List<TreeNode>,
    selected: String,
    taken: Set<String>,
    reachable: Set<String>,
    path: List<String>,
    highlight: Set<String>,
    view: TreeView,
    modifier: Modifier = Modifier,
    focus: String? = null,
    onSelect: (String) -> Unit,
) {
    val byCode = remember(nodes) { nodes.associateBy { it.code } }
    val bounds = remember(nodes) { Bounds.of(nodes) }
    val select by rememberUpdatedState(onSelect)
    var area by remember { mutableStateOf(Size.Zero) }
    // The first look (3.81.0) is on the class's own start, near enough to read, not on the whole tree's centre.
    LaunchedEffect(area, focus) {
        val node = focus?.let(byCode::get) ?: return@LaunchedEffect
        if (view.focused || area.width <= 0f) return@LaunchedEffect
        view.focused = true
        val at = place(node, bounds, area.width, area.height, FOCUS_ZOOM, Offset.Zero)
        val limit = panLimit(bounds, area.width, area.height, FOCUS_ZOOM)
        view.scale = FOCUS_ZOOM
        view.pan = Offset((area.width / 2 - at.x).coerceIn(-limit.x, limit.x), (area.height / 2 - at.y).coerceIn(-limit.y, limit.y))
    }

    // The zoom is about a point — the pinch's centre, the double tap — so what is under the fingers stays there.
    fun zoomAt(focus: Offset, factor: Float, drag: Offset, width: Float, height: Float) {
        val next = (view.scale * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val centre = Offset(width / 2, height / 2)
        val moved = focus - centre - (focus - centre - view.pan) * (next / view.scale) + drag
        val limit = panLimit(bounds, width, height, next)
        view.scale = next
        view.pan = Offset(moved.x.coerceIn(-limit.x, limit.x), moved.y.coerceIn(-limit.y, limit.y))
    }
    // The reachable nodes breathe (3.76.0, «Неон»): their glow swells and fades, drawn again on the draw pass alone.
    // With the animations off in the settings (3.77.0) they glow steadily.
    val breathing: State<Float> = if (LocalMotion.current) {
        rememberInfiniteTransition(label = "tree").animateFloat(
            PULSE_LOW,
            1f,
            infiniteRepeatable(tween(PULSE_MS, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    val pulse by breathing
    Box(modifier.fillMaxWidth()) {
        // Both detectors are keyed on the graph alone and read the view as it is now. Keyed on the zoom and
        // the pan, the tap detector restarted on every frame of a drag and took the second finger's touch
        // for a fresh tap, consuming it — and a consumed touch cancels the pinch before it begins.
        Canvas(
            Modifier.fillMaxSize().clipToBounds().onSizeChanged { area = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(nodes) {
                    detectTransformGestures { centroid, drag, zoom, _ ->
                        zoomAt(centroid, zoom, drag, size.width.toFloat(), size.height.toFloat())
                    }
                }
                .pointerInput(nodes) {
                    detectTapGestures(
                        // A double tap steps in towards the spot, and from the closest zoom back to the whole tree.
                        onDoubleTap = { at ->
                            if (view.scale >= MAX_ZOOM - .01f) {
                                view.reset()
                            } else {
                                zoomAt(at, DOUBLE_TAP_ZOOM, Offset.Zero, size.width.toFloat(), size.height.toFloat())
                            }
                        },
                        onTap = { tap ->
                            // The nearest node wins, but only within its own circle — never smaller than a finger: a tap on bare canvas changes nothing.
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            nodes.minByOrNull { (place(it, bounds, width, height, view.scale, view.pan) - tap).getDistanceSquared() }?.let { hit ->
                                val reach = max(radius(hit) * view.scale.coerceIn(.5f, 2.2f) * 2f, MIN_TOUCH.toPx())
                                if ((place(hit, bounds, width, height, view.scale, view.pan) - tap).getDistance() <= reach) select(hit.code)
                            }
                        },
                    )
                },
        ) {
            val width = size.width
            val height = size.height
            wheel(nodes, bounds, width, height, view.scale, view.pan)
            // Edges first, so a node always sits on top of the lines that reach it.
            nodes.forEach { node ->
                val from = place(node, bounds, width, height, view.scale, view.pan)
                node.connections.forEach { code ->
                    val other = byCode[code] ?: return@forEach
                    val both = node.code in taken && code in taken
                    val open = (node.code in taken && code in reachable) || (code in taken && node.code in reachable)
                    drawLine(
                        when {
                            both -> NEON_LINE
                            open -> NEON_LINE.copy(alpha = .6f)
                            else -> FAR_LINE
                        },
                        from,
                        place(other, bounds, width, height, view.scale, view.pan),
                        (if (both) 3f else 2f) * view.scale.coerceIn(.6f, 1.6f),
                    )
                }
            }
            if (path.isNotEmpty()) {
                // The dashed way to the chosen node: from the taken node it leaves, through every step it would take.
                val anchor = byCode[path.first()]?.connections.orEmpty().plus(nodes.filter { path.first() in it.connections }.map { it.code })
                    .firstOrNull { it in taken && byCode[it]?.type != SkillNodeType.MASTERY }
                val points = (listOfNotNull(anchor) + path).mapNotNull { byCode[it] }.map { place(it, bounds, width, height, view.scale, view.pan) }
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 7f).map { it * view.scale.coerceIn(.6f, 1.6f) }.toFloatArray())
                points.zipWithNext { a, b -> drawLine(GoldBright, a, b, 3f * view.scale.coerceIn(.6f, 1.6f), pathEffect = dash) }
            }
            nodes.forEach { node ->
                medallion(
                    node,
                    place(node, bounds, width, height, view.scale, view.pan),
                    view.scale,
                    node.code in taken,
                    node.code in reachable || node.code in path,
                    node.code == selected,
                    pulse,
                )
            }
            // Узлы фильтра носят зелёное кольцо поверх медальона, не вместо него.
            nodes.forEach { node ->
                if (node.code !in highlight) return@forEach
                drawCircle(Vital, radius(node) * view.scale.coerceIn(.5f, 2.2f) + 5f, place(node, bounds, width, height, view.scale, view.pan), style = Stroke(2.5f))
            }
        }
    }
}

internal fun nodeColour(node: TreeNode, selected: Boolean): Color = when {
    node.type == SkillNodeType.KEYSTONE -> LifeRed

    node.type == SkillNodeType.NOTABLE -> if (selected) GoldBright else Gold

    node.type == SkillNodeType.START -> ShieldCyan

    // A socket gives nothing itself, so on the map it has to stand for what it can hold rather
    // than blend into the small nodes around it.
    node.type == SkillNodeType.JEWEL_SOCKET -> ManaBlue

    node.type == SkillNodeType.MASTERY -> Elder

    node.type == SkillNodeType.ATTRIBUTE -> Vital

    else -> Rune
}

internal fun radius(node: TreeNode): Float = when (node.type) {
    SkillNodeType.KEYSTONE -> 13f
    SkillNodeType.NOTABLE -> 10f
    SkillNodeType.START -> 12f
    SkillNodeType.JEWEL_SOCKET -> 11f
    SkillNodeType.SMALL -> 6f
    SkillNodeType.MASTERY -> 10f
    SkillNodeType.ATTRIBUTE -> 7f
}

/** Extent of the seeded coordinates, so the whole tree fits whatever canvas it is given. */
internal data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    val spanX get() = max(1f, maxX - minX)
    val spanY get() = max(1f, maxY - minY)
    companion object {
        fun of(nodes: List<TreeNode>) = Bounds(
            nodes.minOfOrNull { it.x.toFloat() } ?: 0f,
            nodes.maxOfOrNull { it.x.toFloat() } ?: 1f,
            nodes.minOfOrNull { it.y.toFloat() } ?: 0f,
            nodes.maxOfOrNull { it.y.toFloat() } ?: 1f,
        )
    }
}

internal const val MARGIN = 28f
internal const val MIN_ZOOM = .5f
internal const val MAX_ZOOM = 3f

/** The zoom the tree opens at, centred on the class's start. */
internal const val FOCUS_ZOOM = 1.8f

/** How much closer one double tap brings the map. */
internal const val DOUBLE_TAP_ZOOM = 2f

/** The smallest circle a tap finds a node in, however far the map is zoomed out. */
internal val MIN_TOUCH = 18.dp

/** How far apart the nodes sit against the fitted tree: the shape and the node sizes are kept, the nodes stop crowding each other. */
internal const val SPREAD = 1.3f

/** How many pixels of the seeded graph one pixel of canvas is worth, before the zoom. */
internal fun fitFactor(bounds: Bounds, width: Float, height: Float): Float = min((width - MARGIN * 2) / bounds.spanX, (height - MARGIN * 2) / bounds.spanY) * SPREAD

/**
 * How far the map may be dragged: the tree's own half-extent on screen.
 *
 * At the limit the far edge of the tree has reached the middle of the canvas, so there is always
 * something drawn to drag back by. Anything looser and a flick leaves an empty rectangle.
 */
internal fun panLimit(bounds: Bounds, width: Float, height: Float, scale: Float): Offset {
    val fit = fitFactor(bounds, width, height)
    return Offset(max(0f, bounds.spanX * fit * scale / 2), max(0f, bounds.spanY * fit * scale / 2))
}

internal fun place(node: TreeNode, bounds: Bounds, width: Float, height: Float, scale: Float, pan: Offset): Offset {
    val fit = fitFactor(bounds, width, height)
    val x = (node.x - bounds.minX) * fit - bounds.spanX * fit / 2
    val y = (node.y - bounds.minY) * fit - bounds.spanY * fit / 2
    return Offset(width / 2 + x * scale + pan.x, height / 2 + y * scale + pan.y)
}

/** Each class's colour on the wheel, by the attributes its start node is named after. */
internal fun classTint(code: String): Color = when (code.removeSuffix("_START")) {
    "STR" -> Color(0xFFC0504D)
    "DEX" -> Vital
    "INT" -> Rune
    "STR_DEX" -> Color(0xFFD08A4E)
    "STR_INT" -> Elder
    "DEX_INT" -> ShieldCyan
    else -> Gold
}

/**
 * The wheel under the graph (2.59.0, the owner's pick «Колесо PoE»): a stone disc with its rings,
 * each class's sector washed in its colour. The sectors are read off the start
 * nodes' positions, so the wheel turns with whatever tree the content seeds.
 */
internal fun DrawScope.wheel(nodes: List<TreeNode>, bounds: Bounds, width: Float, height: Float, scale: Float, pan: Offset) {
    drawRect(WHEEL_BG)
    val fit = fitFactor(bounds, width, height) * scale
    val centre = Offset(width / 2 + (-bounds.minX - bounds.spanX / 2) * fit + pan.x, height / 2 + (-bounds.minY - bounds.spanY / 2) * fit + pan.y)
    val rim = max(bounds.spanX, bounds.spanY) / 2 * fit
    drawCircle(Brush.radialGradient(listOf(Color(0xFF1E252E), Color(0xFF141A21)), centre, rim * 1.05f), rim * 1.05f, centre)
    for (ring in 1..8) drawCircle(FAR_LINE.copy(alpha = if (ring % 3 == 0) .35f else .15f), rim * ring / 8f, centre, style = Stroke(1f))
    nodes.filter { it.type == SkillNodeType.START && it.code != "SCION_START" }.forEach { start ->
        val angle = Math.toDegrees(atan2(start.y.toDouble(), start.x.toDouble())).toFloat()
        val tint = classTint(start.code)
        drawArc(tint.copy(alpha = .06f), angle - 30f, 60f, true, centre - Offset(rim, rim), Size(rim * 2, rim * 2))
    }
}

/**
 * One node as a medallion of its kind: a small disc, a notable's double ring, a keystone's hexagon, a mastery's diamond,
 * an attribute's triangle, a socket's hollow ring and a start's octagon. Since 3.76.0 («Неон», the owner's pick of three
 * mockups) every node shines in its kind's colour so the tree reads at a glance: one out of reach is a dark stone with a
 * bright rim and a faint halo, one a step away is ringed thicker and breathes ([pulse]), a taken one is filled with its
 * colour under a light rim and a strong halo. Since 3.77.0 a node out of reach is grey and unlit: only what can be taken
 * and what is taken keep their colour.
 */
internal fun DrawScope.medallion(node: TreeNode, centre: Offset, scale: Float, taken: Boolean, next: Boolean, selected: Boolean, pulse: Float) {
    val r = radius(node) * scale.coerceIn(.5f, 2.2f)
    val tint = if (node.type == SkillNodeType.START) classTint(node.code) else nodeColour(node, false)
    val glow = when {
        taken -> TAKEN_GLOW
        next -> NEXT_GLOW * pulse
        else -> 0f
    }
    if (glow > 0f) drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = glow), Color.Transparent), centre, r * 2.6f), r * 2.6f, centre)
    val fill = if (taken) tint else NEON_STONE
    val edge = when {
        taken -> NEON_RIM
        next -> tint
        else -> FAR_RIM
    }
    val width = (if (node.type == SkillNodeType.SMALL) 1.6f else 2.2f) * (if (next && !taken) 1.35f else 1f) * scale.coerceIn(.6f, 1.6f)
    fun polygon(sides: Int, radius: Float, turn: Float) = Path().apply {
        repeat(sides) { i ->
            val a = turn + i * 2 * PI.toFloat() / sides
            val p = centre + Offset(cos(a), sin(a)) * radius
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    fun shape(path: Path) {
        drawPath(path, fill)
        drawPath(path, edge, style = Stroke(width))
    }
    when (node.type) {
        SkillNodeType.START -> shape(polygon(8, r, PI.toFloat() / 8))

        SkillNodeType.KEYSTONE -> {
            shape(polygon(6, r, 0f))
            drawPath(polygon(6, r * .55f, PI.toFloat() / 6), edge, style = Stroke(width))
        }

        SkillNodeType.NOTABLE -> {
            drawCircle(fill, r, centre)
            drawCircle(edge, r, centre, style = Stroke(width))
            drawCircle(edge, r * .55f, centre, style = Stroke(width))
        }

        SkillNodeType.MASTERY -> shape(polygon(4, r, 0f))

        SkillNodeType.ATTRIBUTE -> shape(polygon(3, r, -PI.toFloat() / 2))

        SkillNodeType.JEWEL_SOCKET -> {
            drawCircle(edge, r, centre, style = Stroke(width * 1.2f))
            drawCircle(if (taken) tint else NEON_STONE, r * .6f, centre)
        }

        SkillNodeType.SMALL -> {
            drawCircle(fill, r, centre)
            drawCircle(edge, r, centre, style = Stroke(width))
        }
    }
    if (selected) drawCircle(GoldBright, r + 5.dp.toPx(), centre, style = Stroke(2.dp.toPx()))
}

/** «Неон» (3.76.0): the wheel a shade lighter, so a dark stone with a bright rim stands off it. */
internal val WHEEL_BG = Color(0xFF141A21)
internal val NEON_STONE = Color(0xFF10151B)
internal val NEON_RIM = Color(0xFFEEF8F1)
internal val NEON_LINE = Color(0xFF57E39A)
internal val FAR_LINE = Color(0xFF3C4D5A)
internal val FAR_RIM = Color(0xFF6E7C86)
internal const val NEXT_GLOW = .45f
internal const val TAKEN_GLOW = .7f
internal const val PULSE_LOW = .4f
internal const val PULSE_MS = 800
