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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.nodeDescription
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.nodeTypeTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
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

@Composable fun SkillTreeScreen() {
    val game by koinViewModel<TreeViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<TreeViewModel>()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    // «Карта на весь экран» (variant A): no header and no scrolling column — the map owns everything between the Hero
    // strip and the bar, and the rest floats over it. A pannable canvas inside a scroll fights the scroll for every drag.
    SkillTreePanel(
        game, selected, query, vm::select, vm::allocate, vm::refund, vm::reset, vm::query, onPath = vm::allocatePath,
        onSocket = vm::socket, onUnsocket = vm::unsocket, onRechoose = vm::rechoose, onRefundBranch = vm::refundBranch,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * The passive tree.
 *
 * The graph is the content's: node positions, edges, costs and bonuses all arrive with the rules, and
 * which node may be taken next is decided by `allocate` on the server rather than guessed at here. The
 * panel draws what it was given and sends one node code at a time.
 *
 * The map takes the whole panel. The point balance is a plaque in its corner; the search, «Итого» and «Ещё»
 * (the details, the reset) are round buttons down its right edge, the way back to the whole tree under
 * them; a chosen node rises as a card over the map's foot rather than a sheet, so the branch stays in sight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreePanel(
    game: GameUi,
    selected: String,
    rawQuery: String,
    onSelect: (String) -> Unit,
    onAllocate: (String, Int?) -> Unit,
    onRefund: (String) -> Unit,
    onReset: () -> Unit,
    onQuery: (String) -> Unit = {},
    onSocket: (String, String) -> Unit = { _, _ -> },
    onUnsocket: (String) -> Unit = {},
    onRechoose: (String, Int) -> Unit = { _, _ -> },
    onPath: (String, Int?) -> Unit = { _, _ -> },
    onRefundBranch: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hero = game.hero
    val index = game.index
    val tree = game.treeState
    var detailsOpen by remember { mutableStateOf(false) }
    // «Итого» (3.54.0): the tree's bonuses at once, without the rest of the details.
    var totalsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var nodeOpen by remember { mutableStateOf(false) }
    // The node's own card is the question (2.72.0): taking or giving back a node acts at once from
    // it, the price written beside the button. Only the whole tree's reset is still asked twice.
    var confirmReset by remember { mutableStateOf(false) }
    val view = remember { TreeView() }
    if (hero == null) {
        Box(modifier.padding(16.dp)) { InfoCard(ui("tree.no_hero"), ui("tree.no_hero_hint")) }
        return
    }
    if (index == null || tree == null || index.content.tree.nodes.isEmpty()) {
        Box(modifier.padding(16.dp)) { InfoCard(ui("tree.not_loaded"), ui("tree.not_loaded_hint")) }
        return
    }
    val nodes = index.content.tree.nodes
    val taken = hero.takenNodes
    val enabled = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    // Which nodes are one step away: neighbours of what is taken, or the class's own start when
    // nothing is taken yet. The server still decides — this only says where to look on 122 nodes.
    val heroClass = game.heroClass
    val reachable = remember(index, heroClass, taken) { reachableFrom(index, heroClass, taken) }
    // The way to a far node (3.39.0): the rules' shortest path from what is taken, drawn dashed and taken at once.
    val path = remember(index, heroClass, taken, selected) {
        heroClass?.startNode?.takeIf { taken.isNotEmpty() && selected !in reachable }?.let { TreeAllocation.path(index.tree, taken, it, selected) }
    }
    // The tag filter (3.47.0): every node whose lines carry the tag lights up.
    var tag by remember { mutableStateOf<String?>(null) }
    // The search (3.54.0): by a node's name or the stats it gives; every match lights up with the tag's.
    val query = rawQuery.trim()
    val found = remember(index, query, game.lang) { if (query.length < 2) emptySet() else nodesMatching(index, query) }
    val highlight = remember(index, tag, found) { tag?.let { nodesTagged(index, it) }.orEmpty() + found }
    // Both live behind the search button, so the map keeps the height; while either is on, the button
    // turns green and wears the number of nodes lit.
    var filtersOpen by remember { mutableStateOf(false) }
    val filtering = tag != null || query.length >= 2
    BackHandler(nodeOpen) { nodeOpen = false }
    Box(modifier) {
        TreeCanvas(nodes, selected, taken, reachable, path.orEmpty(), highlight, view, Modifier.fillMaxSize(), focus = heroClass?.startNode) { code ->
            onSelect(code)
            nodeOpen = true
        }
        Text(
            ui("tree.points", tree.available, tree.total),
            color = Gold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Abyss.copy(alpha = .85f), PILL)
                .border(1.dp, Bronze, PILL).padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Column(
            Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapButton(
                Icons.Outlined.Search,
                ui("tree.search_filters"),
                tint = if (filtering) Vital else Gold,
                badge = highlight.size.takeIf { filtering },
            ) { filtersOpen = true }
            MapButton(Icons.Outlined.Functions, ui("tree.totals_button")) { totalsOpen = true }
            Box {
                MapButton(Icons.Outlined.MoreVert, ui("common.more")) { menuOpen = true }
                DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }, containerColor = PanelRaised) {
                    DropdownMenuItem(text = { Text(ui("tree.details")) }, onClick = {
                        menuOpen = false
                        detailsOpen = true
                    })
                    DropdownMenuItem(
                        text = { Text(ui("tree.reset_all"), color = if (enabled && hero.tree.size > 1) LifeRed else Muted) },
                        enabled = enabled && hero.tree.size > 1,
                        onClick = {
                            menuOpen = false
                            confirmReset = true
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            MapButton(ForgeGlyphs.Target, ui("tree.reset_view"), tint = Muted, size = 34.dp, onClick = view::reset)
        }
        // The card about the chosen node: what it gives, and the commands over it. It holds the map's foot only, so
        // half the point — seeing where the branch leads — is kept.
        if (nodeOpen) {
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().heightIn(max = 380.dp)
                    .depthPanel(shape).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NodeDetails(
                    game, index, heroClass, index.tree.node(selected), taken, reachable, enabled, path, tree.available,
                    onClose = { nodeOpen = false },
                    onAllocate = { code, choice ->
                        nodeOpen = false
                        onAllocate(code, choice)
                    },
                    onPath = { code, choice ->
                        nodeOpen = false
                        onPath(code, choice)
                    },
                    onRefund = {
                        nodeOpen = false
                        onRefund(it)
                    },
                    onRefundBranch = {
                        nodeOpen = false
                        onRefundBranch(it)
                    },
                    onRechoose = { code, choice ->
                        nodeOpen = false
                        onRechoose(code, choice)
                    },
                    onSocket = { instance, code ->
                        nodeOpen = false
                        onSocket(instance, code)
                    },
                    onUnsocket = {
                        nodeOpen = false
                        onUnsocket(it)
                    },
                )
            }
        }
    }

    if (filtersOpen) {
        ForgeSheet(onDismissRequest = { filtersOpen = false }) {
            TreeFilters(rawQuery, onQuery, found.size.takeIf { query.length >= 2 }, tag, onTag = { tag = it })
        }
    }

    TreeConfirmations(game, confirmReset, onClear = { confirmReset = false }, onReset = onReset)

    if (totalsOpen) {
        ForgeSheet(onDismissRequest = { totalsOpen = false }) {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(16.dp)) { TreeTotals(game, tree) }
        }
    }

    if (detailsOpen) {
        ForgeSheet(onDismissRequest = { detailsOpen = false }) {
            LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    ForgePanel {
                        Engraved(hero.info.name)
                        PropertyRow(ui("tree.points_total"), tree.total.toString(), Glyph.LEVEL)
                        PropertyRow(ui("tree.points_spent"), tree.spent.toString(), Glyph.LEVEL)
                        PropertyRow(ui("tree.points_available"), tree.available.toString(), Glyph.LEVEL)
                        PropertyRow(ui("tree.taken_reachable_label"), ui("tree.taken_reachable", taken.size, reachable.size), Glyph.TREE)
                    }
                }
                item { TreeTotals(game, tree) }
                item {
                    TreeSearch(game, rawQuery, selected, nodes) { code ->
                        onSelect(code)
                        detailsOpen = false
                        nodeOpen = true
                    }
                }
            }
        }
    }
}

