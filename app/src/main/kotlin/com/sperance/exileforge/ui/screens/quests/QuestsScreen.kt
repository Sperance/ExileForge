package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.quests.QuestTab
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The quest board of the City: three sections — dailies, weeklies and the contract board (the story is the
 * expedition's own screen; its chapter heads the board as a line) — each a ledger of rows. The server rolls and counts; a finished quest is claimed here, a daily replaced, a notice taken off
 * the board or given up. Pulling down reads the board again.
 *
 * Глава сюжета - строкой 48dp ([StoryLine]) со своей «Сдать» у готового шага (шаг сюжета сдаётся только так, «Сдать всё» его не
 * берёт), ждущий выбор закона или книги - она же плашкой выбора; срок сброса дня и недели - справа в
 * цепочке «Город › Доска заданий» ([TrailData]), касание - пояснение, как задания засчитываются; вкладки - сегментами с числом
 * готовых к сдаче; готовых два и больше по всем вкладкам - внизу «Сдать всё» с их числом. «Сдать» - низкая пилюля, отказ и сброс - значки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen() {
    val game by koinViewModel<QuestViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<QuestViewModel>()
    val quests by vm.quests.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val current by vm.tab.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) { if (game.heroId.isNotBlank()) vm.open() }
    val board = quests.board
    val busy = activity.busy
    board?.let { ResetData(current, it) }
    Column(Modifier.fillMaxSize()) {
        StoryLine(game, board, Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp), onOpen = vm::openStory) { ClaimButton(vm, busy, it) }
        Segments(
            QuestTab.entries,
            current,
            label = { ui("quest.tab.${it.name.lowercase()}") },
            badge = { readyCount(it, board) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            onPick = vm::tab,
        )
        PullToRefreshBox(isRefreshing = Reads.QUESTS in activity.loading, onRefresh = vm::load, modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (board == null) {
                    item { MutedText(ui("quest.loading")) }
                    return@LazyColumn
                }
                when (current) {
                    QuestTab.DAILY -> daily(vm, busy, board)
                    QuestTab.WEEKLY -> weekly(vm, busy, board)
                    QuestTab.CONTRACTS -> contracts(vm, busy, board, game.index?.quests?.board?.refillHours ?: 0)
                }
            }
        }
        val ready = QuestTab.entries.sumOf { readyCount(it, board) }
        if (ready >= CLAIM_ALL_FROM) ClaimAllBar(busy, vm::claimAll)
    }
}

/**
 * Срок сброса открытой вкладки справа в цепочке: «↻ 5 ч 12 мин» дня или «↻ 3 д 4 ч» недели; касание - когда новые задания и как они
 * засчитываются. У контрактов общего срока нет - у каждого места доски свой.
 */
@Composable private fun ResetData(tab: QuestTab, board: QuestBoard) {
    val left = when (tab) {
        QuestTab.DAILY -> untilText(board.dayEndsAt)
        QuestTab.WEEKLY -> weekText(board.weekEndsAt)
        QuestTab.CONTRACTS -> return
    }
    TrailData(ui("quest.trail_reset", left), Tip(ui("quest.reset_tip.${tab.name.lowercase()}", left), ui("quest.counting_note"), Rune))
}

/**
 * Подвал «Сдать всё»: главная кнопка на всю ширину. Числа на ней нет: сервер сдаёт разом ещё готовый шаг сюжета и задания гильдии,
 * которых доска не считает, - сколько сдано, называет тост.
 */
@Composable private fun ClaimAllBar(busy: Boolean, onClaim: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 10.dp)) {
        ForgeButton(onClick = onClaim, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.Check, state = ButtonState.of(!busy)) {
            Text(ui("quest.claim_all"), maxLines = 1)
        }
    }
}

/** How many of a section's quests wait to be claimed. */
private fun readyCount(tab: QuestTab, board: QuestBoard?): Int = when (tab) {
    QuestTab.DAILY -> board?.daily
    QuestTab.WEEKLY -> board?.weekly
    QuestTab.CONTRACTS -> board?.contracts
}.orEmpty().count { it.claimedTogether }

/** Задания дня не меняются за золото: что выпало, то и задание дня. */
private fun LazyListScope.daily(vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    if (board.daily.isEmpty()) item { MutedText(ui("quest.none")) }
    items(board.daily, key = { it.id }) { quest -> QuestRow(quest) { ClaimButton(vm, busy, quest) } }
}

