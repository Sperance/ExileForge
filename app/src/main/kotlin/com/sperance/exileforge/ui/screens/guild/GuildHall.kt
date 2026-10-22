package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildBranch
import com.sperance.exileforge.rules.content.GuildNode
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.rules.content.GuildTreeRule
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Древо бонусов гильдии: три ветви - «Бой», «Добыча», «Хозяйство»; в ветви ряд I, ряд II и вершина, ряды выше первого открывают очки,
 * вложенные в ветвь ([GuildTreeRule.gate]). Очки - за уровень гильдии, вкладывает глава; сброс возвращает все очки, бесплатно раз в
 * неделю.
 *
 * Облик - «созвездие»: герб гильдии в центре, ветви расходятся лучами, кольца - ряды, внешнее - вершины; подписей на круге нет -
 * под ним реестр ветвей со строками узлов, рангами и эффектами. Касание узла на круге или строки реестра - лист узла; сброс - лист
 * с тем, что погаснет, и удержанием.
 */
@Composable internal fun TreeTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val rule = game.index?.guilds?.tree ?: return
    val leader = me != null && GuildPolicy.can(me.role, GuildAction.TREE)
    var opened by remember { mutableStateOf<String?>(null) }
    var resetting by remember { mutableStateOf(false) }
    val free = guild.treePoints
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item(key = "points") { TreePoints(game, guild, rule, leader) { resetting = true } }
        item(key = "rules") { TreeRules(rule, leader) }
        item(key = "constellation") { Constellation(rule, guild, free.takeIf { leader } ?: 0) { opened = it.code } }
        GuildBranch.entries.forEach { branch ->
            item(key = "ledger-${branch.name}") { BranchLedger(rule, branch, guild.tree) { opened = it.code } }
        }
    }
    opened?.let(rule::node)?.let { node ->
        NodeSheet(node, rule, guild, bar = rule.bar(guild.tree, guild.level, node, leader), busy = game.busy, onTake = { vm.takeNode(node.code) }) { opened = null }
    }
    if (resetting) ResetSheet(rule, guild, busy = game.busy, onReset = vm::resetTree) { resetting = false }
}

/**
 * Шапка древа: «свободно / всего» крупно и уровень гильдии; срок «раз в N дней» и живой таймер до следующего сброса видят все участники
 * (первый сброс гильдии - сразу), кнопка сброса - только у главы и открывает лист сброса.
 */
