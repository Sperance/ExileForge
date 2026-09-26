package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.atlas.AtlasGraph
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.atlas.AtlasBranch
import com.sperance.exileforge.core.model.atlas.AtlasEffect
import com.sperance.exileforge.core.model.atlas.AtlasNode
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
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
    AtlasBranch.VAAL -> Color(0xFFFF5A46)
    AtlasBranch.CONTENT -> Color(0xFF7FC8A0)
    AtlasBranch.BOSS -> Color(0xFFB48CFF)
    AtlasBranch.ROOT -> Color.White
}

/**
 * The atlas tree (2.68.0, server 0.60.0) as a night sky: every node a star, the taken ones burning
 * and joined by lit threads, the ones a point could take next twinkling. The graph and every rule
 * are the server's; [AtlasGraph] only foresees them so a refused command shows no button. The sky
 * is dragged and pinched; a tap picks the nearest star within reach, and its sheet takes or gives it.
 */
@Composable fun AtlasScreen(s: ForgeState, vm: ForgeViewModel) {
    val atlas = s.play.atlas ?: return
    BackHandler(onBack = vm::closeAtlas)
    val tree = atlas.tree
    val state = atlas.state
    val graph = remember(tree) { tree?.let(::AtlasGraph) }
    var resetting by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Sky.deep, Sky.night, Sky.dawn)))) {
        if (graph == null || state == null) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Sky.text)
        else {
            val taken = state.allocated.toSet()
            Sky(graph, taken, atlas.selected, Modifier.fillMaxSize(), onSelect = vm::selectAtlasNode)
            graph.byCode[atlas.selected]?.let { node ->
                val price = tree!!.respec.perNode(s.play.hero?.sheet?.level ?: 1)
                NodeSheet(node, graph, taken, state.available, price, enabled = !s.busy,
                    onTake = { vm.allocateAtlas(node.code) }, onRefund = { vm.refundAtlas(node.code) },
                    modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = vm::closeAtlas) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.close"), tint = Sky.text) }
            Column(Modifier.weight(1f)) {
                Text(ui("atlas.title"), color = Color.White, style = MaterialTheme.typography.titleMedium)
                state?.let { Text(ui("atlas.points", it.available, it.points), color = Sky.text, style = MaterialTheme.typography.labelMedium) }
            }
            val spent = (state?.allocated?.size ?: 1) - 1
            OutlinedButton(onClick = { resetting = true }, enabled = spent > 0 && !s.busy) { Text(ui("atlas.reset"), color = Sky.text) }
        }
        if (resetting && tree != null && state != null) {
            val cost = tree.respec.perNode(s.play.hero?.sheet?.level ?: 1) * (state.allocated.size - 1)
            val money = s.play.hero?.character?.money ?: 0L
            ConfirmSheet(title = ui("atlas.reset_q"), confirm = ui("atlas.reset"), danger = true, onDismiss = { resetting = false },
                ledger = listOf(LedgerLine(ui("atlas.reset_cost"), ui("atlas.gold", cost), Tone.SPEND), LedgerLine(ui("atlas.reset_back"), (state.allocated.size - 1).toString(), Tone.GAIN)),
                blocked = money < cost, warning = if (money < cost) ui("atlas.no_gold") else null) { resetting = false; vm.resetAtlas() }
        }
        RefusalLine(s.refusal, vm::dismissMessage, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp))
    }
}

/** The sky itself: faint stars behind, the threads, then the nodes; dragged, pinched and tapped. */
@Composable private fun Sky(graph: AtlasGraph, taken: Set<String>, selected: String, modifier: Modifier, onSelect: (String) -> Unit) {
    val nodes = graph.nodes
    val bounds = remember(nodes) { SkyBounds.of(nodes) }
    val density = LocalDensity.current
    val floor = with(density) { 240.dp.toPx() }
    val margin = with(density) { 28.dp.toPx() }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val clock by produceState(0f) { var start = 0L; while (true) withFrameNanos { if (start == 0L) start = it; value = (it - start) / 1e9f } }
    val reachable = remember(taken, graph) { nodes.filter { graph.canTake(it.code, taken) }.map { it.code }.toSet() }
    // The fog (2.79.1): three links past the taken nodes; what lies beyond is neither drawn nor tappable.
    val sight = remember(taken, graph) { graph.visible(taken) }
    val shown = remember(sight, nodes) { nodes.filter { it.code in sight } }
    Canvas(modifier.clipToBounds()
        .pointerInput(nodes) {
            detectTransformGestures { _, drag, zoom, _ ->
                scale = (scale * zoom).coerceIn(.5f, 3f)
                val limit = size.width.toFloat() * scale
                pan = Offset((pan.x + drag.x).coerceIn(-limit, limit), (pan.y + drag.y).coerceIn(-limit, limit * 2))
            }
        }
        .pointerInput(shown, scale, pan) {
            detectTapGestures { tap ->
                val place = Placement(bounds, size.width.toFloat(), size.height.toFloat(), floor, margin, scale, pan)
                val hit = shown.minByOrNull { (place(it) - tap).getDistanceSquared() } ?: return@detectTapGestures
                if ((place(hit) - tap).getDistance() <= 28.dp.toPx()) onSelect(hit.code)
            }
        }) {
        val place = Placement(bounds, size.width, size.height, floor, margin, scale, pan)
        stars(clock)
        nodes.forEach { node -> node.parents.forEach { parent ->
            val from = graph.byCode[parent] ?: return@forEach
            val near = parent in sight
            if (!near && node.code !in sight) return@forEach
            val lit = node.code in taken && parent in taken
            val a = place(from)
            val b = place(node)
            // A link into the fog is only its first stretch, fading out where the unseen begins.
            if (near != (node.code in sight)) {
                val (seen, hidden) = if (near) a to b else b to a
                val end = seen + (hidden - seen) * FOG_STUB
                drawLine(Brush.linearGradient(listOf(Sky.faint.copy(alpha = .3f), Color.Transparent), seen, end), seen, end, 1.dp.toPx())
            } else drawLine(if (lit) node.branch.hue() else Sky.faint.copy(alpha = .22f), a, b, (if (lit) 2.dp else 1.dp).toPx())
        } }
        val zoom = scale.coerceIn(.8f, 1.6f)
        shown.forEach { star(it, place(it), it.code in taken, it.code in reachable, it.code == selected, clock, zoom) }
    }
}

