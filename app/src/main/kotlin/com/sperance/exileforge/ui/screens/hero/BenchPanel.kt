package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.affixMarks
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Crafted
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * The crafting bench over one item: a crafted modifier placed for orbs, or taken back off.
 *
 * The lines are the server's and are offered for the item's slot only — that much is a fact the
 * bench states, not a rule the client works out. Everything else — one crafted modifier per item,
 * a free place of the right kind, no twin of the same group, the price — is checked when the
 * command arrives, and a refusal costs nothing, so nothing here is disabled on a guess.
 */
@Composable fun BenchPanel(s: ForgeState, instanceId: String, onCraft: (String, String) -> Unit, onUncraft: (String) -> Unit) {
    val hero = s.play.hero ?: return
    val instance = hero.inventory.firstOrNull { it.id == instanceId } ?: return
    val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
    val owned = hero.bag.associate { it.itemId to it.amount }
    val enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    val recipes = s.world.bench.filter { it.fits(document.text("slot")) }
        .sortedWith(compareBy({ it.source }, { it.modifierCode }, { -it.tier }))
    var chosen by remember(instanceId) { mutableStateOf(recipes.firstOrNull()?.code.orEmpty()) }
    val recipe = recipes.firstOrNull { it.code == chosen }
    val crafted = (document["params"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        .firstOrNull { affixMarks(it, s.world.definitions).crafted }
    fun orbTitle(code: String) = s.world.orbs.firstOrNull { it.subCategory == code }?.title(s.lang)
        ?: CurrencyOrb.of(code)?.title(s.lang) ?: code

    Engraved(ui("bench.title"))
    PropertyRow(ui("common.item"), document.text("name"), Glyph.ITEM)
    crafted?.let {
        PropertyRow(ui("bench.current"), modifierText(it, s.world.definitions), Glyph.CRAFT)
        val scouring = s.world.orbs.firstOrNull { orb -> orb.orb == CurrencyOrb.ORB_OF_SCOURING }
        OutlinedButton(enabled = enabled, onClick = { onUncraft(instance.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("bench.remove") + " · " + ui("bench.cost_line", orbTitle(CurrencyOrb.ORB_OF_SCOURING.name), 1, owned[scouring?.id] ?: 0L))
        }
    }
    if (recipes.isEmpty()) { Text(ui("bench.none"), color = Muted); return }
    Spinner(ui("bench.recipe"), chosen,
        recipes.associate { it.code to ui("bench.recipe_line", it.tier, recipeText(it, s.world.definitions)) },
        enabled, glyph = Glyph.CRAFT) { chosen = it }
    recipe?.let { PropertyRow(ui("bench.cost"), ui("bench.cost_line", orbTitle(it.orb), it.amount, owned[it.orbItemId] ?: 0L), Glyph.CURRENCY) }
    Button(enabled = enabled && recipe != null, onClick = { recipe?.let { onCraft(instance.id, it.code) } }, modifier = Modifier.fillMaxWidth()) {
        Icon(ForgeGlyphs.Anvil, null, Modifier.size(18.dp), tint = Crafted); Spacer(Modifier.width(8.dp))
        Text(ui("bench.craft"))
    }
    Text(ui("bench.note"), color = Muted, style = MaterialTheme.typography.bodySmall)
}
