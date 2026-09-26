package com.sperance.exileforge.ui.screens.hero

import com.sperance.exileforge.core.display.Glyph

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.icons.orbArt

/**
 * The administrator's orb panel: one currency orb on one item, with a top-up beside it.
 *
 * A player spends orbs in the forge (`CraftScreen`); this is the same command behind the admin tab.
 *
 * Every orb of the server's `CURRENCY` category is offered as it is seeded, with the count the
 * character owns beside it. What an orb does — which rarity it demands, what it rerolls, what it
 * leaves alone — is printed from its own rule and then decided by the server: the client sends the
 * pair and shows the sentence that comes back, including a refusal.
 *
 * [onGrant] adds the administrator's top-up button, so an orb can be tried without farming it first.
 */
@Composable fun OrbPanel(s: ForgeState, instanceId: String, onSelect: (String) -> Unit,
    onApply: (String, String) -> Unit, onGrant: ((String) -> Unit)? = null) {
    val hero = s.play.hero ?: return
    val instance = hero.inventory.firstOrNull { it.id == instanceId }
    val owned = hero.bag.associate { it.itemId to it.amount }
    val orb = s.world.orbs.firstOrNull { it.id == s.play.selectedOrb }
    val enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    Engraved(ui("orb.title"))
    if (s.world.orbs.isEmpty()) { Text(ui("orb.none"), color = Muted); return }
    Spinner(ui("orb.orb"), s.play.selectedOrb,
        s.world.orbs.associate { it.id to "${it.title(s.lang)} · ${owned[it.id] ?: 0L}" }, enabled, glyph = Glyph.CURRENCY,
        optionArt = orbArt(s.world.orbs), onChange = onSelect)
    // An orb the client has no translation for still explains itself: the server's dictionary has one.
    orb?.let { MutedText(it.details(s.lang)) }
    if (instance == null) { Text(ui("orb.choose_item"), color = Muted); return }
    val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
    PropertyRow(ui("common.item"), document.text("name"), Glyph.ITEM)
    PropertyRow(ui("orb.copy_rarity"), rarityTitle(instance.rarity, s.lang), Glyph.RARITY)
    if (instance.corrupted) Text(ui("orb.corrupted"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
    ForgeButton(enabled = enabled && !instance.corrupted && s.play.selectedOrb.isNotBlank(),
        onClick = { onApply(instance.id, s.play.selectedOrb) }, modifier = Modifier.fillMaxWidth()) {
        orb?.let { com.sperance.exileforge.ui.icons.OrbGlyph(it.orb, Modifier.size(22.dp)) } ?: Icon(ForgeGlyphs.Orb, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text(ui("orb.apply"))
    }
    // The server's sentence about the last orb, as the forge prints it under its item.
    s.play.forgeLine.takeIf { it.isNotBlank() }?.let { Text(it, color = Rune, style = MaterialTheme.typography.bodyMedium) }
    if (onGrant != null) ForgeOutlinedButton(enabled = enabled && s.isAdmin && s.play.selectedOrb.isNotBlank(),
        onClick = { onGrant(s.play.selectedOrb) }, modifier = Modifier.fillMaxWidth()) {
        Text(ui("orb.top_up", ORB_TOP_UP))
    }
}

/** How many orbs the administrator's top-up hands over at once — enough to try one out properly. */
const val ORB_TOP_UP = 10L

/**
 * The administrator's bench: any orb on any item of the character, whatever is in the bag.
 *
 * The target is picked here rather than in the stash, so an orb can be run over the same item
 * repeatedly without leaving the panel.
 */
@Composable fun AdminOrbPanel(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.play.hero ?: return
    val targets = hero.inventory.associate { instance ->
        val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
        instance.id to "${document.text("name")} · ${rarityTitle(instance.rarity, s.lang)}"
    }
    if (targets.isEmpty()) { Text(ui("orb.empty_inventory"), color = Muted); return }
    Spinner(ui("orb.target"), s.play.selectedEquipment, targets, !s.busy, glyph = Glyph.ITEM, onChange = vm::selectEquipment)
    OrbPanel(s, s.play.selectedEquipment, vm::selectOrb, vm::applyOrb) { orb -> vm.adjustItems(orb, ORB_TOP_UP) }
}
