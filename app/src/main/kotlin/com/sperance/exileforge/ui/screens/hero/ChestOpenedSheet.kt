package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.ChestOpening
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.LootPanel
import com.sperance.exileforge.ui.components.LootRow
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.lootSums
import com.sperance.exileforge.ui.components.rememberGearVerdict
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.GoldBright

/**
 * Итог открытых сундуков-добычи: заголовок - имя сундука, у «Открыть все» - «Из сундуков: M предметов» (M - строки стопок и вещей, без
 * золота); строка «Тайник полон - не открыто: K», когда часть стопки осталась; добыча - общим обликом ([LootPanel]): вещи короткими
 * строками ([LootRow]: вердикт к надетому над ценой) по редкости, мифические и уникальные сверху, стопки ячейками, золото чипом; касание -
 * карточка стопки или вещи; затем «Забрать».
 */
@Composable fun ChestOpenedSheet(game: GameUi, opening: ChestOpening, onDismiss: () -> Unit) {
    val gear = remember(opening.equipment, game.index) {
        opening.equipment.mapNotNull { item -> game.view(item)?.let { item to it } }.sortedByDescending { (item, _) -> item.rarity.ordinal }
    }
    val inspect = rememberInspect()
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (opening.batch) {
                Text(ui(pluralKey("chest.from_all", opening.pieces), opening.pieces), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            } else {
                Text(itemTitle(opening.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                Engraved(ui("chest.inside"))
            }
            if (opening.unopened > 0) UnopenedLine(opening.unopened)
            LootPanel(game.index, gear, opening.items, lootSums(opening.gold, 0.0), onStack = { inspect(Inspect.Stack(it)) }) { (item, piece) ->
                key(item.id) { LootRow(piece, verdict = rememberGearVerdict(game, item), price = piece.sellPrice()) { inspect(Inspect.Copy(item)) } }
            }
            if (opening.gold <= 0 && opening.items.isEmpty() && opening.equipment.isEmpty()) MutedText(ui("chest.empty"))
            ForgeButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(ui("chest.take")) }
        }
    }
}

/** «Тайник полон - не открыто: K»: сундуки, что остались в стопке. */
@Composable private fun UnopenedLine(left: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.WarningAmber, null, tint = Caution, modifier = Modifier.size(16.dp))
        Text(ui("chest.unopened", left), color = Caution, style = MaterialTheme.typography.labelLarge)
    }
}
