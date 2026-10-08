package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A shelf of the stash's slot chips (3.30.0): the slots the rules tag alike are one — both weapons, both rings,
 * the three flask bays — and every other slot is its own. С 4.2.0 - вариант «Типа» фильтра предметов ([ItemType.Group]).
 */
@Serializable
@JvmInline
value class SlotGroup(val tag: String) {
    fun title(lang: Lang): String = when (tag) {
        WEAPON, RING, FLASK, TOOL -> ui("stash.group.$tag")
        else -> Slot.entries.firstOrNull { it.tag == tag }?.let { slotTitle(it, lang) } ?: tag
    }

    /** Место группы, чей значок она носит в рейке тайника (3.90.3): первое место с её тегом. */
    val slot: Slot get() = Slot.entries.first { it.tag == tag }

    val isTool: Boolean get() = tag == TOOL

    companion object {
        const val WEAPON = "weapon"
        const val RING = "ring"
        const val FLASK = "flask"
        const val TOOL = "tool"
        fun of(slot: Slot) = SlotGroup(slot.tag)
    }
}

/**
 * Раздел списка шире места (4.2.0): вся экипировка, карты, инструменты, надетое - быстрые варианты «Типа» на полке у наковальни.
 * Новый раздел - запись здесь и в [ItemShelf.sections].
 */
@Serializable
enum class ItemSection {
    GEAR,
    MAPS,
    TOOLS,
    WORN,
    ;

    fun admits(piece: ItemView): Boolean = when (this) {
        GEAR -> piece.slot != Slot.MAP && !piece.slot.isTool
        MAPS -> piece.slot == Slot.MAP
        TOOLS -> piece.slot.isTool
        WORN -> piece.isWorn
    }
}

/** «Тип» фильтра предметов (4.2.0): место (группа мест рейки) или раздел шире места - одно состояние для рейки, полки и шторки. */
@Serializable
sealed interface ItemType {
    fun admits(piece: ItemView): Boolean

    @Serializable
    data class Group(val group: SlotGroup) : ItemType {
        override fun admits(piece: ItemView): Boolean = SlotGroup.of(piece.slot) == group
    }

    @Serializable
    data class Section(val section: ItemSection) : ItemType {
        override fun admits(piece: ItemView): Boolean = section.admits(piece)
    }
}

/** Качество в фильтре предметов (4.2.0): любое, есть хоть сколько-то, на потолке своего вида (фляги - свой), нет совсем. */
@Serializable
enum class QualityFilter {
    ANY,
    SOME,
    MAX,
    NONE,
    ;

    fun admits(piece: ItemView): Boolean {
        val quality = piece.item.quality
        return when (this) {
            ANY -> true
            SOME -> quality > 0
            MAX -> quality > 0 && quality >= piece.qualityCap
            NONE -> quality == 0
        }
    }
}

/** Потолок качества вещи: фляги - свой потолок правил, прочее - общий. */
private val ItemView.qualityCap: Int get() = if (slot.isFlask) index.rules.flasks.maxQuality else index.rules.quality.max

/** Порядки списка предметов (3.30.0, с 4.2.0 - общие для всех списков): новые первыми, по редкости, уровню предмета или цене списка. */
@Serializable
enum class ItemSort {
    NEWEST,
    RARITY,
    LEVEL,
    PRICE,
    ;

    /**
     * Список в этом порядке. Сервер держит вещи старыми первыми, так что новые - список наоборот; прочие порядки среди равных
     * - тоже новые первыми. [price] - цена вещи в этом списке (торговцу, у торговца), когда известна.
     */
    fun order(pieces: List<ItemView>, price: (ItemView) -> Long?): List<ItemView> {
        val newest = pieces.asReversed()
        return when (this) {
            NEWEST -> newest
            RARITY -> newest.sortedByDescending { it.rarity.ordinal }
            LEVEL -> newest.sortedByDescending { it.level }
            PRICE -> newest.sortedByDescending { price(it) ?: -1L }
        }
    }
}

/** Что умеет сузить фильтр списка: каждая грань - строка шторки или переключатель. */
enum class FilterFacet { TYPE, RARITY, QUALITY, MINE, WEARABLE, HIDE_WORN, SORT }

/**
 * Список предметов приложения с фильтром (4.2.0): какие грани фильтра в нём есть ([facets]), какие порядки ([sorts]) и с чего он
 * начинается ([defaults]). Выбор фильтра запоминается на список, на устройстве ([ItemFilters]). Новый список - запись здесь.
 */
enum class ItemShelf(val facets: Set<FilterFacet>, val sorts: List<ItemSort>, val defaults: ItemFilter, val sections: List<ItemSection> = emptyList()) {
    /** Тайник героя: тип - и рейка, цена - торговца за копию; надетое скрыто, пока игрок его не покажет. */
    STASH(FilterFacet.entries.toSet(), ItemSort.entries, ItemFilter(hideWorn = true)),

