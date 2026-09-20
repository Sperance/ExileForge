package com.sperance.exileforge.ui.screens.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.modifierTitle
import com.sperance.exileforge.core.display.modifierValues
import com.sperance.exileforge.core.display.nodeTypeTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@Composable fun SkillTreeScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(tr("Дерево навыков", "Passive tree"),
            tr("Узлов в дереве: ${s.treeNodes.size}", "${s.treeNodes.size} nodes in the tree"), ForgeGlyphs.Constellation)
        EntitySpinner(tr("Персонаж", "Character"), s.characterId, EntitySource.CHARACTER, !s.busy, vm::characterId)
        SkillTreePanel(s, vm::selectNode, vm::allocateNode, vm::refundNode, vm::resetTree)
    }
}

/**
 * The passive tree.
 *
 * The graph is the server's: node positions, edges, costs and bonuses all arrive seeded, and which
 * node may be taken next is decided by `allocate` rather than guessed at here. The panel draws what
 * it was given and sends one node code at a time.
 */
@Composable fun SkillTreePanel(s: ForgeState, onSelect: (String) -> Unit, onAllocate: (String) -> Unit,
    onRefund: (String) -> Unit, onReset: () -> Unit) {
    val hero = s.hero
    val taken = hero?.tree?.takenCodes.orEmpty()
    val enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin) && hero != null
    if (hero == null) InfoCard(tr("Герой не загружен", "The hero is not loaded"),
        tr("Выберите персонажа и обновите его во вкладке «Герой».", "Choose a character and refresh it on the Hero tab."))
    else ForgePanel {
        Engraved(hero.character.name)
        PropertyRow(tr("Очков всего", "Points total"), hero.tree.total.toString(), "level")
        PropertyRow(tr("Потрачено", "Spent"), hero.tree.spent.toString(), "level")
        PropertyRow(tr("Доступно", "Available"), hero.tree.available.toString(), "level")
        Text(tr("Очки дают уровни: таблицу прогрессии ведёт сервер.", "Points come from levels: the progression table is the server's."),
            color = Muted, style = MaterialTheme.typography.bodySmall)
    }
    if (s.treeNodes.isEmpty()) {
        InfoCard(tr("Дерево не загружено", "The tree is not loaded"),
            tr("Сервер не вернул ни одного узла. Обновите героя.", "The server served no nodes. Refresh the hero."))
        return
    }
    TreeCanvas(s, taken, onSelect)
    NodeDetails(s, s.treeNodes.firstOrNull { it.code == s.selectedNode }, taken, enabled, onAllocate, onRefund)
    if (hero != null) OutlinedButton(enabled = enabled && hero.tree.nodes.isNotEmpty(), onClick = onReset,
        modifier = Modifier.fillMaxWidth()) { Text(tr("Сбросить дерево полностью", "Reset the whole tree")) }
}

/**
 * The graph, drawn from the coordinates the server seeded.
 *
 * Panning and zooming are the only interaction beyond a tap: the layout is fixed data, so the
 * canvas never moves a node, it only chooses where to look.
 */
