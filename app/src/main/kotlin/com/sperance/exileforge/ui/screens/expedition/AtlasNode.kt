package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.atlas.AtlasFog
import com.sperance.exileforge.core.display.atlasNodeTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.AtlasNode
import com.sperance.exileforge.rules.content.AtlasNodeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Карточка узла атласа и итоги (3.80.24). */
/** The chosen star: what it is, what it gives, and the one command the server would accept for it. */
@Composable internal fun NodeSheet(
    index: ContentIndex,
    node: AtlasNode,
    taken: Set<String>,
    available: Int,
    price: Long,
    enabled: Boolean,
    onTake: () -> Unit,
    onRefund: () -> Unit,
    modifier: Modifier,
) {
    val graph = index.atlasGraph
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.navigationBarsPadding().padding(10.dp).fillMaxWidth().background(Color(0xE6080C16), shape).border(1.dp, Sky.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val kind = ui("atlas.kind.${node.kind.name}")
        Text(
            if (node.branch == AtlasBranch.ROOT) kind else ui("atlas.kind_branch", kind, ui("atlas.branch.${node.branch.name}")).uppercase(),
            color = Sky.text.copy(alpha = .75f),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(atlasNodeTitle(node.code), color = Color.White, style = MaterialTheme.typography.titleMedium)
        val isTaken = node.code in taken
        // «Было → станет» (3.54.0): each line beside the atlas's whole of its modifier now and with this node taken.
        val totals = remember(taken) { AtlasTotals.of(index, taken) }
        node.lines.forEach { line ->
            effectLines(index, line).forEach { Text("•  $it", color = ModBlue, style = MaterialTheme.typography.bodySmall) }
            if (node.kind != AtlasNodeKind.START) {
                val now = totals[line.code]?.firstOrNull() ?: 0.0
                val value = line.values.firstOrNull() ?: 0.0
                val after = if (isTaken) now - value else now + value
                Text(
                    ui(if (isTaken) "atlas.compare_refund" else "atlas.compare_take", number(now), number(after)),
                    color = Sky.text.copy(alpha = .7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
        val canTake = AtlasFog.canTake(graph, node.code, taken)
        val canRefund = AtlasFog.canRefund(graph, node.code, taken)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MutedText(
                when {
                    node.kind == AtlasNodeKind.START -> ui("atlas.start_hint")
                    isTaken && canRefund -> ui("atlas.refund_hint", price)
                    isTaken -> ui("atlas.holds_hint")
                    canTake && available <= 0 -> ui("atlas.no_points")
                    canTake -> ui("atlas.take_hint")
                    else -> ui("atlas.far_hint")
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            when {
                canTake -> ForgeButton(
                    onClick = onTake,
                    enabled = enabled && available > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Sky.take, contentColor = Color.White),
                ) { Text(ui("atlas.take")) }

                canRefund -> ForgeOutlinedButton(onClick = onRefund, enabled = enabled) { Text(ui("atlas.refund"), color = Sky.text) }
            }
        }
    }
}

/**
 * «+4% Количество предметов на картах», one per stat of a node's line; a count — chests, fountains, Vaal
 * modifiers — carries no percent. A line whose modifier the content does not hold reads as its sentence.
 */
internal fun effectLines(index: ContentIndex, line: Line): List<String> {
    val def = index.modifier(line.code) ?: return listOf(lineText(index, line))
    return def.effects.mapIndexed { i, effect ->
        val value = line.values.getOrElse(i) { 0.0 }
        val sign = if (value >= 0) "+" else "−"
        val unit = if (effect.stat in AtlasEffects.flat) "" else "%"
        // The dictionary's «Атлас: …» prefix is the screen's own title here (2.73.0).
        "$sign${number(abs(value))}$unit ${statTitle(effect.stat).substringAfter(": ")}"
    }
}

/** The atlas's lines added up (3.54.0): by modifier code, value by value, over every taken node. */
internal object AtlasTotals {
    fun of(index: ContentIndex, taken: Set<String>): Map<ModifierCode, List<Double>> = sum(index.atlas.nodes.filter { it.code in taken }.flatMap { it.lines })

    fun sum(lines: List<Line>): Map<ModifierCode, List<Double>> = lines.groupBy { it.code }.mapValues { (_, same) ->
        List(same.maxOf { it.values.size }) { i -> same.sumOf { it.values.getOrElse(i) { 0.0 } } }
    }
}

/** «Итого» (3.54.0): what the taken nodes give, added up and grouped by mechanic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AtlasSummary(index: ContentIndex, taken: Set<String>, onDismiss: () -> Unit) {
    val groups = remember(taken) {
        index.atlas.nodes.filter { it.code in taken }.groupBy { it.branch }
            .mapValues { (_, nodes) -> AtlasTotals.sum(nodes.flatMap { it.lines }) }.filterValues { it.isNotEmpty() }
    }
    ForgeSheet(onDismissRequest = onDismiss, containerColor = Color(0xF20A0E18)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(ui("atlas.summary_title"), color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (groups.isEmpty()) Text(ui("atlas.summary_empty"), color = Sky.text, style = MaterialTheme.typography.bodySmall)
            groups.forEach { (branch, totals) ->
                Text(ui("atlas.branch.${branch.name}").uppercase(), color = branch.hue(), style = MaterialTheme.typography.labelMedium)
                totals.forEach { (code, values) -> effectLines(index, Line(code, values)).forEach { Text("•  $it", color = ModBlue, style = MaterialTheme.typography.bodySmall) } }
            }
        }
    }
}
