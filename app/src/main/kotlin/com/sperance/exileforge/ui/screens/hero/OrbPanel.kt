package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted

/**
 * Applying one currency orb to one item of the inventory.
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
    val hero = s.hero ?: return
    val instance = hero.inventory.firstOrNull { it.id == instanceId }
    val owned = hero.bag.associate { it.itemId to it.amount }
    val orb = s.orbs.firstOrNull { it.id == s.selectedOrb }
    val enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin)
    Engraved(tr("Сферы", "Currency orbs"))
    if (s.orbs.isEmpty()) { Text(tr("Сервер не отдал ни одной сферы", "The server served no orbs"), color = Muted); return }
    Spinner(tr("Сфера", "Orb"), s.selectedOrb,
        s.orbs.associate { it.id to "${it.title(s.lang)} · ${owned[it.id] ?: 0L}" }, enabled, onSelect)
    // An orb the client has no translation for still explains itself: the server seeded a description.
    orb?.let { Text(it.orb?.rule(s.lang) ?: it.description, color = Muted, style = MaterialTheme.typography.bodySmall) }
    if (instance == null) { Text(tr("Выберите предмет в арсенале", "Choose an item in the stash"), color = Muted); return }
    val document = inventoryDocument(instance, s.inventoryBases[instance.equipmentId])
    PropertyRow(tr("Предмет", "Item"), document.text("name"), "item")
    PropertyRow(tr("Редкость экземпляра", "Copy's rarity"), rarityTitle(instance.rarity, s.lang), "rarity")
    if (instance.corrupted) Text(tr("Предмет испорчен: сервер больше не примет на него ни одной сферы.",
        "The item is corrupted: the server accepts no further orb on it."), color = LifeRed, style = MaterialTheme.typography.bodySmall)
    Button(enabled = enabled && !instance.corrupted && s.selectedOrb.isNotBlank(),
        onClick = { onApply(instance.id, s.selectedOrb) }, modifier = Modifier.fillMaxWidth()) {
        Icon(ForgeGlyphs.Orb, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text(tr("Применить сферу", "Apply the orb"))
    }
    if (onGrant != null) OutlinedButton(enabled = enabled && s.isAdmin && s.selectedOrb.isNotBlank(),
        onClick = { onGrant(s.selectedOrb) }, modifier = Modifier.fillMaxWidth()) {
        Text(tr("Выдать $ORB_TOP_UP таких сфер", "Grant $ORB_TOP_UP of these orbs"))
    }
    Text(tr("Сфера списывается сервером в той же транзакции, поэтому отказ ничего не стоит.",
            "The orb is debited by the server in the same transaction, so a refusal costs nothing."),
        color = Muted, style = MaterialTheme.typography.bodySmall)
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
    val hero = s.hero ?: return
    val targets = hero.inventory.associate { instance ->
        val document = inventoryDocument(instance, s.inventoryBases[instance.equipmentId])
        instance.id to "${document.text("name")} · ${rarityTitle(instance.rarity, s.lang)}"
    }
    if (targets.isEmpty()) { Text(tr("Инвентарь пуст — выдайте предмет выше", "The inventory is empty — grant an item above"), color = Muted); return }
    Spinner(tr("Предмет для сферы", "Item to use it on"), s.selectedEquipment, targets, !s.busy, vm::selectEquipment)
    OrbPanel(s, s.selectedEquipment, vm::selectOrb, vm::applyOrb) { orb -> vm.adjustItems(orb, ORB_TOP_UP) }
}
