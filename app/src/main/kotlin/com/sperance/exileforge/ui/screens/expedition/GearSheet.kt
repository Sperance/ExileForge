package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.screens.hero.PetSlots

/**
 * The gear on the map (since 2.40.0): the body's ledger as the Equipment section draws it, over the walking map.
 * Только просмотр: в заходе снаряжение не меняется - ни снять, ни заменить, ни надеть из тайника или лута; надетая
 * вещь открывает свою карточку, пустое место - ничего. Сменить снаряжение можно только в убежище. Под ним - питомцы в деле
 * (3.90.2), тоже только посмотреть.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearSheet(game: GameUi, onDismiss: () -> Unit) {
    var worn by remember { mutableStateOf<String?>(null) }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("expedition.gear")) }
            item { MutedText(ui("expedition.gear_hint")) }
            item { EquipmentLedger(game) { _, id -> worn = id } }
            item { PetSlots(game, onActivate = null) }
        }
    }
    val instance = worn?.let { id -> game.hero?.item(id) } ?: return
    ForgeSheet(onDismissRequest = { worn = null }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            game.view(instance)?.let { ItemCard(it, enabled = false, detailed = true, price = game.sellPrice(instance)) }
        }
    }
}
