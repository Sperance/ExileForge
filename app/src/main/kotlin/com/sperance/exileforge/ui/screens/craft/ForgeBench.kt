package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.forge.Smithy
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Верстак и выбор строки (3.80.24): рецепты, предзнаменования в руках, раскрытие и выбор. */
/**
 * The bench lines for this item's slot, and the crafted modifier it already carries, if any.
 * Only the recipes the hero has found are offered (3.0.0); the rest of the bench stays hidden.
 */
@Composable internal fun BenchLedger(game: GameUi, index: ContentIndex, hero: HeroView, item: ItemView, chosen: String, onChoose: (String) -> Unit) {
    val recipes = game.bench.filter { it.fits(item.slot) }.sortedWith(compareBy({ it.source }, { it.modifier }, { -it.tier }))
    val crafted = item.lines.firstOrNull { it.marks.crafted }
    val scouring = index.rules.bench.uncraftOrb
    Column {
        crafted?.let {
            LedgerRow(
                ForgeGlyphs.Anvil,
                Crafted,
                it.text,
                ui("bench.current") + " · " + ui("bench.cost_line", itemTitle(scouring.name), 1, hero.count(scouring.name)),
                "×",
                selected = chosen == UNCRAFT,
                ink = ModBlue,
            ) { onChoose(UNCRAFT) }
        }
        recipes.forEach { recipe ->
            LedgerRow(
                ForgeGlyphs.Anvil,
                Crafted,
                recipeText(index, recipe),
                ui("bench.cost_line", itemTitle(recipe.orb.name), recipe.amount, hero.count(recipe.orb.name)),
                "T${recipe.tier}",
                selected = chosen == recipe.code,
                ink = ModBlue,
            ) { onChoose(recipe.code) }
        }
    }
    if (recipes.isEmpty()) Text(ui("bench.none"), color = Muted)
}

/** One line of a forge ledger: a spine lit when chosen, a drawing, a name over what it means, and a figure. */
@Composable internal fun LedgerRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    figure: String,
    selected: Boolean,
    orb: Orb? = null,
    ink: Color = Parchment,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(role = Role.Button, onClick = onClick)
            .background(if (selected) Panel else Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RaritySpine(if (selected) GoldBright else accent.copy(alpha = .35f), 3.dp)
        // An orb's line wears its stained glass (2.69.0); the bench and the rest keep their glyph.
        if (orb != null) {
            OrbGlyph(orb, Modifier.size(28.dp))
        } else {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
            Text(title, color = if (selected) GoldBright else ink, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(figure, color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp))
    }
    HorizontalDivider(color = PanelRaised)
}

/** The omens the bag holds (3.36.0; server 1.65.0: the catalysts are omens of the Orb of Quality), in the content's order. */
internal fun heldOmens(index: ContentIndex, hero: HeroView): List<Omen> = index.itemsByCategory[Item.OMEN].orEmpty().mapNotNull { item -> Omen.of(item.code)?.takeIf { hero.count(item.code) > 0 } }

/**
 * A choice of lines waiting on the item: the unveiling's offer (3.36.0) — each modifier the veiled one may become — or the
 * Omen of Choice's (server 1.65.0) — each line an Orb of Alchemy or an Exalted Orb may add. One tap keeps it and the rest are lost.
 */
@Composable internal fun LineChoice(
    game: GameUi,
    instance: ItemInstance,
    options: List<Roll>,
    title: String,
    hint: String,
    enabled: Boolean,
    onChoose: (String, Int) -> Unit,
) {
    if (options.isEmpty()) return
    ChoiceFrame(title, hint) {
        options.forEachIndexed { i, option ->
            val text = game.view(instance.copy(rolls = listOf(option), unveil = emptyList(), offer = emptyList()))?.lines?.firstOrNull()?.text.orEmpty()
            ChoiceRow(text, "T${option.tier}") { if (enabled) onChoose(instance.id, i) }
        }
    }
}

/** The frame of a choice of lines — an item's or a pet's: the title, how it works, and the options under them. */
@Composable fun ChoiceFrame(title: String, hint: String, options: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, Rune, MaterialTheme.shapes.small).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui(title), color = Rune, style = MaterialTheme.typography.titleMedium)
        MutedText(ui(hint))
        options()
    }
}

/** One option of a [ChoiceFrame]: the line as it would read, and a figure beside it. */
@Composable fun ChoiceRow(text: String, figure: String, onClick: () -> Unit) = LedgerRow(ForgeGlyphs.Sigil, Rune, text, "", figure, selected = false, ink = ModBlue, onClick = onClick)