@Composable private fun TreeCanvas(s: ForgeState, taken: Set<String>, onSelect: (String) -> Unit) {
    val nodes = s.treeNodes
    val byCode = remember(nodes) { nodes.associateBy { it.code } }
    val bounds = remember(nodes) { Bounds.of(nodes) }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    ForgePanel {
        Engraved(tr("Карта дерева", "Tree map"))
        Canvas(Modifier.fillMaxWidth().height(360.dp)
            .pointerInput(nodes) {
                detectTransformGestures { _, drag, zoom, _ ->
                    scale = (scale * zoom).coerceIn(.4f, 4f)
                    pan += drag
                }
            }
            .pointerInput(nodes, scale, pan) {
                detectTapGestures { tap ->
                    // The nearest node wins, but only within its own circle: a tap on bare canvas changes nothing.
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val hit = nodes.minByOrNull { (place(it, bounds, width, height, scale, pan) - tap).getDistanceSquared() }
                    if (hit != null && (place(hit, bounds, width, height, scale, pan) - tap).getDistance() <= radius(hit) * scale * 2f) onSelect(hit.code)
                }
            }) {
            val width = size.width
            val height = size.height
            // Edges first, so a node always sits on top of the lines that reach it.
            nodes.forEach { node ->
                val from = place(node, bounds, width, height, scale, pan)
                node.connections.forEach { code ->
                    val other = byCode[code] ?: return@forEach
                    // Each edge is declared on both ends; drawing it once keeps the line crisp.
                    if (node.code < code) {
                        val both = node.code in taken && code in taken
                        drawLine(if (both) Gold.copy(alpha = .8f) else Bronze.copy(alpha = .35f),
                            from, place(other, bounds, width, height, scale, pan), if (both) 3f else 1.5f)
                    }
                }
            }
            nodes.forEach { node ->
                val centre = place(node, bounds, width, height, scale, pan)
                val colour = nodeColour(node, node.code == s.selectedNode)
                drawCircle(colour.copy(alpha = if (node.code in taken) .85f else .18f), radius(node) * scale, centre)
                drawCircle(colour, radius(node) * scale, centre, style = Stroke(if (node.code == s.selectedNode) 3.5f else 1.5f))
            }
        }
        Text(tr("Потяните, чтобы сдвинуть, сведите пальцы для масштаба, коснитесь узла, чтобы выбрать.",
                "Drag to pan, pinch to zoom, tap a node to select it."), color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { scale = 1f; pan = Offset.Zero }) { Text(tr("Сбросить вид", "Reset the view")) }
            Text(tr("Взято узлов: ${taken.size}", "Nodes taken: ${taken.size}"), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The chosen node, its bonuses and the two commands the server accepts for it. */
@Composable private fun NodeDetails(s: ForgeState, node: SkillTreeNode?, taken: Set<String>, enabled: Boolean,
    onAllocate: (String) -> Unit, onRefund: (String) -> Unit) {
    if (node == null) {
        InfoCard(tr("Узел не выбран", "No node selected"), tr("Коснитесь узла на карте, чтобы увидеть, что он даёт.", "Tap a node on the map to see what it gives."))
        return
    }
    val allocated = node.code in taken
    ForgePanel(accent = nodeColour(node, true)) {
        // The node's name is this panel's title, so it keeps its own casing rather than being
        // shouted as an Engraved caption the way a section heading is.
        Text(node.name.ifBlank { node.code }, color = nodeColour(node, true), style = MaterialTheme.typography.titleMedium)
        PropertyRow(tr("Вид узла", "Node type"), nodeTypeTitle(node.type.name, s.lang), "node")
        PropertyRow(tr("Стоимость", "Cost"), node.cost.toString(), "level")
        PropertyRow(tr("Состояние", "State"), if (allocated) tr("Взят", "Taken") else tr("Не взят", "Not taken"), "node")
        node.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
        OrnateDivider()
        if (node.params.isEmpty()) Text(tr("Бонусов нет", "No bonuses"), color = Muted)
        node.params.forEach { modifier ->
            val document = modifierDocument(modifier.modifierId, modifier.values)
            PropertyRow(modifierTitle(document, s.definitions), modifierValues(document, s.definitions), modifier.modifierId)
        }
        OrnateDivider()
        if (allocated) OutlinedButton(enabled = enabled && node.type != SkillNodeType.START, onClick = { onRefund(node.code) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Вернуть узел", "Give the node back"))
        } else Button(enabled = enabled, onClick = { onAllocate(node.code) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Взять узел", "Take the node"))
        }
        Text(if (node.type == SkillNodeType.START)
                tr("Стартовый узел задаёт класс, и вернуть его можно только полным сбросом.", "The start node comes with the class and only a full reset gives it back.")
            else tr("Брать можно только рядом с уже взятым узлом, а вернуть — только если остальное дерево не повиснет. Проверяет сервер.",
                    "A node is taken next to one already taken, and given back only if the rest stays connected. The server checks."),
            color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** A fixed modifier as the display helpers expect it: they read documents, not typed models. */
private fun modifierDocument(modifierId: String, values: List<Double>): JsonObject = buildJsonObject {
    put("modifierId", modifierId)
    putJsonArray("values") { values.forEach { add(JsonPrimitive(it)) } }
}

private fun nodeColour(node: SkillTreeNode, selected: Boolean): Color = when {
    node.type == SkillNodeType.KEYSTONE -> LifeRed
    node.type == SkillNodeType.NOTABLE -> if (selected) GoldBright else Gold
    node.type == SkillNodeType.START -> ShieldCyan
    else -> Rune
}

private fun radius(node: SkillTreeNode): Float = when (node.type) {
    SkillNodeType.KEYSTONE -> 13f; SkillNodeType.NOTABLE -> 10f; SkillNodeType.START -> 12f; SkillNodeType.SMALL -> 6f
}

/** Extent of the seeded coordinates, so the whole tree fits whatever canvas it is given. */
private data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    val spanX get() = max(1f, maxX - minX)
    val spanY get() = max(1f, maxY - minY)
    companion object {
        fun of(nodes: List<SkillTreeNode>) = Bounds(
            nodes.minOfOrNull { it.positionX.toFloat() } ?: 0f, nodes.maxOfOrNull { it.positionX.toFloat() } ?: 1f,
            nodes.minOfOrNull { it.positionY.toFloat() } ?: 0f, nodes.maxOfOrNull { it.positionY.toFloat() } ?: 1f)
    }
}

private fun place(node: SkillTreeNode, bounds: Bounds, width: Float, height: Float, scale: Float, pan: Offset): Offset {
    val margin = 28f
    val fit = min((width - margin * 2) / bounds.spanX, (height - margin * 2) / bounds.spanY)
    val x = (node.positionX - bounds.minX) * fit - bounds.spanX * fit / 2
    val y = (node.positionY - bounds.minY) * fit - bounds.spanY * fit / 2
    return Offset(width / 2 + x * scale + pan.x, height / 2 + y * scale + pan.y)
}