/** Where the map looks: the zoom and the pan, held by the panel so its «back to the whole tree» button can reset them. */
@Stable internal class TreeView {
    var scale by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)

    /** The first look on the class's start was taken; a «back to the whole tree» keeps it from coming back. */
    var focused = false
    fun reset() {
        scale = 1f
        pan = Offset.Zero
    }
}

internal val PILL = RoundedCornerShape(16.dp)

/** A round button floating over the map, with a count in its corner when [badge] is given. */
@Composable internal fun MapButton(icon: ImageVector, label: String, tint: Color = Gold, badge: Int? = null, size: Dp = 40.dp, onClick: () -> Unit) {
    BadgedBox(badge = { badge?.let { Badge(containerColor = Vital, contentColor = Ink) { Text(it.toString(), fontSize = 9.sp) } } }) {
        IconButton(onClick = onClick, modifier = Modifier.size(size).background(Panel.copy(alpha = .9f), CircleShape).border(1.dp, Bronze, CircleShape)) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Which nodes are one step away: the class's own start while nothing is taken, otherwise every node
 * not yet taken that the rules call adjacent — a taken mastery opens no neighbours, and a node kept for
 * another class (the Scion's branches, 3.19.0) is never one.
 */
internal fun reachableFrom(index: ContentIndex, heroClass: HeroClass?, taken: Set<String>): Set<String> = if (taken.isEmpty()) {
    setOfNotNull(heroClass?.startNode)
} else {
    index.content.tree.nodes.mapNotNullTo(HashSet()) { node ->
        node.code.takeIf { it !in taken && (heroClass == null || node.openTo(heroClass.startNode)) && index.tree.isAdjacentTo(it, taken) }
    }
}

@Composable internal fun TreeConfirmations(game: GameUi, reset: Boolean, onClear: () -> Unit, onReset: () -> Unit) {
    val treeIcon: @Composable () -> Unit = { Icon(ForgeGlyphs.Constellation, null, tint = Rune, modifier = Modifier.size(40.dp)) }
    // What a reset is paid with: the orb, named by its code, and how many of it the bag holds right now.
    val regretTitle = itemTitle(Orb.ORB_OF_REGRET.name)
    val regretLeft = game.bagAmount(Orb.ORB_OF_REGRET.name)
    fun regretLines(spent: Int) = listOfNotNull(
        LedgerLine(ui("confirm.spend"), ui("confirm.minus", spent, regretTitle), Tone.SPEND),
        regretLeft?.takeIf { it >= spent }?.let { LedgerLine(ui("confirm.left"), ui("confirm.amount", it - spent, regretTitle)) },
    )
    fun shortage(spent: Int) = regretLeft?.takeIf { it < spent }?.let { ui("confirm.short", it) }

    if (reset) {
        // The start node is not given back, so it is not paid for — the count says what is.
        val returned = (game.hero?.tree?.size ?: 1) - 1
        ConfirmSheet(
            title = ui("tree.reset_q"), subtitle = ui("confirm.count", returned, nodes(returned)), icon = treeIcon,
            ledger = regretLines(returned) + LedgerLine(ui("confirm.returns"), ui("confirm.plus_count", returned, nodes(returned)), Tone.GAIN),
            note = ui("tree.reset_confirm"), warning = shortage(returned), blocked = shortage(returned) != null, danger = true,
            confirm = ui("tree.reset_do"), onDismiss = onClear,
        ) { onReset() }
    }
}

/** Russian counts its nouns in three forms and English in two: see `plural`. */
