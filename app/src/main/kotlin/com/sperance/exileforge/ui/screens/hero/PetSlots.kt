package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * Питомцы в деле под снаряжением героя (3.90.2): по месту на каждый род - боевой и помощник - карточка Зверинца ([PetCard]).
 * Нажатие открывает полную карточку и «Заменить» - список других питомцев того же рода; пустое место открывает список сразу.
 * [onActivate] null - замена закрыта: в заходе и Испытании питомцы, как и снаряжение, не меняются.
 */
@Composable
fun PetSlots(game: GameUi, onActivate: ((String) -> Unit)?) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val menagerie = remember(index) { Menagerie(index) }
    val pets = hero.pets
    var opened by remember { mutableStateOf<Pet?>(null) }
    var replacing by remember { mutableStateOf<PetKind?>(null) }
    val kindOf = { pet: Pet -> menagerie.species(pet.species)?.kind }
    val candidates = { kind: PetKind ->
        pets.pets.filter { !pets.isActive(it.id) && kindOf(it) == kind }.sortedWith(compareBy({ -it.rarity.ordinal }, { -it.level }))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Engraved(ui("pets.slots_title"))
        PetKind.entries.forEach { kind ->
            val pet = pets.active.firstOrNull { kindOf(it) == kind }
            val species = pet?.let { menagerie.species(it.species) }
            if (pet != null && species != null) {
                PetCard(game, index, menagerie, species, pet, active = true, onClick = { opened = pet })
            } else {
                EmptySlot(kind, enabled = onActivate != null && candidates(kind).isNotEmpty()) { replacing = kind }
            }
        }
    }
    opened?.let { pet ->
        val species = menagerie.species(pet.species) ?: return@let
        val others = candidates(species.kind)
        ForgeSheet(onDismissRequest = { opened = null }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PetCard(game, index, menagerie, species, pet, active = true)
                ForgeButton(
                    onClick = {
                        opened = null
                        replacing = species.kind
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = onActivate != null && others.isNotEmpty() && !game.busy,
                ) { Text(ui("pets.replace")) }
                ReplaceNote(onActivate != null, others.isNotEmpty())
            }
        }
    }
    replacing?.let { kind ->
        val others = candidates(kind)
        ForgeSheet(onDismissRequest = { replacing = null }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Engraved(ui("pets.replace_title", kindTitle(kind)))
                ReplaceNote(onActivate != null, others.isNotEmpty())
                others.forEach { pet ->
                    val species = menagerie.species(pet.species) ?: return@forEach
                    val pick = onActivate?.let { activate ->
                        {
                            replacing = null
                            activate(pet.id)
                        }
                    }
                    PetCard(game, index, menagerie, species, pet, active = false, onClick = pick)
                }
            }
        }
    }
}

/** Пустое место рода [kind]: нажатие - выбрать питомца, если есть кого и замена открыта. */
@Composable private fun EmptySlot(kind: PetKind, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().depthInset(shape).border(1.dp, Bronze, shape).clickable(enabled = enabled, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ui("pets.slot_empty", kindTitle(kind)), color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (enabled) Text("+", color = Gold, style = MaterialTheme.typography.titleMedium)
    }
}

/** Почему заменить нельзя: замена закрыта заходом или других питомцев этого рода нет. */
@Composable private fun ReplaceNote(open: Boolean, any: Boolean) {
    when {
        !open -> MutedText(ui("pets.replace_locked"))
        !any -> MutedText(ui("pets.replace_none"))
    }
}

private fun kindTitle(kind: PetKind): String = locOr("pet.kind.$kind", kind.name)
