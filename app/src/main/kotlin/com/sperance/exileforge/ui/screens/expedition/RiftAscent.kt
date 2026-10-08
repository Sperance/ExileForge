package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftNode
import com.sperance.exileforge.rules.rift.RiftRun
import com.sperance.exileforge.rules.rift.RiftStage
import com.sperance.exileforge.ui.components.motionClock
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Карта Разлома «Восхождение» (4.0.1, утверждена владельцем по макету): все акты одной лентой снизу вверх, страж ведёт дальше
 * без разрывов. Узлы - шестигранники-руны, путь - жила с текущей энергией, доступные узлы дышат и пускают кольцо. Дальше
 * обзора ([RiftEngine.seen]) клубится туман, в нём видны только силуэты стражей. Лента сама держит героя в нижней трети.
 * Декор стоит при выключенном `LocalMotion`.
 */
@Composable internal fun RiftAscent(
    rules: RiftRules,
    engine: RiftEngine,
    run: RiftRun?,
    idle: Boolean,
    modifier: Modifier = Modifier,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    onNode: (RiftNode) -> Unit,
) {
    val plan = engine.plan
    val depths = rules.acts * (rules.layout.rows + 1)
    val here = run?.at?.let(plan::node)
    val reachable = remember(run) { if (run != null && run.stage == RiftStage.MOVE) engine.next(run).toSet() else emptySet() }
    val walked = run?.path.orEmpty().toSet()
    val seen = remember(run, plan) { plan.nodes.filter { engine.seen(run, it) }.map { it.id }.toSet() }
    val sight = engine.sight(run)
    val reach = here?.let(engine::depth) ?: -1
    val flow = motionClock(FLOW_MS, "rift-flow")
    val breath = motionClock(BREATH_MS, "rift-breath")
    val drift = motionClock(DRIFT_MS, "rift-drift")
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = maxWidth
        val viewport = maxHeight
        val height = top + ROW * depths + bottom
        fun y(node: RiftNode): Dp = top + ROW * (depths - 1 - engine.depth(node)) + ROW / 2
        fun x(node: RiftNode): Dp = SIDE + (width - SIDE * 2) * ((node.lane + .5f) / node.width)
        val density = LocalDensity.current
        // Герой - в нижней трети: лента едет к нему с каждым шагом
        LaunchedEffect(run?.at, viewport) {
            snapshotFlow { scroll.maxValue }.first { it > 0 }
            val anchor = here?.let(::y) ?: (top + ROW * (depths - 1))
            scroll.animateScrollTo(with(density) { (anchor - viewport * .66f).toPx() }.toInt().coerceIn(0, scroll.maxValue))
        }
        Box(Modifier.fillMaxWidth().verticalScroll(scroll)) {
            Box(Modifier.width(width).height(height)) {
                Canvas(Modifier.fillMaxSize()) {
                    val px = { d: Dp -> d.toPx() }
                    drawRect(Brush.verticalGradient(listOf(RiftColors.Ground, RiftColors.Ground2, RiftColors.Ground)))
                    here?.let { drawCircle(Brush.radialGradient(listOf(RiftColors.Rift.copy(alpha = .16f), Color.Transparent), Offset(px(x(it)), px(y(it))), px(ROW * 3)), px(ROW * 3), Offset(px(x(it)), px(y(it)))) }
                    plan.nodes.forEach { node ->
                        node.links.mapNotNull(plan::node).forEach { next ->
                            if (node.id !in seen || next.id !in seen) return@forEach
                            vein(
                                Offset(px(x(node)), px(y(node))),
                                Offset(px(x(next)), px(y(next))),
                                bend = if (node.lane % 2 == 0) -8.dp.toPx() else 8.dp.toPx(),
                                walked = node.id in walked && next.id in walked,
                                open = node == here && next.id in reachable,
                                flow = flow,
                            )
                        }
                    }
                    // Туман: всё выше последнего видимого ряда
                    val fogEdge = px(top + ROW * (depths - 1 - (reach + sight)))
                    if (fogEdge > 0f) fog(fogEdge, drift)
                }
                plan.nodes.forEach { node ->
                    val visible = node.id in seen
                    val guardian = node.kind == RiftNodeKind.GUARDIAN
                    if (!visible && !guardian) return@forEach
                    val open = node.id in reachable && idle
                    NodeRune(
                        node,
                        open = open,
                        walked = node.id in walked,
                        hero = node == here,
                        silhouette = !visible,
                        breath = breath,
                        name = if (guardian && !visible) rules.guardians.monsters.getOrNull(node.act)?.let { loc("monster.${it.value}.name") } else null,
                        modifier = Modifier.offset(x(node) - NODE_BOX / 2, y(node) - NODE_BOX / 2),
                    ) { onNode(node) }
                }
            }
        }
    }
}

