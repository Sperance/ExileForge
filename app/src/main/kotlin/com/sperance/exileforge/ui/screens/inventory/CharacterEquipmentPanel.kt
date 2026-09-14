package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.model.command.EquipmentSlot
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import java.util.Locale

@Composable fun CharacterEquipmentPanel(s: ForgeState, onUnequip: (EquipmentSlot) -> Unit) {
    val view = s.equipmentView ?: return
    var statsExpanded by remember { mutableStateOf(false) }
    var slotsExpanded by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if(s.inventoryVersion == null) Text("Данные могут быть устаревшими. Обновите экипировку перед следующей операцией.", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { slotsExpanded = !slotsExpanded }) { Text("Надето: ${view.equipped.size} · ${if(slotsExpanded) "свернуть" else "показать"}") }
        if(slotsExpanded) EquipmentSlot.entries.forEach { slot ->
            val instance = view.inventory.firstOrNull { it.text("uuid") == view.equipped[slot] }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(slot.title, color = Gold, style = MaterialTheme.typography.labelLarge)
                    Text(instance?.let { inventoryDocument(it, s.inventoryBases[it.text("equipmentId")]).text("name") } ?: "Пусто", color = Muted)
                }
                if(instance != null) OutlinedButton(enabled = !s.busy && s.pending == null && s.inventoryVersion != null, onClick = { onUnequip(slot) }) { Text("Снять") }
            }
        }
        TextButton(onClick = { statsExpanded = !statsExpanded }) { Text("Характеристики · ${if(statsExpanded) "свернуть" else "показать"}") }
        if(statsExpanded) {
            view.stats.values.forEach { (key, value) ->
                PropertyRow(statTitle(key), String.format(Locale.ROOT, "%.2f", value), key)
            }
            view.stats.weapons.forEach { (slot, weapon) ->
                InfoCard(slot.title, "Урон: %.1f–%.1f\nАтак/с: %.2f · DPS: %.1f\nКрит: %.1f%% · Точность: %.0f".format(Locale.ROOT, weapon.minimumPhysical, weapon.maximumPhysical, weapon.attacksPerSecond, weapon.dps, weapon.criticalChance, weapon.accuracy))
            }
            if(view.stats.unsupported.isNotEmpty()) InfoCard("Не учтено расчётом", view.stats.unsupported.joinToString("\n"))
        }
    }
}
private fun statTitle(key: String): String = when(key) {
    "maximum_life" -> "Здоровье"; "maximum_mana" -> "Мана"; "strength" -> "Сила"; "dexterity" -> "Ловкость"; "intelligence" -> "Интеллект"
    "armour" -> "Броня"; "evasion" -> "Уклонение"; "energy_shield" -> "Энергетический щит"; "accuracy" -> "Точность"
    "life_regeneration" -> "Регенерация здоровья / с"; "mana_regeneration" -> "Регенерация маны / с"; "movement_speed" -> "Множитель скорости движения"
    "fire_resistance" -> "Сопротивление огню, %"; "cold_resistance" -> "Сопротивление холоду, %"; "lightning_resistance" -> "Сопротивление молнии, %"; "chaos_resistance" -> "Сопротивление хаосу, %"
    else -> key.replace('_', ' ')
}