    /** Выбор вещи в кузнице: сферам всё равно, надета ли вещь, поэтому надетое видно. */
    FORGE(FilterFacet.entries.toSet() - FilterFacet.WEARABLE, ItemSort.entries, ItemFilter(sort = ItemSort.RARITY), ItemSection.entries),

    /** Товар торговца: его вещи не сделаны героем и не надеты; цена - торговца. */
    MERCHANT(setOf(FilterFacet.TYPE, FilterFacet.RARITY, FilterFacet.QUALITY, FilterFacet.WEARABLE, FilterFacet.SORT), ItemSort.entries, ItemFilter()),
}

/**
 * Единый фильтр предметов (4.2.0, обобщение фильтра тайника 3.30.0): тип ([type], null - всё), редкости (пусто - все), качество,
 * «сделано мной» (поле `maker` копии - имя героя), «могу надеть», «скрыть надетое», порядок и поиск. Поиск - только экрана и не
 * запоминается; прочее хранится на устройстве для своего списка ([ItemShelf]).
 */
@Serializable
data class ItemFilter(
    val type: ItemType? = null,
    val rarities: Set<Rarity> = emptySet(),
    val quality: QualityFilter = QualityFilter.ANY,
    val mine: Boolean = false,
    val wearable: Boolean = false,
    val hideWorn: Boolean = false,
    val sort: ItemSort = ItemSort.NEWEST,
    @Transient val query: String = "",
) {
    fun toggle(rarity: Rarity) = copy(rarities = if (rarity in rarities) rarities - rarity else rarities + rarity)

    /** Сколько настроек отличается от обычных списка [shelf]: число на значке фильтра. */
    fun tweaks(shelf: ItemShelf): Int {
        val base = shelf.defaults
        return listOf(type != base.type, rarities != base.rarities, quality != base.quality, mine != base.mine, wearable != base.wearable, hideWorn != base.hideWorn, query.isNotBlank())
            .count { it }
    }

    /**
     * Проходит ли [piece]: [heroName] - имя героя для «сделано мной», [unmet] - чего шаблону не хватает до «могу надеть».
     * Грани, которых нет у списка [shelf], не сужают его. «Скрыть надетое» - правило снаряжения: инструмент виден всегда.
     */
    fun admits(piece: ItemView, shelf: ItemShelf, heroName: String, unmet: (String) -> List<String>): Boolean {
        fun on(facet: FilterFacet) = facet in shelf.facets
        return (!on(FilterFacet.TYPE) || type?.admits(piece) != false) &&
            (!on(FilterFacet.RARITY) || rarities.isEmpty() || piece.rarity in rarities) &&
            (!on(FilterFacet.QUALITY) || quality.admits(piece)) &&
            (!on(FilterFacet.MINE) || !mine || piece.item.maker?.hero == heroName) &&
            (!on(FilterFacet.WEARABLE) || !wearable || unmet(piece.code).isEmpty()) &&
            (!on(FilterFacet.HIDE_WORN) || !hideWorn || !piece.isWorn || piece.slot.isTool) &&
            ItemSearch.matches(piece, query)
    }

    /** Список [pieces] под фильтром и в его порядке; [price] - цена вещи в этом списке. */
    fun apply(pieces: List<ItemView>, shelf: ItemShelf, heroName: String, unmet: (String) -> List<String>, price: (ItemView) -> Long?): List<ItemView> = (if (FilterFacet.SORT in shelf.facets) sort else shelf.defaults.sort).order(pieces.filter { admits(it, shelf, heroName, unmet) }, price)
}

/** Запомненные фильтры списков на устройстве (4.2.0): у списка без записи - его обычный. */
@Serializable
data class ItemFilters(val shelves: Map<ItemShelf, ItemFilter> = emptyMap()) {
    operator fun get(shelf: ItemShelf): ItemFilter = shelves[shelf] ?: shelf.defaults

    fun with(shelf: ItemShelf, filter: ItemFilter): ItemFilters = copy(shelves = shelves + (shelf to filter.copy(query = "")))
}

/** Whether the hero wears this copy, on the body or in a socket. */
val ItemView.isWorn: Boolean get() = equipped || socketed

/**
 * Пункты рейки тайника (3.90.3): группы мест в порядке мест тела, инструменты - в конце, со счётом вещей [pieces];
 * группа без вещей пункта не получает. С 4.2.0 - и варианты «Типа» в шторке фильтра.
 */
fun railGroups(pieces: List<ItemView>): List<Pair<SlotGroup, Int>> {
    val counts = pieces.groupingBy { SlotGroup.of(it.slot) }.eachCount()
    return Slot.entries.map(SlotGroup::of).distinct().sortedBy { it.isTool }.mapNotNull { group -> counts[group]?.let { group to it } }
}

/** Список [pieces] под фильтром [filter] списка [shelf] у героя на экране: «сделано мной» - по его имени, «могу надеть» - по его листу. */
fun GameUi.itemShelf(pieces: List<ItemView>, shelf: ItemShelf, filter: ItemFilter, price: (ItemView) -> Long?): List<ItemView> = filter.apply(pieces, shelf, hero?.info?.name.orEmpty(), { unmetFor(it) }, price)