/** Жила между узлами: пройденная горит и течёт, доступная течёт мягче, прочие - тусклая нить. */
private fun DrawScope.vein(from: Offset, to: Offset, bend: Float, walked: Boolean, open: Boolean, flow: Float) {
    val mid = Offset((from.x + to.x) / 2 + bend, (from.y + to.y) / 2)
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo(mid.x, mid.y, to.x, to.y)
    }
    val color = when {
        walked -> RiftColors.Rift
        open -> RiftColors.Soft
        else -> RiftColors.LineHi.copy(alpha = .6f)
    }
    if (walked) drawPath(path, RiftColors.Rift.copy(alpha = .25f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
    val stroke = when {
        walked -> 3.5.dp
        open -> 2.dp
        else -> 1.4.dp
    }
    drawPath(path, color, style = Stroke(stroke.toPx(), cap = StrokeCap.Round))
    if (walked || open) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 8.dp.toPx()), -flow * 24.dp.toPx())
        drawPath(path, if (walked) RiftColors.Hot else RiftColors.Rift, style = Stroke(1.6.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round))
    }
}

/** Туман над краем [edge]: почти чёрная толща, в ней дрейфуют зелёные клубы, нижняя кромка тает. */
private fun DrawScope.fog(edge: Float, drift: Float) {
    drawRect(RiftColors.Ground.copy(alpha = .97f), size = size.copy(height = edge))
    val turn = drift * 2 * PI
    repeat(MIST) { k ->
        val phase = turn + k * 1.7
        val cx = size.width * (.15f + .7f * ((k * 37 % 100) / 100f)) + (sin(phase) * 30.dp.toPx()).toFloat()
        val cy = edge - (k * 61 % 100) / 100f * edge.coerceAtMost(900.dp.toPx()) + (cos(phase * .7) * 12.dp.toPx()).toFloat()
        val r = (90 + k * 23 % 70).dp.toPx()
        drawCircle(Brush.radialGradient(listOf(RiftColors.Deep.copy(alpha = .55f), RiftColors.Rift.copy(alpha = .05f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
    }
    val fade = 56.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, RiftColors.Ground.copy(alpha = .9f), Color.Transparent), edge - fade, edge + fade / 2), Offset(0f, edge - fade), size.copy(height = fade * 1.5f))
}

/** Узел-руна: шестигранник цвета вида, знак вида внутри; доступный дышит и пускает кольцо, над героем - огонёк. */
@Composable private fun NodeRune(
    node: RiftNode,
    open: Boolean,
    walked: Boolean,
    hero: Boolean,
    silhouette: Boolean,
    breath: Float,
    name: String?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val guardian = node.kind == RiftNodeKind.GUARDIAN
    val radius = if (guardian) 24.dp else 16.dp
    val tint = if (silhouette) RiftColors.Warn else node.kind.color()
    val pulse = (1 - cos(breath * 2 * PI).toFloat()) / 2
    Box(modifier.size(NODE_BOX).alpha(if (silhouette) .45f + .2f * pulse else 1f).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = radius.toPx()
            if (open) drawCircle(RiftColors.Rift.copy(alpha = .9f * (1 - breath)), r * (.8f + 1.1f * breath), style = Stroke(2.dp.toPx()))
            val hex = hexagon(center, r)
            if (!silhouette) {
                drawPath(hex, if (walked) RiftColors.Deep else RiftColors.Panel)
                drawPath(hex, if (open) RiftColors.Rift else tint.copy(alpha = if (walked && !hero) .55f else .9f), style = Stroke((if (open) 2.2 else 1.3).dp.toPx()))
            }
            if (hero) drawCircle(RiftColors.Hot, 4.5.dp.toPx(), Offset(center.x, center.y - r - 8.dp.toPx()), alpha = .6f + .4f * pulse)
        }
        Icon(
            node.kind.glyph(),
            null,
            tint = tint,
            modifier = Modifier.size(radius * 1.15f).graphicsLayer {
                val grow = if (open) 1f + .08f * pulse else 1f
                scaleX = grow
                scaleY = grow
            },
        )
        name?.let {
            Text(
                it.uppercase(),
                color = RiftColors.Dim,
                style = relicName(9),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 14.dp).width(NODE_BOX * 2.4f),
            )
        }
    }
}

/** Шестигранник радиуса [r] вокруг [c], вершиной вбок. */
private fun hexagon(c: Offset, r: Float): Path = Path().apply {
    repeat(6) { k ->
        val a = PI / 3 * k + PI / 6
        val p = Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat())
        if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** Знак вида узла. */
internal fun RiftNodeKind.glyph(): ImageVector = when (this) {
    RiftNodeKind.FIGHT -> ForgeGlyphs.Swords
    RiftNodeKind.ELITE -> ForgeGlyphs.Skull
    RiftNodeKind.CHAMPION -> ForgeGlyphs.Banner
    RiftNodeKind.ALTAR -> ForgeGlyphs.Chain
    RiftNodeKind.REST -> ForgeGlyphs.Flask
    RiftNodeKind.TREASURE -> ForgeGlyphs.Coins
    RiftNodeKind.FORGE -> ForgeGlyphs.Anvil
    RiftNodeKind.GUARDIAN -> ForgeGlyphs.Helm
}

private val ROW = 74.dp
private val SIDE = 34.dp
private val NODE_BOX = 56.dp
private const val MIST = 9
private const val FLOW_MS = 1200
private const val BREATH_MS = 2400
private const val DRIFT_MS = 16000
