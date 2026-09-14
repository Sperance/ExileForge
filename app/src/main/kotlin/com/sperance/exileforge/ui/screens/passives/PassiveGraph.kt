package com.sperance.exileforge.ui.screens.passives

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Ink
import kotlin.math.abs

/** Coordinates and graph topology come from the server. Client never decides allocation validity. */
@Composable internal fun PassiveGraph(tree: PassiveTree, allocated: Set<String>, available: Set<String>, selected: String?, onSelect: (String) -> Unit) {
    var zoom by remember(tree.revision) { mutableFloatStateOf(1f) }
    var pan by remember(tree.revision) { mutableStateOf(Offset.Zero) }
    val byId = remember(tree) { tree.nodes.associateBy { it.id } }
    val extentX = tree.nodes.maxOf { abs(it.x) }.toFloat() * 2 + 120f
    val extentY = tree.nodes.maxOf { abs(it.y) }.toFloat() * 2 + 120f
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().height(400.dp).background(Ink)) {
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }
        val fit = minOf(width / extentX.coerceAtLeast(200f), height / extentY.coerceAtLeast(200f))
        val factor = fit * zoom
        val center = Offset(width / 2, height / 2)
        LaunchedEffect(selected) {
            byId[selected]?.let { pan = Offset(-it.x.toFloat(), -it.y.toFloat()) * factor }
        }
        Canvas(Modifier.fillMaxSize().testTag("passive_graph")
            .semantics { contentDescription = "Древо навыков: ${tree.nodes.size} узлов. Масштабируйте жестом или выберите узел из списка ниже." }
            .pointerInput(tree.revision, width, height) {
                detectTransformGestures { centroid, drag, scale, _ ->
                    val next = (zoom * scale).coerceIn(.6f, 6f)
                    pan = (pan - (centroid - center)) * (next / zoom) + centroid - center + drag
                    zoom = next
                }
            }
            .pointerInput(tree, zoom, pan) {
                detectTapGestures { position ->
                    val point = (position - center - pan) / factor
                    val nearest = tree.nodes.minByOrNull { (Offset(it.x.toFloat(), it.y.toFloat()) - point).getDistance() }
                    val tolerance = with(density) { 24.dp.toPx() } / factor
                    if(nearest != null && (Offset(nearest.x.toFloat(), nearest.y.toFloat()) - point).getDistance() <= maxOf(28f, tolerance)) onSelect(nearest.id)
                }
            }) {
            withTransform({ translate(center.x + pan.x, center.y + pan.y); scale(factor, factor, Offset.Zero) }) {
                tree.edges.forEach { edge ->
                    val a = byId[edge.from]; val b = byId[edge.to]
                    if(a != null && b != null) drawLine(if(a.id in allocated && b.id in allocated) Gold else Color(0xFF344152), Offset(a.x.toFloat(), a.y.toFloat()), Offset(b.x.toFloat(), b.y.toFloat()), if(a.id in allocated && b.id in allocated) 5f else 2f)
                }
                tree.nodes.forEach { node ->
                    val point = Offset(node.x.toFloat(), node.y.toFloat())
                    val radius = when(node.kind) { PassiveNodeKind.SMALL -> 12f; PassiveNodeKind.NOTABLE -> 21f; PassiveNodeKind.KEYSTONE -> 28f; PassiveNodeKind.ORIGIN -> 25f }
                    val colour = when(node.id) { in allocated -> Gold; in available -> Color(0xFF62C6B0); else -> Color(0xFF77849A) }
                    drawCircle(Color(0xFF111D2B), radius + 4, point)
                    if(node.kind == PassiveNodeKind.KEYSTONE) {
                        val shape = Path().apply { moveTo(point.x, point.y - radius); lineTo(point.x + radius, point.y); lineTo(point.x, point.y + radius); lineTo(point.x - radius, point.y); close() }
                        drawPath(shape, colour, style = Stroke(4f))
                    } else drawCircle(colour, radius, point, style = Stroke(if(node.kind == PassiveNodeKind.SMALL) 3f else 4f))
                    if(node.id in allocated) drawCircle(colour.copy(alpha = .7f), radius * .5f, point)
                    else if(node.id in available) drawCircle(colour, 4f, point)
                    if(node.id == selected) drawCircle(Color.White, radius + 9, point, style = Stroke(3f))
                }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).background(Ink.copy(alpha = .85f)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = { val next = (zoom / 1.4f).coerceAtLeast(.6f); pan *= next / zoom; zoom = next }, modifier = Modifier.semantics { contentDescription = "Отдалить дерево" }) { Text("−") }
            TextButton(onClick = { val next = (zoom * 1.4f).coerceAtMost(6f); pan *= next / zoom; zoom = next }, modifier = Modifier.semantics { contentDescription = "Приблизить дерево" }) { Text("+") }
            TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Центр") }
        }
        Text("Золото — изучено · зелёный — доступно", color = Gold, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter).background(Ink.copy(alpha = .9f)).padding(6.dp))
    }
}