@Composable private fun TreePoints(game: GameUi, guild: GuildView, rule: GuildTreeRule, leader: Boolean, onReset: () -> Unit) {
    val now by rememberNow(RESET_TICK_MS)
    val ready = now >= guild.respecAt
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(guild.treePoints.toString(), color = Caution, style = relicName(22))
                    MutedText(ui("guild.tree_total", rule.points(guild.level)), style = MaterialTheme.typography.labelMedium)
                }
                MutedText(ui("guild.tree_free_level", guild.level), style = MaterialTheme.typography.labelSmall)
            }
            if (leader) {
                ForgeButton(
                    onClick = onReset,
                    kind = ButtonKind.Danger,
                    state = ButtonState.of(!game.busy && ready && guild.tree.isNotEmpty()),
                ) { Text(ui("guild.tree_reset")) }
            }
        }
        Text(
            ui("guild.tree_reset_rule", rule.respecDays) + " · " + if (ready) ui("guild.tree_reset_ready") else ui("guild.tree_reset_in", resetLeft(guild.respecAt - now)),
            color = if (ready) Vital else Caution,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Правила древа с числами правил - панель [FoldPanel] под шапкой, свёрнутая при каждом входе; глава читает свою версию. */
@Composable private fun TreeRules(rule: GuildTreeRule, leader: Boolean) {
    FoldPanel(ui("guild.tree_rules")) {
        val text = if (leader) ui("guild.tree_hint_leader", rule.pointsPerLevel, rule.gate(ROW_II), rule.gate(rule.crownRow)) else ui("guild.tree_hint", rule.pointsPerLevel)
        MutedText(text, style = MaterialTheme.typography.bodySmall)
    }
}

/** Сколько ждать сброса: «6 д 23 ч 59 мин», меньше суток - без дней. */
private fun resetLeft(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    val days = minutes / (24 * 60)
    val time = ui("merchant.time", minutes / 60 % 24, minutes % 60)
    return if (days > 0) ui("guild.tree_reset_days", days, time) else time
}

/** Шаг живого таймера до сброса. */
private const val RESET_TICK_MS = 1_000L

/** Ряд II - первый ряд за «воротами». */
private const val ROW_II = 2

/** Ряд вершин ветвей - последний ряд древа. */
private val GuildTreeRule.crownRow: Int get() = nodes.maxOf { it.row }

/** Узел - вершина ветви: стоит в её последнем ряду. */
private fun GuildTreeRule.crown(node: GuildNode): Boolean = node.row == crownRow && node.row > 1

/** Состояние узла на круге и в реестре: взят, можно взять (у главы со свободными очками), открыт, закрыт «воротами». */
private enum class NodeState { TAKEN, AVAILABLE, OPEN, LOCKED }

private fun GuildTreeRule.state(taken: Map<String, Int>, node: GuildNode, free: Int): NodeState {
    val ranks = taken[node.code] ?: 0
    val open = open(taken, node)
    return when {
        ranks > 0 -> NodeState.TAKEN
        !open -> NodeState.LOCKED
        ranks < node.max && free >= node.cost -> NodeState.AVAILABLE
        else -> NodeState.OPEN
    }
}

/** Цвет ветви: бой - медь, добыча - золото, хозяйство - лёд. */
private val GuildBranch.hue: Color
    get() = when (this) {
        GuildBranch.COMBAT -> Color(0xFFE0784A)
        GuildBranch.LOOT -> Color(0xFFE8C15A)
        GuildBranch.ECONOMY -> Color(0xFF8FD3FF)
    }

/** Направление луча ветви на круге, градусы от оси x по часовой: бой - слева внизу, добыча - вверху, хозяйство - справа внизу. */
private val GuildBranch.angle: Float
    get() = when (this) {
        GuildBranch.COMBAT -> 150f
        GuildBranch.LOOT -> -90f
        GuildBranch.ECONOMY -> 30f
    }

/**
 * Раскладка «созвездия» в долях ширины круга: центр, радиусы колец рядов (ряд I, ряд II, вершины) и разлёт двух узлов ряда I от луча.
 * Узел ряда с одним узлом стоит на луче, с несколькими - веером на [SPREAD] градусов.
 */
private object Sky {
    const val WIDTH = 336f
    const val HEIGHT = 396f
    val CENTER = Offset(168f, 214f)
    val RINGS = listOf(72f, 116f, 166f)
    const val ROOT = 40f
    const val SPREAD = 48f

    fun point(angle: Float, radius: Float): Offset {
        val a = angle * PI.toFloat() / 180f
        return Offset(CENTER.x + radius * cos(a), CENTER.y + radius * sin(a))
    }

    /** Места узлов ветви [branch] на круге в единицах [WIDTH]: ряд - кольцо, узлы ряда - веером вокруг луча. */
    fun places(rule: GuildTreeRule, branch: GuildBranch): Map<GuildNode, Offset> = rule.nodes.filter { it.branch == branch }.groupBy { it.row }.flatMap { (row, nodes) ->
        val radius = RINGS[(row - 1).coerceIn(0, RINGS.lastIndex)]
        nodes.mapIndexed { i, node ->
            val shift = if (nodes.size < 2) 0f else SPREAD * (i / (nodes.size - 1f) - .5f)
            node to point(branch.angle + shift, radius)
        }
    }.toMap()
}

/**
 * «Созвездие» в панели: кольца рядов, лучи ветвей от герба к узлам ряда I, от них к ряду II и к вершине; луч светится цветом ветви,
 * когда нижний узел взят и верхний ряд открыт, к закрытому ряду - пунктир. Узел - круг с медалью (вершина - ромб), касание 44 dp;
 * над ветвью - её имя и очки в ней.
 */
@Composable private fun Constellation(rule: GuildTreeRule, guild: GuildView, free: Int, onOpen: (GuildNode) -> Unit) {
    val taken = guild.tree
    val places = remember(rule) { GuildBranch.entries.associateWith { Sky.places(rule, it) } }
    ForgePanel {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val unit = maxWidth / Sky.WIDTH
            Box(
                Modifier.fillMaxWidth().height(unit * Sky.HEIGHT).drawBehind {
                    val scale = size.width / Sky.WIDTH
                    Sky.RINGS.forEachIndexed { i, r ->
                        drawCircle(SkyRing, radius = r * scale, center = Sky.CENTER * scale, style = Stroke(1.dp.toPx(), pathEffect = if (i == Sky.RINGS.lastIndex) DOTS else null))
                    }
                    GuildBranch.entries.forEach { branch -> rays(rule, branch, places.getValue(branch), taken, scale) }
                },
            ) {
                GuildBranch.entries.forEach { branch ->
                    places.getValue(branch).forEach { (node, at) ->
                        SkyNode(node, rule.crown(node), branch.hue, rule.state(taken, node, free), Modifier.place(at, unit, NODE_TOUCH)) { onOpen(node) }
                    }
                    val label = Sky.point(branch.angle, Sky.RINGS.last()).let { if (branch == GuildBranch.LOOT) Offset(it.x, 10f) else Offset(it.x, it.y + 34f) }
                    BranchMark(branch, rule.inBranch(taken, branch), Modifier.place(Offset(label.x.coerceIn(48f, Sky.WIDTH - 48f), label.y), unit, LABEL_SIZE))
                }
                Box(Modifier.place(Sky.CENTER, unit, CREST), contentAlignment = Alignment.Center) { GuildEmblem(guild.emblem, guild.color, CREST) }
            }
        }
    }
}

