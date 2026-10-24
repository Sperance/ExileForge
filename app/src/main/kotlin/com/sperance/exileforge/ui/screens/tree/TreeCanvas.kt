package com.sperance.exileforge.ui.screens.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.motionPhase
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Холст древа: колесо, медальоны узлов, масштаб и сдвиг карты. */
/**
 * The graph, drawn from the coordinates the content seeded.
 *
 * Panning and zooming are the only interaction beyond a tap: the layout is fixed data, so the
 * canvas never moves a node, it only chooses where to look. It clips, because a canvas does not:
 * without that the outer nodes are painted over whatever the map happens to be sitting on. And the
 * pan is held to the tree's own half-extent, so a stray flick cannot drag the whole graph away and
 * leave an empty rectangle with no way back but the double-tap reset.
 *
 * Первый взгляд - на старт класса. Масштаб - щипком вокруг точки. Доступные узлы дышат в рисовании, без анимаций светятся
 * ровно. Детекторы - по графу и читают вид как он есть: ключ по масштабу и сдвигу перезапускал бы касание на каждом кадре и съедал бы
 * второй палец щипка. Двойное касание на любом масштабе возвращает к целому дереву. Ближайший узел выигрывает только в своём круге не
 * меньше пальца. Неподвижный слой рисуется заново лишь со сдвигом, масштабом или выбором; дышащий - только узлы на шаг от взятого и только
 * на экране.
 *
 * Гнездо с самоцветом [jewels] (код гнезда - цвет редкости самоцвета) - центр и ореол этого цвета, у невзятого - приглушённый; взятое
 * без самоцвета - светлое кольцо с пунктирной лункой и подписью «Пустое гнездо». Просьба [TreeView.jump] ведёт камеру к узлу и на миг
 * обводит его золотом.
 */
