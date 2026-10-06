package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.upgrades
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.MutedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * «Сменить снаряжение» на карте (3.89.0): всё из тайника, что герой может надеть, улучшения первыми по сумме изменений
 * урона и защиты; у каждой вещи стрелки и «Надеть» одним нажатием - больше в листе ничего нет. Карта стоит, пока он открыт
 * (держит вызывающий); сменённая вещь действует со следующего боя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WardrobeSheet(game: GameUi, vm: ExpeditionViewModel, onDismiss: () -> Unit) {
    // Листы с каждой вещью складываются вне главного потока; до ответа список пуст, а не замер.
    val upgrades by produceState<List<Pair<ItemInstance, GearVerdict>>?>(null, game.hero, game.index) {
        value = withContext(Dispatchers.Default) { game.upgrades() }
    }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Engraved(ui("expedition.wardrobe")) }
            val list = upgrades ?: return@LazyColumn
            if (list.isEmpty()) item { MutedText(ui("expedition.wardrobe_empty")) }
            items(list, key = { it.first.id }) { (instance, verdict) ->
                game.view(instance)?.let { piece ->
                    ItemRow(piece, enabled = false, price = game.sellPrice(instance), verdict = verdict, footer = {
                        ForgeButton(enabled = !game.busy, onClick = { vm.equip(instance.id) }, modifier = Modifier.fillMaxWidth()) { Text(ui("hero.equip")) }
                    }) { }
                }
            }
        }
    }
}
