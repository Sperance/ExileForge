package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.BaseProperty
import com.sperance.exileforge.core.display.CardSeal
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemNeed
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.NeedKind
import com.sperance.exileforge.core.display.PropertyValue
import com.sperance.exileforge.core.display.Term
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.sidesText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.stateTitle
import com.sperance.exileforge.core.display.statsOf
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.FlaskBonuses
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.*

/**
 * One fixed line — a tree node's, a class's — as a whole sentence.
 *
 * The server's dictionary holds the phrasing — "+{0} to armour" — and the line's values fill it,
 * so there is no label to put on the left of a number any more. The glyph is the characteristic
 * the modifier's first effect changes.
 *
 * Замена значения - цена узла, не бонус: строка цветом потери. [explains] false - строка внутри выбираемого касанием (вариант узла):
 * она не нажимается, её характеристики объясняет знак в углу ([StatNoteCorner]).
 */
@Composable fun ModifierLine(index: ContentIndex, line: Line, explains: Boolean = true) {
    val penalty = index.modifier(line.code)?.let { modifierText(index).penalty(it) } == true
    val stats = remember(index, line.code, explains) { if (explains) index.statsOf(line.code) else emptyList() }
    ModifierLine(lineText(index, line), Glyph.ofModifier(line.code, index), if (penalty) LifeRed else ModBlue, stats, index)
}

/**
 * A modifier's sentence already worded, under the glyph of what it changes; [tint] - цвет текста (штраф - потерей). Строка объясняет
 * характеристики [stats], что она двигает ([StatNoted]: касание - окно с их описанием).
 */
