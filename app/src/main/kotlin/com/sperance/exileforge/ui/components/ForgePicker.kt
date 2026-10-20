package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.Boon
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Crafted
import com.sperance.exileforge.ui.theme.Elder
import com.sperance.exileforge.ui.theme.Ember
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
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
 * короткое подназвание ([PickOption.chipLine]), шеврон; ничего не выбрано - «Выбрать». Касание открывает нижнюю шторку: шапка - [label],
 * пояснение [hint] и, с [PickList.SEARCH_FROM] вариантов, их число и плотный поиск, закреплённые над списком; список - карточки-строки
 * [options] по группам с заголовками. Выбор варианта закрывает шторку и отдаёт его ключ в [onPick]; [value] - ключ выбранного сейчас.
 * Ключ - типа вызывающего [K]: «любое» - вариант с ключом null; вызывающий строку не разбирает.
 */
@Composable fun <K> ForgePicker(
    label: String,
    value: K?,
    options: List<PickOption<K>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
    onPick: (K?) -> Unit,
) {
    val list = remember(options) { PickList(options) }
    var open by remember { mutableStateOf(false) }
    val current = list.option(value)
    val title = current?.title ?: ui("common.choose")
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Quiet, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        ForgeButton(
            onClick = { open = true },
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
                current?.chipLine?.let {
                    Text(it, color = LocalContentColor.current.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Outlined.ExpandMore, null, modifier = Modifier.size(18.dp))
        }
    }
    if (open) {
        PickSheet(label, hint, list, value, onDismiss = { open = false }) { key ->
            open = false
            onPick(key)
        }
    }
}

/**
 * Шторка выбора: шапка с поиском (общая строка [SearchBar]) закреплена, ниже - группы карточек-строк; при открытии список стоит на
 * выбранном [value].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun <K> PickSheet(title: String, hint: String?, list: PickList<K>, value: K?, onDismiss: () -> Unit, onPick: (K?) -> Unit) {
    var query by remember { mutableStateOf("") }
    val sections = remember(list, query) { list.sections(query) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = remember(list, value) { list.row(value) })
    ForgeSheet(
        onDismissRequest = onDismiss,
        header = {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title.uppercase(), color = Gold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (list.searchable) Text(list.options.size.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                hint?.let { MutedText(it) }
                if (list.searchable) SearchBar(query, { query = it }, ui("common.find_option"))
            }
        },
    ) {
        if (sections.isEmpty()) {
            NothingFound(searched = query.isNotEmpty()) { query = "" }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f, fill = false),
                state = state,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sections.forEach { section ->
                    section.title?.let { group -> stickyHeader(key = "group:$group") { GroupHead(group, section.options.size) } }
                    items(section.options, key = { option -> option.key?.let { "key:$it" } ?: ANY_ROW }) { option -> PickRow(option, option.key == value) { onPick(option.key) } }
                }
            }
        }
    }
}

/**
 * Карточка-строка варианта: полоса цвета [PickOption.accent] слева, знак, имя и подназвание (переносятся целиком), данные ровной
 * колонкой справа. Выбранный сейчас ([current]) - в золотой рамке на подсвеченном фоне и с пометкой «Сейчас».
 */
@Composable private fun PickRow(option: PickOption<*>, current: Boolean, onPick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val accent = option.accent.paint()
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (current) ButtonInk.ChipOnFill else Panel)
            .drawBehind { drawRect(accent, size = Size(BarWidth.toPx(), size.height)) }
            .border(if (current) 1.5.dp else 1.dp, if (current) Gold else ButtonInk.OffRim, shape)
            .selectable(current, role = Role.RadioButton, onClick = onPick)
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
                    color = if (current) GoldBright else Parchment,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (current) TagChip(ui("picker.now"), Gold)
            }
            option.subtitle?.let { Text(it, color = if (current) Quiet else Muted, style = MaterialTheme.typography.bodySmall) }
        }
        option.data?.let { data ->
            Column(Modifier.widthIn(min = 64.dp).padding(start = 8.dp, end = 12.dp), horizontalAlignment = Alignment.End) {
                Text(data.value, color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                data.caption?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            }
        } ?: Spacer(Modifier.width(12.dp))
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

/** Краска полосы: редкость - её цвет, ступень - место на лестнице [Ladder], группа - цвет по кругу [Bands]; без смысла - [fallback]. */
private fun PickAccent?.paint(fallback: Color = Seam): Color = when (this) {
    null -> fallback
    is PickAccent.OfRarity -> rarityColor(rarity.name)
    is PickAccent.Step -> Ladder[if (steps <= 1) 0 else (step.toFloat() / (steps - 1) * Ladder.lastIndex).roundToInt().coerceIn(0, Ladder.lastIndex)]
    is PickAccent.Band -> Bands[band.mod(Bands.size)]
}

/** Лестница ступеней: от тихого серого к цвету уникального. */
private val Ladder = listOf(Quiet, Rune, Gold, Boon, rarityColor("UNIQUE"))

/** Цвета групп по кругу. */
private val Bands = listOf(ShieldCyan, Crafted, Brass, Vital, Elder, Ember, Rune)

/** Ключ строки варианта «любое» (ключ null) в списке шторки. */
private const val ANY_ROW = "any"

/** Ширина цветной полосы карточки-строки. */
private val BarWidth = 5.dp

/** Наибольшая ширина чипа: подписи слева остаётся место и на узком экране. */
private val ChipMaxWidth = 220.dp
