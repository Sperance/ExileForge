package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildBranch
import com.sperance.exileforge.rules.content.GuildNode
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.rules.content.GuildTreeRule
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos

/**
 * The guild's bonus tree (3.79.0, server 1.74.0): three branches, each node with its ranks; a point for every guild level,
 * spent by the leader, the second row open once the branch holds the rules' points. Combat and loot ride on every member's
 * runs, economy on the guild itself. A reset gives every point back, free once a week.
 *
 * 4.8.0 (макет «Дерево гильдии», вариант A «Созвездие»): ветви - столбцы, узлы - круги с медалью и полосками рангов, связи
 * светятся по взятым рангам, второй ряд - под «воротами» с числом очков из правил; нажатие на узел - лист с эффектом и «+1 ранг».
 */
@Composable internal fun TreeTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val rule = game.index?.guilds?.tree ?: return
    val leader = me != null && GuildPolicy.can(me.role, GuildAction.TREE)
    var opened by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item(key = "points") { TreePoints(game, vm, guild, rule, leader) }
        item(key = "constellation") { Constellation(rule, guild.tree) { opened = it.code } }
    }
    opened?.let(rule::node)?.let { node ->
        NodeSheet(node, rule, guild, bar = rule.bar(guild.tree, guild.level, node, leader), busy = game.busy, onTake = { vm.takeNode(node.code) }) { opened = null }
    }
}

/**
 * Шапка древа: «свободно / всего» крупно, уровень гильдии и подсказка с числами правил. Сброс (0.14.0, ASANA-N03) - у главы
 * удержанием; срок «раз в N дней» и живой таймер до следующего сброса видят все участники (первый сброс гильдии - сразу).
 */
