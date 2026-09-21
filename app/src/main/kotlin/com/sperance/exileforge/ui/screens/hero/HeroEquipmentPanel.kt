package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable fun HeroEquipmentPanel(s: ForgeState, onUnequip: (String) -> Unit) {
    val hero = s.hero ?: return
    var statsOpen by remember(s.characterId) { mutableStateOf(false) }
    var slotsExpanded by remember(s.characterId) { mutableStateOf(true) }
    val equipped = hero.equipped
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ForgePanel {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemEmblem(com.sperance.exileforge.core.display.ItemVisualKind.CHARACTER, Gold, Modifier.size(64.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(hero.character.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                    // The class is the base every percentage is counted from; the server owns it.
                    Text(s.heroClass?.title.orEmpty().ifBlank { tr("Класс неизвестен", "Unknown class") }, color = Rune, style = MaterialTheme.typography.labelLarge)
                    Text(tr("Уровень ${hero.character.level} · Опыт ${number(hero.character.experience)}",
                        "Level ${hero.character.level} · Experience ${number(hero.character.experience)}"), color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(tr("Золото: ${hero.character.money} · Очки дерева: ${hero.tree.available}/${hero.tree.total}",
                            "Gold: ${hero.character.money} · Tree points: ${hero.tree.available}/${hero.tree.total}"), color = Gold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        // Life, mana and shield, exactly as the server calculated them, and the way into the rest:
        // the three vitals are what is read at a glance, the other forty are read on purpose.
        ForgePanel(modifier = Modifier.clickable { statsOpen = true }) {
            listOf(Triple("STOCK_HEALTH", tr("ХП", "HP"), LifeRed), Triple("STOCK_MANA", tr("Мана", "MP"), ManaBlue),
                Triple("STOCK_ENERGY_SHIELD", tr("Щит", "ES"), ShieldCyan)).forEach { (key, title, color) ->
                StatBar(title, statNumber(key, hero.stats[key] ?: 0.0), color)
            }
            Text(tr("Все характеристики: ${hero.stats.size} · нажмите", "All stats: ${hero.stats.size} · tap"),
                color = Rune, style = MaterialTheme.typography.labelMedium)
        }
        ExpandableSection(tr("Надето", "Equipped"), equipped.size, slotsExpanded, { slotsExpanded = !slotsExpanded }) {
            // The cells share the width evenly, so no gap is left on the right, and a wide screen
            // fits a fourth one in the row — a fixed width could do neither.
            BoxWithConstraints {
                val perRow = if (maxWidth >= 480.dp) 4 else 3
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = perRow) {
                    slots.forEach { slot ->
                        val instance = equipped[slot]
                        val document = instance?.let { inventoryDocument(it, s.inventoryBases[it.equipmentId]) } ?: buildJsonObject { put("slot", slot) }
                        val shape = CutCornerShape(8.dp)
                        Column(Modifier.weight(1f)
                            .background(if (instance == null) SolidColor(Panel) else panelBrush(Gold), shape)
                            .border(1.dp, if (instance == null) Bronze.copy(alpha = .4f) else Gold.copy(alpha = .55f), shape)
                            .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(slotTitle(slot, s.lang), style = MaterialTheme.typography.labelSmall, color = Gold, textAlign = TextAlign.Center)
                            ItemIcon(document, GoldBright, Modifier.size(48.dp), tint = Muted.takeIf { instance == null })
                            Text(if (instance == null) tr("Пусто", "Empty") else document.text("name"), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            // An item whose requirements stopped being met keeps its slot and stops counting.
                            instance?.let { worn -> hero.inactive[worn.id]?.let { reasons ->
                                Text(tr("Не работает", "Not working"), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                reasons.forEach { Text(requirementReason(it, s.lang), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
                            } }
                            if (instance != null) TextButton(enabled = !s.busy && (s.ownsCharacter || s.isAdmin), onClick = { onUnequip(instance.id) }) { Text(tr("Снять", "Unequip")) }
                        }
                    }
                }
            }
        }
    }
    if (statsOpen) ModalBottomSheet(onDismissRequest = { statsOpen = false }, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.size(16.dp))
                    Engraved(tr("Расчёт сервера", "Server calculation"), Rune)
                }
                Text(tr("Уровень ${hero.sheet.level} · учтено предметов: ${hero.sheet.active.size}", "Level ${hero.sheet.level} · items counted: ${hero.sheet.active.size}"),
                    color = Muted, style = MaterialTheme.typography.labelMedium)
                OrnateDivider(Rune)
            }
            if (hero.stats.isEmpty()) item { Text(tr("Сервер не вернул характеристик", "The server returned no stats"), color = Muted) }
            // Whatever the sheet carried, whole: nothing here is folded behind another tap.
            items(hero.stats.toSortedMap().toList(), key = { it.first }) { (key, value) ->
                PropertyRow(statTitle(key, s.lang), statNumber(key, value), key)
            }
        }
    }
}
