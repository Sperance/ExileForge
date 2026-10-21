package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.PickAccent
import com.sperance.exileforge.core.display.PickGlyph
import com.sperance.exileforge.core.display.PickList
import com.sperance.exileforge.core.display.PickOption
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CoreAttribute
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.Boon
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Crafted
import com.sperance.exileforge.ui.theme.Elder
import com.sperance.exileforge.ui.theme.Ember
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.ManaBlue
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Seam
import com.sperance.exileforge.ui.theme.ShieldCyan
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.rarityColor
import kotlin.math.roundToInt

/**
 * Общий выбор из списка. Строка - подпись [label] слева и справа кнопка-чип: цветная полоса и знак выбранного варианта, его имя и
 * короткое подназвание ([PickOption.chipLine]), шеврон; ничего не выбрано - «Выбрать». Касание открывает нижнюю шторку ([ForgePickSheet]):
 * шапка - [label], пояснение [hint] и, с [PickList.SEARCH_FROM] вариантов, их число и плотный поиск, закреплённые над списком; список -
 * карточки-строки [options] по группам с заголовками. Выбор варианта закрывает шторку и отдаёт его ключ в [onPick]; [value] - ключ
 * выбранного сейчас; закрытый вариант ([PickOption.locked]) не выбирается. [onPeek] - долгое нажатие на строку (карточка варианта),
 * [hold] - что оно открывает: шторка говорит это подсказкой «Осмотра» ([HoldHint]) в шапке. [empty] - что сказать, когда вариантов нет вовсе. Ключ - типа вызывающего [K]: «любое» - вариант с ключом null; вызывающий строку не
 * разбирает.
 */
