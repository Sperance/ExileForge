package com.sperance.exileforge.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.PickOption
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.filter.FilterFacet
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemQuery
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.filter.ItemShelfView
import com.sperance.exileforge.core.filter.ItemType
import com.sperance.exileforge.core.filter.QualityFilter
import com.sperance.exileforge.core.filter.itemJudge
import com.sperance.exileforge.core.filter.modTitle
import com.sperance.exileforge.core.filter.typeTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Фильтр списка предметов [shelf] на экране: [filter] - текущий, [update] меняет его и запоминает выбор на устройстве; [query] - поиск
 * строки, [search] меняет только его - он не запоминается, и устройство не пишется.
 */
@Stable
class ItemFilterState internal constructor(val shelf: ItemShelf, initial: ItemFilter, private val save: (ItemFilter) -> Unit) {
    var filter by mutableStateOf(initial)
        private set

    var query by mutableStateOf(ItemQuery.NONE)
        private set

    fun update(next: ItemFilter) {
        filter = next
        save(next)
    }

    fun search(text: String) {
        query = ItemQuery(text)
    }
}

/** Фильтр списка [shelf]: запомненный выбор этого списка, а без него - обычный списка. */
@Composable fun rememberItemFilter(shelf: ItemShelf): ItemFilterState {
    val prefs = koinInject<PreferencesRepository>()
    val scope = rememberCoroutineScope()
    return remember(shelf) { ItemFilterState(shelf, prefs.itemFilters.value[shelf]) { next -> scope.launch { prefs.saveItemFilter(shelf, next) } } }
}

/**
 * Витрина списка [pieces] под фильтром [state] у героя на экране ([ItemShelfView]): собирается вне главного потока заново, только
 * когда сдвинулись вещи, фильтр, поиск, язык или то, о чём спрашивает фильтр (контент, вещи и лист героя); [price] - цена вещи в этом
 * списке. Null - пока первая витрина считается; потом до готовой новой видна прежняя.
 */
@Composable fun rememberItemShelf(game: GameUi, pieces: List<ItemView>, state: ItemFilterState, price: (ItemView) -> Long?): ItemShelfView? {
    val hero = game.hero
    val view by produceState<ItemShelfView?>(null, pieces, state.filter, state.query, game.lang, game.index, hero?.items, hero?.sheet, hero?.combat, hero?.info?.name) {
        val judge = game.itemJudge
        val shelf = state.shelf
        val filter = state.filter
        val query = state.query
        value = withContext(Dispatchers.Default) { ItemShelfView(pieces, shelf, filter, query, game.lang, judge, price) }
    }
    return view
}

/**
 * Строка поиска списка предметов ([SearchBar]): поиск по имени и строкам вещи с подсказкой места [hint], кнопка фильтра ([onFilter] -
 * шторка [ItemFilterSheet]) с включёнными сужениями чипами под полем ([ItemFilter.narrowings]) и, если список сортируется, кнопка
 * порядка с нынешним порядком словом.
 */
@Composable fun ItemSearchBar(state: ItemFilterState, hint: String, lang: Lang, onFilter: () -> Unit, modifier: Modifier = Modifier) {
    val filter = state.filter
    val shelf = state.shelf
    val narrowings = remember(filter, shelf, lang) { filter.narrowings(shelf, lang) }
    SearchBar(
        state.query.text,
        state::search,
        hint,
        modifier,
        filter = SearchFilter(rememberChips(narrowings, state::update), onFilter, onReset = { state.update(filter.cleared(shelf)) }),
        action = if (FilterFacet.SORT in shelf.facets) {
            {
                SortButton(SortLabel.Word(filter.sort.title(lang)), shelf.sorts.map { sort -> SortOption(sort.title(lang), sort == filter.sort) { state.update(filter.copy(sort = sort)) } })
            }
        } else {
            null
        },
    )
}

