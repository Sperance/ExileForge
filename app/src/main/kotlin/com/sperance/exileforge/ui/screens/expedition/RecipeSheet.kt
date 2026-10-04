package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RaritySpine
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * A bench recipe the run turned up, behind a tap on its chip: what it is called, the line it puts on an item — the
 * bench's own sentence, with the tier's range — where it is used, what it costs, and the way to the bench itself. During a
 * run ([inRun]) the run holds the whole screen, so the way to the bench is only named, for after it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeSheet(game: GameUi, code: String, inRun: Boolean = false, onDismiss: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val expedition: ExpeditionViewModel = koinViewModel()
    val smithy = koinViewModel<SmithyViewModel>()
    val index = game.index
    val recipe = index?.recipe(code)
    val title = recipe?.let { r -> index?.modifier(r.modifier)?.effects?.map { statTitle(it.stat) }?.distinct()?.joinToString(" / ") }
        ?.ifBlank { null } ?: displayName(recipe?.modifier?.value ?: code)
    ForgeSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).navigationBarsPadding()) {
            RaritySpine(Crafted, 4.dp)
            Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(ForgeGlyphs.Anvil, null, tint = Crafted, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = GoldBright, style = MaterialTheme.typography.titleLarge)
                        MutedText(ui("expedition.report_recipe") + (recipe?.let { " · T${it.tier}" } ?: ""), style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (recipe != null && index != null) {
                    Column(Modifier.fillMaxWidth().background(Abyss, RoundedCornerShape(6.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(recipeText(index, recipe), color = ModBlue, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        MutedText(
                            ui("bench.cost_line", itemTitle(recipe.orb.name), recipe.amount, game.hero?.count(recipe.orb.name) ?: 0L),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (recipe.slots.isNotEmpty()) {
                            MutedText(
                                ui("recipe.slots", recipe.slots.joinToString(", ") { slotTitle(it) }),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Text(ui("recipe.applies"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                if (inRun) {
                    MutedText(ui("recipe.after_run"), style = MaterialTheme.typography.bodyMedium)
                } else {
                    ForgeButton(onClick = {
                        onDismiss()
                        smithy.open(null, ForgeSection.BENCH)
                        shell.tab(TAB_CRAFT)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(ui("recipe.open_forge"), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
