package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
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
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.sheet.ConditionalLine
import com.sperance.exileforge.core.sheet.ConditionalStat
import com.sperance.exileforge.core.sheet.PenaltyFigure
import com.sperance.exileforge.core.sheet.SheetLine
import com.sperance.exileforge.core.sheet.SheetSection
import com.sperance.exileforge.core.sheet.StatSheetModel
import com.sperance.exileforge.core.sheet.might
import com.sperance.exileforge.rules.content.ContentIndex
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
import com.sperance.exileforge.ui.components.TagSize
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.components.ToneTag
import com.sperance.exileforge.ui.components.foldToggle
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

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
 * Что держит лист сводки вне строк: открытый разбор строки ([breakdown]) или условной строки ([conditional]) и раскрытые «Пределы»
 * разделов. Раскрытие «Пределов» запоминается вместе с тем, был ли среди них сдвиг сравнения: сдвиг появился или ушёл - раздел снова в
 * своём виде по умолчанию (со сдвигом - раскрыт).
 */
@Stable
class StatSheetState internal constructor(opened: Map<String, Boolean>) {
    private val limits = mutableStateMapOf<String, Boolean>().apply { putAll(opened) }

    /** Характеристика, чей разбор открыт; null - закрыт. */
    var breakdown by mutableStateOf<String?>(null)

    /** Условная строка, чей разбор открыт; null - закрыт. */
    var conditional by mutableStateOf<ConditionalStat?>(null)

    /** Раскрыты ли «Пределы» раздела [section]; [shifted] - среди них есть сдвиг сравнения. */
    fun limitsOpen(section: StatSection, shifted: Boolean): Boolean = limits[limitsKey(section, shifted)] ?: shifted

    /** Свернуть или раскрыть «Пределы» раздела [section]. */
    fun toggleLimits(section: StatSection, shifted: Boolean) {
        limits[limitsKey(section, shifted)] = !limitsOpen(section, shifted)
    }

    private fun limitsKey(section: StatSection, shifted: Boolean): String = "${section.name}:$shifted"

    internal companion object {
        /** Сохраняет раскрытые «Пределы» на поворот экрана и уход с вкладки; открытый разбор не сохраняется. */
        val Saver: Saver<StatSheetState, Map<String, Boolean>> = Saver({ HashMap(it.limits) }, { StatSheetState(it) })
    }
}

/** Состояние листа сводки ([StatSheetState]), что переживает поворот экрана. */
@Composable fun rememberStatSheetState(): StatSheetState = rememberSaveable(saver = StatSheetState.Saver) { StatSheetState(emptyMap()) }

/**
 * A sheet of figures (apart from the hero, so the run's window draws the same one): строки сводки [model] элементами ленивого списка -
 * шапка раздела, строки по одной, «Пределы» и их строки, раздел «С условием»; кадр рисует лишь видимые. Цифра открывает своё окно
 * разбора ([state], окна - [StatSheetWindows]). Разделы и порядок - реестр контента. Между карточками разделов - свой зазор, список
 * вокруг не добавляет своего.
 *
 * С [trend] (лист похода против листа героя без похода) сдвинутая строка - полоска слева, цвет и ▲/▼ по смыслу характеристики; касание
 * раскрывает под ней вклады ([StatTrend.shares]) - «Герой без похода» первым, затем вклады карты и боя числами в единице строки, их
 * сумма - число строки, - и кнопку разбора; раскрытые строки держатся, пока числа идут. Свёрнутые «Пределы» раздела раскрываются сами,
 * если внутри сдвиг.
 *
 * Строка сопротивления под штрафом акта - «по листу → действует», в шапке раздела - плашка акта, под строками - сноска с регионом и
 * пределом; разбор строки - со штрафом ([StatSheetModel.resists]).
 */
fun LazyListScope.statSheet(model: StatSheetModel, state: StatSheetState, trend: StatTrend?, index: ContentIndex?) {
    if (model.empty) item(key = "sheet:empty") { Text(ui("hero.no_stats"), color = Muted) }
    model.sections.forEachIndexed { at, section -> sectionItems(section, at == 0, state, trend, index) }
    if (model.conditional.isNotEmpty()) conditionalItems(model.conditional, model.sections.isEmpty(), state)
}

/**
 * Окна разбора листа [model], что открыл [state]: строки - [StatBreakdownSheet] ([shifts] - сдвиги эффектов захода по характеристике),
 * условной строки - [ConditionalBreakdownSheet].
 */
@Composable fun StatSheetWindows(game: GameUi, model: StatSheetModel, state: StatSheetState, shifts: Map<String, List<Shift>>) {
    state.breakdown?.let { StatBreakdownSheet(game, it, model.figures.stats, shifts, model.resists) { state.breakdown = null } }
    state.conditional?.let { ConditionalBreakdownSheet(game, it) { state.conditional = null } }
}

/**
 * Раздел - карточка из элементов списка: шапка значком и именем его цвета (со штрафом акта - плашкой), строки по одной, тонкая черта
 * между ними, сноска штрафа. Строки, что поднимают пределы, свёрнуты внизу одной строкой «Пределы · N упёрлись» ([LimitsFold]); касание
 * раскрывает их, сдвиг сравнения [trend] среди них раскрывает их сам. Последний элемент скругляет низ карточки.
 */
