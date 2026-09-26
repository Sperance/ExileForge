package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.screens.hero.StatSheet
import com.sperance.exileforge.ui.theme.*

/**
 * The hero's figures over a map (2.75.0), in two tabs at the foot as the gear's window has them:
 * the hero's own sheet, and the same sheet under the map's and the atlas's effects — [mapEffects],
 * what the run was started with — each changed figure lit beside what it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StatsSheet(s: ForgeState, mapEffects: Map<String, Double>, onDismiss: () -> Unit) {
    val own = s.play.hero?.stats.orEmpty()
    val onMap = remember(own, mapEffects) { MapEffects.hero(own, mapEffects) }
    val changed = remember(own, onMap) { onMap.count { (key, value) -> (own[key] ?: 0.0) != value } }
    var mapTab by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Engraved(ui(if (mapTab) "expedition.stats_map" else "expedition.stats_hero")) }
                item { MutedText(if (!mapTab) ui("expedition.stats_hero_hint") else if (changed == 0) ui("expedition.stats_map_none") else ui("expedition.stats_map_hint", changed)) }
                item { if (mapTab) StatSheet(s, onMap, before = own) else StatSheet(s, own) }
            }
            TabRow(selectedTabIndex = if (mapTab) 1 else 0, containerColor = Abyss, contentColor = Gold) {
                Tab(selected = !mapTab, onClick = { mapTab = false }, text = { Text(ui("expedition.stats_hero")) })
                Tab(selected = mapTab, onClick = { mapTab = true }, text = { Text(ui("expedition.stats_map")) })
            }
        }
    }
}
