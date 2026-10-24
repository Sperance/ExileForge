package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.campaign.stat
import com.sperance.exileforge.core.display.ActPenalty
import com.sperance.exileforge.core.display.ActResists
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.PenalizedResist
import com.sperance.exileforge.core.display.RowFigure
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.rowValue
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.sheet.ConditionalStat
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.core.sheet.StatExplainer
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.might
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.FoldBody
import com.sperance.exileforge.ui.components.FoldChevron
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.MightMark
import com.sperance.exileforge.ui.components.ShortCodeChip
import com.sperance.exileforge.ui.components.SumShareLine
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.components.foldToggle
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
 * Предначертанием. Дар и его текст - лента над сводкой, своей карточки дара в сводке нет. Сопротивления - «по листу → действует» под
 * штрафом акта героя ([ActPenalty]).
 */
@Composable fun HeroSummary(game: GameUi) {
    val hero = game.hero ?: return
    val conditional = remember(hero, game.index) { game.index?.let { ConditionalStats.of(hero, it) }.orEmpty() }
    val progress = game.progress
    val resists = remember(hero, progress, game.index) {
        game.index?.campaign?.let { campaign -> ActPenalty.of(hero, progress, campaign)?.let { ActResists(it, hero.figures.stats, campaign) } }
    }
    StatSheet(game, hero.figures, conditional = conditional, resists = resists)
}

/**
 * Строка над сводкой: слева свой номер героя (касание копирует; сервер старше - нет),
 * справа Мощь - чип «⚜ 150к» из того же листа боя без карты, что и цифры сводки, с разбором стойкости; ещё не
 * сложен - «…». Места на доске Мощи здесь нет - оно в карточке игрока и на доске славы.
 */
@Composable fun NumberAndMight(hero: HeroView) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ShortCodeChip(ui("hero.number_own"), hero.info.number)
        Spacer(Modifier.weight(1f))
        MightMark(hero.might)
    }
}

/**
 * A sheet of figures (apart from the hero, so the run's window draws the same one). [figures] - цифры сводки с отметками ауры и
 * Предначертания, [conditional] - раздел «С условием». Цифра открывает своё окно разбора ([shifts] - сдвиги эффектов захода по
 * характеристике, их читает разбор). Разделы и порядок - реестр контента.
 *
 * С [trend] (лист похода против листа героя без похода) сдвинутая строка - полоска слева, цвет и ▲/▼ по смыслу характеристики; касание
 * раскрывает под ней вклады ([StatTrend.shares]) - «Герой без похода» первым, затем вклады карты и боя числами в единице строки, их
 * сумма - число строки, - и кнопку разбора; раскрытые строки держатся, пока числа идут. Свёрнутые «Пределы» раздела раскрываются сами, если внутри сдвиг.
 *
 * С [resists] (лист без карты под штрафом акта) строка сопротивления - «по листу → действует», в шапке раздела - плашка акта, под
 * строками - сноска с регионом и пределом; разбор строки - со штрафом. Лист, где штраф уже лёг (заход), его не передаёт.
 */
@Composable fun StatSheet(
    game: GameUi,
    figures: StatFigures,
    conditional: List<ConditionalStat> = emptyList(),
    trend: StatTrend? = null,
    shifts: Map<String, List<Shift>> = emptyMap(),
    resists: ActResists? = null,
) {
    var open by remember { mutableStateOf<String?>(null) }
    var openConditional by remember { mutableStateOf<ConditionalStat?>(null) }
    val show: (String) -> Unit = { open = it }
    Column(verticalArrangement = Arrangement.spacedBy(SummaryMetrics.cardGap)) {
        if (figures.values.isEmpty()) Text(ui("hero.no_stats"), color = Muted)
        game.index?.stats?.let { registry ->
            groupedStats(figures.values, registry).forEach { (section, rows) -> StatGroupCard(section, rows, figures, game, trend, resists, show) }
        }
        if (conditional.isNotEmpty()) ConditionalCard(conditional, game) { openConditional = it }
    }
    open?.let { StatBreakdownSheet(game, it, figures.stats, shifts, resists) { open = null } }
    openConditional?.let { ConditionalBreakdownSheet(game, it) { openConditional = null } }
}

