package com.sperance.exileforge.ui.screens.hero

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.hero.PetDuty
import com.sperance.exileforge.core.hero.PetFacts
import com.sperance.exileforge.core.hero.PetFilter
import com.sperance.exileforge.core.hero.PetSort
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.PetState
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.ui.components.AccordionGroup
import com.sperance.exileforge.ui.components.ChoiceChips
import com.sperance.exileforge.ui.components.DefaultInputs
import com.sperance.exileforge.ui.components.FilterAccordionSheet
import com.sperance.exileforge.ui.components.FilterBar
import com.sperance.exileforge.ui.components.PetFilterState
import com.sperance.exileforge.ui.components.RarityTiles

/*
 * Отбор питомцев (4.x): тот же фильтр-аккордеон, что у предметов, - строка над списком ([PetFilterBar]) и шторка
 * ([PetFilterSheet]) с поиском, родом, делом, редкостью, стихией и порядком; граней сразу сколько угодно. Логика отбора - одна,
 * `PetFilter` ядра; выбор запоминается на список ([com.sperance.exileforge.presentation.state.PetShelf]).
 */

/** Что отбор знает о питомцах героя [pets]: вид - из зверинца [menagerie], дело - из состояния, имя - словарём. */
internal fun petFacts(menagerie: Menagerie, pets: PetState): PetFacts = PetFacts({ menagerie.species(it.species) }, { pets.isActive(it.id) }, { petName(it.species) })

/** Подпись рода в отборе. */
private val PetKind.label: String get() = ui(if (this == PetKind.COMBAT) "pets.filter_combat" else "pets.filter_helper")

/** Подпись дела в отборе. */
private val PetDuty.label: String get() = when (this) {
    PetDuty.ANY -> ui("filter.any")
    PetDuty.WORKING -> ui("pets.filter_working")
    PetDuty.RESTING -> ui("pets.filter_resting")
}

/** Имя стихии удара. */
private fun elementTitle(element: String): String = locOr("pet.element.$element", element)

/** Порядок словами: обычный или выбранный со стрелкой направления. */
private fun PetFilter.sortLabel(): String = sort?.let { ui(it.title) + if (descending) " ↓" else " ↑" } ?: ui("pets.sort_default")

private val PetSort.title: String get() = when (this) {
    PetSort.LEVEL -> "pets.sort_level"
    PetSort.RARITY -> "pets.sort_rarity"
    PetSort.NEWEST -> "pets.sort_new"
}

/** Строка над списком питомцев: порядок и поиск словами, сколько видно и значок фильтра; нажатие открывает шторку. */
@Composable internal fun PetFilterBar(state: PetFilterState, shown: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val filter = state.filter
    FilterBar(
        listOfNotNull(filter.sortLabel() + " ▾", filter.query.takeIf { it.isNotBlank() }?.let { "«$it»" }).joinToString(" · "),
        shown,
        filter.tweaks(state.shelf.defaults),
        onOpen,
        modifier,
    )
}

/**
 * Шторка фильтра питомцев (4.x, «Аккордеон»): поиск по имени и роду, род и стихия - любые сразу, дело - одно, редкость плитками,
 * порядок (повторное нажатие - обратное направление). Варианты - те, что есть у питомцев [pets], и уже выбранные (их можно снять).
 * Правки идут в черновик; «Показать N» применяет его, «Сбросить» - к обычному списка.
 */
@Composable internal fun PetFilterSheet(state: PetFilterState, pets: List<Pet>, facts: PetFacts, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(state.filter) }
    val any = ui("filter.any")
    val kinds = remember(pets) { pets.mapNotNull { facts.species(it)?.kind }.distinct().sortedBy { it.ordinal } }
    val rarities = remember(pets) { pets.map { it.rarity }.distinct().sortedByDescending { it.ordinal } }
    val elements = remember(pets) { pets.flatMap(facts::elements).distinct() }
    val groups = listOfNotNull(
        AccordionGroup("kind", ui("pets.filter.kind"), draft.kinds.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.label } ?: any) {
            ChoiceChips((kinds + draft.kinds).distinct(), { it in draft.kinds }, { it.label }) { draft = draft.toggle(it) }
        }.takeIf { kinds.size > 1 || draft.kinds.isNotEmpty() },
        AccordionGroup("duty", ui("pets.filter.duty"), draft.duty.label) {
            ChoiceChips(PetDuty.entries, { it == draft.duty }, { it.label }) { draft = draft.copy(duty = it) }
        },
        AccordionGroup("rarity", ui("filter.rarity"), draft.rarities.takeIf { it.isNotEmpty() }?.sortedByDescending { it.ordinal }?.joinToString(", ") { rarityTitle(it) } ?: any) {
            RarityTiles((rarities + draft.rarities).distinct(), { it in draft.rarities }) { draft = draft.toggle(it) }
        },
        AccordionGroup("element", ui("pets.filter.element"), draft.elements.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(", ", transform = ::elementTitle) ?: any) {
            ChoiceChips((elements + draft.elements).distinct().sorted(), { it in draft.elements }, ::elementTitle) { draft = draft.toggleElement(it) }
        }.takeIf { elements.isNotEmpty() || draft.elements.isNotEmpty() },
        AccordionGroup("sort", ui("filter.sort"), draft.sortLabel()) {
            ChoiceChips(listOf<PetSort?>(null) + PetSort.entries, { it == draft.sort }, { sort -> sort?.let { ui(it.title) } ?: ui("pets.sort_default") }) { draft = draft.ordered(it) }
        },
    )
    val shown = remember(draft, pets) { draft.apply(pets, facts).size }
    val defaults = state.shelf.defaults
    FilterAccordionSheet(
        query = draft.query,
        onQuery = { draft = draft.copy(query = it.take(DefaultInputs.search)) },
        queryHint = ui("pets.search"),
        groups = groups,
        switches = emptyList(),
        show = ui("filter.show", shown),
        resettable = draft != defaults,
        onReset = { draft = defaults },
        onShow = {
            state.update(draft)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}