private val SkyRing = Color(0xFF1E2A33)
private val SkyWire = Color(0xFF2B3A44)
private val DOTS = PathEffect.dashPathEffect(floatArrayOf(4f, 10f))
private val DASH = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
private val NODE_TOUCH = 44.dp
private val NODE_ORB = 34.dp
private val CREST = 46.dp
private val LABEL_SIZE = DpSize(96.dp, 20.dp)

/**
 * Лучи ветви: от корня у герба к узлам ряда I (горит, когда узел взят), от каждого узла ряда к узлам следующего (горит, когда нижний
 * узел взят, а верхний ряд открыт).
 */
private fun DrawScope.rays(rule: GuildTreeRule, branch: GuildBranch, places: Map<GuildNode, Offset>, taken: Map<String, Int>, scale: Float) {
    val rows = places.keys.groupBy { it.row }.toSortedMap()
    val root = Sky.point(branch.angle, Sky.ROOT)
    var below: List<Pair<Offset, Boolean>>? = null
    rows.values.forEach { nodes ->
        nodes.forEach { node ->
            val open = rule.open(taken, node)
            val at = places.getValue(node)
            val sources = below ?: listOf(root to ((taken[node.code] ?: 0) > 0))
            sources.forEach { (from, lit) -> ray(from * scale, at * scale, branch.hue, lit && open, !open) }
        }
        below = nodes.map { places.getValue(it) to ((taken[it.code] ?: 0) > 0) }
    }
}

/** Один луч: горящий - цветом ветви с ореолом, прочий - тусклой нитью, к закрытому ряду - пунктиром. */
private fun DrawScope.ray(from: Offset, to: Offset, hue: Color, lit: Boolean, locked: Boolean) {
    if (lit) drawLine(hue.copy(alpha = .25f), from, to, strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(if (lit) hue else SkyWire, from, to, strokeWidth = (if (lit) 2.5f else 1.5f).dp.toPx(), cap = StrokeCap.Round, pathEffect = if (locked) DASH else null)
}

/** Точка [at] раскладки в единицах круга - место центра элемента размером [size] при единице [unit]. */
private fun Modifier.place(at: Offset, unit: Dp, size: Dp): Modifier = place(at, unit, DpSize(size, size))

private fun Modifier.place(at: Offset, unit: Dp, size: DpSize): Modifier = offset(unit * at.x - size.width / 2, unit * at.y - size.height / 2).size(size)

/**
 * Узел на круге: круг (вершина - ромб) с медалью узла. Взятый - заливка и нить цветом ветви, доступный главе - нить цветом ветви,
 * открытый - бронза, закрытый - тусклый пунктир.
 */
@Composable private fun SkyNode(node: GuildNode, crown: Boolean, hue: Color, state: NodeState, modifier: Modifier, onClick: () -> Unit) {
    val ring = when (state) {
        NodeState.TAKEN, NodeState.AVAILABLE -> hue
        NodeState.OPEN -> Brass
        NodeState.LOCKED -> Bronze
    }
    Box(modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick).alpha(if (state == NodeState.LOCKED) .5f else 1f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(NODE_ORB).then(if (crown) Modifier.rotate(45f) else Modifier).drawBehind {
                val stroke = Stroke(1.5.dp.toPx(), pathEffect = if (state == NodeState.LOCKED) DASH else null)
                val fill = if (state == NodeState.TAKEN) Brush.radialGradient(listOf(hue.copy(alpha = .45f), Panel)) else Brush.radialGradient(listOf(PanelRaised, Panel))
                if (crown) {
                    drawRect(fill)
                    drawRect(ring, style = stroke)
                } else {
                    drawCircle(fill)
                    drawCircle(ring, radius = size.minDimension / 2 - 1.dp.toPx(), style = stroke)
                }
            },
        )
        Icon(node.medal, null, tint = if (state == NodeState.TAKEN) GoldBright else ring, modifier = Modifier.size(16.dp))
    }
}

