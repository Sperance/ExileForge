package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** A species as the dictionary names it. */
fun petName(species: String): String = locOr("pet.$species", species)

/**
 * The menagerie (3.5.0, server 1.5.0): eggs from the bag hatch here, and every pet shows what it is, its
 * level and its lines; one combat pet and one helper go to work, a pet orb changes one, a spare one is let go for gold.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun MenagerieSection(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.hero ?: return
    val index = s.index ?: return
    val pets = hero.pets
    val eggs = index.pets.eggs.values.filter { hero.count(it) > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ForgePanel {
            Engraved(ui("pets.title", pets.pets.size, pets.cap))
            MutedText(ui("pets.hint"))
            if (eggs.isEmpty()) MutedText(ui("pets.no_eggs"))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eggs.forEach { egg ->
                    ForgeOutlinedButton(onClick = { vm.hatchPet(egg) }, enabled = !s.busy && pets.pets.size < pets.cap) {
                        Text(ui("pets.hatch", itemTitle(egg), hero.count(egg)))
                    }
                }
            }
        }
        if (pets.pets.isEmpty()) InfoCard(ui("pets.empty"), ui("pets.empty_hint"))
        pets.pets.sortedWith(compareBy({ !pets.isActive(it.id) }, { -it.rarity.ordinal }, { -it.level })).forEach { pet -> PetCard(s, vm, pet) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PetCard(s: ForgeState, vm: ForgeViewModel, pet: Pet) {
    val hero = s.hero ?: return
    val index = s.index ?: return
    val menagerie = remember(index) { Menagerie(index) }
    val kind = menagerie.species(pet.species) ?: return
    val active = hero.pets.isActive(pet.id)
    var orbs by remember(pet.id) { mutableStateOf(false) }
    var releasing by remember(pet.id) { mutableStateOf(false) }
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(petName(pet.species), color = rarityColor(pet.rarity.name), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val what = when (kind.kind) {
                    PetKind.COMBAT -> listOfNotNull(kind.role?.let { locOr("pet.role.$it", it.name) }, kind.element?.let { locOr("pet.element.$it", it) })
                    PetKind.HELPER -> listOfNotNull(kind.focus?.let { locOr("pet.focus.$it", it.name) })
                }
                MutedText((listOf(locOr("pet.kind.${kind.kind}", kind.kind.name)) + what).joinToString(" · "))
            }
            Text(ui("pets.level", pet.level), color = GoldBright, style = MaterialTheme.typography.labelMedium)
        }
        if (active) Text(ui("pets.at_work"), color = Vital, style = MaterialTheme.typography.labelSmall)
        menagerie.lines(pet).forEach { Text(lineText(index, it), color = ModBlue, style = MaterialTheme.typography.bodySmall) }
        if (kind.kind == PetKind.COMBAT) {
            val sheet = menagerie.sheet(pet)
            MutedText(ui("pets.sheet", number(sheet["STOCK_HEALTH"] ?: 0.0), number(sheet["STOCK_ATTACK_${kind.element}"] ?: 0.0)))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ForgeButton(onClick = { vm.activatePet(pet.id) }, enabled = !s.busy) { Text(ui(if (active) "pets.rest" else "pets.work")) }
            ForgeOutlinedButton(onClick = { orbs = true }, enabled = !s.busy) { Text(ui("pets.orbs")) }
            ForgeTextButton(onClick = { releasing = true }, enabled = !s.busy) { Text(ui("pets.release")) }
        }
    }
    if (orbs) PetOrbs(s, vm, pet) { orbs = false }
    if (releasing) ConfirmSheet(title = ui("pets.release_q"), confirm = ui("pets.release"), danger = true, subtitle = petName(pet.species),
        ledger = listOf(LedgerLine(ui("pets.release_gold"), number(index.rules.pets.releasePrice(pet.rarity, pet.level).toDouble()), Tone.GAIN)),
        onDismiss = { releasing = false }) { releasing = false; vm.releasePet(pet.id) }
}

/** The pet orbs at hand, each with what it does; one tap spends one on the pet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PetOrbs(s: ForgeState, vm: ForgeViewModel, pet: Pet, onDismiss: () -> Unit) {
    val hero = s.hero ?: return
    val index = s.index ?: return
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Engraved(ui("pets.orbs_of", petName(pet.species)))
            index.pets.orbs.keys.forEach { orb ->
                val held = hero.count(orb)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(itemTitle(orb), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                        MutedText(locOr("item.$orb.description", ""))
                    }
                    ForgeOutlinedButton(onClick = { vm.petOrb(pet.id, orb) }, enabled = !s.busy && held > 0) { Text("× ${number(held.toDouble())}") }
                }
            }
        }
    }
}