private fun LazyListScope.weekly(vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    if (board.weekly.isEmpty()) item { MutedText(ui("quest.none")) }
    items(board.weekly, key = { it.id }) { quest -> QuestRow(quest) { ClaimButton(vm, busy, quest) } }
}

/**
 * Контракты: «Мои контракты» с гнёздами взятых в заголовке (готовое к сдаче светится) и доска контрактов - рамка с кромкой и заклёпками,
 * листки приколоты, пустое место - пунктир с кольцом до нового листка. Руки заняты - вместо «Взять» у листков одна фишка-замок в
 * шапке доски. Невзятый листок не истекает: срок у него появится со взятия.
 */
private fun LazyListScope.contracts(vm: QuestViewModel, busy: Boolean, board: QuestBoard, refillHours: Int) {
    item(key = "taken-head") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Engraved(ui("quest.contracts_mine"), Muted, Modifier.weight(1f))
            Sockets(board.contracts, board.activeLimit)
        }
    }
    if (board.contracts.isEmpty()) item { MutedText(ui("quest.contracts_none")) }
    items(board.contracts, key = { it.id }) { quest ->
        QuestRow(quest) {
            if (quest.done) ClaimButton(vm, busy, quest) else BoardIcon(Icons.Outlined.Close, ui("quest.abandon"), ABANDON_TINT, !busy) { vm.abandon(quest.id) }
        }
    }
    item(key = "board") { ContractBoard(vm, busy, board, refillHours) }
}

/**
 * Доска контрактов: базальтовая рамка с латунной кромкой и заклёпками по углам; шапка - «Доска контрактов» и, когда руки заняты,
 * фишка-замок; листки - строки с гвоздиком, у каждого «Сбросить» и, пока есть свободные руки, «Взять». Срок нового листка - у пустого места.
 */
@Composable private fun ContractBoard(vm: QuestViewModel, busy: Boolean, board: QuestBoard, refillHours: Int) {
    val shape = RoundedCornerShape(14.dp)
    val full = board.contracts.size >= board.activeLimit
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(shape).background(Brush.verticalGradient(listOf(BOARD_TOP, BOARD_BOTTOM)), shape)
            .border(1.dp, BOARD_EDGE, shape).boardRim().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 32.dp).padding(start = 10.dp, end = 4.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Engraved(ui("quest.board"), accent = Brass, modifier = Modifier.weight(1f))
            if (full && board.offers.isNotEmpty()) LockChip(board.contracts.size, board.activeLimit)
        }
        if (board.offers.isEmpty() && board.refills.isEmpty()) MutedText(ui("quest.board_empty"), Modifier.padding(horizontal = 10.dp))
        board.offers.forEach { offer ->
            QuestRow(offer, Modifier.pinned()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoardIcon(Icons.Outlined.Refresh, ui("quest.reset"), Muted, !busy) { vm.reset(offer.id) }
                    if (!full) RowButton(ui("quest.take"), onClick = { vm.take(offer.id) }, state = ButtonState.of(!busy))
                }
            }
        }
        board.refills.forEach { at -> EmptySlot(at, refillHours) }
    }
}

/** Гнёзда взятых контрактов в заголовке: по месту на каждый предел, взятое - точкой цвета редкости, готовое к сдаче - светится. */
@Composable private fun Sockets(taken: List<Quest>, limit: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(limit.coerceAtLeast(taken.size)) { i ->
            val quest = taken.getOrNull(i)
            val ready = quest?.let { it.done && !it.claimed } == true
            val shape = RoundedCornerShape(6.dp)
            Box(
                Modifier.size(22.dp).glow(Vital, on = ready, radius = 6.dp, shape = shape).background(Abyss, shape).border(1.dp, if (ready) Vital else Bronze, shape),
                contentAlignment = Alignment.Center,
            ) { quest?.let { Box(Modifier.size(8.dp).clip(CircleShape).background(rarityColor(it.rarity.name))) } }
        }
    }
}

