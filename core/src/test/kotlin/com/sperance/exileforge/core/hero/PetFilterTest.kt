package com.sperance.exileforge.core.hero

import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.content.PetSpecies
import com.sperance.exileforge.rules.content.Rarity
import kotlin.test.Test
import kotlin.test.assertEquals

/** Сочетание граней фильтра питомцев: внутри грани - «или», между гранями - «и»; порядок и обычный порядок. */
class PetFilterTest {
    private val species = mapOf(
        "wolf" to PetSpecies("wolf", PetKind.COMBAT, "forest", element = "FIRE"),
        "drake" to PetSpecies("drake", PetKind.COMBAT, "cave", element = "COLD", element2 = "FIRE"),
        "owl" to PetSpecies("owl", PetKind.HELPER, "forest"),
    )
    private val pets = listOf(
        Pet("a", "wolf", Rarity.MAGIC, level = 5),
        Pet("b", "drake", Rarity.RARE, level = 3),
        Pet("c", "owl", Rarity.RARE, level = 9),
        Pet("d", "wolf", Rarity.UNIQUE, level = 1),
    )
    private val facts = PetFacts({ species[it.species] }, { it.id == "c" }, { it.species })

    private fun ids(filter: PetFilter) = filter.apply(pets, facts).map { it.id }.toSet()

    @Test fun facetsCombine() {
        assertEquals(setOf("b", "c", "d"), ids(PetFilter(rarities = setOf(Rarity.RARE, Rarity.UNIQUE))))
        assertEquals(setOf("b", "d"), ids(PetFilter(kinds = setOf(PetKind.COMBAT), rarities = setOf(Rarity.RARE, Rarity.UNIQUE))))
        assertEquals(setOf("a", "b", "d"), ids(PetFilter(elements = setOf("FIRE"))))
        assertEquals(setOf("b"), ids(PetFilter(elements = setOf("COLD"), duty = PetDuty.RESTING)))
        assertEquals(setOf("c"), ids(PetFilter(duty = PetDuty.WORKING)))
        assertEquals(setOf("a", "d"), ids(PetFilter(query = "wol")))
    }

    @Test fun ordering() {
        assertEquals(listOf("c", "d", "b", "a"), PetFilter().apply(pets, facts).map { it.id })
        val byLevel = PetFilter().ordered(PetSort.LEVEL)
        assertEquals(listOf("c", "a", "b", "d"), byLevel.apply(pets, facts).map { it.id })
        assertEquals(listOf("d", "b", "a", "c"), byLevel.ordered(PetSort.LEVEL).apply(pets, facts).map { it.id })
    }
}
