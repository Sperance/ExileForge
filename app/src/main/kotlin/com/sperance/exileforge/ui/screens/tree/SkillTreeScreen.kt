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
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.currency.CurrencyOrb
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
        ScreenHeader(ui("tree.title"),
            ui("tree.node_count", s.world.treeNodes.size), ForgeGlyphs.Constellation)
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
    val hero = s.play.hero
    var detailsOpen by remember { mutableStateOf(false) }
    var nodeOpen by remember { mutableStateOf(false) }
    // Each of the three tree commands spends something a player cannot get back for free — a point
    // or an orb — so each is asked about, and the question names the price.
    var confirmAllocate by remember { mutableStateOf<String?>(null) }
    var confirmRefund by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    if (hero == null) {
        InfoCard(ui("tree.no_hero"),
            ui("tree.no_hero_hint"))
        return
    }
    if (s.world.treeNodes.isEmpty()) {
        InfoCard(ui("tree.not_loaded"),
            ui("tree.not_loaded_hint"))
        return
    }
    val taken = hero.tree.takenCodes
    val enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    // Which nodes are one step away: neighbours of what is taken, or the class's own start when
    // nothing is taken yet. The server still decides — this only says where to look on 122 nodes.
    val reachable = remember(s.world.treeNodes, taken) { reachableFrom(s.world.treeNodes, taken) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("tree.points", hero.tree.available, hero.tree.total),
                color = Gold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { detailsOpen = true }) { Text(ui("tree.details")) }
        }
        // A tap opens a small window about that one node, so the map stays in sight; everything
        // about the tree as a whole lives behind "Подробно".
        TreeCanvas(s, taken, reachable, Modifier.weight(1f)) { code -> onSelect(code); nodeOpen = true }
        Text(ui("tree.gesture_hint"), color = Muted, style = MaterialTheme.typography.bodySmall)
    }
    // The small window about the chosen node: what it gives, and the one command over it. It is
    // deliberately not expanded to full height — half the point is seeing where the branch leads.
    if (nodeOpen) ModalBottomSheet(onDismissRequest = { nodeOpen = false }, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NodeDetails(s, s.world.treeNodes.firstOrNull { it.code == s.play.selectedNode }, taken, enabled,
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
                    PropertyRow(ui("tree.points_total"), hero.tree.total.toString(), "level")
                    PropertyRow(ui("tree.points_spent"), hero.tree.spent.toString(), "level")
                    PropertyRow(ui("tree.points_available"), hero.tree.available.toString(), "level")
                    Text(ui("tree.points_note"),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                ForgePanel(accent = Rune) {
                    Engraved(ui("tree.totals_title"), Rune)
                    if (hero.tree.totals.isEmpty()) Text(ui("tree.totals_empty"), color = Muted)
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
                    modifier = Modifier.fillMaxWidth()) { Text(ui("tree.reset_all")) }
                Text(ui("tree.reset_note"),
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
    val matches = remember(s.world.treeNodes, s.play.nodeQuery) {
        if (s.play.nodeQuery.isBlank()) emptyList()
        else s.world.treeNodes.filter { it.title.contains(s.play.nodeQuery.trim(), true) || it.code.contains(s.play.nodeQuery.trim(), true) }.take(8)
    }
    ForgePanel {
        Engraved(ui("tree.find_node"))
        OutlinedTextField(s.play.nodeQuery, onQuery, label = { Text(ui("tree.node_name")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (s.play.nodeQuery.isNotBlank() && matches.isEmpty()) Text(ui("tree.nothing_found"), color = Muted)
        matches.forEach { node ->
            TextButton(onClick = { onSelect(node.code) }, modifier = Modifier.fillMaxWidth()) {
                Text("${node.title} · ${nodeTypeTitle(node.type.name, s.lang)}",
                    color = nodeColour(node, node.code == s.play.selectedNode))
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
    val nodes = s.world.treeNodes
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
                val colour = nodeColour(node, node.code == s.play.selectedNode)
                val here = node.code in taken
                val next = node.code in reachable
                // Taken is solid, reachable is half-lit and ringed, the rest is barely there.
                drawCircle(colour.copy(alpha = if (here) .85f else if (next) .45f else .12f), radius(node) * scale, centre)
                drawCircle(if (next && !here) GoldBright else colour, radius(node) * scale, centre,
                    style = Stroke(if (node.code == s.play.selectedNode) 3.5f else if (next) 2.5f else 1f))
            }
        }
        Row(Modifier.align(Alignment.BottomStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { scale = 1f; pan = Offset.Zero }) { Text(ui("tree.reset_view")) }
            Text(ui("tree.taken_reachable", taken.size, reachable.size),
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
        InfoCard(ui("tree.no_selection"), ui("tree.no_selection_hint"))
        return
    }
    val allocated = node.code in taken
    ForgePanel(accent = nodeColour(node, true)) {
        // The node's name is this panel's title, so it keeps its own casing rather than being
        // shouted as an Engraved caption the way a section heading is.
        Text(node.title, color = nodeColour(node, true), style = MaterialTheme.typography.titleMedium)
        PropertyRow(ui("tree.node_type"), nodeTypeTitle(node.type.name, s.lang), "node")
        PropertyRow(ui("tree.cost"), node.cost.toString(), "level")
        PropertyRow(ui("card.state"), if (allocated) ui("tree.taken") else ui("tree.not_taken"), "node")
        node.details.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }

        OrnateDivider()
        if (node.type == SkillNodeType.JEWEL_SOCKET) {
            SocketContents(s, node, allocated, enabled, onSocket, onUnsocket)
        } else {
            if (node.params.isEmpty()) Text(ui("tree.no_bonuses"), color = Muted)
            node.params.forEach { modifier ->
                ModifierLine(modifierDocument(modifier.modifierId, modifier.values), s.world.definitions)
            }
        }

        OrnateDivider()
        if (allocated) OutlinedButton(enabled = enabled && node.type != SkillNodeType.START, onClick = { onRefund(node.code) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("tree.refund"))
        } else Button(enabled = enabled, onClick = { onAllocate(node.code) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("tree.allocate"))
        }
        Text(when {
                node.type == SkillNodeType.START ->
                    ui("tree.start_note")
                allocated ->
                    ui("tree.refund_note")
                else ->
                    ui("tree.allocate_note")
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
    val hero = s.play.hero ?: return
    val inside = hero.jewels[node.code]

    if (inside != null) {
        val document = inventoryDocument(inside, s.world.inventoryBases[inside.equipmentId])
        ItemRow(document, definitions = s.world.definitions, enabled = false,
            note = if (allocated) ui("tree.socket_working") else ui("tree.socket_locked"),
            noteColor = if (allocated) Gold else LifeRed) { }
        OutlinedButton(enabled = enabled, onClick = { onUnsocket(inside.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("tree.jewel_out"))
        }
        return
    }

    val free = hero.inventory.filter { instance ->
        !instance.socketed && s.world.inventoryBases[instance.equipmentId]?.text("slot") == "JEWEL"
    }
    if (!allocated) {
        Text(ui("tree.socket_first"), color = Muted, style = MaterialTheme.typography.bodySmall)
        return
    }
    if (free.isEmpty()) {
        Text(ui("tree.no_jewels"), color = Muted, style = MaterialTheme.typography.bodySmall)
        return
    }
    Engraved(ui("tree.jewel_in"))
    free.forEach { instance ->
        val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
        ItemRow(document, definitions = s.world.definitions, enabled = enabled) { onSocket(instance.id, node.code) }
    }
}

@Composable private fun TreeConfirmations(
    s: ForgeState,
    allocate: String?, refund: String?, reset: Boolean,
    onClear: () -> Unit,
    onAllocate: (String) -> Unit, onRefund: (String) -> Unit, onReset: () -> Unit,
) {
    val nodeName = { code: String -> s.world.treeNodes.firstOrNull { it.code == code }?.title ?: code }
    val treeIcon: @Composable () -> Unit = { Icon(ForgeGlyphs.Constellation, null, tint = Rune, modifier = Modifier.size(40.dp)) }
    val available = s.play.hero?.tree?.available
    // What a refund is paid with: the orb, and how many of it the bag holds right now.
    val regret = s.orbOf(CurrencyOrb.ORB_OF_REGRET)
    val regretTitle = regret?.title(s.lang) ?: CurrencyOrb.ORB_OF_REGRET.title(s.lang)
    val regretLeft = regret?.let { s.bagAmount(it.id) }
    fun regretLines(spent: Int) = listOfNotNull(
        LedgerLine(ui("confirm.spend"), ui("confirm.minus", spent, regretTitle), Tone.SPEND),
        regretLeft?.takeIf { it >= spent }?.let { LedgerLine(ui("confirm.left"), ui("confirm.amount", it - spent, regretTitle)) },
    )
    fun shortage(spent: Int) = regretLeft?.takeIf { it < spent }?.let { ui("confirm.short", it) }

    allocate?.let { code ->
        val cost = s.world.treeNodes.firstOrNull { it.code == code }?.cost ?: 1
        ConfirmSheet(
            title = ui("tree.allocate_q"), subtitle = nodeName(code), icon = treeIcon,
            ledger = listOfNotNull(
                LedgerLine(ui("confirm.spend"), ui("confirm.minus_count", cost, points(cost)), Tone.SPEND),
                available?.takeIf { it >= cost }?.let { LedgerLine(ui("confirm.left"), ui("confirm.count", it - cost, points(it - cost))) },
                LedgerLine(ui("confirm.gain"), nodeName(code), Tone.GAIN),
            ),
            note = ui("tree.allocate_confirm"),
            confirm = ui("tree.allocate_do"), onDismiss = onClear) { onAllocate(code) }
    }
    refund?.let { code ->
        val cost = s.world.treeNodes.firstOrNull { it.code == code }?.cost ?: 1
        ConfirmSheet(
            title = ui("tree.refund_q"), subtitle = nodeName(code), icon = treeIcon,
            ledger = regretLines(1) + LedgerLine(ui("confirm.returns"), ui("confirm.plus_count", cost, points(cost)), Tone.GAIN),
            warning = shortage(1),
            confirm = ui("tree.refund_do"), onDismiss = onClear) { onRefund(code) }
    }
    if (reset) {
        // The start node is not given back, so it is not paid for — the count says what is.
        val returned = (s.play.hero?.tree?.nodes?.size ?: 1) - 1
        ConfirmSheet(
            title = ui("tree.reset_q"), subtitle = ui("confirm.count", returned, nodes(returned)), icon = treeIcon,
            ledger = regretLines(returned) + LedgerLine(ui("confirm.returns"), ui("confirm.plus_count", returned, nodes(returned)), Tone.GAIN),
            note = ui("tree.reset_confirm"), warning = shortage(returned), danger = true,
            confirm = ui("tree.reset_do"), onDismiss = onClear) { onReset() }
    }
}

/** Russian counts its nouns in three forms and English in two: see `plural`. */
private fun points(n: Int) = plural("tree.point", n)

private fun nodes(n: Int) = plural("tree.node", n)

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
