package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.ItemIcon
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        s.hero?.let { hero ->
            ForgePanel {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ForgeIcon("ui-character", Modifier.size(64.dp), description = hero.name) {
                        ItemEmblem(com.sperance.exileforge.core.display.ItemVisualKind.CHARACTER, Gold, Modifier.size(64.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(hero.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                        Text(tr("Уровень ${hero.level} · Опыт ${hero.experience}", "Level ${hero.level} · Experience ${hero.experience}"), color = Muted, style = MaterialTheme.typography.labelMedium)
                        Text(tr("Золото: ${hero.money}", "Gold: ${hero.money}"), color = Gold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        // Life, mana and shield globes, exactly as the server calculated them.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Triple("maximum_life", "HP", LifeRed), Triple("maximum_mana", "MP", ManaBlue),
                Triple("energy_shield", tr("Щит", "ES"), ShieldCyan)).forEach { (key, title, color) ->
                StatGlobe(title, "%.0f".format(Locale.ROOT, view.stats.values[key] ?: 0.0), 1f, color)
            }
        }
        if(s.inventoryVersion == null) Text(tr("Данные могут быть устаревшими. Обновите экипировку перед следующей операцией.", "The data may be stale. Refresh the equipment before the next operation."), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { slotsExpanded = !slotsExpanded }) {
            Text(tr("Надето: ${view.equipped.size} · ${if(slotsExpanded) "свернуть" else "показать"}", "Equipped: ${view.equipped.size} · ${if(slotsExpanded) "hide" else "show"}"))
        }
        if(slotsExpanded) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
            EquipmentSlot.entries.forEach { slot ->
                val instance = view.inventory.firstOrNull { it.uuid == view.equipped[slot] }
                val doc = instance?.let { inventoryDocument(it, s.inventoryBases[it.equipmentId]) } ?: buildJsonObject { put("slot", when(slot) { EquipmentSlot.RING_LEFT, EquipmentSlot.RING_RIGHT -> "RING"; EquipmentSlot.MAIN_HAND -> "WEAPON_1H"; EquipmentSlot.OFF_HAND -> "SHIELD"; else -> slot.name }) }
                val shape = CutCornerShape(8.dp)
                Column(Modifier.widthIn(min = 96.dp, max = 120.dp)
                    .background(if(instance == null) SolidColor(Panel) else panelBrush(Gold), shape)
                    .border(1.dp, if(instance == null) Bronze.copy(alpha = .4f) else Gold.copy(alpha = .55f), shape)
                    .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(slot.title(s.lang), style = MaterialTheme.typography.labelSmall, color = Gold, textAlign = TextAlign.Center)
                    // An empty slot still has an icon: the binding tables answer for the slot itself, muted.
                    ItemIcon(doc, GoldBright, Modifier.size(48.dp), tint = Muted.takeIf { instance == null })
                    Text(if(instance == null) tr("Пусто", "Empty") else doc.text("name"), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    if(instance != null) TextButton(enabled = !s.busy && s.pending == null && s.inventoryVersion != null, onClick = { onUnequip(slot) }) { Text(tr("Снять", "Unequip")) }
                }
            }
        }
        TextButton(onClick = { statsExpanded = !statsExpanded }) {
            Text(tr("Характеристики · ${if(statsExpanded) "свернуть" else "показать"}", "Stats · ${if(statsExpanded) "hide" else "show"}"))
        }
        if(statsExpanded) {
            ForgePanel(accent = Rune) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.size(16.dp))
                    Engraved(tr("Расчёт сервера", "Server calculation"), Rune)
                }
                view.stats.values.forEach { (key, value) ->
                    PropertyRow(statTitle(key), String.format(Locale.ROOT, "%.2f", value), key)
                }
            }
            view.stats.weapons.forEach { (slot, weapon) ->
                InfoCard(slot.title(s.lang), tr("Урон: %.1f–%.1f\nАтак/с: %.2f · DPS: %.1f\nКрит: %.1f%% · Точность: %.0f", "Damage: %.1f–%.1f\nAttacks/s: %.2f · DPS: %.1f\nCrit: %.1f%% · Accuracy: %.0f")
                    .format(Locale.ROOT, weapon.minimumPhysical, weapon.maximumPhysical, weapon.attacksPerSecond, weapon.dps, weapon.criticalChance, weapon.accuracy))
            }
            if(view.stats.unsupported.isNotEmpty()) InfoCard(tr("Не учтено расчётом", "Not covered by the calculation"), view.stats.unsupported.joinToString("\n"))
        }
    }
}
internal fun statTitle(key: String): String = when(key) {
    "maximum_life" -> tr("Здоровье", "Life"); "maximum_mana" -> tr("Мана", "Mana")
    "strength" -> tr("Сила", "Strength"); "dexterity" -> tr("Ловкость", "Dexterity"); "intelligence" -> tr("Интеллект", "Intelligence")
    "armour" -> tr("Броня", "Armour"); "evasion" -> tr("Уклонение", "Evasion"); "energy_shield" -> tr("Энергетический щит", "Energy shield")
    "accuracy" -> tr("Точность", "Accuracy")
    "life_regeneration" -> tr("Регенерация здоровья / с", "Life regeneration / s"); "mana_regeneration" -> tr("Регенерация маны / с", "Mana regeneration / s")
    "movement_speed" -> tr("Множитель скорости движения", "Movement speed multiplier")
    "fire_resistance" -> tr("Сопротивление огню, %", "Fire resistance, %"); "cold_resistance" -> tr("Сопротивление холоду, %", "Cold resistance, %")
    "lightning_resistance" -> tr("Сопротивление молнии, %", "Lightning resistance, %"); "chaos_resistance" -> tr("Сопротивление хаосу, %", "Chaos resistance, %")
    else -> key.replace('_', ' ')
}
