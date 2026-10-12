package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.PetState
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.content.PetSpecies
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.CompactSearchField
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.rarityColor

/**
 * Что знает отбор зверинца о питомце (4.9.0): его вид ([species], null - вид контенту неизвестен) и стоит ли он в деле ([active]).
 */
internal class PetFacts(val species: (Pet) -> PetSpecies?, val active: (Pet) -> Boolean) {
    /** Стихии удара питомца: у гибрида - обе. */
    fun elements(pet: Pet): List<String> = species(pet)?.let { listOfNotNull(it.element, it.element2) }.orEmpty()

    /** Имя вида питомца и его род - то, по чему ищет поле поиска. */
    fun named(pet: Pet, query: String): Boolean {
        val words = query.trim()
        if (words.isEmpty()) return true
        val kind = species(pet)?.kind
        return petName(pet.species).contains(words, ignoreCase = true) || kind?.let { locOr("pet.kind.$it", it.name).contains(words, ignoreCase = true) } == true
    }
}

/**
 * Чип отбора зверинца (4.9.0, макет «Зверинец», вариант A «Как состав гильдии»): подпись, её цвет и кто проходит. Отбор по
 * признаку - запись [Trait]; редкость и стихия - по тем, что есть у питомцев ([of]). Новый отбор - запись, экран его не знает.
 */
internal sealed interface PetFilter {
    val label: String
    val tint: Color? get() = null

    fun keeps(pet: Pet, facts: PetFacts): Boolean

    /** Отбор по роли и делу. */
    enum class Trait(private val key: String, private val test: (Pet, PetFacts) -> Boolean) : PetFilter {
        ALL("pets.filter_all", { _, _ -> true }),
        COMBAT("pets.filter_combat", { pet, facts -> facts.species(pet)?.kind == PetKind.COMBAT }),
        HELPER("pets.filter_helper", { pet, facts -> facts.species(pet)?.kind == PetKind.HELPER }),
        WORKING("pets.filter_working", { pet, facts -> facts.active(pet) }),
        RESTING("pets.filter_resting", { pet, facts -> !facts.active(pet) }),
        ;

        override val label: String get() = ui(key)

        override fun keeps(pet: Pet, facts: PetFacts) = test(pet, facts)
    }

    /** Питомцы редкости [rarity]. */
    data class OfRarity(val rarity: Rarity) : PetFilter {
        override val label: String get() = ui("enum.rarity.${rarity.name}")
        override val tint: Color get() = rarityColor(rarity.name)

        override fun keeps(pet: Pet, facts: PetFacts) = pet.rarity == rarity
    }

    /** Питомцы со стихией удара [element]. */
    data class OfElement(val element: String) : PetFilter {
        override val label: String get() = locOr("pet.element.$element", element)

        override fun keeps(pet: Pet, facts: PetFacts) = element in facts.elements(pet)
    }

    companion object {
        /** Лента чипов: признаки, затем редкости и стихии, что есть у питомцев [pets]. */
        fun of(pets: List<Pet>, facts: PetFacts): List<PetFilter> = Trait.entries +
            pets.map { it.rarity }.distinct().sortedBy { it.ordinal }.map(::OfRarity) +
            pets.flatMap(facts::elements).distinct().sorted().map(::OfElement)
    }
}

/** Порядок зверинца (4.9.0): по чему и в какую сторону; равные - по имени вида. */
internal enum class PetSort(val title: String, val value: (Pet, Int) -> Long) {
    LEVEL("pets.sort_level", { pet, _ -> pet.level.toLong() }),
    RARITY("pets.sort_rarity", { pet, _ -> pet.rarity.ordinal.toLong() }),

    /** Новые: позже вылупленный стоит в списке героя дальше. */
    NEWEST("pets.sort_new", { _, place -> place.toLong() }),
}

/** Выбранный порядок: [sort] и направление. */
internal data class PetOrder(val sort: PetSort, val descending: Boolean = true) {
    fun apply(pets: List<Pet>): List<Pet> {
        val place = pets.withIndex().associate { (i, pet) -> pet.id to i }
        val by = compareBy<Pet> { sort.value(it, place.getValue(it.id)) }.let { if (descending) it.reversed() else it }
        return pets.sortedWith(by.thenBy { petName(it.species).lowercase() })
    }

    companion object {
        /** Без выбора: в деле первыми, затем редкость и уровень по убыванию. */
        fun standard(pets: List<Pet>, state: PetState): List<Pet> = pets.sortedWith(compareBy({ !state.isActive(it.id) }, { -it.rarity.ordinal }, { -it.level }))
    }
}

/** Повторный выбор того же порядка - обратный, новый - по убыванию. */
internal fun PetOrder?.next(sort: PetSort): PetOrder = if (this?.sort == sort) copy(descending = !descending) else PetOrder(sort)

/** Плотный поиск по имени и виду и справа кнопка порядка «⇅» (4.9.0): как у состава гильдии. */
@Composable internal fun PetSearch(query: String, limit: Int, order: PetOrder?, onQuery: (String) -> Unit, onOrder: (PetOrder?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactSearchField(query, { onQuery(it.take(limit)) }, ui("pets.search"), Modifier.weight(1f))
        PetSortButton(order, onOrder)
    }
}

/** Кнопка «⇅»: меню порядков и порядок по умолчанию; выбранный порядок подсвечивает кнопку. */
@Composable private fun PetSortButton(order: PetOrder?, onOrder: (PetOrder?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box {
        Box(
            Modifier.size(40.dp).clip(shape).background(Panel).border(1.dp, if (order != null) Gold.copy(alpha = .6f) else Bronze, shape)
                .clickable(onClickLabel = ui("pets.sort")) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SwapVert, ui("pets.sort"), tint = if (order != null) GoldBright else Muted, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            DropdownMenuItem(
                text = { Text(ui("pets.sort_default"), color = if (order == null) GoldBright else Parchment) },
                onClick = {
                    open = false
                    onOrder(null)
                },
            )
            PetSort.entries.forEach { sort ->
                val mark = when {
                    order?.sort != sort -> ""
                    order.descending -> " ↓"
                    else -> " ↑"
                }
                DropdownMenuItem(
                    text = { Text(ui(sort.title) + mark, color = if (order?.sort == sort) GoldBright else Parchment) },
                    onClick = {
                        open = false
                        onOrder(order.next(sort))
                    },
                )
            }
        }
    }
}

/** Одна лента плотных чипов со счётчиками (4.9.0): у каждого - сколько питомцев [pets] под ним; выбранный - в нити `Gold`. */
@Composable internal fun PetFilterRow(filters: List<PetFilter>, pets: List<Pet>, facts: PetFacts, chosen: PetFilter, onChoose: (PetFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(filters, key = { it.toString() }) { filter ->
            val on = filter == chosen
            val shape = RoundedCornerShape(8.dp)
            Text(
                ui("pets.filter_count", filter.label, pets.count { filter.keeps(it, facts) }),
                color = filter.tint ?: if (on) GoldBright else Muted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.height(28.dp).clip(shape).background(if (on) PanelRaised else Color.Transparent)
                    .border(1.dp, if (on) Gold.copy(alpha = .5f) else Bronze, shape).clickable { onChoose(filter) }
                    .wrapContentHeight(Alignment.CenterVertically).padding(horizontal = 10.dp),
            )
        }
    }
}
