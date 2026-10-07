package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.BaseProperty
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.PropertyValue
import com.sperance.exileforge.core.display.Term
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.stateTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
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
 */
@Composable fun ModifierLine(index: ContentIndex, line: Line) = ModifierLine(lineText(index, line), Glyph.ofModifier(line.code, index))

/** A modifier's sentence already worded, under the glyph of what it changes. */
@Composable fun ModifierLine(text: String, glyph: Glyph = Glyph.INFO) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(glyph.vector, null, tint = Rune, modifier = Modifier.size(16.dp))
        Text(text, color = ModBlue, style = MaterialTheme.typography.bodyMedium)
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
 * Предмет как страница «Реликвария» (3.88.6, выбор владельца): рамка в свете редкости, иконка в гнезде, имя серифами,
 * под ним что это и плашки уровня, качества и цены; база - крупными числами в три колонки; строки - со значком тира и вилкой
 * справа, нажатие открывает тир и место ролла. Уникальная и мифическая вещь лежат в «Астролябии»: кольца вокруг гнезда, искры
 * или звёзды, завитки по углам и номер экземпляра печатью. [totals] - итог героя «сейчас → станет» под строками,
 * [requirementsMet] красит строку требований. Всё напечатанное - вид [item] (3.0.0): копия поверх шаблона и контента.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCard(
    item: ItemView,
    enabled: Boolean = true,
    selected: Boolean = false,
    detailed: Boolean = false,
    actionLabel: String = ui("common.open"),
    /** What the merchant pays for this copy (2.46.0); it replaces the template's bare base price. */
    price: Long? = null,
    /** Whether the card shows its [actionLabel]: a short card always, a full one only when it leads somewhere (3.2.0). */
    action: Boolean = !detailed,
    /** The padlock (3.30.0): the copy is locked against selling and listing. */
    locked: Boolean = item.item.locked,
    /** A command about this copy waits for the network (3.30.0). */
    waiting: Boolean = false,
    /** Итог героя «если надеть» (3.88.6) - таблица под строками; null - без неё. */
    totals: (@Composable (RelicLook) -> Unit)? = null,
    /** Выполнены ли требования (3.88.6): зелёная галка, красный крест или нейтрально, когда героя не с чем сравнить. */
    requirementsMet: Boolean? = null,
    onClick: () -> Unit = {},
) {
    val look = relicLook(item.rarity)
    val legend = look.legend != null
    val serial = item.item.serial
    val shape = RoundedCornerShape(22.dp)
    val rolled = item.lines
    val shown = if (detailed) rolled else rolled.take(3)
    var opened by remember { mutableStateOf<ItemLine?>(null) }
    Column(
        Modifier.fillMaxWidth().relicGround(look, shape, selected)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 18.dp, end = 18.dp, top = if (legend) 6.dp else 18.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Плашки состояний и замок - в углу, их видят, а не читают.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item.states.forEach { Icon(stateGlyph(it), stateTitle(it), tint = stateColor(it), modifier = Modifier.size(15.dp)) }
            if (locked) Icon(Icons.Outlined.Lock, ui("item.locked"), tint = GoldBright, modifier = Modifier.size(15.dp))
            if (waiting) PendingMark()
            Spacer(Modifier.weight(1f))
            if (detailed && rolled.isNotEmpty() && !legend) item.summary.quality?.let { RollRing(it) }
            if (selected) Icon(Icons.Outlined.CheckCircle, ui("card.selected"), tint = GoldBright, modifier = Modifier.size(22.dp))
        }
        if (legend) {
            Astrolabe(look, size = if (detailed) 200.dp else 140.dp, socket = if (detailed) 108.dp else 76.dp) {
                ItemIcon(item, look.rarity, Modifier.size(if (detailed) 66.dp else 46.dp))
            }
        } else {
            RelicSocket(item, look, 88.dp)
        }
        Text(
            item.title,
            color = look.name,
            style = look.nameStyle(if (legend) 23 else 22),
            textAlign = TextAlign.Center,
            maxLines = if (detailed) 4 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        val kind = listOfNotNull(item.weaponType?.let { weaponTitle(it) } ?: slotTitle(item.slot), rarityTitle(item.rarity)).joinToString(" · ")
        Text(
            if (legend) kind.uppercase() else kind,
            color = look.muted,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = if (legend) 2.4.sp else .4.sp,
            textAlign = TextAlign.Center,
        )
        // Английское торговое имя (2.51.0): по нему ищут на аукционе.
        if (detailed) item.trade?.let { Text(it, color = look.faint, style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic) }
        if (serial > 0) SerialSeal(serial, look)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RelicChip { Text(ui("row.level", item.level), color = Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
            QualityBadge(item)
            item.baseQuality?.let { RelicChip { Text(ui("card.base_quality", it), color = Parchment, style = MaterialTheme.typography.labelMedium) } }
            (price ?: item.template.price?.toLong())?.let { RelicChip { GoldPrice(it) } }
        }
        RelicDivider(look, Modifier.padding(horizontal = 4.dp))
        BaseFigures(item, look)
        if (shown.isNotEmpty()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEachIndexed { i, line ->
                    RelicModLine(line, if (detailed) ({ opened = line }) else null)
                    // Врождённые строки - над чертой: они от основы, а не от ролла.
                    val next = shown.getOrNull(i + 1)
                    if (line.marks.kind == AffixKind.IMPLICIT && next != null && next.marks.kind != AffixKind.IMPLICIT) {
                        HorizontalDivider(thickness = .5.dp, color = Color.White.copy(alpha = .08f))
                    }
                }
            }
        }
        if (!detailed && rolled.size > shown.size) MutedText(ui("card.more_properties", rolled.size - shown.size), style = MaterialTheme.typography.labelMedium)
        if (detailed) {
            item.description.takeIf { it.isNotBlank() }?.let {
                Text(
                    "«$it»",
                    color = look.muted,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            // Мастер (3.91.0, сервер 1.82.0): кто и когда изготовил снаряжение ремеслом.
            item.item.maker?.let { maker ->
                Text(ui("card.maker", maker.hero, stampText(maker.at)), color = look.muted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
            totals?.let {
                HorizontalDivider(thickness = 1.dp, color = look.gold.copy(alpha = .2f))
                it(look)
            }
            RelicFolds(item, look, requirementsMet)
        }
        if (action) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(actionLabel.uppercase(), color = Gold, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Outlined.ChevronRight, null, tint = Gold)
            }
        }
    }
    opened?.let { ModifierInfoSheet(it) { opened = null } }
}

/** Плашка шапки карточки: тихий фон-пилюля под уровнем, качеством и ценой. */
@Composable private fun RelicChip(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.height(24.dp).background(Color.White.copy(alpha = .05f), RoundedCornerShape(12.dp)).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * База вещи крупными числами (3.88.6): по три в ряд, под числом - что оно считает; число, сдвинутое строками копии, -
 * голубое. Свойство без характеристики остаётся фразой.
 */
@Composable private fun BaseFigures(item: ItemView, look: RelicLook) {
    val figures = item.base.flatMap { property -> property.values.filter { it.stat.isNotBlank() } }
    val sentences = item.base.filter { property -> property.values.all { it.stat.isBlank() } }
    figures.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            row.forEachIndexed { i, value ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(look.gold.copy(alpha = .16f)))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value.text, color = if (value.augmented) Rune else Color(0xFFFFF6E6), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(statTitle(value.stat), color = look.muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
    sentences.forEach { BasePropertyLine(it) }
}

/**
 * Свёрнутые строки внизу карточки (3.88.6): требования одной строкой с отметкой, выполнены ли они, и термины строк -
 * раскрываются нажатием.
 */
@Composable private fun RelicFolds(item: ItemView, look: RelicLook, met: Boolean?) {
    val terms = remember(item) { Term.ofStats(item.lines.flatMap { line -> line.definition?.effects.orEmpty().map { it.stat } }) }
    var termsOpen by remember(item) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        item.requirements.takeIf { it.isNotEmpty() }?.let { needs ->
            FoldRow(
                ui("relic.requirements"),
                needs.joinToString(" · ") + when (met) {
                    true -> " ✓"
                    false -> " ✕"
                    null -> ""
                },
                when (met) {
                    true -> Vital
                    false -> LifeRed
                    null -> look.muted
                },
                open = null,
            )
        }
        if (terms.isNotEmpty()) {
            FoldRow(ui("term.title"), terms.size.toString(), look.muted, open = termsOpen) { termsOpen = !termsOpen }
            if (termsOpen) TermsBlock(terms, Modifier.padding(bottom = 6.dp))
        }
    }
}

/** Строка свёртки: название слева, сводка справа и шеврон, когда раскрывается. */
@Composable private fun FoldRow(title: String, value: String, tint: Color, open: Boolean?, onClick: () -> Unit = {}) {
    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = .06f))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(enabled = open != null, role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = Parchment, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text(value, color = tint, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, maxLines = 2)
        if (open != null) Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** «Ждёт отправки» (3.30.0): a command about this thing waits for the network; its result comes with the server's answer. */
@Composable fun PendingMark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Outlined.Schedule, null, tint = Muted, modifier = Modifier.size(13.dp))
        Text(ui("link.pending"), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