private fun LazyListScope.sectionItems(section: SheetSection, first: Boolean, state: StatSheetState, trend: StatTrend?, index: ContentIndex?) {
    val group = section.section
    val accent = group.accent()
    val shifted = trend != null && section.limits.any { trend.shift.of(it.stat) != null }
    val unfolded = section.limits.isNotEmpty() && state.limitsOpen(group, shifted)
    val open: (String) -> Unit = { state.breakdown = it }
    item(key = "sheet:${group.name}", contentType = HEAD) {
        SectionHead(group.glyph, section.title, accent, first) { section.penalty?.let { ToneTag(it.chip, Caution, size = TagSize.Medium) } }
    }
    section.lines.forEachIndexed { at, line ->
        val last = at == section.lines.lastIndex && section.penalty == null && section.limits.isEmpty()
        item(key = "stat:${line.stat}", contentType = ROW) { CardPiece(last) { StatCell(line, accent, trend, index, open) } }
    }
    section.penalty?.let { penalty ->
        item(key = "sheet:${group.name}:note", contentType = NOTE) {
            CardPiece(section.limits.isEmpty()) { Text(penalty.note, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) }
        }
    }
    if (section.limits.isEmpty()) return
    item(key = "sheet:${group.name}:limits", contentType = FOLD) {
        CardPiece(!unfolded) { LimitsFold(section.reached, unfolded, accent) { state.toggleLimits(group, shifted) } }
    }
    if (!unfolded) return
    section.limits.forEachIndexed { at, line ->
        item(key = "stat:${line.stat}", contentType = ROW) { CardPiece(at == section.limits.lastIndex, Modifier.animateItem()) { StatCell(line, accent, trend, index, open) } }
    }
}

/**
 * Последний раздел сводки - «С условием»: строки листа, что бой кладёт, пока держится условие; в итог своей характеристики они не входят
 * и печатаются один раз - здесь. Строка - характеристика с условием одной фразой и вклад справа; касание - разбор
 * ([ConditionalBreakdownSheet]).
 */
private fun LazyListScope.conditionalItems(lines: List<ConditionalLine>, first: Boolean, state: StatSheetState) {
    item(key = "sheet:conditional", contentType = HEAD) { ConditionalHead(first) }
    lines.forEachIndexed { at, line ->
        item(key = "conditional:$at:${line.row.condition}:${line.row.stat}:${line.row.op}", contentType = ROW) {
            CardPiece(at == lines.lastIndex) { ConditionalRow(line) { state.conditional = line.row } }
        }
    }
}

