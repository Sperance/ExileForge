package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.filter.FilterFacet
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.filter.ItemType
import com.sperance.exileforge.core.filter.QualityFilter
import com.sperance.exileforge.core.filter.SlotGroup
import com.sperance.exileforge.core.filter.modTitle
import com.sperance.exileforge.core.filter.typeTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Фильтр одного списка на экране (предметов): [filter] - текущий, [update] меняет его и запоминает выбор
 * на устройстве для своего списка [shelf]; [search] меняет только поиск строки - он не запоминается, и устройство не пишется.
 */
@Stable
class FilterState<S, F> internal constructor(val shelf: S, initial: F, private val save: (F) -> Unit) {
    var filter by mutableStateOf(initial)
        private set

    fun update(next: F) {
        filter = next
        save(next)
    }

    fun search(next: F) {
        filter = next
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

/**
 * Строка поиска списка предметов ([SearchBar]): поиск по имени и строкам вещи с подсказкой места [hint], кнопка фильтра ([onFilter] -
 * шторка [ItemFilterSheet]) с включёнными сужениями чипами под полем ([ItemFilter.narrowings]) и, если список сортируется, кнопка
 * порядка с нынешним порядком словом.
 */
@Composable fun ItemSearchBar(state: ItemFilterState, hint: String, lang: Lang, onFilter: () -> Unit, modifier: Modifier = Modifier) {
    val filter = state.filter
    val shelf = state.shelf
    SearchBar(
        filter.query,
        { state.search(filter.copy(query = it.take(DefaultInputs.search))) },
        hint,
        modifier,
        filter = SearchFilter(filter.narrowings(shelf, lang).chips(state::update), onFilter, onReset = { state.update(filter.cleared(shelf)) }),
        action = if (FilterFacet.SORT in shelf.facets) {
            {
                SortButton(ui("filter.sort.${filter.sort.name}"), shelf.sorts.map { sort -> SortOption(ui("filter.sort.${sort.name}"), sort == filter.sort) { state.update(filter.copy(sort = sort)) } })
            }
        } else {
            null
        },
    )
}

/**
 * Шторка фильтра списка предметов («Аккордеон»): в шапке - вилка уровня предмета ([LevelRangeHeader], «Около моего» - от
 * уровня героя [heroLevel]), грани списка [ItemFilterState.shelf] - тип ([groups] - что есть в списке; у списка с рейкой - нет),
 * редкость плитками ([rarities]), качество, «по моду» (характеристики строк [mods] с поиском), и переключатели «сделано мной»,
 * «сделано ремеслом», «могу надеть», «лучше надетого», «скрыть надетое». Поиск и порядок - в строке поиска экрана.
 * Правки идут в черновик; «Показать N» ([count] - сколько пройдёт черновик) применяет его, «Сбросить» сразу
 * снимает все сужения ([ItemFilter.cleared]) и закрывает шторку.
 */
@Composable
fun ItemFilterSheet(state: ItemFilterState, groups: List<SlotGroup>, rarities: List<Rarity>, mods: List<String>, lang: Lang, heroLevel: Int, count: (ItemFilter) -> Int, onDismiss: () -> Unit) {
    val shelf = state.shelf
    var draft by remember { mutableStateOf(state.filter) }
    val any = ui("filter.any")
    fun has(facet: FilterFacet) = facet in shelf.facets
    val accordion = listOfNotNull(
        AccordionGroup("type", ui("filter.type"), draft.type?.let { typeTitle(it, lang) } ?: any) {
            val types = listOf<ItemType?>(null) + shelf.sections.map(ItemType::Section) + groups.map(ItemType::Group)
            ChoiceChips(types, { it == draft.type }, { it?.let { t -> typeTitle(t, lang) } ?: any }) { draft = draft.copy(type = it) }
        }.takeIf { has(FilterFacet.TYPE) && !shelf.rail && (groups.isNotEmpty() || shelf.sections.isNotEmpty()) },
        AccordionGroup("rarity", ui("filter.rarity"), draft.rarities.takeIf { it.isNotEmpty() }?.sortedByDescending { it.ordinal }?.joinToString(", ") { rarityTitle(it, lang) } ?: any) {
            RarityTiles(rarities, { it in draft.rarities }) { draft = draft.toggle(it) }
        }.takeIf { has(FilterFacet.RARITY) && rarities.isNotEmpty() },
        AccordionGroup("quality", ui("filter.quality"), ui("filter.quality.${draft.quality.name}")) {
            ChoiceChips(QualityFilter.entries, { it == draft.quality }, { ui("filter.quality.${it.name}") }) { draft = draft.copy(quality = it) }
        }.takeIf { has(FilterFacet.QUALITY) },
        AccordionGroup("mod", ui("filter.mod"), draft.mod?.let { modTitle(it, lang) } ?: any) {
            ModChoice(mods, draft.mod, lang, any) { draft = draft.copy(mod = it) }
        }.takeIf { has(FilterFacet.MOD) && (mods.isNotEmpty() || draft.mod != null) },
    )
    val switches = listOfNotNull(
        FilterSwitch(ui("filter.mine"), draft.mine) { draft = draft.copy(mine = it) }.takeIf { has(FilterFacet.MINE) },
        FilterSwitch(ui("filter.crafted"), draft.crafted) { draft = draft.copy(crafted = it) }.takeIf { has(FilterFacet.CRAFTED) },
        FilterSwitch(ui("filter.wearable"), draft.wearable) { draft = draft.copy(wearable = it) }.takeIf { has(FilterFacet.WEARABLE) },
        FilterSwitch(ui("filter.better"), draft.better) { draft = draft.copy(better = it) }.takeIf { has(FilterFacet.BETTER) },
        FilterSwitch(ui("filter.hide_worn"), draft.hideWorn) { draft = draft.copy(hideWorn = it) }.takeIf { has(FilterFacet.HIDE_WORN) },
    )
    val shown = remember(draft) { count(draft) }
    FilterAccordionSheet(
        groups = accordion,
        switches = switches,
        show = ui("filter.show", shown),
        resettable = draft != draft.cleared(shelf),
        onReset = {
            state.update(draft.cleared(shelf))
            onDismiss()
        },
        onShow = {
            state.update(draft)
            onDismiss()
        },
        onDismiss = onDismiss,
        header = if (has(FilterFacet.LEVEL)) {
            { LevelRangeHeader(draft.level, heroLevel) { draft = draft.copy(level = it) } }
        } else {
            null
        },
    )
}

/** Выбор характеристики «по моду»: плотный поиск по имени и фишки «Любое» и характеристик [mods], что под него подходят. */
@Composable private fun ModChoice(mods: List<String>, chosen: String?, lang: Lang, any: String, onPick: (String?) -> Unit) {
    var find by remember { mutableStateOf("") }
    val titles = remember(mods, lang) { mods.associateWith { modTitle(it, lang) } }
    val options = remember(titles, find) { listOf<String?>(null) + mods.filter { find.isBlank() || titles.getValue(it).contains(find.trim(), ignoreCase = true) } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SearchBar(find, { find = it.take(DefaultInputs.search) }, ui("filter.mod_find"))
        ChoiceChips(options, { it == chosen }, { it?.let { stat -> titles.getValue(stat) } ?: any }, onPick)
    }
}
