package com.sperance.exileforge.ui.screens.hero

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.ChestOpening
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Parchment

/** A loot chest just opened (3.76.0): its name and everything it gave — gold, stacks, things — then «Забрать». */
@Composable fun ChestOpenedSheet(s: ForgeState, opening: ChestOpening, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(itemTitle(opening.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Engraved(ui("chest.inside"))
            if (opening.gold > 0) Line(s, null, ui("chest.gold", opening.gold))
            opening.items.forEach { (code, amount) -> Line(s, code, "${itemTitle(code)} ×$amount") }
            opening.equipment.forEach { item -> s.view(item)?.let { ItemCard(it, enabled = false, detailed = true) } }
            if (opening.gold <= 0 && opening.items.isEmpty() && opening.equipment.isEmpty()) MutedText(ui("chest.empty"))
            ForgeButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(ui("chest.take")) }
        }
    }
}

@Composable private fun Line(s: ForgeState, code: String?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        code?.let { StackIcon(s.game, it, 28) }
        Text(text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
}
