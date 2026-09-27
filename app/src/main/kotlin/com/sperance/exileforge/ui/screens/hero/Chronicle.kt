package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.Achievement
import com.sperance.exileforge.rules.content.Counter
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** A title as the dictionary names it; its code while the dictionary has not come. */
fun titleName(code: String): String = locOr("title.$code", code)

/** The three steps of an achievement: bronze, silver, gold — a single one-off step is gold. */
private val Medals = listOf(Color(0xFFC08457), Color(0xFFC9D1D9), Color(0xFFFFD166))

/**
 * The chronicle (3.3.0, server 1.3.0): the hero's title, and a way into all they have done — the
 * counters by section, the achievements with their steps and the titles they open. Only what the
 * server counts is counted; the level, the zones and the atlas are read off the hero.
 */
@Composable fun ChronicleCard(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.hero ?: return
    val achievements = s.index?.achievements ?: return
    var open by remember { mutableStateOf(false) }
    val values = hero.chronicle
    val done = achievements.achievements.count { it.complete(values[it.counter] ?: 0L) }
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Engraved(ui("chronicle.title"))
                Text(hero.info.title.takeIf { it.isNotBlank() }?.let(::titleName) ?: ui("chronicle.no_title"),
                    color = if (hero.info.title.isNotBlank()) GoldBright else Muted, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("chronicle.done", done, achievements.achievements.size))
            }
            ForgeOutlinedButton(onClick = { open = true }) { Text(ui("chronicle.open")) }
        }
    }
    if (open) ChronicleSheet(s, vm, values) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable private fun ChronicleSheet(s: ForgeState, vm: ForgeViewModel, values: Map<String, Long>, onDismiss: () -> Unit) {
    val hero = s.hero ?: return
    val achievements = s.index?.achievements ?: return
    val titles = achievements.titles(values)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("chronicle.titles"))
            if (titles.isEmpty()) MutedText(ui("chronicle.no_titles"))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = hero.info.title.isBlank(), enabled = !s.busy, onClick = { vm.setTitle("") }, label = { Text(ui("chronicle.no_title")) })
                titles.forEach { code ->
                    FilterChip(selected = hero.info.title == code, enabled = !s.busy, onClick = { vm.setTitle(code) }, label = { Text(titleName(code)) })
                }
            }
            Engraved(ui("chronicle.achievements"))
            achievements.achievements.forEach { AchievementRow(it, values[it.counter] ?: 0L) }
            Counter.SECTIONS.forEach { (section, counters) ->
                Engraved(ui("chronicle.section.$section"))
                counters.forEach { counter ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(ui("chronicle.counter.$counter"), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(number((values[counter] ?: 0L).toDouble()), color = GoldBright, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** An achievement: its name, what the next step asks and how far it is, and a medal for each step taken. */
@Composable private fun AchievementRow(achievement: Achievement, value: Long) {
    val reached = achievement.reached(value)
    val next = achievement.tiers.getOrNull(reached)
    val target = next ?: achievement.tiers.last()
    Column(Modifier.fillMaxWidth().background(PanelRaised, MaterialTheme.shapes.small).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(loc("achievement.${achievement.code}.name"), color = if (next == null) GoldBright else Parchment, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f))
            val medals = if (achievement.tiers.size == 1) listOf(Medals.last()) else Medals.take(achievement.tiers.size)
            medals.forEachIndexed { i, medal ->
                Box(Modifier.size(12.dp).background(if (i < reached) medal else medal.copy(alpha = .18f), CircleShape))
            }
        }
        MutedText(loc("achievement.${achievement.code}.desc", listOf(number(target.toDouble()))))
        LinearProgressIndicator(progress = { (value.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
            color = if (next == null) GoldBright else Gold, trackColor = Abyss)
        Text(ui("chronicle.progress", number(value.coerceAtMost(target).toDouble()), number(target.toDouble())) +
            (achievement.title.takeIf { it.isNotBlank() }?.let { " · " + ui("chronicle.opens", titleName(it)) } ?: ""),
            color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}