@Composable private fun TreePoints(game: GameUi, vm: GuildViewModel, guild: GuildView, rule: GuildTreeRule, leader: Boolean) {
    val now by produceState(System.currentTimeMillis(), guild.respecAt) {
        while (guild.respecAt > value) {
            delay(RESET_TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    val ready = now >= guild.respecAt
    ForgePanel {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(guild.treePoints.toString(), color = Caution, style = relicName(22))
                MutedText(ui("guild.tree_total", rule.points(guild.level)), style = MaterialTheme.typography.labelMedium)
            }
            MutedText(ui("guild.tree_free_level", guild.level), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            ui("guild.tree_reset_rule", rule.respecDays) + " · " + if (ready) ui("guild.tree_reset_ready") else ui("guild.tree_reset_in", resetLeft(guild.respecAt - now)),
            color = if (ready) Vital else Caution,
            style = MaterialTheme.typography.labelMedium,
        )
        if (leader) {
            Spacer(Modifier.height(6.dp))
            HoldButton(
                label = ui("guild.tree_reset_hold"),
                accent = LifeRed,
                enabled = !game.busy && ready && guild.tree.isNotEmpty(),
                rearm = true,
                millis = RESET_HOLD_MS,
                onHeld = vm::resetTree,
            )
        }
        Spacer(Modifier.height(6.dp))
        MutedText(if (leader) ui("guild.tree_hint_leader", rule.rowGate) else ui("guild.tree_hint"), style = MaterialTheme.typography.bodySmall)
    }
}

/** Сколько ждать сброса: «6 д 23 ч 59 мин», меньше суток - без дней. */
private fun resetLeft(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    val days = minutes / (24 * 60)
    val time = ui("merchant.time", minutes / 60 % 24, minutes % 60)
    return if (days > 0) ui("guild.tree_reset_days", days, time) else time
}

/** Удержание сброса древа - дольше обычного подтверждения: сброс снимает все узлы гильдии. */
private const val RESET_HOLD_MS = 1_200

/** Шаг живого таймера до сброса. */
private const val RESET_TICK_MS = 1_000L

/**
 * «Созвездие»: подписи ветвей с очками в них, затем ряды древа; ряд выше первого - под «воротами» с [GuildTreeRule.rowGate].
 * В ряду ветвь - столбец своих узлов, между узлами столбца - связь, что светится, когда верхний узел взят.
 */
@Composable private fun Constellation(rule: GuildTreeRule, taken: Map<String, Int>, onOpen: (GuildNode) -> Unit) {
    val branches = GuildBranch.entries
    ForgePanel {
        Row(Modifier.fillMaxWidth()) {
            branches.forEach { branch ->
                val points = rule.inBranch(taken, branch)
                Text(
                    loc("guild.branch.${branch.name}").uppercase() + if (points > 0) " · $points" else "",
                    color = Brass,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        rule.nodes.map { it.row }.distinct().sorted().forEach { row ->
            if (row > 1) RowGate(row, rule.rowGate)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                branches.forEach { branch ->
                    val open = row <= 1 || rule.inBranch(taken, branch) >= rule.rowGate
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val nodes = rule.nodes.filter { it.branch == branch && it.row == row }
                        nodes.forEachIndexed { i, node ->
                            val ranks = taken[node.code] ?: 0
                            if (i > 0) Link(lit = (taken[nodes[i - 1].code] ?: 0) > 0 && open)
                            NodeStar(node, ranks, open) { onOpen(node) }
                        }
                    }
                }
            }
        }
    }
}

/** «Ворота» ряда: пунктир через всё древо и подпись, сколько очков ветви открывают ряд. */
@Composable private fun RowGate(row: Int, gate: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(Brass.copy(alpha = .6f), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        }
        Text(ui("guild.tree_row_gate", roman(row), gate), color = Brass, style = MaterialTheme.typography.labelSmall)
    }
}

/** Номер ряда римской цифрой: ряды древа немногочисленны. */
private fun roman(n: Int): String = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I").fold(n to "") { (rest, out), (value, sign) ->
    rest % value to out + sign.repeat(rest / value)
}.second

/** Связь узлов столбца: светится, когда верхний узел взят (свечение дышит часами декора). */
@Composable private fun Link(lit: Boolean) {
    val phase by motionPhase(LINK_PERIOD_MS)
    Canvas(Modifier.width(10.dp).height(18.dp)) {
        val x = size.width / 2
        if (lit) drawLine(Gold.copy(alpha = .18f + .14f * breath(phase)), Offset(x, 0f), Offset(x, size.height), strokeWidth = size.width, cap = StrokeCap.Round)
        drawLine(if (lit) Gold else Bronze, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    }
}

/** Мягкая волна 0..1 по фазе часов декора. */
private fun breath(phase: Float): Float = (1f - cos(phase * 2f * PI.toFloat())) / 2f

/**
 * Узел-звезда: круг с медалью узла и полосками рангов под ним, ниже имя. Взятый - в нити `Gold` с дышащим ореолом, открытый -
 * в `Brass`, закрытый «воротами» - тусклый пунктир.
 */
@Composable private fun NodeStar(node: GuildNode, ranks: Int, open: Boolean, onClick: () -> Unit) {
    val phase by motionPhase(LINK_PERIOD_MS)
    val taken = ranks > 0
    val ring = when {
        taken -> Gold
        open -> Brass
        else -> Bronze
    }
    Column(
        Modifier.width(88.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp).alpha(if (open) 1f else .4f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier.size(52.dp).drawBehind {
                val r = size.minDimension / 2
                if (taken) drawCircle(Gold.copy(alpha = .16f + .14f * breath(phase)), radius = r + 6.dp.toPx())
                drawCircle(Brush.radialGradient(listOf(PanelRaised, Panel), center, r), radius = r)
                drawCircle(
                    ring,
                    radius = r - 1.dp.toPx(),
                    style = Stroke(2.dp.toPx(), pathEffect = if (open) null else PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(node.medal, null, tint = if (taken) GoldBright else ring, modifier = Modifier.size(22.dp))
        }
        RankPips(ranks, node.max)
        Text(
            loc("guild.node.${node.code}.name"),
            color = if (taken) GoldBright else Parchment,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Полоски рангов: взятые - `Gold`, остальные - `Bronze`. */
@Composable private fun RankPips(ranks: Int, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { i -> Box(Modifier.size(width = 9.dp, height = 4.dp).background(if (i < ranks) Gold else Bronze, RoundedCornerShape(2.dp))) }
    }
}

/**
 * Лист узла: имя и ранг, ветвь и ряд, описание, эффект «сейчас → со следующим рангом» (числа правил, `perRank`), и «+1 ранг»
 * для главы - или причина, почему ранг не взять.
 */
@Composable private fun NodeSheet(node: GuildNode, rule: GuildTreeRule, guild: GuildView, bar: NodeBar?, busy: Boolean, onTake: () -> Unit, onDismiss: () -> Unit) {
    val ranks = guild.tree[node.code] ?: 0
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(node.medal, null, tint = if (ranks > 0) Gold else Brass, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(loc("guild.node.${node.code}.name") + " · $ranks/${node.max}", color = GoldBright, style = relicName(18))
                    MutedText(loc("guild.branch.${node.branch.name}") + " · " + ui("guild.tree_row", roman(node.row)), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(loc("guild.node.${node.code}.desc", listOf(number(node.perRank))), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            val now = number(node.perRank * ranks)
            Text(
                if (ranks < node.max) ui("guild.tree_effect", now, number(node.perRank * (ranks + 1))) else ui("guild.tree_effect_max", now),
                color = Vital,
                style = MaterialTheme.typography.bodySmall,
            )
            if (bar == null) {
                ForgeButton(enabled = !busy, onClick = onTake, modifier = Modifier.fillMaxWidth()) { Text(ui("guild.tree_take")) }
            } else {
                MutedText(bar.text(rule))
            }
        }
    }
}

/** Почему ранг узла не взять сейчас; подпись - словарь, число ворот - из правил. */
private enum class NodeBar(private val key: String) {
    LEADER("guild.tree_bar_leader"),
    MAX("guild.tree_bar_max"),
    ROW("guild.tree_bar_row"),
    POINTS("guild.tree_bar_points"),
    ;

    fun text(rule: GuildTreeRule): String = ui(key, rule.rowGate)
}

/** Первая причина отказа в ранге [node] - в порядке проверки [GuildTreeRule.canTake]; null - ранг взять можно. */
private fun GuildTreeRule.bar(taken: Map<String, Int>, level: Int, node: GuildNode, leader: Boolean): NodeBar? = when {
    !leader -> NodeBar.LEADER
    (taken[node.code] ?: 0) >= node.max -> NodeBar.MAX
    node.row > 1 && inBranch(taken, node.branch) < rowGate -> NodeBar.ROW
    !canTake(taken, level, node.code) -> NodeBar.POINTS
    else -> null
}

/**
 * Медали узлов древа по коду узла (4.8.0): новый узел контента - строка здесь; без строки узел рисуется знаком своей строки
 * ([Glyph.ofStat] по `effect`).
 */
private val NodeMedals: Map<String, ImageVector> = mapOf(
    "C_LIFE" to Icons.Outlined.FavoriteBorder,
    "C_DAMAGE" to ForgeGlyphs.Swords,
    "C_RESIST" to ForgeGlyphs.Kite,
    "C_FURY" to ForgeGlyphs.Banner,
    "C_BULWARK" to ForgeGlyphs.Keep,
    "L_GOLD" to ForgeGlyphs.Coins,
    "L_EXPERIENCE" to ForgeGlyphs.Tome,
    "L_RARITY" to Icons.Outlined.AutoAwesome,
    "L_QUANTITY" to ForgeGlyphs.Scales,
    "L_CHESTS" to Icons.Outlined.Key,
    "E_TABS" to ForgeGlyphs.Stash,
    "E_CRAFT" to ForgeGlyphs.Anvil,
    "E_LIMIT" to ForgeGlyphs.Aura,
    "E_TAKES" to ForgeGlyphs.Scroll,
    "E_GROWTH" to Icons.Outlined.StarOutline,
)

private val GuildNode.medal: ImageVector get() = NodeMedals[code] ?: Glyph.ofStat(effect).vector

private const val LINK_PERIOD_MS = 2400
