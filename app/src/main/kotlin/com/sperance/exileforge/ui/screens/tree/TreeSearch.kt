package com.sperance.exileforge.ui.screens.tree

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.MoreVert
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
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.nodeTypeTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
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

/** Поиск и план древа: строка поиска, фильтр по тегу, планировщик сборки. */
/**
 * Finding a node by name.
 *
 * [matches] - узлы, что нашёл поиск панели древа (те же, что подсвечены на карте); null - запрос короче двух букв, панели нет.
 *
 * The tree is over a hundred nodes across seven class areas, so panning to one by eye is not
 * realistic. A match selects the node, which is what the map draws a ring around.
 */
@Composable internal fun TreeSearch(game: GameUi, matches: List<TreeNode>?, selected: String, onSelect: (String) -> Unit) {
    matches ?: return
    ForgePanel {
        Engraved(ui("tree.find_node"))
        if (matches.isEmpty()) Text(ui("tree.nothing_found"), color = Muted)
        matches.take(FOUND_SHOWN).forEach { node ->
            ForgeButton(onClick = { onSelect(node.code) }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Text, accent = nodeColour(node, node.code == selected)) {
                Text(
                    "${nodeTitle(node.code)} · ${nodeTypeTitle(node.type, game.lang)}",
                )
            }
        }
    }
}

/**
 * The search and the tag filter, in a sheet of their own.
 *
 * Both only light nodes up on the map, so they are set once and then read off the tree rather than
 * kept on screen beside it. The search is the shared search bar ([SearchBar]); [found] is its match count, null while the query
 * is too short.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TreeFilters(query: String, onQuery: (String) -> Unit, found: Int?, tag: String?, onTag: (String?) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Engraved(ui("tree.search_filters"))
        SearchBar(query, onQuery, ui("tree.search_hint"), found = found)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TREE_TAGS.forEach { code ->
                ForgeChip(ui("tree.tag.$code"), tag == code, { onTag(if (tag == code) null else code) }, mode = ChipMode.Choice)
            }
        }
        if (query.isNotBlank() || tag != null) {
            ForgeButton(onClick = {
                onQuery("")
                onTag(null)
            }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                Text(ui("tree.filters_clear"))
            }
        }
    }
}

/** Сколько найденных узлов показывает поиск древа. */
private const val FOUND_SHOWN = 8

/** The tags the tree's filter offers: the ones its lines carry most. */
internal val TREE_TAGS = listOf("life", "defences", "critical", "fire", "cold", "lightning", "chaos", "physical", "elemental", "attack", "caster", "speed", "mana")

internal fun nodesTagged(index: ContentIndex, tag: String): Set<String> = index.content.tree.nodes.mapNotNullTo(HashSet()) { node ->
    node.code.takeIf { (node.lines + node.options.flatten()).any { line -> index.modifier(line.code)?.tags?.contains(tag) == true } }
}