/** Шапка раздела «С условием»: значок-развилка и имя латунью. */
@Composable private fun ConditionalHead(first: Boolean) {
    Row(
        Modifier.cardTop(first).padding(horizontal = 12.dp, vertical = SummaryMetrics.headPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.AutoMirrored.Outlined.CallSplit, null, tint = ConditionalAccent, modifier = Modifier.size(SummaryMetrics.icon))
        Text(ui("stat.conditional.group"), color = ConditionalAccent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** Условная строка [line]: значок, характеристика с условием одной фразой и вклад справа; касание - [onOpen]. */
@Composable private fun ConditionalRow(line: ConditionalLine, onOpen: () -> Unit) {
    val title = remember(line) { conditionalTitle(line.stat, line.condition) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = SummaryMetrics.rowPadding).heightIn(min = SummaryMetrics.rowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatIcon(line.row.stat, ConditionalAccent, Modifier.size(SummaryMetrics.icon))
        Text(title, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(line.text, color = Parchment, style = MaterialTheme.typography.titleSmall, fontSize = SummaryMetrics.figure, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Цвет раздела «С условием» и условия в имени строки - латунь. */
internal val ConditionalAccent: Color get() = Brass

/** Имя условной строки одной фразой: характеристика [stat] и условие [condition], условие - цветом раздела ([ConditionalAccent]). */
private fun conditionalTitle(stat: String, condition: String): AnnotatedString = buildAnnotatedString {
    append(stat)
    append(' ')
    withStyle(SpanStyle(color = ConditionalAccent)) { append(condition) }
}

/** Имя условной строки [row] словами игры [game] - как у строки сводки; его печатает и шапка разбора. */
internal fun conditionalTitle(row: ConditionalStat, game: GameUi): AnnotatedString = conditionalTitle(statTitle(row.stat, game.lang), loc(LocaleKey.condition(row.condition)))

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

/** Шапка карточки раздела: глиф [glyph] и имя [title] цветом [accent], справа - [trailing] (плашка штрафа); [first] - без зазора сверху. */
@Composable private fun SectionHead(glyph: Glyph, title: String, accent: Color, first: Boolean, trailing: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.cardTop(first).padding(horizontal = 12.dp, vertical = SummaryMetrics.headPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlyphIcon(glyph, accent, Modifier.size(SummaryMetrics.icon))
        Text(title, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Верх карточки раздела: зазор от прошлой карточки (у первой - нет) и подложка со скруглённым верхом. */
private fun Modifier.cardTop(first: Boolean): Modifier = padding(top = if (first) 0.dp else SummaryMetrics.cardGap).fillMaxWidth().clip(TopShape).background(Panel)

/** Элемент карточки ниже шапки: черта сверху и подложка; [last] скругляет низ карточки. */
@Composable private fun CardPiece(last: Boolean, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape: Shape = if (last) BottomShape else RectangleShape
    Column(modifier.fillMaxWidth().clip(shape).background(Panel)) {
        HorizontalDivider(thickness = 1.dp, color = PanelRaised)
        content()
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
 * Плотность сводки героя: строка не ниже [rowHeight] с полями [rowPadding] - вместе 40 dp касания, значок строки и шапки раздела [icon],
 * итог [figure], шапка раздела с полями [headPadding], между карточками разделов [cardGap]; полоска сдвинутой строки [stripe], отступ её
 * вкладов [indent].
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
 * Строка сводки [line]. Без сдвига сравнения [trend] касание открывает разбор ([open]). Сдвинутая - полоска слева, цвет и знак отметки
 * ([TrendMark]); касание раскрывает под ней вклады ([ChangeSources]) с кнопкой разбора.
 */
@Composable private fun StatCell(line: SheetLine, accent: Color, trend: StatTrend?, index: ContentIndex?, open: (String) -> Unit) {
    val key = line.stat
    val mark = trend?.of(key, line.value, index)
    if (trend == null || mark == null) {
        StatRow(line, accent, null, Modifier.fillMaxWidth().clickable { open(key) }.padding(horizontal = 12.dp, vertical = SummaryMetrics.rowPadding))
        return
    }
    var unfolded by rememberSaveable(key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(mark.tint.copy(alpha = .06f)).drawBehind { drawRect(mark.tint, size = Size(SummaryMetrics.stripe.toPx(), size.height)) }) {
        StatRow(
            line,
            accent,
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
 * Строка сводки в одну линию: значок характеристики, имя и итог справа ([SheetLine.text]); сдвинутая ([mark]) - итог цветом отметки и её
 * знак. За числом - значок ауры и ✦ Предначертания, если они её сдвинули. Подписей под именем нет: части, доля в резерве, предел с
 * набранным и рейтинг защиты - только в разборе. Упёршееся в предел - цветом предупреждения. Сопротивление под штрафом акта - «по
 * листу → действует» ([PenalizedFigure]).
 */
@Composable private fun StatRow(line: SheetLine, accent: Color, mark: TrendMark?, modifier: Modifier) {
    Row(
        modifier.heightIn(min = SummaryMetrics.rowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatIcon(line.stat, accent, Modifier.size(SummaryMetrics.icon))
        Text(line.title, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        val penalty = line.penalty
        if (penalty != null) {
            PenalizedFigure(penalty)
        } else {
            Text(
                line.text,
                color = when {
                    mark != null -> mark.tint
                    line.clamped -> Caution
                    else -> Parchment
                },
                style = MaterialTheme.typography.titleSmall,
                fontSize = SummaryMetrics.figure,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        mark?.mark?.let { Text(it, color = mark.tint, fontSize = 11.sp) }
        if (line.aura) Tipped({ Tip(ui("hero.mark.aura"), ui("hero.mark.aura_body"), tint = Rune) }) { GlyphIcon(Glyph.AURA, Rune, Modifier.size(13.dp)) }
        if (line.fated) Tipped({ Tip(ui("hero.mark.fate"), ui("hero.mark.fate_body"), tint = Caution) }) { Text("✦", color = Caution, fontSize = 11.sp) }
    }
}

/**
 * Число сопротивления под штрафом акта: набранное по листу мелко, стрелка, после штрафа - если бой читает другое (предел или пол), и
 * действующее: упёршееся в предел - цветом предупреждения, ниже нуля - кровью.
 */
@Composable private fun PenalizedFigure(figure: PenaltyFigure) {
    val minor = MaterialTheme.typography.labelMedium
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = figure.spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(figure.sheet, color = Muted, style = minor, maxLines = 1)
        Text("→", color = Muted, style = minor)
        figure.penalized?.let {
            Text(it, color = Muted, style = minor, maxLines = 1)
            Text("→", color = Muted, style = minor)
        }
        Text(
            figure.effective,
            color = when {
                figure.below -> LifeRed
                figure.over -> Caution
                else -> Parchment
            },
            style = MaterialTheme.typography.titleSmall,
            fontSize = SummaryMetrics.figure,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Цвет запаса резерва аур: мана - синим, здоровье - красным. */
internal fun ReservePool.tint(): Color = when (this) {
    ReservePool.MANA -> ManaBlue
    ReservePool.LIFE -> LifeRed
}

/** Скругление верха и низа карточки раздела. */
private val TopShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
private val BottomShape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)

/** Виды элементов листа для переиспользования ленивым списком. */
private const val HEAD = "sheet-head"
private const val ROW = "sheet-row"
private const val NOTE = "sheet-note"
private const val FOLD = "sheet-fold"
