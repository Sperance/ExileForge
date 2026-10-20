package com.sperance.exileforge.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.filter.Narrowing
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital

/** Включённое сужение под строкой поиска: подпись чипа [title] и его снятие [onRemove]. */
@Immutable class SearchChip(val title: String, val onRemove: () -> Unit)

/** Сужения ядра чипами: снятие применяет фильтр без сужения ([Narrowing.without]) через [apply]. */
fun <F> List<Narrowing<F>>.chips(apply: (F) -> Unit): List<SearchChip> = map { narrowing -> SearchChip(narrowing.title) { apply(narrowing.without) } }

/**
 * Фильтр строки поиска: включённые сужения [chips] (их число - на кнопке фильтра), [onOpen] - шторка фильтра, [onReset] - снять
 * все сужения сразу, без шторки; [enabled] - кнопка фильтра доступна.
 */
class SearchFilter(val chips: List<SearchChip>, val onOpen: () -> Unit, val onReset: () -> Unit, val enabled: Boolean = true)

/**
 * Единая строка поиска списков: плотное поле во всю ширину ([CompactSearchField]) с подсказкой по месту [hint] и крестиком очистки,
 * справа снаружи - кнопка фильтра с числом включённых сужений ([filter], null - фильтра нет) и необязательное действие места
 * [action] (порядок - [SortButton], «+»). Под полем - ряд чипов включённых сужений с крестиком и «Сбросить», только пока сужения
 * есть; ряд листается вбок. [onSearch] - поиск у сервера с клавиатуры, его же зовёт очистка поля; null - список сужается на месте.
 * [found] - сколько нашлось, мелко в поле.
 */
@Composable fun SearchBar(
    query: String,
    onQuery: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    found: Int? = null,
    filter: SearchFilter? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactSearchField(
                query,
                onQuery,
                hint,
                Modifier.weight(1f),
                onSearch = onSearch,
                found = found,
                onClear = {
                    onQuery("")
                    onSearch?.invoke()
                }.takeIf { query.isNotEmpty() },
            )
            filter?.let { FilterButton(it.chips.size, it.enabled, it.onOpen) }
            action?.invoke(this)
        }
        filter?.takeIf { it.chips.isNotEmpty() }?.let { NarrowingRow(it.chips, it.onReset) }
    }
}

/** Ряд включённых сужений: чип с крестиком снимает своё, «Сбросить» в конце - все. */
@Composable private fun NarrowingRow(chips: List<SearchChip>, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEach { chip ->
            ForgeChip(chip.title, selected = true, onClick = chip.onRemove, leading = { Icon(Icons.Outlined.Close, null) }, contentDescription = ui("filter.remove_chip", chip.title))
        }
        ForgeButton(onClick = onReset, kind = ButtonKind.Text, size = ButtonSize.Compact) { Text(ui("filter.reset"), maxLines = 1) }
    }
}

/** Кнопка фильтра списка (общая): значок отбора; при суженном списке - тональная, золотая и с числом сужений [count]. */
@Composable fun FilterButton(count: Int, enabled: Boolean = true, onClick: () -> Unit) {
    ForgeIconButton(
        Icons.Outlined.FilterList,
        ui("filter.title"),
        onClick = onClick,
        kind = if (count > 0) ButtonKind.Secondary else ButtonKind.Text,
        size = ButtonSize.Compact,
        state = ButtonState.of(enabled),
        accent = filterTint(count),
        badge = count,
        badgeColor = Vital,
    )
}

/** Пункт меню порядка: подпись [title], выбран ли он ([chosen]) и выбор [onPick]. */
@Immutable class SortOption(val title: String, val chosen: Boolean, val onPick: () -> Unit)

/**
 * Кнопка порядка строки поиска: «⇅» и [label] - нынешний порядок словом (null - только значок, подсвеченный, пока порядок не
 * обычный - [marked]); касание - меню порядков [options].
 */
@Composable fun SortButton(label: String?, options: List<SortOption>, marked: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box {
        if (label == null) {
            ForgeIconButton(Icons.Outlined.SwapVert, ui("filter.sort"), onClick = { open = true }, kind = ButtonKind.Secondary, size = ButtonSize.Compact, accent = GoldBright.takeIf { marked })
        } else {
            ForgeButton(onClick = { open = true }, kind = ButtonKind.Secondary, size = ButtonSize.Compact, icon = Icons.Outlined.SwapVert) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        ForgeMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title, color = if (option.chosen) GoldBright else Parchment) },
                    onClick = {
                        open = false
                        option.onPick()
                    },
                )
            }
        }
    }
}
