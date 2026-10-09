package com.sperance.exileforge.ui.screens.craft

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetLine
import com.sperance.exileforge.rules.roll.Menagerie

/**
 * Варианты строки знамения выбора над питомцем (сервер 1.65.0), что ждут решения игрока; нажатие ставит выбранную строку.
 * С 4.4.x питомец - цель кузницы наравне с вещью ([com.sperance.exileforge.presentation.forge.ForgeTarget.Beast]): это его
 * пара к [LineChoice] вещи.
 */
@Composable internal fun PetChoices(game: GameUi, pet: Pet, enabled: Boolean, onChoose: (String, Int) -> Unit) {
    val index = game.index ?: return
    if (pet.offer.isEmpty()) return
    val menagerie = remember(index) { Menagerie(index) }
    ChoiceFrame("forge.choice_title", "forge.choice_hint") {
        pet.offer.forEachIndexed { i, option ->
            val text = menagerie.lines(pet.copy(lines = listOf(option), offer = emptyList())).firstOrNull()?.let { lineText(index, it) } ?: displayName(option.code.value)
            ChoiceRow(text, "T${option.tier}") { if (enabled) onChoose(pet.id, i) }
        }
    }
}

/** A pet line has no tiers of its own; its rolls read as five steps, T1 the best, as an item's tiers do. */
private val PetLine.tier: Int get() = PET_TIERS - (shares.average().takeIf { it.isFinite() } ?: 0.0).times(PET_TIERS).toInt().coerceIn(0, PET_TIERS - 1)

private const val PET_TIERS = 5
