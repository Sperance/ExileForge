package com.sperance.exileforge.ui.screens.hero

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
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalLayoutApi::class)
@Composable fun HeroEquipmentPanel(s: ForgeState, onUnequip: (String) -> Unit) {
    val hero = s.hero ?: return
    var statsExpanded by remember { mutableStateOf(false) }
    var slotsExpanded by remember { mutableStateOf(true) }
    val equipped = hero.equipped
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ForgePanel {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemEmblem(com.sperance.exileforge.core.display.ItemVisualKind.CHARACTER, Gold, Modifier.size(64.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(hero.character.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                    Text(tr("Уровень ${hero.character.level} · Опыт ${hero.character.experience}", "Level ${hero.character.level} · Experience ${hero.character.experience}"), color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(tr("Золото: ${hero.character.money}", "Gold: ${hero.character.money}"), color = Gold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        // Life, mana and shield globes, exactly as the server calculated them.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Triple("STOCK_HEALTH", "HP", LifeRed), Triple("STOCK_MANA", "MP", ManaBlue),
                Triple("STOCK_ENERGY_SHIELD", tr("Щит", "ES"), ShieldCyan)).forEach { (key, title, color) ->
                StatGlobe(title, "%.0f".format(Locale.ROOT, hero.stats[key] ?: 0.0), 1f, color)
            }
        }
        TextButton(onClick = { slotsExpanded = !slotsExpanded }) {
            Text(tr("Надето: ${equipped.size} · ${if (slotsExpanded) "свернуть" else "показать"}", "Equipped: ${equipped.size} · ${if (slotsExpanded) "hide" else "show"}"))
        }
        if (slotsExpanded) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
            slots.forEach { slot ->
                val instance = equipped[slot]
                val document = instance?.let { inventoryDocument(it, s.inventoryBases[it.equipmentId]) } ?: buildJsonObject { put("slot", slot) }
                val shape = CutCornerShape(8.dp)
                Column(Modifier.widthIn(min = 96.dp, max = 120.dp)
                    .background(if (instance == null) SolidColor(Panel) else panelBrush(Gold), shape)
                    .border(1.dp, if (instance == null) Bronze.copy(alpha = .4f) else Gold.copy(alpha = .55f), shape)
                    .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(slotTitle(slot, s.lang), style = MaterialTheme.typography.labelSmall, color = Gold, textAlign = TextAlign.Center)
                    ItemIcon(document, GoldBright, Modifier.size(48.dp), tint = Muted.takeIf { instance == null })
                    Text(if (instance == null) tr("Пусто", "Empty") else document.text("name"), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    if (instance != null) TextButton(enabled = !s.busy && (s.ownsCharacter || s.isAdmin), onClick = { onUnequip(instance.id) }) { Text(tr("Снять", "Unequip")) }
                }
            }
        }
        TextButton(onClick = { statsExpanded = !statsExpanded }) {
            Text(tr("Характеристики · ${if (statsExpanded) "свернуть" else "показать"}", "Stats · ${if (statsExpanded) "hide" else "show"}"))
        }
        if (statsExpanded) ForgePanel(accent = Rune) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.size(16.dp))
                Engraved(tr("Расчёт сервера", "Server calculation"), Rune)
            }
            if (hero.stats.isEmpty()) Text(tr("Сервер не вернул характеристик", "The server returned no stats"), color = Muted)
            hero.stats.toSortedMap().forEach { (key, value) ->
                PropertyRow(statTitle(key, s.lang), String.format(Locale.ROOT, "%.1f", value), key)
            }
        }
    }
}