/** Цвет раздела «С условием» и условия в имени строки - латунь. */
internal val ConditionalAccent: Color get() = Brass

/** Имя условной строки одной фразой: характеристика и условие, условие - цветом раздела ([ConditionalAccent]). */
internal fun conditionalTitle(row: ConditionalStat, game: GameUi): AnnotatedString = buildAnnotatedString {
    append(statTitle(row.stat, game.lang))
    append(' ')
    withStyle(SpanStyle(color = ConditionalAccent)) { append(loc(LocaleKey.condition(row.condition))) }
}

/**
 * Последний раздел сводки - «С условием»: строки листа, что бой кладёт, пока держится условие ([ConditionalStats]); в итог своей
 * характеристики они не входят и печатаются один раз - здесь. Строка - характеристика с условием одной фразой и вклад справа;
 * касание - разбор ([ConditionalBreakdownSheet]). Пустой раздел не рисуется.
 */
@Composable private fun ConditionalCard(rows: List<ConditionalStat>, game: GameUi, open: (ConditionalStat) -> Unit) {
    val explainer = remember(game) { StatExplainer(game) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Panel)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = SummaryMetrics.headPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.CallSplit, null, tint = ConditionalAccent, modifier = Modifier.size(SummaryMetrics.icon))
            Text(ui("stat.conditional.group"), color = ConditionalAccent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        rows.forEach { row ->
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            Row(
                Modifier.fillMaxWidth().clickable { open(row) }.padding(horizontal = 12.dp, vertical = SummaryMetrics.rowPadding).heightIn(min = SummaryMetrics.rowHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatIcon(row.stat, ConditionalAccent, Modifier.size(SummaryMetrics.icon))
                Text(conditionalTitle(row, game), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    explainer.fmt(row.stat, row.value, row.op, game.index?.stats?.isPercent(row.stat) == true),
                    color = Parchment,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = SummaryMetrics.figure,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
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
 * пределы ([StatLimits.raisers]), свёрнуты внизу одной строкой «Пределы · N упёрлись» ([LimitsFold]); касание раскрывает их, сдвиг
 * сравнения [trend] среди них раскрывает их сам. [open] - разбор строки. Строки под штрафом акта ([resists]) - с плашкой акта в шапке
 * и сноской под ними ([PenaltyChip], [PenaltyNote]).
 */
@Composable private fun StatGroupCard(
    group: StatSection,
    stats: List<Pair<String, Double>>,
    figures: StatFigures,
    game: GameUi,
    trend: StatTrend?,
    resists: ActResists?,
    open: (String) -> Unit,
) {
    val accent = group.accent()
    val shape = RoundedCornerShape(10.dp)
    val raisers = remember(game.index) { game.index?.let(StatLimits::raisers).orEmpty() }
    val (limits, plain) = stats.partition { it.first in raisers }
    val reached = remember(plain, figures, game.index) { game.index?.campaign?.let { campaign -> plain.count { (key) -> StatLimits.of(key, figures.stats, campaign)?.reached == true } } ?: 0 }
    val shiftedInside = trend != null && limits.any { (key) -> trend.shift.of(key) != null }
    var unfolded by rememberSaveable(group, shiftedInside) { mutableStateOf(shiftedInside) }
    val penalized = remember(plain, resists) { resists?.takeIf { r -> plain.any { (key) -> r.of(key) != null } } }
    Column(Modifier.fillMaxWidth().clip(shape).background(Panel)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = SummaryMetrics.headPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlyphIcon(group.glyph, accent, Modifier.size(SummaryMetrics.icon))
            Text(group.title(game.lang), color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            penalized?.let {
                Spacer(Modifier.weight(1f))
                PenaltyChip(it.penalty)
            }
        }
        plain.forEach { (key, value) ->
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            StatCell(key, value, accent, figures, game, trend, penalized, open)
        }
        penalized?.let {
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            PenaltyNote(it.penalty, game)
        }
        if (limits.isNotEmpty()) {
            HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            LimitsFold(reached, unfolded, accent) { unfolded = !unfolded }
            FoldBody(unfolded) {
                limits.forEach { (key, value) ->
                    HorizontalDivider(thickness = 1.dp, color = PanelRaised)
                    StatCell(key, value, accent, figures, game, trend, penalized, open)
                }
            }
        }
    }
}

/** Свёрнутая строка пределов раздела: «Пределы · N упёрлись» ([reached] строк раздела дошли до предела) и стрелка; касание - [onToggle]. */
@Composable private fun LimitsFold(reached: Int, unfolded: Boolean, accent: Color, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().foldToggle(unfolded, onToggle = onToggle).padding(horizontal = 12.dp, vertical = SummaryMetrics.rowPadding).heightIn(min = SummaryMetrics.rowHeight),
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
        FoldChevron(unfolded, size = SummaryMetrics.icon)
    }
}

/**
 * Плотность сводки героя: строка не ниже [rowHeight] с полями [rowPadding], значок строки и шапки раздела [icon], итог [figure],
 * шапка раздела с полями [headPadding], между карточками разделов [cardGap]; полоска сдвинутой строки [stripe], отступ её вкладов [indent].
 */
private object SummaryMetrics {
    val rowHeight = 34.dp
    val rowPadding = 3.dp
    val icon = 16.dp
    val figure = 14.sp
    val headPadding = 4.dp
    val cardGap = 6.dp
    val stripe = 3.dp
    val indent = 38.dp
}

/**
 * Строка сводки. Без сдвига сравнения [trend] касание открывает разбор ([open]). Сдвинутая - полоска слева, цвет и знак отметки
 * ([TrendMark]); касание раскрывает под ней вклады ([ChangeSources]) с кнопкой разбора. Под штрафом акта ([resists]) - «по листу →
 * действует» ([StatRow]).
 */
@Composable private fun StatCell(key: String, value: Double, accent: Color, figures: StatFigures, game: GameUi, trend: StatTrend?, resists: ActResists?, open: (String) -> Unit) {
    val mark = trend?.of(key, value, game.index)
    if (trend == null || mark == null) {
        val resist = remember(key, resists) { resists?.of(key) }
        StatRow(key, value, accent, figures, game, null, Modifier.fillMaxWidth().clickable { open(key) }.padding(horizontal = 12.dp, vertical = SummaryMetrics.rowPadding), resist)
        return
    }
    var unfolded by rememberSaveable(key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(mark.tint.copy(alpha = .06f)).drawBehind { drawRect(mark.tint, size = Size(SummaryMetrics.stripe.toPx(), size.height)) }) {
        StatRow(
            key,
            value,
            accent,
            figures,
            game,
            mark,
            Modifier.fillMaxWidth().foldToggle(unfolded, ui(if (unfolded) "common.collapse" else "common.expand")) { unfolded = !unfolded }
                .padding(horizontal = 12.dp, vertical = SummaryMetrics.rowPadding),
        )
        FoldBody(unfolded) { ChangeSources(key, trend) { open(key) } }
    }
}

/** Вклады в сдвинутую цифру [key] ([StatTrend.shares]): «Герой без похода», вклады карты и боя - их сумма и есть число строки; под ними - кнопка разбора. */
@Composable private fun ChangeSources(key: String, trend: StatTrend, onBreakdown: () -> Unit) {
    val shares = remember(key, trend) { trend.shares(key) }
    Column(Modifier.fillMaxWidth().padding(start = SummaryMetrics.indent, end = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        shares.forEach { SumShareLine(it) }
        ForgeButton(onClick = onBreakdown, kind = ButtonKind.Text, size = ButtonSize.Small) { Text(ui("stat.compare.breakdown")) }
    }
}

/**
 * Строка сводки в одну линию: значок характеристики, имя и итог справа; сдвинутая ([mark]) - итог цветом отметки и её знак.
 * За числом - значок ауры и ✦ Предначертания, если они её сдвинули. Подписей под именем нет: части, доля в резерве, предел
 * с набранным и рейтинг защиты - только в разборе. Пределы: печатается действующее число, упёршееся в предел - цветом
 * предупреждения; уклонение и броня - процентом защиты ([StatLimits.defence]). Сопротивление под штрафом акта [resist] - «по листу →
 * действует» ([PenalizedFigure]); без штрафа - null.
 */
@Composable private fun StatRow(key: String, value: Double, accent: Color, figures: StatFigures, game: GameUi, mark: TrendMark?, modifier: Modifier, resist: PenalizedResist? = null) {
    Row(
        modifier.heightIn(min = SummaryMetrics.rowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatIcon(key, accent, Modifier.size(SummaryMetrics.icon))
        val level = game.hero?.level
        val row = remember(key, figures, game.index, level) { game.index?.let { RowFigure.of(key, figures, level, it) } }
        Text(statTitle(key, game.lang), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (resist != null) {
            PenalizedFigure(key, resist, game)
        } else {
            Text(
                row?.text(game.index, figures.stats) ?: rowValue(key, value, game.index, figures.stats),
                color = when {
                    mark != null -> mark.tint
                    row?.clamped == true -> Caution
                    else -> Parchment
                },
                style = MaterialTheme.typography.titleSmall,
                fontSize = SummaryMetrics.figure,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        mark?.mark?.let { Text(it, color = mark.tint, fontSize = 11.sp) }
        if (key in figures.skilled) Tipped({ Tip(ui("hero.mark.aura"), ui("hero.mark.aura_body"), tint = Rune) }) { GlyphIcon(Glyph.AURA, Rune, Modifier.size(13.dp)) }
        if (key in figures.fated) Tipped({ Tip(ui("hero.mark.fate"), ui("hero.mark.fate_body"), tint = Caution) }) { Text("✦", color = Caution, fontSize = 11.sp) }
    }
}

/**
 * Число сопротивления под штрафом акта: набранное по листу мелко, стрелка, после штрафа - если бой читает другое (предел или пол), и
 * действующее: упёршееся в предел - цветом предупреждения, ниже нуля - кровью.
 */
@Composable private fun PenalizedFigure(key: String, resist: PenalizedResist, game: GameUi) {
    val minor = MaterialTheme.typography.labelMedium
    val reading = ui("stat.penalty.reading", rowValue(key, resist.sheet, game.index), rowValue(key, resist.effective, game.index))
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = reading },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(number(resist.sheet), color = Muted, style = minor, maxLines = 1)
        Text("→", color = Muted, style = minor)
        if (resist.clamped) {
            Text(number(resist.penalized), color = Muted, style = minor, maxLines = 1)
            Text("→", color = Muted, style = minor)
        }
        Text(
            rowValue(key, resist.effective, game.index),
            color = when {
                resist.effective < 0 -> LifeRed
                resist.over > 0 -> Caution
                else -> Parchment
            },
            style = MaterialTheme.typography.titleSmall,
            fontSize = SummaryMetrics.figure,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Плашка штрафа в шапке раздела сопротивлений: «Акт III · −30%», на картах Атласа - «Атлас · −60%». */
@Composable private fun PenaltyChip(penalty: ActPenalty) {
    val percent = number(penalty.percent)
    val shape = RoundedCornerShape(6.dp)
    Text(
        if (penalty.atlas) ui("stat.penalty.chip_atlas", percent) else ui("stat.penalty.chip", penalty.numeral, percent),
        color = Caution,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier.clip(shape).background(Caution.copy(alpha = .08f)).border(1.dp, Caution.copy(alpha = .35f), shape).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** Сноска под сопротивлениями: где штраф (регион или карты Атласа) и сколько, что слева и справа, предел правил. */
@Composable private fun PenaltyNote(penalty: ActPenalty, game: GameUi) {
    val place = penalty.region?.let(::regionTitle) ?: ui("stat.penalty.atlas")
    val cap = game.index?.campaign?.combat?.resistCap ?: 0.0
    Text(
        ui("stat.penalty.note", place, number(penalty.percent), number(cap)),
        color = Muted,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Цвет запаса резерва аур: мана - синим, здоровье - красным. */
internal fun ReservePool.tint(): Color = when (this) {
    ReservePool.MANA -> ManaBlue
    ReservePool.LIFE -> LifeRed
}
