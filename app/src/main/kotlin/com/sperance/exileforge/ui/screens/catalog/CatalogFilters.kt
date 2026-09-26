package com.sperance.exileforge.ui.screens.catalog

import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.ui.components.MutedText

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.weapons
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Spinner
import com.sperance.exileforge.core.model.modifier.PoolKind
import com.sperance.exileforge.ui.components.ForgeTextButton

@Composable fun CatalogFilters(s: ForgeState, vm: ForgeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ForgeTextButton(onClick = { expanded = !expanded }) { Text(ui("catalog.filters", if (expanded) ui("common.hide") else ui("common.show"))) }
    if (!expanded) return
    val f = s.admin.filter
    val any = ui("common.all")
    Spinner(ui("common.slot"), f.slot, mapOf("" to any) + slots.associateWith { slotTitle(it, s.lang) }, !s.busy, glyph = Glyph.ITEM) { vm.filter(f.copy(slot = it)) }
    Spinner(ui("common.rarity"), f.rarity, mapOf("" to any) + rarities.associateWith { rarityTitle(it, s.lang) }, !s.busy, glyph = Glyph.RARITY) { vm.filter(f.copy(rarity = it)) }
    Spinner(ui("card.weapon_type"), f.weaponType, mapOf("" to any) + weapons.associateWith { weaponTitle(it, s.lang) }, !s.busy, glyph = Glyph.ATTACK) { vm.filter(f.copy(weaponType = it)) }
    OutlinedTextField(f.minLevel, { vm.filter(f.copy(minLevel = it)) }, label = { Text(ui("catalog.level_from")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(f.maxLevel, { vm.filter(f.copy(maxLevel = it)) }, label = { Text(ui("catalog.level_to")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    ForgeTextButton(enabled = !s.busy, onClick = vm::loadDefinitions) { Text(ui("catalog.load_modifiers")) }
    // Pools are one list of the world since server 0.56.0: tags of modifiers and of templates alike.
    val pools = s.world.pools.filter { it.kind != PoolKind.MONSTER }.map { it.code }.toSortedSet()
    Spinner(ui("catalog.pool"), f.pool, mapOf("" to ui("common.any")) + pools.associateWith { it }, !s.busy, glyph = Glyph.RULE) { vm.filter(f.copy(pool = it)) }
}
