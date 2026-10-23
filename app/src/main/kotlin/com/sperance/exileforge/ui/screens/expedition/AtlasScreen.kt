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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.atlas.AtlasBranch
import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.atlas.AtlasFog
import com.sperance.exileforge.core.campaign.Places
import com.sperance.exileforge.core.display.atlasNodeTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.rules.content.AtlasNode
import com.sperance.exileforge.rules.content.AtlasNodeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.ShellViewModel
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

/** The night the atlas is drawn on (the owner's mockup I «Звёздная карта»). */
internal object Sky {
    val deep = Color(0xFF030408)
    val night = Color(0xFF070A12)
    val dawn = Color(0xFF10182A)
    val line = Color(0xFF2B3A57)
    val faint = Color(0xFF96A5C8)
    val text = Color(0xFFC9D8EF)
    val take = Color(0xFF3B6FA8)
}

/** Each trunk's own light, so a player reads which direction a star belongs to before its name; Скверна - жила своей палитры. */
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
    AtlasBranch.ALTAR -> Color(0xFFD03040)
    AtlasBranch.BLIGHT -> Color(0xFFD81B60)
    AtlasBranch.TRAPS -> Color(0xFFB9C2CF)
    AtlasBranch.SECRETS -> Color(0xFFE8E0C8)
    AtlasBranch.GATHER -> Color(0xFF4FA048)
    AtlasBranch.ROOT -> Color.White
}

/** Which trunk a node belongs to, read off its code. */
internal val AtlasNode.branch: AtlasBranch get() = AtlasFog.branch(code)

/**
 * The atlas tree as a night sky: every node a star, the taken ones burning and joined by lit
 * threads, the ones a point could take next twinkling. The tree is the content's and the hero's
 * nodes and points are the hero's own; [AtlasFog] only foresees the server's rule so a refused command
 * shows no button. The sky is dragged and pinched; a tap picks the nearest star within reach, and its
 * sheet takes or gives it.
 *
 * Возврат узла спрашивается удержанием: очки возвращаются, золото нет. «Итого» - строки всех взятых узлов по механикам. Очки парят над
 * небом над листом узла. Старт не возвращается. Атлас - страница «Похода»: имя и возврат - в полосе страниц.
 */
@Composable fun AtlasScreen() {
    val game by koinViewModel<ExpeditionViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val model = koinViewModel<ExpeditionViewModel>()
    val expedition by model.state.collectAsStateWithLifecycle()
    val atlas = expedition.atlas ?: return
    val index = game.index
    val state = game.atlasState
    var resetting by remember { mutableStateOf(false) }
    var refunding by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var centerOn by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val camera = remember { SkyCamera() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Sky.deep, Sky.night, Sky.dawn)))) {
        if (index == null || state == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Sky.text)
        } else {
            val taken = state.allocated.toSet()
            Sky(index, taken, atlas.selected, camera, Modifier.fillMaxSize(), centerOn, { centerOn = null }, onSelect = model::selectAtlasNode)
            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                val node = index.atlasGraph.node(atlas.selected)
                PointsPill(state.available, state.points, if (node == null) Modifier.padding(bottom = 16.dp) else Modifier)
                node?.let {
                    NodeSheet(
                        index, it, taken, state.available, game.goldBill(index.atlas.respec.price(game.heroLevel, 1)), enabled = !game.busy,
                        onTake = { model.allocateAtlas(it.code) }, onRefund = { refunding = it.code }, modifier = Modifier,
                    )
                }
            }
        }
        val spent = (state?.allocated?.size ?: 1) - 1
        val menu = @Composable {
            AtlasMenu(menuOpen, summary = spent > 0, reset = spent > 0 && !game.busy, onDismiss = { menuOpen = false }, onSummary = { summary = true }, onReset = { resetting = true })
        }
        val tools = listOfNotNull(
            CanvasTool(Icons.Outlined.Search, ui("atlas.search"), enabled = true, onClick = { searching = true }).takeIf { index != null },
            CanvasTool(Icons.Outlined.MoreVert, ui("common.more"), enabled = true, onClick = { menuOpen = true }, anchored = menu),
        )
        CanvasToolbar(tools, Modifier.align(Alignment.TopEnd).padding(horizontal = 12.dp, vertical = 8.dp), accent = Sky.text)
        if (searching && index != null) {
            AtlasSearch(index, state?.allocated?.toSet().orEmpty(), { searching = false }) { code ->
                model.selectAtlasNode(code)
                centerOn = code
            }
        }
        val regrets = game.hero?.count(Orb.ORB_OF_REGRET.name) ?: 0L
        if (resetting && index != null && state != null) {
            val nodes = state.allocated.size - 1
            RespecSheet(
                ui("atlas.reset_q"),
                null,
                ui("atlas.reset"),
                nodes,
                game.goldBill(index.atlas.respec.price(game.heroLevel, nodes)),
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
                    game.goldBill(index.atlas.respec.price(game.heroLevel, 1)),
                    regrets,
                    onDismiss = { refunding = null },
                ) { regret ->
                    refunding = null
                    model.refundAtlas(code, regret)
                }
            }
        }
        if (summary && index != null && state != null) AtlasSummary(index, state.allocated.toSet()) { summary = false }
        ToastBelow(64.dp)
    }
}

