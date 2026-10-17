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
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Comparison
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.Term
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.nodeTypeTitle
import com.sperance.exileforge.core.display.partFigure
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.refusalText
import com.sperance.exileforge.core.i18n.ruleRefusal
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.allocationDelta
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.view
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
import com.sperance.exileforge.rules.sheet.StatPart
import com.sperance.exileforge.rules.sheet.StatTotal
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.expedition.factor
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Карточка узла: описание, действия, самоцвет в гнезде, «Итого». */
/**
 * The chosen node: what it gives, and the commands the server accepts for it.
 *
 * A socket is the exception, because it gives nothing by itself — what it holds is a jewel, and
 * the command there is to put one in or take it out.
 *
 * Взятый вариант читается из снимка. Узел мастерства или атрибута - один вариант при взятии; атрибут меняется за Сферу хаоса, мастерство
 * нет. Термины строк открывают свои правила. Возврат держится секунду; узел, без которого ветка оторвётся, правила не отдадут - кнопка
 * гаснет с причиной. Причина отказа взять - та же, что вернул бы сервер.
 */
@Composable internal fun NodeDetails(
    game: GameUi,
    index: ContentIndex,
    heroClass: HeroClass?,
    node: TreeNode?,
    taken: Set<String>,
    reachable: Set<String>,
    enabled: Boolean,
    available: Int,
    onClose: () -> Unit,
    onAllocate: (String, Int?) -> Unit,
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
    val start = heroClass?.startNode
    val cost = index.tree.cost(node)
    val options = node.options
    val choosing = options.isNotEmpty()
    val chosen = game.hero?.tree?.firstOrNull { it.code == node.code }?.choice
    var picked by remember(node.code) { mutableStateOf<Int?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(nodeTitle(node.code), color = nodeColour(node, true), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            MutedText(
                listOf(
                    nodeTypeTitle(node.type, game.lang),
                    "${ui("tree.cost")} $cost",
                    ui(if (allocated) "tree.taken" else "tree.not_taken"),
                ).joinToString(" · "),
            )
        }
        CloseButton(onClose)
    }
    if (node.type == SkillNodeType.JEWEL_SOCKET) {
        SocketContents(game, index, node, allocated, enabled, onSocket, onUnsocket)
    } else if (choosing) {
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
        options.forEachIndexed { at, option ->
            val on = when {
                picked != null -> at == picked
                allocated -> at == chosen
                else -> false
            }
            OptionCard(on, enabled = (!allocated && node.code in reachable) || (rechoosable && enabled), onClick = { picked = at }) {
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

    if (!allocated) AllocationChange(game, index, node, picked.takeIf { choosing })

    val terms = remember(node.code, index) { Term.ofLines(index, node.lines + node.options.flatten()) }
    TermsBlock(terms, index.campaign.combat)

    if (allocated) {
        val refusal = remember(node.code, taken) { ruleRefusal { TreeAllocation.requireRefundable(index.tree, node, taken) } }
        HoldButton(ui("tree.refund"), LifeRed, Modifier.fillMaxWidth(), enabled = enabled && refusal == null, rearm = true, millis = REFUND_HOLD_MS) { onRefund(node.code) }
        if (refusal != null && node.type != SkillNodeType.START) Text(refusal, color = LifeRed, style = MaterialTheme.typography.bodySmall)
        val branch = remember(node.code, taken) { runCatching { TreeAllocation.branch(index.tree, node, taken) }.getOrNull().orEmpty() }
        if (branch.size > 1) {
            val owned = game.bagAmount(Orb.ORB_OF_REGRET.name) ?: 0L
            HoldButton(ui("tree.refund_branch", branch.size), LifeRed, Modifier.fillMaxWidth(), enabled = enabled && owned >= branch.size, rearm = true, millis = REFUND_HOLD_MS) {
                onRefundBranch(node.code)
            }
            MutedText(ui("tree.refund_branch_note", branch.size, owned))
        }
    } else if (start == null) {
        MutedText(ui("tree.path_none"))
    } else {
        val placement = remember(node.code, taken, start) { TreeAllocation.placement(index.tree, node, taken, start) }
        if (placement == null) {
            val short = remember(node.code, taken, start, available) { TreeAllocation.refusal(index.tree, node, taken, start, available) }
            NodeActions {
                ForgeButton(
                    enabled = enabled && short == null && (!choosing || picked != null),
                    onClick = { onAllocate(node.code, picked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (short != null) ui("tree.not_enough_points") else ui("tree.allocate"))
                }
            }
            short?.let { MutedText(refusalText(it)) }
        } else {
            MutedText(refusalText(placement))
            MutedText(ui("tree.path_none"))
        }
    }
    if (node.type == SkillNodeType.START) MutedText(ui("tree.start_note"))
}

/**
 * Что станет с листом героя, если взять узел: сравнение «было → стало» тем же
 * расчётом, что сервер, сложенное вне главного потока, без запроса. У узла с выбором - по выбранному варианту [choice],
 * пока вариант не выбран - ничего; гнездо самоцвета само ничего не даёт.
 */
@Composable private fun AllocationChange(game: GameUi, index: ContentIndex, node: TreeNode, choice: Int?) {
    if (node.type == SkillNodeType.JEWEL_SOCKET || (node.options.isNotEmpty() && choice == null)) return
    val change by produceState<Comparison?>(null, node.code, choice, game.hero, index) {
        val lines = node.lines + choice?.let { node.options.getOrNull(it) }.orEmpty()
        value = withContext(Dispatchers.Default) { Comparison.of(game.allocationDelta(node.code, choice), game.index, Comparison.switchedOff(index, lines), game.hero?.figures?.values, game.hero?.level ?: 0) }
    }
    change?.let { StatComparison(ui("tree.changes"), it) }
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
 *
 * Имя гнезда - тема, а не бонус. Взятое гнездо может держать бездействующий самоцвет (вторая копия уникального) - причина листа; уникальный
 * самоцвет, уже стоящий в другом гнезде, ждёт и говорит почему.
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
    val inspect = rememberInspect()
    MutedText(ui("tree.socket_hint"))

    if (inside != null) {
        val idle = hero.inactive[inside.id]?.joinToString("\n") { requirementReason(it, game.lang) }
        game.view(inside)?.let { jewel ->
            ItemRow(
                jewel,
                note = when {
                    !allocated -> ui("tree.socket_locked")
                    idle != null -> idle
                    else -> ui("tree.socket_working")
                },
                noteColor = if (allocated && idle == null) Gold else LifeRed,
            ) { inspect(Inspect.Copy(inside)) }
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
        val single = hero.jewelFree(index, instance)
        game.view(instance)?.let { jewel ->
            ItemRow(
                jewel,
                price = game.sellPrice(instance),
                note = if (single) null else ui("tree.jewel_unique_taken"),
                noteColor = LifeRed,
            ) {
                inspect(Inspect.Copy(instance, InspectAction(ui("tree.jewel_in"), enabled = enabled && single) { onSocket(instance.id, node.code) }))
            }
        }
    }
}

internal fun nodes(n: Int) = plural("tree.node", n)

/**
 * Строка вклада дерева: одна на характеристику, части через « · » в порядке правил ([StatTotal]) - своя характеристика,
 * затем её доли (`partOf`); без условия раньше условных, внутри - ADD · INCREASED · MORE · SET.
 */
internal fun contributionText(total: StatTotal, index: ContentIndex?): String = total.parts.joinToString(PART_GLUE) { partText(total.stat, it, index) }

/**
 * Одна часть строки. Операция решает вид числа: «+120» и «+40%» к одной характеристике - разные утверждения; MORE - множитель
 * «×1.25»; SET заменяет базу и знака не несёт. Доля строки - словами [partFigure], как в сводке героя; условная
 * часть - со словами условия из словаря модификаторов.
 */
private fun partText(row: String, part: StatPart, index: ContentIndex?): String {
    val stat = part.stat
    val signed = signedNumber(part.value) { statNumber(stat, it) }
    val figure = when (part.op) {
        Op.ADD -> signed + if (statPercent(stat, index)) "%" else ""
        Op.INCREASED -> "$signed%"
        Op.MORE -> "×" + factor(1 + part.value / 100)
        Op.SET -> statValue(stat, part.value, index)
    }
    val shared = partFigure(row, stat, figure, index)
    return part.condition?.let { "$shared ${locOr(LocaleKey.condition(it), it.name)}" } ?: shared
}

private const val PART_GLUE = " · "

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

/**
 * The tree's bonuses as the rules sum them (behind «Итого» as well as in the details).
 *
 * Сумму считают правила: два «увеличения» складываются, два «больше» множатся. Это вклад дерева, не итог героя.
 */
@Composable internal fun TreeTotals(game: GameUi, tree: com.sperance.exileforge.core.model.tree.TreeState) {
    ForgePanel(accent = Rune) {
        Engraved(ui("tree.totals_title"), Rune)
        if (tree.totals.isEmpty()) Text(ui("tree.totals_empty"), color = Muted)
        tree.totals.forEach { total -> PropertyRow(statTitle(total.stat, game.lang), contributionText(total, game.index), stat = total.stat) }
    }
}

/** How long a refund of the tree is held before it goes through. */
private const val REFUND_HOLD_MS = 1_000
