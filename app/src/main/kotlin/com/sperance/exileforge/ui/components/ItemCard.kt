package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.BaseProperty
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
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.stateHint
import com.sperance.exileforge.core.display.stateTitle
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
 * Замена значения - цена узла, не бонус: строка цветом потери.
 */
@Composable fun ModifierLine(index: ContentIndex, line: Line) {
    val penalty = index.modifier(line.code)?.let { modifierText(index).penalty(it) } == true
    ModifierLine(lineText(index, line), Glyph.ofModifier(line.code, index), if (penalty) LifeRed else ModBlue)
}

/** A modifier's sentence already worded, under the glyph of what it changes; [tint] - цвет текста (штраф - потерей). */
@Composable fun ModifierLine(text: String, glyph: Glyph = Glyph.INFO, tint: Color = ModBlue) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(glyph.vector, null, tint = if (tint == ModBlue) Rune else tint, modifier = Modifier.size(16.dp))
        Text(text, color = tint, style = MaterialTheme.typography.bodyMedium)
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
 * под ним что это и плашки уровня, качества и цены; база - крупными числами в три колонки; строки - со значком тира и вилкой
 * справа, нажатие открывает тир и место ролла. Уникальная и мифическая вещь лежат в «Астролябии»: кольца вокруг гнезда, искры
 * или звёзды, завитки по углам и номер экземпляра печатью. [totals] - итог героя «сейчас → станет» под строками,
 * [unmet] отмечает чипы требований. Всё напечатанное - вид [item]: копия поверх шаблона и контента.
 *
 * Цена торговца копии заменяет голую цену шаблона. Подпись действия - у короткой карточки всегда, у полной - когда оно куда-то ведёт. Замок
 * - копия заперта от продажи и лотов; «ждёт отправки» - команда о копии ждёт сети. Итог «если надеть» - таблица под строками; отметки
 * требований - галка или крест, null - не с чем сравнить. Плашки состояний и замок - в углу (у легенды ниже и от края, мимо завитков
 * рамки), у полной карточки касание значка открывает подсказку, что он значит; кольцо качества роллов - у всех редкостей по строкам с вилкой. Английское торговое имя - для поиска на аукционе. Врождённые
 * строки - над чертой; судьбоносная строка - под строками вещи; мастер - кто и когда изготовил снаряжение. [notes] - общий блок
 * над модификаторами (`GameUi.itemNotes`: качество карты с Атласом героя).
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
    action: Boolean = !detailed,
    locked: Boolean = item.item.locked,
    waiting: Boolean = false,
    totals: (@Composable (RelicLook) -> Unit)? = null,
    unmet: Set<NeedKind>? = null,
    notes: List<String> = emptyList(),
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
        Modifier.fillMaxWidth().relicGround(look, shape, selected, sky = detailed)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 18.dp, end = 18.dp, top = if (legend) 6.dp else 18.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().then(if (legend) Modifier.padding(top = LEGEND_CORNER_TOP, start = LEGEND_CORNER_SIDE, end = LEGEND_CORNER_SIDE) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item.states.forEach { state ->
                Tipped(if (detailed) ({ Tip(stateTitle(state), stateHint(state), tint = stateColor(state)) }) else null) {
                    Icon(stateGlyph(state), stateTitle(state), tint = stateColor(state), modifier = Modifier.size(15.dp))
                }
            }
            if (locked) {
                Tipped(if (detailed) ({ Tip(ui("item.locked"), ui("item.locked_hint")) }) else null) {
                    Icon(Icons.Outlined.Lock, ui("item.locked"), tint = GoldBright, modifier = Modifier.size(15.dp))
                }
            }
            if (waiting) PendingMark()
            Spacer(Modifier.weight(1f))
            if (detailed && rolled.isNotEmpty()) item.summary.quality?.let { RollRing(it) }
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
        if (detailed) item.trade?.let { Text(it, color = look.faint, style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic) }
        if (serial > 0) SerialSeal(serial, look)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            RelicChip { Text(ui("row.level", item.level), color = Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
            QualityBadge(item)
            (price ?: item.template.price?.toLong())?.let { RelicChip { GoldPrice(it) } }
        }
        RelicDivider(look, Modifier.padding(horizontal = 4.dp))
        BaseFigures(item, look)
        CardNotes(notes)
        if (shown.isNotEmpty()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEachIndexed { i, line ->
                    RelicModLine(line, if (detailed) ({ opened = line }) else null)
                    val next = shown.getOrNull(i + 1)
                    if (line.marks.kind == AffixKind.IMPLICIT && next != null && next.marks.kind != AffixKind.IMPLICIT) {
                        HorizontalDivider(thickness = .5.dp, color = Color.White.copy(alpha = .08f))
                    }
                }
            }
        }
        if (!detailed && rolled.size > shown.size) MutedText(ui("card.more_properties", rolled.size - shown.size), style = MaterialTheme.typography.labelMedium)
        FatedSlot(item.item)
        if (detailed && item.slot.isFlask) FlaskYours(look)
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
    opened?.let { ModifierInfoSheet(it) { opened = null } }
}

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

/** Общий блок над модификаторами: строка на заметку ядра, значком сведений, как фраза базы. */
@Composable private fun CardNotes(notes: List<String>) {
    notes.forEach { note ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Glyph.INFO.vector, null, tint = Rune, modifier = Modifier.size(16.dp))
            Text(note, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * База вещи крупными числами: по три в ряд, под числом - что оно считает; число, сдвинутое строками копии, -
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
            if (open) {
                FlowRow(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    needs.forEach { need -> NeedChip(need, unmet?.let { need.kind !in it }, look) }
                }
            }
        }
        if (terms.isNotEmpty()) {
            FoldRow(ui("term.title"), terms.size.toString(), look.muted, open = termsOpen) { termsOpen = !termsOpen }
            if (termsOpen) TermsBlock(terms, item.index.campaign.combat, Modifier.padding(bottom = 6.dp))
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
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(enabled = open != null, role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = titleTint, style = MaterialTheme.typography.labelLarge, fontWeight = if (titleTint == Parchment) null else FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = tint, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, maxLines = 2)
        if (open != null) Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
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
        if (open) {
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