/** Поиск узла Атласа ([PlaceSearchSheet]): имя узла и ветви, взятый - с пометкой; выбор ставит звезду посреди неба и открывает её лист. */
@Composable private fun AtlasSearch(index: ContentIndex, taken: Set<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val places = remember(index, taken) { Places.atlas(index.atlas.nodes, taken) }
    val text = PlaceSearchText(ui("atlas.search_hint"), ui("atlas.search_help"), ui("atlas.search_none_help"))
    PlaceSearchSheet(places, emptyList(), text, onDismiss, filter = null, onPick = onPick)
}

/**
 * Giving [nodes] atlas nodes back: [gold] at the respec price or, when the bag holds [regrets],
 * an Orb of Regret per node — the player picks; a payment the hero cannot make keeps the button off and says why. Золото - счёт [GoldBill] против кошелька: «Есть · Спишется · Останется»; null - герой не прочитан, платить нечем.
 */
@Composable internal fun RespecSheet(
    title: String,
    subtitle: String?,
    confirm: String,
    nodes: Int,
    gold: GoldBill?,
    regrets: Long,
    onDismiss: () -> Unit,
    onConfirm: (regret: Boolean) -> Unit,
) {
    var regret by remember { mutableStateOf(false) }
    val noRegret = regret && regrets < nodes
    ConfirmSheet(
        title = title, subtitle = subtitle, confirm = confirm, danger = true, onDismiss = onDismiss,
        ledger = listOfNotNull(
            LedgerLine(ui("atlas.reset_cost"), "$nodes × ${itemTitle(Orb.ORB_OF_REGRET.name)}", Tone.SPEND).takeIf { regret },
            LedgerLine(ui("atlas.reset_back"), nodes.toString(), Tone.GAIN),
        ),
        blocked = noRegret || (!regret && gold == null),
        warning = ui("atlas.no_regret").takeIf { noRegret },
        gold = gold.takeUnless { regret },
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

/** Меню «⋮» ряда Атласа ([open]): итоги ([summary]) и сброс узлов ([reset]); выбор пункта закрывает меню ([onDismiss]). */
@Composable private fun AtlasMenu(open: Boolean, summary: Boolean, reset: Boolean, onDismiss: () -> Unit, onSummary: () -> Unit, onReset: () -> Unit) {
    ForgeMenu(expanded = open, onDismissRequest = onDismiss) {
        val entries = listOf(
            Triple(ui("atlas.summary"), summary, onSummary),
            Triple(ui("atlas.reset"), reset, onReset),
        )
        entries.forEach { (label, enabled, action) ->
            DropdownMenuItem(text = { Text(label) }, enabled = enabled, onClick = {
                onDismiss()
                action()
            })
        }
    }
}