/**
 * Шторка фильтра списка предметов («Аккордеон») над витриной [view]: в шапке - вилка уровня предмета ([LevelRangeHeader], «Около
 * моего» - от уровня героя), грани списка [ItemFilterState.shelf] - тип ([ItemShelfView.groups] - что есть в списке; у списка с
 * рейкой - нет), редкость фишками ([ItemShelfView.rarities]), качество, «по моду» (характеристики строк с поиском,
 * [ItemShelfView.mods]), и переключатели «сделано мной», «сделано ремеслом», «могу надеть», «лучше надетого», «скрыть надетое».
 * Поиск и порядок - в строке поиска экрана. Правки идут в черновик; «Показать N» (сколько пройдёт черновик, [ItemShelfView.count] -
 * вне главного потока) применяет его, «Сбросить» сразу снимает все сужения ([ItemFilter.cleared]) и закрывает шторку.
 */
@Composable
fun ItemFilterSheet(state: ItemFilterState, view: ItemShelfView, onDismiss: () -> Unit) {
    val shelf = state.shelf
    val lang = view.lang
    var draft by remember { mutableStateOf(state.filter) }
    val any = ui("filter.any")
    fun has(facet: FilterFacet) = facet in shelf.facets
    val groups = view.groups.map { it.first }
    val accordion = listOfNotNull(
        AccordionGroup("type", ui("filter.type"), draft.type?.let { typeTitle(it, lang) } ?: any) {
            val types = listOf<ItemType?>(null) + shelf.sections.map(ItemType::Section) + groups.map(ItemType::Group)
            ChoiceChips(types, { it == draft.type }, { it?.let { t -> typeTitle(t, lang) } ?: any }) { draft = draft.copy(type = it) }
        }.takeIf { has(FilterFacet.TYPE) && !shelf.rail && (groups.isNotEmpty() || shelf.sections.isNotEmpty()) },
        AccordionGroup("rarity", ui("filter.rarity"), draft.rarities.takeIf { it.isNotEmpty() }?.sortedByDescending { it.ordinal }?.joinToString(", ") { rarityTitle(it, lang) } ?: any) {
            RarityChoice(view.rarities, { it in draft.rarities }, { picked -> picked?.let { draft = draft.toggle(it) } }, mode = ChipMode.Filter, size = ChipSize.Dense)
        }.takeIf { has(FilterFacet.RARITY) && view.rarities.isNotEmpty() },
        AccordionGroup("quality", ui("filter.quality"), ui("filter.quality.${draft.quality.name}")) {
            ChoiceChips(QualityFilter.entries, { it == draft.quality }, { ui("filter.quality.${it.name}") }) { draft = draft.copy(quality = it) }
        }.takeIf { has(FilterFacet.QUALITY) },
        AccordionGroup("mod", ui("filter.mod"), draft.mod?.let { modTitle(it, lang) } ?: any) {
            SearchChoice(view.mods, draft.mod, ui("filter.mod_find"), listOf(PickOption(null, any))) { draft = draft.copy(mod = it) }
        }.takeIf { has(FilterFacet.MOD) && (view.mods.isNotEmpty() || draft.mod != null) },
    )
    val switches = listOfNotNull(
        FilterSwitch(ui("filter.mine"), draft.mine) { draft = draft.copy(mine = it) }.takeIf { has(FilterFacet.MINE) },
        FilterSwitch(ui("filter.crafted"), draft.crafted) { draft = draft.copy(crafted = it) }.takeIf { has(FilterFacet.CRAFTED) },
        FilterSwitch(ui("filter.wearable"), draft.wearable) { draft = draft.copy(wearable = it) }.takeIf { has(FilterFacet.WEARABLE) },
        FilterSwitch(ui("filter.better"), draft.better) { draft = draft.copy(better = it) }.takeIf { has(FilterFacet.BETTER) },
        FilterSwitch(ui("filter.hide_worn"), draft.hideWorn) { draft = draft.copy(hideWorn = it) }.takeIf { has(FilterFacet.HIDE_WORN) },
    )
    val shown by produceState(view.count(state.filter), draft, view) { value = withContext(Dispatchers.Default) { view.count(draft) } }
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
            { LevelRangeHeader(draft.level, view.heroLevel) { draft = draft.copy(level = it) } }
        } else {
            null
        },
    )
}
