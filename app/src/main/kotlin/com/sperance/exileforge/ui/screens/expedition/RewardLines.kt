package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.expedition.presentLoot
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * What the server rolled - experience, gold, orbs and items - for a kill and a chest alike, in the one look of loot ([LootPanel]): items
 * as short rows ([LootRow]), stacks as cells, gold and experience as chips. [awaiting]: some of it is still on its way, and the sheet
 * says so instead of «nothing else». With [onItem] a card opens its comparison with what is worn. Вещь показывается по её положению у
 * героя ([presentLoot]): проданной нет, надетая - с меткой «Надето»; [arriving] - журнал ещё несёт ответы, и вещи, которой нет у героя,
 * ждут, а не пропадают. Вещь лучше надетого - с меткой «▲ Лучше надетого» ([lootMark]) - по набору похода, когда он идёт
 * ([rememberRunGame]). Без [onItem] вещь и стопка открывают общую карточку предмета.
 *
 * Без нелокальных return из составных лямбд: D8 не переводит их в dex. Сравнение - с копией героя, когда она уже у него.
 */
@Composable internal fun RewardLines(game: GameUi, reward: Reward, onItem: ((ItemView) -> Unit)? = null, awaiting: Boolean = false, arriving: Boolean = awaiting) {
    val gear = game.presentLoot(reward.equipment, arriving = awaiting || arriving).mapNotNull { (instance, presence) -> game.view(instance)?.let { it to presence } }
    val inspect = rememberInspect()
    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LootPanel(game.index, gear, reward.items, lootSums(reward.gold, reward.experience), onStack = { inspect(Inspect.Stack(it)) }) { (piece, presence) ->
            key(piece.id) {
                if (presence.worn) {
                    WornLootRow(piece)
                } else {
                    val (verdict, mark) = rememberLootVerdict(game, piece.item, flasks = false)
                    LootRow(piece, verdict = verdict, price = piece.sellPrice(), note = mark) {
                        if (onItem != null) onItem(piece) else inspect(Inspect.Copy(piece.item))
                    }
                }
            }
        }
        if (awaiting) {
            Receiving()
        } else if (reward.items.isEmpty() && gear.isEmpty()) {
            Text(ui("expedition.loot_nothing"), color = Muted)
        }
    }
}

/** The loot on its way: the server alone rolls it, and it shows as its answers arrive — offline, once the connection is back. */
@Composable internal fun Receiving(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(Modifier.size(12.dp), color = Rune, strokeWidth = 2.dp)
        Text(ui("expedition.receiving"), color = Rune, style = MaterialTheme.typography.bodySmall)
    }
}

/** Надетая вещь лута: короткая строка с отметкой надетого, без цены и действий - во всех списках лута одна; касание - карточка. */
@Composable internal fun WornLootRow(piece: ItemView) {
    val inspect = rememberInspect()
    LootRow(piece, worn = true) { inspect(Inspect.Copy(piece.item)) }
}
