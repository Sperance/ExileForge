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
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * How far along this level the character is.
 *
 * A bar of its own, because experience is the one number here that really fills up: the vitals are
 * maxima the server reports and nothing says how much of one is left. The level table says what
 * the next level costs — the client reads it to show what is coming, never to work out a level,
 * which stays the server's to decide.
 *
 * At the last level there is nothing left to fill, so the bar is whole and says so rather than
 * dividing by a step that does not exist.
 */
@Composable private fun ExperiencePanel(s: ForgeState, level: Int, experience: Double) {
    val floor = s.levels.firstOrNull { it.level == level }?.experience ?: 0.0
    val next = s.levels.firstOrNull { it.level == level + 1 }
    val within = (experience - floor).coerceAtLeast(0.0)
    val span = next?.let { it.experience - floor } ?: 0.0
    val fraction = if (span > 0.0) (within / span).toFloat() else 1f
    val percent = Math.round(fraction * 100).toInt()
    ForgePanel {
        Engraved(ui("grant.experience"))
        StatBar(ui("hero.xp_short"),
            if (next == null) ui("hero.max") else "$percent%",
            Rune, fraction = fraction)
        if (next == null) Text(
            ui("hero.last_level", number(experience)),
            color = Muted, style = MaterialTheme.typography.labelSmall)
        else Text(
            ui("hero.xp_progress", number(within), number(span), level + 1, number(experience)),
            color = Muted, style = MaterialTheme.typography.labelSmall)
        // Without the table there is nothing to measure against, and a bar with no scale would lie.
        if (s.levels.isEmpty()) Text(ui("hero.no_levels"),
            color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable fun HeroEquipmentPanel(s: ForgeState, onUnequip: (String) -> Unit) {
    val hero = s.hero ?: return
    var statsOpen by remember(s.characterId) { mutableStateOf(false) }
    var slotsExpanded by remember(s.characterId) { mutableStateOf(true) }
    val equipped = hero.equipped
    // A jewel is worn in a socket on the tree, not on the body, so it has no cell in this grid —
    // the tree draws it where it actually sits.
    val bodySlots = slots.filterNot { it == "JEWEL" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ForgePanel {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemEmblem(com.sperance.exileforge.core.display.ItemVisualKind.CHARACTER, Gold, Modifier.size(64.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(hero.character.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                    // The class is the base every percentage is counted from; the server owns it.
                    Text(s.heroClass?.title.orEmpty().ifBlank { ui("hero.unknown_class") }, color = Rune, style = MaterialTheme.typography.labelLarge)
                    Text(ui("hero.level", hero.character.level),
                        color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(ui("hero.purse", hero.character.money, hero.tree.available, hero.tree.total), color = Gold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        ExperiencePanel(s, hero.character.level, hero.character.experience)
        // Life, mana and shield, exactly as the server calculated them, and the way into the rest:
        // the three vitals are what is read at a glance, the other forty are read on purpose.
        ForgePanel(modifier = Modifier.clickable { statsOpen = true }) {
            listOf(Triple("STOCK_HEALTH", ui("hero.hp"), LifeRed), Triple("STOCK_MANA", ui("hero.mp"), ManaBlue),
                Triple("STOCK_ENERGY_SHIELD", ui("hero.es"), ShieldCyan)).forEach { (key, title, color) ->
                StatBar(title, statNumber(key, hero.stats[key] ?: 0.0), color)
            }
            Text(ui("hero.all_stats", hero.stats.size),
                color = Rune, style = MaterialTheme.typography.labelMedium)
        }
        ExpandableSection(ui("hero.equipped"), equipped.size, slotsExpanded, { slotsExpanded = !slotsExpanded }) {
            // The cells share the width evenly, so no gap is left on the right, and a wide screen
            // fits a fourth one in the row — a fixed width could do neither.
            BoxWithConstraints {
                val perRow = if (maxWidth >= 480.dp) 4 else 3
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = perRow) {
                    bodySlots.forEach { slot ->
                        val instance = equipped[slot]
                        val document = instance?.let { inventoryDocument(it, s.inventoryBases[it.equipmentId]) } ?: buildJsonObject { put("slot", slot) }
                        val shape = CutCornerShape(8.dp)
                        Column(Modifier.weight(1f)
                            .background(if (instance == null) SolidColor(Panel) else panelBrush(Gold), shape)
                            .border(1.dp, if (instance == null) Bronze.copy(alpha = .4f) else Gold.copy(alpha = .55f), shape)
                            .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(slotTitle(slot, s.lang), style = MaterialTheme.typography.labelSmall, color = Gold, textAlign = TextAlign.Center)
                            ItemIcon(document, GoldBright, Modifier.size(48.dp), tint = Muted.takeIf { instance == null })
                            Text(if (instance == null) ui("hero.empty_slot") else document.text("name"), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            // An item whose requirements stopped being met keeps its slot and stops counting.
                            instance?.let { worn -> hero.inactive[worn.id]?.let { reasons ->
                                Text(ui("hero.inactive"), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                reasons.forEach { Text(requirementReason(it, s.lang), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
                            } }
                            if (instance != null) TextButton(enabled = !s.busy && (s.ownsCharacter || s.isAdmin), onClick = { onUnequip(instance.id) }) { Text(ui("hero.unequip")) }
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
                    Engraved(ui("hero.server_calc"), Rune)
                }
                Text(ui("hero.sheet_line", hero.sheet.level, hero.sheet.active.size),
                    color = Muted, style = MaterialTheme.typography.labelMedium)
                OrnateDivider(Rune)
            }
            if (hero.stats.isEmpty()) item { Text(ui("hero.no_stats"), color = Muted) }
            // Whatever the sheet carried, whole: nothing here is folded behind another tap.
            items(hero.stats.toSortedMap().toList(), key = { it.first }) { (key, value) ->
                PropertyRow(statTitle(key, s.lang), statNumber(key, value), key)
            }
        }
    }
}
