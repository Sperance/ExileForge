package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.EntitySpinner
import com.sperance.exileforge.ui.components.Spinner
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune
import kotlinx.serialization.json.*

@Composable fun InventoryForge(s: ForgeState, vm: ForgeViewModel) {
    var confirmCurrency by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Экипировка персонажа", style = MaterialTheme.typography.titleLarge)
            if(!s.signedIn) Text("Для выпадения и крафта войдите во вкладке «Сервер».")
            EntitySpinner("Персонаж", s.characterId, EntitySource.CHARACTER, !s.busy && s.pending == null, vm::characterId)
            OutlinedButton(enabled = !s.busy && s.signedIn, onClick = vm::loadInventory) { Text("Загрузить инвентарь") }
            Button(enabled = !s.busy && s.signedIn && s.inventoryVersion != null && s.pending == null, onClick = { vm.inventoryAction("drop") }) { Text("Получить случайный предмет") }
            Text("Тестовое выпадение доступно администратору для собственного персонажа.", color = Muted)
            Spinner("Экземпляр", s.selectedEquipment, s.inventory.associate { item ->
                val poe = item["poe"] as? JsonObject
                item.text("uuid") to "${poe?.text("rarity").orEmpty()} ${poe?.text("baseId") ?: item.text("equipmentId")} · ${item.text("uuid")}" 
            }, !s.busy, vm::selectEquipment)
            val item = s.inventory.firstOrNull { it.text("uuid") == s.selectedEquipment }
            val poe = item?.get("poe") as? JsonObject
            if(poe != null) {
                Text("Уровень ${poe.text("itemLevel")} · качество ${poe.text("quality")}")
                listOf("implicits", "explicits").forEach { key ->
                    (poe[key] as? JsonArray).orEmpty().forEach { raw ->
                        val roll = raw.jsonObject
                        Text("${roll.text("id")} · v${roll.text("revision").ifBlank { "1" }}: ${(roll["values"] as? JsonArray).orEmpty().joinToString { it.jsonPrimitive.content }}${if(roll.text("fractured") == "true") " · fractured" else ""}", color = Rune)
                    }
                }
            } else if(item != null) Text("Старый экземпляр без состояния PoE: крафт сфер недоступен.")
            Spinner("Сфера", s.selectedCurrency, s.currencies.associate { it.text("id") to it.text("name") }, !s.busy, vm::selectCurrency)
            Button(enabled = !s.busy && s.signedIn && poe != null && s.selectedCurrency.isNotBlank() && s.inventoryVersion != null && s.pending == null, onClick = { confirmCurrency = true }) { Text("Применить сферу") }
            if(s.pending != null) {
                Text("Результат запроса не подтверждён. Повторите тот же запрос; сервер защитит от повторного списания.")
                OutlinedButton(enabled = !s.busy && s.signedIn, onClick = vm::retryInventoryAction) { Text("Повторить запрос") }
            }
        }
    }
    if(confirmCurrency) AlertDialog(onDismissRequest = { confirmCurrency = false }, title = { Text("Применить сферу?") }, text = { Text("Будет потрачена одна ${s.selectedCurrency} на выбранный экземпляр. Модификаторы могут измениться или удалиться.") }, confirmButton = { TextButton(onClick = { confirmCurrency = false; vm.inventoryAction("craft") }) { Text("Применить") } }, dismissButton = { TextButton(onClick = { confirmCurrency = false }) { Text("Отмена") } })
}
