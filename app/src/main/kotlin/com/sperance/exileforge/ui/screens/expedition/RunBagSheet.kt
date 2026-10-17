package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.presentLoot
import com.sperance.exileforge.core.campaign.run.BagEntry
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.LootKind
import com.sperance.exileforge.core.campaign.saleMarkable
import com.sperance.exileforge.core.character.view
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital
import kotlinx.coroutines.delay

/**
 * Лист «Сумки захода»: [bag] - порции новые сверху, [gold] и [experience] - итог захода, [awaiting] - событий ждут ответа.
 *
 * «Сумка захода»: вся добыча захода - бои, сундуки, объекты карты, Ваал-зона - порциями по событию, новое сверху; над списком - золото и
 * опыт захода. Данные - пришедшие ответы сервера (`RunHud.bag`), новых запросов нет; заход стоит, пока лист открыт. Подпись «N мин назад» -
 * по часам захода шагом в полминуты. Пометка к продаже удержанием - монета на строке, отклик, счёт в шапке.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RunBagSheet(game: GameUi, vm: ExpeditionViewModel, run: ExpeditionRun, bag: List<BagEntry>, gold: Long, experience: Double, awaiting: Int, onClose: () -> Unit) {
    val expedition by vm.state.collectAsStateWithLifecycle()
    var looked by remember { mutableStateOf<ItemView?>(null) }
    val inspect = rememberInspect()
    val now by produceState(run.now()) {
        while (true) {
            delay(AGE_TICK_MS)
            value = run.now()
        }
    }
    val pieces = bag.sumOf { entry -> entry.reward?.let { it.equipment.size + it.items.values.sum().toInt() } ?: 0 }
    val haptics = LocalHapticFeedback.current
    val mark = { id: String ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.toggleSaleMark(id)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        RunPanel(Modifier, GoldBright, footer = { ForgeOutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(ui("common.close")) } }) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(ui("expedition.bag.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(ui("expedition.bag.count", pieces, bag.size), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            if (expedition.saleMarks.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SaleCoin()
                    Text(ui("expedition.bag.marked", expedition.saleMarks.size), color = GoldBright, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (gold > 0) Text(ui("expedition.loot_gold", gold), color = GoldBright)
                if (experience > 0) Text(ui("expedition.loot_experience", number(experience)), color = Rune)
                Text(ui("expedition.bag.kept"), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            if (bag.isEmpty()) Text(ui("expedition.bag.empty"), color = Muted, style = MaterialTheme.typography.bodySmall)
            bag.forEachIndexed { at, entry ->
                key(entry.n) {
                    if (at > 0) HorizontalDivider(color = Bronze)
                    BagPortion(game, entry, fresh = at == 0, now = now, arriving = expedition.pending > 0, marks = expedition.saleMarks, onMark = mark, onItem = { looked = it }) { inspect(Inspect.Stack(it)) }
                }
            }
            if (awaiting > 0) Text(ui("expedition.bag.awaiting", awaiting), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
    looked?.let { item ->
        HoldsRun(run)
        LootSheet(game, vm, item, onDismiss = { looked = null })
    }
}

/** Порция одного события: «когда · откуда» (у новейшей - «новое»), вещи строками, стопки чипами. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BagPortion(game: GameUi, entry: BagEntry, fresh: Boolean, now: Long, arriving: Boolean, marks: Set<String>, onMark: (String) -> Unit, onItem: (ItemView) -> Unit, onStack: (String) -> Unit) {
    val reward = entry.reward ?: return
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOf(age(now - entry.at), fromTitle(entry)).joinToString(" · "),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (fresh) Text(ui("expedition.bag.new"), color = Vital, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        game.presentLoot(reward.equipment, arriving = arriving).forEach { (instance, presence) ->
            key(instance.id) {
                game.view(instance)?.let { piece ->
                    val marked = piece.id in marks
                    PieceLine(
                        piece,
                        presence,
                        selected = marked,
                        onLongClick = { onMark(piece.id) }.takeIf { presence.actionable && (marked || game.saleMarkable(piece.id, arriving)) },
                        trailing = if (marked) ({ SaleCoin() }) else null,
                    ) { onItem(piece) }
                }
            }
        }
        if (reward.items.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                reward.items.entries.sortedByDescending { it.value }.forEach { (code, amount) -> StackChip(game, code, amount) { onStack(code) } }
            }
        }
    }
}

/** Монета пометки к продаже. */
@Composable private fun SaleCoin() = GlyphIcon(Glyph.CURRENCY, GoldBright, Modifier.size(16.dp))

/** Откуда порция: монстр - его именем, прочее - видом события. */
private fun fromTitle(entry: BagEntry): String = entry.from.monster?.takeIf { entry.from.kind == LootKind.MONSTER }?.let(::monsterTitle) ?: ui("expedition.bag.from.${entry.from.kind.name}")

/** «только что» / «N мин назад» по часам захода. */
private fun age(millis: Long): String = (millis / MINUTE_MS).let { if (it < 1) ui("expedition.bag.just_now") else ui("expedition.bag.minutes_ago", it) }

private const val MINUTE_MS = 60_000L

/** Шаг часов подписи «N мин назад». */
private const val AGE_TICK_MS = 30_000L
