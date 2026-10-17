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
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.partFigure
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.figures
import com.sperance.exileforge.presentation.state.manaReserve
import com.sperance.exileforge.presentation.state.might
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.components.MightMark
import com.sperance.exileforge.ui.components.ShortCodeChip
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

/**
 * The character's figures: life and shield on top, then everything else the sheet counts, one
 * card per section, one figure to a full-width row so each reads at a glance.
 *
 * The first of the Hero tab's three sections. Nothing here is a command — it is the sheet a player
 * reads before deciding what to wear, whole and in place rather than behind another tap. Раздел строки - `section`
 * характеристики в контенте, «Прочего» нет.
 *
 * Цифры - лист боя ([com.sperance.exileforge.core.campaign.CombatSheet]) с аурами и
 * Предначертанием. Дар и его текст - лента над сводкой, своей карточки дара в сводке нет.
 */
@Composable fun HeroSummary(game: GameUi) {
    val hero = game.hero ?: return
    StatSheet(game, hero.figures)
}

/**
 * Строка над сводкой: слева свой номер героя (касание копирует; сервер старше - нет),
 * справа Мощь - чип «⚜ 12.5k» из того же листа боя без карты, что и цифры сводки, с разбором стойкости; ещё не
 * сложен - «…».
 */
@Composable fun NumberAndMight(hero: HeroView) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ShortCodeChip(ui("hero.number_own"), hero.info.number)
        Spacer(Modifier.weight(1f))
        MightMark(hero.might, hero.combat?.mightDefense)
    }
}

/**
 * A sheet of figures (apart from the hero, so the map's window draws the same one). Given
 * [before], a figure that differs from it is lit and says what it was — the sheet under a map's
 * effects held against the hero's own. [figures] - цифры сводки с отметками ауры и Предначертания.
 *
 * Цифра открывает своё окно. Разделы и порядок - реестр контента.
 */
@Composable fun StatSheet(game: GameUi, figures: StatFigures, before: Map<String, Double>? = null, shifts: Map<String, List<Shift>> = emptyMap()) {
    var open by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (figures.values.isEmpty()) Text(ui("hero.no_stats"), color = Muted)
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
            StatCell(key, value, before?.let { it[key] ?: 0.0 }?.takeIf { it != value }, before, accent, figures, game, Modifier.fillMaxWidth().clickable { open(key) })
        }
    }
}

/**
 * One figure on one full-width row: its icon, the stat's name, and the number on the right — lit, with the old one, when [was] differs.
 * За числом - значок ауры и ✦ Предначертания, если они её сдвинули. Строка с частями (`partOf`) - итог боя, под
 * именем - из чего он: «12 + 1.5% от максимума».
 *
 * Доля маны, что держат ауры, - под именем, справа - весь запас. Пределы: сводка печатает действующее число, набранное сверх - под именем
 * «предел; набрано N». Уклонение и броня - главным шанс уклонения и снижение физического урона.
 */
@Composable private fun StatCell(key: String, value: Double, was: Double?, before: Map<String, Double>?, accent: Color, figures: StatFigures, game: GameUi, modifier: Modifier) {
    Row(
        modifier.then(if (was != null) Modifier.background(Ember.copy(alpha = .08f)) else Modifier).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tipped({ Tip(statTitle(key, game.lang), tint = accent, facts = listOf(ui("tip.value") to statValue(key, value, game.index))) }) { StatIcon(key, accent, Modifier.size(18.dp)) }
        val reserve = if (key == MANA_STAT) remember(game.hero, game.index) { game.manaReserve() }?.takeIf { it.percent > 0 } else null
        val parts = remember(key, figures, game.index) { game.index?.let { partsText(key, figures.stats, it) } }
        val limit = remember(key, figures, game.index) { game.index?.campaign?.let { StatLimits.of(key, figures.stats, it) } }
        val level = game.hero?.level
        val defence = remember(key, figures, game.index, level) { game.index?.let { index -> level?.let { StatLimits.defence(key, figures.stats, it, index) } } }
        val defenceWas = remember(key, before, game.index, level) { if (was == null) null else game.index?.let { index -> level?.let { before?.let { sheet -> StatLimits.defence(key, sheet, it, index) } } } }
        Column(Modifier.weight(1f)) {
            Text(statTitle(key, game.lang), color = Parchment, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            reserve?.let { Text(ui("hero.mana_reserved", number(it.percent)), color = ManaBlue, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            parts?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            limit?.takeIf { it.clamped }?.let { Text(ui("stat.cap.reached", statValue(key, it.gathered, game.index)), color = Caution, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            defence?.let {
                val raw = ui(if (key == StatLimits.EVASION) "stat.defence_raw.evasion" else "stat.defence_raw.armour", statValue(key, value, game.index), number(it.cap))
                Text(raw, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        was?.let { Text(defenceWas?.let { old -> ui("stat.defence_percent", number(old.effective)) } ?: statValue(key, it, game.index), color = Muted, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough) }
        Text(
            defence?.let { ui("stat.defence_percent", number(it.effective)) } ?: statValue(key, limit?.effective ?: value, game.index),
            color = when {
                was != null -> Ember
                limit?.clamped == true -> Caution
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
 * Из чего строка с частями: своя характеристика и каждая часть (`partOf`) листа боя [stats], словами [partFigure] - доля
 * максимума или «увеличение». Без частей - null.
 */
private fun partsText(key: String, stats: Map<String, Double>, index: ContentIndex): String? {
    val registry = index.stats
    val parts = registry.parts(key).mapNotNull { part -> stats[part]?.takeIf { it != 0.0 }?.let { part to it } }
    if (parts.isEmpty()) return null
    val own = stats[key] ?: 0.0
    val texts = parts.map { (part, value) -> partFigure(key, part, statValue(part, value, index), index) }
    return (listOfNotNull(statValue(key, own, index).takeIf { own != 0.0 }) + texts).joinToString(" + ")
}

internal val MANA_STAT: String = CoreStat.MANA.code