@Composable internal fun TreeCanvas(
    nodes: List<TreeNode>,
    selected: String,
    taken: Set<String>,
    reachable: Set<String>,
    highlight: Set<String>,
    view: TreeView,
    modifier: Modifier = Modifier,
    focus: String? = null,
    jewels: Map<String, Color> = emptyMap(),
    onSelect: (String) -> Unit,
) {
    val byCode = remember(nodes) { nodes.associateBy { it.code } }
    val bounds = remember(nodes) { Bounds.of(nodes) }
    val select by rememberUpdatedState(onSelect)
    var area by remember { mutableStateOf(Size.Zero) }
    val flash = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()
    val emptyWord = ui("tree.empty_socket")
    val emptyLabel = remember(measurer, emptyWord) { measurer.measure(emptyWord, TextStyle(color = ManaBlue, fontSize = EMPTY_LABEL_SIZE)) }
    val empty = remember(nodes, taken, jewels) { nodes.filter { it.type == SkillNodeType.JEWEL_SOCKET && it.code in taken && it.code !in jewels } }
    LaunchedEffect(area, focus) {
        val node = focus?.let(byCode::get) ?: return@LaunchedEffect
        if (view.focused || area.width <= 0f) return@LaunchedEffect
        view.focused = true
        view.scale = FOCUS_ZOOM
        view.pan = centred(node, bounds, area, FOCUS_ZOOM)
    }
    LaunchedEffect(view.jump, area) {
        val node = view.jump?.code?.let(byCode::get) ?: return@LaunchedEffect
        if (area.width <= 0f) return@LaunchedEffect
        val fromScale = view.scale
        val fromPan = view.pan
        val scale = max(fromScale, FOCUS_ZOOM)
        val toPan = centred(node, bounds, area, scale)
        animate(0f, 1f, animationSpec = tween(JUMP_MS)) { t, _ ->
            view.scale = fromScale + (scale - fromScale) * t
            view.pan = fromPan + (toPan - fromPan) * t
        }
        flash.snapTo(1f)
        flash.animateTo(0f, tween(FLASH_MS))
    }

    fun zoomAt(focus: Offset, factor: Float, drag: Offset, width: Float, height: Float) {
        val next = (view.scale * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val centre = Offset(width / 2, height / 2)
        val moved = focus - centre - (focus - centre - view.pan) * (next / view.scale) + drag
        val limit = panLimit(bounds, width, height, next)
        view.scale = next
        view.pan = Offset(moved.x.coerceIn(-limit.x, limit.x), moved.y.coerceIn(-limit.y, limit.y))
    }
    val phase = motionPhase(PULSE_MS * 2, still = .5f)
    val breathing: State<Float> = remember(phase) { derivedStateOf { PULSE_LOW + (1f - PULSE_LOW) * (1f - kotlin.math.abs(phase.value * 2 - 1f)) } }
    val pulse by breathing
    fun breathes(node: TreeNode) = node.code in reachable && node.code !in taken
    Box(modifier.fillMaxWidth()) {
        Canvas(
            Modifier.fillMaxSize().clipToBounds().onSizeChanged { area = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(nodes) {
                    detectTransformGestures { centroid, drag, zoom, _ ->
                        zoomAt(centroid, zoom, drag, size.width.toFloat(), size.height.toFloat())
                    }
                }
                .pointerInput(nodes) {
                    detectTapGestures(
                        onDoubleTap = { view.reset() },
                        onTap = { tap ->
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
            val thickness = view.scale.coerceIn(.6f, 1.6f)
            val masteryDash = PathEffect.dashPathEffect(floatArrayOf(MASTERY_DASH * thickness, MASTERY_GAP * thickness))
            nodes.forEach { node ->
                val from = place(node, bounds, width, height, view.scale, view.pan)
                node.connections.forEach { code ->
                    val other = byCode[code] ?: return@forEach
                    val to = place(other, bounds, width, height, view.scale, view.pan)
                    if (!crosses(from, to, width, height)) return@forEach
                    val both = node.code in taken && code in taken
                    if (node.type == SkillNodeType.MASTERY || other.type == SkillNodeType.MASTERY) {
                        drawLine(MASTERY_EDGE.copy(alpha = if (both) .95f else .6f), from, to, MASTERY_WIDTH * thickness, pathEffect = masteryDash)
                        return@forEach
                    }
                    val open = (node.code in taken && code in reachable) || (code in taken && node.code in reachable)
                    drawLine(
                        when {
                            both -> NEON_LINE
                            open -> NEON_LINE.copy(alpha = .6f)
                            else -> FAR_LINE
                        },
                        from,
                        to,
                        (if (both) 3f else 2f) * thickness,
                    )
                }
            }
            nodes.forEach { node ->
                if (breathes(node)) return@forEach
                val at = place(node, bounds, width, height, view.scale, view.pan)
                if (!onScreen(node, at, view.scale, width, height)) return@forEach
                medallion(node, at, view.scale, node.code in taken, false, node.code == selected, 0f, jewels[node.code])
            }
            empty.forEach { node ->
                val at = place(node, bounds, width, height, view.scale, view.pan)
                if (onScreen(node, at, view.scale, width, height)) emptyCaption(emptyLabel, at, radius(node) * view.scale.coerceIn(.5f, 2.2f))
            }
            nodes.forEach { node ->
                if (node.code !in highlight || breathes(node)) return@forEach
                drawCircle(Vital, radius(node) * view.scale.coerceIn(.5f, 2.2f) + 5f, place(node, bounds, width, height, view.scale, view.pan), style = Stroke(2.5f))
            }
        }
        val breathers = remember(nodes, taken, reachable) { nodes.filter(::breathes) }
        Canvas(Modifier.fillMaxSize().clipToBounds()) {
            val width = size.width
            val height = size.height
            if (breathers.isNotEmpty()) {
                val now = pulse
                breathers.forEach { node ->
                    val at = place(node, bounds, width, height, view.scale, view.pan)
                    if (!onScreen(node, at, view.scale, width, height)) return@forEach
                    medallion(node, at, view.scale, false, true, node.code == selected, now, jewels[node.code])
                    if (node.code in highlight) drawCircle(Vital, radius(node) * view.scale.coerceIn(.5f, 2.2f) + 5f, at, style = Stroke(2.5f))
                }
            }
            val lit = flash.value
            val target = view.jump?.code?.let(byCode::get)
            if (lit > 0f && target != null) {
                val at = place(target, bounds, width, height, view.scale, view.pan)
                drawCircle(Boon.copy(alpha = lit), radius(target) * view.scale.coerceIn(.5f, 2.2f) + FLASH_GAP.toPx(), at, style = Stroke(FLASH_WIDTH.toPx()))
            }
        }
    }
}

/** Узел с ореолом ([GLOW_REACH] радиусов) и кольцами хоть краем на холсте. */
private fun onScreen(node: TreeNode, at: Offset, scale: Float, width: Float, height: Float): Boolean {
    val reach = radius(node) * scale.coerceIn(.5f, 2.2f) * GLOW_REACH + 8f
    return at.x > -reach && at.y > -reach && at.x < width + reach && at.y < height + reach
}

/** Сдвиг карты, при котором [node] стоит в середине холста [area] на масштабе [scale], - в пределах сдвига ([panLimit]). */
private fun centred(node: TreeNode, bounds: Bounds, area: Size, scale: Float): Offset {
    val at = place(node, bounds, area.width, area.height, scale, Offset.Zero)
    val limit = panLimit(bounds, area.width, area.height, scale)
    return Offset((area.width / 2 - at.x).coerceIn(-limit.x, limit.x), (area.height / 2 - at.y).coerceIn(-limit.y, limit.y))
}

/** Подпись [label] под гнездом радиуса [r] в точке [at] - на тёмной плашке, чтобы читалась поверх нитей. */
private fun DrawScope.emptyCaption(label: TextLayoutResult, at: Offset, r: Float) {
    val pad = CAPTION_PAD.toPx()
    val topLeft = Offset(at.x - label.size.width / 2f, at.y + r + CAPTION_GAP.toPx())
    drawRoundRect(NEON_STONE.copy(alpha = .85f), topLeft - Offset(pad, pad / 2), Size(label.size.width + pad * 2, label.size.height + pad), CornerRadius(pad))
    drawText(label, topLeft = topLeft)
}

/** Нить от [from] до [to] хоть краем на холсте: её рамка пересекает его. */
private fun crosses(from: Offset, to: Offset, width: Float, height: Float): Boolean = max(from.x, to.x) >= 0f && min(from.x, to.x) <= width && max(from.y, to.y) >= 0f && min(from.y, to.y) <= height

/** Ореол узла - столько его радиусов. */
private const val GLOW_REACH = 2.6f

/** Гнездо само ничего не даёт, поэтому на карте стоит за то, что может держать. */
internal fun nodeColour(node: TreeNode, selected: Boolean): Color = when {
    node.type == SkillNodeType.KEYSTONE -> LifeRed
    node.type == SkillNodeType.NOTABLE -> if (selected) GoldBright else Gold
    node.type == SkillNodeType.START -> ShieldCyan
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
 * The wheel under the graph (the owner's pick «Колесо PoE»): a stone disc with its rings,
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
 * an attribute's triangle, a socket's hollow ring and a start's octagon. In «Неон»
 * every node shines in its kind's colour so the tree reads at a glance: one out of reach is a dark stone with a
 * bright rim and a faint halo, one a step away is ringed thicker and breathes ([pulse]), a taken one is filled with its
 * colour under a light rim and a strong halo. A node out of reach is grey and unlit: only what can be taken
 * and what is taken keep their colour.
 */
internal fun DrawScope.medallion(node: TreeNode, centre: Offset, scale: Float, taken: Boolean, next: Boolean, selected: Boolean, pulse: Float, jewel: Color? = null) {
    val r = radius(node) * scale.coerceIn(.5f, 2.2f)
    val tint = when {
        node.type == SkillNodeType.START -> classTint(node.code)
        jewel != null && taken -> jewel
        else -> nodeColour(node, false)
    }
    val glow = when {
        taken -> TAKEN_GLOW
        next -> NEXT_GLOW * pulse
        else -> 0f
    }
    if (glow > 0f) drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = glow), Color.Transparent), centre, r * GLOW_REACH), r * GLOW_REACH, centre)
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
            drawCircle(NEON_STONE, r, centre)
            drawCircle(edge, r, centre, style = Stroke(width * 1.2f))
            when {
                jewel != null -> drawCircle(if (taken) jewel else jewel.copy(alpha = UNTAKEN_JEWEL), r * .6f, centre)
                taken -> drawCircle(ManaBlue, r * .55f, centre, style = Stroke(EMPTY_RING.dp.toPx(), pathEffect = EMPTY_DASH))
                else -> drawCircle(NEON_STONE, r * .6f, centre)
            }
        }

        SkillNodeType.SMALL -> {
            drawCircle(fill, r, centre)
            drawCircle(edge, r, centre, style = Stroke(width))
        }
    }
    if (selected) drawCircle(GoldBright, r + 5.dp.toPx(), centre, style = Stroke(2.dp.toPx()))
}

/** «Неон»: the wheel a shade lighter, so a dark stone with a bright rim stands off it. */
internal val WHEEL_BG = Color(0xFF141A21)
internal val NEON_STONE = Color(0xFF10151B)
internal val NEON_RIM = Color(0xFFEEF8F1)
internal val NEON_LINE = Color(0xFF57E39A)

/** Связь с мастерством - не линия пути (мастерство путь к соседям не открывает): тонкий пунктир цвета мастерства. */
private val MASTERY_EDGE = Elder
private const val MASTERY_WIDTH = 1.6f
private const val MASTERY_DASH = 4f
private const val MASTERY_GAP = 5f
internal val FAR_LINE = Color(0xFF3C4D5A)
internal val FAR_RIM = Color(0xFF6E7C86)
internal const val NEXT_GLOW = .45f
internal const val TAKEN_GLOW = .7f
internal const val PULSE_LOW = .4f
internal const val PULSE_MS = 800

/** Самоцвет в невзятом гнезде - приглушённый цвет его редкости. */
private const val UNTAKEN_JEWEL = .35f

/** Пустое взятое гнездо: пунктирная лунка толщиной [EMPTY_RING] dp и подпись под ним. */
private const val EMPTY_RING = 1.6f
private val EMPTY_DASH = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
private val EMPTY_LABEL_SIZE = 10.sp
private val CAPTION_GAP = 4.dp
private val CAPTION_PAD = 4.dp

/** Переход камеры к гнезду и золотая обводка после него: длительности и вид кольца. */
private const val JUMP_MS = 450
private const val FLASH_MS = 900
private val FLASH_GAP = 7.dp
private val FLASH_WIDTH = 2.5.dp