@Composable fun ModifierLine(
    text: String,
    glyph: Glyph = Glyph.INFO,
    tint: Color = ModBlue,
    stats: List<String> = emptyList(),
    index: ContentIndex? = null,
) {
    val ink = if (tint == ModBlue) Rune else tint
    StatNoted(stats, index, Modifier.fillMaxWidth(), ink) {
        Icon(glyph.vector, null, tint = ink, modifier = Modifier.padding(end = 10.dp).size(16.dp))
        Text(text, color = tint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/**
 * A base property as one sentence, with the number the item really carries.
 *
 * The dictionary's template is filled with the folded value, and that value is coloured when a
 * modifier moved it — the base it started from follows in brackets, so the line answers both
 * "how much" and "why is it not the number on the shelf".
 */
fun basePropertyText(property: BaseProperty, withBase: Boolean): AnnotatedString = buildAnnotatedString {
    fun value(one: PropertyValue) {
        if (!one.augmented) {
            append(one.text)
            return
        }
        withStyle(SpanStyle(color = Rune, fontWeight = FontWeight.SemiBold)) { append(one.text) }
        if (withBase) withStyle(SpanStyle(color = Muted)) { append(" (${one.baseText})") }
    }
    if (property.template.isBlank()) {
        property.values.forEachIndexed { index, one ->
            if (index > 0) append(" · ")
            value(one)
            if (one.stat.isNotBlank()) {
                append(" ")
                append(statTitle(one.stat))
            }
        }
        return@buildAnnotatedString
    }
    var rest = property.template
    while (true) {
        val next = property.values.indices
            .mapNotNull { index -> rest.indexOf("{$index}").takeIf { it >= 0 }?.let { it to index } }
            .minByOrNull { it.first } ?: break
        val (at, index) = next
        append(rest.substring(0, at))
        value(property.values[index])
        rest = rest.substring(at + "{$index}".length)
    }
    append(rest)
}

/**
 * One line of an item's base, drawn like a modifier but carrying the folded number.
 */
@Composable fun BasePropertyLine(property: BaseProperty, withBase: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Glyph.ofStat(property.values.firstOrNull()?.stat.orEmpty()).vector, null, tint = Rune, modifier = Modifier.size(16.dp))
        Text(basePropertyText(property, withBase), color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Предмет как страница «Реликвария» (выбор владельца): рамка в свете редкости, иконка в гнезде, имя серифами,
 * под ним что это и плашки уровня, качества и цены; строки - со значком тира, нажатие открывает тир и место ролла.
 * Полная карточка начинается «Медальоном» ([MedallionHead]): база слева от гнезда, кольцо качества роллов и значки состояний
 * справа, у легенды гнездо в «Астролябии» 112dp; касание имени - английское имя для поиска на аукционе, касание значка - что он
 * значит. Короткая - значки в углу, гнездо по центру, база крупными числами под разделителем. Уникальная и мифическая вещь -
 * искры или звёзды, завитки по углам и номер экземпляра. [totals] - итог героя «сейчас → станет» под строками,
 * [unmet] отмечает чипы требований. Всё напечатанное - вид [item]: копия поверх шаблона и контента.
 *
 * Цена торговца копии заменяет голую цену шаблона; [priced] false - плашки цены нет (место само печатает цену сделки). Подпись действия - у короткой карточки всегда, у полной - когда оно куда-то ведёт. Замок
 * - копия заперта от продажи и лотов; «ждёт отправки» - команда о копии ждёт сети. Итог «если надеть» - таблица под строками; отметки
 * требований - галка или крест, null - не с чем сравнить.
 * Строки - в порядке [ItemView.lineGroups]: врождённые над чертой, у полной - особые источники и [seals] (`GameUi.itemSeals`: захват
 * и качество карты с Атласом героя) плашками особого блока, основные - списком, под ними стороны аффиксов; у короткой - тот же порядок
 * без плашек. Судьбоносная строка - под строками вещи; мастер - кто и когда изготовил снаряжение.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCard(
    item: ItemView,
    enabled: Boolean = true,
    selected: Boolean = false,
    detailed: Boolean = false,
    actionLabel: String = ui("common.open"),
    price: Long? = null,
    priced: Boolean = true,
    action: Boolean = !detailed,
    locked: Boolean = item.item.locked,
    waiting: Boolean = false,
    totals: (@Composable (RelicLook) -> Unit)? = null,
    unmet: Set<NeedKind>? = null,
    seals: List<CardSeal> = emptyList(),
    onClick: () -> Unit = {},
) {
    val look = relicLook(item.rarity)
    val legend = look.legend != null
    val shape = RoundedCornerShape(22.dp)
    val rolled = item.orderedLines
    val chipPrice = (price ?: item.template.price?.toLong()).takeIf { priced }
    var opened by remember { mutableStateOf<ItemLine?>(null) }
    Column(
        Modifier.fillMaxWidth().relicGround(look, shape, selected, sky = detailed)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 18.dp, end = 18.dp, top = if (legend && !detailed) 6.dp else 18.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (detailed) {
            MedallionHead(item, look, chipPrice, locked, waiting, selected)
        } else {
            ShortHead(item, look, chipPrice, locked, waiting, selected)
        }
        if (detailed) {
            RelicLines(item, look, seals) { opened = it }
        } else {
            ShortLines(rolled.take(SHORT_LINES))
            if (rolled.size > SHORT_LINES) MutedText(ui("card.more_properties", rolled.size - SHORT_LINES), style = MaterialTheme.typography.labelMedium)
        }
        FatedSlot(item.item, explained = detailed)
        if (detailed && item.slot.isFlask) FlaskYours(look)
        if (detailed) {
            item.item.maker?.let { maker ->
                Text(ui("card.maker", maker.hero, stampText(maker.at)), color = look.muted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
            totals?.let {
                HorizontalDivider(thickness = 1.dp, color = look.gold.copy(alpha = .2f))
                it(look)
            }
            RelicFolds(item, look, unmet)
        }
        if (action) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(actionLabel.uppercase(), color = Gold, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
            }
        }
    }
    opened?.let { ModifierInfoSheet(it, item.index) { opened = null } }
}

/** Значки состояний копии, замок и «ждёт отправки»; у полной карточки ([tipped]) касание значка - подсказка, что он значит. */
@Composable private fun StateMarks(item: ItemView, locked: Boolean, waiting: Boolean, tipped: Boolean) {
    item.states.forEach { state ->
        Tipped(if (tipped) ({ Tip(stateTitle(state), item.stateHint(state), tint = stateColor(state)) }) else null) {
            Icon(stateGlyph(state), stateTitle(state), tint = stateColor(state), modifier = Modifier.size(15.dp))
        }
    }
    if (locked) {
        Tipped(if (tipped) ({ Tip(ui("item.locked"), ui("item.locked_hint")) }) else null) {
            Icon(Icons.Outlined.Lock, ui("item.locked"), tint = GoldBright, modifier = Modifier.size(15.dp))
        }
    }
    if (waiting) PendingMark()
}

/** Отметка выбранной карточки. */
@Composable private fun SelectedMark() {
    Icon(Icons.Outlined.CheckCircle, ui("card.selected"), tint = GoldBright, modifier = Modifier.size(22.dp))
}

/** Имя предмета цветом редкости по центру; под ним - «тип или оружие · редкость» (у легенды заглавными). */
@Composable private fun RelicTitle(item: ItemView, look: RelicLook, size: Int, maxLines: Int) {
    val legend = look.legend != null
    Text(item.title, color = look.name, style = look.nameStyle(size), textAlign = TextAlign.Center, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    val kind = listOfNotNull(item.weaponType?.let { weaponTitle(it) } ?: slotTitle(item.slot), rarityTitle(item.rarity)).joinToString(" · ")
    Text(
        if (legend) kind.uppercase() else kind,
        color = look.muted,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = if (legend) 2.4.sp else .4.sp,
        textAlign = TextAlign.Center,
    )
}

/**
 * Плашки шапки одной линией: уровень (кроме [noLevel] - уровень уже напечатан у гнезда), качество и цена [price]
 * (null - без плашки цены); [serial] - номер экземпляра легенды последней плашкой.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelicChips(item: ItemView, look: RelicLook, price: Long?, noLevel: Boolean = false, serial: Boolean = false) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (!noLevel) {
            RelicChip { Text(item.mapLevel?.let { ui("card.map_level", it) } ?: ui("row.level", item.level), color = Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
        }
        QualityBadge(item)
        price?.let { RelicChip { GoldPrice(it) } }
        if (serial && item.item.serial > 0) SerialChip(item.item.serial, look)
    }
}

/** Шапка короткой карточки: значки в углу, гнездо или «Астролябия» по центру, имя, тип, печать номера и плашки. */
@Composable private fun ShortHead(item: ItemView, look: RelicLook, price: Long?, locked: Boolean, waiting: Boolean, selected: Boolean) {
    val legend = look.legend != null
    Row(
        Modifier.fillMaxWidth().then(if (legend) Modifier.padding(top = LEGEND_CORNER_TOP, start = LEGEND_CORNER_SIDE, end = LEGEND_CORNER_SIDE) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StateMarks(item, locked, waiting, tipped = false)
        Spacer(Modifier.weight(1f))
        if (selected) SelectedMark()
    }
    if (legend) {
        Astrolabe(look, size = 140.dp, socket = 76.dp) { ItemIcon(item, look.rarity, Modifier.size(46.dp)) }
    } else {
        RelicSocket(item, look, 88.dp)
    }
    RelicTitle(item, look, if (legend) 23 else 22, maxLines = 2)
    if (item.item.serial > 0) SerialSeal(item.item.serial, look)
    RelicChips(item, look, price)
    RelicDivider(look, Modifier.padding(horizontal = 4.dp))
    BaseFigures(item, look)
}

/**
 * Шапка полной карточки - «Медальон»: в ряду гнезда слева база (до [MEDALLION_FIGURES] чисел; у карты без базы - уровень карты),
 * в центре гнездо или «Астролябия» 112dp, справа кольцо качества роллов и значки состояний. Ниже имя (касание - английское имя
 * для поиска на аукционе), тип, плашки и разделитель; база сверх ряда гнезда - под разделителем.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedallionHead(item: ItemView, look: RelicLook, price: Long?, locked: Boolean, waiting: Boolean, selected: Boolean) {
    val figures = item.base.flatMap { property -> property.values.filter { it.stat.isNotBlank() } }
    val mapLevel = item.mapLevel.takeIf { figures.isEmpty() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            figures.take(MEDALLION_FIGURES).forEach { MedallionFigure(it.text, statTitle(it.stat), if (it.augmented) Rune else FigureInk, look) }
            mapLevel?.let { MedallionFigure(it.toString(), ui("card.map_level_short"), FigureInk, look) }
        }
        if (look.legend != null) {
            Astrolabe(look, size = 112.dp, socket = 72.dp) { ItemIcon(item, look.rarity, Modifier.size(48.dp)) }
        } else {
            RelicSocket(item, look, 72.dp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.orderedLines.isNotEmpty()) item.summary.quality?.let { RollRing(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                StateMarks(item, locked, waiting, tipped = true)
                if (selected) SelectedMark()
            }
        }
    }
    val trade = item.trade
    Tipped(trade?.let { { Tip(it, ui("card.trade_hint")) } }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            RelicTitle(item, look, MEDALLION_NAME, maxLines = 4)
        }
    }
    RelicChips(item, look, price, noLevel = mapLevel != null, serial = true)
    RelicDivider(look, Modifier.padding(horizontal = 4.dp))
    BaseFigures(item, look, skip = MEDALLION_FIGURES)
}

/** Число базы у гнезда «Медальона»: крупно цветом [ink], под ним подпись [label] цветом `muted`, по правому краю. */
@Composable private fun MedallionFigure(value: String, label: String, ink: Color, look: RelicLook) {
    Column(horizontalAlignment = Alignment.End) {
        val figure = MaterialTheme.typography.titleMedium
        BasicText(
            value,
            style = figure.copy(color = ink, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = FIGURE_MIN, maxFontSize = figure.fontSize, stepSize = FIGURE_STEP),
        )
        Text(label, color = look.muted, style = MaterialTheme.typography.labelSmall, letterSpacing = .8.sp, textAlign = TextAlign.End, maxLines = 2)
    }
}

/** Номер экземпляра легенды плашкой шапки: Cinzel цветом акцента в золотой нити. */
@Composable private fun SerialChip(serial: Long, look: RelicLook) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.height(RelicChipHeight).background(look.gold.copy(alpha = .08f), shape).border(1.dp, look.gold.copy(alpha = .4f), shape).padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(ui("relic.serial", serial), color = look.accent, style = TextStyle(fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = .6.sp), maxLines = 1)
    }
}

/** Чисел базы в ряду гнезда «Медальона»; остальные - под разделителем. */
private const val MEDALLION_FIGURES = 3

/** Кегль имени полной карточки. */
private const val MEDALLION_NAME = 21

/** Цвет крупного числа базы, не сдвинутого строками копии. */
private val FigureInk = Color(0xFFFFF6E6)

/** Высота плашек шапки карточки: уровень, жетон качества, цена. */
internal val RelicChipHeight = 24.dp

/** Плашка шапки карточки: тихий фон-пилюля под уровнем, качеством и ценой. */
@Composable private fun RelicChip(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.height(RelicChipHeight).background(Color.White.copy(alpha = .05f), RoundedCornerShape(12.dp)).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** Строк у короткой карточки. */
private const val SHORT_LINES = 3

/** Строки короткой карточки: порядок [ItemView.lineGroups] без плашек, врождённые над чертой. */
@Composable private fun ShortLines(lines: List<ItemLine>) {
    if (lines.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEachIndexed { i, line ->
            RelicModLine(line, null)
            if (implicitEnds(line, lines.getOrNull(i + 1))) HorizontalDivider(thickness = .5.dp, color = Color.White.copy(alpha = .08f))
        }
    }
}

/** Черта под врождёнными: [line] - последняя врождённая, а за ней есть [next]. */
internal fun implicitEnds(line: ItemLine, next: ItemLine?): Boolean = line.marks.kind == AffixKind.IMPLICIT && next != null && next.marks.kind != AffixKind.IMPLICIT

/**
 * Строки полной карточки по группам [ItemView.lineGroups]: врождённые и черта под ними, особый блок - сначала плашки [seals], затем
 * строки особых источников плашками, - основные списком и стороны аффиксов под ними. Касание строки - [onOpen] (тир и место ролла).
 */
@Composable private fun RelicLines(item: ItemView, look: RelicLook, seals: List<CardSeal>, onOpen: (ItemLine) -> Unit) {
    val groups = item.lineGroups
    val implicit = groups.firstOrNull { it.kind == AffixKind.IMPLICIT }?.lines.orEmpty()
    val special = groups.filter { it.special }.flatMap { it.lines }
    val main = groups.filter { it.kind != AffixKind.IMPLICIT && !it.special }.flatMap { it.lines }
    if (implicit.isEmpty() && special.isEmpty() && main.isEmpty() && seals.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        implicit.forEach { RelicModLine(it, { onOpen(it) }) }
        if (implicit.isNotEmpty() && (special.isNotEmpty() || main.isNotEmpty() || seals.isNotEmpty())) {
            HorizontalDivider(thickness = .5.dp, color = Color.White.copy(alpha = .08f))
        }
        if (seals.isNotEmpty() || special.isNotEmpty()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                seals.forEach { CardSealPlaque(it) }
                special.forEach { LinePlaque(it) { onOpen(it) } }
            }
        }
        main.forEach { RelicModLine(it, { onOpen(it) }) }
        item.sides?.let {
            Text(sidesText(it), color = look.faint, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * База вещи крупными числами без первых [skip] (они - у гнезда): по три в ряд, под числом - что оно считает; число, сдвинутое строками копии, -
 * голубое. Свойство без характеристики остаётся фразой.
 */
@Composable private fun BaseFigures(item: ItemView, look: RelicLook, skip: Int = 0) {
    val figures = item.base.flatMap { property -> property.values.filter { it.stat.isNotBlank() } }.drop(skip)
    val sentences = item.base.filter { property -> property.values.all { it.stat.isBlank() } }
    figures.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            row.forEachIndexed { i, value ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(look.gold.copy(alpha = .16f)))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value.text, color = if (value.augmented) Rune else FigureInk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(statTitle(value.stat), color = look.muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
    sentences.forEach { BasePropertyLine(it) }
}

/**
 * Свёрнутые строки внизу карточки: требования и термины строк. Требования всегда свёрнуты одной строкой цветом вердикта -
 * «Можно надеть» `Vital`, «Нельзя надеть» `LifeRed`, без героя для сравнения - «Требования»; касание раскрывает чипы с переносом,
 * у каждого своя отметка, и раскрытость помнится на устройстве ([GameSettings.requirementsOpen]) для всех карточек.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelicFolds(item: ItemView, look: RelicLook, unmet: Set<NeedKind>?) {
    val terms = remember(item) { Term.ofItem(item) }
    var termsOpen by remember(item) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        item.requirements.takeIf { it.isNotEmpty() }?.let { needs ->
            val barred = unmet?.let { lacking -> needs.any { it.kind in lacking } }
            val (title, tint) = when (barred) {
                null -> ui("relic.requirements") to Parchment
                true -> ui("relic.cannot_wear") to LifeRed
                false -> ui("relic.can_wear") to Vital
            }
            val settings = LocalSettings.current
            val edit = LocalSettingsEdit.current
            var held by remember { mutableStateOf(settings.requirementsOpen) }
            val open = if (edit != null) settings.requirementsOpen else held
            FoldRow(title, needs.size.toString(), look.muted, open = open, titleTint = tint) {
                edit?.invoke { copy(requirementsOpen = !requirementsOpen) } ?: run { held = !held }
            }
            FoldBody(open) {
                FlowRow(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    needs.forEach { need -> NeedChip(need, unmet?.let { need.kind !in it }, look) }
                }
            }
        }
        if (terms.isNotEmpty()) {
            FoldRow(ui("term.title"), terms.size.toString(), look.muted, open = termsOpen) { termsOpen = !termsOpen }
            FoldBody(termsOpen) { TermsBlock(terms, item.index.campaign.combat, Modifier.padding(bottom = 6.dp)) }
        }
    }
}

/** Чип требования: подпись и отметка - ✓ `Vital`, ✕ `LifeRed`, без отметки и цвета, когда [met] неизвестно. */
@Composable private fun NeedChip(need: ItemNeed, met: Boolean?, look: RelicLook) {
    val tint = when (met) {
        true -> Vital
        false -> LifeRed
        null -> look.muted
    }
    val mark = when (met) {
        true -> " ✓"
        false -> " ✕"
        null -> ""
    }
    RelicChip { Text(need.text + mark, color = tint, style = MaterialTheme.typography.labelMedium) }
}

/** Строка свёртки: название слева цветом [titleTint], сводка справа и шеврон, когда раскрывается. */
@Composable private fun FoldRow(title: String, value: String, tint: Color, open: Boolean?, titleTint: Color = Parchment, onClick: () -> Unit = {}) {
    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = .06f))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).then(if (open != null) Modifier.foldToggle(open, title, onClick) else Modifier).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = titleTint, style = MaterialTheme.typography.labelLarge, fontWeight = if (titleTint == Parchment) null else FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = tint, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, maxLines = 2)
        if (open != null) FoldChevron(open)
    }
}

/** Бонусы героя к флягам для блока «У вас» карточки фляги; null - героя нет, блок не рисуется. */
val LocalFlaskBonuses = compositionLocalOf<FlaskBonuses?> { null }

/**
 * Блок «У вас» под числами фляги: бонусы героя ко всем флягам свёрнуты строкой с их числом, раскрытые - итог каждого и под ним
 * его источники с вкладом. Числа фляги выше - сама фляга; бонусов нет - блока нет.
 */
@Composable private fun FlaskYours(look: RelicLook) {
    val bonuses = LocalFlaskBonuses.current?.bonuses.orEmpty()
    if (bonuses.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        FoldRow(ui("card.flask_yours"), bonuses.size.toString(), look.muted, open = open) { open = !open }
        FoldBody(open) {
            Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MutedText(ui("card.flask_yours_hint"), style = MaterialTheme.typography.labelSmall)
                bonuses.forEach { bonus ->
                    YoursRow(statTitle(bonus.stat), bonus.total, bonus.better, MaterialTheme.typography.labelLarge, Parchment)
                    bonus.sources.forEach { source -> YoursRow(source.title, source.value, source.better, MaterialTheme.typography.labelSmall, look.muted, Modifier.padding(start = 12.dp)) }
                }
            }
        }
    }
}

/** Строка блока «У вас»: название слева, число справа цветом оценки - рост `Vital`, потеря `LifeRed`, без оценки - [ink]. */
@Composable private fun YoursRow(title: String, value: String, better: Boolean?, style: TextStyle, ink: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = ink, style = style, modifier = Modifier.weight(1f))
        Text(value, color = better?.let { if (it) Vital else LifeRed } ?: ink, style = style, fontWeight = FontWeight.SemiBold)
    }
}

/** «Ждёт отправки»: a command about this thing waits for the network; its result comes with the server's answer. */
@Composable fun PendingMark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Outlined.Schedule, null, tint = Muted, modifier = Modifier.size(13.dp))
        Text(ui("link.pending"), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Отступ значков шапки легенды от верха и края - мимо завитков рамки. */
private val LEGEND_CORNER_TOP = 14.dp
private val LEGEND_CORNER_SIDE = 22.dp

/** Наименьший кегль числа базы у гнезда и шаг его уменьшения: длинный диапазон («120–245») сжимается, а не обрезается. */
private val FIGURE_MIN = 10.sp
private val FIGURE_STEP = 0.5.sp
