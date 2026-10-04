package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.forge.Smithy
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetLine
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.hero.PetIcon
import com.sperance.exileforge.ui.screens.hero.PetLines
import com.sperance.exileforge.ui.screens.hero.petName
import com.sperance.exileforge.ui.screens.hero.petSubtitle
import com.sperance.exileforge.ui.theme.*

/**
 * The forge over a pet (3.81.0): the orbs that change a pet are spent here and only here. The crafting orbs of items
 * (server 1.65.0) are tried over the pet by the rules, as over an item, an omen laid on the next one — the Omen of Choice,
 * of Corruption — is picked above them, and the pets' own growth orb follows; the Omen of Choice's lines wait for the pick here.
 */
@Composable internal fun PetForge(game: GameUi, smithy: Smithy, vm: SmithyViewModel) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val pets = hero.pets.pets
    if (pets.isEmpty()) {
        InfoCard(ui("pets.empty"), ui("pets.empty_hint"))
        return
    }
    val pet = pets.firstOrNull { it.id == smithy.pet } ?: pets.first()
    val menagerie = remember(index) { Menagerie(index) }
    val enabled = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    Spinner(
        ui("forge.pet_pick"),
        pet.id,
        pets.associate { it.id to "${petName(it.species)} · ${ui("pets.level", it.level)}" },
        !game.busy,
        onChange = vm::selectPet,
    )
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetIcon(game, pet.species, 40)
            Column(Modifier.weight(1f)) {
                Text(petName(pet.species), color = rarityColor(pet.rarity.name), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                menagerie.species(pet.species)?.let { MutedText(petSubtitle(it, pet)) }
            }
            Text(ui("pets.level", pet.level), color = GoldBright, style = MaterialTheme.typography.labelMedium)
        }
        PetLines(index, menagerie, pet)
    }
    if (pet.offer.isNotEmpty()) {
        ChoiceFrame("forge.choice_title", "forge.choice_hint") {
            pet.offer.forEachIndexed { i, option ->
                val text = menagerie.lines(pet.copy(lines = listOf(option), offer = emptyList())).firstOrNull()?.let { lineText(index, it) } ?: displayName(option.code.value)
                ChoiceRow(text, "T${option.tier}") { if (enabled) vm.choosePetLine(pet.id, i) }
            }
        }
    }
    PetOrbs(game, pet, enabled, vm)
}

/** The orbs at hand that go on this pet, each with what it does; one tap spends one on it. */
@Composable private fun PetOrbs(game: GameUi, pet: Pet, enabled: Boolean, vm: SmithyViewModel) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val applier = remember(index) { OrbApplier(index) }
    val beast = OrbTarget.Beast(pet)
    val held = remember(index, hero.bag) { heldOmens(index, hero) }
    // Every crafting orb in the bag, each with the omens it goes on this pet with; null - the orb alone.
    val fits: List<Pair<Item, List<Omen?>>> = remember(applier, pet, held, hero.bag) {
        game.orbs.mapNotNull { item ->
            val orb = Orb.of(item.code.value)?.takeIf { hero.count(item.code.value) > 0 } ?: return@mapNotNull null
            (listOf<Omen?>(null) + held.filter { it.fits(orb) }).filter { applier.accepts(orb, beast, it) }.takeIf { it.isNotEmpty() }?.let { item to it }
        }
    }
    val omens = held.filter { omen -> fits.any { (_, with) -> omen in with } }
    var picked by remember(pet.id) { mutableStateOf<Omen?>(null) }
    // An omen spent to the last one, or one the pet no longer takes, falls away by itself.
    val omen = picked?.takeIf { it in omens }
    val growth = index.pets.orbs.keys.toList()
    ForgePanel {
        Engraved(ui("pets.orbs_of", petName(pet.species)))
        if (omens.isNotEmpty()) {
            Text(ui("forge.omen"), color = Rune, style = MaterialTheme.typography.titleSmall)
            PillTabs(listOf(ui("forge.omen_none")) + omens.map { itemTitle(it.code) }, omen?.let { omens.indexOf(it) + 1 } ?: 0, { picked = omens.getOrNull(it - 1) })
        }
        val shown = fits.filter { (_, with) -> omen in with }
        if (shown.isEmpty() && growth.none { hero.count(it) > 0 }) MutedText(ui("pets.no_orbs"))
        shown.forEach { (item, _) ->
            PetOrbRow(item.code.value, hero.count(item.code.value), Orb.of(item.code.value), enabled) { vm.petOrb(pet.id, item.code.value, omen?.code) }
        }
        growth.forEach { code -> PetOrbRow(code, hero.count(code), null, enabled && hero.count(code) > 0) { vm.petOrb(pet.id, code) } }
    }
}

/** One orb for the pet: its glass, its name over what it does, and the button that spends one. */
@Composable private fun PetOrbRow(code: String, held: Long, orb: Orb?, enabled: Boolean, onSpend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (orb != null) OrbGlyph(orb, Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(itemTitle(code), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            MutedText(itemDescription(code))
        }
        ForgeOutlinedButton(onClick = onSpend, enabled = enabled) { Text("× ${number(held.toDouble())}") }
    }
}

/** A pet line has no tiers of its own; its rolls read as five steps, T1 the best, as an item's tiers do. */
private val PetLine.tier: Int get() = PET_TIERS - (shares.average().takeIf { it.isFinite() } ?: 0.0).times(PET_TIERS).toInt().coerceIn(0, PET_TIERS - 1)

private const val PET_TIERS = 5
