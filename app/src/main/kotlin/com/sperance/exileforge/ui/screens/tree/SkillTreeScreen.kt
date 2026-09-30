package com.sperance.exileforge.ui.screens.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.nodeDescription
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.nodeTypeTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TreeAllocation
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.rules.sheet.StatContribution
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable fun SkillTreeScreen(s: ForgeState, vm: ForgeViewModel) {
    // No scrolling column here: the map owns the height, and everything that used to sit under it
    // lives in the sheet. A pannable canvas inside a scroll fights the scroll for every drag.
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(ui("tree.title"),
            ui("tree.node_count", s.index?.content?.tree?.nodes?.size ?: 0), ForgeGlyphs.Constellation, guide = Guide.TREE)
        SkillTreePanel(s, vm::selectNode, vm::allocateNode, vm::refundNode, vm::resetTree, vm::nodeQuery, onPath = vm::allocatePath,
            onSocket = vm::socketJewel, onUnsocket = vm::unsocketJewel, onRechoose = vm::rechooseNode, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * The passive tree.
 *
 * The graph is the content's: node positions, edges, costs and bonuses all arrive with the rules, and
 * which node may be taken next is decided by `allocate` on the server rather than guessed at here. The
 * panel draws what it was given and sends one node code at a time.
 *
 * The map takes the whole panel and everything else — the point balance, the search, the chosen
 * node and its two commands — opens as a sheet over it. A hundred and twenty nodes need the room,
 * and the details are read one node at a time rather than alongside.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SkillTreePanel(s: ForgeState, onSelect: (String) -> Unit, onAllocate: (String, Int?) -> Unit,
    onRefund: (String) -> Unit, onReset: () -> Unit, onQuery: (String) -> Unit = {},
    onSocket: (String, String) -> Unit = { _, _ -> }, onUnsocket: (String) -> Unit = {},
    onRechoose: (String, Int) -> Unit = { _, _ -> }, onPath: (String, Int?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier) {
    val hero = s.hero
    val index = s.index
    val tree = s.treeState
    var detailsOpen by remember { mutableStateOf(false) }
    var nodeOpen by remember { mutableStateOf(false) }
    // The node's own window is the question (2.72.0): taking or giving back a node acts at once from
    // it, the price written beside the button. Only the whole tree's reset is still asked twice.
    var confirmReset by remember { mutableStateOf(false) }
    if (hero == null) {
        InfoCard(ui("tree.no_hero"),
            ui("tree.no_hero_hint"))
        return
    }
    if (index == null || tree == null || index.content.tree.nodes.isEmpty()) {
        InfoCard(ui("tree.not_loaded"),
            ui("tree.not_loaded_hint"))
        return
    }
    val nodes = index.content.tree.nodes
    val taken = hero.takenNodes
    val enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    // Which nodes are one step away: neighbours of what is taken, or the class's own start when
    // nothing is taken yet. The server still decides — this only says where to look on 122 nodes.
    val heroClass = s.heroClass
    val reachable = remember(index, heroClass, taken) { reachableFrom(index, heroClass, taken) }
    // The way to a far node (3.39.0): the rules' shortest path from what is taken, drawn dashed and taken at once.
    val selected = s.play.selectedNode
    val path = remember(index, heroClass, taken, selected) {
        heroClass?.startNode?.takeIf { taken.isNotEmpty() && selected !in reachable }?.let { TreeAllocation.path(index.tree, taken, it, selected) }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("tree.points", tree.available, tree.total),
                color = Gold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            ForgeTextButton(onClick = { detailsOpen = true }) { Text(ui("tree.details")) }
        }
        // A tap opens a small window about that one node, so the map stays in sight; everything
        // about the tree as a whole lives behind "Подробно".
        TreeCanvas(nodes, selected, taken, reachable, path.orEmpty(), Modifier.weight(1f)) { code -> onSelect(code); nodeOpen = true }
        MutedText(ui("tree.gesture_hint"))
    }
    // The small window about the chosen node: what it gives, and the one command over it. It is
    // deliberately not expanded to full height — half the point is seeing where the branch leads.
    if (nodeOpen) ModalBottomSheet(onDismissRequest = { nodeOpen = false }, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NodeDetails(s, index, index.tree.node(selected), taken, reachable, enabled, path, tree.available,
                onAllocate = { code, choice -> nodeOpen = false; onAllocate(code, choice) },
                onPath = { code, choice -> nodeOpen = false; onPath(code, choice) },
                onRefund = { nodeOpen = false; onRefund(it) },
                onRechoose = { code, choice -> nodeOpen = false; onRechoose(code, choice) },
                onSocket = { instance, code -> nodeOpen = false; onSocket(instance, code) },
                onUnsocket = { nodeOpen = false; onUnsocket(it) })
            Spacer(Modifier.height(8.dp))
        }
    }

    TreeConfirmations(s, confirmReset, onClear = { confirmReset = false }, onReset = onReset)

    if (detailsOpen) ModalBottomSheet(onDismissRequest = { detailsOpen = false }, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ForgePanel {
                    Engraved(hero.info.name)
                    PropertyRow(ui("tree.points_total"), tree.total.toString(), Glyph.LEVEL)
                    PropertyRow(ui("tree.points_spent"), tree.spent.toString(), Glyph.LEVEL)
                    PropertyRow(ui("tree.points_available"), tree.available.toString(), Glyph.LEVEL)
                }
            }
            item {
                ForgePanel(accent = Rune) {
                    Engraved(ui("tree.totals_title"), Rune)
                    if (tree.totals.isEmpty()) Text(ui("tree.totals_empty"), color = Muted)
                    // The rules sum this: two INCREASED add up while two MORE multiply, so
                    // adding the snapshots here would lie exactly where a player is choosing.
                    // It is the tree's contribution, not the character's total — which is why a
                    // percentage stays a percentage and is written with its sign.
                    tree.totals.forEach { total ->
                        PropertyRow(statTitle(total.stat, s.lang), contributionText(total), stat = total.stat)
                    }
                }
            }
            item { TreeSearch(s, nodes, onQuery) { code -> onSelect(code); detailsOpen = false; nodeOpen = true } }
            item {
                ForgeOutlinedButton(enabled = enabled && hero.tree.size > 1, onClick = { detailsOpen = false; confirmReset = true },
                    modifier = Modifier.fillMaxWidth()) { Text(ui("tree.reset_all")) }
                MutedText(ui("tree.reset_note"))
            }
        }
    }
}

