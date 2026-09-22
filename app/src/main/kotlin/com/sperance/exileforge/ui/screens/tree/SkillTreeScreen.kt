package com.sperance.exileforge.ui.screens.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.nodeTypeTitle
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.skilltree.SkillNodeType
import com.sperance.exileforge.core.model.skilltree.StatContribution
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.reachableFrom
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
    // No scrolling column here: the map owns the height, and everything that used to sit under it
    // lives in the sheet. A pannable canvas inside a scroll fights the scroll for every drag.
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(tr("Дерево навыков", "Passive tree"),
            tr("Узлов в дереве: ${s.treeNodes.size}", "${s.treeNodes.size} nodes in the tree"), ForgeGlyphs.Constellation)
        SkillTreePanel(s, vm::selectNode, vm::allocateNode, vm::refundNode, vm::resetTree, vm::nodeQuery,
            onSocket = vm::socketJewel, onUnsocket = vm::unsocketJewel, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * The passive tree.
 *
 * The graph is the server's: node positions, edges, costs and bonuses all arrive seeded, and which
 * node may be taken next is decided by `allocate` rather than guessed at here. The panel draws what
 * it was given and sends one node code at a time.
 *
 * The map takes the whole panel and everything else — the point balance, the search, the chosen
 * node and its two commands — opens as a sheet over it. A hundred and twenty nodes need the room,
 * and the details are read one node at a time rather than alongside.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SkillTreePanel(s: ForgeState, onSelect: (String) -> Unit, onAllocate: (String) -> Unit,
    onRefund: (String) -> Unit, onReset: () -> Unit, onQuery: (String) -> Unit = {},
    onSocket: (String, String) -> Unit = { _, _ -> }, onUnsocket: (String) -> Unit = {},
    modifier: Modifier = Modifier) {
    val hero = s.hero
    var detailsOpen by remember { mutableStateOf(false) }
    var nodeOpen by remember { mutableStateOf(false) }
    // Each of the three tree commands spends something a player cannot get back for free — a point
    // or an orb — so each is asked about, and the question names the price.
    var confirmAllocate by remember { mutableStateOf<String?>(null) }
    var confirmRefund by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    if (hero == null) {
        InfoCard(tr("Герой не загружен", "The hero is not loaded"),
            tr("Обновите героя во вкладке «Герой».", "Refresh the hero on the Hero tab."))
        return
    }
    if (s.treeNodes.isEmpty()) {
        InfoCard(tr("Дерево не загружено", "The tree is not loaded"),
            tr("Сервер не вернул ни одного узла. Обновите героя.", "The server served no nodes. Refresh the hero."))
        return
    }
    val taken = hero.tree.takenCodes
    val enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin)
    // Which nodes are one step away: neighbours of what is taken, or the class's own start when
    // nothing is taken yet. The server still decides — this only says where to look on 122 nodes.
    val reachable = remember(s.treeNodes, taken) { reachableFrom(s.treeNodes, taken) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Очки: ${hero.tree.available} из ${hero.tree.total}", "Points: ${hero.tree.available} of ${hero.tree.total}"),
                color = Gold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { detailsOpen = true }) { Text(tr("Подробно", "Details")) }
        }
        // A tap opens a small window about that one node, so the map stays in sight; everything
        // about the tree as a whole lives behind "Подробно".
        TreeCanvas(s, taken, reachable, Modifier.weight(1f)) { code -> onSelect(code); nodeOpen = true }
        Text(tr("Потяните, чтобы сдвинуть, сведите пальцы для масштаба, коснитесь узла, чтобы выбрать.",
                "Drag to pan, pinch to zoom, tap a node to select it."), color = Muted, style = MaterialTheme.typography.bodySmall)
    }
    // The small window about the chosen node: what it gives, and the one command over it. It is
    // deliberately not expanded to full height — half the point is seeing where the branch leads.
    if (nodeOpen) ModalBottomSheet(onDismissRequest = { nodeOpen = false }, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NodeDetails(s, s.treeNodes.firstOrNull { it.code == s.selectedNode }, taken, enabled,
                onAllocate = { nodeOpen = false; confirmAllocate = it },
                onRefund = { nodeOpen = false; confirmRefund = it },
                onSocket = { instance, code -> nodeOpen = false; onSocket(instance, code) },
                onUnsocket = { nodeOpen = false; onUnsocket(it) })
            Spacer(Modifier.height(8.dp))
        }
    }

    TreeConfirmations(s, confirmAllocate, confirmRefund, confirmReset,
        onClear = { confirmAllocate = null; confirmRefund = null; confirmReset = false },
        onAllocate = onAllocate, onRefund = onRefund, onReset = onReset)

    if (detailsOpen) ModalBottomSheet(onDismissRequest = { detailsOpen = false }, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ForgePanel {
                    Engraved(hero.character.name)
                    PropertyRow(tr("Очков всего", "Points total"), hero.tree.total.toString(), "level")
                    PropertyRow(tr("Потрачено", "Spent"), hero.tree.spent.toString(), "level")
                    PropertyRow(tr("Доступно", "Available"), hero.tree.available.toString(), "level")
                    Text(tr("Очки дают уровни: таблицу прогрессии ведёт сервер.", "Points come from levels: the progression table is the server's."),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                ForgePanel(accent = Rune) {
                    Engraved(tr("Даёт дерево целиком", "What the whole tree gives"), Rune)
                    if (hero.tree.totals.isEmpty()) Text(tr("Взятые узлы ничего не дают", "The taken nodes give nothing"), color = Muted)
                    // The server sums this: two INCREASED add up while two MORE multiply, so
                    // adding the snapshots here would lie exactly where a player is choosing.
                    // It is the tree's contribution, not the character's total — which is why a
                    // percentage stays a percentage and is written with its sign.
                    hero.tree.totals.forEach { total ->
                        PropertyRow(statTitle(total.stat, s.lang), contributionText(total), total.stat)
                    }
                }
            }
            item { TreeSearch(s, onQuery) { code -> onSelect(code); detailsOpen = false; nodeOpen = true } }
            item {
                OutlinedButton(enabled = enabled && hero.tree.nodes.size > 1, onClick = { detailsOpen = false; confirmReset = true },
                    modifier = Modifier.fillMaxWidth()) { Text(tr("Сбросить дерево полностью", "Reset the whole tree")) }
                Text(tr("Сброс стоит по сфере сожаления за каждый возвращаемый узел — столько же, сколько вернуть их по одному.",
                        "A reset costs one Orb of Regret per node returned — the same as giving them back one at a time."),
                    color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Finding a node by name.
 *
 * The tree is over a hundred nodes across seven class areas, so panning to one by eye is no longer
 * realistic. A match selects the node, which is what the map draws a ring around.
 */
@Composable private fun TreeSearch(s: ForgeState, onQuery: (String) -> Unit, onSelect: (String) -> Unit) {
    val matches = remember(s.treeNodes, s.nodeQuery) {
        if (s.nodeQuery.isBlank()) emptyList()
        else s.treeNodes.filter { it.title.contains(s.nodeQuery.trim(), true) || it.code.contains(s.nodeQuery.trim(), true) }.take(8)
    }
    ForgePanel {
        Engraved(tr("Найти узел", "Find a node"))
        OutlinedTextField(s.nodeQuery, onQuery, label = { Text(tr("Название узла", "Node name")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (s.nodeQuery.isNotBlank() && matches.isEmpty()) Text(tr("Ничего не найдено", "Nothing found"), color = Muted)
        matches.forEach { node ->
            TextButton(onClick = { onSelect(node.code) }, modifier = Modifier.fillMaxWidth()) {
                Text("${node.title} · ${nodeTypeTitle(node.type.name, s.lang)}",
                    color = nodeColour(node, node.code == s.selectedNode))
            }
        }
    }
}

/**
 * The graph, drawn from the coordinates the server seeded.
 *
 * Panning and zooming are the only interaction beyond a tap: the layout is fixed data, so the
 * canvas never moves a node, it only chooses where to look. It clips, because a canvas does not:
 * without that the outer nodes are painted over whatever the map happens to be sitting on. And the
 * pan is held to the tree's own half-extent, so a stray flick cannot drag the whole graph away and
 * leave an empty rectangle with no way back but the reset.
 */
@Composable private fun TreeCanvas(s: ForgeState, taken: Set<String>, reachable: Set<String>,
    modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val nodes = s.treeNodes
    val byCode = remember(nodes) { nodes.associateBy { it.code } }
    val bounds = remember(nodes) { Bounds.of(nodes) }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxSize().clipToBounds()
            .pointerInput(nodes) {
                detectTransformGestures { _, drag, zoom, _ ->
                    scale = (scale * zoom).coerceIn(.4f, 4f)
                    val limit = panLimit(bounds, size.width.toFloat(), size.height.toFloat(), scale)
                    pan = Offset((pan.x + drag.x).coerceIn(-limit.x, limit.x), (pan.y + drag.y).coerceIn(-limit.y, limit.y))
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
                val here = node.code in taken
                val next = node.code in reachable
                // Taken is solid, reachable is half-lit and ringed, the rest is barely there.
                drawCircle(colour.copy(alpha = if (here) .85f else if (next) .45f else .12f), radius(node) * scale, centre)
                drawCircle(if (next && !here) GoldBright else colour, radius(node) * scale, centre,
                    style = Stroke(if (node.code == s.selectedNode) 3.5f else if (next) 2.5f else 1f))
            }
        }
        Row(Modifier.align(Alignment.BottomStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { scale = 1f; pan = Offset.Zero }) { Text(tr("Сбросить вид", "Reset the view")) }
            Text(tr("Взято ${taken.size} · доступно ${reachable.size}", "${taken.size} taken · ${reachable.size} within reach"),
                color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The chosen node: what it gives, and the one command the server accepts for it.
 *
 * A socket is the exception, because it gives nothing by itself — what it holds is a jewel, and
 * the command there is to put one in or take it out.
 */
@Composable private fun NodeDetails(s: ForgeState, node: SkillTreeNode?, taken: Set<String>, enabled: Boolean,
    onAllocate: (String) -> Unit, onRefund: (String) -> Unit,
    onSocket: (String, String) -> Unit = { _, _ -> }, onUnsocket: (String) -> Unit = {}) {
    if (node == null) {
        InfoCard(tr("Узел не выбран", "No node selected"), tr("Коснитесь узла на карте, чтобы увидеть, что он даёт.", "Tap a node on the map to see what it gives."))
        return
    }
    val allocated = node.code in taken
    ForgePanel(accent = nodeColour(node, true)) {
        // The node's name is this panel's title, so it keeps its own casing rather than being
        // shouted as an Engraved caption the way a section heading is.
        Text(node.title, color = nodeColour(node, true), style = MaterialTheme.typography.titleMedium)
        PropertyRow(tr("Вид узла", "Node type"), nodeTypeTitle(node.type.name, s.lang), "node")
        PropertyRow(tr("Стоимость", "Cost"), node.cost.toString(), "level")
        PropertyRow(tr("Состояние", "State"), if (allocated) tr("Взят", "Taken") else tr("Не взят", "Not taken"), "node")
        node.details.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }

        OrnateDivider()
        if (node.type == SkillNodeType.JEWEL_SOCKET) {
            SocketContents(s, node, allocated, enabled, onSocket, onUnsocket)
        } else {
            if (node.params.isEmpty()) Text(tr("Бонусов нет", "No bonuses"), color = Muted)
            node.params.forEach { modifier ->
                ModifierLine(modifierDocument(modifier.modifierId, modifier.values), s.definitions)
            }
        }

        OrnateDivider()
        if (allocated) OutlinedButton(enabled = enabled && node.type != SkillNodeType.START, onClick = { onRefund(node.code) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Сбросить (сфера сожаления)", "Give back (Orb of Regret)"))
        } else Button(enabled = enabled, onClick = { onAllocate(node.code) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Взять узел", "Take the node"))
        }
        Text(when {
                node.type == SkillNodeType.START ->
                    tr("Стартовый узел задаёт класс, и вернуть его можно только полным сбросом.", "The start node comes with the class and only a full reset gives it back.")
                allocated ->
                    tr("Возврат стоит одну сферу сожаления. Вернуть можно только тот узел, без которого остальное дерево не повиснет — проверяет сервер.",
                       "Giving a node back costs one Orb of Regret, and only a node the rest of the tree does not hang from — the server checks.")
                else ->
                    tr("Брать можно только рядом с уже взятым узлом. Проверяет сервер.",
                       "A node is taken next to one already taken. The server checks.")
            },
            color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * What sits in a socket, and what can be put there.
 *
 * A jewel is an ordinary equipment instance, so the stash is where the candidates come from: every
 * jewel the character owns that is not already in another socket. Whether this one may go in is
 * still the server's call — the socket has to be taken and free — so the list offers and the
 * refusal explains.
 */
@Composable private fun SocketContents(s: ForgeState, node: SkillTreeNode, allocated: Boolean, enabled: Boolean,
    onSocket: (String, String) -> Unit, onUnsocket: (String) -> Unit) {
    val hero = s.hero ?: return
    val inside = hero.jewels[node.code]

    if (inside != null) {
        val document = inventoryDocument(inside, s.inventoryBases[inside.equipmentId])
        ItemRow(document, definitions = s.definitions, enabled = false,
            note = if (allocated) tr("Работает", "Working") else tr("Гнездо не взято", "Socket not taken"),
            noteColor = if (allocated) Gold else LifeRed) { }
        OutlinedButton(enabled = enabled, onClick = { onUnsocket(inside.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Вынуть самоцвет", "Take the jewel out"))
        }
        return
    }

    val free = hero.inventory.filter { instance ->
        !instance.socketed && s.inventoryBases[instance.equipmentId]?.text("slot") == "JEWEL"
    }
    if (!allocated) {
        Text(tr("Сначала возьмите гнездо.", "Take the socket first."), color = Muted, style = MaterialTheme.typography.bodySmall)
        return
    }
    if (free.isEmpty()) {
        Text(tr("Свободных самоцветов нет.", "No free jewels."), color = Muted, style = MaterialTheme.typography.bodySmall)
        return
    }
    Engraved(tr("Вставить самоцвет", "Put a jewel in"))
    free.forEach { instance ->
        val document = inventoryDocument(instance, s.inventoryBases[instance.equipmentId])
        ItemRow(document, definitions = s.definitions, enabled = enabled) { onSocket(instance.id, node.code) }
    }
}

@Composable private fun TreeConfirmations(
    s: ForgeState,
    allocate: String?, refund: String?, reset: Boolean,
    onClear: () -> Unit,
    onAllocate: (String) -> Unit, onRefund: (String) -> Unit, onReset: () -> Unit,
) {
    val nodeName = { code: String -> s.treeNodes.firstOrNull { it.code == code }?.title ?: code }
    allocate?.let { code ->
        val cost = s.treeNodes.firstOrNull { it.code == code }?.cost ?: 1
        ConfirmDialog(
            title = tr("Взять узел?", "Take the node?"),
            text = tr("«${nodeName(code)}» стоит $cost ${points(cost)}. Вернуть узел потом можно только за сферу сожаления.",
                      "\"${nodeName(code)}\" costs $cost ${points(cost)}. Giving it back later costs an Orb of Regret."),
            confirm = tr("Взять", "Take"), onDismiss = onClear) { onAllocate(code) }
    }
    refund?.let { code ->
        ConfirmDialog(
            title = tr("Вернуть узел?", "Give the node back?"),
            text = tr("«${nodeName(code)}» вернётся, и это спишет одну сферу сожаления.",
                      "\"${nodeName(code)}\" goes back, and that spends one Orb of Regret."),
            confirm = tr("Вернуть", "Give back"), onDismiss = onClear) { onRefund(code) }
    }
    if (reset) {
        // The start node is not given back, so it is not paid for — the count says what is.
        val returned = (s.hero?.tree?.nodes?.size ?: 1) - 1
        ConfirmDialog(
            title = tr("Сбросить дерево?", "Reset the tree?"),
            text = tr("Вернётся $returned ${nodes(returned)} и спишется столько же сфер сожаления. Стартовый узел класса останется.",
                      "$returned ${nodes(returned)} go back and as many Orbs of Regret are spent. The class's start node stays."),
            confirm = tr("Сбросить", "Reset"), onDismiss = onClear) { onReset() }
    }
}

/** Russian counts its nouns; English does not have to try. */
private fun points(n: Int) = tr(if (n % 10 == 1 && n % 100 != 11) "очко" else if (n % 10 in 2..4 && n % 100 !in 12..14) "очка" else "очков",
                                if (n == 1) "point" else "points")

private fun nodes(n: Int) = tr(if (n % 10 == 1 && n % 100 != 11) "узел" else if (n % 10 in 2..4 && n % 100 !in 12..14) "узла" else "узлов",
                               if (n == 1) "node" else "nodes")

/**
 * One line of what the tree gives: a number, and whether it is a flat one or a percentage.
 *
 * The operation is the whole point. "+120" and "+40%" to the same characteristic are different
 * statements, and a line that dropped the distinction would be a third, untrue one.
 */
private fun contributionText(total: StatContribution): String {
    val number = statNumber(total.stat, total.value)
    val signed = if (total.value > 0) "+$number" else number
    return when (total.operation) {
        "INCREASED", "MORE" -> "$signed%"
        // SET replaces the base outright, so it is not an addition and carries no sign.
        "SET" -> statNumber(total.stat, total.value)
        else -> signed
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
    // A socket gives nothing itself, so on the map it has to stand for what it can hold rather
    // than blend into the small nodes around it.
    node.type == SkillNodeType.JEWEL_SOCKET -> ManaBlue
    else -> Rune
}

private fun radius(node: SkillTreeNode): Float = when (node.type) {
    SkillNodeType.KEYSTONE -> 13f; SkillNodeType.NOTABLE -> 10f; SkillNodeType.START -> 12f
    SkillNodeType.JEWEL_SOCKET -> 11f; SkillNodeType.SMALL -> 6f
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

private const val MARGIN = 28f

/** How many pixels of the seeded graph one pixel of canvas is worth, before the zoom. */
private fun fitFactor(bounds: Bounds, width: Float, height: Float): Float =
    min((width - MARGIN * 2) / bounds.spanX, (height - MARGIN * 2) / bounds.spanY)

/**
 * How far the map may be dragged: the tree's own half-extent on screen.
 *
 * At the limit the far edge of the tree has reached the middle of the canvas, so there is always
 * something drawn to drag back by. Anything looser and a flick leaves an empty rectangle.
 */
private fun panLimit(bounds: Bounds, width: Float, height: Float, scale: Float): Offset {
    val fit = fitFactor(bounds, width, height)
    return Offset(max(0f, bounds.spanX * fit * scale / 2), max(0f, bounds.spanY * fit * scale / 2))
}

private fun place(node: SkillTreeNode, bounds: Bounds, width: Float, height: Float, scale: Float, pan: Offset): Offset {
    val fit = fitFactor(bounds, width, height)
    val x = (node.positionX - bounds.minX) * fit - bounds.spanX * fit / 2
    val y = (node.positionY - bounds.minY) * fit - bounds.spanY * fit / 2
    return Offset(width / 2 + x * scale + pan.x, height / 2 + y * scale + pan.y)
}
