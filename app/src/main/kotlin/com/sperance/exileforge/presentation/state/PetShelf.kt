package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.hero.PetFilter
import kotlinx.serialization.Serializable

/**
 * Список питомцев приложения с фильтром (4.x, как [ItemShelf]): с чего он начинается ([defaults]); выбор фильтра запоминается на
 * список, на устройстве ([PetFilters]). Новый список - запись здесь.
 */
enum class PetShelf(val defaults: PetFilter = PetFilter()) {
    /** Зверинец героя. */
    MENAGERIE,

    /** Выбор питомца целью кузницы. */
    FORGE,
}

/** Запомненные фильтры списков питомцев на устройстве (4.x): у списка без записи - его обычный. */
@Serializable
data class PetFilters(val shelves: Map<PetShelf, PetFilter> = emptyMap()) {
    operator fun get(shelf: PetShelf): PetFilter = shelves[shelf] ?: shelf.defaults

    fun with(shelf: PetShelf, filter: PetFilter): PetFilters = copy(shelves = shelves + (shelf to filter.copy(query = "")))
}
