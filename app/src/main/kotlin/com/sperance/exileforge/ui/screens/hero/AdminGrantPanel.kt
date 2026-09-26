package com.sperance.exileforge.ui.screens.hero

import com.sperance.exileforge.core.display.Glyph

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs

/**
 * Administrator tools for one character's inventory.
 *
 * The headline action rolls an item: the client picks a template of the chosen rarity and category,
 * the server rolls its modifiers, their tiers and their values when the instance is created.
 */
@Composable fun AdminGrantPanel(s: ForgeState, vm: ForgeViewModel) {
    if (!s.adminTools || s.play.characterId.isBlank()) return
    var expanded by remember(s.play.characterId) { mutableStateOf(true) }
    val enabled = !s.busy && s.account.signedIn && s.play.characterId.isNotBlank()
    val any = ui("grant.any")
    ForgeTextButton(onClick = { expanded = !expanded }) {
        Text(ui("grant.title", if (expanded) ui("common.hide") else ui("common.show")))
    }
    if (!expanded) return
    ForgePanel {
        Engraved(ui("grant.random_item"))
        Spinner(ui("common.rarity"), s.play.grantRarity, mapOf("" to any) + rarities.associateWith { rarityTitle(it, s.lang) }, enabled, glyph = Glyph.RARITY, onChange = vm::grantRarity)
        Spinner(ui("grant.category"), s.play.grantSlot, mapOf("" to any) + slots.associateWith { slotTitle(it, s.lang) }, enabled, glyph = Glyph.ITEM, onChange = vm::grantSlot)
        ForgeButton(enabled = enabled, onClick = vm::grantRandom, modifier = Modifier.fillMaxWidth()) {
            Icon(ForgeGlyphs.Anvil, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(ui("grant.roll"))
        }
        MutedText(ui("grant.roll_note"))

        OrnateDivider()
        Engraved(ui("grant.named_template"))
        var equipmentId by remember(s.play.characterId) { mutableStateOf("") }
        EntitySpinner(ui("grant.equipment"), equipmentId, EntitySource.EQUIPMENT, enabled) { equipmentId = it }
        ForgeButton(enabled = enabled && equipmentId.isNotBlank(), onClick = { vm.grant(equipmentId) }) { Text(ui("grant.chosen_item")) }

        OrnateDivider()
        Engraved(ui("grant.orbs"))
        MutedText(ui("grant.orbs_note"))
        AdminOrbPanel(s, vm)

        OrnateDivider()
        Engraved(ui("grant.experience"))
        var experience by remember(s.play.characterId) { mutableStateOf("100") }
        OutlinedTextField(experience, { experience = it }, label = { Text(ui("grant.grant_xp")) },
            supportingText = { Text(ui("grant.xp_note")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        ForgeButton(enabled = enabled && experience.toDoubleOrNull()?.let { it > 0 && it.isFinite() } == true,
            onClick = { vm.addExperience(experience.toDouble()) }) { Text(ui("grant.grant")) }

        OrnateDivider()
        Engraved(ui("grant.stacking"))
        var itemId by remember(s.play.characterId) { mutableStateOf("") }
        var amount by remember(s.play.characterId) { mutableStateOf("1") }
        EntitySpinner(ui("common.item"), itemId, EntitySource.ITEM, enabled) { itemId = it }
        OutlinedTextField(amount, { amount = it }, label = { Text(ui("grant.change_amount_label")) },
            supportingText = { Text(ui("grant.negative_note")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ForgeButton(enabled = enabled && itemId.isNotBlank() && amount.toLongOrNull()?.let { it != 0L } == true,
            onClick = { vm.adjustItems(itemId, amount.toLong()) }) { Text(ui("grant.change_amount")) }
    }
}
