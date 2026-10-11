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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.figures
import com.sperance.exileforge.presentation.state.manaReserve
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

/**
 * The character's figures: life and shield on top, then everything else the sheet counts, one
 * card per section, one figure to a full-width row so each reads at a glance.
 *
 * The first of the Hero tab's three sections. Nothing here is a command — it is the sheet a player
 * reads before deciding what to wear, whole and in place rather than behind another tap. Раздел строки (4.6.5) - `section`
 * характеристики в контенте, «Прочего» нет; силы уникальных - блок «Силы снаряжения» (макет «Силы снаряжения», вариант B).
 *
 * 4.6.4 (макет «Сводка характеристик», вариант A): цифры - лист боя ([com.sperance.exileforge.core.campaign.CombatSheet]) с аурами и
 * Предначертанием. Дар и его текст - лента над сводкой, своей карточки дара в сводке нет (4.6.5).
 */
@Composable fun HeroSummary(game: GameUi) {
    val hero = game.hero ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatSheet(game, hero.figures)
        GearPowers(game)
    }
}

/** Сила надетой вещи (4.6.5): строка `POWER_*` фразой сервера и вещь, что её несёт. */
private data class GearPower(val text: String, val item: ItemInstance, val title: String)

/**
 * «Силы снаряжения» (4.6.5, макет «Силы снаряжения», вариант B): силы надетых уникальных и мифических вещей фразами, вещь - мелко
 * справа; нажатие открывает карточку вещи. Числа сил, что меняют характеристики, уже в строках сводки и их разборе.
 */
