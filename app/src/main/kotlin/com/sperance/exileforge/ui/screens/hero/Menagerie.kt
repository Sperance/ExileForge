package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetKind
import com.sperance.exileforge.rules.content.PetLine
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.craft.ChoiceFrame
import com.sperance.exileforge.ui.screens.craft.ChoiceRow
import com.sperance.exileforge.ui.screens.craft.heldOmens
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** A species as the dictionary names it. */
fun petName(species: String): String = locOr("pet.$species", species)

/**
 * The menagerie (3.5.0, server 1.5.0): eggs from the bag hatch here, and every pet shows what it is, its
 * level and its lines; one combat pet and one helper go to work, an orb changes one, a spare one is let go for gold.
 * Since server 1.65.0 a pet takes the crafting orbs of items — rarity, lines, quality, a fractured line, corruption — and the
 * Omen of Choice's lines wait on it for the player's pick, as an item's do in the forge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun MenagerieSection(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.hero ?: return
    val index = s.index ?: return
    val pets = hero.pets
    val eggs = index.pets.eggs.values.filter { hero.count(it) > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ForgePanel {
            FirstVisit(Guide.PETS)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Engraved(ui("pets.title", pets.pets.size), modifier = Modifier.weight(1f))
                GuideButton(Guide.PETS)
            }
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
        if (pet.corrupted) Text(ui("pets.corrupted"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
        if (pet.quality > 0) Text(ui("pets.quality", pet.quality), color = GoldBright, style = MaterialTheme.typography.labelSmall)
        // Line by line, so a fractured one (server 1.65.0) is told apart: it stays through every orb.
        pet.lines.forEach { line ->
            menagerie.lines(pet.copy(lines = listOf(line))).firstOrNull()?.let {
                val text = lineText(index, it)
                Text(if (line.fractured) "$text · ${ui("pets.fractured")}" else text, color = if (line.fractured) Gold else ModBlue, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (pet.offer.isNotEmpty()) ChoiceFrame("forge.choice_title", "forge.choice_hint") {
            pet.offer.forEachIndexed { i, option ->
                val text = menagerie.lines(pet.copy(lines = listOf(option), offer = emptyList())).firstOrNull()?.let { lineText(index, it) } ?: displayName(option.code)
                ChoiceRow(text, "T${option.tier}") { if (!s.busy) vm.choosePetLine(pet.id, i) }
            }
        }
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

/**
 * The orbs at hand that go on this pet, each with what it does; one tap spends one on it. The crafting orbs of items
 * (server 1.65.0) are tried over the pet by the rules, as the forge tries them over an item, and an omen laid on the
 * next one — the Omen of Choice, of Corruption — is picked above them; the pets' own growth orb follows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PetOrbs(s: ForgeState, vm: ForgeViewModel, pet: Pet, onDismiss: () -> Unit) {
    val hero = s.hero ?: return
    val index = s.index ?: return
    val applier = remember(index) { OrbApplier(index) }
    val beast = OrbTarget.Beast(pet)
    val held = remember(index, hero.bag) { heldOmens(index, hero) }
    // Every crafting orb in the bag, each with the omens it goes on this pet with; null - the orb alone.
    val fits: List<Pair<Item, List<Omen?>>> = remember(applier, pet, held, hero.bag) {
        s.orbs.mapNotNull { item ->
            val orb = Orb.of(item.code)?.takeIf { hero.count(item.code) > 0 } ?: return@mapNotNull null
            (listOf<Omen?>(null) + held.filter { it.fits(orb) }).filter { applier.accepts(orb, beast, it) }.takeIf { it.isNotEmpty() }?.let { item to it }
        }
    }
    val omens = held.filter { omen -> fits.any { (_, with) -> omen in with } }
    var picked by remember(pet.id) { mutableStateOf<Omen?>(null) }
    // An omen spent to the last one, or one the pet no longer takes, falls away by itself.
    val omen = picked?.takeIf { it in omens }
    val growth = index.pets.orbs.keys.toList()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Engraved(ui("pets.orbs_of", petName(pet.species)))
            if (omens.isNotEmpty()) {
                Text(ui("forge.omen"), color = Rune, style = MaterialTheme.typography.titleSmall)
                PillTabs(listOf(ui("forge.omen_none")) + omens.map { itemTitle(it.code) }, omen?.let { omens.indexOf(it) + 1 } ?: 0, { picked = omens.getOrNull(it - 1) })
            }
            val shown = fits.filter { (_, with) -> omen in with }
            if (shown.isEmpty() && growth.none { hero.count(it) > 0 }) MutedText(ui("pets.no_orbs"))
            shown.forEach { (item, _) ->
                PetOrbRow(item.code, hero.count(item.code), Orb.of(item.code), enabled = !s.busy) { vm.petOrb(pet.id, item.code, omen?.code) }
            }
            growth.forEach { code -> PetOrbRow(code, hero.count(code), null, enabled = !s.busy && hero.count(code) > 0) { vm.petOrb(pet.id, code) } }
        }
    }
}

/** One orb of the pet's sheet: its glass, its name over what it does, and the button that spends one. */
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
