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
import com.sperance.exileforge.core.display.Term
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
import com.sperance.exileforge.presentation.state.sellPrice
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

/** Карточка узла (3.80.16): описание, действия, самоцвет в гнезде, «Итого». */
/**
 * The chosen node: what it gives, and the commands the server accepts for it.
 *
 * A socket is the exception, because it gives nothing by itself — what it holds is a jewel, and
 * the command there is to put one in or take it out.
 */
@Composable internal fun NodeDetails(
    game: GameUi,
    index: ContentIndex,
    heroClass: HeroClass?,
    node: TreeNode?,
    taken: Set<String>,
    reachable: Set<String>,
    enabled: Boolean,
    path: List<String>?,
    available: Int,
    onClose: () -> Unit,
    onAllocate: (String, Int?) -> Unit,
    onPath: (String, Int?) -> Unit,
    onRefund: (String) -> Unit,
    onRefundBranch: (String) -> Unit,
    onSocket: (String, String) -> Unit,
    onUnsocket: (String) -> Unit,
    onRechoose: (String, Int) -> Unit,
) {
    if (node == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ui("tree.no_selection"), color = GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("tree.no_selection_hint"))
            }
            CloseButton(onClose)
        }
        return
    }
    val allocated = node.code in taken
    val choosing = node.options.isNotEmpty()
    // The option the hero took is theirs: it is read from the snapshot, not from the tree.
    val chosen = game.hero?.tree?.firstOrNull { it.code == node.code }?.choice
    var picked by remember(node.code) { mutableStateOf<Int?>(null) }
    // The node's name heads the card in its own colour, with its kind, its price and whether it is taken under it.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(nodeTitle(node.code), color = nodeColour(node, true), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            MutedText(
                listOf(
                    nodeTypeTitle(node.type, game.lang),
                    "${ui("tree.cost")} ${node.cost}",
                    ui(if (allocated) "tree.taken" else "tree.not_taken"),
                ).joinToString(" · "),
            )
        }
        CloseButton(onClose)
    }
    nodeDescription(node.code).takeIf { it.isNotBlank() }?.let { MutedText(it) }
    if (node.type == SkillNodeType.JEWEL_SOCKET) {
        SocketContents(game, index, node, allocated, enabled, onSocket, onUnsocket)
    } else if (choosing) {
        // A mastery or an attribute node (server 0.52.0): one option, chosen when it is taken. A taken
        // attribute node may change it for a Chaos Orb (2.72.0, server 0.63.0); a mastery may not.
        val rechoosable = allocated && node.type == SkillNodeType.ATTRIBUTE
        MutedText(
            ui(
                when {
                    rechoosable -> "tree.option_rechoose"
                    allocated -> "tree.option_chosen"
                    else -> "tree.option_pick"
                },
            ),
            style = MaterialTheme.typography.labelMedium,
        )
        node.options.forEachIndexed { at, option ->
            val on = when {
                picked != null -> at == picked
                allocated -> at == chosen
                else -> false
            }
            OptionCard(on, enabled = (!allocated && (node.code in reachable || path != null)) || (rechoosable && enabled), onClick = { picked = at }) {
                option.forEach { line -> ModifierLine(index, line) }
            }
        }
        if (rechoosable) {
            val owned = game.bagAmount(Orb.CHAOS_ORB.name) ?: 0L
            ForgeButton(
                enabled = enabled && picked != null && picked != chosen && owned > 0,
                onClick = { picked?.let { onRechoose(node.code, it) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OrbGlyph(Orb.CHAOS_ORB, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui("tree.rechoose", owned))
            }
        }
        if (node.type == SkillNodeType.MASTERY && !allocated && node.code !in reachable) {
            MutedText(ui("tree.mastery_locked"))
        }
    } else {
        if (node.lines.isEmpty()) Text(ui("tree.no_bonuses"), color = Muted)
        node.lines.forEach { line -> ModifierLine(index, line) }
    }

    // The game's words the lines use (3.81.0): «Всплеск магии», a charge, an ailment — each with its rule on a tap.
    val terms = remember(node.code, index) { Term.ofLines(index, node.lines + node.options.flatten()) }
    TermsBlock(terms)

    if (allocated) {
        // A refund is held for a second (3.81.0): a tap on the wrong node no longer costs the point.
        HoldButton(ui("tree.refund"), LifeRed, Modifier.fillMaxWidth(), enabled = enabled && node.type != SkillNodeType.START, rearm = true, millis = REFUND_HOLD_MS) { onRefund(node.code) }
        // The branch (3.54.0, server 1.52.0): this node and everything that would hang loose without it, an Orb of Regret each.
        val branch = remember(node.code, taken) { runCatching { TreeAllocation.branch(index.tree, node, taken) }.getOrNull().orEmpty() }
        if (branch.size > 1) {
            val owned = game.bagAmount(Orb.ORB_OF_REGRET.name) ?: 0L
            HoldButton(ui("tree.refund_branch", branch.size), LifeRed, Modifier.fillMaxWidth(), enabled = enabled && owned >= branch.size, rearm = true, millis = REFUND_HOLD_MS) {
                onRefundBranch(node.code)
            }
            MutedText(ui("tree.refund_branch_note", branch.size, owned))
        }
    } else if (path != null) {
        // A far node (3.39.0): the whole way at once, for the sum of its steps; short of points, the button says so.
        val cost = path.sumOf { index.tree.node(it)?.cost ?: 0 }
        MutedText("${ui("tree.path")}: ${ui("tree.path_value", path.size, cost)}")
        NodeActions {
            ForgeButton(
                enabled = enabled && cost <= available && (!choosing || picked != null),
                onClick = { onPath(node.code, picked) },
                modifier = Modifier.weight(1f),
            ) { Text(if (cost > available) ui("tree.not_enough_points") else ui("tree.path_take", cost)) }
        }
        if (cost > available) MutedText(ui("tree.path_short", cost, available))
    } else if (node.code in reachable || taken.isEmpty()) {
        // Short of points the button says so and stays grey: the server would only refuse (ST_008).
        val short = node.cost > available
        NodeActions {
            ForgeButton(
                enabled = enabled && !short && (!choosing || picked != null),
                onClick = { onAllocate(node.code, picked) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (short) ui("tree.not_enough_points") else ui("tree.allocate"))
            }
        }
        if (short) MutedText(ui("tree.path_short", node.cost, available))
    } else {
        MutedText(ui("tree.path_none"))
    }
    if (node.type == SkillNodeType.START) MutedText(ui("tree.start_note"))
}

/** Ряд команд узла: та, что его берёт. */
@Composable internal fun NodeActions(take: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        take()
    }
}

