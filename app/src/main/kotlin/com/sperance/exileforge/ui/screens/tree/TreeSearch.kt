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

/** Поиск и план древа (3.80.16): строка поиска, фильтр по тегу, планировщик сборки. */
/**
 * Finding a node by name.
 *
 * The tree is over a hundred nodes across seven class areas, so panning to one by eye is no longer
 * realistic. A match selects the node, which is what the map draws a ring around.
 */
@Composable internal fun TreeSearch(game: GameUi, rawQuery: String, selected: String, nodes: List<TreeNode>, onSelect: (String) -> Unit) {
    val matches = remember(nodes, rawQuery, game.lang) {
        val query = rawQuery.trim()
        if (query.isBlank()) {
            emptyList()
        } else {
            nodes.filter { nodeTitle(it.code).contains(query, true) || it.code.contains(query, true) }.take(8)
        }
    }
    if (rawQuery.isBlank()) return
    ForgePanel {
        Engraved(ui("tree.find_node"))
        if (matches.isEmpty()) Text(ui("tree.nothing_found"), color = Muted)
        matches.forEach { node ->
            ForgeTextButton(onClick = { onSelect(node.code) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${nodeTitle(node.code)} · ${nodeTypeTitle(node.type, game.lang)}",
                    color = nodeColour(node, node.code == selected),
                )
            }
        }
    }
}

/**
 * The search and the tag filter, in a sheet of their own.
 *
 * Both only light nodes up on the map, so they are set once and then read off the tree rather than
 * kept on screen beside it. [found] is the search's match count, null while the query is too short.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TreeFilters(query: String, onQuery: (String) -> Unit, found: Int?, tag: String?, onTag: (String?) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Engraved(ui("tree.search_filters"))
        OutlinedTextField(
            query,
            { onQuery(it.take(DefaultInputs.search)) },
            placeholder = { Text(ui("tree.search_hint")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                found?.let {
                    Text(
                        ui("tree.search_found", it),
                        color = Vital,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TREE_TAGS.forEach { code ->
                FilterChip(selected = tag == code, onClick = { onTag(if (tag == code) null else code) }, label = { Text(ui("tree.tag.$code")) })
            }
        }
        if (query.isNotBlank() || tag != null) {
            ForgeOutlinedButton(onClick = {
                onQuery("")
                onTag(null)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("tree.filters_clear"))
            }
        }
    }
}

/** The tags the tree's filter offers (3.47.0): the ones its lines carry most. */
internal val TREE_TAGS = listOf("life", "defences", "critical", "fire", "cold", "lightning", "chaos", "physical", "elemental", "attack", "caster", "speed", "mana")

/** Every node whose lines — or any of its options — carry [tag]. */

/** Nodes whose name, or any stat they give, holds [query] (3.54.0). */
internal fun nodesMatching(index: ContentIndex, query: String): Set<String> = index.content.tree.nodes.mapNotNullTo(HashSet()) { node ->
    val stats = (node.lines + node.options.flatten()).flatMap { line -> index.modifier(line.code)?.effects?.map { statTitle(it.stat) }.orEmpty() }
    node.code.takeIf { nodeTitle(it).contains(query, true) || stats.any { stat -> stat.contains(query, true) } }
}

internal fun nodesTagged(index: ContentIndex, tag: String): Set<String> = index.content.tree.nodes.mapNotNullTo(HashSet()) { node ->
    node.code.takeIf { (node.lines + node.options.flatten()).any { line -> index.modifier(line.code)?.tags?.contains(tag) == true } }
}

/**
 * The chosen node against the plan (3.47.0): out of it, or the rules' shortest way to it — from what is taken and
 * what is planned already — added to its end. A node with options is taken by hand: its choice is the player's.
 */
internal sealed interface PlanOption {
    data class Remove(val code: String) : PlanOption
    data class Add(val way: List<String>) : PlanOption

    /** No button: a [key] of the dictionary says why, or nothing at all when the node is taken. */
    data class Note(val key: String?) : PlanOption
}

internal fun planOption(index: ContentIndex, heroClass: HeroClass?, taken: Set<String>, plan: List<TakenNode>, node: TreeNode): PlanOption {
    if (node.code in taken) return PlanOption.Note(null)
    if (plan.any { it.code == node.code }) return PlanOption.Remove(node.code)
    if (node.options.isNotEmpty()) return PlanOption.Note("tree.plan_choice")
    val start = heroClass?.startNode ?: return PlanOption.Note(null)
    val from = taken + plan.map { it.code }
    val way = if (from.isEmpty()) null else TreeAllocation.path(index.tree, from, start, node.code)
    return way?.let { PlanOption.Add(it) } ?: PlanOption.Note("tree.plan_no_way")
}

/** The plan as a whole (3.47.0): how many nodes are still to take, what they cost, and what they will give. */
@Composable internal fun PlanPanel(game: GameUi, index: ContentIndex, taken: Set<String>, plan: List<TakenNode>, enabled: Boolean, onClear: () -> Unit) {
    val left = plan.filter { it.code !in taken }
    val cost = left.sumOf { index.tree.node(it.code)?.cost ?: 0 }
    val totals = remember(index, left) { SheetCalculator(index).let { it.contributions(it.expand(index.tree.lines(left))) } }
    ForgePanel(accent = Rune) {
        Engraved(ui("tree.plan_title"), Rune)
        PropertyRow(ui("tree.plan_left"), ui("tree.plan_cost", left.size, cost), Glyph.LEVEL)
        MutedText(ui("tree.plan_note"))
        totals.forEach { total -> PropertyRow(statTitle(total.stat, game.lang), contributionText(total), stat = total.stat) }
        ForgeOutlinedButton(enabled = enabled, onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text(ui("tree.plan_clear")) }
    }
}
