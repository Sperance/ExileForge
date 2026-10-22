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
import com.sperance.exileforge.core.display.atlasBranchTitle
import com.sperance.exileforge.core.display.atlasNodeTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statsOf
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.GoldBill
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Карточка узла атласа и итоги. */
/**
 * The chosen star: what it is, what it gives, and the one command the server would accept for it.
 *
 * [refund] - цена снятия узла золотом против кошелька; null - герой не прочитан. «Было → станет»: каждая строка рядом с итогом Атласа по её
 * модификатору сейчас и с этим узлом; под строкой - пояснение сервера; итог - с основой правил («шанс 15% + узлы 3,6% = 18,6%»).
 * Взаимоисключающие ключевые узлы: пара названа всегда, взятый второй гасит кнопку с причиной.
 */
@Composable internal fun NodeSheet(
    index: ContentIndex,
    node: AtlasNode,
    taken: Set<String>,
    available: Int,
    refund: GoldBill?,
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
            if (node.branch == AtlasBranch.ROOT) kind else ui("atlas.kind_branch", kind, atlasBranchTitle(node.branch)).uppercase(),
            color = Sky.text.copy(alpha = .75f),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(atlasNodeTitle(node.code), color = Color.White, style = MaterialTheme.typography.titleMedium)
        val isTaken = node.code in taken
        val totals = remember(taken) { AtlasTotals.of(index, taken) }
        node.lines.forEach { line ->
            val stats = remember(index, line.code) { index.statsOf(line.code) }
            StatNoted(stats, index, Modifier.fillMaxWidth(), Sky.text) {
                Text("•  ${lineText(index, line)}", color = ModBlue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            if (node.kind != AtlasNodeKind.START) {
                val now = totals[line.code]?.firstOrNull() ?: 0.0
                val value = line.values.firstOrNull() ?: 0.0
                val after = if (isTaken) now - value else now + value
                val base = index.modifier(line.code)?.effectOf(0)?.let { AtlasEffects.base(index, it.stat) }
                Text(
                    if (base != null) {
                        ui(if (isTaken) "atlas.total_refund" else "atlas.total_take", number(base + now), number(base + after), number(base))
                    } else {
                        ui(if (isTaken) "atlas.compare_refund" else "atlas.compare_take", number(now), number(after))
                    },
                    color = Sky.text.copy(alpha = .7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
        val blocker = AtlasFog.excludedBy(graph, node.code, taken)
        node.excludes.forEach { other ->
            Text(ui("atlas.excludes", atlasNodeTitle(other)), color = Caution, style = MaterialTheme.typography.labelSmall)
        }
        val canTake = AtlasFog.canTake(graph, node.code, taken)
        val canRefund = AtlasFog.canRefund(graph, node.code, taken)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MutedText(
                when {
                    node.kind == AtlasNodeKind.START -> ui("atlas.start_hint")
                    isTaken && canRefund -> ui("atlas.refund_hint")
                    isTaken -> ui("atlas.holds_hint")
                    blocker != null -> ui("atlas.excluded_hint", atlasNodeTitle(blocker))
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
                    state = ButtonState.of(enabled && available > 0),
                    accent = Sky.take,
                ) { Text(ui("atlas.take")) }

                canRefund -> {
                    refund?.let { GoldCost(it) }
                    ForgeButton(onClick = onRefund, state = ButtonState.of(enabled), kind = ButtonKind.Secondary, accent = Sky.text) { Text(ui("atlas.refund")) }
                }

                blocker != null -> ForgeButton(onClick = {}, state = ButtonState.of(false)) { Text(ui("atlas.take")) }
            }
        }
    }
}

/** The atlas's lines added up: by modifier code, value by value, over every taken node. */
internal object AtlasTotals {
    fun of(index: ContentIndex, taken: Set<String>): Map<ModifierCode, List<Double>> = sum(index.atlas.nodes.filter { it.code in taken }.flatMap { it.lines })

    fun sum(lines: List<Line>): Map<ModifierCode, List<Double>> = lines.groupBy { it.code }.mapValues { (_, same) ->
        List(same.maxOf { it.values.size }) { i -> same.sumOf { it.values.getOrElse(i) { 0.0 } } }
    }
}

/** «Итого»: what the taken nodes give, added up and grouped by mechanic. */
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
                Text(atlasBranchTitle(branch).uppercase(), color = branch.hue(), style = MaterialTheme.typography.labelMedium)
                totals.forEach { (code, values) -> Text("•  ${lineText(index, Line(code, values))}", color = ModBlue, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
