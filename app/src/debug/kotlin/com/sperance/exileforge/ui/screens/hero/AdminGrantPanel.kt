package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.orbArt

/**
 * Administrator tools for one hero's inventory.
 *
 * The headline action rolls an item: the client picks a template of the chosen rarity and slot out
 * of the content, the server rolls its modifiers, their tiers and their values when the copy is made.
 * Since 3.0.0 the templates and the stacks are picked from the content on the device — there is no
 * catalogue to page through — and the bag is only ever added to: the server takes nothing back.
 */
@Composable fun AdminGrantPanel(s: ForgeState, vm: ForgeViewModel) {
    if (!s.adminTools || s.play.heroId.isBlank()) return
    var expanded by remember(s.play.heroId) { mutableStateOf(true) }
    val enabled = !s.busy && s.account.signedIn && s.play.heroId.isNotBlank()
    val any = ui("grant.any")
    // Редкость и слот случайной выдачи (3.80.32: черновик панели); пусто - любые.
    var rarity by rememberSaveable { mutableStateOf("") }
    var slot by rememberSaveable { mutableStateOf("") }
    ForgeTextButton(onClick = { expanded = !expanded }) {
        Text(ui("grant.title", if (expanded) ui("common.hide") else ui("common.show")))
    }
    if (!expanded) return
    // The content's templates and stacks, named and keyed by code: what the pickers search through.
    val templates = remember(s.index, s.lang) {
        s.index?.templates?.values.orEmpty().sortedBy { it.code }.associate { it.code to "${equipmentTitle(it.code)} · ${it.code}" }
    }
    val stacks = remember(s.index, s.lang) {
        s.index?.items?.values.orEmpty().sortedWith(compareBy({ it.category }, { it.code })).associate { it.code to "${itemTitle(it.code)} · ${it.code}" }
    }
    ForgePanel {
        Engraved(ui("grant.random_item"))
        // The rarity is the roll's for a random template and a named one alike; blank leaves the template's own.
        Spinner(ui("common.rarity"), rarity, mapOf("" to any) + Rarity.entries.associate { it.name to rarityTitle(it, s.lang) }, enabled, glyph = Glyph.RARITY, onChange = { rarity = it })
        Spinner(ui("grant.category"), slot, mapOf("" to any) + Slot.entries.associate { it.name to slotTitle(it, s.lang) }, enabled, glyph = Glyph.ITEM, onChange = { slot = it })
        ForgeButton(enabled = enabled, onClick = { vm.grantRandom(rarity, slot) }, modifier = Modifier.fillMaxWidth()) {
            Icon(ForgeGlyphs.Anvil, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(ui("grant.roll"))
        }
        MutedText(ui("grant.roll_note"))

        OrnateDivider()
        Engraved(ui("grant.named_template"))
        var template by remember(s.play.heroId) { mutableStateOf("") }
        Spinner(ui("grant.equipment"), template, templates, enabled && templates.isNotEmpty(), glyph = Glyph.ITEM) { template = it }
        ForgeButton(enabled = enabled && template in templates, onClick = { vm.grant(template, Rarity.of(rarity)) }) { Text(ui("grant.chosen_item")) }

        OrnateDivider()
        Engraved(ui("grant.orbs"))
        MutedText(ui("grant.orbs_note"))
        AdminOrbPanel(s)

        OrnateDivider()
        Engraved(ui("grant.experience"))
        var experience by remember(s.play.heroId) { mutableStateOf("100") }
        OutlinedTextField(
            experience,
            { experience = it },
            label = { Text(ui("grant.grant_xp")) },
            supportingText = { Text(ui("grant.xp_note")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ForgeButton(
            enabled = enabled && experience.toDoubleOrNull()?.let { it > 0 && it.isFinite() } == true,
            onClick = { vm.addExperience(experience.toDouble()) },
        ) { Text(ui("grant.grant")) }

        OrnateDivider()
        Engraved(ui("grant.stacking"))
        var code by remember(s.play.heroId) { mutableStateOf("") }
        var amount by remember(s.play.heroId) { mutableStateOf("1") }
        Spinner(ui("common.item"), code, stacks, enabled && stacks.isNotEmpty(), glyph = Glyph.CURRENCY, optionArt = orbArt(s.orbs)) { code = it }
        OutlinedTextField(
            amount,
            { value -> amount = value.filter(Char::isDigit) },
            label = { Text(ui("auction.amount")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // A positive amount only: the server hands stacks out and never takes them back (3.0.0).
        ForgeButton(
            enabled = enabled && code in stacks && amount.toLongOrNull()?.let { it > 0L } == true,
            onClick = { vm.grantItem(code, amount.toLong()) },
        ) { Text(ui("grant.chosen_item")) }
    }
}
