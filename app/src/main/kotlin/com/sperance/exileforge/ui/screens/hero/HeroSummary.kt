package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.StatGroup
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

/**
 * The character's figures: life, mana and shield on top, then everything else the server counted,
 * one card per group.
 *
 * The first of the Hero tab's three sections. Nothing here is a command — it is the sheet a player
 * reads before deciding what to wear, whole and in place rather than behind another tap. Which
 * group a stat belongs to is [StatGroup]'s, so a stat nobody named still lands in «Прочее».
 */
@Composable fun HeroSummary(s: ForgeState) {
    val hero = s.play.hero ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeroVitals(s)
        if (hero.stats.isEmpty()) Text(ui("hero.no_stats"), color = Muted)
        groupedStats(hero.stats).forEach { (group, stats) -> StatGroupCard(group, stats, s) }
    }
}

private fun StatGroup.accent(): Color = when (this) {
    StatGroup.RESERVE -> LifeRed
    StatGroup.DEFENCE -> Gold
    StatGroup.RESISTANCE -> ShieldCyan
    StatGroup.ATTACK -> Ember
    StatGroup.ATTRIBUTE -> Rune
    StatGroup.OTHER -> Muted
}

/** A group: a band of its colour, its name, and its figures two to a row. */
@Composable private fun StatGroupCard(group: StatGroup, stats: List<Pair<String, Double>>, s: ForgeState) {
    val accent = group.accent()
    val shape = CutCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, accent.copy(alpha = .3f), shape)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.title(s.lang), color = accent, style = MaterialTheme.typography.labelLarge)
            stats.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (key, value) -> StatCell(key, value, accent, s, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** One figure: a small icon and the stat's name over the number the server sent. */
@Composable private fun StatCell(key: String, value: Double, accent: Color, s: ForgeState, modifier: Modifier) {
    Column(modifier.background(Abyss, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatIcon(key, accent, Modifier.size(14.dp))
            Text(statTitle(key, s.lang), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(statNumber(key, value), color = Parchment, style = MaterialTheme.typography.titleLarge)
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
