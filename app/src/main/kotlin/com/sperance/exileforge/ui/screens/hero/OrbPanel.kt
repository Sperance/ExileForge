package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune
import org.koin.compose.viewmodel.koinViewModel

/**
 * The administrator's orb panel: one currency orb on one item, with a top-up beside it.
 *
 * A player spends orbs in the forge (`CraftScreen`); this is the same command behind the admin tab.
 *
 * Every orb of the content's `CURRENCY` category is offered, with the count the hero owns beside
 * it. What an orb does — which rarity it demands, what it rerolls, what it leaves alone — is
 * printed from its own rule and then decided by the server: the client sends the pair of codes and
 * shows the sentence that comes back, including a refusal.
 *
 * [onGrant] adds the administrator's top-up button, so an orb can be tried without farming it first.
 */
@Composable fun OrbPanel(
    game: GameUi,
    itemId: String,
    selected: String,
    onSelect: (String) -> Unit,
    onApply: (String, String) -> Unit,
    onGrant: ((String) -> Unit)? = null,
) {
    val hero = game.hero ?: return
    val instance = hero.item(itemId)
    val orbs = game.orbs
    val orb = orbs.firstOrNull { it.code == selected }
    val enabled = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    Engraved(ui("orb.title"))
    if (orbs.isEmpty()) {
        Text(ui("orb.none"), color = Muted)
        return
    }
    Spinner(
        ui("orb.orb"),
        selected,
        orbs.associate { it.code to "${itemTitle(it.code)} · ${hero.count(it.code)}" },
        enabled,
        glyph = Glyph.CURRENCY,
        optionArt = orbArt(orbs),
        onChange = onSelect,
    )
    // An orb the client has no translation for still explains itself: the server's dictionary has one.
    orb?.let { itemDescription(it.code).takeIf { rule -> rule.isNotBlank() } }?.let { MutedText(it) }
    if (instance == null) {
        Text(ui("orb.choose_item"), color = Muted)
        return
    }
    PropertyRow(ui("common.item"), game.view(instance)?.title ?: equipmentTitle(instance.template), Glyph.ITEM)
    PropertyRow(ui("orb.copy_rarity"), rarityTitle(instance.rarity, game.lang), Glyph.RARITY)
    if (instance.corrupted) Text(ui("orb.corrupted"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
    ForgeButton(
        enabled = enabled && !instance.corrupted && selected.isNotBlank(),
        onClick = { onApply(instance.id, selected) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Orb.of(selected)?.let { OrbGlyph(it, Modifier.size(22.dp)) } ?: Icon(ForgeGlyphs.Orb, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(ui("orb.apply"))
    }
    // The server's sentence about the last orb, as the forge prints it under its item.
    game.holding.forgeLine.takeIf { it.isNotBlank() }?.let { Text(it, color = Rune, style = MaterialTheme.typography.bodyMedium) }
    if (onGrant != null) {
        ForgeOutlinedButton(
            enabled = enabled && game.isAdmin && selected.isNotBlank(),
            onClick = { onGrant(selected) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(ui("orb.top_up", ORB_TOP_UP))
        }
    }
}

/** How many orbs the administrator's top-up hands over at once — enough to try one out properly. */
const val ORB_TOP_UP = 10L

/**
 * The administrator's bench: any orb on any item of the hero, whatever is in the bag.
 *
 * The target is picked here rather than in the stash, so an orb can be run over the same item
 * repeatedly without leaving the panel.
 */
@Composable fun AdminOrbPanel(game: GameUi) {
    val heroModel: HeroViewModel = koinViewModel()
    val smithy = koinViewModel<SmithyViewModel>()
    val chosen by smithy.smithy.collectAsStateWithLifecycle()
    val hero = game.hero ?: return
    val targets = hero.items.associate { instance ->
        instance.id to "${game.view(instance)?.title ?: equipmentTitle(instance.template)} · ${rarityTitle(instance.rarity, game.lang)}"
    }
    if (targets.isEmpty()) {
        Text(ui("orb.empty_inventory"), color = Muted)
        return
    }
    Spinner(ui("orb.target"), game.holding.selectedEquipment, targets, !game.busy, glyph = Glyph.ITEM, onChange = smithy::selectEquipment)
    OrbPanel(game, game.holding.selectedEquipment, chosen.orb, smithy::selectOrb, smithy::applyOrb) { orb -> heroModel.grantItem(orb, ORB_TOP_UP) }
}
