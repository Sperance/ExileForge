package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.StatGroup
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

/**
 * The character's figures: life and shield on top, then everything else the sheet counts, one
 * card per group — since 2.48.0 a figure is one short line, so the whole sheet fits a screen or two.
 *
 * The first of the Hero tab's three sections. Nothing here is a command — it is the sheet a player
 * reads before deciding what to wear, whole and in place rather than behind another tap. Which
 * group a stat belongs to is [StatGroup]'s, so a stat nobody named still lands in «Прочее».
 */
@Composable fun HeroSummary(s: ForgeState) {
    val hero = s.play.hero ?: return
    StatSheet(s, hero.stats)
}

/**
 * A sheet of figures (2.75.0: apart from the hero, so the map's window draws the same one). Given
 * [before], a figure that differs from it is lit and says what it was — the sheet under a map's
 * effects held against the hero's own.
 */
@Composable fun StatSheet(s: ForgeState, stats: Map<String, Double>, before: Map<String, Double>? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HeroVitals(stats)
        if (stats.isEmpty()) Text(ui("hero.no_stats"), color = Muted)
        groupedStats(stats).forEach { (group, figures) -> StatGroupCard(group, figures, s, before) }
    }
}

private fun StatGroup.accent(): Color = when (this) {
    StatGroup.RESERVE -> LifeRed
    StatGroup.DEFENCE -> Gold
    StatGroup.RESISTANCE -> ShieldCyan
    StatGroup.ATTACK -> Ember
    StatGroup.AILMENT -> Vital
    StatGroup.ATTRIBUTE -> Rune
    StatGroup.OTHER -> Muted
}

/** A group: a band of its colour, its name, and its figures two to a row. */
@Composable private fun StatGroupCard(group: StatGroup, stats: List<Pair<String, Double>>, s: ForgeState, before: Map<String, Double>?) {
    val accent = group.accent()
    val shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, accent.copy(alpha = .3f), shape)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(group.title(s.lang), color = accent, style = MaterialTheme.typography.labelMedium)
            stats.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { (key, value) -> StatCell(key, value, before?.let { it[key] ?: 0.0 }?.takeIf { it != value }, accent, s, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** One figure on one line: a small icon, the stat's name, and the number on the right — lit, with the old one, when [was] differs. */
@Composable private fun StatCell(key: String, value: Double, was: Double?, accent: Color, s: ForgeState, modifier: Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Row(modifier.background(Abyss, shape).then(if (was != null) Modifier.border(1.dp, Ember.copy(alpha = .7f), shape) else Modifier)
        .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Tipped({ Tip(statTitle(key, s.lang), tint = accent, facts = listOf(ui("tip.value") to statNumber(key, value))) }) { StatIcon(key, accent, Modifier.size(12.dp)) }
        Text(statTitle(key, s.lang), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
        was?.let { Text(statNumber(key, it), color = Muted, style = MaterialTheme.typography.labelSmall, textDecoration = TextDecoration.LineThrough) }
        Text(statNumber(key, value), color = if (was != null) Ember else Parchment, style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

/**
 * Life and shield as figures, not bars: the sheet carries a maximum and no current value, so a bar
 * could only ever be full. Mana left the game in 2.48.0.
 */
@Composable fun HeroVitals(s: ForgeState) {
    HeroVitals(s.play.hero?.stats ?: return)
}

@Composable private fun HeroVitals(stats: Map<String, Double>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("STOCK_HEALTH", ui("hero.hp"), LifeRed), Triple("STOCK_ENERGY_SHIELD", ui("hero.es"), ShieldCyan)).forEach { (key, title, color) ->
            VitalTile(title, statNumber(key, stats[key] ?: 0.0), color, Modifier.weight(1f))
        }
    }
}

/** One vital: its name in its own colour over the figure the server sent. */
@Composable private fun VitalTile(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Column(modifier.background(Color.Black.copy(alpha = .22f), shape).border(1.dp, color.copy(alpha = .45f), shape)
        .padding(vertical = 6.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(title, color = color, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Parchment, style = MaterialTheme.typography.titleMedium)
    }
}
