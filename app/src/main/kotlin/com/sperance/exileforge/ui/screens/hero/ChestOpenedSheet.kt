package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.ChestOpening
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Parchment

/** A loot chest just opened: its name and everything it gave — gold, stacks, things — then «Забрать». */
@Composable fun ChestOpenedSheet(game: GameUi, opening: ChestOpening, onDismiss: () -> Unit) {
    val gear = remember(opening.equipment, game.index) { opening.equipment.mapNotNull { item -> game.view(item)?.let { item to it } } }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(itemTitle(opening.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Engraved(ui("chest.inside"))
            if (opening.gold > 0) Line(game, null, ui("chest.gold", opening.gold))
            opening.items.forEach { (code, amount) -> Line(game, code, "${itemTitle(code)} ×$amount") }
            gear.forEach { (item, piece) -> ItemCard(piece, enabled = false, detailed = true, seals = game.itemSeals(item)) }
            if (opening.gold <= 0 && opening.items.isEmpty() && opening.equipment.isEmpty()) MutedText(ui("chest.empty"))
            ForgeButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(ui("chest.take")) }
        }
    }
}

/** Строка добычи сундука; у стопки касание открывает её карточку. */
@Composable private fun Line(game: GameUi, code: String?, text: String) {
    val inspect = rememberInspect()
    Row(code?.let { Modifier.clickable { inspect(Inspect.Stack(it)) } } ?: Modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        code?.let { StackIcon(game, it, 28) }
        Text(text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
}
