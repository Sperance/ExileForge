package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The character's figures: life, mana and shield, and everything else the server counted.
 *
 * The first of the Hero tab's three sections. Nothing here is a command — it is the sheet a player
 * reads before deciding what to wear. Who the character is lives in [HeroHeader] above the sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HeroSummary(s: ForgeState) {
    val hero = s.play.hero ?: return
    var statsOpen by remember(s.play.characterId) { mutableStateOf(false) }
    ForgePanel(modifier = Modifier.clickable { statsOpen = true }) {
        HeroVitals(s)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ui("hero.all_stats", hero.stats.size), color = Rune, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, null, tint = Rune, modifier = Modifier.size(18.dp))
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
                Text(ui("hero.sheet_line", hero.sheet.level, hero.sheet.active.size), color = Muted, style = MaterialTheme.typography.labelMedium)
                OrnateDivider(Rune)
            }
            if (hero.stats.isEmpty()) item { Text(ui("hero.no_stats"), color = Muted) }
            // Whatever the sheet carried, whole: nothing here is folded behind another tap.
            items(hero.stats.toSortedMap().toList(), key = { it.first }) { (key, value) ->
                PropertyRow(statTitle(key, s.lang), statNumber(key, value), stat = key)
            }
        }
    }
}

/**
 * Life, mana and shield as figures, not bars: the sheet carries a maximum and no current value, so
 * a bar could only ever be full — and a full bar for a mana of 0 said the opposite.
 */
@Composable fun HeroVitals(s: ForgeState) {
    val hero = s.play.hero ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("STOCK_HEALTH", ui("hero.hp"), LifeRed), Triple("STOCK_MANA", ui("hero.mp"), ManaBlue),
            Triple("STOCK_ENERGY_SHIELD", ui("hero.es"), ShieldCyan)).forEach { (key, title, color) ->
            VitalTile(title, statNumber(key, hero.stats[key] ?: 0.0), color, Modifier.weight(1f))
        }
    }
}

/** One vital: its name in its own colour over the figure the server sent. */
@Composable private fun VitalTile(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(6.dp)
    Column(modifier.background(Color.Black.copy(alpha = .22f), shape).border(1.dp, color.copy(alpha = .45f), shape)
        .padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = color, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Parchment, style = MaterialTheme.typography.headlineSmall)
    }
}