/**
 * Which nodes are one step away: the class's own start while nothing is taken, otherwise every node
 * not yet taken that the rules call adjacent — a taken mastery opens no neighbours, and a node kept for
 * another class (the Scion's branches, 3.19.0) is never one.
 */
private fun reachableFrom(index: ContentIndex, heroClass: HeroClass?, taken: Set<String>): Set<String> =
    if (taken.isEmpty()) setOfNotNull(heroClass?.startNode)
    else index.content.tree.nodes.mapNotNullTo(HashSet()) { node ->
        node.code.takeIf { it !in taken && (heroClass == null || node.openTo(heroClass.startNode)) && index.tree.isAdjacentTo(it, taken) }
    }

/**
 * Finding a node by name.
 *
 * The tree is over a hundred nodes across seven class areas, so panning to one by eye is no longer
 * realistic. A match selects the node, which is what the map draws a ring around.
 */
@Composable private fun TreeSearch(s: ForgeState, nodes: List<TreeNode>, onQuery: (String) -> Unit, onSelect: (String) -> Unit) {
    val matches = remember(nodes, s.play.nodeQuery, s.lang) {
        val query = s.play.nodeQuery.trim()
        if (query.isBlank()) emptyList()
        else nodes.filter { nodeTitle(it.code).contains(query, true) || it.code.contains(query, true) }.take(8)
    }
    ForgePanel {
        Engraved(ui("tree.find_node"))
        OutlinedTextField(s.play.nodeQuery, onQuery, label = { Text(ui("tree.node_name")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (s.play.nodeQuery.isNotBlank() && matches.isEmpty()) Text(ui("tree.nothing_found"), color = Muted)
        matches.forEach { node ->
            ForgeTextButton(onClick = { onSelect(node.code) }, modifier = Modifier.fillMaxWidth()) {
                Text("${nodeTitle(node.code)} · ${nodeTypeTitle(node.type, s.lang)}",
                    color = nodeColour(node, node.code == s.play.selectedNode))
            }
        }
    }
}

/**
 * The graph, drawn from the coordinates the content seeded.
 *
 * Panning and zooming are the only interaction beyond a tap: the layout is fixed data, so the
 * canvas never moves a node, it only chooses where to look. It clips, because a canvas does not:
 * without that the outer nodes are painted over whatever the map happens to be sitting on. And the
 * pan is held to the tree's own half-extent, so a stray flick cannot drag the whole graph away and
 * leave an empty rectangle with no way back but the reset.
 */
@Composable private fun TreeCanvas(nodes: List<TreeNode>, selected: String, taken: Set<String>, reachable: Set<String>, path: List<String>,
    modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val byCode = remember(nodes) { nodes.associateBy { it.code } }
    val bounds = remember(nodes) { Bounds.of(nodes) }
    var scale by remember { mutableFloatStateOf(1f) }
    val labels = rememberTextMeasurer()
    var pan by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxSize().clipToBounds()
            .pointerInput(nodes) {
                detectTransformGestures { _, drag, zoom, _ ->
                    scale = (scale * zoom).coerceIn(.4f, MAX_ZOOM)
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
            wheel(nodes, bounds, width, height, scale, pan, labels)
            // Edges first, so a node always sits on top of the lines that reach it.
            nodes.forEach { node ->
                val from = place(node, bounds, width, height, scale, pan)
                node.connections.forEach { code ->
                    val other = byCode[code] ?: return@forEach
                    val both = node.code in taken && code in taken
                    val open = (node.code in taken && code in reachable) || (code in taken && node.code in reachable)
                    drawLine(when { both -> Gold; open -> Gold.copy(alpha = .35f); else -> Bronze },
                        from, place(other, bounds, width, height, scale, pan), (if (both) 3f else 2f) * scale.coerceIn(.6f, 1.6f))
                }
            }
            if (path.isNotEmpty()) {
                // The dashed way to the chosen node: from the taken node it leaves, through every step it would take.
                val anchor = byCode[path.first()]?.connections.orEmpty().plus(nodes.filter { path.first() in it.connections }.map { it.code })
                    .firstOrNull { it in taken && byCode[it]?.type != SkillNodeType.MASTERY }
                val points = (listOfNotNull(anchor) + path).mapNotNull { byCode[it] }.map { place(it, bounds, width, height, scale, pan) }
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 7f).map { it * scale.coerceIn(.6f, 1.6f) }.toFloatArray())
                points.zipWithNext { a, b -> drawLine(GoldBright, a, b, 3f * scale.coerceIn(.6f, 1.6f), pathEffect = dash) }
            }
            nodes.forEach { node -> medallion(node, place(node, bounds, width, height, scale, pan), scale, node.code in taken,
                node.code in reachable || node.code in path, node.code == selected) }
            // Names at zoom (3.39.0): a notable's and a keystone's title under it, once the map is close enough to read.
            if (scale >= LABEL_ZOOM) nodes.filter { it.type == SkillNodeType.NOTABLE || it.type == SkillNodeType.KEYSTONE }.forEach { node ->
                val at = place(node, bounds, width, height, scale, pan)
                if (at.x !in -80f..width + 80f || at.y !in -40f..height + 40f) return@forEach
                val layout = labels.measure(nodeTitle(node.code), TextStyle(color = Parchment.copy(alpha = .85f), fontSize = 10.sp))
                drawText(layout, topLeft = at + Offset(-layout.size.width / 2f, radius(node) * scale.coerceIn(.5f, 2.2f) + 3f))
            }
        }
        Row(Modifier.align(Alignment.BottomStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ForgeTextButton(onClick = { scale = 1f; pan = Offset.Zero }) { Text(ui("tree.reset_view")) }
            MutedText(ui("tree.taken_reachable", taken.size, reachable.size), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The chosen node: what it gives, and the one command the server accepts for it.
 *
 * A socket is the exception, because it gives nothing by itself — what it holds is a jewel, and
 * the command there is to put one in or take it out.
 */
@Composable private fun NodeDetails(s: ForgeState, index: ContentIndex, node: TreeNode?, taken: Set<String>, reachable: Set<String>, enabled: Boolean,
    path: List<String>?, available: Int, onAllocate: (String, Int?) -> Unit, onPath: (String, Int?) -> Unit, onRefund: (String) -> Unit,
    onSocket: (String, String) -> Unit = { _, _ -> }, onUnsocket: (String) -> Unit = {}, onRechoose: (String, Int) -> Unit = { _, _ -> }) {
    if (node == null) {
        InfoCard(ui("tree.no_selection"), ui("tree.no_selection_hint"))
        return
    }
    val allocated = node.code in taken
    val choosing = node.options.isNotEmpty()
    // The option the hero took is theirs: it is read from the snapshot, not from the tree.
    val chosen = s.hero?.tree?.firstOrNull { it.code == node.code }?.choice
    var picked by remember(node.code) { mutableStateOf<Int?>(null) }
    ForgePanel(accent = nodeColour(node, true)) {
        // The node's name is this panel's title, so it keeps its own casing rather than being
        // shouted as an Engraved caption the way a section heading is.
        Text(nodeTitle(node.code), color = nodeColour(node, true), style = MaterialTheme.typography.titleMedium)
        PropertyRow(ui("tree.node_type"), nodeTypeTitle(node.type, s.lang), Glyph.TREE)
        PropertyRow(ui("tree.cost"), node.cost.toString(), Glyph.LEVEL)
        PropertyRow(ui("card.state"), if (allocated) ui("tree.taken") else ui("tree.not_taken"), Glyph.TREE)
        nodeDescription(node.code).takeIf { it.isNotBlank() }?.let { MutedText(it) }

        OrnateDivider()
        if (node.type == SkillNodeType.JEWEL_SOCKET) {
            SocketContents(s, index, node, allocated, enabled, onSocket, onUnsocket)
        } else if (choosing) {
            // A mastery or an attribute node (server 0.52.0): one option, chosen when it is taken. A taken
            // attribute node may change it for a Chaos Orb (2.72.0, server 0.63.0); a mastery may not.
            val rechoosable = allocated && node.type == SkillNodeType.ATTRIBUTE
            MutedText(ui(when { rechoosable -> "tree.option_rechoose"; allocated -> "tree.option_chosen"; else -> "tree.option_pick" }),
                style = MaterialTheme.typography.labelMedium)
            node.options.forEachIndexed { at, option ->
                val on = when { picked != null -> at == picked; allocated -> at == chosen; else -> false }
                OptionCard(on, enabled = (!allocated && (node.code in reachable || path != null)) || (rechoosable && enabled), onClick = { picked = at }) {
                    option.forEach { line -> ModifierLine(index, line) }
                }
            }
            if (rechoosable) {
                val owned = s.bagAmount(Orb.CHAOS_ORB.name) ?: 0L
                ForgeButton(enabled = enabled && picked != null && picked != chosen && owned > 0, onClick = { picked?.let { onRechoose(node.code, it) } },
                    modifier = Modifier.fillMaxWidth()) {
                    OrbGlyph(Orb.CHAOS_ORB, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ui("tree.rechoose", owned))
                }
            }
            if (node.type == SkillNodeType.MASTERY && !allocated && node.code !in reachable)
                MutedText(ui("tree.mastery_locked"))
        } else {
            if (node.lines.isEmpty()) Text(ui("tree.no_bonuses"), color = Muted)
            node.lines.forEach { line -> ModifierLine(index, line) }
        }

        OrnateDivider()
        if (allocated) ForgeOutlinedButton(enabled = enabled && node.type != SkillNodeType.START, onClick = { onRefund(node.code) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("tree.refund"))
        } else if (path != null) {
            // A far node (3.39.0): the whole way at once, for the sum of its steps; short of points, the button says so.
            val cost = path.sumOf { index.tree.node(it)?.cost ?: 0 }
            PropertyRow(ui("tree.path"), ui("tree.path_value", path.size, cost), Glyph.TREE)
            ForgeButton(enabled = enabled && cost <= available && (!choosing || picked != null), onClick = { onPath(node.code, picked) },
                modifier = Modifier.fillMaxWidth()) { Text(ui("tree.path_take", cost)) }
            if (cost > available) MutedText(ui("tree.path_short", cost, available))
        } else if (node.code in reachable || taken.isEmpty()) ForgeButton(enabled = enabled && (!choosing || picked != null),
            onClick = { onAllocate(node.code, picked) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("tree.allocate"))
        } else MutedText(ui("tree.path_none"))
        if (node.type == SkillNodeType.START) Text(ui("tree.start_note"), color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * What sits in a socket, and what can be put there.
 *
 * A jewel is an ordinary item, so the stash is where the candidates come from: every jewel the hero
 * owns that is neither worn nor already in another socket. Whether this one may go in is still the
 * server's call — the socket has to be taken and free — so the list offers and the refusal explains.
 */
@Composable private fun SocketContents(s: ForgeState, index: ContentIndex, node: TreeNode, allocated: Boolean, enabled: Boolean,
    onSocket: (String, String) -> Unit, onUnsocket: (String) -> Unit) {
    val hero = s.hero ?: return
    val inside = hero.jewels[node.code]

    if (inside != null) {
        // A taken socket may still hold a jewel that does nothing - a second copy of a unique one (1.31.0): the sheet's reason.
        val idle = hero.inactive[inside.id]?.joinToString("\n") { requirementReason(it, s.lang) }
        s.view(inside)?.let { jewel ->
            ItemRow(jewel, enabled = false,
                note = when { !allocated -> ui("tree.socket_locked"); idle != null -> idle; else -> ui("tree.socket_working") },
                noteColor = if (allocated && idle == null) Gold else LifeRed) { }
        }
        ForgeOutlinedButton(enabled = enabled, onClick = { onUnsocket(inside.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("tree.jewel_out"))
        }
        return
    }

    val free = hero.items.filter { instance ->
        !instance.equipped && !instance.socketed && index.template(instance.template)?.slot == Slot.JEWEL
    }
    if (!allocated) {
        MutedText(ui("tree.socket_first"))
        return
    }
    if (free.isEmpty()) {
        MutedText(ui("tree.no_jewels"))
        return
    }
    Engraved(ui("tree.jewel_in"))
    free.forEach { instance ->
        // A unique jewel already in another socket (server 1.31.0): one of its kind per hero, so this copy waits, and says why.
        val single = hero.jewelFree(index, instance)
        s.view(instance)?.let { jewel ->
            ItemRow(jewel, enabled = enabled && single, price = s.sellPrice(instance),
                note = if (single) null else ui("tree.jewel_unique_taken"), noteColor = LifeRed) { onSocket(instance.id, node.code) }
        }
    }
}

@Composable private fun TreeConfirmations(s: ForgeState, reset: Boolean, onClear: () -> Unit, onReset: () -> Unit) {
    val treeIcon: @Composable () -> Unit = { Icon(ForgeGlyphs.Constellation, null, tint = Rune, modifier = Modifier.size(40.dp)) }
    // What a reset is paid with: the orb, named by its code, and how many of it the bag holds right now.
    val regretTitle = itemTitle(Orb.ORB_OF_REGRET.name)
    val regretLeft = s.bagAmount(Orb.ORB_OF_REGRET.name)
    fun regretLines(spent: Int) = listOfNotNull(
        LedgerLine(ui("confirm.spend"), ui("confirm.minus", spent, regretTitle), Tone.SPEND),
        regretLeft?.takeIf { it >= spent }?.let { LedgerLine(ui("confirm.left"), ui("confirm.amount", it - spent, regretTitle)) },
    )
    fun shortage(spent: Int) = regretLeft?.takeIf { it < spent }?.let { ui("confirm.short", it) }

    if (reset) {
        // The start node is not given back, so it is not paid for — the count says what is.
        val returned = (s.hero?.tree?.size ?: 1) - 1
        ConfirmSheet(
            title = ui("tree.reset_q"), subtitle = ui("confirm.count", returned, nodes(returned)), icon = treeIcon,
            ledger = regretLines(returned) + LedgerLine(ui("confirm.returns"), ui("confirm.plus_count", returned, nodes(returned)), Tone.GAIN),
            note = ui("tree.reset_confirm"), warning = shortage(returned), blocked = shortage(returned) != null, danger = true,
            confirm = ui("tree.reset_do"), onDismiss = onClear) { onReset() }
    }
}

/** Russian counts its nouns in three forms and English in two: see `plural`. */

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
    return when (total.op) {
        Op.INCREASED, Op.MORE -> "$signed%"
        // SET replaces the base outright, so it is not an addition and carries no sign.
        Op.SET -> number
        Op.ADD -> signed + if (statPercent(total.stat)) "%" else ""
    }
}

private fun nodeColour(node: TreeNode, selected: Boolean): Color = when {
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

private fun radius(node: TreeNode): Float = when (node.type) {
    SkillNodeType.KEYSTONE -> 13f; SkillNodeType.NOTABLE -> 10f; SkillNodeType.START -> 12f
    SkillNodeType.JEWEL_SOCKET -> 11f; SkillNodeType.SMALL -> 6f
    SkillNodeType.MASTERY -> 10f; SkillNodeType.ATTRIBUTE -> 7f
}

/** Extent of the seeded coordinates, so the whole tree fits whatever canvas it is given. */
private data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    val spanX get() = max(1f, maxX - minX)
    val spanY get() = max(1f, maxY - minY)
    companion object {
        fun of(nodes: List<TreeNode>) = Bounds(
            nodes.minOfOrNull { it.x.toFloat() } ?: 0f, nodes.maxOfOrNull { it.x.toFloat() } ?: 1f,
            nodes.minOfOrNull { it.y.toFloat() } ?: 0f, nodes.maxOfOrNull { it.y.toFloat() } ?: 1f)
    }
}

private const val MARGIN = 28f
private const val MAX_ZOOM = 10f
/** From this zoom a notable's and a keystone's name is written under it. */
private const val LABEL_ZOOM = 2.6f

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

private fun place(node: TreeNode, bounds: Bounds, width: Float, height: Float, scale: Float, pan: Offset): Offset {
    val fit = fitFactor(bounds, width, height)
    val x = (node.x - bounds.minX) * fit - bounds.spanX * fit / 2
    val y = (node.y - bounds.minY) * fit - bounds.spanY * fit / 2
    return Offset(width / 2 + x * scale + pan.x, height / 2 + y * scale + pan.y)
}

/** A mastery's or an attribute node's option: a framed card, gold when it is the one. */
@Composable private fun OptionCard(on: Boolean, enabled: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(if (on) Gold.copy(alpha = .12f) else Abyss)
        .border(if (on) 1.5.dp else 1.dp, if (on) Gold else Bronze.copy(alpha = .5f), shape)
        .clickable(enabled = enabled, onClick = onClick).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
}

/** Each class's colour on the wheel, by the attributes its start node is named after. */
private fun classTint(code: String): Color = when (code.removeSuffix("_START")) {
    "STR" -> Color(0xFFC0504D); "DEX" -> Vital; "INT" -> Rune
    "STR_DEX" -> Color(0xFFD08A4E); "STR_INT" -> Elder; "DEX_INT" -> ShieldCyan
    else -> Gold
}

/**
 * The wheel under the graph (2.59.0, the owner's pick «Колесо PoE»): a stone disc with its rings,
 * each class's sector washed in its colour and named at the rim. The sectors are read off the start
 * nodes' positions, so the wheel turns with whatever tree the content seeds.
 */
private fun DrawScope.wheel(nodes: List<TreeNode>, bounds: Bounds, width: Float, height: Float, scale: Float, pan: Offset,
    labels: TextMeasurer) {
    drawRect(Color(0xFF0B0D11))
    val fit = fitFactor(bounds, width, height) * scale
    val centre = Offset(width / 2 + (-bounds.minX - bounds.spanX / 2) * fit + pan.x, height / 2 + (-bounds.minY - bounds.spanY / 2) * fit + pan.y)
    val rim = max(bounds.spanX, bounds.spanY) / 2 * fit
    drawCircle(Brush.radialGradient(listOf(Color(0xFF1A1E25), Color(0xFF0E1015)), centre, rim * 1.05f), rim * 1.05f, centre)
    for (ring in 1..8) drawCircle(Bronze.copy(alpha = if (ring % 3 == 0) .2f else .08f), rim * ring / 8f, centre, style = Stroke(1f))
    nodes.filter { it.type == SkillNodeType.START && it.code != "SCION_START" }.forEach { start ->
        val angle = Math.toDegrees(atan2(start.y.toDouble(), start.x.toDouble())).toFloat()
        val tint = classTint(start.code)
        drawArc(tint.copy(alpha = .06f), angle - 30f, 60f, true, centre - Offset(rim, rim), Size(rim * 2, rim * 2))
        if (scale > .55f) {
            val layout = labels.measure(nodeTitle(start.code), TextStyle(color = tint.copy(alpha = .85f), fontSize = (11 * scale.coerceAtMost(1.6f)).sp))
            val at = centre + Offset(cos(Math.toRadians(angle.toDouble())).toFloat(), sin(Math.toRadians(angle.toDouble())).toFloat()) * rim * 1.0f
            drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
    }
}

/**
 * One node as a medallion of its kind: a small disc, a notable's double ring, a keystone's
 * hexagon, a mastery's violet diamond, an attribute's green triangle, a socket's hollow ring and a
 * start's octagon in its class's colour. Taken is gold and glows, one step away is ringed in dull
 * gold, the rest is stone.
 */
private fun DrawScope.medallion(node: TreeNode, centre: Offset, scale: Float, taken: Boolean, next: Boolean, selected: Boolean) {
    val r = radius(node) * scale.coerceIn(.5f, 2.2f)
    val edge = when { taken -> Gold; next -> Gold.copy(alpha = .55f); else -> Bronze }
    val width = (if (node.type == SkillNodeType.SMALL) 1.5f else 2.2f) * scale.coerceIn(.6f, 1.6f)
    if (taken && node.type != SkillNodeType.SMALL)
        drawCircle(Brush.radialGradient(listOf(nodeColour(node, true).copy(alpha = .35f), Color.Transparent), centre, r * 2.6f), r * 2.6f, centre)
    fun polygon(sides: Int, radius: Float, turn: Float) = Path().apply {
        repeat(sides) { i ->
            val a = turn + i * 2 * PI.toFloat() / sides
            val p = centre + Offset(cos(a), sin(a)) * radius
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    val stone = Color(0xFF161A21)
    when (node.type) {
        SkillNodeType.START -> {
            val tint = classTint(node.code)
            val shape = polygon(8, r, PI.toFloat() / 8)
            drawPath(shape, if (taken) tint.copy(alpha = .55f) else stone); drawPath(shape, tint, style = Stroke(width))
        }
        SkillNodeType.KEYSTONE -> {
            val shape = polygon(6, r, 0f)
            drawPath(shape, if (taken) LifeRed.copy(alpha = .5f) else Color(0xFF141820)); drawPath(shape, if (taken) Gold else edge, style = Stroke(width * 1.2f))
            drawPath(polygon(6, r * .55f, PI.toFloat() / 6), edge, style = Stroke(width))
        }
        SkillNodeType.NOTABLE -> {
            drawCircle(if (taken) Gold.copy(alpha = .6f) else stone, r, centre); drawCircle(edge, r, centre, style = Stroke(width))
            drawCircle(edge, r * .55f, centre, style = Stroke(width))
        }
        SkillNodeType.MASTERY -> {
            val shape = polygon(4, r, 0f)
            drawPath(shape, if (taken) Elder.copy(alpha = .6f) else Color(0xFF15131C))
            drawPath(shape, Elder.copy(alpha = if (taken) 1f else if (next) .9f else .35f), style = Stroke(width))
        }
        SkillNodeType.ATTRIBUTE -> {
            val shape = polygon(3, r, -PI.toFloat() / 2)
            drawPath(shape, if (taken) Vital.copy(alpha = .7f) else stone); drawPath(shape, if (taken) Vital else edge, style = Stroke(width))
        }
        SkillNodeType.JEWEL_SOCKET -> {
            drawCircle(edge, r, centre, style = Stroke(width * 1.2f))
            drawCircle(if (taken) ShieldCyan.copy(alpha = .5f) else Color(0xFF0B0E13), r * .6f, centre)
        }
        SkillNodeType.SMALL -> { drawCircle(if (taken) Gold else Color(0xFF1A1F27), r, centre); drawCircle(edge, r, centre, style = Stroke(width)) }
    }
    if (selected) drawCircle(GoldBright, r + 5.dp.toPx(), centre, style = Stroke(2.dp.toPx()))
}
