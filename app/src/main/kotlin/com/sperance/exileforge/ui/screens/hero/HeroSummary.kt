package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.StatGroup
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.manaReserve
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.theme.*

/**
 * The character's figures: life and shield on top, then everything else the sheet counts, one
 * card per group, one figure to a full-width row so each reads at a glance.
 *
 * The first of the Hero tab's three sections. Nothing here is a command — it is the sheet a player
 * reads before deciding what to wear, whole and in place rather than behind another tap. Which
 * group a stat belongs to is [StatGroup]'s, so a stat nobody named still lands in «Прочее».
 */
@Composable fun HeroSummary(s: ForgeState) {
    val hero = s.hero ?: return
    StatSheet(s, hero.stats)
}

/**
 * A sheet of figures (2.75.0: apart from the hero, so the map's window draws the same one). Given
 * [before], a figure that differs from it is lit and says what it was — the sheet under a map's
 * effects held against the hero's own.
 */
@Composable fun StatSheet(s: ForgeState, stats: Map<String, Double>, before: Map<String, Double>? = null, shifts: Map<String, List<Shift>> = emptyMap()) {
    // A figure opens its own window (3.11.0): what it is and what it is made of.
    var open by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (stats.isEmpty()) Text(ui("hero.no_stats"), color = Muted)
        // The registry's order inside a group, once the content is here; the code's before that.
        groupedStats(stats, s.index?.stats).forEach { (group, figures) -> StatGroupCard(group, figures, s, before) { open = it } }
    }
    open?.let { StatBreakdownSheet(s, it, shifts) { open = null } }
}

internal fun StatGroup.accent(): Color = when (this) {
    StatGroup.RESERVE -> LifeRed
    StatGroup.DEFENCE -> Gold
    StatGroup.RESISTANCE -> ShieldCyan
    StatGroup.ATTACK -> Ember
    StatGroup.AILMENT -> Vital
    StatGroup.ATTRIBUTE -> Rune
    StatGroup.OTHER -> Muted
}

/** The drawing at the head of a group's card: its meaning, in its colour. */
private val StatGroup.glyph: Glyph get() = when (this) {
    StatGroup.RESERVE -> Glyph.LIFE
    StatGroup.DEFENCE -> Glyph.DEFENCE
    StatGroup.RESISTANCE -> Glyph.FIRE
    StatGroup.ATTACK -> Glyph.ATTACK
    StatGroup.AILMENT -> Glyph.CHAOS
    StatGroup.ATTRIBUTE -> Glyph.ATTRIBUTE
    StatGroup.OTHER -> Glyph.INFO
}

/** A group: a card headed by its icon and name in its colour, then its figures one to a row, a thin line between. */
@Composable private fun StatGroupCard(group: StatGroup, stats: List<Pair<String, Double>>, s: ForgeState, before: Map<String, Double>?, open: (String) -> Unit) {
    val accent = group.accent()
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphIcon(group.glyph, accent, Modifier.size(18.dp))
            Text(group.title(s.lang), color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        stats.forEach { (key, value) ->
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            StatCell(key, value, before?.let { it[key] ?: 0.0 }?.takeIf { it != value }, accent, s, Modifier.fillMaxWidth().clickable { open(key) })
        }
    }
}

/** One figure on one full-width row: its icon, the stat's name, and the number on the right — lit, with the old one, when [was] differs. */
@Composable private fun StatCell(key: String, value: Double, was: Double?, accent: Color, s: ForgeState, modifier: Modifier) {
    Row(modifier.then(if (was != null) Modifier.background(Ember.copy(alpha = .08f)) else Modifier).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tipped({ Tip(statTitle(key, s.lang), tint = accent, facts = listOf(ui("tip.value") to statValue(key, value, s.index))) }) { StatIcon(key, accent, Modifier.size(18.dp)) }
        // The mana's share the auras hold reads under its name: the figure on the right is the whole pool.
        val reserve = if (key == MANA_STAT) remember(s.hero, s.index) { s.manaReserve() }?.takeIf { it.percent > 0 } else null
        Column(Modifier.weight(1f)) {
            Text(statTitle(key, s.lang), color = Parchment, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            reserve?.let { Text(ui("hero.mana_reserved", number(it.percent)), color = ManaBlue, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
        was?.let { Text(statValue(key, it, s.index), color = Muted, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough) }
        // A capped figure (3.12.0) reads as the fight reads it, greyed when it lies above; the ceiling is in the figure's window.
        val limit = s.index?.campaign?.combat?.let { StatLimits.of(key, s.hero?.stats.orEmpty(), it) }
        Text(statValue(key, limit?.effective ?: value, s.index), color = when { was != null -> Ember; (limit?.over ?: 0.0) > 0 -> Muted; else -> Parchment },
            style = MaterialTheme.typography.titleMedium, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

internal const val MANA_STAT = "STOCK_MANA"
