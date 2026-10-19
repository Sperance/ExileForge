package com.sperance.exileforge.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.filter.FilterFacet
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.filter.ItemType
import com.sperance.exileforge.core.filter.QualityFilter
import com.sperance.exileforge.core.filter.SlotGroup
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Фильтр одного списка на экране (предметов): [filter] - текущий, [update] меняет его и запоминает выбор
 * на устройстве для своего списка [shelf] (поиск не запоминается).
 */
@Stable
class FilterState<S, F> internal constructor(val shelf: S, initial: F, private val save: (F) -> Unit) {
    var filter by mutableStateOf(initial)
        private set

    fun update(next: F) {
        filter = next
        save(next)
    }
}

/** Фильтр списка предметов. */
typealias ItemFilterState = FilterState<ItemShelf, ItemFilter>

/** Фильтр списка [shelf]: запомненный выбор этого списка, а без него - обычный списка. */
@Composable fun rememberItemFilter(shelf: ItemShelf): ItemFilterState {
    val prefs = koinInject<PreferencesRepository>()
    val scope = rememberCoroutineScope()
    return remember(shelf) { FilterState(shelf, prefs.itemFilters.value[shelf]) { next: ItemFilter -> scope.launch { prefs.saveItemFilter(shelf, next) } } }
}

/** Значок фильтра списка (общий): компактная иконка-кнопка, при суженном списке - тональная, светлая и со счётчиком сужений. */
@Composable fun ItemFilterButton(tweaks: Int, onClick: () -> Unit) {
    ForgeIconButton(
        Icons.Outlined.FilterList,
        ui("filter.title"),
        onClick = onClick,
        style = if (tweaks > 0) IconButtonStyle.Tonal else IconButtonStyle.Ghost,
        compact = true,
        tint = filterTint(tweaks),
        badge = tweaks,
    )
}

/**
 * Строка над списком: порядок коротким словом и сколько видно, справа - значок фильтра; любое нажатие открывает
 * шторку ([ItemFilterSheet]).
 */
@Composable fun ItemFilterBar(state: ItemFilterState, shown: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val filter = state.filter
    FilterBar(
        listOfNotNull(
            if (FilterFacet.SORT in state.shelf.facets) ui("filter.sort.${filter.sort.name}") + " ▾" else ui("filter.title"),
            filter.query.takeIf { it.isNotBlank() }?.let { "«$it»" },
        ).joinToString(" · "),
        shown,
        filter.tweaks(state.shelf),
        onOpen,
        modifier,
        onClear = { state.update(state.shelf.defaults) }.takeIf { filter != state.shelf.defaults },
    )
}

/**
 * Строка над любым списком с фильтром: [summary] - порядок и поиск словами, [shown] - сколько
 * видно, справа значок фильтра с числом [tweaks]; любое нажатие - [onOpen]. Фильтр сужает список - крестик [onClear] сбрасывает его
 * сразу, без шторки.
 */
@Composable fun FilterBar(summary: String, shown: Int, tweaks: Int, onOpen: () -> Unit, modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            summary,
            color = GoldBright,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = ui("filter.title"), onClick = onOpen)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        )
        Text(shown.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
        onClear?.let { clear ->
            ForgeIconButton(Icons.Outlined.Close, ui("filter.reset"), onClick = clear, compact = true)
        }
        ItemFilterButton(tweaks, onOpen)
    }
}

/**
 * Шторка фильтра списка предметов («Аккордеон»): грани списка [ItemFilterState.shelf] - тип ([groups] - что есть в
 * списке), редкость плитками ([rarities]), качество, порядок, и переключатели «сделано мной», «сделано ремеслом», «могу надеть»,
 * «скрыть надетое».
 * Правки идут в черновик; «Показать N» ([count] - сколько пройдёт черновик) применяет его, «Сбросить» сразу
 * применяет обычный списка и закрывает шторку.
 */
@Composable
fun ItemFilterSheet(state: ItemFilterState, groups: List<SlotGroup>, rarities: List<Rarity>, lang: Lang, count: (ItemFilter) -> Int, onDismiss: () -> Unit) {
    val shelf = state.shelf
    var draft by remember { mutableStateOf(state.filter) }
    val any = ui("filter.any")
    fun has(facet: FilterFacet) = facet in shelf.facets
    val accordion = listOfNotNull(
        AccordionGroup("type", ui("filter.type"), draft.type?.let { typeTitle(it, lang) } ?: any) {
            val types = listOf<ItemType?>(null) + shelf.sections.map(ItemType::Section) + groups.map(ItemType::Group)
            ChoiceChips(types, { it == draft.type }, { it?.let { t -> typeTitle(t, lang) } ?: any }) { draft = draft.copy(type = it) }
        }.takeIf { has(FilterFacet.TYPE) && (groups.isNotEmpty() || shelf.sections.isNotEmpty()) },
        AccordionGroup("rarity", ui("filter.rarity"), draft.rarities.takeIf { it.isNotEmpty() }?.sortedByDescending { it.ordinal }?.joinToString(", ") { rarityTitle(it, lang) } ?: any) {
            RarityTiles(rarities, { it in draft.rarities }) { draft = draft.toggle(it) }
        }.takeIf { has(FilterFacet.RARITY) && rarities.isNotEmpty() },
        AccordionGroup("quality", ui("filter.quality"), ui("filter.quality.${draft.quality.name}")) {
            ChoiceChips(QualityFilter.entries, { it == draft.quality }, { ui("filter.quality.${it.name}") }) { draft = draft.copy(quality = it) }
        }.takeIf { has(FilterFacet.QUALITY) },
        AccordionGroup("sort", ui("filter.sort"), ui("filter.sort.${draft.sort.name}")) {
            ChoiceChips(shelf.sorts, { it == draft.sort }, { ui("filter.sort.${it.name}") }) { draft = draft.copy(sort = it) }
        }.takeIf { has(FilterFacet.SORT) },
    )
    val switches = listOfNotNull(
        FilterSwitch(ui("filter.mine"), draft.mine) { draft = draft.copy(mine = it) }.takeIf { has(FilterFacet.MINE) },
        FilterSwitch(ui("filter.crafted"), draft.crafted) { draft = draft.copy(crafted = it) }.takeIf { has(FilterFacet.CRAFTED) },
        FilterSwitch(ui("filter.wearable"), draft.wearable) { draft = draft.copy(wearable = it) }.takeIf { has(FilterFacet.WEARABLE) },
        FilterSwitch(ui("filter.hide_worn"), draft.hideWorn) { draft = draft.copy(hideWorn = it) }.takeIf { has(FilterFacet.HIDE_WORN) },
    )
    val shown = remember(draft) { count(draft) }
    FilterAccordionSheet(
        query = draft.query,
        onQuery = { draft = draft.copy(query = it.take(DefaultInputs.search)) },
        queryHint = ui("hero.search_hint"),
        groups = accordion,
        switches = switches,
        show = ui("filter.show", shown),
        resettable = draft != shelf.defaults,
        onReset = {
            state.update(shelf.defaults)
            onDismiss()
        },
        onShow = {
            state.update(draft)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

/** Имя варианта «Типа»: раздел - своим словом, группа мест - как в рейке. */
fun typeTitle(type: ItemType, lang: Lang): String = when (type) {
    is ItemType.Group -> type.group.title(lang)
    is ItemType.Section -> ui("filter.section.${type.section.name}")
}
