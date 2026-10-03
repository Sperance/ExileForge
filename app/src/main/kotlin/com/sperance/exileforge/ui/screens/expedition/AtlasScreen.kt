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
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.AtlasNode
import com.sperance.exileforge.rules.content.AtlasNodeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The night the atlas is drawn on (2.68.0, the owner's mockup I «Звёздная карта»). */
internal object Sky {
    val deep = Color(0xFF030408)
    val night = Color(0xFF070A12)
    val dawn = Color(0xFF10182A)
    val line = Color(0xFF2B3A57)
    val faint = Color(0xFF96A5C8)
    val text = Color(0xFFC9D8EF)
    val take = Color(0xFF3B6FA8)
}

/** Each trunk's own light, so a player reads which direction a star belongs to before its name. */
internal fun AtlasBranch.hue(): Color = when (this) {
    AtlasBranch.LOOT -> Color(0xFFE8CF94)
    AtlasBranch.MAPS -> Color(0xFF8EC5FF)
    AtlasBranch.TIERS -> Color(0xFFBFE3EF)
    AtlasBranch.BOSSES -> Color(0xFFB48CFF)
    AtlasBranch.ABYSS -> Color(0xFFA070FF)
    AtlasBranch.VAAL -> Color(0xFFFF5A46)
    AtlasBranch.CRYSTALS -> Color(0xFF9FD8E8)
    AtlasBranch.EXPEDITION -> Color(0xFF7FC8A0)
    AtlasBranch.CRAFT -> Color(0xFFE8A05A)
    AtlasBranch.POWER -> Color(0xFFD24A43)
    AtlasBranch.INFLUENCE -> Color(0xFF9FD2F0)
    AtlasBranch.ROOT -> Color.White
}

/** Which trunk a node belongs to, read off its code. */
internal val AtlasNode.branch: AtlasBranch get() = AtlasFog.branch(code)

/**
 * The atlas tree (2.68.0) as a night sky: every node a star, the taken ones burning and joined by lit
 * threads, the ones a point could take next twinkling. The tree is the content's (3.0.0) and the hero's
 * nodes and points are the hero's own; [AtlasFog] only foresees the server's rule so a refused command
 * shows no button. The sky is dragged and pinched; a tap picks the nearest star within reach, and its
 * sheet takes or gives it.
 */
@Composable fun AtlasScreen(s: ForgeState, vm: ForgeViewModel) {
    val model = koinViewModel<ExpeditionViewModel>()
    val atlas = s.play.atlas ?: return
    val index = s.index
    val state = s.atlasState
    var resetting by remember { mutableStateOf(false) }
    // A node given back asks first, held to confirm (3.2.0): the points come back, the gold does not
    var refunding by remember { mutableStateOf<String?>(null) }
    // «Итого» (3.54.0): every taken node's lines added up, by mechanic.
    var summary by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Sky.deep, Sky.night, Sky.dawn)))) {
        if (index == null || state == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Sky.text)
        } else {
            val taken = state.allocated.toSet()
            Sky(index, taken, atlas.selected, Modifier.fillMaxSize(), onSelect = model::selectAtlasNode)
            // The points float over the sky, stacked above the node's sheet so neither hides the other.
            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                val node = index.atlasGraph.node(atlas.selected)
                PointsPill(state.available, state.points, if (node == null) Modifier.navigationBarsPadding().padding(bottom = 16.dp) else Modifier)
                node?.let {
                    NodeSheet(
                        index, it, taken, state.available, index.atlas.respec.price(s.heroLevel, 1), enabled = !s.busy,
                        onTake = { model.allocateAtlas(it.code) }, onRefund = { refunding = it.code }, modifier = Modifier,
                    )
                }
            }
        }
        // The start is nobody's to give back: what was spent is every taken node but it.
        val spent = (state?.allocated?.size ?: 1) - 1
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = model::closeAtlas) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.close"), tint = Sky.text) }
            Text(ui("atlas.title"), color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FirstVisit(Guide.ATLAS)
            AtlasMenu(summary = spent > 0, reset = spent > 0 && !s.busy, onSummary = { summary = true }, onReset = { resetting = true })
        }
        val money = s.hero?.money ?: 0L
        val regrets = s.hero?.count(Orb.ORB_OF_REGRET.name) ?: 0L
        if (resetting && index != null && state != null) {
            val nodes = state.allocated.size - 1
            RespecSheet(
                ui("atlas.reset_q"),
                null,
                ui("atlas.reset"),
                nodes,
                index.atlas.respec.price(s.heroLevel, nodes),
                money,
                regrets,
                onDismiss = { resetting = false },
            ) { regret ->
                resetting = false
                model.resetAtlas(regret)
            }
        }
        refunding?.let { code ->
            if (index != null) {
                RespecSheet(
                    ui("atlas.refund_q"),
                    atlasNodeTitle(code),
                    ui("atlas.refund"),
                    1,
                    index.atlas.respec.price(s.heroLevel, 1),
                    money,
                    regrets,
                    onDismiss = { refunding = null },
                ) { regret ->
                    refunding = null
                    model.refundAtlas(code, regret)
                }
            }
        }
        if (summary && index != null && state != null) AtlasSummary(index, state.allocated.toSet()) { summary = false }
        ToastHost(s, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp))
    }
}