@Composable fun <K> ForgePicker(
    label: String,
    value: K?,
    options: List<PickOption<K>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
    empty: PickEmpty? = null,
    hold: HoldSubject? = null,
    onPeek: ((K) -> Unit)? = null,
    onPick: (K?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = remember(options, value) { options.firstOrNull { it.key == value } }
    PickChip(label, current, current?.title ?: ui("common.choose"), current?.chipLine, modifier, enabled) { open = true }
    if (open) {
        ForgePickSheet(label, value, options, hint, empty, hold, onDismiss = { open = false }, onPeek = onPeek) { key ->
            open = false
            onPick(key)
        }
    }
}

/**
 * Выбор нескольких из списка - тот же облик, что у [ForgePicker]: на чипе первый выбранный и «и ещё N · 2 из 3», ничего - «Нет». В
 * шторке строки с флажками: касание отмечает и снимает, набран предел [max] - неотмеченные гаснут; «Готово · [counted]» в подвале
 * отдаёт отмеченное в [onPick] (в порядке отметки), закрытие шторки мимо подвала выбор не меняет. [values] - выбранные сейчас; вариант
 * «любое» (ключ null) выбору нескольких не нужен - его нет. [onPeek] и [hold] - как у [ForgePicker].
 */
@Composable fun <K : Any> ForgeMultiPicker(
    label: String,
    values: List<K>,
    options: List<PickOption<K>>,
    max: Int,
    counted: (Int) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
    hold: HoldSubject? = null,
    onPeek: ((K) -> Unit)? = null,
    onPick: (List<K>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val keyed = remember(options) { options.filter { it.key != null } }
    val chosen = remember(keyed, values) { values.mapNotNull { key -> keyed.firstOrNull { it.key == key } } }
    val first = chosen.firstOrNull()
    val line = when {
        chosen.size > 1 -> ui("picker.more_of", chosen.size - 1, chosen.size, max)
        first != null -> ui("picker.count_of", chosen.size, max)
        else -> null
    }
    PickChip(label, first, first?.title ?: ui("picker.none"), line, modifier, enabled) { open = true }
    if (open) {
        MultiPickSheet(label, hint, hold, remember(keyed) { PickList(keyed) }, values, max, counted, onDismiss = { open = false }, onPeek = onPeek) { picked ->
            open = false
            onPick(picked)
        }
    }
}

/** Что сказать в шторке без вариантов вовсе: [title] и пояснение [hint] («Подходящих инструментов нет» и где их взять). */
data class PickEmpty(val title: String, val hint: String? = null)

/** Строка выбора: подпись [label] слева, справа кнопка-чип - полоса и знак [current], имя [title], строка [line] под ним и шеврон. */
@Composable private fun PickChip(label: String, current: PickOption<*>?, title: String, line: String?, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Quiet, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        ForgeButton(
            onClick = onClick,
            modifier = Modifier.widthIn(max = ChipMaxWidth).semantics { contentDescription = "$label: $title" },
            kind = ButtonKind.Secondary,
            size = ButtonSize.Wide,
            state = ButtonState.of(enabled),
            contentPadding = PaddingValues(start = 8.dp, end = 6.dp),
        ) {
            Box(Modifier.size(4.dp, 28.dp).background(current?.accent.paint(), RoundedCornerShape(2.dp)))
            current?.glyph?.let { PickMark(it, current.accent.paint(Gold), 22.dp, Modifier.padding(start = 8.dp)) }
            Column(Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                line?.let { Text(it, color = LocalContentColor.current.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Icon(Icons.Outlined.ExpandMore, null, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Шторка выбора одного варианта из [options] - и за чипом [ForgePicker], и сама по себе (смена инструмента профессии): шапка [title] с
 * пояснением [hint] и поиском закреплена, ниже - группы карточек-строк; при открытии список стоит на выбранном [value]. Выбор - [onPick],
 * долгое нажатие на строку - [onPeek], что оно открывает - [hold]; вариантов нет вовсе - [empty].
 */
@Composable fun <K> ForgePickSheet(
    title: String,
    value: K?,
    options: List<PickOption<K>>,
    hint: String? = null,
    empty: PickEmpty? = null,
    hold: HoldSubject? = null,
    onDismiss: () -> Unit,
    onPeek: ((K) -> Unit)? = null,
    onPick: (K?) -> Unit,
) {
    val list = remember(options) { PickList(options) }
    val focus = remember(list) { list.row(value) }
    PickSheetFrame(title, list.options.size.toString().takeIf { list.searchable }, false, hint, hold.takeIf { onPeek != null }, list, focus, empty, onDismiss) { option ->
        PickRow(option, RowMark.One(option.key == value), onPick = { onPick(option.key) }, onPeek = peekOf(option, onPeek))
    }
}

/** Шторка выбора нескольких: черновик отметок живёт, пока она открыта; «Готово» отдаёт его в [onDone]. */
@Composable private fun <K : Any> MultiPickSheet(
    title: String,
    hint: String?,
    hold: HoldSubject?,
    list: PickList<K>,
    values: List<K>,
    max: Int,
    counted: (Int) -> String,
    onDismiss: () -> Unit,
    onPeek: ((K) -> Unit)?,
    onDone: (List<K>) -> Unit,
) {
    var draft by remember { mutableStateOf(values) }
    val room = draft.size < max
    PickSheetFrame(
        title,
        ui("picker.count_of", draft.size, max),
        !room,
        hint,
        hold.takeIf { onPeek != null },
        list,
        0,
        null,
        onDismiss,
        footer = {
            ForgeButton(onClick = { onDone(draft) }, modifier = Modifier.weight(1f), size = ButtonSize.Wide) {
                Text(if (draft.isEmpty()) ui("picker.done") else ui("picker.done_count", counted(draft.size)))
            }
        },
    ) { option ->
        val key = option.key ?: return@PickSheetFrame
        val checked = key in draft
        PickRow(option, RowMark.Many(checked, room), onPick = { draft = if (checked) draft - key else draft + key }, onPeek = peekOf(option, onPeek))
    }
}

/** Долгое нажатие на строку [option]: [onPeek] с её ключом; у варианта «любое» и без [onPeek] - ничего. */
private fun <K> peekOf(option: PickOption<K>, onPeek: ((K) -> Unit)?): (() -> Unit)? {
    val key = option.key ?: return null
    return onPeek?.let { { it(key) } }
}

/**
 * Рама шторки выбора: шапка - [title], справа [count] (золотом, если [full]), пояснение [hint], подсказка «Осмотра» [hold] и с [PickList.SEARCH_FROM] вариантов общая
 * строка поиска [SearchBar]; ниже - группы [list] с закреплёнными заголовками, строки рисует [row], список открывается на строке [focus].
 * Пусто - [empty] (вариантов нет вовсе) или «Ничего не найдено»; [footer] - закреплённый подвал.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <K> PickSheetFrame(
    title: String,
    count: String?,
    full: Boolean,
    hint: String?,
    hold: HoldSubject?,
    list: PickList<K>,
    focus: Int,
    empty: PickEmpty?,
    onDismiss: () -> Unit,
    footer: (@Composable RowScope.() -> Unit)? = null,
    row: @Composable (PickOption<K>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sections = remember(list, query) { list.sections(query) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = focus)
    ForgeSheet(
        onDismissRequest = onDismiss,
        header = {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title.uppercase(), color = Gold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    count?.let { Text(it, color = if (full) Gold else Muted, style = MaterialTheme.typography.labelMedium, fontWeight = if (full) FontWeight.Bold else null) }
                }
                hint?.let { MutedText(it) }
                hold?.let { HoldHint(it) }
                if (list.searchable) SearchBar(query, { query = it }, ui("common.find_option"))
            }
        },
        footer = footer,
    ) {
        when {
            list.options.isEmpty() && empty != null -> EmptyList(empty)

            sections.isEmpty() -> NothingFound(searched = query.isNotEmpty()) { query = "" }

            else -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f, fill = false),
                state = state,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sections.forEach { section ->
                    section.title?.let { group -> stickyHeader(key = "group:$group") { GroupHead(group, section.options.size) } }
                    items(section.options, key = { option -> option.key?.let { "key:$it" } ?: ANY_ROW }) { option -> row(option) }
                }
            }
        }
    }
}

/** Знак строки шторки справа: [One] - выбор одного (выбранный сейчас - «Сейчас»), [Many] - флажок; [room] - ещё можно отметить. */
private sealed interface RowMark {
    /** Строка в золотой рамке: выбрана или отмечена. */
    val lit: Boolean

    data class One(val current: Boolean) : RowMark {
        override val lit: Boolean get() = current
    }

    data class Many(val checked: Boolean, val room: Boolean) : RowMark {
        override val lit: Boolean get() = checked
    }
}

/**
 * Карточка-строка варианта: полоса цвета [PickOption.accent] слева, знак, имя и подназвание (переносятся целиком), данные ровной
 * колонкой справа (нехватка - красным), у выбора нескольких - флажок. Выбранный ([RowMark.lit]) - в золотой рамке на подсвеченном фоне,
 * у выбора одного - с пометкой «Сейчас». Закрытый ([PickOption.locked]) и неотмеченный при набранном пределе - тусклые и не выбираются;
 * у закрытого справа замок и почему. Отмеченный снимается всегда, даже закрытый. Долгое нажатие - [onPeek], и у закрытого тоже.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickRow(option: PickOption<*>, mark: RowMark, onPick: () -> Unit, onPeek: (() -> Unit)?) {
    val shape = RoundedCornerShape(12.dp)
    val accent = option.accent.paint()
    val lit = mark.lit
    val available = when (mark) {
        is RowMark.One -> option.locked == null
        is RowMark.Many -> mark.checked || (option.locked == null && mark.room)
    }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (lit) ButtonInk.ChipOnFill else Panel)
            .drawBehind { drawRect(accent, size = Size(BarWidth.toPx(), size.height)) }
            .border(if (lit) 1.5.dp else 1.dp, if (lit) Gold else ButtonInk.OffRim, shape)
            .semantics {
                when (mark) {
                    is RowMark.One -> selected = mark.current
                    is RowMark.Many -> toggleableState = ToggleableState(mark.checked)
                }
                option.locked?.let { stateDescription = it }
            }
            .combinedClickable(
                enabled = available || onPeek != null,
                role = if (mark is RowMark.Many) Role.Checkbox else Role.RadioButton,
                onLongClick = onPeek,
                onClick = { if (available) onPick() },
            )
            .alpha(if (available || lit) 1f else DimAlpha)
            .padding(start = BarWidth),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        option.glyph?.let { glyph ->
            Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) { PickMark(glyph, option.accent.paint(Gold), 26.dp) }
        } ?: Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    option.title,
                    color = if (lit) GoldBright else Parchment,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (mark is RowMark.One && mark.current) TagChip(ui("picker.now"), Gold)
            }
            option.subtitle?.let { Text(it, color = if (lit) Quiet else Muted, style = MaterialTheme.typography.bodySmall) }
        }
        val locked = option.locked
        val data = option.data
        when {
            locked != null -> Row(Modifier.padding(start = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(14.dp))
                Text(locked, color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }

            data != null -> Column(Modifier.widthIn(min = 64.dp).padding(start = 8.dp, end = 12.dp), horizontalAlignment = Alignment.End) {
                Text(data.value, color = if (data.lacking) LifeRed else Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                data.caption?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            }

            else -> Spacer(Modifier.width(12.dp))
        }
        if (mark is RowMark.Many) CheckMark(mark.checked, Modifier.padding(end = 12.dp))
    }
}

/** Флажок строки выбора нескольких: отмеченный - золотая заливка с галочкой, нет - пустой квадрат в окантовке. */
@Composable private fun CheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier.size(22.dp).clip(shape).background(if (checked) Gold else Color.Transparent).border(1.5.dp, if (checked) Gold else Muted, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Outlined.Check, null, tint = Ink, modifier = Modifier.size(16.dp))
    }
}

/** Вариантов нет вовсе: [PickEmpty.title] и пояснение по центру. */
@Composable private fun EmptyList(empty: PickEmpty) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(empty.title, color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        empty.hint?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
    }
}

/** Заголовок группы шторки: имя заглавными и сколько в ней вариантов; закреплён, пока группа на экране. */
@Composable private fun GroupHead(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().background(PanelRaised).padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), color = Quiet, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(count.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

/** Пустой результат: «Ничего не найдено» и, если искали ([searched]), «Очистить поиск» - [onClear]. */
@Composable private fun NothingFound(searched: Boolean, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(ui("common.no_options"), color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (searched) ForgeButton(onClick = onClear, kind = ButtonKind.Secondary, icon = Icons.Outlined.Close) { Text(ui("picker.clear_search")) }
    }
}

/** Знак варианта размера [size] цветом [tint]: значок смысла или эмблема вида вещи; для чтения с экрана немой - вариант назван словами. */
@Composable private fun PickMark(glyph: PickGlyph, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        when (glyph) {
            is PickGlyph.Sign -> Icon(glyph.glyph.vector, null, tint = tint, modifier = Modifier.size(size))
            is PickGlyph.Emblem -> ItemEmblem(glyph.kind, tint, Modifier.size(size))
        }
    }
}

/**
 * Краска полосы: редкость - её цвет, ступень - место на лестнице [Ladder], группа - цвет по кругу [Bands], атрибут - его цвет (сила -
 * кровь, ловкость - топаз, интеллект - мана); без смысла - [fallback].
 */
private fun PickAccent?.paint(fallback: Color = Seam): Color = when (this) {
    null -> fallback
    is PickAccent.OfRarity -> rarityColor(rarity.name)
    is PickAccent.Step -> Ladder[if (steps <= 1) 0 else (step.toFloat() / (steps - 1) * Ladder.lastIndex).roundToInt().coerceIn(0, Ladder.lastIndex)]
    is PickAccent.Band -> Bands[band.mod(Bands.size)]
    is PickAccent.OfAttribute -> when (attribute) {
        CoreAttribute.STRENGTH -> LifeRed
        CoreAttribute.DEXTERITY -> Caution
        CoreAttribute.INTELLIGENCE -> ManaBlue
    }
}

/** Лестница ступеней: от тихого серого к цвету уникального. */
private val Ladder = listOf(Quiet, Rune, Gold, Boon, rarityColor("UNIQUE"))

/** Цвета групп по кругу. */
private val Bands = listOf(ShieldCyan, Crafted, Brass, Vital, Elder, Ember, Rune)

/** Ключ строки варианта «любое» (ключ null) в списке шторки. */
private const val ANY_ROW = "any"

/** Непрозрачность строки, что сейчас не выбрать. */
private const val DimAlpha = .5f

/** Ширина цветной полосы карточки-строки. */
private val BarWidth = 5.dp

/** Наибольшая ширина чипа: подписи слева остаётся место и на узком экране. */
private val ChipMaxWidth = 220.dp