@Composable private fun GearPowers(game: GameUi) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val powers = remember(hero.items, index, game.lang) {
        hero.items.filter { it.equipped }.mapNotNull { ItemView.of(it, index) }.flatMap { view ->
            view.lines.filter { line -> line.stats.any { index.stats[it]?.group == StatGroup.POWER } }.map { GearPower(it.text, view.item, view.title) }
        }
    }
    if (powers.isEmpty()) return
    val inspect = rememberInspect()
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✦", color = rarityColor("UNIQUE"), fontSize = 15.sp)
            Text(ui("hero.gear_powers"), color = rarityColor("UNIQUE"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        powers.forEach { power ->
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            Row(
                Modifier.fillMaxWidth().clickable { inspect(Inspect.Copy(power.item)) }.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("✦", color = rarityColor("UNIQUE"), fontSize = 12.sp)
                Text(power.text, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(power.title, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
            }
        }
    }
}

/**
 * A sheet of figures (2.75.0: apart from the hero, so the map's window draws the same one). Given
 * [before], a figure that differs from it is lit and says what it was — the sheet under a map's
 * effects held against the hero's own. 4.6.4: [figures] - цифры сводки с отметками ауры и Предначертания.
 */
@Composable fun StatSheet(game: GameUi, figures: StatFigures, before: Map<String, Double>? = null, shifts: Map<String, List<Shift>> = emptyMap()) {
    // A figure opens its own window (3.11.0): what it is and what it is made of.
    var open by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (figures.values.isEmpty()) Text(ui("hero.no_stats"), color = Muted)
        // Разделы и порядок - реестр контента (4.6.5); без контента сводке нечего раскладывать.
        game.index?.stats?.let { registry -> groupedStats(figures.values, registry).forEach { (section, rows) -> StatGroupCard(section, rows, figures, game, before) { open = it } } }
    }
    open?.let { StatBreakdownSheet(game, it, shifts) { open = null } }
}

internal fun StatSection.accent(): Color = when (this) {
    StatSection.RESERVE -> LifeRed
    StatSection.DEFENCE -> Gold
    StatSection.RESISTANCE -> ShieldCyan
    StatSection.ATTACK -> Ember
    StatSection.AILMENT -> Vital
    StatSection.ATTRIBUTE -> Rune
    StatSection.OUTSIDE, StatSection.NONE -> Brass
}

/** The drawing at the head of a section's card: its meaning, in its colour. */
private val StatSection.glyph: Glyph get() = when (this) {
    StatSection.RESERVE -> Glyph.LIFE
    StatSection.DEFENCE -> Glyph.DEFENCE
    StatSection.RESISTANCE -> Glyph.FIRE
    StatSection.ATTACK -> Glyph.ATTACK
    StatSection.AILMENT -> Glyph.CHAOS
    StatSection.ATTRIBUTE -> Glyph.ATTRIBUTE
    StatSection.OUTSIDE, StatSection.NONE -> Glyph.CURRENCY
}

/** A section: a card headed by its icon and name in its colour, then its figures one to a row, a thin line between. */
@Composable private fun StatGroupCard(group: StatSection, stats: List<Pair<String, Double>>, figures: StatFigures, game: GameUi, before: Map<String, Double>?, open: (String) -> Unit) {
    val accent = group.accent()
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlyphIcon(group.glyph, accent, Modifier.size(18.dp))
            Text(group.title(game.lang), color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        stats.forEach { (key, value) ->
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            StatCell(key, value, before?.let { it[key] ?: 0.0 }?.takeIf { it != value }, accent, figures, game, Modifier.fillMaxWidth().clickable { open(key) })
        }
    }
}

/**
 * One figure on one full-width row: its icon, the stat's name, and the number on the right — lit, with the old one, when [was] differs.
 * 4.6.4: за числом - значок ауры и ✦ Предначертания, если они её сдвинули. 4.6.5: строка с частями (`partOf`) - итог боя, под
 * именем - из чего он: «12 + 1.5% от максимума».
 */
@Composable private fun StatCell(key: String, value: Double, was: Double?, accent: Color, figures: StatFigures, game: GameUi, modifier: Modifier) {
    Row(
        modifier.then(if (was != null) Modifier.background(Ember.copy(alpha = .08f)) else Modifier).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tipped({ Tip(statTitle(key, game.lang), tint = accent, facts = listOf(ui("tip.value") to statValue(key, value, game.index))) }) { StatIcon(key, accent, Modifier.size(18.dp)) }
        // The mana's share the auras hold reads under its name: the figure on the right is the whole pool.
        val reserve = if (key == MANA_STAT) remember(game.hero, game.index) { game.manaReserve() }?.takeIf { it.percent > 0 } else null
        val parts = remember(key, figures, game.index) { game.index?.let { partsText(key, figures.stats, it) } }
        Column(Modifier.weight(1f)) {
            Text(statTitle(key, game.lang), color = Parchment, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            reserve?.let { Text(ui("hero.mana_reserved", number(it.percent)), color = ManaBlue, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            parts?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
        was?.let { Text(statValue(key, it, game.index), color = Muted, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough) }
        // A capped figure (3.12.0) reads as the fight reads it, greyed when it lies above; the ceiling is in the figure's window.
        val limit = game.index?.campaign?.combat?.let { StatLimits.of(key, figures.stats, it) }
        Text(
            statValue(key, value, game.index),
            color = when {
                was != null -> Ember
                (limit?.over ?: 0.0) > 0 -> Muted
                else -> Parchment
            },
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        if (key in figures.skilled) Tipped({ Tip(ui("hero.mark.aura"), ui("hero.mark.aura_body"), tint = Rune) }) { GlyphIcon(Glyph.AURA, Rune, Modifier.size(13.dp)) }
        if (key in figures.fated) Tipped({ Tip(ui("hero.mark.fate"), ui("hero.mark.fate_body"), tint = Caution) }) { Text("✦", color = Caution, fontSize = 11.sp) }
    }
}

/**
 * Из чего строка с частями (4.6.5): своя характеристика и каждая часть (`partOf`) листа боя [stats]; доля другого вида
 * (процент к числу) - «от максимума». Без частей - null.
 */
private fun partsText(key: String, stats: Map<String, Double>, index: ContentIndex): String? {
    val registry = index.stats
    val parts = registry.parts(key).mapNotNull { part -> stats[part]?.takeIf { it != 0.0 }?.let { part to it } }
    if (parts.isEmpty()) return null
    val own = stats[key] ?: 0.0
    val texts = parts.map { (part, value) -> if (registry.isPercent(part) && !registry.isPercent(key)) ui("hero.part_share", statValue(part, value, index)) else statValue(part, value, index) }
    return (listOfNotNull(statValue(key, own, index).takeIf { own != 0.0 }) + texts).joinToString(" + ")
}

internal val MANA_STAT: String = CoreStat.MANA.code
