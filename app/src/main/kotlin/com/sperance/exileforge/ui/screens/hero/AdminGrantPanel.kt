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
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.screens.admin.AdminViewModel

/**
 * Administrator tools for one hero's inventory.
 *
 * The headline action rolls an item: the client picks a template of the chosen rarity and slot out
 * of the content, the server rolls its modifiers, their tiers and their values when the copy is made.
 * The templates and the stacks are picked from the content on the device — there is no
 * catalogue to page through — and the bag is only ever added to: the server takes nothing back.
 *
 * Редкость и слот случайной выдачи - черновик панели, пусто - любые; редкость общая для случайного и названного шаблона, пусто - своя у
 * шаблона. Количество только положительное: сервер стопки не забирает.
 */
@Composable fun AdminGrantPanel(game: GameUi, vm: AdminViewModel) {
    if (!game.isAdmin || game.heroId.isBlank()) return
    var expanded by remember(game.heroId) { mutableStateOf(true) }
    val enabled = !game.busy && game.session.signedIn && game.heroId.isNotBlank()
    val any = ui("grant.any")
    var rarity by rememberSaveable { mutableStateOf("") }
    var slot by rememberSaveable { mutableStateOf("") }
    ForgeButton(onClick = { expanded = !expanded }, kind = ButtonKind.Text) {
        Text(ui("grant.title", if (expanded) ui("common.hide") else ui("common.show")))
    }
    if (!expanded) return
    val templates = remember(game.index, game.lang) {
        game.index?.templates?.values.orEmpty().sortedBy { it.code }.associate { it.code to "${equipmentTitle(it.code)} · ${it.code}" }
    }
    val stacks = remember(game.index, game.lang) {
        game.index?.items?.values.orEmpty().sortedWith(compareBy({ it.category }, { it.code })).associate { it.code.value to "${itemTitle(it.code.value)} · ${it.code}" }
    }
    ForgePanel {
        Engraved(ui("grant.random_item"))
        Spinner(ui("common.rarity"), rarity, mapOf("" to any) + Rarity.entries.associate { it.name to rarityTitle(it, game.lang) }, enabled, glyph = Glyph.RARITY, onChange = { rarity = it })
        Spinner(ui("grant.category"), slot, mapOf("" to any) + Slot.entries.associate { it.name to slotTitle(it, game.lang) }, enabled, glyph = Glyph.ITEM, onChange = { slot = it })
        ForgeButton(state = ButtonState.of(enabled), onClick = { vm.grantRandom(rarity, slot) }, modifier = Modifier.fillMaxWidth()) {
            Icon(ForgeGlyphs.Anvil, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(ui("grant.roll"))
        }
        MutedText(ui("grant.roll_note"))

        OrnateDivider()
        Engraved(ui("grant.named_template"))
        var template by remember(game.heroId) { mutableStateOf("") }
        Spinner(ui("grant.equipment"), template, templates, enabled && templates.isNotEmpty(), glyph = Glyph.ITEM) { template = it }
        ForgeButton(state = ButtonState.of(enabled && template in templates), onClick = { vm.grant(template, Rarity.of(rarity)) }) { Text(ui("grant.chosen_item")) }

        OrnateDivider()
        Engraved(ui("grant.orbs"))
        MutedText(ui("grant.orbs_note"))
        AdminOrbPanel(game)

        OrnateDivider()
        Engraved(ui("grant.experience"))
        var experience by remember(game.heroId) { mutableStateOf("100") }
        OutlinedTextField(
            experience,
            { experience = it },
            label = { Text(ui("grant.grant_xp")) },
            supportingText = { Text(ui("grant.xp_note")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ForgeButton(
            state = ButtonState.of(enabled && experience.toDoubleOrNull()?.let { it > 0 && it.isFinite() } == true),
            onClick = { vm.addExperience(experience.toDouble()) },
        ) { Text(ui("grant.grant")) }

        OrnateDivider()
        Engraved(ui("grant.stacking"))
        var code by remember(game.heroId) { mutableStateOf("") }
        var amount by remember(game.heroId) { mutableStateOf("1") }
        Spinner(ui("common.item"), code, stacks, enabled && stacks.isNotEmpty(), glyph = Glyph.CURRENCY, optionArt = orbArt(game.orbs)) { code = it }
        OutlinedTextField(
            amount,
            { value -> amount = value.filter(Char::isDigit) },
            label = { Text(ui("auction.amount")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ForgeButton(
            state = ButtonState.of(enabled && code in stacks && amount.toLongOrNull()?.let { it > 0L } == true),
            onClick = { vm.grantItem(code, amount.toLong()) },
        ) { Text(ui("grant.chosen_item")) }
    }
}