/** Фишка-замок шапки доски «Руки заняты»: почему у листков нет «Взять»; касание - сколько занято и как освободить руки. */
@Composable private fun LockChip(taken: Int, limit: Int) {
    val shape = RoundedCornerShape(8.dp)
    Tipped({ Tip(ui("quest.hands_full"), ui("quest.take_full", taken, limit), LOCK_TINT) }) {
        Row(
            Modifier.heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.height(26.dp).background(Abyss.copy(alpha = .65f), shape).border(1.dp, Brass.copy(alpha = .25f), shape).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Outlined.Lock, null, tint = LOCK_TINT, modifier = Modifier.size(12.dp))
                Text(ui("quest.hands_full"), color = LOCK_TINT, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

/** Пустое место доски: пунктир, кольцо - сколько прошло из [refillHours] до нового листка, и когда он ляжет. */
@Composable private fun EmptySlot(at: Long, refillHours: Int) {
    val shape = RoundedCornerShape(8.dp)
    val span = refillHours * HOUR_MS
    val share = if (span <= 0) 0f else (1f - (at - System.currentTimeMillis()).toFloat() / span).coerceIn(0f, 1f)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).dashedBorder(BOARD_EDGE, shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { share }, Modifier.fillMaxSize(), color = Gold, trackColor = Bronze, strokeWidth = 3.dp)
            Icon(ForgeGlyphs.Scroll, null, tint = Muted, modifier = Modifier.size(12.dp))
        }
        Text(ui("quest.slot_refill", untilText(at)), color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
    }
}

/** «Сдать»: малая главная кнопка строки с галочкой справа в строке задания. */
@Composable internal fun ClaimButton(vm: QuestViewModel, busy: Boolean, quest: Quest) {
    if (quest.done && !quest.claimed) RowButton(ui("quest.claim"), onClick = { vm.claim(quest.id) }, icon = Icons.Outlined.Check, state = ButtonState.of(!busy))
}

/** Значок действия строки задания (компактная иконка-кнопка): отказаться, сбросить листок. */
@Composable private fun BoardIcon(icon: ImageVector, label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    ForgeIconButton(icon, label, onClick = onClick, size = ButtonSize.Compact, state = ButtonState.of(enabled), accent = tint)
}

/** Внутренняя латунная кромка доски и заклёпки по углам. */
private fun Modifier.boardRim(): Modifier = drawBehind {
    val inset = 4.dp.toPx()
    drawRoundRect(Brass.copy(alpha = .25f), Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(11.dp.toPx()), style = Stroke(1.dp.toPx()))
    val r = 3.5.dp.toPx()
    val at = 7.dp.toPx() + r
    listOf(Offset(at, at), Offset(size.width - at, at), Offset(at, size.height - at), Offset(size.width - at, size.height - at)).forEach { c ->
        drawCircle(Brush.radialGradient(listOf(RIVET_LIGHT, Brass, RIVET_DARK), center = c - Offset(r * .3f, r * .4f), radius = r * 1.4f), r, c)
    }
}

/** Гвоздик листка доски - латунная точка в правом верхнем углу. */
private fun Modifier.pinned(): Modifier = drawWithContent {
    drawContent()
    val r = 2.5.dp.toPx()
    val c = Offset(size.width - 5.dp.toPx() - r, 5.dp.toPx() + r)
    drawCircle(Brush.radialGradient(listOf(RIVET_LIGHT, Brass), center = c - Offset(r * .3f, r * .4f), radius = r * 1.4f), r, c, alpha = .7f)
}

/** Пунктирная рамка пустого места доски. */
private fun Modifier.dashedBorder(color: Color, shape: RoundedCornerShape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline, color, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
}

/** Базальт доски: сверху светлее. */
private val BOARD_TOP = Color(0xFF141C22)
private val BOARD_BOTTOM = Color(0xFF0C1216)

/** Тёмная латунь внешней кромки доски и пунктира пустого места. */
private val BOARD_EDGE = Color(0xFF3A3326)

/** Свет и тень латунной заклёпки. */
private val RIVET_LIGHT = Color(0xFFF3D9A8)
private val RIVET_DARK = Color(0xFF3A2C18)

/** Песочный текст фишки-замка. */
private val LOCK_TINT = Color(0xFFCDB98A)

/** Приглушённый красный «Отказаться». */
private val ABANDON_TINT = Color(0xFFB06A66)

private const val HOUR_MS = 3_600_000L

/** С какого числа готовых к сдаче внизу встаёт «Сдать всё»: одно сдаётся своей кнопкой. */
private const val CLAIM_ALL_FROM = 2

/** Days and hours to a week's end — a week does not fit in the merchant's hours and minutes. */
internal fun weekText(at: Long): String {
    val hours = ((at - System.currentTimeMillis()) / 3_600_000).coerceAtLeast(0)
    return ui("quest.days_hours", hours / 24, hours % 24)
}
