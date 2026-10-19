package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.campaign.stat
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.might
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.components.MightMark
import com.sperance.exileforge.ui.components.MightPlaceMark
import com.sperance.exileforge.ui.components.ShortCodeChip
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

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
 * сложен - «…»; рядом - место героя [heroId] на доске Мощи своего сервера отдельным чипом «№37» (своя карточка игрока, касание - доска
 * славы «Мощь» на герое); героя нет на доске - чипа нет.
 */
@Composable fun NumberAndMight(hero: HeroView, heroId: String) {
    val vm = koinViewModel<HeroViewModel>()
    val place by produceState(0L, heroId) { value = heroId.takeIf { it.isNotBlank() }?.let { vm.ownCard(it) }?.mightPlace ?: 0L }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ShortCodeChip(ui("hero.number_own"), hero.info.number)
        Spacer(Modifier.weight(1f))
        MightMark(hero.might)
        if (place > 0) MightPlaceMark(place, onClick = { vm.openMightBoard(heroId) })
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
    val show: (String) -> Unit = { open = it }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (figures.values.isEmpty()) Text(ui("hero.no_stats"), color = Muted)
        game.index?.stats?.let { registry -> groupedStats(figures.values, registry).forEach { (section, rows) -> StatGroupCard(section, rows, figures, game, before, show) } }
    }
    open?.let { StatBreakdownSheet(game, it, figures.stats, shifts) { open = null } }
}

internal fun StatSection.accent(): Color = when (this) {
    StatSection.RESERVE -> LifeRed
    StatSection.DEFENCE -> Gold
    StatSection.RESISTANCE -> ShieldCyan
    StatSection.ATTACK -> Ember
    StatSection.CHARGES -> Handcrafted
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
    StatSection.CHARGES -> Glyph.STATE
    StatSection.AILMENT -> Glyph.CHAOS
    StatSection.ATTRIBUTE -> Glyph.ATTRIBUTE
    StatSection.OUTSIDE, StatSection.NONE -> Glyph.CURRENCY
}

/**
 * A section: a card headed by its icon and name in its colour, then its figures one to a row, a thin line between. Строки, что поднимают
 * пределы ([StatLimits.raisers]), свёрнуты внизу одной строкой «Пределы · N упёрлись» ([LimitsFold]); касание раскрывает их. [open] -
 * разбор строки.
 */
@Composable private fun StatGroupCard(group: StatSection, stats: List<Pair<String, Double>>, figures: StatFigures, game: GameUi, before: Map<String, Double>?, open: (String) -> Unit) {
    val accent = group.accent()
    val shape = RoundedCornerShape(10.dp)
    val raisers = remember(game.index) { game.index?.let(StatLimits::raisers).orEmpty() }
    val (limits, plain) = stats.partition { it.first in raisers }
    val reached = remember(plain, figures, game.index) { game.index?.campaign?.let { campaign -> plain.count { (key) -> StatLimits.of(key, figures.stats, campaign)?.reached == true } } ?: 0 }
    var unfolded by rememberSaveable(group) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlyphIcon(group.glyph, accent, Modifier.size(18.dp))
            Text(group.title(game.lang), color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        plain.forEach { (key, value) ->
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            StatCell(key, value, before?.let { it[key] ?: 0.0 }?.takeIf { it != value }, before, accent, figures, game, open)
        }
        if (limits.isNotEmpty()) {
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            LimitsFold(reached, unfolded, accent) { unfolded = !unfolded }
            if (unfolded) {
                limits.forEach { (key, value) ->
                    HorizontalDivider(thickness = 1.dp, color = PanelRaised)
                    StatCell(key, value, before?.let { it[key] ?: 0.0 }?.takeIf { it != value }, before, accent, figures, game, open)
                }
            }
        }
    }
}

/** Свёрнутая строка пределов раздела: «Пределы · N упёрлись» ([reached] строк раздела дошли до предела) и стрелка; касание - [onToggle]. */
@Composable private fun LimitsFold(reached: Int, unfolded: Boolean, accent: Color, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlyphIcon(Glyph.DEFENCE, accent.copy(alpha = .7f), Modifier.size(16.dp))
        Text(
            if (reached > 0) ui(pluralKey("stat.limits_reached", reached), reached) else ui("stat.limits"),
            color = if (reached > 0) Caution else Muted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(if (unfolded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** Строка сводки: касание открывает разбор ([open]). */
@Composable private fun StatCell(key: String, value: Double, was: Double?, before: Map<String, Double>?, accent: Color, figures: StatFigures, game: GameUi, open: (String) -> Unit) {
    val lit = if (was != null) Modifier.background(Ember.copy(alpha = .08f)) else Modifier
    StatRow(key, value, was, before, accent, figures, game, Modifier.fillMaxWidth().then(lit).clickable { open(key) }.padding(horizontal = 12.dp, vertical = 6.dp))
}

/**
 * Строка сводки в одну линию: значок характеристики, имя и итог справа; при [was] - старое число зачёркнуто, новое подсвечено.
 * За числом - значок ауры и ✦ Предначертания, если они её сдвинули. Подписей под именем нет: части, доля в резерве, предел
 * с набранным и рейтинг защиты - только в разборе. Пределы: печатается действующее число, упёршееся в предел - цветом
 * предупреждения; уклонение и броня - процентом защиты ([StatLimits.defence]).
 */
@Composable private fun StatRow(key: String, value: Double, was: Double?, before: Map<String, Double>?, accent: Color, figures: StatFigures, game: GameUi, modifier: Modifier) {
    Row(
        modifier.heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatIcon(key, accent, Modifier.size(18.dp))
        val limit = remember(key, figures, game.index) { game.index?.campaign?.let { StatLimits.of(key, figures.stats, it) } }
        val level = game.hero?.level
        val defence = remember(key, figures, game.index, level) { game.index?.let { index -> level?.let { StatLimits.defence(key, figures.stats, it, index) } } }
        val defenceWas = remember(key, before, game.index, level) { if (was == null) null else game.index?.let { index -> level?.let { before?.let { sheet -> StatLimits.defence(key, sheet, it, index) } } } }
        Text(statTitle(key, game.lang), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        was?.let { Text(defenceWas?.let { old -> ui("stat.defence_percent", number(old.effective)) } ?: statValue(key, it, game.index), color = Muted, style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.LineThrough) }
        Text(
            defence?.let { ui("stat.defence_percent", number(it.effective)) } ?: statValue(key, limit?.effective ?: value, game.index),
            color = when {
                was != null -> Ember
                limit?.clamped == true -> Caution
                else -> Parchment
            },
            style = MaterialTheme.typography.titleSmall,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (key in figures.skilled) Tipped({ Tip(ui("hero.mark.aura"), ui("hero.mark.aura_body"), tint = Rune) }) { GlyphIcon(Glyph.AURA, Rune, Modifier.size(13.dp)) }
        if (key in figures.fated) Tipped({ Tip(ui("hero.mark.fate"), ui("hero.mark.fate_body"), tint = Caution) }) { Text("✦", color = Caution, fontSize = 11.sp) }
    }
}

/** Цвет запаса резерва аур: мана - синим, здоровье - красным. */
internal fun ReservePool.tint(): Color = when (this) {
    ReservePool.MANA -> ManaBlue
    ReservePool.LIFE -> LifeRed
}