/**
 * Giving [nodes] atlas nodes back: [gold] at the respec price or, since server 1.65.0 and when the bag holds [regrets],
 * an Orb of Regret per node — the player picks; a payment the hero cannot make keeps the button off and says why.
 */
@Composable internal fun RespecSheet(
    title: String,
    subtitle: String?,
    confirm: String,
    nodes: Int,
    gold: Long,
    money: Long,
    regrets: Long,
    onDismiss: () -> Unit,
    onConfirm: (regret: Boolean) -> Unit,
) {
    var regret by remember { mutableStateOf(false) }
    val short = if (regret) regrets < nodes else money < gold
    val cost = if (regret) "$nodes × ${itemTitle(Orb.ORB_OF_REGRET.name)}" else ui("atlas.gold", gold)
    ConfirmSheet(
        title = title, subtitle = subtitle, confirm = confirm, danger = true, onDismiss = onDismiss,
        ledger = listOf(LedgerLine(ui("atlas.reset_cost"), cost, Tone.SPEND), LedgerLine(ui("atlas.reset_back"), nodes.toString(), Tone.GAIN)),
        blocked = short,
        warning = when {
            !short -> null
            regret -> ui("atlas.no_regret")
            else -> ui("atlas.no_gold")
        },
        options = if (regrets > 0) {
            (
                {
                    PillTabs(listOf(ui("atlas.pay_gold"), ui("atlas.pay_regret", regrets, nodes)), if (regret) 1 else 0, { regret = it == 1 }, segmented = true)
                }
                )
        } else {
            null
        },
    ) { onConfirm(regret) }
}

/** «Доступно очков: X из Y», a pill floating over the bottom of the sky. */
@Composable internal fun PointsPill(available: Int, points: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        ui("atlas.points", available, points),
        color = if (available > 0) Color.White else Sky.text,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.background(Color(0xE6080C16), shape).border(1.dp, Sky.line, shape).padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** The header's ⋮: the totals, the reset, the guide and the bug report, so the title keeps the bar. */
@Composable internal fun AtlasMenu(summary: Boolean, reset: Boolean, onSummary: () -> Unit, onReset: () -> Unit) {
    val guides = LocalGuideDesk.current
    val bug = LocalBugReport.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, ui("common.more"), tint = Sky.text) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val entries = listOfNotNull(
                Triple(ui("atlas.summary"), summary, onSummary),
                Triple(ui("atlas.reset"), reset, onReset),
                guides?.let { desk -> Triple(ui("guide.help"), true) { desk.show(Guide.ATLAS) } },
                bug?.let { Triple(ui("bug.open"), true, it) },
            )
            entries.forEach { (label, enabled, action) ->
                DropdownMenuItem(text = { Text(label) }, enabled = enabled, onClick = {
                    open = false
                    action()
                })
            }
        }
    }
}