@Composable internal fun CloseButton(onClose: () -> Unit) {
    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Close, ui("common.close"), tint = Muted, modifier = Modifier.size(18.dp)) }
}

/**
 * What sits in a socket, and what can be put there.
 *
 * A jewel is an ordinary item, so the stash is where the candidates come from: every jewel the hero
 * owns that is neither worn nor already in another socket. Whether this one may go in is still the
 * server's call — the socket has to be taken and free — so the list offers and the refusal explains.
 */
@Composable internal fun SocketContents(
    game: GameUi,
    index: ContentIndex,
    node: TreeNode,
    allocated: Boolean,
    enabled: Boolean,
    onSocket: (String, String) -> Unit,
    onUnsocket: (String) -> Unit,
) {
    val hero = game.hero ?: return
    val inside = hero.jewels[node.code]

    if (inside != null) {
        // A taken socket may still hold a jewel that does nothing - a second copy of a unique one (1.31.0): the sheet's reason.
        val idle = hero.inactive[inside.id]?.joinToString("\n") { requirementReason(it, game.lang) }
        game.view(inside)?.let { jewel ->
            ItemRow(
                jewel,
                enabled = false,
                note = when {
                    !allocated -> ui("tree.socket_locked")
                    idle != null -> idle
                    else -> ui("tree.socket_working")
                },
                noteColor = if (allocated && idle == null) Gold else LifeRed,
            ) { }
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
        game.view(instance)?.let { jewel ->
            ItemRow(
                jewel,
                enabled = enabled && single,
                price = game.sellPrice(instance),
                note = if (single) null else ui("tree.jewel_unique_taken"),
                noteColor = LifeRed,
            ) { onSocket(instance.id, node.code) }
        }
    }
}

internal fun nodes(n: Int) = plural("tree.node", n)

/**
 * One line of what the tree gives: a number, and whether it is a flat one or a percentage.
 *
 * The operation is the whole point. "+120" and "+40%" to the same characteristic are different
 * statements, and a line that dropped the distinction would be a third, untrue one.
 */
internal fun contributionText(total: StatContribution): String {
    val number = statNumber(total.stat, total.value)
    val signed = if (total.value > 0) "+$number" else number
    return when (total.op) {
        Op.INCREASED, Op.MORE -> "$signed%"

        // SET replaces the base outright, so it is not an addition and carries no sign.
        Op.SET -> number

        Op.ADD -> signed + if (statPercent(total.stat)) "%" else ""
    }
}

/** A mastery's or an attribute node's option: a framed card, gold when it is the one. */
@Composable internal fun OptionCard(on: Boolean, enabled: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(if (on) Gold.copy(alpha = .12f) else Abyss)
            .border(if (on) 1.5.dp else 1.dp, if (on) Gold else Bronze.copy(alpha = .5f), shape)
            .clickable(enabled = enabled, onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** The tree's bonuses as the rules sum them (3.54.0: behind «Итого» as well as in the details). */
@Composable internal fun TreeTotals(game: GameUi, tree: com.sperance.exileforge.core.model.tree.TreeState) {
    ForgePanel(accent = Rune) {
        Engraved(ui("tree.totals_title"), Rune)
        if (tree.totals.isEmpty()) Text(ui("tree.totals_empty"), color = Muted)
        // The rules sum this: two INCREASED add up while two MORE multiply, so adding the snapshots here would lie exactly
        // where a player is choosing. It is the tree's contribution, not the character's total.
        tree.totals.forEach { total -> PropertyRow(statTitle(total.stat, game.lang), contributionText(total), stat = total.stat) }
    }
}

/** How long a refund of the tree is held before it goes through. */
private const val REFUND_HOLD_MS = 1_000
