package com.sperance.exileforge.core.filter

import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.PickOption
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.rules.content.Rarity

/** Поиск строки списка предметов: [text] как набран; находит вещь по имени и строкам ([ItemSearch]). Не запоминается. */
@JvmInline
value class ItemQuery(val text: String) {
    fun matches(piece: ItemView): Boolean = ItemSearch.matches(piece, text)

    companion object {
        /** Ничего не набрано: находит всё. */
        val NONE = ItemQuery("")
    }
}

/**
 * Витрина списка предметов с фильтром: вещи [pieces] списка [shelf] под фильтром [filter] и поиском [query]; о герое спрашивает
 * [judge], цена вещи в этом списке - [price], подписи - на языке [lang]. Видимое ([visible]), рейка ([groups]) и варианты шторки
 * считаются при сборке, так что витрину собирают вне главного потока: «лучше надетого» меряет вещи.
 */
class ItemShelfView(
    val pieces: List<ItemView>,
    val shelf: ItemShelf,
    val filter: ItemFilter,
    val query: ItemQuery,
    val lang: Lang,
    private val judge: ItemJudge,
    private val price: (ItemView) -> Long?,
) {
    /** Вещи списка без спрятанного «скрыть надетое» ([ItemFilter.hides]) - их считает рейка. */
    val counted: List<ItemView> = pieces.filterNot { filter.hides(it, shelf) }

    /** Группы мест со счётом вещей ([railGroups]): пункты рейки и варианты «Типа» в шторке. */
    val groups: List<Pair<SlotGroup, Int>> = railGroups(counted)

    /** Редкости, что есть в списке, от высшей: плитки редкости в шторке. */
    val rarities: List<Rarity> = pieces.map { it.rarity }.distinct().sortedByDescending { it.ordinal }

    /** Вещи под фильтром и поиском, в порядке фильтра. */
    val visible: List<ItemView> = filter.order(shelf).order(pieces.filter { passes(it, filter) }, price)

    /**
     * Варианты «по моду» - характеристики, что есть у вещей списка ([modChoices]), ключ - характеристика, имя - [modTitle]: набор
     * собирается один раз, при первом чтении.
     */
    val mods: List<PickOption<String>> by lazy { modChoices(pieces, lang).map { PickOption(it, modTitle(it, lang)) } }

    /** Уровень героя - для «Около моего» вилки уровня. */
    val heroLevel: Int get() = judge.heroLevel

    /** Сколько вещей пройдёт черновик фильтра [draft] при том же поиске - «Показать N» шторки. */
    fun count(draft: ItemFilter): Int = if (draft == filter) visible.size else pieces.count { passes(it, draft) }

    private fun passes(piece: ItemView, with: ItemFilter): Boolean = query.matches(piece) && with.admits(piece, shelf, judge)
}
