package com.sperance.exileforge.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.presentation.state.FilterFacet
import com.sperance.exileforge.presentation.state.ItemFilter
import com.sperance.exileforge.presentation.state.ItemShelf
import com.sperance.exileforge.presentation.state.QualityFilter
import com.sperance.exileforge.presentation.state.SlotGroup
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Фильтр одного списка предметов на экране (4.2.0): [filter] - текущий, [update] меняет его и запоминает выбор на устройстве
 * для своего списка [shelf] (поиск не запоминается).
 */
@Stable
class ItemFilterState internal constructor(val shelf: ItemShelf, initial: ItemFilter, private val save: (ItemFilter) -> Unit) {
    var filter by mutableStateOf(initial)
        private set

    fun update(next: ItemFilter) {
        filter = next
        save(next)
    }
}

/** Фильтр списка [shelf]: запомненный выбор этого списка, а без него - обычный списка. */
@Composable fun rememberItemFilter(shelf: ItemShelf): ItemFilterState {
    val prefs = koinInject<PreferencesRepository>()
    val scope = rememberCoroutineScope()
    return remember(shelf) { ItemFilterState(shelf, prefs.itemFilters.value[shelf]) { next -> scope.launch { prefs.saveItemFilter(shelf, next) } } }
}

/**
 * Значок фильтра списка (3.91.0, с 4.2.0 - общий): маленький круг, золотой и с числом того, что сужает список; его размер
 * обязателен, строка его не сжимает.
 */
@Composable fun ItemFilterButton(tweaks: Int, onClick: () -> Unit) {
    val tint = filterTint(tweaks)
    BadgedBox(badge = { if (tweaks > 0) Badge(containerColor = Vital, contentColor = Ink) { Text(tweaks.toString(), fontSize = 9.sp, maxLines = 1) } }) {
        Box(
            Modifier.requiredSize(30.dp).clip(CircleShape).border(1.dp, tint.copy(alpha = .6f), CircleShape).clickable(role = Role.Button, onClickLabel = ui("filter.title"), onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.FilterList, ui("filter.title"), tint = tint, modifier = Modifier.size(16.dp)) }
    }
}

/**
 * Строка над списком (4.2.0): порядок коротким словом и сколько видно, справа - значок фильтра; любое нажатие открывает
 * шторку ([ItemFilterSheet]).
 */
@Composable fun ItemFilterBar(state: ItemFilterState, shown: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val filter = state.filter
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            listOfNotNull(
                if (FilterFacet.SORT in state.shelf.facets) ui("filter.sort.${filter.sort.name}") + " ▾" else ui("filter.title"),
                filter.query.takeIf { it.isNotBlank() }?.let { "«$it»" },
            ).joinToString(" · "),
            color = GoldBright,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = ui("filter.title"), onClick = onOpen)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        )
        Text(shown.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
        ItemFilterButton(filter.tweaks(state.shelf), onOpen)
    }
}

/**
 * Шторка фильтра списка предметов (4.2.0, «Аккордеон»): грани списка [ItemFilterState.shelf] - тип ([groups] - что есть в
 * списке), редкость плитками ([rarities]), качество, порядок, и переключатели «сделано мной», «могу надеть», «скрыть надетое».
 * Правки идут в черновик; «Показать N» ([count] - сколько пройдёт черновик) применяет его, «Сбросить» - к обычному списка.
 */
@Composable
fun ItemFilterSheet(state: ItemFilterState, groups: List<SlotGroup>, rarities: List<Rarity>, lang: Lang, count: (ItemFilter) -> Int, onDismiss: () -> Unit) {
    val shelf = state.shelf
    var draft by remember { mutableStateOf(state.filter) }
    val any = ui("filter.any")
    fun has(facet: FilterFacet) = facet in shelf.facets
    val accordion = listOfNotNull(
        AccordionGroup("type", ui("filter.type"), draft.group?.title(lang) ?: any) {
            ChoiceChips(listOf<SlotGroup?>(null) + groups, { it == draft.group }, { it?.title(lang) ?: any }) { draft = draft.copy(group = it) }
        }.takeIf { has(FilterFacet.TYPE) && groups.isNotEmpty() },
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
        onReset = { draft = shelf.defaults },
        onShow = {
            state.update(draft)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}