/** How much of a link into the fog is drawn before it fades. */
private const val FOG_STUB = .3f

/** Where a node lands on screen: the start at the bottom middle above the sheet, y up, fitted to the width. */
private class Placement(private val b: SkyBounds, private val width: Float, private val height: Float, private val floor: Float,
                        margin: Float, private val scale: Float, private val pan: Offset) {
    private val fit = min((width - margin * 2) / b.spanX, (height - floor - margin * 3) / b.spanY)
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

/**
 * One node: a burning white core once taken, a twinkle while a point could take it, dim otherwise.
 * A notable wears a ring, a keystone six orbiting motes; the selected one a dashed halo.
 */
private fun DrawScope.star(node: AtlasNode, at: Offset, taken: Boolean, open: Boolean, selected: Boolean, clock: Float, zoom: Float) {
    val r = when (node.kind) { AtlasNode.KEYSTONE -> 13.dp; AtlasNode.NOTABLE -> 9.dp; AtlasNode.START -> 11.dp; else -> 5.dp }.toPx() * zoom
    val hue = node.branch.hue()
    if (taken || selected) drawCircle(Brush.radialGradient(listOf(hue.copy(alpha = .55f), Color.Transparent), at, r * 3), r * 3, at)
    val core = when { taken -> Color.White; open -> Color.White.copy(alpha = .45f + .35f * sin(clock * 4f + node.code.hashCode() % 7)); else -> Sky.faint.copy(alpha = .45f) }
    drawCircle(core, r * .55f, at)
    val rim = if (taken) hue else Sky.faint.copy(alpha = .5f)
    if (node.kind != AtlasNode.SMALL) drawCircle(rim, r, at, style = Stroke(1.5.dp.toPx()))
    if (node.kind == AtlasNode.KEYSTONE) repeat(6) { k ->
        val a = k * PI / 3 + clock * .3f
        drawCircle(rim, 1.6.dp.toPx() * zoom, Offset(at.x + (cos(a) * r * 1.5f).toFloat(), at.y + (sin(a) * r * 1.5f).toFloat()))
    }
    if (selected) drawCircle(Color.White, r + 6.dp.toPx(), at, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
}

/** The chosen star: what it is, what it gives, and the one command the server would accept for it. */
@Composable private fun NodeSheet(node: AtlasNode, graph: AtlasGraph, taken: Set<String>, available: Int, price: Long, enabled: Boolean,
                                  onTake: () -> Unit, onRefund: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.navigationBarsPadding().padding(10.dp).fillMaxWidth().background(Color(0xE6080C16), shape).border(1.dp, Sky.line, shape)
        .padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val kind = ui("atlas.kind.${node.kind}")
        Text(if (node.branch == AtlasBranch.ROOT) kind else ui("atlas.kind_branch", kind, ui("atlas.branch.${node.branch.name}")).uppercase(),
            color = Sky.text.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
        Text(loc("atlas.node.${node.code}.name"), color = Color.White, style = MaterialTheme.typography.titleMedium)
        node.effects.forEach { Text("•  " + effectLine(it), color = ModBlue, style = MaterialTheme.typography.bodySmall) }
        val isTaken = node.code in taken
        val canTake = graph.canTake(node.code, taken)
        val canRefund = graph.canRefund(node.code, taken)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MutedText(when {
                node.kind == AtlasNode.START -> ui("atlas.start_hint")
                isTaken && canRefund -> ui("atlas.refund_hint", price)
                isTaken -> ui("atlas.holds_hint")
                canTake && available <= 0 -> ui("atlas.no_points")
                canTake -> ui("atlas.take_hint")
                else -> ui("atlas.far_hint")
            }, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            when {
                canTake -> Button(onClick = onTake, enabled = enabled && available > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Sky.take, contentColor = Color.White)) { Text(ui("atlas.take")) }
                canRefund -> OutlinedButton(onClick = onRefund, enabled = enabled) { Text(ui("atlas.refund"), color = Sky.text) }
            }
        }
    }
}

/** «+4% Количество предметов на картах»; a count — chests, fountains, Vaal modifiers — carries no percent. */
private fun effectLine(effect: AtlasEffect): String {
    val sign = if (effect.value >= 0) "+" else "−"
    val unit = if (effect.stat in AtlasEffects.flat) "" else "%"
    // The dictionary's «Атлас: …» prefix is the screen's own title here (2.73.0).
    return "$sign${number(kotlin.math.abs(effect.value))}$unit ${statTitle(effect.stat).substringAfter(": ")}"
}
