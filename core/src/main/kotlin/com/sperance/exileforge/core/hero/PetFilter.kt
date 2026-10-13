package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.content.PetSpecies
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Что знает отбор питомцев о питомце (4.9.0; с 4.x - общий для зверинца и выбора цели кузницы): его вид ([species], null - вид
 * контенту неизвестен), стоит ли он в деле ([active]) и имя вида словами ([name]).
 */
class PetFacts(val species: (Pet) -> PetSpecies?, val active: (Pet) -> Boolean, val name: (Pet) -> String) {
    /** Стихии удара питомца: у гибрида - обе. */
    fun elements(pet: Pet): List<String> = species(pet)?.let { listOfNotNull(it.element, it.element2) }.orEmpty()

    /** Подходит ли питомец под поиск [query]: имя вида или его род; пустой поиск - все. */
    fun named(pet: Pet, query: String): Boolean {
        val words = query.trim()
        if (words.isEmpty()) return true
        val kind = species(pet)?.kind
        return name(pet).contains(words, ignoreCase = true) || kind?.let { locOr("pet.kind.$it", it.name).contains(words, ignoreCase = true) } == true
    }
}

/** Дело питомца в отборе: любое, в деле или в запасе. */
@Serializable
enum class PetDuty {
    ANY,
    WORKING,
    RESTING,
    ;

    fun admits(active: Boolean): Boolean = when (this) {
        ANY -> true
        WORKING -> active
        RESTING -> !active
    }
}

/** Порядок питомцев (4.9.0): по чему; равные - по имени вида. */
@Serializable
enum class PetSort {
    LEVEL,
    RARITY,

    /** Новые: позже вылупленный стоит в списке героя дальше. */
    NEWEST,
    ;

    fun value(pet: Pet, place: Int): Long = when (this) {
        LEVEL -> pet.level.toLong()
        RARITY -> pet.rarity.ordinal.toLong()
        NEWEST -> place.toLong()
    }
}

/**
 * Единый фильтр питомцев (4.x, как фильтр предметов): роды ([kinds], пусто - все), дело ([duty]), редкости и стихии (пусто - все),
 * порядок ([sort], null - обычный: в деле первыми, затем редкость и уровень по убыванию; [descending] - направление) и поиск
 * ([query] - только экрана, не запоминается). Внутри грани выбранное складывается «или», грани между собой - «и».
 */
@Serializable
data class PetFilter(
    val kinds: Set<PetKind> = emptySet(),
    val duty: PetDuty = PetDuty.ANY,
    val rarities: Set<Rarity> = emptySet(),
    val elements: Set<String> = emptySet(),
    val sort: PetSort? = null,
    val descending: Boolean = true,
    @Transient val query: String = "",
) {
    fun toggle(kind: PetKind) = copy(kinds = kinds.toggled(kind))

    fun toggle(rarity: Rarity) = copy(rarities = rarities.toggled(rarity))

    fun toggleElement(element: String) = copy(elements = elements.toggled(element))

    /** Выбор порядка: тот же - обратное направление, новый - по убыванию; null - обычный. */
    fun ordered(next: PetSort?): PetFilter = when {
        next == null -> copy(sort = null, descending = true)
        next == sort -> copy(descending = !descending)
        else -> copy(sort = next, descending = true)
    }

    /** Проходит ли [pet] все грани фильтра. */
    fun keeps(pet: Pet, facts: PetFacts): Boolean = (kinds.isEmpty() || facts.species(pet)?.kind in kinds) &&
        duty.admits(facts.active(pet)) &&
        (rarities.isEmpty() || pet.rarity in rarities) &&
        (elements.isEmpty() || facts.elements(pet).any { it in elements }) &&
        facts.named(pet, query)

    /** Питомцы [pets] (в порядке списка героя) под фильтром и в его порядке. */
    fun apply(pets: List<Pet>, facts: PetFacts): List<Pet> {
        val kept = pets.filter { keeps(it, facts) }
        val sort = sort ?: return kept.sortedWith(compareBy({ !facts.active(it) }, { -it.rarity.ordinal }, { -it.level }))
        val place = pets.withIndex().associate { (i, pet) -> pet.id to i }
        val by = compareBy<Pet> { sort.value(it, place.getValue(it.id)) }.let { if (descending) it.reversed() else it }
        return kept.sortedWith(by.thenBy { facts.name(it).lowercase() })
    }

    /** Сколько граней отличается от обычного фильтра [defaults] (порядок не сужает): число на значке фильтра. */
    fun tweaks(defaults: PetFilter): Int = listOf(kinds != defaults.kinds, duty != defaults.duty, rarities != defaults.rarities, elements != defaults.elements, query.isNotBlank()).count { it }

    private companion object {
        fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value
    }
}
