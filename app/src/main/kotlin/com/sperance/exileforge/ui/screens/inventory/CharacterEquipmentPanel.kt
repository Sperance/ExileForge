package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.ui.icons.ItemEmblem
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.sperance.exileforge.core.model.command.EquipmentSlot
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable fun CharacterEquipmentPanel(s: ForgeState, onUnequip: (EquipmentSlot) -> Unit) {
    val view = s.equipmentView ?: return
    var statsExpanded by remember { mutableStateOf(false) }
    var slotsExpanded by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        s.hero?.let { hero ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ItemEmblem(com.sperance.exileforge.core.display.ItemVisualKind.CHARACTER, Gold)
                    Column { Text(hero.name, style = MaterialTheme.typography.headlineSmall); Text("Уровень ${hero.level} · Опыт ${hero.experience}"); Text("Золото: ${hero.money}") }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("maximum_life" to "HP", "maximum_mana" to "MP", "energy_shield" to "Щит").forEach { (key, title) ->
                Column { Text(title, color = Gold); Text("%.0f".format(Locale.ROOT, view.stats.values[key] ?: 0.0), style = MaterialTheme.typography.titleLarge) }
            }
        }
        if(s.inventoryVersion == null) Text("Данные могут быть устаревшими. Обновите экипировку перед следующей операцией.", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { slotsExpanded = !slotsExpanded }) { Text("Надето: ${view.equipped.size} · ${if(slotsExpanded) "свернуть" else "показать"}") }
        if(slotsExpanded) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
            EquipmentSlot.entries.forEach { slot ->
                val instance = view.inventory.firstOrNull { it.uuid == view.equipped[slot] }
                val doc = instance?.let { inventoryDocument(it, s.inventoryBases[it.equipmentId]) } ?: buildJsonObject { put("slot", when(slot) { EquipmentSlot.RING_LEFT, EquipmentSlot.RING_RIGHT -> "RING"; EquipmentSlot.MAIN_HAND -> "WEAPON_1H"; EquipmentSlot.OFF_HAND -> "SHIELD"; else -> slot.name }) }
                Card(Modifier.widthIn(min = 96.dp, max = 120.dp), colors = CardDefaults.cardColors(containerColor = if(instance == null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(slot.title, style = MaterialTheme.typography.labelMedium, color = Gold)
                        ItemEmblem(itemVisualKind(doc), if(instance == null) Muted else Gold, Modifier.size(48.dp))
                        Text(if(instance == null) "Пусто" else doc.text("name"), style = MaterialTheme.typography.bodySmall)
                        if(instance != null) TextButton(enabled = !s.busy && s.pending == null && s.inventoryVersion != null, onClick = { onUnequip(slot) }) { Text("Снять") }
                    }
                }
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
