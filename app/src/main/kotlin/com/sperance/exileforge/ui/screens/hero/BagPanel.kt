package com.sperance.exileforge.ui.screens.hero

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.Muted

/**
 * The bag: everything the character owns that is not an equipment instance.
 *
 * Promo codes moved to the Account tab and recipes to the forge, so what is left is the one thing
 * the name promised. A stack has no card of its own — it is an id and a count — so these rows open
 * nothing, and the section is folded away because it is read on purpose, not at a glance.
 */
@Composable fun BagPanel(s: ForgeState) {
    val hero = s.hero ?: return
    var expanded by remember(s.characterId) { mutableStateOf(false) }
    ExpandableSection(ui("hero.bag"), hero.bag.size, expanded, { expanded = !expanded }) {
        if (hero.bag.isEmpty()) Text(ui("hero.bag_empty"), color = Muted)
        // Currency is named from the catalogue the hero screen already read; anything else is an id.
        hero.bag.forEach { item ->
            val orb = s.orbs.firstOrNull { it.id == item.itemId }
            PropertyRow(orb?.title(s.lang) ?: (ui("common.item") + " …${item.itemId.takeLast(6)}"), item.amount.toString(), "item")
        }
    }
}