/** Имя ветви и очки в ней над её лучом. */
@Composable private fun BranchMark(branch: GuildBranch, points: Int, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            loc("guild.branch.${branch.name}").uppercase() + " · $points",
            color = branch.hue,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
        )
    }
}

/**
 * Реестр ветви под кругом: имя ветви и очки в ней, строки узлов сверху вниз - вершина, ряд II, ряд I: медаль, имя, эффект (взятые - что
 * дают сейчас, прочие - за ранг; за «воротами» - сколько очков открывает ряд) и ранги («2/3», у вершины - «взята» или цена).
 */
@Composable private fun BranchLedger(rule: GuildTreeRule, branch: GuildBranch, taken: Map<String, Int>, onOpen: (GuildNode) -> Unit) {
    val points = rule.inBranch(taken, branch)
    ForgePanel(accent = branch.hue) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(loc("guild.branch.${branch.name}"), color = branch.hue, style = relicName(17), modifier = Modifier.weight(1f))
            MutedText(ui("guild.tree_branch_points", points), style = MaterialTheme.typography.labelSmall)
        }
        rule.nodes.filter { it.branch == branch }.sortedByDescending { it.row }.forEach { node ->
            val ranks = taken[node.code] ?: 0
            val open = rule.open(taken, node)
            val crown = rule.crown(node)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).clickable { onOpen(node) }.alpha(if (open) 1f else .55f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(if (open) node.medal else Icons.Outlined.Lock, null, tint = if (ranks > 0) branch.hue else Brass, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(loc("guild.node.${node.code}.name"), color = if (ranks > 0) GoldBright else Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MutedText(
                        when {
                            !open -> ui("guild.tree_row_gate", if (crown) ui("guild.tree_apex") else roman(node.row), rule.gate(node.row))
                            else -> loc("guild.node.${node.code}.desc", listOf(number(node.perRank * ranks.coerceAtLeast(1))))
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    when {
                        !crown -> "$ranks/${node.max}"
                        ranks > 0 -> ui("guild.tree_apex_taken")
                        else -> ui("guild.tree_cost", node.cost)
                    },
                    color = if (ranks > 0) Vital else Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Номер ряда римской цифрой: ряды древа немногочисленны. */
private fun roman(n: Int): String = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I").fold(n to "") { (rest, out), (value, sign) ->
    rest % value to out + sign.repeat(rest / value)
}.second

/**
 * Лист узла: имя и ранг, ветвь и ряд (у вершины - «вершина ветви»), описание с числом правил (`perRank`), эффект «сейчас → со
 * следующим рангом» и «Взять ранг N» («Взять вершину · цена N») для главы - или причина, почему ранг не взять.
 */
@Composable private fun NodeSheet(node: GuildNode, rule: GuildTreeRule, guild: GuildView, bar: NodeBar?, busy: Boolean, onTake: () -> Unit, onDismiss: () -> Unit) {
    val ranks = guild.tree[node.code] ?: 0
    val crown = rule.crown(node)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(node.medal, null, tint = if (ranks > 0) node.branch.hue else Brass, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(loc("guild.node.${node.code}.name") + if (node.max > 1) " · $ranks/${node.max}" else "", color = GoldBright, style = relicName(18))
                    MutedText(loc("guild.branch.${node.branch.name}") + " · " + if (crown) ui("guild.tree_apex") else ui("guild.tree_row", roman(node.row)), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(loc("guild.node.${node.code}.desc", listOf(number(node.perRank))), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            if (node.max > 1) {
                val now = number(node.perRank * ranks)
                Text(
                    if (ranks < node.max) ui("guild.tree_effect", now, number(node.perRank * (ranks + 1))) else ui("guild.tree_effect_max", now),
                    color = Vital,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (bar == null) {
                ForgeButton(state = ButtonState.of(!busy), onClick = onTake, modifier = Modifier.fillMaxWidth()) {
                    Text(if (crown) ui("guild.tree_take_apex", node.cost) else ui("guild.tree_take_rank", roman(ranks + 1)))
                }
            } else {
                MutedText(bar.text(rule, node, guild.tree))
            }
        }
    }
}

/**
 * Лист сброса древа: что вернётся (все вложенные очки) и что погаснет - строки ветвей с очками в них и отметкой вершины; сброс -
 * удержанием, как у сброса дерева героя.
 */
@Composable private fun ResetSheet(rule: GuildTreeRule, guild: GuildView, busy: Boolean, onReset: () -> Unit, onDismiss: () -> Unit) {
    val taken = guild.tree
    ConfirmSheet(
        title = ui("guild.tree_reset_q"),
        confirm = ui("guild.tree_reset"),
        onDismiss = onDismiss,
        ledger = GuildBranch.entries.mapNotNull { branch ->
            val points = rule.inBranch(taken, branch).takeIf { it > 0 } ?: return@mapNotNull null
            val crowned = rule.nodes.any { it.branch == branch && rule.crown(it) && (taken[it.code] ?: 0) > 0 }
            LedgerLine(loc("guild.branch.${branch.name}"), ui(if (crowned) "guild.tree_lost_crown" else "guild.tree_lost", points), Tone.SPEND)
        },
        note = ui("guild.tree_reset_note", rule.spent(taken), rule.respecDays),
        danger = true,
        busy = busy,
        holdMillis = Hold.RESET_MS,
        onConfirm = onReset,
    )
}

/** Почему ранг узла не взять сейчас; подпись - словарь, числа - из правил и древа. */
private enum class NodeBar(private val key: String) {
    LEADER("guild.tree_bar_leader"),
    MAX("guild.tree_bar_max"),
    ROW("guild.tree_bar_row"),
    POINTS("guild.tree_bar_points"),
    ;

    fun text(rule: GuildTreeRule, node: GuildNode, taken: Map<String, Int>): String = ui(key, rule.gate(node.row), rule.inBranch(taken, node.branch))
}

/** Первая причина отказа в ранге [node] - в порядке проверки [GuildTreeRule.canTake]; null - ранг взять можно. */
private fun GuildTreeRule.bar(taken: Map<String, Int>, level: Int, node: GuildNode, leader: Boolean): NodeBar? = when {
    !leader -> NodeBar.LEADER
    (taken[node.code] ?: 0) >= node.max -> NodeBar.MAX
    !open(taken, node) -> NodeBar.ROW
    !canTake(taken, level, node.code) -> NodeBar.POINTS
    else -> null
}

/**
 * Медали узлов древа по коду узла: новый узел контента - строка здесь; без строки узел рисуется знаком своей строки
 * ([Glyph.ofStat] по `effect`).
 */
private val NodeMedals: Map<String, ImageVector> = mapOf(
    "C_LIFE" to Icons.Outlined.FavoriteBorder,
    "C_DAMAGE" to ForgeGlyphs.Swords,
    "C_RESIST" to ForgeGlyphs.Kite,
    "C_APEX" to ForgeGlyphs.Banner,
    "L_GOLD" to ForgeGlyphs.Coins,
    "L_EXPERIENCE" to ForgeGlyphs.Tome,
    "L_RARITY" to Icons.Outlined.AutoAwesome,
    "L_APEX" to Icons.Outlined.Key,
    "E_TABS" to ForgeGlyphs.Stash,
    "E_CRAFT" to ForgeGlyphs.Anvil,
    "E_TAKES" to ForgeGlyphs.Scroll,
    "E_APEX" to ForgeGlyphs.Scales,
)

private val GuildNode.medal: ImageVector get() = NodeMedals[code] ?: Glyph.ofStat(effect).vector
